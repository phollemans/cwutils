////////////////////////////////////////////////////////////////////////
/*
     FILE: DataProjection.java
  PURPOSE: To act as a projection based on explicit lat/lon data.
   AUTHOR: Peter Hollemans
     DATE: 2002/05/31
  CHANGES: 2002/07/25, PFH, converted to location classes
           2003/11/22, PFH, fixed Javadoc comments
           2004/10/05, PFH, modified for getDimensions() in EarthTransform
           2005/05/16, PFH, modified for in-place transform
           2005/05/20, PFH, now extends 2D transform
           2007/04/24, PFH, added getLat() and getLon()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util.trans;

// Imports
// -------
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;

/**
 * The <code>DataProjection</code> class implements Earth transform
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

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    earthLoc.setCoords (lat.getValue (dataLoc), lon.getValue (dataLoc));

  } // transformImpl

  ////////////////////////////////////////////////////////////

  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    dataLoc.set (Grid.ROWS, Double.NaN);
    dataLoc.set (Grid.COLS, Double.NaN);

  } // transformImpl

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
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof DataProjection)) return (false);

    // Check each lat/lon value
    // ------------------------
    DataProjection data = (DataProjection) obj;
    int n = this.lat.getValues ();
    for (int i = 0; i < n; i++) {
      if (this.lat.getValue (i) != data.lat.getValue (i)) return (false);
      if (this.lon.getValue (i) != data.lon.getValue (i)) return (false);
    } // for

    return (true);

  } // equals

  ////////////////////////////////////////////////////////////

} // DataProjection class

////////////////////////////////////////////////////////////////////////
