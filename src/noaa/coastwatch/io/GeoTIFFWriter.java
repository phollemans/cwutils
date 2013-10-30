////////////////////////////////////////////////////////////////////////
/*
     FILE: GeoTIFFWriter.java
  PURPOSE: To write GeoTIFF format TIFF files.
   AUTHOR: Peter Hollemans
     DATE: 2003/03/03
  CHANGES: 2003/08/28, PFH, added DateTime field, setAuthor, setDescription
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2005/05/18, PFH, changed "datum" to "spheroid"
           2006/01/19, PFH, added compression

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.image.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.text.*;
import com.sun.media.jai.codec.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.render.*;

/**
 * A GeoTIFF writer uses an Earth image transform and rendered image
 * to create a TIFF file with extra TIFF tags decribing the Earth
 * location of the image data.  The GeoTIFF content conforms to the
 * GeoTIFF specification version 1.8.2 as obtained from the <a
 * href="http://www.remotesensing.org/geotiff/geotiff.html">RemoteSensing.Org</a>
 * website.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class GeoTIFFWriter {

  // Constants
  // ---------
  /** The GeoTIFF Earth transform translations file. */
  private static final String PROPERTIES_FILE = "geotiff.properties";

  /** The GCTP spheroid citation. */
  private static final String GCTP_CITE = "US Geological Survey (USGS) General Cartographic Transformations Package (GCTP)";

  /** The GeoTIFF key directory version. */
  private static final int KEY_DIRECTORY_VERSION = 1;

  /** The GeoTIFF key revision. */
  private static final int KEY_REVISION = 1;

  /** The GeoTIFF key minor revision. */
  private static final int MINOR_REVISION = 0;

  /** The TIFF Artist tag value. */
  private static final int TIFFTAG_ARTIST = 315; 

  /** The TIFF DateTime tag value. */
  private static final int TIFFTAG_DATETIME = 306; 

  /** The TIFF DateTime field format. */
  private static final String TIFF_DATETIME_FMT = "yyyy:MM:dd HH:mm:ss";

  /** The TIFF ImageDescription tag value. */
  private static final int TIFFTAG_IMAGEDESCRIPTION = 270;

  /** The TIFF none compression type. */
  public static final int COMP_NONE = TIFFEncodeParam.COMPRESSION_NONE;

  /** The TIFF deflate compression type. */
  public static final int COMP_DEFLATE = TIFFEncodeParam.COMPRESSION_DEFLATE;

  /** The TIFF PackBits compression type. */
  public static final int COMP_PACK = TIFFEncodeParam.COMPRESSION_PACKBITS;

  // Variables
  // ---------
  /** The TIFF image encoder. */
  private ImageEncoder encoder;

  /** The properties used for GeoTIFF values. */
  private static Properties props = null;

  /** The hash set of ignored projection system parameters. */
  private static HashSet ignoreSet;

  ////////////////////////////////////////////////////////////

  /** Creates the ignored set of projection system parameters. */
  static {

    ignoreSet = new HashSet();
    ignoreSet.add ("SMajor");
    ignoreSet.add ("SMinor");
    ignoreSet.add ("Sphere");

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Gets a string value for the specified key in the property table.
   * Alias property strings are recursively searched until a final
   * value is found.
   *
   * @param key the key string to look for.
   *
   * @return the key string value.
   *
   * @throws RuntimeException if the key value is not found.
   */
  private static String getStringValue (
    String key
  ) {

    // Get value
    // ---------
    String value = props.getProperty (key);
    if (value == null) 
      throw new RuntimeException ("No value found for key '" + key + "'");

    // Search for recursively defined value
    // ------------------------------------
    String newValue;
    do {
      try { 
        newValue = getStringValue (value); 
        value = newValue;
      } // try
      catch (RuntimeException e) { newValue = null; }
    } while (newValue != null);
    return (value);

  } // getStringValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets an integer value for the specified key in the property
   * table.
   *
   * @param key the key string to look for.
   *
   * @return the key integer value.
   *
   * @throws RuntimeException if the key value is not found.
   */
  private static int getIntValue (
    String key
  ) {

    String value = getStringValue (key);
    return (Integer.parseInt (value));

  } // getIntValue

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF keys representing the specified map projection
   * system as TIFF field objects.
   *
   * @param map the map projection to convert.
   *
   * @return a vector of GeoTIFF keys as TIFF field objects.
   *
   * @throws IllegalArgumentException if the map projection system is
   * not supported.
   */
  private List getProjectionSystemKeys (
    MapProjection map
  ) {

    // Get coordinate transformation code
    // ----------------------------------
    String systemName = map.getSystemName();
    int coordTrans;
    try {
      String key = "GCTP_" + systemName.replace (' ', '_');
      coordTrans = getIntValue (key);
    } // try
    catch (Exception e) {
      throw new IllegalArgumentException ("Unsupported projection system '" +
        systemName + "'");
    } // catch

    // Get system and parameters
    // -------------------------
    int system = map.getSystem();
    double[] params = map.getParameters();

    // Initialize key list
    // -------------------
    List keyList = new ArrayList();

    // Set projection system and citation
    // ----------------------------------
    keyList.add (new TIFFField (getIntValue ("ProjectedCSTypeGeoKey"),
      TIFFField.TIFF_SHORT, 1, 
      new char[] {(char) getIntValue ("KvUserDefined")}));
    keyList.add (new TIFFField (getIntValue ("ProjectionGeoKey"),
      TIFFField.TIFF_SHORT, 1, 
      new char[] {(char) getIntValue ("KvUserDefined")}));
    keyList.add (new TIFFField (getIntValue ("ProjCoordTransGeoKey"),
      TIFFField.TIFF_SHORT, 1, new char[] {(char) coordTrans}));
    keyList.add (new TIFFField (getIntValue ("ProjLinearUnitsGeoKey"), 
      TIFFField.TIFF_SHORT, 1, 
      new char[] {(char) getIntValue ("Linear_Meter")}));
    keyList.add (new TIFFField (getIntValue ("PCSCitationGeoKey"),
      TIFFField.TIFF_ASCII, 1, new String[] {systemName}));

    // Add projection system requirements
    // ----------------------------------
    GCTP.Requirements require = GCTP.getRequirements (system)[0];
    for (int i = 0; i < GCTP.Requirements.MAX_PARAMETERS; i++) {

      // Check if parameter is required and not ignored
      // ----------------------------------------------
      String shortDesc = require.getShortDescription (i);
      if (!require.isRequired (i) || ignoreSet.contains (shortDesc)) 
        continue;

      // Add parameter value
      // -------------------
      double value = params[i];
      if (require.getUnits(i).equals ("degrees"))
        value = GCTP.unpack_angle (value);
      keyList.add (new TIFFField (getIntValue ("GCTP_" + shortDesc),
        TIFFField.TIFF_DOUBLE, 1, new double[] {value}));

    } // for

    return (keyList);

  } // getProjectionSystemKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF keys representing the specified map projection
   * spheroid as TIFF field objects.
   *
   * @param map the map projection to convert.
   *
   * @return a vector of GeoTIFF keys as TIFF field objects.
   *
   * @throws IllegalArgumentException if the map projection spheroid is
   * not supported.
   */
  private List getSpheroidKeys (
    MapProjection map
  ) {

    // Check for negative spheroid code
    // --------------------------------
    int spheroid = map.getSpheroid();
    if (spheroid < 0) throw new IllegalArgumentException (
      "Encountered unsupported negative spheroid code in map projection");

    // Get ellipsoid code
    // ------------------
    String spheroidName = map.getSpheroidName();
    String ellipsoidValue;
    try {
      String key = "GCTP_" + spheroidName.replace (' ', '_');
      ellipsoidValue = getStringValue (key);
    } // try
    catch (Exception e) {
      throw new IllegalArgumentException ("Unsupported projection spheroid '" +
        spheroidName + "'");
    } // catch
    int ellipsoid = -1;
    double semiMajor = 0, semiMinor = 0;
    try {
      ellipsoid = Integer.parseInt (ellipsoidValue);
    } // try
    catch (NumberFormatException e) {
      String[] array = ellipsoidValue.split (",");
      semiMajor = Double.parseDouble (array[0]);
      semiMinor = Double.parseDouble (array[1]);
    } // catch

    // Create key list
    // ---------------
    List keyList = new ArrayList();

    // Specify known ellipsoid
    // -----------------------
    if (ellipsoid != -1) {    
      keyList.add (new TIFFField (getIntValue ("GeographicTypeGeoKey"),
        TIFFField.TIFF_SHORT, 1, new char[] {(char) ellipsoid}));
    } // if

    // Specify semi-major and semi-minor axes
    // --------------------------------------
    else {
      keyList.add (new TIFFField (getIntValue ("GeographicTypeGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new TIFFField (getIntValue ("GeogGeodeticDatumGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new TIFFField (getIntValue ("GeogCitationGeoKey"),
        TIFFField.TIFF_ASCII, 1, new String[] {spheroidName}));
      keyList.add (new TIFFField (getIntValue ("GeogEllipsoidGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new TIFFField (getIntValue ("GeogLinearUnitsGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("Linear_Meter")}));
      keyList.add (new TIFFField (getIntValue ("GeogSemiMajorAxisGeoKey"),
        TIFFField.TIFF_DOUBLE, 1, new double[] {semiMajor}));
      keyList.add (new TIFFField (getIntValue ("GeogSemiMinorAxisGeoKey"),
        TIFFField.TIFF_DOUBLE, 1, new double[] {semiMinor}));
    } // else

    return (keyList);

  } // getSpheroidKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Translates a set of GeoTIFF keys into TIFF fields.
   * 
   * @param keyList the list of GeoTIFF keys as TIFF field objects. Only
   * TIFF types short, ASCII, and double are allowed.
   *
   * @return a vector of TIFF field objects.
   * 
   * @throws IllegalArgumentException if a TIFF field type is not among
   * the allowed types for GeoTIFF keys.
   */
  private List translateKeys (
    TreeSet keyList
  ) {

    // Count field values
    // ------------------
    int keys = keyList.size();
    int doubleValues = 0;
    int asciiValues = 0;
    Iterator iter = keyList.iterator();
    while (iter.hasNext()) {
      TIFFField field = (TIFFField) iter.next();
      switch (field.getType()) {
      case TIFFField.TIFF_SHORT: 
        break;
      case TIFFField.TIFF_DOUBLE: 
        doubleValues += field.getCount();
        break; 
      case TIFFField.TIFF_ASCII: 
        asciiValues += field.getAsString(0).length() + 1; 
        break;
      default:
        throw new IllegalArgumentException (
          "Unsupported TIFF type in GeoTIFF keys");
      } // switch
    } // while

    // Create field data arrays
    // ------------------------
    char[] directoryArray = new char[(keys+1)*4];
    double[] doubleArray = new double[doubleValues];
    String[] asciiArray = new String[] {""};

    // Initialize directory array
    // --------------------------
    directoryArray[0] = (char) KEY_DIRECTORY_VERSION;
    directoryArray[1] = (char) KEY_REVISION;
    directoryArray[2] = (char) MINOR_REVISION;
    directoryArray[3] = (char) keys;

    // Get tag values
    // --------------
    int directoryTag = getIntValue ("GeoKeyDirectoryTag");
    int doubleTag = getIntValue ("GeoDoubleParamsTag");
    int asciiTag = getIntValue ("GeoAsciiParamsTag");

    // Add keys to field data arrays
    // -----------------------------   
    int directoryIndex = 1;
    int doubleIndex = 0;
    int asciiIndex = 0;    
    int count, i;
    iter = keyList.iterator();
    while (iter.hasNext()) {
      TIFFField field = (TIFFField) iter.next();
      switch (field.getType()) {
      case TIFFField.TIFF_SHORT: 
        directoryArray[directoryIndex*4 + 0] = (char) field.getTag();
        directoryArray[directoryIndex*4 + 1] = (char) 0;
        directoryArray[directoryIndex*4 + 2] = (char) 1;
        directoryArray[directoryIndex*4 + 3] = (char) field.getAsInt (0);
        break;
      case TIFFField.TIFF_DOUBLE: 
        count = field.getCount();
        directoryArray[directoryIndex*4 + 0] = (char) field.getTag();
        directoryArray[directoryIndex*4 + 1] = (char) doubleTag;
        directoryArray[directoryIndex*4 + 2] = (char) count;
        directoryArray[directoryIndex*4 + 3] = (char) doubleIndex;
        for (i = 0; i < count; i++) 
          doubleArray[doubleIndex+i] = field.getAsDouble (i);
        doubleIndex += count;
        break; 
      case TIFFField.TIFF_ASCII: 
        count = field.getAsString(0).length() + 1;
        directoryArray[directoryIndex*4 + 0] = (char) field.getTag();
        directoryArray[directoryIndex*4 + 1] = (char) asciiTag;
        directoryArray[directoryIndex*4 + 2] = (char) count;
        directoryArray[directoryIndex*4 + 3] = (char) asciiIndex;
        asciiArray[0] += field.getAsString(0) + "|";
        asciiIndex += count;
        break; 
      } // switch
      directoryIndex++;
    } // while

    // Create TIFF fields
    // ------------------
    List tagList = new ArrayList();
    TIFFField directoryField = new TIFFField (directoryTag, 
      TIFFField.TIFF_SHORT, directoryArray.length, directoryArray);
    tagList.add (directoryField);
    if (doubleValues != 0) {
      TIFFField doubleField = new TIFFField (doubleTag, 
        TIFFField.TIFF_DOUBLE, doubleArray.length, doubleArray);
      tagList.add (doubleField);
    } // if
    if (asciiValues != 0) {
      TIFFField asciiField = new TIFFField (asciiTag, 
        TIFFField.TIFF_ASCII, 1, asciiArray);
      tagList.add (asciiField);
    } // if

    return (tagList);

  } // translateKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF fields representing the specified Earth image
   * transform.
   * 
   * @param trans the Earth image transform for encoding.
   *
   * @return the TIFF fields to use as extra fields.
   *
   * @throws RuntimeException if the image affine transform is not
   * invertible.
   */
  private TIFFField[] getGeoFields (
    EarthImageTransform trans
  ) {

    // Get transforms
    // --------------
    EarthTransform earthTrans = trans.getEarthTransform();
    ImageTransform imageTrans = trans.getImageTransform();
    
    // Check for map projection
    // ------------------------
    if (!(earthTrans instanceof MapProjection)) 
      throw new IllegalArgumentException (
        "Earth transform must be a map projection");
    MapProjection map = (MapProjection) earthTrans;

    // Create lists of tags and keys
    // -----------------------------
    TreeSet tagList = new TreeSet();
    TreeSet keyList = new TreeSet();

    // Set main citation
    // -----------------
    keyList.add (new TIFFField (getIntValue ("GTCitationGeoKey"),
      TIFFField.TIFF_ASCII, 1, new String[] {GCTP_CITE}));

    // Set raster type
    // ---------------
    keyList.add (new TIFFField (getIntValue ("GTRasterTypeGeoKey"),
      TIFFField.TIFF_SHORT, 1, 
      new char[] {(char) getIntValue ("RasterPixelIsArea")}));

    // Set geographic keys
    // -------------------
    if (map.getSystem() == GCTP.GEO) {
      keyList.add (new TIFFField (getIntValue ("GTModelTypeGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("ModelTypeGeographic")}));
    } // if

    // Set map projection keys
    // -----------------------
    else {
      keyList.add (new TIFFField (getIntValue ("GTModelTypeGeoKey"),
        TIFFField.TIFF_SHORT, 1, 
        new char[] {(char) getIntValue ("ModelTypeProjected")}));
      keyList.addAll (getProjectionSystemKeys (map));
    } // else

    // Set spheroid keys
    // -----------------
    keyList.addAll (getSpheroidKeys (map));

    // Get image (x,y) to model (x,y) transform
    // ----------------------------------------
    AffineTransform imageAffine;
    try { 
      imageAffine = imageTrans.getAffine().createInverse(); 
      imageAffine.translate (-0.5, -0.5);
    } // try
    catch (NoninvertibleTransformException e) { 
      throw new RuntimeException (e.getMessage());
    } // catch
    AffineTransform mapAffine = map.getAffine();
    AffineTransform modelAffine = (AffineTransform) mapAffine.clone();
    modelAffine.concatenate (imageAffine);
    double[] matrix = new double[6];
    modelAffine.getMatrix (matrix);

    // Set model transformation tag
    // ----------------------------
    double[] modelTrans = new double[16];
    modelTrans[0] = matrix[0];
    modelTrans[1] = matrix[2];
    modelTrans[3] = matrix[4];
    modelTrans[4] = matrix[1];
    modelTrans[5] = matrix[3];
    modelTrans[7] = matrix[5];
    modelTrans[15] = 1;
    tagList.add (new TIFFField (getIntValue ("ModelTransformationTag"),
      TIFFField.TIFF_DOUBLE, 16, modelTrans));

    // Translate keys to tags
    // ----------------------
    tagList.addAll (translateKeys (keyList));
    return ((TIFFField[]) tagList.toArray (new TIFFField[] {}));

  } // getGeoFields

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GeoTIFF writer using the specified output stream,
   * Earth image transform, and no TIFF compression.
   * 
   * @param output the output stream for writing.
   * @param trans the Earth image transform for Earth location metadata.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public GeoTIFFWriter (
    OutputStream output,
    EarthImageTransform trans
  ) throws IOException {

    this (output, trans, COMP_NONE);

  } // GeoTIFFWriter

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GeoTIFF writer using the specified output stream,
   * Earth image transform, and compression.
   * 
   * @param output the output stream for writing.
   * @param trans the Earth image transform for Earth location metadata.
   * @param compress the TIFF compression type.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public GeoTIFFWriter (
    OutputStream output,
    EarthImageTransform trans,
    int compress
  ) throws IOException {

    // Initialize properties
    // ---------------------
    props = new Properties();
    InputStream stream = getClass().getResourceAsStream (
      PROPERTIES_FILE);
    if (stream == null) {
      throw new IOException ("Cannot find resource '" + 
        PROPERTIES_FILE + "'");
    } // if
    props.load (stream);
    stream.close();    

    // Create initial TIFF parameters
    // ------------------------------
    TIFFEncodeParam param = new TIFFEncodeParam();
    param.setCompression (compress);
    param.setExtraFields (getGeoFields (trans));
   
    // Create encoder
    // --------------
    encoder = ImageCodec.createImageEncoder ("tiff", output, param);

    // Set encoding date/time
    // ----------------------
    String dateTime =  new SimpleDateFormat (TIFF_DATETIME_FMT).format (
      new Date());
    addField (new TIFFField (TIFFTAG_DATETIME, TIFFField.TIFF_ASCII, 
      1, new String[] {dateTime}));

  } // GeoTIFFWriter

  ////////////////////////////////////////////////////////////

  /** 
   * Adds an extra TIFF field to the existing parameters.  The field
   * must be added prior to encoding in order to take effect.
   *
   * @param field the TIFF field to add.
   */
  private void addField (
    TIFFField field
  ) {

    // Get the existing fields
    // -----------------------
    TIFFEncodeParam param = (TIFFEncodeParam) encoder.getParam();
    TIFFField[] fields = param.getExtraFields();     
    if (fields == null) fields = new TIFFField[0];

    // Add one more array entry
    // ------------------------
    TIFFField[] newFields = new TIFFField[fields.length+1];    
    System.arraycopy (fields, 0, newFields, 0, fields.length);
    newFields[fields.length] = field;

    // Set the new fields
    // ------------------
    param.setExtraFields (newFields);

  } // addField

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the ImageDescription TIFF field.
   *
   * @param description the description of the image.
   */
  public void setDescription (
    String description
  ) {

    addField (new TIFFField (TIFFTAG_IMAGEDESCRIPTION,
      TIFFField.TIFF_ASCII, 1, new String[] {description}));

  } // setDescription

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the Artist TIFF field.
   *
   * @param artist the artist who created the image.
   */
  public void setArtist (
    String artist
  ) {

    addField (new TIFFField (TIFFTAG_ARTIST,
      TIFFField.TIFF_ASCII, 1, new String[] {artist}));

  } // setArtist

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a GeoTIFF file to the output stream using the specified
   * image data.
   *
   * @param image the image to write.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public void encode (
    RenderedImage image
  ) throws IOException {

    encoder.encode (image);

  } // encode

  ////////////////////////////////////////////////////////////

} // GeoTIFFWriter class

////////////////////////////////////////////////////////////////////////
