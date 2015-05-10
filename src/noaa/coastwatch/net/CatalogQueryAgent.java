////////////////////////////////////////////////////////////////////////
/*
     FILE: CatalogQueryAgent.java
  PURPOSE: Abstract class for performing earth data catalog queries.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/09
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.net;

// Imports
// -------
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * A <code>CatalogQueryAgent</code> is an abstract class for
 * performing queries of earth data catalogs.  The catalog may be
 * queried using temporal and spatial criteria.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class CatalogQueryAgent {

  // Constants
  // ---------
  
  /** The number of milliseconds per day. */
  protected static final long MSEC_PER_DAY = 86400000L;

  // Variables
  // ---------

  /** The catalog query URL. */
  protected URL url;
  
  /** The search by time flag. */
  protected boolean searchByTime = true;

  /** The starting date. */
  protected Date startDate;

  /** The ending date. */
  protected Date endDate;

  /** The search by coverage flag. */
  protected boolean searchByCoverage = true;

  /** The coverage region. */
  protected String region;

  /** The coverage as a percentage. */
  protected int coverage;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new catalog agent that uses the specified URL for
   * performing queries.  By default, time and coverage searching
   * are enabled unless disabled using {@link #setSearchByTime}
   * or {@link #setSearchByCoverage}.
   *
   * @param url the query url.
   */
  protected CatalogQueryAgent (
    URL url
  ) {

    this.url = url;

  } // CatalogQueryAgent constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the time search flag.
   * 
   * @param searchByTime the time flag, true to search by time, or
   * false otherwise.
   *
   * @see #setTimeByDate
   * @see #setTimeByAge
   */
  public void setSearchByTime (
    boolean searchByTime
  ) {

    this.searchByTime = searchByTime;

  } // setSearchByTime

  ////////////////////////////////////////////////////////////

  /**
   * Sets the coverage search flag.
   * 
   * @param searchByCoverage the coverage flag, true to search by
   * coverage, or false otherwise.
   *
   * @see #setCoverageByRegion
   */
  public void setSearchByCoverage (
    boolean searchByCoverage
  ) {

    this.searchByCoverage = searchByCoverage;

  } // setSearchByCoverage

  ////////////////////////////////////////////////////////////

  /**
   * Sets the query time constraints by start and end date.  The
   * query will return data whose starting date falls in the
   * specified range.
   *
   * @param startDate the starting data date.
   * @param endDate the ending data date.
   */
  public void setTimeByDate (
    Date startDate,
    Date endDate
  ) {

    this.startDate = startDate;
    this.endDate = endDate;

  } // setTimeByDate

  ////////////////////////////////////////////////////////////

  /**
   * Sets the query time constraints by data age.  The query will
   * return data whose starting date falls during the past
   * specified number of days.
   *
   * @param days the data age in days.
   */
  public void setTimeByAge (
    double days
  ) {

    endDate = new Date();
    startDate = new Date (endDate.getTime() - (long)(days*MSEC_PER_DAY));

  } // setTimeByAge

  ////////////////////////////////////////////////////////////

  /**
   * Sets the spatial coverage constraints using a predefined
   * region code.  The query will return data with at least the
   * specified coverage in the region.
   *
   * @param region the coverage region code.
   * @param coverage the coverage as a percent.
   */
  public void setCoverageByRegion (
    String region,
    int coverage
  ) {

    this.region = region;
    this.coverage = coverage;

  } // setCoverageByRegion

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>Entry</code> class holds information about one
   * catalog entry.  The field values should be assigned by the
   * subclass as completely as possible.  If some field does not
   * apply to the specific subclass, the field may be assigned a
   * null value.  Fields marked with (**) are highly recommended
   * to have real values.
   */
  public static class Entry implements Comparable {

    /** The data starting date. (**) */
    public Date startDate;

    /** The data ending date. (**) */
    public Date endDate;

    /** The data source, such as satellite/sensor or model. (**) */
    public String dataSource;

    /** The region coverage value. (**) */
    public int coverage;

    /** The scene time as day or night. */
    public String sceneTime;

    /** The data download URL for the full file data. (**) */
    public String downloadUrl;

    /** The data access URL, such as for a data acceess protocol. */
    public String dataUrl;

    /** The data preview URL, such as a simple PNG or JPEG image. (**) */
    public String previewUrl;

    ////////////////////////////////////////////////////////

    /** Converts this entry to a string. */
    public String toString() {
      return (
        "Entry[" +
        "startDate=" + startDate + "," +
        "endDate=" + endDate + "," +
        "dataSource=" + dataSource + "," +
        "coverage=" + coverage + "," +
        "sceneTime=" + sceneTime + "," + 
        "downloadUrl=" + downloadUrl + "," +
        "dataUrl=" + dataUrl + "," +
        "previewUrl=" + previewUrl + "]"
      );
    } // toString

    ////////////////////////////////////////////////////////

    /** Compares this entry to another by start date. */
    public int compareTo (Object o) {

      return (startDate.compareTo (((Entry)o).startDate));

    } // compareTo

    ////////////////////////////////////////////////////////

  } // Entry class

  ////////////////////////////////////////////////////////////

  /** 
   * Gets all entries in the catalog by turning off time and
   * coverage searching, and then calling {@link #getEntries}.
   * This is simply a convenience method for getting a complete
   * catalog dump.
   *
   * @return all the entries in the catalog as a list.
   *
   * @throws IOException if an error occurred performing the
   * query.
   */
  public List getAllEntries () throws IOException {

    setSearchByTime (false);
    setSearchByCoverage (false);
    return (getEntries());

  } // getAllEntries

  ////////////////////////////////////////////////////////////

  /**
   * Gets the catalog data entries whose temporal and spatial
   * properties match those currently set in the query agent.
   *
   * @return the list of catalog <code>Entry</code> objects.
   *
   * @throws IOException if an error occurred performing the
   * query.
   */
  public abstract List getEntries ()
    throws IOException;

  ////////////////////////////////////////////////////////////

} // CatalogQueryAgent class

////////////////////////////////////////////////////////////////////////
