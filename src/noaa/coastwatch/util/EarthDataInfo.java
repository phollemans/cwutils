////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataInfo.java
   Author: Peter Hollemans
     Date: 2004/07/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.MetadataContainer;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.SolarZenith;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.trans.EarthTransform;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>EarthDataInfo</code> class is a container for global
 * metadata pertaining to a number of {@link DataVariable} objects.
 * The required global metadata includes the data source, the date and
 * duration of data recording (possible more than one), and the {@link
 * EarthTransform} object used to translate between data array
 * coordinates and geographic coordinates.  Other metadata may also be
 * attached using the inherited <code>TreeMap</code> functions and
 * Java <code>String</code> objects as keys.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public class EarthDataInfo
  extends MetadataContainer {

  private static final Logger LOGGER = Logger.getLogger (EarthDataInfo.class.getName());

  // Variables
  // ---------

  /** The data source. */
  private String source;

  /** The time period list. */
  private List<TimePeriod> periodList;

  /** Earth transform object. */
  private EarthTransform trans;

  /** Original producer of the data. */
  private String origin;

  /** History of processing commands leading to the data. */
  private String history;

  ////////////////////////////////////////////////////////////

  /** Gets the data source. */
  public String getSource () { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Collapses the list of time periods in the metadata to a single
   * period covering the full time range of data recording.
   *
   * @since 3.5.1
   */
  public void collapseTimePeriods () {


// TODO: What if we have a set of time periods that encompass each other?
// Then we'd need to set the start date as below, but the end date would
// need to be determined by looking at all the time periods and selecting
// the last end date.  Actually, this should be implemented as part of
// getEndDate().  We have been assuming up until now time periods that don't
// overlap.



    TimePeriod fullPeriod = new TimePeriod (
      getStartDate(),
      getEndDate()
    );
    periodList = new ArrayList<>();
    periodList.add (fullPeriod);

  } // collapseTimePeriods

  ////////////////////////////////////////////////////////////

  /**
   * Clears the history of processing commands.
   *
   * @since 3.7.0
   */
  public void clearHistory () {

    this.history = "";
    var map = getMetadataMap();
    if (map.containsKey ("history")) map.put ("history", "");
  
  } // clearHistory

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the data recording date.  Since data recording may have
   * occurred over a number of different time periods, the date
   * returned is the first date in the time period list.
   *
   * @see #getTimePeriods 
   */
  public Date getDate () { 

    TimePeriod firstPeriod = Collections.min (periodList);
    return (firstPeriod.getStartDate());

  } // getDate

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data recording start date, the same value as the
   * getDate() method.
   *
   * @see #getDate
   * @see #getEndDate
   */
  public Date getStartDate () { return (getDate()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data recording ending date.
   *
   * @see #getStartDate
   */
  public Date getEndDate () { 

    TimePeriod lastPeriod = Collections.max (periodList);
    return (lastPeriod.getEndDate());

  } // getEndDate

  ////////////////////////////////////////////////////////////

  /**
   * Returns true if the data recording originated from one date
   * and time with essentially no data recording duration.
   */
  public boolean isInstantaneous () {

    boolean instantaneous = false;
    if (periodList.size() == 1) {
      TimePeriod firstPeriod = periodList.get (0);
      instantaneous = (firstPeriod.getDuration() == 0);
    } // if

    return (instantaneous);

  } // isInstantaneous

  ////////////////////////////////////////////////////////////

  /** 
   * Formats the first date.  This method mainly exists as legacy code
   * for classes that have no knowledge of multi-temporal data and may
   * be deprecated in the future.
   *
   * @see #getDate
   * @see DateFormatter#formatDate(Date,String)
   */
  public String formatDate (
    String format
  ) {

    return (DateFormatter.formatDate (getDate(), format));

  } // formatDate  

  ////////////////////////////////////////////////////////////

  /** 
   * Formats the first date.  This method mainly exists as legacy code
   * for classes that have no knowledge of multi-temporal data and may
   * be deprecated in the future.
   *
   * @see #getDate
   * @see DateFormatter#formatDate(Date,String,TimeZone)
   */
  public String formatDate (
    String format,
    TimeZone zone
  ) {

    return (DateFormatter.formatDate (getDate(), format, zone));

  } // formatDate  

  ////////////////////////////////////////////////////////////

  /** 
   * Formats the first date.  This method mainly exists as legacy code
   * for classes that have no knowledge of multi-temporal data and may
   * be deprecated in the future.
   *
   * @see #getDate
   * @see DateFormatter#formatDate(Date,String,EarthLocation)
   */
  public String formatDate (
    String format,
    EarthLocation loc
  ) {

    return (DateFormatter.formatDate (getDate(), format, loc));

  } // formatDate  

  ////////////////////////////////////////////////////////////

  /**
   * Gets the list of time periods for data recording.
   *
   * @return the list of time periods.
   *
   * @see #getDate
   * @see #setTimePeriods
   */
  public List<TimePeriod> getTimePeriods () {

    return (new ArrayList<> (periodList));

  } // getTimePeriods

  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of time periods for data recording.
   *
   * @param periodList the new list of time periods.
   *
   * @see #getTimePeriods
   */
  public void setTimePeriods (
    List<TimePeriod> periodList
  ) {

    this.periodList = new ArrayList<TimePeriod> (periodList);

  } // setTimePeriods

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform object. */
  public EarthTransform getTransform () { return (trans); }

  ////////////////////////////////////////////////////////////

  /** Gets the data origin. */
  public String getOrigin () { return (origin); }

  ////////////////////////////////////////////////////////////

  /** Gets the data command history. */
  public String getHistory () { return (history); }

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new earth data info object with the specified
   * properties.
   * 
   * @param source the data source.  The source name should be a data
   * collection instrument or numerical model name using to collect or
   * generate the data.
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
   * @see #EarthDataInfo(String,List,EarthTransform,String,String)
   */
  public EarthDataInfo (
    String source,
    List<TimePeriod> periodList,
    EarthTransform trans,
    String origin,
    String history
  ) {

    this.source = source;
    this.periodList = new ArrayList<> (periodList);
    this.trans = trans;
    this.origin = origin;
    this.history = history;

  } // EarthDataInfo constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new earth data info object with the specified
   * properties.
   * 
   * @param source the data source.  The source name should be a data
   * collection instrument or numerical model name using to collect or
   * generate the data.
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
   * @see #EarthDataInfo(String,List,EarthTransform,String,String)
   */
  public EarthDataInfo (
    String source,
    Date date,
    EarthTransform trans,
    String origin,
    String history
  ) {

    this (source, Arrays.asList (new TimePeriod[] {new TimePeriod (date, 0)}),
      trans, origin, history);

  } // EarthDataInfo constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the earth transform.
   *
   * @param trans the new earth transform to use.
   */
  public void setTransform (
    EarthTransform trans
  ) {

    this.trans = trans;

  } // setTransform

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scene time in terms of day or night for the specified 2D
   * scene bounds.  The date and time used to determine the scene time
   * is the starting date of the first time period.
   *
   * @param upperLeft the upper-left 2D scene boundary. 
   * @param lowerRight the lower-right 2D scene boundary. 
   *
   * @return a descriptive scene time string. The scene time is
   * <code>day</code> for daytime scenes, <code>night</code> for
   * nighttime scenes, or <code>day/night</code> for a scene that
   * crosses the terminator.
   */
  public String getSceneTime (
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    // Check for a null transform
    // --------------------------
    if (trans == null) return ("Unknown");

    // Create solar zenith object
    // --------------------------
    SolarZenith sz = new SolarZenith (getDate());

    // Get boundary points
    // -------------------
    double[] rows = new double[] {
      upperLeft.get(Grid.ROWS), 
      (upperLeft.get(Grid.ROWS) + lowerRight.get(Grid.ROWS)) / 2,
      lowerRight.get(Grid.ROWS)
    };
    double[] cols = new double[] {
      upperLeft.get(Grid.COLS), 
      (upperLeft.get(Grid.COLS) + lowerRight.get(Grid.COLS)) / 2,
      lowerRight.get(Grid.COLS)
    };

    // Loop over each point
    // --------------------
    int sum = 0;
    for (int i = 0; i < rows.length; i++) {
      for (int j = 0; j < cols.length; j++) {
        DataLocation loc = new DataLocation (rows[i], cols[j]);
        double elev = 90 - sz.getSolarZenith (trans.transform (loc));
        sum += (elev < 0 ? -1 : 1);
      } // for
    } // for

    // Return scene time
    // -----------------
    int points = rows.length*cols.length;
    if (sum == -points) return ("night");
    else if (sum == points) return ("day");
    else return ("day/night");

  } // getSceneTime

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scene time in terms of day or night for the specified 2D
   * scene dimensions.  The date and time used to determine the scene
   * time is the starting date of the first time period.
   *
   * @param dims the scene boundary dimensions as [rows, columns].
   *
   * @return a descriptive scene time string. The scene time is
   * <code>day</code> for daytime scenes, <code>night</code> for
   * nighttime scenes, or <code>day/night</code> for a scene that
   * crosses the terminator.
   */
  public String getSceneTime (
    int[] dims
  ) {

    DataLocation upperLeft = new DataLocation (0, 0);
    DataLocation lowerRight = new DataLocation (dims[Grid.ROWS]-1,
      dims[Grid.COLS]-1);
    return (getSceneTime (upperLeft, lowerRight));

  } // getSceneTime

  ////////////////////////////////////////////////////////////

  /**
   * Appends a command line to the history attribute.
   * 
   * @param command the command or program name.
   * @param argv an array of command line arguments.
   */
  public void updateHistory (
    String command,
    String[] argv
  ) {

    // Append command line to history
    // ------------------------------
    history = MetadataServices.append (history, 
      MetadataServices.getCommandLine (command, argv));

  } // updateHistory

  ////////////////////////////////////////////////////////////

  public Object clone () {

    EarthDataInfo info = (EarthDataInfo) super.clone();
    info.periodList = new ArrayList<> (periodList);
    return (info);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Appends another info object to this one in duplicate removal mode.
   *
   * @see #append(EarthDataInfo, boolean)
   *
   * @since 3.5.0
   */
  public EarthDataInfo appendWithoutDuplicates (
    EarthDataInfo appendInfo
  ) {
  
    return (append (appendInfo, false));
  
  } // appendWithoutDuplicates

  ////////////////////////////////////////////////////////////

  /**
   * Appends another info object to this one in duplicate preserving mode.
   *
   * @see #append(EarthDataInfo, boolean)
   *
   * @since 3.5.0
   */
  public EarthDataInfo appendWithDuplicates (
    EarthDataInfo appendInfo
  ) {
  
    return (append (appendInfo, true));
  
  } // appendWithDuplicates

  ////////////////////////////////////////////////////////////

  /**
   * Appends another info object to this one.
   * 
   * @param appendInfo the info object to append.
   * @param duplicatePreserving the dupicate mode flag, true if metadata should be
   * appended exactly so that duplicate values are preserved, false if
   * not.
   * 
   * @return the newly created object.
   *
   * @throws IllegalArgumentException if the classes or earth
   * transforms for this object and the object to append do not match.
   */
  public EarthDataInfo append (
    EarthDataInfo appendInfo,
    boolean duplicatePreserving
  ) {

    // Check types
    // -----------
    if (!this.getClass().equals (appendInfo.getClass()))
      throw new IllegalArgumentException ("Classes do not match");

    // Check earth transforms
    // ----------------------
    if (!this.getTransform().equals (appendInfo.getTransform()))
      throw new IllegalArgumentException ("Transforms do not match");

    // Create new info
    // ---------------
    EarthDataInfo newInfo = (EarthDataInfo) this.clone();

    // Append standard attributes
    // --------------------------
    newInfo.source = MetadataServices.append (newInfo.source, appendInfo.source);
    if (!duplicatePreserving)
      newInfo.source = MetadataServices.collapse (newInfo.source);
    newInfo.periodList.addAll (appendInfo.periodList);
    newInfo.origin = MetadataServices.append (newInfo.origin, appendInfo.origin);
    if (!duplicatePreserving)
      newInfo.origin = MetadataServices.collapse (newInfo.origin);
    newInfo.history = MetadataServices.append (newInfo.history, appendInfo.history);

    // Append user attributes
    // ----------------------
    Iterator iter = newInfo.getMetadataMap().entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      Object newValue = MetadataServices.append (entry.getValue(), 
        appendInfo.getMetadataMap().get (entry.getKey()));
      if (!duplicatePreserving) newValue = MetadataServices.collapse (newValue);
      entry.setValue (newValue);
    } // for

    // Set composite attribute
    // -----------------------

    // TODO: This should probably be an inherent property of the info
    // object.  At least, we should modify the EarthDataInfo class and
    // DataVariable class to extend MetadataContainer and come up with
    // some "official" way of having required, optional, and user
    // metadata.

    newInfo.getMetadataMap().put ("composite", "true");

    return (newInfo);

  } // append

  ////////////////////////////////////////////////////////////

} // EarthDataInfo class

////////////////////////////////////////////////////////////////////////
