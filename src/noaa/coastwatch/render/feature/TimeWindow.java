////////////////////////////////////////////////////////////////////////
/*
     FILE: TimeWindow.java
  PURPOSE: Represents a time interval.
   AUTHOR: Peter Hollemans
     DATE: 2016/07/17
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.util.Date;

// Testing
// -------
import noaa.coastwatch.test.TestLogger;

/**
 * A <code>TimeWindow</code> represents a date and window centered around 
 * the date.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
@noaa.coastwatch.test.Testable
public class TimeWindow {

  // Variables
  // ---------
  
  /** The central date for the time window. */
  private Date centralDate;
  
  /** The window size in milliseconds. */
  private long windowSize;
  
  /** The pre-computed time bounds for this window. */
  private Date[] timeBounds;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates a new time window.
   *
   * @param centralDate the central date for the time window.
   * @param windowSize the time window size in milliseconds.  Dates more than
   * this duration before or after the central date are considered to be outside
   * the window.
   */
  public TimeWindow (
    Date centralDate,
    long windowSize
  ) {

    this.centralDate = centralDate;
    this.windowSize = windowSize;
    this.timeBounds = new Date[] {
      new Date (centralDate.getTime() - windowSize),
      new Date (centralDate.getTime() + windowSize)
    };

  } // TimeWindow

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the central date for the time window.
   *
   * @return the central date.
   */
  public Date getCentralDate () { return (centralDate); }
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the window size.
   *
   * @return the window size in milliseconds.
   */
  public long getWindowSize () { return (windowSize); }
  
  ////////////////////////////////////////////////////////////

  /**
   * Determines if a date is within the time window.
   *
   * @param date the date to check.
   *
   * @return true if the specified date is within the time window.
   */
  public boolean isInWindow (
    Date date
  ) {

    boolean isInWindow = !date.before (timeBounds[0]) && !date.after (timeBounds[1]);
    return (isInWindow);

  } // isInWindow

  ////////////////////////////////////////////////////////////

  @Override
  public boolean equals (Object obj) {

    boolean isEqual = false;
    if (obj instanceof TimeWindow) {
      TimeWindow window = (TimeWindow) obj;
      isEqual = (this.centralDate.equals (window.centralDate) && this.windowSize == window.windowSize);
    } // if
    
    return (isEqual);

  } // equals

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (TimeWindow.class);

    logger.test ("constructor");
    Date now = new Date();
    long oneDay = 24*60*60*1000L;
    long thirtyDays = oneDay * 30;
    TimeWindow window = new TimeWindow (now, thirtyDays);
    assert (window.getWindowSize() == thirtyDays);
    assert (window.getCentralDate().equals (now));
    logger.passed();

    logger.test ("isInWindow");
    assert (window.isInWindow (new Date (now.getTime() + oneDay)));
    assert (window.isInWindow (new Date (now.getTime() - oneDay)));
    assert (window.isInWindow (new Date (now.getTime() + thirtyDays)));
    assert (window.isInWindow (new Date (now.getTime() - thirtyDays)));
    assert (!window.isInWindow (new Date (now.getTime() + thirtyDays + 1)));
    assert (!window.isInWindow (new Date (now.getTime() - thirtyDays - 1)));
    assert (!window.isInWindow (new Date (now.getTime() + thirtyDays + oneDay)));
    assert (!window.isInWindow (new Date (now.getTime() - thirtyDays - oneDay)));
    logger.passed();
    
    logger.test ("isEqual");
    TimeWindow windowSame = new TimeWindow (now, thirtyDays);
    TimeWindow windowDifferent = new TimeWindow (now, thirtyDays + oneDay);
    assert (window.equals (windowSame));
    assert (!window.equals (windowDifferent));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // TimeWindow class

////////////////////////////////////////////////////////////////////////
