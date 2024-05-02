////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataAnalysisPanel.java
   Author: Peter Hollemans
     Date: 2004/02/17

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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.CardLayout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.AbstractButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import noaa.coastwatch.gui.EarthDataViewController;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.EnhancementChooser;
import noaa.coastwatch.gui.FullScreenWindow;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.LegendPanel;
import noaa.coastwatch.gui.LightTable;
import noaa.coastwatch.gui.OverlayListChooser;
import noaa.coastwatch.gui.PaletteChooser;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.VariableChooser;
import noaa.coastwatch.gui.ViewOperationChooser;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.gui.IconFactory;
import noaa.coastwatch.gui.nav.NavigationAnalysisPanel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.DataColorScale;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.util.MetadataServices;

import java.util.logging.Logger;

// Testing
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.VariableStatisticsGenerator;

/**
 * The <code>EarthDataAnalysisPanel</code> groups together a variety
 * of earth data view and chooser components into one main panel that
 * may be used to display the contents of a
 * <code>EarthDataReader</code> object.  The grouped components
 * are:
 * <ol>
 *   <li> an {@link EarthDataViewPanel} that shows a data
 *   enhancement view of 2D variable data, </li>
 *   <li> a {@link VariableChooser} that allows the user to select
 *   which variable from the file to look at, </li>
 *   <li> an {@link EarthDataViewPanel.TrackBar} that shows the current
 *   mouse cursor position in image and geographic coordinates, and </li>
 *   <li> a number of chooser panels that select the view properties
 *   of the currently displayed variable data, such as the palette,
 *   enhancement function, overlays, etc. </li>
 * </ol>
 *
 * @see noaa.coastwatch.gui.EarthDataViewController
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class EarthDataAnalysisPanel
  extends JPanel
  implements TabComponent, RequestHandler {

  private static final Logger LOGGER = Logger.getLogger (EarthDataAnalysisPanel.class.getName());    

  // Constants
  // ---------

  /** The initial view panel width and height. */
  private static final int VIEW_PANEL_SIZE = 512;

  /** The view panel background color. */
  private static final Color VIEW_BACK = Color.BLACK;

  /** 
   * The tabbed pane width.  We force the tabbed pane here to a
   * certain width to avoid problems when the contents of tabs
   * suddenly decide to resize themselves, causing the view panel to
   * redraw.  A better fix might be to lay the contents of the
   * analysis panel out in a GridBagLayout.
   */
  private static final int TABBED_PANE_WIDTH = 300;

  /** The maximum length of the tab title. */
  private static final int MAX_TITLE_LENGTH = 20;

  // Variables
  // ---------

  private EarthDataReader reader;
  private EarthDataViewPanel viewPanel;
  private LightTable lightTable;
  private EarthDataViewController controller;
  private JPanel controlPanel;
  private ManagedLayeredPane viewLayeredPane;
  private OnScreenMessage messageBox;
  private JToggleButton viewButton, layersButton;
  private JTabbedPane layersTabbedPane;

  ////////////////////////////////////////////////////////////

  /**
   * 
   * @since 3.8.1
   */
  public static List<AbstractButton> getToolBarButtons () {

    List<AbstractButton> buttons = new ArrayList<>();
    buttons.addAll (OverlayListChooser.getToolBarButtons());
    buttons.addAll (SurveyListChooser.getToolBarButtons());
    buttons.addAll (AnnotationListChooser.getToolBarButtons());

    return (buttons);

  } // getToolBarButtons

  ////////////////////////////////////////////////////////////

  public void resetView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.RESET); }
  public void actualSizeView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.ONE_TO_ONE); }
  public void fillWindowView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.FIT); }
  public void magnifyView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.MAGNIFY); }
  public void shrinkView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.SHRINK); }
  public void zoomSelectionView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.ZOOM); }
  public void panView () { controller.getViewOperationChooser().performViewOperation (OnScreenViewOperationChooser.Mode.PAN); }

  ////////////////////////////////////////////////////////////

  @Override
  public void handleRequest (Request request) { 

    if (controller.getOverlayChooser().canHandleRequest (request))
      controller.getOverlayChooser().handleRequest (request);
    else if (controller.getSurveyChooser().canHandleRequest (request))
      controller.getSurveyChooser().handleRequest (request);
    else if (controller.getAnnotationChooser().canHandleRequest (request))
      controller.getAnnotationChooser().handleRequest (request);
    else
      throw new IllegalArgumentException ("Cannot handle request type ID " + request.getTypeID());

  } // handleRequest

  ////////////////////////////////////////////////////////////

  @Override
  public boolean canHandleRequest (Request request) { 

    boolean canHandle = (
      controller.getOverlayChooser().canHandleRequest (request) ||
      controller.getSurveyChooser().canHandleRequest (request) ||
      controller.getAnnotationChooser().canHandleRequest (request)
    );
    return (canHandle);

  } // canHandleRequest

  ////////////////////////////////////////////////////////////

  /**
   * Shows or hides the view controls tabs in this panel.
   *
   * @param isVisible the visiblity flag, true to show the tabbed pane or false
   * to hide it.
   */
  public void setTabbedPaneVisible (
    boolean isVisible
  ) {

//    tabbedPane.setVisible (isVisible);
    controlPanel.setVisible (isVisible);

  } // setTabbedPaneVisible

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current view panel size.
   *
   * @return the view panel size.
   */
  public Dimension getViewPanelSize() { return (viewPanel.getSize()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the current view panel size.
   *
   * @param panelSize the new view panel size.
   */
  public void setViewPanelSize (Dimension panelSize) { viewPanel.setSize (panelSize); }

  ////////////////////////////////////////////////////////////

  /** Shows this analysis panel in a full screen mode. */
  public void showFullScreen () {

    // Save the parent window of the layered view panel to restore it after
    // the full screen mode is done with it.
    JPanel centerPanel = (JPanel) viewLayeredPane.getParent();
    var window = new FullScreenWindow (viewLayeredPane, null);

    // Create a close button to go at the top left of the full screen
    // window.
    var closeButton = GUIServices.createOnScreenStyleButton (48, IconFactory.Purpose.CLOSE_CIRCLE);
    closeButton.setForeground (Color.WHITE);
    viewLayeredPane.add (closeButton, JLayeredPane.PALETTE_LAYER, 
      ManagedLayeredPane.Position.NORTH_WEST, new Insets (15, 15, 15, 15));

    // Set up the close button to end full screen mode and also remove itself
    // from the center panel when complete.
    closeButton.addActionListener (event -> {
      if (window.isFullScreen()) {
        window.stop();
        viewLayeredPane.remove (closeButton);
        centerPanel.add (viewLayeredPane, BorderLayout.CENTER);
        centerPanel.validate();
      } // if
    });

    // Start full screen mode.
    window.start();

  } // showFullScreen

  ////////////////////////////////////////////////////////////

  private void configureControlsForWindow () {

    var viewControls = controller.getViewOperationChooser();
    viewControls.reconfigure (BoxLayout.Y_AXIS, false, null, viewControls.defaultModes());
    viewLayeredPane.setComponentConstraints (viewControls, 
      ManagedLayeredPane.Position.EAST, new Insets (40, 40, 40, 15));

  } // configureControlsForWindow

  ////////////////////////////////////////////////////////////

  private void configureControlsForFullscreen (Runnable closeAction) {

    // NB: This is not currently used.

//    var viewControls = controller.getViewOperationChooser();

    // viewControls.reconfigure (BoxLayout.Y_AXIS, true, closeAction, viewControls.subsetModes());    
    // viewControls.setBorder (BorderFactory.createEmptyBorder (40, 40, 60, 40));
    // viewLayeredPane.setComponentPosition (viewControls, ManagedLayeredPane.Position.SOUTH);

    // viewControls.reconfigure (BoxLayout.Y_AXIS, true, closeAction, viewControls.subsetModes());    
    // viewControls.setBorder (BorderFactory.createEmptyBorder (40, 40, 60, 40));
    // viewLayeredPane.setComponentPosition (viewControls, ManagedLayeredPane.Position.SOUTH);

  } // configureControlsForFullscreen
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the reader used by this panel.
   * 
   * @return the reader object.
   */
  public EarthDataReader getReader () { return (reader); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the earth data view from the view panel.
   *
   * @return the data view.
   */
  public EarthDataView getView () { return (viewPanel.getView()); }

  ////////////////////////////////////////////////////////////

  /** Shows the navigation analysis dialog. */
  public void showNavAnalysisDialog () {

    NavigationAnalysisPanel panel = controller.getNavAnalysisPanel();
    panel.showDialog (this);

  } // showNavAnalysisDialog

  ////////////////////////////////////////////////////////////

  private void showMessageBox (String message, Runnable closeAction) {

    if (messageBox != null) hideMessageBox();
    messageBox = new OnScreenMessage (message, closeAction);
    viewLayeredPane.add (messageBox, JLayeredPane.PALETTE_LAYER, 
      ManagedLayeredPane.Position.NORTH, new Insets (10, 5, 5, 5));
    viewLayeredPane.repaint();

  } // showMessageBox

  ////////////////////////////////////////////////////////////

  private void hideMessageBox() {

    if (messageBox != null) {
      viewLayeredPane.remove (messageBox);
      viewLayeredPane.repaint();
      messageBox = null;
    } // if

  } // hideMessageBox

  ////////////////////////////////////////////////////////////

  private void legendChangeEvent () {

    // TODO: We need to set the legend size here based on the minimum
    // size required by the color scale.


    var legendPanel = controller.getLegendPanel();
    var colorScale = (DataColorScale) legendPanel.getLegend();
    colorScale.setLegendAxis (DataColorScale.HORIZONTAL_AXIS);    
    var font = UIManager.getFont ("Label.font");
    colorScale.setFont (font.deriveFont (font.getSize() * 0.8f));



  } // legendChangeEvent

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new analysis panel from the specified reader.  The
   * analysis panel is initially set up to show the first variable
   * in the specified list.
   *
   * @param reader the reader to use.
   * @param variableList the list of variable names to make available.
   */
  public EarthDataAnalysisPanel (
    EarthDataReader reader,
    List<String> variableList
  ) {
    
    super (new BorderLayout());

    // Create the view controller that contains all the various 
    // panels that we need for the layout.
    this.reader = reader;
    controller = new EarthDataViewController (reader, variableList);

    // Create the main panel for the analysis and the chooser
    // for variables at the top.
    JPanel centerPanel = new JPanel (new BorderLayout());
    this.add (centerPanel, BorderLayout.CENTER);
    VariableChooser chooser = controller.getVariableChooser();

    var chooserBox = Box.createHorizontalBox();
    chooserBox.add (Box.createHorizontalGlue());
    chooserBox.add (controller.getVariableChooser());
    chooserBox.add (Box.createHorizontalGlue());
    centerPanel.add (chooserBox, BorderLayout.NORTH);

    // Add a panel in the center of the main panel that has a beveled
    // border.
    var centerBorderedPane = new JPanel (new BorderLayout());
    centerBorderedPane.setBorder (new BevelBorder (BevelBorder.LOWERED));
    centerPanel.add (centerBorderedPane, BorderLayout.CENTER);

    // Setup the light table to catch drawing events and put it inside of
    // a layered pane so that translucent components can float on
    // top.
    lightTable = controller.getLightTable();
    lightTable.setPreferredSize (new Dimension (VIEW_PANEL_SIZE, VIEW_PANEL_SIZE));
    lightTable.setBackground (VIEW_BACK);
    viewLayeredPane = new ManagedLayeredPane();
    viewLayeredPane.add (lightTable, JLayeredPane.DEFAULT_LAYER, ManagedLayeredPane.Position.FULL);
    centerBorderedPane.add (viewLayeredPane, BorderLayout.CENTER);

    // Create the view manipulation controls so that they're placed on the
    // right side and fade in when the mouse is over them.
    var viewControls = controller.getViewOperationChooser();
    viewLayeredPane.add (viewControls, JLayeredPane.PALETTE_LAYER);
    configureControlsForWindow();
    var helper = new TimedOpacityHelper (viewControls, 0.5f, 200);
    viewControls.setHelper (helper);

    // Set up the message operation so that when needed, a cancellable 
    // instruction message is shown to the user at the top of the view.
    var messageOperation = controller.getMessageOperation();
    Runnable cancelAction = () -> { hideMessageBox(); messageOperation.cancel(); };
    messageOperation.setShowAction (message -> showMessageBox (message, cancelAction));
    messageOperation.setDisposeAction (() -> hideMessageBox());

    // Create the legend panel as a floating translucent panel near the bottom
    // of the view panel.
    var legendContainerPanel = new OnScreenStylePanel();
    legendContainerPanel.setLayout (new BorderLayout());
    var legendPanel = controller.getLegendPanel();

    legendPanel.setForeground (Color.WHITE);
    legendPanel.setOpaque (false);
    legendPanel.setPreferredSize (new Dimension (360, 80));
    legendPanel.setSize (new Dimension (360, 80));
//    legendPanel.setBorder (BorderFactory.createEmptyBorder (0, 20, 0, 20));
    legendPanel.addPropertyChangeListener ("enabled", event -> legendContainerPanel.setVisible ((Boolean) event.getNewValue()));

    legendContainerPanel.add (legendPanel, BorderLayout.CENTER);
    viewLayeredPane.add (legendContainerPanel, JLayeredPane.PALETTE_LAYER, 
      ManagedLayeredPane.Position.SOUTH, new Insets (0, 0, 30, 0));

    legendPanel.addPropertyChangeListener (LegendPanel.LEGEND_PROPERTY, event -> legendChangeEvent());
    legendChangeEvent();




    
    // Create tabbed pane
    // ------------------


    // Insets tabInsets = (Insets) UIManager.get ("TabbedPane.tabInsets");
    // Insets smallTabInsets = (tabInsets == null ? new Insets (0, 0, 0, 0) : (Insets) tabInsets.clone());
    // smallTabInsets.left = 4;
    // smallTabInsets.right = 4;
    // UIManager.put ("TabbedPane.tabInsets", smallTabInsets);
    // tabbedPane = new JTabbedPane();
    // UIManager.put ("TabbedPane.tabInsets", tabInsets);
    // List<TabComponent> tabs = controller.getTabComponentPanels();
    // for (TabComponent tab : tabs) {
    //   JPanel tabPanel = new JPanel (new BorderLayout());
    //   Box helpBox = Box.createHorizontalBox();
    //   helpBox.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
    //   helpBox.add (GUIServices.getHelpButton (tab.getClass()));
    //   helpBox.add (Box.createHorizontalGlue());
    //   tabPanel.add (helpBox, BorderLayout.SOUTH);
    //   tabPanel.add ((Component) tab, BorderLayout.CENTER);
    //   tabbedPane.addTab (tab.getTitle(), tab.getIcon(), tabPanel, tab.getToolTip());
    // } // for
    // this.add (tabbedPane, BorderLayout.WEST);
    // tabbedPane.setPreferredSize (new Dimension (TABBED_PANE_WIDTH, 0));





    // Get the tabs from the controller.
    var tabs = controller.getTabComponentPanels();    

    // Create a control panel using a card layout so that the view and 
    // layer controls have their own card.
    controlPanel = new JPanel (new BorderLayout (5, 5));
    controlPanel.setPreferredSize (new Dimension (TABBED_PANE_WIDTH, 0));
    var controlPanelContainer = Box.createHorizontalBox();
    controlPanelContainer.add (Box.createHorizontalStrut (5));
    controlPanelContainer.add (new JSeparator (JSeparator.VERTICAL));
    controlPanelContainer.add (controlPanel);
    this.add (controlPanelContainer, BorderLayout.EAST);

    var cardLayout = new CardLayout();
    var cards = new JPanel (cardLayout);
    controlPanel.add (cards, BorderLayout.CENTER);

    // Add the view controls to the first card.
    var viewTabbedPane = new JTabbedPane();
    var viewTabs = List.of (tabs.get (0), tabs.get (1), tabs.get (5));
    var viewTabTitles = List.of ("Enhancement", "Palette", "Composite");
    for (int i = 0; i < viewTabs.size(); i++) {
      var tab = viewTabs.get (i);
      JPanel tabPanel = new JPanel (new BorderLayout());
      tabPanel.setBorder (BorderFactory.createEmptyBorder (5, 2, 5, 2));
      Box helpBox = Box.createHorizontalBox();
      helpBox.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
      helpBox.add (GUIServices.getHelpButton (tab.getClass()));
      helpBox.add (Box.createHorizontalGlue());
      tabPanel.add (helpBox, BorderLayout.SOUTH);
      tabPanel.add ((Component) tab, BorderLayout.CENTER);
      viewTabbedPane.addTab (viewTabTitles.get (i), null, tabPanel, null);
    } // for
    cards.add (viewTabbedPane, "View");

    // Add the layer controls to the second card.
    layersTabbedPane = new JTabbedPane();
    var layersTabs = List.of (tabs.get (2), tabs.get (3), tabs.get (4));
    var layersTabTitles = List.of ("Overlay", "Survey", "Annotation");
    for (int i = 0; i < layersTabs.size(); i++) {
      var tab = layersTabs.get (i);
      JPanel tabPanel = new JPanel (new BorderLayout());
      tabPanel.setBorder (BorderFactory.createEmptyBorder (5, 2, 5, 2));
      Box helpBox = Box.createHorizontalBox();
      helpBox.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
      helpBox.add (GUIServices.getHelpButton (tab.getClass()));
      helpBox.add (Box.createHorizontalGlue());
      tabPanel.add (helpBox, BorderLayout.SOUTH);
      tabPanel.add ((Component) tab, BorderLayout.CENTER);
      layersTabbedPane.addTab (layersTabTitles.get (i), null, tabPanel, null);
    } // for
    cards.add (layersTabbedPane, "Layers");

    // Now create a toolbar for the card layout and put it on the top
    // of the control panels.
    var toolbar = new JToolBar();
    toolbar.setLayout (new BoxLayout (toolbar, BoxLayout.X_AXIS));
    toolbar.setFloatable (false);

    var toolBarPanel = Box.createVerticalBox();
    toolBarPanel.add (toolbar);
    toolBarPanel.add (new JSeparator (JSeparator.HORIZONTAL));
    controlPanel.add (toolBarPanel, BorderLayout.NORTH);

    // There will be two toggle buttons in the toolbar that select the 
    // cards: one for the view and one for the layers.
    var group = new ButtonGroup();
    boolean isFirst = true;
    toolbar.add (Box.createHorizontalGlue());
    var maxSize = new Dimension();
    var buttonList = new ArrayList<JToggleButton>();
    for (var title : List.of ("View", "Layers")) {
      var button = new JToggleButton (title);
      buttonList.add (button);
      var size = button.getMinimumSize();
      if (size.width > maxSize.width) maxSize.width = size.width;
      if (size.height > maxSize.height) maxSize.height = size.height;
      button.setSelected (isFirst);
      group.add (button);
      button.setHorizontalTextPosition (SwingConstants.CENTER);
      button.addActionListener (event -> cardLayout.show (cards, title));
      button.putClientProperty ("JButton.buttonType", "segmented");
      button.putClientProperty ("JButton.segmentPosition", isFirst ? "first" : "last");
      toolbar.add (button);
      isFirst = false;
    } // for
    maxSize.width += 10;
    for (var button : buttonList) button.setPreferredSize (maxSize);
    viewButton = buttonList.get (0);
    layersButton = buttonList.get (1);
    toolbar.add (Box.createHorizontalGlue());

    // We add these listeners in to receive updates when new layers are added,
    // so we can switch to the layer panel that has the new layer.
    controller.getOverlayChooser().addPropertyChangeListener (
      OverlayListChooser.OVERLAY_LIST_PROPERTY, 
      event -> overlayListEvent (event)
    );
    controller.getSurveyChooser().addPropertyChangeListener (
      SurveyListChooser.SURVEY_LIST_PROPERTY, 
      event -> surveyListEvent (event)
    );
    controller.getAnnotationChooser().addPropertyChangeListener (
      AnnotationListChooser.ANNOTATION_LIST_PROPERTY, 
      event -> annotationListEvent (event)
    );

    // Add the tracking bar from the view panel to the bottom of the layered
    // panel.
    viewPanel = controller.getViewPanel();
    var trackBar = viewPanel.createTrackBar();
    viewLayeredPane.add (trackBar, JLayeredPane.PALETTE_LAYER, ManagedLayeredPane.Position.SOUTH_FULL);

    // Put some useful ancillary information on the left side of the
    // track bar.
    var info = reader.getInfo();
    var startDate = info.getStartDate();
    String timeString;
    if (info.isInstantaneous()) {
      timeString = "Date: " + DateFormatter.formatDate (startDate, MetadataServices.DATE_TIME_FMT);
    } // if
    else {
      var endDate = info.getEndDate();
      timeString = "Start: " + DateFormatter.formatDate (startDate, MetadataServices.DATE_TIME_FMT);
      timeString += "  |  Timespan: " + getDuration ((endDate.getTime() - startDate.getTime()) / 1000);
    } // if
//    trackBar.setLeftLabelText (timeString + "  |  Data: " + info.getOrigin());
    trackBar.setLeftLabelText (timeString);

  } // EarthDataAnalysisPanel constructor

  ////////////////////////////////////////////////////////////

  private String getDuration (long seconds) {

    double value = seconds;
    var units = "s";

    // Check if we should convert to minutes.
    if (value > 60) {
      value = value / 60;
      units = "min";

      // Check if we should convert to hours.
      if (value > 60) {
        value = value/60;
        units = "h";

        // Check if we should convert to days.
        if (value > 24) {
          value = value/24;
          units = "days";

          // Check if we should convert to years.
          if (value > 365) {
            value = value/365;
            units = "years";
          } // if

        } // if

      } // if

    } // if

    return (String.format ("%.1f %s", value, units));

  } // getDuration

  ////////////////////////////////////////////////////////////

  private void showLayerTabWithTitle (String title) {

    int index = layersTabbedPane.indexOfTab (title);
    if (index != -1) layersTabbedPane.setSelectedIndex (index);

  } // showLayerTabWithTitle

  ////////////////////////////////////////////////////////////

  private void overlayListEvent (PropertyChangeEvent event) {

    if (event.getNewValue() != null) {
      layersButton.doClick();
      showLayerTabWithTitle ("Overlay");
    } // if

  } // overlayListEvent

  ////////////////////////////////////////////////////////////

  private void surveyListEvent (PropertyChangeEvent event) {

    if (event.getNewValue() != null) {
      layersButton.doClick();
      showLayerTabWithTitle ("Survey");
    } // if

  } // surveyListEvent

  ////////////////////////////////////////////////////////////

  private void annotationListEvent (PropertyChangeEvent event) {

    if (event.getNewValue() != null) {
      layersButton.doClick();
      showLayerTabWithTitle ("Annotation");
    } // if

  } // annotationListEvent

  ////////////////////////////////////////////////////////////

  /** Disposes of any resources used by this panel. */
  public void dispose () {

    try { reader.close(); }
    catch (IOException e) { }
    controller.dispose();

  } // dispose

  ////////////////////////////////////////////////////////////

  /** Resets the controller interaction mode. */
  public void resetInteraction () { controller.resetInteraction(); }

  ////////////////////////////////////////////////////////////

  @Override
  public Icon getIcon () { return (null); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () {

    String tip = new File (reader.getSource()).getName();
    return (tip);
  
  } // getToolTip

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () {
  
    String title = new File (reader.getSource()).getName();
    return (GUIServices.ellipsisString (title, 20));

  } // getTitle
  
  ////////////////////////////////////////////////////////////

  /** 
   * Loads a set of overlays and enhancement functions.
   *
   * @param file the file to load overlays and enhancements from.
   *
   * @throws IOException if there was an error reading from the specified file.
   * @throws ClassNotFoundException if the classes in the file are unknown
   * to the JVM.
   */
  public void loadProfile (
    File file
  ) throws IOException, ClassNotFoundException {
    
    // Open file
    // ---------
    FileInputStream fileInput = new FileInputStream (file);
    GZIPInputStream zipInput = new GZIPInputStream (fileInput);
    ObjectInputStream objectInput = new ObjectInputStream (zipInput);

    // Read objects
    // ------------
    try {
      List overlayObject = (List) objectInput.readObject();
      String palette = (String) objectInput.readObject();
      EnhancementFunction function = (EnhancementFunction) objectInput.readObject();
      OverlayListChooser overlayChooser = controller.getOverlayChooser();
      overlayChooser.addOverlays(overlayObject);
      EnhancementChooser enhancementChooser = controller.getEnhancementChooser();
      enhancementChooser.setFunction (function);
      PaletteChooser paletteChooser = controller.getPaletteChooser();
      paletteChooser.setPalette (palette);
    } // try
    finally {
      objectInput.close();
      zipInput.close();
      fileInput.close();
    } // finally

  } // loadProfile
  
  ////////////////////////////////////////////////////////////

  /** 
   * Saves a set of overlays and enhancement functions. 
   *
   * @param file the file to create.
   *
   * @throws IOException if there was an error writing to the specified file.
   */
  public void saveProfile (
    File file
  ) throws IOException {

    // Create file
    // -----------
    FileOutputStream fileOutput = new FileOutputStream (file);
    GZIPOutputStream zipOutput = new GZIPOutputStream (fileOutput);
    ObjectOutputStream objectOutput = new ObjectOutputStream (zipOutput);

    // Write objects
    // -------------
    try {
      OverlayListChooser overlayChooser = controller.getOverlayChooser();
      Object overlayObject = overlayChooser.getOverlays();
      PaletteChooser paletteChooser = controller.getPaletteChooser();
      String paletteObject = paletteChooser.getPalette().getName();
      EnhancementChooser enhancementChooser = controller.getEnhancementChooser();
      EnhancementFunction function = enhancementChooser.getFunction();
      objectOutput.writeObject (overlayObject);
      objectOutput.writeObject (paletteObject);
      objectOutput.writeObject (function);
      objectOutput.flush();
    } // try
    finally {
      objectOutput.close();
      zipOutput.close();
      fileOutput.close();
    } // finally

  } // saveProfile

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Get reader
    // ----------
    EarthDataReader reader = null;
    List<String> variables = null;
    try { 
      reader = EarthDataReaderFactory.create (argv[0]); 
      variables = reader.getAllGrids();
    } // try
    catch (IOException e) { e.printStackTrace(); System.exit (1); }

    // Compute stats
    // -------------
    for (int i = 0; i < variables.size(); i++) {
      var name = variables.get (i);     
      LOGGER.fine ("Computing statistics [" + (i+1) + "/" + variables.size() + "] for " + name);
      DataVariable var;
      try { var = reader.getVariable (name); }
      catch (IOException e) { LOGGER.warning ("Computation failed for " + name); continue; }
      var constraints = new DataLocationConstraints();
      constraints.fraction = 0.01;
      var stats = VariableStatisticsGenerator.getInstance().generate (var, constraints);
      reader.putStatistics (name, stats);          
    } // for

    // Create panel
    // ------------
    EarthDataAnalysisPanel panel = null;
    panel = new EarthDataAnalysisPanel (reader, variables);
    panel.setPreferredSize (new Dimension (1200, 800));

    // Add panel to frame
    // ------------------
    final JFrame frame = new JFrame (EarthDataAnalysisPanel.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (panel);
    frame.pack();

    // Create operation chooser frame
    // ------------------------------
    // final JFrame frame2 = new JFrame (ViewOperationChooser.class.getName());
    // frame2.addWindowListener (new WindowMonitor());
    // frame2.setContentPane (ViewOperationChooser.getInstance());
    // frame2.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          GUIServices.centerOnScreen (frame);
          frame.setVisible (true);
//          frame2.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataAnalysisPanel class

////////////////////////////////////////////////////////////////////////
