/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.render.feature;

import java.io.File;
import java.io.IOException;

import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.feature.ESRIShapefileReader;
import noaa.coastwatch.render.feature.LatLonLineReader;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The <code>ShapeOverlayFactory</code> class reads various shape file
 * formats and returns the shapes as an 
 * {@link noaa.coastwatch.render.EarthDataOverlay} object.</p>
 * 
 * @see noaa.coastwatch.render.feature.ESRIShapefileReader
 * @see noaa.coastwatch.render.feature.LatLonLineReader
 *
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class ShapeOverlayFactory {

  private static final Logger LOGGER = Logger.getLogger (ShapeOverlayFactory.class.getName());

  private static ShapeOverlayFactory instance;

  ////////////////////////////////////////////////////////////

  protected ShapeOverlayFactory () {}

  ////////////////////////////////////////////////////////////

  public static ShapeOverlayFactory getInstance() { 

	  if (instance == null) instance = new ShapeOverlayFactory(); 
	  return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new overlay from data in a shape data file.
   * 
   * @param filename the filename of the shape data file to read.
   * 
   * @return the overlay of shape data.
   */
  public EarthDataOverlay create (
    String filename
  ) throws IOException {

  	EarthDataOverlay overlay = null;
  	var file = new File (filename);

  	// First try reading as an ESRI shapefile
    try {
      var shapeReader = new ESRIShapefileReader (file.toURI().toURL());
      overlay = shapeReader.getOverlay();
    } // try
    catch (IOException e) { 
      LOGGER.log (Level.FINE, "Error opening ESRI shapefile", e);
    } // catch

    // If that didn't work, try parsing as a set of lat/lon points
    if (overlay == null) {    
      try {
        var latLonReader = new LatLonLineReader (file.getAbsolutePath());
        overlay = latLonReader.getOverlay();
      } // try
      catch (IOException e) { 
        LOGGER.log (Level.FINE, "Error parsing as lat/lon point file", e);
      } // catch
    } // if
      
    if (overlay == null) throw new IOException ("Unsupported shape data file format");

		return (overlay);

  } // create

  ////////////////////////////////////////////////////////////

} // ShapeOverlayFactory
