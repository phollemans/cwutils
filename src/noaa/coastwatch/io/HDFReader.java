////////////////////////////////////////////////////////////////////////
/*
     FILE: HDFReader.java
  PURPOSE: A class to read HDF format files using the JNI interface 
           to the HDF library.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/06
  CHANGES: 2002/06/25, PFH, added constructor
           2002/07/12, PFH, added close, closed test
           2002/07/23, PFH, added new chunking/compression native calls
           2002/11/07, PFH, added HDFSD interface
           2002/12/26, PFH, moved getDecimals to DataVariable
           2003/04/21, PFH, added getFilename, HDFWriter-based constructor
           2003/05/28, PFH, added getVariableDimensions
           2004/02/11, PFH, modified getAttribute() exception throwing
           2004/02/15, PFH, added super() call in constructor
           2004/02/16, PFH, added unsigned type handling
           2004/04/11, PFH, fixed exception report string
           2004/09/10, PFH, modified value formatting string to remove commas
           2004/09/14, PFH, added getAttributeData(), getAttributeAsArray()
           2004/09/23, PFH, added readAllMetadata()
           2004/09/30, PFH, added getAttributes(int,List,Map)
           2004/10/05, PFH, modified to use MetadataContainer methods
           2005/06/15, PFH, added common units conversions
           2005/06/22, PFH, added raw metadata reading
           2005/09/12, PFH, modified to ignore coord variables in variable list
           2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)
           2010/03/30, PFH, modified constructor to close file on failure

  CoastWatch Software Library and Utilities
  Copyright 2004-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import java.awt.geom.*;
import ncsa.hdf.hdflib.*;
import noaa.coastwatch.util.*;

/**
 * An HDF reader is an Earth data reader that reads HDF format
 * files using the HDF library class.  The HDF reader class is
 * abstract -- subclasses handle specific metadata variants.
 */
