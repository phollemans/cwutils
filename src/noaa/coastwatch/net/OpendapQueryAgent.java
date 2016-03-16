////////////////////////////////////////////////////////////////////////
/*
     FILE: OpendapQueryAgent.java
  PURPOSE: Abstract class for performing earth data catalog queries.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/09
  CHANGES: 2008/02/18, PFH, modified to use opendap.dap classes
           2016/03/16, PFH
           - Changes: Updated to use new opendap.dap.DConnect2 class and call
             DArray.getClearName().
           - Issue: The Java NetCDF library uses the newer OPeNDAP Java
             classes and they were conflicting with the older API that we were
             using, so we had to remove the old dap2 jar and conform to the 
             API found in the classes in the latest toolsUI jar file.

  CoastWatch Software Library and Utilities
  Copyright 2006-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.net;

// Imports
// -------
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.net.CatalogQueryAgent;
import opendap.dap.DConnect2;
import opendap.dap.DFloat64;
import opendap.dap.DInt16;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DataDDS;
import opendap.dap.NoSuchVariableException;

/**
 * A <code>OpendapQueryAgent</code> uses OPeNDAP to query a
 * network server for catalog entries.  The query uses OPeNDAP
 * sequence data and constraint expressions to select the entries
 * of interest.  The OPeNDAP DDS is as follows:
 *
 * <pre>
 *   Dataset {
 *     Sequence {
 *       String date;
 *       String time;
 *       String sat;
 *       String sensor;
 *       Float64 epoch;
 *       String sceneTime;
 *       String dapUrl;
 *       String dataUrl;
 *       String previewUrl;
 *       Int16 cover_??;
 *     } catalogRecords;
 *   } catalog.dat;
 * </pre>
 *
 * Since only one region can be searched at once, only one
 * cover_?? key value will appear in the entry.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class OpendapQueryAgent 
  extends CatalogQueryAgent {

  // Constants
  // ---------

  /** The set of standard sequence data variables. */
  private static final String VARS = "epoch,sat,sensor,sceneTime,dapUrl," +
    "dataUrl,previewUrl";

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new catalog agent that uses the specified URL for
   * performing OPeNDAP queries.
   *
   * @param url the query url.
   */
  public OpendapQueryAgent (
    URL url
  ) {

    super (url);

  } // OpendapQueryAgent constructor

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
  public List getEntries () 
    throws IOException {

    // Create coverage constraint expression
    // -------------------------------------
    String coverVar, coverExpr;
    String vars = VARS;
    if (searchByCoverage) {
      coverVar = "cover_" + region;
      vars += "," + coverVar;
      coverExpr = "&" + coverVar + ">=" + coverage;
    } // if
    else {
      coverVar = "";
      coverExpr = "";
    } // else

    // Create epoch constraint expression
    // ----------------------------------
    String epochExpr;
    if (searchByTime) {
      double startEpoch = (double) startDate.getTime() / MSEC_PER_DAY;
      double endEpoch = (double) endDate.getTime() / MSEC_PER_DAY;
      epochExpr = "&epoch<=" + endEpoch + "&epoch>=" + startEpoch;
    } // if
    else
      epochExpr = "";

    // Get data
    // --------
    String expr = "?" + vars + coverExpr + epochExpr;
    DSequence sequence;
    try {
      DConnect2 connect = new DConnect2 (url.toString(), true);
      DataDDS dds = connect.getData (expr, null);
      sequence = (DSequence) dds.getVariable ("catalogRecords");
    } // try
    catch (Exception e) {
      throw new IOException ("Error getting catalog entries: "+e.getMessage());
    } // catch

    // Create entry list
    // -----------------
    List entryList = new ArrayList();
    HashSet entryKeys = new HashSet();
    for (int i = 0; i < sequence.getRowCount(); i++) {
      try {

        // Get entry data
        // --------------
        String sat = ((DString) sequence.getVariable (i, "sat")).getValue();
        String sensor = 
          ((DString) sequence.getVariable (i, "sensor")).getValue();
        double epoch = 
          ((DFloat64) sequence.getVariable (i, "epoch")).getValue();
        Date startDate = new Date ((long) (epoch*MSEC_PER_DAY));
        Date endDate = startDate;
        String sceneTime = 
          ((DString) sequence.getVariable (i, "sceneTime")).getValue();
        String dataUrl = 
          ((DString) sequence.getVariable (i, "dataUrl")).getValue();
        String dapUrl = 
          ((DString) sequence.getVariable (i, "dapUrl")).getValue();
        String previewUrl = 
          ((DString) sequence.getVariable (i, "previewUrl")).getValue();
        int coverage = (
          searchByCoverage ? 
          ((DInt16) sequence.getVariable (i, coverVar)).getValue() :
          -1
        );

        // Check for identical entry by data URL
        // -------------------------------------
        if (entryKeys.contains (dataUrl)) continue;
        else entryKeys.add (dataUrl);
                                  
        // Create entry
        // ------------
        Entry entry = new Entry();
        entry.startDate = startDate;
        entry.endDate = endDate;
        entry.dataSource = sat + "/" + sensor;
        entry.coverage = coverage;
        entry.sceneTime = sceneTime;
        entry.downloadUrl = dataUrl;
        entry.dataUrl = dapUrl;
        entry.previewUrl = previewUrl;
        entryList.add (entry);

      } // try
      catch (NoSuchVariableException e) {
        throw new IOException ("Error accessing sequence data: " + 
          e.getMessage());
      } // catch
    } // for

    // Sort the entries by start date
    // ------------------------------
    Collections.sort (entryList);

    return (entryList);

  } // getEntries

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    OpendapQueryAgent agent = new OpendapQueryAgent (new URL (argv[0]));
    agent.setTimeByAge (100);
    agent.setCoverageByRegion ("xx", 0);
    List list = agent.getEntries();
    for (Iterator iter = list.iterator(); iter.hasNext();)
      System.out.println (iter.next());

  } // main

  ////////////////////////////////////////////////////////////

} // OpendapQueryAgent class

////////////////////////////////////////////////////////////////////////
