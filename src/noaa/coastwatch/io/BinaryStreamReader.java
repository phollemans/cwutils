////////////////////////////////////////////////////////////////////////
/*
     FILE: BinaryStreamReader.java
  PURPOSE: Reads streams of byte data described by an XML document.
   AUTHOR: Peter Hollemans
     DATE: 2007/09/01
  CHANGES: 2007/011/22, PFH, updated to print array values in main()

  CoastWatch Software Library and Utilities
  Copyright 2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.reflect.*;
import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.validation.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * The <code>BinaryStreamReader</code> class reads streams of
 * binary data described by an XML template.
 *
 * <pre>
 *   Insert example XML template here.
 * </pre>
 */
public class BinaryStreamReader {

  // Constants
  // ---------

  /** The enumeration of allowed binary types for reading. */
  private enum ValueType {
    BYTE,
    UBYTE,
    SHORT,
    USHORT,
    INT,
    UINT,
    LONG,
    ULONG,
    FLOAT,
    DOUBLE,
    BYTEARRAY,
    UBYTEARRAY,
    SHORTARRAY,
    USHORTARRAY,
    INTARRAY,
    UINTARRAY,
    LONGARRAY,
    ULONGARRAY,
    FLOATARRAY,
    DOUBLEARRAY,
    STRING
  };

  /** The enumeration of allowed label types for reading. */
  private enum LabelType {
    BITLABEL,
    NBITLABEL
  };

  /** The resource used for parsing templates. */
  private static final String SCHEMA_RESOURCE = "binaryStream.xsd";

  // Variables
  // ---------
  
  /** The document builder for parsing binary stream templates. */
  private static DocumentBuilder docBuilder;

  /** The map of name to value or label info. */
  private Map<String,Object> infoMap;

  ////////////////////////////////////////////////////////////

