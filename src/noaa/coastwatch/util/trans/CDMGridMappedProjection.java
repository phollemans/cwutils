////////////////////////////////////////////////////////////////////////
/*

     File: CDMGridMappedProjection.java
   Author: Peter Hollemans
     Date: 2015/12/24

  CoastWatch Software Library and Utilities
  Copyright (c) 2015 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.UnitFactory;

import java.util.List;
import java.util.Arrays;

import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.util.Parameter;
import ucar.units.ConversionException;
import ucar.units.Unit;

// Testing
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.LatLonProjection;
import java.io.StringReader;
import java.util.Formatter;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.test.TestLogger;
import java.io.File;
import java.io.PrintStream;

import java.util.logging.Logger;

/**
 * The <code>CDMGridMappedProjection</code> class wraps a Java NetCDF
 * CDM projection and allows access to transform calculations through the 
 * standard {@link noaa.coastwatch.util.trans.EarthTransform} interface.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class CDMGridMappedProjection
  extends EarthTransform2D {

  private static final Logger LOGGER = Logger.getLogger (CDMGridMappedProjection.class.getName());

  // Constants
  // ---------

  /** Projection description string. */
  public final static String DESCRIPTION = "cdm_grid_mapped";

  // Variables
  // ---------
  
  /** The X axis [scale, offset] for transforming index to projection coordinate. */
  private double[] xAxisTrans;

  /** The Y axis [scale, offset] for transforming index to projection coordinate. */
  private double[] yAxisTrans;

  /** The projection coordinate transform to use for (x,y) <--> (lat,lon) calculations. */
  private Projection proj;

  /** The datum detected for this projection. */
  private Datum datum;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the projection name.
   *
   * @return the projection name.
   *
   * @since 3.6.1
   */
  public String getName() {
  
    // We've found here that the name from the NetCDF library consists
    // of a class name which is not as readable as we'd like.  We make a few
    // changes to the format of the name to introduce spaces and remove the
    // word "Projection" if found.
    var name = proj.getName().replaceAll ("([a-z0-9])([A-Z])", "$1 $2")
      .replaceAll ("Projection", "").trim();
    
    return (name);
    
  } // getName

  ////////////////////////////////////////////////////////////

  /**
   * Gets the map pixel size.
   *
   * @return the pixel size in projection units.
   *
   * @since 3.6.1
   *
   * @see #getProjectionUnits
   */
  public double getPixelSize() {
  
    return (xAxisTrans[0]);
        
  } // getPixelSize

  ////////////////////////////////////////////////////////////

  /**
   * Gets the projection system units.
   *
   * @return the projection units that match the pixel size
   * (km, radians, or degrees) or null if they could not be determined.
   *
   * @since 3.6.1
   *
   * @see #getPixelSize
   */
  public String getProjectionUnits() {
  
    return (getProjectionUnits (proj));
        
  } // getProjectionUnits

  ////////////////////////////////////////////////////////////

  public String describe () { return (DESCRIPTION); }

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    return ("CDMGridMappedProjection[proj=" + proj + ",datum=" + datum + "]");

  } // toString

  ////////////////////////////////////////////////////////////

  /**
   * Checks if a coordinate system is compatible with this class.
   * 
   * @param system the grid coordinate system to test.
   *
   * @return true if the coordinate system can be used with this class
   * or false if not.
   */
  public static boolean isCompatibleSystem (
    GridCoordSystem system
  ) {

    return (system.isRegularSpatial() && !system.isLatLon());

  } // isCompatibleSystem

  ////////////////////////////////////////////////////////////

  /**
   * Guesses the projection coordinate units using a set of empirical
   * tests.
   *
   * @param proj the projection to get the units for.
   * 
   * @return the units (m, km, radians, or degrees) or null if they could not
   * be determined.
   */
  private static String getProjectionUnits (
    Projection proj
  ) {

    // What we're trying to do here is discover what units the projection
    // system uses.  We do this by calculating how far in kilometers one unit
    // in the projection system is.  If a distance of 1 projection unit is 1 km,
    // then the projection system uses kilometers.  

    // Start by getting the (lat,lon) at the projection origin. Then step 
    // one projection unit away in the projection X axis direction,
    // and get the (lat,lon) there.  Then see how far in kilometers that step
    // is away from the origin.
    var centerPoint = ProjectionPoint.create (0, 0);
    var centerLatLon = proj.projToLatLon (centerPoint);
    var unitXPoint = ProjectionPoint.create (1, 0);
    var unitXLatLon = proj.projToLatLon (unitXPoint);
    double unitXDist = EarthLocation.distance (
      centerLatLon.getLatitude(),
      centerLatLon.getLongitude(),
      unitXLatLon.getLatitude(),
      unitXLatLon.getLongitude()
    );

    LOGGER.fine ("Testing projection units distance unitXDist = " + unitXDist);

    // Now perform some tests -- if the distance is 1 km +/- 0.2 km, the units
    // are probably kilometers.  If the distance is 100 km +/- 20 km, the
    // units are probably degrees.
    String units = null;
    if (!Double.isNaN (unitXDist)) {
      if (Math.abs (1 - unitXDist) < 0.2) units = "km";
      else if (Math.abs (1 - unitXDist*1000) < 0.2) units = "m";
      else if (Math.abs (100 - unitXDist) < 20) units = "degrees";
    } // if

    // If neither of these are successful, we suspect that the projection is 
    // some kind of perspective orbit.  Calculate the distance in kilometers
    // when we move one projection unit, divided by the distance from
    // a geostationary satellite to the surface.  If we get 1 km then the
    // units are radians.
    if (units == null) {

      var radXPoint = ProjectionPoint.create (1.0/(42164.0 - 6378.137), 0);
      var radXLatLon = proj.projToLatLon (radXPoint);

      double radXDist = EarthLocation.distance (
        centerLatLon.getLatitude(),
        centerLatLon.getLongitude(),
        radXLatLon.getLatitude(),
        radXLatLon.getLongitude()
      );
      if (Math.abs (1 - radXDist) < 0.2) units = "radians";

      LOGGER.fine ("Testing projection units distance radXDist = " + radXDist);

    } // if

    // Similar calculation as the last one, but convert to degrees in the
    // projection point before calculating.  If we get 1 km in this case,
    // then the units are degrees.




// QUESTION: Why do we not just get the projection units directly from
// the projection axes?




    if (units == null) {

      var degXPoint = ProjectionPoint.create (Math.toDegrees (1.0/(42164.0 - 6378.137)), 0);
      var degXLatLon = proj.projToLatLon (degXPoint);

      double degXDist = EarthLocation.distance (
        centerLatLon.getLatitude(),
        centerLatLon.getLongitude(),
        degXLatLon.getLatitude(),
        degXLatLon.getLongitude()
      );
      if (Math.abs (1 - degXDist) < 0.2) units = "degrees";

      LOGGER.fine ("Testing projection units distance degXDist = " + degXDist);

    } // if

    if (units == null) LOGGER.warning ("Cannot determine projection system units");

    return (units);
  
  } // getProjectionUnits

  ////////////////////////////////////////////////////////////

  @Override
  public Datum getDatum () { return (datum); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets a parameter value from the list.
   *
   * @param paramList the list of parameters.
   * @param name the name of the parameter to return.
   *
   * @return the parameter value or Double.NaN if not found.
   */
  private static double getParameterValue (
    List<Parameter> paramList,
    String name
  ) {

    double value = Double.NaN;
    for (Parameter param : paramList) {
      if (param.getName().equals (name)) {
        value = param.getNumericValue();
        break;
      } // if
    } // for
    
    return (value);
  
  } // getParameterValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets a datum using a set of ellipsoid axis parameters.
   * 
   * @param rMajor the semi-major axis length in meters.
   * @param rMinor the semi-minor axis length in meters or Double.NaN to use 
   * the inverse flattening.
   * @param invFlat the inverse flattening or Double.NaN to use the 
   * semi-major axis.
   *
   * @return the datum corresponding to the parameters.
   */
  private static Datum getDatum (
    double rMajor,
    double rMinor,
    double invFlat
  ) {
  
    Datum datum;

    // Compute semi-minor axis if needed
    // ---------------------------------
    if (Double.isNaN (rMinor)) {
      if (Double.isInfinite (invFlat)) { rMinor = rMajor; }
      else { rMinor = rMajor - (rMajor/invFlat); }
    } // if
  
    // Compute inverse flattening if needed
    // ------------------------------------
    if (Double.isNaN (invFlat)) {
      if (rMajor == rMinor) { invFlat = Double.POSITIVE_INFINITY; }
      else { invFlat = rMajor/(rMajor - rMinor); }
    } // if

    // Get spheroid from axes
    // ----------------------
    int spheroid = getSpheroid (rMajor, rMinor);
    if (spheroid != -1)
      datum = DatumFactory.create (spheroid);

    // Create custom spheroid
    // ----------------------
    else {
      datum = new Datum ("User defined", "User defined", rMajor, invFlat, 0, 0, 0);
      LOGGER.warning ("Grid mapped projection using unknown datum: semi-major axis = " +
        rMajor + " m, semi-minor axis = " + rMinor + " m");
    } // else

    return (datum);

  } // getDatum

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scaling factor and offset for a coordinate axis.  The factor and
   * offset can be used to translate between an index and the projection
   * coordinate value used by a projection.  The units conversion specified
   * by the axis and projection system are taken into account in the
   * translation.
   * 
   * @param proj the projection to use for projection coordinate units.
   * @param axis the axis to retrieve the scaling factor and offset.
   *
   * @return the translation as [scale, offset].  To compute a projection
   * coordinate from an index, use <code>coord = offset + index*scale</code>.  
   * To compute an index, use <code>index = (coord - offset) / scale</code>.
   *
   * @throws IllegalArgumentException if there is a problem with units
   * compatibility between the projection system and declared axis units.
   * If the projection system units or axis units could not be determined,
   * it is assumed that they have the same units.
   */
  private static double[] getProjectionAxisTransform (
    Projection proj,
    CoordinateAxis1D axis
  ) {

    // Get scale and offset
    // --------------------
    double scale = axis.getIncrement();
    double offset = axis.getStart();

    // Perform units conversion if needed
    // ----------------------------------
    String projUnitStr = getProjectionUnits (proj);

    LOGGER.fine ("For axis " + axis.getAxisType() + ", scale = " + scale + ", offset = " + offset + ", units = " + projUnitStr);

    if (projUnitStr != null) {
      Unit projUnit = UnitFactory.create (projUnitStr);
      String axisUnitStr = axis.getUnitsString();
      if (axisUnitStr != null) {
        Unit axisUnit = UnitFactory.create (axisUnitStr);
        if (!axisUnit.isCompatible (projUnit))
          throw new IllegalArgumentException ("Axis " + axis.getShortName() +
            " units '" + axisUnitStr + "' incompatible with projection units '" +
            projUnitStr + "'");
        try {
          double factor = axisUnit.convertTo (1.0, projUnit);
          scale = scale*factor;
          offset = offset*factor;
        } // try
        catch (ConversionException e) {
          throw new RuntimeException (e);
        } // catch
      } // if
    } // if

    return (new double[] {scale, offset});
  
  } // getProjectionAxisTransform

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new projection.
   * 
   * @param coordSystem the grid coordinate system to use to create the
   * projection.
   *
   * @throws IllegalArgumentException if the coordinate system cannot be used
   * by this class.
   */
  public CDMGridMappedProjection (
    GridCoordSystem coordSystem
  ) {

    // Check coordinate system
    // -----------------------
    if (!isCompatibleSystem (coordSystem))
      throw new IllegalArgumentException ("Coordinate system must have 1D X and Y axes with regularly spaced coordinates");

    // Set dimensions
    // --------------
    CoordinateAxis1D xAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
    CoordinateAxis1D yAxis = (CoordinateAxis1D) coordSystem.getYHorizAxis();
    dims = new int[2];
    dims[Grid.ROWS] = yAxis.getShape (0);
    dims[Grid.COLS] = xAxis.getShape (0);

    // Set axis values
    // ---------------
    proj = coordSystem.getProjection();
    xAxisTrans = getProjectionAxisTransform (proj, xAxis);
    yAxisTrans = getProjectionAxisTransform (proj, yAxis);

    LOGGER.fine ("xAxisTrans [scale,offset] = " + Arrays.toString (xAxisTrans));
    LOGGER.fine ("yAxisTrans [scale,offset] = " + Arrays.toString (yAxisTrans));
    
    // Detect datum
    // ------------
    double rMajor, rMinor, invFlat;
    List<Parameter> paramList = proj.getProjectionParameters();
    double radius = getParameterValue (paramList, "earth_radius");
    if (!Double.isNaN (radius)) {
      rMajor = rMinor = radius;
      invFlat = Double.NaN;
    } // if
    else {
      rMajor = getParameterValue (paramList, "semi_major_axis");
      rMinor = getParameterValue (paramList, "semi_minor_axis");
      invFlat = getParameterValue (paramList, "inverse_flattening");
    } // else
    datum = getDatum (rMajor, rMinor, invFlat);

  } // CDMGridMappedProjection constructor

 ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {

    var geoPoint = LatLonPoint.create (earthLoc.lat, earthLoc.lon);
    var projPoint = proj.latLonToProj (geoPoint);
    dataLoc.set (Grid.COLS, (projPoint.getX() - xAxisTrans[1]) / xAxisTrans[0]);
    dataLoc.set (Grid.ROWS, (projPoint.getY() - yAxisTrans[1]) / yAxisTrans[0]);

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  protected void transformImpl (
    DataLocation dataLoc,
    EarthLocation earthLoc
  ) {

    double x = dataLoc.get (Grid.COLS)*xAxisTrans[0] + xAxisTrans[1];
    double y = dataLoc.get (Grid.ROWS)*yAxisTrans[0] + yAxisTrans[1];
    var projPoint = ProjectionPoint.create (x, y);
    var geoPoint = proj.projToLatLon (projPoint);
    earthLoc.lat = geoPoint.getLatitude();
    earthLoc.lon = geoPoint.getLongitude();

  } // transformImpl

  ////////////////////////////////////////////////////////////

  @Override
  public boolean equals (
    Object obj
  ) {

    // Check object instance
    // ---------------------
    if (!(obj instanceof CDMGridMappedProjection)) return (false);

    // Check projection and datum
    // --------------------------
    CDMGridMappedProjection otherObject = (CDMGridMappedProjection) obj;
    if (!this.proj.equals (otherObject.proj)) return (false);
    if (!this.datum.equals (otherObject.datum)) return (false);

    // Check axis transformations
    // --------------------------
    if (!Arrays.equals (this.xAxisTrans, otherObject.xAxisTrans)) return (false);
    if (!Arrays.equals (this.yAxisTrans, otherObject.yAxisTrans)) return (false);
    
    return (true);
    
  } // equals

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (CDMGridMappedProjection.class);

    // Create test dataset
    // -------------------
    logger.test ("Framework");

    var file = File.createTempFile ("CDMGridMappedProjection", ".nc");
    file.deleteOnExit();
    var output = new PrintStream (file);
    output.print (
"<?xml version='1.0' encoding='UTF-8'?>\n" + 
"<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
"  <dimension name='time' length='1' />\n" + 
"  <dimension name='nj' length='5500' />\n" + 
"  <dimension name='ni' length='5500' />\n" + 
"  <attribute name='Conventions' value='CF-1.6' />\n" + 
"  <variable name='lat' shape='nj ni' type='float'>\n" + 
"    <attribute name='comment' value='Latitude of retrievals' />\n" + 
"    <attribute name='long_name' value='latitude' />\n" + 
"    <attribute name='standard_name' value='latitude' />\n" + 
"    <attribute name='units' value='degrees_north' />\n" + 
"    <attribute name='valid_max' type='float' value='90.' />\n" + 
"    <attribute name='valid_min' type='float' value='-90.' />\n" + 
"  </variable>\n" + 
"  <variable name='lon' shape='nj ni' type='float'>\n" + 
"    <attribute name='comment' value='Longitude of retrievals' />\n" + 
"    <attribute name='long_name' value='longitude' />\n" + 
"    <attribute name='standard_name' value='longitude' />\n" + 
"    <attribute name='units' value='degrees_east' />\n" + 
"    <attribute name='valid_max' type='float' value='180.' />\n" + 
"    <attribute name='valid_min' type='float' value='-180.' />\n" + 
"  </variable>\n" + 
"  <variable name='perspective_proj' type='int'>\n" + 
"    <attribute name='grid_mapping_name' value='geostationary' />\n" + 
"    <attribute name='semi_major_axis' type='double' value='6378137.' />\n" + 
"    <attribute name='semi_minor_axis' type='double' value='6356752.3' />\n" + 
"    <attribute name='inverse_flattening' type='double' value='298.257024882281' />\n" + 
"    <attribute name='latitude_of_projection_origin' type='double' value='0.' />\n" + 
"    <attribute name='longitude_of_projection_origin' type='double' value='140.7' />\n" + 
"    <attribute name='perspective_point_height' type='double' value='42164000.' />\n" + 
"    <attribute name='sweep_angle_axis' value='y' />\n" + 
"  </variable>\n" + 
"  <variable name='sea_surface_temperature' shape='time nj ni' type='short'>\n" + 
"    <attribute name='add_offset' type='float' value='273.15' />\n" + 
"    <attribute name='comment' value='SST obtained by regression with buoy measurements' />\n" + 
"    <attribute name='coordinates' value='time x y lat lon' />\n" + 
"    <attribute name='long_name' value='sea surface skin temperature' />\n" + 
"    <attribute name='scale_factor' type='float' value='0.01' />\n" + 
"    <attribute name='source' value='NOAA' />\n" + 
"    <attribute name='standard_name' value='sea_surface_skin_temperature' />\n" + 
"    <attribute name='units' value='kelvin' />\n" + 
"    <attribute name='valid_max' type='short' value='32767' />\n" + 
"    <attribute name='valid_min' type='short' value='-32767' />\n" + 
"    <attribute name='_FillValue' type='short' value='-32768' />\n" + 
"    <attribute name='grid_mapping' value='perspective_proj' />\n" + 
"  </variable>\n" + 
"  <variable name='time' shape='time' type='int'>\n" + 
"    <attribute name='comment' value='seconds since 1981-01-01 00:00:00' />\n" + 
"    <attribute name='long_name' value='reference time of sst file' />\n" + 
"    <attribute name='standard_name' value='time' />\n" + 
"    <attribute name='units' value='seconds since 1981-01-01 00:00:00' />\n" + 
"    <attribute name='calendar' value='Gregorian' />\n" + 
"    <attribute name='axis' value='T' />\n" + 
"  </variable>\n" + 
"  <variable name='x' shape='ni' type='double'>\n" + 
"    <attribute name='standard_name' value='projection_x_coordinate' />\n" + 
"    <attribute name='units' value='degrees' />\n" + 
"    <values start='-8.80430034288115' increment='0.00320214596940' />\n" + 
"  </variable>\n" + 
"  <variable name='y' shape='nj' type='double'>\n" + 
"    <attribute name='standard_name' value='projection_y_coordinate' />\n" + 
"    <attribute name='units' value='degrees' />\n" + 
"    <values start='8.80430034288115' increment='-0.00320214596940' />\n" +
"  </variable>\n" + 
"</netcdf>\n"
    );
    output.close();

    // NetcdfDataset dataset = NcMLReader.readNcML (reader, null);
    // assert (dataset != null);
    NetcdfDataset dataset = NetcdfDataset.openDataset (file.getPath());
    assert (dataset != null);

    // Get grid coordinate system
    // --------------------------
    Formatter errorLog = new Formatter();
    GridDataset gridDataset = (GridDataset) FeatureDatasetFactoryManager.wrap (
      FeatureType.GRID, dataset, null, errorLog);
    assert (gridDataset != null);
    List<GridDataset.Gridset> gridsets = gridDataset.getGridsets();
    assert (gridsets.size() == 1);
    GridDataset.Gridset gridset = gridsets.get (0);
    GridCoordSystem system = gridset.getGeoCoordSystem();
    logger.passed();
    
    // Test methods
    // ------------
    logger.test ("isCompatibleSystem");
    assert (isCompatibleSystem (system));
    logger.passed();

    logger.test ("getProjectionUnits");
    Projection proj = system.getProjection();
    String units;
    units = getProjectionUnits (proj);
    assert (units != null);
    assert (units.equals ("degrees"));
    Projection mercator = new Mercator();
    units = getProjectionUnits (mercator);
    assert (units != null);
    assert (units.equals ("km"));
    Projection latlon = new LatLonProjection();
    units = getProjectionUnits (latlon);
    assert (units != null);
    assert (units.equals ("degrees"));
    logger.passed();

    logger.test ("getDatum");
    assert (getDatum (6378137.0, 6356752.3, 298.257024882281).equals (DatumFactory.create (SpheroidConstants.GRS1980)));
    assert (getDatum (6378137.0, Double.NaN, 298.257024882281).equals (DatumFactory.create (SpheroidConstants.GRS1980)));
    assert (getDatum (6378137.0, 6356752.3, Double.NaN).equals (DatumFactory.create (SpheroidConstants.GRS1980)));
    logger.passed();

    logger.test ("getProjectionAxisTransform");
    double[] trans = getProjectionAxisTransform (proj, (CoordinateAxis1D) system.getXHorizAxis());
    assert (Math.abs ((trans[0] - 0.00320214596940)/trans[0]) < 0.01);
    assert (Math.abs ((trans[1] - (-8.80430034288115))/trans[1]) < 0.01);
    trans = getProjectionAxisTransform (proj, (CoordinateAxis1D) system.getYHorizAxis());
    assert (Math.abs ((trans[0] - (-0.00320214596940))/trans[0]) < 0.01);
    assert (Math.abs ((trans[1] - 8.80430034288115)/trans[1]) < 0.01);
    logger.passed();

    // Create projection
    // -----------------
    logger.test ("constructor");
    CDMGridMappedProjection mapProj = new CDMGridMappedProjection (system);
    EarthLocation center = mapProj.transform (new DataLocation (5499/2.0, 5499/2.0));
    assert (Math.abs (center.lat) < 1e-5);
    assert (Math.abs (center.lon - 140.7) < 1e-5);
    EarthLocation offsetLon = mapProj.transform (new DataLocation (5499/2.0, 5499/2.0-1));
    assert (Math.abs (offsetLon.lat) < 1e-5);
    assert (Math.abs (offsetLon.lon - (140.7 - 0.01796)) < 1e-4);
    EarthLocation offsetLat = mapProj.transform (new DataLocation (5499/2.0-1, 5499/2.0));
    assert (Math.abs (offsetLat.lat - 0.0180873890) < 1e-4);
    assert (Math.abs (offsetLat.lon - 140.7) < 1e-5);
    logger.passed();
    
  } // main

  ////////////////////////////////////////////////////////////

} // CDMGridMappedProjection class

////////////////////////////////////////////////////////////////////////
