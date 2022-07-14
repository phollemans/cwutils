////////////////////////////////////////////////////////////////////////
/*

     File: BinnedGSHHSLineReader.java
   Author: Peter Hollemans
     Date: 2006/06/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/**
 * <p>The <code>BinnedGSHHSLineReader</code> class reads Global
 * Self-consistent Hierarchical High-resolution Shorelines
 * (GSHHS) border and river data in the binned format provided
 * with the Generic Mapping Tools (GMT).  For source code and
 * data files, see:</p>
 * <blockquote>
 *   http://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html<br>
 *   http://gmt.soest.hawaii.edu
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class BinnedGSHHSLineReader
  extends LineFeatureSource {

  // Variables	
  // ---------

  /** The minimum allowed hierarchical level. */
  private int minLevel;

  /** The maximum allowed hierarchical level. */
  private int maxLevel;

  /** The database name. */
  protected String database;

  /** The database ID. */
  protected int sdID;

  /** The segment level ID. */
  protected int segmentLevelID;

  /** The segment points ID. */
  protected int segmentPointsID;

  /** The relative longitude point ID. */
  protected int dxID;

  /** The relative latitude point ID. */
  protected int dyID;

  /** The multiplier for converting scaled segment units to degrees. */
  protected double multiplier;

  /** The bin size in degrees. */
  protected double binSize;

  /** The number of bins in the longitude direction. */
  protected int lonBins;

  /** The number of bins in the latitude direction. */
  protected int latBins;

  /** The total number of bins. */
  protected int totalBins;

  /** The total number of points. */
  protected int totalPoints;

  /** The total number of segments. */
  protected int totalSegments;

  /** The index of the first segment in each bin. */
  protected int[] firstSegment;

  /** The number of segments in each bin. */
  protected short[] numSegments;

  /** The starting point for each segment. */
  protected int[] segmentStart;

  /** The level of each feature in the list. */
  private List levelList = new ArrayList();

  ////////////////////////////////////////////////////////////

  /** Gets a bin index using the specified earth location. */
  public int getBinIndex (
    EarthLocation loc
  ) {

    int latBin = (int) Math.floor ((90 - loc.lat) / binSize);
    if (latBin == 180) latBin = 179;
    int lonBin = (int) Math.floor ((loc.lon < 0 ? loc.lon + 360 : loc.lon) 
      / binSize);
    return (latBin*lonBins + lonBin);

  } // getBinIndex

  ////////////////////////////////////////////////////////////

  /**
   * The bin class acts as a container for GSHHS segments.  Each
   * bin contains a number of segments and a corner location.
   * Bin indexing starts at 0 at (90N, 0E) and proceeds west to
   * east, north to south.
   */
  public class Bin {

    // Variables
    // ---------

    /** The bin index. */
    private int index;

    /** The bin south-west corner. */
    private EarthLocation corner;

    /** The currently selected bin segments. */
    private List segments;

    ////////////////////////////////////////////////////////

    /** Gets the number of segments. */
    public int getSegments () { return (segments.size()); }

    ////////////////////////////////////////////////////////

    /** Gets the specified segment. */
    public Segment getSegment (int index) { 

      return ((Segment) segments.get (index)); 

    } // getSegment

    ////////////////////////////////////////////////////////

    /** Gets the south-west corner earth location. */
    public EarthLocation getCorner () { return (corner); }

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new bin from the specified index.  The segments
     * are read into the bin using the current maximum
     * hierarchical level.
     *
     * @param binIndex the bin index.
     *
     * @see #getBinIndex
     */
    public Bin (
      int binIndex
    ) throws IOException {

      // Set index
      // ---------
      index = binIndex;

      // Calculate south-west corner
      // ---------------------------
      int latBin = index / lonBins;
      int lonBin = index % lonBins;
      corner = new EarthLocation (90 - (latBin+1)*binSize, lonBin*binSize);

      // Create new segments vector
      // --------------------------
      segments = new ArrayList();
      if (numSegments[binIndex] == 0) return;

      // Set bin access hint
      // -------------------
      setBinHint (binIndex);

      // Read segment information
      // ------------------------
      short[] segmentLevel = new short[numSegments[binIndex]];
      short[] segmentPoints = new short[numSegments[binIndex]];
      int[] start = new int[] {firstSegment[binIndex]};
      int[] count = new int[] {numSegments[binIndex]};
      readData (segmentLevelID, start, count, segmentLevel);
      readData (segmentPointsID, start, count, segmentPoints);

      // Read all points for bin
      // -----------------------
      int binPoints = 0;
      for (int i = 0; i < numSegments[binIndex]; i++)
        binPoints += segmentPoints[i];
      start[0] = segmentStart[firstSegment[binIndex]];
      count[0] = binPoints;
      short[] dxAll = new short[binPoints];
      readData (dxID, start, count, dxAll);
      short[] dyAll = new short[binPoints];
      readData (dyID, start, count, dyAll);

      // Create segments
      // ---------------
      for (int i = 0; i < numSegments[binIndex]; i++) {

        // Get segment data
        // ----------------
        int points = segmentPoints[i];
        byte level = (byte) segmentLevel[i];
        int startPoint = segmentStart[firstSegment[binIndex]+i] - 
          segmentStart[firstSegment[binIndex]];
        short[] dx = new short[points];
        System.arraycopy (dxAll, startPoint, dx, 0, points);
        short[] dy = new short[points];
        System.arraycopy (dyAll, startPoint, dy, 0, points);
        
        // Create segment and add to list
        // ------------------------------
        Segment segment = new Segment (level, dx, dy);
        segments.add (segment);

      } // for

    } // Bin

    ////////////////////////////////////////////////////////

    /** 
     * Gets an earth vector for the specified segment.
     *
     * @param index the index of the segment to convert.
     *
     * @return the new earth vector.
     */
    public LineFeature getLineFeature (
      int index
    ) {

      return (getSegment (index).getLineFeature());

    } // getLineFeature

    ////////////////////////////////////////////////////////

    /**
     * The <code>Segment</code> class acts as a container for
     * GSHHS line segment data.  Segments are part of a larger
     * GSHHS line feature that has been divided into shorter
     * sections based on their respective bins.
     */
    public class Segment {

      // Variables
      // ---------

      /** 
       * The segment level.  The interpretation of the level
       * depends on the data file.
       */
      private byte level;

      /** The array of scaled longitudes relative to the bin corner. */
      private short[] dx;

      /** The array of scaled latitudes relative to the bin corner. */
      private short[] dy;

      ////////////////////////////////////////////////////

      /** Returns a string representation of this segment. */
      public String toString () {
    
        String str = "Segment[";
        str += "level=" + level + ",";
        str += "dx=" + dx + ",";
        str += "dy=" + dy + ",";
        str += "]";
        return (str);

      } // toString

      ////////////////////////////////////////////////////

      /** Gets the segment level. */
      public byte getLevel () { return (level); }

      ////////////////////////////////////////////////////

      /** Gets the earth vector for this segment. */
      public LineFeature getLineFeature () {

        LineFeature vector = new LineFeature();
        for (int i = 0; i < dx.length; i++) {
          if (i > 0 && (dx[i] == dx[i-1]) && (dy[i] == dy[i-1])) continue;
          EarthLocation loc = new EarthLocation (
            corner.lat + (dy[i] & 0xffff)*multiplier,
            corner.lon + (dx[i] & 0xffff)*multiplier
          );
          vector.add (loc);
        } // for

        return (vector);

      } // getLineFeature
      
      ////////////////////////////////////////////////////

      /** Gets the raw segment longitude offsets. */
      public short[] getDx () { return (dx); }

      ////////////////////////////////////////////////////

      /** Gets the raw segment latitude offsets. */
      public short[] getDy () { return (dy); }

      ////////////////////////////////////////////////////

      /**
       * Creates a new segment from the specified parameters.
       * 
       * @param level the segment level: land, lake, island, or pond.
       * @param dx the array of scaled longitudes relative to the bin
       * corner.
       * @param dy the array of scaled latitudes relative to the bin
       * corner.
       */
      public Segment (
        byte level,
        short[] dx,
        short[] dy
      ) {

        // Initialize
        // ----------
        this.level = level;
        this.dx = dx;
        this.dy = dy;

      } // Segment

    } // Segment class

    ////////////////////////////////////////////////////////

  } // Bin class

  ////////////////////////////////////////////////////////////

  /**
   * Sets the hierarchical level range useds in line segment
   * rendering.  By default all line segments are rendered,
   * regardless of level.
   *
   * @param minLevel the maximum hierarchical level.
   * @param maxLevel the maximum hierarchical level.
   */
  public void setLevelRange (
    int minLevel,
    int maxLevel
  ) {

    this.minLevel = minLevel;
    this.maxLevel = maxLevel;

  } // setMaxLevel

  ////////////////////////////////////////////////////////////

  /** 
   * Reads data for the specified variable ID. 
   *
   * @param sdsid the identifier for the variable.
   * @param start the starting data index for reading.
   * @param count the number of data values to read.
   * @param data the data array to fill with values.
   *
   * @throws IOException if an error occurred reading the data.
   */
  protected abstract void readData (
    int sdsid,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Reads data for the specified variable name.
   *
   * @param var the name of the variable.
   * @param start the starting data index for reading.
   * @param count the number of data values to read.
   * @param data the data array to fill with values.
   *
   * @throws IOException if an error occurred reading the data.
   */
  protected abstract void readData (
    String var,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable ID for the specified variable. 
   *
   * @param var the variable name to retrieve an ID.
   *
   * @return the variable ID.
   *
   * @throws IOException if an error occurred retrieving the ID.
   */
  protected abstract int selectData (
    String var
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Opens the data file and returns the file ID.
   *
   * @param name the data file name.
   *
   * @throws IOException if an error occurred opening the file.
   */
  protected abstract int openFile (
    String name
  ) throws IOException;

  ////////////////////////////////////////////////////////////

  /** Creates a new reader with no initialization. */
  protected BinnedGSHHSLineReader () { }

  ////////////////////////////////////////////////////////////

  /**
   * Reads data about the entire file, including binSize,
   * multiplier, lonBins, latBins, totalBins, firstSegment,
   * numSegments, and segmentStart.
   *
   * @throws IOException if an error occurred reading the data.
   */
  protected abstract void getGlobalData () throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Initializes this reader using the specified database.
   *
   * @see BinnedGSHHSReaderFactory#getDatabaseName
   */
  protected void init (
    String name
  ) throws IOException {

    // Initialize
    // ----------
    minLevel = Integer.MIN_VALUE;
    maxLevel = Integer.MAX_VALUE;
    database = name;
    sdID = openFile (name);

    // Get global info
    // ---------------
    getGlobalData();

    // Get segment data IDs
    // --------------------
    segmentLevelID = selectData ("Hierarchial_level_of_a_segment");
    segmentPointsID = selectData ("N_points_for_a_segment");

    // Get point data IDs
    // ------------------
    dxID = selectData ("Relative_longitude_from_SW_corner_of_bin");
    dyID = selectData ("Relative_latitude_from_SW_corner_of_bin");

  } // init

  ////////////////////////////////////////////////////////////

  /**
   * Sets a hint that subsequent bin data access is about to occur for
   * a list of bins.  This may be used by child classes to increase
   * the speed of bin data access.
   *
   * @param indexList the list of bin indices to be accessed sorted in
   * increasing order.
   */
  protected void setBinListHint (
    List indexList
  ) {

    // Do nothing

  } // setBinListHint

  ////////////////////////////////////////////////////////////

  /**
   * Sets a hint that subsequent bin data access is about to occur.
   * This may be used by child classes to increase the speed of bin
   * data access.
   *
   * @param index the bin that data access will be performed for next.
   */
  protected void setBinHint (
    int index
  ) {

    // Do nothing

  } // setBinHint

  ////////////////////////////////////////////////////////////

  protected void select () throws IOException {

    // Initialize
    // ----------
    TreeSet indices = new TreeSet (getBinIndices (area));
    Iterator iter = indices.iterator();
    featureList.clear();
    levelList.clear();
    setBinListHint (new ArrayList (indices));

    // Loop over each bin
    // ------------------
    while (iter.hasNext()) {

      // Read bin data 
      // -------------
      int index = ((Integer)iter.next()).intValue();
      Bin bin = new Bin (index);

      // Add segments to list
      // --------------------
      for (int i = 0; i < bin.getSegments(); i++) {
        Bin.Segment segment = bin.getSegment (i); 
        featureList.add (segment.getLineFeature());
        levelList.add (Integer.valueOf (segment.getLevel()));
      } // for

    } // while

  } // select

  ////////////////////////////////////////////////////////////

  public Iterator iterator () {

    // Create list of features at current level range
    // ----------------------------------------------
    List levelFeatures = new ArrayList();
    for (int i = 0; i < featureList.size(); i++) {
      int level = ((Integer) levelList.get (i)).intValue();
      if (level < minLevel || level > maxLevel) continue;
      levelFeatures.add (featureList.get (i));
    } // for

    return (levelFeatures.iterator());

  } // iterator

  ////////////////////////////////////////////////////////////

  /** Gets the total number of bins. */
  public int getBins () { return (totalBins); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the bin indices containing the specified earth area. 
   * 
   * @param area the earth area.
   *
   * @return a collection of bin indices as <code>Integer</code> objects.
   */
  public Collection getBinIndices (
    EarthArea area
  ) {

    // Initialize
    // ----------
    HashSet binIndices = new HashSet();
    Iterator iter = area.getIterator();

    // Loop over each square
    // ---------------------
    while (iter.hasNext()) {

      // Get bin index
      // -------------
      int[] square = (int[]) iter.next(); 
      Integer binIndex = Integer.valueOf (getBinIndex (new EarthLocation (
        square[0]+0.5, square[1]+0.5)));

      // Add index to hash set
      // ---------------------
      if (!binIndices.contains (binIndex))
        binIndices.add (binIndex);

    } // while

    return (binIndices);

  } // getBinIndices

  ////////////////////////////////////////////////////////////

  /** Gets the database name currently being used for selection. */
  public String getDatabase() { return (database); }

  ////////////////////////////////////////////////////////////

} // BinnedGSHHSLineReader class

////////////////////////////////////////////////////////////////////////
