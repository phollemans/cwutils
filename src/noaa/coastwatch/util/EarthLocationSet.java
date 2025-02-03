////////////////////////////////////////////////////////////////////////
/*

     File: EarthLocationSet.java
   Author: Peter Hollemans
     Date: 2018/12/08

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util;

// Imports
// -------
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import noaa.coastwatch.util.EarthLocation;

// Testing
import java.util.logging.Logger;
import noaa.coastwatch.test.TestLogger;

/**
 * An <code>EarthLocationSet</code> holds a number of earth locations and allows
 * for fast retrieval of the nearest location in the set to a given point and
 * an associated data object.  The implementation uses bins and assumes that
 * locations are contiguous or highly clustered and that bins that
 * contain locations are adjacent to some other bin in the set.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
@noaa.coastwatch.test.Testable
public class EarthLocationSet<T> {

  private static final Logger LOGGER = Logger.getLogger (EarthLocationSet.class.getName());

  // Constants
  // ---------

  /** A small number used to nudge values one way or the other. */
  private static final double EPSILON = 1e-6;

  // Variables
  // ---------

  /** The number of latitude degrees covered by each bin. */
  private double latDegreesPerBin;

  /** The number of longitude degrees covered by each bin. */
  private double[] lonDegreesPerBinAtLatitudeRing;

  /** The number of latitude rings. */
  private int latRings;

  /** The bin count at each latitude ring. */
  private int[] binsAtLatitudeRing;

  /** The bin index of the first bin in each latitude ring. */
  private int[] binIndexAtLatitudeRing;

  /** The earth location bin index to bin data. */
  private Map<Integer, BinData> binDataMap;

  ////////////////////////////////////////////////////////////

  /**
   * Holds the data for a single bin of values. We tried using a kd-tree here
   * but it was computationally much more expensive than just searching
   * the data for the nearest entry.  There may be a less computationally
   * expensive data structure or search method here but so far we haven't
   * found it.
   */
  private class BinData {

    /** The list of entries in this bin. */
    private List<Entry> entryList;

    /** Creates a new bin of entries. */
    public BinData () {
      entryList = new ArrayList<>();
    } // BinData const
    
    /** Inserts a new entry into the bin. */
    public void insert (Entry entry) {
      entryList.add (entry);
    } // insert

    /**
     * Finds the nearest entry in the bin to the specified search coords.
     *
     * @param search the search ECF coordinates.
     * @param dist2Ret the distance squared value computed for the nearest
     * entry (modified).
     *
     * @return the entry in the bin that is nearest the specified ECF coords.
     */
    public Entry nearest (double[] search, double[] dist2Ret) {

      double minDist2 = Double.MAX_VALUE;
      Entry nearestEntry = null;
      int index = 0;

      for (Entry entry : entryList) {
        double dist2 = 0;
        for (int i = 0; i < entry.ecfCoords.length; i++) {
          double diff = entry.ecfCoords[i] - search[i];
          dist2 += diff*diff;
        } // for
        if (dist2 < minDist2) {
          minDist2 = dist2;
          nearestEntry = entryList.get (index);
        } // if
        index++;
      } // for

      dist2Ret[0] = minDist2;
      return (nearestEntry);

    } // nearest
    
  } // BinData class

  ////////////////////////////////////////////////////////////

  /**
   * An entry in the set holds the data for an earth location and it's
   * associated data.
   */
  public class Entry {

    /** The earth location for this entry. */
    public EarthLocation loc;

    /** The ECF coordinates for the location, computed upon insertion. */
    public double[] ecfCoords;

    /** The data passed to the insert method. */
    public T data;

  } // Entry class

  ////////////////////////////////////////////////////////////

  /**
   * A search context is used to hold onto some search state that can later be
   * used to improve the performance of the search.
   */
  private static class SearchContext {
  
    /** The map of bin index to adjacency array map. */
    private Map<Integer, int[]> binAdjacencyMap = new HashMap<>();

  } // SearchContext class

  ////////////////////////////////////////////////////////////

  /**
   * Gets a search context object to use with repeated calls to the
   * {@link #nearest} method.
   *
   * @return the new search context object.
   */
  public Object getContext () { return (new SearchContext()); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new empty location set.
   *
   * @param binsPerDegree the number of bins to use per degree of
   * latitude, minimum 1.
   */
  public EarthLocationSet (
    int binsPerDegree
  ) {
  
    latRings = 180*binsPerDegree;
    latDegreesPerBin = 1.0/binsPerDegree;

    // Compute bins at each latitude
    // -----------------------------
    binsAtLatitudeRing = new int[latRings];
    lonDegreesPerBinAtLatitudeRing = new double[latRings];
    int binsAtEquator = 360*binsPerDegree;
    for (int i = 0; i < latRings; i++) {
      double baseLat = latDegreesPerBin*i - 90;
      double binFactor = (Math.sin (Math.toRadians (baseLat + latDegreesPerBin)) -
        Math.sin (Math.toRadians (baseLat))) / Math.sin (Math.toRadians (latDegreesPerBin));
      int bins = (int) Math.round (binsAtEquator * binFactor);
      if (bins < 1) bins = 1;
      binsAtLatitudeRing[i] = bins;
      lonDegreesPerBinAtLatitudeRing[i] = 360.0/bins;
    } // for

    // Compute index of first bin at each latitude ring
    // ------------------------------------------------
    binIndexAtLatitudeRing = new int[latRings];
    int binIndex = 0;
    for (int i = 0; i < latRings; i++) {
      binIndexAtLatitudeRing[i] = binIndex;
      binIndex += binsAtLatitudeRing[i];
    } // for
  
    binDataMap = new HashMap<>();

  } // EarthLocationSet constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the count of actively used bins.  These are bins that have
   * locations as a result of insertions.
   *
   * @return the count of active bins.
   */
  public int getBinCount () { return (binDataMap.size()); }

  ////////////////////////////////////////////////////////////

  /** Clears the set so that it contains no locations. */
  public void clear () {

    binDataMap.clear();
  
  } // clear

  ////////////////////////////////////////////////////////////

  /**
   * Gets a bin index for a location.
   *
   * @param loc the earth location.
   *
   * @return the bin index or -1 if the location data is invalid.
   */
  private int getBinIndex (
    EarthLocation loc
  ) {

    int binIndex = -1;

    int latRing = getLatRing (loc.lat);
    if (latRing != -1) {
      int lonBin = getLonBin (latRing, loc.lon);
      if (lonBin != -1)
        binIndex = getBinIndex (latRing, lonBin);
    } // if

    return (binIndex);

  } // getBinIndex
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the latitude ring for a location.
   *
   * @param lat the earth location latitude in the range [-90 .. 90].
   *
   * @return the latitude ring index or -1 if the latitude is invalid.
   */
  private int getLatRing (
    double lat
  ) {

    int latRing = (int) Math.floor ((lat + 90) / latDegreesPerBin);

    if (latRing == latRings) latRing = latRings-1;
    if (latRing < 0 || latRing > latRings-1) latRing = -1;
  
    return (latRing);
  
  } // getLatRing

  ////////////////////////////////////////////////////////////

  /**
   * Gets the longitude bin index for a location.
   *
   * @param latRing the latitude ring for the specified longitude.
   * @param lon the earth location longitude in the range [-180 .. 360].
   *
   * @return the longitude bin index or -1 if the location data is invalid.
   */
  private int getLonBin (
    int latRing,
    double lon
  ) {
  
    if (lon >= 180) lon -= 360;
    int lonBin = (int) Math.floor ((lon + 180) / lonDegreesPerBinAtLatitudeRing[latRing]);
    if (lonBin < 0 || lonBin > binsAtLatitudeRing[latRing]-1) lonBin = -1;

    return (lonBin);
  
  } // getLonBin

  ////////////////////////////////////////////////////////////

  /**
   * Inserts a new location and its data into the location set.
   *
   * @param loc the location to insert.
   * @param data the data to insert with the location.
   */
  public void insert (
    EarthLocation loc,
    T data
  ) {
  
    // Get bin index
    // -------------
    int index = getBinIndex (loc);

    if (index != -1) {

      // Create new entry
      // ----------------
      Entry entry = new Entry();
      entry.loc = loc;
      entry.ecfCoords = new double[3];;
      loc.computeECF (entry.ecfCoords);
      entry.data = data;
      
      // Retrieve entry list
      // -------------------
      BinData binData = binDataMap.get (index);
      if (binData == null) {
        binData = new BinData();
        binDataMap.put (index, binData);
      } // if

      // Add the new entry
      // -----------------
      binData.insert (entry);

    } // if

  } // insert

  ////////////////////////////////////////////////////////////

  /**
   * Gets the bin index for a bin at specific latitude and longitude position.
   *
   * @param latRing the latitude ring to use in the range [0 .. latRings-1].
   * @param lonBin the longitude bin to use in the range [0 .. binsAtLatitudeRing[latRing]-1].
   *
   * @return the bin index for the latitude ring and longitude bin specified.
   */
  private int getBinIndex (
    int latRing,
    int lonBin
  ) {

    return (binIndexAtLatitudeRing[latRing] + lonBin);

  } // getBinIndex
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of bin indices adjacent to the specified bin.
   *
   * @param latRing the latitude ring to use in the range [0 .. latRings-1].
   * @param lonBin the longitude bin to use in the range [0 .. binsAtLatitudeRing[latRing]-1].
   *
   * @return the array of bin indices adjacent to that specified.
   */
  private int[] getAdjacentBins (
    int latRing,
    int lonBin
  ) {

    // Get left and right bins
    // -----------------------
    int lonBinLeft = (lonBin == 0 ? binsAtLatitudeRing[latRing]-1 : lonBin-1);
    int lonBinRight = (lonBin == binsAtLatitudeRing[latRing]-1 ? 0 : lonBin+1);

    // Get edges of left and right bins
    // --------------------------------
    double lonBinLeftEdge = lonBinLeft * lonDegreesPerBinAtLatitudeRing[latRing] + EPSILON - 180;
    double lonBinRightEdge = (lonBinRight+1) * lonDegreesPerBinAtLatitudeRing[latRing] - EPSILON - 180;

    // Get bins above
    // --------------
    int upperBins;
    int lonBinUpperLeft;
    int lonBinUpperRight;

    if (latRing == latRings-1) {
      upperBins = 0;
      lonBinUpperLeft = 0;
      lonBinUpperRight = 0;
    } // if
    
    else {
    
      lonBinUpperLeft = getLonBin (latRing+1, lonBinLeftEdge);
      lonBinUpperRight = getLonBin (latRing+1, lonBinRightEdge);

      int upperBin = lonBinUpperLeft;
      upperBins = 1;
      while (upperBin != lonBinUpperRight) {
        upperBins++;
        upperBin = (upperBin+1) % binsAtLatitudeRing[latRing+1];
      } // while
      
    } // else

    // Get bins below
    // --------------
    int lowerBins;
    int lonBinLowerLeft;
    int lonBinLowerRight;
    
    if (latRing == 0) {
      lowerBins = 0;
      lonBinLowerLeft = 0;
      lonBinLowerRight = 0;
    } // if

    else {
    
      lonBinLowerLeft = getLonBin (latRing-1, lonBinLeftEdge);
      lonBinLowerRight = getLonBin (latRing-1, lonBinRightEdge);

      int lowerBin = lonBinLowerLeft;
      lowerBins = 1;
      while (lowerBin != lonBinLowerRight) {
        lowerBins++;
        lowerBin = (lowerBin+1) % binsAtLatitudeRing[latRing-1];
      } // while

    } // else

    // Make a full list of bins
    // ------------------------
    int[] adjacent = new int[upperBins + 3 + lowerBins];
    int bin = 0;

    for (int i = 0; i < upperBins; i++)
      adjacent[bin++] = getBinIndex (latRing+1, (lonBinUpperLeft+i) % binsAtLatitudeRing[latRing+1]);

    adjacent[bin++] = getBinIndex (latRing, lonBinLeft);
    adjacent[bin++] = getBinIndex (latRing, lonBin);
    adjacent[bin++] = getBinIndex (latRing, lonBinRight);

    for (int i = 0; i < lowerBins; i++)
      adjacent[bin++] = getBinIndex (latRing-1, (lonBinLowerLeft+i) % binsAtLatitudeRing[latRing-1]);

    return (adjacent);

  } // getAdjacentBins
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the nearest location entry to the specified location.
   *
   * @param loc the location to search for.
   *
   * @return the nearest entry found or null for none.
   */
  public Entry nearest (
    EarthLocation loc
  ) {

    return (nearest (loc, null));

  } // nearest
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the nearest location entry to the specified location using a
   * context obtained from {@link #getContext}.
   *
   * @param loc the location to search for.
   * @param context the context to use for searching, or null for none.
   *
   * @return the nearest entry found or null for none.
   */
  public Entry nearest (
    EarthLocation loc,
    Object context
  ) {

    Entry nearestEntry = null;

    // Get bin coordinates
    // -------------------
    int latRing = getLatRing (loc.lat);
    int lonBin = getLonBin (latRing, loc.lon);

    if (latRing != -1 && lonBin != -1) {

      // Get adjacent bins to search
      // ---------------------------
      int binIndex = getBinIndex (latRing, lonBin);
      int[] adjacent;
      if (context != null) {
        SearchContext searchContext = (SearchContext) context;
        adjacent = searchContext.binAdjacencyMap.get (binIndex);
        if (adjacent == null) {
          adjacent = getAdjacentBins (latRing, lonBin);
          searchContext.binAdjacencyMap.put (binIndex, adjacent);
        } // if
      } // if
      else {
        adjacent = getAdjacentBins (latRing, lonBin);
      } // else

      // Search each bin data
      // --------------------
      double[] dist2 = null;
      double minDist2 = Double.MAX_VALUE;
      double[] searchCoords = null;
      boolean isInitialized = false;

      for (int i = 0; i < adjacent.length; i++) {
        BinData binData = binDataMap.get (adjacent[i]);
        if (binData != null) {
        
          // Initialize search variables
          // ---------------------------
          if (!isInitialized) {
            searchCoords = new double[3];
            dist2 = new double[1];
            loc.computeECF (searchCoords);
            isInitialized = true;
          } // if

          // Get nearest entry in bin data
          // -----------------------------
          Entry entry = binData.nearest (searchCoords, dist2);
          if (dist2[0] < minDist2) {
            minDist2 = dist2[0];
            nearestEntry = entry;
          } // if

        } // if
      } // for

    } // if

    return (nearestEntry);

  } // nearest
  
  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   *
   * @throws Exception if an error occurred.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (EarthLocationSet.class);

    // ------------------------->

    logger.test ("constructor");
    EarthLocationSet<String> locationSet = new EarthLocationSet<> (2);
    logger.passed();

    // ------------------------->

    {
    
    logger.test ("insert -- North Pole)");

    int locations = 10000;
    List<EarthLocation> locList = new ArrayList<>();
    for (int i = 0; i < locations; i++) {
      double lat = 80 + Math.random() * 10;
      double lon = Math.random() * 360;
      EarthLocation loc = new EarthLocation (lat, lon);
      locationSet.insert (loc, Integer.toString (i));
      locList.add (loc);
    } // for

    logger.passed();

    // ------------------------->

    logger.test ("nearest");

    boolean notFound = false;
    Object context = locationSet.getContext();
    for (EarthLocation loc : locList) {
      EarthLocationSet<String>.Entry entry = locationSet.nearest (loc, context);
      if (entry == null || entry.loc != loc) {
        notFound = true;
        break;
      } // if
    }// for
    assert (notFound != true);

    logger.passed();

    }
    
    locationSet.clear();

    // ------------------------->

    {
    
    logger.test ("insert -- South Pole");

    int locations = 10000;
    List<EarthLocation> locList = new ArrayList<>();
    for (int i = 0; i < locations; i++) {
      double lat = -80 - Math.random() * 10;
      double lon = Math.random() * 360;
      EarthLocation loc = new EarthLocation (lat, lon);
      locationSet.insert (loc, Integer.toString (i));
      locList.add (loc);
    } // for

    logger.passed();

    // ------------------------->

    logger.test ("nearest");

    long startTime = System.nanoTime();

    boolean notFound = false;
    Object context = locationSet.getContext();
    for (EarthLocation loc : locList) {
      EarthLocationSet<String>.Entry entry = locationSet.nearest (loc, context);
      if (entry == null || entry.loc != loc) {
        notFound = true;
        break;
      } // if
    }// for

    long elapsedNanos = System.nanoTime() - startTime;

    assert (notFound != true);
    logger.passed();

    LOGGER.fine ("Nearest method test results (South Pole):"); 
    LOGGER.fine ("Total elapsed time = " + elapsedNanos + " ns" + " (" + (elapsedNanos*1e-9) + " s)");
    LOGGER.fine ("Time per call to nearest = " + (elapsedNanos/locations) + " ns");

    }
    
    locationSet.clear();

    // ------------------------->

    {
    
    logger.test ("insert -- Equator, Prime Meridian");

    int locations = 10000;
    List<EarthLocation> locList = new ArrayList<>();
    for (int i = 0; i < locations; i++) {
      double lat = Math.random() * 10;
      double lon = Math.random() * 10;
      EarthLocation loc = new EarthLocation (lat, lon);
      locationSet.insert (loc, Integer.toString (i));
      locList.add (loc);
    } // for

    logger.passed();

    // ------------------------->

    long startTime = System.nanoTime();

    logger.test ("nearest");

    boolean notFound = false;
    Object context = locationSet.getContext();
    for (EarthLocation loc : locList) {
      EarthLocationSet<String>.Entry entry = locationSet.nearest (loc, context);
      if (entry == null || entry.loc != loc) {
        notFound = true;
        break;
      } // if
    } // for

    long elapsedNanos = System.nanoTime() - startTime;

    assert (notFound != true);
    logger.passed();
    
    LOGGER.fine ("Nearest method test results (Equator, Prime Meridian):"); 
    LOGGER.fine ("Total elapsed time = " + elapsedNanos + " ns" + " (" + (elapsedNanos*1e-9) + " s)");
    LOGGER.fine ("Time per call to nearest = " + (elapsedNanos/locations) + " ns");

    }
    
    locationSet.clear();
    
    // ------------------------->

    {
    
    logger.test ("insert -- Equator, Date line");

    int locations = 10000;
    List<EarthLocation> locList = new ArrayList<>();
    for (int i = 0; i < locations; i++) {
      double lat = Math.random() * 10;
      double lon = 175 + Math.random() * 10;
      EarthLocation loc = new EarthLocation (lat, lon);
      locationSet.insert (loc, Integer.toString (i));
      locList.add (loc);
    } // for

    logger.passed();

    // ------------------------->

    logger.test ("nearest");

    boolean notFound = false;
    Object context = locationSet.getContext();
    for (EarthLocation loc : locList) {
      EarthLocationSet<String>.Entry entry = locationSet.nearest (loc, context);
      if (entry == null || entry.loc != loc) {
        notFound = true;
        break;
      } // if
    }// for
    assert (notFound != true);

    logger.passed();

    }
    
    locationSet.clear();

  } // main

  ////////////////////////////////////////////////////////////

} // EarthLocationSet class

////////////////////////////////////////////////////////////////////////
