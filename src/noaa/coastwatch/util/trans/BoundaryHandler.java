////////////////////////////////////////////////////////////////////////
/*

     File: BoundaryHandler.java
   Author: Peter Hollemans
     Date: 2019/09/17

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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiPredicate;

import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Topology;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.util.logging.Logger;

/**
 * The <code>BoundaryHandler</code> class handles lines of {@link EarthLocation}
 * data that cross boundaries in an {@link EarthTransform} and provides
 * splitting geometries for the boundary, as well as a predictate function
 * for testing boundary cuts.
 *
 * @author Peter Hollemans
 * @since 3.5.1
 */
public class BoundaryHandler {

  private static final Logger LOGGER = Logger.getLogger (BoundaryHandler.class.getName());

  /** The collection of line strings used for generating the boundary splitter. */
  private Geometry cutLines;

  /** The geometry to use for boundary splitting. */
  private Geometry splitter;

  /** The function for testing a boundary cut. */
  private BiPredicate<EarthLocation, EarthLocation> cutTest;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new handler.
   *
   * @param cutTest the test function for cuts conforming to
   * {@link #isBoundaryCut}.
   * @param lineList the list of location lines to use for
   * boundary splitting.
   */
  public BoundaryHandler (
    BiPredicate<EarthLocation, EarthLocation> cutTest,
    List<List<EarthLocation>> lineList
  ) {

    this.cutTest = cutTest;
    for (List<EarthLocation> line : lineList) addCutLine (line);
  
  } // BoundaryHandler

  ////////////////////////////////////////////////////////////

  /**
   * Adds the specified cut line to the boundary splitting geometry
   * for this handler.
   *
   * @param locList the list of locations forming the new cut line.
   */
  private void addCutLine (
    List<EarthLocation> locList
  ) {
  
    Geometry geom = getLineGeometry (locList);
    cutLines = (cutLines == null ? geom : cutLines.union (geom));

    LOGGER.fine ("Added cut line of size " + locList.size() + " with " +
      geom.getNumGeometries() + " geometries");

  } // addCutLine

  ////////////////////////////////////////////////////////////

  /**
   * Gets the geometry corresponding to the list of locations, taking
   * into account any line segments that cross the anti-meridian.
   *
   * @param locList the list of locations to convert to a geometry.
   *
   * @return the geometry corresponding to the locations.
   */
  private static Geometry getLineGeometry (
    List<EarthLocation> locList
  ) {
  
    // Detect anti-meridian crossings
    // ------------------------------
    List<Integer> crossingList = new ArrayList<>();
    int count = locList.size();
    for (int i = 1; i < count; i++) {
      EarthLocation a = locList.get (i-1);
      EarthLocation b = locList.get (i);
      
      // Store the second location in each crossing
      
      if (a.crossesAntiMeridian (b)) crossingList.add (i);
    } // for

    // Create location segments
    // ------------------------
    
    // If there are n crossings, there are n+1 segments
    
    int crossings = crossingList.size();
    int segments = crossings + 1;
    List[] segmentArray = new List[segments];

    // Handles the edge case of segments = 1
    // Also the case of a crossing at loc = 1
    // Also the case of a crossing at loc = count-1

    for (int i = 0; i < segments; i++) {
      int start = (i == 0 ? 0 : crossingList.get (i-1));
      int end = (i == (segments-1) ? count : crossingList.get (i));
      segmentArray[i] = locList.subList (start, end);
    } // for

    // Convert each segment to geometry
    // --------------------------------
    GeometryFactory factory = Topology.getFactory();
    Geometry geom = null;
    for (int i = 0; i < segments; i++) {

      // We don't create a line string if the segment length is < 2

      int length = segmentArray[i].size();
      if (length > 1) {
        Coordinate[] coords = new Coordinate[length];
        for (int j = 0; j < length; j++) {
          EarthLocation loc = (EarthLocation) segmentArray[i].get (j);
          coords[j] = Topology.createCoordinate (loc.lon, loc.lat);
        } // for
        Geometry lineString = factory.createLineString (coords);
        geom = (geom == null ? lineString : geom.union (lineString));
      } // if
    
    } // for

    return (geom);

  } // getLineGeometry

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a line joining two earth locations crosses a boundary cut
   * line in this handler, according to the cut test.
   *
   * @param a the first earth location.
   * @param b the second earth location.
   *
   * @return true if the line joining the two locations is cut by a boundary,
   * or false if not.
   */
  public boolean isBoundaryCut (
    EarthLocation a,
    EarthLocation b
  ) {
  
    return (cutTest.test (a, b));
  
  } // isBoundaryCut

  ////////////////////////////////////////////////////////////

  /**
   * Gets the boundary splitting geometry for this handler to perform splits on
   * earth location lines and polygons.
   *
   * @return the geometry to use for splitting.
   */
  public Geometry getSplitter () {

    if (splitter == null) {
      BufferParameters params = new BufferParameters (0,
        BufferParameters.CAP_SQUARE, BufferParameters.JOIN_BEVEL, 0);
      splitter = BufferOp.bufferOp (cutLines, Topology.EPSILON*2, params);
    } // if

    return (splitter);

  } // getSplitter

  ////////////////////////////////////////////////////////////

} // BoundaryHandler class

////////////////////////////////////////////////////////////////////////

