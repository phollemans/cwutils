////////////////////////////////////////////////////////////////////////
/*

     File: HDFGSHHSReader.java
   Author: Peter Hollemans
     Date: 2006/06/12

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.io.IOException;
import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.render.feature.BinnedGSHHSReader;

/**
 * The <code>HDFGSHHSReader</code> extends
 * <code>BinnedGSHHSReader</code> to read data from an HDF binned
 * data file.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class HDFGSHHSReader
  extends BinnedGSHHSReader {

  ////////////////////////////////////////////////////////////

 protected void readData (
    int sdsid,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException {

    try {
      int[] stride = new int[] {1};
      if (!HDFLib.getInstance().SDreaddata (sdsid, start, stride, count, data))
        throw new HDFException ("Cannot read data for sdsid " + sdsid);
    } // try
    catch (HDFException e) { throw new IOException (e.getMessage()); }

  } // readData

  ////////////////////////////////////////////////////////////

  protected void readData (
    String var,
    int[] start, 
    int[] count,
    Object data
  ) throws IOException {

    try {
      int sdsid = selectData (var);
      int[] stride = new int[] {1};
      if (!HDFLib.getInstance().SDreaddata (sdsid, start, stride, count, data))
        throw new HDFException ("Cannot read data for " + var);
      HDFLib.getInstance().SDendaccess (sdsid);
    } // try
    catch (HDFException e) { throw new IOException (e.getMessage()); }

  } // readData

  ////////////////////////////////////////////////////////////

  protected int selectData (
    String var
  ) throws IOException {

    try {
      int index = HDFLib.getInstance().SDnametoindex (sdID, var);
      if (index < 0)
        throw new HDFException ("Cannot access variable " + var);
      int sdsid = HDFLib.getInstance().SDselect (sdID, index);
      if (sdsid < 0)
        throw new HDFException ("Cannot access variable at index "+index);
      return (sdsid);
    } // try
    catch (HDFException e) { throw new IOException (e.getMessage()); }

  } // selectData

  ////////////////////////////////////////////////////////////

  protected int openFile (
    String name
  ) throws IOException {

    String path = IOServices.getFilePath (getClass(), name);
    try {
      int id = HDFLib.getInstance().SDstart (path, HDFConstants.DFACC_READ);
      if (sdID < 0) 
        throw new HDFException ("Invalid HDF file " + path);
      return (id);
    } // try
    catch (HDFException e) { throw new IOException (e.getMessage()); }

  } // openFile 

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binned GSHHS reader from the database file name.
   * By default, there is no minimum area for polygon selection and no
   * polygons are selected.
   * 
   * @param name the database name.  Several predefined database
   * names are available from {@link
   * BinnedGSHHSReaderFactory#getDatabaseName}.
   *
   * @throws IOException if an error occurred reading the file.
   */
  public HDFGSHHSReader (
    String name
  ) throws IOException {

    init (name);

  } // HDFGSHHSReader constructor

  ////////////////////////////////////////////////////////////

  protected void getGlobalData () throws IOException {

    // Get bin size and multiplier
    // ---------------------------
    int[] intData = new int[1];
    int[] start = new int[] {0};
    int[] count = new int[] {1};
    readData ("Bin_size_in_minutes", start, count, intData);
    binSize = intData[0] / 60.0;
    multiplier = binSize / 65535.0;

    // Get bin counts
    // --------------
    readData ("N_bins_in_360_longitude_range", start, count, intData);
    lonBins = intData[0];
    readData ("N_bins_in_180_degree_latitude_range", start, count, 
      intData);
    latBins = intData[0];
    readData ("N_bins_in_file", start, count, intData);
    totalBins = intData[0];

    // Get point count
    // ---------------
    readData ("N_points_in_file", start, count, intData);
    totalPoints = intData[0];

    // Get segment count
    // -----------------
    readData ("N_segments_in_file", start, count, intData);
    totalSegments = intData[0];

    // Get index of first segment in each bin
    // --------------------------------------
    firstSegment = new int[totalBins];
    count[0] = totalBins;
    readData ("Id_of_first_segment_in_a_bin", start, count, 
      firstSegment);
    
    // Get number of segments in each bin
    // ----------------------------------
    numSegments = new short[totalBins];
    readData ("N_segments_in_a_bin", start, count, numSegments);

    // Get bin information
    // -------------------
    binInfo = new short[totalBins];
    readData ("Embedded_node_levels_in_a_bin", start, count, binInfo);

    // Get starting point of each segment
    // ----------------------------------
    segmentStart = new int[totalSegments];
    count[0] = totalSegments;
    readData ("Id_of_first_point_in_a_segment", start, count, segmentStart);

  } // getGlobalData

  ////////////////////////////////////////////////////////////

  @Override
  protected void finalize () throws Throwable {

    try {
      HDFLib.getInstance().SDendaccess (segmentInfoID);
      HDFLib.getInstance().SDendaccess (segmentAreaID);
      HDFLib.getInstance().SDendaccess (dxID);
      HDFLib.getInstance().SDendaccess (dyID);
      HDFLib.getInstance().SDend (sdID);
    } // try
    finally {
      super.finalize();
    } // finally

  } // finalize

  ////////////////////////////////////////////////////////////

} // HDFGSHHSReader class

////////////////////////////////////////////////////////////////////////