  static {

    try {

      // Load XML template schema
      // ------------------------
      SchemaFactory schemaFactory =
        SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = 
        schemaFactory.newSchema (BinaryStreamReader.class.getResource (
          SCHEMA_RESOURCE));

      // Create document builder
      // -----------------------
      DocumentBuilderFactory builderFactory = 
        DocumentBuilderFactory.newInstance();
      builderFactory.setSchema (schema);
      builderFactory.setNamespaceAware (true);
      docBuilder = builderFactory.newDocumentBuilder();

    } // try
    catch (Exception e) {
      throw new RuntimeException (e.toString());
    } // catch

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Reads a set of values for a value mapping and returns the
   * (possibly empty) map.
   *
   * @param element the document element containing the value map.
   * @param keyType the value type for map keys.
   *
   * @return the map of values.
   */
  private Map<Object,Object> getValueMap (
    Element element,
    ValueType keyType
  ) {

    // Create value map
    // ----------------
    Map<Object,Object> map = new HashMap<Object,Object>();

    // Find value map
    // --------------
    NodeList nodeList = element.getChildNodes();
    Element mapElement = null;
    for (int i = 0; i < nodeList.getLength(); i++) {
      if (nodeList.item (i).getNodeName().equals ("valueMap")) {
        mapElement = (Element) nodeList.item (i);
        break;
      } // if
    } // for

    // Create map of keys to values
    // ----------------------------
    if (mapElement != null) {
      ValueType valueType = ValueType.valueOf (ValueType.class, 
        mapElement.getAttribute ("valueType").toUpperCase());
      NodeList entryList = mapElement.getElementsByTagName ("entry");

      // Loop over each map entry
      // ------------------------
      for (int i = 0; i < entryList.getLength(); i++) {
        Element entryElement = (Element) entryList.item (i);

        // Check for key
        // -------------
        Object key = convertText (entryElement.getAttribute ("key"), keyType);
        if (map.containsKey (key)) {
          throw new IllegalStateException ("Multiple mapping for key '" + 
            key + "'");
        } // if

        // Add entry to map
        // ----------------
        Object value = convertText (entryElement.getAttribute ("value"), 
          valueType);
        map.put (key, value);

      } // for

    } // if

    return (map);

  } // getValueMap

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>LabelInfo</code> class holds information needed to
   * retrieve a labeled bit sequence within a value.
   */
  private class LabelInfo {

    // Variables
    // ---------

    /** The label name. */
    public String name;

    /** The type of label: BITLABEL or NBITLABEL. */
    public LabelType labelType;

    /** The mask to apply after reading the main value. */
    public long mask;

    /** The right shift to apply after reading the main value. */
    public int rightShift;

    /** The map of values to outputs. */
    public Map<Object,Object> valueMap;

    /** The value info for this label. */
    public ValueInfo valueInfo;

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new label info using the XML element and value info.
     *
     * @param element the document element for the new label info
     * object.
     * @param valueInfo the value info for this label.
     */
    public LabelInfo (
      Element element,
      ValueInfo valueInfo
    ) {

      // Initialize
      // ----------
      name = element.getAttribute ("name");
      String tag = element.getTagName();
      labelType = LabelType.valueOf (LabelType.class, tag.toUpperCase());
      this.valueInfo = valueInfo;
      valueMap = getValueMap (element, ValueType.LONG);

      // Compute mask and shift
      // ----------------------
      switch (labelType) {
      case BITLABEL:
        int position = Integer.parseInt (element.getAttribute ("position"));
        mask = 0x1L << position;
        rightShift = position;
        break;
      case NBITLABEL:
        String[] rangeArray = element.getAttribute ("range").split ("-");
        int startBit = Integer.parseInt (rangeArray[0]);
        int endBit = Integer.parseInt (rangeArray[1]);
        mask = 0L;
        for (int i = startBit; i <= endBit; i++)
          mask |= (0x1L << i);
        rightShift = startBit;
        break;
      } // switch

    } // LabelInfo constructor

    ////////////////////////////////////////////////////////

  } // LabelInfo class

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>ValueInfo</code> class holds information needed to
   * retrieve a value.
   */
  private class ValueInfo {

    // Variables
    // ---------
    
    /** The value name. */
    public String name;

    /** The value type. */
    public ValueType valueType;

    /** The offset into the binary stream. */
    public int offset;

    /** The scale to apply after reading. */
    public double scale;

    /** The number of values to read. */
    public int length;

    /** The map of values to outputs. */
    public Map<Object,Object> valueMap;

    /** The list of labels for this value. */
    public List<LabelInfo> labelList;

    ////////////////////////////////////////////////////////

    /**
     * Gets a (possibly empty) list of labels for a value.
     * 
     * @param element the document element containing the labels.
     *
     * @return the list of labels. 
     */
    private List<LabelInfo> getLabels (
      Element element
    ) {

      // Create label list
      // -----------------
      List<LabelInfo> labelList = new ArrayList<LabelInfo>();

      // Get lebels
      // ----------
      NodeList nodeList = element.getElementsByTagName ("bitLabel");
      for (int i = 0; i < nodeList.getLength(); i++)
        labelList.add (new LabelInfo ((Element) nodeList.item (i), this));
      nodeList = element.getElementsByTagName ("nbitLabel");
      for (int i = 0; i < nodeList.getLength(); i++)
        labelList.add (new LabelInfo ((Element) nodeList.item (i), this));

      return (labelList);

    } // getLabels

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new info using the XML element and current
     * byte offset.
     *
     * @param element the document element for the new info object.
     * @param offset the current offset into the binary
     * stream.
     */
    public ValueInfo (
      Element element,
      int offset
    ) {

      // Get name and type
      // -----------------
      name = element.getAttribute ("name");
      String tag = element.getTagName();
      valueType = ValueType.valueOf (ValueType.class, tag.toUpperCase());
      valueMap = getValueMap (element, valueType);
      labelList = getLabels (element);

      // Get offset value
      // ----------------
      String attValue = element.getAttribute ("absoluteOffset");
      if (!attValue.equals (""))
        this.offset = Integer.parseInt (attValue);
      else {
        attValue = element.getAttribute ("relativeOffset");
        if (!attValue.equals (""))
          this.offset = offset + Integer.parseInt (attValue);
        else
          this.offset = offset;
      } // else
      
      // Get scaling factor
      // ------------------
      attValue = element.getAttribute ("scale");
      if (!attValue.equals (""))
        scale = Double.parseDouble (attValue);
      else
        scale = Double.NaN;
      
      // Get string length
      // -----------------
      if (valueType == ValueType.STRING) {
        attValue = element.getAttribute ("length");
        length = Integer.parseInt (attValue);
      } // if

      // Get array dimensions
      // --------------------
      else if (isArrayType (valueType)) {
        attValue = element.getAttribute ("dims");
        String[] dimsArray = attValue.split (" +");
        length = 1;
        for (int i = 0; i < dimsArray.length; i++)
          length *= Integer.parseInt (dimsArray[i]);
      } // else if
      
      // Set length of 1
      // ---------------
      else {
        length = 1;
      } // else
 
    } // ValueInfo constructor

    ////////////////////////////////////////////////////////

    public String toString() {

      return (
        "ValueInfo[" +
        "name=" + name + "," +
        "valueType=" + valueType + "," +
        "offset=" + offset + "," +
        "scale=" + scale + "," +
        "length=" + length + "," +
        "valueMap=" + valueMap + "]"
      );

    } // toString

    ////////////////////////////////////////////////////////

    /**
     * Computes the next byte offset after this value, taking
     * into account the value type and number of values.
     *
     * @return the next offset in bytes.
     */
    public int getNextOffset () {

      return (offset + getTypeSize (valueType)*length);

    } // getNextOffset

    ////////////////////////////////////////////////////////

  } // ValueInfo class

  ////////////////////////////////////////////////////////////

  /** 
   * Converts a text value to a typed value.
   *
   * @param text the text string to convert.
   * @param type the type to convert to.
   *
   * @return the typed value.
   */
  public static Object convertText (
    String text,
    ValueType type
  ) {

    switch (type) {
    case BYTE: case BYTEARRAY:
      return (new Byte (text));
    case SHORT: case SHORTARRAY: case UBYTE: case UBYTEARRAY:
      return (new Short (text));
    case INT: case INTARRAY: case USHORT: case USHORTARRAY:
      return (new Integer (text));
    case LONG: case LONGARRAY: case UINT: case UINTARRAY: 
    case ULONG: case ULONGARRAY:
      return (new Long (text));
    case FLOAT: case FLOATARRAY:
      return (new Float (text));
    case DOUBLE: case DOUBLEARRAY:
      return (new Double (text));
    case STRING: 
      return (text);
    default:
      throw new IllegalArgumentException ("Unknown type: " + type);
    } // switch
    
  } // convertText
  
  ////////////////////////////////////////////////////////////

  /** Gets the size of a value type. */
  private static int getTypeSize (
    ValueType type
  ) {

    switch (type) {
    case BYTE: case BYTEARRAY: case UBYTE: case UBYTEARRAY: return (1);
    case SHORT: case SHORTARRAY: case USHORT: case USHORTARRAY: return (2);
    case INT: case INTARRAY: case UINT: case UINTARRAY: return (4);
    case LONG: case LONGARRAY: case ULONG: case ULONGARRAY: return (8);
    case FLOAT: case FLOATARRAY: return (4);
    case DOUBLE: case DOUBLEARRAY: return (8);
    case STRING: return (1);
    default:
      throw new IllegalArgumentException ("Unknown type: " + type);
    } // switch

  } // getTypeSize

  ////////////////////////////////////////////////////////////

  /** Determines if a type is a numerical array. */
  private static boolean isArrayType (
    ValueType type
  ) {

    switch (type) {
    case BYTEARRAY: case UBYTEARRAY:
    case SHORTARRAY: case USHORTARRAY:
    case INTARRAY: case UINTARRAY:
    case LONGARRAY: case ULONGARRAY:
    case FLOATARRAY: case DOUBLEARRAY:
      return (true);
    default: 
      return (false);
    } // switch

  } // isArrayType

  ////////////////////////////////////////////////////////////

  /** Determines if a type is a single numerical value. */
  private static boolean isNumberType (
    ValueType type
  ) {

    switch (type) {
    case BYTE: case UBYTE:
    case SHORT: case USHORT:
    case INT: case UINT:
    case LONG:
    case FLOAT: case DOUBLE:
      return (true);
    default: 
      return (false);
    } // switch

  } // isArrayType

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new reader from the specified buffer and template.
   * 
   * @param template the XML template to use for reading binary
   * data values.
   *
   * @throws IOException if an error occurred reading the XML
   * template.
   * @throws SAXException if an error occurred validating the XML
   * template.
   */
  public BinaryStreamReader (
    InputStream template
  ) throws SAXException, IOException {

    // Initialize
    // ----------
    Document doc = docBuilder.parse (template);
    infoMap = new HashMap<String,Object>();

    // Add value info to map
    // ---------------------
    int offset = 0;
    NodeList children = doc.getFirstChild().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {

      // Filter out non-element nodes
      // ----------------------------
      Element element;
      try { element = (Element) children.item (i); }
      catch (ClassCastException e) { continue; }

      // Check for a skip tag
      // --------------------
      if (element.getTagName().equals ("skip")) 
        offset += Integer.parseInt (element.getAttribute ("length"));

      // Add a value to the map
      // ----------------------
      else {
        ValueInfo info = new ValueInfo (element, offset);
        offset = info.getNextOffset();
        infoMap.put (info.name, info);
        for (LabelInfo label : info.labelList) 
          infoMap.put (label.name, label);
      } // else

    } // for

  } // BinaryStreamReader constructor

  ////////////////////////////////////////////////////////////

  /** Gets a byte value (convenience method for {@link #getValue}). */
  public byte getByte (String name, ByteBuffer buffer) { 

    return (((Byte) getValue (name, buffer)).byteValue());

  } // getByte

  ////////////////////////////////////////////////////////////

  /** Gets a byte array (convenience method for {@link #getValue}). */
  public byte[] getByteArray (String name, ByteBuffer buffer) { 

    return ((byte[]) getValue (name, buffer));

  } // getByteArray

  ////////////////////////////////////////////////////////////

  /** Gets a short value (convenience method for {@link #getValue}). */
  public short getShort (String name, ByteBuffer buffer) { 

    return (((Short) getValue (name, buffer)).shortValue());

  } // getShort

  ////////////////////////////////////////////////////////////

  /** Gets a short array (convenience method for {@link #getValue}). */
  public short[] getShortArray (String name, ByteBuffer buffer) { 

    return ((short[]) getValue (name, buffer));

  } // getShortArray

  ////////////////////////////////////////////////////////////
  
  /** Gets an integer value (convenience method for {@link #getValue}). */
  public int getInt (String name, ByteBuffer buffer) { 

    return (((Integer) getValue (name, buffer)).intValue());

  } // getInt

  ////////////////////////////////////////////////////////////

  /** Gets an integer array (convenience method for {@link #getValue}). */
  public int[] getIntArray (String name, ByteBuffer buffer) { 

    return ((int[]) getValue (name, buffer));

  } // getIntArray

  ////////////////////////////////////////////////////////////

  /** Gets a long value (convenience method for {@link #getValue}). */
  public long getLong (String name, ByteBuffer buffer) { 

    return (((Long) getValue (name, buffer)).longValue());

  } // getLong

  ////////////////////////////////////////////////////////////

  /** Gets a long array (convenience method for {@link #getValue}). */
  public long[] getLongArray (String name, ByteBuffer buffer) { 

    return ((long[]) getValue (name, buffer));

  } // getLongArray

  ////////////////////////////////////////////////////////////

  /** Gets a float value (convenience method for {@link #getValue}). */
  public float getFloat (String name, ByteBuffer buffer) { 

    return (((Float) getValue (name, buffer)).floatValue());

  } // getFloat

  ////////////////////////////////////////////////////////////

  /** Gets a float array (convenience method for {@link #getValue}). */
  public float[] getFloatArray (String name, ByteBuffer buffer) { 

    return ((float[]) getValue (name, buffer));

  } // getFloatArray

  ////////////////////////////////////////////////////////////

  /** Gets a double value (convenience method for {@link #getValue}). */
  public double getDouble (String name, ByteBuffer buffer) { 

    return (((Double) getValue (name, buffer)).doubleValue());

  } // getDouble

  ////////////////////////////////////////////////////////////

  /** Gets a double array (convenience method for {@link #getValue}). */
  public double[] getDoubleArray (String name, ByteBuffer buffer) { 

    return ((double[]) getValue (name, buffer));

  } // getDoubleArray

  ////////////////////////////////////////////////////////////

  /** Gets a string value (convenience method for {@link #getValue}). */
  public String getString (String name, ByteBuffer buffer) { 

    return ((String) getValue (name, buffer));

  } // getString

  ////////////////////////////////////////////////////////////

  /**
   * Gets the byte offset into the binary stream of the specified
   * value.
   *
   * @param name the name of the value to retrieve.
   *
   * @return the byte offset.
   *
   * @throws IllegalArgumentException if the data value name does
   * not exist in the template.
   */
  public int getOffset (
    String name
  ) {

    // Get the info object
    // -------------------
    Object infoObject = infoMap.get (name);
    if (infoObject == null) 
      throw new IllegalArgumentException ("Unknown value '" + name + "'");
    ValueInfo info = ((infoObject instanceof LabelInfo) ? 
      ((LabelInfo) infoObject).valueInfo : (ValueInfo) infoObject);

    return (info.offset);

  } // getOffset

  ////////////////////////////////////////////////////////////

  /**
   * Gets a data value from a byte buffer using this reader's
   * template.
   *
   * @param name the name of the value to retrieve.
   * @param inputBuffer the input buffer to read from.
   *
   * @return the data value as a number, array, or string.
   *
   * @throws IllegalArgumentException if the data value name does
   * not exist in the template.
   */
  public Object getValue (
    String name,
    ByteBuffer inputBuffer
  ) {

    // Get the info object
    // -------------------
    Object infoObject = infoMap.get (name);
    if (infoObject == null) 
      throw new IllegalArgumentException ("Unknown value '" + name + "'");

    // Get label information
    // ---------------------
    boolean isLabel = infoObject instanceof LabelInfo;
    LabelInfo label = (isLabel ? (LabelInfo) infoObject : null);
    ValueInfo info = (isLabel ? ((LabelInfo) infoObject).valueInfo : 
      (ValueInfo) infoObject);

    // Read the data
    // -------------
    Object value;
    switch (info.valueType) {
      
    // Handle byte data
    // ----------------
    case BYTE:
      byte byteValue = inputBuffer.get (info.offset);
      value = new Byte (byteValue);
      break;

    case UBYTE:
      short ubyteValue = (short) (inputBuffer.get (info.offset) & 0xff);
      value = new Short (ubyteValue);
      break;

    case BYTEARRAY:
      byte[] byteArray = new byte[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.get (byteArray);
      value = byteArray;
      break;

    case UBYTEARRAY:
      byte[] ubyteArray = new byte[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.get (ubyteArray);
      short[] ubyteShortArray = new short[info.length];
      for (int i = 0; i < info.length; i++)
        ubyteShortArray[i] = (short) (ubyteArray[i] & 0xff);
      value = ubyteShortArray;
      break;

    // Handle short data
    // -----------------
    case SHORT:
      short shortValue = inputBuffer.getShort (info.offset);
      value = new Short (shortValue);
      break;

    case USHORT:
      int ushortValue = inputBuffer.getShort (info.offset) & 0xffff;
      value = new Integer (ushortValue);
      break;
      
    case SHORTARRAY: 
      short[] shortArray = new short[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asShortBuffer().get (shortArray);
      value = shortArray;
      break;

    case USHORTARRAY: 
      short[] ushortArray = new short[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asShortBuffer().get (ushortArray);
      int[] ushortIntArray = new int[info.length];
      for (int i = 0; i < info.length; i++)
        ushortIntArray[i] = ushortArray[i] & 0xffff;
      value = ushortIntArray;
      break;

    // Handle int data
    // ---------------
    case INT:
      int intValue = inputBuffer.getInt (info.offset);
      value = new Integer (intValue);
      break;

    case UINT:
      long uintValue = inputBuffer.getInt (info.offset) & 0xffffffffL;
      value = new Long (uintValue);
      break;

    case INTARRAY:
      int[] intArray = new int[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asIntBuffer().get (intArray);
      value = intArray;
      break;

    case UINTARRAY:
      int[] uintArray = new int[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asIntBuffer().get (uintArray);
      long[] uintLongArray = new long[info.length];
      for (int i = 0; i < info.length; i++)
        uintLongArray[i] = uintArray[i] & 0xffffffffL;
      value = uintLongArray;
      break;

    // Handle long data
    // ----------------

    /** 
     * Note that since Java has no unsigned long type, we can't
     * convert to unsigned long here.  We just hope that the
     * programmer is smart enough to handle this.
     */

    case LONG: case ULONG:
      long longValue = inputBuffer.getLong (info.offset);
      value = new Long (longValue);
      break;

    case LONGARRAY: case ULONGARRAY:
      long[] longArray = new long[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asLongBuffer().get (longArray);
      value = longArray;
      break;

    // Handle floating-point data
    // --------------------------
    case FLOAT: 
      float floatValue = inputBuffer.getFloat (info.offset);
      value = new Float (floatValue);
      break;

    case FLOATARRAY:
      float[] floatArray = new float[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asFloatBuffer().get (floatArray);
      value = floatArray;
      break;

    case DOUBLE: 
      double doubleValue = inputBuffer.getDouble (info.offset);
      value = doubleValue;
      break;

    case DOUBLEARRAY:
      double[] doubleArray = new double[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.asDoubleBuffer().get (doubleArray);
      value = doubleArray;
      break;

    // Handle string data
    // ------------------
    case STRING:
      byte[] stringByteArray = new byte[info.length];
      inputBuffer.position (info.offset);
      inputBuffer.get (stringByteArray);
      String stringValue = new String (stringByteArray).trim();
      value = stringValue;
      break;

    default:
      throw new IllegalArgumentException ("Unknown type: " + info.valueType);
    } // switch

    // Get labeled value
    // -----------------
    if (label != null) {
      long longValue = ((Number) value).longValue();
      long labelBits = (longValue & label.mask) >>> label.rightShift;
      value = new Long (labelBits);
    } // if

    // Get scaled value
    // ----------------
    else if (!Double.isNaN (info.scale)) {
      long longValue = ((Number) value).longValue();
      value = new Double (longValue*info.scale);
    } // else if

    // Get mapped value
    // ----------------
    Map<Object,Object> map = (label != null ? label.valueMap : info.valueMap);
    if (map.size() != 0) {
      Object mappedValue = map.get (value);
      if (mappedValue == null)
        throw new IllegalStateException ("No mapping for key '" + value + "'" +
          " in data value '" + (label != null ? label.name : info.name) + "'");
      value = mappedValue;
    } // if

    return (value);

  } // getValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data value names.
   *
   * @return the list of data value names.
   */
  public Set<String> getNames() { return (infoMap.keySet()); }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    // Read the XML template
    // ---------------------
    BinaryStreamReader reader = 
      new BinaryStreamReader (new FileInputStream (argv[0]));

    // Open the data file
    // ------------------
    File file = new File (argv[1]);
    FileChannel inputChannel = new RandomAccessFile (file, "r").getChannel();
    ByteBuffer inputBuffer = inputChannel.map (FileChannel.MapMode.READ_ONLY,
      0, inputChannel.size());

    // Retrieve values
    // ---------------
    Set<String> names = new TreeSet<String> (reader.getNames());
    System.out.println ("Binary stream data values:");
    for (String name : names) {
      Object value = reader.getValue (name, inputBuffer);
      int offset = reader.getOffset (name);
      String valueString;
      if (value.getClass().isArray()) {
        valueString = "[";
        int length = Array.getLength (value);
        for (int i = 0; i < length; i++) 
          valueString += Array.get (value, i) + (i < length-1 ? "," : "]");
      } // if
      else {
        valueString = value.toString();
      } // else
      System.out.println ("  " + name + " = " + valueString + " (type=" + 
        value.getClass().getName() + ", offset=" + offset + ")");
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // BinaryStreamReader class

////////////////////////////////////////////////////////////////////////


