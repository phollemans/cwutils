////////////////////////////////////////////////////////////////////////
/*

     File: PathTransformer.java
   Author: Peter Hollemans
     Date: 2019/08/29

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.Topology;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.logging.Logger;

/**
 * The <code>PathTransformer</code> class converts line segments of
 * earth location data to projected image data, taking account
 * of discontinuities and boundary cuts in the line segments.
 *
 * @author Peter Hollemans
 * @since 3.5.1
 */
public class PathTransformer {

  private static final Logger LOGGER = Logger.getLogger (PathTransformer.class.getName());

  // Variables
  // ---------
  
  /** The diagnostic mode, true to perform extra diagnostics. */
  private static boolean diagnosticMode = false;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the diagnostic mode.  With diagnostic mode on, an extra step
   * is performed when transforming the path to check for inconsistencies
   * in earth transforms that return true from the
   * {@link EarthTransform#hasBoundaryCheck} method and the actual points
   * returned to make sure that a discontinuity didn't occur.  If the extra
   * test reveals a discontinuity, an error is logged with the locations
   * of the issue.
   *
   * @parm flag the diagnostic mode flag, true to perform the diagnostic.
   * By default, diagnostics are off.
   */
  public static void setDiagnosticMode (
    boolean flag
  ) {

    diagnosticMode = flag;

  } // setDiagnosticMode
  
  ////////////////////////////////////////////////////////////

  /**
   * Determines if any of the line segments in this feature cross the
   * anti-meridian line at +180/-180 longitude.
   *
   * @param points the earth location points to check.
   *
   * @return true if any line segment crosses the anti-meridian or false if not.
   * The test for crossing the anti-meridian is as defined in
   * {@link noaa.coastwatch.util.EarthLocation#crossesAntiMeridian}.
   */
  private boolean crossesAntiMeridian (
    List<EarthLocation> points
  ) {

    boolean crosses = false;

    int count = points.size();
    if (count > 0) {
      EarthLocation lastLoc = points.get (0);
      for (int i = 1; i < count; i++) {
        EarthLocation currentLoc = points.get (i);
        if (lastLoc.crossesAntiMeridian (currentLoc)) {
          crosses = true;
          break;
        } // if
        lastLoc = currentLoc;
      } // for
    } // if

    return (crosses);

  } // crossesAntiMeridian

  ////////////////////////////////////////////////////////////

  /**
   * Determines the winding order of a polygon.
   *
   * @param poly to polygon to compute the winding order.
   *
   * @return 1 if the winding order is clockwise or -1 if counter-clockwise.
   */
  private int windingOrder (
    Polygon poly
  ) {
  
    double sum = 0;
    Coordinate[] coords = poly.getCoordinates();
    
    for (int i = 0; i < coords.length; i++) {
      Coordinate n = coords[i];
      Coordinate np1 = coords[(i+1) % coords.length];
      sum += (np1.getX() - n.getX())*(np1.getY() + n.getY());
    } // for
    
    return (sum < 0 ? -1 : 1);
  
  } // windingOrder

  ////////////////////////////////////////////////////////////

  /**
   * Converts a sequence of coordinates to a path of image points. If the
   * coordinates are found to have invalid image points, the conversion is
   * terminated.
   */
  private static class CoordinateToPathConvertor
    implements CoordinateSequenceFilter {
  
    private boolean pathInvalid = false;
    private GeneralPath path = new GeneralPath();
    private EarthImageTransform imageTrans;
    private EarthLocation loc = new EarthLocation();

    public CoordinateToPathConvertor (
      EarthImageTransform imageTrans
    ) {
    
      this.imageTrans = imageTrans;
    
    } // CoordinateToPathConvertor
  
    /**
     * Gets the path resulting from the conversion.
     *
     * @return the path or null if the conversion failed.
     */
    public GeneralPath getPath () { return (path); }
  
    @Override
    public void filter (CoordinateSequence sequence, int index) {

      loc.setCoords (sequence.getY (index), sequence.getX (index));
      Point2D point = imageTrans.transform (loc);

      // If we get a null point here, stop processing
      
      if (point == null) {
        pathInvalid = true;
        path = null;
      } // if
      else if (index == 0)
        path.moveTo ((float) point.getX(), (float) point.getY());
      else
        path.lineTo ((float) point.getX(), (float) point.getY());

    } // filter
    
    @Override
    public boolean isDone() { return (pathInvalid); }

    @Override
    public boolean isGeometryChanged() { return (false); }
    
  } // CoordinateToPathConvertor class

  ////////////////////////////////////////////////////////////

  /**
   * Transforms the specified list of locations to a path in the image space.
   *
   * @param locList the list of locations to transform.  The locations are
   * assumed to be joined by line segments.
   * @param imageTrans the transform for converting earth locations to image
   * points.
   * @param isPolygon true if the location list forms a polygon and the
   * result should be a polygon, or false if not.
   * @param checkDiscontinuous true to check if a line between location pairs
   * is discontinuous in the image space, or false to not.  The check is
   * done by calculating the resolution of the image space, so is slower than
   * not checking.
   *
   * @return the path in the image space that follows the list of locations,
   * or null if the list represents a polygon and the polygon could not be
   * transformed due to discontinuities.
   */
  public GeneralPath transformPath (
    List<EarthLocation> locList,
    EarthImageTransform imageTrans,
    boolean isPolygon,
    boolean checkDiscontinuous
  ) {

    // Initialize path
    // ---------------
    GeneralPath path = new GeneralPath();

    // Initialize location loop
    // ------------------------
    Point2D point = null;
    Point2D lastPoint = null;
    EarthLocation lastLoc = null;
    boolean needsMoveTo = true;
    int moveToCount = 0;

    EarthTransform earthTrans = imageTrans.getEarthTransform();
    boolean boundaryCheck = earthTrans.hasBoundaryCheck();

    // Loop over each location in the list
    // -----------------------------------
    boolean boundaryCut = false;
    for (EarthLocation loc : locList) {

      // Transform location to image point
      // ---------------------------------
      point = imageTrans.transform (loc);

      // Detect invalid location transform
      // ---------------------------------
      if (point == null) {

        // Here we conclude that if a boundary checking transform has
        // returned a null image point, then the point fell outside
        // of some allowed boundary and therefore a boundary cut should
        // be applied.

        if (boundaryCheck) {
          boundaryCut = true;
          break;
        } // if
        
        needsMoveTo = true;
      } // if

      // Handle valid segment
      // --------------------
      else if (lastPoint != null && point != null) {

        boolean pointJumped = false;

        // Perform boundary check
        // ----------------------
        if (boundaryCheck) {
          if (earthTrans.isBoundaryCut (lastLoc, loc)) {
            boundaryCut = true;
            break;
          } // if
        } // if

        // Perform discontinuity check
        // ---------------------------
        else {
          pointJumped = (checkDiscontinuous ?
            imageTrans.isDiscontinuous (lastLoc, loc, lastPoint, point) : false);
        } // if

        // We perform an extra diagnostic here to help solve issues with
        // earth transforms that perform boundary checks and are
        // returning false from the isBoundaryCut method but a discontinuity
        // is detected between two ends of a line segment
        
        if (diagnosticMode) {
          boolean jumped = imageTrans.isDiscontinuous (lastLoc, loc, lastPoint, point);
          if (boundaryCheck && jumped) {
            LOGGER.warning ("Earth transform failed diagsnostic, jump detected between " +
              "(" + lastLoc.format (EarthLocation.RAW) + ") and " +
              "(" + loc.format (EarthLocation.RAW) + ")");
          } // if
        } // if

        // If jumped, don't append to path
        // -------------------------------
        if (pointJumped)
          needsMoveTo = true;

        // Otherwise, append to path
        // -------------------------
        else {
          if (needsMoveTo) {
            path.moveTo ((float) lastPoint.getX(), (float) lastPoint.getY());
            moveToCount++;
          } // if
          path.lineTo ((float) point.getX(), (float) point.getY());
          needsMoveTo = false;
        } // else

      } // else if

      // Save last location data
      // -----------------------
      lastPoint = point;
      lastLoc = loc;

    } // for

    // Split locations around boundary
    // -------------------------------
    if (boundaryCut) {

      // Check if polygon needs closing (for using JTS)
      // ----------------------------------------------
      if (isPolygon) {
        if (locList.size() > 1) {
          EarthLocation first = locList.get (0);
          EarthLocation last = locList.get (locList.size() - 1);
          if (!first.equals (last)) {
            locList = new ArrayList<> (locList);
            locList.add (first);
          } // if
        } // if
      } // if

      // Create geometry for locations
      // -----------------------------
      GeometryFactory factory = Topology.getFactory();
      boolean crosses = crossesAntiMeridian (locList);
      int coordCount = locList.size();
      Coordinate[] coords = new Coordinate[coordCount];
      for (int i = 0; i < coordCount; i++) {
        EarthLocation loc = locList.get (i);
        double lon = loc.lon;
        if (crosses && lon < 0) lon += 360;
        coords[i] = Topology.createCoordinate (lon, loc.lat);
      } // for

      // We save the parent polygon winding order here in order to restore
      // later in case the split changes the winding order.

      Geometry geom;
      int parentWinding = 0;
      if (isPolygon) {
        Polygon polygon = factory.createPolygon (coords);
        parentWinding = windingOrder (polygon);
        geom = polygon;
      } // if
      else {
        geom = factory.createLineString (coords);
      } // else

      // Split geometry
      // --------------
      Geometry splitter = earthTrans.getBoundarySplitter();

      // We do this next line for polygon splits because we were
      // getting an error thrown in the JTS difference method about
      // encountering a non-noded intersection.  The advice online
      // was to fix the polygon first by performing a buffer operation.
      
      if (isPolygon) {
        geom = geom.buffer (0);
      } // if

      Geometry splitGeom = geom.difference (splitter);
      
      // Loop over all resulting split components
      // ----------------------------------------
      int geomCount = splitGeom.getNumGeometries();
      GeneralPath geomPath = new GeneralPath();

      for (int i = 0; i < geomCount; i++) {

        Geometry component = splitGeom.getGeometryN (i);

        // Restore parent polygon winding order
        // ------------------------------------
        if (isPolygon) {
          int componentWinding = windingOrder ((Polygon) component);
          if (parentWinding != componentWinding)
            component = component.reverse();
        } // if

        // Convert component to path
        // -------------------------
        CoordinateToPathConvertor convertor = new CoordinateToPathConvertor (imageTrans);
        component.apply (convertor);
        
        // If we get a null path here, it means that after splitting
        // the geometry, there are some invalid components and some
        // valid components, and we must be inside one of the invalid
        // components.  So we discard it.

        GeneralPath componentPath = convertor.getPath();
        if (componentPath != null) {
          if (isPolygon) componentPath.closePath();
          geomPath.append (componentPath, false);
        } // if

      } // for

      // Save path to return
      // -------------------
      path = geomPath;

    } // if

    // Check if we had a discontinuous polygon
    // ---------------------------------------
    else {
      if (moveToCount > 1 && isPolygon) path = null;
    } // else

    return (path);

  } // transformPath

  ////////////////////////////////////////////////////////////

} // PathTransformer class

////////////////////////////////////////////////////////////////////////
