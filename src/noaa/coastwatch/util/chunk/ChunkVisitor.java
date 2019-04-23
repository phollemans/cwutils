////////////////////////////////////////////////////////////////////////
/*

     File: ChunkVisitor.java
   Author: Peter Hollemans
     Date: 2017/11/20

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

/**
 * The <code>ChunkVisitor</code> interface is implemented by any class that
 * perticipates in the visitor pattern to perform operations on
 * {@link DataChunk} instances.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface ChunkVisitor {
  
  /**
   * Visits a chunk of native byte data.
   *
   * @param chunk the byte chunk to visit.
   */
  default public void visitByteChunk (ByteChunk chunk) { }

  /**
   * Visits a chunk of native short data.
   *
   * @param chunk the short chunk to visit.
   */
  default public void visitShortChunk (ShortChunk chunk) { }

  /**
   * Visits a chunk of native int data.
   *
   * @param chunk the int chunk to visit.
   */
  default public void visitIntChunk (IntChunk chunk) { }

  /**
   * Visits a chunk of native long data.
   *
   * @param chunk the long chunk to visit.
   */
  default public void visitLongChunk (LongChunk chunk) { }

  /**
   * Visits a chunk of native float data.
   *
   * @param chunk the float chunk to visit.
   */
  default public void visitFloatChunk (FloatChunk chunk) { }

  /**
   * Visits a chunk of native double data.
   *
   * @param chunk the double chunk to visit.
   */
  default public void visitDoubleChunk (DoubleChunk chunk) { }

} // ChunkVisitor interface

////////////////////////////////////////////////////////////////////////