public abstract class HDFReader
  extends EarthDataReader
  implements HDFSD {

  // Variables
  // ---------

  /** HDF file id. */
  protected int sdid;

  /** Flag to signify that the file is closed. */
  private boolean closed;

  ////////////////////////////////////////////////////////////

  /** Loads the shared library. */
  static {
    System.loadLibrary ("HDFReader");
  } // static

  ////////////////////////////////////////////////////////////

  public int getSDID () { return (sdid); }

  ////////////////////////////////////////////////////////////

  public String getFilename () { return (getSource()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs an HDF reader from the specified HDF writer.
   *
   * @param writer the writer to use for reading.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred reading the file metadata.
   * @throws NoninvertibleTransformException if the Earth transform object
   * could not be initialized.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  protected HDFReader (
    HDFWriter writer
  ) throws HDFException, IOException, NoninvertibleTransformException,
    ClassNotFoundException {

    super (writer.getDestination());

    // Copy ID
    // -------
    this.sdid = writer.getSDID();

    // Set closed to true (we let the writer do the closing)
    // -----------------------------------------------------
    this.closed = true;

    // Get variable names
    // ------------------
    variables = getVariableNames (sdid);

    // Create Earth data info object
    // -----------------------------
    info = getGlobalInfo();
    if (readAllMetadata()) getAttributes (sdid, info.getMetadataMap(), true);

    // Get raw metadata
    // ----------------
    getAttributes (sdid, rawMetadataMap, true);

  } // HDFReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs an HDF reader from the specified file.
   *
   * @param file the file name to read.
   *
   * @throws IOException if an error opening or reading the file
   * metadata.
   */
  protected HDFReader (
    String file
  ) throws IOException {

    super (file);

    try {

      // Open the file
      // -------------
      closed = true;
      sdid = HDFLibrary.SDstart (file, HDFConstants.DFACC_READ);
      closed = false;

      // Get variable names
      // ------------------
      variables = getVariableNames (sdid);
      
      // Create Earth data info object
      // -----------------------------
      info = getGlobalInfo();
      if (readAllMetadata()) getAttributes (sdid, info.getMetadataMap(), true);
      
      // Get raw metadata
      // ----------------
      getAttributes (sdid, rawMetadataMap, true);
      
    } // try

    // Catch exception and close file
    // ------------------------------
    catch (Exception e) {
      try { close(); }
      catch (IOException e2) { }
      throw new IOException (e);
    } // catch

  } // HDFReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets an array of HDF dataset variable names.
   *
   * @param sdid the HDF dataset to read.
   *
   * @return the array of variable names.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static String[] getVariableNames (
    int sdid
  ) throws HDFException, ClassNotFoundException {

    // Get the variable and attribute count
    // ------------------------------------
    int[] fileInfo = new int[2];
    if (!HDFLibrary.SDfileinfo (sdid, fileInfo))
      throw new HDFException ("Cannot get file info for sdid = " + sdid);
    int varCount = fileInfo[0];

    // Loop over all variables
    // -----------------------
    List variables = new ArrayList();
    for (int i = 0; i < varCount; i++) {

      // Access variable
      // ---------------
      int sdsid = HDFLibrary.SDselect (sdid, i);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + i);

      // Check for coordinate variable
      // -----------------------------
      if (HDFLibrary.SDiscoordvar (sdsid)) continue;

      // Get variable name
      // -----------------
      String[] varName = new String[] {""};
      int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
      int varInfo[] = new int[3];
      if (!HDFLibrary.SDgetinfo (sdsid, varName, varDims, varInfo))
        throw new HDFException ("Cannot get variable info at index " + i);
      variables.add (varName[0]);

      // End access
      // ----------
      HDFLibrary.SDendaccess (sdsid);

    } // for

    // Return variable names
    // ---------------------
    return ((String[]) variables.toArray (new String[]{}));

  } // getVariableNames

  ////////////////////////////////////////////////////////////

  /** 
   * Reads the Earth data info metadata.
   *
   * @return the Earth data info object.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred reading the file metadata.
   * @throws NoninvertibleTransformException if the Earth transform object
   * could not be initialized.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */  
  protected abstract EarthDataInfo getGlobalInfo () throws HDFException, 
    IOException, NoninvertibleTransformException, ClassNotFoundException;

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the full set of attributes should be read into the
   * global EarthDataInfo object and DataVariable objects.  Generally
   * this is only desirable if the HDF metadata in the data source is
   * compatible with the intended HDF data sink.  By default, this
   * method returns false unless overridden in the child class.
   *
   * @return true if full metadata should be read, or false if not.
   */
  protected boolean readAllMetadata () { return (false); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the Java primitive class associated with an HDF type.
   *
   * @param type the HDF data type.
   *
   * @return the Java primitive class.
   *
   * @throws ClassNotFoundException if a class cannot be found that
   * matches the HDF type.
   */
  public static Class getClass (
    int type
  ) throws ClassNotFoundException {    

    switch (type) {
    case HDFConstants.DFNT_UCHAR8: return (Byte.TYPE);
    case HDFConstants.DFNT_CHAR8: return (Byte.TYPE);
    case HDFConstants.DFNT_UINT8: return (Byte.TYPE);
    case HDFConstants.DFNT_INT8: return (Byte.TYPE);
    case HDFConstants.DFNT_UINT16: return (Short.TYPE);
    case HDFConstants.DFNT_INT16: return (Short.TYPE);
    case HDFConstants.DFNT_UINT32: return (Integer.TYPE);
    case HDFConstants.DFNT_INT32: return (Integer.TYPE);
    case HDFConstants.DFNT_UINT64: return (Long.TYPE);
    case HDFConstants.DFNT_INT64: return (Long.TYPE);
    case HDFConstants.DFNT_FLOAT32: return (Float.TYPE);
    case HDFConstants.DFNT_FLOAT64: return (Double.TYPE);
    default: 
      throw new ClassNotFoundException ("Unsupported HDF type: " + type);
    } // switch

  } // getClass

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if the HDF datatype is unsigned.
   *
   * @param type the HDF type.
   *
   * @return true if the type is unsigned, false otherwise.
   */
  public static boolean getUnsigned (
    int type
  ) {

    switch (type) {
    case HDFConstants.DFNT_UCHAR8: return (true);
    case HDFConstants.DFNT_CHAR8: return (false);
    case HDFConstants.DFNT_UINT8: return (true);
    case HDFConstants.DFNT_INT8: return (false);
    case HDFConstants.DFNT_UINT16: return (true);
    case HDFConstants.DFNT_INT16: return (false);
    case HDFConstants.DFNT_UINT32: return (true);
    case HDFConstants.DFNT_INT32: return (false);
    case HDFConstants.DFNT_UINT64: return (true);
    case HDFConstants.DFNT_INT64: return (false);
    case HDFConstants.DFNT_FLOAT32: return (false);
    case HDFConstants.DFNT_FLOAT64: return (false);
    default: 
      throw new IllegalArgumentException ("Unsupported HDF type: " + type);
    } // switch

  } // getUnsigned

  ////////////////////////////////////////////////////////////

  /**
   * Gets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param name the attribute name.
   *
   * @return the attribute value.  If the attribute is a character
   * string, a Java <code>String</code> is returned.  If the attribute
   * has one value only, a Java object will be returned wrapping the
   * primitive type.  If the attribute has more than one value, a Java
   * primitive array is returned.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static Object getAttribute (
    int sdid,
    String name
  ) throws HDFException, ClassNotFoundException {
  
    // Get attribute value
    // -------------------
    int attIndex = HDFLibrary.SDfindattr (sdid, name);
    if (attIndex < 0)
      throw new HDFException ("Cannot get attribute value for " + name);
    return (getAttribute (sdid, attIndex));

  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param name the attribute name.
   *
   * @return the attribute data as an array of Java primitives.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static Object getAttributeAsArray (
    int sdid,
    String name
  ) throws HDFException, ClassNotFoundException {
  
    // Get attribute value
    // -------------------
    int attIndex = HDFLibrary.SDfindattr (sdid, name);
    if (attIndex < 0)
      throw new HDFException ("Cannot get attribute value for " + name);
    return (getAttributeAsArray (sdid, attIndex));

  } // getAttributeAsArray

  ////////////////////////////////////////////////////////////

  /** 
   * Gets HDF attribute data.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param index the attribute index.
   * @param attInfo the 2-element attribute info array (modified).
   * When the method returns, the info array contains [attributeType,
   * attributeLength].
   *
   * @return the array of attribute data.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  private static Object getAttributeData (
    int sdid,
    int index,
    int[] attInfo
  ) throws HDFException, ClassNotFoundException {
   
    // Get attribute info
    // ------------------
    String[] attNameArray = new String[] {""};
    if (!HDFLibrary.SDattrinfo (sdid, index, attNameArray, attInfo))
      throw new HDFException ("Cannot get attribute info at index " + index);
    String attName = attNameArray[0];
    int attType = attInfo[0];
    int attLength = attInfo[1];

    // Get attribute value
    // -------------------        
    Class attClass = getClass (attType);
    Object attData = Array.newInstance (attClass, attLength);
    if (!HDFLibrary.SDreadattr (sdid, index, attData))
      throw new HDFException ("Cannot get attribute value for " + attName);

    return (attData);

  } // getAttributeData

  ////////////////////////////////////////////////////////////

  /**
   * Gets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param index the attribute index.
   *
   * @return the attribute data as an array of Java primitives.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static Object getAttributeAsArray (
    int sdid,
    int index
  ) throws HDFException, ClassNotFoundException {

    // Get attribute data
    // ------------------
    int[] attInfo = new int[2];
    Object attData = getAttributeData (sdid, index, attInfo); 

    return (attData);

  } // getAttributeAsArray

  ////////////////////////////////////////////////////////////

  /**
   * Gets an HDF attribute value.
   *
   * @param sdid the HDF scientific dataset ID.
   * @param index the attribute index.
   *
   * @return the attribute value.  If the attribute is a character
   * string, a Java <code>String</code> is returned.  If the attribute
   * has one value only, a Java object will be returned wrapping the
   * primitive type.  If the attribute has more than one value, a Java
   * primitive array is returned.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static Object getAttribute (
    int sdid,
    int index
  ) throws HDFException, ClassNotFoundException {
  
    // Get attribute data
    // ------------------
    int[] attInfo = new int[2];
    Object attData = getAttributeData (sdid, index, attInfo); 
    int attType = attInfo[0];
    int attLength = attInfo[1];
    Class attClass = getClass (attType);

    // Return string object
    // --------------------
    if (attType == HDFConstants.DFNT_UCHAR8 || 
      attType == HDFConstants.DFNT_CHAR8)
      return ((new String ((byte[])attData)).trim());

    // Return wrapped primitive
    // ------------------------
    if (attLength == 1) {
      if (attClass.equals (Byte.TYPE))
        return (new Byte (((byte[])attData)[0]));
      else if (attClass.equals (Short.TYPE))
        return (new Short (((short[])attData)[0]));
      else if (attClass.equals (Integer.TYPE))
        return (new Integer (((int[])attData)[0]));
      else if (attClass.equals (Float.TYPE))
        return (new Float (((float[])attData)[0]));
      else if (attClass.equals (Double.TYPE))
        return (new Double (((double[])attData)[0]));
      else
        throw new ClassNotFoundException ("Unsupported attribute class");
    } // if

    // Return primitive array
    // ----------------------
    return (attData);

  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets a number of attributes specified by a list of name and
   * stores them in a map.
   *
   * @param sdid the HDF scientific dataset or variable ID for reading.
   * @param attList the list of attribute names to store.
   * @param attMap the attribute map for storing attribute values
   * (modified).  If an attribute value cannot be found for the
   * attribute name, no attribute value is stored.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static void getAttributes (
    int sdid,
    List attList,
    Map attMap
  ) throws HDFException, ClassNotFoundException {

    for (Iterator iter = attList.iterator(); iter.hasNext();) {
      String attName = (String) iter.next();
      try { attMap.put (attName, getAttribute (sdid, attName)); }
      catch (HDFException e) { }
    } // for

  } // getAttributes

  ////////////////////////////////////////////////////////////

  /**
   * Gets all attributes and stores them in a map.
   *
   * @param sdid the HDF scientific dataset or variable ID for reading.
   * @param map the attribute map.
   * @param global set to true if the HDF attributes are global.  If false,
   * it is assumed that the attributes belong to an HDF variable.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  public static void getAttributes (
    int sdid,
    Map map,
    boolean global
  ) throws HDFException, ClassNotFoundException {

    // Get the attribute count
    // -----------------------
    int attCount;
    if (global) {
      int[] fileInfo = new int[2];
      if (!HDFLibrary.SDfileinfo (sdid, fileInfo))
        throw new HDFException ("Cannot get file info for sdid = " + sdid);
      attCount = fileInfo[1];
    } // if
    else {
      String[] varName = new String[] {""};
      int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
      int varInfo[] = new int[3];
      if (!HDFLibrary.SDgetinfo (sdid, varName, varDims, varInfo))
        throw new HDFException ("Cannot get variable info for sdid = " + sdid);
      attCount = varInfo[2];
    } // else

    // Loop over all attributes
    // ------------------------
    for (int i = 0; i < attCount; i++) {

      // Get attribute name
      // ------------------
      String[] attName = new String[] {""};
      int attInfo[] = new int[2];
      if (!HDFLibrary.SDattrinfo (sdid, i, attName, attInfo))
        throw new HDFException ("Cannot get attribute info for " + attName);

      // Get attribute value
      // -------------------
      map.put (attName[0], getAttribute (sdid, i));

    } // for

  } // getAttributes

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException {

    try {

      // Access variable
      // ---------------
      int sdsid = HDFLibrary.SDselect (sdid, HDFLibrary.SDnametoindex (sdid, 
        variables[index]));
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + index);

      // Get variable info
      // -----------------
      String[] varName = new String[] {""};
      int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
      int varInfo[] = new int[3];
      if (!HDFLibrary.SDgetinfo (sdsid, varName, varDims, varInfo))
        throw new HDFException ("Cannot get variable info at index " + index);
      String name = varName[0];
      int rank = varInfo[0];
      int varType = varInfo[1];
      boolean isUnsigned = getUnsigned (varType);

      // Create fake data array
      // ----------------------
      Class varClass = getClass (varType);
      Object data = Array.newInstance (varClass, 1);

      // Get data strings
      // ----------------
      String[] dataStrings = new String[] {"", "", "", ""};
      if (!HDFLibrary.SDgetdatastrs (sdsid, dataStrings, 256))
        throw new HDFException ("Cannot get data strings for " + name);
      String longName = dataStrings[0];
      String units = dataStrings[1];
      String formatStr = dataStrings[2];

      // Convert units
      // -------------
      units = units.trim();
      if (units.equals ("temp_deg_c")) units = "celsius";
      else if (units.equals ("albedo*100%")) units = "percent";
      else if (units.equals ("-")) units = "";

      // Get calibration
      // ---------------
      double[] calInfo = new double[4];
      int[] calType = new int[1];
      double[] scaling;
      try {
        if (!HDFLibrary.SDgetcal (sdsid, calInfo, calType))
          throw new HDFException ("Cannot get calibration info for " + name);
        scaling = new double[] {calInfo[0], calInfo[2]};
      } // try
      catch (HDFException e) {
        scaling = null;
      } // catch

      // Get missing value
      // -----------------
      Object[] fillValue = new Object[1];
      try {
        if (!HDFLibrary.SDgetfillvalue (sdsid, fillValue))
          throw new HDFException ("Cannot get fill value for " + name);
      } // try
      catch (HDFException e) {
        fillValue[0] = null;
      } // catch
      Object missing = fillValue[0];

      // Convert missing value
      // ---------------------
      if (missing != null) {
        Class missingClass = (Class) missing.getClass().getField(
          "TYPE").get(missing);
        if (!missingClass.equals (varClass)) {
          Number missingNumber = (Number) missing;
          if (varClass.equals (Byte.TYPE))
            missing = new Byte (missingNumber.byteValue());
          else if (varClass.equals (Short.TYPE))
            missing = new Short (missingNumber.shortValue());
          else if (varClass.equals (Integer.TYPE))
            missing = new Integer (missingNumber.intValue());
          else if (varClass.equals (Float.TYPE))
            missing = new Float (missingNumber.floatValue());
          else if (varClass.equals (Double.TYPE))
            missing = new Double (missingNumber.doubleValue());
          else
            throw new ClassNotFoundException ("Unsupported variable class");
        } // if
      } // if

      // Try getting fraction digits attribute
      // -------------------------------------
      int digits = -1;
      try { 
        digits = ((Integer) getAttribute (sdsid, 
          "fraction_digits")).intValue();
      } // try
      catch (Exception e) { }

      // Try using format string
      // -----------------------         
      if (digits == -1 && !formatStr.equals ("")) {
        int dot = formatStr.indexOf ('.');
        digits = 0;
        if (dot != -1 && dot != formatStr.length()-1)
          digits = Character.digit (formatStr.charAt (dot+1), 10);
      } // if

      // Try guessing from scaling factor
      // --------------------------------
      if (digits == -1 && scaling != null) {
        double maxValue = 0;
        if (varClass.equals (Byte.TYPE)) 
          maxValue = Byte.MAX_VALUE*scaling[0];
        else if (varClass.equals (Short.TYPE)) 
          maxValue = Short.MAX_VALUE*scaling[0];
        else if (varClass.equals (Integer.TYPE)) 
          maxValue = Integer.MAX_VALUE*scaling[0];
        else if (varClass.equals (Float.TYPE)) 
          maxValue = Float.MAX_VALUE*scaling[0];
        else if (varClass.equals (Double.TYPE)) 
          maxValue = Double.MAX_VALUE*scaling[0];
        digits = DataVariable.getDecimals (Double.toString (maxValue));
      } // else if

      // Set fractional digits
      // ---------------------
      String decFormat = "0";
      for (int i = 0; i < digits; i++) {
        if (i == 0) decFormat += ".";
        decFormat += "#";
      } // for
      NumberFormat format = new DecimalFormat (decFormat);

      // Try getting navigation transform
      // --------------------------------
      AffineTransform nav = null;
      if (rank == 2) {
        try { 
          double[] matrix  = (double[]) getAttribute (sdsid, "nav_affine");
          nav = new AffineTransform (matrix);
        } // try
        catch (Exception e) { }
      } // if

      // Create variable
      // ---------------
      DataVariable var;
      if (rank == 1)
        var = new Line (name, longName, units, varDims[0], data, format,
          scaling, missing);
      else if (rank == 2) {
        var = new Grid (name, longName, units, varDims[0], varDims[1], 
          data, format, scaling, missing);
        if (nav != null) ((Grid) var).setNavigation (nav);
      } // else if 
      else
        throw new UnsupportedEncodingException ("Unsupported rank = " + rank +
          " for " + name);
      var.setUnsigned (isUnsigned);

      // Get attributes
      // --------------
      if (readAllMetadata()) 
        getAttributes (sdsid, var.getMetadataMap(), false);

      // End access
      // ----------
      HDFLibrary.SDendaccess (sdsid);

      // Return the new grid
      // -------------------
      return (var);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index
  ) throws IOException {

    // Get a variable preview
    // ----------------------
    DataVariable var = getPreview (index);

    // Check for grid
    // --------------
    if (var instanceof Grid)
      return (new HDFCachedGrid ((Grid) var, this));

    // Read full variable data
    // -----------------------
    try {

      // Access variable
      // ---------------
      int sdsid = HDFLibrary.SDselect (sdid, HDFLibrary.SDnametoindex (sdid, 
        variables[index]));
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + index);

      // Read data
      // ---------
      Object data = Array.newInstance (var.getDataClass(), var.getValues());
      int[] dims = var.getDimensions();
      int[] start = new int[dims.length];
      Arrays.fill (start, 0);
      int[] stride = new int[dims.length];
      Arrays.fill (stride, 1);
      if (!HDFLibrary.SDreaddata (sdsid, start, stride, dims, data))
        throw new HDFException ("Cannot read data for " + var.getName());

      // End access
      // ----------
      HDFLibrary.SDendaccess (sdsid);

      // Return variable
      // ---------------
      var.setData (data);
      return (var);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage ());
    } // catch

  } // getVariable

  ////////////////////////////////////////////////////////////

  /**
   * Gets the chunk lengths for a chunked SDS HDF variable.  
   * 
   * @param sdsid the HDF SDS variable ID.
   *
   * @return the chunk lengths, one for each dimension or null if the
   * variable is not chunked.
   * 
   * @throws HDFException if an HDF error occurred.
   */
  public static native int[] getChunkLengths (
    int sdsid
  ) throws HDFException;

  ////////////////////////////////////////////////////////////

  public void close () throws IOException {

    // Close the HDF file
    // ------------------
    if (closed) return;
    try {
      if (!HDFLibrary.SDend (sdid)) 
        throw new HDFException ("Failed to end access");
    } // try
    catch (HDFException e) { 
      throw new IOException (e.getMessage ()); 
    } // catch
    closed = true;

  } // close

  ////////////////////////////////////////////////////////////

  /*
   * Gets the variable dimensions.
   * 
   * @param sdid the HDF dataset to read.
   * @param name the variable name.
   *
   * @return the dimensions of the specified variable.
   * 
   * @throws HDFException if an HDF error occurred.
   */
  public static int[] getVariableDimensions (
    int sdid,
    String varName
  ) throws HDFException {

      // Get variable index
      // ------------------
      int index = HDFLibrary.SDnametoindex (sdid, varName);
      if (index < 0)
        throw new HDFException ("Cannot access variable '" + varName + "'");

      // Access variable
      // ---------------
      int sdsid = HDFLibrary.SDselect (sdid, index);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index " + index);
 
      // Get variable info
      // -----------------
      String[] varNameArray = new String[] {""};
      int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
      int varInfo[] = new int[3];
      if (!HDFLibrary.SDgetinfo (sdsid, varNameArray, varDims, varInfo))
        throw new HDFException ("Cannot get variable info at index " + index);
      HDFLibrary.SDendaccess (sdsid);

      // Return dimensions
      // -----------------
      int rank = varInfo[0];
      int[] dims = new int[rank];
      System.arraycopy (varDims, 0, dims, 0, rank);
      return (dims);

  } // getVariableDimensions

  ////////////////////////////////////////////////////////////

} // HDFReader class

////////////////////////////////////////////////////////////////////////
