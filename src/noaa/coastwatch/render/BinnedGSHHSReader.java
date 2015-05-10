////////////////////////////////////////////////////////////////////////
/*
     FILE: BinnedGSHHSReader.java
  PURPOSE: To provide GSHHS coastline data from binned data files.
   AUTHOR: Peter Hollemans
     DATE: 2002/12/29
  CHANGES: 2003/05/11, PFH
           - changed to extend PolygonFeatureReader
           - added Bin.getPolygonFeatures
           - modified various Segment methods
           2003/05/21, PFH, fixed bin boundary fill problems
           2003/10/04, PFH, fixed URL to path conversion problem on Win32
           2003/11/22, PFH, fixed Javadoc comments
           2003/12/10, PFH, changed PolygonFeatureReader to 
             PolygonFeatureSource
           2003/12/28, PFH, modified to use IOServices.getFilePath()
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/10/15, PFH, fixed coastline bugs and updated to use GSHHS 1.3
           2005/05/26, PFH, changed vectorList to featureList
           2006/06/10, PFH
           - updated to allow for non-HDF child class
           - moved some functionality to BinnedGSHHSReaderFactory
           - moved HDF calls to HDFGSHHSReader
           2006/11/16, PFH, added get methods for polygonRendering and minArea

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import noaa.coastwatch.render.LineFeature;
import noaa.coastwatch.render.PolygonFeature;
import noaa.coastwatch.render.PolygonFeatureSource;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/**
 * The binned GSHHS reader class reads Global Self-consistent
 * Hierarchical High-resolution Shorelines (GSHHS) data from the
 * binned format provided with the Generic Mapping Tools (GMT).  For
 * source code and data files, see:
 * <blockquote>
 *   http://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html<br>
 *   http://gmt.soest.hawaii.edu
 * </blockquote><p>
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public abstract class BinnedGSHHSReader
  extends PolygonFeatureSource {

  // Variables	
  // ---------

  /** The minimum allowed polygon area in km^2. */
  private double minArea;

  /** The database name. */
  protected String database;

  /** The database ID. */
  protected int sdID;

  /** The segment info ID. */
  protected int segmentInfoID;

  /** The segment area ID. */
  protected int segmentAreaID;

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

  /** The info for each bin. */
  protected short[] binInfo;

  /** The starting point for each segment. */
  protected int[] segmentStart;

  /** The polygon rendering flag, true if polygon rendering is required. */
  private boolean polygonRendering;

  /** The high resolution flag, true if we are using the 0.2 km database. */
  private boolean isHigh;

  ////////////////////////////////////////////////////////////

  /** Gets a bin index using the specified Earth location. */
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
   * The bin class acts as a container for GSHHS shore segments.  Each
   * bin contains a number of segments and a corner location.  Bin
   * indexing starts at 0 at (90N, 0E) and proceeds west to east,
   * north to south.
   */
  public class Bin {

    // Constants
    // ---------
    /** The south-west corner. */
    public static final int SOUTH_WEST = 0;

    /** The south-east corner. */
    public static final int SOUTH_EAST = 1;

    /** The north-east corner. */
    public static final int NORTH_EAST = 2;

    /** The north-west corner. */
    public static final int NORTH_WEST = 3;

    /** The south side constant. */
    public final static int SOUTH = 0;

    /** The east side constant. */
    public final static int EAST = 1;

    /** The north side constant. */
    public final static int NORTH = 2;

    /** The west side constant. */
    public final static int WEST = 3;

    // Variables
    // ---------

    /** The bin index. */
    private int index;

    /** The bin south-west corner. */
    private EarthLocation corner;

    /** The currently selected bin segments. */
    private List segments;

    /** The bin corners: SW, SE, NE, NW. */
    private EarthLocation[] corners;

    /** The bin corner levels: SW, SE, NE, NW. */
    private byte[] cornerLevels;

    /** The bin minimum corner level. */
    private byte minLevel;

    ////////////////////////////////////////////////////////
 
    /** Creates a segment sorting key. */
    private Integer getSegmentKey (
      int side,
      int offset
    ) {
  
      return (new Integer ((side << 16) + offset));

    } // getSegmentKey

    ////////////////////////////////////////////////////////

    /** Translates a segment sorting key into an Earth location. */
    private EarthLocation translateSegmentKey (
      Integer key
    ) {

      // Get key side and offset
      // -----------------------
      int keyValue = key.intValue();
      if (keyValue < 0) keyValue *= -1;
      int side = keyValue >> 16;
      int offset = keyValue & 0xffff;
      if (side == NORTH || side == WEST) 
        offset = 65535 - offset;

      // Create Earth location
      // ---------------------
      double lat = corner.lat, lon = corner.lon;
      switch (side) {
      case SOUTH: 
        lon = corner.lon + offset*multiplier;
        break;
      case EAST: 
        lat += offset*multiplier; 
        lon += binSize;
        break;
      case NORTH: 
        lat += binSize;
        lon += offset*multiplier;
        break;
      case WEST: 
        lat += offset*multiplier;
        break;
      } // switch

      return (new EarthLocation (lat, lon));

    } // translateSegmentKey

    ////////////////////////////////////////////////////////

    /** Finds a successor segment sorting key. */
    private Integer successorSegmentKey (
      Integer key,
      boolean reverse
    ) {

      int keyValue = key.intValue();
      if (reverse && keyValue == 0) return (getSegmentKey (WEST, 65535));
      else if (keyValue == 0x3ffff) return (getSegmentKey (SOUTH, 0));
      else return (new Integer (keyValue + (reverse ? -1 : 1)));

    } // successorSegmentKey

    ////////////////////////////////////////////////////////

    /** Gets the number of segments. */
    public int getSegments () { return (segments.size()); }

    ////////////////////////////////////////////////////////

    /** Gets the specified segment. */
    public Segment getSegment (int index) { 

      return ((Segment) segments.get (index)); 

    } // getSegment

    ////////////////////////////////////////////////////////

    /** Gets the south-west corner Earth location. */
    public EarthLocation getCorner () { return (corner); }

    ////////////////////////////////////////////////////////

    /** Gets the specified corner Earth location. */
    public EarthLocation getCorner (
      int index
    ) {

      return (corners[index]);

    } // getCorner

    ////////////////////////////////////////////////////////

    /** Gets the specified corner level. */
    public byte getCornerLevel (
      int index
    ) {

      return (cornerLevels[index]);

    } // getCornerLevel

    ////////////////////////////////////////////////////////

    /** Gets the minimum bin corner level. */
    public byte getMinimumLevel () { return (minLevel); }

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new bin from the specified index.  The segments
     * are read into the bin using the current minimum area.
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

      // Calculate other corners
      // -----------------------
      corners = new EarthLocation[4];
      corners[SOUTH_WEST] = new EarthLocation (corner.lat, corner.lon);
      corners[SOUTH_EAST] = new EarthLocation (corner.lat, corner.lon+binSize);
      corners[NORTH_EAST] = new EarthLocation (corner.lat+binSize, 
        corner.lon+binSize);
      corners[NORTH_WEST] = new EarthLocation (corner.lat+binSize, corner.lon);

      // Set corner levels
      // -----------------
      cornerLevels = new byte[4];
      cornerLevels[0] = (byte) (binInfo[binIndex] >>> 9);
      cornerLevels[1] = (byte) ((binInfo[binIndex] >>> 6) & 0x7);
      cornerLevels[2] = (byte) ((binInfo[binIndex] >>> 3) & 0x7);
      cornerLevels[3] = (byte) (binInfo[binIndex] & 0x7);
      minLevel = cornerLevels[0];
      for (int i = 1; i < 4; i++) 
        if (cornerLevels[i] < minLevel) minLevel = cornerLevels[i];

      // Create new segments vector
      // --------------------------
      segments = new ArrayList();
      if (numSegments[binIndex] == 0) return;

      // Set bin access hint
      // -------------------
      setBinHint (binIndex);

      // Read segment information
      // ------------------------
      int[] segmentInfo = new int[numSegments[binIndex]];
      int[] segmentArea = new int[numSegments[binIndex]];
      int[] start = new int[] {firstSegment[binIndex]};
      int[] count = new int[] {numSegments[binIndex]};
      readData (segmentInfoID, start, count, segmentInfo);
      readData (segmentAreaID, start, count, segmentArea);

      // Read all points for bin
      // -----------------------
      int binPoints = 0;
      for (int i = 0; i < numSegments[binIndex]; i++)
        binPoints += (int) (segmentInfo[i] >>> 9);
      start[0] = segmentStart[firstSegment[binIndex]];
      count[0] = binPoints;
      short[] dxAll = new short[binPoints];
      readData (dxID, start, count, dxAll);
      short[] dyAll = new short[binPoints];
      readData (dyID, start, count, dyAll);

      // Create segments
      // ---------------
      for (int i = 0; i < numSegments[binIndex]; i++) {

        // Check polygon area
        // ------------------
        double area = segmentArea[i]/10.0;
        if (minArea > 0 && area < minArea) continue;
    
        // Get segment data
        // ----------------
        int points = (int) (segmentInfo[i] >>> 9);
        byte level = (byte) ((segmentInfo[i] >>> 6) & 0x7);
        byte entry = (byte) ((segmentInfo[i] >>> 3) & 0x7);
        byte exit = (byte) (segmentInfo[i] & 0x7);
        int startPoint = segmentStart[firstSegment[binIndex]+i] - 
          segmentStart[firstSegment[binIndex]];
        short[] dx = new short[points];
        System.arraycopy (dxAll, startPoint, dx, 0, points);
        short[] dy = new short[points];
        System.arraycopy (dyAll, startPoint, dy, 0, points);
        
        // Filter segment data
        // -------------------
        /** 
         * There is an error in the GSHHS high resolution binned file.
         * One of the segments is recorded as being at the wrong
         * level, and consequently has the wrong winding order as
         * well.  We do a test for that segment here and rearrange it
         * if found.  The specific segment:
         *
         *   database = HIGH
         *   corner.lat = 6
         *   corner.lon = 0
         *   level = 2 (should be 3, it is an island in a lake)
         *   entry = 3
         *   exit = 3
         *   area == 11.8
         */
        if (isHigh && binIndex == 7380 && i == 11) {
          level = 3;
          short stmp;
          for (int j = 0; j < dx.length/2; j++) {
            stmp = dx[j]; dx[j] = dx[dx.length-1-j]; dx[dx.length-1-j] = stmp;
            stmp = dy[j]; dy[j] = dy[dy.length-1-j]; dy[dy.length-1-j] = stmp;
          } // for
        } // if

        // Create segment and add to list
        // ------------------------------
        Segment segment = new Segment (level, entry, exit, area, dx, dy);
        segments.add (segment);

      } // for

    } // Bin

    ////////////////////////////////////////////////////////

    /** 
     * Gets an Earth vector for the specified segment.
     *
     * @param index the index of the segment to convert.
     *
     * @return the new Earth vector.
     */
    public LineFeature getLineFeature (
      int index
    ) {

      return (getSegment (index).getLineFeature());

    } // getLineFeature

    ////////////////////////////////////////////////////////

    /** Compares segments by level. */
    private class SegmentLevelComparator 
      implements Comparator {

      public int compare (Object o1, Object o2) {
        return (((Segment) o1).getLevel() - ((Segment) o2).getLevel());
      } // compare

    } // SegmentLevelComparator

    ////////////////////////////////////////////////////////

    /** Gets the next key in a circular sorted map. */
    private Object getNextKey (
      SortedMap map,
      Object key
    ) { 

      SortedMap tail = map.tailMap (key);
      if (tail.isEmpty()) tail = map;
      return (tail.firstKey());

    } // getNextKey

    ////////////////////////////////////////////////////////

    /** Gets the next value in a circular sorted set. */
    private Object getNextValue (
      SortedSet set,
      Object value
    ) { 

      SortedSet tail = set.tailSet (value);
      if (tail.isEmpty()) tail = set;
      return (tail.first());

    } // getNextValue

    ////////////////////////////////////////////////////////

    /**
     * Gets the segments in this bin as a list of closed Earth
     * polygons.  The convention used is for land polygons to have the
     * counter-clockwise winding direction and water polygons to have
     * clockwise winding.  The polygons in the list are specified in
     * rendering order.
     *
     * @return a list of Earth polygon objects based on the segments
     * in this bin.
     */
    public List getPolygonFeatures () {

      // Sort segments by level
      // ----------------------
      /** 
       * We do this because we want to deal with each level
       * individually.  The level represents the depth of the polygon
       * within the hierarchy: land, lake, island in lake, pond in
       * island.  First all the land polygons should be dealt with,
       * then all the lake polygons, and so on.
       */
      List levelSortedSegments = new LinkedList (segments);
      Collections.sort (levelSortedSegments, new SegmentLevelComparator());

      // Separate open segments from closed segments
      // -------------------------------------------
      /**
       * We do this because we only really have to do complicated
       * things with the open segments.  Any closed segments we can
       * just add to the polygon list as-is.  The open segments need
       * to be joined together to form complete polygons, possibly by
       * adding parts of the edge of the bin and its corners.
       */
      List openSegments = new ArrayList();
      List closedSegments = new ArrayList();
      Iterator iter = levelSortedSegments.iterator();
      while (iter.hasNext()) {
        Segment segment = (Segment) iter.next();
        if (segment.isClosed()) closedSegments.add (segment);
        else openSegments.add (segment);
      } // while

      // Create sorted sets of perimeter crossing points
      // -----------------------------------------------
      /**
       * Perimeter crossings are where segments enter or exit the bin.
       * We need these so that when we close any open segments to form
       * a polygon, we can trace the edge of the bin and its corners
       * into the polygon.  The tracing of edges is important so that
       * when two adjacent bins are rendered side-by-side, there are
       * no gaps between polygons that share the bin edge.  Perimeter
       * points are also traced when we need a prefill of the land
       * (for example, in the middle of a continent where there is no
       * initial land/ocean polygon).
       */
      TreeSet sortedPerimeterPoints = new TreeSet();
      TreeSet reverseSortedPerimeterPoints = new TreeSet (
        Collections.reverseOrder());
      iter = openSegments.iterator();
      while (iter.hasNext()) {
        Segment segment = (Segment) iter.next();
        Integer entryKey = segment.getEntryKey();
        Integer exitKey = segment.getExitKey();
        sortedPerimeterPoints.add (entryKey);
        sortedPerimeterPoints.add (exitKey);
        reverseSortedPerimeterPoints.add (entryKey);
        reverseSortedPerimeterPoints.add (exitKey);
      } // while

      // Add corners to sorted crossing points
      // -------------------------------------
      for (int i = 0; i < 4; i++) {
        sortedPerimeterPoints.add (getSegmentKey (i, 0));
        reverseSortedPerimeterPoints.add (getSegmentKey (i, 0));
      } // for

      // Create polygons from open segments
      // ----------------------------------
      List polygons = new ArrayList();
      while (openSegments.size() > 0) {

        // Extract segments at current level
        // ---------------------------------
        /**
         * We need to work with any open segments in the list which
         * are all at the same level first.  That way, we can join
         * them together to form closed polygons one by one until
         * there are no open segments at that level.  Then, we do the
         * next level, and so on until all levels have been processed.
         */
        Segment levelSegment = (Segment) openSegments.get (0);
        int level = levelSegment.getLevel();
        List levelSegments = new ArrayList();
        do {
          levelSegments.add (levelSegment);
          openSegments.remove (0);
          if (openSegments.size() == 0) break;
          levelSegment = (Segment) openSegments.get (0);
        } while (levelSegment.getLevel() == level);

        // Set segment orientation values
        // ------------------------------
        /**
         * Segments are oriented in counter-clockwise order for
         * land-enclosing segments and clockwise order for
         * water-enclosing segments.  We need to make some settings
         * here so that the perimeter points are searched in the
         * correct order when connecting segments into polygons.
         */
        int direction;
        Comparator comp;
        TreeSet perimeterPoints;
        boolean reverse;
        if (level == Segment.LAND || level == Segment.ISLAND_IN_LAKE) {
          direction = PolygonFeature.COUNTER_CLOCKWISE;
          comp = null;
          perimeterPoints = sortedPerimeterPoints;
          reverse = false;
        } // if
        else {
          direction = PolygonFeature.CLOCKWISE;
          comp = Collections.reverseOrder();
          perimeterPoints = reverseSortedPerimeterPoints;
          reverse = true;
        } // else
        
        // Sort segments by entry and exit
        // -------------------------------
        /**
         * We need to create sorted maps of segments here, similar to
         * the sorted perimeter point set.  This will allow us to
         * query for the next segment around the perimeter based on
         * its entry or exit point.  The entry and exit keys are
         * integers that are used to sort the segments.
         */
        TreeMap entrySortedSegments = new TreeMap (comp);
        TreeMap exitSortedSegments = new TreeMap (comp); 
        iter = levelSegments.iterator();
        while (iter.hasNext()) {
          Segment segment = (Segment) iter.next();
          entrySortedSegments.put (segment.getEntryKey(), segment);
          exitSortedSegments.put (segment.getExitKey(), segment);
        } // while

        // Create polygons from level segments
        // -----------------------------------
        while (levelSegments.size() > 0) {

          // Start polygon using first segment
          // ---------------------------------
          /**
           * Now we start the polygon by grabbing the first segment
           * from the list of segments at this level and keep looping
           * until we see this same segment again.
           */
          PolygonFeature polygon = new PolygonFeature (direction);
          Segment firstSegment = (Segment) levelSegments.get (0);
          Segment thisSegment = null, nextSegment = firstSegment;

          // Loop until the first segment is reached again
          // ---------------------------------------------
          do {        

            // Add this segment to the polygon
            // -------------------------------
            /**
             * We insert the current segment here, making sure that
             * its starting point isn't an ending point from a
             * previous segment or bin perimeter point already in the
             * polygon.  This helps to avoid duplicate location points
             * which can cause problems in the polygon rendering
             * algorithm used later.
             */
            thisSegment = nextSegment;
            LineFeature thisVector = thisSegment.getLineFeature();
            if (polygon.size() > 0 && thisVector.size() > 0) {
              if (polygon.get (polygon.size()-1).equals (thisVector.get (0)))
                polygon.remove (polygon.size()-1);
            } // if
            polygon.addAll (thisVector);
            levelSegments.remove (thisSegment);

            // Find the next segment
            // ---------------------
            /**
             * Now we search for the next segment to insert.  We look
             * using the exit point for the current segment and ask
             * for the next segment that enters the bin after this one
             * exits.
             */
            Integer thisExitKey = thisSegment.getExitKey();
            Integer nextEntryKey = (Integer) getNextKey (entrySortedSegments,
              thisExitKey);
            nextSegment = (Segment) entrySortedSegments.get (nextEntryKey);
            if (nextSegment == thisSegment && nextSegment != firstSegment) {
              nextEntryKey = firstSegment.getEntryKey();
              nextSegment = firstSegment;
            } // if

            // Trace any perimeter points
            // --------------------------         
            /**
             * If the current segment leaves the bin and somewhere
             * later on, the next segment enters, we need to trace any
             * perimeter points that may occur between the exit and
             * the next enter point.  This includes any perimeter
             * points caused by segment crossings and any bin corner
             * points.
             */
            if (!thisExitKey.equals (nextEntryKey)) {
              Integer nextPerimeterKey = (Integer) getNextValue (
                perimeterPoints, successorSegmentKey (thisExitKey, reverse));
              while (!nextPerimeterKey.equals (nextEntryKey)) {
                polygon.add (translateSegmentKey (nextPerimeterKey));
                nextPerimeterKey = (Integer) getNextValue (perimeterPoints, 
                  successorSegmentKey (nextPerimeterKey, reverse));
              } // while
            } // if

          } while (nextSegment != firstSegment);

          // Add polygon to list
          // -------------------
          polygons.add (polygon);

        } // while

      } // while

      // Create polygons from closed segments
      // ------------------------------------
      /**
       * Here is the easy part, where all the already closed segments
       * are converted to polygons of the correct winding direction.
       */
      iter = closedSegments.iterator();
      while (iter.hasNext()) {
        Segment segment = (Segment) iter.next();
        int direction = (segment.isLand() ? PolygonFeature.COUNTER_CLOCKWISE :
          PolygonFeature.CLOCKWISE);
        PolygonFeature polygon = new PolygonFeature (direction);
        polygon.addAll (segment.getLineFeature());
        polygons.add (polygon);
      } // while

      // Check if land prefill is required
      // ---------------------------------
      /**
       * A land prefill is required when (i) there are no polygons and
       * the bin is entirely either land or island in pond, or (ii)
       * when the minimum segment level found is water-enclosing.
       */
      boolean prefill;
      if (polygons.size() == 0) {
        prefill = (minLevel == Segment.LAND || 
          minLevel == Segment.ISLAND_IN_LAKE);
      } // if
      else {
        int minSegmentLevel = ((Segment)levelSortedSegments.get(0)).getLevel();
        prefill = (minSegmentLevel == Segment.LAKE || 
          minSegmentLevel == Segment.POND_IN_ISLAND);
      } // else

      // Prefill land
      // ------------
      /**
       * Here we create a land prefill polygon by simply tracing out
       * around the bin perimeter points.
       */
      if (prefill) {
        PolygonFeature land = new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE);
        iter = sortedPerimeterPoints.iterator();
        while (iter.hasNext()) {
          Integer key = (Integer) iter.next();
          land.add (translateSegmentKey (key));
        } // while
        polygons.add (0, land);
      } // if

      // Filter for duplicate points
      // ---------------------------
      /**
       * This is a possible alternative to the test above that checks
       * for a duplicate point when adding an open segment to a
       * polygon.  For now though, we only need one of the two tests,
       * and the one above is less costly because it doesn't go
       * through every polygon point.
       */
      /*
      for (int i = 0; i < polygons.size(); i++) {
        PolygonFeature polygon = (PolygonFeature) polygons.get (i);
        for (int j = 1; j < polygon.size(); j++) {
          EarthLocation lastLoc = (EarthLocation) polygon.get (j-1);
          EarthLocation thisLoc = (EarthLocation) polygon.get (j);
          if (thisLoc.equals (lastLoc)) {
            polygon.remove (j);
            j--;
          } // if
        } // for
      } // for
      */

      return (polygons);

    } // getPolygonFeatures

    ////////////////////////////////////////////////////////

    /**
     * The segment class acts as a container for GSHHS shore segment
     * data.  Segments are part of a larger GSHHS polygon that has
     * been divided into shorter sections based on their respective
     * bins.  A segment contains a number of keys to help sort
     * segments based on entry and exit positions.  The entry and exit
     * keys sort the segment based on a counter-clockwise position
     * starting at the south-west corner of the bin.
     */
    public class Segment {

      // Constants
      // ---------
      /** Land level constant. */
      public final static int LAND = 1;

      /** Lake level constant. */
      public final static int LAKE = 2;

      /** Island in lake level constant. */
      public final static int ISLAND_IN_LAKE = 3;

      /** Pond in island in lake level constant. */
      public final static int POND_IN_ISLAND = 4;

      /** The segment closed constant. */
      public final static int CLOSED = 4;

      // Variables
      // ---------
      /** The segment level: land, lake, island, or pond. */
      private byte level;

      /** The segment starting side: north, south, east, west, or closed. */
      private byte entry;

      /** The segment ending side: north, south, east, west, or closed. */
      private byte exit;

      /** The area of the segment polygon in km^2. */
      private double area;

      /** The array of scaled longitudes relative to the bin corner. */
      private short[] dx;

      /** The array of scaled latitudes relative to the bin corner. */
      private short[] dy;

      /** The entry sorting key. */
      private Integer entryKey;

      /** The exit sorting key. */
      private Integer exitKey;

      ////////////////////////////////////////////////////

      /** Returns a string representation of this segment. */
      public String toString () {
    
        String str = "Segment[";
        str += "level=" + level + ",";
        str += "entry=" + entry + ",";
        str += "exit=" + exit + ",";
        str += "area=" + area + ",";
        str += "dx=" + dx + ",";
        str += "dy=" + dy + ",";
        str += "entryKey=" + entryKey + ",";
        str += "exitKey=" + exitKey;
        str += "]";
        return (str);

      } // toString

      ////////////////////////////////////////////////////

      /** Gets the segment level. */
      public byte getLevel () { return (level); }

      ////////////////////////////////////////////////////

      /** Gets the segment entry side. */
      public byte getEntrySide () { return (entry); }
  
      ////////////////////////////////////////////////////

      /** Gets the segment exit side. */
      public byte getExitSide () { return (exit); }

      ////////////////////////////////////////////////////

      /** Gets the segment area in km^2. */
      public double getArea () { return (area); }

      ////////////////////////////////////////////////////

      /** Gets the Earth vector for this segment. */
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

      /** Determines if this segment is closed. */
      public boolean isClosed () { return (entry == CLOSED); }

      ////////////////////////////////////////////////////

      /**
       * Creates a new segment from the specified parameters.
       * 
       * @param level the segment level: land, lake, island, or pond.
       * @param entry the segment starting side: north, south, east,
       * west, or closed.
       * @param exit the segment ending side: north, south, east, west,
       * or closed.
       * @param area the area of the segment polygon in km^2.
       * @param dx the array of scaled longitudes relative to the bin
       * corner.
       * @param dy the array of scaled latitudes relative to the bin
       * corner.
       */
      public Segment (
        byte level,
        byte entry,
        byte exit,
        double area,
        short[] dx,
        short[] dy
      ) {

        // Initialize
        // ----------
        this.level = level;
        this.entry = entry;
        this.exit = exit;
        this.area = area;
        this.dx = dx;
        this.dy = dy;

        // Create entry sorting key
        // ------------------------
        switch (entry) {
        case SOUTH: 
          entryKey = getSegmentKey (entry, dx[0] & 0xffff);
          break;
        case EAST: 
          entryKey = getSegmentKey (entry, dy[0] & 0xffff);
          break;
        case NORTH: 
          entryKey = getSegmentKey (entry, 65535 - (dx[0] & 0xffff)); 
          break;
        case WEST: 
          entryKey = getSegmentKey (entry, 65535 - (dy[0] & 0xffff));
          break;
        default: 
          entryKey = null;
        } // switch

        // Create exit sorting key
        // -----------------------
        switch (exit) {
        case SOUTH: 
          exitKey = getSegmentKey (exit, dx[dx.length-1] & 0xffff);
          break;
        case EAST: 
          exitKey = getSegmentKey (exit, dy[dy.length-1] & 0xffff); 
          break;
        case NORTH: 
          exitKey = getSegmentKey (exit, 65535 - (dx[dx.length-1] & 0xffff));
          break;
        case WEST: 
          exitKey = getSegmentKey (exit, 65535 - (dy[dy.length-1] & 0xffff));
          break;
        default: 
          exitKey = null;
        } // switch

      } // Segment

      ////////////////////////////////////////////////////

      /** Gets the entry sorting key. */
      public Integer getEntryKey () { return (entryKey); }

      ////////////////////////////////////////////////////

      /** Gets the exit sorting key. */
      public Integer getExitKey () { return (exitKey); }

      ////////////////////////////////////////////////////

      /** Determines if this polygon segment encloses land. */
      public boolean isLand () { return (level == LAND || 
        level == ISLAND_IN_LAKE); }

      ////////////////////////////////////////////////////

      /** Determines if this polygon segment encloses water. */
      public boolean isWater () { return (level == LAKE || 
        level == POND_IN_ISLAND); }

      ////////////////////////////////////////////////////

    } // Segment class

    ////////////////////////////////////////////////////////

  } // Bin class

  ////////////////////////////////////////////////////////////

  /**
   * Sets the minimum area used in polygon selection.  Polygons less
   * than the minimum area are not included.  By default the minimum
   * area is -1, which causes all polygons to be selected, regardless
   * of area.
   *
   * @param minArea the minimum area in km^2.  Negative values disable
   * filtering by minimum area.
   */
  public void setMinArea (
    double minArea
  ) {

    this.minArea = minArea;

  } // setMinArea

  ////////////////////////////////////////////////////////////

  /**
   * Gets the minimum area used in polygon selection.
   *
   * @return the minimum area in km^2, or a negative value to
   * select all polygons regardless of area.
   */
  public double getMinArea () { return (minArea); }

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
   * Sets the polygon rendering flag.  By default, the polygon
   * rendering capability is turned off because of the extra work
   * required for assembling segment data into closed polygons.  When
   * off, no polygons are created when the <code>select()</code>
   * method is called, thus no polygons are ever rendered.  When on,
   * the <code>select()</code> call causes a set of polygons to be
   * created from segment data, which may then be rendered via a call
   * to <code>renderPolygons()</code>.
   */
  public void setPolygonRendering (boolean flag) { polygonRendering = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the polygon rendering flag.
   *
   * @return the polygon rendering flag, true if polygons will be
   * rendered from selected data.
   *
   * @see #setPolygonRendering
   */
  public boolean getPolygonRendering () { return (polygonRendering); }

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

  /**
   * Creates a new binned GSHHS reader from the database file name.
   * By default, there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param name the database name.  Several predefined database names
   * are available using the constants in {@link
   * BinnedGSHHSReaderFactory}: <code>HIGH</code>, <code>INTER</code>,
   * <code>LOW</code>, and <code>CRUDE</code>.  See the constants for
   * descriptions of the database resolutions.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public BinnedGSHHSReader (
    String name
  ) throws IOException {

    init (name);
    isHigh = name.equals (BinnedGSHHSReaderFactory.getDatabaseName (
      BinnedGSHHSReaderFactory.COAST, BinnedGSHHSReaderFactory.HIGH));

  } // BinnedGSHHSReader constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new reader with no initialization. */
  protected BinnedGSHHSReader () { }

  ////////////////////////////////////////////////////////////

  /**
   * Reads data about the entire file, including binSize, multiplier,
   * lonBins, latBins, totalBins, firstSegment, numSegments, binInfo,
   * and segmentStart.
   *
   * @throws IOException if an error occurred reading the data.
   */
  protected abstract void getGlobalData () throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Initializes this reader using the specified database.
   *
   * @see #BinnedGSHHSReader(String)
   */
  protected void init (
    String name
  ) throws IOException {

    // Initialize
    // ----------
    minArea = -1;
    database = name;
    polygonRendering = false;
    sdID = openFile (name);

    // Get global info
    // ---------------
    getGlobalData();

    // Get segment data IDs
    // --------------------
    segmentInfoID = selectData (
      "Embedded_npts_levels_exit_entry_for_a_segment");
    segmentAreaID = selectData (
      "Ten_times_the_km_squared_area_of_the_parent_polygon_of_a_segmen");

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
    polygonList.clear();
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
      for (int i = 0; i < bin.getSegments(); i++) 
        featureList.add (bin.getLineFeature (i));

      // Add polygons to list
      // --------------------
      if (polygonRendering) {
        polygonList.addAll (bin.getPolygonFeatures());
        polygonList.add (new PolygonFeature (PolygonFeature.CLOCKWISE));
      } // if
      
    } // while

  } // select

  ////////////////////////////////////////////////////////////

  /** Gets the total number of bins. */
  public int getBins () { return (totalBins); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the bin indices containing the specified Earth area. 
   * 
   * @param area the Earth area.
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
      Integer binIndex = new Integer (getBinIndex (new EarthLocation (
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

} // BinnedGSHHSReader class

////////////////////////////////////////////////////////////////////////
