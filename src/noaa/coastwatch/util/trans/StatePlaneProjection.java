////////////////////////////////////////////////////////////////////////
/*
     FILE: StatePlaneProjection.java
  PURPOSE: Handles State Plane map transformations.
   AUTHOR: Peter Hollemans
     DATE: 2012/11/02
  CHANGES: 2013/09/23, PFH
           - change: modified call to super to pass actual zone (not zero)
           - issue: cwmaster reverting to 0 for zone on Apply
           - change: added call to setDatum in constructor
           - issue: datum reported was incorrect, only set correctly in actual

  CoastWatch Software Library and Utilities
  Copyright 2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;
import java.io.*;
import java.nio.*;

/**
 * The <code>StatePlaneProjection</code> class performs 
 * State Plane map projection calculations.
 *
 * @author Peter Hollemans
 * @since 3.3.0
 */
public class StatePlaneProjection 
  extends GCTPCStyleProjection {
  
  // Constants
  // ---------

  /** The NAD 27 state plane zones. */
  static long NAD27[] = new long[] {
    101,102,5010,5300,201,202,203,301,302,401,402,403,404,
    405,406,407,501,502,503,600,700,901,902,903,1001,1002,5101,
    5102,5103,5104,5105,1101,1102,1103,1201,1202,1301,1302,1401,
    1402,1501,1502,1601,1602,1701,1702,1703,1801,1802,1900,2001,
    2002,2101,2102,2103,2111,2112,2113,2201,2202,2203,2301,2302,
    2401,2402,2403,2501,2502,2503,2601,2602,2701,2702,2703,2800,
    2900,3001,3002,3003,3101,3102,3103,3104,3200,3301,3302,3401,
    3402,3501,3502,3601,3602,3701,3702,3800,3901,3902,4001,4002,
    4100,4201,4202,4203,4204,4205,4301,4302,4303,4400,4501,4502,
    4601,4602,4701,4702,4801,4802,4803,4901,4902,4903,4904,5001,
    5002,5003,5004,5005,5006,5007,5008,5009,5201,5202,5400};

  /** The NAD 83 state plane zones. */
  static long NAD83[] = new long[] {
    101,102,5010,5300,201,202,203,301,302,401,402,403,
    404,405,406,0000,501,502,503,600,700,901,902,903,1001,1002,
    5101,5102,5103,5104,5105,1101,1102,1103,1201,1202,1301,1302,
    1401,1402,1501,1502,1601,1602,1701,1702,1703,1801,1802,1900,
    2001,2002,2101,2102,2103,2111,2112,2113,2201,2202,2203,2301,
    2302,2401,2402,2403,2500,0000,0000,2600,0000,2701,2702,2703,
    2800,2900,3001,3002,3003,3101,3102,3103,3104,3200,3301,3302,
    3401,3402,3501,3502,3601,3602,3701,3702,3800,3900,0000,4001,
    4002,4100,4201,4202,4203,4204,4205,4301,4302,4303,4400,4501,
    4502,4601,4602,4701,4702,4801,4802,4803,4901,4902,4903,4904,
    5001,5002,5003,5004,5005,5006,5007,5008,5009,5200,0000,5400};

  // Variables
  // ---------

  /** The actual projection used for the state plane transformations. */
  MapProjection actualProj;

  ////////////////////////////////////////////////////////////

  /**
   * Performs initialization of the projection constants.
   *
   * @param zone the zone number.
   * @param sphere the spheroid number.
   * @param fn27 the name of file containing the NAD27 parameters.
   * @param fn83 the name of file containing the NAD83 parameters.
   *
   * @return OK on success, or not OK on failure.   
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  private long projinit (
    long   zone,
    long   sphere,
    String fn27,
    String fn83
  ) throws NoninvertibleTransformException {

    long ind;                       // index for the zone
    int i;                          // loop control variable
    long nadval;                    // datum value for the report (27 or 83)
    double table[] = new double[9]; // array containing the projection information
    char pname[] = new char[32];    // projection name
    char buf[] = new char[100];     // buffer for error messages

    double r_maj, r_min, scale_fact, center_lon;
    double center_lat, false_east, false_north;
    double azimuth, lat_orig, lon_orig, lon1, lat1, lon2, lat2;
    long mode, iflg[] = new long[1];
    int id;
    
    ind = -1;
    lat1 = 0;
    lon1 = 0;
    lat2 = 0;
    lon2 = 0;
    
    /*Find the index for the zone
      --------------------------*/
    if (zone > 0)
       {
       if (sphere == 0)
          {
          for (i = 0; i < 134; i++)
             {
             if (zone == NAD27[i])
                {
                ind = i;
                break;
                }
             }
          }
       else
       if (sphere == 8)
          {
          for (i = 0; i < 134; i++)
             {
             if (zone == NAD83[i])
                {
                ind = i;
                break;
                }
             }
          }
       else
          {
          sprintf (buf,"Illegal spheroid #%4d", sphere);
          p_error (buf,"state-spheroid");
          return (23);
          }
       }
    if (ind == -1)
       {
       sprintf (buf,"Illegal zone #%4d  for spheroid #%4d", zone, sphere);
       p_error (buf,"state-init");
       return (21);
       }
    
    /*Open and read the parameter file to get this zone's parameters
      --------------------------------------------------------------*/
    RandomAccessFile file;
    try {
      if (sphere == 0)
         file = new RandomAccessFile (fn27, "r");
      else
         file = new RandomAccessFile (fn83, "r");
    } // try
    catch (FileNotFoundException e)
          {
          p_error ("Error opening State Plane parameter file","state-for");
          return (22);
          }

    try {
      file.seek (ind*432);
      for (i = 0; i < 32; i++)
        pname[i] = (char) file.readUnsignedByte();
      id = file.readInt();
      for (i = 0; i < 9; i++)
        table[i] = file.readDouble();
      file.close();
    } // try
    catch (IOException e) {
          {
          p_error ("Error reading State Plane parameter file","state-for");
          return (22);
          }
    } // catch

    if (id <= 0)
         {
         sprintf (buf,"Illegal zone #%4d  for spheroid #%4d", zone, sphere);
         p_error (buf,"state-init");
         return (21);
         }

    /*Report parameters to the user
      -----------------------------*/
    ptitle ("STATE PLANE");
    genrpt_long (zone,"Zone:     ");
    if (sphere == 0)
       nadval = 27;
    else
       nadval = 83;
    genrpt_long (nadval,"Datum:     NAD");
    
    r_maj = table[0];
    r_min = (Math.sqrt (1.0 - table[1]))*table[0];
    
    /*initialize proper projection
    -----------------------------*/
    if (id == 1)
       {
       scale_fact = table[3];
       center_lon = paksz (pakcz (table[2]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       center_lat = paksz (pakcz (table[6]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       false_east = table[7];
       false_north = table[8];
       actualProj = new TransverseMercatorProjection (
         r_maj, r_min,
         getDimensions(),
         getAffine(),
         scale_fact, center_lon, center_lat,
         false_east, false_north
       );
       }
    else
    if (id == 2)
       {
       lat1 = paksz (pakcz (table[5]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       lat2 = paksz (pakcz (table[4]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       center_lon = paksz (pakcz (table[2]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       center_lat = paksz (pakcz (table[6]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       false_east = table[7];
       false_north = table[8];
       actualProj = new LambertConformalConicProjection (
         r_maj, r_min,
         getDimensions(),
         getAffine(),
         lat1, lat2, center_lon, center_lat,
         false_east, false_north
       );
       }
    else
    if (id == 3)
       {
       center_lon = paksz (pakcz (table[2]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       center_lat = paksz (pakcz (table[3]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       false_east = table[4];
       false_north = table[5];
       actualProj = new PolyconicProjection (
         r_maj, r_min,
         getDimensions(),
         getAffine(),
         center_lon, center_lat,
         false_east, false_north
       );
       }
    else
    if (id == 4)
       {
       scale_fact = table[3];
       azimuth = paksz (pakcz (table[5]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       lon_orig = paksz (pakcz (table[2]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       lat_orig = paksz (pakcz (table[6]),iflg)*D2R;
       if (iflg[0] != 0)
           return (iflg[0]);
       false_east = table[7];
       false_north = table[8];
       mode = 1;
       actualProj = new HotineObliqueMercatorProjection (
         r_maj, r_min,
         getDimensions(),
         getAffine(),
         scale_fact, azimuth, lon_orig, lat_orig,
         lon1, lat1, lon2, lat2, mode,
         false_east, false_north
       );
       }

    return (OK);

  } // projinit

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a map projection from the specified projection and
   * affine transform.   The {@link SpheroidConstants} and
   * {@link ProjectionConstants} class should be consulted for
   * valid parameter constants.
   *
   * @param dimensions the dimensions of the data grid as <code>[rows,
   * columns]</code>.
   * @param affine the affine transform for translating data
   * <code>[row, column]</code> to map <code>[x, y]</code>.
   * @param zone the zone number.
   * @param sphere the spheroid number.
   * @param fn27 the name of file containing the NAD27 parameters.
   * @param fn83 the name of file containing the NAD83 parameters.
   *
   * @throws NoninvertibleTransformException if the map
   * projection to data coordinate affine transform is not
   * invertible.
   * @throws IllegalArgumentException if the paramaters have an inconsistency.
   */
  public StatePlaneProjection (
    int[] dimensions,
    AffineTransform affine,
    long   zone,                    // zone number
    long   sphere,                  // spheroid number
    String fn27,
    String fn83
  ) throws NoninvertibleTransformException {

    // Initialize
    // ----------
    super (SPCS, (int) zone, SpheroidConstants.STD_RADIUS,
      SpheroidConstants.STD_RADIUS, dimensions, affine);
    long result = projinit (zone, sphere, fn27, fn83);
    if (result != OK)
      throw new IllegalArgumentException ("Projection parameter inconsistency detected");

    // Set datum based on actual projection
    // ------------------------------------
    setDatum (actualProj.getDatum());

  } // StatePlaneProjection constructor

  ////////////////////////////////////////////////////////////

  protected long projfor (
    double lat,
    double lon,
    double x[],
    double y[]
  ) {

    /**
     * This is an illegal state to be in, because the actual
     * projection should be handling this call.
     */
    throw new IllegalStateException();
    
  } // projfor

  ////////////////////////////////////////////////////////////

  public void mapTransformFor (
    double[] lonLat,
    double[] xy
  ) {

    actualProj.mapTransformFor (lonLat, xy);

  } // mapTransformFor
  
  ////////////////////////////////////////////////////////////

  protected long projinv (
    double x,
    double y,
    double lon[],
    double lat[]
  ) {

    /**
     * This is an illegal state to be in, because the actual
     * projection should be handling this call.
     */
    throw new IllegalStateException();
    
  } // projinv

  ////////////////////////////////////////////////////////////

  public void mapTransformInv (
    double[] xy,
    double[] lonLat
  ) {

    actualProj.mapTransformInv (xy, lonLat);

  } // mapTransformInv
  
  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    StatePlaneProjection statePlaneProj = new StatePlaneProjection (
      new int[] {512, 512},
      new AffineTransform(),
      3800,
      0,
      IOServices.getFilePath (StatePlaneProjection.class, "nad27sp"),
      IOServices.getFilePath (StatePlaneProjection.class, "nad27sp")
    );
    
    // 41.7000° N, 71.5000° W (Rhode Island)

    System.out.println ("**********");
    Datum datum = statePlaneProj.getDatum();
    System.out.println ("datum = " + datum + " <-- (should be Clarke 1866)");
    EarthLocation earthLoc = new EarthLocation (41.7, -71.5, datum);
    System.out.println ("earthLoc = " + earthLoc + "<-- (should be 41.7, -71.5)");
    DataLocation dataLoc = statePlaneProj.transform (earthLoc);
    System.out.println ("dataLoc = " + dataLoc);
    EarthLocation newEarthLoc = statePlaneProj.transform (dataLoc);
    System.out.println ("newEarthLoc = " + newEarthLoc + "<-- (should be 41.7, -71.5)");
    
    System.out.println ("**********");
    int[] dims = new int[] {512, 512};
    MapProjection mapProj = MapProjectionFactory.getInstance().create (
      ProjectionConstants.SPCS,
      3800,
      new double[15],
      SpheroidConstants.CLARKE1866,
      dims, 
      earthLoc,
      new double[] {1, 1}
    );
    datum = mapProj.getDatum();
    System.out.println ("datum = " + datum + " <-- (should be Clarke 1866)");
    dataLoc = new DataLocation ((dims[Grid.ROWS]-1)/2.0, (dims[Grid.COLS]-1)/2.0);
    System.out.println ("dataLoc = " + dataLoc);
    EarthLocation centerLoc = mapProj.transform (dataLoc);
    System.out.println ("centerLoc = " + centerLoc + "<-- (should be 41.7, -71.5)");
    
  } // main

  ////////////////////////////////////////////////////////////

} // StatePlaneProjection

////////////////////////////////////////////////////////////////////////
