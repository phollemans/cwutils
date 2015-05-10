////////////////////////////////////////////////////////////////////////
/*
     FILE: ReaderList.java
  PURPOSE: Holds a set of related data readers.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/22
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.util.Date;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>ReaderList</code> interface is designed to group together
 * a set of related {@link noaa.coastwatch.io.EarthDataReader}
 * instances with the same earth transform and allow them to be
 * accessible with less I/O overhead than if they were each opened
 * separately.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public interface ReaderList {

  ////////////////////////////////////////////////////////////

  /** Gets the earth transform for this list. */
  public EarthTransform getTransform();

  ////////////////////////////////////////////////////////////

  /** Gets the number of readers in this list. */
  public int size();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the starting date for the specified reader.
   *
   * @param index the reader index to query.
   * 
   * @return the starting date for the specified reader.
   */
  public Date getStartDate (int index);

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a variable from the specified reader.
   * 
   * @param index the reader index to query.
   * @param varName the variable name to retrieve.
   *
   * @return the data variable from the reader.
   *
   * @throws IOException if an error occurred accessing the variable.
   */
  public DataVariable getVariable (int index, String varName) 
    throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the specified earth data reader.  This method is probably
   * I/O intensive and should only be used when necessary.
   *
   * @param index the reader index to return.
   *
   * @return the reader at the specified index.
   *
   * @throws IOException if an error occurred accessing the reader.
   */
  public EarthDataReader getReader (int index) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Get the index of the reader closest in start date to the
   * specified date.
   *
   * @param date the date to get the closest reader for.
   *
   * @return the reader index or -1 if none are found.
   */
  public int getClosestIndex (Date date);

  ////////////////////////////////////////////////////////////

} // ReaderList interface

////////////////////////////////////////////////////////////////////////
