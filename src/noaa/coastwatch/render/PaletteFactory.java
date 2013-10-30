////////////////////////////////////////////////////////////////////////
/*
     FILE: PaletteFactory.java
  PURPOSE: To manage a set of predefined written color palettes.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/18
  CHANGES: 2005/02/25, PFH, added chlorophyll palettes
           2005/04/28, PFH, added spectrum and wind palettes
           2006/02/03, PFH, added coral reef palettes
           2006/02/02, PFH, added step sequential 25 palette for ECN
           2012/08/11, PFH, added HSB-Cycle palette

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import noaa.coastwatch.io.*;

/**
 * The <code>PaletteFactory</code> class handles written tables of
 * predefined index color models.  Each model has a maximum of 256
 * colors and is associated with a descriptive name.  The palette file
 * has an XML format, as follows:
 * <pre>
 *   &lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;
 *   &lt;!DOCTYPE palette SYSTEM "http://coastwatch.noaa.gov/xml/palette.dtd"&gt;
 *   
 *   &lt;palette name="BW-Linear" length="256"&gt;
 *     &lt;color r="0" g="0" b="0" /&gt;
 *     &lt;color r="1" g="1" b="1" /&gt;
 *     &lt;color r="2" g="2" b="2" /&gt;
 *     ...
 *   &lt;/palette&gt;
 *</pre><p>
 *
 * Users may generate files of this form and use them as input to the
 * palette constructor.  Alternately, palettes may be created by
 * specifying the name and index color model data.  A number of predefined
 * palettes are also available by name or index:
 * <pre>
 *   0  BW-Linear
 *   1  HSL256
 *   2  RAMSDIS
 *   3  Blue-Red
 *   4  Blue-White
 *   5  Grn-Red-Blu-Wht
 *   6  Red-Temperature
 *   7  Blue-Green-Red-Yellow
 *   8  Std-Gamma-II
 *   9  Prism
 *   10 Red-Purple
 *   11 Green-White-Linear
 *   12 Grn-Wht-Exponential
 *   13 Green-Pink
 *   14 Blue-Red2
 *   15 16-Level
 *   16 Rainbow
 *   17 Steps
 *   18 Stern-Special
 *   19 Haze
 *   20 Blue-Pastel-Red
 *   21 Pastels
 *   22 Hue-Sat-Lightness-1
 *   23 Hue-Sat-Lightness-2
 *   24 Hue-Sat-Value-1
 *   25 Hue-Sat-Value-2
 *   26 Purple-Red-Stripes
 *   27 Beach
 *   28 Mac-Style
 *   29 Eos-A
 *   30 Eos-B
 *   31 Hardcandy
 *   32 Nature
 *   33 Ocean
 *   34 Peppermint
 *   35 Plasma
 *   36 Rainbow2
 *   37 Blue-Waves
 *   38 Volcano
 *   39 Waves
 *   40 Rainbow18
 *   41 Rainbow-white
 *   42 Rainbow-black
 *   43 NDVI
 *   44 GLERL-Archive
 *   45 GLERL-30-Degrees
 *   46 Chlora-1
 *   47 Chlora-anom
 *   48 Spectrum
 *   49 Wind-0-50
 *   50 CRW_SST
 *   51 CRW_SSTANOMALY
 *   52 CRW_HOTSPOT
 *   53 CRW_DHW
 *   54 StepSeq25
 *   55 HSB-Cycle
 * </pre> 
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PaletteFactory { 

  // Constants
  // ---------

  /** The table of predefined color palette names. */
  private final static String[] PREDEFINED = {
    "BW-Linear",
    "HSL256",
    "RAMSDIS",
    "Blue-Red",
    "Blue-White",
    "Grn-Red-Blu-Wht",
    "Red-Temperature",
    "Blue-Green-Red-Yellow",
    "Std-Gamma-II",
    "Prism",
    "Red-Purple",
    "Green-White-Linear",
    "Grn-Wht-Exponential",
    "Green-Pink",
    "Blue-Red2",
    "16-Level",
    "Rainbow",
    "Steps",
    "Stern-Special",
    "Haze",
    "Blue-Pastel-Red",
    "Pastels",
    "Hue-Sat-Lightness-1",
    "Hue-Sat-Lightness-2",
    "Hue-Sat-Value-1",
    "Hue-Sat-Value-2",
    "Purple-Red-Stripes",
    "Beach",
    "Mac-Style",
    "Eos-A",
    "Eos-B",
    "Hardcandy",
    "Nature",
    "Ocean",
    "Peppermint",
    "Plasma",
    "Rainbow2",
    "Blue-Waves",
    "Volcano",
    "Waves",
    "Rainbow18",
    "Rainbow-white",
    "Rainbow-black",
    "NDVI",
    "GLERL-Archive",
    "GLERL-30-Degrees",
    "Chlora-1",
    "Chlora-anom",
    "Spectrum",
    "Wind-0-50",
    "CRW_SST",
    "CRW_SSTANOMALY",
    "CRW_HOTSPOT",
    "CRW_DHW",
    "StepSeq25",
    "HSB-Cycle"
  };    

  /** The palette DTD URL. */
  private static final String DTD_URL = 
    "http://coastwatch.noaa.gov/xml/palette.dtd";

  /** The palette DTD local resource. */
  private static final String DTD_RESOURCE = "palette.dtd";

  /** The palette file extension. */
  private static final String FILE_EXTENSION = ".xml";

  // Variables
  // ---------

  /** The map of predefined palettes. */
  private static HashMap paletteMap;

  /** The list of predefined palette names. */
  private static ArrayList predefinedList;

  ////////////////////////////////////////////////////////////

  /** Gets the list of predefined palette names. */
  public static List getPredefined () { 

    return ((List) predefinedList.clone()); 

  } // getPredefined

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the specified palette to the list of predefined palettes.
   * 
   * @param palette the palette to add to the list.
   */
  public static void addPredefined (
    Palette palette
  ) { 

    predefinedList.add (palette.getName());
    paletteMap.put (palette.getName(), palette);

  } // addPredefined

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the palettes in the specified directory to the list of
   * predefined palettes.  Palettes are identified as any file ending
   * in '.xml'.  Thus, palettes should be kept in a directory by
   * themselves to use this method.
   * 
   * @param paletteDir the palette directory to list.
   *
   * @throws IOException if an error occurred reading the directory or
   * parsing palette file contents.
   */
  public static void addPredefined (
    File paletteDir
  ) throws IOException { 

    File[] files = paletteDir.listFiles (new FilenameFilter () {
        public boolean accept (File dir, String name) {
          return (name.endsWith (FILE_EXTENSION));
        } // accept
      });
    for (int i = 0; i < files.length; i++) {
      Palette palette = create (new FileInputStream (files[i]));
      addPredefined (palette);
    } // for

  } // addPredefined

  ////////////////////////////////////////////////////////////

  /** Initializes the palette hash map and list. */
  static {

    paletteMap = new HashMap();
    predefinedList = new ArrayList (Arrays.asList (PREDEFINED));

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new palette from the specified file.
   *
   * @param file the file to read.
   *
   * @return the new palette.
   *
   * @throws FileNotFoundException if the file is not valid.
   * @throws IOException if the file had input format errors.
   */
  public static Palette create (
    File file
  ) throws FileNotFoundException, IOException {

    return (create (new FileInputStream (file)));

  } // create

  ////////////////////////////////////////////////////////////

  /** Handles palette parsing events. */
  private static class PaletteHandler 
    extends DefaultHandler {

    // Variables
    // ---------

    /** The palette name. */
    private String name;

    /** The palette color model. */
    private IndexColorModel model;

    /** The buffer used to hold red data. */
    private byte[] red;

    /** The buffer used to hold green data. */
    private byte[] green;

    /** The buffer used to hold blue data. */
    private byte[] blue;

    /** The next color index to store. */
    private int index;

    ////////////////////////////////////////////////////////

    /** Gets the palette parsed by this handler. */
    public Palette getPalette () {

      return (new Palette (name, model));

    } // getPalette

    ////////////////////////////////////////////////////////

    public InputSource resolveEntity (
      String publicId, 
      String systemId
    ) {

      if (systemId.equals (DTD_URL)) {
        InputStream stream = getClass().getResourceAsStream (DTD_RESOURCE);
        return (new InputSource (stream));
      }  // if

      return (null);

    } // resolveEntity

    ////////////////////////////////////////////////////////

    public void error (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // error

    ////////////////////////////////////////////////////////

    public void fatalError (
      SAXParseException e
    ) throws SAXException {
 
      throw (new SAXException ("Line " + e.getLineNumber() + ": " + 
        e.getMessage())); 

    } // fatalError

    ////////////////////////////////////////////////////////

    public void warning (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // warning

    ////////////////////////////////////////////////////////

    public void startElement (
        String uri,
        String localName,
        String qName,
        Attributes attributes
    ) throws SAXException {

      // Start palette
      // -------------
      if (qName.equals ("palette")) {
        name = attributes.getValue ("name");
        int length = Integer.parseInt (attributes.getValue ("length"));
        red = new byte[length];
        green = new byte[length];
        blue = new byte[length];
        index = 0;
      } // if

      // Add color to palette
      // --------------------
      else if (qName.equals ("color")) {
        red[index] = (byte) Integer.parseInt (attributes.getValue ("r"));
        green[index] = (byte) Integer.parseInt (attributes.getValue ("g"));
        blue[index] = (byte) Integer.parseInt (attributes.getValue ("b"));
        index++;
      } // else if

    } // startElement

    ////////////////////////////////////////////////////////////

    public void endElement (
        String uri,
        String localName,
        String qName
    ) throws SAXException {

      // Create color model
      // ------------------
      if (qName.equals ("palette")) {
        model = new IndexColorModel (8, red.length, red, green, blue);
      } // if

    } // endElement

    ////////////////////////////////////////////////////////

  } // PaletteHandler

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new palette from an input stream.
   *
   * @param stream the input stream to read palette data from.
   *
   * @return the new palette.
   *
   * @throws IOException if the file had input format errors.
   */
  public static Palette create (
    InputStream stream
  ) throws IOException {

    try {

      // Create parser
      // -------------
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating (true);
      SAXParser parser = factory.newSAXParser();
      PaletteHandler handler = new PaletteHandler();

      // Parse file
      // ----------
      parser.parse (stream, handler);
      return (handler.getPalette());

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // create

  ////////////////////////////////////////////////////////////

  /** 
   * Constructs a new palette from a predefined palette name.  This
   * method returns the same instance of the predefined palette when
   * called multiple times with the same name.  A hash map is kept of
   * predefined palettes, and reading of palette data only occurs when
   * required.
   *
   * @param name the predefined palette name.
   *
   * @return the new palette.
   *
   * @throws RuntimeException if the predefined palette had input
   * format errors or the predefined palette is invalid.
   */
  public static Palette create (
    String name
  ) {

    // Try getting palette from map
    // ----------------------------
    Palette palette = (Palette) paletteMap.get (name);

    // Update map
    // ----------
    if (palette == null) {
      try { 
        String resource = name + FILE_EXTENSION;
        InputStream stream = 
          PaletteFactory.class.getResourceAsStream (resource);
        if (stream == null)
          throw new IOException ("Cannot find resource '" + resource + "'");
        palette = create (stream);
        paletteMap.put (name, palette);
      } // try
      catch (IOException e) {
        String error = "Error creating new palette instance for " + 
          name + ": " + e.getMessage();
        throw new RuntimeException (error);
      } // catch
    } // if

    return (palette);

  } // create

  ////////////////////////////////////////////////////////////

} // PaletteFactory class

////////////////////////////////////////////////////////////////////////
