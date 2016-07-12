////////////////////////////////////////////////////////////////////////
/*
     FILE: L2PProjection.java
  PURPOSE: A class to perform L2P swath earth transform calculations.
   AUTHOR: X. Liu
     DATE: 2011/06/15
  CHANGES: 2011/06/15, XL, initial version
           2015/04/17, PFH
           - Changes: Deprecated this class.
           - Issue: We moved the support for L2P data projection to the
             CommonDataModelNCReader class.

  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

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
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.ValueSource;
import noaa.coastwatch.util.VariableEstimator;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.SwathProjection;

/**
 * The <code>L2PProjection</code> class implements earth transform
 * calculations for satellite swath (also called sensor scan) 2D
 * projections for L2P data. See also <code>SwathProjection</code>.
 *
 * @author Xiaoming Liu
 * @since 3.3.0
 *
 * @deprecated As of 3.3.1, GHRSST L2P data for which this class was designed
 * is handled by the {@link noaa.coastwatch.io.CommonDataModelNCReader} class.
 */
@Deprecated
public class L2PProjection
  extends EarthTransform2D{

  // Constants
  // ---------
  /** The default tolerance distance as a fraction of the resolution. */
  public static final double TOLERANCE_FACTOR = 0.1;

  /** The <code>transform(EarthLocation)</code> maximum iterations. */
  public static final int MAX_ITERATIONS = 1000;

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

  /** 
   * The last data coordinate from an earth location to data location
   * transform.  The idea here is to save the last coordinate
   * transform to use as a starting point for the next transform.
   * Generally, locations are transformed in polylines of Earth
   * locations that are fairly close together both on the earth and in
   * the data location space.
   */
  private DataLocation lastDataLoc = null;

  /** The list of seed data locations to try. */
  private DataLocation[] seedDataLocations;

  /** The list of seed earth locations to try. */
  private EarthLocation[] seedEarthLocations;
  
  /** The north flag, true if latitudes vary inversely as row index. */
  private boolean northIsUp;
  
  private DataVariable lat;
  private DataVariable lon;

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
  public L2PProjection (
    DataVariable lat,
    DataVariable lon,
    double maxSize,
    int[] maxDims
  ) {

	this.lat = lat;
	this.lon = lon;
    // Set north flag
    // --------------
    dims = lat.getDimensions();
    setNorthFlag (lat, dims);

    // Check for null mode
    // -------------------
    if (nullMode) return;

    // Create estimators
    // -----------------
    //EarthTransform trans = new DataProjection (lat, lon);
    //latEst = new VariableEstimator (lat, trans, maxSize, maxDims);
    //lonEst = new VariableEstimator (lon, new LongitudeFilter(), latEst);
    latEst = null;
    lonEst = null;

    // Initialize
    // ----------
    resetTolerance();
    resetArea();

  } // SwathProjection constructor

  ////////////////////////////////////////////////////////////

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

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
      earthLoc.setCoords (lat.interpolate(dataLoc), 
        lon.interpolate(dataLoc));
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

  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    // Check for containment
    // ---------------------
    if (area != null && !area.contains (earthLoc)) {
      lastDataLoc = null;
      dataLoc.set (Grid.ROWS, Double.NaN);
      dataLoc.set (Grid.COLS, Double.NaN);
      return;
    } // if

    // Initialize search using last or seed value
    // ------------------------------------------
    DataLocation testDataLoc = null;
    double d = Double.MAX_VALUE;
    if (lastDataLoc != null) {
      testDataLoc = lastDataLoc;
      d = earthLoc.distance (transform (testDataLoc));
    } // if
    for (int i = 0; i < seedEarthLocations.length; i++) {
      double seedDist = earthLoc.distance (seedEarthLocations[i]);
      if (seedDist < d) { testDataLoc = seedDataLocations[i]; d = seedDist; }
    } // for
    double lastd = d;
    double[] inc = {0.5, 0.5};
    int iter = 0;

    // Search until tolerance or max iterations hit
    // --------------------------------------------
    while (d > tolerance && iter < MAX_ITERATIONS) {

      // Compute increments in x and y
      // -----------------------------
      DataLocation x1 = testDataLoc.translate (-inc[0], 0).truncate (dims);
      DataLocation x2 = testDataLoc.translate (+inc[0], 0).truncate (dims);
      DataLocation y1 = testDataLoc.translate (0, -inc[1]).truncate (dims);
      DataLocation y2 = testDataLoc.translate (0, +inc[1]).truncate (dims);
      EarthLocation x1Geo = transform (x1);
      EarthLocation x2Geo = transform (x2);
      EarthLocation y1Geo = transform (y1);
      EarthLocation y2Geo = transform (y2);
      double dx = x2.get(0) - x1.get(0);
      double dy = y2.get(1) - y1.get(1);

      // Calculate resolution in km/pixel
      // --------------------------------
      double[] res = new double[] {
        x1Geo.distance (x2Geo) / dx,
        y1Geo.distance (y2Geo) / dy
      };

      // Calculate unit gradient
      // -----------------------
      double[] grad = new double[] {
        (earthLoc.distance (x2Geo) - earthLoc.distance (x1Geo)) / dx,
        (earthLoc.distance (y2Geo) - earthLoc.distance (y1Geo)) / dy
      };
      double mag = Math.sqrt (Math.pow (grad[0],2) + Math.pow (grad[1],2));
      grad[0] /= mag;
      grad[1] /= mag;

      // Estimate new coordinates
      // ------------------------
      double[] correction = new double[] {
        -(d/res[0]) * grad[0], 
        -(d/res[1]) * grad[1]
      };
      testDataLoc = testDataLoc.translate (correction).truncate (dims);
      EarthLocation tmp = transform (testDataLoc);
      d = earthLoc.distance (tmp);

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
    if (Double.isNaN(d) || d > tolerance) {
      lastDataLoc = null;
      dataLoc.set (Grid.ROWS, Double.NaN);
      dataLoc.set (Grid.COLS, Double.NaN);
    } // if
    
    // Return data coordinate
    // ----------------------
    else {
      lastDataLoc = testDataLoc;
      dataLoc.setCoords (testDataLoc);
      //dataLoc.setCoords(new double[]{1.e10,1.e10});
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
  public L2PProjection (
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
   * compared value by value.
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

} // SwathProjection class

////////////////////////////////////////////////////////////////////////

