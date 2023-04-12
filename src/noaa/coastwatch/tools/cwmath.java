////////////////////////////////////////////////////////////////////////
/*

     File: cwmath.java
   Author: Peter Hollemans
     Date: 2003/04/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.logging.Level;

import noaa.coastwatch.io.CWHDFReader;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.HDFCachedGrid;
import noaa.coastwatch.io.tile.TilingScheme;

import noaa.coastwatch.tools.CleanupHook;
import noaa.coastwatch.tools.ToolServices;

import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;

import noaa.coastwatch.util.expression.ExpressionParserFactory;
import noaa.coastwatch.util.expression.ExpressionParserFactory.ParserStyle;
import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ExpressionParser.ResultType;
import noaa.coastwatch.util.expression.ParseImp;
import noaa.coastwatch.util.expression.EvaluateImp;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkCollector;
import noaa.coastwatch.util.chunk.ChunkProducer;
import noaa.coastwatch.util.chunk.ChunkConsumer;
import noaa.coastwatch.util.chunk.GridChunkProducer;
import noaa.coastwatch.util.chunk.GridChunkConsumer;
import noaa.coastwatch.util.chunk.ChunkingScheme;
import noaa.coastwatch.util.chunk.ChunkComputation;
import noaa.coastwatch.util.chunk.ChunkPosition;
import noaa.coastwatch.util.chunk.ExpressionFunction;
import noaa.coastwatch.util.chunk.PoolProcessor;
import noaa.coastwatch.util.chunk.PackingScheme;
import noaa.coastwatch.util.chunk.DoublePackingScheme;
import noaa.coastwatch.util.chunk.DataChunkFactory;

/**
 * <p>The math tool combines earth data using a mathematical expression.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwmath - combines earth data using a mathematical expression.
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
 * -c, --scale=FACTOR/OFFSET | none <br>
 * -e, --expr=EXPRESSION <br>
 * -h, --help <br>
 * -k, --skip-missing <br>
 * -l, --longname=STRING <br>
 * -m, --missing=VALUE <br>
 * -p, --parser=TYPE <br>
 * -s, --size=TYPE <br>
 * --serial <br>
 * --threads=MAX <br>
 * -t, --template=VARIABLE <br>
 * -f, --full-template <br>
 * -u, --units=STRING <br>
 * -v, --verbose <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p> The math tool combines earth data using a mathematical
 * expression.  The expression takes the form:</p>
 * <pre>
 *   variable = formula
 * </pre>
 * <p>where the variable is the output variable to create and the formula
 * is a mathematical combination of input variables.  The formula may
 * contain a number of standard operators, for example addition and
 * subtraction, as well as functions such as sine and cosine and
 * numerical and symbolic constants.  The formula syntax can follow one of
 * two standards specified by the <b>--parser</b> option: the
 * legacy syntax as originally supported by the math tool,
 * or Java syntax that takes advantage of the full java.lang.Math function
 * library and in some cases allows for simpler expressions.  See
 * the <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/Math.html">Java API documentation</a>
 * for a full list of java.lang.Math functions.  A comparison of
 * the most common language features is as follows:</p>
 *
 * <table style="border-spacing: 50px 2px">
 *
 *   <caption>Parser Expressions</caption>
 *
 *   <tr>
 *     <th abbr="Inverse hyperbolic tangent">Language Feature</th>
 *     <th abbr="Legacy Parser Syntax">Legacy Parser Syntax</th>
 *     <th abbr="((flag &amp; bitmask) == 0 ? x : NaN)">Java Parser Syntax</th>
 *   </tr>
 *
 *   <tr>
 *     <td>Unary plus</td>
 *     <td>+x</td>
 *     <td>Unsupported</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Unary minus</td>
 *     <td>-x</td>
 *     <td>-x</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Addition</td>
 *     <td>x + y</td>
 *     <td>x + y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Subtraction</td>
 *     <td>x - y</td>
 *     <td>x - y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Multiplication</td>
 *     <td>x * y</td>
 *     <td>x * y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Division</td>
 *     <td>x / y</td>
 *     <td>x / y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Modulus</td>
 *     <td>x % y or mod (x, y)</td>
 *     <td>x % y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Exponentiation</td>
 *     <td>x ^ y</td>
 *     <td>pow (x, y)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise AND</td>
 *     <td>and (x, y)</td>
 *     <td>x &amp; y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise OR</td>
 *     <td>or (x, y)</td>
 *     <td>x | y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise XOR</td>
 *     <td>xor (x, y)</td>
 *     <td>x ^ y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Bitwise NOT</td>
 *     <td>not (x)</td>
 *     <td>~x</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Less than</td>
 *     <td>x &lt; y</td>
 *     <td>x &lt; y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Greater than</td>
 *     <td>x &gt; y</td>
 *     <td>x &gt; y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Less than or equal to</td>
 *     <td>x &lt;= y</td>
 *     <td>x &lt;= y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Greater than or equal to</td>
 *     <td>x &gt;= y</td>
 *     <td>x &gt;= y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Not equal to</td>
 *     <td>x != y</td>
 *     <td>x != y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Equal to</td>
 *     <td>x == y</td>
 *     <td>x == y</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Boolean conjunction</td>
 *     <td>x &amp;&amp; y</td>
 *     <td>x &amp;&amp; y</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Boolean disjunction</td>
 *     <td>x || y</td>
 *     <td>x || y</td>
 *   </tr>
 *
 *    <tr>
 *      <td>Boolean negation</td>
 *      <td>!x</td>
 *      <td>!x</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Sine</td>
 *     <td>sin (x)</td>
 *     <td>sin (x)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Cosine</td>
 *     <td>cos (x)</td>
 *     <td>cos (x)</td>
 *   </tr>
 *
 *    <tr> 
 *     <td>Tangent</td>
 *     <td>tan (x)</td>
 *     <td>tan (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arcsine</td>
 *     <td>asin (x)</td>
 *     <td>asin (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arccosine</td>
 *     <td>acos (x)</td>
 *     <td>acos (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Arctangent</td>
 *     <td>atan (x)</td>
 *     <td>atan (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic sine</td>
 *     <td>sinh (x)</td>
 *     <td>sinh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic cosine</td>
 *     <td>cosh (x)</td>
 *     <td>cosh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Hyperbolic tangent</td>
 *     <td>tanh (x)</td>
 *     <td>tanh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse hyperbolic sine</td>
 *     <td>asinh (x)</td>
 *     <td>asinh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse hyperbolic cosine</td>
 *     <td>acosh (x)</td>
 *     <td>acosh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Inverse hyperbolic tangent</td>
 *     <td>atanh (x)</td>
 *     <td>atanh (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Natural log</td>
 *     <td>ln (x)</td>
 *     <td>log (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Base 10 log</td>
 *     <td>log (x)</td>
 *     <td>log10 (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Polar coordinate angle</td>
 *     <td>angle (y, x)</td>
 *     <td>atan2 (y, x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Absolute value</td>
 *     <td>abs (x)</td>
 *     <td>abs (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Random value [0..1]</td>
 *     <td>rand()</td>
 *     <td>random()</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Square root</td>
 *     <td>sqrt (x)</td>
 *     <td>sqrt (x)</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Sum of values</td>
 *     <td>sum (x1, x2, ...)</td>
 *     <td>sum (x1, x2, ...)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Conditional operator</td>
 *     <td>select (condition, x, y)</td>
 *     <td>(condition ? x : y)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Hexadecimal constant</td>
 *     <td>hex ("0xffff")</td>
 *     <td>0xffff</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Data value masking</td>
 *     <td>mask (x, flag, bitmask)</td>
 *     <td>((flag &amp; bitmask) == 0 ? x : NaN)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Value of e (2.71828...)</td>
 *     <td>e</td>
 *     <td>E</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Value Pi (3.14159...)</td>
 *     <td>pi</td>
 *     <td>PI</td>
 *   </tr>
 *
 *   <tr> 
 *     <td>Not-a-Number as 64-bit</td>
 *     <td>nan</td>
 *     <td>NaN</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Test for Not-a-Number</td>
 *     <td>Unsupported</td>
 *     <td>isNaN (x)</td>
 *   </tr>
 *
 *   <tr>
 *     <td>Haversine distance (km)</td>
 *     <td>Unsupported</td>
 *     <td>dist (lat1, lon1, lat2, lon2)</td>
 *   </tr>
 *
 * </table>
 *
 * <p>Note that in legacy parser expressions, boolean result values from
 * operators (==, !=, &gt;, &lt;, &gt;=, &lt;=, &amp;&amp;, ||, !)
 * evaluated to either 1.0 (true) or 0.0 (false).  As a result, legacy parser
 * expressions could treat boolean values as numbers in arithmatic expressions
 * such as addition, subtraction, etc.  This is not the case with the Java
 * parser, and even when emulating the legacy parser, arithmatic operations
 * on boolean values generate a parsing error.  The only solution for this is
 * to modify the expression.  For example:</p>
 * <pre>
 *   result = (var1 != 0) + (var2 != 0)
 * </pre>
 * <p>should be modified to:</p>
 * <pre>
 *   result = (var1 != 0 ? 1 : 0) + (var2 != 0 ? 1 : 0)
 * </pre>
 * <p>and the new Java parser used (see the <b>--parser</b> option below).</p>
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
 *   a compatible earth transform and the new variable data will be
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
 *   <dt> -c, --scale=FACTOR/OFFSET | none </dt>
 *   <dd> The output variable scale and offset.  The scaling is effectively a
 *   packing scheme to reduce data file size by scaling floating-point values
 *   to integers using the equation:
 *   <pre>
 *     integer = value/factor + offset
 *   </pre>
 *   The default is '0.01/0'.  If 'none' is specified, the expression result
 *   is considered to be an integer and stored directly in the output value.
 *   This option is ignored for floating-point storage types, namely
 *   if <b>--size</b> is 'float' or 'double' or if the template variable
 *   type is floating-point. </dd>
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
 *   <dt> -k, --skip-missing </dt>
 *   <dd> Turns on skip missing mode.  When on, the computation skips any
 *   locations where one or more variable values are set to the missing value.
 *   This is an optimization for when the expression does not contain any
 *   tests for NaN values, and data values flagged as missing would lead to
 *   in an invalid computation result. </dd>
 *
 *   <dt> -l, --longname=STRING </dt>
 *   <dd> The output variable long name.  The long name is a verbose
 *   string to describe the variable in common terms.  For example, the
 *   variable named 'sst' might have the long name 'sea surface
 *   temperature'.  The default is to use the output variable name as
 *   the long name. </dd>
 *
 *   <dt> -m, --missing=VALUE </dt>
 *   <dd> The missing output value.  The missing value is used to mark
 *   output locations in the variable where there is no valid value, either
 *   because the computation of the expression failed, or no value was ever
 *   or should ever be written to the location (possibly, it falls outside
 *   some geographic domain).  By default, the missing value is type-dependent
 *   and is set to the minimum representable value for integer types.  For
 *   floating point types, this option is ignored and the missing value is
 *   set to the NaN value or inherited from the template variable.</dd>
 *
 *   <dt> -p, --parser=TYPE </dt>
 *   <dd> The expression parser type.  Valid choices are 'java' or 'emulated':
 *   <ul>
 *
 *     <li> <b>Java (DEFAULT)</b> - The Java expression parser accepts expressions written
 *     in the Java Language Specification, with access to the full java.lang.Math
 *     class static methods, in addition to the constants E, PI, NaN, and the
 *     functions isNaN(x), asinh(x), acosh(x), atanh(x), and sum(x1,x2,...).
 *     The Java parser compiles expressions to Java byte code and runs the code
 *     natively for high speed expression evaluation.  The Java parser is
 *     more strict with type checking than the legacy or emulated legacy
 *     parsers.  For example, operands of the bitwise operators must be an
 *     integer type (or cast to an integer type) in order to pass the parsing
 *     phase without a type error.</li>
 *
 *     <li> <b>Legacy emulated</b> - The emulated expression parser emulates
 *     the legacy parser (the original parser used in the math tool) by internally
 *     converting the legacy expression syntax, operators, functions, and
 *     constants to Java Language Specification.  The converted expression is
 *     then parsed and evaluated by the high speed Java expression parser.</li>
 *
 *   </ul></dd>
 *
 *   <dt> -s, --size=TYPE </dt>
 *   <dd> The output variable type.  Valid choices include integer data in
 *   both signed and unsigned types, and floating-point data as follows:
 *   <ul>
 *
 *     <li> 8-bit byte: 'byte' or 'ubyte' </li>
 *
 *     <li> 16-bit short integer: 'short' or 'ushort' </li>
 *
 *     <li> 32-bit integer: 'int' or 'uint' </li>
 *
 *     <li> 64-bit long integer: 'long' or 'ulong' </li>
 *
 *     <li> 32-bit floating-point: 'float' </li>
 *
 *     <li> 64-bit floating-point: 'double' </li>
 *
 *   </ul>
 *   Integer output types can be used to represent floating-point values by
 *   specifying the <b>--scale</b> option. The default variable type is 'short',
 *   and with the default scaling factor of 0.01, floating-point values are
 *   packed into 16-bit signed integers with a range of [-327.68 ... 327.67] and
 *   two decimals of accuracy. </dd>
 *
 *   <dt>--serial</dt>
 *
 *   <dd>Turns on serial processing mode.  By default the program will
 *   use multiple processors in parallel to process chunks of data.</dd>
 *
 *   <dt>--threads=MAX</dt>
 *
 *   <dd>Specifies the maximum number of threads for parallel processing. By
 *   default the program will automatically detect the maximum number of
 *   threads possible.</dd>
 *
 *   <dt> -t, --template=VARIABLE </dt>
 *   <dd> The output template variable.  When a template is used, the
 *   output variable size, scaling, units, long name, and missing
 *   value are all determined from the template variable.  Any of
 *   these properties set from the command line using <b>--size</b>,
 *   <b>--scale</b>, <b>--units</b>, <b>--longname</b> or <b>--missing</b>
 *   override the corresponding template property.  There is no template
 *   variable by default, rather the math routine outputs 16-bit scaled
 *   integer values.  See the <b>--size</b> and <b>--scale</b> options
 *   for details.</dd>
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
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input or output file names </li>
 *   <li> Unsupported input file format </li>
 *   <li> Invalid mathematical expression </li>
 *   <li> Output variable already exists in input file </li>
 *   <li> Invalid scale or size specified </li>
 *   <li> Unsupported variable rank detected </li>
 *   <li> Invalid expression variable name </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the correction of AVHRR channel 2
 * data for solar zenith angle.  The output variable is named
 * 'avhrr_ch2_corr' and is written to the input file:</p>
 * <pre>
 *   phollema$ cwmath -v --units percent --longname "AVHRR channel 2 corrected"
 *     --expr "avhrr_ch2_corr = avhrr_ch2 / cos (toRadians (sun_zenith))"
 *     2019_250_2241_n19_mr.hdf
 *
 *   [INFO] Opening input/output 2019_250_2241_n19_mr.hdf
 *   [INFO] Creating avhrr_ch2_corr variable
 *   [INFO] Total grid size is 1101x1401
 *   [INFO] Found 8 processor(s) to use
 *   [INFO] Processing 9 data chunks of size 512x512
 * </pre>
 * <p>Another example below shows the computation of Normalized
 * Difference Vegetation Index (NDVI):</p>
 * <pre>
 *   phollema$ cwmath -v --longname "Normalized Difference Vegetation Index"
 *     --expr "ndvi = (avhrr_ch2 - avhrr_ch1)/(avhrr_ch2 + avhrr_ch1)" 
 *     2019_015_2121_n19_er.hdf
 *
 *   [INFO] Opening input/output 2019_015_2121_n19_er.hdf
 *   [INFO] Creating ndvi variable
 *   [INFO] Total grid size is 1401x1302
 *   [INFO] Found 8 processor(s) to use
 *   [INFO] Processing 9 data chunks of size 512x512
 * </pre>
 * <p>To show how to mark certain data values in a variable as invalid,
 * the example below masks the 'sst' variable using data from the 'cloud'
 * variable:</p>
 * <pre>
 *   phollema$ cwmath -v --template sst
 *     --expr 'sst_masked = ((cloud &amp; 0x6f) == 0 ? sst : NaN)'
 *     2019_015_2121_n19_er.hdf
 *
 *   [INFO] Opening input/output 2019_015_2121_n19_er.hdf
 *   [INFO] Creating sst_masked variable
 *   [INFO] Total grid size is 1401x1302
 *   [INFO] Found 8 processor(s) to use
 *   [INFO] Processing 9 data chunks of size 512x512
 * </pre>
 * <p>We use a hexadecimal value to set which bits from the 8-bit cloud mask
 * to use for masking.  In this case the value '0x6f' tests the cloud mask
 * bits 1, 2, 3, 4, 6, and 7 -- to use all cloud mask bits, the value would be '0xff'.
 * The output value 'NaN' is a special number that marks the SST as invalid
 * where the cloud mask has detected cloud (ie: at least one of the cloud mask bits
 * specified in the mask is set to 1).  The conditional operator is
 * also used: (condition ? x : y) means 'if condition is true, the value of x,
 * otherwise the value of y'.</p>
 *
 * <p>A final example below shows how the tool may be used to compute
 * complex formulas using a Unix Bourne shell script.  The example
 * computes the theoretical AVHRR channel 3b albedo at night for
 * NOAA-17 using actual channel 3b temperatures and channel 3b
 * emission temperatures estimated from channel 4 and 5:</p>
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
 *   rad3="(($planck_c1*pow ($w3, 3)) / (pow (E, ($planck_c2*$w3)/($c3b_a + $c3b_b*$t3)) - 1.0))"
 *   rad3e="(($planck_c1*pow ($w3, 3)) / (pow (E, ($planck_c2*$w3)/($c3b_a + $c3b_b*$t3e)) - 1.0))"
 *   alb3="(100*(1 - $rad3/$rad3e))"
 *   cwmath -v --longname "AVHRR channel 3 albedo" --units "percent" \
 *     --expr "avhrr_ch3_albedo=$alb3" $input
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.4
 */
