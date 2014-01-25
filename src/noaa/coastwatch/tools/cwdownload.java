////////////////////////////////////////////////////////////////////////
/*
     FILE: cwdownload.java
  PURPOSE: To download CoastWatch data from a server.
   AUTHOR: Peter Hollemans
     DATE: 2002/05/08
  CHANGES: 2002/05/13, PFH, added multiple satellite, region, and type
             specifications, added more verbose output
           2002/07/09, PFH, modified command line processing
           2002/07/16, PFH, added man page, and ground, test, force options
           2002/11/16, PFH, modified verbose messages
           2002/12/23, PFH, modified for pattern matching, added more
             documentation
           2003/01/24, PFH, removed default server, added host parameter
           2003/03/25, PFH, modified to use new ServerQuery, added coverage
             option
           2003/04/29, PFH, added age option
           2003/04/30, PFH, modified so that coverage and region indicate
             mapped products
           2003/05/16, PFH, updated documentation
           2003/09/02, PFH, modified to *not* exit on connection error
           2003/09/03, PFH, modified to abort when network hangs
           2004/10/10, PFH
           - modified to handle more network problems 
           - added --timeout option
           2005/01/30, PFH
           - added --projection option
           - modified to use CleanupHook class
           2005/03/16, PFH, reformatted documentation and usage note
           2005/04/23, PFH, added ToolServices.setCommandLine()
           2006/01/13, PFH, modified to delete partial file on transfer error
           2007/04/19, PFH, added version printing

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// -------
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.net.*;
import jargs.gnu.*;
import jargs.gnu.CmdLineParser.*;

/**
 * <p>The download tool facilitates the downloading of specific data
 * files from a CoastWatch data server.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwdownload - downloads data from a CoastWatch server.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>cwdownload [OPTIONS] host</p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -c, --script=PATH <br>
 * -d, --dir=PATH <br>
 * -f, --force <br>
 * -h, --help <br>
 * -t, --test <br>
 * -T, --timeout=SECONDS <br>
 * --version <br>
 * </p>
 *
 * <h3>Data file selection options:</h3>
 *
 * <p>
 * -a, --age=HOURS <br>
 * -C, --coverage=PERCENT <br>
 * -G, --station=PATTERN <br>
 * -p, --projection=TYPE <br>
 * -r, --region=PATTERN <br>
 * -s, --satellite=PATTERN <br>
 * -S, --scenetime=PATTERN <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The download tool retrieves a set of user-specified data files
 * from a CoastWatch data server.  Data files may be selected based on
 * satellite, scene time, region, ground station, and other
 * parameters.  Without any command line options, all data files on
 * the server are retrieved.  The command line options are used to
 * filter the list of data files.  Multiple options are used in
 * conjunction, for example if both <b>--satellite</b> and
 * <b>--scenetime</b> are specified, only files matching the specified
 * satellites and scene times will be retrieved.  The download tool
 * has a built-in facility for avoiding redundant file downloads.
 * Unless the <b>--force</b> option is used, files are only downloaded
 * if no file with the same name already exists in the local
 * directory.  The <b>--test</b> option may be used for testing file
 * download options without downloading any actual data. </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> host </dt>
 *   <dd> The CoastWatch server host name.  There is no default host
 *   name. </dd>
 *
 * </dl>
 *
 * <h3>General options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --script=PATH </dt> 
 *   <dd> ADVANCED USERS ONLY.  The query script path.  The default is
 *   /ctera/query.cgi. </dd>
 *
 *   <dt> -d, --dir=PATH </dt> 
 *   <dd> The download directory path.  The default is to download
 *   to the current directory. </dd>
 *
 *   <dt> -f, --force </dt> 
 *   <dd> Turns on forced mode.  When forced mode is in effect, no
 *   check is performed to determine if the file already exists in the
 *   download directory.  The default is to check if a file of the
 *   same name exists, and if so skip the download for that particular
 *   file. </dd>
 *
 *   <dt> -h, --help </dt> 
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -t, --test </dt> 
 *   <dd> Turns on test mode.  When the test mode is in effect, the
 *   data files are selected based on the specified command line
 *   parameters but no actual data is downloaded.  The default is to
 *   perform the data download.  Test mode may be used to determine if
 *   a certain set of command line parameters have the desired effect
 *   without having to wait for data files to transfer. </dd>
 *
 *   <dt> -T, --timeout=SECONDS </dt> 
 *   <dd> The network timeout in seconds.  If the network becomes
 *   unresponsive for the timeout period, the download is aborted.  If
 *   there is a file currently in the process of downloading when the
 *   timeout occurs, the partial file is deleted.  The default timeout
 *   is 30 seconds. </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h3>Data file selection options:</h3>
 *
 * <dl>
 *
 *   <dt> -a, --age=HOURS </dt>
 *   <dd> The maximum age of the data in hours.  Datasets contain a
 *   time stamp for the date and time that the data was taken by the
 *   sensor.  Only datasets created more recently than the specified
 *   number of hours ago are retrieved, based on the clock on the host
 *   computer running the download.  The default is to download
 *   regardless of date and time. </dd>
 *
 *   <dt> -C, --coverage=PERCENT </dt>
 *   <dd> The minimum data coverage in percent.  Mapped regions may
 *   have less than 100% coverage when the edge of the satellite pass
 *   intersects the region area.  The default minimum coverage is 0.
 *   The use of this option implies <b>--projection mapped</b>.</dd>
 *  
 *   <dt> -G, --station=PATTERN </dt>
 *   <dd> The ground station matching pattern.  Multiple ground
 *   stations may be specified with a regular expression, for example
 *   '(wi|mo|hi)' for the wi, mo, and hi stations.  The default is to
 *   download all ground stations. </dd>
 *
 *   <dt> -p, --projection=TYPE </dt>
 *   <dd> The data projection type.  Valid values are 'mapped' and
 *   'swath'.  Mapped datasets are those that have possibly been
 *   reduced in resolution, and registered to a standard map
 *   projection.  Swath datasets are generally at the full sensor
 *   resolution and unregistered sensor scan projection.  By default,
 *   all types of datasets are downloaded.</dd>
 *
 *   <dt> -r, --region=PATTERN </dt> 
 *   <dd> The region code matching pattern.  Multiple regions may be
 *   specified with a regular expression, for example '(er|sr|gr)' for
 *   the 'er', 'sr', and 'gr' regions.  The default is to download all
 *   regions. The use of this option implies <b>--projection
 *   mapped</b>.</dd>
 *
 *   <dt> -s, --satellite=PATTERN </dt> 
 *   <dd> The satellite name matching pattern.  For example,
 *   'noaa-16'.  Multiple satellites may be specified with a regular
 *   expression, for example 'noaa-1[56]' for NOAA-15 and NOAA-16.
 *   The default is to download data from all satellites.</dd>
 *
 *   <dt> -S, --scenetime=PATTERN </dt> 
 *   <dd> The scene time matching pattern.  Valid times are 'day',
 *   'night', and 'day/night'.  Multiple times may be specified with a
 *   regular expression, for example '(day|night)' for day and night.
 *   The default is to download all scene times.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Cannot contact data server. </li>
 *   <li> Invalid or write-protected download directory. </li>
 *   <li> Error transferring data file. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows a download command that retrieves
 * any NOAA-16 daytime data files for the East Coast north and south
 * regions captured at Wallops Island to the ~/cwatch/satdata directory from
 * the fictitious server <code>frobozz.noaa.gov</code>:</p>
 * <pre>
 *   phollema$ cwdownload --satellite noaa-16 --scenetime day 
 *     --region '(er|sr)' --station wi --dir ~/cwatch/satdata frobozz.noaa.gov
 *
 *   cwdownload: Contacting frobozz.noaa.gov
 *   cwdownload: Retrieving 2002_197_1719_n16_er.hdf
 *   cwdownload: Retrieving 2002_197_1719_n16_sr.hdf
 *   cwdownload: Retrieving 2002_197_1900_n16_er.hdf
 *   cwdownload: Retrieving 2002_197_1900_n16_sr.hdf
 *   cwdownload: Transferred 31715 kb in 4 files
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public final class cwdownload {

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwdownload";

  /** The maximum network stall time in milliseconds. */
  private static final int STALL_TIME = 30*1000;

  // Variables
  // ---------

  /** The current byte count. */
  private static long byteCount;

  /** The current count of files transferred. */
  private static int fileCount;

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String[] argv) {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option scriptOpt = cmd.addStringOption ('c', "script");
    Option satelliteOpt = cmd.addStringOption ('s', "satellite");
    Option scenetimeOpt = cmd.addStringOption ('S', "scenetime");
    Option regionOpt = cmd.addStringOption ('r', "region");
    Option stationOpt = cmd.addStringOption ('G', "station");
    Option coverageOpt = cmd.addDoubleOption ('C', "coverage");
    Option ageOpt = cmd.addDoubleOption ('a', "age");
    Option dirOpt = cmd.addStringOption ('d', "dir");
    Option testOpt = cmd.addBooleanOption ('t', "test");
    Option forceOpt = cmd.addBooleanOption ('f', "force");
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option timeoutOpt = cmd.addIntegerOption ('T', "timeout");
    Option projectionOpt = cmd.addStringOption ('p', "projection");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage ();
      System.exit (1);
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage ();
      System.exit (0);
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      System.exit (0);
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      System.err.println (PROG + ": At least " + NARGS + 
        " argument(s) required");
      usage ();
      System.exit (1);
    } // if
    String host = remain[0];

    // Set defaults
    // ------------
    String script = (String) cmd.getOptionValue (scriptOpt);
    if (script == null) script = "/ctera/query.cgi";
    String satellite = (String) cmd.getOptionValue (satelliteOpt);
    String scenetime = (String) cmd.getOptionValue (scenetimeOpt);
    String region = (String) cmd.getOptionValue (regionOpt);
    String station = (String) cmd.getOptionValue (stationOpt);
    Double coverage = (Double) cmd.getOptionValue (coverageOpt);
    Double age = (Double) cmd.getOptionValue (ageOpt);
    String dir = (String) cmd.getOptionValue (dirOpt);
    if (dir == null) dir = ".";
    boolean test = (cmd.getOptionValue (testOpt) != null);
    boolean force = (cmd.getOptionValue (forceOpt) != null);
    Integer timeoutObj = (Integer) cmd.getOptionValue (timeoutOpt);
    int timeout = (timeoutObj == null ? STALL_TIME : 
      timeoutObj.intValue()*1000);
    String projection = (String) cmd.getOptionValue (projectionOpt);
    
    // Create server query keys
    // ------------------------
    HashMap queryKeys = new HashMap();
    queryKeys.put ("query", "datasetDetails");
    queryKeys.put ("details", "file_name,data_url");
    queryKeys.put ("order", "date,time");
    if (satellite != null) queryKeys.put ("satellite", satellite);
    if (scenetime != null) queryKeys.put ("scene_time", scenetime);
    if (region != null) queryKeys.put ("region_id", region);
    if (station != null) queryKeys.put ("station_id", station);
    if (coverage != null) queryKeys.put ("coverage", coverage.toString());
    if (age != null) {
      Date date = new Date ((long) (System.currentTimeMillis() - 
        age.doubleValue()*3600000));
      SimpleDateFormat dateFormat = new SimpleDateFormat (
        "yyyy-MM-dd HH:mm:ss");
      dateFormat.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
      String after = dateFormat.format (date);
      queryKeys.put ("after", after);
    } // if
    if (region != null || coverage != null) 
      queryKeys.put ("projection_type", "mapped");
    else if (projection != null) {
      if (!projection.equals ("mapped") && !projection.equals ("swath")) {
        System.err.println (PROG + ": Invalid projection type '" + 
          projection + "'");
        System.exit (2);
      } // if
      queryKeys.put ("projection_type", projection);
    } // else if

    // Create query timeout
    // --------------------
    Timeout queryTimeout = new Timeout (timeout, new Runnable() { 
       public void run () {
          System.err.println (PROG + ": Server not responding, aborting");
          System.exit (2);
        } // run
      });

    // Perform server query
    // --------------------
    ServerQuery query = null;
    System.out.println (PROG + ": Contacting " + host);
    queryTimeout.start();
    try {
      query = new ServerQuery (host, script, queryKeys);
    } catch (Exception e) { 
      System.err.println (PROG + ": " + e.getMessage());
      System.exit (2);
    } // catch
    queryTimeout.cancel();

    // Create stall monitor
    // --------------------
    StallMonitor monitor = new StallMonitor (timeout, new Runnable() {
        public void run () {
          System.err.println (PROG + ": Network stall detected, aborting");
          System.exit (2);
        } // run
      });

    // Loop over each result
    // ---------------------
    for (int i = 0; i < query.getResults(); i++) {

      // Get file name
      // -------------
      String file = query.getValue (i, "file_name");

      // Check if file exists
      // --------------------
      String outName = dir + "/" + file;
      final File outFile = new File (outName);
      if (!force && outFile.exists()) {
        System.out.println (PROG + ": File " + file + " exists, skipping");
        continue;
      } // if

      // Check for test mode
      // -------------------
      System.out.print (PROG + ": Retrieving " + file);
      if (test) {
        System.out.println (" (test mode)");
        fileCount++;
        continue;
      } // if
      else System.out.println();

      // Create data transfer
      // --------------------
      URL url = null;
      FileOutputStream out = null;
      try { 
        url = new URL (query.getValue (i, "data_url"));
        out = new FileOutputStream (outFile);
      } // try
      catch (Exception e) {
        e.printStackTrace();
        System.exit (2);
      } // catch
      final DataTransfer transfer = new URLTransfer (url, out);
      transfer.addDataTransferListener (monitor);
      transfer.addDataTransferListener (new DataTransferAdapter () {
          public void transferError (DataTransferEvent e) {
            System.err.println (PROG + ": Error in data transfer, skipping");
            if (outFile.exists()) outFile.delete();
          } // transferError
          public void transferEnded (DataTransferEvent e) {
            byteCount += transfer.getTransferred();
            fileCount++;
          } // transferEnded
        });
      
      // Perform transfer
      // ----------------
      CleanupHook.getInstance().scheduleDelete (outFile);
      transfer.run();
      try { transfer.close(); } catch (IOException e) { }
      CleanupHook.getInstance().cancelDelete (outFile);

    } // for

    // Print final counts
    // ------------------
    System.out.println (PROG + ": Transferred " + byteCount/1024 +
      " kb in " + fileCount + " files");    
    System.exit (0);

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwdownload [OPTIONS] host\n" +
"Retrieves a set of user-specified data files from a CoastWatch data\n" +
"server.\n" +
"\n" +
"Main parameters:\n" +
"  host                       The CoastWatch server host name.\n" +
"\n" +
"General options:\n" +
"  -c, --script=PATH          Set host query script path (advanced users).\n" +
"  -d, --dir=PATH             Set local directory path.\n" +
"  -f, --force                Do not check if file already exists locally.\n" +
"  -h, --help                 Show this help message.\n" +
"  -t, --test                 Do not download, run test mode only.\n" +
"  -T, --timeout=SECONDS      Set network timeout in seconds.\n" +
"  --version                  Show version information.\n" +
"\n" +
"Data file selection options:\n" +
"  -a, --age=HOURS            Set maximum data file age in hours.\n" +
"  -c, --script=PATH          Set host query script path (advanced users).\n" +
"  -C, --coverage=PERCENT     Set minimum data coverage in percent.\n" +
"  -G, --station=PATTERN      Set ground station matching pattern.\n" +
"  -p, --projection=TYPE      Set desired projection type.  TYPE may be\n" +
"                              'mapped' or 'swath'.\n" +
"  -r, --region=PATTERN       Set region code matching pattern.\n" +
"  -s, --satellite=PATTERN    Set satellite name matching pattern.\n" +
"  -S, --scenetime=PATTERN    Set scene time matching pattern.  Scene times\n"+
"                              are 'day', 'night', or 'day/night' for\n" +
"                              mixed scenes.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwdownload () { }

  ////////////////////////////////////////////////////////////

} // cwdownload class

////////////////////////////////////////////////////////////////////////
