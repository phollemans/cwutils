////////////////////////////////////////////////////////////////////////
/*
     FILE: GSHHSReader.java
  PURPOSE: To provide GSHHS coastline data.
   AUTHOR: Peter Hollemans
     DATE: 2002/01/18
  CHANGES: 2002/04/30, PFH, fixed coastline EOF error
           2002/09/06, PFH, incorporated into cwf classes and added
             index file and selection methods
           2002/09/30, PFH, added non-indexed data file options
           2002/10/11, PFH, moved to render package
           2002/11/30, PFH, added polygon filtering
           2002/12/06, PFH, modified select for new stored area, added
             non-static getDatabase
           2002/12/10, PFH, added check for invalid resolution in getDatabase
           2002/12/16, PFH, added polygon caching
           2002/12/29, PFH, deprecated
           2003/05/11, PFH, changed internal variable list to vectorList
           2003/12/10, PFH, changed LineFeatureReader to LineFeatureSource
           2004/03/24, PFH, changed Vector to List
           2005/05/26, PFH, changed vectorList to featureList

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.util.*;

/**
 * The GSHHS reader class reads Global Self-consistent Hierarchical
 * High-resolution Shorelines (GSHHS) binary data files.  GSHHS is
 * distributed as a set of data files and routines that provide
 * worldwide vector coastline data in a closed polygon format.  GSHHS
 * is descibed in:
 * <blockquote>
 *   Wessel, P., and W. H. F. Smith, 1996, <i>A global self-consistent,
 *   hierarchical, high-resolution shoreline database</i>, J. Geophys.
 *   Res., 101, 8741-8743.
 * </blockquote>
 * and is available from:
 * <blockquote>
 *   http://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html
 * </blockquote><p>
 *
 * The GSHHS reader can read GSHHS data files either from a set of
 * predefined resource data files or from a user supplied data file.
 * The predefined resources have been passed through a precomputation
 * routine that creates an index of the files based on a 1x1 degree
 * global grid.  If the GSHHS reader is created from one of the
 * predefined resource files, the precomputed index file allows for
 * quick selection of polygons via the {@link #select} routine.  If
 * the GSHHS reader is created from a user file, the entire set of
 * GSHHS polygons is read into memory and the selection method
 * performs no operation.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 *
 * @deprecated This class has been replaced by {@link
 * BinnedGSHHSReader} which provides better overall performance.  This
 * class uses the full GSHHS binary data files as input, with a
 * supplementary index file to speed up polygon access.  The new
 * binned class uses true binned GSHHS polygon data in HDF format with
 * access routines and polygon assembly methods.  The new class uses a
 * reduced data file size and is faster at handling small sections of
 * polygons with a large number of points outside the area of
 * interest.<p>
 */