public final class cwmath {

  private static final String PROG = cwmath.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 1;

  // Variables
  // ---------

  /**
   * The list of new variable names mapped to old ones.  We have this because
   * some variables in input files can contain characters like "-" that would
   * be parsed as a subtraction.  So we allow these variables in the input
   * expression, but convert them before parsing.
   */
  private static HashMap<String,String> newNameMap = new HashMap<String,String>();

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

    // Get actual variable name in file
    // --------------------------------
    if (newNameMap.containsKey (exprVarName))
      exprVarName = newNameMap.get (exprVarName);

    // Parse expression variable name
    // ------------------------------
    if (readers.length > 1) {

      // Check format
      // ------------
      if (!exprVarName.matches ("^file[1-9]+_.+$")) {
        throw new RuntimeException ("Invalid expression variable " + exprVarName);
      } // if
      
      // Get file index and variable name
      // --------------------------------
      fileIndex = Integer.parseInt (exprVarName.replaceFirst ("^file([1-9]+)_.+$", "$1")) - 1;
      fileVarName = exprVarName.replaceFirst ("^file[1-9]+_(.+)$", "$1");

    } // if

    // Copy simple variable name
    // -------------------------
    else {
      fileIndex = 0;
      fileVarName = exprVarName;
    } // else

    return (readers[fileIndex].getVariable (fileVarName));

  } // getInputVariable

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of allowed input variable names.  This method has the
   * side-effect of building a map of variable names with illegal characters
   * that are not in the set [a-zA-Z0-9_] with an underscore so that they
   * don't interfere with expression parsing.
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
    ListIterator<String> iter = varNames.listIterator();
    while (iter.hasNext()) {
      String name = iter.next();
      String newName = name.replaceAll ("[^0-9a-zA-Z_]", "_");
      if (!newName.equals (name)) {
        LOGGER.fine ("Replacing variable name " + name + " with " + newName + " for expressions");
        newNameMap.put (newName, name);
        iter.set (newName);
      } // if
    } // for

    return (varNames);

  } // getInputVariables

  ////////////////////////////////////////////////////////////

  /**
   * Implements a parser helper that retrieves data from an array of
   * data readers and adds the needed variables to a list of grids.
   */
  private static class ReaderParseImp implements ParseImp {
  
    /** The map of variable name to index. */
    private Map<String, Integer> variableMap = new HashMap<>();
  
    /** The array of readers to get data variables from. */
    private EarthDataReader[] readers;
    
    /** The list of chunk producers to build as variables are parsed. */
    private List<GridChunkProducer> chunkProducerList;
  
    public ReaderParseImp (
      EarthDataReader[] readers,
      List<GridChunkProducer> chunkProducerList
    ) {
    
      this.readers = readers;
      this.chunkProducerList = chunkProducerList;
    
    } // ReaderParseImp constructor

    @Override
    public int indexOfVariable (String varName) {

      Integer index = variableMap.get (varName);
      if (index == null) {
        DataVariable inputVar;
        try { inputVar = getInputVariable (readers, varName); }
        catch (Exception e) { inputVar = null; }
        if (inputVar == null)
          index = -1;
        else {
          if (inputVar.getRank() != 2)
            throw new RuntimeException ("Unsupported rank for variable " + varName);
          index = chunkProducerList.size();
          chunkProducerList.add (new GridChunkProducer ((Grid) inputVar));
          variableMap.put (varName, index);
        } // else
      } // if

      return (index);

    } // indexOfVariable

    @Override
    public String typeOfVariable (String varName) {

      Integer index = indexOfVariable (varName);
      String typeName;
      if (index == -1)
        typeName = null;
      else {
        GridChunkProducer producer = chunkProducerList.get (index);
        switch (producer.getExternalType()) {
        case BYTE: typeName = "Byte"; break;
        case SHORT: typeName = "Short"; break;
        case INT: typeName = "Integer"; break;
        case LONG: typeName = "Long"; break;
        case FLOAT: typeName = "Float"; break;
        case DOUBLE: typeName = "Double"; break;
        default: typeName = null;
        } // switch
      } // else
      
      return (typeName);

    } // typeOfVariable

  } // ReaderParseImp

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
    CmdLineParser cmd = new CmdLineParser();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option templateOpt = cmd.addStringOption ('t', "template");
    Option fulltemplateOpt = cmd.addBooleanOption ('f', "full-template");
    Option skipmissingOpt = cmd.addBooleanOption ('k', "skip-missing");
    Option sizeOpt = cmd.addStringOption ('s', "size");
    Option scaleOpt = cmd.addStringOption ('c', "scale");
    Option unitsOpt = cmd.addStringOption ('u', "units");
    Option longnameOpt = cmd.addStringOption ('l', "longname");
    Option exprOpt = cmd.addStringOption ('e', "expr");
    Option parserOpt = cmd.addStringOption ('p', "parser");
    Option missingOpt = cmd.addStringOption ('m', "missing");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option serialOpt = cmd.addBooleanOption ("serial");
    Option threadsOpt = cmd.addIntegerOption ("threads");
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
        LOGGER.severe ("Error reading expression");
        ToolServices.exitWithCode (2);
        return;
      } // if
    } // if

    // Get variable name and formula
    // -----------------------------
    String[] expressionArray = expression.split (" *= *", 2);
    if (expressionArray.length != 2) {
      String message;
      if (expressionArray.length == 1)
        message = "Missing equals sign in '" + expression + "'";
      else
        message = "Too many equals signs in '" + expression + "'";
      LOGGER.severe (message);
      ToolServices.exitWithCode (2);
      return;
    } // if
    String outputVarName = expressionArray[0];
    String outputExpression = expressionArray[1];

    // Set defaults
    // ------------
    boolean verbose = (cmd.getOptionValue (verboseOpt) != null);
    if (verbose) VERBOSE.setLevel (Level.INFO);
    String template = (String) cmd.getOptionValue (templateOpt);
    boolean fullTemplate = (cmd.getOptionValue (fulltemplateOpt) != null);
    boolean skipMissing = (cmd.getOptionValue (skipmissingOpt) != null);
    String size = (String) cmd.getOptionValue (sizeOpt);
    String scale = (String) cmd.getOptionValue (scaleOpt);
    String units = (String) cmd.getOptionValue (unitsOpt);
    String longName = (String) cmd.getOptionValue (longnameOpt);
    String parserType = (String) cmd.getOptionValue (parserOpt);
    boolean parserWasSet = false;
    if (parserType == null) {
      parserType = "java";
    } // if
    else {
      parserWasSet = true;
    } // else
    String missingStr = (String) cmd.getOptionValue (missingOpt);
    boolean serialOperations = (cmd.getOptionValue (serialOpt) != null);
    Integer threadsObj = (Integer) cmd.getOptionValue (threadsOpt);
    int threads = (threadsObj == null ? -1 : threadsObj.intValue());

    // We have the readers and writers declared outside the try statement so
    // we can close them later if there's an error that doesn't result in
    // exiting the Java VM.
    EarthDataReader[] readers = new EarthDataReader[input.length];
    CWHDFWriter writer = null;
    CWHDFReader outputReader = null;

    try {

      // Get output transform if output exists
      // -------------------------------------
      EarthTransform outputTransform = null;
      boolean singleFile = (input.length == 1 && input[0].equals (output));
      File outputFile = new File (output);
      if (!singleFile && outputFile.exists()) {
        VERBOSE.info ("Checking output " + output);
        outputReader = new CWHDFReader (output);
        outputTransform = outputReader.getInfo().getTransform();
        outputReader.close();
        outputReader = null;
      } // if

      // Create array of readers
      // -----------------------
      for (int i = 0; i < input.length; i++) {

        // Open file for both input and output
        // -----------------------------------
        if (input[i].equals (output)) {
          VERBOSE.info ("Opening input/output " + input[i]);
          writer = new CWHDFWriter (output);
          readers[i] = new CWHDFReader (writer);
        } // if

        // Open file for input only and check transform
        // --------------------------------------------
        else {
          VERBOSE.info ("Opening input " + input[i]);
          readers[i] = EarthDataReaderFactory.create (input[i]);
          if (outputTransform != null) {
            EarthTransform inputTransform = readers[i].getInfo().getTransform();
            if (!inputTransform.equals (outputTransform)) {
              LOGGER.severe ("Earth transforms do not match for " + input[i] + " and " + output);
              ToolServices.exitWithCode (2);
              return;
            } // if
          } // if
        } // else

      } // for

      // Open output file
      // ----------------
      if (writer == null) {
        if (outputFile.exists()) {
          VERBOSE.info ("Opening output " + output);
          writer = new CWHDFWriter (output);
        } // if
        else {
          VERBOSE.info ("Creating output " + output);
          CleanupHook.getInstance().scheduleDelete (output);
          writer = new CWHDFWriter (readers[0].getInfo(), output);
        } // else
      } // if

      // Get input variable names and modify expression
      // ----------------------------------------------
      List<String> nameList = getInputVariables (readers);
      nameList.forEach (name -> LOGGER.fine ("Found input variable " + name));
      for (String newName : newNameMap.keySet()) {
        String newExpression = outputExpression.replaceAll (newNameMap.get (newName), newName);
        if (!newExpression.equals (outputExpression))
          outputExpression = newExpression;
      } // for

      // Create parser
      // -------------
      List<GridChunkProducer> chunkProducerList = new ArrayList<>();
      ParseImp parseImp = new ReaderParseImp (readers, chunkProducerList);

      // Get parser style
      // ----------------
      ParserStyle parserStyle = null;
      if (parserType.equals ("emulated"))
        parserStyle = ParserStyle.LEGACY_EMULATED;
      else if (parserType.equals ("java"))
        parserStyle = ParserStyle.JAVA;
      else {
        LOGGER.severe ("Invalid parser type, " + parserType);
        ToolServices.exitWithCode (2);
        return;
      } // else

      // Parse expression
      // ----------------
      if (parserStyle == ParserStyle.LEGACY_EMULATED) {
        ExpressionParser emulationParser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.LEGACY_EMULATED);
        emulationParser.init (parseImp);
        outputExpression = emulationParser.translate (outputExpression);
        VERBOSE.info ("Using expression '" + outputExpression + "'");
      } // if
      ExpressionParser parser = ExpressionParserFactory.getFactoryInstance().create (ParserStyle.JAVA);
      parser.init (parseImp);
      try { parser.parse (outputExpression); }
      catch (RuntimeException e) {
        if (!parserWasSet)
          LOGGER.warning ("As of version 3.5.1, cwmath defaults to using the Java expression parser");
        throw (e);
      } // catch

      // Get template variable
      // ---------------------
      DataVariable templateVar = null;
      if (template != null) {
        templateVar = getInputVariable (readers, template);
      } // if

      // Get dimensions of output grid
      // -----------------------------
      int producers = chunkProducerList.size();
      if (producers == 0) {
        LOGGER.severe ("Expression contains no input variables");
        ToolServices.exitWithCode (2);
        return;
      }// if
      int[] dims = readers[0].getInfo().getTransform().getDimensions();
      
      // Get output data scaling
      // -----------------------
      double[] scaling = null;
      if (scale == null && templateVar != null)
        scaling = templateVar.getScaling();
      else {
        if (scale == null) scale = "0.01/0";
        if (!scale.equals ("none")) {
          String[] scaleArray = scale.split (ToolServices.SPLIT_REGEX);
          if (scaleArray.length != 2) {
            LOGGER.severe ("Invalid scale '" + scale + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if
          double factor = Double.parseDouble (scaleArray[0]);
          double offset = Double.parseDouble (scaleArray[1]);
          scaling = new double[] {factor, offset};
        } // if
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
          data = new byte[0];
          if (missingStr != null) {
            if (isUnsigned) missing = (byte) (Short.parseShort (missingStr) & 0xff);
            else missing = Byte.parseByte (missingStr);
          } // if
          else {
            if (isUnsigned) missing = Byte.valueOf ((byte) 0);
            else missing = Byte.valueOf (Byte.MIN_VALUE);
          } // else
          format = NumberFormat.getInstance();
          int digits = (scaling == null ? 0 :
            DataVariable.getDecimals (Double.toString (Byte.MAX_VALUE*scaling[0])));
          format.setMaximumFractionDigits (digits);
        } // else if

        else if (size.equals ("short") || size.equals ("ushort")) {
          data = new short[0];
          if (missingStr != null) {
            if (isUnsigned) missing = (short) (Integer.parseInt (missingStr) & 0xffff);
            else missing = Short.parseShort (missingStr);
          } // if
          else {
            if (isUnsigned) missing = Short.valueOf ((short) 0);
            else missing = Short.valueOf (Short.MIN_VALUE);
          } // else
          format = NumberFormat.getInstance();
          int digits = (scaling == null ? 0 :
            DataVariable.getDecimals (Double.toString (Short.MAX_VALUE*scaling[0])));
          format.setMaximumFractionDigits (digits);
        } // else if
        
        else if (size.equals ("int") || size.equals ("uint")) {
          data = new int[0];
          if (missingStr != null) {
            if (isUnsigned) missing = Integer.parseUnsignedInt (missingStr);
            else missing = Integer.parseInt (missingStr);
          } // if
          else {
            if (isUnsigned) missing = Integer.valueOf (0);
            else missing = Integer.valueOf (Integer.MIN_VALUE);
          } // else
          format = NumberFormat.getInstance();
          int digits = (scaling == null ? 0 :
            DataVariable.getDecimals (Double.toString (Integer.MAX_VALUE*scaling[0])));
          format.setMaximumFractionDigits (digits);
        } // else if

        else if (size.equals ("long") || size.equals ("ulong")) {
          data = new long[0];
          if (missingStr != null) {
            if (isUnsigned) missing = Long.parseUnsignedLong (missingStr);
            missing = Long.parseLong (missingStr);
          } // if
          else {
            if (isUnsigned) missing = Long.valueOf (0);
            else missing = Long.valueOf (Long.MIN_VALUE);
          } // else
          format = NumberFormat.getInstance();
          int digits = (scaling == null ? 0 :
            DataVariable.getDecimals (Double.toString (Long.MAX_VALUE*scaling[0])));
          format.setMaximumFractionDigits (digits);
        } // else if
        
        else if (size.equals ("float")) {
          scaling = null;
          data = new float[0];
          missing = Float.valueOf (Float.NaN);
          format = NumberFormat.getInstance();
          int digits = 6;
          format.setMaximumFractionDigits (digits);
        } // else if

        else if (size.equals ("double")) {
          scaling = null;
          data = new double[0];
          missing = Double.valueOf (Double.NaN);
          format = NumberFormat.getInstance();
          int digits = 10;
          format.setMaximumFractionDigits (digits);
        } // else if

        else {
          LOGGER.severe ("Invalid size '" + size + "'");
          ToolServices.exitWithCode (2);
          return;
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
      VERBOSE.info ("Creating " + outputVarName + " variable");
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
      Grid outputVar = new HDFCachedGrid (grid, writer);

      // Set up chunk producers
      // ----------------------
      ChunkCollector collector = new ChunkCollector();
      chunkProducerList.forEach (collector::addProducer);

      // Set up chunk consumer
      // ---------------------
      ChunkConsumer consumer = new GridChunkConsumer (outputVar);
      ChunkingScheme scheme = consumer.getNativeScheme();
      DataChunk prototypeChunk = consumer.getPrototypeChunk();

      // Check if we need to adapt parse output type
      // -------------------------------------------
      String resultType = parser.getResultType().toString().toLowerCase();
      String chunkType = prototypeChunk.getExternalType().toString().toLowerCase();
      if (!resultType.equals (chunkType)) {
        LOGGER.warning ("Casting " + resultType + " expression result to " + chunkType);
        parser.adapt (ResultType.valueOf (chunkType.toUpperCase()));
      } // if

      // Create chunk function
      // ---------------------
      ExpressionFunction function = new ExpressionFunction();
      function.setSkipMissing (skipMissing);
      function.init (parser, prototypeChunk);

      // Create chunk computation
      // ------------------------
      ChunkComputation op = new ChunkComputation (collector, consumer, function);
      //op.setTracked (true); // testing
      List<ChunkPosition> positions = new ArrayList<>();
      scheme.forEach (positions::add);

      // Perform chunk processing
      // ------------------------
      boolean isParallel = parser.isThreadSafe() && !serialOperations;
      int[] chunkingDims = scheme.getDims();
      VERBOSE.info ("Total grid size is " + chunkingDims[0] + "x" + chunkingDims[1]);

      int maxOps = 0;
      if (isParallel) {
        int processors = Runtime.getRuntime().availableProcessors();
        if (threads < 0) maxOps = processors;
        else maxOps = Math.min (threads, processors);
        if (maxOps < 1) maxOps = 1;
        VERBOSE.info ("Using " + maxOps + " parallel threads for processing");
      } // if

      int[] chunkSize = scheme.getChunkSize();
      VERBOSE.info ("Processing " + positions.size() +
        " data chunks of size " + chunkSize[0] + "x" + chunkSize[1]);

      if (isParallel) {
        PoolProcessor processor = new PoolProcessor();
        processor.init (positions, op);
        processor.setMaxOperations (maxOps);
        processor.start();
        processor.waitForCompletion();
      } // if
      else {
        positions.forEach (pos -> op.perform (pos));
      } // else

      // For testing
      //op.getTrackingData().forEach ((type, time) -> {
      //  System.out.println ("Total " + type + " time = " + time + "s");
      //});

      // Close files
      // -----------
      for (int i = 0; i < readers.length; i++) {
        readers[i].close();
        readers[i] = null;
      } // for
      writer.close();
      writer = null;
      CleanupHook.getInstance().cancelDelete (output);

    } // try

    catch (OutOfMemoryError | Exception e) {
      ToolServices.warnOutOfMemory (e);
      LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
      ToolServices.exitWithCode (2);
      return;
    } // catch

    finally {
      try {
        for (int i = 0; i < readers.length; i++) {
          if (readers[i] != null) readers[i].close();
        } // for
        if (writer != null) writer.close();
        if (outputReader != null) outputReader.close();
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

    UsageInfo info = new UsageInfo ("cwmath");

    info.func ("Combines earth data using a mathematical expression");

    info.param ("input", "Single data file for input and output", 1);
    info.param ("input1 [input2 ...]", "Input data file(s)", 2);
    info.param ("output", "Output data file", 2);

    info.option ("-c, --scale=FACTOR/OFFSET", "Set integer packing parameters");
    info.option ("-e, --expr=EXPRESSION", "Compute output using expression");
    info.option ("-h, --help", "Show help message");
    info.option ("-p, --parser=TYPE", "Set parser type for expression");
    info.option ("-k, --skip-missing", "Skip output for missing input values");
    info.option ("-l, --longname=STRING", "Set output variable long name");
    info.option ("-m, --missing=VALUE", "Set output variable missing value");
    info.option ("-s, --size=TYPE", "Set output variable binary type");
    info.option ("--serial", "Perform serial operations");
    info.option ("--threads=MAX", "Set maximum number of parallel threads");
    info.option ("-t, --template=VARIABLE", "Use template for output attributes");
    info.option ("-f, --full-template", "Use all template variable attributes");
    info.option ("-u, --units=STRING", "Set output variable units");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwmath () { }

  ////////////////////////////////////////////////////////////

} // cwmath class

////////////////////////////////////////////////////////////////////////
