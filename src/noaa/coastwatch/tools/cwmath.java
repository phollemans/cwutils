////////////////////////////////////////////////////////////////////////
/*
     FILE: cwmath.java
  PURPOSE: To combine Earth data variables using an expression.
   AUTHOR: Peter Hollemans
     DATE: 2003/04/20
  CHANGES: 2003/05/22, PFH, added bitwise or/and functions
           2004/01/23, PFH, modified to use SPLIT_REGEX and updated docs
           2004/02/16, PFH, updated documentation, added --template option
           2004/09/22, PFH, modified to allow existing output file
           2004/09/28, PFH, modified to use ToolServices.setCommandLine()
           2004/11/16, PFH, modified to allow multiple input files
           2005/01/30, PFH, modified to use CleanupHook class
           2005/03/15, PFH
           - reformatted documentation and usage note
           - modified to use no scaling for float output
           2005/05/05, PFH, modified to not read full template data array
           2005/06/08, PFH, updated units strings
           2006/06/05, PFH, added ubyte and ushort types
           2006/06/06, PFH, added xor, not functions
           2006/07/10, PFH, moved JEP creation to ExpressionParserFactory
           2007/04/19, PFH, added version printing
           2008/03/04, PFH, added special code for symbols in variable names
           2008/04/03, PFH, fixed problem with nan, pi, e constants
           2009/06/26, PFH, added extra attribute copying for template variables
          
  CoastWatch Software Library and Utilities
  Copyright 1998-2009, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// --------
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import jargs.gnu.*;
import jargs.gnu.CmdLineParser.*;
import org.nfunk.jep.*;

/**
 * The math tool combines Earth data using a mathematical expression.<p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwmath - combines Earth data using a mathematical expression.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>
 *   cwmath [OPTIONS] input <br>
 *   cwmath [OPTIONS] input1 [input2 ...] output
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --scale=FACTOR/OFFSET <br>
 * -e, --expr=EXPRESSION <br>
 * -h, --help <br>
 * -l, --longname=STRING <br>
 * -s, --size=TYPE <br>
 * -t, --template=VARIABLE <br>
 * -u, --units=STRING <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The math tool combines Earth data using a mathematical
 * expression.  The expression takes the form:
 * <pre>
 *   variable = formula
 * </pre>
 * where the variable is the output variable to create and the formula
 * is a mathematical combination of input variables.  The formula may
 * contain a number of standard operators, for example addition and
 * subtraction, as well as functions such as sine and cosine and
 * numerical and symbolic constants.  The supported operators and
 * functions are as follows:</p>
 *
 * <p><table>
 *
 *   <tr>
 *     <th>Operator</th>
 *     <th>Symbol</th>
 *   </tr>
 *
 *    <tr> 
 *      <td>Power</td>
 *      <td>^</td>
 *    </tr>
 *
 *    <tr> 
 *      <td>Boolean Not</td>
 *      <td>!</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Unary Plus, Unary Minus</td>
 *     <td>+x, -x</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Modulus</td>
 *     <td>%</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Division</td>
 *     <td>/</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Multiplication</td>
 *     <td>*</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Addition, Subtraction</td>
 *     <td>+, -</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Less or Equal, More or Equal</td>
 *     <td>&lt;=, &gt;=</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Less Than, Greater Than</td>
 *     <td>&lt;, &gt;</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Not Equal, Equal</td>
 *     <td>!=, ==</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Boolean And</td>
 *     <td>&amp;&amp;</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Boolean Or</td>
 *     <td>||</td>
 *   </tr>
 *
 * </table></p>
 *
 * <p><table>
 *
 *   <tr> 
 *     <th>Function</th>
 *     <th>Calling sequence</th>
 *   </tr>
 *
 *   <tr> 
 *     <td>Sine</td>
 *     <td>sin (x)</td>
 *   </tr> 
 *
 *   <tr>
 *     <td>Cosine</td>
 *     <td>cos (x)</td>
 *   </tr>
 *
 *    <tr> 
 *     <td>Tangent</td>
 *     <td>tan (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arc Sine</td>
 *     <td>asin (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arc Cosine</td>
 *     <td>acos (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arc Tangent</td>
 *     <td>atan (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic Sine</td>
 *     <td>sinh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic Cosine</td>
 *     <td>cosh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic Tangent</td>
 *     <td>tanh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse Hyperbolic Sine</td>
 *     <td>asinh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse Hyperbolic Cosine</td>
 *     <td>acosh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse Hyperbolic Tangent</td>
 *     <td>atanh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Natural Logarithm</td>
 *     <td>ln (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Logarithm base 10</td>
 *     <td>log (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Angle</td>
 *     <td>angle (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Absolute Value / Magnitude</td>
 *     <td>abs (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Random number (between 0 and 1)</td>
 *     <td>rand ()</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Modulus</td>
 *     <td>mod (x)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Square Root</td>
 *     <td>sqrt (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Sum</td>
 *     <td>sum (x1, x2, ...)</td>
 *   </tr>
 *
 *   <tr valign=top>
 *     <td>if (condition is true) then return (x1)<br>
 *     else return (x2)</td>
 *     <td>select (condition, x1, x2)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Hexadecimal decoder</td>
 *     <td>hex (string)</td>
 *   </tr>
 *
 *   <tr valign=top>
 *     <td>if (b (BITWISE OR) mask == 0) then return (x)<br>
 *     else return (Not-a-Number)</td>
 *     <td>mask (x, b, mask)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise And</td>
 *     <td>and (x1, x2)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise Or</td>
 *     <td>or (x1, x2)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise Xor</td>
 *     <td>xor (x1, x2)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise Not</td>
 *     <td>not (x)</td>
 *   </tr>
 *
 * </table></p>
 *
 * <p><table>
 *
 *   <tr> 
 *     <th>Constant</th>
 *     <th>Value</th>
 *   </tr>
 *
 *   <tr> 
 *     <td>e</td>
 *     <td>2.7182818...</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>pi</td>
 *     <td>3.1415927...</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>nan (Not-a-Number)</td>
 *     <td>NaN</td>
 *   </tr>
 *
 * </table></p>
 *
 * <p>Note that boolean expressions are evaluated to be either 1 or 0
 * (true or false respectively).  
 * </p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input</dt>
 *   <dd>The single input and output data file.  In the case that a
 *   single input file is specified and no output file, data is read
 *   from and written to the same file.  The new variable created by
 *   the expression must not already exist in the input file.</dd>
 *
 *   <dt>input1 [input2...]</dt>
 *   <dd>The input data file name(s).  If multiple input files are
 *   specified, variables on the right hand side of the expression (or
 *   used in the <b>--template</b> option) must be prefixed by the
 *   string 'file&lt;N&gt;_' where &lt;N&gt; is replaced with the
 *   index of the input file which contains the variable and input
 *   file indexing starts at 1.  For example, to reference the
 *   variable 'avhrr_ch4' in the second input file, use
 *   'file2_avhrr_ch4' in the expression.</dd>
 *
 *   <dt>output</dt>
 *   <dd>The output data file name.  If specified and the file does not
 *   already exist, it will be created using metadata from the first
 *   input file.  If it does exist, it will be opened and checked for
 *   a compatible Earth transform and the new variable data will be
 *   added to the file.  The new variable created by the expression
 *   must not already exist in the output file.  The output file can
 *   be one of the input files if needed.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --scale=FACTOR/OFFSET </dt>
 *   <dd> The output variable scale and offset.  The scaling is used
 *   to store floating-point values as integers using the equation:
 *   <pre>
 *     integer = value/factor + offset
 *   </pre>
 *   The default is '0.01,0'.  This option is ignored if
 *   <b>--size</b> is 'float'. </dd>
 *
 *   <dt> -e, --expr=EXPRESSION </dt>
 *   <dd> The mathematical expression.  See above for the expression
 *   syntax and supported operators and functions.  If no expression
 *   is specified, the user will be prompted to enter an expression at
 *   the keyboard.  The latter method is recommended for operating
 *   systems such as Microsoft Windows in which the command line shell
 *   can mangle some expression characters such as the equals sign. </dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -l, --longname=STRING </dt>
 *   <dd> The output variable long name.  The long name is a verbose
 *   string to describe the variable in common terms.  For example, the
 *   variable named 'sst' might have the long name 'sea surface
 *   temperature'.  The default is to use the output variable name as
 *   the long name. </dd>
 *
 *   <dt> -s, --size=TYPE </dt>
 *   <dd> The output variable value size.  Valid choices are
 *   'byte' or 'ubyte' for 8-bit signed or unsigned bytes,
 *   'short' or 'ushort' for 16-bit signed or unsigned integers,
 *   and 'float' for 32-bit floating-point values with no
 *   scaling.  The default is 'short'. </dd>
 *
 *   <dt> -t, --template=VARIABLE </dt>
 *   <dd> The output template variable.  When a template is used, the
 *   output variable size, scaling, units, long name, and missing
 *   value are all determined from the template variable.  Any of
 *   these properties set from the command line override the
 *   corresponding template property.  There is no default template
 *   variable. </dd>
 *
 *   <dt> -f, --full-template </dt>
 *   <dd> Turns on full template attribute mode.  All attributes from 
 *   the template variable (except for those overridden at the command
 *   line) are copied to the output variable.  By default only the minimal
 *   set of attributes is written.  </dd>
 *
 *   <dt> -u, --units=STRING </dt>
 *   <dd> The output variable units.  For example if the output
 *   variable data is based on temperature in Celsius, the variable
 *   units might be 'celsius'.  There is no default units
 *   value. </dd>
 *
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  The current status of computation
 *   is printed periodically.  The default is to run
 *   quietly. </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, > 0 on failure.  Possible causes of errors:
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input or output file names. </li>
 *   <li> Unsupported input file format. </li>
 *   <li> Invalid mathematical expression. </li>
 *   <li> Output variable already exists in input file. </li>
 *   <li> Invalid scale or size specified. </li>
 *   <li> Unsupported variable rank detected. </li>
 *   <li> Invalid expression variable name. </li>
 * </ul> </p>
 *
 * <h2>Examples</h2>
 * <p> The following shows the correction of AVHRR channel 2
 * data for solar zenith angle.  The output variable is named
 * 'avhrr_ch2_corr' and is written to the input file:
 * <pre>
 *   phollema@localhost:<~/cwatch/satdata/hdf> cwmath -v --units 
 *     "percent" --longname "AVHRR channel 2 corrected" 
 *     --expr "avhrr_ch2_corr = avhrr_ch2/cos(sun_zenith*pi/180)" 
 *     2003_104_1513_n17_er.hdf
 *
 *   cwmath: Reading input 2003_104_1513_n17_er.hdf
 *   cwmath: Creating avhrr_ch2_corr variable
 *   cwmath: Computing row 0
 *   cwmath: Computing row 100
 *   cwmath: Computing row 200
 *   cwmath: Computing row 300
 *   ...
 * </pre>
 * Another example below shows the computation of Normalized
 * Difference Vegetation Index (NDVI):
 * <pre>
 *   phollema@localhost:<~/cwatch/satdata/hdf> cwmath -v --longname 
 *     "Normalized Difference Vegetation Index" 
 *     --expr "ndvi = (avhrr_ch2 - avhrr_ch1)/(avhrr_ch2 + avhrr_ch1)" 
 *     2003_104_1513_n17_er.hdf
 *
 *   cwmath: Reading input 2003_104_1513_n17_er.hdf
 *   cwmath: Creating ndvi variable
 *   cwmath: Computing row 0
 *   cwmath: Computing row 100
 *   cwmath: Computing row 200
 *   cwmath: Computing row 300
 *   ...
 * </pre>
 * In order to demonstrate the use of the 'mask' function, the example
 * below shows the masking of the 'sst' variable using the 'cloud'
 * variable.  Note that a hexadecimal value is used to determine which
 * values from the cloud mask are used in the masking procedure.
 * Since the cloud data is represented by 8-bit bytes, the hexadecimal
 * mask value need only specify two hexadecimal digits.  In this case,
 * the value '0x6f' represents bits 1, 2, 3, 4, 6, and 7 (for all
 * cloud mask bits, the value would be '0xff'):
 * <pre>
 *   phollema@localhost:<~/cwatch/satdata/hdf> cwmath -v --template sst
 *     --expr 'sst_masked = mask (sst, cloud, hex ("0x6f"))'
 *     2003_104_1513_n17_er.hdf
 *
 *   cwmath: Reading input 2003_104_1513_n17_er.hdf
 *   cwmath: Creating sst_masked variable
 *   cwmath: Computing row 0
 *   cwmath: Computing row 100
 *   cwmath: Computing row 200
 *   cwmath: Computing row 300
 *   ...
 * </pre>
 * A final example below shows how the tool may be used to compute
 * complex formulas using a Unix Bourne shell script.  The example
 * computes the theoretical AVHRR channel 3b albedo at night for
 * NOAA-17 using actual channel 3b temperatures and channel 3b
 * emission temperatures estimated from channel 4 and 5:
 * <pre>
 *   #!/bin/sh
 *   
 *   input=$1
 *   T3E_A=6.82947
 *   T3E_B=0.97232
 *   T3E_C=1.66366
 *   ZERO_C=273.15
 *   t3="(avhrr_ch3 + $ZERO_C)"
 *   t4="(avhrr_ch4 + $ZERO_C)"
 *   t5="(avhrr_ch5 + $ZERO_C)"
 *   t3e="($T3E_A + $T3E_B*$t4 + $T3E_C*($t4 - $t5))"
 *   planck_c1=1.1910427e-5
 *   planck_c2=1.4387752
 *   w3=2669.3554
 *   c3b_a=1.702380
 *   c3b_b=0.997378
 *   rad3="(($planck_c1*($w3^3)) / (e^(($planck_c2*$w3)/($c3b_a + $c3b_b*$t3)) - 1.0))"
 *   rad3e="(($planck_c1*($w3^3)) / (e^(($planck_c2*$w3)/($c3b_a + $c3b_b*$t3e)) - 1.0))"
 *   alb3="(100*(1 - $rad3/$rad3e))"
 *   cwmath -v --longname "AVHRR channel 3 albedo" --units "percent" \
 *     --expr "avhrr_ch3_albedo=$alb3" $input
 * </pre>
 * </p>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public final class cwmath {

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  /** Name of program. */
  private static final String PROG = "cwmath";

  // Variables
  // ---------

  /** The list of new variable names mapped to old ones. */
  private static HashMap<String,String> newNameMap = 
    new HashMap<String,String>();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the input variable from the list of readers.  The variable
   * name must conform to the pattern 'fileN_name' where N is the
   * reader number starting at 1, and name is the actual variable
   * name.
   * 
   * @param readers the array of readers to select the variable from.
   * @param exprVarName the variable name from the expression.
   *
   * @throws RuntimeException if the expression variable name does not
   * conform to the pattern 'fileN_name' and there are multiple
   * readers.
   * @throws IOException if an error occurred retrieving the input
   * variable from the reader.
   */
  private static DataVariable getInputVariable (
    EarthDataReader[] readers,
    String exprVarName
  ) throws IOException {

    int fileIndex;
    String fileVarName;

    // Parse expression variable name
    // ------------------------------
    if (readers.length > 1) {

      // Check format
      // ------------
      if (!exprVarName.matches ("^file[1-9]+_.+$")) {
        throw new RuntimeException ("Invalid expression variable name: " + 
          exprVarName);
      } // if
      
      // Get file index and variable name
      // --------------------------------
      fileIndex = Integer.parseInt (
        exprVarName.replaceFirst ("^file([1-9]+)_.+$", "$1")) - 1;
      fileVarName = exprVarName.replaceFirst ("^file[1-9]+_(.+)$", "$1");

    } // if

    // Copy simple variable name
    // -------------------------
    else {
      fileIndex = 0;
      fileVarName = exprVarName;
    } // else

    // Get variable
    // ------------
    if (newNameMap.containsKey (fileVarName))
      fileVarName = newNameMap.get (fileVarName);
    return (readers[fileIndex].getVariable (fileVarName));

  } // getInputVariable

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of allowed input variable names.
   * 
   * @param readers the array of readers to select the variable from.
   *
   * @return the list of available variables.
   *
   * @throws IOException if an error occurred retrieving the input
   * variable from the reader.
   */
  static private List<String> getInputVariables (
    EarthDataReader[] readers
  ) throws IOException {

    // Create variable names
    // ---------------------
    List<String> varNames = new ArrayList<String>();
    if (readers.length == 1) {
      varNames.addAll ((List<String>) readers[0].getAllVariables());
    } // if
    else {
      for (int i = 0; i < readers.length; i++) {
        List<String> nameList = (List<String>) readers[i].getAllVariables();
        String prefix = "file" + (i+1) + "_";
        for (String name : nameList) varNames.add (prefix + name);
      } // for
    } // else

    // Remove disallowed characters
    // ----------------------------
    for (ListIterator<String> iter = varNames.listIterator();iter.hasNext();) {
      String name = iter.next();
      String newName = name.replaceAll ("[^0-9a-zA-Z_]", "_");
      if (!newName.equals (name)) {
        newNameMap.put (newName, name);
        iter.set (newName);
      } // if
    } // for

    return (varNames);

  } // getInputVariables

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option templateOpt = cmd.addStringOption ('t', "template");
    Option fulltemplateOpt = cmd.addBooleanOption ('f', "full-template");
    Option sizeOpt = cmd.addStringOption ('s', "size");
    Option scaleOpt = cmd.addStringOption ('c', "scale");
    Option unitsOpt = cmd.addStringOption ('u', "units");
    Option longnameOpt = cmd.addStringOption ('l', "longname");
    Option exprOpt = cmd.addStringOption ('e', "expr");
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
    String[] input;
    String output;
    if (remain.length == 1) {
      input = new String[1];
      input[0] = remain[0];
      output = remain[0];
    } // if
    else {
      input = new String[remain.length-1];
      System.arraycopy (remain, 0, input, 0, input.length);
      output = remain[remain.length-1];
    } // else

    // Get expression string
    // ---------------------
    String expression = (String) cmd.getOptionValue (exprOpt);
    if (expression == null) {
      System.out.println ("Enter an expression to calculate:");
      System.out.print ("> ");
      BufferedReader in = new BufferedReader (
        new InputStreamReader (System.in));
      try { expression = in.readLine(); }
      catch (IOException e) { }
      if (expression == null || expression.equals ("")) {
        System.err.println (PROG + ": Error reading expression");
        System.exit (2);
      } // if
    } // if

    // Get variable name and formula
    // -----------------------------
    String[] expressionArray = expression.split (" *= *", 2);
    if (expressionArray.length != 2) {
      System.err.println (PROG + ": Invalid expression '" + expression + "'");
      System.exit (1);
    } // if
    String outputVarName = expressionArray[0];
    String outputExpression = expressionArray[1];

    /*

    // Parse expression
    // ----------------
    JEP parser = ExpressionParserFactory.getInstance();
    parser.parseExpression (outputExpression);
    if (parser.hasError()) {
      System.err.println (PROG + ": Error parsing expression: " + 
        parser.getErrorInfo());
      System.exit (1);
    } // if

    // Get list of variables
    // ---------------------
    HashSet keySet = new HashSet (parser.getSymbolTable().keySet());
    keySet.remove ("e");
    keySet.remove ("pi");
    keySet.remove ("nan");
    String[] inputVarNames = (String[]) keySet.toArray (new String[] {});

    */

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    String template = (String) cmd.getOptionValue (templateOpt);
    boolean fullTemplate = (cmd.getOptionValue (fulltemplateOpt) != null);
    String size = (String) cmd.getOptionValue (sizeOpt);
    String scale = (String) cmd.getOptionValue (scaleOpt);
    String units = (String) cmd.getOptionValue (unitsOpt);
    String longName = (String) cmd.getOptionValue (longnameOpt);

    try {

      // Get output transform if output exists
      // -------------------------------------
      EarthTransform outputTransform = null;
      boolean singleFile = (input.length == 1 && input[0].equals (output));
      File outputFile = new File (output);
      if (!singleFile && outputFile.exists()) {
        if (verbose) System.out.println (PROG + ": Checking output " + output);
        CWHDFReader outputReader = new CWHDFReader (output);
        outputTransform = outputReader.getInfo().getTransform();
        outputReader.close();
      } // if

      // Open input files
      // ----------------
      EarthDataReader[] readers = new EarthDataReader[input.length];
      CWHDFWriter writer = null;
      for (int i = 0; i < input.length; i++) {

        // Open file for input and output
        // ------------------------------
        if (input[i].equals (output)) {
          if (verbose) 
            System.out.println (PROG + ": Opening input/output " + input[i]);
          writer = new CWHDFWriter (output);
          readers[i] = new CWHDFReader (writer);
        } // if

        // Open file for input only and check transform
        // --------------------------------------------
        else {
          if (verbose) 
            System.out.println (PROG + ": Opening input " + input[i]);
          readers[i] = EarthDataReaderFactory.create (input[i]);
          if (outputTransform != null) {
            EarthTransform inputTransform = 
              readers[i].getInfo().getTransform();
            if (!inputTransform.equals (outputTransform)) {
              System.err.println (PROG + 
                ": Earth transforms do not match for " + input[i] + " and " + 
                output);
              System.exit (2);
            } // if
          } // if
        } // else

      } // for

      // Open output file
      // ----------------
      if (writer == null) {
        if (outputFile.exists()) {
          if (verbose) 
            System.out.println (PROG + ": Opening output " + output);
          writer = new CWHDFWriter (output);
        } // if
        else {
          if (verbose) 
            System.out.println (PROG + ": Creating output " + output);
          CleanupHook.getInstance().scheduleDelete (output);
          writer = new CWHDFWriter (readers[0].getInfo(), output);
        } // else
      } // if

      // Get input variable names and modify expression
      // ----------------------------------------------
      List<String> nameList = getInputVariables (readers);
      for (Map.Entry<String,String> entry : newNameMap.entrySet()) {
        String newOutputExpression = 
          outputExpression.replaceAll (entry.getValue(), entry.getKey());
        if (!newOutputExpression.equals (outputExpression))
          outputExpression = newOutputExpression;
      } // for

      // Parse expression
      // ----------------
      JEP parser = ExpressionParserFactory.getInstance();
      parser.setAllowUndeclared (false);
      for (String name : nameList) parser.addVariable (name, Double.NaN);
      parser.parseExpression (outputExpression);
      if (parser.hasError()) {
        System.err.println (PROG + ": Error parsing expression: " + 
          parser.getErrorInfo());
        System.exit (1);
      } // if

      // Get variable names in expression
      // --------------------------------
      List<String> symbolList = new ArrayList<String>();
      Stack<Node> nodeStack = new Stack<Node>();
      nodeStack.push (parser.getTopNode());
      while (!nodeStack.empty()) {
        Node node = nodeStack.pop();
        for (int i = 0; i < node.jjtGetNumChildren(); i++)
          nodeStack.push (node.jjtGetChild (i));
        if (node instanceof ASTVarNode) 
          symbolList.add (((ASTVarNode)node).getName());
      } // while
      symbolList.remove ("e");
      symbolList.remove ("pi");
      symbolList.remove ("nan");
      String[] inputVarNames = symbolList.toArray (new String[]{});

      // Access variables
      // ----------------
      DataVariable[] inputVars = new DataVariable[inputVarNames.length];
      for (int i = 0; i < inputVarNames.length; i++) {
        inputVars[i] = getInputVariable (readers, inputVarNames[i]);
        if (inputVars[i].getRank() != 2) {
          System.err.println (PROG + ": Unsupported rank for variable " + 
            inputVarNames[i]);
          System.exit (2);
        } // if
      } // for

      // Get template variable
      // ---------------------
      DataVariable templateVar = null;
      if (template != null) {
        templateVar = getInputVariable (readers, template);
      } // if

      // Get dimensions
      // --------------
      int[] dims = inputVars[0].getDimensions();

      // Get output data scaling
      // -----------------------
      double[] scaling;
      if (scale == null && templateVar != null)
        scaling = templateVar.getScaling();
      else {
        if (scale == null) scale = "0.01,0";
        String[] scaleArray = scale.split (ToolServices.SPLIT_REGEX);
        if (scaleArray.length != 2) {
          System.err.println (PROG + ": Invalid scale '" + scale + "'");
          System.exit (1);
        } // if
        double factor = Double.parseDouble (scaleArray[0]);
        double offset = Double.parseDouble (scaleArray[1]);
        scaling = new double[] {factor, offset};
      } // else

      // Get output data type and properties
      // -----------------------------------
      Object data = null;
      Object missing = null;
      NumberFormat format = null;
      boolean isUnsigned;
      if (size == null && templateVar != null) {
        data = Array.newInstance (templateVar.getDataClass(), 0);
        missing = templateVar.getMissing();
        format = templateVar.getFormat();
        isUnsigned = templateVar.getUnsigned();
      } // if
      else {
        if (size == null) size = "short";
        isUnsigned = size.startsWith ("u");
        if (size.equals ("byte") || size.equals ("ubyte")) {
          data = new byte[] {};
          if (isUnsigned) missing = new Byte ((byte) 0);
          else missing = new Byte (Byte.MIN_VALUE);
          format = NumberFormat.getInstance();
          int digits = DataVariable.getDecimals (
            Double.toString (Byte.MAX_VALUE*scaling[0]));
          format.setMaximumFractionDigits (digits);
        } // else if
        else if (size.equals ("short") || size.equals ("ushort")) {
          data = new short[] {};
          if (isUnsigned) missing = new Short ((short) 0);
          else missing = new Short (Short.MIN_VALUE);
          format = NumberFormat.getInstance();
          int digits = DataVariable.getDecimals (
            Double.toString (Short.MAX_VALUE*scaling[0]));
          format.setMaximumFractionDigits (digits);
        } // else if
        else if (size.equals ("float")) {
          scaling = null;
          data = new float[] {};
          missing = new Float (Float.NaN);
          format = NumberFormat.getInstance();
          int digits = 6;
          format.setMaximumFractionDigits (digits);
        } // else if
        else {
          System.err.println (PROG + ": Invalid size '" + size + "'");
          System.exit (2);
        } // else
      } // else

      // Get output data strings
      // -----------------------
      if (longName == null && templateVar != null)
        longName = templateVar.getLongName();
      else {
        if (longName == null) longName = outputVarName;
      } // else
      if (units == null && templateVar != null)
        units = templateVar.getUnits();

      // Create output variable
      // ----------------------
      if (verbose) {
        System.out.println (PROG + ": Creating " + outputVarName 
          + " variable");
      } // if
      Grid grid = new Grid (outputVarName, longName, units, dims[Grid.ROWS],
        dims[Grid.COLS], data, format, scaling, missing);
      grid.setUnsigned (isUnsigned);
      if (templateVar != null) {
        if (fullTemplate) {
          grid.getMetadataMap().putAll (templateVar.getMetadataMap());
        } // if
        else {
          Map templateMap = templateVar.getMetadataMap();
          Map gridMap = grid.getMetadataMap();
          if (templateMap.containsKey ("calibrated_nt")) 
            gridMap.put ("calibrated_nt", templateMap.get ("calibrated_nt"));
        } // else
      } // if
      DataVariable outputVar = new HDFCachedGrid (grid, writer);

      // Loop over each location
      // -----------------------
      DataLocation start = new DataLocation (dims.length);
      DataLocation end = new DataLocation (dims.length);
      for (int i = 0; i < dims.length; i++) end.set (i, dims[i]-1);
      int[] stride = new int[dims.length]; 
      Arrays.fill (stride, 1);
      DataLocation loc = (DataLocation) start.clone();
      do {

        // Print status
        // ------------
        if (verbose && (int) loc.get (Grid.COLS) == 0) {
          int row = (int) loc.get (Grid.ROWS);
          if (row%100 == 0) 
            System.out.println (PROG + ": Computing row " + row);
        } // if

        // Update variable values
        // ----------------------
        for (int i = 0; i < inputVars.length; i++)
          parser.addVariable (inputVarNames[i], inputVars[i].getValue (loc));

        // Compute expression value
        // ------------------------
        outputVar.setValue (loc, parser.getValue());

      } while (loc.increment (stride, start, end));

      // Close files
      // -----------
      for (int i = 0; i < readers.length; i++)
        readers[i].close();
      writer.close();
      CleanupHook.getInstance().cancelDelete (output);

    } // try
    catch (Exception e) {
      e.printStackTrace();
      System.exit (2);
    } // catch

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwmath [OPTIONS] input\n" +
"       cwmath [OPTIONS] input1 [input2 ...] output\n" +
"Combines Earth data using a mathematical expression.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The single input/output data file name.\n" +
"  input1 [input2 ...]        The input data file name(s)\n" +
"  output                     The output data file name.\n" + 
"\n" +
"Options:\n" +
"  -c, --scale=FACTOR/OFFSET  Set scale factor and offset for output\n" +
"                              variable integer data.\n" +
"  -e, --expr=EXPRESSION      Compute output variable using expression.\n" +
"  -h, --help                 Show this help message.\n" +
"  -l, --longname=STRING      Set long name of output variable.\n" +
"  -s, --size=TYPE            Set binary type of output variable.  TYPE\n" +
"                              may be 'byte', 'ubyte', 'short', 'ushort',\n" +
"                              or 'float'.\n" +
"  -t, --template=VARIABLE    Use attributes of template variable for\n" +
"                              output variable.\n" +
"  -f, --full-template        Copy full attribute set for template variable.\n" +
"  -u, --units=STRING         Set units of output variable.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwmath () { }

  ////////////////////////////////////////////////////////////

} // cwmath class

////////////////////////////////////////////////////////////////////////
