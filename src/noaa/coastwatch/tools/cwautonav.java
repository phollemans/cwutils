////////////////////////////////////////////////////////////////////////
/*
     FILE: cwautonav.java
  PURPOSE: To automatically add navigation corrections to data files.
   AUTHOR: Peter Hollemans
     DATE: 2005/02/08
  CHANGES: 2005/03/15, PFH, reformatted documentation and usage note
           2005/04/23, PFH, added ToolServices.setCommandLine()
           2005/05/10, PFH, added search, correlation, separation, fraction
             options
           2005/05/18, PFH, modified box centers for reader datum
           2006/10/25, PFH, fixed problem with lat/lon separator characters
           2007/04/20, PFH, added version printing
           2012/12/04, PFH, added call to canUpdateNavigation for reader

  CoastWatch Software Library and Utilities
  Copyright 1998-2012, USDOC/NOAA/NESDIS CoastWatch

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
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.NavigationOffsetEstimator;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * <p>The autonavigation tool automatically determines a navigation
 * correction based on Earth image data.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwautonav - automatically determines a navigation correction based on 
 *   Earth image data.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>cwautonav [OPTIONS] locations-file variable input</p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -c, --correlation=FACTOR <br>
 * -f, --fraction=FRACTION <br>
 * -h, --help <br>
 * -H, --height=PIXELS <br>
 * -m, --match=PATTERN <br>
 * -M, --minboxes=N <br>
 * -s, --search=LEVEL <br>
 * -S, --separation=DISTANCE <br>
 * -t, --test <br>
 * -v, --verbose <br>
 * -w, --width=PIXELS <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 * <p>The autonavigation tool automatically determines a navigation
 * correction based on Earth image data.  The algorithm is as
 * follows:</p>
 * <ul>
 *
 *   <li><b>Step 1</b> - The user supplies a number of boxes of
 *   coastal data to use for navigation.  The boxes are specified
 *   by the latitude and longitude of each box center in a text file
 *   separate from the Earth data file.  The box dimensions are
 *   controlled by command line options.</li>
 * 
 *   <li><b>Step 2</b> - Each box is run through an offset estimation
 *   algorithm.  The algorithm first attempts to separate the pixels
 *   within a given box into two classes: land and water.  If the
 *   classes are sufficiently separable, an image correlation is run
 *   by "shifting" the image data around to find the maximum
 *   correlation between land/water classes and a precomputed land
 *   mask database.</li>
 *
 *   <li><b>Step 3</b> - All navigation boxes with successful offset
 *   estimates are used to compute the mean offset for the entire
 *   input file.  All user-specified variables in the input file
 *   are then corrected with the mean offset.</li>
 * 
 * </ul>
 *
 * <p>Note that because of the autonavigation algorithm design, there
 * are a number of <b>limitations</b>:</p>
 * <ul>
 * 
 *   <li><b>Coastline features</b> - The algorithm relies partly on
 *   the user being able to specify navigation boxes containing
 *   "wiggly" coastline features such as peninsulas and bays.  A flat
 *   coastline can cause the algorithm to generate inaccurate offset
 *   estimates.</li>
 *
 *   <li><b>Distinct classes</b> - Image data in the navigation boxes
 *   must be separable into distinct land and water classes.  If the
 *   image data contains cloud, or if the land and water pixels do not
 *   differ significantly (too similar in visible or thermal
 *   radiance), then the class separation step will fail for some
 *   boxes.</li>
 *
 *   <li><b>Large areas</b> - The mean offset generated from the set
 *   of successful offset estimates in Step 3 may not model the actual
 *   navigation correction for data files that cover a large
 *   physical area.  If the offsets differ significantly for
 *   navigation boxes at a great distance from each other, then the
 *   user should treat a number of subsets of the data file
 *   separately.</li>
 *
 *   <li><b>Rotation or scaling</b> - An offset correction cannot
 *   model the actual navigation correction if the data requires a
 *   rotation or scale correction.</li>
 *
 * </ul>
 *
 * <p>Note that satellite channel data or channel-derived variables
 * should be corrected with navigation but GIS-derived variables such
 * as coastline and lat/lon grid graphics should not be corrected.
 * Applying a navigation correction simply establishes a mapping
 * between desired and actual data coordinates -- it does not change
 * the gridded data values themselves.  Once a data file has been
 * autonavigated successfully, other CoastWatch tools in this package
 * will take the correction into account when reading the data.</p>
 *
 * <p>See the <b>cwnavigate</b> tool in this package for details on
 * how to set a navigation correction manually, or to reset the
 * existing navigation.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> locations-file </dt>
 *   <dd> The file name containing a list of navigation box centers.
 *   The file must be a text file containing center points as latitude
 *   / longitude pairs, one line per pair, with values separated by
 *   spaces or tabs.  The points are specified in terms of Earth
 *   location latitude and longitude in the range [-90..90] and
 *   [-180..180]. </dd>
 *
 *   <dt> variable </dt>
 *   <dd> The variable name to use for image data. </dd>
 *
 *   <dt> input </dt>
 *   <dd> The input data file name.  The navigation corrections are
 *   applied to the input file in-situ.  For CoastWatch HDF files, the
 *   corrections are applied to individual variables.  For CoastWatch
 *   IMGMAP files, corrections are applied to the global attributes
 *   and the <b>--match</b> option has no effect.  No other file
 *   formats are supported.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --correlation=FACTOR </dt>
 *   <dd> The minimum allowed image versus land mask correlation
 *   factor in the range [0..1].  If the image data matches the
 *   precomputed land mask to within the specified correlation factor,
 *   the navigation is considered to be successful.  The default
 *   correlation factor is 0.95.  Caution should be used in lowering
 *   this value, as it has a significant impact on the quality of
 *   navigation results. </dd>
 *
 *   <dt> -f, --fraction=FRACTION </dt>
 *   <dd> The minimum allowed class fraction in the range [0..1].  The
 *   class fraction is the count of land or water pixels from the
 *   class separation stage, divided by the total number of pixels in
 *   the navigation box.  If the fraction of either land or water
 *   pixels is too low, the image data is rejected.  The default
 *   minimum fraction is 0.05. </dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -H, --height=PIXELS </dt>
 *   <dd> The navigation box height.  By default, each navigation box is
 *   100 pixels in height. </dd>
 *
 *   <dt> -m, --match=PATTERN </dt>
 *   <dd> The variable name matching pattern.  If specified, the
 *   pattern is used as a regular expression to match variable names.
 *   Only variables matching the pattern will have the navigation
 *   correction applied.  By default, no pattern matching is performed
 *   and all variables are navigated. </dd>
 *
 *   <dt> -M, --minboxes=N </dt>
 *   <dd> The minimum number of successful navigation boxes needed to
 *   apply the navigation correction.  The default is 2. </dd>
 *
 *   <dt> -s, --search=LEVEL </dt> 
 *   <dd> The search level starting from 0.  This option should only be
 *   used if the magnitude of the navigation correction is likely to
 *   be half or more the size of the navigation box, as it can
 *   significantly increase the algorithm running time.  In these
 *   cases, the offset estimation can often fail because the image
 *   data is so far off the correct geographic features that the class
 *   separation and image correlation steps are meaningless.  When
 *   this option is specified, an area of (n+1)^2 times the size of
 *   the navigation box is searched, where n is the search level.  By
 *   default, only image data within the navigation box is used (n =
 *   0). </dd>
 *
 *   <dt> -S, --separation=DISTANCE </dt>
 *   <dd> The minimum allowed class separation distance in standard
 *   deviation units.  Typical values are in the range [1..4].  The
 *   greater the distance, the more distinct the land and water
 *   classes are.  The default distance is 2.5. </dd>
 *
 *   <dt> -t, --test </dt>
 *   <dd> Turns on test mode.  All operations that compute the
 *   navigation correction are performed, but no actual correction
 *   is applied to the input file.  By default, test mode is off and
 *   the input file is modified if a correction can be computed. </dd>
 *
 *   <dt> -v, --verbose </dt>
 *   <dd> Turns verbose mode on.  Details on offset estimation and 
 *   navigation correction are printed.  The default is to run
 *   with minimal messages. </dd>
 *
 *   <dt> -w, --width=PIXELS </dt>
 *   <dd> The navigation box width.  By default, each navigation box is
 *   100 pixels in width. </dd>
 *
 *   <dt>--version</dt>
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 *   <li> Unsupported input file format. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>The following example shows an automatic correction of an East
 * Coast CWF (IMGMAP format) file containing AVHHR channel 2 data.  A
 * total of 3 navigation boxes are specified in a text file, and the
 * size of each box set to 60 by 60 pixels.  The output shows that 2
 * of the 3 boxes were successful and a final navigation correction
 * of (rows, cols) = (-3, 1) was applied to the file.</p>
 * <pre>
 *   phollema$ cwautonav -v --width 60 --height 60 navbox.txt 
 *     avhrr_ch2 2004_064_1601_n17_er_c2.cwf
 *
 *   cwautonav: Reading input 2004_064_1601_n17_er_c2.cwf
 *   cwautonav: Testing box at 37.0503 N, 76.2111 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.33
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 44.2783 N, 66.1377 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation
 *     distance = 3.436
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.965 at
 *     offset = (-3, 1)
 *   cwautonav: Box offset = (-3, 1)
 *   cwautonav: Testing box at 45.1985 N, 65.9262 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.814
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.987 at
 *     offset = (-3, 1)
 *   cwautonav: Box offset = (-3, 1)
 *   cwautonav: Mean offset = (-3, 1)
 *   cwautonav: Applying navigation correction
 * </pre>
 * <p>The next example below shows the import and automatic correction of
 * multiple CWF files from the Gulf of Mexico.  The AVHRR channel 1,
 * channel 2, SST, and cloud mask variables are first imported to an
 * HDF file.  The automatic correction then runs using only data from
 * AVHRR channel 2 which provides high contrast between land and water
 * during the day.  The final correction is applied to all variables
 * in the input file.  This combination of import and autonavigation
 * is a convenient way of correcting a set of older CWF data files all
 * at once, using just data from AVHRR channel 2.</p>
 * <pre>
 *   phollema$ cwimport -v --match '(avhrr.*|sst|cloud)' 2004_313_1921_n16_mr*.cwf
 *     2004_313_1921_n16_mr.hdf
 *
 *   cwimport: Reading input 2004_313_1921_n16_mr_c1.cwf
 *   cwimport: Creating output 2004_313_1921_n16_mr.hdf
 *   cwimport: Converting file [1/4], 2004_313_1921_n16_mr_c1.cwf
 *   cwimport: Writing avhrr_ch1
 *   cwimport: Converting file [2/4], 2004_313_1921_n16_mr_c2.cwf
 *   cwimport: Writing avhrr_ch2
 *   cwimport: Converting file [3/4], 2004_313_1921_n16_mr_cm.cwf
 *   cwimport: Writing cloud
 *   cwimport: Converting file [4/4], 2004_313_1921_n16_mr_d7.cwf
 *   cwimport: Writing sst
 *
 *   phollema$ cwautonav -v --width 60 --height 60 navbox2.txt avhrr_ch2 2004_313_1921_n16_mr.hdf
 *
 *   cwautonav: Reading input 2004_313_1921_n16_mr.hdf
 *   cwautonav: Testing box at 26.7734 N, 82.1731 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.239
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.945 at
 *     offset = (-2, 1)
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient correlation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 29.1666 N, 83.0324 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.54
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.985 at 
 *     offset = (-2, 1)
 *   cwautonav: Box offset = (-2, 1)
 *   cwautonav: Testing box at 29.9141 N, 84.3543 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 4.514
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.976 at 
 *     offset = (-2, 0)
 *   cwautonav: Box offset = (-2, 0)
 *   cwautonav: Testing box at 30.3258 N, 88.1352 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.006
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.954 at 
 *     offset = (-3, 0)
 *   cwautonav: Box offset = (-3, 0)
 *   cwautonav: Testing box at 27.8423 N, 82.5433 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.59
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.953 at 
 *     offset = (-2, 1)
 *   cwautonav: Box offset = (-2, 1)
 *   cwautonav: Mean offset = (-2.25, 0.5)
 *   cwautonav: Applying navigation correction
 * </pre>
 * <p>Another example below shows the correction of a Hawaii AVHRR HDF
 * file using many 15 by 15 pixel navigation boxes distributed
 * throughout the islands.  AVHRR channel 2 data is used to compute
 * the optimal offset, and the final correction is applied only to
 * AVHRR sensor bands and derived variables.</p>
 * <pre>
 *   phollema$ cwautonav -v --match '(avhrr.*|sst|cloud)' --width 15 --height 15
 *     navbox3.txt avhrr_ch2 2005_042_0051_n16_hr.hdf
 *
 *   cwautonav: Reading input 2005_042_0051_n16_hr.hdf
 *   cwautonav: Testing box at 21.7885 N, 160.2259 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.537
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.9856 N, 160.0938 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.395
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.6033 N, 158.2847 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.562
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.7144 N, 157.9678 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.982
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.0961 N, 157.3207 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.517
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.2448 N, 157.2547 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.252
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.2076 N, 156.9774 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.236
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.973 at 
 *     offset = (-2, 0)
 *   cwautonav: Box 
 *     offset = (-2, 0)
 *   cwautonav: Testing box at 21.1581 N, 156.7001 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.293
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.9225 N, 157.0698 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.448
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.7115 N, 156.9642 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.506
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.3067 N, 158.1130 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.601
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 21.3067 N, 157.6508 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.593
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.5374 N, 156.7001 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 5.142
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.978 at 
 *     offset = (-2, 0)
 *   cwautonav: Box 
 *     offset = (-2, 0)
 *   cwautonav: Testing box at 20.5499 N, 156.5680 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.834
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.947 at 
 *     offset = (-2, -1)
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient correlation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.5996 N, 156.4360 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.285
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.8108 N, 156.5152 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.092
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.9349 N, 156.4756 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 3.629
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.969 at 
 *     offset = (-2, -1)
 *   cwautonav: Box 
 *     offset = (-2, -1)
 *   cwautonav: Testing box at 20.8357 N, 156.1190 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.46
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 20.2635 N, 155.8813 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 2.522
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Image correlation = 0.964 at 
 *     offset = (-2, 0)
 *   cwautonav: Box 
 *     offset = (-2, 0)
 *   cwautonav: Testing box at 19.5140 N, 154.7985 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.64
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 19.7392 N, 155.0230 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.833
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 19.7267 N, 155.1022 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.688
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 18.9119 N, 155.6965 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.378
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 19.8642 N, 155.9342 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.583
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 19.0375 N, 155.8813 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.638
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 22.0349 N, 159.7901 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.919
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Testing box at 22.1826 N, 159.3279 W
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Land/water class separation 
 *     distance = 1.496
 *   class noaa.coastwatch.util.NavigationOffsetEstimator: Insufficient separation
 *   cwautonav: Box failed
 *   cwautonav: Mean offset = (-2, -0.25)
 *   cwautonav: Applying navigation correction
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public final class cwautonav {

  // Constants
  // ------------

  /** Minimum required command line parameters. */
  private static final int NARGS = 3;

  /** Name of program. */
  private static final String PROG = "cwautonav";

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) throws Exception {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option verboseOpt = cmd.addBooleanOption ('v', "verbose");
    Option matchOpt = cmd.addStringOption ('m', "match");
    Option widthOpt = cmd.addIntegerOption ('w', "width");
    Option heightOpt = cmd.addIntegerOption ('H', "height");
    Option minboxesOpt = cmd.addIntegerOption ('M', "minboxes");
    Option testOpt = cmd.addBooleanOption ('t', "test");
    Option searchOpt = cmd.addIntegerOption ('s', "search");
    Option correlationOpt = cmd.addDoubleOption ('c', "correlation");
    Option fractionOpt = cmd.addDoubleOption ('f', "fraction");
    Option separationOpt = cmd.addDoubleOption ('S', "separation");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage();
      System.exit (1);
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage();
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
      usage();
      System.exit (1);
    } // if
    String locations = remain[0];
    String variable = remain[1];
    String input = remain[2];

    // Set defaults
    // ------------
    Boolean verboseObj = (Boolean) cmd.getOptionValue (verboseOpt);
    boolean verbose = (verboseObj == null ? false : verboseObj.booleanValue());
    String match = (String) cmd.getOptionValue (matchOpt);
    Integer widthObj = (Integer) cmd.getOptionValue (widthOpt);
    int width = (widthObj == null ? 100 : widthObj.intValue());
    Integer heightObj = (Integer) cmd.getOptionValue (heightOpt);
    int height = (heightObj == null ? 100 : heightObj.intValue());
    Integer minboxesObj = (Integer) cmd.getOptionValue (minboxesOpt);
    int minboxes = (minboxesObj == null ? 2 : minboxesObj.intValue());
    Boolean testObj = (Boolean) cmd.getOptionValue (testOpt);
    boolean test = (testObj == null ? false : testObj.booleanValue());
    Integer searchObj = (Integer) cmd.getOptionValue (searchOpt);
    int search = (searchObj == null ? 0 : searchObj.intValue());
    Double correlationObj = (Double) cmd.getOptionValue (correlationOpt);
    Double fractionObj = (Double) cmd.getOptionValue (fractionOpt);
    Double separationObj = (Double) cmd.getOptionValue (separationOpt);

    // Open input file
    // ---------------
    if (verbose) System.out.println (PROG + ": Reading input " + input);
    EarthDataReader reader = EarthDataReaderFactory.create (input);
    EarthTransform trans = reader.getInfo().getTransform();
    Datum datum = trans.getDatum();
    Grid grid = (Grid) reader.getVariable (variable);

    // Check file format
    // ----------------- 
    if (!reader.canUpdateNavigation()) {
      System.err.println (PROG + ": Unsupported file format for " + input);
      System.exit (2);
    } // if       

    // Loop over each location
    // -----------------------
    BufferedReader locationReader = 
      new BufferedReader (new FileReader (locations));
    List offsetList = new ArrayList();
    String line;
    NavigationOffsetEstimator estimator = new NavigationOffsetEstimator();
    estimator.setVerbose (verbose);
    if (correlationObj != null) 
      estimator.setMinCorrelation (correlationObj.doubleValue());
    if (fractionObj != null) 
      estimator.setMinFraction (fractionObj.doubleValue());
    if (separationObj != null) 
      estimator.setMinStdevDist (separationObj.doubleValue());
    while ((line = locationReader.readLine()) != null) {
                                                        
      // Get center location
      // -------------------
      String[] values = line.split ("[ \t]+");
      EarthLocation earthLoc = new EarthLocation (
        Double.parseDouble (values[0]), Double.parseDouble (values[1]), datum);

      // Get offset
      // ----------

      // TODO: Should we add a test here if the box center is out
      // of bounds?

      System.out.println (PROG + ": Testing box at " + earthLoc.format());
      int[] offset = estimator.getOffset (grid, trans, earthLoc, height, 
        width, search);
      if (offset != null) {
        System.out.println (PROG + ": Box offset = (" + offset[0] + ", " + 
          offset[1] + ")");
        offsetList.add (offset);
      } // if
      else {
        System.out.println (PROG + ": Box failed");
      } // else
      
    } // while

    // Test for minimum boxes
    // ----------------------
    if (offsetList.size() < minboxes) {
      System.out.println (PROG + ": Not enough good boxes (minimum " + 
        minboxes + " required)");
      System.exit (1);
    } // if

    // Compute mean
    // ------------
    double[] mean = new double[2];
    for (Iterator iter = offsetList.iterator(); iter.hasNext(); ) {
      int[] offset = (int[]) iter.next();
      mean[0] += offset[0];
      mean[1] += offset[1];
    } // for
    mean[0] /= offsetList.size();
    mean[1] /= offsetList.size();
    DecimalFormat fmt = new DecimalFormat ("0.###");
    System.out.println (PROG + ": Mean offset = (" + fmt.format (mean[0]) + 
      ", " + fmt.format (mean[1]) + ")");

    // Check for test mode
    // -------------------
    if (test) System.exit (0);

    // Get variable names
    // ------------------
    List variables = new ArrayList();
    for (int i = 0; i < reader.getVariables(); i++) {
      DataVariable var = reader.getPreview(i);
      if (var.getRank() != 2) continue;
      String varName = var.getName();
      if (match != null && !varName.matches (match)) continue;
      variables.add (varName);
    } // for      

    // Perform navigation
    // ------------------
    if (verbose)
      System.out.println (PROG + ": Applying navigation correction");
    AffineTransform affine = AffineTransform.getTranslateInstance (mean[0],
      mean[1]);
    reader.updateNavigation (variables, affine);
    reader.close();

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwautonav [OPTIONS] locations-file variable input\n" +
"Automatically determines a navigation correction based on Earth image\n" +
"data.\n" +
"\n" +
"Main parameters:\n" +
"  locations-file             The text file of latitude/longitude\n" +
"                              locations.\n" +
"  variable                   The input data variable used for image data.\n" +
"  input                      The input data file name.\n" +
"\n" +
"Options:\n" +
"  -c, --correlation=FACTOR   Set minimum correlation factor.\n" + 
"  -f, --fraction=FRACTION    Set minimum class fraction.\n" +
"  -h, --help                 Show this help message.\n" +
"  -H, --height=PIXELS        Set navigation box height in pixels.\n" +
"  -m, --match=PATTERN        Apply correction to variables matching the\n" +
"                              pattern.\n" +
"  -M, --minboxes=N           Set minimum number of successful navigation\n" +
"                              boxes needed to apply mean correction.\n" +
"  -s, --search=LEVEL         Set search level.\n" +
"  -S, --separation=DISTANCE  Set minimum class separation distance.\n" +
"  -t, --test                 Do not apply correction, run test mode only.\n" +
"  -v, --verbose              Print verbose messages.\n" +
"  -w, --width=PIXELS         Set navigation box width in pixels.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

  private cwautonav () { }

  ////////////////////////////////////////////////////////////

} // cwautonav

////////////////////////////////////////////////////////////////////////
