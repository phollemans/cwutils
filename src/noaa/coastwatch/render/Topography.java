/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2023 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.render;

import java.io.IOException;

import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.Grid;

/**
 * The <code>Topography</code> class holds topography data, useful for 
 * classes that need topography data for contouring, atmospheric correction, 
 * etc.  Note that the topography includes bathymetry.
 * 
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class Topography {

  /** The topography data files. */
  private static final String TOPOGRAPHY_FILE_LOW = "etopo5.hdf";
  private static final String TOPOGRAPHY_FILE_HIGH = "etopo1.hdf";

  /** The topography variable name. */
  private static final String TOPOGRAPHY_VARIABLE = "elevation";

  /** The grid to use for topography data. */
  private Grid elev;

  /** The Earth transform that matches the topography grid. */
  private EarthTransform trans;

  ////////////////////////////////////////////////////////////

  protected Topography () {}

  ////////////////////////////////////////////////////////////

  /**
   * Gets an instance of the topography class.
   *
   * @return the topography object.
   */
  public static Topography getInstance () throws IOException {

    // Get the file path.  We get the best topography available, by
    // trying the high resolution file first and then the low resolution.
    String path = null;

    try { path = IOServices.getFilePath (Topography.class, TOPOGRAPHY_FILE_HIGH); }
    catch (IOException e) { }

    if (path == null) {
      try { path = IOServices.getFilePath (Topography.class, TOPOGRAPHY_FILE_LOW); }
      catch (IOException e) { }
    } // if

    if (path == null) {
      throw new IOException ("No topography data file found in resources");
    } // if

    // Now get the grid and transform for the topography data
    var reader = EarthDataReaderFactory.create (path);
    var topo = new Topography();
    topo.elev = (Grid) reader.getVariable (TOPOGRAPHY_VARIABLE);
    topo.trans = reader.getInfo().getTransform();

    return (topo);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the elevation grid.
   * 
   * @return the elevation grid.
   */
  public Grid getElevation() { return (elev); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the earth transform for the elevation grid.
   * 
   * @return the earh transform.
   */
  public EarthTransform getTransform() { return (trans); }

  ////////////////////////////////////////////////////////////

} // Topography
