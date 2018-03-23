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
  default public void visitByteChunk (ByteChunk chunk) { }
  default public void visitShortChunk (ShortChunk chunk) { }
  default public void visitIntChunk (IntChunk chunk) { }
  default public void visitLongChunk (LongChunk chunk) { }
  default public void visitFloatChunk (FloatChunk chunk) { }
  default public void visitDoubleChunk (DoubleChunk chunk) { }
} // ChunkVisitor interface

////////////////////////////////////////////////////////////////////////
