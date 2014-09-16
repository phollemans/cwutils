////////////////////////////////////////////////////////////////////////
/*
     FILE: cwmaster.java
  PURPOSE: To create master projection data files.
   AUTHOR: Peter Hollemans
     DATE: 2002/12/09
  CHANGES: 2003/01/24, PFH, changed zoom and pan icons slightly
           2003/02/24, PFH, added initial history and changed data to epoch
             origin
           2003/03/29, PFH, changed default datum to WGS84
           2003/04/04, PFH, modified file chooser behaviour
           2003/05/22, PFH, added polygon filling for coastlines
           2004/01/08, PFH, added splash window
           2004/02/21, PFH, added viewPanel.setStaticView (true)
           2004/04/06, PFH
            - fixed problem with setCurrentDirectory()
            - added --nosplash option
           2004/05/22, PFH, modified to use GUIServices.getIcon()
           2004/06/01, PFH
            - modified overlay menu
            - updated to call viewPanel.stopRendering() when needed
           2004/06/03, PFH
            - modified to detect a zero-length pan
            - changed border colors for better legibility
           2004/06/17, PFH, modified to use ColorLookup for colors
           2004/06/18, PFH, added update agent
           2004/10/05, PFH, removed rows and cols usage in metadata
           2005/01/30, PFH, added error dialog to replace standard error
           2005/03/15, PFH, reformatted documentation and usage note
           2005/04/04, PFH, added menu item icons
           2005/04/06, PFH, modified help dialog size
           2005/04/23, PFH, added ToolServices.setCommandLine()
           2005/05/18, PFH, changed "datum" to "spheroid"
           2005/05/21, PFH, removed "Created by" history attribute
           2005/06/20, PFH, added minimized window icon
           2006/03/11, PFH, modified menu keymask for better Mac integration
           2006/05/28, PFH, modified to use MapProjectionFactory
           2006/06/23, PFH, changed --nosplash to --splash command line option
           2006/06/27, PFH, modified to use PoliticalOverlay class
           2006/11/04, PFH, changed to use LONG_NAME for GUI components
           2007/04/19, PFH, added version printing
           2007/07/27, PFH
           - changed DrawingProxy to LightTable
           - temporarily removed invokeLater() call in setMaster()
           2007/09/18, PFH
           - changed to use GUIServices.getFileChooser()
           - added calls to rescanCurrentDirectory() 
           2014/09/15, PFH
           - Changes: Removed splash screen options and replaced splash
             functionality with JRE builtin capability.
           - Issues: In some cases we were getting a slow startup, so we want
             to make sure the user knows there is something happening.


  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// --------
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.render.*;
import jargs.gnu.*;
import jargs.gnu.CmdLineParser.*;

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
 *   cwmaster [OPTIONS] input <br>
 *   cwmaster [OPTIONS]
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
 * Polar Stereographic, Orthograpic, Lambert Conformal Conic, and so
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
 *   <dd>The input data file name.  If specified, the data file is
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
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 *   <li> Unsupported input file format. </li>
 *   <li> Input file does not contain a map projection. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> The following shows the use of cwmaster to load master
 * parameters from a CoastWatch IMGMAP file:</p>
 * <pre>
 *   phollema$ cwmaster 2002_319_2144_n16_wl_c2.cwf
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public final class cwmaster
  extends JFrame {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 0;

  /** Name of program. */
  private static final String PROG = "cwmaster";

  /** The long program name. */
  private static final String LONG_NAME = 
    "CoastWatch Master Tool";

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
  private static final Color VIEW_SOLID = 
    ColorLookup.getInstance().getColor ("water");
  private static final Color VIEW_BACK = Color.BLACK;
  private static final Color VIEW_FORE = Color.WHITE;
  private static final Color VIEW_FILL = 
    ColorLookup.getInstance().getColor ("land");
  private static final Color VIEW_POLITICAL = Color.RED;
  private static final Color VIEW_STATE = VIEW_FILL.darker();

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

  /** The Earth data view panel. */
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
        throw new RuntimeException ("Data center has no valid Earth location");

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
    menuBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.setJMenuBar (menuBar);

    // Create file menu
    // ----------------
    JMenu fileMenu = new JMenu ("File");
    fileMenu.setMnemonic (KeyEvent.VK_F);
    menuBar.add (fileMenu);

    AbstractAction fileAction = new FileAction();
    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem = new JMenuItem (OPEN_COMMAND, 
      GUIServices.getIcon ("menu.open"));
    menuItem.setMnemonic (KeyEvent.VK_O);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, keymask));
    menuItem.addActionListener (fileAction); 
    fileMenu.add (menuItem);

    menuItem = new JMenuItem (SAVE_COMMAND, 
      GUIServices.getIcon ("menu.save"));
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
    menuItem = new JMenuItem (HELP_COMMAND,
      GUIServices.getIcon ("menu.support"));
    menuItem.setMnemonic (KeyEvent.VK_H);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (
      KeyEvent.VK_F1, 0));
    menuItem.addActionListener (helpAction); 
    helpMenu.add (menuItem);
    helpMenu.addSeparator();

    menuItem = new JMenuItem (ABOUT_COMMAND, 
      GUIServices.getIcon ("menu.about"));
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

    JButton applyButton = new JButton (APPLY_COMMAND);
    AbstractAction chooserAction = new ChooserAction();
    applyButton.addActionListener (chooserAction);
    chooserButtonPanel.add (applyButton);

    JButton revertButton = new JButton (REVERT_COMMAND);
    revertButton.addActionListener (chooserAction);
    chooserButtonPanel.add (revertButton);

    // Create view
    // -----------
    EarthDataView view = createView (proj, dims);
    coastOverlay = new CoastOverlay (VIEW_FORE);
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
    gridOverlay = new LatLonOverlay (VIEW_FORE);
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
    button = new JButton (ZOOMIN_COMMAND, 
      GUIServices.getIcon ("view.magnify"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (viewAction);
    toolBar.add (button);

    button = new JButton (ZOOMOUT_COMMAND, 
      GUIServices.getIcon ("view.shrink"));
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (viewAction);
    toolBar.add (button);

    toolButtonGroup = new ButtonGroup ();
    JToggleButton toggleButton;
    toggleButton = new JToggleButton (ZOOM_COMMAND, 
      GUIServices.getIcon ("view.zoom"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (ZOOM_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    toggleButton = new JToggleButton (PAN_COMMAND, 
      GUIServices.getIcon ("view.pan"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (PAN_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    toggleButton = new JToggleButton (RECENTER_COMMAND, 
      GUIServices.getIcon ("view.recenter"));
    toggleButton.setHorizontalTextPosition (SwingConstants.CENTER);
    toggleButton.setVerticalTextPosition (SwingConstants.BOTTOM);
    toggleButton.setIconTextGap (0);
    toggleButton.addActionListener (viewAction);
    toggleButton.getModel().setActionCommand (RECENTER_COMMAND);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    button = new JButton (RESET_COMMAND, 
      GUIServices.getIcon ("view.reset"));
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
    setIconImage (GUIServices.getIcon ("tools.cwmaster").getImage());

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
        int ret = fileChooser.showSaveDialog (cwmaster.this);

        // Read new master
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
          lightTable.setDrawingMode (LightTable.BOX_ZOOM_MODE);
          lightTable.setActive (true);
          viewPanel.setDefaultCursor (Cursor.getPredefinedCursor (
            Cursor.CROSSHAIR_CURSOR));
        } // if
      } // else if

      // Turn on pan mode
      // ----------------
      else if (command.equals (PAN_COMMAND)) {
        if (((JToggleButton) event.getSource()).isSelected ()) {
          lightTable.setDrawingMode (LightTable.IMAGE_MODE);
          lightTable.setActive (true);
          viewPanel.setDefaultCursor (Cursor.getPredefinedCursor (
            Cursor.MOVE_CURSOR));
        } // if
      } // else if

      // Turn on recenter mode
      // ---------------------
      else if (command.equals (RECENTER_COMMAND)) {
        if (((JToggleButton) event.getSource()).isSelected ()) {
          lightTable.setDrawingMode (LightTable.POINT_MODE);
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
      SatelliteDataInfo info = new SatelliteDataInfo ("unknown", "unknown", 
        date, proj, "unknown", "");
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
    proj[0] = (MapProjection) info.getTransform();
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

    SplashScreenManager.updateSplash (LONG_NAME);
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option versionOpt = cmd.addBooleanOption ("version");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage ();
      System.exit (1);
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage ();
      System.exit (0);
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      System.exit (0);
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      System.err.println (PROG + ": At least " + NARGS + 
        " argument(s) required");
      usage ();
      System.exit (1);
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
        e.printStackTrace();
        System.exit (2);
      } // catch
    } // if

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        JFrame frame = new cwmaster (projArray[0], dims);
        frame.addWindowListener (new WindowMonitor());
        frame.addWindowListener (new UpdateAgent (PROG));
        frame.pack();
        GUIServices.createErrorDialog (frame, "Error", 
          ToolServices.ERROR_INSTRUCTIONS);
        frame.setVisible (true);
      } // run
    });

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cwmaster [OPTIONS] input\n" +
"       cwmaster [OPTIONS]\n" +
"Creates map projection master data sets using a graphical user interface.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The initial input data file to open.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  --version                  Show version information.\n"
    );
  } // usage

  ////////////////////////////////////////////////////////////

} // cwmaster class

////////////////////////////////////////////////////////////////////////
