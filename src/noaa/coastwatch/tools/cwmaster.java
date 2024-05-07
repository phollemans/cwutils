////////////////////////////////////////////////////////////////////////
/*

     File: cwmaster.java
   Author: Peter Hollemans
     Date: 2002/12/09

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.HTMLPanel;
import noaa.coastwatch.gui.LightTable;
import noaa.coastwatch.gui.MapProjectionChooser;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.SplashScreenManager;
import noaa.coastwatch.gui.UpdateAgent;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.io.CWHDFWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.ColorLookup;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.PoliticalOverlay;
import noaa.coastwatch.render.PolygonOverlay;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.SatelliteDataInfo;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The master utility creates map projection master datasets.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwmaster - creates map projection master datasets.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cwmaster [OPTIONS] [input]
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p> The master utility creates map projection master data sets
 * using a graphical user interface.  A master projection specifies
 * the translation between grid row and column coordinates and Earth
 * latitude and longitude coordinates.  A number of common map
 * projections are available such as Mercator, Transverse Mercator,
 * Polar Stereographic, Orthographic, Lambert Conformal Conic, and so
 * on.  Detailed help on the usage of cwmaster is available from
 * within the utility using the menu bar under <i>Help | Help and
 * Support</i>.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input</dt>
 *   <dd>The optional input data file name.  If specified, the data file is
 *   opened and used as the initial map projection master.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt>-h, --help</dt>
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>--version</dt>
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 *   <li> Invalid input file name </li>
 *   <li> Unsupported input file format </li>
 *   <li> Input file does not contain a map projection </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the use of cwmaster to load master
 * parameters from a CoastWatch HDF file:</p>
 * <pre>
 *   phollema$ cwmaster 2021_232_1630_l20_gr.hdf
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
 public final class cwmaster
  extends JFrame {

  private static final String PROG = cwmaster.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 0;

  /** The long program name. */
  private static final String LONG_NAME = "CoastWatch Master Tool";

  /** The file commands. */
  private static final String OPEN_COMMAND = "Open";
  private static final String SAVE_COMMAND = "Save";
  private static final String QUIT_COMMAND = "Quit";

  /** The option commands. */
  private static final String COAST_COMMAND = "Show coast geography";
  private static final String GRID_COMMAND = "Show grid lines";
  private static final String POLITICAL_COMMAND = "Show international borders";
  private static final String STATE_COMMAND = "Show state borders";

  /** The help commands. */
  private static final String HELP_COMMAND = "Help and support";
  private static final String ABOUT_COMMAND = "About " + LONG_NAME;

  /** The chooser commands. */
  private static final String APPLY_COMMAND = "Apply";
  private static final String REVERT_COMMAND = "Revert";

  /** The view panel colors. */
  private static final Color VIEW_SOLID = new Color (158, 188, 214);
  private static final Color VIEW_BACK = Color.BLACK;
  private static final Color VIEW_GRID = Color.WHITE;
  private static final Color VIEW_FILL = new Color (221, 210, 208);
  private static final Color VIEW_POLITICAL = Color.RED;
  private static final Color VIEW_STATE = VIEW_FILL.darker();
  private static final Color VIEW_COAST = Color.BLACK;

  /** The view commands. */
  private static final String ZOOMIN_COMMAND = "Magnify";
  private static final String ZOOMOUT_COMMAND = "Shrink";
  private static final String ZOOM_COMMAND = "Zoom";
  private static final String PAN_COMMAND = "Pan";
  private static final String RECENTER_COMMAND = "Recenter";
  private static final String RESET_COMMAND = "Reset";

  /** The help index file. */
  private final static String HELP_INDEX = "cwmaster_index.html";

  // Variables
  // ---------
  /** The data overlays. */
  private EarthDataOverlay coastOverlay;
  private EarthDataOverlay gridOverlay;
  private PoliticalOverlay politicalOverlay;
  private PoliticalOverlay stateOverlay;

  /** The master projection. */
  private MapProjection proj;

  /** The master dimensions. */
  private int[] dims;

  /** The earth data view panel. */
  private EarthDataViewPanel viewPanel;  

  /** The map projection chooser. */
  private MapProjectionChooser projectionChooser;

  /** The file chooser. */
  private final JFileChooser fileChooser = GUIServices.getFileChooser();

  /** The default master projection. */
  private static MapProjection defaultProj;

  /** The default master dimensions. */
  private static int[] defaultDims;

  /** The light table for the view panel. */
  private LightTable lightTable;

  /** The button group for tool buttons. */
  private ButtonGroup toolButtonGroup;

  ////////////////////////////////////////////////////////////

  /** Initializes the static variables. */
  static {

    // Create default map projection
    // -----------------------------
    defaultDims = new int[] {512, 512};
    try { 
      defaultProj = MapProjectionFactory.getInstance().create (GCTP.MERCAT, 
        0, new double[15], GCTP.WGS84, defaultDims, 
        new EarthLocation (30, -90), new double[] {20000, 20000});
    } // try
    catch (NoninvertibleTransformException e) { }

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new view from the specified parameters. 
   *
   * @param proj the view map projection.
   * @param dims the view dimensions as [rows, columns].
   *
   * @return the new view or null on error.
   */
  private EarthDataView createView (
    MapProjection proj,
    int[] dims
  ) { 

    try { 

      // Check if center point is valid
      // ------------------------------
      DataLocation dataLoc = new DataLocation ((dims[Grid.ROWS]-1)/2.0, 
        (dims[Grid.COLS]-1)/2.0);
      EarthLocation earthLoc = proj.transform (dataLoc);
      if (!earthLoc.isValid())
        throw new RuntimeException ("Data center has no valid earth location");

      // Create new view
      // ---------------
      EarthDataView newView = new SolidBackground (proj, dims, VIEW_SOLID); 
      return (newView);

    } // try

    // Catch error
    // -----------
    catch (Exception error) { 
      JOptionPane.showMessageDialog (this,
        "A problem occurred using the master projection parameters.\n" + 
        error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      return (null); 
    } // catch

  } // createView

  ////////////////////////////////////////////////////////////

  /**
   * Sets the master projection and dimensions.  The view is recreated
   * and redrawn.  The map projection chooser is updated.
   */
  private void setMaster (
    final MapProjection proj,
    final int[] dims
  ) { 

    // Check for redundant call
    // ------------------------
    if (this.proj.equals (proj) && Arrays.equals (this.dims, dims)) {
      projectionChooser.setMapProjection (proj, dims);
      return;
    } // if

    /*
    SwingUtilities.invokeLater (new Runnable() {
      public void run() {
    */

        // Create new view
        // ---------------
        EarthDataView view = createView (proj, dims);
        if (view != null) {

          // Set master projection
          // ---------------------
          cwmaster.this.proj = proj;
          cwmaster.this.dims = dims;
          projectionChooser.setMapProjection (proj, dims);

          // Copy old overlays
          // -----------------
          view.addOverlays (viewPanel.getView().getOverlays());

          // Set new view
          // ------------
          viewPanel.setView (view);

        } // if

        viewPanel.repaint (viewPanel.getVisibleRect());
        /*

      } // run

    });
        */

  } // setMaster

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new master selection frame.
   *
   * @param proj the initial map projection or null.
   * @param dims the initial dimensions as [rows, columns] or null.
   */
  public cwmaster (
    MapProjection proj,
    int[] dims
  ) {

    // Initialize
    // ----------
    super (LONG_NAME);

    // Set master projection
    // ---------------------
    if (proj == null) {
      proj = defaultProj;
      dims = defaultDims;
    } // if
    this.proj = proj;
    this.dims = dims;

    // Create menu bar
    // ---------------
    JMenuBar menuBar = new JMenuBar();

// For now, we remove the raised border for menus, so that look and feels
// operate correctly, some of which may not used a raised border.
//    menuBar.setBorder (new BevelBorder (BevelBorder.RAISED));

    this.setJMenuBar (menuBar);

    // Create file menu
    // ----------------
    JMenu fileMenu = new JMenu ("File");
    fileMenu.setMnemonic (KeyEvent.VK_F);
    menuBar.add (fileMenu);

    AbstractAction fileAction = new FileAction();
    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
//    menuItem = new JMenuItem (OPEN_COMMAND, GUIServices.getIcon ("menu.open"));
    menuItem = new JMenuItem (OPEN_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_O);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, keymask));
    menuItem.addActionListener (fileAction); 
    fileMenu.add (menuItem);

//    menuItem = new JMenuItem (SAVE_COMMAND, GUIServices.getIcon ("menu.save"));
    menuItem = new JMenuItem (SAVE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_S, keymask));
    menuItem.addActionListener (fileAction); 
    fileMenu.add (menuItem);

    if (!GUIServices.IS_AQUA) {
      fileMenu.addSeparator();
      menuItem = new JMenuItem (QUIT_COMMAND);
      menuItem.setMnemonic (KeyEvent.VK_Q);
      menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q,keymask));
      menuItem.addActionListener (fileAction); 
      fileMenu.add (menuItem);
    } // if

    // Create option menu
    // ------------------
    JMenu optionMenu = new JMenu ("Options");
    optionMenu.setMnemonic (KeyEvent.VK_O);
    menuBar.add (optionMenu);

    AbstractAction overlaysAction = new OverlaysAction();
    JCheckBoxMenuItem checkBox;
    checkBox = new JCheckBoxMenuItem (COAST_COMMAND, true);
    checkBox.setMnemonic (KeyEvent.VK_C);
    checkBox.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_C, keymask));
    checkBox.addActionListener (overlaysAction);
    optionMenu.add (checkBox);

    checkBox = new JCheckBoxMenuItem (GRID_COMMAND, true);
    checkBox.setMnemonic (KeyEvent.VK_G);
    checkBox.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_G, keymask));
    checkBox.addActionListener (overlaysAction);
    optionMenu.add (checkBox);

    checkBox = new JCheckBoxMenuItem (POLITICAL_COMMAND, false);
    checkBox.setMnemonic (KeyEvent.VK_I);
    checkBox.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_I, keymask));
    checkBox.addActionListener (overlaysAction);
    optionMenu.add (checkBox);

    checkBox = new JCheckBoxMenuItem (STATE_COMMAND, false);
    checkBox.setMnemonic (KeyEvent.VK_T);
    checkBox.setDisplayedMnemonicIndex (6);
    checkBox.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_T, keymask));
    checkBox.addActionListener (overlaysAction);
    optionMenu.add (checkBox);

    // Create help menu
    // ----------------
    JMenu helpMenu = new JMenu ("Help");
    helpMenu.setMnemonic (KeyEvent.VK_H);
    menuBar.add (helpMenu);

    AbstractAction helpAction = new HelpAction();
