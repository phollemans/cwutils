////////////////////////////////////////////////////////////////////////
/*

     File: Preferences.java
   Author: Peter Hollemans
     Date: 2004/05/19

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
// -------
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.EnhancementFunctionFactory;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.IconElementFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.logging.Logger;

/**
 * The <code>Preferences</code> class handles preferences set by the
 * user for CoastWatch tool operations.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class Preferences
  implements Cloneable { 

  private static final Logger LOGGER = Logger.getLogger (Preferences.class.getName());

  // Constants
  // ---------

  /** The preferences DTD URL. */
  private static final String DTD_URL = 
    "http://coastwatch.noaa.gov/xml/preferences.dtd";

  /** The preferences DTD local resource. */
  private static final String DTD_RESOURCE = "preferences.dtd";

  // Variables
  // ---------

  /** The map of variable name to color enhancement settings. */
  private HashMap<String, ColorEnhancementSettings> enhancementMap;

  /** The map of variable name to units. */
  private HashMap<String, String> unitsMap;

  /** 
   * The earth location degrees flag, true to display earth locations
   * as decimal degrees, false to display as degrees/minutes/seconds.
   */
  private boolean earthLocDegrees = true;

  /** The Java VM heap size in megabytes. */
  private int heapSize = 2048;
  
  /** The data tile cache size in megabytes. */
  private int cacheSize = 512;

  /** The plot legend logo. */
  private String logo = IconElementFactory.getInstance().getDefaultIcon();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the legend logo. 
   * 
   * @since 3.8.1
   */
  public String getLogo () { return (logo); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the legend logo. 
   * 
   * @param logo the new logo to use.
   * 
   * @since 3.8.1
   */
  public void setLogo (String logo) { this.logo = logo; }

  ////////////////////////////////////////////////////////////

  /** Gets the map of variable name to units. */
  public Map<String, String> getUnitsMap() { return ((Map<String, String>) unitsMap.clone()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the preferred units for a variable.
   * 
   * @param varName the variable name to set units for.
   * @param units the new units string for the variable.
   */
  public void setUnits (String varName, String units) { 
    
    unitsMap.put (varName, units); 

  }  // setUnits

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the preferred units for a variable.
   * 
   * @param varName the variable name to get units for.
   *
   * @return the units string for the variable, or null if there are
   * no preferred units.
   */
  public String getUnits (String varName) {
    
    return (unitsMap.get (varName));

  }  // getUnits

  ////////////////////////////////////////////////////////////

  /** 
   * Removes the preferred units for a variable.
   * 
   * @param varName the variable name to remove units for.
   */
  public void removeUnits (String varName) {
    
    unitsMap.remove (varName);

  }  // removeUnits

  ////////////////////////////////////////////////////////////

  /** Gets the earth location in decimal degrees flag. */
  public boolean getEarthLocDegrees () { return (earthLocDegrees); }

  ////////////////////////////////////////////////////////////

  /** Sets the earth location in decimal degrees flag. */
  public void setEarthLocDegrees (boolean flag) { earthLocDegrees = flag; }

  ////////////////////////////////////////////////////////////

  /** Sets the heap size in megabytes. */
  public void setHeapSize (int heapSize) { this.heapSize = heapSize; }

  ////////////////////////////////////////////////////////////

  /** Gets the heap size in megabytes. */
  public int getHeapSize () { return (heapSize); }

  ////////////////////////////////////////////////////////////

  /** Sets the tile cache size in megabytes. */
  public void setCacheSize (int cacheSize) { this.cacheSize = cacheSize; }

  ////////////////////////////////////////////////////////////

  /** Gets the tile cache size in megabytes. */
  public int getCacheSize () { return (cacheSize); }

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    try {
      Preferences prefs = (Preferences) super.clone();
      prefs.enhancementMap = new LinkedHashMap<>();
      for (var settings : enhancementMap.values()) {
        prefs.enhancementMap.put (settings.getName(), (ColorEnhancementSettings) settings.clone());
      } // for
      prefs.unitsMap = (HashMap<String, String>) unitsMap.clone();
      return (prefs);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the enhancement setting variable names.
   *
   * @return a list of variable names.
   */
  public List<String> getEnhancementVariables () {

    return (new ArrayList<String> (enhancementMap.keySet()));

  } // getEnhancementVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the enhancement settings for the specified variable.
   *
   * @param variableName the variable name for which to retrieve
   * settings.
   *
   * @return the settings, or null if no settings could be found.
   */
  public ColorEnhancementSettings getEnhancement (
    String variableName
  ) {

    return (enhancementMap.get (variableName));

  } // getEnhancement

  ////////////////////////////////////////////////////////////

  /** 
   * Adds new enhancement settings to the preferences.
   *
   * @param settings the new enhancement settings to add.
   */
  public void setEnhancement (
    ColorEnhancementSettings settings
  ) {

    enhancementMap.put (settings.getName(), settings);

  } // setEnhancement

  ////////////////////////////////////////////////////////////

  /** 
   * Removes the specified enhancement settings from the preferences.
   *
   * @param variableName the enhancement settings variable to remove.
   */
  public void removeEnhancement (
    String variableName
  ) {

    enhancementMap.remove (variableName);

  } // removeEnhancement

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the set of options to the specified output stream.
   *
   * @param output the output stream to write.
   *
   * @since 3.3.1
   */
  public void writeOptions (
    OutputStream output
  ) {

    PrintStream printStream = new PrintStream (output);
    printStream.println ("-Xmx" + heapSize + "m");
    printStream.println ("-Dcw.cache.size=" + cacheSize);
    printStream.println();

  } // writeOptions

  ////////////////////////////////////////////////////////////
  
  /** 
   * Writes this set of preferences to the specified output stream in
   * an XML format.
   *
   * @param output the output stream to write.
   */
  public void write (
    OutputStream output
  ) {

    PrintStream printStream = new PrintStream (output);

    // Write header
    // ------------
    printStream.println ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    printStream.println ("<!DOCTYPE preferences SYSTEM \"http://coastwatch.noaa.gov/xml/preferences.dtd\">\n");
    printStream.println ("<preferences>");

    // Write enhancement settings
    // --------------------------
    for (var settings : enhancementMap.values()) {
      EnhancementFunction func = settings.getFunction();
      double[] range = func.getRange();
      String varName = settings.getName();
      printStream.print ("  <enhancement ");
      printStream.print ("variable=\"" + varName + "\" ");
      printStream.print ("palette=\"" + settings.getPalette().getName() + 
        "\" ");
      printStream.print ("min=\"" + range[0] + "\" ");
      printStream.print ("max=\"" + range[1] + "\" ");
      printStream.print ("function=\"" + func.describe() + "\" ");
      if (unitsMap.containsKey (varName))
        printStream.print ("units=\"" + unitsMap.get (varName) + "\" ");
      printStream.println ("/>");
    } // for

    // Write general settings
    // ----------------------
    printStream.print ("  <general ");
    printStream.print ("item=\"earthLocationDisplay\" ");
    printStream.print ("value=\"" + (earthLocDegrees ? "dd" : "dms") + "\" ");
    printStream.println ("/>");

    printStream.print ("  <general ");
    printStream.print ("item=\"heapSize\" ");
    printStream.print ("value=\"" + heapSize + "\" ");
    printStream.println ("/>");

    printStream.print ("  <general ");
    printStream.print ("item=\"cacheSize\" ");
    printStream.print ("value=\"" + cacheSize + "\" ");
    printStream.println ("/>");

    printStream.print ("  <export ");
    printStream.print ("item=\"logo\" ");
    printStream.print ("value=\"" + logo + "\" ");
    printStream.println ("/>");

    // Write footer
    // ------------
    printStream.println ("</preferences>");
    printStream.println ("");
    printStream.println ("<!-- Last modified: " + new Date() + " -->");

  } // write

  ////////////////////////////////////////////////////////////

  /** Creates a new empty set of preferences. */
  public Preferences () {

    // Initialize
    // ----------
    enhancementMap = new LinkedHashMap<>();
    unitsMap = new HashMap<>();

  } // Preferences constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new preferences object by parsing the specified XML
   * input.
   *
   * @param input the input XML stream to read.
   *
   * @throws IOException if the input had format errors.
   */
  public Preferences (
    InputStream input
  ) throws IOException {

    // Initialize
    // ----------
    this();

    try {

      // Create parser
      // -------------
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating (true);
      SAXParser parser = factory.newSAXParser();
      PreferencesHandler handler = new PreferencesHandler();

      // Parse file
      // ----------
      parser.parse (input, handler);

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // Preferences constructor

  ////////////////////////////////////////////////////////////

  /** Handles preferences parsing events. */
  private class PreferencesHandler 
    extends DefaultHandler {

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

      // Create new enhancement
      // ----------------------
      if (qName.equals ("enhancement")) {

        // Get required attributes
        // -----------------------
        String variableName = attributes.getValue ("variable");
        String paletteName = attributes.getValue ("palette");
        double min = Double.parseDouble (attributes.getValue ("min"));
        double max = Double.parseDouble (attributes.getValue ("max"));

        // Get function
        // ------------
        String functionType = attributes.getValue ("function");
        EnhancementFunction function = EnhancementFunctionFactory.create (
          functionType, new double[] {min, max});

        // Get units
        // ---------
        String units = attributes.getValue ("units").trim();

        // Create settings
        // ---------------
        if (!PaletteFactory.getPredefined().contains (paletteName)) {
          LOGGER.fine ("Ignoring preference entry with unknown palette '" + paletteName + "'");
        } // if
        else {
          ColorEnhancementSettings settings = new ColorEnhancementSettings (
            variableName, PaletteFactory.create (paletteName), function);
          enhancementMap.put (variableName, settings);
          if (!units.equals ("")) unitsMap.put (variableName, units);
        } // else

      } // if

      // Get general setting
      // -------------------
      else if (qName.equals ("general")) {
        String item = attributes.getValue ("item");
        String value = attributes.getValue ("value");
        if (item.equals ("earthLocationDisplay"))
          earthLocDegrees = value.equals ("dd");
        else if (item.equals ("heapSize"))
          heapSize = Integer.parseInt (value);
        else if (item.equals ("cacheSize"))
          cacheSize = Integer.parseInt (value);
      } // else if

      // Get any export settings.
      else if (qName.equals ("export")) {
        String item = attributes.getValue ("item");
        String value = attributes.getValue ("value");

        if (item.equals ("logo")) {
          // We do this check because a previous version defined no defualt
          // logo in this class so the object value "null" was written
          // instead of an actual value.
          if (!value.equals ("null"))
            logo = value;
        } // if

      } // else if

      /** 
       * Other preference settings should go here.
       */

    } // startElement

    ////////////////////////////////////////////////////////////

    public void endElement (
        String uri,
        String localName,
        String qName
    ) throws SAXException {

      // Do nothing

    } // endElement

    ////////////////////////////////////////////////////////

  } // PreferencesHandler

  ////////////////////////////////////////////////////////////

} // Preferences class

////////////////////////////////////////////////////////////////////////
