////////////////////////////////////////////////////////////////////////
/*

     File: BinnedGSHHSReaderFactory.java
   Author: Peter Hollemans
     Date: 2006/06/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import noaa.coastwatch.render.feature.BinnedGSHHSLineReader;
import noaa.coastwatch.render.feature.BinnedGSHHSReader;
import noaa.coastwatch.render.feature.HDFGSHHSLineReader;
import noaa.coastwatch.render.feature.HDFGSHHSReader;
import noaa.coastwatch.render.feature.OpendapGSHHSLineReader;
import noaa.coastwatch.render.feature.OpendapGSHHSReader;

/**
 * A <code>BinnedGSHHSReaderFactory</code> creates instances of
 * {@link BinnedGSHHSReader} and {@link BinnedGSHHSLineReader}
 * objects based on a resolution requirement.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class BinnedGSHHSReaderFactory {

  // Constants
  // ---------

  /** High resolution (0.2 km) database level. */
  public final static int HIGH = 0;

  /** Intermediate resolution (1.0 km) database level. */
  public final static int INTERMEDIATE = 1;

  /** Low resolution (5.0 km) database level. */
  public final static int LOW = 2;

  /** Crude resolution (25 km) database level. */
  public final static int CRUDE = 3;

  /** The coastline database type. */
  public final static int COAST = 0;

  /** The border database type. */
  public final static int BORDER = 1;

  /** The riever database type. */
  public final static int RIVER = 2;

  // Variables
  // ---------

  /** The instance of this factory. */
  private static BinnedGSHHSReaderFactory instance;

  /** The cache of readers by server and database name. */
  private static Map readerCache = new HashMap();

  /** The server path to use or null for local files. */
  private String serverPath;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of this factory the uses local HDF files for
   * reader data.
   */
  public static BinnedGSHHSReaderFactory getInstance () {

    if (instance == null) instance = new BinnedGSHHSReaderFactory();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of this factory the uses the specified
   * OPeNDAP server path for reader data.
   *
   * @param serverPath the full path to the OPeNDAP server and
   * subdirectory, for example "http://server.com/data".
   */
  public static BinnedGSHHSReaderFactory getInstance (
    String serverPath
  ) {

    return (new BinnedGSHHSReaderFactory (serverPath));

  } // getInstance

  ////////////////////////////////////////////////////////////

  private BinnedGSHHSReaderFactory () { }

  ////////////////////////////////////////////////////////////

  private BinnedGSHHSReaderFactory (String path) { this.serverPath = path; }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the reader database resolution level.
   * 
   * @param resolution the pixel resolution in kilometers.  The
   * resolution determines the tolerance level used to decimate
   * polygons from the full resolution database.  Resolution
   * values should reflect the desired accuracy of line
   * rendering.  For example, if the lines are to be rendered on
   * an image where each pixel measures 5 km across, the lines
   * (and possibly polygons) need not include features any
   * smaller than 5 km.  The resolution is used to determine the
   * appropriate database name.
   *
   * @return the resolution level: <code>HIGH</code>,
   * <code>INTERMEDIATE</code>, <code>LOW</code>, or
   * <code>CRUDE</code>.
   */
  public static int getDatabaseLevel (
    double resolution        
  ) {

    // Choose most appropriate database
    // --------------------------------
    double resolutions[] = {0.2, 1, 5, 25};
    int index = -1;
    double minDiff = Double.POSITIVE_INFINITY;
    for (int i = 0; i < resolutions.length; i++) {
      double diff = Math.abs (resolutions[i] - resolution);
      if (diff < minDiff) { minDiff = diff; index = i; }
    } // for
    if (index == -1) return (CRUDE);
    else return (index);

  } // getDatabaseLevel

  ////////////////////////////////////////////////////////////

  /**
   * Gets a database name using its type and resolution level.
   *
   * @param type the database type: <code>COAST</code>,
   * <code>BORDER</code>, or <code>RIVER</code>.
   * @param level the database resolution level:
   * <code>HIGH</code>, <code>INTERMEDIATE</code>,
   * <code>LOW</code>, or <code>CRUDE</code>.
   *
   * @return the name of the matching database.
   */
  public static String getDatabaseName (
    int type,
    int level
  ) {

    String typeString;
    switch (type) {
    case COAST: typeString = "GSHHS"; break;
    case BORDER: typeString = "border"; break;
    case RIVER: typeString = "river"; break;
    default: throw new IllegalArgumentException ("Type = " + type);
    } // switch

    String levelString;
    switch (level) {
    case HIGH: levelString = "h"; break;
    case INTERMEDIATE: levelString = "i"; break;
    case LOW: levelString = "l"; break;
    case CRUDE: levelString = "c"; break;
    default: throw new IllegalArgumentException ("Level = " + level);
    } // switch

    return ("binned_" + typeString + "_" + levelString + ".hdf");

  } // getDatabaseName

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of the {@link BinnedGSHHSReader} class that
   * uses the specified database.  Only databases of type
   * <code>COAST</code> are allowed.
   *
   * @param name the database name as returned from {@link
   * #getDatabaseName}.
   *
   * @return the reader object.
   */
  public BinnedGSHHSReader getPolygonReader (
    String name
  ) throws IOException {

    // Get cached reader for network instance
    // --------------------------------------
    BinnedGSHHSReader reader;
    if (serverPath != null) {
      String cacheKey = serverPath + "/" + name;
      reader = (BinnedGSHHSReader) readerCache.get (cacheKey);
      if (reader == null) {
        reader = new OpendapGSHHSReader (serverPath, name);
        readerCache.put (cacheKey, reader);
      } // if
    } // if

    // Get new reader for local instance
    // ---------------------------------
    else {
      reader = new HDFGSHHSReader (name);
    } // if
      
    return (reader);

  } // getPolygonReader

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an instance of a {@link BinnedGSHHSLineReader} object
   * that uses the specified database.  Only databases of type
   * <code>BORDER</code> and <code>RIVER</code> are allowed.
   *
   * @param name the database name as returned from {@link
   * #getDatabaseName}.
   *
   * @return the reader object.
   */
  public BinnedGSHHSLineReader getLineReader (
    String name
  ) throws IOException {

    // Get cached reader for network instance
    // --------------------------------------
    BinnedGSHHSLineReader reader;
    if (serverPath != null) {
      String cacheKey = serverPath + "/" + name;
      reader = (BinnedGSHHSLineReader) readerCache.get (cacheKey);
      if (reader == null) {
        reader = new OpendapGSHHSLineReader (serverPath, name);
        readerCache.put (cacheKey, reader);
      } // if
    } // if

    // Get new reader for local instance
    // ---------------------------------
    else {
      reader = new HDFGSHHSLineReader (name);
    } // if
      
    return (reader);

  } // getLineReader

  ////////////////////////////////////////////////////////////

} // BinnedGSHHSReaderFactory class

////////////////////////////////////////////////////////////////////////