//    menuItem = new JMenuItem (HELP_COMMAND, GUIServices.getIcon ("menu.support"));
    menuItem = new JMenuItem (HELP_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_H);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (
      KeyEvent.VK_F1, 0));
    menuItem.addActionListener (helpAction); 
    helpMenu.add (menuItem);
    helpMenu.addSeparator();

//    menuItem = new JMenuItem (ABOUT_COMMAND, GUIServices.getIcon ("menu.about"));
    menuItem = new JMenuItem (ABOUT_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_A);
    menuItem.addActionListener (helpAction); 
    helpMenu.add (menuItem);

    // Create projection chooser panel
    // -------------------------------
    JPanel chooserPanel = new JPanel (new GridBagLayout());
    this.getContentPane().add (chooserPanel, BorderLayout.WEST);    

    projectionChooser = new MapProjectionChooser (proj, dims);
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    chooserPanel.add (projectionChooser, gc);

    JPanel chooserButtonPanel = new JPanel();
    chooserButtonPanel.setLayout (new BoxLayout (chooserButtonPanel,
      BoxLayout.X_AXIS));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 1);
    gc.anchor = GridBagConstraints.NORTHWEST;
    gc.insets = new Insets (2,2,2,2);
    chooserPanel.add (chooserButtonPanel, gc);
    gc.anchor = GridBagConstraints.CENTER;
    gc.insets = new Insets (0,0,0,0);

    JButton applyButton = GUIServices.getTextButton (APPLY_COMMAND);
    AbstractAction chooserAction = new ChooserAction();
    applyButton.addActionListener (chooserAction);
    chooserButtonPanel.add (applyButton);

    JButton revertButton = GUIServices.getTextButton (REVERT_COMMAND);
    revertButton.addActionListener (chooserAction);
    chooserButtonPanel.add (revertButton);

    // Create view
    // -----------
    EarthDataView view = createView (proj, dims);
    coastOverlay = new CoastOverlay (VIEW_COAST);
    ((PolygonOverlay) coastOverlay).setFillColor (VIEW_FILL);
    coastOverlay.setLayer (1);
    view.addOverlay (coastOverlay);
    try {
      politicalOverlay = new PoliticalOverlay (VIEW_POLITICAL);
      politicalOverlay.setInternational (true);
      politicalOverlay.setState (false);
      politicalOverlay.setLayer (2);
      politicalOverlay.setVisible (false);
      view.addOverlay (politicalOverlay);
      stateOverlay = new PoliticalOverlay (VIEW_STATE);
      stateOverlay.setInternational (false);
      stateOverlay.setState (true);
      stateOverlay.setLayer (3);
      stateOverlay.setVisible (false);
      view.addOverlay (stateOverlay);
    } catch (Exception e) { }
    gridOverlay = new LatLonOverlay (VIEW_GRID);
    gridOverlay.setLayer (4);
    view.addOverlay (gridOverlay);

    // Create view panel
    // -----------------
    viewPanel = new EarthDataViewPanel (view);
    viewPanel.setStaticView (true);

    // Create track bar
    // ----------------    
    EarthDataViewPanel.TrackBar trackBar = viewPanel.new TrackBar (
      true, true, false, false);
    trackBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.getContentPane().add (trackBar, BorderLayout.SOUTH);

    // Create light table
    // --------------------
    lightTable = new LightTable (viewPanel);
    lightTable.addChangeListener (new DrawListener()); 
    lightTable.setPreferredSize (new Dimension (512, 512));
    lightTable.setBackground (VIEW_BACK);
    this.getContentPane().add (lightTable, BorderLayout.CENTER);

    // Create tool bar
    // ---------------
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable (false);
    toolBar.setLayout (new GridLayout (1, 0, 2, 2));
    JPanel toolBarPanel = new JPanel (new FlowLayout (FlowLayout.LEFT, 0, 0));
    toolBarPanel.setBorder (new BevelBorder (BevelBorder.RAISED));
    toolBarPanel.add (toolBar);
    this.getContentPane().add (toolBarPanel, BorderLayout.NORTH);

    JButton button;
    button = new JButton (OPEN_COMMAND, GUIServices.getIcon ("file.open"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (fileAction);
    toolBar.add (button);

    button = new JButton (SAVE_COMMAND, GUIServices.getIcon ("file.save"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (fileAction);
    toolBar.add (button);

    ViewAction viewAction = new ViewAction();
    button = new JButton (ZOOMIN_COMMAND, GUIServices.getIcon ("view.magnify"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (viewAction);
    toolBar.add (button);

    button = new JButton (ZOOMOUT_COMMAND, GUIServices.getIcon ("view.shrink"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (viewAction);
    toolBar.add (button);

    toolButtonGroup = new ButtonGroup ();
    JToggleButton toggleButton;
    toggleButton = new JToggleButton (ZOOM_COMMAND, GUIServices.getIcon ("view.zoom"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (ZOOM_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    toggleButton = new JToggleButton (PAN_COMMAND, GUIServices.getIcon ("view.pan"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (PAN_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    toggleButton = new JToggleButton (RECENTER_COMMAND, GUIServices.getIcon ("view.recenter"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (RECENTER_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    button = new JButton (RESET_COMMAND, GUIServices.getIcon ("view.reset"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (viewAction);
    toolBar.add (button);

    // Setup file chooser
    // ------------------
    SimpleFileFilter filter = new SimpleFileFilter (
      new String[] {"cwf", "hdf"}, "CoastWatch data");
    fileChooser.addChoosableFileFilter (filter);

    // Set minimized window icon
    // -------------------------
    setIconImages (List.of (
      GUIServices.getIcon ("tools.cwmaster").getImage(),
      GUIServices.getIcon ("tools.cwmaster.taskbar").getImage()
    ));

  } // cwmaster constructor

  ////////////////////////////////////////////////////////////

  /** Handles help operations. */
  private class HelpAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Show help dialog
      // ----------------
      String command = event.getActionCommand();
      if (command.equals (HELP_COMMAND)) {
        URL helpIndex = cwmaster.this.getClass().getResource (HELP_INDEX);
        HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
        helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
        helpPanel.showDialog (cwmaster.this, "Help");
      } // if

      // Show about dialog
      // -----------------
      else if (command.equals (ABOUT_COMMAND)) {
        JOptionPane.showMessageDialog (cwmaster.this, 
          ToolServices.getAbout (LONG_NAME), "About", 
          JOptionPane.INFORMATION_MESSAGE);
      } // else if

    } // actionPerformed

  } // HelpAction class

  ////////////////////////////////////////////////////////////

  /** Handles file operations. */
  private class FileAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Open new file
      // -------------
      String command = event.getActionCommand();
      if (command.equals (OPEN_COMMAND)) {

        // Show file chooser
        // -----------------
        fileChooser.rescanCurrentDirectory();
        int ret = fileChooser.showOpenDialog (cwmaster.this);

        // Read new master
        // ---------------
        if (ret == JFileChooser.APPROVE_OPTION) {
          try {
            String file = fileChooser.getSelectedFile().getPath();
            MapProjection[] projArray = new MapProjection[1];
            int[] dims = new int[2];
            readMaster (file, projArray, dims);
            setMaster (projArray[0], dims);
          } // try
          catch (Exception error) {
            JOptionPane.showMessageDialog (cwmaster.this,
              "A problem occurred reading the master file.\n" + 
              error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
        } // if

      } // if

      // Save master to file
      // -------------------
      else if (command.equals (SAVE_COMMAND)) {

        // Show file chooser
        // -----------------
        fileChooser.rescanCurrentDirectory();
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
          File currentDir = fileChooser.getCurrentDirectory();
          selectedFile = new File (currentDir, "Untitled.hdf");
          fileChooser.setSelectedFile (selectedFile);
        } // if
        int ret = fileChooser.showSaveDialog (cwmaster.this);

        // Save new master
        // ---------------
        if (ret == JFileChooser.APPROVE_OPTION) {
          try {
            String file = fileChooser.getSelectedFile().getPath();
            writeMaster (file, proj, dims);
          } // try
          catch (Exception error) {
            JOptionPane.showMessageDialog (cwmaster.this,
              "A problem occurred writing the master file.\n" + 
              error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
        } // if

      } // else if

      // Quit program
      // ------------
      else if (command.equals (QUIT_COMMAND)) {
        System.exit (0);
      } // else if

    } // actionPerformed

  } // FileAction class

  ////////////////////////////////////////////////////////////

  /** Handles a change in overlay graphics. */
  private class OverlaysAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Initialize
      // ----------
      String command = event.getActionCommand();
      JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();
      boolean selected = item.isSelected();

      // Change overlay visibility
      // -------------------------
      viewPanel.stopRendering();
      if (command.equals (COAST_COMMAND))
        coastOverlay.setVisible (selected);
      else if (command.equals (GRID_COMMAND))
        gridOverlay.setVisible (selected);
      else if (command.equals (POLITICAL_COMMAND))
        politicalOverlay.setVisible (selected);
      else if (command.equals (STATE_COMMAND))
        stateOverlay.setVisible (selected);
      viewPanel.getView().setChanged();

      // Repaint view panel
      // ------------------
      viewPanel.repaint();

    } // actionPerformed

  } // OverlaysAction class

  ////////////////////////////////////////////////////////////

  private class ChooserAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Apply map projection changes
      // ----------------------------
      String command = event.getActionCommand();
      if (command.equals (APPLY_COMMAND)) {
        try {
          MapProjection newProj = projectionChooser.getMapProjection(); 
          int[] newDims = projectionChooser.getDimensions();
          setMaster (newProj, newDims);
        } // try
        catch (Exception error) {
          JOptionPane.showMessageDialog (cwmaster.this,
            "A problem occurred parsing the projection parameters.\n" + 
            error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        } // catch
      } // if

      // Revert chooser to current map projection
      // ----------------------------------------
      else if (command.equals (REVERT_COMMAND)) {
        projectionChooser.setMapProjection (proj, dims);
      } // else if

    } // actionPerformed

  } // ChooserAction class

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the light table. */
  private class DrawListener
    implements ChangeListener {

    public void stateChanged (ChangeEvent event) {

      // Initialize
      // ----------
      String command = toolButtonGroup.getSelection().getActionCommand();
      Shape s = lightTable.getShape();
      EarthLocation center = null;
      double[] pixelDims = proj.getPixelDimensions();
      EarthDataView view = viewPanel.getView();
      EarthImageTransform trans = view.getTransform();

      // Zoom in on box
      // --------------
      if (command.equals (ZOOM_COMMAND)) {

        // Check if box is empty
        // ---------------------
        Rectangle2D rect = (Rectangle2D) s;
        int width = (int) rect.getWidth();
        int height = (int) rect.getHeight();
        if (width == 0 || height == 0) return;

        // Set center point and pixel dimensions
        // -------------------------------------
        Point2D centerPoint = viewPanel.translate (
          new Point2D.Double (rect.getCenterX(), rect.getCenterY()));
        center = trans.transform (centerPoint);
        Dimension viewDims = trans.getImageTransform().getImageDimensions();
        double factor = Math.max (rect.getWidth() / viewDims.width,
          rect.getHeight() / viewDims.height);
        pixelDims[0] *= factor;
        pixelDims[1] *= factor;

      } // if

      // Pan to new area
      // ---------------
      else if (command.equals (PAN_COMMAND)) {
        Line2D line = (Line2D) s;
        Point2D p1 = line.getP1();
        Point2D p2 = line.getP2();
        if (p1.equals (p2)) return;
        double[] offset = new double[] {
          p1.getX() - p2.getX(),
          p1.getY() - p2.getY()
        };
        Dimension viewDims = trans.getImageTransform().getImageDimensions();
        Point2D oldCenter = new Point2D.Double ((viewDims.width-1)/2.0,
          (viewDims.height-1)/2.0);
        Point2D newCenter = new Point2D.Double (
          oldCenter.getX() + offset[0],
          oldCenter.getY() + offset[1]
        );
        center = trans.transform (newCenter);

      } // else if

      // Recenter on target
      // ------------------
      else if (command.equals (RECENTER_COMMAND)) {
        Point2D centerPoint = viewPanel.translate (((Line2D) s).getP1());
        center = trans.transform (centerPoint);
      } // else if

      // Create new projection
      // ---------------------
      MapProjection newProj;
      try {
        newProj = MapProjectionFactory.getInstance().create (proj.getSystem(),
          proj.getZone(), proj.getParameters(), proj.getSpheroid(), dims,
          center, pixelDims);
      } // try
      catch (Exception error) {
        JOptionPane.showMessageDialog (cwmaster.this,
          "A problem occurred creating the new projection.\n" + 
          error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } // catch

      // Set projection active
      // ---------------------
      setMaster (newProj, dims);

    } // stateChanged

  } // DrawListener class

  ////////////////////////////////////////////////////////////

  private class ViewAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Zoom in or out
      // --------------
      String command = event.getActionCommand();
      if (command.equals (ZOOMIN_COMMAND) ||
        command.equals (ZOOMOUT_COMMAND)) {

        // Create new projection
        // ---------------------
        MapProjection newProj;
        try {
          double[] pixelDims = proj.getPixelDimensions();
          double factor = (command.equals (ZOOMIN_COMMAND) ? 0.5 : 2);
          pixelDims[0] *= factor;
          pixelDims[1] *= factor;
          EarthLocation center = proj.transform (new DataLocation (
            (dims[Grid.ROWS]-1)/2.0, (dims[Grid.COLS]-1)/2.0));
          newProj = MapProjectionFactory.getInstance().create (
            proj.getSystem(), proj.getZone(), proj.getParameters(), 
            proj.getSpheroid(), dims, center, pixelDims);
        } // try
        catch (Exception error) {
          JOptionPane.showMessageDialog (cwmaster.this,
            "A problem occurred creating the new projection.\n" + 
            error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          return;
        } // catch

        // Set projection active
        // ---------------------
        setMaster (newProj, dims);

      } // if

      // Turn on zoom mode
      // -----------------
      else if (command.equals (ZOOM_COMMAND)) {
        if (((JToggleButton) event.getSource()).isSelected ()) {
          lightTable.setDrawingMode (LightTable.Mode.BOX_ZOOM);
          lightTable.setActive (true);
          viewPanel.setDefaultCursor (Cursor.getPredefinedCursor (
            Cursor.CROSSHAIR_CURSOR));
        } // if
      } // else if

      // Turn on pan mode
      // ----------------
      else if (command.equals (PAN_COMMAND)) {
        if (((JToggleButton) event.getSource()).isSelected ()) {
          lightTable.setDrawingMode (LightTable.Mode.IMAGE);
          lightTable.setActive (true);
          viewPanel.setDefaultCursor (Cursor.getPredefinedCursor (
            Cursor.MOVE_CURSOR));
        } // if
      } // else if

      // Turn on recenter mode
      // ---------------------
      else if (command.equals (RECENTER_COMMAND)) {
        if (((JToggleButton) event.getSource()).isSelected ()) {
          lightTable.setDrawingMode (LightTable.Mode.POINT);
          lightTable.setActive (true);
          viewPanel.setDefaultCursor (Cursor.getPredefinedCursor (
            Cursor.CROSSHAIR_CURSOR));
        } // if
      } // else if
      
      // Reset view
      // ----------
      else if (command.equals (RESET_COMMAND)) {
        setMaster (defaultProj, defaultDims);
      } // else if

    } // actionPerformed

  } // ViewAction class

  ////////////////////////////////////////////////////////////

  /**
   * Write the master projection parameters to a file.
   *
   * @param file the file name to read from.
   * @param proj the map projection (modified).
   * @param dims the dimensions as [rows, columns] (modified).
   *
   * @throws IOException if an error occurred writing the master file.
   */
  private static void writeMaster (
    String file,
    MapProjection proj,
    int[] dims
  ) throws IOException {

    try {
      Date date = new Date (0L);
      SatelliteDataInfo info = new SatelliteDataInfo ("Unknown", "Unknown",
        date, proj, "Unknown", "");
      CWHDFWriter writer = new CWHDFWriter (info, file);
      int sdid = writer.getSDID();
      writer.close();
    } catch (Exception e) { throw new IOException (e.getMessage()); }

  } // writeMaster

  ////////////////////////////////////////////////////////////

  /**
   * Reads the master projection parameters from a file.
   *
   * @param file the file name to read from.
   * @param proj the map projection (modified).
   * @param dims the dimensions as [rows, columns] (modified).
   *
   * @throws IOException if an error occurred reading the master file.
   */
  private static void readMaster (
    String file,
    MapProjection[] proj,
    int[] dims
  ) throws IOException {

    EarthDataReader reader = EarthDataReaderFactory.create (file);
    EarthDataInfo info = reader.getInfo();
    var trans = info.getTransform();
    if (!(trans instanceof MapProjection)) 
      throw new IOException ("File does not contain a usable map projection");
    proj[0] = (MapProjection) trans;
    System.arraycopy (proj[0].getDimensions(), 0, dims, 0, 2);
    reader.close();

  } // readMaster

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) {

    SplashScreenManager.updateSplash (LONG_NAME, ToolServices.getVersion());
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option versionOpt = cmd.addBooleanOption ("version");
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
      usage ();
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

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // if
    String input = (remain.length == 0 ? null : remain[0]);

    // Read master
    // -----------
    final MapProjection[] projArray = new MapProjection[1];
    final int[] dims = new int[2];
    if (input != null) {
      try {
        readMaster (input, projArray, dims);
      } // try
      catch (Exception e) {
        LOGGER.log (Level.SEVERE, "Aborting", ToolServices.shortTrace (e, "noaa.coastwatch"));
        ToolServices.exitWithCode (2);
        return;
      } // catch
    } // if

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        GUIServices.initializeLaf();        
        JFrame frame = new cwmaster (projArray[0], dims);
        frame.addWindowListener (new WindowMonitor());
        frame.addWindowListener (new UpdateAgent (PROG));
        frame.pack();
        GUIServices.createErrorDialog (frame, "Error", ToolServices.ERROR_INSTRUCTIONS);
        frame.setVisible (true);
      } // run
    });

  } // main

  ////////////////////////////////////////////////////////////

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cwmaster");

    info.func ("Creates map projection master datasets interactively");

    info.param ("[input]", "The optional initial input data file to open");

    info.option ("-h, --help", "Show help message");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

  private cwmaster () { }

  ////////////////////////////////////////////////////////////

} // cwmaster class

////////////////////////////////////////////////////////////////////////
