////////////////////////////////////////////////////////////////////////
/*
     FILE: CWFProjectionInfo.java
  PURPOSE: To hold information concerning CWF file projection
           attributes.
   AUTHOR: Mark Robinson
     DATE: 2002/03/19
  CHANGES: 2002/05/13, PFH, removed access methods, added javadoc,
             package name, and reformatted 

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

/**
 * The CWF projection information class is a data container for CWF
 * data projection attributes.  All member variables are publically
 * accessible.
 */
public class CWFProjectionInfo {

  // Variables
  // ---------
  /** File projection type.  See {@link CWF CWF} for a list of valid
  projection constants. */
  public int projection_type;

  /** Data pixel resolution in kilometers. */
  public float resolution;

  /** Primary longitude for polar steregraphic projections. */
  public float prime_longitude;

  /** Hemisphere code.  See {@link CWF CWF} for a list of valid
  hemisphere constants. */
  public short hemisphere;

  /** Grid offset in the columns direction. */
  public short iOffset;

  /** Grid offset in the rows direction. */
  public short jOffset;

  ////////////////////////////////////////////////////////////

  /**
   * Initializes a projection object so that it contains zero values
   * for all member variables.
   */
  public CWFProjectionInfo () { }

  ////////////////////////////////////////////////////////////

} // CWFProjectionInfo class

////////////////////////////////////////////////////////////////////////
