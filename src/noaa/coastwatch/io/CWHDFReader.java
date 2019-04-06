////////////////////////////////////////////////////////////////////////
/*

     File: CWHDFReader.java
   Author: Peter Hollemans
     Date: 2002/04/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.CachedGrid;
import noaa.coastwatch.io.HDFReader;
import noaa.coastwatch.io.HDFWriter;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Line;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.SensorScanProjection;
import noaa.coastwatch.util.trans.SensorScanProjectionFactory;
import noaa.coastwatch.util.trans.SwathProjection;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A CWHDF reader is an earth data reader that reads CoastWatch
 * HDF format files using the HDF library class.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class CWHDFReader
  extends HDFReader {

  private static final Logger LOGGER = Logger.getLogger (CWHDFReader.class.getName());

  // Constants
  // ---------

  /** Swath maximum polynomial size in kilometers. */
  public static final double SWATH_POLY_SIZE = 100.0;

  /** The data format description. */
  private static final String DATA_FORMAT = "CoastWatch HDF";

  /** Number of milliseconds in a day. */
  private static final long MSEC_PER_DAY = (1000L * 3600L * 24L);

  /** List of extra map projection metadata. */
  private static final List MAP_METADATA = Arrays.asList (
    new String[] {"region_code", "region_name"});

  /** List of extra swath projection metadata. */
  private static final List SWATH_METADATA = Arrays.asList (
    new String[] {"station_code", "station_name"});

  ////////////////////////////////////////////////////////////

  /** Gets the data format description. */
  public String getDataFormat () { 

    return (DATA_FORMAT + " version " + getMetaVersion (getSDID())); 

  } // getDataFormat

  ////////////////////////////////////////////////////////////

  /**
   * Gets the CoastWatch HDF metadata version.
   *
   * @param sdid the HDF scientific dataset ID.
   *
   * @return the metadata version.  If no metadata attribute can be found,
   * the metadata version is assumed to be 2.3.
   */
  public static double getMetaVersion (  
    int sdid
  ) {

    // Get metadata attribute
    // ----------------------
    try {
      String att = (String) getAttribute (sdid, "cwhdf_version");
      return (Double.parseDouble (att));
    } // try
    catch (Exception e) {
      return (2.3);
    } // catch

  } // getMetaVersion

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a CWHDF reader from the specified writer.
   * 
   * @param writer the writer to use for reading.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred reading the file metadata.
   * @throws NoninvertibleTransformException if the earth transform object
   * could not be initialized.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */  
  public CWHDFReader (
    CWHDFWriter writer
  ) throws HDFException, IOException, NoninvertibleTransformException,
    ClassNotFoundException {

    super (writer);

  } // CWHDFReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a CWHDF reader from the specified file.
   * 
   * @param file the file name to read.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws IOException if an error occurred reading the file metadata.
   * @throws NoninvertibleTransformException if the earth transform object
   * could not be initialized.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */  
  public CWHDFReader (
    String file
  ) throws HDFException, IOException, NoninvertibleTransformException,
    ClassNotFoundException {

    super (file);

  } // CWHDFReader constructor

  ////////////////////////////////////////////////////////////
  
  /** 
   * Returns true so that the parent is instructed to read all global
   * and variable attributes.
   */
  protected boolean readAllMetadata () { return (true); }

  ////////////////////////////////////////////////////////////
  
  protected EarthDataInfo getGlobalInfo () throws HDFException, 
    IOException, NoninvertibleTransformException, ClassNotFoundException {

    // Get simple attributes
    // ---------------------
    String sat = null;
    try { sat = (String) getAttribute (sdid, "satellite"); }
    catch (HDFException e) { }
    String sensor = null;
    try { sensor = (String) getAttribute (sdid, "sensor"); }
    catch (HDFException e) { }
    String source = null;
    try { source = (String) getAttribute (sdid, "data_source"); }
    catch (HDFException e) { }
    String origin;
    try { origin = (String) getAttribute (sdid, "origin"); }
    catch (HDFException e) { origin = "Unknown"; }
    String history;
    try { history = (String) getAttribute (sdid, "history"); }
    catch (HDFException e) { history = ""; }

    // Get time period list and transform
    // ----------------------------------
    List periodList = getPeriodList();
    EarthTransform transform = getTransform();

    // Create info object
    // ------------------
    EarthDataInfo info;
    if (sat == null && sensor == null && source == null)
      source = "Unknown";
    if (source != null) {
      info = new EarthDataInfo (source, periodList, transform, 
        origin, history);
    } // if
    else {
      if (sat == null) sat = "Unknown";
      if (sensor == null) sensor = "Unknown";
      info = new SatelliteDataInfo (sat, sensor, periodList, transform, 
        origin, history);
    } // else

    return (info);

  } // getGlobalInfo

  ////////////////////////////////////////////////////////////

  /**
   * Reads the time period list.  The date and time metadata in the
   * HDF file are converted into the equivalent list of TimePeriod
   * objects.
   *
   * @return a list of TimePeriod objects from data in the HDF file
   * data.
   *
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   *
   * @see noaa.coastwatch.util.TimePeriod
   */
  private List getPeriodList () 
    throws HDFException, ClassNotFoundException {

    // Read data
    // ---------
    int[] passDateArray = (int[]) getAttributeAsArray (sdid, "pass_date");
    double[] startTimeArray = (double[]) getAttributeAsArray (sdid, 
      "start_time");
    int periods = passDateArray.length;
    double[] extentArray;
    try { 
      extentArray = (double[]) getAttributeAsArray (sdid, "temporal_extent"); 
    } // try
    catch (HDFException e) {
      extentArray = new double[periods];
    } // catch
    if (passDateArray.length != startTimeArray.length ||
      startTimeArray.length != extentArray.length)
      throw new RuntimeException ("Date, time, and extent attributes have " + 
        "different lengths");

    // Create period list
    // ------------------
    List periodList = new ArrayList();
    for (int i = 0; i < periods; i++) {
      long msec = (long) passDateArray[i] * MSEC_PER_DAY;
      msec += (long) (startTimeArray[i] * 1000L);
      periodList.add (new TimePeriod (new Date (msec), 
        (long) extentArray[i] * 1000L));
    } // for

    return (periodList);

  } // getPeriodList

  ////////////////////////////////////////////////////////////

  /**
   * Reads the earth transform information.  The projection metadata
   * in the HDF file is converted into the equivalent {@link
   * EarthTransform}.
   *
   * @return an earth transform based on the HDF file data.
   *
   * @throws HDFException if there were errors reading the HDF metadata.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   * @throws NoninvertibleTransformException if the map projection object
   * could not be initialized.
   */
  private EarthTransform getTransform () 
    throws HDFException, ClassNotFoundException,
    NoninvertibleTransformException {

    // Get projection type
    // -------------------
    double version = getMetaVersion (sdid);
    String type;
    if (version >= 3)
      type = (String) getAttribute (sdid, "projection_type");
    else
      type = MapProjection.DESCRIPTION;

    // Read map projection
    // -------------------
    if (type.equals (MapProjection.DESCRIPTION)) {

      // Get GCTP parameters
      // -------------------
      int system = ((Integer) getAttribute (sdid, "gctp_sys")).intValue();
      int zone = ((Integer) getAttribute (sdid, "gctp_zone")).intValue();
      double[] parameters = (double[]) getAttribute (sdid, "gctp_parm");
      int spheroid = ((Integer) getAttribute (sdid, 
        "gctp_datum")).intValue();

      // Get affine transform
      // --------------------
      AffineTransform affine;
      double[] matrix = (double[]) getAttribute (sdid, "et_affine");
      if (version >= 3)
        affine = new AffineTransform (matrix);
      else {
        double[] newMatrix = new double[6];
        newMatrix[0] = matrix[1];
        newMatrix[1] = matrix[3];
        newMatrix[2] = matrix[0];
        newMatrix[3] = matrix[2];
        newMatrix[4] = matrix[0] + matrix[1] + matrix[4];
        newMatrix[5] = matrix[2] + matrix[3] + matrix[5];
        affine = new AffineTransform (newMatrix);
      } // else

      // Get dimensions
      // --------------
      int rows = ((Integer) getAttribute (sdid, "rows")).intValue();
      int cols = ((Integer) getAttribute (sdid, "cols")).intValue();

      // Create map projection
      // ---------------------
      EarthTransform2D trans = MapProjectionFactory.getInstance().create (
        system, zone, parameters, spheroid, new int[] {rows, cols}, affine);

      // Check for point mode data
      // -------------------------
      String rasterType;
      try { rasterType = (String) getAttribute (sdid, "raster_type"); }
      catch (HDFException e) { rasterType = null; }
      if (rasterType != null && rasterType.equals ("RasterPixelIsPoint")) {
        DataVariable lat = null, lon = null;
        try {
          lat = getVariable ("latitude");
          lon = getVariable ("longitude");
          DataProjection pointTrans = new DataProjection (lat, lon);
          trans.setPointTransform (pointTrans);
        } // try
        catch (Exception e) { }
      } // if
      
      // Attach metadata
      // ---------------
      getAttributes (sdid, MAP_METADATA, trans.getMetadataMap());

      return (trans);

    } // if

    // Read swath projection
    // ---------------------
    else if (type.equals (SwathProjection.DESCRIPTION)) {

      // Try reading lat/lon estimators
      // ------------------------------
      EarthTransform trans = null;
      try {

        // Read partition structure info
        // ----------------------------
        Line structure = (Line) getVariable ("swath_struct");
        BitSet bits = SwathProjection.toBits ((byte[])structure.getData());

        // Read partition boundaries
        // -------------------------
        Line bounds = (Line) getVariable ("swath_bounds");
        double[] boundsData = (double[]) bounds.getData ();
        int nBounds = boundsData.length / 2;
        List boundsList = new ArrayList();
        for (int i = 0; i < nBounds; i++) {
          double[] thisBound = new double[] {boundsData[i*2], 
            boundsData[i*2+1]};
          boundsList.add (thisBound);
        } // for

        // Read lat/lon estimators
        // -----------------------
        Line latEst = (Line) getVariable ("swath_lat");
        Line lonEst = (Line) getVariable ("swath_lon");
        double[] latData = (double[]) latEst.getData ();
        double[] lonData = (double[]) lonEst.getData ();
        int nParts = latData.length / 9;
        List latList = new ArrayList();
        List lonList = new ArrayList();
        for (int i = 0; i < nParts; i++) {
          double[] latPartCoefs = new double[9];
          double[] lonPartCoefs = new double[9];
          System.arraycopy (latData, i*9, latPartCoefs, 0, 9);
          System.arraycopy (lonData, i*9, lonPartCoefs, 0, 9);
          latList.add (latPartCoefs);
          lonList.add (lonPartCoefs);
        } // for

        // Read swath dimensions
        // ---------------------
        int rows = ((Integer) getAttribute (sdid, "rows")).intValue ();
        int cols = ((Integer) getAttribute (sdid, "cols")).intValue ();

        // Return swath projection
        // -----------------------
        trans = new SwathProjection (new Object[] {bits, boundsList,
          latList, lonList, new int[] {rows, cols}});

      } // try
      catch (Exception e) { }

      // Try reading lat/lon data explicitly
      // -----------------------------------
      /*
       * A note to the developer: Reading the lat/lon data in this way
       * means that is is best if the data is not chunked and/or
       * compressed in square 2D tiles.  If the data *is* chunked
       * and/or compressed in square tiles, the call to setTileDims()
       * below has no effect and every time a row of lat/lon data is
       * read, the entire chunk will be read and/or decompressed.
       * That could cause a major slow-down in reading swath data.  So
       * the best options for a earth locations in a swath file are:
       * 
       * 1) Use polynomial estimators for earth locations.
       * 2) Use explicit lat/lon data but do *not* chunk or compress
       *    the lat/lon variables in the HDF file.
       * 3) If chunking or compression are desired, use a chunk size
       *    of [1, cols] so that reading earth locations is efficient.
       */
      if (trans == null) {
        DataVariable lat = null, lon = null;
        try {
          lat = getVariable ("latitude");
          int cols = lat.getDimensions()[Grid.COLS];
          ((CachedGrid) lat).setTileDims (new int[] {1, cols});
          lon = getVariable ("longitude");
          ((CachedGrid) lon).setTileDims (new int[] {1, cols});
          if (dataProjection) {
            trans = new DataProjection (lat, lon);
          } // if
          else {
            trans = new SwathProjection (lat, lon, SWATH_POLY_SIZE, 
              new int[] {cols, cols});
          } // else
        } // try
        catch (Exception e) {
          LOGGER.log (Level.WARNING, "Problems encountered using earth location data", e);
          if (lat != null && lon != null) {
            LOGGER.warning ("No (lat,lon) --> (row,col) transform available");
            trans = new DataProjection (lat, lon);
          } // if
        } // catch
      } // if

      // Attach metadata
      // ---------------
      getAttributes (sdid, SWATH_METADATA, trans.getMetadataMap());

      return (trans);

    } // else if

    // Read sensor scan projection
    // ---------------------------
    else if (type.equals (SensorScanProjection.DESCRIPTION)) {
      
      // Get sensor scan parameters
      // --------------------------
      int sensorCode = ((Integer) getAttribute (sdid, 
        "sensor_code")).intValue();
      double[] parameters = (double[]) getAttribute (sdid, "sensor_parm");

      // Get dimensions
      // --------------
      int rows = ((Integer) getAttribute (sdid, "rows")).intValue();
      int cols = ((Integer) getAttribute (sdid, "cols")).intValue();

      // Create projection
      // -----------------
      EarthTransform trans = SensorScanProjectionFactory.create (
        sensorCode, parameters, new int[] {rows, cols});
      return (trans);

    } // else if

    // Unknown projection
    // ------------------
    else return (null);

  } // getTransform

  ////////////////////////////////////////////////////////////

  public boolean canUpdateNavigation () { return (true); }

  ////////////////////////////////////////////////////////////

  /**
   * Updates the navigation transform for the specified list of
   * variables.
   *
   * @param variableNames the list of variable names to update.
   * @param affine the navigation transform to apply.  If null, the
   * navigation is reset to the identity.
   *
   * @throws IOException if an error occurred writing the file metadata.
   */
  public void updateNavigation (
    List variableNames,
    AffineTransform affine
  ) throws IOException {

    try {

      // Check metadata version
      // ----------------------
      double version = getMetaVersion (sdid);
      if (version < 3) {
        LOGGER.warning ("Writing navigation transform to file with metadata version " + version);
      } // if

      // TODO: This should be fixed so that if the file fails to open
      // (possibly it is write-protected) then we re-open the file as
      // read-only and throw an error.

      // Reopen file as read/write
      // -------------------------
      if (!HDFLib.getInstance().SDend (sdid)) 
        throw new HDFException ("Failed to end access to HDF file");
      sdid = HDFLib.getInstance().SDstart (getSource(), HDFConstants.DFACC_WRITE);

      // Loop over each variable
      // -----------------------
      for (Iterator iter = variableNames.iterator(); iter.hasNext(); ) {
        String var = (String) iter.next();

        // Access variable
        // ---------------
        int index = HDFLib.getInstance().SDnametoindex (sdid, var);
        if (index < 0)
          throw new HDFException ("Cannot access variable " + var);
        int sdsid = HDFLib.getInstance().SDselect (sdid, index);
        if (sdsid < 0)
          throw new HDFException ("Cannot access variable at index " + index);

        // Get variable info
        // -----------------
        String[] varName = new String[] {""};
        int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
        int varInfo[] = new int[3];
        if (!HDFLib.getInstance().SDgetinfo (sdsid, varName, varDims, varInfo))
          throw new HDFException ("Cannot get variable info for " + var);
        int rank = varInfo[0];
        if (rank != 2) {
          throw new IOException ("Navigation transforms can only be set " +
            "for 2D variables");
        } // if
          
        // Get current navigation transform
        // ----------------------------------
        AffineTransform currentNav = null;
        try {
          double[] matrix  = (double[]) HDFReader.getAttribute (sdsid, 
            "nav_affine");
          currentNav = new AffineTransform (matrix);
        } // try
        catch (HDFException e) { }

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
          double[] matrix = new double[6];
          newNav.getMatrix (matrix); 
          HDFWriter.setAttribute (sdsid, "nav_affine", matrix);
        } // if

        // End access
        // ----------
        HDFLib.getInstance().SDendaccess (sdsid);

      } // for

      // Reopen file as read only
      // ------------------------
      if (!HDFLib.getInstance().SDend (sdid)) 
        throw new HDFException ("Failed to end access to HDF file");
      sdid = HDFLib.getInstance().SDstart (getSource(), HDFConstants.DFACC_READ);

    } // try
    catch (HDFException e1) { 
      throw new IOException (e1.getMessage()); 
    } // catch
    catch (ClassNotFoundException e2) { 
      throw new IOException (e2.getMessage()); 
    } // catch

  } // updateNavigation

  ////////////////////////////////////////////////////////////

} // CWHDFReader class

////////////////////////////////////////////////////////////////////////
