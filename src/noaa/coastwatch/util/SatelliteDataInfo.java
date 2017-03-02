////////////////////////////////////////////////////////////////////////
/*

     File: SatelliteDataInfo.java
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
package noaa.coastwatch.util;

// Imports
// -------
import java.util.Date;
import java.util.List;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>SatelliteDataInfo</code> class is a special
 * <code>EarthDataInfo</code> class for satellite data.  It adds extra
 * metadata for the data source satellite and sensor.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class SatelliteDataInfo
  extends EarthDataInfo {

  // Variables
  // ---------

  /** The satellite name. */
  private String sat;

  /** The sensor name. */
  private String sensor;

  ////////////////////////////////////////////////////////////

  /** Gets the satellite name. */
  public String getSatellite () { return (sat); }

  ////////////////////////////////////////////////////////////

  /** Gets the sensor name. */
  public String getSensor () { return (sensor); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new satellite data info object with the specified
   * properties.
   * 
   * @param sat a short satellite name.  The satellite name should
   * reflect the agency and series or model number that the satellite
   * is commonly known by, for example <code>noaa-16</code> or
   * <code>orbview-2</code>.
   * @param sensor a short sensor name.  The sensor name is usually an
   * acronym for the instrument on the satellite that recorded the
   * data, for example <code>avhrr</code> or <code>seawifs</code>.
   * @param date the data recording date.  It is assumed that the data
   * was recorded on the specified date and time, with essentially no
   * time duration.
   * @param trans the earth transform.  The transform specifies the
   * translation between data array coordinates and geographic
   * coordinates.  The parameter is null if no transform is known.
   * @param origin the original data producer.  The origin should be
   * specified as accurately as possible to reflect the agency and
   * division that initially processed and formatted the data.
   * @param history the data command history.  The history is a
   * newline separated list of commands and parameters that lead to
   * the creation of the data.
   *
   * @see #SatelliteDataInfo(String,String,List,EarthTransform,String,String)
   */
  public SatelliteDataInfo (
    String sat,
    String sensor,
    Date date,
    EarthTransform trans,
    String origin,
    String history
  ) {

    super (sat + " " + sensor, date, trans, origin, history);
    this.sat = sat;
    this.sensor = sensor;

  } // SatelliteDataInfo constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new satellite data info object with the specified
   * properties.
   * 
   * @param sat a short satellite name.  The satellite name should
   * reflect the agency and series or model number that the satellite
   * is commonly known by, for example <code>noaa-16</code> or
   * <code>orbview-2</code>.
   * @param sensor a short sensor name.  The sensor name is usually an
   * acronym for the instrument on the satellite that recorded the
   * data, for example <code>avhrr</code> or <code>seawifs</code>.
   * @param periodList the list of data recording time periods.
   * @param trans the earth transform.  The transform specifies the
   * translation between data array coordinates and geographic
   * coordinates.  The parameter is null if no transform is known.
   * @param origin the original data producer.  The origin should be
   * specified as accurately as possible to reflect the agency and
   * division that initially processed and formatted the data.
   * @param history the data command history.  The history is a
   * newline separated list of commands and parameters that lead to
   * the creation of the data.
   *
   * @see #SatelliteDataInfo(String,String,Date,EarthTransform,String,String)
   */
  public SatelliteDataInfo (
    String sat,
    String sensor,
    List periodList,
    EarthTransform trans,
    String origin,
    String history
  ) {

    super (sat + " " + sensor, periodList, trans, origin, history);
    this.sat = sat;
    this.sensor = sensor;

  } // SatelliteDataInfo constructor

  ////////////////////////////////////////////////////////////

  /**
   * Appends another info object to this one.
   * 
   * @param appendInfo the info object to append.
   * @param pedantic the pedantic flag, true if metadata should be
   * appended exactly so that duplicate values are preserved, false if
   * not.
   * 
   * @return the newly created object.
   *
   * @throws IllegalArgumentException if the classes or Earth
   * transforms for this object and the object to append do not match.
   */
  public EarthDataInfo append (
    EarthDataInfo appendInfo,
    boolean pedantic
  ) {

    // Append superclass info
    // ----------------------
    SatelliteDataInfo newInfo = (SatelliteDataInfo) super.append (
      appendInfo, pedantic);

    // Append satellite-specific info
    // ------------------------------
    newInfo.sat = MetadataServices.append (newInfo.sat, 
      ((SatelliteDataInfo) appendInfo).sat);
    if (!pedantic) 
      newInfo.sat = MetadataServices.collapse (newInfo.sat);
    newInfo.sensor = MetadataServices.append (newInfo.sensor, 
      ((SatelliteDataInfo) appendInfo).sensor);
    if (!pedantic)
      newInfo.sensor = MetadataServices.collapse (newInfo.sensor);

    return (newInfo);

  } // append

  ////////////////////////////////////////////////////////////

} // SatelliteDataInfo class

////////////////////////////////////////////////////////////////////////
