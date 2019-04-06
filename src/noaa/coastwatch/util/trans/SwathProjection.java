////////////////////////////////////////////////////////////////////////
/*

     File: SwathProjection.java
   Author: Peter Hollemans
     Date: 2002/04/15

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
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Encodable;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.LongitudeFilter;
import noaa.coastwatch.util.ValueSource;
import noaa.coastwatch.util.VariableEstimator;
import noaa.coastwatch.util.trans.DataProjection;
import noaa.coastwatch.util.trans.EarthTransform2D;

// Testing
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.DataLocationIterator;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.DataLocationIteratorFactory;

/**
 * <p>The <code>SwathProjection</code> class implements earth transform
 * calculations for satellite swath (also called sensor scan) 2D
 * projections.  Swaths may be created by supplying a field of
 * latitude and longitude values -- one for each data value location.
 * The swath projection uses polynomial estimator objects for
 * estimating the latitude and longitude values in order to perform an
 * accurate reverse lookup (ie: computing the data location for a
 * given latitude and longitude).</p>
 *
 * <p><b>This class is thread-safe as of version 3.5.0.</b></p>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class SwathProjection
  extends EarthTransform2D
  implements Encodable {

  // Constants
  // ---------
  /** The default tolerance distance as a fraction of the resolution. */
  public static final double TOLERANCE_FACTOR = 0.1;

  /** The <code>transform(EarthLocation)</code> maximum iterations. */
  public static final int MAX_ITERATIONS = 100;

  /** Projection description string. */
  public final static String DESCRIPTION = "swath";  

  // Variables
  // ---------
  
  /** Latitude and longitude variable estimators. */
  private VariableEstimator latEst, lonEst;  

  /** Tolerance distance in kilometres. */
  private double tolerance;

  /** Geographic area covered by the swath. */
  private EarthArea area;

  /** Flag to indicate no operation mode. */
  private static boolean nullMode = false;

  /** Flag to indicate test mode. */
  private static boolean testMode = false;

  /** 
   * The last data coordinate from an earth location to data location
   * transform.  The idea here is to save the last coordinate
   * transform to use as a starting point for the next transform.
   * Generally, locations are transformed in polylines of Earth
   * locations that are fairly close together both on the earth and in
   * the data location space.  We keep a hash map here to handle coordinate
   * transforms happening from multiple threads.
   */
  private Map<Long, DataLocation> lastDataLocMap = new HashMap<>();

  /** The list of seed data locations to try. */
  private DataLocation[] seedDataLocations;

  /** The list of seed earth locations to try. */
  private EarthLocation[] seedEarthLocations;
  
  /** The north flag, true if latitudes vary inversely as row index. */
  private boolean northIsUp;
  
  /** The data projection used to create the lat/lon estimators. */
  private EarthTransform dataTrans;

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isInvertible () { return (false); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the null operation mode for newly constructed swath
   * transforms.  In null mode, no operations may be performed with
   * the swath, except the {@link #describe} and {@link #getNorthIsUp}
   * methods.  This mode is useful for informational routines that
   * create swath transforms but want to avoid the time spent setting
   * up the swath transform polynomials.  But default, null mode is
   * off.
   *
   * @param flag the null mode flag, true for null mode.
   */
  public static void setNullMode (boolean flag) { nullMode = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the test operation mode for newly constructed swath
   * transforms.  In test mode, swath projections created from
   * lat/lon data variables save the data projection for later
   * testing.  But default, test mode is off.
   *
   * @param flag the test mode flag, true for test mode.
   */
  public static void setTestMode (boolean flag) { testMode = flag; }

  ////////////////////////////////////////////////////////////

  /**
   * Returns true if north is at the top of the data.  If true, then
   * latitude values decrease as row index increases.  If false, then
   * latitude values increase as row index increases.  This is a
   * useful flag for rendering code that presents the data in a way
   * that is familiar to the user.
   */
  public boolean getNorthIsUp () { return (northIsUp); }

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the tolerance based on the resolution at the center
   * of the swath.  Also creates the seeds used to initialize the
   * location search.
   */
  public void resetTolerance () {

    // Calculate tolerance distance
    // ----------------------------
    DataLocation center = new DataLocation (
      (dims[Grid.ROWS]-1)/2.0, 
      (dims[Grid.COLS]-1)/2.0
    );
    double[] res = getResolution (center);
    tolerance = Math.min (res[0], res[1]) * TOLERANCE_FACTOR;

    // Create seed list
    // ----------------
    seedDataLocations = new DataLocation[] {
      new DataLocation (dims[Grid.ROWS]*0.125, dims[Grid.COLS]/2),
      new DataLocation (dims[Grid.ROWS]*0.375, dims[Grid.COLS]/2),
      new DataLocation (dims[Grid.ROWS]*0.625, dims[Grid.COLS]/2),
      new DataLocation (dims[Grid.ROWS]*0.875, dims[Grid.COLS]/2)
    };
    seedEarthLocations = new EarthLocation[seedDataLocations.length];
    for (int i = 0; i < seedDataLocations.length; i++) {
      seedEarthLocations[i] = transform (seedDataLocations[i]);
    } // for

  } // resetTolerance

  ////////////////////////////////////////////////////////////

  /** Resets the earth area object. */
  private void resetArea () {

    // Initialize area
    // ---------------
    area = null;
    EarthArea swathArea = new EarthArea (this, new DataLocation(0,0),
      new DataLocation (dims[Grid.ROWS]-1, dims[Grid.COLS]-1));
    area = swathArea;

  } // resetArea

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the internal north flag.
   *
   * @param lat the value source for latitude data.
   * @param dims the dimensions of the latitude data.
   */
  private void setNorthFlag (
    ValueSource lat,
    int[] dims
  ) {

    /** 
     * We set the north flag here by starting at the center and
     * working our way up and down in the swath until we get valid
     * latitudes.  Then we compare them.
     */
    DataLocation center = new DataLocation (
      (dims[Grid.ROWS]-1)/2.0, 
      (dims[Grid.COLS]-1)/2.0
    );

    DataLocation topLoc = (DataLocation) center.clone();
    topLoc.set (Grid.ROWS, topLoc.get (Grid.ROWS) - 1);
    double top = Double.NaN;
    while (topLoc.isContained (dims) && Double.isNaN (top)) {
      top = lat.getValue (topLoc);
      topLoc.set (Grid.ROWS, topLoc.get (Grid.ROWS) - 1);
    } // while
    
    DataLocation bottomLoc = (DataLocation) center.clone();
    bottomLoc.set (Grid.ROWS, bottomLoc.get (Grid.ROWS) + 1);
    double bottom = Double.NaN;
    while (bottomLoc.isContained (dims) && Double.isNaN (bottom)) {
      bottom = lat.getValue (bottomLoc);
      bottomLoc.set (Grid.ROWS, bottomLoc.get (Grid.ROWS) + 1);
    } // while

    northIsUp = (top > bottom);

  } // setNorthFlag

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a swath projection from the specified latitude and
   * longitude data and desired polynomial size.  A default tolerance
   * is initially set for earth location to data location translation
   * based on the resolution at the center of the projection.
   *
   * @param lat a data variable containing latitude data.
   * @param lon a data variable containing longitude data.
   * @param maxSize the maximum polynomial partition size in
   * kilometres.
   * @param maxDims the maximum partition size in any dimension
   * in terms of data locations.
   *
   * @see VariableEstimator
   * @see noaa.coastwatch.util.EarthPartition
   */
  public SwathProjection (
    DataVariable lat,
    DataVariable lon,
    double maxSize,
    int[] maxDims
  ) {

    // Set north flag
    // --------------
    dims = lat.getDimensions();
    setNorthFlag (lat, dims);

    // Check for null mode
    // -------------------
    if (nullMode) return;

    // Create estimators
    // -----------------
    EarthTransform trans = new DataProjection (lat, lon);
    if (testMode) dataTrans = trans;
    latEst = new VariableEstimator (lat, trans, maxSize, maxDims);
    lonEst = new VariableEstimator (lon, new LongitudeFilter(), latEst);

    // Initialize
    // ----------
    resetTolerance();
    resetArea();

  } // SwathProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data projection used to create the lat/lon estimators.
   * This is only available if the swath projection was created in
   * test mode.
   *
   * @return the data peojection or null if not available.
   */
  public EarthTransform getDataTransform () {

    return (dataTrans);
    
  } // getDataTransform

  ////////////////////////////////////////////////////////////

  @Override
  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    // Check for 2D location
    // ---------------------
    if (dataLoc.getRank() != 2) {
      earthLoc.setCoords (Double.NaN, Double.NaN);
    } // if

    // Estimate geographic coords
    // --------------------------
    else {
      earthLoc.setCoords (latEst.getValue (dataLoc), 
        lonEst.getValue (dataLoc));
    } // else

  } // transformImpl

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the tolerance distance for earth location transformations.
   * The tolerance distance governs the accuracy of earth location to
   * data location translations.  Once set, a call to
   * <code>transform(EarthLocation)</code> returns an approximate data
   * location that is within the tolerance distance of the actual data
   * location.
   * 
   * @param tolerance the tolerance distance in kilometres.
   */
  public void setTolerance (double tolerance) { this.tolerance = tolerance; }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the tolerance distance for earth location transformations.
   *
   * @return the tolerance distance in kilometres.
   *
   * @see #setTolerance
   */
  public double getTolerance () { return (tolerance); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the last valid data location for this thread.
   *
   * @return the last valid data location or null for none.
   *
   * @since 3.5.0
   */
  private DataLocation getLastDataLoc () {
  
    DataLocation dataLoc;
    long id = Thread.currentThread().getId();
    synchronized (lastDataLocMap) {
      dataLoc = lastDataLocMap.get (id);
    } // synchronized

    return (dataLoc);
  
  } // getLastDataLoc

  ////////////////////////////////////////////////////////////

  /**
   * Sets the last valid data location for this thread.
   *
   * @param dataLoc the new last valid data location or null for none.
   *
   * @since 3.5.0
   */
  private void setLastDataLoc (
    DataLocation dataLoc
  ) {

    long id = Thread.currentThread().getId();
    synchronized (lastDataLocMap) {
      lastDataLocMap.put (id, dataLoc);
    } // synchronized
  
  } // setLastDataLoc

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {



    // TODO: Need to adjust the behaviour of EarthArea to
    // successfully explore all the partitions of the swath even
    // if some of them contain missing location data.  Possibly,
    // we could explore each child partition and then form a
    // union of the results.


    // TODO: We also need to adjust the (lat,lon) finding
    // algorithm so that it can skip over missing areas and
    // handle very large swaths like full orbit swaths.




    // Check for containment
    // ---------------------
    if (area != null) {
      boolean isContained = area.contains (earthLoc);
      if (!isContained) {
        setLastDataLoc (null);
        dataLoc.set (Grid.ROWS, Double.NaN);
        dataLoc.set (Grid.COLS, Double.NaN);
        return;
      } // if
    } // if

    // Initialize search using last or seed value
    // ------------------------------------------
    EarthLocation testEarthLoc = new EarthLocation();
    double distProxy = Double.MAX_VALUE;
    DataLocation initDataLoc = null;

    DataLocation lastDataLoc = getLastDataLoc();
    if (lastDataLoc != null) {
      initDataLoc = lastDataLoc;
      transform (initDataLoc, testEarthLoc);
      distProxy = earthLoc.distanceProxy (testEarthLoc);
    } // if

    for (int i = 0; i < seedEarthLocations.length; i++) {
      double seedDistProxy = earthLoc.distanceProxy (seedEarthLocations[i]);
      if (seedDistProxy < distProxy) {
        initDataLoc = seedDataLocations[i];
        distProxy = seedDistProxy;
      } // if
    } // for
    
    // Create iteration objects
    // ------------------------
    DataLocation testDataLoc = (DataLocation) initDataLoc.clone();
    DataLocation x1 = new DataLocation (2);
    DataLocation x2 = new DataLocation (2);
    DataLocation y1 = new DataLocation (2);
    DataLocation y2 = new DataLocation (2);
    EarthLocation x1Geo = new EarthLocation();
    EarthLocation x2Geo = new EarthLocation();
    EarthLocation y1Geo = new EarthLocation();
    EarthLocation y2Geo = new EarthLocation();
    double[] res = new double[2];
    double[] grad = new double[2];
    double[] correction = new double[2];
    
    // Search until tolerance or max iterations hit
    // --------------------------------------------
    double d = EarthLocation.distanceProxyToDistance (distProxy);
    double lastd = d;
    double[] inc = {0.5, 0.5};
    int iter = 0;

    while (d > tolerance && iter < MAX_ITERATIONS) {

      // Compute increments in x and y
      // -----------------------------
      testDataLoc.translate (-inc[0], 0, x1); x1.truncateInPlace (dims);
      testDataLoc.translate (+inc[0], 0, x2); x2.truncateInPlace (dims);
      testDataLoc.translate (0, -inc[1], y1); y1.truncateInPlace (dims);
      testDataLoc.translate (0, +inc[1], y2); y2.truncateInPlace (dims);
      transform (x1, x1Geo);
      transform (x2, x2Geo);
      transform (y1, y1Geo);
      transform (y2, y2Geo);
      double dx = x2.get(0) - x1.get(0);
      double dy = y2.get(1) - y1.get(1);

      // Calculate resolution in km/pixel
      // --------------------------------
      res[0] = x1Geo.distance (x2Geo) / dx;
      res[1] = y1Geo.distance (y2Geo) / dy;

      // Calculate unit gradient
      // -----------------------
      grad[0] = (earthLoc.distance (x2Geo) - earthLoc.distance (x1Geo)) / dx;
      grad[1] = (earthLoc.distance (y2Geo) - earthLoc.distance (y1Geo)) / dy;
      double mag = Math.sqrt (Math.pow (grad[0],2) + Math.pow (grad[1],2));
      grad[0] /= mag;
      grad[1] /= mag;

      // Estimate new coordinates
      // ------------------------
      correction[0] = -(d/res[0]) * grad[0];
      correction[1] = -(d/res[1]) * grad[1];
      testDataLoc.translate (correction[0], correction[1], testDataLoc);
      testDataLoc.truncateInPlace (dims);
      transform (testDataLoc, testEarthLoc);
      d = earthLoc.distance (testEarthLoc);

      // Test for convergence
      // --------------------
      /** 
       * What we do here is check to see if the distance is changing
       * very much.  If it changes by less than a factor of 1e-6, then
       * we know that we're stuck somewhere.  First check that lastd
       * is non-zero just for safety, although that condition should
       * never occur.
       */
      if (lastd == 0) break;
      if (Math.abs ((lastd - d)/lastd) < 1e-6) {
        break;
      } // if
      lastd = d;

      // Adjust gradient increment
      // -------------------------
      if (d < inc[0]*2*res[0]) inc[0] = (d/2)/res[0];
      if (d < inc[1]*2*res[1]) inc[1] = (d/2)/res[1];

      // Add to iterations
      // -----------------
      iter++;

    } // while

    // Check for convergence
    // ---------------------
    if (d > tolerance) {
      setLastDataLoc (null);
      dataLoc.set (Grid.ROWS, Double.NaN);
      dataLoc.set (Grid.COLS, Double.NaN);
    } // if
    
    // Return data coordinate
    // ----------------------
    else {
      setLastDataLoc (testDataLoc);
      dataLoc.setCoords (testDataLoc);
    } // else

  } // transformImpl

  ////////////////////////////////////////////////////////////

  /**
   * Gets an encoded representation of this swath projection. The
   * encoding may later be used to recreate the swath without
   * using the original variable data.
   *
   * @return the object encoding.  The encoding object is an
   * <code>Object[5]</code> array containing:
   * <ul>
   *   <li>a <code>BitSet</code> object used for encoding swath partition
   *   structure information</li>
   *   <li>a <code>List</code> of <code>double[]</code> coordinates
   *   specifying swath partition boundaries as min and max (2 arrays per
   *   partition)</li>
   *   <li>a <code>List</code> of <code>double[]</code> arrays specifying 
   *   latitude estimator coefficients</li>
   *   <li>a <code>List</code> of <code>double[]</code> arrays specifying 
   *   longitude estimator coefficients</li>
   *   <li>an <code>int[2]</code> array specifying swath dimensions</li> 
   * </ul>
   *
   * @see #useEncoding
   */
  public Object getEncoding () {

    // Get variable estimator encodings
    // --------------------------------
    Object[] latEncoding = (Object[]) latEst.getEncoding();
    Object[] lonEncoding = (Object[]) lonEst.getEncoding();

    // Return swath encoding
    // ---------------------
    Object[] obj = new Object[] {latEncoding[0], latEncoding[1],
      latEncoding[2], lonEncoding[2], dims.clone()};
    return (obj);

  } // getEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a swath projection from the specified encoding.
   * The encoding must be a valid encoding of a swath as created
   * by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  ) {

    // Create estimators
    // -----------------
    latEst = new VariableEstimator (obj);
    ((Object[]) obj)[2] = ((Object[]) obj)[3];
    lonEst = new VariableEstimator (obj, latEst);

    // Initialize
    // ----------
    dims = (int[]) ((int[]) ((Object[]) obj)[4]).clone ();
    resetTolerance();
    resetArea();
    setNorthFlag (latEst, dims);

  } // useEncoding

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a swath projection from the specified encoding.
   * The encoding must be a valid encoding of a swath as created
   * by <code>getEncoding</code>.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public SwathProjection (
    Object obj
  ) {

    // Check for null mode
    // -------------------
    if (nullMode) return;

    // Perform decoding
    // ----------------
    useEncoding (obj);

  } // SwathProjection constructor

  ////////////////////////////////////////////////////////////

  /**
   * Converts a bit set to a byte array.  This method is useful for
   * encoding the swath projection structure information into a byte
   * stream.
   *
   * @param bits the bits to convert.
   *
   * @return a byte array large enough to hold all bits.
   *
   * @see #getEncoding
   */
  public static byte[] toBytes (
    BitSet bits
  ) {

    // Create byte array
    // -----------------
    int nBits = bits.length();
    int nBytes = nBits/8;
    if (nBits%8 != 0) nBytes++;
    byte[] byteArray = new byte[nBytes];

    // Fill byte array
    // ---------------
    for (int i = 0; i < nBits; i++) {
      if (bits.get (i)) {
        int byteIndex = i/8;
        int bitIndex = 7 - i%8; 
        byteArray[byteIndex] |= (0x01 << bitIndex);
      } // if
    } // for
    return (byteArray);

  } // toBytes

  ////////////////////////////////////////////////////////////

  /**
   * Converts an array of bytes to a bit set.  This method is useful for
   * decoding the swath projection structure information from a byte
   * stream.
   *
   * @param byteArray the bytes to convert.
   *
   * @return a bit set large enough to hold all the byte information.
   *
   * @see #useEncoding
   */
  public static BitSet toBits (
    byte[] byteArray
  ) {

    // Create and fill bit set
    // -----------------------
    BitSet bits = new BitSet ();
    for (int i = 0; i < byteArray.length; i++) {
      if (byteArray[i] != 0) {
        for (int j = 0; j < 8; j++) {   
          if ((byteArray[i] & (0x01 << (7-j))) != 0)
            bits.set (i*8 + j);
        } // for
      } // if
    } // for
    return (bits);

  } // toBits

  ////////////////////////////////////////////////////////////

  /**
   * Compares the specified object with this swath projection for
   * equality.  The encodings of the two swath projections are
   * compared value by value.  If both swath projections were created
   * in null mode, then they are considered equal (although this may not
   * actually be the case).
   *
   * @param obj the object to be compared for equality.
   *
   * @return true if the swath projections are equivalent, or false if
   * not.
   */
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof SwathProjection)) return (false);

    // Check for null mode used
    // ------------------------
    if (
      this.latEst == null &&
      this.lonEst == null &&
      ((SwathProjection) obj).latEst == null &&
      ((SwathProjection) obj).lonEst == null
    ) {
      return (true);
    } // if

    // Get encodings
    // -------------
    Object[] thisEnc = (Object[]) this.getEncoding ();
    Object[] objEnc = (Object[]) ((SwathProjection) obj).getEncoding ();

    // Compare bit sets
    // ----------------
    if (!((BitSet) thisEnc[0]).equals ((BitSet) objEnc[0])) return (false);

    // Compare boundaries
    // ------------------
    for (int i = 0; i < ((List) thisEnc[1]).size(); i++) {
      if (!Arrays.equals (
        (double[]) ((List) thisEnc[1]).get(i), 
        (double[]) ((List) objEnc[1]).get(i))) 
        return (false);
    } // for

    // Compare lat and lon estimators
    // ------------------------------
    for (int i = 0; i < ((List) thisEnc[2]).size(); i++) {
      if (!Arrays.equals (
        (double[]) ((List) thisEnc[2]).get(i), 
        (double[]) ((List) objEnc[2]).get(i))) 
        return (false);
    } // for
    for (int i = 0; i < ((List) thisEnc[3]).size(); i++) {
      if (!Arrays.equals (
        (double[]) ((List) thisEnc[3]).get(i), 
        (double[]) ((List) objEnc[3]).get(i))) 
        return (false);
    } // for

    // Compare dimensions
    // ------------------
    if (!Arrays.equals ((int[]) thisEnc[4], (int[]) objEnc[4])) 
      return (false);

    return (true);

  } // equals

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Read the earth transform
    // ------------------------
    SwathProjection.setTestMode (true);
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]);
    EarthTransform trans = reader.getInfo().getTransform();

    if (trans instanceof SwathProjection) {
      SwathProjection swathProj = (SwathProjection) trans;
      EarthTransform dataProj = swathProj.getDataTransform();
      if (dataProj == null) {
        System.out.println ("Encountered null data projection in swath");
      } // if
      else {

        // Create data iterator
        // --------------------
        DataLocationConstraints cons = new DataLocationConstraints();
        cons.dims = swathProj.getDimensions();
        cons.fraction = 1;
        DataLocationIterator iter = DataLocationIteratorFactory.getInstance().create (cons);

        // Initialize statistics
        // ---------------------
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        int valid = 0;
        int values = 0;

        // Calculate statistics
        // --------------------
        EarthLocation swathEarthLoc = new EarthLocation();
        EarthLocation actualEarthLoc = new EarthLocation();
        DataLocation dataLoc = new DataLocation (2);
        DataLocation maxErrorLoc = null;
        while (iter.hasNext()) {

          iter.nextLocation (dataLoc);
          swathProj.transform (dataLoc, swathEarthLoc);
          dataProj.transform (dataLoc, actualEarthLoc);

          double dist = swathEarthLoc.distance (actualEarthLoc);
          if (!Double.isNaN (dist)) {
            min = Math.min (dist, min);
            max = Math.max (dist, max);
            if (dist == max) {
              maxErrorLoc = (DataLocation) dataLoc.clone();
            } // if
            sum += dist;
            valid++;
          } // if
          values++;
        
        } // while
        double mean = (valid == 0 ? Double.NaN : sum/valid);

        // Print statistics
        // ----------------
        System.out.println ("Locations checked = " + values);
        System.out.println ("Locations valid = " + valid);
        System.out.println ("Locations invalid = " + (values-valid));
        System.out.println ("Min error = " + min + " km");
        System.out.println ("Max error = " + max + " km");
        System.out.println ("Max error location = " + maxErrorLoc);
        System.out.println ("Mean error = " + mean + " km");

      } // else
    } // if
    else {
      System.out.println ("Found non-swath transform: " + trans);
    } // else

  } // main

  ////////////////////////////////////////////////////////////

} // SwathProjection class

////////////////////////////////////////////////////////////////////////