@Deprecated
public class GSHHSReader
  extends LineFeatureSource {

  // Constants
  // ---------
  /** High resolution (0.2 km) database. */
  public final static String HIGH = "gshhs_h";

  /** Intermediate resolution (1.0 km) database. */
  public final static String INTER = "gshhs_i";

  /** Low resolution (5.0 km) database. */
  public final static String LOW = "gshhs_l";

  /** Crude resolution (25 km) database. */
  public final static String CRUDE = "gshhs_c";

  /** The default polygon cache size in bytes. */
  public final static int DEFAULT_CACHE_SIZE = 2048*1024;

  // Variables	
  // ---------
  /** The index file for GSHHS polygons. */
  private String indexFile;

  /** The database file for GSHHS polygons. */
  private String dataFile;

  /** The minimum polygon area in km^2. */
  private double minArea;

  /** A flag to indicate no index file mode. */
  private boolean noIndex;

  /** The database name. */
  private String database;

  /** The polygon cache. */
  private static Map cache;

  /** The number of polygon points in the cache. */
  private static int cachePoints;  

  /** The maximum number of allowed polygon points in the cache. */
  private static int maxCachePoints;

  /** The cache flag, true if this object should use the cache. */
  private boolean useCache;

  ////////////////////////////////////////////////////////////

  /** Creates the polygon cache. */
  static {

    setCacheSize (DEFAULT_CACHE_SIZE);
    resetCache();

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Sets the polygon cache size.  The cache size is measured in terms
   * of the number of polygon Earth location points.
   *
   * @param cacheSize the cache size in bytes.
   */
  public static void setCacheSize (
    int cacheSize
  ) {

    maxCachePoints = cacheSize / 8;

  } // setCacheSize

  ////////////////////////////////////////////////////////////

  /**
   * The GSHHS polygon header class acts as a container for the GSHHS
   * reader class polygon header values.  Each polygon is a GSHHS
   * database is accompanied by a list of header attributes.  An
   * instance of this class is returned by the {@link
   * GSHHSReader#readHeader} routine.
   */
  public class PolygonHeader {

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

    // Variables
    // ---------
    /** The unique polygon id number, starting at 0. */
    public int id;

    /** The number of points in this polygon. */
    public int n;

    /** The polygon level [1..4]. */
    public int level;

    /** The maximum western value in degrees [-180..180]. */
    public double west;

    /** The maximum eastern value in degrees [-180..180]. */
    public double east;

    /** The maximum southern value in degrees [-90..90]. */
    public double south;

    /** The maximum northern value in degrees [-90..90]. */
    public double north;

    /** The area of the polygon in km^2. */
    public double area;

    /** The Greenwich-crossing flag, 1 if Greenwich is crossed or 0 if not. */
    public short greenwich;

    /** The data source, 0 for CIA WDBII or 1 or WVS. */
    public short source;

  } // PolygonHeader

  ////////////////////////////////////////////////////////////

  /**
   * Sets the minimum area used in polygon selection.  Polygons less
   * than the minimum area are not included.
   *
   * @param minArea the minimum area in km^2.
   */
  public void setMinArea (
    double minArea
  ) {

    this.minArea = minArea;

  } // setMinArea

  ////////////////////////////////////////////////////////////

  /** Resets the cache. */
  private static void resetCache () {

    // Create new cache
    // ----------------
    cache = new LinkedHashMap (16, .75f, true) {
      public boolean removeEldestEntry (Map.Entry eldest) {
        if (cachePoints > maxCachePoints) {
          LineFeature vector = (LineFeature) eldest.getValue();
          cachePoints -= vector.size();
          return (true);
        } // if
        else
          return (false);
      } // removeEldestEntry
    }; 

  } // resetCache

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GSHHS reader instance from the database name.  By
   * default, there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param name the database name.  Several predefined database
   * names are available using the constants <code>HIGH</code>,
   * <code>INTER</code>, <code>LOW</code>, and <code>CRUDE</code>.
   * See the constants for descriptions of the database resolutions.  
   */
  public GSHHSReader (
    String name
  ) {

    // Initialize
    // ----------
    minArea = -1;
    noIndex = false;
    useCache = true;

    // Set file names
    // --------------
    database = name;
    indexFile = this.getClass().getResource(name + ".index").getFile();
    dataFile = this.getClass().getResource(name + ".b").getFile();

  } // GSHHSReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GSHHS reader instance from the resolution.  By
   * default, there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param resolution the pixel resolution in kilometers.  The
   * resolution determines the tolerance level used to decimate
   * polygons from the full resolution database.  Resolution values
   * should reflect the desired accuracy of coastline rendering.  For
   * example, if the coastlines are to be rendered on an image where
   * each pixel measures 5 km across, the polygons need not include
   * features any smaller than 5 km.  The resolution is used to
   * determine the appropriate predefined database.  
   */
  public GSHHSReader (
    double resolution
  ) {

    this (getDatabase (resolution));

  } // GSHHSReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GSHHS reader from the data file.  With this
   * constructor, all polygons are selected and the selection method
   * performs no operation.
   *
   * @param file the data file to read.
   *
   * @throws IOException if the file had input errors.
   */
  public GSHHSReader (
    File file
  ) throws IOException {

    this (new FileInputStream (file));

  } // GSHHSReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GSHHS reader from the input stream.  With this
   * constructor, all polygons are selected and the selection method
   * performs no operation.
   *
   * @param stream the input stream to read.
   *
   * @throws IOException if the stream had input errors.
   */
  public GSHHSReader (
    InputStream stream
  ) throws IOException {

    // Initialize
    // ----------
    minArea = -1;
    noIndex = true;
    database = "";
    useCache = false;

    // Read each polygon
    // -----------------
    DataInputStream data = new DataInputStream (stream);
    PolygonHeader header;
    try { header = readHeader (data); }
    catch (IOException e) { header = null; }
    while (header != null) {
      featureList.add (readPolygon (header, data));
      try { header = readHeader (data); }
      catch (IOException e) { header = null; }
    } // while

  } // GSHHSReader constructor

  ////////////////////////////////////////////////////////////

  protected void select () throws IOException {

    // Check index mode
    // ----------------
    if (noIndex) return;

    // Open index and database
    // -----------------------
    RandomAccessFile index = new RandomAccessFile (indexFile, "r");
    RandomAccessFile data = new RandomAccessFile (dataFile, "r");

    // Read each polygon
    // -----------------
    TreeSet offsets = readOffsets (area, index);
    Iterator iter = offsets.iterator();
    featureList.clear();
    while (iter.hasNext()) {

      // Read all polygon points
      // -----------------------
      int offset = ((Integer)iter.next()).intValue();
      data.seek (offset);
      PolygonHeader header = readHeader (data);
      if (minArea > 0 && header.area < minArea) continue;
      LineFeature polygon = readPolygon (header, data);

      // Filter polygon
      // --------------
      List filteredPolygon = polygon.filter (area);
      featureList.addAll (filteredPolygon);

    } // while

    // Close files
    // -----------
    index.close();
    data.close();

  } // select

  ////////////////////////////////////////////////////////////

  /**
   * Reads the GSHHS database offsets for the specified area.
   *
   * @param area the Earth area for polygon selection.
   * @param index the index file for reading.
   *
   * @return a set of sorted offsets as <code>Integer</code> objects.
   * 
   * @throws IOException if an error occurred reading the data file.
   */
  private TreeSet readOffsets (
    EarthArea area,
    RandomAccessFile index
  ) throws IOException {

    // Loop over each square
    // ---------------------
    TreeSet offsets = new TreeSet();
    Iterator iter = area.getIterator();
    while (iter.hasNext()) {

      // Seek to square offset
      // ---------------------
      int[] square = (int[]) iter.next(); 
      int squareIndex = (square[0]+90)*360 + (square[1]+180);
      index.seek (squareIndex*4);
      int squareOffset = index.readInt();
      if (squareOffset == -1) continue;
      index.seek (squareOffset);

      // Read polygon offsets
      // -------------------- 
      int polygons = index.readShort();
      for (int k = 0; k < polygons; k++) {
        int offset = index.readInt();
        offsets.add (new Integer (offset));
      } // for

    } // while

    return (offsets);

  } // readOffsets

  ////////////////////////////////////////////////////////////

  /**
   * Reads a polygon header from the specified input.
   * 
   * @param in the data input.  The header is constructed by reading
   * at the current input position.
   *
   * @return the GSHHS polygon header read.
   * 
   * @throws IOException if an error occurred reading the data file.
   */
  public PolygonHeader readHeader (
    DataInput in
  ) throws IOException {

    // Create header and read values
    // -----------------------------
    PolygonHeader header = new PolygonHeader();
    header.id = in.readInt();
    header.n = in.readInt();
    header.level = in.readInt();
    header.west = EarthLocation.lonRange (in.readInt() * 1.0e-6);
    header.east = EarthLocation.lonRange (in.readInt() * 1.0e-6);
    header.south = in.readInt() * 1.0e-6;
    header.north = in.readInt() * 1.0e-6;
    header.area = in.readInt() * 0.1;
    header.greenwich = in.readShort();
    header.source = in.readShort();
    return (header);

  } // readHeader

  ////////////////////////////////////////////////////////////

  /**
   * Reads a polygon from the specified input.
   *
   * @param header the polygon header from the last call to {@link
   * #readHeader}.
   * @param in the data input.  The polygon is constructed by reading
   * at the current input position.
   *
   * @return a new polygon as a vector of Earth locations.
   * 
   * @throws IOException if an error occurred reading the data file.
   */
  public LineFeature readPolygon (
    PolygonHeader header,
    DataInput in
  ) throws IOException {

    // Check for polygon in cache
    // --------------------------
    String polygonKey = null;
    LineFeature polygon = null;
    if (useCache) {
      polygonKey = database + Integer.toString (header.id);
      polygon = (LineFeature) cache.get (polygonKey);           
      if (polygon != null) return (polygon);
    } // if 

    // Create polygon and read values
    // ------------------------------
    polygon = new LineFeature();
    for (int i = 0; i < header.n; i++) {
      double lon = in.readInt() * 1.0e-6;
      double lat = in.readInt() * 1.0e-6;
      EarthLocation loc = new EarthLocation (lat, lon);
      polygon.add (loc);
    } // for

    // Add polygon to cache
    // --------------------
    if (useCache) {
      cachePoints += polygon.size();
      cache.put (polygonKey, polygon);
    } // if

    return (polygon);

  } // readPolygon

  ////////////////////////////////////////////////////////////

  /** Gets the database name currently being used for selection. */
  public String getDatabase() { return (database); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the predefined database name with resolution closest to
   * the specified resolution.  If the resolution is invalid, the
   * crude resolution database is returned.
   *
   * @param resolution the image resolution in km/pixel.
   *
   * @return the GSHHS database name.
   */
  public static String getDatabase (
    double resolution        
  ) {

    // Choose most appropriate database
    // --------------------------------
    String[] files = {CRUDE, LOW, INTER, HIGH};
    double resolutions[] = {25, 5, 1, 0.2};
    int index = -1;
    double minDiff = Double.POSITIVE_INFINITY;
    for (int i = 0; i < resolutions.length; i++) {
      double diff = Math.abs (resolutions[i] - resolution);
      if (diff < minDiff) { minDiff = diff; index = i; }
    } // for
    if (index == -1) return (CRUDE);
    else return (files[index]);

  } // getDatabase

  ////////////////////////////////////////////////////////////

} // GSHHSReader class

////////////////////////////////////////////////////////////////////////
