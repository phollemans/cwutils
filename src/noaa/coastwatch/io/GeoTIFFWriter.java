////////////////////////////////////////////////////////////////////////
/*

     File: GeoTIFFWriter.java
   Author: Peter Hollemans
     Date: 2003/03/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;

import java.awt.image.BandedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Vector;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.Transparency;

import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.GeoTIFFTagSet;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFTagSet;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;

import java.util.logging.Logger;

/**
 * <p>A GeoTIFF writer uses an earth image transform and rendered image
 * to create a TIFF file with extra TIFF tags decribing the earth
 * location of the image data.  The GeoTIFF content conforms to the
 * GeoTIFF specification version 1.8.2 as obtained from the <a
 * href="http://www.remotesensing.org/geotiff/geotiff.html">RemoteSensing.Org</a>
 * website.  The TIFF tag types and Java types are documented in the
 * javax.imageio.plugins.tiff.TIFFField class and extra notes on encoding
 * TIFF metadata linked from the javax.imageio package Javadoc.</p>
 *
 * <p>As of version 3.5.1, this class was re-written to use the javax.imageio
 * API rather than Java Advanced Imaging (JAI), due to conflicts between
 * JAI and JDK 11.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class GeoTIFFWriter {

  private static final Logger LOGGER = Logger.getLogger (GeoTIFFWriter.class.getName());

  // Constants
  // ---------
  /** The GeoTIFF earth transform translations file. */
  private static final String PROPERTIES_FILE = "geotiff.properties";

  /** The GCTP spheroid citation. */
  private static final String GCTP_CITE = "US Geological Survey (USGS) General Cartographic Transformations Package (GCTP)";

  /** The GeoTIFF key directory version. */
  private static final int KEY_DIRECTORY_VERSION = 1;

  /** The GeoTIFF key revision. */
  private static final int KEY_REVISION = 1;

  /** The GeoTIFF key minor revision. */
  private static final int MINOR_REVISION = 0;

  /**
   * We use the following tags to describe the TIFF file data,
   * baseline TIFF tags detailed on this site:
   *
   * http://www.awaresystems.be/imaging/tiff/tifftags/baseline.html
   *
   * Artist               Person who created the image.
   * DateTime             Date and time of image creation (YYYY:MM:DD HH:MM:SS).
   * ImageDescription     A string that describes the subject of the image.
   * Software             Name and version number of the software package(s)
   *                      used to create the image.
   * HostComputer         The computer and/or operating system in use
   *                      at the time of image creation.
   */

  /** The TIFF DateTime field format. */
  private static final String TIFF_DATE_TIME_FMT = "yyyy:MM:dd HH:mm:ss";

  /** The TIFF none compression type. */
  public static final int COMP_NONE = 0;

  /** The TIFF deflate compression type. */
  public static final int COMP_DEFLATE = 1;

  /** The TIFF PackBits compression type. */
  public static final int COMP_PACK = 2;

  /** The TIFF LZW compression type. */
  public static final int COMP_LZW = 3;

  /** The TIFF JPEG compression type. */
  public static final int COMP_JPEG = 4;

  // Variables
  // ---------
  
  /** The TIFF image writer */
  private ImageWriter tiffWriter;
  
  /** The TIFF directory of fields to use for encoding the file. */
  private TIFFDirectory tiffDir;
  
  /** The properties used for GeoTIFF values. */
  private static Properties props = null;

  /** The hash set of ignored projection system parameters. */
  private static HashSet<String> ignoreSet;

  ////////////////////////////////////////////////////////////

  /** Creates the ignored set of projection system parameters. */
  static {

    ignoreSet = new HashSet<>();
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
   * A <code>GeoKey</code> holds data for a single key of geographic
   * information.  Similar in some ways to a TIFF field.  But in the end,
   * a series of GeoKey data is encoded into TIFFField objects.
   *
   * @since 3.5.1
   */
  private static class GeoKey implements Comparable<GeoKey> {
  
    private int key;
    private int type;
    private int count;
    private Object data;
  
    public GeoKey (int key, int type, int count, Object data) {
      this.key = key;
      this.type = type;
      this.count = count;
      this.data = data;
    } // GeoKey
  
    public int getKey () { return (key); }
    public int getType () { return (type); }
    public int getCount () { return (count); }
    public String getAsString (int index) { return (((String[])data)[index]); }
    public int getAsInt (int index) { return ((int) (((char[])data)[index])); }
    public double getAsDouble (int index) { return (((double[])data)[index]); }
    
    @Override
    public boolean equals (Object obj) {
      boolean isEqual = false;
      if (obj != null && obj instanceof GeoKey) {
        GeoKey otherKey = (GeoKey) obj;
        isEqual = (this.key == otherKey.key);
      } // if
      return (isEqual);
    } // equals

    @Override
    public int hashCode() { return (Integer.hashCode (this.key)); }

    @Override
    public int compareTo​ (GeoKey otherKey) { return (Integer.compare (this.key, otherKey.key)); }

  } // GeoKey class

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF keys representing the specified UTM map projection
   * system.
   *
   * @param map the UTM map projection to convert.
   *
   * @return the list of GeoTIFF keys.
   *
   * @throws IllegalArgumentException if the map projection system is
   * not UTM.
   *
   * @since 3.3.2
   */
  private List<GeoKey> getUTMKeys (
    MapProjection map
  ) {

    // Check system
    // ------------
    if (map.getSystem() != GCTP.UTM)
      throw new IllegalArgumentException ("Projection system is not UTM");

    // Check zone
    // ----------
    int zone = map.getZone();
    int zoneValue = Math.abs (zone);
    String zoneHemisphere = (zone > 0 ? "N" : "S");
    if (zoneValue < 1 || zoneValue > 60)
      throw new IllegalArgumentException ("Expected UTM zone in range +[1..60] for north or -[1..60] for south");

    // Get UTM projection type
    // -----------------------
    String spheroidName = map.getSpheroidName();
    String spheroidKey = spheroidName.replace (" ", "");
    String zoneKey = zoneValue + zoneHemisphere;
    String pcsTypeKey = "PCS_" + spheroidKey + "_UTM_zone_" + zoneKey;
    String pcsCitation = "UTM Zone " + zoneValue + " " + zoneHemisphere + " with " + spheroidKey;
    int pcsTypeValue;
    try { pcsTypeValue = getIntValue (pcsTypeKey); }
    catch (Exception e) {
      throw new IllegalArgumentException ("Unsupported UTM zone / spheroid combination '" +
        pcsCitation + "'");
    } // catch

    LOGGER.fine ("UTM zone = " + zoneKey);

    // Create key list
    // ---------------
    List<GeoKey> keyList = new ArrayList<>();
    keyList.add (new GeoKey (getIntValue ("ProjectedCSTypeGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) pcsTypeValue}));
    keyList.add (new GeoKey (getIntValue ("PCSCitationGeoKey"),
      TIFFTag.TIFF_ASCII, 1, new String[] {pcsCitation}));

    return (keyList);

  } // getUTMKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF keys representing the specified map projection
   * system.
   *
   * @param map the map projection to convert.
   *
   * @return the list of GeoTIFF keys.
   *
   * @throws IllegalArgumentException if the map projection system is
   * not supported.
   */
  private List<GeoKey> getProjectionSystemKeys (
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
    List<GeoKey> keyList = new ArrayList<>();

    // Set projection system and citation
    // ----------------------------------
    keyList.add (new GeoKey (getIntValue ("ProjectedCSTypeGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("KvUserDefined")}));
    keyList.add (new GeoKey (getIntValue ("ProjectionGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("KvUserDefined")}));
    keyList.add (new GeoKey (getIntValue ("ProjCoordTransGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) coordTrans}));
    keyList.add (new GeoKey (getIntValue ("ProjLinearUnitsGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("Linear_Meter")}));
    keyList.add (new GeoKey (getIntValue ("PCSCitationGeoKey"),
      TIFFTag.TIFF_ASCII, 1, new String[] {systemName}));

    LOGGER.fine ("Proj system = " + systemName);

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
      keyList.add (new GeoKey (getIntValue ("GCTP_" + shortDesc),
        TIFFTag.TIFF_DOUBLE, 1, new double[] {value}));

      LOGGER.fine ("Proj param " + shortDesc + " = " + value);

    } // for

    return (keyList);

  } // getProjectionSystemKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF keys representing the specified map projection
   * spheroid.
   *
   * @param map the map projection to convert.
   *
   * @return a list of GeoTIFF keys.
   *
   * @throws IllegalArgumentException if the map projection spheroid is
   * not supported.
   */
  private List<GeoKey> getSpheroidKeys (
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
    List<GeoKey> keyList = new ArrayList<>();

    // Specify known ellipsoid
    // -----------------------
    if (ellipsoid != -1) {    
      keyList.add (new GeoKey (getIntValue ("GeographicTypeGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) ellipsoid}));

        LOGGER.fine ("Known ellipsoid = " + spheroidName);

    } // if

    // Specify semi-major and semi-minor axes
    // --------------------------------------
    else {
      keyList.add (new GeoKey (getIntValue ("GeographicTypeGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new GeoKey (getIntValue ("GeogGeodeticDatumGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new GeoKey (getIntValue ("GeogCitationGeoKey"),
        TIFFTag.TIFF_ASCII, 1, new String[] {spheroidName}));
      keyList.add (new GeoKey (getIntValue ("GeogEllipsoidGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("KvUserDefined")}));
      keyList.add (new GeoKey (getIntValue ("GeogLinearUnitsGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("Linear_Meter")}));
      keyList.add (new GeoKey (getIntValue ("GeogSemiMajorAxisGeoKey"),
        TIFFTag.TIFF_DOUBLE, 1, new double[] {semiMajor}));
      keyList.add (new GeoKey (getIntValue ("GeogSemiMinorAxisGeoKey"),
        TIFFTag.TIFF_DOUBLE, 1, new double[] {semiMinor}));

      LOGGER.fine ("Custom ellipsoid = " + spheroidName + ", semiMajor = " +
        semiMajor + ", semiMinor = " + semiMinor);

    } // else

    return (keyList);

  } // getSpheroidKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Translates a set of GeoTIFF keys into TIFF field objects.  This is how
   * the GeoTIFF standard stores values in a TIFF file, by encoding the
   * GeoTIFF keys as lists of values within a set of GeoTIFF-specific TIFF
   * tags.
   * 
   * @param keyList the list of GeoTIFF keys. Only TIFF types short,
   * ASCII, and double are allowed.
   *
   * @return the list of TIFF field objects to use as metadata in the TIFF
   * file.
   * 
   * @throws IllegalArgumentException if a GeoKey type is not among
   * the allowed types for GeoTIFF keys.
   */
  private List<TIFFField> translateKeys (
    TreeSet<GeoKey> keySet
  ) {

    // Count field values
    // ------------------
    int keys = keySet.size();
    int doubleValues = 0;
    int asciiValues = 0;
    Iterator<GeoKey> iter = keySet.iterator();
    while (iter.hasNext()) {
      GeoKey key = iter.next();
      switch (key.getType()) {
      case TIFFTag.TIFF_SHORT:
        break;
      case TIFFTag.TIFF_DOUBLE:
        doubleValues += key.getCount();
        break; 
      case TIFFTag.TIFF_ASCII:
        asciiValues += key.getAsString (0).length() + 1;
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
    iter = keySet.iterator();
    while (iter.hasNext()) {
      GeoKey key = iter.next();
      switch (key.getType()) {
      case TIFFTag.TIFF_SHORT:
        directoryArray[directoryIndex*4 + 0] = (char) key.getKey();
        directoryArray[directoryIndex*4 + 1] = (char) 0;
        directoryArray[directoryIndex*4 + 2] = (char) 1;
        directoryArray[directoryIndex*4 + 3] = (char) key.getAsInt (0);
        break;
      case TIFFTag.TIFF_DOUBLE:
        count = key.getCount();
        directoryArray[directoryIndex*4 + 0] = (char) key.getKey();
        directoryArray[directoryIndex*4 + 1] = (char) doubleTag;
        directoryArray[directoryIndex*4 + 2] = (char) count;
        directoryArray[directoryIndex*4 + 3] = (char) doubleIndex;
        for (i = 0; i < count; i++) 
          doubleArray[doubleIndex+i] = key.getAsDouble (i);
        doubleIndex += count;
        break; 
      case TIFFTag.TIFF_ASCII:
        count = key.getAsString (0).length() + 1;
        directoryArray[directoryIndex*4 + 0] = (char) key.getKey();
        directoryArray[directoryIndex*4 + 1] = (char) asciiTag;
        directoryArray[directoryIndex*4 + 2] = (char) count;
        directoryArray[directoryIndex*4 + 3] = (char) asciiIndex;
        asciiArray[0] += key.getAsString(0) + "|";
        asciiIndex += count;
        break; 
      } // switch
      directoryIndex++;
    } // while

    // Create TIFF fields
    // ------------------
    GeoTIFFTagSet geotiffSet = GeoTIFFTagSet.getInstance();
    TIFFTag tag;
    TIFFField field;

    List<TIFFField> tagList = new ArrayList<>();

    tag = geotiffSet.getTag ("GeoKeyDirectoryTag");
    field = new TIFFField (tag, TIFFTag.TIFF_SHORT, directoryArray.length, directoryArray);
    tagList.add (field);

    if (doubleValues != 0) {
      tag = geotiffSet.getTag ("GeoDoubleParamsTag");
      field = new TIFFField (tag, TIFFTag.TIFF_DOUBLE, doubleArray.length, doubleArray);
      tagList.add (field);
    } // if
    
    if (asciiValues != 0) {
      tag = geotiffSet.getTag ("GeoAsciiParamsTag");
      field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, asciiArray);
      tagList.add (field);
    } // if

    return (tagList);

  } // translateKeys

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the GeoTIFF fields representing the specified earth image
   * transform.
   * 
   * @param trans the earth image transform for encoding.
   *
   * @return the list of TIFF fields to use as extra fields.
   *
   * @throws RuntimeException if the image affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the earth transform is not a map
   * projection.
   */
  private List<TIFFField> getGeoFields (
    EarthImageTransform trans
  ) {

    // Get transforms
    // --------------
    EarthTransform earthTrans = trans.getEarthTransform();
    ImageTransform imageTrans = trans.getImageTransform();
    
    // Check for map projection
    // ------------------------
    if (!(earthTrans instanceof MapProjection)) 
      throw new IllegalArgumentException ("Earth transform must be a map projection");
    MapProjection map = (MapProjection) earthTrans;

    // Create lists of tags and keys
    // -----------------------------
    List<TIFFField> tagList = new ArrayList<>();
    TreeSet<GeoKey> keySet = new TreeSet<>();

    // Set main citation
    // -----------------
    keySet.add (new GeoKey (getIntValue ("GTCitationGeoKey"),
      TIFFTag.TIFF_ASCII, 1, new String[] {GCTP_CITE}));

    // Set raster type
    // ---------------
    keySet.add (new GeoKey (getIntValue ("GTRasterTypeGeoKey"),
      TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("RasterPixelIsArea")}));

    // Set geographic keys
    // -------------------
    if (map.getSystem() == GCTP.GEO) {
      keySet.add (new GeoKey (getIntValue ("GTModelTypeGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("ModelTypeGeographic")}));
      keySet.addAll (getSpheroidKeys (map));
    } // if

    // Set map projection keys
    // -----------------------
    else {
      keySet.add (new GeoKey (getIntValue ("GTModelTypeGeoKey"),
        TIFFTag.TIFF_SHORT, 1, new char[] {(char) getIntValue ("ModelTypeProjected")}));
      if (map.getSystem() == GCTP.UTM) {
        keySet.addAll (getUTMKeys (map));
      } // if
      else {
        keySet.addAll (getProjectionSystemKeys (map));
        keySet.addAll (getSpheroidKeys (map));
      } // else
    
    } // if

    // Get image (x,y) to model (x,y) transform
    // ----------------------------------------
    AffineTransform imageAffine;
    try { 
      imageAffine = imageTrans.getAffine().createInverse(); 
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
    
    GeoTIFFTagSet geotiffSet = GeoTIFFTagSet.getInstance();
    TIFFTag tag;
    TIFFField field;

    tag = geotiffSet.getTag ("ModelTransformationTag");
    field = new TIFFField (tag, TIFFTag.TIFF_DOUBLE, 16, modelTrans);
    tagList.add (field);

    // Translate keys to tags
    // ----------------------
    tagList.addAll (translateKeys (keySet));

    return (tagList);

  } // getGeoFields

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GeoTIFF writer using the specified output stream,
   * earth image transform, and no TIFF compression.
   * 
   * @param output the output stream for writing.
   * @param trans the earth image transform for earth location metadata.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public GeoTIFFWriter (
    ImageOutputStream output,
    EarthImageTransform trans
  ) throws IOException {

    this (output, trans, COMP_NONE);

  } // GeoTIFFWriter

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GeoTIFF writer using the specified output stream,
   * earth image transform, and compression.
   * 
   * @param output the output stream for writing.
   * @param trans the earth image transform for earth location metadata.
   * @param compress the TIFF compression type, either COMP_NONE, COMP_DEFLATE,
   * or COMP_PACK.
   *
   * @throws IOException if an error occurred setting up the GeoTIFF output.
   */
  public GeoTIFFWriter (
    ImageOutputStream output,
    EarthImageTransform trans,
    int compress
  ) throws IOException {

    // Initialize properties
    // ---------------------
    props = new Properties();
    InputStream stream = getClass().getResourceAsStream (PROPERTIES_FILE);
    if (stream == null) {
      throw new IOException ("Cannot find resource '" + PROPERTIES_FILE + "'");
    } // if
    props.load (stream);
    stream.close();    

    // Create writer
    // -------------
    tiffWriter = ImageIO.getImageWritersByFormatName ("tif").next();
    tiffWriter.setOutput (output);

    // Create directory of TIFF fields
    // -------------------------------
    BaselineTIFFTagSet baselineSet = BaselineTIFFTagSet.getInstance();
    GeoTIFFTagSet geotiffSet = GeoTIFFTagSet.getInstance();
    tiffDir = new TIFFDirectory (new TIFFTagSet[] {baselineSet, geotiffSet}, null);

    // Set encoding date/time
    // ----------------------
    TIFFTag tag;
    TIFFField field;
    
    String dateTime =  new SimpleDateFormat (TIFF_DATE_TIME_FMT).format (new Date());
    tag = baselineSet.getTag ("DateTime");
    field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, new String[] {dateTime});
    addField (field);

    // Set compression
    // ---------------
    tag = baselineSet.getTag ("Compression");
    char compressValue;
    switch (compress) {
    case COMP_NONE: compressValue = (char) BaselineTIFFTagSet.COMPRESSION_NONE; break;
    case COMP_PACK: compressValue = (char) BaselineTIFFTagSet.COMPRESSION_PACKBITS; break;
    case COMP_DEFLATE: compressValue = (char) BaselineTIFFTagSet.COMPRESSION_DEFLATE; break;
    case COMP_LZW: compressValue = (char) BaselineTIFFTagSet.COMPRESSION_LZW; break;
    case COMP_JPEG: compressValue = (char) BaselineTIFFTagSet.COMPRESSION_JPEG; break;
    default: throw new IOException ("Invalid compression type");
    } // switch
    field = new TIFFField (tag, TIFFTag.TIFF_SHORT, 1, new char[] {compressValue});
    addField (field);

    // If the compression is LZW, it's been discovered that using a predictor
    // that uses horizontal differencing can reduce the image size, so we add
    // that here.  Note that LZW is a TIFF extension.
    if (compress == COMP_LZW) {
      tag = baselineSet.getTag ("Predictor");
      field = new TIFFField (tag, TIFFTag.TIFF_SHORT, 1, new char[] {(char) BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING});
      addField (field);
    } // if

    // Set the tile sizes for cloud optimized GeoTIFFs.  This was requested
    // in 2021 for use with TIFF data stored in the cloud, so that a
    // cloud-optimized TIFF reading library can read byte ranges over HTTP
    // of just the tiles needed.  See https://trac.osgeo.org/gdal/wiki/CloudOptimizedGeoTIFF
    // for details.  The TIFF tags and usage are documented by Adobe at
    // https://www.adobe.io/content/dam/udp/en/open/standards/tiff/TIFF6.pdf.
    // Note that tiled images are a TIFF extension.

    tag = baselineSet.getTag ("TileWidth");
    field = new TIFFField (tag, TIFFTag.TIFF_SHORT, 1, new char[] {256});
    addField (field);

    tag = baselineSet.getTag ("TileLength");
    field = new TIFFField (tag, TIFFTag.TIFF_SHORT, 1, new char[] {256});
    addField (field);

    // Set geography
    // -------------
    List<TIFFField> geoFields = getGeoFields (trans);
    geoFields.forEach (this::addField);

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

    tiffDir.addTIFFField (field);

    TIFFTag tag = field.getTag();
    int count = field.getCount();
    StringBuffer value = new StringBuffer();
    for (int i = 0; i < count; i++) {
      value.append (field.getValueAsString (i));
      if (field.isIntegral() && tag.hasValueNames()) {
        String valueName = tag.getValueName (field.getAsInt (i));
        if (valueName != null) value.append ("(" + valueName + ")");
      } // if
      if (i < (count-1)) value.append (", ");
    } // for
    if (count > 1) { value.insert (0, "["); value.append ("]"); }
    String type = TIFFField.getTypeName (field.getType());

    LOGGER.fine ("TIFF field " + tag.getNumber() + "(" + tag.getName() +
      "), type = " + type + ", value = " + value);

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

    TIFFTag tag = BaselineTIFFTagSet.getInstance().getTag ("ImageDescription");
    TIFFField field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, new String[] {description});
    addField (field);

  } // setDescription

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the Artist TIFF field.
   *
   * @param artist the person who created the image.
   */
  public void setArtist (
    String artist
  ) {

    TIFFTag tag = BaselineTIFFTagSet.getInstance().getTag ("Artist");
    TIFFField field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, new String[] {artist});
    addField (field);

  } // setArtist

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the Software TIFF field.
   *
   * @param software the name and version number of the software package(s)
   * used to create the image.
   */
  public void setSoftware (
    String software
  ) {

    TIFFTag tag = BaselineTIFFTagSet.getInstance().getTag ("Software");
    TIFFField field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, new String[] {software});
    addField (field);

  } // setSoftware

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HostComputer TIFF field.
   *
   * @param computer the computer and/or operating system in use
   * at the time of image creation.
   */
  public void setComputer (
    String computer
  ) {

    TIFFTag tag = BaselineTIFFTagSet.getInstance().getTag ("HostComputer");
    TIFFField field = new TIFFField (tag, TIFFTag.TIFF_ASCII, 1, new String[] {computer});
    addField (field);

  } // setComputer

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

    IIOMetadata metadata = tiffDir.getAsMetadata();
    IIOImage imageData = new IIOImage (image, null, metadata);
    tiffWriter.write (metadata, imageData, null);

  } // encode

  ////////////////////////////////////////////////////////////

  /**
   * Holds arbitrary data with fake color model as a rendered image.  The
   * image is not suitable for display, but rather for passing directly to
   * the {@link #encode} method.
   *
   * @since 3.5.1
   */
  private static class DataImage implements RenderedImage {

    private Raster raster;
    private ColorModel colorModel;
    
    public DataImage (Raster raster) {

      this.raster = raster;

      // We trick the writer here and supply a color model that it
      // will not throw an error on.
      ColorSpace colorSpace = ColorSpace.getInstance (ColorSpace.CS_GRAY);
      colorModel = new ComponentColorModel (colorSpace, false, false,
        Transparency.OPAQUE, DataBuffer.TYPE_FLOAT) {
          public boolean isCompatibleRaster​ (Raster raster) { return (true); }
          public boolean isCompatibleSampleModel​ (SampleModel sm) { return (true); }
      };

    } // DataImage

    public WritableRaster copyData​ (WritableRaster copy) {
      if (copy == null) copy = raster.createCompatibleWritableRaster();
      copy.setRect (raster);
      return (copy);
    } // copyData

    public ColorModel getColorModel() { return (colorModel); }

    public Raster getData() { return (getData (raster.getBounds())); }

    public Raster getData​ (Rectangle rect) {
      WritableRaster copy = raster.createCompatibleWritableRaster​ (rect);
      copy.setRect (raster);
      return (copy);
    } // getData

    public int getHeight() { return (raster.getHeight()); }
    public int getMinTileX() { return (0); }
    public int getMinTileY() { return (0); }
    public int getMinX() { return (0); }
    public int getMinY() { return (0); }
    public int  getNumXTiles() { return (0); }
    public int  getNumYTiles() { return (0); }
    public Object getProperty​ (String name) { return (Image.UndefinedProperty); }
    public String[] getPropertyNames() { return (null); }
    public SampleModel getSampleModel() { return (raster.getSampleModel()); }
    public Vector<RenderedImage> getSources() { return (null); }
    public Raster getTile​ (int tileX, int tileY) { throw new UnsupportedOperationException(); }
    public int getTileGridXOffset() { return (0); }
    public int getTileGridYOffset() { return (0); }
    public int getTileHeight() { return (0); }
    public int getTileWidth() { return (0); }
    public int getWidth() { return (raster.getWidth()); }

  } // DataImage class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a multibanded image based on data from 32-bit float arrays.
   *
   * @param width the image width in pixels.
   * @param height the image height in pixels.
   * @param floatDataList the list of float data arrays for the bands, each
   * of length width*height.
   *
   * @return an image suitable for passing directly to the {@link #encode}
   * method.  The image should not be used for display.
   *
   * @since 3.5.1
   */
  static public RenderedImage createImageForData (
    int width,
    int height,
    List<float[]> floatDataList
  ) {

    int bands = floatDataList.size();
    BandedSampleModel sampleModel =
      new BandedSampleModel (DataBuffer.TYPE_FLOAT, width, height, bands);
    float[][] floatData = floatDataList.toArray (new float[0][0]);
    DataBuffer buffer = new DataBufferFloat (floatData, width*height);
    WritableRaster raster = Raster.createWritableRaster (sampleModel, buffer, null);
    RenderedImage dataImage = new DataImage (raster);

    return (dataImage);

  } // createImageForData

  ////////////////////////////////////////////////////////////

  /**
   * Writes a multi-image GeoTIFF file to the output stream using the
   * specified image data.
   *
   * @param imageList the list of images to write.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   *
   * @since 3.5.1
   */
  public void encodeImages (
    List<RenderedImage> imageList
  ) throws IOException {

    IIOMetadata metadata = tiffDir.getAsMetadata();
    tiffWriter.prepareWriteSequence (metadata);

    for (RenderedImage image : imageList) {
      IIOImage imageData = new IIOImage (image, null, metadata);
      tiffWriter.writeToSequence (imageData, null);
    } // for

    tiffWriter.endWriteSequence();
    
  } // encodeImages

  ////////////////////////////////////////////////////////////

} // GeoTIFFWriter class

////////////////////////////////////////////////////////////////////////
