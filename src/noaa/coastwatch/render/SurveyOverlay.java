////////////////////////////////////////////////////////////////////////
/*
     FILE: SurveyOverlay.java
  PURPOSE: An overlay for survey objects.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/01
  CHANGES: 2005/03/22, PFH, added transparency handling
           2006/01/16, PFH, added check for null color
           2007/01/15, PFH, fixed inaccuracy in box survey rendering

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
import java.util.List;
import noaa.coastwatch.util.*;

/**
 * The <code>SurveyOverlay</code> class may be used to display the
 * shape of an <code>EarthDataSurvey</code> object.  For point
 * surveys, the overlay displays a crosshair centered at the point.
 * For a line survey, a line is drawn between the start and end
 * points.  For a box survey, a rectangle is drawn enclosing the
 * survey area.  For a polygon survey, the polygon itself is drawn.
 */
public class SurveyOverlay 
  extends LineOverlay {

  // Variables
  // ---------

  /** The survey displayed by this overlay. */
  private EarthDataSurvey survey;

  /** The shape used to display the survey. */
  private Shape shape;

  /** The point flag, true if this is a point survey overlay. */
  private boolean isPoint;

  ////////////////////////////////////////////////////////////

  /** Gets the survey for this overlay. */
  public EarthDataSurvey getSurvey () { return (survey); }
  
  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new survey overlay with the specified survey and color.
   * The layer number is initialized to 0, and the stroke to the
   * default <code>BasicStroke</code>.
   */
  public SurveyOverlay (
    EarthDataSurvey survey,
    Color color
  ) {

    // Initialize
    // ----------
    super (color);
    this.survey = survey;

    // Add survey element
    // ------------------
    if (survey instanceof PointSurvey) {
      GeneralPath path = new GeneralPath();
      DataLocation loc = survey.getExtents()[0];
      Point point = new Point ((int) loc.get(0), (int) loc.get(1));
      path.moveTo (point.x, point.y);
      shape = path;
      isPoint = true;
    } // if
    else {
      DataLocation[] locs = survey.getExtents();
      Point start = new Point ((int) locs[0].get(0), (int) locs[0].get(1));
      Point end = new Point ((int) locs[1].get(0), (int) locs[1].get(1));
      if (survey instanceof LineSurvey)
        shape = new Line2D.Float (start, end);
      else if (survey instanceof PolygonSurvey)
        shape = ((PolygonSurvey) survey).getShape();
      else if (survey instanceof BoxSurvey)
        shape = new Rectangle2D.Float (start.x - 0.5f, start.y - 0.5f, 
          end.x - start.x + 1, end.y - start.y + 1);
    } // else

    // Check shape
    // -----------
    if (shape == null)
      throw new IllegalArgumentException ("Unknown survey class " + 
        survey.getClass());

  } // SurveyOverlay constructor

  ////////////////////////////////////////////////////////////

  /** Returns false as this class needs no preparation. */
  protected boolean needsPrepare () { return (false); }

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // do nothing

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for null color
    // --------------------
    if (getColor() == null) return;

    // Transform draw shape
    // --------------------
    AffineTransform at = view.getTransform().getImageTransform().getAffine();
    Shape drawShape = at.createTransformedShape (shape);

    // Modify draw shape for point survey
    // ----------------------------------
    if (isPoint) {
      GeneralPath path = (GeneralPath) drawShape;
      Point2D center = path.getCurrentPoint();
      float cX = (float) center.getX();
      float cY = (float) center.getY();
      path.moveTo (cX - 5, cY);
      path.lineTo (cX + 5, cY);
      path.moveTo (cX, cY - 5);
      path.lineTo (cX, cY + 5);
    } // if

    // Draw shape
    // ----------
    g.setColor (getColorWithAlpha());
    g.setStroke (getStroke());
    g.draw (drawShape);

  } // draw

  ////////////////////////////////////////////////////////////

} // SurveyOverlay class

////////////////////////////////////////////////////////////////////////
