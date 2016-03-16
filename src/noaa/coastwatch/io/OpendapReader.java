////////////////////////////////////////////////////////////////////////
/*
     FILE: OpendapReader.java
  PURPOSE: Reads data through the DODS/OPeNDAP interface.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/08
  CHANGES: 2008/02/18, PFH, modified to use opendap.dap classes
           2016/03/16, PFH
           - Changes: Updated to use new opendap.dap.DConnect2 class and call
             DArray.getClearName().
           - Issue: The Java NetCDF library uses the newer OPeNDAP Java
             classes and they were conflicting with the older API that we were
             using, so we had to remove the old dap2 jar and conform to the 
             API found in the classes in the latest toolsUI jar file.

  CoastWatch Software Library and Utilities
  Copyright 2006-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.util.DataVariable;
import opendap.dap.Attribute;
import opendap.dap.AttributeTable;
import opendap.dap.BaseType;
import opendap.dap.BooleanPrimitiveVector;
import opendap.dap.BytePrimitiveVector;
import opendap.dap.DAS;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import opendap.dap.DVector;
import opendap.dap.Float32PrimitiveVector;
import opendap.dap.Float64PrimitiveVector;
import opendap.dap.Int16PrimitiveVector;
import opendap.dap.Int32PrimitiveVector;
import opendap.dap.NoSuchAttributeException;
import opendap.dap.PrimitiveVector;
import opendap.dap.UInt16PrimitiveVector;
import opendap.dap.UInt32PrimitiveVector;

/** 
 * The <code>OpendapReader</code> class is the base class for readers that
 * use the OPeNDAP API to read data and metadata.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class OpendapReader
  extends EarthDataReader {

  // Variables
  // ---------

  /** The data connection. */
  protected DConnect2 connect;

  /** The data attribute tables. */
  protected DAS das;

  /** The data descriptor object. */
  protected DDS dds;

  ////////////////////////////////////////////////////////////

  /** 
   * Converts an attribute into a Java string, primitive array, or
   * wrapped primitive.
   * 
   * @param att the attribute to convert.
   * @param asArray the array flag, true to return single values as an
   * array, false to return as a wrapped primitive.
   * 
   * @return the attribute value or null if the value type is not
   * supported.
   *
   * @throws NumberFormatException if an error occurred while
   * parsing a numeric value.
   */
  private static Object convertAttributeValue (
    Attribute att, 
    boolean asArray
  ) throws NumberFormatException {

    if (att == null) return (null);
    
    // Check type
    // ----------
    int type = att.getType();
    if (type == Attribute.UNKNOWN || type == Attribute.CONTAINER)
      return (null);
    boolean isNumeric = (type != Attribute.URL && type != Attribute.STRING);

    // Get values as a list
    // --------------------
    List values;
    try { values = Collections.list (att.getValues()); }
    catch (NoSuchAttributeException e) {
      throw new NumberFormatException ("Attribute value not available");
    } // catch
    int count = values.size();

    // Convert numeric values
    // ----------------------
    Object value;
    if (isNumeric) {

      // Create primitive array
      // ----------------------
      switch (type) {
      case Attribute.BYTE:
        byte[] byteArray = new byte[count];
        for (int i = 0; i < count; i++)
          byteArray[i] = Byte.parseByte ((String) values.get (i));
        value = byteArray;
        break;
      case Attribute.UINT16:
      case Attribute.INT32:
        int[] intArray = new int[count];
        for (int i = 0; i < count; i++)
          intArray[i] = Integer.parseInt ((String) values.get (i));
        value = intArray;
        break;
      case Attribute.INT16:
        short[] shortArray = new short[count];
        for (int i = 0; i < count; i++)
          shortArray[i] = Short.parseShort ((String) values.get (i));
        value = shortArray;
        break;
      case Attribute.UINT32:
        long[] longArray = new long[count];
        for (int i = 0; i < count; i++)
          longArray[i] = Long.parseLong ((String) values.get (i));
        value = longArray;
        break;
      case Attribute.FLOAT32:
        float[] floatArray = new float[count];
        for (int i = 0; i < count; i++) {
          String str = (String) values.get (i);
          if (str.equals ("nan")) str = "NaN";
          floatArray[i] = Float.parseFloat (str);
        } // for
        value = floatArray;
        break;
      case Attribute.FLOAT64:
        double[] doubleArray = new double[count];
        for (int i = 0; i < count; i++) {
          String str = (String) values.get (i);
          if (str.equals ("nan")) str = "NaN";
          doubleArray[i] = Double.parseDouble (str);
        } // for
        value = doubleArray;
        break;
      default: return (null);
      } // switch

      // Get single wrapped primitive
      // ----------------------------
      if (count == 1 && !asArray)
        value = Array.get (value, 0);

    } // if

    // Convert to string
    // -----------------
    else {

      // Get string value
      // ----------------
      String str = (String) values.get (0);
      str = str.trim();

      // Remove octal sequences
      // ----------------------
      if (str.indexOf ("\\0") != -1)
        str = IOServices.convertOctal (str).trim();

      // Remove quotes
      // -------------
      str = str.substring (1, str.length()-1);

      value = str;

    } // else

    return (value);

  } // convertAttributeValue

  ////////////////////////////////////////////////////////////

  /**
   * Creates a map of attributes and values from an attribute
   * table.  This makes it easier to use the attributes as they
   * are given appropriate Java datatype values rather than
   * string values.  Any numeric values are converted to
   * primitive arrays.
   * 
   * @param table the attribute table to read.
   * @param map the map to add attribute values to, or null to
   * create a new map.
   *
   * @return the map of attribute names to values.
   *
   * @throws IOException if an error occurred converting an
   * attribute value to a numeric type.
   */
  protected static Map getAttributeMap (
    AttributeTable table,
    Map map
  ) throws IOException {

    // Create map if needed
    // --------------------
    if (map == null) map = new LinkedHashMap();

    // Add attributes to map
    // ---------------------
    for (Enumeration en = table.getNames(); en.hasMoreElements();) {
      String name = (String) en.nextElement();
      Attribute att = table.getAttribute (name);
      Object value;
      try { value = convertAttributeValue (att, true); }
      catch (NumberFormatException e) { 
        throw new IOException ("Error converting value to numeric type " +
          "for attribute " + name); 
      } // catch
      if (value != null) map.put (name, value);
    } // for

    return (map);

  } // getAttributeMap

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive Java class type for the specified OPeNDAP
   * base type.
   *
   * @param base the OPeNDAP base type.
   *
   * @return the Java primitive class or null if the base type cannot be
   * represented with a Java primitive.
   */ 
  protected static Class getPrimitiveClassType (
    BaseType base
  ) {

    if (!(base instanceof DVector)) return (null);
    PrimitiveVector vector = ((DVector) base).getPrimitiveVector();

    if (vector instanceof BooleanPrimitiveVector)
      return (Boolean.TYPE);
    else if (vector instanceof BytePrimitiveVector)
      return (Byte.TYPE);
    else if (vector instanceof Int16PrimitiveVector)
      return (Short.TYPE);
    else if (vector instanceof Int32PrimitiveVector)
      return (Integer.TYPE);
    else if (vector instanceof Float32PrimitiveVector)
      return (Float.TYPE);
    else if (vector instanceof Float64PrimitiveVector)
      return (Double.TYPE);
    else
      return (null);

  } // getPrimitiveClassType

  ////////////////////////////////////////////////////////////

  /** Returns true if the OPeNDAP primitive type is unsigned. */
  protected static boolean isUnsigned (
    BaseType base
  ) { 

    if (!(base instanceof DVector)) return (false);
    PrimitiveVector vector = ((DVector) base).getPrimitiveVector();

    if (vector instanceof BytePrimitiveVector)
      return (true);
    else if (vector instanceof UInt16PrimitiveVector)
      return (true);
    else if (vector instanceof UInt32PrimitiveVector)
      return (true);
    else
      return (false);

  } // isUnsigned
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new reader using the specified URL.
   *
   * @param url the network location.
   *
   * @throws IOException if the an error occurred accessing the dataset.
   */
  protected OpendapReader (
    String url
  ) throws IOException {

    super (url);
    connect = new DConnect2 (url, true);

    // Get dataset attributes
    // ----------------------
    try { das = connect.getDAS(); }
    catch (Exception e) { 
      throw new IOException ("Error getting OPeNDAP attribute data: " + 
        e.getMessage());
    } // catch

    // Create global attribute map
    // ---------------------------
    AttributeTable globalTable = null;
    for (Enumeration en = das.getNames(); en.hasMoreElements();) {
      String name = (String) en.nextElement();
      if (name.toLowerCase().indexOf ("global") != -1) {
        try { globalTable = das.getAttributeTable (name); }
        catch (NoSuchAttributeException e) {
          throw new IOException ("Error getting OPeNDAP attribute data: " +
            e.getMessage());
        } // catch
        break;
      } // if
    } // for
    if (globalTable == null)
      throw new IOException ("Cannot find global attribute table");
    getAttributeMap (globalTable, rawMetadataMap);

    // Get data descriptor object
    // --------------------------
    try { dds = connect.getDDS(); }
    catch (Exception e) { 
      throw new IOException ("Error getting OPeNDAP descriptor data: " + 
        e.getMessage());
    } // catch

  } // OpendapReader constructor

  ////////////////////////////////////////////////////////////

  /** Closes the reader. */
  public void close () throws IOException { }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    OpendapReader reader = new OpendapReader (argv[0]) {
        protected DataVariable getPreviewImpl (int index) 
          throws java.io.IOException { return (null); }
        public DataVariable getVariable (int index) 
          throws java.io.IOException { return (null); }
        public String getDataFormat () { return (null); }
      };
    Map map = reader.getRawMetadata();
    for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      System.out.println (entry.getKey() + " = " + entry.getValue());
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // OpendapReader class

////////////////////////////////////////////////////////////////////////
