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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;

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
        needsMoveTo = true;
      } // if

      // Handle valid segment
      // --------------------
      else if (lastPoint != null && point != null) {

        boolean pointJumped = false;

        // Perform boundary check
        // ----------------------
        if (earthTrans.hasBoundaryCheck()) {
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
      GeometryFactory factory = new GeometryFactory();
      boolean crosses = crossesAntiMeridian (locList);
      int coordCount = locList.size();
      Coordinate[] coords = new Coordinate[coordCount];
      for (int i = 0; i < coordCount; i++) {
        EarthLocation loc = locList.get (i);
        double lon = loc.lon;
        if (crosses && lon < 0) lon += 360;
        coords[i] = new Coordinate (lon, loc.lat);
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
      Geometry splitGeom = geom.difference (splitter);

      // Loop over all resulting split components
      // ----------------------------------------
      int geomCount = splitGeom.getNumGeometries();
      GeneralPath geomPath = new GeneralPath();
      EarthLocation loc = new EarthLocation();
      
      for (int i = 0; i < geomCount; i++) {

        Geometry component = splitGeom.getGeometryN (i);

        // Restore parent polygon winding order
        // ------------------------------------
        if (isPolygon) {
          int componentWinding = windingOrder ((Polygon) component);
          if (parentWinding != componentWinding)
            component = component.reverse();
        } // if

        // Convert component to path components
        // ------------------------------------
        component.apply (new CoordinateFilter () {
          boolean firstCoord = true;
          public void filter (Coordinate coord) {
            loc.setCoords (coord.getY(), coord.getX());
            Point2D point = imageTrans.transform (loc);
            if (firstCoord) {
              geomPath.moveTo ((float) point.getX(), (float) point.getY());
              firstCoord = false;
            } // if
            else {
              geomPath.lineTo ((float) point.getX(), (float) point.getY());
            } // else
          } // filter
        });

        if (isPolygon) geomPath.closePath();

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
