////////////////////////////////////////////////////////////////////////
/*
     FILE: CWFReader.java
  PURPOSE: A class to read CoastWatch IMGMAP format files using the
           JNI interface to the CWF library.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/04/21, MSR, added implementation
           2002/05/14, PFH, added javadoc, package, revised code
           2002/05/17, PFH, added long names to variables
           2002/06/01, PFH, added GCTP.STD_RADIUS
           2002/06/07, PFH, added variable names array, allowed null transform
           2002/07/12, PFH, added close, closed test
           2002/07/28, PFH, converted to location classes
           2002/11/12, PFH, removed pass type attribute
           2002/11/18, PFH, added dimension attributes to info metadata
           2002/12/03, PFH, modified for map projection changes
           2003/01/10, PFH, modified initial history string
           2003/02/20, PFH, added 2-digit year detection for older CWF files
           2003/03/10, PFH, fixed sst name translation in variables array
           2004/02/15, PFH, added super() call in constructor
           2004/02/16, PFH, forced CW byte data to unsigned byte variable
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/03/30, PFH
             - added getCWID() method
             - modified to use CWFCachedGrid
           2004/04/10, PFH, added getDataFormat() method
           2004/06/08, PFH, added updateNavigation() method
           2004/09/28, PFH, removed automatic history append on read
           2004/10/05, PFH, modified for new MapProjection constructor
           2005/02/11, PFH, added check in updateNavigation() for identity
           2005/05/18, PFH, changed "datum" to "spheroid"
           2005/05/19, PFH, changed to assume WGS72 spheroid (ie: from L1B)
           2005/06/08, PFH, updated units strings
           2005/06/22, PFH, added raw metadata reading
           2006/01/10, PFH, changed missing value for byte variable to 0
           2006/05/28, PFH, modified to use MapProjectionFactory
           2006/11/03, PFH, changed getPreview(int) to getPreviewImpl(int)

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.text.*;
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A CWF reader is an Earth data reader that reads CoastWatch
 * IMGMAP format files using the CWF class.
 */
