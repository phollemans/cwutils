////////////////////////////////////////////////////////////////////////
/*
     FILE: VIIRSBowtieFilter.java
  PURPOSE: Filters locations that have been deleted from VIIRS files.
   AUTHOR: Peter Hollemans
     DATE: 2016/06/07
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Includes
// --------
import noaa.coastwatch.util.LocationFilter;

/**
 * The <code>VIIRSBowtieFilter</code> class detects locations
 * in a level 2 swath file from the VIIRS sensor in which certain pixels 
 * are deleted because of a bow-tie overlap effect of the successive scan 
 * head sweeps.  The filter returns true for those pixels that are actual 
 * data, and false for deleted pixels.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class VIIRSBowtieFilter implements LocationFilter {

  // Variables
  // ---------
  
  /** The single instance of this class. */
  private static VIIRSBowtieFilter instance;
  
  /** 
   * The VIIRS bow-tie deletion pattern array, true for locations
   * removed by bow-tie deletion.
   */
  private boolean[][] isBowtieDeleted = null;
  
  ////////////////////////////////////////////////////////////

  /**
   * Creates an instance of this class and sets up the bow-tie
   * deletion array.
   *
   * @return the instance of this class.
   */
  private VIIRSBowtieFilter () {
  
    // Set up bow-tie deletion pattern
    // -------------------------------
    isBowtieDeleted = new boolean[16][3200];
    for (int i = 0; i < 16; i++) {
      for (int j = 0; j < 3200; j++) {
        isBowtieDeleted[i][j] = false;
        if ((i == 0 || i == 15) && (j < 1008 || j >= 2192))
          isBowtieDeleted[i][j] = true;
        else if ((i == 1 || i == 14) && (j < 640 || j >= 2560))
          isBowtieDeleted[i][j] = true;
      } // for
    } // for
  
  } // VIIRSBowtieFilter

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static VIIRSBowtieFilter getInstance() {
  
    if (instance == null) {
      instance = new VIIRSBowtieFilter();
    } // if
    return (instance);
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  @Override
  public boolean useLocation (DataLocation loc) {
  
    int sourceScanLine = (int) loc.get (Grid.ROWS) % 16;
    int sourceCol = (int) loc.get (Grid.COLS);
    return (!isBowtieDeleted[sourceScanLine][sourceCol]);
  
  } // useLocation

  ////////////////////////////////////////////////////////////

} // VIIRSBowtieFilter class

////////////////////////////////////////////////////////////////////////
