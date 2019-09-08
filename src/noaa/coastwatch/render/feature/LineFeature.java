////////////////////////////////////////////////////////////////////////
/*

     File: LineFeature.java
   Author: Peter Hollemans
     Date: 2002/10/11

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
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import noaa.coastwatch.render.feature.AbstractFeature;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.PathTransformer;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;

/**
 * The <code>LineFeature</code> class holds a list of earth location
 * data.  Various methods are provided for converting from lines of
 * earth location data to projected image data.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class LineFeature
  extends AbstractFeature {

  private static final Logger LOGGER = Logger.getLogger (LineFeature.class.getName());

  // Variables
  // ---------
  
  /** The last transformed path. */
  protected GeneralPath lastPath;

  /** The last earth image transform. */
  protected EarthImageTransform lastTrans;

  /** The last discontinuous status. */
  protected boolean lastDiscontinuous = false;

  /** The fast mode flag. */
  protected static boolean fastMode = false;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the discontinuous flag from the last call to {@link #getPath} or
   * {@link #transform}.
   *
   * @return the discontinuous flag, true if this line feature was discontinuous
   * during the last transformation from the set of {@link EarthLocation}
   * points to a {@link java.awt.geom.GeneralPath} or false if not.  When fast
   * rendering mode is on (see {@link #setFastMode}), this method will always
   * return false.  If this line feature has never been transformed, this
   * method will return false.
   */
  public boolean isDiscontinuous () { 

    if (fastMode) return (false);
    else return (lastDiscontinuous); 

  } // isDiscontinuous

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the fast rendering mode flag.
   *
   * @return the fast mode flag, true if currently performing fast rendering
   * or false if not.
   *
   * @see #setFastMode
   */
  public static boolean getFastMode () { return (fastMode); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the fast rendering mode flag.  Under fast rendering mode,
   * the line feature is not checked for discontinuities.  By default, fast
   * rendering mode is off.
   *
   * @param flag the fast mode, true for fast rendering or false for not.
   *
   * @see #getFastMode
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
   * Filters this feature based on an earth area.  If the line
   * segments leave and re-enter the specified earth area, they are
   * split into multiple features.  The resulting features are
   * returned.
   *
   * @param area the earth area to use for filtering earth locations.
   *
   * @return the list of features resulting from the filter operation.
   *
   * @deprecated This method was used by {@link GSHHSReader} but is no longer
   * needed.
   */
  @Deprecated
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
   * Transforms this line feature to a path.  The
   * {@link EarthImageTransform} is used to eliminate any line
   * segments which do not transform to valid image points.
   *
   * @param imageTrans the transform for converting earth locations to image
   * points.
   *
   * @return the path that follows this feature's points in the
   * transformed space.
   */
  public GeneralPath transform (
    EarthImageTransform imageTrans
  ) {

    // Transform path
    // --------------
    PathTransformer transformer = new PathTransformer();
    GeneralPath path = transformer.transformPath (points, imageTrans, false, !fastMode);
    
    // Record discontinuous status
    // ---------------------------
    lastDiscontinuous = (path == null);

    if (path == null) path = new GeneralPath();
    return (path);

  } // transform

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the general path for this feature under the specified
   * transform.
   *
   * @param trans the earth image transform for converting Earth
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
   * @param trans the earth image transform for converting Earth
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
