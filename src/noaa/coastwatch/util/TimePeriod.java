////////////////////////////////////////////////////////////////////////
/*
     FILE: TimePeriod.java
  PURPOSE: A class to represent a date and time duration.
   AUTHOR: Peter Hollemans
     DATE: 2004/07/02
  CHANGES: 2010/02/22, PFH, added Comparable interface

  CoastWatch Software Library and Utilities
  Copyright 2004-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;

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
