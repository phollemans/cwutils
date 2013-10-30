////////////////////////////////////////////////////////////////////////
/*
     FILE: ContourGenerator.java
  PURPOSE: Generates contour lines based on gridded data.
   AUTHOR: Peter Hollemans
     DATE: 2003/12/10
  CHANGES: 2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/10/12, PFH
           - fixed zero contour levels problem
           - added setLevelNudge() for contouring problems
           2005/05/21, PFH, modified to handle Earth location datum
           2005/05/26, PFH, changed vectorList to featureList

    NOTES: The algorithm for contouring gridded data values using a
           triangular mesh is based on the CONREC subroutine from BYTE
           magazine, June, 1987, and on information from the web site:

             http://astronomy.swin.edu.au/~pbourke/projection/conrec

           accessed December, 2003.  Substantial additions have been
           made to the algorithm in order to join individual contour
           segments into continuous lines where possible.

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.*;
import java.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A contour generator creates lists of Earth data points that follow
 * lines of constant value in a gridded dataset.  A contour generator
 * may be used, for example, to create bathymetry or topographic
 * contours from digital elevation model data, or to create contour
 * lines from data in any 2D dataset.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ContourGenerator
  extends LineFeatureSource {

  // Constants
  // ---------
  
  /** The grid data location offsets for triangles. */
  private static final double[][] OFFSETS = new double[][] {
    {0,1,1,0,0.5},
    {0,0,1,1,0.5}
  };

  /** The case table for triangle/contour intersections. */
  private static final int[][][] CASE_TABLE = new int[][][] {
    {{0,0,8}, {0,2,5}, {7,6,9}},
    {{0,3,4}, {1,3,1}, {4,3,0}},
    {{9,6,7}, {5,2,0}, {8,0,0}}
  };

  /** The location increment table for triangle sides. */
  private static final double[][][] LOCATION_SIDES = new double[][][] {
    {{0,1,0.5}, {1,1,0.5}, {1,0,0.5}, {0,0,0.5}},
    {{0,0,0.5}, {0,1,0.5}, {1,1,0.5}, {1,0,0.5}}
  };

  /** The location increment table for triangle side offsets. */
  private static double[][][] LOCATION_OFFSETS = new double[][][] {
    {{1,-0.5,-0.5}, {0,-0.5,0.5}, {-1,0.5,0.5}, {0,0.5,-0.5}},
    {{0,0.5,-0.5}, {1,-0.5,-0.5}, {0,-0.5,0.5}, {-1,0.5,0.5}}
  };

  /** The accuracy for data locations (helps with hashing). */
  private double LOCATION_ACCURACY = 1e-6;

  // Variables
  // ---------

  /** The grid data for contouring. */
  private Grid grid;

  /** The Earth transform for the grid data. */
  private EarthTransform trans;

  /** The contour levels to generate. */
  private double[] levels;

  /** The starting data location for contouring. */
  private DataLocation start;

  /** The ending data location for contouring. */
  private DataLocation end;

  /** The number of data rows. */
  private int rows;

  /** The number of data columns. */
  private int cols;

  /** The maximum index at each level. */
  private int[] levelMaxIndex;

  /** The fast mode flag, true if fast mode is on. */
  private boolean fastMode = false;

  /** The level nudge value to combat data digitization problems. */
  private double levelNudge;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the level nudge value.  The nudge value is used to nudge the
   * contour levels specified via <code>setLevels()</code> so that
   * limitations in the digitization accuracy do not appear as contour
   * problems.  For example, if a contour level of 20 is requested and
   * the data takes on a value of exactly 20 in a group of adjacent
   * grid locations, then strange contour levels can result, including
   * thin polygons with no interior area and bullseye patterns.  The
   * default nudge value is 0.
   *
   * @param nudge the level nudge value.
   *
   * @see #setLevels
   */
  public void setLevelNudge (double nudge) { levelNudge = nudge; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the fast contouring mode flag.  Under fast contouring mode,
   * contours are generated as discontinuous line segments.  If fast
   * mode is off, contour segments are connected together as
   * continuous lines as much as possible.  This has the effect that
   * the initial generation of contours is slower, but subsequent
   * storage and conversion to screen coordinates is more efficient
   * especially when repeated rendering is required.  By default, fast
   * contouring is off.
   *
   * @param flag the fast mode flag, true for fast contouring.
   */
  public void setFastMode (boolean flag) { fastMode = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the contour levels to generate.  By default, no levels are
   * selected.
   *
   * @param levels the contour levels.
   */
  public void setLevels (
    double[] levels
  ) { 

    // Copy and sort levels
    // --------------------
    this.levels = (double[]) levels.clone();
    Arrays.sort (this.levels);

    // Nudge levels
    // ------------
    /**
     * This helps when the dataset being contoured has limited
     * accuracy and the contour levels match exactly some of the data
     * values.  We nudge the level values slightly so that there are
     * no exact matches.
     */
    for (int i = 0; i < this.levels.length; i++)
      this.levels[i] += levelNudge;
    
    // Create maximum index array
    // --------------------------
    this.levelMaxIndex = new int[levels.length];

  } // setLevels

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new contour generator based on data in the grid.
   * Initially, no contours are available until a call to
   * <code>select()</code> is made.
   *
   * @param grid the grid data for contouring.
   * @param trans the Earth transform for the grid.   
   */
  public ContourGenerator (
    Grid grid,
    EarthTransform trans
  ) {

    this.grid = grid;
    this.trans = trans;

  } // ContourGenerator constructor

  ////////////////////////////////////////////////////////////

  protected void select () throws IOException {

    // Check for levels
    // ----------------
    if (levels == null)
      throw (new IllegalStateException ("No contour levels defined"));

    // Set data bounds
    // ---------------
    int[] extremes = area.getExtremes();
    Datum datum = trans.getDatum();
    DataLocation northWest = trans.transform (new EarthLocation (extremes[0], 
      extremes[3], datum));
    DataLocation southEast = trans.transform (new EarthLocation (extremes[1], 
      extremes[2], datum));
    int[] dims = grid.getDimensions();
    start = new DataLocation (
      Math.floor (Math.min (northWest.get(0), southEast.get(0))),
      Math.floor (Math.min (northWest.get(1), southEast.get(1)))
    ).truncate (dims);
    end = new DataLocation (
      Math.ceil (Math.max (northWest.get(0), southEast.get(0))),
      Math.ceil (Math.max (northWest.get(1), southEast.get(1)))
    ).truncate (dims);

    // Set data dimensions
    // -------------------
    rows = (int) Math.round (end.get (Grid.ROWS) - start.get (Grid.ROWS));
    cols = (int) Math.round (end.get (Grid.COLS) - start.get (Grid.COLS));

    // Create new contour vectors
    // --------------------------
    Contour[] contours = getContours();
    featureList.clear();
    for (int i = 0; i < levels.length; i++) {
      List contourData = contours[i].getContour();
      if (i == 0) levelMaxIndex[i] = contourData.size() - 1;
      else levelMaxIndex[i] = levelMaxIndex[i] + contourData.size();
      featureList.addAll (contourData);
    } // for

  } // select

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the contour level of the Earth vector at the specified
   * index.
   *
   * @param index the Earth vector index.
   *
   * @return the contour level value for the specified vector.
   *
   * @see #setLevels
   * @see LineFeatureSource#iterator
   */
  public double getLevel (
    int index
  ) {

    // Check for levels
    // ----------------
    if (levels == null)
      throw (new IllegalStateException ("No contour levels defined"));

    // Get level value
    // ---------------
    for (int i = 0; i < levels.length; i++)
      if (index <= levelMaxIndex[i]) return (levels[i]);
    return (Double.NaN);

  } // getLevel

  ////////////////////////////////////////////////////////////

  /**
   * A triangle is one of many in a dataset of gridded data in which
   * each data square has been broken up into four triangles.  For 
   * example:
   *
   *    o-------------------o  
   *    | \       3       / |
   *    |   \           /   |
   *    |     \       /     |
   *    |       \   /       |     Each triangle has an index in [0..3],
   *    | 0       o       2 |     counter-clockwise order.
   *    |       /   \       |
   *    |     /       \     |
   *    |   /           \   |
   *    | /       1       \ |
   *    o-------------------o  
   */
  private class Triangle {

    // Variables
    // ---------

    /** The triangle grid row. */
    private int row;

    /** The triangle grid column. */
    private int col;

    /** The triangle index. */
    private int index;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new triangle.
     *
     * @param row the triangle row within the grid.
     * @param col the triangle column within the grid.
     * @param index the triangle index within the grid square.
     */
    public Triangle (
      int row,
      int col,
      int index
    ) {

      this.row = row;
      this.col = col;
      this.index = index;

    } // Triangle constructor 

    ////////////////////////////////////////////////////////

    /**
     * Gets the data location of a point along the triangle side.
     *
     * @param side the triangle side in the range [0..2].
     * @param offset the offset along the side in the range [0..1].
     *
     * @return the data location of the specified point.
     */
    public DataLocation getLocation (
      int side,
      double offset
    ) {

      double row = this.row + LOCATION_SIDES[Grid.ROWS][index][side] + 
        LOCATION_OFFSETS[Grid.ROWS][index][side]*offset;
      double col = this.col + LOCATION_SIDES[Grid.COLS][index][side] + 
        LOCATION_OFFSETS[Grid.COLS][index][side]*offset;
      row = Math.round (row / LOCATION_ACCURACY) * LOCATION_ACCURACY;
      col = Math.round (col / LOCATION_ACCURACY) * LOCATION_ACCURACY;
      return (new DataLocation (row, col));

    } // getLocation

    ////////////////////////////////////////////////////////
    
  } // Triangle class

  ////////////////////////////////////////////////////////////

  /**
   * A contour segment represents a section of a data contour line
   * that is confined to a triangle in the triangular mesh.  A contour
   * segment can supply a unique hash code for its starting and ending
   * points.
   */ 
  private class ContourSegment {

    // Variables
    // ---------

    /** The segment start location. */
    private DataLocation start;

    /** The segment end location. */
    private DataLocation end;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new contour segment.
     *
     * @param tri the contour segment triangle.
     * @param startSide the segment starting side in the range [0..2].
     * @param endSide the segment ending side in the range [0..2].
     * @param startOffset the segment starting offset in the range [0..1].
     * @param endOffset the segment ending offset in the range [0..1].
     */
    public ContourSegment (
      Triangle tri,
      int startSide,
      int endSide,
      double startOffset,
      double endOffset      
    ) {

      // Initialize
      // ----------
      start = tri.getLocation (startSide, startOffset);
      end = tri.getLocation (endSide, endOffset);

    } // ContourSegment constructor

    ////////////////////////////////////////////////////////

    public String toString () {

      return ("ContourSegment[" +
        "start=" + start + "," + 
        "end=" + end + "]");

    } // toString

    ////////////////////////////////////////////////////////

    /** Gets the segment start location. */
    public DataLocation getStart () { return (start); }

    ////////////////////////////////////////////////////////

    /** Gets the segment end location. */
    public DataLocation getEnd () { return (end); }

    ////////////////////////////////////////////////////////

  } // ContourSegment class

  ////////////////////////////////////////////////////////////

  /**
   * A contour line stores a number of contour data locations from one
   * contour level.
   */
  private class ContourLine {

    // Variables
    // ---------

    /** The contour line data locations. */
    private LinkedList locations;

    ////////////////////////////////////////////////////////

    /** Creates a new empty contour line. */
    public ContourLine () {

      locations = new LinkedList();

    } // ContourLine constructor

    ////////////////////////////////////////////////////////

    /** Gets the line start location. */
    public DataLocation getStart () { 

      return ((DataLocation) locations.getFirst()); 

    } // getStart

    ////////////////////////////////////////////////////////

    /** Gets the line end location. */
    public DataLocation getEnd () { 

      return ((DataLocation) locations.getLast()); 

    } // getEnd

    ////////////////////////////////////////////////////////

    /** Gets the list of data locations. */
    public List getLocations () { return (locations); }

    ////////////////////////////////////////////////////////

    public String toString () { 

      return ("ContourLine[" +
        "locations=" + locations + "]");

    } // toString

    ////////////////////////////////////////////////////////

    /** 
     * Adds a new segment to the line.
     *
     * @param segment the new segment to add.  In order for the
     * operation to be successful, the new segment must have an 
     * endpoint in common with this line, or the line must be empty.
     */
    public void addSegment (
      ContourSegment segment
    ) {

      // Insert into empty line
      // ----------------------
      if (locations.size() == 0) {
        locations.add (segment.getStart());
        locations.add (segment.getEnd());
      } // if

      // Insert on one end of line
      // -------------------------
      else {
        DataLocation segmentStart = segment.getStart();
        DataLocation segmentEnd = segment.getEnd();
        DataLocation lineStart = this.getStart();
        DataLocation lineEnd = this.getEnd();
        if (segmentStart.equals (lineStart))
          locations.addFirst (segmentEnd);
        else if (segmentStart.equals (lineEnd))
          locations.addLast (segmentEnd);
        else if (segmentEnd.equals (lineStart))
          locations.addFirst (segmentStart);
        else if (segmentEnd.equals (lineEnd))
          locations.addLast (segmentStart);
      } // else

    } // addSegment

    ////////////////////////////////////////////////////////

    /** 
     * Adds another contour line to this one.
     *
     * @param otherLine the contour line to add.  In order for the
     * operation to be successful, the contour line must have an 
     * endpoint in common with this line.
     */
    public void addLine (
      ContourLine otherLine
    ) {

      // Check for empty line
      // --------------------
      if (otherLine.locations.size() == 0) return;
      if (locations.size() == 0) {
        locations = otherLine.locations;
      } // if 

      // Add other locations
      // -------------------
      else {
        DataLocation otherLineStart = otherLine.getStart();
        DataLocation otherLineEnd = otherLine.getEnd();
        DataLocation lineStart = this.getStart();
        DataLocation lineEnd = this.getEnd();
        LinkedList otherLineLocations = 
          (LinkedList) otherLine.locations.clone();
        if (otherLineStart.equals (lineStart)) {
          Collections.reverse (otherLineLocations);
          otherLineLocations.removeLast();
          otherLineLocations.addAll (locations);
          locations = otherLineLocations;
        } // if
        else if (otherLineStart.equals (lineEnd)) {
          otherLineLocations.removeFirst();
          locations.addAll (otherLineLocations);
        } // else if
        else if (otherLineEnd.equals (lineStart)) {
          otherLineLocations.removeLast();
          otherLineLocations.addAll (locations);
          locations = otherLineLocations;
        } // else if
        else if (otherLineEnd.equals (lineEnd)) {
          Collections.reverse (otherLineLocations);
          otherLineLocations.removeFirst();
          locations.addAll (otherLineLocations);
        } // else if
      } // else

    } // addLine

    ////////////////////////////////////////////////////////

  } // ContourLine class

  ////////////////////////////////////////////////////////////

  /**
   * A contour stores a number of contour segments from one contour
   * level as a set of continuous lines.
   */
  private class Contour {

    // Variables
    // ---------

    /** The list of contour segments (for fast mode). */
    private List segmentList;

    /** The map of contours, hashed by start location. */
    private HashMap startMap;

    /** The map of contours, hashed by end location. */
    private HashMap endMap;

    /** The contour level. */
    private double level;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new contour.
     *
     * @param level the contour level.
     */
    public Contour (
      double level
    ) {

      this.level = level;
      segmentList = new LinkedList();
      startMap = new HashMap();
      endMap = new HashMap();

    } // Contour

    ////////////////////////////////////////////////////////

    /**
     * Adds a segment to the existing contour.  The segment must be
     * from the same contour level, otherwise an inconsistency may
     * arise in joining the new segment with the existing segments.
     *
     * @param segment the new segment to add.
     */
    public void addSegment (
      ContourSegment segment
    ) {

      // Add segment in fast mode
      // ------------------------
      if (fastMode) {
        segmentList.add (segment);
        return;
      } // if

      // Get existing contour line
      // -------------------------
      ContourLine line = null;
      HashMap[] maps = new HashMap[] {startMap, endMap};
      DataLocation[] keys = new DataLocation[] {
        segment.getStart(),
        segment.getEnd()
      };
      int mapIndex = 0, keyIndex = 0;
      for (mapIndex = 0; mapIndex < 2; mapIndex++) {
        for (keyIndex = 0; keyIndex < 2; keyIndex++) {
          if (maps[mapIndex].containsKey (keys[keyIndex])) {
            line = (ContourLine) maps[mapIndex].get (keys[keyIndex]);
            break;
          } // if
        } // for
        if (line != null) break;
      } // for

      // Create new contour line
      // -----------------------
      if (line == null) {
        line = new ContourLine();
        line.addSegment (segment);
        startMap.put (line.getStart(), line);
        endMap.put (line.getEnd(), line);
      } // if

      // Add segment to contour line
      // ---------------------------
      else {
        line.addSegment (segment);
        maps[mapIndex].remove (keys[keyIndex]);

        // Join start-to-start or end-to-end
        // ---------------------------------
        DataLocation[] lineKeys = new DataLocation[] {
          line.getStart(),
          line.getEnd()
        };
        if (maps[mapIndex].containsKey (lineKeys[mapIndex])) {
          maps[1-mapIndex].remove (lineKeys[1-mapIndex]);
          ContourLine existingLine = (ContourLine) maps[mapIndex].remove (
            lineKeys[mapIndex]);
          DataLocation[] existingLineKeys = new DataLocation[] {
            existingLine.getStart(), 
            existingLine.getEnd()
          };
          maps[1-mapIndex].remove (existingLineKeys[1-mapIndex]);
          line.addLine (existingLine);
          startMap.put (line.getStart(), line);
          endMap.put (line.getEnd(), line);
        } // if

        // Join start-to-end
        // -----------------
        else if (maps[1-mapIndex].containsKey (lineKeys[mapIndex]) &&
          !lineKeys[mapIndex].equals (lineKeys[1-mapIndex])) {
          maps[1-mapIndex].remove (lineKeys[1-mapIndex]);
          ContourLine existingLine = (ContourLine) maps[1-mapIndex].remove (
            lineKeys[mapIndex]);
          DataLocation[] existingLineKeys = new DataLocation[] {
            existingLine.getStart(), 
            existingLine.getEnd()
          };
          maps[mapIndex].remove (existingLineKeys[mapIndex]);
          line.addLine (existingLine);
          startMap.put (line.getStart(), line);
          endMap.put (line.getEnd(), line);
        } // else if

        // Insert back into map
        // --------------------
        else maps[mapIndex].put (lineKeys[mapIndex], line);

      } // else

    } // addSegment

    ////////////////////////////////////////////////////////

    /** 
     * Gets the contour as a list of Earth vector objects.
     */
    public List getContour () {

      // Create return list
      // --------------------
      List list = new ArrayList();
      
      // Get fast mode contour segments
      // ------------------------------
      if (fastMode) {
        for (Iterator iter = segmentList.iterator(); iter.hasNext();) {
          ContourSegment segment = (ContourSegment) iter.next();
          LineFeature vector = new LineFeature();
          vector.add (trans.transform (segment.getStart()));
          vector.add (trans.transform (segment.getEnd()));
          list.add (vector);
        } // for
      } // if

      // Get continuous contour lines
      // ----------------------------
      else {
        for (Iterator iter = startMap.values().iterator(); iter.hasNext();) {
          List locations = ((ContourLine) iter.next()).getLocations();
          LineFeature vector = new LineFeature();
          for (Iterator locIter = locations.iterator(); locIter.hasNext();) {
            EarthLocation loc = trans.transform (
              (DataLocation) locIter.next());
            vector.add (loc);
          } // for
          list.add (vector);
        } // for
      } // else

      return (list);

    } // getContour

    ////////////////////////////////////////////////////////

  } // Contour class

  ////////////////////////////////////////////////////////////

  /**
   * Gets the set of contours based on grid data.
   * 
   * @return the array of contours.
   */
  private Contour[] getContours () {

    // Create contours
    // ---------------
    int nlevels = levels.length;
    Contour[] contours = new Contour[nlevels];
    for (int i = 0; i < nlevels; i++)
      contours[i] = new Contour (i);

    // Check for zero levels
    // ---------------------
    if (nlevels == 0) return (contours);

    // Initialize coordinates
    // ----------------------
    DataLocation dataStart = (DataLocation) start.clone();
    DataLocation dataEnd = ((DataLocation) end.clone()).translate (-1, -1);
    int[] stride = new int[] {1, 1};
    DataLocation loc = (DataLocation) start.clone();

    // Loop over each grid square
    // --------------------------
    do {

      // Get square index
      // ----------------
      int row = (int) Math.round (loc.get (Grid.ROWS));
      int col = (int) Math.round (loc.get (Grid.COLS));

      // Get values at corners of square and center
      // ------------------------------------------
      /*
       *  0 o-------------------o 3
       *    | \               / |
       *    |   \           /   |     
       *    |     \       /     |     
       *    |       \   /       |     Each data point has an index in the
       *    |         o         |     range [0..4], counter-clockwise order,
       *    |       / 4 \       |     with 4 in the center.
       *    |     /       \     |     
       *    |   /           \   |
       *    | /               \ |
       *  1 o-------------------o 2
       */
      double[] values = new double[5];
      for (int i = 0; i < 4; i++)
        values[i] = grid.getValue (loc.translate (OFFSETS[Grid.ROWS][i], 
          OFFSETS[Grid.COLS][i]));
      values[4] = (values[0] + values[1] + values[2] + values[3]) / 4;

      // Find minimum and maximum values
      // -------------------------------
      double minValue = values[0], maxValue = values[0];
      for (int i = 1; i < 4; i++) {
        if (values[i] < minValue) minValue = values[i];
        if (values[i] > maxValue) maxValue = values[i];
      } // for

      // Check for any contours in square
      // --------------------------------
      if (maxValue < levels[0] || minValue > levels[nlevels-1])
        continue;

      // Loop over each contour level
      // ----------------------------
      for (int level = 0; level < nlevels; level++) {

        // Check for contour level in square
        // ---------------------------------
        if (levels[level] < minValue || levels[level] > maxValue)
          continue;

        // Calculate triangle height differences
        // -------------------------------------
        double[] heights = new double[5];
        int signs[] = new int[5];
        for (int i = 0; i < 5; i++) {
          heights[i] = values[i] - levels[level];
          signs[i] = (heights[i] < 0 ? -1 : heights[i] > 0 ? 1 : 0);
        } // for

        // Loop over each triangle in square
        // ---------------------------------
        for (int index = 0; index < 4; index++) {

          // Initialize triangle
          // -------------------
          Triangle tri = null;

          // Get triangle corner points
          // --------------------------
          int p0 = index;
          int p1 = (index+1)%4;
          int p2 = 4;

          // Calculate contour line coordinates
          // ----------------------------------
          int caseValue = CASE_TABLE[signs[p0]+1][signs[p1]+1][signs[p2]+1];
          if (caseValue != 0) {
            int startSide = 0, endSide = 0;
            double startOffset = 0, endOffset = 0;
            switch (caseValue) {
            case 1: // Line between points 0 and 1
              startSide = 0;
              endSide = 1;
              startOffset = endOffset = 0;
              break;
            case 2: // Line between points 1 and 2
              startSide = 1;
              endSide = 2;
              startOffset = endOffset = 0;
              break;
            case 3: // Line between points 2 and 0
              startSide = 2;
              endSide = 0;
              startOffset = endOffset = 0;
              break;
            case 4: // Line between point 0 and side 1-2
              startSide = 0;
              startOffset = 0;
              endSide = 1;
              endOffset = Math.abs (heights[p1] / (heights[p2] - heights[p1]));
              break;
            case 5: // Line between point 1 and side 2-0
              startSide = 1;
              startOffset = 0;
              endSide = 2;
              endOffset = Math.abs (heights[p2] / (heights[p0] - heights[p2]));
              break;
            case 6: // Line between point 2 and side 0-1
              startSide = 2;
              startOffset = 0;
              endSide = 0;
              endOffset = Math.abs (heights[p0] / (heights[p1] - heights[p0]));
              break;
            case 7: // Line between sides 0-1 and 1-2
              startSide = 0;
              startOffset = 
                Math.abs (heights[p0] / (heights[p1] - heights[p0]));
              endSide = 1;
              endOffset = Math.abs (heights[p1] / (heights[p2] - heights[p1]));
              break;
            case 8: // Line between sides 1-2 and 2-0
              startSide = 1;
              startOffset = 
                Math.abs (heights[p1] / (heights[p2] - heights[p1]));
              endSide = 2;
              endOffset = Math.abs (heights[p2] / (heights[p0] - heights[p2]));
              break;
            case 9: // Line between sides 2-0 and 0-1
              startSide = 2;
              startOffset = 
                Math.abs (heights[p2] / (heights[p0] - heights[p2]));
              endSide = 0;
              endOffset = Math.abs (heights[p0] / (heights[p1] - heights[p0]));
              break;
            default: 
              break;
            } // switch

            // Add segment to contour
            // ----------------------
            if (tri == null) tri = new Triangle (row, col, index);
            ContourSegment segment = new ContourSegment (tri, 
              startSide, endSide, startOffset, endOffset);
            contours[level].addSegment (segment);

          } // if

        } // for

      } // for

    } while (loc.increment (stride, start, end));

    return (contours);
  
  } // getContours

  ////////////////////////////////////////////////////////////

} // ContourGenerator class

////////////////////////////////////////////////////////////////////////
