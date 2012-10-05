////////////////////////////////////////////////////////////////////////
/*
     FILE: PointFeatureSource.java
  PURPOSE: Supplies point feature rendering.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/22
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import jahuwaldt.plot.PlotSymbol;
import noaa.coastwatch.util.*;

/**
 * The <code>PointFeatureSource</code> class supplied and renders
 * <code>PointFeature</code> data with user-supplied plot symbols.
 */
public abstract class PointFeatureSource 
  extends AbstractFeatureSource {

  ////////////////////////////////////////////////////////////

  /**
   * Renders the selected point feature data to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   * @param symbol the symbol to use for rendering each point feature.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans,
    PointFeatureSymbol symbol
  ) {

    for (Iterator iter = iterator(); iter.hasNext(); ) {
      PointFeature feature = (PointFeature) iter.next();
      symbol.setFeature (feature);
      Point2D point = trans.transform (feature.getPoint());
      symbol.draw (g, (int) Math.round (point.getX()), 
        (int) Math.round (point.getY()));
    } // for

  } // render

  ////////////////////////////////////////////////////////////

} // PointFeatureSource class

////////////////////////////////////////////////////////////////////////
