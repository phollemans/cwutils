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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
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
import noaa.coastwatch.gui.nav.NavigationAnalysisPanel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.Palette;

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
  implements TabComponent {

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

  /** The icon for each tab. */
  private static final Icon TAB_ICON = GUIServices.getIcon ("analysis.tab");

  /** The maximum length of the tab title. */
  private static final int MAX_TITLE_LENGTH = 20;

  // Variables
  // ---------

  /** The reader object. */
  private EarthDataReader reader;

  /** The view panel. */
  private EarthDataViewPanel viewPanel;

  /** The light table that holds the view panel. */
  private LightTable lightTable;

  /** The controller object. */
  private EarthDataViewController controller;

  /** The tabbed pane displaying the view control tabs. */
  private JTabbedPane tabbedPane;

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

    tabbedPane.setVisible (isVisible);

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

    // Create window
    // -------------
    ViewOperationChooser opChooser = ViewOperationChooser.getInstance();
    FullScreenWindow window = new FullScreenWindow (lightTable, opChooser.getFullScreenChooser());
    opChooser.addPropertyChangeListener (ViewOperationChooser.OPERATION_PROPERTY, event -> {
      if (event.getNewValue().equals (ViewOperationChooser.CLOSE)) {
        if (window.isFullScreen()) window.stop();
      } // if
    });

    // Show window
    // -----------
    opChooser.performOperation (ViewOperationChooser.PAN);
    window.start();

  } // showFullScreen

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

    // Set reader
    // ----------
    this.reader = reader;

    // Create view controller
    // ----------------------
    controller = new EarthDataViewController (reader, variableList);

    // Create center panel with variable chooser and view
    // --------------------------------------------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    
    
    
/*
    centerPanel.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent event) {
        System.out.println ("centerPanel: \n" +
          "  size = " + event.getComponent().getSize() + "\n" +
          "  pref = " + event.getComponent().getPreferredSize());
      } // componentResized
    });
*/
    
    
    
    VariableChooser chooser = controller.getVariableChooser();
    centerPanel.add (chooser, BorderLayout.NORTH);
    viewPanel = controller.getViewPanel();
    lightTable = controller.getLightTable();
    lightTable.setPreferredSize (new Dimension (VIEW_PANEL_SIZE, 
      VIEW_PANEL_SIZE));
    lightTable.setBackground (VIEW_BACK);




/*
    viewPanel.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent event) {
        System.out.println ("viewPanel: \n" +
          "  size = " + event.getComponent().getSize() + "\n" +
          "  pref = " + event.getComponent().getPreferredSize());
      } // componentResized
    });
    lightTable.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent event) {
        System.out.println ("lightTable: \n" +
          "  size = " + event.getComponent().getSize() + "\n" +
          "  pref = " + event.getComponent().getPreferredSize());
      } // componentResized
    });
*/



    JPanel viewPanelContainer = new JPanel (new BorderLayout());
    viewPanelContainer.setBorder (new BevelBorder (BevelBorder.LOWERED));
    viewPanelContainer.add (lightTable, BorderLayout.CENTER);
    centerPanel.add (viewPanelContainer, BorderLayout.CENTER);





/*
    viewPanelContainer.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent event) {
        System.out.println ("viewPanelContainer: \n" +
          "  size = " + event.getComponent().getSize() + "\n" +
          "  pref = " + event.getComponent().getPreferredSize());
      } // componentResized
    });
*/





    LegendPanel legendPanel = controller.getLegendPanel();
    legendPanel.setPreferredSize (new Dimension (120, 0));
    centerPanel.add (legendPanel, BorderLayout.EAST);
    this.add (centerPanel, BorderLayout.CENTER);
    
    // Create tabbed pane
    // ------------------
    Insets tabInsets = (Insets) UIManager.get ("TabbedPane.tabInsets");
    Insets smallTabInsets = (tabInsets == null ? new Insets (0, 0, 0, 0) : (Insets) tabInsets.clone());
    smallTabInsets.left = 4;
    smallTabInsets.right = 4;
    UIManager.put ("TabbedPane.tabInsets", smallTabInsets);
    tabbedPane = new JTabbedPane();
    UIManager.put ("TabbedPane.tabInsets", tabInsets);
    List<TabComponent> tabs = controller.getTabComponentPanels();
    for (TabComponent tab : tabs) {
      JPanel tabPanel = new JPanel (new BorderLayout());
      Box helpBox = Box.createHorizontalBox();
      helpBox.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
      helpBox.add (GUIServices.getHelpButton (tab.getClass()));
      helpBox.add (Box.createHorizontalGlue());
      tabPanel.add (helpBox, BorderLayout.SOUTH);
      tabPanel.add ((Component) tab, BorderLayout.CENTER);
      tabbedPane.addTab (tab.getTitle(), tab.getIcon(), tabPanel, tab.getToolTip());
    } // for
    this.add (tabbedPane, BorderLayout.WEST);
    tabbedPane.setPreferredSize (new Dimension (TABBED_PANE_WIDTH, 0));

    // Put track bar at the bottom
    // ---------------------------
    EarthDataViewPanel.TrackBar trackBar = viewPanel.new TrackBar (
      true, true, true, false);
    trackBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.add (trackBar, BorderLayout.SOUTH);





/*
    trackBar.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent event) {
        System.out.println ("trackBar: \n" +
          "  size = " + event.getComponent().getSize() + "\n" +
          "  pref = " + event.getComponent().getPreferredSize());
      } // componentResized
    });
*/







  } // EarthDataAnalysisPanel constructor

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
    try { reader = EarthDataReaderFactory.create (argv[0]); }
    catch (IOException e) { e.printStackTrace(); System.exit (1); }

    // Create panel
    // ------------
    EarthDataAnalysisPanel panel = null;
    try { panel = new EarthDataAnalysisPanel (reader, reader.getAllGrids()); }
    catch (IOException e) { e.printStackTrace(); System.exit (1); }

    // Add panel to frame
    // ------------------
    final JFrame frame = new JFrame (EarthDataAnalysisPanel.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (panel);
    frame.pack();

    // Create operation chooser frame
    // ------------------------------
    final JFrame frame2 = new JFrame (ViewOperationChooser.class.getName());
    frame2.addWindowListener (new WindowMonitor());
    frame2.setContentPane (ViewOperationChooser.getInstance());
    frame2.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
          frame2.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataAnalysisPanel class

////////////////////////////////////////////////////////////////////////
