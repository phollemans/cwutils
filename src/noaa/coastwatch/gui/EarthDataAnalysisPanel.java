////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataAnalysisPanel.java
  PURPOSE: Groups together related Earth data view and chooser
           components in one panel.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/17
           2006/10/24, PFH, added tab icon
           2006/10/30, PFH, added legend panel
           2006/11/06, PFH, added help buttons
           2006/12/14, PFH, added showNavAnalysisDialog()
           2007/07/26, PFH, added showFullScreen()
           2007/08/09, PFH, added width to control tabs for overlay selection
           2007/12/21, PFH, added with to control tabs for enhance buttons
           2011/05/13, XL,  added methods to load and save profiles of overlays 
           					and enhancement functions

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.*;
import javax.swing.border.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.gui.nav.*;

/**
 * The <code>EarthDataAnalysisPanel</code> groups together a variety
 * of Earth data view and chooser components into one main panel that
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

  /** The full screen window for this panel or null if not yet created. */
  private FullScreenWindow fsWindow;

  ////////////////////////////////////////////////////////////

  /** 
   * Shows this analysis panel in a full screen mode.
   *
   * @throws UnsupportedOperationException if full screen mode is
   * not supported.
   */
  public void showFullScreen () {

    // Create window
    // -------------
    ViewOperationChooser opChooser = ViewOperationChooser.getInstance();
    if (fsWindow == null) {
      fsWindow = new FullScreenWindow (lightTable,
        opChooser.getFullScreenChooser());
      opChooser.addPropertyChangeListener (
        ViewOperationChooser.OPERATION_PROPERTY, new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent event) {
              if (event.getNewValue().equals (ViewOperationChooser.CLOSE)) {
                if (fsWindow != null && fsWindow.isFullScreen())
                  fsWindow.stop();
              } // if
            } // propertyChange
          });
    } // if

    // Show window
    // -----------
    opChooser.performOperation (ViewOperationChooser.PAN);
    fsWindow.start();

  } // showFullScreen

  ////////////////////////////////////////////////////////////

  /** Gets the reader used by this panel. */
  public EarthDataReader getReader () { return (reader); }

  ////////////////////////////////////////////////////////////

  /** Gets the Earth data view from the view panel. */
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
    List variableList
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
    VariableChooser chooser = controller.getVariableChooser();
    centerPanel.add (chooser, BorderLayout.NORTH);
    viewPanel = controller.getViewPanel();
    lightTable = controller.getLightTable();
    lightTable.setPreferredSize (new Dimension (VIEW_PANEL_SIZE, 
      VIEW_PANEL_SIZE));
    lightTable.setBackground (VIEW_BACK);

    JPanel viewPanelContainer = new JPanel (new BorderLayout());
    viewPanelContainer.setBorder (new BevelBorder (BevelBorder.LOWERED));
    viewPanelContainer.add (lightTable, BorderLayout.CENTER);
    centerPanel.add (viewPanelContainer, BorderLayout.CENTER);

    LegendPanel legendPanel = controller.getLegendPanel();
    legendPanel.setPreferredSize (new Dimension (90, 0));
    centerPanel.add (legendPanel, BorderLayout.EAST);
    this.add (centerPanel, BorderLayout.CENTER);
    
    // Create tabbed pane
    // ------------------
    Insets tabInsets = (Insets) UIManager.get ("TabbedPane.tabInsets");
    Insets smallTabInsets = (Insets) tabInsets.clone();
    smallTabInsets.left = 4;
    smallTabInsets.right = 4;
    UIManager.put ("TabbedPane.tabInsets", smallTabInsets);
    JTabbedPane tabbedPane = new JTabbedPane();
    UIManager.put ("TabbedPane.tabInsets", tabInsets);
    List tabs = controller.getTabComponentPanels();
    for (Iterator iter = tabs.iterator(); iter.hasNext();) {
      TabComponent tab = (TabComponent) iter.next();
      JPanel tabPanel = new JPanel (new BorderLayout());
      Box helpBox = Box.createHorizontalBox();
      helpBox.add (GUIServices.getHelpButton (tab.getClass()));
      helpBox.add (Box.createHorizontalGlue());
      tabPanel.add (helpBox, BorderLayout.SOUTH);
      tabPanel.add ((Component) tab, BorderLayout.CENTER);
      tabbedPane.addTab (tab.getTitle(), tab.getIcon(), tabPanel,
        tab.getToolTip());
    } // for
    this.add (tabbedPane, BorderLayout.WEST);
    tabbedPane.setPreferredSize (new Dimension (TABBED_PANE_WIDTH, 0));

    // Put track bar at the bottom
    // ---------------------------
    EarthDataViewPanel.TrackBar trackBar = viewPanel.new TrackBar (
      true, true, true, false);
    trackBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.add (trackBar, BorderLayout.SOUTH);

  } // EarthDataAnalysisPanel constructor

  ////////////////////////////////////////////////////////////

  /** Disposes of any resources used by this controller. */
  public void dispose () {

    try { reader.close(); }
    catch (IOException e) { }
    controller.dispose();

  } // dispose

  ////////////////////////////////////////////////////////////

  /** Resets the controller interaction mode. */
  public void resetInteraction () { controller.resetInteraction(); }

  ////////////////////////////////////////////////////////////

  /** Gets the panel tab icon. */
  public Icon getIcon () { return (TAB_ICON); }

  ////////////////////////////////////////////////////////////

  /** Gets the panel tooltip. */
  public String getToolTip () { return (null); }

  ////////////////////////////////////////////////////////////

  /** Gets the panel title. */
  public String getTitle () { 

    return (new File (reader.getSource()).getName()); 

  } // getTitle
  
  
  ////////////////////////////////////////////////////////////

  /** Load profiles of overlays and enhancement functions. */
  public void loadProfile(File file)
  	throws IOException, ClassNotFoundException{
	  FileInputStream fileInput = new FileInputStream (file);
	  GZIPInputStream zipInput = new GZIPInputStream (fileInput);
	  ObjectInputStream objectInput = new ObjectInputStream (zipInput);

	  // Read object
	  // -----------
	  try {
	      //Object paletteObject = objectInput.readObject();
	      List overlayObject = (List) objectInput.readObject();
	      String palette = (String) objectInput.readObject();
	      EnhancementFunction function = (EnhancementFunction) objectInput.readObject();
	      OverlayListChooser overlayChooser = controller.getOverlayChooser();
		  overlayChooser.addOverlays(overlayObject);
		  EnhancementChooser enhancementChooser = controller.getEnhancementChooser();
		  enhancementChooser.setFunction(function);
		  PaletteChooser paletteChooser = controller.getPaletteChooser();
		  paletteChooser.setPalette(palette);
	  } // try
	  finally {
	      objectInput.close();
	      zipInput.close();
	      fileInput.close();
	  } // finally
  }
  
  ////////////////////////////////////////////////////////////

  /** Save profiles of overlays and enhancement functions. */
  public void saveProfile(File file)
  throws IOException{
	  FileOutputStream fileOutput = new FileOutputStream (file);
	  GZIPOutputStream zipOutput = new GZIPOutputStream (fileOutput);
	  ObjectOutputStream objectOutput = new ObjectOutputStream (zipOutput);

	    // Write object
	    // ------------
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
  }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
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
