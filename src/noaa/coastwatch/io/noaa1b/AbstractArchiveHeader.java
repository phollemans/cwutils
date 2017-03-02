////////////////////////////////////////////////////////////////////////
/*

     File: AbstractArchiveHeader.java
   Author: Peter Hollemans
     Date: 2007/08/27

  CoastWatch Software Library and Utilities
  Copyright (c) 2007 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.io.noaa1b;

// Imports
// -------
import java.io.IOException;
import java.nio.ByteBuffer;
import noaa.coastwatch.io.BinaryStreamReader;
import noaa.coastwatch.io.BinaryStreamReaderFactory;
import noaa.coastwatch.io.noaa1b.ArchiveHeader;

/**
 * The <code>AbstractArchiveHeader</code> class reads header data
 * from NOAA 1b data files.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public abstract class AbstractArchiveHeader implements ArchiveHeader {

  // Variables
  // ---------

  /** The binary reader for this class. */
  protected BinaryStreamReader reader;

  /** The input buffer to use for data. */
  protected ByteBuffer inputBuffer;

  ////////////////////////////////////////////////////////////

  /** Determines if the byte buffer data is compatible with this header. */
  protected abstract boolean isCompatible (ByteBuffer inputBuffer);

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new header using the specified byte data.
   *
   * @param inputBuffer the buffer to read for byte data.
   *
   * @throws IOException if an error occurred checking the data.
   */
  public AbstractArchiveHeader (
    ByteBuffer inputBuffer
  ) throws IOException {

    if (!isCompatible (inputBuffer)) 
      throw new IOException ("Incompatible header data");
    this.inputBuffer = inputBuffer;
    reader = BinaryStreamReaderFactory.getReader (getClass());

  } // AbstractArchiveHeader constructor

  ////////////////////////////////////////////////////////////

  public boolean[] getChannelSelection() {

    String channelString = reader.getString ("channelSelect", inputBuffer);
    int flags = channelString.length();
    boolean[] selectArray = new boolean[flags];
    for (int i = 0; i < flags; i++) 
      selectArray[i] = (channelString.charAt (i) == 'Y');

    return (selectArray);

  } // getChannelSelection

  ////////////////////////////////////////////////////////////

  public int getSensorWordSize() {

    return (Integer.parseInt (reader.getString ("sensorDataWordSize", 
      inputBuffer)));

  } // getSensorWordSize

  ////////////////////////////////////////////////////////////

} // AbstractArchiveHeader class

////////////////////////////////////////////////////////////////////////
