////////////////////////////////////////////////////////////////////////
/*

     File: hdatt.java
   Author: Peter Hollemans
     Date: 2005/04/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import hdf.hdflib.HDFConstants;
import noaa.coastwatch.io.HDFLib;
import noaa.coastwatch.io.HDFReader;
import noaa.coastwatch.io.HDFWriter;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.MetadataServices;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The attribute tool reads and writes HDF file attributes.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->          
 *   hdatt - reads or writes HDF file attributes.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>hdatt [OPTIONS] input</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help<br>
 * -n, --name=STRING<br>
 * -t, --type=TYPE<br>
 * -l, --value=STRING1[/STRING2/...]<br>
 * -V, --variable=STRING<br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The attribute tool reads or writes HDF file attributes using the
 * HDF Scientific Data Sets (SDS) interface.  The two modes work as
 * follows:</p>
 * <dl>
 *
 *   <dt>Read mode</dt>
 *
 *   <dd>In read mode, the tool can read from either the global
 *   attribute set (the default), or the attribute set specific to a
 *   variable (when then <b>--variable</b> option is specified).  It
 *   can read either all attribute values in the set (the default), or
 *   just a single attribute value (when the <b>--name</b> option is
 *   specified).</dd>
 * 
 *   <dt>Write mode</dt>
 *
 *   <dd>Write mode is specified by the use of the <b>--value</b>
 *   option, which provides a value for a named attribute. In write
 *   mode, the user is required to supply an attribute name and value,
 *   and optionally a type.  If no type is specified, the type
 *   defaults to 'string' (see the <b>--type</b> option below for the
 *   meanings of various type names).  Attributes may be written to
 *   the global attribute set (the default), or to specific variables
 *   in the data file using the <b>--variable</b> option.</dd>
 *
 * </dl>
 *
 * <p><b>Note:</b> The attribute tool is currently limited to reading
 * and writing only the signed HDF data types.  In read mode, unsigned
 * HDF attribute data are read correctly, but the value displayed as if
 * it were signed.</p>
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
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -n, --name=STRING </dt>
 *   <dd> The name of the attribute to read or write. </dd>
 *
 *   <dt> -t, --type=TYPE </dt>
 *   <dd> The attribute data type (write mode only).  The valid types and
 *   their HDF equivalents are as follows:
 *
 *   <table style="border-spacing: 50px 2px">
 *
 *     <caption>Attribute Types</caption>
 *
 *     <tr>
 *       <th abbr="Type name">Type name</th>
 *       <th abbr="DFNT_FLOAT64">HDF type</th>
 *     </tr>
 *     <tr>
 *       <td>string</td>
 *       <td>DFNT_CHAR8</td>
 *     </tr>
 *     <tr>
 *       <td>byte</td>
 *       <td>DFNT_INT8</td>
 *     </tr>
 *     <tr>
 *       <td>short</td>
 *       <td>DFNT_INT16</td>
 *     </tr>
 *     <tr>
 *       <td>int</td>
 *       <td>DFNT_INT32</td>
 *     </tr>
 *     <tr>
 *       <td>long</td>
 *       <td>DFNT_INT64</td>
 *     </tr>
 *     <tr>
 *       <td>float</td>
 *       <td>DFNT_FLOAT32</td>
 *     </tr>
 *     <tr>
 *       <td>double</td>
 *       <td>DFNT_FLOAT64</td>
 *     </tr>
 *   </table></dd>
 *
 *   <dt> -l, --value=STRING1[/STRING2/...] </dt>
 *   <dd> The value(s) for the named attribute.  If specified, this
 *   places the tool into write mode, and <b>--name</b> must specify
 *   an attribute name.  If an attribute already exists, its value is
 *   overwritten with the new value.  If an attribute with the name
 *   does not exist, it is created and the new value assigned to it.
 *   By default if this option is not used, the tool is in read
 *   mode. </dd>
 * 
 *   <dt> -V, --variable=STRING </dt>
 *   <dd> The variable to read or write the attribute data.  By
 *   default, the attribute is read from or written to the global
 *   attribute set.</dd>
 * 
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 *
 * <p>0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Invalid variable name </li>
 *   <li> Invalid attribute name in read mode </li>
 *   <li> Invalid attribute type in write mode </li>
 *   <li> Value does not convert to the specified attribute data type </li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>As an example of read mode, the following command reads and prints
 * all the global attribute data from a CoastWatch HDF file:</p>
 * <pre>
 *   phollema$ hdatt 2005_095_1522_n17_er.hdf
 *
 *   satellite = noaa-17
 *   sensor = avhrr
 *   origin = USDOC/NOAA/NESDIS CoastWatch
 *   cwhdf_version = 3.2
 *   pass_type = day
 *   pass_date = 12878
 *   start_time = 55371.0
 *   projection_type = mapped
 *   projection = Mercator
 *   gctp_sys = 5
 *   gctp_zone = 0
 *   gctp_parm = 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0
 *   gctp_datum = 12
 *   et_affine = 0.0 -1470.0 1470.0 0.0 -8804259.100925786 5723777.271647277
 *   rows = 1401
 *   cols = 1302
 *   polygon_latitude = 45.83810150571052 45.83810150571052 45.83810150571052 
 *     45.83810150571052 45.83810150571052 42.51315402540104 38.99999999998719 
 *     35.30179546333813 31.424886223357582 31.424886223357582 31.424886223357582 
 *     31.424886223357582 31.424886223357582 35.30179546333813 38.99999999998719 
 *     42.51315402540104 45.83810150571052
 *   polygon_longitude = -79.09000515710031 -74.79500257855015 -70.5 -66.20499742144985 
 *     -61.90999484289969 -61.90999484289969 -61.90999484289969 -61.90999484289969 
 *     -61.90999484289969 -66.20499742144985 -70.5 -74.79500257855015 -79.09000515710031 
 *     -79.09000515710031 -79.09000515710031 -79.09000515710031 -79.09000515710031
 *   history = cwimport product.tshdf product.hdf
 * </pre>
 * <p>To dump only a single attribute:</p>
 * <pre>
 *   phollema$ hdatt --name satellite 2005_095_1522_n17_er.hdf
 *
 *   noaa-17
 * </pre>
 * <p>or a single attribute from a specific variable:</p>
 * <pre>
 *   phollema$ hdatt --name units --variable avhrr_ch3a 2005_095_1522_n17_er.hdf
 *
 *   albedo*100%
 * </pre>
 * 
 * <p>As an example of write mode, suppose that we wanted to save the
 * date when the file was originally downloaded from the server:</p>
 * <pre>
 *   phollema$ hdatt --name download_date --value "Mon Apr 11 18:20:15 PDT 2005" 
 *     2005_095_1522_n17_er.hdf
 * </pre>
 * <p>Now suppose we wanted to assign an integer quality value of 65% to the file
 * based on some test that was performed on the file data:</p>
 * <pre>
 *   phollema$ hdatt --name quality_value --value 65 --type int 2005_095_1522_n17_er.hdf
 * </pre>
 * <p>Finally, suppose that we wanted to change the units and scaling
 * factor / offset of a variable, originally in degrees Celsius and
 * scaled by 0.01, to degrees Fahrenheit:</p>
 * <pre>
 *   phollema$ hdatt --name units --value "deg F" --variable sst 2005_095_1522_n17_er.hdf
 *   phollema$ hdatt --name scale_factor --value 0.018 --type double --variable sst 
 *     2005_095_1522_n17_er.hdf
 *   phollema$ hdatt --name add_offset --value -1777.777777 --type double --variable sst 
 *     2005_095_1522_n17_er.hdf
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
 public final class hdatt {

  private static final String PROG = hdatt.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

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
    Option nameOpt = cmd.addStringOption ('n', "name");
    Option valueOpt = cmd.addStringOption ('l', "value");
    Option typeOpt = cmd.addStringOption ('t', "type");
    Option variableOpt = cmd.addStringOption ('V', "variable");
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

    // Set defaults
    // ------------
    String name = (String) cmd.getOptionValue (nameOpt);
    String value = (String) cmd.getOptionValue (valueOpt);
    String type = (String) cmd.getOptionValue (typeOpt);
    if (type == null) type = "string";
    String variable = (String) cmd.getOptionValue (variableOpt);

    // Check name/value
    // ----------------
    if (name == null && value != null) {
      LOGGER.severe ("Cannot specify attribute value or type without name");
      ToolServices.exitWithCode (2);
      return;
    } // if

    int sdid = -1, sdsid = -1;

    try {

      // Open file
      // ---------
      int mode = (value == null ? HDFConstants.DFACC_READ : HDFConstants.DFACC_WRITE);
      sdid = HDFLib.getInstance().SDstart (input, mode);

      // Set target to variable
      // ----------------------
      int targetid;
      if (variable != null) {
        int index = HDFLib.getInstance().SDnametoindex (sdid, variable);
        if (index < 0) {
          LOGGER.severe ("Cannot access variable '" + variable + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if
        sdsid = HDFLib.getInstance().SDselect (sdid, index);
        if (sdsid < 0) {
          LOGGER.severe ("Cannot access variable at index " + index);
          ToolServices.exitWithCode (2);
          return;
        } // if
        targetid = sdsid;
      } // if    

      // Set target to global
      // --------------------
      else {
        targetid = sdid;
      } // else

      // Read attribute value
      // --------------------
      if (value == null) {

        // Print single value
        // ------------------
        if (name != null) {
          Object attValue = HDFReader.getAttribute (targetid, name);
          System.out.println (MetadataServices.toString (attValue));
        } // if

        // Print all values
        // ----------------
        else {
          Map attMap = new LinkedHashMap();
          HDFReader.getAttributes (targetid, attMap, (variable == null));
          for (Iterator iter = attMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            System.out.print (entry.getKey() + " = ") ;
            System.out.println (MetadataServices.toString (entry.getValue()));
          } // for
        } // else

      } // if

      // Write attribute value
      // ---------------------
      else {

        // Write string
        // ------------
        if (type.equals ("string")) {
          HDFWriter.setAttribute (targetid, name, value);
        } // if

        // Write numerical types
        // ---------------------
        else {



          // We have to convert the values provided on the command line here.
          // Unsigned values are supported by parsing as the value as if it's
          // the next highest type and then stuffing the binary data into the
          // appropriate type. 

          String[] valueArray = value.split (ToolServices.SPLIT_REGEX);


/*

          int values = valueArray.length;
          Object valueObj = null;
          var unsigned = type.startsWith ("u");
          if (unsigned) type = type.substring (1);

          if (type.equals ("byte")) {
            var array = new byte[values];
            for (int i = 0; i < values; i++) {
              array[i] = (
                unsigned ? 
                (byte) (Short.parseShort (valueArray[i]) & 0xff) : 
                Byte.parseByte (valueArray[i])
              );
            } // for
            valueObj = array;
          } // else if

          else if (type.equals ("short")) {
            var array = new short[values];
            for (int i = 0; i < values; i++) {
              array[i] = (
                unsigned ? 
                (short) (Integer.parseInteger (valueArray[i]) & 0xffff) : 
                Short.parseShort (valueArray[i])
              );
            } // for
            valueObj = array;
          } // else if

          else if (type.equals ("int")) {
            var array = new int[values];
            for (int i = 0; i < values; i++) {
              array[i] = (
                unsigned ? 
                (int) (Long.parseLong (valueArray[i]) & 0xffffffff) : 
                Integer.parseInt (valueArray[i])
              );
            } // for
            valueObj = array;
          } // else if

          else if (type.equals ("long")) {
            var array = new int[values];



            // TODO: Shoud we somehow use BigDecimal here?


            for (int i = 0; i < values; i++) {
              array[i] = (
                unsigned ? 
                (int) (Long.parseLong (valueArray[i]) & 0xffffffff) : 
                Long.parseLong (valueArray[i])
              );
            } // for
            valueObj = array;

          } // if



*/




          Class valuePrimitiveClass;
          Class valueClass;
          if (type.equals ("byte")) {
            valuePrimitiveClass = Byte.TYPE;
            valueClass = Byte.class;
          } // if
          else if (type.equals ("short")) {
            valuePrimitiveClass = Short.TYPE;
            valueClass = Short.class;
          } // else if
          else if (type.equals ("int")) {
            valuePrimitiveClass = Integer.TYPE;
            valueClass = Integer.class;
          } // else if
          else if (type.equals ("long")) {
            valuePrimitiveClass = Long.TYPE;
            valueClass = Long.class;
          } // else if
          else if (type.equals ("float")) {
            valuePrimitiveClass = Float.TYPE;
            valueClass = Float.class;
          } // else if
          else if (type.equals ("double")) {
            valuePrimitiveClass = Double.TYPE;
            valueClass = Double.class;
          } // else if
          else {
            LOGGER.severe ("Unsupported type '" + type + "'");
            ToolServices.exitWithCode (2);
            return;
          } // else
          Object valueObj = Array.newInstance (valuePrimitiveClass, valueArray.length);
          Constructor cons = valueClass.getConstructor (new Class[] {String.class});
          for (int i = 0; i < valueArray.length; i++) {
            Object thisValue = cons.newInstance (new Object[] {valueArray[i]});
            Array.set (valueObj, i, thisValue);
          } // for
          HDFWriter.setAttribute (targetid, name, valueObj);
        } // else

      } // else

      // Close file
      // ----------
      if (variable != null) {
        HDFLib.getInstance().SDendaccess (sdsid);
        sdsid = -1;
      } // if
      HDFLib.getInstance().SDend (sdid);
      sdid = -1;

    } // try

    catch (Exception e) {
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try {
        if (sdsid != -1) HDFLib.getInstance().SDendaccess (sdsid);
        if (sdid != -1) HDFLib.getInstance().SDend (sdid);
      } // try
      catch (Exception e) { LOGGER.log (Level.SEVERE, "Error closing resources", e); }
    } // finally

    ToolServices.finishExecution (PROG);

  } // main

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("hdatt");

    info.func ("Reads or writes HDF file attributes");

    info.param ("input", "Input data file name");

    info.option ("-h, --help", "Show help message");
    info.option ("-n, --name=STRING", "Name of attribute to read or write");
    info.option ("-t, --type=TYPE", "Set attribute value type for writing");
    info.option ("-l, --value=STRING1[/STRING2/...]", "Value of attribute to write");
    info.option ("-V, --variable=STRING", "Set variable to which attribute belongs");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private hdatt () { }

  ////////////////////////////////////////////////////////////

} // hdatt

////////////////////////////////////////////////////////////////////////
