////////////////////////////////////////////////////////////////////////
/*

     File: LineFeatureSource.java
   Author: Peter Hollemans
     Date: 2002/09/12

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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.awt.Graphics2D;
import java.util.Iterator;
import noaa.coastwatch.render.feature.AbstractFeatureSource;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.feature.LineFeature;

import java.util.logging.Logger;

/**
 * The <code>LineFeatureSource</code> class supplies methods that read
 * or generate vector-specified earth data such as coast lines,
 * political boundaries, bathymetry, and so on.  A source must have
 * the capability to select data from the data source and supply it as
 * a list of {@link LineFeature} objects.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class LineFeatureSource 
  extends AbstractFeatureSource {

  private static final Logger LOGGER = Logger.getLogger (LineFeatureSource.class.getName());

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected line feature data to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the earth image transform for converting Earth
   * locations to image points.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    int featureCount = 0;
    
    for (Iterator iter = iterator(); iter.hasNext(); ) {
      LineFeature line = (LineFeature) iter.next();
      line.render (g, trans);
      featureCount++;
    } // for

    LOGGER.fine ("Rendered " + featureCount + " line features");

  } // render

  ////////////////////////////////////////////////////////////

} // LineFeatureSource class

////////////////////////////////////////////////////////////////////////
