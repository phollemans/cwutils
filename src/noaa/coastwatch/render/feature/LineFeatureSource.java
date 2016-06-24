////////////////////////////////////////////////////////////////////////
/*
     FILE: LineFeatureSource.java
  PURPOSE: An abstract class for line feature data source methods.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/12
  CHANGES: 2002/10/11, PFH, moved to render package and made abstract
           2002/12/06, PFH, added getArea
           2003/12/10, PFH, changed class name from LineFeatureReader
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2005/05/03, PFH, modified to extend AbstractFeatureSource

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

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

/**
 * The <code>LineFeatureSource</code> class supplies methods that read
 * or generate vector-specified Earth data such as coast lines,
 * political boundaries, bathymetry, and so on.  A source must have
 * the capability to select data from the data source and supply it as
 * a list of {@link LineFeature} objects.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class LineFeatureSource 
  extends AbstractFeatureSource {

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected line feature data to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    for (Iterator iter = iterator(); iter.hasNext(); ) 
      ((LineFeature) iter.next()).render (g, trans);

  } // render

  ////////////////////////////////////////////////////////////

} // LineFeatureSource class

////////////////////////////////////////////////////////////////////////
