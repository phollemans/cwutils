////////////////////////////////////////////////////////////////////////
/*
     FILE: GridSubsetReader.java
  PURPOSE: Interface for reading grid subsets.
   AUTHOR: Peter Hollemans
     DATE: 2005/07/31

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>GridSubsetReader</code> is an interface that any {@link
 * EarthDataReader} can implement to indicate that it is capable of
 * reading grid data in a subset/subsampled form.  It contains only
 * one method: {@link #getGridSubset}.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface GridSubsetReader {

  /** 
   * Reads a subset of a data grid.  This is similar to the {@link
   * EarthDataReader#getVariable(String)} method except that it reads
   * only grid variables and is capable of returning just a subset of
   * the data values.  In some cases, such as across a network
   * connection, it may be more efficient to access only a subset or
   * subsampling of data in a variable.
   *
   * @param varName the variable name to access.
   * @param start the 2D starting data coordinates.
   * @param stride the 2D data stride.
   * @param length the total number of values to read in each dimension.
   *
   * @return the subset and/or subsampled grid variable.
   *
   * @throws IOException if the data source had I/O errors.
   */
  public Grid getGridSubset (
    String varName,
    int[] start,
    int[] stride,
    int[] length
  ) throws IOException;

} // GridSubsetReader

////////////////////////////////////////////////////////////////////////
