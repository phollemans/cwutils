////////////////////////////////////////////////////////////////////////
/*

     File: OverlayListChooser.java
   Author: Peter Hollemans
     Date: 2004/02/21

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import noaa.coastwatch.gui.AbstractOverlayListPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.visual.MultilayerBitmaskOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooserFactory;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.render.BitmaskOverlay;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.DataReferenceOverlay;
import noaa.coastwatch.render.feature.ESRIShapefileReader;
import noaa.coastwatch.render.feature.IQuamNCReader;
import noaa.coastwatch.render.feature.LatLonLineReader;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.JavaExpressionMaskOverlay;
import noaa.coastwatch.render.GridContainerOverlay;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.MultilayerBitmaskOverlay;
import noaa.coastwatch.render.PoliticalOverlay;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.TopographyOverlay;

import java.util.logging.Logger;

/**
 * <p>The <code>OverlayListChooser</code> class is a panel that
 * allows the user to manipulate a list of {@link
 * EarthDataOverlay} objects.  The user may add a new overlay,
 * delete an overlay, edit the overlay properties, and change the
 * overlay layer.</p>
 *
 * <p>The chooser signals a change in the overlay list by firing a
 * property change event whose property name is given by
 * <code>OVERLAY_LIST_PROPERTY</code>.  See the {@link
 * AbstractOverlayListPanel} class for details on how the property
 * change events should be interpreted.</p>
 *
 * @see LatLonOverlay
 * @see CoastOverlay
 * @see PoliticalOverlay
 * @see TopographyOverlay
 * @see BitmaskOverlay
 * @see MultilayerBitmaskOverlay
 * @see JavaExpressionMaskOverlay
 * @see ESRIShapefileReader
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class OverlayListChooser
  extends JPanel
  implements TabComponent, RequestHandler {

  private static final Logger LOGGER = Logger.getLogger (OverlayListChooser.class.getName());

  // Constants
  // ---------

  /** The overlay list property. */
  public static final String OVERLAY_LIST_PROPERTY = 
    AbstractOverlayListPanel.OVERLAY_PROPERTY;

  /** The grid command. */
  private static final String GRID_COMMAND = "Grid";

  /** The lat/lon grid command. */
  private static final String LATLON_COMMAND = "Lat/Lon";

  /** The data reference command. */
  private static final String DATAREF_COMMAND = "Data reference";

  /** The coast command. */
  private static final String COAST_COMMAND = "Coast";

  /** The political command. */
  private static final String POLITICAL_COMMAND = "Political";

  /** The topography command. */
  private static final String TOPOGRAPHY_COMMAND = "Topography";

  /** The mask command. */
  private static final String MASK_COMMAND = "Mask";

  /** The bitmask command. */
  private static final String BITMASK_COMMAND = "Bitmask";

  /** The multilayer bitmask command. */
  private static final String MULTILAYER_COMMAND = "Multilayer";

  /** The expression mask command. */
  private static final String EXPRMASK_COMMAND = "Expression mask";

  /** The shapefile command. */
  private static final String SHAPE_COMMAND = "Shape";

  /** The overlay list tooltip. */
  private static final String OVERLAY_LIST_TOOLTIP = "Overlay Layers";

  /** The overlay list panel title. */
  private static final String OVERLAY_TITLE = "Overlay";

  // Variables
  // ---------

  /** The overlay list panel. */
  private OverlayListPanel listPanel;

  /** The data reader for bitmask overlays. */
  private EarthDataReader reader;

  /** The list of variables for bitmask overlays. */
  private List variableList;

  /** The chooser dialog for overlay properties. */
  private JDialog chooserDialog;

  /** The popup menu for mask overlays. */
  private JPopupMenu maskMenu;

  /** The popup menu for grid overlays. */
  private JPopupMenu gridMenu;

  private DispatchTable dispatch;

  ////////////////////////////////////////////////////////////

  /**
   * 
   * @since 3.8.1
   */
  public static List<AbstractButton> getToolBarButtons () {

    var className = OverlayListChooser.class.getName();

    var buttons = new ArrayList<AbstractButton>();

    var button = new DropDownButton (GUIServices.createAction (className, "add_overlay", "Overlay", "layers.overlays", "Add geographic overlay"));
    button.addItem (GUIServices.createAction (className, "add_coast", "Coastlines", "overlay.menu.coast", null));
    button.addItem (GUIServices.createAction (className, "add_latlon", "Latitude / longitude grid", "overlay.menu.latlon", null));
    button.addItem (GUIServices.createAction (className, "add_dataref", "Row / column grid", "overlay.menu.dataref", null));
    button.addItem (GUIServices.createAction (className, "add_political", "Country and state borders", "overlay.menu.political", null));
    button.addItem (GUIServices.createAction (className, "add_topo", "Topographic and bathymetric contours", "overlay.menu.topography", null));
    button.addItem (GUIServices.createAction (className, "add_shape", "Shape files", "overlay.menu.shape", null));
    buttons.add (button);

    button = new DropDownButton (GUIServices.createAction (className, "add_mask", "Mask", "layers.masks", "Mask data"));
    button.addItem (GUIServices.createAction (className, "add_bitmask", "Single layer bitmask", "overlay.menu.bitmask", null));
    button.addItem (GUIServices.createAction (className, "add_multilayer", "Multilayer bitmask", "overlay.menu.multilayer", null));
    button.addItem (GUIServices.createAction (className, "add_exprmask", "Expression mask", "overlay.menu.expression", null));
    buttons.add (button);

    return (buttons);

  } // getToolBarButtons

  ////////////////////////////////////////////////////////////

  private void addCoastEvent() {

    var overlay = new CoastOverlay (Color.WHITE);
    listPanel.addNewOverlay (overlay, "Coast");

  } // addCoastEvent

  ////////////////////////////////////////////////////////////

  private void addLatLonEvent() {

    var overlay = new LatLonOverlay (Color.WHITE);
    listPanel.addNewOverlay (overlay, "Lat/Lon");

  } // addLatLonEvent

  ////////////////////////////////////////////////////////////

  private void addDataRefEvent() {

    var overlay = new DataReferenceOverlay (Color.WHITE);
    listPanel.addNewOverlay (overlay, "Row/Col");

  } // addDataRefEvent

  ////////////////////////////////////////////////////////////

  private void addPoliticalEvent() {

    var overlay = new PoliticalOverlay (Color.WHITE);
    listPanel.addNewOverlay (overlay, "Political");

  } // addPoliticalEvent

  ////////////////////////////////////////////////////////////

  private void addTopoEvent() {

    try {
      var overlay = new TopographyOverlay (Color.WHITE);
      overlay.setLevels (TopographyOverlay.BATH_LEVELS);
      listPanel.addNewOverlay (overlay, "Topographic");
    } // try
    catch (Exception e) { throw new RuntimeException (e); }

  } // addTopoEvent

  ////////////////////////////////////////////////////////////

  private void addBitmaskEvent() {

    var overlay = new BitmaskOverlay (Color.GRAY, reader, variableList, (String) variableList.get (0), 255);
    var chooserPanel = OverlayPropertyChooserFactory.create (overlay);
    listPanel.showDialog (chooserPanel, "Bitmask");

  } // addBitmaskEvent

  ////////////////////////////////////////////////////////////

  private void addMultilayerEvent() {

    var chooserPanel = OverlayPropertyChooserFactory.create (listPanel.createDefaultMultilayer());
    listPanel.showDialog (chooserPanel, "Multilayer");

  } // addMultilayerEvent

  ////////////////////////////////////////////////////////////

  private void addExprMaskEvent() {

    var overlay = new JavaExpressionMaskOverlay (Color.GRAY, reader, variableList, "");
    var chooserPanel = OverlayPropertyChooserFactory.create (overlay);
    listPanel.showDialog (chooserPanel, "Expression");

  } // addExprMaskEvent

  ////////////////////////////////////////////////////////////

  private void addShapeEvent() {

    var overlayList = listPanel.createShapeOverlays();
    for (var overlay : overlayList)
      listPanel.addNewOverlay (overlay, overlay.getName());

  } // addShapeEvent

  ////////////////////////////////////////////////////////////

  @Override
  public void handleRequest (Request request) { dispatch.handleRequest (request); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean canHandleRequest (Request request) { return (dispatch.canHandleRequest (request)); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new overlay list chooser. 
   * 
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public OverlayListChooser (
    EarthDataReader reader,
    List variableList
  ) {

    super (new BorderLayout());
    
    // Set bitmask overlay data sources
    // --------------------------------
    this.reader = reader;
    this.variableList = variableList;

    // Create main panel
    // -----------------
    listPanel = new OverlayListPanel();
    this.add (listPanel, BorderLayout.CENTER);

    dispatch = new DispatchTable (OverlayListChooser.class.getName());
    dispatch.addDispatch ("add_coast", () -> addCoastEvent());
    dispatch.addDispatch ("add_latlon", () -> addLatLonEvent());
    dispatch.addDispatch ("add_dataref", () -> addDataRefEvent());
    dispatch.addDispatch ("add_political", () -> addPoliticalEvent());
    dispatch.addDispatch ("add_topo", () -> addTopoEvent());
    dispatch.addDispatch ("add_bitmask", () -> addBitmaskEvent());
    dispatch.addDispatch ("add_multilayer", () -> addMultilayerEvent());
    dispatch.addDispatch ("add_exprmask", () -> addExprMaskEvent());
    dispatch.addDispatch ("add_shape", () -> addShapeEvent());

  } // OverlayListChooser constructor

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () { return (OVERLAY_LIST_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () { return (null); }
  
  ////////////////////////////////////////////////////////////

  /** Gets the list of overlays. */
  public List getOverlays(){
	  return listPanel.getOverlayList();
  }
  
  ////////////////////////////////////////////////////////////

  /** Gets a list of overlays to the chooser. */
  public void addOverlays(List overlays){
	  for (Iterator iter = overlays.iterator(); iter.hasNext(); ) {
		  EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
		  if (overlay instanceof GridContainerOverlay)
		        ((GridContainerOverlay) overlay).setDataSource(reader, variableList);
		  listPanel.addOverlay(overlay);
	  }
  }

  ////////////////////////////////////////////////////////////

  @Override
  public Icon getIcon () { return (GUIServices.getIcon ("overlay.tab")); }

  ////////////////////////////////////////////////////////////

  /** Redirects overlay property listeners to the list panel. */
  public void addPropertyChangeListener (
    String propertyName,
    PropertyChangeListener listener
  ) {

    if (propertyName.equals (AbstractOverlayListPanel.OVERLAY_PROPERTY))
      listPanel.addPropertyChangeListener (propertyName, listener);
    else
      super.addPropertyChangeListener (propertyName, listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////
  
  /** Implements overlay list buttons and title. */
  private class OverlayListPanel 
    extends AbstractOverlayListPanel {

    ////////////////////////////////////////////////////////

    /** Creates a new list panel. */
    public OverlayListPanel () {

      super (true, true, true, false, true);
      setDataSource (reader, variableList);

    } // OverlayListPanel constructor

    ////////////////////////////////////////////////////////

    /** Creates the overlay list add buttons. */
    protected List getAddButtons () {

      // // Create button list and listener
      // // -------------------------------
      // List buttons = new LinkedList();
      // ActionListener buttonListener = new OverlayListener();
      
      // // Create buttons
      // // --------------
      // String[] commandArray = new String[] {
      //   GRID_COMMAND,
      //   COAST_COMMAND,
      //   POLITICAL_COMMAND,
      //   TOPOGRAPHY_COMMAND,
      //   MASK_COMMAND,
      //   SHAPE_COMMAND
      // };
      // String[] iconArray = new String[] {
      //   "overlay.grid",
      //   "overlay.coast",
      //   "overlay.political",
      //   "overlay.topography",
      //   "overlay.masks",
      //   "overlay.shape"
      // };
      // for (int i = 0; i < commandArray.length; i++) {
      //   JButton button = GUIServices.getIconButton (iconArray[i]);
      //   button.setActionCommand (commandArray[i]);
      //   button.addActionListener (buttonListener);
      //   button.setToolTipText (commandArray[i]);
      //   buttons.add (button);
      // } // for

      // // Create mask overlay menu
      // // ------------------------
      // maskMenu = new JPopupMenu();
      // String[] maskCommands = new String[] {
      //   BITMASK_COMMAND,
      //   MULTILAYER_COMMAND,
      //   EXPRMASK_COMMAND
      // };
      // String[] maskIcons = new String[] {
      //   "overlay.menu.bitmask",
      //   "overlay.menu.multilayer",
      //   "overlay.menu.expression"
      // };
      // for (int i = 0; i < maskCommands.length; i++) {
      //   JMenuItem item = new JMenuItem (maskCommands[i], 
      //     GUIServices.getIcon (maskIcons[i]));
      //   maskMenu.add (item);
      //   item.addActionListener (buttonListener);
      // } // for

      // // Create grid overlay menu
      // // ------------------------
      // gridMenu = new JPopupMenu();
      // String[] gridCommands = new String[] {
      //   LATLON_COMMAND,
      //   DATAREF_COMMAND
      // };
      // String[] gridIcons = new String[] {
      //   "overlay.menu.latlon",
      //   "overlay.menu.dataref"
      // };
      // for (int i = 0; i < gridCommands.length; i++) {
      //   JMenuItem item = new JMenuItem (gridCommands[i], 
      //     GUIServices.getIcon (gridIcons[i]));
      //   gridMenu.add (item);
      //   item.addActionListener (buttonListener);
      // } // for

      // return (buttons);

      return (null);

    } // getAddButtons

    ////////////////////////////////////////////////////////

    /** Gets the overlay list title. */
    protected String getTitle () { return (OVERLAY_TITLE); }

    ////////////////////////////////////////////////////////

    /** 
     * Adds a new overlay to the list panel and sets the overlay name
     * appropriately. 
     * 
     * @param overlay the overlay to add to the panel.
     * @param name the name of the overlay to use (a number will be added
     * if there are more than one overlay with the same name) or null to
     * use the existing overlay name.
     */
    private void addNewOverlay (
      EarthDataOverlay overlay,
      String name
    ) {

      if (name != null) overlay.setName (name + " " + listPanel.getOverlayCount (name));
      listPanel.addOverlay (overlay);

    } // addNewOverlay

    ////////////////////////////////////////////////////////

    /** Handles overlay add events. */
    private class OverlayListener implements ActionListener {
      public void actionPerformed (ActionEvent event) {

        final String command = event.getActionCommand();

        // Show mask menu
        // --------------
        if (command.equals (MASK_COMMAND)) {
          JButton button = (JButton) event.getSource();
          maskMenu.show (button, 0, button.getHeight());
        } // if

        // Show grid menu
        // --------------
        if (command.equals (GRID_COMMAND)) {
          JButton button = (JButton) event.getSource();
          gridMenu.show (button, 0, button.getHeight());
        } // if

        // Create interactive overlay (extra input required)
        // -------------------------------------------------
        else if (command.equals (BITMASK_COMMAND) || 
            command.equals (MULTILAYER_COMMAND) ||
            command.equals (SHAPE_COMMAND) ||
            command.equals (EXPRMASK_COMMAND)) {

          // Create bitmask overlay
          // ----------------------
          if (command.equals (BITMASK_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (
                new BitmaskOverlay (Color.GRAY, reader, variableList, 
                  (String) variableList.get (0), 255));
            showDialog (chooserPanel, command);
          } // if

          // Create multilayer bitmask overlay
          // ---------------------------------
          else if (command.equals (MULTILAYER_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (createDefaultMultilayer());
            showDialog (chooserPanel, command);
          } // else if

          // Create expression mask overlay
          // ------------------------------
          else if (command.equals (EXPRMASK_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (
                new JavaExpressionMaskOverlay (Color.GRAY, reader, variableList, ""));
            showDialog (chooserPanel, command);
          } // else if

          // Create shape overlay
          // --------------------
          else if (command.equals (SHAPE_COMMAND)) {
            List<EarthDataOverlay> overlayList = createShapeOverlays();
            for (EarthDataOverlay overlay : overlayList)
              addNewOverlay (overlay, overlay.getName());
          } // else if

        } // if

        // Create non-interactive overlay
        // ------------------------------
        else {
          EarthDataOverlay overlay = null;
          if (command.equals (LATLON_COMMAND))
            overlay = new LatLonOverlay (Color.WHITE);
          else if (command.equals (DATAREF_COMMAND))
            overlay = new DataReferenceOverlay (Color.WHITE);
          else if (command.equals (COAST_COMMAND))
            overlay = new CoastOverlay (Color.WHITE);
          else if (command.equals (POLITICAL_COMMAND))
            overlay = new PoliticalOverlay (Color.WHITE);
          else if (command.equals (TOPOGRAPHY_COMMAND)) {
            try {
              TopographyOverlay topo = new TopographyOverlay (Color.WHITE);
              topo.setLevels (TopographyOverlay.BATH_LEVELS);
              overlay = topo;
            } // try
            catch (Exception e) { throw new RuntimeException (e); }
          } // else if
          if (overlay != null) addNewOverlay (overlay, command);
        } // else

      } // actionPerformed
    } // OverlayListener class

    ////////////////////////////////////////////////////////

    /** Shows a dialog for the specified chooser panel. */
    private void showDialog (
      final OverlayPropertyChooser chooserPanel,
      final String command
    ) { 

      // Create dialog
      // -------------
      final JDialog[] dialog = new JDialog[1];
      Action okAction = GUIServices.createAction ("OK", new Runnable() {
          public void run () {
            try { 
              addNewOverlay (chooserPanel.getOverlay(), command); 
              dialog[0].dispose();
            } // try
            catch (IllegalStateException e) {
              String errorMessage = 
                "An error occurred checking the input:\n" +
                e.getMessage() + "\n" + 
                "Please correct the problem and try again.";
              JOptionPane.showMessageDialog (dialog[0], errorMessage,
                "Error", JOptionPane.ERROR_MESSAGE);
            } // catch
          } // run
        });
      Action cancelAction = GUIServices.createAction ("Cancel", null);
      var showing = OverlayListChooser.this.isShowing();
      var parent = showing ? OverlayListChooser.this : JOptionPane.getFrameForComponent (OverlayListChooser.this);
      dialog[0] = GUIServices.createDialog (
        parent, "Select the overlay properties", true, 
        chooserPanel, null, new Action[] {okAction, cancelAction}, 
        new boolean[] {false, true}, true);

      // Show dialog
      // -----------
      dialog[0].setVisible (true);

    } // showDialog

    ////////////////////////////////////////////////////////

    /** Creates a new default multilayer bitmask. */
    private EarthDataOverlay createDefaultMultilayer () {

      MultilayerBitmaskOverlay multilayer = new MultilayerBitmaskOverlay();
      List bitmaskList = 
        MultilayerBitmaskOverlayPropertyChooser.createBitmaskList (1, 8,
          reader, variableList, (String) variableList.get (0));
      multilayer.addOverlays (bitmaskList);
      return (multilayer);

    } // getDefaultMultilayer

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new set of shape overlays. 
     * 
     * @return the new set of overlays, possibly empty.
     */
    private List<EarthDataOverlay> createShapeOverlays () {

      // Show file chooser
      // -----------------
      JFileChooser fileChooser = GUIServices.getFileChooser();

      SimpleFileFilter shapefileFilter = new SimpleFileFilter (
        new String[] {"shp"}, "ESRI shapefile data");
      fileChooser.addChoosableFileFilter (shapefileFilter);

      SimpleFileFilter iquamFilter = new SimpleFileFilter (
        new String[] {"nc"}, "iQuam in-situ point data");
      fileChooser.addChoosableFileFilter (iquamFilter);

      SimpleFileFilter textFilter = new SimpleFileFilter (
        new String[] {"txt"}, "Lat/lon line data");
      fileChooser.addChoosableFileFilter (textFilter);

      fileChooser.setDialogType (JFileChooser.OPEN_DIALOG);
      final Frame frame = 
        JOptionPane.getFrameForComponent (OverlayListChooser.this);
      int returnVal = fileChooser.showOpenDialog (frame);

      // Open selected file
      // ------------------
      List<EarthDataOverlay> overlayList = new LinkedList<EarthDataOverlay>();
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();

        // Try reading as shapefile
        // ------------------------
        boolean failed = false;
        try {
          ESRIShapefileReader shapeReader = new ESRIShapefileReader (file.toURI().toURL());
          EarthDataOverlay overlay = shapeReader.getOverlay();
          if (overlay instanceof PointFeatureOverlay) {
            ((PointFeatureOverlay) overlay).setFillColor (Color.WHITE);
            overlay.setColor (Color.BLACK);
          } // if
          else if (overlay instanceof PolygonOverlay) {
            ((PolygonOverlay) overlay).setFillColor (null);
            overlay.setColor (Color.WHITE);
          } // else if
          else {
            overlay.setColor (Color.WHITE);
          } // else
          overlay.setName ("ESRI data");
          overlayList.add (overlay);
        } // try
        catch (IOException e) { failed = true; }
        
        // Try reading as iQuam file
        // -------------------------
        if (failed) {
          failed = false;
          try {
            IQuamNCReader iquamReader = new IQuamNCReader (file.getAbsolutePath());
            Date date = reader.getInfo().getDate();
            EarthTransform trans = reader.getInfo().getTransform();
            List<Grid> gridList = new ArrayList<Grid>();
            for (Object name : variableList)
              gridList.add ((Grid) reader.getVariable ((String) name));
            overlayList.addAll (iquamReader.getStandardOverlays (date, trans, gridList));
          } // try
          catch (IOException e) { failed = true; }
        } // if

        // Try reading as a lat/lon line file
        // ----------------------------------
        if (failed) {
          failed = false;
          try {
            var latLonReader = new LatLonLineReader (file.getAbsolutePath());
            EarthDataOverlay overlay = latLonReader.getOverlay();
            overlay.setColor (Color.WHITE);
            overlay.setName ("Lat/lon data");
            overlayList.add (overlay);
          } // try
          catch (IOException e) { failed = true; }
        } // if
        
        // Show error message
        // ------------------
        if (failed || overlayList.size() == 0) {
          String errorMessage =
            "An error occurred creating the shape overlay.\n" +
            "The file format may not be supported. Please choose\n" +
            "another shape data file and try again.";
          JOptionPane.showMessageDialog (frame, errorMessage,
            "Error", JOptionPane.ERROR_MESSAGE);
        } // if

      } // if

      return (overlayList);

    } // createShapeOverlays

    ////////////////////////////////////////////////////////

  } // OverlayListPanel class

  ////////////////////////////////////////////////////////////

} // OverlayListChooser class

////////////////////////////////////////////////////////////////////////
