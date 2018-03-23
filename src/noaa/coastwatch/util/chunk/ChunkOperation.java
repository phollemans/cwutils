////////////////////////////////////////////////////////////////////////
/*

     File: ChunkOperation.java
   Author: Peter Hollemans
     Date: 2017/11/01

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.chunk;

// Imports
// --------
import noaa.coastwatch.util.chunk.ChunkPosition;

/**
 * The <code>ChunkOperation</code> interface is implemented by any class that
 * performs some unit of work on the chunk or chunks at a given position.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ChunkOperation {

  /**
   * Performs an operation on the chunks at the specified position.
   *
   * @param pos the chunk position to act on.
   */
  public void perform (ChunkPosition pos);

} // ChunkOperation interface

////////////////////////////////////////////////////////////////////////
