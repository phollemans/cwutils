////////////////////////////////////////////////////////////////////////
/*

     File: TimePeriod.java
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
import java.util.Date;

/**
 * The <code>TimePeriod</code> class is a container for a date and 
 * length of time.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public class TimePeriod implements Comparable {

  // Variables
  // ---------

  /** The time period start date. */
  private Date startDate;

  /** The time period duration in milliseconds. */
  private long duration;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new time period.
   *
   * @param startDate the time period starting date.
   * @param duration the time period duration in milliseconds.
   */
  public TimePeriod (
    Date startDate,
    long duration
  ) {

    this.startDate = (Date) startDate.clone();
    this.duration = duration;

  } // TimePeriod constructor

  ////////////////////////////////////////////////////////////

  /** Gets the time period starting date. */
  public Date getStartDate () { return ((Date) startDate.clone()); }

  ////////////////////////////////////////////////////////////

  /** Gets the time period duration in milliseconds. */
  public long getDuration () { return (duration); }

  ////////////////////////////////////////////////////////////

  /** Gets the time period ending date. */
  public Date getEndDate () { 

    return (new Date (startDate.getTime() + duration));

  } // getEndDate

  ////////////////////////////////////////////////////////////

  public int compareTo (
    Object o
  ) throws ClassCastException {

    return (this.startDate.compareTo (((TimePeriod) o).startDate));

  } // compareTo

  ////////////////////////////////////////////////////////////

} // TimePeriod class

////////////////////////////////////////////////////////////////////////
