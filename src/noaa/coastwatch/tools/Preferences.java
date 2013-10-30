////////////////////////////////////////////////////////////////////////
/*
     FILE: Preferences.java
  PURPOSE: To manage a set of user preferences.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/19
  CHANGES: 2005/02/04, PFH, added general settings and enhancement functions
           2006/11/03, PFH, added units support

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// -------
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import noaa.coastwatch.render.*;

/**
 * The <code>Preferences</code> class handles preferences set by the
 * user for CoastWatch tool operations.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class Preferences
  implements Cloneable { 

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
  private HashMap enhancementMap;

  /** The map of variable name to units. */
  private HashMap unitsMap;

  /** 
   * The Earth location degrees flag, true to display Earth locations
   * as decimal degrees, false to display as degrees/minutes/seconds.
   */
  private boolean earthLocDegrees = true;

  ////////////////////////////////////////////////////////////

  /** Gets the map of variable name to units. */
  public Map getUnitsMap() { return ((Map) unitsMap.clone()); }

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
    
    return ((String) unitsMap.get (varName));

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

  /** Gets the Earth location in decimal degrees flag. */
  public boolean getEarthLocDegrees () { return (earthLocDegrees); }

  ////////////////////////////////////////////////////////////

  /** Sets the Earth location in decimal degrees flag. */
  public void setEarthLocDegrees (boolean flag) { earthLocDegrees = flag; }

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    try {
      Preferences prefs = (Preferences) super.clone();
      prefs.enhancementMap = new LinkedHashMap();
      for (Iterator iter = enhancementMap.values().iterator(); 
        iter.hasNext(); ) {
        ColorEnhancementSettings settings = 
          (ColorEnhancementSettings) iter.next();
        prefs.enhancementMap.put (settings.getName(), settings.clone());
      } // for
      prefs.unitsMap = (HashMap) unitsMap.clone();
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
  public List getEnhancementVariables () {

    return (new ArrayList (enhancementMap.keySet()));

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

    return ((ColorEnhancementSettings) enhancementMap.get (variableName));

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
    for (Iterator iter = enhancementMap.values().iterator(); iter.hasNext();) {
      ColorEnhancementSettings settings = 
        (ColorEnhancementSettings) iter.next();
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
    enhancementMap = new LinkedHashMap();
    unitsMap = new HashMap();

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
        ColorEnhancementSettings settings = new ColorEnhancementSettings (
          variableName, PaletteFactory.create (paletteName), function);
        enhancementMap.put (variableName, settings);
        if (!units.equals ("")) unitsMap.put (variableName, units);

      } // if

      // Get general setting
      // -------------------
      else if (qName.equals ("general")) {
        String item = attributes.getValue ("item");
        String value = attributes.getValue ("value");
        if (item.equals ("earthLocationDisplay"))
          earthLocDegrees = value.equals ("dd");
      } // if

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
