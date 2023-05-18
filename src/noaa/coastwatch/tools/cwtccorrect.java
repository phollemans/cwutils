/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.tools;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.io.IOServices;
import noaa.coastwatch.render.Topography;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.AtmosphericCorrection;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.MetadataServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ChunkCollector;
import noaa.coastwatch.util.chunk.PoolProcessor;
import noaa.coastwatch.util.chunk.ChunkComputation;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.GridChunkConsumer;
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkOperation;
import noaa.coastwatch.util.chunk.ChunkDataCast;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The true color correction tool corrects top-of-atmosphere true color reflectance data.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->
 *   cwtccorrect - corrects top-of-atmosphere true color reflectance data.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p> cwtccorrect [OPTIONS] input [output] </p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -b, --bands=RED/GREEN/BLUE <br>
 * -e, --esun=RED/GREEN/BLUE <br>
 * -n, --nameext=TEXT <br>
 * -p, --props=FILE <br>
 * -s, --sensor=NAME <br>
 * --threads=MAX <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 * 
 * <h3>Angle options:</h3>
 * 
 * <p>
 * -l, --latitude=VARIABLE <br>
 * -L, --longitude=VARIABLE <br>
 * -u, --sunzen=VARIABLE <br>
 * -U, --sunazi=VARIABLE <br>
 * -a, --satzen=VARIABLE <br>
 * -A, --satazi=VARIABLE <br>
 * -r, --relazi=VARIABLE <br>
 * </p>
 *
 * <h2>Description</h2>
 * 
 * <p>The true color correction tool corrects top-of-atmosphere true color 
 * reflectance data.  The correction accounts for molecular (Rayleigh) 
 * scattering and gaseous absorption (water vapor, ozone) but performs no 
 * aerosol correction (dust, ash, smoke, sulfate).  No real-time input or 
 * ancillary data is required.
 * The algorithm used was originally written by Jacques Descloitres 
 * for use with the <a href="http://rapidfire.sci.gsfc.nasa.gov">MODIS 
 * Rapid Response Project</a>, NASA/GSFC/SSAI.</p>
 * 
 * <p>The input file can be in either level 2 or level 3 projection, and must at
 * a minimum contain:</p>
 * <ul> 
 * 
 *   <li> Red, green, and blue color band data.  By default these are found
 *   by looking for common variable names for a given sensor used in CoastWatch product 
 *   files, or can be specified directly using the <b>--bands</b> option.  Band
 *   data can either be in radiance units or reflectance units.  If in radiance
 *   units, the data is first normalized to reflectance using a known solar irradiance
 *   value for the sensor band and solar zenith angle data. </li>
 * 
 *   <li> Latitude and longitude geolocation data, and zenith and azimuth 
 *   angles for both sun and satellite. By default
 *   the correct variables containing the angle data are detected from a 
 *   combination of the variable names and the long name (if any) specified in the 
 *   metadata.  Note that if relative azimuth angle data is found,
 *   it will be used instead of the individal solar and satellite azimuth 
 *   angles. </li>
 * 
 * </ul>
 * 
 * <p>The result of the operation is to write a new set of band reflectance 
 * variables into the input file, or into a new output file, by 
 * appending '_refl' to the band variable names.  The corrected surface 
 * reflectance is an approximation only, but is well suited for providing
 * near real time true color imagery.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> input </dt>
 *   <dd> The input data file name. </dd>
 *
 *   <dt> output </dt>
 *   <dd> The output data file name.  If specified and the file does not
 *   already exist, it will be created using metadata from the 
 *   input file.  If it does exist, it will be opened and checked for
 *   a compatible earth transform and the new corrected reflectance data will be
 *   added to the file. The new variables must not already exist in the 
 *   output file. </dd>
 *
 * </dl>
 *
 * <h3>General options:</h3>
 *
 * <dl>
 *
 *   <dt> -h, --help </dt>
 * 
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -b, --bands=RED/GREEN/BLUE </dt>
 * 
 *   <dd> The names of the band variables to use for each of red, green,
 *   and blue wavelengths.  By default when the sensor is known, the band names 
 *   are automatically determined.  Use this option to overrride the default
 *   band names, or in case the sensor is unknown. </dd>
 * 
 *   <dt> -e, --esun=RED/GREEN/BLUE </dt>
 *   
 *   <dd> Specifies the values of the solar irradiance (the esun constant) for each of the
 *   red, green, and blue wavelengths in mW/cm<sup>2</sup>/um. This is only needed when the data
 *   is in radiance units rather than reflectance units.  By default when the 
 *   sensor is known, the solar irradiance values are automatically determined.  
 *   Use this option to overrride the default solar irradiance values, or in 
 *   case the sensor is unknown. </dd>
 *
 *   <dt>-n, --nameext=TEXT </dt>
 *
 *   <dd> The text to use to extend the band variable names when creating the new
 *   corrected reflectance variables.  By default '_refl' is used to extend
 *   the band variable names. </dd>   
 *
 *   <dt> -p, --props=FILE </dt>
 * 
 *   <dd> The path for a custom properties file giving the names of supported
 *   sensors and their bands and solar irradiance values.
 *   By default the <code>true_color.properties</code> 
 *   file in the installation directory <code>data/noaa/coastwatch/tools</code> 
 *   is used.  The format of the properties file is a set of key/value pairs.
 *   For example to add the new <i>Zork Visible Spectrum Imager</i> aboard the satellite 
 *   <i>Frobozz-5</i> to the list of allowed sensors, you can create a new properties
 *   file as follows:
 *   <pre>
 *     sensors = fbz5_zvsi
 *
 *     fbz5_zvsi.keywords = frobozz-5 fbz-5 zvsi
 *     fbz5_zvsi.red.band = Band_3
 *     fbz5_zvsi.green.band = Band_2
 *     fbz5_zvsi.blue.band = Band_1
 *     fbz5_zvsi.red.esun = 150.072
 *     fbz5_zvsi.green.esun = 181.820
 *     fbz5_zvsi.blue.esun = 195.128
 *   </pre>
 *   Then supply the new properties file path to the <b>--props</b> option.  
 *   This helps to reduce the command line parameters needed to correct data 
 *   for an unsupported sensor.  Alternatively, you can also add the content 
 *   above to the default <code>true_color.properties</code> file and the
 *   new sensor will be automatically detecting using the keywords.</dd>
 * 
 *   <dt> -s, --sensor=NAME </dt>
 * 
 *   <dd> The name of the sensor to use.  This is normally determined
 *   automatically from the metadata, but if the automatic test fails, use
 *   this option.  The currently supported sensors are 'n20_viirs', 'npp_viirs',
 *   's2a_msi', 's2b_msi', 's3a_olci', and 's3b_olci'.  The sensor name is 
 *   used to determine the solar irradiance 
 *   values and the band names for known sensors, and is not needed if both 
 *   the <b>--bands</b> and <b>--esun</b> options are set. </dd>
 *
 *   <dt>--threads=MAX</dt>
 *
 *   <dd>Specifies the maximum number of threads for parallel processing. By
 *   default the program will automatically detect the maximum number of
 *   threads possible.  To process data in serial, use a max value of 1.</dd>
 *
 *   <dt> -v, --verbose </dt>
 *
 *   <dd> Turns verbose mode on.  The current status of data
 *   conversion is printed periodically.  The default is to run
 *   quietly. </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h3>Angle options:</h3>
 *
 * <dl>
 * 
 *   <dt> -l, --latitude=VARIABLE </dt>
 * 
 *   <dd> The latitude angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -L, --longitude=VARIABLE </dt>
 * 
 *   <dd> The longitude angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -u, --sunzen=VARIABLE </dt>
 * 
 *   <dd> The solar zenith angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -U, --sunazi=VARIABLE </dt>
 * 
 *   <dd> The solar azimuth angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -a, --satzen=VARIABLE </dt>
 * 
 *   <dd> The satellite zenith angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -A, --satazi=VARIABLE </dt>
 * 
 *   <dd> The satellite azimuth angle variable name, by default determined from metadata. </dd>
 *
 *   <dt> -r, --relazi=VARIABLE </dt>
 *
 *   <dd> The relative azimuth angle variable name, by default determined from metadata. Note
 *   that relative azimuth is only needed if either the satellite azimuth
 *   or solar azimuth are not available. </dd>
 *
 * </dl>
 * 
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Unsupported input file format </li>
 *   <li> Input / output file dates or earth transforms do not match </li>
 *   <li> Sensor not supported </li>
 *   <li> Angle data could not be automatically detected </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the true color correction of level 2 NOAA-20 VIIRS 
 * data with verbose mode on:</p>
 * <pre>
 *   phollema$ cwtccorrect -v VRSLCW.C2023001.102322.hdf 
 *     VRSLCW.C2023001.102322.corr.hdf
 *   [INFO] Opening input VRSLCW.C2023001.102322.hdf
 *   [INFO] Creating output VRSLCW.C2023001.102322.corr.hdf
 *   [INFO] Found supported sensors: [n20_viirs, npp_viirs, s2a_msi, s2b_msi, s3a_olci, s3b_olci]
 *   [INFO] Detected sensor 'n20_viirs'
 *   [INFO] Computing reflectance for red band 'EV_BandM5'
 *   [INFO] Using Earth-Sun distance factor 1.0169916, solar irradiance 151.096 mW/cm^2/um
 *   [INFO] Using variable 'sun_zenith' in reflectance computation
 *   [INFO] Total grid size is 768x3200
 *   [INFO] Using 12 parallel threads for processing
 *   [INFO] Processing 27 data chunks of size 362x362
 *   [INFO] Computing reflectance for green band 'EV_BandM4'
 *   [INFO] Using Earth-Sun distance factor 1.0169916, solar irradiance 182.753 mW/cm^2/um
 *   [INFO] Total grid size is 768x3200
 *   [INFO] Using 12 parallel threads for processing
 *   [INFO] Processing 27 data chunks of size 362x362
 *   [INFO] Computing reflectance for blue band 'EV_BandM3'
 *   [INFO] Using Earth-Sun distance factor 1.0169916, solar irradiance 197.808 mW/cm^2/um
 *   [INFO] Total grid size is 768x3200
 *   [INFO] Using 12 parallel threads for processing
 *   [INFO] Processing 27 data chunks of size 362x362
 *   [INFO] Using variable 'latitude' for latitude angle data
 *   [INFO] Using variable 'longitude' for longitude angle data
 *   [INFO] Using variable 'sun_zenith' for solar zenith angle data
 *   [INFO] Using variable 'sun_azimuth' for solar azimuth angle data
 *   [INFO] Using variable 'sat_zenith' for satellite zenith angle data
 *   [INFO] Using variable 'sat_azimuth' for satellite azimuth angle data
 *   [INFO] Computing atmospheric correction for true color bands
 *   [INFO] Total grid size is 768x3200
 *   [INFO] Using 12 parallel threads for processing
 *   [INFO] Processing 27 data chunks of size 362x362
 * 
 *   phollema$ cwinfo VRSLCW.C2023001.102322.corr.hdf
 *   Contents of VRSLCW.C2023001.102322.corr.hdf
 *
 *   Global information:
 *     Satellite:           NOAA-20
 *     Sensor:              VIIRS
 *     Date:                2023/01/01 JD 001
 *     Start time:          10:23:22 UTC
 *     End time:            10:24:46 UTC
 *     Projection type:     swath
 *     Transform ident:     noaa.coastwatch.util.trans.SwathProjection
 *     Origin:              NOAA/NESDIS/STAR/SOCD
 *     Format:              CoastWatch HDF version 3.4
 *     Reader ident:        noaa.coastwatch.io.CWHDFReader
 *   
 *   Variable information:
 *     Variable         Type    Dimensions   Units     Scale   Offset   
 *     latitude         float   768x3200     degrees   -       -        
 *     longitude        float   768x3200     degrees   -       -        
 *     EV_BandM5_refl   float   768x3200     1         -       -        
 *     EV_BandM4_refl   float   768x3200     1         -       -        
 *     EV_BandM3_refl   float   768x3200     1         -       -        
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class cwtccorrect {

  private static final String PROG = cwtccorrect.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");
  
  // Constants
  // ------------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  private static final int RED = 0;
  private static final int GREEN = 1;
  private static final int BLUE = 2;

  private static final String PROPERTIES_FILE = "true_color.properties";

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    ToolServices.startExecution (PROG);
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();

    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option sensorOpt = cmd.addStringOption ('s', "sensor");
    Option bandsOpt = cmd.addStringOption ('b', "bands");
    Option esunOpt = cmd.addStringOption ('e', "esun");
    Option propsOpt = cmd.addStringOption ('p', "props");
    Option threadsOpt = cmd.addIntegerOption ("threads");
    Option nameextOpt = cmd.addStringOption ('n', "nameext");

    Option latitudeOpt = cmd.addStringOption ('l', "latitude");
    Option longitudeOpt = cmd.addStringOption ('L', "longitude");
    Option sunzenOpt = cmd.addStringOption ('u', "sunzen");
    Option sunaziOpt = cmd.addStringOption ('U', "sunazi");
    Option satzenOpt = cmd.addStringOption ('a', "satzen");
    Option sataziOpt = cmd.addStringOption ('A', "satazi");
    Option relaziOpt = cmd.addStringOption ('r', "relazi");

    Option versionOpt = cmd.addBooleanOption ("version");

    try { cmd.parse (argv); }
    catch (OptionException e) {
      LOGGER.warning (e.getMessage());
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage();
      ToolServices.exitWithCode (0);
      return;
    } // if

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      ToolServices.exitWithCode (0);
      return;
    } // if

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if
    String input = remain[0];
    String output = (remain.length > 1 ? remain[1] : input);

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);

    String sensor = (String) cmd.getOptionValue (sensorOpt);
    String bands = (String) cmd.getOptionValue (bandsOpt);
    String esunStr = (String) cmd.getOptionValue (esunOpt);

    String latitude = (String) cmd.getOptionValue (latitudeOpt);
    String longitude = (String) cmd.getOptionValue (longitudeOpt);
    String sunzen = (String) cmd.getOptionValue (sunzenOpt);
    String sunazi = (String) cmd.getOptionValue (sunaziOpt);
    String satzen = (String) cmd.getOptionValue (satzenOpt);
    String satazi = (String) cmd.getOptionValue (sataziOpt);
    String relazi = (String) cmd.getOptionValue (relaziOpt);

    String propsStr = (String) cmd.getOptionValue (propsOpt);
    Integer threadsObj = (Integer) cmd.getOptionValue (threadsOpt);
    int threads = (threadsObj == null ? -1 : threadsObj.intValue());
    String nameExt = (String) cmd.getOptionValue (nameextOpt);
    if (nameExt == null) nameExt = "_refl";

    EarthDataReader reader = null;
    CWHDFWriter writer = null;
    CWHDFReader outputReader = null;

    // We set the reader to default to reading a swath projection as a data
    // projection to preserve the sensor geometery as much as possible if there
    // are level 2 files
    EarthDataReader.setDataProjection (true);

    try {

      // Start by opening the output file if it exists and retrieving
      // the output transform.  We'll use this later to check against the
      // input file.
      EarthTransform outputTransform = null;
      File outputFile = new File (output);
      if (outputFile.exists()) {
        VERBOSE.info ("Checking output " + output);
        outputReader = new CWHDFReader (output);
        outputTransform = outputReader.getInfo().getTransform();
        outputReader.close();
        outputReader = null;
      } // if

      // If there's only one file specified, open that file for both input
      // and output.  It needs to be a CWHDF file in that case.
      if (input.equals (output)) {
        VERBOSE.info ("Opening input/output " + output);
        writer = new CWHDFWriter (output);
        reader = new CWHDFReader (writer);
      } // if

      // If there are two files specified, then open the input file and check
      // that the transforms match if there's an existing output file.
      else {
        VERBOSE.info ("Opening input " + input);
        reader = EarthDataReaderFactory.create (input);
        if (outputTransform != null) {
          EarthTransform inputTransform = reader.getInfo().getTransform();
          if (!inputTransform.equals (outputTransform)) {
            LOGGER.severe ("Earth transforms do not match for " + input + " and " + output);
            ToolServices.exitWithCode (2);
            return;
          } // if
        } // if
      } // else

      // If there's an existing output file, then open it, otherwise create
      // a new one with the same transform as the input.
      if (writer == null) {
        if (outputFile.exists()) {
          VERBOSE.info ("Opening output " + output);
          writer = new CWHDFWriter (output);
        } // if
        else {
          VERBOSE.info ("Creating output " + output);
          CleanupHook.getInstance().scheduleDelete (output);
          writer = new CWHDFWriter (reader.getInfo(), output);
        } // else
      } // if

      // Access the properties and find the supported sensors
      var props = new Properties();
      String propsPath = propsStr != null ? propsStr : IOServices.getFilePath (cwtccorrect.class, PROPERTIES_FILE);
      var stream = new FileInputStream (propsPath);
      props.load (stream);
      stream.close();    
      String[] sensors = props.getProperty ("sensors").split (" +");
      VERBOSE.info ("Found supported sensors: " + java.util.Arrays.toString (sensors));

      // Determine the input data platform and sensor if the user didn't supply
      // it.  This will be used to get the band names and solar irradiance 
      // values (if needed).
      if (sensor == null) {
        var source = reader.getInfo().getSource().toLowerCase();
        int maxScore = 0;
        for (var sensorName : sensors) {  
          String keywords = props.getProperty (sensorName + ".keywords");
          if (keywords == null) {
            LOGGER.warning ("Sensor keywords not set in properties file for " + sensorName);
          } // if
          else {
            String[] keywordArray = keywords.toLowerCase().split (" +");
            int score = 0;
            for (var keyword : keywordArray) { if (source.indexOf (keyword) != -1) score++; }
            if (score > maxScore) { maxScore = score; sensor = sensorName; }
          } // else
        } // for
        if (sensor == null) {
          LOGGER.severe ("Cannot detect sensor from data source '" + source + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        if (maxScore < 2)
          LOGGER.warning ("Detected sensor '" + sensor + "' with low confidence score");
        else
          VERBOSE.info ("Detected sensor '" + sensor + "'");
      } // if

      else {
        VERBOSE.info ("Using sensor '" + sensor + "'");
      } // else

      boolean sensorSupported = false;
      for (int i = 0; i < sensors.length; i++)
        if (sensors[i].equals (sensor)) { sensorSupported = true; break; }

      // Get the band variable names if the user supplied them, or read them
      // from the properties file if not
      String[] bandColors = new String[] {"red", "green", "blue"};
      String[] bandNames = new String[3];
      int[] bandIndices = new int[] {RED, GREEN, BLUE};
      if (bands != null) {
        String[] bandArray = bands.split (ToolServices.SPLIT_REGEX);
        if (bandArray.length != 3) {
          LOGGER.severe ("Invalid number of bands specified '" + bands + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        for (int i : bandIndices) bandNames[i] = bandArray[i];
      } // if
      else if (sensorSupported) {
        for (int i : bandIndices) bandNames[i] = props.getProperty (sensor + "." + bandColors[i] + ".band");
      } // else if
      else {
        LOGGER.severe ("No support found for sensor '" + sensor + "', specify a different sensor or use --bands");
        ToolServices.exitWithCode (2);
        return;
      } // else

      // Parse the solar irradiance values if the user supplied them
      float[] esunArray = null;
      if (esunStr != null) {
        esunArray = new float[3];
        String[] esunStrArray = esunStr.split (ToolServices.SPLIT_REGEX);
        if (esunStrArray.length != 3) {
          LOGGER.severe ("Invalid number of solar irradiance values specified '" + esunStr + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        for (int i : bandIndices) {
          esunArray[i] = Float.parseFloat (esunStrArray[i]);
        } // for
      } // if

      // Compute the Earth-Sun distance factor for use in reflectance
      // calculations (if we need to convert radiance to reflectance)
      Calendar cal = Calendar.getInstance();
      cal.setTime (reader.getInfo().getStartDate());
      cal.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
      int julianDay = cal.get (Calendar.DAY_OF_YEAR);
      float d = (float) (1.0/(1-0.01673*Math.cos (0.9856*(julianDay-4)*Math.PI/180)));

      // Find the solar zenith angle variable needed to compute reflectance 
      // from radiance
      if (sunzen == null) 
        sunzen = findVariable (reader, List.of ("solar zenith", "sun zenith"), 0.8);
      Grid sunzenGrid = null;

      // Access each true color band and determine if it needs to be normalized 
      // to reflectance units from radiance.  Perform the calculation if needed
      // using the solar zenith angle and the solar irradiance, and write the
      // data to the output file.  If not, just copy the chunk data into the
      // new reflectance variable so that it's ready for atmospheric correction.
      var bandReflGrids = new Grid[3];
      for (int i : bandIndices) {

        var band = (Grid) reader.getVariable (bandNames[i]);
        boolean isReflectance = band.getUnits().equals ("1");
    
        int[] dims = band.getDimensions();
        Grid bandRefl = new Grid (
          band.getName() + nameExt, 
          "Corrected reflectance in " + bandColors[i] + " true color band", "1",
          dims[Grid.ROWS], dims[Grid.COLS],
          new float[] {}, new DecimalFormat ("0.#######"), 
          null, Float.NaN
        );
        bandRefl = new HDFCachedGrid (bandRefl, writer);
        var consumer = new GridChunkConsumer (bandRefl);
        var prototypeReflChunk = consumer.getPrototypeChunk();
        bandReflGrids[i] = bandRefl;

        var collector = new ChunkCollector();
        int bandIndex = 0;
        collector.addProducer (new GridChunkProducer (band));

        ChunkFunction function = null;
        if (isReflectance) {
          VERBOSE.info ("Creating copy of " + bandColors[i] + " reflectance band '" + bandNames[i] + "'");
          function = chunks -> { return (chunks.get (bandIndex)); };
        } // if

        else {

          if (!sensorSupported && esunArray == null) {
            LOGGER.severe ("Reflectance computation requires solar irradiance values, use --esun option");
            ToolServices.exitWithCode (2);
            return;
          } // if

          VERBOSE.info ("Computing reflectance for " + bandColors[i] + " band '" + bandNames[i] + "'");
          float esun = (esunArray != null ? esunArray[i] : 
            Float.parseFloat (props.getProperty (sensor + "." + bandColors[i] + ".esun"))
          );
          VERBOSE.info ("Using Earth-Sun distance factor " + d + ", solar irradiance " + esun + " mW/cm^2/um");

          if (sunzenGrid == null) {
            if (sunzen != null && reader.getAllGrids().contains (sunzen)) 
              VERBOSE.info ("Using variable '" + sunzen + "' in reflectance computation");
            else {
              LOGGER.severe ("Solar zenith angle data not found, use --sunzen option");
              ToolServices.exitWithCode (2);
              return;
            } // else
          } // if
    
          int sunzenIndex = 1;
          if (sunzenGrid == null) sunzenGrid = (Grid) reader.getVariable (sunzen);
          collector.addProducer (new GridChunkProducer (sunzenGrid));

          function = chunks -> {
            
            var bandChunk = chunks.get (bandIndex);
            int values = bandChunk.getValues();
            float[] bandData = new float[values];
            ChunkDataCast.toFloatArray (bandChunk, bandData);
                
            var sunzenChunk = chunks.get (sunzenIndex);
            float[] sunzenData = new float[values];
            ChunkDataCast.toFloatArray (sunzenChunk, sunzenData);

            var reflData = new float[values];
            for (int j = 0; j < values; j++) {
              double cosTheta = Math.cos (Math.toRadians (sunzenData[j]));
              reflData[j] = (float) ((Math.PI * bandData[j])/(d * esun * cosTheta));
            } // for

            var reflChunk = prototypeReflChunk.blankCopyWithValues (values);
            ChunkDataCast.fromFloatArray (reflData, reflChunk);
            
            return (reflChunk);

          };

        } // else

        perform (new ChunkComputation (collector, consumer, function), consumer.getNativeScheme(), threads);

      } // for

      // Access the true color bands and create both chunk producers and
      // chunk consumers.
      var bandProducers = new ChunkProducer[3];
      var bandConsumers = new ChunkConsumer[3];
      for (int i : bandIndices) {
        bandProducers[i] = new GridChunkProducer (bandReflGrids[i]);
        bandConsumers[i] = new GridChunkConsumer (bandReflGrids[i]);
      } // for

      // Access the angle data: lat, lon, sat zenith,
      // sun zenith, and either sat/sun azimuth or relative azimuth
      int LAT = 0;
      int LON = 1;
      int SUN_ZEN = 2;
      int SUN_AZI = 3;
      int SAT_ZEN = 4;
      int SAT_AZI = 5;
      int REL_AZI = 6;
      var angleNames = new String[] {
        latitude,
        longitude,
        sunzen,
        sunazi,
        satzen,
        satazi,
        relazi
      };
      var angleOptions = new String[] {
        "latitude",
        "longitude",
        "sunzen",
        "sunazi",
        "satzen",
        "satazi",
        "relazi"
      };
      var angleTerms = List.of (
        List.of ("latitude", "lat"),
        List.of ("longitude", "lon"),
        List.of ("solar zenith", "sun zenith"),
        List.of ("solar azimuth", "sun azimuth"),
        List.of ("satellite zenith", "sat zenith", "sensor zenith"),
        List.of ("satellite azimuth", "sat azimuth", "sensor azimuth"),
        List.of ("relative azimuth", "rel azimuth")
      );
      var angleOptional = new boolean[] {
        false,
        false,
        false,
        true,
        false,
        true,
        true
      };

      for (int i = 0; i < angleNames.length; i++) {

        if (angleNames[i] == null)
          angleNames[i] = findVariable (reader, angleTerms.get (i), 0.8);

        var angle = angleTerms.get (i).get (0);
        if (angleNames[i] != null) {
          if (reader.getAllGrids().contains (angleNames[i]))
            VERBOSE.info ("Using variable '" + angleNames[i] + "' for " + angle + " angle data");
          else {
            LOGGER.severe ("Variable '" + angleNames[i] + "' not found for " + angle + " angle data");
            ToolServices.exitWithCode (2);
            return;
          } // else
        } // if
        else if (!angleOptional[i]) {
          angle = angle.substring (0, 1).toUpperCase() + angle.substring (1);
          LOGGER.severe (angle + " angle data not found, use --" + angleOptions[i] + " option");
          ToolServices.exitWithCode (2);
          return;
        } // else if

      } // for 

      boolean haveSunAzi = (angleNames[SUN_AZI] != null);
      boolean haveSatAzi = (angleNames[SAT_AZI] != null);
      boolean haveRelAzi = (angleNames[REL_AZI] != null);
      boolean needAzimuth = (!haveRelAzi && (!haveSunAzi || !haveSatAzi));

      if (!haveRelAzi) {
        if (!haveSunAzi)
          LOGGER.severe ("No relative azimuth or solar azimuth angle data found, use either --relazi or --sunazi");
        else if (!haveSatAzi)
          LOGGER.severe ("No relative azimuth or satellite azimuth angle data found, use either --relazi or --satazi");
      } // if
      if (needAzimuth) {
        ToolServices.exitWithCode (2);
        return;
      } // if

      var angleProducers = new ChunkProducer[angleNames.length];
      for (int i = 0; i < angleNames.length; i++) {
        if (angleNames[i] != null) {
          var grid = (Grid) reader.getVariable (angleNames[i]);
          angleProducers[i] = new GridChunkProducer (grid);
        } // if
      } // for

      // Access the elevation data
      var topo = Topography.getInstance();
      var elev = topo.getElevation();
      var elevTrans = topo.getTransform();

      // Set up for computing the atmospheric correction operation
      var atmosCorrect = AtmosphericCorrection.getInstance();
      var bandNumbers = new int[] {1, 4, 3};
      ChunkOperation op = pos -> {

        // Get the band data into primitive float arrays
        int width = pos.length[0];
        int height = pos.length[1];
        int count = width*height;

        var bandChunks = new DataChunk[3];
        var bandReflect = new float[3][count];
        for (int i : bandIndices) {
          bandChunks[i] = bandProducers[i].getChunk (pos);
          ChunkDataCast.toFloatArray (bandChunks[i], bandReflect[i]);
        } // for

        // Get the angle data into primitive float arrays
        var angleData = new float[angleProducers.length][];
        for (int i = 0; i < angleProducers.length; i++) {
          if (angleProducers[i] != null) {
            var chunk = angleProducers[i].getChunk (pos);
            angleData[i] = new float[count];
            ChunkDataCast.toFloatArray (chunk, angleData[i]);
          } // if
        } // for

        // Get the elevation data.  Note that we have to synchronize the 
        // access to the elevation grid here, just like in the GridChunkProducer
        // class.
        var elevData = new float[count];
        DataLocation dataLoc = new DataLocation (2);
        EarthLocation earthLoc = new EarthLocation();
        synchronized (elev) {
          for (int i = 0; i < count; i++) {
            earthLoc.setCoords (angleData[LAT][i], angleData[LON][i]);
            elevTrans.transform (earthLoc, dataLoc);
            elevData[i] = (float) elev.getValue (dataLoc);
            if (Float.isNaN (elevData[i]) || elevData[i] < 0) elevData[i] = 0;
          } // for
        } // synchronized

        // Perform the atmospheric correction and save to the reflectance
        // chunks
        atmosCorrect.correct (width, height, bandNumbers, bandReflect, angleData[SUN_ZEN],
          angleData[SUN_AZI], angleData[SAT_ZEN], angleData[SAT_AZI], angleData[REL_AZI], 
          elevData);

        for (int i : bandIndices) {
          ChunkDataCast.fromFloatArray (bandReflect[i], bandChunks[i]);
          bandConsumers[i].putChunk (pos, bandChunks[i]);
        } // for
      
      };

      // Perform the operation
      VERBOSE.info ("Computing atmospheric correction for true color bands");
      perform (op, bandConsumers[RED].getNativeScheme(), threads);

      // Close the reader and writer
      reader.close();
      reader = null;
      if (writer != null) { 
        writer.close(); 
        writer = null; 
        CleanupHook.getInstance().cancelDelete (output);
      } // if

    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try { 
        if (reader != null) reader.close(); 
        if (writer != null) writer.close();
      } // try
      catch (Exception e) { LOGGER.log (Level.SEVERE, "Error closing resources", e); }
    } // finally

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Performs a chunk operation at all positions in a chunking scheme.
   * 
   * @param op the operation to perform.
   * @param scheme the chunking scheme to perform over.
   * @param threads the number of processing threads to use or -1 for all 
   * available threads.
   */
  private static void perform (
    ChunkOperation op,
    ChunkingScheme scheme,
    int threads
  ) {

    int[] chunkingDims = scheme.getDims();
    VERBOSE.info ("Total grid size is " + chunkingDims[0] + "x" + chunkingDims[1]);

    int maxOps = 0;
    int processors = Runtime.getRuntime().availableProcessors();
    if (threads < 0) maxOps = processors;
    else maxOps = Math.min (threads, processors);
    if (maxOps < 1) maxOps = 1;
    if (maxOps > 1) VERBOSE.info ("Using " + maxOps + " parallel threads for processing");

    List<ChunkPosition> positions = new ArrayList<>();
    scheme.forEach (positions::add);

    int[] chunkSize = scheme.getChunkSize();
    VERBOSE.info ("Processing " + positions.size() +
      " data chunks of size " + chunkSize[0] + "x" + chunkSize[1]);

    if (maxOps == 1) {
      positions.forEach (pos -> op.perform (pos));
    } // if
    else {
      PoolProcessor processor = new PoolProcessor();
      processor.init (positions, op);
      processor.setMaxOperations (maxOps);
      processor.start();
      processor.waitForCompletion();
    } // else

  } // perform

  ////////////////////////////////////////////////////////////

  /**
   * Searches for a variable in a dataset using a set of search terms.
   * 
   * @param reader the data reader to search for the variable.
   * @param searchTerms the search terms to use.
   * @param minScore the minimum acceptable matching score in the range [0..1].
   * 
   * @return the variable name with the best match for the search terms, or 
   * null if no variables could be found.  Match quality is measured based on how
   * similar the variable name or its long name are to one of to search terms.
   */
  private static String findVariable (
    EarthDataReader reader,
    List<String> searchTerms,
    double minScore
  ) throws IOException {

    var variableList = reader.getAllGrids();
    double highScore = 0;
    String match = null;
    for (var name : variableList) {

      var variable = reader.getPreview (name);

      for (var term : searchTerms) {
        double score = MetadataServices.similarity (term, name);
        var longName = variable.getLongName();
        if (longName != null && !longName.isEmpty())
          score = Math.max (MetadataServices.similarity (term, longName), score);
        if (score > highScore) {
          highScore = score;
          match = name;
        } // if
      } // for

    } // for

    if (highScore < minScore) match = null;

    if (match != null) LOGGER.fine ("Found variable " + match + " with score " + highScore + " (minimum " + minScore + ")");

    return (match);

  } // findVariable

  ////////////////////////////////////////////////////////////
  
  private static void usage () { System.out.println (getUsage()); }
  
  ////////////////////////////////////////////////////////////
  
  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {
    
    UsageInfo info = new UsageInfo ("cwtccorrect");
    
    info.func ("Corrects top-of-atmosphere true color reflectance data");

    info.param ("input", "Input data file");
    info.param ("[output]", "Output data file");

    info.section ("General");

    info.option ("-h, --help", "Show help message");
    info.option ("-b, --bands=R/G/B", "Set red/green/blue band variables");
    info.option ("-e, --esun=R/G/B", "Set red/green/blue band solar irradiance values");
    info.option ("-n, --nameext=TEXT", "Extend band variable names with specified text");
    info.option ("-p, --props=FILE", "Set properties file for sensor parameters");
    info.option ("-s, --sensor=NAME", "Set sensor name");
    info.option ("--threads=MAX", "Set maximum number of parallel threads");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    info.section ("Angle");

    info.option ("-l, --latitude=VAR", "Set latitude variable name");
    info.option ("-L, --longitude=VAR", "Set longitude variable name");
    info.option ("-u, --sunzen=VAR", "Set solar zenith variable name");
    info.option ("-U, --sunazi=VAR", "Set solar azimuth variable name");
    info.option ("-a, --satzen=VAR", "Set satellite zenith variable name");
    info.option ("-A, --satazi=VAR", "Set satellite azimuth variable name");
    info.option ("-r, --relazi=VAR", "Set relative azimuth variable name");

    return (info);
    
  } // usage

  ////////////////////////////////////////////////////////////

  private cwtccorrect () { }

  ////////////////////////////////////////////////////////////

} // cwtccorrect
