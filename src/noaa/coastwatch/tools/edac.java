////////////////////////////////////////////////////////////////////////
/*
     FILE: edac.java
  PURPOSE: To access earth data data over a network.
   AUTHOR: Peter Hollemans
     DATE: 2006/04/07
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// --------
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import java.net.*;
import java.beans.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.validation.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import netscape.javascript.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.gui.open.*;
import noaa.coastwatch.gui.browse.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.net.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.net.CatalogQueryAgent.Entry;

/**
 * The CoastWatch Earth Data Access Client (EDAC) is a tool
 * designed for use with CoastWatch node websites for accessing
 * the various datasets and interactively viewing the data
 * online.  The tool may be deployed as a Java applet with the
 * single parameter "config" giving the URL of the XML
 * configuration file.
 *
 * @see noaa.coastwatch.tools.edac.Configuration
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public final class edac
  extends JPanel {

  // Constants
  // ---------

  /** Name of program for command line. */
  private static final String PROG = "edac";

  /** The short program name. */
  private static final String SHORT_NAME = "EDAC";

  /** The long program name. */
  private static final String LONG_NAME = 
    "CoastWatch Earth Data Access Client";

  /** The version of this browser. */
  private static final String VERSION = "1.0";

  /** The browser code author. */
  private static final String AUTHOR = "Peter.Hollemans@noaa.gov";

  /** The code version number label. */
  private static final String VERSION_LABEL = SHORT_NAME + " " + VERSION;

  /** The application window size. */
  private static final Dimension FRAME_SIZE = new Dimension (580, 660);

  /** The help window size. */
  private static final Dimension HELP_SIZE = new Dimension (450, 350);

  /** The help index file. */
  private static final String HELP_INDEX = "edac_overview.html";
  
  /** The resource used for parsing configurations. */
  private static final String CONFIG_RESOURCE = "configuration.xsd";

  /** Date format for data downloads. */
  private static final String DATE_FMT = "yyyy/MM/dd HH:mm 'UTC'";

  // Variables
  // ---------

  /** The dataset combo box. */
  private JComboBox datasetCombo;

  /** The browse panel. */
  private EarthDataBrowsePanel browsePanel;

  /** The catalog panel. */
  private EarthDataCatalogPanel catalogPanel;
  
  /** The currently selected dataset. */
  private Dataset currentDataset;

  /** The swirl icon for busy status. */
  private Swirl swirl;
  
  /** The counter for the number of busy requests (used with swirl). */
  private int busyCount;

  /** The applet object, non-null if we are running as an applet. */
  private JApplet applet;

  /** The tabbed pane for view and catalog components. */
  private JTabbedPane tabbedPane;

  /** The catalog download data button. */
  private JButton catDownloadButton;

  /** The catalog OPeNDAP button. */
  private JButton catOpendapButton;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new EDAC panel with the specified configuration. 
   *
   * @param config the configuration object to use for this panel.
   * @param applet the applet that this panel is part of or
   * null if there is no applet.
   */
  public edac (
    Configuration config,
    JApplet applet
  ) {

    // Initialize
    // ----------
    super (new BorderLayout());
    this.applet = applet;

    // Create control panel
    // --------------------
    Box controlPanel = Box.createHorizontalBox();
    this.add (controlPanel, BorderLayout.NORTH);

    // Create left panel
    // -----------------
    JPanel leftPanel = new JPanel (new FlowLayout (FlowLayout.LEFT));
    controlPanel.add (leftPanel);
    leftPanel.add (new JLabel ("Browse Dataset:"));
    datasetCombo = new JComboBox();
    for (Iterator iter = config.getDatasetList().iterator(); iter.hasNext();)
      datasetCombo.addItem ((Dataset) iter.next());
    datasetCombo.addActionListener (new DatasetListener());
    leftPanel.add (datasetCombo);
    controlPanel.add (Box.createHorizontalGlue());

    // Create right panel
    // ------------------
    JPanel rightPanel = new JPanel (new FlowLayout (FlowLayout.RIGHT));
    controlPanel.add (rightPanel);

    JButton helpButton = GUIServices.getIconButton ("general.help");
    helpButton.setToolTipText ("Help");
    GUIServices.setSquare (helpButton);
    helpButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          showHelpDialog();
        } // performAction
      });
    rightPanel.add (helpButton);

    JButton aboutButton = GUIServices.getIconButton ("general.about");
    aboutButton.setToolTipText ("About");
    GUIServices.setSquare (aboutButton);
    aboutButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          showAboutDialog();
        } // performAction
      });
    rightPanel.add (aboutButton);

    rightPanel.add (new JLabel (VERSION_LABEL));
    swirl = new Swirl();
    swirl.setSpeed (1.5);
    swirl.setPreferredSize (new Dimension (18, 18));
    rightPanel.add (swirl);
    busyCount = 0;

    // Create data buttons
    // -------------------
    List dataButtonList = new ArrayList();
    if (applet != null) {

      JButton downloadButton = GUIServices.getIconButton ("browse.download");
      downloadButton.setToolTipText ("Download Data");
      downloadButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            downloadData (getSelectedEntry());
          } // actionPerformed
        });
      dataButtonList.add (downloadButton);

      JButton opendapButton = GUIServices.getIconButton ("browse.opendap");
      opendapButton.setToolTipText ("Show OPeNDAP");
      opendapButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            opendapData (getSelectedEntry());
          } // actionPerformed
        });
      dataButtonList.add (opendapButton);

    } // if

    // Create tabbed pane
    // ------------------
    tabbedPane = new JTabbedPane();
    this.add (tabbedPane, BorderLayout.CENTER);

    browsePanel = new EarthDataBrowsePanel (
      (LineOverlay) config.getOverlay ("grid"),
      (PolygonOverlay) config.getOverlay ("coast"),
      (LineOverlay) config.getOverlay ("inter"),
      (LineOverlay) config.getOverlay ("state"),
      (LineOverlay) config.getOverlay ("topo"),
      dataButtonList);
    browsePanel.addPropertyChangeListener (EarthDataBrowsePanel.BUSY_PROPERTY,
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent e) {
          setBusy (((Boolean) e.getNewValue()).booleanValue());
        } // propertyChange
      });
    tabbedPane.addTab ("View", GUIServices.getIcon ("view.tab"), browsePanel);

    // Create catalog control bar
    // --------------------------
    JPanel controlBar = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    if (applet != null) {

      catDownloadButton = GUIServices.getIconButton ("list.download");
      GUIServices.setSquare (catDownloadButton);
      catDownloadButton.setToolTipText ("Download Data");
      catDownloadButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            downloadData (catalogPanel.getSelectedEntry());
          } // actionPerformed
        });
      catDownloadButton.setEnabled (false);
      controlBar.add (catDownloadButton);

      catOpendapButton = GUIServices.getIconButton ("list.opendap");
      GUIServices.setSquare (catOpendapButton);
      catOpendapButton.setToolTipText ("Show OPeNDAP");
      catOpendapButton.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            opendapData (catalogPanel.getSelectedEntry());
          } // actionPerformed
        });
      catOpendapButton.setEnabled (false);
      controlBar.add (catOpendapButton);
      
    } // if
    controlBar.add (new JLabel ("Double-click catalog result to view"));

    // Create catalog tab
    // ------------------
    catalogPanel = new EarthDataCatalogPanel (controlBar);
    tabbedPane.addTab ("Catalog", GUIServices.getIcon ("catalog.tab"), 
      catalogPanel);
    catalogPanel.addPropertyChangeListener (new CatalogListener());

    // Activate the first dataset
    // --------------------------
    datasetCombo.setSelectedIndex (0);

  } // edac contructor

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected entry from the browse panel, or null. */
  private Entry getSelectedEntry () {

    int index = browsePanel.getSelectedIndex();
    if (index == -1) return (null);
    else return (currentDataset.readerList.getEntry (index));

  } // getSelectedEntry

  ////////////////////////////////////////////////////////////

  /**
   * The <code>Configuration</code> class holds configuration
   * information for an instance of EDAC.
   */
  public static class Configuration {

    // Variables
    // ---------

    /** The list of datasets. */
    private List datasetList;

    /** The XML data as a document. */
    private Document doc;

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new configuration object using the XML at the
     * specified URL.  The XML must conform to the CoastWatch
     * Earth Data Access Client (EDAC) schema located at
     * <code>http://coastwatch.noaa.gov/xml/configuration.xsd</code>
     * (or the local copy found in the resources for this class).
     * The schema language is W3C XML Schema 1.0 referenced at
     * <code>http://www.w3.org/2001/XMLSchema</code>.
     *
     * @param configStream the configuration stream for XML data.
     *
     * @throws IOException if an error occurred reading the XML
     * or schema.
     * @throws SAXException if an error occurred parsing the XML
     * or schema.
     * @throws NumberFormatException if an error occurred parsing
     * the value of a number from an XML attribute.
     * @throws MalformedURLException if an error occurred
     * converting the catalog string into a URL.
     * @throws ParserConfigurationException if an error occurred
     * creating the document parser.
     */
    public Configuration (
      InputStream configStream
    ) throws IOException, SAXException, NumberFormatException, 
      MalformedURLException, ParserConfigurationException,
      InterruptedException {
    
      // Create the validator
      // --------------------
      SchemaFactory schemaFactory =
        SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = 
        schemaFactory.newSchema (getClass().getResource (CONFIG_RESOURCE));

      // Parse and validate the document
      // -------------------------------
      DocumentBuilderFactory builderFactory = 
        DocumentBuilderFactory.newInstance();
      builderFactory.setSchema (schema);
      builderFactory.setNamespaceAware (true);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      doc = builder.parse (configStream);

      // Build a map of all subregions by short name
      // -------------------------------------------
      Map subregionMap = new HashMap();
      NodeList subregionNodes = doc.getElementsByTagName ("subregion");
      for (int i = 0; i < subregionNodes.getLength(); i++) {
        Node node = subregionNodes.item (i);
        NamedNodeMap map = node.getAttributes();
        String shortName = map.getNamedItem ("shortName").getNodeValue();
        String name = map.getNamedItem ("name").getNodeValue();
        EarthLocation centerLoc = new EarthLocation (
          Double.parseDouble (map.getNamedItem ("centerLat").getNodeValue()),
          Double.parseDouble (map.getNamedItem ("centerLon").getNodeValue())
        );
        double radius = 
          Double.parseDouble (map.getNamedItem ("radius").getNodeValue());
        subregionMap.put (shortName, new Subregion (centerLoc, radius, name,
          shortName));
      } // for

      // Build a map of datasets
      // -----------------------
      datasetList = new ArrayList();
      NodeList datasetNodes = doc.getElementsByTagName ("dataset");
      int datasetCount = datasetNodes.getLength();
      Executor exec = Executors.newCachedThreadPool();
      final CountDownLatch latch = new CountDownLatch (datasetCount);
      final Map datasetMap = Collections.synchronizedMap (new TreeMap());
      for (int i = 0; i < datasetCount; i++) {

        // Get basic dataset attributes
        // ----------------------------
        Node node = datasetNodes.item (i);
        NamedNodeMap map = node.getAttributes();
        final String name = map.getNamedItem ("name").getNodeValue();
        final String varName = map.getNamedItem ("varName").getNodeValue();
        String catalog = map.getNamedItem ("catalog").getNodeValue();
        final URL catalogUrl = new URL (catalog);
        String subregions = map.getNamedItem ("subregions").getNodeValue();

        // Create list of subregions
        // -------------------------
        final List subregionList = new ArrayList();
        String[] subregionArray = subregions.split (",");
        for (int j = 0; j < subregionArray.length; j++) {
          Subregion subregion = 
            (Subregion)subregionMap.get (subregionArray[j]);
          if (subregion == null) {
            throw new IOException ("Unknown subregion " + subregionArray[j] +
              " references from dataset " + name);
          }  // if
          subregionList.add (subregion);
        } // for

        // Get enhancement settings
        // ------------------------
        Node eNode = 
          ((Element)node).getElementsByTagName ("enhancement").item (0);
        NamedNodeMap eMap = eNode.getAttributes();
        String palette = eMap.getNamedItem ("palette").getNodeValue();
        double[] range = new double[] {
          Double.parseDouble (eMap.getNamedItem ("min").getNodeValue()),
          Double.parseDouble (eMap.getNamedItem ("max").getNodeValue())
        };
        String functionType = 
          eMap.getNamedItem ("functionType").getNodeValue();
        final ColorEnhancementSettings settings = 
          new ColorEnhancementSettings (
            varName, 
            PaletteFactory.create (palette),
            EnhancementFunctionFactory.create (functionType, range)
          );

        // Add dataset to sorted map
        // -------------------------
        final Integer orderKey = new Integer (i);
        Runnable runnable = new Runnable () {
            public void run () {
              try {
                CatalogQueryAgent agent = new OpendapQueryAgent (catalogUrl);
                OpendapReaderList readerList = new OpendapReaderList (agent);
                Dataset dataset = new Dataset (name, readerList, varName, 
                  settings, subregionList, agent);
                datasetMap.put (orderKey, dataset);
              } // try
              catch (IOException e) {
                System.err.println ("Error adding dataset "+ name +" to list");
              } // catch
              latch.countDown();
            } // run
          };        
        exec.execute (runnable);

      } // for
      latch.await();

      // Add datasets to list
      // --------------------
      for (Iterator iter = datasetMap.keySet().iterator(); iter.hasNext();) {
        datasetList.add (datasetMap.get (iter.next()));
      } // for

    } // Configuration constructor

    ////////////////////////////////////////////////////////

    /** Gets the list of datasets. */
    public List getDatasetList () { return (datasetList); }

    ////////////////////////////////////////////////////////

    /** 
     * Gets a color value from an XML attribute.
     *
     * @param attMap the XML attribute map.
     * @param attName the XML attribute name for the color.
     *
     * @return the color of null if none was available.
     */
    private static Color getColorFromAtt (
      NamedNodeMap attMap,
      String attName
    ) {

      ColorLookup lookup = ColorLookup.getInstance();
      Node node = attMap.getNamedItem (attName);
      if (node == null) 
        return (null);
      else
        return (lookup.convert (node.getNodeValue()));

    } // getColorFromAtt

    ////////////////////////////////////////////////////////

    /** 
     * Gets an overlay of the specified type.
     * 
     * @param type the overlay type, one of "grid", "coast",
     * "inter", "state", or "topo".
     *
     * @return the overlay of the specified type, or null if none
     * was available.
     */
    EarthDataOverlay getOverlay (
      String type
    ) {

      // Get list of overlay nodes
      // -------------------------
      NodeList overlayNodes = doc.getElementsByTagName ("overlay");

      // Search for specified overlay
      // ----------------------------
      NamedNodeMap overlayMap = null;
      for (int i = 0; i < overlayNodes.getLength(); i++) {
        Node node = overlayNodes.item (i);
        NamedNodeMap map = node.getAttributes();
        String typeValue = map.getNamedItem ("type").getNodeValue();
        if (typeValue.equals (type)) { overlayMap = map; break; }
      } // for
      if (overlayMap == null) return (null);

      // Get ready to create new overlay
      // -------------------------------
      Color lineColor = getColorFromAtt (overlayMap, "lineColor");
      EarthDataOverlay overlay = null;

      // Create grid
      // -----------
      if (type.equals ("grid")) {
        overlay = new LatLonOverlay (lineColor);
      } // if

      // Create coast
      // ------------
      else if (type.equals ("coast")) {
        Color fillColor = getColorFromAtt (overlayMap, "fillColor");
        String data = overlayMap.getNamedItem ("data").getNodeValue();
        CoastOverlay coast = new CoastOverlay (lineColor);
        if (fillColor != null) coast.setFillColor (fillColor);
        coast.setReaderFactory (BinnedGSHHSReaderFactory.getInstance (data));
        overlay = coast;
      } // else if

      // Create international
      // --------------------
      else if (type.equals ("inter")) {
        String data = overlayMap.getNamedItem ("data").getNodeValue();
        PoliticalOverlay inter = new PoliticalOverlay (lineColor);
        inter.setInternational (true);
        inter.setState (false);
        inter.setReaderFactory (BinnedGSHHSReaderFactory.getInstance (data));
        overlay = inter;
      } // else if

      // Create state
      // ------------
      else if (type.equals ("state")) {
        String data = overlayMap.getNamedItem ("data").getNodeValue();
        PoliticalOverlay state = new PoliticalOverlay (lineColor);
        state.setInternational (false);
        state.setState (true);
        state.setReaderFactory (BinnedGSHHSReaderFactory.getInstance (data));
        overlay = state;
      } // else if

      // Create topo
      // ------------
      else if (type.equals ("topo")) {
        String data = overlayMap.getNamedItem ("data").getNodeValue();


      } // else if

      // Set initial overlay visibility
      // ------------------------------
      boolean visible = Boolean.parseBoolean (overlayMap.getNamedItem (
        "visible").getNodeValue());
      if (overlay != null) overlay.setVisible (visible);

      return (overlay);

    } // getOverlay

    ////////////////////////////////////////////////////////

  } // Configuration class

  ////////////////////////////////////////////////////////////

  /** 
   * Performs a data download.
   *
   * @param entry the entry to download data for.
   */
  private void downloadData (
    Entry entry
  ) {

    if (entry == null) return;

    try {
      JSObject window = JSObject.getWindow (applet);
      Object[] args = new Object[] {
        entry.previewUrl,
        DateFormatter.formatDate (entry.startDate, DATE_FMT),
        entry.dataSource,
        entry.downloadUrl
      };
      window.call ("showDownload", args);
    } // try
    catch (JSException e) {
      e.printStackTrace();
    } // catch

  } // downloadData

  ////////////////////////////////////////////////////////////

  /** 
   * Performs a data access via OPeNDAP.
   *
   * @param entry the entry to access via OPeNDAP.
   */
  private void opendapData (
    Entry entry
  ) {

    if (entry == null) return;

    try {
      JSObject window = JSObject.getWindow (applet);
      Object[] args = new Object[] {
        entry.dataUrl + ".html"
      };
      window.call ("showOpendap", args);
    } // try
    catch (JSException e) {
      e.printStackTrace();
    } // catch

  } // opendapData

  ////////////////////////////////////////////////////////////

  /** Listens for catalog events. */
  private class CatalogListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {
      String name = event.getPropertyName();

      // Turn on/off progress
      // --------------------
      if (name.equals (EarthDataCatalogPanel.SEARCH_PROPERTY)) {
        setBusy (((Boolean)event.getNewValue()).booleanValue());
      } // if

      // Show entry in view
      // ------------------
      else if (name.equals (EarthDataCatalogPanel.ENTRY_PROPERTY)) {
        Entry entry = (Entry) event.getNewValue();
        tabbedPane.setSelectedIndex (0);
        int index = 
          currentDataset.readerList.getClosestIndex (entry.startDate);
        if (index != -1)
          browsePanel.setSelectedIndex (index);
      } // else if

      // Enable/disable control buttons
      // ------------------------------
      else if (name.equals (EarthDataCatalogPanel.CHANGED_PROPERTY)) {
        if (applet != null) {
          Entry entry = (Entry) event.getNewValue();
          boolean isEnabled = (entry != null);
          catDownloadButton.setEnabled (isEnabled);
          catOpendapButton.setEnabled (isEnabled);
        } // if
      } // else if

    } // propertyChange
  } // CatalogListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Starts or stops the busy swirl and wait cursor, keeping
   * track of the request count since various components can
   * contribute to a busy status.
   *
   * @param isBusy the busy flag, true to indicate busy status or
   * false for not busy.
   */
  private void setBusy (
    boolean isBusy                      
  ) {

    // Increase the busy count
    // -----------------------
    if (isBusy) {
      busyCount++;
      if (busyCount == 1) { 
        swirl.start();
        setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
      } // if
    } // if

    // Decrease the busy count
    // -----------------------
    else {
      if (busyCount == 0)
        throw new IllegalArgumentException ("Busy count cannot be negative");
      busyCount--;
      if (busyCount == 0) { 
        swirl.stop();
        setCursor (Cursor.getDefaultCursor());
      } // if
    } // else

  } // setBusy

  ////////////////////////////////////////////////////////////
  
  /** Responds to changes in the dataset combo box. */
  private class DatasetListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Check new dataset
      // -----------------
      Dataset dataset = (Dataset) datasetCombo.getSelectedItem();
      if (dataset == null || dataset == currentDataset) return;
      currentDataset = dataset;

      // Update dataset in browse panel
      // ------------------------------
      browsePanel.setData (dataset.name, dataset.readerList,
        dataset.varName, dataset.settings);
      browsePanel.setSubregionList (dataset.subregionList);

      // Update catalog panel
      // --------------------
      catalogPanel.setSubregionList (dataset.subregionList);
      catalogPanel.setQueryAgent (dataset.agent);

    } // actionPerformed
  } // DatasetListener class

  ////////////////////////////////////////////////////////////

  /** Holds information for a browse dataset. */
  public static class Dataset {

    /** The dataset name. */
    public String name;

    /** The list of data readers. */
    public OpendapReaderList readerList;
    
    /** The variable name in the reader for view images. */
    public String varName;

    /** The default color enhancement settings for the view. */
    public ColorEnhancementSettings settings;

    /** The list of subregions for the view. */
    public List subregionList;

    /** The catalog query agent for catalog searches. */
    public CatalogQueryAgent agent;

    /** Creates a new dataset. */
    public Dataset (
      String name,
      OpendapReaderList readerList,
      String varName,
      ColorEnhancementSettings settings,
      List subregionList,
      CatalogQueryAgent agent
    ) {
      
      this.name = name;
      this.readerList = readerList;
      this.varName = varName;
      this.settings = settings;
      this.subregionList = subregionList; 
      this.agent = agent;

    } // Dataset constructor

    /** Returns a string for this dataset (the dataset name). */
    public String toString () { return (name); }

    /** Returns a hash code for this dataset. */
    public int hashCode () { return (name.hashCode()); }

    /** Determines if two datasets are equal (uses the name only). */
    public boolean equals (Object obj) { 

      return (obj instanceof Dataset && ((Dataset) obj).name.equals (name));

    } // equals

  } // Dataset class

  ////////////////////////////////////////////////////////////

  /** Shows the help dialog. */
  private void showHelpDialog () {
    
    URL helpIndex = edac.this.getClass().getResource (HELP_INDEX);
    HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
    helpPanel.setPreferredSize (HELP_SIZE);
    helpPanel.showDialog (edac.this, "EDAC: Help");

  } // showHelpDialog

  ////////////////////////////////////////////////////////////

  /** Shows the about dialog. */
  private void showAboutDialog () {
    
    JOptionPane.showMessageDialog (edac.this, 
      "Program: " + LONG_NAME + "\n" +
      "Version: " + VERSION + "\n" +
      "Author: " + AUTHOR + "\n",
      "About", JOptionPane.INFORMATION_MESSAGE
    );

  } // showAboutDialog

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>Applet</class> class wraps a EDAC instance
   * inside an applet for use in a web browser.
   */
  public static class Applet 
    extends JApplet {

    /** Initializes the applet. */
    public void init () {

      // Set look and feel
      // -----------------
      try {
        UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName());
      } catch (Exception e) { }

      // Create applet
      // -------------
      try {
        SwingUtilities.invokeAndWait (new Runnable() {
            public void run () {
              try {
                String configUrl = getParameter ("config");
                if (configUrl == null)
                  throw new RuntimeException ("No applet config parameter");
                InputStream stream = new URL (configUrl).openStream();
                Configuration config = new Configuration (stream);
                edac panel = new edac (config, Applet.this);
                getContentPane().add (panel, BorderLayout.CENTER);
              } // try
              catch (Exception e) {
                JPanel panel = new JPanel();
                getContentPane().add (Box.createVerticalStrut (200), 
                  BorderLayout.NORTH);
                getContentPane().add (panel, BorderLayout.CENTER);
                panel.add (new JLabel (
                  "An error occurred configuring the data access client.",
                  JLabel.CENTER));
                panel.add (new JLabel (
                  "Check the Java Console for error messages.", 
                  JLabel.CENTER));
                e.printStackTrace();
              } // catch
            } // run
          });
      } catch (Exception e) {
        e.printStackTrace();
      } // catch

    } // init
    
  } // Applet class

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) throws Exception {

    ToolServices.setCommandLine (PROG, argv);

    // Get configuration
    // -----------------
    String configFile = argv[0];
    InputStream stream;
    if (configFile.indexOf ("://") != -1)
      stream = new URL (configFile).openStream();
    else
      stream = new FileInputStream (configFile);
    final Configuration config = new Configuration (stream);
    
    // Get fake applet parameter (for testing)
    // ---------------------------------------
    final boolean fakeApplet = (argv.length > 1 && argv[1].equals ("applet"));

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        
        // Create main frame
        // -----------------
        JFrame frame = new JFrame (LONG_NAME);
        JApplet applet = (fakeApplet ? new JApplet() : null);
        edac panel = new edac (config, applet);
        frame.getContentPane().add (panel, BorderLayout.CENTER);
        frame.addWindowListener (new WindowMonitor());
        frame.pack();
        frame.setSize (FRAME_SIZE);
        GUIServices.createErrorDialog (frame, "Error", 
          ToolServices.ERROR_INSTRUCTIONS);
        frame.setIconImage (GUIServices.getIcon ("tools.edac").getImage());

        // Show frame
        // ----------
        frame.setVisible (true);

      } // run
    });

  } // main

  ////////////////////////////////////////////////////////////

} // edac class

////////////////////////////////////////////////////////////////////////
