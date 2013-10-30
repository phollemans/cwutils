////////////////////////////////////////////////////////////////////////
/*
     FILE: SatelliteDataInfo.java
  PURPOSE: A class to hold all the essential global satellite
           information, along with additional attributes if needed.
           The hash map acts as a catch-all container for attribute
           key/value pairs.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/05/14, PFH, added javadoc, package
           2002/05/17, PFH, added extra metadata
           2002/10/04, PFH, added dateFormat with time zone option
           2002/10/29, PFH, added solar zenith calculations
           2002/11/12, PFH, replaced pass type with scene time, added
             new constructor
           2002/11/28, PFH, added check for null transform in getSceneTime
           2003/01/13, PFH, added initialize for sub-classes
           2003/01/15, PFH, moved solar zenith calculations to another class
           2004/05/05, PFH, added clone() method
           2004/07/02, PFH, moved main functionality to parent class
           2004/09/14, PFH, removed constructor accepting transform
           2004/09/27, PFH, added append() to override parent

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import noaa.coastwatch.util.trans.*;

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
   * @param trans the Earth transform.  The transform specifies the
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
   * @param trans the Earth transform.  The transform specifies the
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
