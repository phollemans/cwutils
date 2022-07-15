////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataSurvey.java
   Author: Peter Hollemans
     Date: 2004/03/26

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
import java.text.NumberFormat;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ReportFormatter;

/**
 * The <code>EarthDataSurvey</code> class is used to perform surveys
 * on <code>DataVariable</code> objects, and is the abstract parent of
 * all surveys.  It holds information on the survey variable,
 * statistics, extents, and so on.  Child classes should implement an
 * appropriate constructor and a <code>getResults()</code> method for
 * the results formatting.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class EarthDataSurvey {

  // Variables
  // ---------
  
  /** The survey variable name. */
  private String name;

  /** The survey variable units. */
  private String units;

  /** The survey variable value formatter. */
  private NumberFormat format;

  /** The earth transform object for the variable. */
  private EarthTransform trans;

  /** The survey statistics. */
  private Statistics stats;

  /** The survey extents as [start, end]. */
  private DataLocation[] extents;

  ////////////////////////////////////////////////////////////

  /** Gets the survey variable name. */
  public String getVariableName () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Gets the survey variable units. */
  public String getVariableUnits () { return (units); }

  ////////////////////////////////////////////////////////////

  /** Gets the survey variable number formatter. */
  public NumberFormat getVariableFormat () { return (format); }

  ////////////////////////////////////////////////////////////

  /** Gets the survey variable earth transform. */
  public EarthTransform getTransform () { return (trans); }

  ////////////////////////////////////////////////////////////

  /** Gets the survey statistics. */
  public Statistics getStatistics () { return (stats); }

  ////////////////////////////////////////////////////////////

  /** Gets the survey extents as [start, end]. */
  public DataLocation[] getExtents () { 

    return ((DataLocation[]) extents.clone()); 

  } // getExtents

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a results report for the survey.
   * 
   * @param formatter the formatter to use for the report.
   * 
   * @since 3.7.1
   */
  public void getResults (ReportFormatter formatter) { }

  ////////////////////////////////////////////////////////////

  /** Creates a new empty survey. */
  protected EarthDataSurvey () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Initializes the survey with the specified information.
   * 
   * @param name the survey variable name.
   * @param units the survey variable units.
   * @param format the survey variable formatter.
   * @param trans the survey variable earth transform.
   * @param stats the survey statistics.
   * @param extents the survey extents as [start,end].
   */
  protected void init (
    String name,
    String units,
    NumberFormat format,
    EarthTransform trans,
    Statistics stats,
    DataLocation[] extents
  ) { 

    this.name = name;
    this.units = units;
    this.format = format;
    this.trans = trans;
    this.stats = stats;
    this.extents = extents;

  } // init

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new data survey.
   * 
   * @param name the survey variable name.
   * @param units the survey variable units.
   * @param format the survey variable formatter.
   * @param trans the survey variable earth transform.
   * @param stats the survey statistics.
   * @param extents the survey extents as [start,end].
   */
  protected EarthDataSurvey (
    String name,
    String units,
    NumberFormat format,
    EarthTransform trans,
    Statistics stats,
    DataLocation[] extents
  ) { 

    init (name, units, format, trans, stats, extents);

  } // EarthDataSurvey constructor

  ////////////////////////////////////////////////////////////

} // EarthDataSurvey class

////////////////////////////////////////////////////////////////////////
