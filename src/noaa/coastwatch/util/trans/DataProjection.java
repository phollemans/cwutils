////////////////////////////////////////////////////////////////////////
/*

     File: DataProjection.java
   Author: Peter Hollemans
     Date: 2002/05/31

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
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.Rectangle;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.StrideLocationIterator;
import noaa.coastwatch.util.sensor.SensorIdentifier;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>DataProjection</code> class implements earth transform
 * calculations for data coordinates with explicit latitude and
 * longitude data.  The only possible operation is translation from
 * data coordinates to geographic coordinates -- the reverse is not
 * implemented.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class DataProjection 
  extends EarthTransform2D {

  private static final Logger LOGGER = Logger.getLogger (DataProjection.class.getName());

  // Constants
  // ---------
  /** Projection description string. */
  public final static String DESCRIPTION = "data";  

  // Variables
  // ---------
  /** Latitude and longitude variables. */
  private DataVariable lat, lon;

  ////////////////////////////////////////////////////////////

  /** Gets the latitude variable used in this projection. */
  public DataVariable getLat() { return (lat); }

  ////////////////////////////////////////////////////////////

  /** Gets the longitude variable used in this projection. */
  public DataVariable getLon() { return (lon); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a data projection from the specified latitude and
   * longitude data.
   *
   * @param lat a data variable containing latitude data.
   * @param lon a data variable containing longitude data.
   */
  public DataProjection (
    DataVariable lat,
    DataVariable lon
  ) {

    // Initialize variables
    // --------------------
    this.lat = lat;
    this.lon = lon;
    this.dims = lat.getDimensions();

  } // DataProjection constructor

  ////////////////////////////////////////////////////////////

  @Override
  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    earthLoc.setCoords (lat.getValue (dataLoc), lon.getValue (dataLoc));

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    dataLoc.set (Grid.ROWS, Double.NaN);
    dataLoc.set (Grid.COLS, Double.NaN);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  /**
   * Gets the median value in a list of double values.  A side effect is that
   * the parameter list is sorted.
   * 
   * @param values the list of values to return the median.
   * 
   * @return the median value or Double.NaN if the list length is zero.
   * 
   * @since 3.8.1
   */
  public static double median (List<Double> values) {

    Collections.sort (values);

    double median;
    int items = values.size();
    if (items == 0)
      median = Double.NaN;
    else if (items%2 == 0) 
      median = (values.get (items/2 - 1) + values.get (items/2)) / 2.0;
    else 
      median = values.get ((items+1)/2 - 1);

    return (median);

  } // median

  ////////////////////////////////////////////////////////////

  @Override
  public DataLocation closest ( 
    EarthLocation targetEarthLoc,
    DataLocation targetDataLoc
  ) {

    int rows = dims[Grid.ROWS];
    int cols = dims[Grid.COLS];

    // We hold onto the current location found to be closest to the target 
    // earth location.

    if (targetDataLoc == null) targetDataLoc = new DataLocation (2);

    // We track the row resolution and column resolution in order to do a final
    // check at the end to see if we came within a pixel of the target
    // location.

    double targetRowRes = 0, targetColRes = 0;

    // The initial search window is the entire set of locations.  The search
    // window is then reduced to a certain point where an exhaustive search
    // doesn't require much time.

    int minRow = 0;
    int maxRow = rows-1;
    int minCol = 0;
    int maxCol = cols-1;

    // We track the search window and keep a list to make sure that the  
    // window is getting smaller over a number of iterations.

    Rectangle searchWindow = new Rectangle (minCol, minRow, maxCol-minCol+1, maxRow-minRow+1);
    List<Rectangle> searchWindowList = new ArrayList<>();
    searchWindowList.add (searchWindow);
    boolean searchFailed = false;

    // Track the number of calls to the distance formula.  This gives us a
    // comparison between the exhaustive search and an optimized search for
    // the closest location.

    int distFuncCalls = 0;

    // Get an estimate of how far apart any geolocation discontinuities are.
    // This helps to set the search size window, which in turn helps to
    // minimize the number of iterations before the exhaustive search is 
    // needed.

    int scanLength = SensorIdentifier.getSensorScanLength (this);
    int searchSizeMax = Math.max (scanLength * 3, 10);

    if (scanLength != 1) LOGGER.fine ("Found scan discontinuties every " + scanLength + " pixels");
    LOGGER.fine ("Using search size max of " + searchSizeMax);








    // Initialize the search loop and iterate as long as:
    // (1) the search window size is still above some threshold, 
    // (2) we haven't hit some maximum number of iterations,
    // (3) the search window size is still getting smaller.

    int iter = 0;
    int maxIter = 20;




// There's an issue with the search size max.  It's possible with MODIS data
// to reduce the search window to below the search size max and then obtain 
// the incorrect pixel because it's farther away than the maximum window 
// allows for.





    int samples = 10;
    EarthLocation earthLoc = new EarthLocation();
    List<Double> rowResList = new ArrayList<>();
    List<Double> colResList = new ArrayList<>();

    while (Math.max (searchWindow.width, searchWindow.height) > searchSizeMax && iter < maxIter && !searchFailed) {




      // Start each loop by probing some set of locations within the search 
      // window.  In this case we probe a set of random locations to find 
      // one with the minimum distance to the target.

      double minDistance = Double.MAX_VALUE;
      int rowSpan = maxRow - minRow;
      int colSpan = maxCol - minCol;



      List<DataLocation> locList = new ArrayList<>();


      for (int i = 0; i < samples; i++) {
        locList.add (new DataLocation (
          minRow + (int) Math.round (Math.random() * rowSpan),
          minCol + (int) Math.round (Math.random() * colSpan
        )));
      } // for



      int gridPoints = 5;
      int centerPoint = gridPoints/2;
      for (int i = 0; i < gridPoints; i++) {
        for (int j = 0; j < gridPoints; j++) {
          if (i == centerPoint && j == centerPoint) continue;
          locList.add (new DataLocation (
            minRow + (int) Math.round ((i+1)*(1.0/(gridPoints+1))*rowSpan),
            minCol + (int) Math.round ((j+1)*(1.0/(gridPoints+1))*colSpan)
          ));
        } // for
      } // for





      // Another issue is that we may be assigning the search window to be much
      // too large in one of the two dimensions, because we don't know how much
      // of the distance is due to being off in the row direction, and how much
      // in the column direction.





      for (var dataLoc : locList) {

        this.transform (dataLoc, earthLoc);
        double dist = targetEarthLoc.distance (earthLoc);
        if (dist < minDistance) {
          minDistance = dist;
          targetDataLoc.setCoords (dataLoc);
        } // if

        distFuncCalls++;

      } // for





      // Next compute the resolution in km of the grid in a 3x3 window 
      // around the closest target location and find the median value 
      // independently in the row and column directions.

      rowResList.clear();
      colResList.clear();
      var startLoc = targetDataLoc.translate (-1, -1).truncate (dims);
      var endLoc = targetDataLoc.translate (1, 1).truncate (dims);

      DataLocation iterDataLoc = null;
      var locIter = new StrideLocationIterator (startLoc, endLoc, new int[] {1, 1});
      while (locIter.hasNext()) {
        iterDataLoc = locIter.nextLocation (iterDataLoc);
        double[] res = this.getResolution (iterDataLoc);
        if (Double.isFinite (res[Grid.ROW])) rowResList.add (res[Grid.ROW]);
        if (Double.isFinite (res[Grid.COL])) colResList.add (res[Grid.COL]);

        distFuncCalls += 2;

      } // while

      double medianRowRes = median (rowResList);
      double medianColRes = median (colResList);

      // Now compute the resolution in km of the grid over a larger area to
      // gauge how much the row and column resolutions differ from their 
      // local values.  Pick the minimum resolution so that in the next step,
      // the search window is a little larger and takes account of the 
      // resolution difference.

      double rowRadius = minDistance / medianRowRes;
      var topLoc = targetDataLoc.translate (-rowRadius, 0).truncate (dims).round();
      int topRow = (int) topLoc.get (Grid.ROW);
      var bottomLoc = targetDataLoc.translate (rowRadius, 0).truncate (dims).round();
      int bottomRow = (int) bottomLoc.get (Grid.ROW);
      double windowRowRes = this.distance (topLoc, bottomLoc)/(bottomRow - topRow);
      double rowRes = 
        Double.isNaN (windowRowRes) ? medianRowRes :  
        Double.isNaN (medianRowRes) ? windowRowRes :  
        Math.min (medianRowRes, windowRowRes);

      double colRadius = minDistance / medianColRes;
      var leftLoc = targetDataLoc.translate (0, -colRadius).truncate (dims).round();
      int leftCol = (int) leftLoc.get (Grid.COL);
      var rightLoc = targetDataLoc.translate (0, colRadius).truncate (dims).round();
      int rightCol = (int) rightLoc.get (Grid.COL);
      double windowColRes = this.distance (leftLoc, rightLoc)/(rightCol - leftCol);
      double colRes = 
        Double.isNaN (windowColRes) ? medianColRes :  
        Double.isNaN (medianColRes) ? windowColRes :  
        Math.min (medianColRes, windowColRes);

      distFuncCalls += 2;

      // We should never have the next condition be true.  If it is, something
      // has gone drastically wrong with the resolution calculations.  Possibly
      // there are nulls in the earth location data.

      if (Double.isNaN (rowRes) || Double.isNaN (colRes)) { 
        searchFailed = true;
        continue;
      } // if

      // Using the row and column resolutions, compute a new search window
      // that brackets the closest target location.  Make sure the new 
      // search window is some subset of the previous window.

      double newRowSpan = (minDistance / rowRes) * 3;
      double newColSpan = (minDistance / colRes) * 3;

      startLoc = targetDataLoc.translate (-newRowSpan/2, -newColSpan/2).truncate (dims).round();
      endLoc = targetDataLoc.translate (newRowSpan/2, newColSpan/2).truncate (dims).round();

      minRow = Math.max (minRow, (int) startLoc.get (Grid.ROW));
      maxRow = Math.min (maxRow, (int) endLoc.get (Grid.ROW));
      minCol = Math.max (minCol, (int) startLoc.get (Grid.COL));
      maxCol = Math.min (maxCol, (int) endLoc.get (Grid.COL));

      // Check that the search window is getting smaller.  If it's not, this
      // may indicate that the target location is outside the area covered
      // by this grid of locations.

      searchWindow = new Rectangle (minCol, minRow, maxCol-minCol+1, maxRow-minRow+1);
      searchWindowList.add (searchWindow);

      if (searchWindowList.size() == 3) {
        Rectangle firstWindow = searchWindowList.get (0);
        Rectangle lastWindow = searchWindowList.get (searchWindowList.size() - 1);
        if (lastWindow.equals (firstWindow)) searchFailed = true;
        else searchWindowList.remove (0);
      } // if

      iter++;

      if (LOGGER.isLoggable (Level.FINER)) {
        LOGGER.finer ("***** Iteration [" + iter + "/" + maxIter + "] *****");
        LOGGER.finer ("targetDataLoc = " + targetDataLoc);
        LOGGER.finer ("minDistance (km) = " + minDistance);
        LOGGER.finer ("medianRowRes (km) = " + medianRowRes);
        LOGGER.finer ("medianColRes (km) = " + medianColRes);
        LOGGER.finer ("windowRowRes (km) = " + windowRowRes);
        LOGGER.finer ("windowColRes (km) = " + windowColRes);
        LOGGER.finer ("minRow = " + minRow);
        LOGGER.finer ("maxRow = " + maxRow);
        LOGGER.finer ("minCol = " + minCol);
        LOGGER.finer ("maxCol = " + maxCol);
//        LOGGER.finer ("searchWindow = " + searchWindow);
      } // if

      // Save the median resolutions so that we can do a test on them later.

      targetRowRes = medianRowRes;
      targetColRes = medianColRes;

    } // while

    // If the search failed, either the window wasn't decreasing in size, or
    // there was some issue with computing the resolution.  If so, mark 
    // the coordinates of the target data location as invalid.  Otherwise, do 
    // an exhaustive search in a small window.

    if (searchFailed) { 
      LOGGER.finer ("Search failed for target location = " + targetEarthLoc);
      targetDataLoc.markInvalid();
    } // if

    else {

      var startLoc = targetDataLoc.translate (-searchSizeMax/2, -searchSizeMax/2).truncate (dims).round();
      var endLoc = targetDataLoc.translate (searchSizeMax/2, searchSizeMax/2).truncate (dims).round();
      var locIter = new StrideLocationIterator (startLoc, endLoc, new int[] {1, 1});
      double minDistance = Double.MAX_VALUE;

      DataLocation iterDataLoc = null;
      while (locIter.hasNext()) {
        iterDataLoc = locIter.nextLocation (iterDataLoc);
        this.transform (iterDataLoc, earthLoc);
        double dist = earthLoc.distance (targetEarthLoc);
        if (dist < minDistance) {
          minDistance = dist;
          targetDataLoc.setCoords (iterDataLoc);
        } // if

        distFuncCalls++;

      } // while

      // Check the results of the exhaustive search.  If the distance to the
      // target earth location isn't within the largest resolution circle of the 
      // location found, then we can't say that we found the target.  If the 
      // distance isn't within the smallest resolution circle of the location, 
      // we also can't say for sure that we found it, but we print a different
      // debugging message. 

      if (minDistance > Math.max (targetRowRes, targetColRes)) { 
        LOGGER.finer ("Search failed max radius distance test for target location = " + targetEarthLoc);
        targetDataLoc.markInvalid();
      } // if
      else if (minDistance > Math.min (targetRowRes, targetColRes)) { 
        LOGGER.finer ("Search is undetermined for target location = " + targetEarthLoc);
        targetDataLoc.markInvalid();
      } // else if

      else {

        // If all these tests are successful, then we conclude that we found the 
        // target and report.

        if (LOGGER.isLoggable (Level.FINER)) {
          LOGGER.finer ("Final targetDataLoc = " + targetDataLoc);
          LOGGER.finer ("Final minDistance (km) = " + minDistance);
          LOGGER.finer ("Number of calls to distance function = " + distFuncCalls);
          LOGGER.finer ("Improvement over exhaustive search = " + (int) (((double) (rows*cols)) / distFuncCalls));
        } // if

      } // else

    } // else

    return (targetDataLoc);

  } // closest

  ////////////////////////////////////////////////////////////

  /**
   * Searches for the closest integer data location to a specified geographic
   * location by computing the distance to each earth location in the data
   * and finding the minimum.  This routine is meant to be used for testing 
   * and comparison purposes only.
   * 
   * @param targetEarthLoc the target earth location to locate the closest 
   * valid data location.
   * @param targetDataLoc the data location or null.  If null, an object
   * is created and returned.  If non-null, the object is simply
   * modified.
   *
   * @return the data location.
   * 
   * @see #closest
   * 
   * @since 3.8.1
   */
  public DataLocation closestBySearch ( 
    EarthLocation targetEarthLoc,
    DataLocation targetDataLoc
  ) {

    var earthLoc = new EarthLocation();
    var dataLoc = new DataLocation (2);
    double minDistance = Double.MAX_VALUE;
    int[] coords = new int[2];
    if (targetDataLoc == null) targetDataLoc = new DataLocation (2);

    for (coords[Grid.ROW] = 0; coords[Grid.ROW] < dims[Grid.ROWS]; coords[Grid.ROW]++) {
      for (coords[Grid.COL] = 0; coords[Grid.COL] < dims[Grid.COLS]; coords[Grid.COL]++) {

        dataLoc.setCoords (coords);
        this.transform (dataLoc, earthLoc);

        double dist = earthLoc.distance (targetEarthLoc);
        if (dist < minDistance) {
          minDistance = dist;
          targetDataLoc.setCoords (dataLoc);
        } // if

      } // for
    } // for

    return (targetDataLoc);

  } // closestBySearch

  ////////////////////////////////////////////////////////////

  /** Tests the {@link #closest} method on this transform. */
  public void testClosest () {

    var earthLoc = new EarthLocation();
    var dataLoc = new DataLocation (2);
    int[] coords = new int[2];
    var foundDataLoc = new DataLocation (2);

    for (coords[Grid.ROW] = 0; coords[Grid.ROW] < dims[Grid.ROWS]; coords[Grid.ROW]++) {

      if (coords[Grid.ROW]%100 == 0) System.out.println ("Testing at row " + coords[Grid.ROW]);

      for (coords[Grid.COL] = 0; coords[Grid.COL] < dims[Grid.COLS]; coords[Grid.COL]++) {

        dataLoc.setCoords (coords);
        this.transform (dataLoc, earthLoc);
        this.closest (earthLoc, foundDataLoc);

        if (!foundDataLoc.equals (dataLoc)) {
          LOGGER.warning ("Mismatch in closest(), found " + foundDataLoc + 
            " searching for " + earthLoc.format() + " at " + dataLoc);



          return;



        } // if

      } // for
    } // for

  } // testClosest

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isInvertible () { return (false); }

  ////////////////////////////////////////////////////////////

  /**
   * Compares the specified object with this data projection for
   * equality.  The latitudes and longitudes of the two data
   * projections are compared value by value.
   *
   * @param obj the object to be compared for equality.
   *
   * @return true if the data projections are equivalent, or false if
   * not.  
   */
  @Override
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof DataProjection)) return (false);

    // Check each lat/lon value
    // ------------------------
    DataProjection data = (DataProjection) obj;

    // int n = this.lat.getValues ();
    // for (int i = 0; i < n; i++) {
    //   if (this.lat.getValue (i) != data.lat.getValue (i)) return (false);
    //   if (this.lon.getValue (i) != data.lon.getValue (i)) return (false);
    // } // for

    // We test here if the projections even have the same dimensions
    if (!Arrays.equals (this.dims, data.dims)) return (false);

    // Since this operation can take a very long time for large data 
    // projections, we calculate the statistics of 1% of the data instead,
    // then compare the results

    LOGGER.warning ("Comparing data projections using statistics, results may not be exact");

    boolean isEqual = 
      equalStats (this.lat.getStatistics (0.01), data.lat.getStatistics (0.01)) &&
      equalStats (this.lon.getStatistics (0.01), data.lon.getStatistics (0.01));

    return (isEqual);

  } // equals

  ////////////////////////////////////////////////////////////

  /**
   * Compares two stats objects for equality on the main statistics.
   * 
   * @param a the first stats object.
   * @param b the second stats object.
   * 
   * @return true if the stats are equal in valid, min, max, mean, and stdev.
   * 
   * @since 3.8.0
   */
  private boolean equalStats (Statistics a, Statistics b) {

    return (
      a.getValid() == b.getValid() &&
      equalDouble (a.getMin(), b.getMin()) &&
      equalDouble (a.getMax(), b.getMax()) &&
      equalDouble (a.getMean(), b.getMean()) &&
      equalDouble (a.getStdev(), b.getStdev())
    );

  } // equalStats

  ////////////////////////////////////////////////////////////

  /**
   * Compares two doubles for equality.
   * 
   * @param a the first double.
   * @param b the second double.
   *
   * @return true if the doubles are equal or both ar NaN, or false
   * if they are different.
   * 
   * @since 3.8.0
   */
  private boolean equalDouble (double a, double b) {

    var aNaN = Double.isNaN (a);
    var bNaN = Double.isNaN (b);
    return ((aNaN && bNaN) || (a == b));

  } // equalDouble

  ////////////////////////////////////////////////////////////

} // DataProjection class

////////////////////////////////////////////////////////////////////////
