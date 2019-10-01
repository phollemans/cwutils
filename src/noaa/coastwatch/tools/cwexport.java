////////////////////////////////////////////////////////////////////////
/*

     File: cwexport.java
   Author: Mark Robinson
     Date: 2002/07/07

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
package noaa.coastwatch.tools;

// Imports
// --------
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;
import java.io.IOException;

import noaa.coastwatch.io.ArcWriter;
import noaa.coastwatch.io.BinaryWriter;
import noaa.coastwatch.io.ByteWriter;
import noaa.coastwatch.io.CFNCWriter;
import noaa.coastwatch.io.CFNC4Writer;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.EarthDataWriter;
import noaa.coastwatch.io.FloatWriter;
import noaa.coastwatch.io.ShortWriter;
import noaa.coastwatch.io.TextWriter;
import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.io.GeoTIFFWriter;
import noaa.coastwatch.io.GeoTIFFDataWriter;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The export tool translates earth data into external file
 * formats.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->          
 *   cwexport - translates earth data into external file formats.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>cwexport [OPTIONS] input output</p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -f, --format=TYPE <br>
 * -h, --help <br>
 * -H, --header <br>
 * -m, --match=PATTERN <br>
 * -M, --missing=VALUE <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 * 
 * <h3>Binary raster options:</h3>
 * 
 * <p> 
 * -c, --scale=FACTOR/OFFSET <br>
 * -o, --byteorder=ORDER <br>
 * -r, --range=MIN/MAX <br>
 * -s, --size=TYPE <br>
 * </p>
 *
 * <h3>ASCII text options:</h3>
 * 
 * <p> 
 * -d, --dec=DECIMALS <br>
 * -D, --delimit=STRING <br>
 * -n, --nocoords <br>
 * -R, --reverse <br>
 * </p>
 *
 * <h3>NetCDF options:</h3>
 * 
 * <p> 
 * -S, --dcs <br>
 * -C, --cw <br>
 * </p>
 *
 * <h3>GeoTIFF options:</h3>
 *
 * <p>
 * -T, --tiffcomp=TYPE <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The export tool translates earth data into external formats as
 * described below.  In all cases, 2D data sets are exported in row
 * major order starting from row 0.  For example, if the earth
 * data values form the 2D array:</p>
 * <pre>
 *    0  1  2  3
 *    4  5  6  7
 *    8  9 10 11
 *   12 13 14 15
 * </pre>
 * <p>then values are output in the order:</p>
 * <pre>
 *   0 1 2 3 4 5 6 7 ...
 * </pre>
 *
 * <p>In the general case, multiple variables may be exported to the same
 * data file.  The use of the <b>--match</b> option may be used to
 * select a specific variable or subset of variables.</p>
 *
 * <h3>Binary raster:</h3>
 *
 * <p>
 * The output is a stream of binary data values &#8212; either 8-bit
 * unsigned bytes, 16-bit signed integers, or 32-bit IEEE floating
 * point values.  For 8- and 16-bit output, data values may be scaled
 * to integers (essentially a packing scheme to reduce the output file size)
 * using a minimum and maximum or by using a scaling
 * factor and offset.  For minimum/maximum scaling, integer data is
 * calculated from data values using the equation:</p>
 * <pre>
 *   integer = type_min + type_range*((value - min) / (max - min))
 * </pre>
 * <p>where <code>type_min</code> is 0 for 8-bit and -32768 for 16-bit, and
 * <code>type_range</code> is 255 for 8-bit and 65535 for 16-bit. For 
 * scaling factor and offset, the following equation is used:</p>
 * <pre>
 *   integer = value/factor + offset
 * </pre>
 * <p>In both cases, the results are rounded to the nearest integer and
 * out of range values are assigned the missing value.</p>
 *
 * <h3>ASCII text:</h3>
 *
 * <p>The output is an ASCII text file with latitude, longitude, and data 
 * value printed &#8212; one data value per line.</p>
 *
 * <h3>ArcGIS binary grid:</h3>
 * 
 * <p>The output is a stream of 32-bit IEEE floating point values,
 * ready for input to ArcGIS applications as a binary grid file.  A
 * header file may also be created to specify the earth location and
 * other parameters.  In the case of the ArcGIS format, only one variable
 * is allowed per binary grid file.  If an attempt to export multiple
 * variables is made, only the first variable is actually written.</p>
 *
 * <h3>NetCDF:</h3>
 * 
 * <p>The output is either a NetCDF 3 or 4 dataset with CF 1.4 convention
 * metadata.  The formatting follows as much as possible the
 * recommendations and examples in the document "Encoding
 * CoastWatch Satellite Data in NetCDF using the CF Metadata
 * Conventions", Peter Hollemans, February 2010.  In some cases,
 * the source data for some attributes may not be available, in
 * which case the output NetCDF may need to be modified and
 * extended.</p>
 *
 * <h3>GeoTIFF:</h3>
 *
 * <p>The output is a 32-bit floating point TIFF file with GeoTIFF
 * georeference tags.  The number of samples per pixel in the TIFF image
 * matches the number of variables exported.  The resulting image file
 * is not suitable for display as a regular TIFF image, but rather is
 * meant for import into a GIS package.</p>
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
 *   <dd> The output data file name.  Unless the <b>--format</b>
 *   option is used, the output file extension indicates the
 *   desired output format:
 *   <ul>
 *     <li>filename.raw = binary</li>
 *     <li>filename.txt = text</li>
 *     <li>filename.flt = ArcGIS</li>
 *     <li>filename.nc = NetCDF 3</li>
 *     <li>filename.nc4 = NetCDF 4</li>
 *     <li>filename.tif = GeoTIFF</li>
 *   </ul></dd>
 *
 * </dl>
 *
 * <h3>General options:</h3>
 *
 * <dl>
 *
 *   <dt> -f, --format=TYPE </dt>
 *   <dd> The output format.  The current formats are 'bin' for
 *   binary raster, 'text' for ASCII text, 'arc' for ArcGIS
 *   binary grid, 'netcdf' for NetCDF 3, 'netcdf4' for NetCDF 4,
 *   'geotiff' for GeoTIFF, or 'auto' to detect the format from the output file name.
 *   The default is 'auto'.</dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -H, --header </dt>
 *   <dd> Specifies that a header should be written with the
 *   output data.  The header is
 *   written before any data and is different depending on the
 *   output format: <ul>
 *
 *     <li>Binary: The header consists of one byte specifying the
 *     number of dimensions followed by a series of 32-bit signed
 *     integers specifying the dimension lengths.</li>
 *
 *     <li>Text: The header is one line consisting of an integer
 *     specifying the number of dimensions followed by a series of
 *     integers specifying the dimension lengths.</li>
 *
 *     <li>ArcGIS: The header is a separate file used by ArcGIS
 *     applications to determine the dimensions of the data, the
 *     geographic position and resolution, and other parameters.  The
 *     header file name is created by replacing any '.' followed by an
 *     extension in the output file name with '.hdr'</li>
 *
 *     <li>NetCDF, GeoTIFF: Not applicable, all metadata in NetCDF and
 *     GeoTIFF is written into the dataset itself.</li>
 *
 *   </ul>
 *   By default no header is written. </dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will be exported.  By
 *   default, no pattern matching is performed and all variables are
 *   exported. </dd>
 *
 *   <dt> -M, --missing=VALUE </dt>
 *   <dd> The output value for missing or out of range data.  The
 *   default missing value is different depending on the output format:
 *   <ul>
 *
 *     <li>Binary: The default is 0 for 8-bit unsigned bytes, -32768
 *     for 16-bit signed integers, and the IEEE NaN value for floating
 *     point.</li>
 *
 *     <li>Text: The default is to print 'NaN' for missing values.</li>
 *
 *     <li>ArcGIS: The missing value is fixed at -3.4e38 and the
 *     <b>--missing</b> option is ignored.</li>
 *
 *     <li>NetCDF: Not applicable, the missing values for
 *     variable data are copied from the input data source.</li>
 *
 *     <li>GeoTIFF: The default is to use write 32-float IEEE NaN values
 *     with bit pattern 0x7fc00000 in the TIFF image data.</li>
 *
 *   </ul></dd>
 *
 *   <dt> -v, --verbose </dt>
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
 * <h3>Binary raster options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --scale=FACTOR/OFFSET </dt>
 *   <dd> The data scale factor and offset for integer packing.  Data values
 *   are scaled to integers using the factor and offset (see the equation in the
 *   <b>Description</b> section above, under <b>Binary raster</b>).
 *   The default factor is 1 and offset is 0. </dd>
 *
 *   <dt> -o, --byteorder=ORDER </dt>
 *   <dd> The output byte order.  Valid choices are 'host' for the
 *   host byte order, 'msb' for most significant byte first, or 'lsb'
 *   for least significant byte first.  The default is the host
 *   byte order. </dd>
 *
 *   <dt> -r, --range=MIN/MAX </dt>
 *   <dd> The data scaling range for integer packing.  Data values are mapped to integers
 *   using the minimum and maximum values (see the equation in the
 *   <b>Description</b> section above, under <b>Binary raster</b>).
 *   There is no default range. </dd>
 *
 *   <dt> -s, --size=TYPE </dt>
 *   <dd> The binary value size.  Valid choices are 'byte' for 8-bit
 *   unsigned bytes, 'short' for 16-bit signed integers, or 'float'
 *   for 32-bit IEEE floating point values.  The default is 32-bit
 *   floats. </dd>
 *
 * </dl>
 *
 * <h3>ASCII text options:</h3>
 *
 * <dl>
 *
 *   <dt> -d, --dec=DECIMALS </dt>
 *   <dd> The number of decimal places for printed geographic
 *   coordinate values.  The default is 6 decimals. </dd>
 *
 *   <dt> -D, --delimit=STRING </dt>
 *   <dd> The value delimiter string.  By default, values are
 *   separated with a single space character. </dd>
 *
 *   <dt> -n, --nocoords </dt>
 *   <dd> Turns geographic coordinate printing off.  By default, each
 *   line has the form 'latitude longitude value' but with no
 *   coordinates, each line simply contains the data value. </dd>
 *
 *   <dt> -R, --reverse </dt>
 *   <dd> Specifies that coordinates should be printed in reverse
 *   order, 'longitude latitude'.  The default is 'latitude
 *   longitude'. </dd>
 *
 * </dl>
 *
 * <h3>NetCDF options:</h3>
 * 
 * <dl>
 *
 *   <dt> -S, --dcs </dt>
 *   <dd> Turns on writing of ocean color Data Content Standard
 *   metadata.  The default is to write only CF-1.4 metadata.
 *   DCS metadata is written as a set of global NetCDF attributes
 *   with the namespace prefix 'dcs'.  Only the minimal set of 15
 *   required attributes are written.  Since the NetCDF file will
 *   generally contain more than one variable, the required DCS
 *   attributes observedProperty and observedPropertyAlgorithm
 *   are set to 'Unknown' and must be modified manually.</dd>
 *
 *   <dt> -C, --cw </dt>
 *   <dd> Turns on writing of CoastWatch HDF-style metadata.  The
 *   default is to write only CF-1.4 metadata.  CoastWatch
 *   metadata is written as a set of global- and variable-level
 *   NetCDF attributes with the namespace prefix 'cw'.  Only a
 *   very small subset of the original CoastWatch HDF metadata is
 *   written, those attributes that have no CF-1.4
 *   equivalent.</dd>
 *
 * </dl>
 *
 * <h3>GeoTIFF options:</h3>
 *
 * <dl>
 *
 *   <dt>-T, --tiffcomp=TYPE</dt>
 *
 *   <dd>The TIFF compression algorithm.  The valid types are 'none'
 *   for no compression (the default), or 'deflate' for ZIP style
 *   compression.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Invalid variable name </li>
 *   <li> Unrecognized format, size, or byte order </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the export of AVHRR channel 1 data
 * from a CoastWatch HDF file with to a binary 32-bit IEEE floating
 * point value format, host byte order, no header, in verbose mode:</p>
 * <pre>
 *   phollema$ cwexport -v --match 'avhrr_ch1' 2019_250_2241_n19_mr.hdf
 *     2019_250_2241_n19_mr_ch1.raw
 *
 *   [INFO] Creating output 2019_250_2241_n19_mr_ch1.raw
 *   [INFO] Writing avhrr_ch1
 * </pre>
 * <p>The example below shows the export of AVHRR channels 1, 2, and
 * 4 to the same output file from a CoastWatch HDF file using 8-bit
 * unsigned byte format, no header, in verbose mode.  Range scaling is
 * used to scale all values between -30 and 30 prior to conversion to
 * byte values in the range 0 to 255.  Note that some values may fall
 * outside the range and be clipped, especially albedo values which
 * can range up to 100.  The clipped values are assigned the default
 * missing value, which for byte data is 0.</p>
 * <pre>
 *   phollema$ cwexport -v --match 'avhrr_ch[124]' --size byte --range -30/30
 *     2019_250_2241_n19_mr.hdf 2019_250_2241_n19_mr_ch124.raw
 *
 *   [INFO] Creating output 2019_250_2241_n19_mr_ch124.raw
 *   [INFO] Writing avhrr_ch1
 *   [INFO] Writing avhrr_ch2
 *   [INFO] Writing avhrr_ch4
 * </pre>
 * <p>The next example shows the export of AVHRR channel 4 data to an
 * ASCII text file from a CoastWatch HDF file in verbose mode.  The
 * geographic coordinates are printed in the order (longitude, latitude),
 * and delimited with a comma character.  Any missing values
 * are denoted with the value -999.  A one line dimension header is
 * prepended to the dataset.</p>
 * <pre>
 *   phollema$ cwexport -v --match 'avhrr_ch4' --format text --reverse
 *     --delimit ',' --missing -999 --header 2019_250_2241_n19_mr.hdf
 *     2019_250_2241_n19_mr_ch4.txt
 *
 *   [INFO] Creating output 2019_250_2241_n19_mr_ch4.txt
 *   [INFO] Writing avhrr_ch4
 * </pre>
 * <p>The first few lines of the output file are as follows:</p>
 * <pre>
 *   2,1101,1401
 *   -98.243664,31.051575,31.48
 *   -98.230459,31.051575,31.59
 *   -98.217254,31.051575,31.59
 *   -98.204049,31.051575,31.59
 *   -98.190843,31.051575,32.03
 *   -98.177638,31.051575,32.03
 *   -98.164433,31.051575,32.47
 *   -98.151228,31.051575,32.25
 *   -98.138022,31.051575,32.58
 * </pre>
 * <p>The example below shows the export of AVHRR channel 2 data to
 * an ArcGIS binary grid file from a CoastWatch HDF file in verbose
 * mode.  The binary grid data is written to a '.flt' file and the
 * header data to a '.hdr' file.</p>
 * <pre>
 *   phollema$ cwexport -v --format arc --match 'avhrr_ch2' --header
 *     2019_250_2241_n19_mr.hdf 2019_250_2241_n19_mr_ch4.flt
 *
 *   [INFO] Creating output 2019_250_2241_n19_mr_ch4.flt
 *   [INFO] Writing avhrr_ch2
 * </pre>
 * <p>The header data is written to <b>2019_250_2241_n19_mr_ch4.hdr</b> as follows:</p>
 * <pre>
 *   nrows 1101
 *   ncols 1401
 *   xllcorner -1.0937169680601347E7
 *   yllcorner 1999676.93911416
 *   cellsize 1470.0
 *   nodata_value -3.4E38
 *   byteorder MSBFIRST
 *   nbits 32
 * </pre>
 * <p>A final example shows the export of SST and cloud data to a
 * NetCDF dataset:</p>
 * <pre>
 *   phollema$ cwexport -v --match '(sst|cloud)' 2010_040_1636_m02_wj.hdf 
 *     2010_040_1636_m02_wj.nc
 *
 *   [INFO] Creating output 2010_040_1636_m02_wj.nc
 *   [INFO] Writing cloud
 *   [INFO] Writing sst
 * </pre>
 * <p>Running the NetCDF software <code>ncdump -h</code> command shows the
 * file contents:</p>
 * <pre>
 *   netcdf 2010_040_1636_m02_wj {
 *   dimensions:
 *     time = 1 ;
 *     level = 1 ;
 *     row = 1024 ;
 *     column = 1024 ;
 *   variables:
 *     int coord_ref ;
 *       coord_ref:grid_mapping_name = "mercator" ;
 *       coord_ref:longitude_of_projection_origin = 0. ;
 *       coord_ref:standard_parallel = 0. ;
 *       coord_ref:false_easting = 0. ;
 *       coord_ref:false_northing = 0. ;
 *       coord_ref:semi_major_axis = 6378137. ;
 *       coord_ref:inverse_flattening = 298.257223653 ;
 *       coord_ref:longitude_of_prime_meridian = 0. ;
 *     double x(column) ;
 *       x:standard_name = "projection_x_coordinate" ;
 *       x:units = "m" ;
 *     double y(row) ;
 *       y:standard_name = "projection_y_coordinate" ;
 *       y:units = "m" ;
 *     double lat(row, column) ;
 *       lat:standard_name = "latitude" ;
 *       lat:units = "degrees_north" ;
 *     double lon(row, column) ;
 *       lon:standard_name = "longitude" ;
 *       lon:units = "degrees_east" ;
 *     double time(time) ;
 *       time:standard_name = "time" ;
 *       time:units = "seconds since 1970-01-01 00:00:00 UTC" ;
 *     double level(level) ;
 *       level:standard_name = "height" ;
 *       level:units = "m" ;
 *       level:positive = "up" ;
 *     byte cloud(time, level, row, column) ;
 *       cloud:missing = 0b ;
 *       cloud:valid_range = 0, 255 ;
 *       cloud:coordinates = "lat lon" ;
 *       cloud:cell_methods = "area: mean" ;
 *       cloud:grid_mapping = "coord_ref" ;
 *     short sst(time, level, row, column) ;
 *       sst:scale_factor = 0.01 ;
 *       sst:add_offset = -0. ;
 *       sst:missing = -32768s ;
 *       sst:units = "celsius" ;
 *       sst:coordinates = "lat lon" ;
 *       sst:cell_methods = "area: mean" ;
 *       sst:grid_mapping = "coord_ref" ;
 *       sst:source = "nonlinear_split_window linear_triple_window_modified" ;
 * 
 *   // global attributes:
 *     :Conventions = "CF-1.4" ;
 *     :source = "METOP2_AVHRR " ;
 *     :institution = "USDOC/NOAA/NESDIS CoastWatch" ;
 *     :history = "[2010-03-13 09:46:43 IST cwf-3.2.4-pre-build169 phollema] cwexport -v --match (sst|cloud) 2010_040_1636_m02_wj.hdf 2010_040_1636_m02_wj.nc" ;
 *   }
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public class cwexport {

  private static final String PROG = cwexport.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ------------
  
  /** Minimum required command line parameters. */
  private static final int NARGS = 2;

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
    Option formatOpt = cmd.addStringOption ('f', "format");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option sizeOpt = cmd.addStringOption ('s', "size");
    Option rangeOpt = cmd.addStringOption ('r', "range");
    Option scaleOpt = cmd.addStringOption ('c', "scale");
    Option missingOpt = cmd.addStringOption ('M', "missing");
    Option byteorderOpt = cmd.addStringOption ('o', "byteorder");
    Option headerOpt = cmd.addBooleanOption ('H', "header");
    Option decOpt = cmd.addIntegerOption ('d', "dec");
    Option nocoordsOpt = cmd.addBooleanOption ('n', "nocoords");
    Option reverseOpt = cmd.addBooleanOption ('R', "reverse");
    Option delimitOpt = cmd.addStringOption ('D', "delimit");
    Option dcsOpt = cmd.addBooleanOption ('S', "dcs");
    Option cwOpt = cmd.addBooleanOption ('C', "cw");
    Option tiffcompOpt = cmd.addStringOption ('T', "tiffcomp");
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
    String output = remain[1];

    // Detect output format
    // --------------------
    String format = (String) cmd.getOptionValue (formatOpt);
    if (format == null) format = "auto";
    if (format.equals ("auto")) {
      int index = output.lastIndexOf ('.');
      if (index == -1) {
        LOGGER.severe ("Cannot find output file extension and no format specified");
        ToolServices.exitWithCode (2);
        return;
      } // if
      String ext = output.substring (index+1);
      if (ext.equalsIgnoreCase ("raw"))
        format = "bin";
      else if (ext.equalsIgnoreCase ("txt"))
        format = "text";
      else if (ext.equalsIgnoreCase ("flt"))
        format = "arc";
      else if (ext.equalsIgnoreCase ("nc"))
        format = "netcdf";
      else if (ext.equalsIgnoreCase ("nc4"))
        format = "netcdf4";
      else if (ext.equalsIgnoreCase ("tif"))
        format = "geotiff";
      else {
        LOGGER.severe ("Cannot determine output format from extension '" + ext + "'");
        ToolServices.exitWithCode (2);
        return;
      } // else
    } // if

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    String match = (String) cmd.getOptionValue (matchOpt);
    String size = (String) cmd.getOptionValue (sizeOpt);
    if (size == null) size = "float";
    String range = (String) cmd.getOptionValue (rangeOpt);
    String scale = (String) cmd.getOptionValue (scaleOpt);
    String missingStr = (String) cmd.getOptionValue (missingOpt);
    String byteorder = (String) cmd.getOptionValue (byteorderOpt);
    if (byteorder == null) byteorder = "host";
    boolean header = (cmd.getOptionValue (headerOpt) != null);
    Integer decObj = (Integer) cmd.getOptionValue (decOpt);
    int dec = (decObj == null ? 6 : decObj.intValue());
    boolean nocoords = (cmd.getOptionValue (nocoordsOpt) != null);
    boolean reverse = (cmd.getOptionValue (reverseOpt) != null);
    boolean dcs =  (cmd.getOptionValue (dcsOpt) != null);
    boolean cw =  (cmd.getOptionValue (cwOpt) != null);
    String delimit = (String) cmd.getOptionValue (delimitOpt);
    if (delimit == null) delimit = " ";
    String tiffcomp = (String) cmd.getOptionValue (tiffcompOpt);
    if (tiffcomp == null) tiffcomp = "none";

    // Check range and scaling
    // -----------------------
    String[] rangeArray = (range != null ? 
      range.split (ToolServices.SPLIT_REGEX) : null);
    String[] scaleArray = (scale != null ? 
      scale.split (ToolServices.SPLIT_REGEX) : null);
    if (rangeArray != null && rangeArray.length != 2) {
      LOGGER.severe ("Invalid range '" + range + "'");
      ToolServices.exitWithCode (2);
      return;
    } // if
    if (scaleArray != null && scaleArray.length != 2) {
      LOGGER.severe ("Invalid scale '" + scale + "'");
      ToolServices.exitWithCode (2);
      return;
    } // if

    // Check missing
    // -------------
    Double missing = null;
    if (missingStr != null) {
      try { missing = Double.valueOf (missingStr); }
      catch (NumberFormatException e) {
        LOGGER.severe ("Invalid missing value '" + missingStr + "'");
        ToolServices.exitWithCode (2);
        return;
      } // catch
    } // if

    try {

      // Open input file
      // ---------------
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthDataInfo info = reader.getInfo();
   
      // Setup for output
      // ----------------
      EarthDataWriter writer = null;
      VERBOSE.info ("Creating output " + output);
      CleanupHook.getInstance().scheduleDelete (output);

      // Create binary writer
      // --------------------
      if (format.equals ("bin")) {

        // Create writer based on size
        // ---------------------------
        BinaryWriter binWriter = null;
        if (size.equals ("byte")) 
          binWriter = new ByteWriter (info, output);
        else if (size.equals ("short")) 
          binWriter = new ShortWriter (info, output);
        else if (size.equals ("float"))
          binWriter = new FloatWriter (info, output);
        else {
          LOGGER.severe ("Invalid size '" + size + "'");
          ToolServices.exitWithCode (2);
          return;
        } // else

        // Set scaling
        // -----------
        if (rangeArray != null) {
          double min = Double.parseDouble (rangeArray[0]);
          double max = Double.parseDouble (rangeArray[1]);
          binWriter.setRange (min, max);
        } // if
        else if (scaleArray != null) {
          double factor = Double.parseDouble (scaleArray[0]);
          double offset = Double.parseDouble (scaleArray[1]);
          binWriter.setScaling (new double[] {factor, offset});
        } // else if

        // Set missing value
        // -----------------
        binWriter.setMissing (missing);

        // Set byte order
        // --------------
        if (byteorder.equals ("host")) 
          binWriter.setOrder (BinaryWriter.HOST);
        else if (byteorder.equals ("lsb")) 
          binWriter.setOrder (BinaryWriter.LSB);
        else if (byteorder.equals ("msb")) 
          binWriter.setOrder (BinaryWriter.MSB);
        else {
          LOGGER.severe ("Invalid order '" + byteorder + "'");
          ToolServices.exitWithCode (2);
          return;
        } // else

        // Set header
        // ----------
        binWriter.setHeader (header);

        writer = binWriter;

      } // if

      // Create Arc writer
      // -----------------
      else if (format.equals ("arc")) {
        ArcWriter arcWriter = new ArcWriter (info, output);
        arcWriter.setHeader (header);
        writer = arcWriter;
      } // else if

      // Create text writer
      // ------------------
      else if (format.equals ("text")) {
        TextWriter textWriter = new TextWriter (info, output);
        textWriter.setDecimals (dec);
        textWriter.setReverse (reverse);
        textWriter.setDelimiter (delimit);
        textWriter.setCoords (!nocoords);
        textWriter.setMissing (missing);
        textWriter.setHeader (header);
        writer = textWriter;
      } // else if

      // Create NetCDF writer
      // --------------------
      else if (format.equals ("netcdf")) {
        CFNCWriter ncWriter = new CFNCWriter (info, output, dcs, cw);
        writer = ncWriter;
      } // else if

      // Create NetCDF-4 writer
      // ----------------------
      else if (format.equals ("netcdf4")) {
        CFNC4Writer nc4Writer = new CFNC4Writer (info, output, dcs, cw);
        writer = nc4Writer;
      } // else if

      // Create GeoTIFF 32-bit writer
      // ----------------------------
      else if (format.equals ("geotiff")) {

        // Determine compression
        // ---------------------
        int compress = -1;
        if (tiffcomp.equals ("none"))
          compress = GeoTIFFWriter.COMP_NONE;
        else if (tiffcomp.equals ("deflate"))
          compress = GeoTIFFWriter.COMP_DEFLATE;
        else {
          LOGGER.severe ("Unsupported TIFF compression: " + tiffcomp);
          ToolServices.exitWithCode (2);
          return;
        } // else

        GeoTIFFDataWriter tiffWriter = new GeoTIFFDataWriter (info, output, compress);
        if (missing != null) tiffWriter.setMissing (missing.floatValue());
        writer = tiffWriter;

      } // else if

      // Report invalid format
      // ---------------------
      else {
        LOGGER.severe ("Invalid format '" + format + "'");
        ToolServices.exitWithCode (2);
        return;
      } // else

      // Loop over each variable
      // -----------------------
      for (int i = 0; i < reader.getVariables(); i++) {

        // Check for name match
        // --------------------
        String varName = reader.getName(i);
        if (match != null && !varName.matches (match))
          continue;

        // Get variable and flush
        // ----------------------
        try {
          DataVariable var = reader.getVariable (i);
          VERBOSE.info ("Writing " + varName);
          writer.addVariable (var);
          writer.flush(); 
        } // try
        catch (IOException e) { 
          LOGGER.warning ("Error writing " + varName + ": " + e.getMessage() + ", skipping variable");
        } // catch

      } // for

      // Close files
      // -----------
      writer.close();
      reader.close();
      CleanupHook.getInstance().cancelDelete (output);

    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", e);
      ToolServices.exitWithCode (2);
      return;
    } // catch

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  private static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwexport");

    info.func ("Translates earth data into external formats");

    info.param ("input", "Input data file");
    info.param ("output", "Output data file");

    info.section ("General");
    info.option ("-f, --format=TYPE", "Set output format");
    info.option ("-h, --help", "Show help message");
    info.option ("-H, --header", "Write header before data");
    info.option ("-m, --match=PATTERN", "Write only variables matching regular expression");
    info.option ("-M, --missing=VALUE", "Set value for missing data");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    info.section ("Binary raster");
    info.option ("-c, --scale=FACTOR/OFFSET", "Set scale-based integer packing parameters");
    info.option ("-o, --byteorder=ORDER", "Set byte order for multi-byte output");
    info.option ("-r, --range=MIN/MAX", "Set range-based integer packing parameters");
    info.option ("-s, --size=TYPE", "Set binary output value size");

    info.section ("ASCII text");
    info.option ("-d, --dec=DECIMALS", "Set geographic coordinate decimal accuracy");
    info.option ("-D, --delimit=STRING", "Set data column delimiter");
    info.option ("-n, --nocoords", "Do not output geographic coordinates");
    info.option ("-R, --reverse", "Reverse geographic coordinate order");

    info.section ("NetCDF");
    info.option ("-S, --dcs", "Write global DCS metadata");
    info.option ("-C, --cw", "Write CoastWatch metadata");

    info.section ("GeoTIFF");
    info.option ("-T, --tiffcomp=TYPE", "Set TIFF compression type");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwexport () { }

  ////////////////////////////////////////////////////////////

} // cwexport

////////////////////////////////////////////////////////////////////////