public class CWFReader 
  extends EarthDataReader {

  // Constants
  // ---------
  /** Short variable names. */
  private static final String[] SHORT_NAMES = {
    "avhrr_ch1",          
    "avhrr_ch2",            
    "avhrr_ch3",
    "avhrr_ch4",          
    "avhrr_ch5",            
    "sst",
    "scan_angle",         
    "sat_zenith",           
    "solar_zenith",
    "rel_azimuth",        
    "scan_time",            
    "ocean_reflect",
    "turbidity",
    "cloud",
    "graphics"
  };

  /** Descriptive variable names. */
  private static final String[] LONG_NAMES = {
    "AVHRR channel 1",
    "AVHRR channel 2",
    "AVHRR channel 3",
    "AVHRR channel 4",         
    "AVHRR channel 5",
    "sea surface temperature",
    "scan angle",
    "satellite zenith angle",   
    "solar zenith angle",
    "relative azimuth angle",
    "scan time",
    "ocean reflectance",
    "turbidity",
    "NESDIS CLAVR cloud mask",
    "graphics overlay planes"
  };

  /** The data format description. */
  private static final String DATA_FORMAT = "CoastWatch IMGMAP (.cwf)";

  // Variables
  // ---------

  /** CoastWatch file id. */
  private int cwid;

  /** Flag to signify that the file is closed. */
  private boolean closed;

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { return (DATA_FORMAT); }

  ////////////////////////////////////////////////////////////

  /**
   * Construct a CWF reader from the specified file.
   * 
   * @param file the file name to read.
   *
   * @throws IOException if the file had an IO error.
   * @throws UnsupportedEncodingException if an error occurred reading
   * the file metadata.
   * @throws NoninvertibleTransformException if the Earth transform object
   * could not be initialized.
   */  
  public CWFReader (
    String file
  ) throws IOException, UnsupportedEncodingException, 
    NoninvertibleTransformException  {

    super (file);

    int attr_num;
    int var_id = 0;
    EarthTransform trans;

    // Open the file
    // -------------
    closed = true;
    try { cwid = CWF.open (file, CWF.CW_NOWRITE); }
    catch (Exception e) { throw new IOException (e.getMessage()); }
    closed = false;

    // Create satellite data info
    // --------------------------
    String sat = CWF.get_attribute_string (cwid, var_id, "satellite_id");
    String sensor = "avhrr";
    Date date = getDate ();
    EarthTransform transform = getTransform ();
    String origin = "USDOC/NOAA/NESDIS CoastWatch";
    String history = "";
    info = new SatelliteDataInfo (sat, sensor, date, transform,
      origin, history);

    // Get variable names
    // ------------------
    List list = new ArrayList();
    for (int i = 0; ; i++) {
      String name = null;
      try { name = CWF.inquire_variable_name (cwid, i); }
      catch (Exception e) { break; }
      if (name.indexOf ("sst") != -1) name = "sst";
      list.add (name);
    } // for
    variables = new String[list.size()];
    list.toArray (variables);    

    // Add global metadata
    // -------------------
    int atts = CWF.inquire_variable_attributes (cwid, var_id);
    for (int i = 0; i < atts; i++) {

      // Get attribute name
      // ------------------
      String attName = CWF.inquire_attribute_name (cwid, var_id, i);
      int attType = CWF.inquire_attribute_type (cwid, var_id, attName);

      // Get attribute value
      // -------------------
      Object attValue;
      switch (attType) {
        case CWF.CW_CHAR:
          attValue = CWF.get_attribute_string (cwid, var_id, attName);
          break;
        case CWF.CW_SHORT:
          attValue = new Short (CWF.get_attribute_short (cwid, var_id,
            attName));
          break;
        case CWF.CW_FLOAT:
          attValue = new Float (CWF.get_attribute_float (cwid, var_id,
            attName));
          break;
        default:
          throw new UnsupportedEncodingException (
            "Unsupported CWF attribute type = " + attType);
      } // switch

      // Add name/value pair
      // -------------------
      rawMetadataMap.put (attName, attValue);

    } // for

  } // CWFReader

  ////////////////////////////////////////////////////////////

  protected DataVariable getPreviewImpl (
    int index
  ) throws IOException {

    // Check validity of index
    // -----------------------
    if (index < 0 || index > variables.length - 1) 
      throw new ArrayIndexOutOfBoundsException ();

    // Get attributes
    // --------------
    String name = CWF.inquire_variable_name (cwid, index);
    if (name.indexOf ("sst") != -1) name = "sst";
    String longName = getLongName (name);
    String units = getUnits (index);
    int dim_ids[] = CWF.inquire_variable_dimension_ids (cwid, index);
    int rows = CWF.inquire_dimension_length (cwid, dim_ids[0]);
    int cols = CWF.inquire_dimension_length (cwid, dim_ids[1]);

    // Set format, scaling, missing
    // ----------------------------
    NumberFormat format = NumberFormat.getInstance();
    double[] scaling;
    Object missing;
    Object data;
    int varType = CWF.inquire_variable_type (cwid, index);
    boolean isUnsigned;
    if (varType == CWF.CW_FLOAT) {
      if (units.equals ("degrees")) {
        format.setMaximumFractionDigits (3);
        scaling = new double[] {0.005, 0};
        missing = new Short ((short)-32768);
      } // if
      else {
        format.setMaximumFractionDigits (2);
        scaling = new double[] {0.01, 0};
        missing = new Short ((short)-32768);
      } // else
      data = new short[1];
      isUnsigned = false;
    } // if
    else if (varType == CWF.CW_BYTE) {
      format.setMaximumFractionDigits (0);
      scaling = null;
      missing = new Byte ((byte) 0);
      data = new byte[1];
      isUnsigned = true;
    } // else
    else 
      throw new UnsupportedEncodingException (
        "Unsupported variable type: " + varType);

    // Create grid and return
    // ----------------------
    Grid grid = new Grid (name, longName, units, rows, cols, data,
      format, scaling, missing);
    grid.setUnsigned (isUnsigned);
    return (grid);

  } // getPreviewImpl

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index
  ) throws IOException {

    // Get a preview grid
    // ------------------
    Grid grid = (Grid) getPreview (index);

    // Create cached grid
    // ------------------
    return (new CWFCachedGrid (grid, this, index));

  } // getVariable

  ////////////////////////////////////////////////////////////

  public void close () throws IOException {

    // Close the CWF file
    // ------------------
    if (closed) return;
    try {
      CWF.close (cwid);
    } // try
    catch (Exception e) {
      throw new IOException (e.getMessage ()); 
    } // catch
    closed = true;

  } // close

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the descriptive variable name.
   *
   * @param name the short variable name.
   *
   * @return the descriptive variable name.
   */
  private String getLongName (
    String name
  ) {

    // Find variable name in the list
    // ------------------------------
    for (int i = 0; i < SHORT_NAMES.length; i++)
      if (name.equals (SHORT_NAMES[i])) return (LONG_NAMES[i]);
    return ("unknown");

  } // getLongName

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable units.
   *
   * @param index the variable index.
   *
   * @return the variable units string.  If the variable has no known
   * units, an empty string is returned.
   */
  private String getUnits (
    int index
  ) {

    // Get variable data id
    // --------------------
    String data_id;
    try {
      data_id = CWF.get_attribute_string (cwid, index, "data_id");
    } //try
    catch (Exception e) {
      return ("");
    } // catch

    // Create units string
    // -------------------
    String varName = CWF.inquire_variable_name (cwid, index);
    if (data_id.equals ("infrared"))
      return ("celsius");
    else if (data_id.equals ("visible"))
      return ("percent");
    else if (data_id.equals ("ancillary")) {
      if (varName.equals ("scan_time"))
        return ("hours");
      else
        return ("degrees");
    } // else if
    else
      return ("");

  } // getUnits

  ////////////////////////////////////////////////////////////

  /**
   * Reads the date and time.  The date and time metadata in the CWF
   * file are converted into the equivalent <code>Date</code>.
   *
   * @return a new date based on the CWF file data.
   */
  private Date getDate () {

    // Read the date
    // -------------
    int var_id = 0;
    int year = CWF.get_attribute_short (cwid, var_id, "orbit_start_year");
    if (year < 100) {
      if (year >= 70)
        year = 1900 + year;
      else
        year = 2000 + year;
    } /* if */
    short month_day = CWF.get_attribute_short (cwid, var_id, 
      "orbit_start_month_day");
    int day = month_day%100;
    int month = month_day/100;
    short hour_minute = CWF.get_attribute_short (cwid, var_id,
      "orbit_start_hour_minute");

    // Read the time
    // -------------
    int minute = hour_minute%100;
    int hour = hour_minute/100;
    int second = CWF.get_attribute_short (cwid, var_id,
      "orbit_start_second");

    // Return the date
    // ---------------
    Calendar cal = new GregorianCalendar (year, month-1, day, hour, minute, 
      second);
    cal.setTimeZone (TimeZone.getTimeZone ("GMT+00:00"));
    return (cal.getTime ());

  } // getDate

  ////////////////////////////////////////////////////////////

  /**
   * Reads the Earth transform information.  The projection metadata
   * in the CWF file is converted into the equivalent {@link
   * MapProjection}.
   *
   * @return an Earth transform based on the CWF file data.
   *
   * @throws NoninvertibleTransformException if the map projection object
   * could not be initialized.
   */
  private EarthTransform getTransform () 
    throws NoninvertibleTransformException {

    // Get the dimensions
    // ------------------
    int var_id = 0;
    int[] dim_ids = CWF.inquire_variable_dimension_ids (cwid, var_id);
    int rows = CWF.inquire_dimension_length (cwid, dim_ids[0]);
    int cols = CWF.inquire_dimension_length (cwid, dim_ids[1]);

    // Initialize the CWF projection routines
    // --------------------------------------
    CWF.init_projection (cwid);
    CWFProjectionInfo info = CWF.projection_info();

    // Initialize GCTP parameters
    // --------------------------
    double[] parameters = new double[15];
    double pixelSize;
    int system;
    int zone = 0;
    int spheroid = GCTP.WGS72;
    double center_ll[] = CWF.get_latitiude_longitude ((cols+1)/2.0, 
      (rows+1)/2.0);
    EarthLocation center = new EarthLocation (center_ll[0], center_ll[1], 
      DatumFactory.create (GCTP.WGS72));

    // Create a projection based on type
    // ---------------------------------
    switch (info.projection_type) {

      // Polar sterographic
      // ------------------
      case CWF.POLAR:
        system = GCTP.PS;
        pixelSize = info.resolution*(GCTP.STD_RADIUS/CWF.EARTH_RADIUS)*1000;
        parameters[4] = GCTP.pack_angle (info.prime_longitude);
        parameters[5] = GCTP.pack_angle (info.hemisphere*60);
        break;

      // Mercator
      // --------
      case CWF.MERCATOR:
        system = GCTP.MERCAT;
        pixelSize = info.resolution*(GCTP.STD_RADIUS/CWF.EARTH_RADIUS)*1000;
        break;

      // Linear lat/lon
      // --------------
      case CWF.LINEAR:
        system = GCTP.GEO;
        pixelSize = info.resolution;
        break;

      // Unknown
      // -------
      default:
        return (null);

    } // switch

    // Return the projection
    // ---------------------
    return (MapProjectionFactory.getInstance().create (system, zone, 
      parameters, spheroid, new int[] {rows, cols}, center, 
      new double[] {pixelSize, pixelSize}));

  } // getTransform

  ////////////////////////////////////////////////////////////

  /** Gets the CWF dataset ID. */
  public int getCWID() { return (cwid); }

  ////////////////////////////////////////////////////////////

  /**
   * Updates the navigation transform for the specified list of
   * variables.  CWF files use a global navigation transform, so the
   * list of variable names is ignored.  The only supported transform
   * for CWF files is a translation by integer offsets.
   *
   * @param variableNames the list of variable names to update (ignored).
   * @param affine the navigation transform to apply.  If null, the
   * navigation is reset to the identity.
   *
   * @throws IOException if an error occurred writing the file metadata.
   * @throws IllegalArgumentException if the affine transform is not
   * a translation.
   */
  public void updateNavigation (
    List variableNames,
    AffineTransform affine
  ) throws IOException {

    // Check affine transform
    // ----------------------
    if (affine != null) {
      if (affine.isIdentity()) return;
      else if ((affine.getType() ^ AffineTransform.TYPE_TRANSLATION) != 0)
        throw new IllegalArgumentException (
          "CWF format only supports translation transform");
    } // if  

    // TODO: This should be fixed so that if the file fails to open
    // (possibly it is write-protected) then we re-open the file as
    // read-only and throw an error.

    // Reopen file as read/write
    // -------------------------
    try { 
      CWF.close (cwid); 
      cwid = CWF.open (getSource(), CWF.CW_WRITE);
    } // try
    catch (Exception e) { 
      throw new IOException (e.getMessage()); 
    } // catch
 
    // Get current navigation transform
    // ----------------------------------
    AffineTransform currentNav = null;
    short hShift = CWF.get_attribute_short (cwid, 0, "horizontal_shift");
    short vShift = CWF.get_attribute_short (cwid, 0, "vertical_shift");
    if (hShift != 0 || vShift != 0)
    currentNav = AffineTransform.getTranslateInstance (-vShift, -hShift);

    // Reset transform
    // ---------------
    AffineTransform newNav = null;
    if (affine == null) {
      if (currentNav != null) newNav = new AffineTransform();
    } // if

    // Multiply transforms
    // -------------------
    else {
      if (currentNav == null) currentNav = new AffineTransform();
      newNav = (AffineTransform) currentNav.clone();
      newNav.preConcatenate (affine);
    } // else

    // Write new navigation transform
    // --------------------------------
    if (newNav != null) {
      hShift = (short) -Math.round (newNav.getTranslateY());
      vShift = (short) -Math.round (newNav.getTranslateX());
      CWF.put_attribute (cwid, 0, "horizontal_shift", hShift);
      CWF.put_attribute (cwid, 0, "vertical_shift", vShift);
    } // if

    // Reopen file as read only
    // ------------------------
    try { 
      CWF.close (cwid); 
      cwid = CWF.open (getSource(), CWF.CW_NOWRITE);
    } // try
    catch (Exception e) { 
      throw new IOException (e.getMessage()); 
    } // catch

  } // updateNavigation

  ////////////////////////////////////////////////////////////

} // CWFReader class

////////////////////////////////////////////////////////////////////////
