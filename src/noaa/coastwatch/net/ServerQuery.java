////////////////////////////////////////////////////////////////////////
/*

     File: ServerQuery.java
   Author: Peter Hollemans
     Date: 2002/01/18

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
package noaa.coastwatch.net;

// Imports
// -------
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * A server query interfaces to a CoastWatch data server and handles
 * the query URL connection and response text.  The response is read
 * as a series of results, each with the same number of values.
 * Values may be retrieved by specifying the desired result index and
 * value key or index.<p>
 *
 * The specifics of the query are handled using a set of key/value
 * pairs specified by a map.  Each key and value in the map is expected
 * to be a string.  The allowed key/values pairs are as follows:
 * <ul>
 *   <li> query = <code>datasetDetails</code> | <code>serverStatus</code> </li>
 *   <li> details = comma-separated list of desired dataset attributes </li>
 *   <li> projection_type = <code>swath</code> | <code>mapped</code> </li>
 *   <li> satellite = regular expression match string </li>
 *   <li> sensor = regular expression match string </li>
 *   <li> scene_time = regular expression match string </li>
 *   <li> file_name = regular expression match string </li>
 *   <li> region_id = regular expression match string </li>
 *   <li> station_id = regular expression match string </li>
 *   <li> before = <code>yyyy-mm-dd hh:mm:ss</code> time stamp </li>
 *   <li> after = <code>yyyy-mm-dd hh:mm:ss</code> time stamp </li>
 *   <li> order = comma-separated list of desired ordering attributes </li>
 *   <li> coverage = minimum coverage as a percentage </li>
 * </ul>
 * where valid dataset attributes include the following:
 * <ul>
 *   <li> <code>satellite</code>
 *   <li> <code>sensor</code>
 *   <li> <code>date</code>
 *   <li> <code>time</code>
 *   <li> <code>scene_time</code>
 *   <li> <code>file_name</code>
 *   <li> <code>format</code>
 *   <li> <code>data_url</code>
 *   <li> <code>preview_url</code>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public class ServerQuery {

  // Variables
  // ---------
  /** The query URL. */
  private URL url;
  
  /** The mapping from value key name to index. */
  private HashMap valueKeyMap;  

  /** The array of value key names. */
  private String[] keyNames;

  /** The table of result strings. */
  private String[][] resultTable;

  ////////////////////////////////////////////////////////////

  /** Gets the number of values in each result. */
  public int getValues () {

    return (keyNames.length);

  } // getValues

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the value key at the specified value index.
   *
   * @param value the value index.
   * 
   * @return the value key.
   */
  public String getValueKey (
    int value
  ) {  
 
    return (keyNames[value]);

  } // getValueKey

  ////////////////////////////////////////////////////////////

  /** Gets the number of query results. */
  public int getResults () {

    return (resultTable.length);

  } // getResults

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a result value.
   * 
   * @param result the result index.
   * @param value the value index.
   *
   * @return the value string.
   */
  public String getValue (
    int result,
    int value
  ) {

    return (resultTable[result][value]);

  } // getValue

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the value index using the specified value key.
   *
   * @param valueKey the value key to find.
   * 
   * @return the value index, or -1 if the value key is not found.
   */
  public int getValueIndex (
    String valueKey
  ) {  
 
    Integer valueIndex = (Integer) valueKeyMap.get (valueKey);
    if (valueIndex == null) return (-1);
    else return (valueIndex.intValue());

  } // getValueIndex

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a result value.
   * 
   * @param result the result index.
   * @param valueKey the value key.
   *
   * @return the value string.
   */
  public String getValue (
    int result,
    String valueKey
  ) {

    return (resultTable[result][getValueIndex (valueKey)]);

  } // getValue

  ////////////////////////////////////////////////////////////

  /** Gets the host name used for the query. */
  public String getHost() { return (url.getHost()); }

  ////////////////////////////////////////////////////////////

  /** Gets the query URL as a string. */
  public String getURL() { return (url.toString()); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new server query using the server protocol, host, query path, and
   * query key/value set.
   *
   * @param host the server host name to use for the query.  The
   * host name must be a valid Internet domain name.
   * @param path the absolute path to the query program.
   * @param query a map containing the query key/value pairs.
   *
   * @throws IOException if the host, path, or query were not valid or
   * the server responded with an error.
   */
  public ServerQuery (
    String host,
    String path,
    Map query
  ) throws IOException {

    this ("http", host, path, query);

  } // ServerQuery constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new server query using the server host, query path, and
   * query key/value set.
   *
   * @param protocol the connection protocol, either 'http' or 'https'.
   * @param host the server host name to use for the query.  The
   * host name must be a valid Internet domain name.
   * @param path the absolute path to the query program.
   * @param query a map containing the query key/value pairs.
   *
   * @throws IOException if the host, path, or query were not valid or
   * the server responded with an error.
   *
   * @since 3.4.1
   */
  public ServerQuery (
    String protocol,
    String host,
    String path,
    Map query
  ) throws IOException {
                            
    // Create query string
    // ------------------- 
    Iterator iter = query.entrySet().iterator();
    String queryString = "";
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      String encoding = URLEncoder.encode ((String) entry.getValue(), "UTF-8");
      queryString += entry.getKey() + "=" + encoding;
      if (iter.hasNext()) queryString += "&";
    } // while

    // Create URL connection
    // ---------------------
    InputStream in;
    try {  
      url = new URL (protocol + "://" + host + path + "?" + queryString);
      in = url.openStream();
    } catch (Exception e) { 
      throw new IOException ("Error in URL connection");
    } // catch

    // Read response header 
    // --------------------
    BufferedReader buf = new BufferedReader (new InputStreamReader (in));
    String line;
    try { line = buf.readLine(); }
    catch (Exception e) { 
      throw new IOException ("Error reading response header"); 
    } // catch
    if (line.startsWith ("+ERR"))
      throw new IOException ("Error in query");
    else if (!line.startsWith ("+OK"))
      throw new IOException ("Malformed response header");

    // Read value key names
    // --------------------
    try { line = buf.readLine(); }
    catch (Exception e) { 
      throw new IOException ("Error reading value key names");
    } // catch
    keyNames = line.split ("\\|");
    valueKeyMap = new HashMap();
    for (int i = 0; i < keyNames.length; i++) 
      valueKeyMap.put (keyNames[i], Integer.valueOf (i));

    // Read result lines
    // -----------------
    Vector results = new Vector();
    try {    
      while ((line = buf.readLine ()) != null) {
        String[] values = line.split ("\\|", -1);
        if (values.length != keyNames.length)
          throw new IOException();
        results.add (values);
      } // while
      buf.close();
    } catch (Exception e) { 
      throw new IOException ("Error reading response body");
    } // catch

    // Create result table
    // -------------------
    resultTable = (String[][]) results.toArray (new String[][] {});

  } // ServerQuery constructor

  ////////////////////////////////////////////////////////////

} // ServerQuery class

////////////////////////////////////////////////////////////////////////
