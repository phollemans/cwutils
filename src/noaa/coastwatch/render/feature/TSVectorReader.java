////////////////////////////////////////////////////////////////////////
/*

     File: TSVectorReader.java
   Author: Peter Hollemans
     Date: 2002/09/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/**
 * <p>The TeraScan vector reader class reads databases in the SeaSpace
 * TeraScan package vector file format.  The file format is as
 * follows. Each file contains a 180x360 1 degree square index,
 * followed by a heap filled with variable length vectors. The
 * 1 degree squares are numbered as follows:</p>
 * <pre>
 *   square = (lower-lat + 90) * 360 + left-lon + 180
 * </pre>
 * <p>where -90 &lt;= lat &lt; 90 and -180 &lt;= lon &lt; 180.  The index is a
 * 180x360 long integer array, indexed by square number. The index
 * entry for a given square is the offset to the first vector in the
 * heap for that square. The offset to the first vector in the heap
 * is 0. All vectors for a given square are contiguous in the
 * heap. The index entry for a square with no vectors is 0. (-1
 * would have been a better choice!)</p>
 *
 * <p>The format for a given vector in the heap is shown here:</p>
 * <pre>
 *   long integer square number
 *   long integer number of (lat,lon) pairs in vector
 *   short int lat offset #1 from lower lat in seconds
 *   short int lon offset #1 from left lon in seconds
 *   short int lat offset #2 from lower lat in seconds
 *   short int lon offset #2 from left lon in seconds
 * </pre>
 * <p>Because vector segments can cross the boundaries of
 * squares, lat and lon offsets can fall outside the range 0
 * to 3600 seconds.</p>
 *
 * @deprecated The functionality of this class has been replaced
 * by the {@link BinnedGSHHSLineReader} class which provides
 * better performance for reading and rendering.  The problem is
 * that the TeraScan vector files only have one resolution and so
 * render very slowly for large areas.  The actual vectors
 * rendered will differ between the data files provided by this
 * class and the GSHHS data files because the TeraScan data is
 * from the Digital Chart of the World (DCW) and the GSHHS data
 * is from the CIA World Data Bank II (WDBII).
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
@Deprecated
public class TSVectorReader
  extends LineFeatureSource {

  // Constants
  // ---------
  /** Offset into the vector heap. */
  private final static int HEAP_OFFSET = 259200;

  /** Coast line geography database. */
//  public final static String COAST = "dcw_coast.tsv";

  /** Inland water geography database. */
//  public final static String INLAND = "dcw_inland.tsv";

  /** International boundaries database. */
  public final static String POLITICAL = "dcw_political.tsv";

  /** National boundaries (states) database. */
  public final static String STATES = "dcw_states.tsv";

  // Variables	
  // ---------
  /** The database file for TeraScan vectors. */
  private File dataFile;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new TeraScan vector reader from a predefined 
   * database name.
   * 
   * @param name the predefined database name.
   *
   * @throws IOException if the predefined database is invalid.
   */
  public TSVectorReader (
    String name
  ) throws IOException {

    // Set file
    // --------
    URL url = getClass().getResource(name);
    if (url == null)
      throw new IOException ("Cannot find resource '" + name + "'");
    try { dataFile = new File (new URI (url.toString())); }
    catch (URISyntaxException e) { throw new IOException (e.getMessage()); }

  } // TSVectorReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new TeraScan vector reader from the database file.
   * 
   * @param file the database file.
   */
  public TSVectorReader (
    File file
  ) {

    // Set file
    // --------
    dataFile = file;

  } // TSVectorReader constructor

  ////////////////////////////////////////////////////////////

  protected void select () throws IOException {

    // Open database
    // -------------
    RandomAccessFile data = new RandomAccessFile (dataFile, "r");

    // Loop over each square
    // ---------------------
    Iterator iter = area.getIterator();
    featureList.clear();
    while (iter.hasNext()) {

      // Seek to square offset
      // ---------------------
      int[] square = (int[]) iter.next(); 
      int squareIndex = (square[0]+90)*360 + (square[1]+180);
      data.seek (squareIndex*4);
      int squareOffset = data.readInt();
      data.seek (HEAP_OFFSET+squareOffset);

      // Loop over each vector
      // ---------------------
      int thisSquareIndex = data.readInt();
      while (thisSquareIndex == squareIndex) {
        featureList.add (readPolyline (square, data));
        thisSquareIndex = data.readInt();
      } // while

    } // while

    // Close files
    // -----------
    data.close();

  } // select

  ////////////////////////////////////////////////////////////

  /**
   * Reads a polyline from the specified input.
   *
   * @param square the grid square coordinates as [lat, lon].
   * @param in the data input.  The polyline is constructed by reading
   * at the current input position.
   *
   * @return a new polyline as a vector of earth locations.
   * 
   * @throws IOException if an error occurred reading the data file.
   */
  public LineFeature readPolyline (
    int[] square,
    DataInput in
  ) throws IOException {

    // Create polyline and read values
    // ------------------------------
    LineFeature polyline = new LineFeature();
    int pairs = in.readInt();
    for (int i = 0; i < pairs; i++) {
      double lat = square[0] + (double)in.readShort() / 3600;
      double lon = square[1] + (double)in.readShort() / 3600;
      EarthLocation loc = new EarthLocation (lat, lon);
      polyline.add (loc);
    } // for
    return (polyline);

  } // readPolyline

  ////////////////////////////////////////////////////////////

} // TSVectorReader class

////////////////////////////////////////////////////////////////////////
