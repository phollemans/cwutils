////////////////////////////////////////////////////////////////////////
/*
     FILE: LandMask.java
  PURPOSE: Gets land mask data from a pre-rendered database.
   AUTHOR: Peter Hollemans
     DATE: 2004/12/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>LandMask</code> class may be used to retrieve a true or
 * false value for the presence of land at a certain Earth location.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class LandMask {

  // Constants
  // ---------

  /** The database names. */
  private String[] DATABASES = new String[] {
    "north.hdf",
    "south.hdf",
    "east1.hdf",
    "east2.hdf",
    "west1.hdf",
    "west2.hdf"
  };

  /** The land variable name. */
  private static final String VARIABLE = "land";

  /** The database north index. */
  private static final int NORTH = 0;

  /** The database south index. */
  private static final int SOUTH = 1;

  /** The database east #1 index. */
  private static final int EAST1 = 2;

  /** The database east #2 index. */
  private static final int EAST2 = 3;

  /** The database west #1 index. */
  private static final int WEST1 = 4;

  /** The database west #1 index. */
  private static final int WEST2 = 5;

  // Variables
  // ---------

  /** The one and only instance. */
  private static LandMask instance;

  /** The array of grids. */
  private Grid[] gridArray;

  /** The array of Earth transforms. */
  private EarthTransform[] transArray;

  ////////////////////////////////////////////////////////////

  /** Gets an instance of the <code>LandMask</code> class. */
  public static LandMask getInstance () {

    if (instance == null) instance = new LandMask();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  /** Gets the database index for the specified Earth location. */
  private int getDatabase (
    EarthLocation loc
  ) {

    int lat = (int) Math.floor (loc.lat);
    if (lat == 90) lat = 89;
    int latIndex = (lat+90) / 45;
    switch (latIndex) {
    case 0: return (SOUTH);
    case 1:
    case 2:
      int lon = (int) Math.floor (loc.lon);
      int lonIndex = (lon+180) / 90;
      switch (lonIndex) {
      case 0: return (WEST2);
      case 1: return (WEST1);
      case 2: return (EAST1);
      case 3: return (EAST2);
      default: throw new IllegalArgumentException ("Invalid Earth location");
      } // switch
    case 3: return (NORTH);
    default: throw new IllegalArgumentException ("Invalid Earth location");
    } // switch

  } // getDatabase

  ////////////////////////////////////////////////////////////

  /** Creates a new <code>LandMask</code> object. */
  private LandMask () {

    // Initialize arrays
    // -----------------
    gridArray = new Grid[DATABASES.length];
    transArray = new EarthTransform[DATABASES.length];

  } // LandMask constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the land flag for a specified Earth location.  
   * 
   * @param loc the Earth location for the land flag.
   *
   * @return true if land is present, false if not.
   */
  public boolean isLand (
    EarthLocation loc
  ) {

    // Load database
    // -------------
    int index = getDatabase (loc);
    if (gridArray[index] == null) {
      try {
        String path = IOServices.getFilePath (getClass(), DATABASES[index]);
        EarthDataReader reader = EarthDataReaderFactory.create (path);
        gridArray[index] = (Grid) reader.getVariable (VARIABLE);
        transArray[index] = reader.getInfo().getTransform();
      } // try
      catch (Exception e) {
        throw new RuntimeException (e.getMessage());
      } // catch
    } // if
      
    // Get land value
    // --------------
    DataLocation dataLoc = transArray[index].transform (loc);
    return (((byte) gridArray[index].getValue (dataLoc)) != 0);

  } // isLand

  ////////////////////////////////////////////////////////////

} // LandMask class

////////////////////////////////////////////////////////////////////////
