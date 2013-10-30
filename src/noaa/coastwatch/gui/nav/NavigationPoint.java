////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationPoint.java
  PURPOSE: Hold navigation offset data.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/12
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.nav;

// Imports
// -------
import noaa.coastwatch.util.*;

/**
 * The <code>NavigationPoint</code> class holds data for a single
 * navigation analysis point.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NavigationPoint {

  // Variables
  // ---------

  /** The earth location of the point. */
  private EarthLocation earthLoc;

  /** The data location of the point. */
  private DataLocation dataLoc;

  /** The offset of the navigation point from the latest analysis. */
  private double[] offset;

  /** The point annotation comment. */
  private String comment;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new navigation point.  The offset is initialized
   * to 0, and the comment to the empty string.
   *
   * @param earthLoc the earth location of the point.
   * @param dataLoc the data location of the point (or null if
   * there is no associated data location.
   */
  public NavigationPoint (
    EarthLocation earthLoc,
    DataLocation dataLoc
  ) {
    
    this.earthLoc = earthLoc;
    this.dataLoc = dataLoc;
    this.offset = new double[] {0, 0};
    this.comment = "";

  } // NavigationPoint constructor

  ////////////////////////////////////////////////////////////

  /** Gets the earth location. */
  public EarthLocation getEarthLoc () { return (earthLoc); }

  ////////////////////////////////////////////////////////////

  /** Gets the data location. */
  public DataLocation getDataLoc () { return (dataLoc); }

  ////////////////////////////////////////////////////////////

  /** Gets the offset. */
  public double[] getOffset () { return (offset); }

  ////////////////////////////////////////////////////////////

  /** Gets the comment. */
  public String getComment () { return (comment); }

  ////////////////////////////////////////////////////////////

  /** Sets the comment. */
  public void setComment (String comment) { this.comment = comment; }

  ////////////////////////////////////////////////////////////

  /** Sets the offset. */
  public void setOffset (double[] offset) { this.offset = offset; }

  ////////////////////////////////////////////////////////////

} // NavigationPoint class

////////////////////////////////////////////////////////////////////////
