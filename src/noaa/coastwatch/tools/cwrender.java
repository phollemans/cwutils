////////////////////////////////////////////////////////////////////////
/*

     File: cwrender.java
   Author: Peter Hollemans
     Date: 2002/10/11

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.GraphicsEnvironment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.EarthImageWriter;
import noaa.coastwatch.render.BitmaskOverlay;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.ColorArrowSymbol;
import noaa.coastwatch.render.ColorComposite;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.ColorLookup;
import noaa.coastwatch.render.ColorPointEnhancement;
import noaa.coastwatch.render.ColorWindBarbSymbol;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.render.DirectionSymbol;
import noaa.coastwatch.render.feature.ESRIShapefileReader;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.EnhancementFunctionFactory;
import noaa.coastwatch.render.ExpressionMaskOverlay;
import noaa.coastwatch.render.feature.GriddedPointGenerator;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.IconElementFactory;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.Legend;
import noaa.coastwatch.render.OverlayGroupManager;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.PoliticalOverlay;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.render.Renderable;
import noaa.coastwatch.render.TextElement;
import noaa.coastwatch.render.TextOverlay;
import noaa.coastwatch.render.TopographyOverlay;
import noaa.coastwatch.render.WindBarbSymbol;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.TimePeriod;
import noaa.coastwatch.util.UnitFactory;
import noaa.coastwatch.util.trans.EarthTransform2D;
import ucar.units.Unit;

/**
 * <p>The render tool performs earth data visualization.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->          
 *   cwrender - performs earth data visualization.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 * cwrender {-c, --composite=RED/GREEN/BLUE} [OPTIONS] input output <br>
 * cwrender {-e, --enhance=VARIABLE1[/VARIABLE2]} [OPTIONS] input output
 * </p>
 *
 * <h3>General options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -v, --verbose <br>
 * --version <br>
 * --split=EXPRESSION <br>
 * </p>
 *
 * <h3>Output content and format options:</h3>
 *
 * <p>
 * -a, --noantialias <br>
 * -D, --date=STARTDATE[/ENDDATE] <br>
 * --font=FAMILY[/STYLE[/SIZE]] <br>
 * --fontlist <br>
 * -f, --format=TYPE <br>
 * -i, --indexed <br>
 * -I, --imagecolors=NUMBER <br>
 * -l, --nolegends <br>
 * -m, --magnify=LATITUDE/LONGITUDE/FACTOR <br>
 * -o, --logo=NAME <br>
 * -s, --size=PIXELS | full <br>
 * -T, --tiffcomp=TYPE <br>
 * -W, --worldfile=FILE
 * </p>
 *
 * <h3>Plot overlay options:</h3>
 *
 * <p>
 * -A, --bath=COLOR[/LEVEL1/LEVEL2/...]  <br>
 * -b, --bitmask=VARIABLE/MASK/COLOR <br>
 * -C, --coast=COLOR[/FILL] <br>
 * -d, --cloud=COLOR <br>
 * -g, --grid=COLOR <br>
 * -H, --shape=FILE/COLOR[/FILL] <br>
 * -L, --land=COLOR <br>
 * -p, --political=COLOR <br>
 * -S, --nostates <br>
 * -t, --topo=COLOR[/LEVEL1/LEVEL2/...] <br>
 * -u, --group=GROUP <br>
 * -w, --water=COLOR <br>
 * -X, --exprmask=EXPRESSION/COLOR <br>
 * --watermark=TEXT[/COLOR[/SIZE[/ANGLE]]] <br>
 * --watermarkshadow
 * </p>
 * 
 * <h3>Color enhancement options:</h3>
 *
 * <p>
 * -E, --enhancevector=STYLE/SYMBOL[/SIZE] <br>
 * -F, --function=TYPE <br>
 * -k, --background=COLOR <br>
 * -M, --missing=COLOR <br>
 * -P, --palette=NAME <br>
 * --palettefile=FILE <br>
 * --palettecolors=COLOR1[/COLOR2[/COLOR3...]] <br>
 * --palettelist <br>
 * -r, --range=MIN/MAX <br>
 * --scalewidth=PIXELS <br>
 * --ticklabels=LABEL1[/LABEL2[/LABEL3/...]] <br>
 * -U, --units=UNITS
 * </p>
 *
 * <h3>Color composite options:</h3>
 *
 * <p>  
 * -B, --bluerange=MIN/MAX <br>
 * -G, --greenrange=MIN/MAX <br>
 * -R, --redrange=MIN/MAX <br>
 * -x, --redfunction=TYPE <br>
 * -y, --greenfunction=TYPE <br>
 * -z, --bluefunction=TYPE
 * </p>
 *
 * <h2>Description</h2>
 *
 * <h3>Overview</h3>
 *
 * <p>The render tool performs earth data visualization by
 * converting 2D data sets in the input file to color images.
 * The data values are converted from scientific units to a color
 * using either an enhancement function and color palette or by
 * performing a color composite of three data variables &#8212; one
 * for each of the red, green, and blue color components.  The
 * resulting earth data plot may have legends displaying the
 * color scale, data origin, date, time, projection info, as well
 * as data overlays showing latitude/longitude grid lines, coast
 * lines, political boundaries, masks, and shapes.</p>
 *
 * <h3>Overlay colors</h3>
 *
 * <p>Overlay colors may be specified using simple color names
 * such as 'red', 'gray', 'cyan', 'blue', and 'green'.  Overlays
 * may be made to appear slightly transparent (allowing the color
 * behind to show through) by following the color name with a
 * colon ':' and a transparency value in percent, for example
 * 'red:50' would make the overlay red with a 50% transparency.
 * Transparency values range from 0 (completely opaque) to 100
 * (completely transparent).</p>
 * 
 * <p>Colors may also be specified using explicit hexadecimal
 * notation for red/green/blue color components and optional
 * alpha component as follows:</p>
 * <pre>
 *   0xAARRGGBB
 *     ^ ^ ^ ^          
 *     | | | ----- Blue              \
 *     | | ------- Green             |---- Range: 00 -&gt; ff
 *     | --------- Red               |
 *     ----------- Alpha (optional)  /
 * </pre>
 *
 * <p>Note that the prepended '0x' denotes a hexadecimal constant,
 * and must be used even though it is not part of the color
 * component values.  As an example, the simple color names above
 * may be specified as hexadecimal values:</p>
 *
 * <pre>
 *   0xff0000    red
 *   0x555555    gray
 *   0x00ffff    cyan
 *   0x0000ff    blue
 *   0x00ff00    green
 *   0x80ff0000  red, 50% transparent
 * </pre>
 *
 * <h3>Rendering order</h3>
 * 
 * <p>The data view itself (not including the legends) is
 * rendered in such a way that overlays may overlap each other.
 * For example, latitude/longitude grid lines may fall on top of
 * land polygons because the grid overlay is rendered after the
 * coastline overlay.  Knowing the order in which the data and
 * overlays are rendered may answer some questions if the data
 * view doesn't look the way the user expects.  The data view is
 * rendered in the following order:</p>
 *
 * <ol>
 * 
 *   <li>Before any overlay or data, the data view is filled with
 *   a background color (normally white) for vector plots or a
 *   missing color (normally black) for color enhancement or
 *   color composite plots.</li>
 *
 *   <li>Color vectors or image pixels are rendered to the data
 *   view.  The background or missing color will show though
 *   where no vectors or pixels were rendered.</li>
 *   
 *   <li>Data overlays are rendered to the view in the following
 *   order (see the description of each option below):
 *   <ul>
 *     <li>Cloud mask (<b>--cloud</b>)</li>
 *     <li>Bit masks (<b>--bitmask</b>), possibly more than one</li>
 *     <li>Expression masks (<b>--exprmask</b>), possibly more than one</li>
 *     <li>Water mask (<b>--water</b>)</li>
 *     <li>Bathymetric contours (<b>--bath</b>)</li>
 *     <li>Land mask (<b>--land</b>)</li>
 *     <li>Coastline and filled land polygons (<b>--coast</b>)</li>
 *     <li>Political lines (<b>--political</b>)</li>
 *     <li>Topography contours (<b>--topo</b>)</li>
 *     <li>Shape files (<b>--shape</b>), possibly more than one</li>
 *     <li>Latitude/longitude grid lines (<b>--grid</b>)</li>
 *     <li>Overlay groups (<b>--group</b>)</li>
 *   </ul></li>
 *
 * </ol>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>-c, --composite=RED/GREEN/BLUE</dt>
 *
 *   <dd>Specifies color composite mode using the named variables.
 *   The data variable values are converted to colors using an
 *   individual enhancement function for each variable.  The
 *   data values are scaled to the range [0..255] and used as the red,
 *   green, and blue components of each pixel's color.  Either this
 *   option or <b>--enhance</b> must be specified, but not both.</dd>
 *
 *   <dt>-e, --enhance=VARIABLE1[/VARIABLE2]</dt>
 *
 *   <dd>Specifies color enhancement mode using the named variable(s).
 *   The data variable values are converted to colors using an
 *   enhancement function and color palette.  Either this option or
 *   <b>--composite</b> must be specified, but not both.  If one
 *   variable name is specified, the plot shows color-enhanced image
 *   data.  If two variable names are specified, the plot shows
 *   color-enhanced vectors whose direction is derived using the two
 *   variables as vector components.  See the <b>--enhancevector</b>
 *   and <b>--background</b> options for settings that are specific to
 *   vector plots.</dd>
 *
 *   <dt>input</dt>
 *   <dd>The input data file name.</dd>
 *
 *   <dt>output</dt>
 *   <dd>The output image file name.  Unless the <b>--format</b>
 *   option is used, the file extension indicates the desired output
 *   format: '.png', '.jpg', '.tif', or '.pdf'.</dd>
 *
 * </dl>
 *
 * <h3>General options:</h3>
 *
 * <dl>
 *
 *   <dt>-h, --help</dt>

 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-v, --verbose</dt>
 *
 *   <dd>Turns verbose mode on.  The current status of data
 *   rendering is printed periodically.  The default is to run
 *   quietly.</dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 *   <dt>--split=EXPRESSION</dt>
 *
 *   <dd>The command line parameter splitting expression.  By default,
 *   parameters on the command line are specified using a slash '/' character
 *   between multiple arguments, for example <b>--coast white/brown</b>.  
 *   But in some cases, for example when a variable name includes a slash,
 *   another character should be used to parse the command line parameters.
 *   A common alternative to the slash is a comma ',' character, for example
 *   <b>--coast white,brown</b>.</dd>
 *
 * </dl>
 *
 * <h3>Output content and format options:</h3>
 *
 * <dl>
 *
 *   <dt>-a, --noantialias</dt>
 *
 *   <dd>Turns off line and font antialiasing.  By default, the edges
 *   of lines and fonts are smoothed using shades of the drawing
 *   color.  It may be necessary to turn off antialiasing if the
 *   smoothing is interfering with the readability of annotation
 *   graphics, such as in the case of very small fonts.  This option
 *   only effects raster image output formats such as PNG, GIF and
 *   JPEG.</dd>
 *
 *   <dt>-D, --date=STARTDATE[/ENDDATE]</dt>
 *
 *   <dd>Specifies that the plot legend should be rendered with the
 *   given date(s).  By default the date is automatically detected
 *   from the input file metadata.  In some cases the metadata is incorrect
 *   or the date and time information is not available.  In these cases,
 *   the data start and end date can be manually specified by this option, in 
 *   ISO date/time format 'yyyy-mm-ddThh:mm:ssZ'.  For example, 
 *   Dec 19, 2013 at 11 pm UTC would be specified as '2013-12-19T23:00:00Z'.
 *   If the data has no known end date, the end date value may be omitted.</dd>
 *
 *   <dt>--font=FAMILY[/STYLE[/SIZE]]</dt>
 *
 *   <dd>The legend and overlay text font with optional style and size.
 *   The default is 'Dialog/plain/9'. The font family names available differ
 *   based on the fonts installed on a given system.  The family names 'Dialog',
 *   'DialogInput', 'Monospaced', 'Serif', and 'SansSerif' are always available
 *   and map to certain system fonts at runtime.  For a full listing of fonts
 *   families available on the system, use the <b>--fontlist</b> option.
 *   Valid styles  are 'plain', 'bold', 'italic', and 'bold-italic'.
 *   For example to use an Arial font available on some systems, bold style,
 *   of 12 point size, specify 'Arial/bold/12'.  To use a sans serif font
 *   with default style and size, specify 'SansSerif'.</dd>
 *
 *   <dt>--fontlist</dt>
 *
 *   <dd>Lists the font family names available on the system.</dd>
 *
 *   <dt>-f, --format=TYPE</dt>
 *
 *   <dd>The output format.  The current formats are 'png' for
 *   Portable Network Graphics, 'gif' for Graphics Interchange Format,
 *   'jpg' for Joint Picture Experts Group, 'tif' for Tagged Image
 *   File Format with geolocation tags (GeoTIFF), 'pdf' for Portable
 *   Document Format, or 'auto' to detect the format from the output
 *   file name.  The default is 'auto'.  The correct choice of output
 *   format is governed by the desired use of the rendered image as
 *   follows:
 *   <ul>
 *
 *     <li> <b>PNG</b> is a non-lossy compressed image format
 *     supported by most web browsers and image manipulation software.
 *     It has similar data compression characteristics to GIF and
 *     additionally supports 24-bit color images. </li>
 *
 *     <li> <b>GIF</b> is a non-lossy compressed format also supported
 *     by most web browsers and image manipulation software.  The GIF
 *     files produced use LZW compression.  Images stored in GIF
 *     format are run through a color quantization algorithm to reduce
 *     the color map to 256 colors or less.  Although file sizes are
 *     generally smaller than PNG, image quality may be compromised by
 *     the reduced color map.</li>
 *
 *     <li> <b>JPEG</b> is a lossy compressed format that should be
 *     used with caution for images with sharp color lines such as
 *     those found in text and annotation graphics.  The JPEG format
 *     generally achieves higher compression than PNG or GIF resulting
 *     in smaller image file sizes. </li>
 *
 *     <li> <b>GeoTIFF</b> is a flexible image format with
 *     support for earth location metadata.  Many popular GIS
 *     packages handle GeoTIFF images and allow the user to
 *     combine a GeoTIFF base map image with other sources of
 *     raster and vector data.  The GeoTIFF images generated are
 *     non-lossy uncompressed image data (unless a compression is
 *     specified using <b>--tiffcomp</b>), and can be much larger
 *     than the corresponding PNG, GIF, or JPEG.  Since GeoTIFF
 *     images are generally destined for import into a GIS
 *     system, the use of this format turns on the
 *     <b>--nolegends</b> option.  In general the GeoTIFFs
 *     generated are 24-bit colour images, but when no overlays
 *     are specified or the <b>--indexed</b> or
 *     <b>--imagecolors</b> options are used, a special 8-bit
 *     paletted image file is generated and comments describing
 *     the data value scaling are inserting into the image
 *     description tags.  </li>
 *
 *     <li> <b>PDF</b> is a standard for high quality publishing
 *     developed by Adobe Systems and is used for output to a printer
 *     via such tools as the Adobe Acrobat Reader.  In general PDF
 *     files are slightly larger than the equivalent PNG but retain
 *     highly accurate vector graphics components such as lines and
 *     fonts. </li>
 *
 *   </ul></dd>
 *
 *   <dt>-i, --indexed</dt>
 *
 *   <dd>Short for <b>--imagecolors 256</b>.  See the
 *   <b>--imagecolors</b> option below.</dd>
 *
 *   <dt>-I, --imagecolors=NUMBER</dt>
 *
 *   <dd>The number of colors to use for the index color model of
 *   the data image, up to 256.  Normally the data image uses an
 *   unlimited number of colors because this achieves the best
 *   visual rendering quality.  But in some cases it may be
 *   desirable to make the output file smaller by limiting the
 *   number of colors to &lt;=256 values and using a index color
 *   model so that each data pixel can be represented as 8-bit
 *   bytes.  This option can only be used with PNG, GIF, GeoTIFF,
 *   and PDF output formats, and only with color enhancements,
 *   not color composites.  While in index color mode,
 *   antialiasing is turned off.</dd>
 *
 *   <dt>-l, --nolegends</dt>
 *
 *   <dd>Turns the plot legends off.  By default, the Earth data
 *   view is shown in a frame on the left and to the right color scale
 *   and plot information legends are drawn.  With no legends, the
 *   Earth data is simply rendered by itself with no frame,
 *   borders, or legends.</dd>
 *
 *   <dt>-m, --magnify=LATITUDE/LONGITUDE/FACTOR</dt> 
 *
 *   <dd>The magnification center and factor.  The data view is set
 *   to the specified center and pixel magnification factor.  The
 *   center position is specified in terms of Earth location latitude
 *   and longitude in the range [-90..90] and [-180..180] and the
 *   magnification factor as a fractional number (0..1] where factors
 *   &gt; 1 magnify and factors &lt; 1 shrink.  By default, the data view
 *   shows the entire data field with an optimal magnification factor
 *   to fit the desired view size (see <b>--size</b>).</dd>
 *
 *   <dt>-o, --logo=NAME</dt>
 *
 *   <dd>The logo used for plot legends.  The current predefined logo
 *   names are 'noaa3d' (the default), 'nasa3d', 'nws3d', 'doc3d', and
 *   their corresponding non-3D versions 'noaa', 'nasa', 'nws', and
 *   'doc'.  The predefined logos are named for their respective
 *   government agencies: NOAA, NASA, National Weather Service (NWS),
 *   and Department of Commerce (DOC).  The user may also specify a
 *   custom logo file name, which can be any PNG, GIF, or JPEG
 *   file.</dd>
 *
 *   <dt>-s, --size=PIXELS | full</dt>
 *
 *   <dd>The Earth data view size in pixels.  The data view is
 *   normally accompanied by a set of legends unless the
 *   <b>--nolegends</b> option is used.  By default, the view
 *   size is 512 pixels, plus the size of any legends.  If 'full'
 *   is specified rather than a size in pixels, the view size is
 *   set to match the actual full extent of the data, ie: full
 *   resolution.</dd>
 *
 *   <dt>-T, --tiffcomp=TYPE</dt>
 *
 *   <dd>The TIFF compression algorithm.  The valid types are 'none'
 *   for no compression (the default), 'deflate' for ZIP style
 *   compression, and 'pack' for RLE style PackBits compression.  This
 *   option is only used with GeoTIFF output.</dd>
 *
 *   <dt>-W, --worldfile=FILE</dt>
 *
 *   <dd>The name of the world file to write.  A world file is an
 *   ASCII text file used for georeferencing images that contains the
 *   following lines:
 *   <ul>
 *     <li>Line 1: x-dimension of a pixel in map units</li>
 *     <li>Line 2: rotation parameter</li>
 *     <li>Line 3: rotation parameter</li>
 *     <li>Line 4: NEGATIVE of y-dimension of a pixel in map units</li>
 *     <li>Line 5: x-coordinate of center of upper left pixel</li>
 *     <li>Line 6: y-coordinate of center of upper left pixel</li>
 *   </ul>
 *   World files may be written for any GIF, PNG, or JPEG image.  The
 *   use of this option turns on the <b>--nolegends</b> option.  The
 *   convention used in GIS is to name the world file similarly to the
 *   image file, but with a different extension.  GDAL expects world
 *   files with a ".wld" extension, where as ESRI applications expect
 *   ".pgw" for PNG, ".gfw" for GIF, and ".jgw" for JPEG.  Users
 *   should name their world files accordingly.</dd>
 *
 * </dl>
 *
 * <h3>Plot overlay options:</h3>
 *
 * <dl>
 *
 *   <dt>-A, --bath=COLOR[/LEVEL1/LEVEL2/...]</dt>
 *
 *   <dd>The bathymetric contour color and levels.  The color is
 *   specified by name or hexadecimal value (see above).  Bathymetric
 *   contours are generated for the specified integer levels in
 *   meters.  If no levels are specified, contours are drawn at 200 m
 *   and 2000 m.  The default is not to render bathymetric
 *   contours.</dd>
 *
 *   <dt>-b, --bitmask=VARIABLE/MASK/COLOR</dt>
 *
 *   <dd>Specifies that a mask should be rendered on top of the
 *   data image whose pixels are obtained by a bitwise AND with
 *   the mask value.  The named variable is used to mask the
 *   Earth data with the specified color and mask.  The color is
 *   a name or hexadecimal value (see above).  The mask is a
 *   32-bit integer hexadecimal value specifying the mask bits.
 *   The bitmask is formed by bitwise ANDing the data value and
 *   mask value.  If the result of the operation is non zero, the
 *   pixel is colored with the bitmask color.  This option is
 *   useful for overlaying graphics on the data image when the
 *   graphics are stored as an integer valued variable in the
 *   data set.  Such variables include cloud and land mask
 *   graphics.  Multiple values of the <b>--bitmask</b> option
 *   may be given, in which case the masks are applied in the
 *   order that they are specified.</dd>
 *
 *   <dt>-C, --coast=COLOR[/FILL]</dt>
 *
 *   <dd>The coast line color and optional fill color.  The colors are
 *   specified by name or hexadecimal value (see above).  The default
 *   is not to render coast lines.</dd>
 *
 *   <dt>-d, --cloud=COLOR</dt> 
 *
 *   <dd>The cloud mask color.  The color is specified by name or
 *   hexadecimal value (see above).  Cloud masking requires that a
 *   'cloud' variable exists in the input file.  The default is not to
 *   render a cloud mask.</dd>
 *
 *   <dt>-g, --grid=COLOR</dt>
 *
 *   <dd>The latitude/longitude grid line color.  The color is
 *   specified by name or hexadecimal value (see above).  The default
 *   is not to render grid lines.</dd>
 * 
 *   <dt>-H, --shape=FILE/COLOR[/FILL]</dt>
 *
 *   <dd>The name and drawing/fill colors for a user-supplied
 *   shape file.  The colors are specified by name or hexadecimal
 *   value (see above).  The only file format currently supported
 *   is ESRI shapefile format, and only line and polygon data (no
 *   point data).  The fill color is optional and is used to fill
 *   polygons if any are found in the file.  Multiple values of
 *   the <b>--shape</b> option may be given, in which case the
 *   shape overlays are rendered in the order that they are
 *   specified.</dd>
 * 
 *   <dt>-L, --land=COLOR</dt> 
 *
 *   <dd>The land mask color.  The color is specified by name or
 *   hexadecimal value (see above).  Land masking requires that a
 *   'graphics' variable exists in the input file with a land mask at
 *   bit 3 where bit numbering starts at 0 for the least significant
 *   bit.  The default is not to render a land mask.  For an
 *   alternative to the <b>--land</b> option, try using the
 *   <b>--coast</b> option with a fill color.</dd>
 *
 *   <dt>-p --political=COLOR</dt>
 *
 *   <dd>The political boundaries color.  The color is specified by
 *   name or hexadecimal value (see above).  The default is not to
 *   render political boundaries.</dd>
 *
 *   <dt>-S, --nostates</dt> 
 *
 *   <dd>Turns off state boundary rendering.  The default when
 *   <b>--political</b> is specified is to render international and
 *   state boundaries.  With this option is specified, only
 *   international boundaries are rendered.</dd>
 *
 *   <dt>-t, --topo=COLOR[/LEVEL1/LEVEL2/...]</dt>
 *
 *   <dd>The topographic contour color and levels.  The color is
 *   specified by name or hexadecimal value (see above).  Topographic
 *   contours are generated for the specified integer levels in
 *   meters.  If no levels are specified, contours are drawn at 200 m,
 *   500 m, 1000 m, 2000 m, and 3000 m.  The default is not to render
 *   topographic contours.</dd>
 *
 *   <dt>-u, --group=GROUP</dt>
 *
 *   <dd>The overlay group name to render.  Overlay groups are a
 *   concept from the CoastWatch Data Analysis Tool (CDAT).  CDAT
 *   users can save a set of preferred overlays as a group and then
 *   restore those overlays when viewing a new data file.  The same
 *   group names saved from CDAT are available to be rendered here.
 *   <u>This is an extremely useful option</u> that allows users to
 *   design a set of overlays graphically and adjust the various
 *   overlay properties beyond what can be achieved using the command
 *   line options for cwrender.  If specified, this option will cause
 *   all overlays in the group to be drawn on top of any other
 *   overlays specified by command line options.</dd>
 *
 *   <dt>-w, --water=COLOR</dt> 
 *
 *   <dd>The water mask color.  The color is specified by name or
 *   hexadecimal value (see above).  Water masking is performed
 *   similarly to land masking (see the <b>--land</b> option), but the
 *   sense of the land mask is inverted.  The default is not to render
 *   a water mask.</dd>
 *
 *   <dt>-X, --exprmask=EXPRESSION/COLOR</dt>
 *
 *   <dd>Specifies that a mask should be rendered on top of the
 *   data image whose pixels are obtained by evaluating the
 *   expression.  The color is specified by name or hexadecimal
 *   value (see above).  An expression mask is a special type of
 *   multipurpose mask similar to a bitmask (see the
 *   <b>--bitmask</b> option above) but which allows the user to
 *   specify a mathematical expression to determine the mask.  If
 *   the result of the expression is true (in the case of a
 *   boolean result) or non-zero (in the case of a numerical
 *   result), the data image is masked at the given location with
 *   the given color.  Multiple values of the <b>--exprmask</b>
 *   option may be given, in which case the masks are applied in
 *   the order that they are specified.  The syntax for the
 *   expression is identical to the right-hand-side of a
 *   <b>cwmath</b> expression (see the <b>cwmath</b> tool manual
 *   page).</dd>
 *
 *   <dt>--watermark=TEXT[/COLOR[/SIZE[/ANGLE]]]</dt>
 *
 *   <dd>Specifies the text for a watermark that is placed in the center of the
 *   image plot to denote special status such as experimental or restricted,
 *   or some other property of the data.  The default watermark text is white,
 *   50% opacity, 50 point font, and 0 degrees rotation.  Optional
 *   parameters may be specified by appending the watermark color (name or
 *   hexadecimal value as described above), the point size, and baseline 
 *   angle (0 is horizontal, 90 is vertical).  For example, 
 *   --watermark=EXPERIMENTAL/white/36/20 adds the text EXPERIMENTAL in solid
 *   white, 36 point font, at a 20 degree baseline rotation.</dd>
 *
 *   <dt>--watermarkshadow</dt>
 *   
 *   <dd>Draws a drop shadow behind the watermark to increase visibility.  
 *   By default the watermark is drawn plain with no drop shadow.</dd>
 *
 * </dl>
 *
 * <h3>Color enhancement options:</h3>
 *
 * <dl>
 *
 *   <dt>-E, --enhancevector=STYLE/SYMBOL[/SIZE]</dt>
 *
 *   <dd>The color-enhanced vector specifications.  This option is
 *   only used if two variable names are passed to the
 *   <b>--enhance</b> option.  The vector style may be either 'uvcomp'
 *   or 'magdir'; the default is 'uvcomp'.  In uvcomp mode, the
 *   variables that are passed to the <b>--enhance</b> option are
 *   taken to be the U (x-direction) and V (y-direction) components of
 *   the vector.  In magdir mode, the first variable is taken to be
 *   the vector magnitude, and the second to be the vector direction
 *   in degrees clockwise from north.  The vector symbol may be either
 *   'arrow' to draw arrows in the direction of the vector, or 'barb'
 *   to draw WMO wind barbs; the default is 'arrow'.  If wind barbs
 *   are used, the feathered end of the barb points in the direction
 *   of the wind.  Lastly, the size of the vector symbols in pixels
 *   may be specified; the default size is 10.</dd>
 *
 *   <dt>-F, --function=TYPE</dt>
 *
 *   <dd>The color enhancement function.  Data values are mapped to
 *   the range [0..255] by the enhancement function and range, and
 *   then to colors using the color palette.  The valid enhancement
 *   function types are 'linear', 'boolean', 'stepN', 'log', 'gamma', 'linear-reverse',
 *   'stepN-reverse','log-reverse', and 'gamma-reverse' where N is the number of steps
 *   in the function, for example 'step10'.  The 'boolean' function is a
 *   shorthand way of specifying 'step2' as the function, and '0/1' 
 *   as the range, useful for data with only 0 and 1 as data values.  The 
 *   reverse functions are equivalent to the non-reversed functions but map 
 *   data values to the range [255..0] rather then [0..255].  By default, the
 *   enhancement function is 'linear'.  A log enhancement may be
 *   necessary when the data value range does not scale well with a
 *   linear enhancement such as with chlorophyll concentration derived
 *   from ocean color data. A gamma enhancement may be needed when the data
 *   values represent a brightness from 0% to 100% and compensates for the
 *   gamma correction in computer displays. </dd>
 *
 *   <dt>-k, --background=COLOR</dt>
 *
 *   <dd>The color for the background of vector plots. The color is
 *   specified by name or hexadecimal value (see above).  The default
 *   background color is white.</dd>
 *
 *   <dt>-M, --missing=COLOR</dt>
 *
 *   <dd>The color for missing or out of range data values.  The
 *   color is specified by name or hexadecimal value (see above).  The
 *   default missing color is black.</dd>
 *
 *   <dt>-P, --palette=NAME</dt>
 *
 *   <dd>The color palette for converting data values to colors.  The
 *   color palettes have been derived from a number of sources including
 *   SeaSpace Terascan, Interactive Data Language (IDL) v5.4, and the Python
 *   matplotlib cmocean package, and have similar names.  The valid
 *   color palette names are as follows (line indexes are simply for
 *   reference):
 *   <pre>
 *     0  BW-Linear
 *     1  HSL256
 *     2  RAMSDIS
 *     3  Blue-Red
 *     4  Blue-White
 *     5  Grn-Red-Blu-Wht
 *     6  Red-Temperature
 *     7  Blue-Green-Red-Yellow
 *     8  Std-Gamma-II
 *     9  Prism
 *     10 Red-Purple
 *     11 Green-White-Linear
 *     12 Grn-Wht-Exponential
 *     13 Green-Pink
 *     14 Blue-Red2
 *     15 16-Level
 *     16 Rainbow
 *     17 Steps
 *     18 Stern-Special
 *     19 Haze
 *     20 Blue-Pastel-Red
 *     21 Pastels
 *     22 Hue-Sat-Lightness-1
 *     23 Hue-Sat-Lightness-2
 *     24 Hue-Sat-Value-1
 *     25 Hue-Sat-Value-2
 *     26 Purple-Red-Stripes
 *     27 Beach
 *     28 Mac-Style
 *     29 Eos-A
 *     30 Eos-B
 *     31 Hardcandy
 *     32 Nature
 *     33 Ocean
 *     34 Peppermint
 *     35 Plasma
 *     36 Rainbow2
 *     37 Blue-Waves
 *     38 Volcano
 *     39 Waves
 *     40 Rainbow18
 *     41 Rainbow-white
 *     42 Rainbow-black
 *     43 NDVI
 *     44 GLERL-Archive
 *     45 GLERL-30-Degrees
 *     46 Chlora-1
 *     47 Chlora-anom
 *     48 Spectrum
 *     49 Wind-0-50
 *     50 CRW_SST
 *     51 CRW_SSTANOMALY
 *     52 CRW_HOTSPOT
 *     53 CRW_DHW
 *     54 StepSeq25
 *     55 HSB-Cycle
 *     56 Ocean-algae
 *     57 Ocean-amp
 *     58 Ocean-balance
 *     59 Ocean-curl
 *     60 Ocean-deep
 *     61 Ocean-delta
 *     62 Ocean-dense
 *     63 Ocean-gray
 *     64 Ocean-haline
 *     65 Ocean-ice
 *     66 Ocean-matter
 *     67 Ocean-oxy
 *     68 Ocean-phase
 *     69 Ocean-solar
 *     70 Ocean-speed
 *     71 Ocean-tempo
 *     72 Ocean-thermal
 *     73 Ocean-turbid
 *   </pre>
 *   By default, the 'BW-Linear' palette is used which is a gray scale
 *   color ramp from black to white.</dd>
 *
 *   <dt>--palettefile=FILE</dt>
 *
 *   <dd>The file of color palette XML data for converting data values 
 *   to colors.  The format of the XML file is described in the User's 
 *   Guide.  By default, the 'BW-Linear' palette is used.</dd>
 *
 *   <dt>--palettecolors=COLOR1[/COLOR2[/COLOR3...]]</dt>
 *
 *   <dd>The palette colors for converting data values to colors.  Up to 256
 *   colors may be specified by name or hexadecimal value (see above).
 *   By default, the 'BW-Linear' palette is used.</dd>
 *
 *   <dt>--palettelist</dt>
 *
 *   <dd>Lists the palette names available on the system, including any
 *   palettes in the user's resource directory.</dd>
 *
 *   <dt>-r, --range=MIN/MAX</dt>
 *
 *   <dd>The color enhancement range.  Data values are mapped to
 *   colors using the minimum and maximum values and an enhancement
 *   function.  By default, the enhancement range is derived from the
 *   data value mean and standard deviation to form an optimal
 *   enhancement window of 1.5 standard deviation units around the
 *   mean.</dd>
 *
 *   <dt>--scalewidth=PIXELS</dt>
 * 
 *   <dd>The data color scale width.  By default the data color scale is
 *   90 pixels wide which includes the color bar, tick marks, value labels,
 *   and the variable name and units.  This default accommodates most scales,
 *   but if a scale requires a wider size, it will grow to fit.
 *   In some cases this results in data plots with different data
 *   ranges being different widths overall, which may be undesirable.  In these
 *   cases the scale width can be set explicitly to a larger value.</dd>
 *
 *   <dt>--ticklabels=LABEL1[/LABEL2[/LABEL3/...]]</dt>
 *
 *   <dd>The numeric tick mark labels to use for the data color scale.  By
 *   default the tick mark labels are generated automatically.  For example:<br>
 *   --ticklabels=1.0/1.1/1.2/1.3/1.4/1.5<br>would put tick marks and labels 
 *   at evenly spaced locations on the color scale from 1.0 to 1.5.</dd>
 *
 *   <dt>-U, --units=UNITS</dt>
 *
 *   <dd>The range and color scale units for the enhancement
 *   variable(s).  By default, the user must specify the values for
 *   the <b>--range</b> option in the standard units indicated in the
 *   data.  If the user prefers a different set of units to be used,
 *   they may be specified here.  Many common units are accepted (and
 *   various forms of those units), for example 'kelvin', 'celsius'
 *   and 'fahrenheit' for temperature data, 'knots', 'meters per
 *   second' or 'm/s' for windspeed, and 'mg per m^-3' or 'kg/m-3' for
 *   concentration.  For other possible unit names, see the
 *   conventions used by the <a href="https://www.google.com/search?q=udunits">Unidata
 *   UDUNITS package</a> and its <a href="https://www.google.com/search?q=udunits.txt">supported
 *   units</a> file.</dd>
 *
 * </dl>
 *
 * <h3>Color composite options:</h3>
 *
 * <dl>
 *
 *   <dt>-B, --bluerange=MIN/MAX</dt>
 *
 *   <dd>The blue component enhancement range, see <b>--redrange</b>.</dd>
 *
 *   <dt>-G, --greenrange=MIN/MAX</dt>
 *
 *   <dd>The green component enhancement range, see <b>--redrange</b>.</dd>
 *
 *   <dt>-R, --redrange=MIN/MAX</dt>
 *
 *   <dd>The red component enhancement range.  Data values are mapped
 *   to the range [0..255] using the minimum and maximum values and an
 *   enhancement function.  By default, the enhancement range is
 *   derived from the data value mean and standard deviation to form
 *   an optimal enhancement window of 1.5 standard deviation units
 *   around the mean.</dd>
 *
 *   <dt>-x, --redfunction=TYPE</dt>
 *
 *   <dd>The red component enhancement function.  Data values are
 *   mapped to the range [0..255] by the enhancement function and
 *   range, and then the pixel color is created by compositing the
 *   red, green, and blue mapped values into one 32-bit integer color
 *   value.  See the <b>--function</b> option for valid function types.
 *   By default, the red, green, and blue enhancements are linear.</dd>
 *
 *   <dt>-y, --greenfunction=TYPE</dt>
 *
 *   <dd>The green component enhancement function, see
 *   <b>--redfunction</b>.</dd>
 *
 *   <dt>-z, --bluefunction=TYPE</dt>
 *
 *   <dd>The blue component enhancement function, see
 *   <b>--redfunction</b>.</dd>
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
 *   <li> Unrecognized format </li>
 *   <li> Unrecognized color name </li>
 *   <li> Invalid palette name </li>
 *   <li> Invalid magnification center </li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>As an example of color enhancement, the following
 * command shows the rendering of AVHRR channel 2 data from a
 * CoastWatch HDF file to a PNG image, with coast and grid lines in
 * red and the default linear black to white palette.  We allow the
 * routine to calculate data statistics on channel 2 for an optimal
 * enhancement range:</p>
 * <pre>
 *   phollema$ cwrender --verbose --enhance avhrr_ch2 --coast red --grid red 
 *     2002_288_1435_n17_er.hdf 2002_288_1435_n17_er_ch2.png
 *
 *   [INFO] Reading input 2002_288_1435_n17_er.hdf
 *   [INFO] Normalizing color enhancement
 *   [INFO] Preparing data image
 *   [INFO] Rendering overlay noaa.coastwatch.render.CoastOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.LatLonOverlay
 *   [INFO] Writing output 2002_288_1435_n17_er_ch2.png
 * </pre>
 * <p>For a color composite of the same file, the following command shows
 * the rendering of AVHRR channels 1, 2, and 4 to a PNG image.  Again,
 * we allow the routine to calculate statistics for optimal
 * enhancement ranges.  Note that the final enhancement function is
 * reversed in order to map warm AVHRR channel 4 values to dark and
 * cold values to bright:</p>
 * <pre>
 *   phollema$ cwrender --verbose --composite avhrr_ch1/avhrr_ch2/avhrr_ch4
 *     --bluefunction reverse-linear --coast black --grid gray 
 *     2002_288_1435_n17_er.hdf 2002_288_1435_n17_er_ch124.png
 *
 *   [INFO] Reading input 2002_288_1435_n17_er.hdf
 *   [INFO] Normalizing red enhancement
 *   [INFO] Normalizing green enhancement
 *   [INFO] Normalizing blue enhancement
 *   [INFO] Preparing data image
 *   [INFO] Rendering overlay noaa.coastwatch.render.CoastOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.LatLonOverlay
 *   [INFO] Writing output 2002_288_1435_n17_er_ch124.png
 * </pre>
 * <p>A further example below shows the rendering of AVHRR derived
 * sea-surface-temperature data from the same file with a cloud mask
 * applied.  The color enhancement uses a blue to red color palette
 * and an explicit range from 5 to 20 degrees Celsius:</p>
 * <pre>
 *   phollema$ cwrender --verbose --enhance sst --coast white --grid white 
 *     --palette HSL256 --range 5/20 --cloud gray 2002_288_1435_n17_er.hdf
 *     2002_288_1435_n17_er_sst.png
 *
 *   [INFO] Reading input 2002_288_1435_n17_er.hdf
 *   [INFO] Preparing data image
 *   [INFO] Rendering overlay noaa.coastwatch.render.BitmaskOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.CoastOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.LatLonOverlay
 *   [INFO] Writing output 2002_288_1435_n17_er_sst.png
 * </pre>
 * <p>An example usage of the <b>--magnify</b> option is shown below to
 * create a plot of cloud masked sea-surface-temperature data off Nova
 * Scotia:</p>
 * <pre>
 *   phollema$ cwrender --verbose --enhance sst --coast white --grid white 
 *     --palette HSL256 --range 5/20 --cloud gray --magnify 43/-66/1
 *     2002_288_1435_n17_er.hdf 2002_288_1435_n17_er_sst_mag.png
 *
 *   [INFO] Reading input 2002_288_1435_n17_er.hdf
 *   [INFO] Preparing data image
 *   [INFO] Rendering overlay noaa.coastwatch.render.BitmaskOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.CoastOverlay
 *   [INFO] Rendering overlay noaa.coastwatch.render.LatLonOverlay
 *   [INFO] Writing output 2002_288_1435_n17_er_sst_mag.png
 * </pre>
 *
 * <h2>Known Bugs</h2>
 *
 * <p>When using the <b>--coast</b> option with a fill color and
 * output is to a PDF file, lakes may contain a thin stripe of land in
 * some places.  In this case, use the <b>--land</b> for land filling
 * instead.</p>
 *
 * <p>When using the <b>--coast</b> option with a fill color, map
 * projection discontinuities or swath projection edges may not be
 * filled correctly.</p>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class cwrender {

  private static final String PROG = cwrender.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 2;

  /** The fraction of values for statistics calculations. */
  private static final double STATS_FRACTION = 0.01;

  /** The standard deviation units for normalization windows. */
  private static final double STDEV_UNITS = 1.5;
  
  /** ISO date format. */
  private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ssX";

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
    Option enhanceOpt = cmd.addStringOption ('e', "enhance");
    Option compositeOpt = cmd.addStringOption ('c', "composite");
    Option coastOpt = cmd.addStringOption ('C', "coast");
    Option gridOpt = cmd.addStringOption ('g', "grid");
    Option shapeOpt = cmd.addStringOption ('H', "shape");
    Option politicalOpt = cmd.addStringOption ('p', "political");
    Option nostatesOpt = cmd.addBooleanOption ('S', "nostates");
    Option topoOpt = cmd.addStringOption ('t', "topo");
    Option bathOpt = cmd.addStringOption ('A', "bath");
    Option bitmaskOpt = cmd.addStringOption ('b', "bitmask");
    Option cloudOpt = cmd.addStringOption ('d', "cloud");
    Option landOpt = cmd.addStringOption ('L', "land");
    Option waterOpt = cmd.addStringOption ('w', "water");
    Option sizeOpt = cmd.addStringOption ('s', "size");
    Option magnifyOpt = cmd.addStringOption ('m', "magnify");
    Option nolegendsOpt = cmd.addBooleanOption ('l', "nolegends");
    Option noantialiasOpt = cmd.addBooleanOption ('a', "noantialias");
    Option paletteOpt = cmd.addStringOption ('P', "palette");
    Option palettefileOpt = cmd.addStringOption ("palettefile");
    Option palettecolorsOpt = cmd.addStringOption ("palettecolors");
    Option rangeOpt = cmd.addStringOption ('r', "range");
    Option functionOpt = cmd.addStringOption ('F', "function");
    Option missingOpt = cmd.addStringOption ('M', "missing");
    Option redrangeOpt = cmd.addStringOption ('R', "redrange");
    Option greenrangeOpt = cmd.addStringOption ('G', "greenrange");
    Option bluerangeOpt = cmd.addStringOption ('B', "bluerange");
    Option redfunctionOpt = cmd.addStringOption ('x', "redfunction");
    Option greenfunctionOpt = cmd.addStringOption ('y', "greenfunction");
    Option bluefunctionOpt = cmd.addStringOption ('z', "bluefunction");
    Option logoOpt = cmd.addStringOption ('o', "logo");
    Option groupOpt = cmd.addStringOption ('u', "group");
    Option worldfileOpt = cmd.addStringOption ('W', "worldfile");
    Option unitsOpt = cmd.addStringOption ('U', "units");
    Option enhancevectorOpt = cmd.addStringOption ('E', "enhancevector");
    Option backgroundOpt = cmd.addStringOption ('k', "background");
    Option tiffcompOpt = cmd.addStringOption ('T', "tiffcomp");
    Option indexedOpt = cmd.addBooleanOption ('i', "indexed");
    Option imagecolorsOpt = cmd.addIntegerOption ('I', "imagecolors");
    Option exprmaskOpt = cmd.addStringOption ('X', "exprmask");
    Option watermarkOpt = cmd.addStringOption ("watermark");
    Option watermarkshadowOpt = cmd.addBooleanOption ("watermarkshadow");
    Option ticklabelsOpt = cmd.addStringOption ("ticklabels");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option splitOpt = cmd.addStringOption ("split");
    Option dateOpt = cmd.addStringOption ('D', "date");    
    Option scalewidthOpt = cmd.addIntegerOption ("scalewidth");
    Option fontOpt = cmd.addStringOption ("font");
    Option fontlistOpt = cmd.addBooleanOption ("fontlist");
    Option palettelistOpt = cmd.addBooleanOption ("palettelist");
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

    // Print font list
    // ---------------
    if (cmd.getOptionValue (fontlistOpt) != null) {
      String[] families =
        GraphicsEnvironment.getLocalGraphicsEnvironment().
        getAvailableFontFamilyNames();
      for (int i = 0; i < families.length; i++)
        System.out.println (families[i]);
      ToolServices.exitWithCode (0);
      return;
    } // if

    // Print palette list
    // ------------------
    if (cmd.getOptionValue (palettelistOpt) != null) {
      try { ResourceManager.setupPalettes (false); }
      catch (IOException e) {
        LOGGER.severe ("Error initializing palettes");
        ToolServices.exitWithCode (2);
        return;
      } // catch
      List<String> paletteList = (List<String>) PaletteFactory.getPredefined();
      for (String name : paletteList)
        System.out.println (name);
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

    // Set split expression
    // --------------------
    String splitStr = (String) cmd.getOptionValue (splitOpt);
    if (splitStr != null) ToolServices.setSplitRegex (splitStr);

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
      if (ext.equalsIgnoreCase ("png"))
        format = "png";
      else if (ext.equalsIgnoreCase ("pdf"))
        format = "pdf";
      else if (ext.equalsIgnoreCase ("tif") || ext.equalsIgnoreCase ("tiff"))
        format = "tif";
      else if (ext.equalsIgnoreCase ("jpg") || ext.equalsIgnoreCase ("jpeg"))
        format = "jpg";
      else if (ext.equalsIgnoreCase ("gif"))
        format = "gif";
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
    String enhance = (String) cmd.getOptionValue (enhanceOpt);
    String composite = (String) cmd.getOptionValue (compositeOpt);
    String coast = (String) cmd.getOptionValue (coastOpt);
    String grid = (String) cmd.getOptionValue (gridOpt);
    List shapeList = cmd.getOptionValues (shapeOpt);
    String political = (String) cmd.getOptionValue (politicalOpt);
    boolean nostates = (cmd.getOptionValue (nostatesOpt) != null);
    String topo = (String) cmd.getOptionValue (topoOpt);
    String bath = (String) cmd.getOptionValue (bathOpt);
    List bitmaskList = cmd.getOptionValues (bitmaskOpt);
    String cloud = (String) cmd.getOptionValue (cloudOpt);
    String land = (String) cmd.getOptionValue (landOpt);
    String water = (String) cmd.getOptionValue (waterOpt);
    String sizeStr = (String) cmd.getOptionValue (sizeOpt);
    String magnify = (String) cmd.getOptionValue (magnifyOpt);
    boolean nolegends = (cmd.getOptionValue (nolegendsOpt) != null);
    if (format.equals ("tif")) nolegends = true;
    boolean noantialias = (cmd.getOptionValue (noantialiasOpt) != null);
    String paletteName = (String) cmd.getOptionValue (paletteOpt);
    if (paletteName == null) paletteName = "BW-Linear";
    String paletteFile = (String) cmd.getOptionValue (palettefileOpt);
    String paletteColors = (String) cmd.getOptionValue (palettecolorsOpt);
    String range = (String) cmd.getOptionValue (rangeOpt);
    String functionType = (String) cmd.getOptionValue (functionOpt);
    if (functionType == null) functionType = "linear";
    if (functionType.equals ("boolean")) {
      functionType = "step2";
      range = "0/1";
    } // if
    String missing = (String) cmd.getOptionValue (missingOpt);
    if (missing == null) missing = "black";
    String redrange = (String) cmd.getOptionValue (redrangeOpt);
    String greenrange = (String) cmd.getOptionValue (greenrangeOpt);
    String bluerange = (String) cmd.getOptionValue (bluerangeOpt);
    String redfunction = (String) cmd.getOptionValue (redfunctionOpt);
    if (redfunction == null) redfunction = "linear";
    String greenfunction = (String) cmd.getOptionValue (greenfunctionOpt);
    if (greenfunction == null) greenfunction = "linear";
    String bluefunction = (String) cmd.getOptionValue (bluefunctionOpt);
    if (bluefunction == null) bluefunction = "linear";
    String logo = (String) cmd.getOptionValue (logoOpt);
    if (logo == null) logo = "noaa3d";
    String group = (String) cmd.getOptionValue (groupOpt);
    String worldfile = (String) cmd.getOptionValue (worldfileOpt);
    if (worldfile != null) nolegends = true;
    String enhancevector = (String) cmd.getOptionValue (enhancevectorOpt);
    if (enhancevector == null) enhancevector = "uvcomp/arrow";
    String units = (String) cmd.getOptionValue (unitsOpt);
    String background = (String) cmd.getOptionValue (backgroundOpt);
    if (background == null) background = "white";
    String tiffcomp = (String) cmd.getOptionValue (tiffcompOpt);
    if (tiffcomp == null) tiffcomp = "none";
    Integer imagecolorsObj = (Integer) cmd.getOptionValue (imagecolorsOpt);
    int imagecolors  = 
      (imagecolorsObj == null ? 0 : imagecolorsObj.intValue());
    boolean indexed = (cmd.getOptionValue (indexedOpt) != null);
    if (indexed) imagecolors = 256;
    List exprmaskList = cmd.getOptionValues (exprmaskOpt);
    String watermark = (String) cmd.getOptionValue (watermarkOpt);
    boolean watermarkshadow = (cmd.getOptionValue (watermarkshadowOpt) != null);
    String ticklabels = (String) cmd.getOptionValue (ticklabelsOpt);
    String date = (String) cmd.getOptionValue (dateOpt);
    Integer scalewidthObj = (Integer) cmd.getOptionValue (scalewidthOpt);
    int scalewidth = (scalewidthObj == null ? 90 : scalewidthObj.intValue());
    String fontStr = (String) cmd.getOptionValue (fontOpt);
    if (fontStr == null) fontStr = "Dialog/plain/9";

    try {

      // Open input file
      // ---------------
      VERBOSE.info ("Reading input " + input);
      EarthDataReader reader = EarthDataReaderFactory.create (input);
      EarthDataInfo info = reader.getInfo();
      EarthTransform2D trans2d = (EarthTransform2D) info.getTransform();

      // Create color lookup
      // -------------------
      ColorLookup lookup = new ColorLookup();

      // Create color enhancement
      // ------------------------
      EarthDataView view = null;
      if (enhance != null) {

        // Create palette from specified colors
        // ------------------------------------
        Palette palette = null;
        if (paletteColors != null) {
          String[] colorStringArray = paletteColors.split (ToolServices.getSplitRegex());
          int colors = colorStringArray.length;
          int[] rgb = new int[colors];
          for (int i = 0; i < colors; i++) {
            rgb[i] = lookup.convert (colorStringArray[i]).getRGB();
          } // for
          IndexColorModel model = new IndexColorModel (8, colors,
            rgb, 0, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
          palette = new Palette ("palette", model);
        } // if

        // Create palette from user file
        // -----------------------------
        else if (paletteFile != null) {
          palette = PaletteFactory.create (new File (paletteFile));
        } // if

        // Get palette from palette name
        // -----------------------------
        else {
          ResourceManager.setupPalettes (false);
          List paletteList = PaletteFactory.getPredefined();
          if (!paletteList.contains (paletteName)) {
            LOGGER.severe ("Palette '" + paletteName + "' not found");
            ToolServices.exitWithCode (2);
            return;
          } // if
          palette = PaletteFactory.create (paletteName);
        } // else

        // Get enhancement range
        // ---------------------
        double min, max;
        if (range != null) {
          String[] rangeArray = range.split (ToolServices.getSplitRegex());
          if (rangeArray.length != 2) {
            LOGGER.severe ("Invalid range '" + range + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if
          min = Double.parseDouble (rangeArray[0]);
          max = Double.parseDouble (rangeArray[1]);
        } // if
        else { min = 0; max = 1; }

        // Create enhancement function
        // ---------------------------
        EnhancementFunction function = EnhancementFunctionFactory.create (
          functionType, new double[] {min, max});

        // Check for vector/scalar enhancement
        // -----------------------------------
        String[] enhanceArray = enhance.split (ToolServices.getSplitRegex());
        if (enhanceArray.length > 2) {
          LOGGER.severe ("Invalid enhancement '" + enhance + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if

        // Create scalar enhancement view
        // ------------------------------
        if (enhanceArray.length == 1) {
          Grid gridVar = (Grid) reader.getVariable (enhance);
          if (units != null) gridVar.convertUnits (units);
          ColorEnhancement enhancement = new ColorEnhancement (trans2d,
            gridVar, palette, function);
          enhancement.setMissingColor (lookup.convert (missing));
          view = enhancement;         
        } // if

        // Create vector enhancement view
        // ------------------------------
        else {

          // Check vector array
          // ------------------
          String[] vectorArray = 
            enhancevector.split (ToolServices.getSplitRegex());
          if (vectorArray.length < 2) {
            LOGGER.severe ("Invalid vector specification '" + enhancevector + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if

          // Get vector style
          // ----------------
          String style = vectorArray[0];
          if (!style.equals ("magdir") && !style.equals ("uvcomp")) {
            LOGGER.severe ("Invalid vector style '" + style + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if

          // Get vector symbol
          // -----------------
          String symbol = vectorArray[1];
          if (!symbol.equals ("barb") && !symbol.equals ("arrow")) {
            LOGGER.severe ("Invalid vector symbol '" + symbol + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if

          // Get vector size
          // ---------------
          int symbolSize = (vectorArray.length == 3 ? 
            Integer.parseInt (vectorArray[2]) : 10);

          // Get vector variables
          // --------------------
          Grid var1 = (Grid) reader.getVariable (enhanceArray[0]);
          Grid var2 = (Grid) reader.getVariable (enhanceArray[1]);

          // Convert units
          // -------------
          if (units != null) {
            var1.convertUnits (units);
            if (style.equals ("uvcomp")) var2.convertUnits (units);
          } // if

          // Create data source
          // ------------------
          PointFeatureSource pointSource = new GriddedPointGenerator (
            new Grid[] {var1, var2}, trans2d);

          // Create wind barb symbol
          // -----------------------
          DirectionSymbol pointSymbol = null;
          if (symbol.equals ("barb")) {

            // Detect wind units
            // -----------------
            Unit unit = UnitFactory.create (var1.getUnits());
            int windUnits = -1;
            if (unit.equals (UnitFactory.create ("knots")))
              windUnits = WindBarbSymbol.SPEED_KNOTS;
            else if (unit.equals (UnitFactory.create ("m/s")))
              windUnits = WindBarbSymbol.SPEED_METERS_PER_SECOND;
            else {
              LOGGER.severe ("Cannot draw wind barbs with units in '" + var1.getUnits() + "'");
              ToolServices.exitWithCode (2);
              return;
            } // else

            // Create symbol
            // -------------
            if (style.equals ("magdir")) {
              pointSymbol = new ColorWindBarbSymbol (0, 1, windUnits, trans2d, 
                palette, function);
            } // if
            else {
              LOGGER.severe ("U,V component mode not currently supported for drawing wind barbs");
              ToolServices.exitWithCode (2);
              return;
            } // else

          } // if

          // Create arrow symbol
          // -------------------
          else if (symbol.equals ("arrow")) {
            if (style.equals ("uvcomp"))
              pointSymbol = new ColorArrowSymbol (0, 1, palette, function);
            else if (style.equals ("magdir"))
              pointSymbol = new ColorArrowSymbol (0, 1, trans2d, palette, 
                function);
          } // else if

          // Set symbol size
          // ---------------
          pointSymbol.setSize (symbolSize);

          // Set direction from flag
          // -----------------------
          if (style.equals ("magdir")) {
            String convention = 
              (String) var1.getMetadataMap().get ("direction_convention");
            if (convention == null || convention.equals ("DirectionIsFrom"))
              pointSymbol.setDirectionIsFrom (true);
          } // if

          // Create view
          // -----------
          ColorPointEnhancement enhancement = new ColorPointEnhancement (
            new PointFeatureOverlay (pointSymbol, pointSource), var1.getName(),
            var1.getUnits(), trans2d);
          enhancement.setMissingColor (lookup.convert (missing));
          enhancement.setBackground (lookup.convert (background));
          view = enhancement;         

        } // else

      } // if

      // Create color composite
      // ----------------------
      else if (composite != null) {

        // Get variables
        // -------------
        String[] compositeArray = composite.split (ToolServices.getSplitRegex());
        if (compositeArray.length != 3) {
          LOGGER.severe ("Composite '" + composite + "' must contain 3 variables");
          ToolServices.exitWithCode (2);
          return;
        } // if
        Grid[] grids = new Grid[3]; 
        for (int i = 0; i < 3; i++)
          grids[i] = (Grid) reader.getVariable (compositeArray[i]);

        // Get enhancement ranges
        // ----------------------
        EnhancementFunction[] funcs = new EnhancementFunction[3];
        String[] ranges = new String[] {redrange, greenrange, bluerange};
        double[] max = new double[3];
        double[] min = new double[3];
        for (int i = 0; i < 3; i++) {
          if (ranges[i] != null) {
            String[] rangeArray = ranges[i].split (ToolServices.getSplitRegex());
            if (rangeArray.length != 2) {
              LOGGER.severe ("Invalid range '" + ranges[i] + "'");
              ToolServices.exitWithCode (2);
              return;
            } // if
            min[i] = Double.parseDouble (rangeArray[0]);
            max[i] = Double.parseDouble (rangeArray[1]);
          } // if
          else { min[i] = 0; max[i] = 1; }
        } // for

        // Get enhancement functions
        // -------------------------
        EnhancementFunction[] functions = new EnhancementFunction[3];
        String[] functionTypes = new String[] {redfunction, greenfunction,
          bluefunction};
        for (int i = 0; i < 3; i++) {
          functions[i] = EnhancementFunctionFactory.create (functionTypes[i], 
            new double[] {min[i], max[i]});
        } // for

        // Create view
        // -----------
        view = new ColorComposite (trans2d, grids, functions);

      } // else if

      // Data view type not specified
      // ----------------------------
      else {
        LOGGER.severe ("Must specify --enhance or --composite");
        ToolServices.exitWithCode (2);
        return;
      } // else

      // Get font
      // --------
      String[] fontArray = fontStr.split (ToolServices.getSplitRegex());
      if (fontArray.length < 1 || fontArray.length > 3) {
        LOGGER.severe ("Invalid font '" + fontStr + "'");
        ToolServices.exitWithCode (2);
        return;
      } // if
      String family = fontArray[0];
      String fontStyleStr = (fontArray.length >= 2 ? fontArray[1] : "plain");
      int fontStyle = 0;
      if (fontStyleStr.equals ("plain")) fontStyle = Font.PLAIN;
      else if (fontStyleStr.equals ("bold")) fontStyle = Font.BOLD;
      else if (fontStyleStr.equals ("italic")) fontStyle = Font.ITALIC;
      else if (fontStyleStr.equals ("bold-italic")) fontStyle = Font.BOLD + Font.ITALIC;
      else {
        LOGGER.severe ("Invalid font style '" + fontStyleStr + "'");
        ToolServices.exitWithCode (2);
        return;
      } // else
      String fontSizeStr = (fontArray.length == 3 ? fontArray[2] : "9");
      int fontSize = Integer.parseInt (fontSizeStr);
      Font font = new Font (family, fontStyle, fontSize);

      // Modify color scale legend
      // -------------------------
      Legend legend = view.getLegend();
      if (legend != null && legend instanceof DataColorScale) {
        DataColorScale scale = (DataColorScale) legend;
        if (ticklabels != null) {
          String[] ticklabelArray = ticklabels.split (ToolServices.getSplitRegex());
          scale.setTickLabels (ticklabelArray);
        } // if
        else if (scalewidth != 0) {
          legend.setPreferredSize (new Dimension (scalewidth, 0));
        } // else if
      } // if

      // Add cloud overlay
      // -----------------
      if (cloud != null) {
        Grid cloudVar = (Grid) reader.getVariable ("cloud");
        view.addOverlay (new BitmaskOverlay (lookup.convert (cloud), 
          cloudVar, 0xffffffff));
      } // if

      // Add bitmask overlays
      // --------------------
      if (bitmaskList.size() != 0) {
        for (Iterator iter = bitmaskList.iterator(); iter.hasNext();) {
          String bitmask = (String) iter.next();

          // Get bitmask parameters
          // ----------------------
          String[] bitmaskArray = bitmask.split (ToolServices.getSplitRegex());
          if (bitmaskArray.length != 3) {
            LOGGER.severe ("Invalid bitmask parameters '" + bitmask + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if
          String maskVarName = bitmaskArray[0];
          int maskVal = 0;
          try { maskVal = Integer.decode(bitmaskArray[1]).intValue(); }
          catch (NumberFormatException e) { 
            LOGGER.severe ("Invalid bitmask value '" + bitmaskArray[1] + "'");
            ToolServices.exitWithCode (2);
            return;
          } // catch
          Color maskColor = lookup.convert (bitmaskArray[2]);

          // Add bitmask to overlays
          // -----------------------
          Grid maskVar = (Grid) reader.getVariable (maskVarName);
          view.addOverlay (new BitmaskOverlay (maskColor, maskVar, maskVal));

        } // for
      } // if

      // Add expression mask overlays
      // ----------------------------
      if (exprmaskList.size() != 0) {
        for (Iterator iter = exprmaskList.iterator(); iter.hasNext();) {
          String exprmask = (String) iter.next();

          // Get expression mask parameters
          // ------------------------------
          int index = exprmask.lastIndexOf ("/");
          if (index == -1) {
            LOGGER.severe ("Invalid expression mask parameters '" + exprmask + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if
          String expression = exprmask.substring (0, index);
          Color maskColor = lookup.convert (exprmask.substring (index+1));

          // Add expression mask to overlays
          // -------------------------------
          List gridNameList = reader.getAllGrids();
          view.addOverlay (new ExpressionMaskOverlay (maskColor, reader, 
            gridNameList, expression));

        } // for
      } // if

      // Add water overlay
      // -----------------
      if (water != null) {
        Grid graphicsVar = (Grid) reader.getVariable ("graphics");
        BitmaskOverlay waterOverlay = new BitmaskOverlay (
          lookup.convert (water), graphicsVar, 0x08);
        waterOverlay.setInverse (true);
        view.addOverlay (waterOverlay);
      } // if

      // Add bathymetric contours
      // ------------------------
      if (bath != null) {
        String[] bathArray = bath.split (ToolServices.getSplitRegex());
        Color bathColor = lookup.convert (bathArray[0]);
        int[] bathLevels;
        if (bathArray.length == 1)
          bathLevels = TopographyOverlay.BATH_LEVELS;
        else {
          bathLevels = new int[bathArray.length-1];
          for (int i = 0; i < bathLevels.length; i++)
            bathLevels[i] = -1 * Integer.parseInt (bathArray[i+1]);
        } // else
        TopographyOverlay bathOverlay = new TopographyOverlay (bathColor);
        bathOverlay.setLevels (bathLevels);
        view.addOverlay (bathOverlay);
      } // if

      // Add land overlay
      // ----------------
      if (land != null) {
        Grid graphicsVar = (Grid) reader.getVariable ("graphics");
        view.addOverlay (new BitmaskOverlay (lookup.convert (land), 
          graphicsVar, 0x08));
      } // if

      // Add coast lines
      // ---------------
      if (coast != null) {
        String[] coastArray = coast.split (ToolServices.getSplitRegex());
        Color lineColor = lookup.convert (coastArray[0]);
        Color fillColor = (coastArray.length == 1 ? null : 
          lookup.convert (coastArray[1]));
        CoastOverlay coastOverlay = new CoastOverlay (lineColor);
        coastOverlay.setFillColor (fillColor);
        view.addOverlay (coastOverlay);
      } // if

      // Add political boundaries
      // ------------------------
      if (political != null) {
        Color color = lookup.convert (political); 
        PoliticalOverlay politicalOverlay = new PoliticalOverlay (color);
        politicalOverlay.setState (!nostates);
        view.addOverlay (politicalOverlay);
      } // if

      // Add topographic contours
      // ------------------------
      if (topo != null) {
        String[] topoArray = topo.split (ToolServices.getSplitRegex());
        Color topoColor = lookup.convert (topoArray[0]);
        int[] topoLevels;
        if (topoArray.length == 1)
          topoLevels = TopographyOverlay.TOPO_LEVELS;
        else {
          topoLevels = new int[topoArray.length-1];
          for (int i = 0; i < topoLevels.length; i++)
            topoLevels[i] = Integer.parseInt (topoArray[i+1]);
        } // else
        TopographyOverlay topoOverlay = new TopographyOverlay (topoColor);
        topoOverlay.setLevels (topoLevels);
        view.addOverlay (topoOverlay);
      } // if

      // Add shapes
      // ----------
      if (shapeList.size() != 0) {
        for (Iterator iter = shapeList.iterator(); iter.hasNext();) {
          String shape = (String) iter.next();
        
          // Get shape parameters
          // --------------------
          String[] shapeArray = shape.split (ToolServices.getSplitRegex());
          if (shapeArray.length < 2) {
            LOGGER.severe ("Invalid shape parameters '" + shape + "'");
            ToolServices.exitWithCode (2);
            return;
          } // if
          URL url = new File (shapeArray[0]).toURI().toURL();
          Color lineColor = lookup.convert (shapeArray[1]);
          Color fillColor = (shapeArray.length == 2 ? null : 
            lookup.convert (shapeArray[2]));

          // Add shape overlay to view
          // -------------------------
          ESRIShapefileReader shapeReader = new ESRIShapefileReader (url);
          EarthDataOverlay shapeOverlay = shapeReader.getOverlay();
          if (shapeOverlay instanceof PolygonOverlay)
            ((PolygonOverlay) shapeOverlay).setFillColor (fillColor);
          shapeOverlay.setColor (lineColor);
          view.addOverlay (shapeOverlay);

        } // for
      } // if

      // Add grid lines
      // --------------
      if (grid != null) {
        LatLonOverlay gridOverlay = new LatLonOverlay (lookup.convert (grid));
        gridOverlay.setFont (font.deriveFont (font.getSize() + 3.0f));
        view.addOverlay (gridOverlay);
      } // if

      // Add overlay group
      // -----------------
      if (group != null) {
        OverlayGroupManager groupManager = ResourceManager.getOverlayManager();
        groupManager.setDataSource (reader, reader.getAllVariables());
        List overlayList = groupManager.loadGroup (group);
        for (int i = overlayList.size()-1; i >= 0; i--) {
          EarthDataOverlay overlay = (EarthDataOverlay) overlayList.get (i);
          overlay.setLayer (0);
          view.addOverlay (overlay);
        } // for
      } // if

      // Get plot size
      // -------------
      boolean isFullSize = (sizeStr != null && sizeStr.equals ("full"));
      if (magnify != null && isFullSize) {
        LOGGER.severe ("Cannot specify full size with magnification parameters");
        ToolServices.exitWithCode (2);
        return;
      } // if
      int size = 512;
      if (isFullSize) { 
        int[] dims = trans2d.getDimensions();
        size = Math.max (dims[Grid.ROWS], dims[Grid.COLS]);
      } // if
      else if (sizeStr != null) {
        size = Integer.parseInt (sizeStr);
      } // else

      // Magnify view
      // ------------
      if (magnify != null) {

        // Get magnification parameters
        // ----------------------------
        String[] magnifyArray = magnify.split (ToolServices.getSplitRegex());
        if (magnifyArray.length != 3) {
          LOGGER.severe ("Invalid magnification parameters '" + magnify + "'");
          ToolServices.exitWithCode (2);
          return;
        } // if

        // Check magnification parameters
        // ------------------------------
        double lat = Double.parseDouble (magnifyArray[0]);
        if (lat < -90 || lat > 90) {
          LOGGER.severe ("Invalid magnification latitude: " + lat);
          ToolServices.exitWithCode (2);
          return;
        } // if
        double lon = Double.parseDouble (magnifyArray[1]);
        if (lon < -180 || lon > 180) {
          LOGGER.severe ("Invalid magnification longitude: " + lon);
          ToolServices.exitWithCode (2);
          return;
        } // if
        EarthLocation center = new EarthLocation (lat, lon,trans2d.getDatum());
        double factor = Double.parseDouble (magnifyArray[2]);

        // Perform view magnification
        // --------------------------
        DataLocation loc = trans2d.transform (center);
        if (loc == null) {
          LOGGER.severe ("Magnification center " + center + " does not transform to a valid data location");
          ToolServices.exitWithCode (2);
          return;
        } // if
        view.magnify (loc, factor);
        view.setSize (new Dimension (size, size));

      } // if
      else
        view.resizeMaxAspect (size);

      // Add watermark overlay
      // ---------------------
      if (watermark != null) {
        String[] watermarkArray = watermark.split (ToolServices.getSplitRegex());
        String watermarkText = watermarkArray[0];
        Color watermarkColor = (watermarkArray.length >= 2 ?
          lookup.convert (watermarkArray[1]) : new Color (0x80ffffff, true));
        int watermarkSize = (watermarkArray.length >= 3 ?
          Integer.parseInt (watermarkArray[2]) : 50);
        int watermarkAngle = (watermarkArray.length >= 4 ?
          Integer.parseInt (watermarkArray[3]) : 0);
        TextOverlay watermarkOverlay = new TextOverlay (watermarkColor);
        Font watermarkFont = new Font ("Arial", Font.BOLD, watermarkSize);
        DataLocation baseLocation = view.getCenter();
        Point2D basePoint = new Point2D.Double (baseLocation.get (Grid.ROWS),
          baseLocation.get (Grid.COLS));
        TextElement watermarkElement = new TextElement (watermarkText, watermarkFont,
          basePoint, new double[] {0.5, 0.5}, watermarkAngle);
        watermarkOverlay.addElement (watermarkElement);
        if (watermarkshadow) watermarkOverlay.setTextDropShadow (true);
        view.addOverlay (watermarkOverlay);
      } // if

      // Normalize view color enhancement
      // --------------------------------
      if (view instanceof ColorEnhancement && range == null) {
        ColorEnhancement colorEnhance = (ColorEnhancement) view;
        VERBOSE.info ("Normalizing color enhancement");
        colorEnhance.normalize (STDEV_UNITS);
        double[] funcRange = colorEnhance.getFunction().getRange();
        VERBOSE.info ("Normalized range = " + Arrays.toString (funcRange));
      } // if

      // Normalize view color composite
      // ------------------------------
      else if (view instanceof ColorComposite) {
        ColorComposite colorComp = (ColorComposite) view;
        int[] components = new int[] {ColorComposite.RED, ColorComposite.GREEN,
          ColorComposite.BLUE};
        String[] compNames = new String[] {"red", "green", "blue"};
        String[] compRangeVars = new String[] {redrange, greenrange, bluerange};
        for (int i = 0; i < 3; i++) {
          if (compRangeVars[i] == null) {
            VERBOSE.info ("Normalizing " + compNames[i] + " component");
            colorComp.normalize (components[i], STDEV_UNITS);
            EnhancementFunction func = colorComp.getFunctions()[components[i]];
            double[] funcRange = func.getRange();
            VERBOSE.info ("Normalized range = " + Arrays.toString (funcRange));
          } // if
        } // for
      } // else if

      // Set verbose mode
      // ----------------
      view.setVerbose (verbose);

      // Get logo
      // --------
      IconElement logoIcon = null;
      if (!nolegends) logoIcon = IconElementFactory.create (logo);

      // Set custom start/end date
      // -------------------------
      if (date != null) {
        String[] dateArray = date.split (ToolServices.getSplitRegex());
        ArrayList<TimePeriod> periodList = new ArrayList<TimePeriod>();
        if (dateArray.length > 2) {
          LOGGER.severe ("Invalid start/end date specification: " + date);
          ToolServices.exitWithCode (2);
          return;
        } // if
        for (String dateStr : dateArray) {
          Date dateValue = null;
          try { dateValue = DateFormatter.parseDate (dateStr, ISO_DATE_FMT); }
          catch (ParseException e) { }
          if (dateValue == null) {
            LOGGER.severe ("Error parsing date: " + dateStr);
            ToolServices.exitWithCode (2);
            return;
          } // if
          periodList.add (new TimePeriod (dateValue, 0));
        } // for
        info.setTimePeriods (periodList);
      } // if

      // Write image
      // -----------
      EarthImageWriter writer = EarthImageWriter.getInstance();
      writer.setFont (font);
      writer.write (view, info, verbose, !nolegends, logoIcon, !noantialias,
        new File (output), format, worldfile, tiffcomp, imagecolors);

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

    UsageInfo info = new UsageInfo ("cwrender");

    info.func ("Performs earth data visualization");

    info.param ("input", "Input data file");
    info.param ("output", "Output image file");

    info.section ("Rendering type");
    info.option ("-c, --composite=R/G/B", "Create red/green/blue composite");
    info.option ("-e, --enhance=VAR1[/VAR2]", "Create color enhancement");

    info.section ("General");
    info.option ("-h, --help", "Show help message");
    info.option ("-v, --verbose", "Print verbose messages");
    info.option ("--version", "Show version information");
    info.option ("--split=EXPRESSION", "Set command parameter split expression");

    info.section ("Content and format");
    info.option ("-a, --noantialias", "Do not smooth lines and fonts");
    info.option ("-D, --date=STARTDATE[/ENDDATE]", "Set legend date(s)");
    info.option ("--font=FAMILY[/STYLE[/SIZE]]", "Set font properties");
    info.option ("--fontlist", "Print system fonts");
    info.option ("-f, --format=TYPE", "Set output format");
    info.option ("-i, --indexed", "Set image colors to 256");
    info.option ("-I, --imagecolors=NUMBER", "Set number of image colors");
    info.option ("-l, --nolegends", "Do not draw legends");
    info.option ("-m, --magnify=LAT/LON/FACTOR", "Center and magnify location");
    info.option ("-o, --logo=NAME", "Set legend logo");
    info.option ("-s, --size=PIXELS | full", "Set maximum data view size");
    info.option ("-T, --tiffcomp=TYPE", "Set TIFF compression type");
    info.option ("-W, --worldfile=FILE", "Write georeferencing world file");

    info.section ("Overlays");
    info.option ("-A, --bath=COLOR[/LEVEL1/LEVEL2/...] ", "Render bathymetric contours");
    info.option ("-b, --bitmask=VAR/MASK/COLOR", "Render variable bitmask");
    info.option ("-C, --coast=COLOR[/FILL]", "Render coastlines");
    info.option ("-d, --cloud=COLOR", "Render cloud mask");
    info.option ("-g, --grid=COLOR", "Render lat/lon grid");
    info.option ("-H, --shape=FILE/COLOR[/FILL]", "Render shape file");
    info.option ("-L, --land=COLOR", "Render land mask");
    info.option ("-p, --political=COLOR", "Render politial boundaries");
    info.option ("-S, --nostates", "Do not render state boundaries");
    info.option ("-t, --topo=COLOR[/LEVEL1/LEVEL2/...]", "Render topographic contours");
    info.option ("-u, --group=GROUP", "Render overlay group from CDAT");
    info.option ("-w, --water=COLOR", "Render water mask");
    info.option ("-X, --exprmask=EXPRESSION/COLOR", "Render expression mask");
    info.option ("--watermark=TEXT[/COLOR[/SIZE[/ANGLE]]]", "Render watermark");
    info.option ("--watermarkshadow", "Add drop shadow to watermark");
 
    info.section ("Color enhancement");
    info.option ("-E, --enhancevector=STYLE/SYMBOL[/SIZE]", "Set vector style");
    info.option ("-F, --function=TYPE", "Set color enhancement function type");
    info.option ("-k, --background=COLOR", "Set vector background color");
    info.option ("-M, --missing=COLOR", "Set missing data color");
    info.option ("-P, --palette=NAME", "Set color enhancment palette");
    info.option ("--palettefile=FILE", "Use palette XML file");
    info.option ("--palettecolors=COLOR1[/COLOR2[/COLOR3...]]", "Use palette colors");
    info.option ("--palettelist", "Print palette names");
    info.option ("-r, --range=MIN/MAX", "Set color enhancement range");
    info.option ("--scalewidth=PIXELS", "Set color scale width");
    info.option ("--ticklabels=LABEL1[/LABEL2[/LABEL3/...]]", "Set color scale tick labels");
    info.option ("-U, --units=UNITS", "Set range and color scale units");

    info.section ("Color composite");
    info.option ("-B, --bluerange=MIN/MAX", "Set blue component range");
    info.option ("-G, --greenrange=MIN/MAX", "Set green component range");
    info.option ("-R, --redrange=MIN/MAX", "Set red component range");
    info.option ("-x, --redfunction=TYPE", "Set red component function type");
    info.option ("-y, --greenfunction=TYPE", "Set green component function type");
    info.option ("-z, --bluefunction=TYPE", "Set blue component function type");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwrender () { }

  ////////////////////////////////////////////////////////////

} // cwrender class

////////////////////////////////////////////////////////////////////////

