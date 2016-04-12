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
           2014/11/11, PFH
           - Changes: Added ability to get/set view panel size.
           - Issue: We wanted to be able to let the user set the view panel size
             independently of the enclosing panels, so that data exports could
             be a consistent size.
 
  CoastWatch Software Library and Utilities
  Copyright 2004-2014, USDOC/NOAA/NESDIS CoastWatch

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

  /** The full screen window for this panel or null if not yet created. */
  private FullScreenWindow fsWindow;

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

  /** 
   * Gets the reader used by this panel.
   * 
   * @return the reader object.
   */
  public EarthDataReader getReader () { return (reader); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the Earth data view from the view panel.
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

/*
 * We're experimenting here with smaller tab titles.  In reality what would
 * be really great is if the tabs would alter size according to how many
 * tabs exist, and each tab would get an equal amount of space.  Then their
 * titles would be some truncated version of the file name.
 */
 
    int length = title.length();
    if (length > MAX_TITLE_LENGTH) {
      String head = title.substring (0, MAX_TITLE_LENGTH/2-2);
      String tail = title.substring (length - MAX_TITLE_LENGTH/2-1, length);
      title = head + "..." + tail;
    } // if
    

    return (title);
    
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
