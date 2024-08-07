////////////////////////////////////////////////////////////////////////
/*

     File: OpendapReaderList.java
   Author: Peter Hollemans
     Date: 2006/05/23

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
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.util.Date;
import java.util.List;
import noaa.coastwatch.io.AbstractReaderList;
import noaa.coastwatch.io.CWOpendapReader;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.OpendapGrid;
import noaa.coastwatch.io.OpendapReader;
import noaa.coastwatch.net.CatalogQueryAgent;
import noaa.coastwatch.net.CatalogQueryAgent.Entry;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * An <code>OpendapReaderList</code> holds a list of {@link
 * noaa.coastwatch.io.OpendapReader} objects created through the use
 * of the data URL entries in the results of a {@link
 * noaa.coastwatch.net.CatalogQueryAgent}.  The readers are special in
 * that they provide efficient access to the earth transform and start
 * dates of the OPeNDAP datasets without the I/O overhead of having to
 * open and read the DAS and DDS for each dataset.  Variables returned
 * from the OPeNDAP readers are efficient for interactive data access
 * as they listen through the {@link
 * noaa.coastwatch.util.DataVariable#setAccessHint} method so that only the
 * data that is required is downloaded from the OPeNDAP server.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class OpendapReaderList
  extends AbstractReaderList {

  // Variables
  // ---------
  
  /** The current list of catalog entries. */
  private List entryList;

  /** The catalog agent used for retrieving entry data. */
  private CatalogQueryAgent agent;

  /** The prototype reader for use in various reader operations. */
  private OpendapReader protoReader;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new list from entries returned by the query agent.
   *
   * @param agent the query agent to use for entries.
   *
   * @throws IOException if an error occurred performing the entries
   * query.
   */
  public OpendapReaderList (
    CatalogQueryAgent agent
  ) throws IOException {

    // Get list of entries
    // -------------------
    this.agent = agent;
    entryList = agent.getAllEntries();

    // Create prototype reader
    // -----------------------
    if (entryList.size() == 0)
      throw new IOException ("Got zero entries from catalog");
    protoReader = new CWOpendapReader (getEntry (0).dataUrl);

  } // OpendapReaderList constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the catalog entry for the specified index.
   *
   * @param index the index of the entry to retrieve.
   *
   * @return the catalog entry at the specified index.
   */
  public Entry getEntry (
    int index
  ) {

    return ((Entry) entryList.get (index));

  } // getEntry

  ////////////////////////////////////////////////////////////

  public EarthTransform getTransform () {

    return (protoReader.getInfo().getTransform());

  } // getTransform

  ////////////////////////////////////////////////////////////

  public int size () { return (entryList.size()); }

  ////////////////////////////////////////////////////////////

  public Date getStartDate (
    int index
  ) { 

    return (getEntry (index).startDate);

  } // getStartDate

  ////////////////////////////////////////////////////////////

  public DataVariable getVariable (
    int index,
    String varName
  ) throws IOException {

    // Create and return special grid
    // ------------------------------
    Grid protoGrid = (Grid) protoReader.getPreview (varName);
    String dataUrl = getEntry (index).dataUrl;
    return (new OpendapGrid (protoGrid, dataUrl));

  } // getVariable

  ////////////////////////////////////////////////////////////

  public EarthDataReader getReader (
    int index
  ) throws IOException {

    return (new CWOpendapReader (getEntry (index).dataUrl));

  } // getReader

  ////////////////////////////////////////////////////////////

} // OpendapReaderList class

////////////////////////////////////////////////////////////////////////


