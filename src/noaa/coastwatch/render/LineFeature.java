////////////////////////////////////////////////////////////////////////
/*
     FILE: LineFeature.java
  PURPOSE: A class for holding vectors of Eath location data.
   AUTHOR: Peter Hollemans
     DATE: 2002/10/11
  CHANGES: 2002/11/29, PFH, optimized render for same transform
           2002/11/30, PFH, added filter method
           2002/12/10, PFH, corrected zero length bug in transform
           2002/12/11, PFH, removed clip bounds from transform
           2002/12/12, PFH, modified transform to handle projection 
             discontinuities
           2003/01/15, PFH, added fast rendering mode
           2003/05/09, PFH, changed to protected variables
           2003/05/11, PFH, added getPath
           2003/05/22, PFH, added isDisjoint
           2004/03/23, PFH, modified to extend ArrayList
           2005/04/29, PFH, modified to extend AbstractFeature
           2005/05/27, PFH
           - changed disjoint to discontinuous
           - added getFastMode()
           - changed isDiscontinuous() to always return false in fast mode

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.render.AbstractFeature;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/**
 * The <code>LineFeature</code> class holds a list of Earth location
 * data.  Various methods are provided for converting from lines of
 * Earth location data to projected image data.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LineFeature
  extends AbstractFeature {

  // Variables
  // ---------
  /** The last transformed path. */
  protected GeneralPath lastPath;

  /** The last Earth image transform. */
  protected EarthImageTransform lastTrans;

  /** The last discontinuous status. */
  protected boolean lastDiscontinuous = false;

  /** The fast mode flag. */
  protected static boolean fastMode = false;

  /** The list of geographic points. */
  protected List points = new ArrayList();

  ////////////////////////////////////////////////////////////

  /** 
   * Returns the discontinuous flag.  If during the last transformation
   * from a set of Earth locations to a general path under an Earth
   * image transform, the vector was discontinuous at some point due
   * to an invalid transformation, then the path is discontinuous.  If the
   * fast rendering mode is on, or the vector has never been
   * transformed to a general path, then the discontinuous flag is
   * false.
   */
  public boolean isDiscontinuous () { 

    if (fastMode) return (false);
    else return (lastDiscontinuous); 

  } // isDiscontinuous

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the fast rendering mode flag.
   *
   * @return the fast mode flag, true for fast rendering or false for
   * not.
   */
  public static boolean getFastMode () { return (fastMode); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the fast rendering mode flag.  Under fast rendering mode,
   * the vector is not checked for discontinuities.  By default, fast
   * rendering is off.
   *
   * @param flag the fast mode, true for fast rendering.
   */
  public static void setFastMode (boolean flag) { fastMode = flag; }

  ////////////////////////////////////////////////////////////

  /** Creates a new empty line feature with no attributes. */
  public LineFeature () { lastPath = null; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty line feature with attributes. 
   * 
   * @param attributeArray the array of feature attributes.
   */
  public LineFeature (
    Object[] attributeArray
  ) {

    setAttributes (attributeArray);

  } // LineFeature constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Filters this feature based on an Earth area.  If the line
   * segments leave and re-enter the specified Earth area, they are
   * split into multiple features.  The resulting features are
   * returned.
   *
   * @param area the Earth area to use for filtering Earth locations.
   *
   * @return the list of features resulting from the filter operation.
   */
  public List filter (
    EarthArea area
  ) {

    // Initialize
    // ----------
    ArrayList filteredList = new ArrayList();
    LineFeature filteredVector = null;
    EarthLocation p1 = (EarthLocation) this.get(0);
    boolean containsP1 = area.contains (p1);
    EarthLocation p2 = null;
    boolean containsP2;
    boolean discontinuous = true;
    int points = size();

    // Loop over each point
    // --------------------
    for (int i = 1; i < points; i++) {
      p2 = (EarthLocation) this.get(i);
      containsP2 = area.contains (p2);
      boolean segmentAdded = false;

      // Add segment to filtered vector
      // ------------------------------
      if (containsP1 || containsP2) {
        if (discontinuous) {
          if (filteredVector != null) filteredList.add (filteredVector);
          filteredVector = new LineFeature();
          filteredVector.add (p1);
        } // if
        filteredVector.add (p2);
        segmentAdded = true;
      } // if

      // Check for discontinuous path
      // ----------------------------
      p1 = p2;
      containsP1 = containsP2;
      discontinuous = !segmentAdded;

    } // for

    // Return filtered list
    // --------------------
    if (filteredVector != null) filteredList.add (filteredVector);
    return (filteredList);

  } // filter

  ////////////////////////////////////////////////////////////
  
  /** 
   * Transforms this feature to a general path.  The Earth image
   * transform is used to eliminate any line segments which do not
   * transform to valid image points.
   *
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   *
   * @return the general path that follows the feature data.
   */
  public GeneralPath transform (
    EarthImageTransform trans
  ) {

    // Initialize path
    // ---------------
    GeneralPath path = new GeneralPath();

    // Initialize points
    // -----------------
    int points = size();
    EarthLocation e1 = null;
    Point2D p1 = null;    
    if (points != 0) {
      e1 = (EarthLocation) get(0);
      p1 = trans.transform (e1);
    } // if
    EarthLocation e2 = null;
    Point2D p2 = null;

    // Initialize loop
    // ---------------
    boolean discontinuous = true;
    int moveToCount = 0;

    // Loop over each segment
    // ----------------------
    for (int i = 1; i < points; i++) {
      e2 = (EarthLocation) get(i);
      p2 = trans.transform (e2);

      // Add segment to path
      // -------------------
      boolean jumped;
      jumped = (fastMode ? false : trans.isDiscontinuous (e1, e2, p1, p2));
      if (p1 != null && p2 != null && !jumped) {
        if (discontinuous) {
          path.moveTo ((float) p1.getX(), (float) p1.getY());          
          moveToCount++;
        } // if
        path.lineTo ((float) p2.getX(), (float) p2.getY());
        discontinuous = false;
      } // if

      // Record invalid segment
      // ----------------------
      else {
        discontinuous = true;      
      } // else

      // Copy values
      // -----------
      e1 = e2;
      p1 = p2;

    } // for

    // Record discontinuous status
    // ---------------------------
    lastDiscontinuous = (moveToCount > 1);

    return (path);

  } // transform

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the general path for this feature under the specified
   * transform.
   *
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   *
   * @return the general path corresponding to this feature.
   */
  public GeneralPath getPath (
    EarthImageTransform trans
  ) {  

    // Create path
    // -----------
    GeneralPath path;
    if (lastPath == null || lastTrans != trans) {
      path = transform (trans);
      lastPath = path;
      lastTrans = trans;
    } // if

    // Get saved path
    // --------------
    else path = lastPath;

    return (path);

  } // getPath

  ////////////////////////////////////////////////////////////

  /**
   * Renders this feature to a graphics context.
   *
   * @param g the graphics context for drawing.
   * @param trans the Earth image transform for converting Earth
   * locations to image points.
   */
  public void render (
    Graphics2D g,
    EarthImageTransform trans
  ) {

    GeneralPath path = getPath (trans);
    g.draw (path);

  } // render

  ////////////////////////////////////////////////////////////

} // LineFeature class

////////////////////////////////////////////////////////////////////////
