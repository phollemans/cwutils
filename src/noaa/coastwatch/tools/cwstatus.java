////////////////////////////////////////////////////////////////////////
/*

     File: cwstatus.java
   Author: Peter Hollemans
     Date: 2003/01/21

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Insets;

import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.Action;
import javax.swing.JDialog;

import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.HTMLPanel;
import noaa.coastwatch.gui.SatellitePassCoveragePanel;
import noaa.coastwatch.gui.SatellitePassPreviewPanel;
import noaa.coastwatch.gui.SatellitePassTable;
import noaa.coastwatch.gui.SatellitePassTableModel;
import noaa.coastwatch.gui.ServerStatusPanel;
import noaa.coastwatch.gui.SplashScreenManager;
import noaa.coastwatch.gui.UpdateAgent;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.SatellitePassInfo;

/**
 * <p>The status utility shows the status of a CoastWatch data server.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p>
 *   <!-- START NAME -->
 *   cwstatus - shows the status of a CoastWatch data server.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 * <p>
 *   cwstatus [OPTIONS] <br>
 *   cwstatus [OPTIONS] host
 * </p>
 *
 * <h3>Options:</h3>
 * 
 * <p>
 * -c, --script=PATH <br>
 * -h, --help <br>
 * -o, --operator <br>
 * -s, --ssl <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p> The status utility shows the status of a CoastWatch data server
 * using a graphical user interface.  The incoming, unprocessed,
 * processing, and online data are shown and continually updated as
 * the server is processing data.  Individual passes with coverage
 * area and preview image may be selected from a list.  The status
 * utility is designed for use by research personnel and system
 * operators to monitor a CoastWatch data processing server and select
 * data of interest.  Detailed help on the usage of cwstatus is
 * available from within the utility using the menu bar under <i>Help
 * | Help and Support</i>.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt> host </dt>
 *   <dd> The CoastWatch server host name.  There is no default host
 *   name.  If specified, the host is contacted and polled for its
 *   status immediately after the status utility starts.  Otherwise,
 *   the user must connect to the server manually using <i>Connect | New
 *   server</i> on the menu bar. </dd>
 *
 * </dl>
 * 
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt> -c, --script=PATH </dt> 
 *   <dd> ADVANCED USERS ONLY. The query script path.  The default is
 *   /ctera/query.cgi. </dd>
 *
 *   <dt> -h, --help </dt>
 *   <dd> Prints a brief help message. </dd>
 *
 *   <dt> -o, --operator </dt>
 *   <dd> Specifies that operator messages should be displayed.  By
 *   default, errors on the server are not of interest to normal users
 *   and are not displayed.  Operator messages take the form of a
 *   special message box that appears when an error occurs.</dd>
 *
 *   <dt> -s, --ssl </dt>
 *   <dd> Turns on SSL mode.  This makes the server connection use an
 *   SSL-encrypted protocol (https).  The default is to use an unsecured
 *   connection (http). </dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 * <p> 0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p> 
 * The following command shows the startup of the status monitor
 * for the fictitious server <code>frobozz.noaa.gov</code>:</p>
 * <pre>
 *   phollema$ cwstatus frobozz.noaa.gov
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public final class cwstatus
  extends JFrame {

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 0;

  /** Name of program. */
  private static final String PROG = "cwstatus";

  /** The long program name. */
  private static final String LONG_NAME = "CoastWatch Status Tool";

  /** The new server file command. */
  private static final String SERVER_COMMAND = "New server";

  /** The quit command. */
  private static final String QUIT_COMMAND = "Quit";

  /** The help command. */
  private static final String HELP_COMMAND = "Help and support";

  /** The about command. */
  private static final String ABOUT_COMMAND = "About " + LONG_NAME;

  /** The help index file. */
  private final static String HELP_INDEX = "cwstatus_index.html";

  /** The status update delay in milliseconds. */
  public static final int UPDATE_DELAY = 1000 * 60;

  /** The default status script. */
  public static final String DEFAULT_SCRIPT = "/ctera/query.cgi";

  // Variables
  // ---------

  /** The server status panel. */
  private ServerStatusPanel statusPanel;

  /** The online data table. */
  private SatellitePassTable passTable;

  /** The data coverage panel. */
  private SatellitePassCoveragePanel coveragePanel;

  /** The data preview panel. */
  private SatellitePassPreviewPanel previewPanel;

  /** The status update timer. */
  private Timer timer;

  /** The currently selected pass ID. */
  private String selectedPassID;

  /** The current status protocol. */
  private String protocol;

  /** The current status host. */
  private String host;

  /** The status script path. */
  private String path;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new status frame using the specified server.
   *
   * @param protocol the communication protocol.
   * @param host the server host, or null for no initial host.
   * @param path the query script path, or null for the default script.
   * @param operator the operator mode flag.
   */
  public cwstatus (
    String protocol,
    String host,
    String path,
    boolean operator
  ) {

    // Initialize
    // ----------
    super (LONG_NAME);
    this.path = (path == null ? DEFAULT_SCRIPT : path);
    this.protocol = protocol;

    // Create menu bar
    // ---------------
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.setJMenuBar (menuBar);

    // Create connect menu
    // -------------------
    JMenu connectMenu = new JMenu ("Connect");
    connectMenu.setMnemonic (KeyEvent.VK_C);
    menuBar.add (connectMenu);

    AbstractAction connectAction = new ConnectAction();
    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    menuItem = new JMenuItem (SERVER_COMMAND,
      GUIServices.getIcon ("menu.server"));
    menuItem.setMnemonic (KeyEvent.VK_N);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_N, keymask));
    menuItem.addActionListener (connectAction);
    connectMenu.add (menuItem);

    if (!GUIServices.IS_AQUA) {
      connectMenu.addSeparator();
      menuItem = new JMenuItem (QUIT_COMMAND);
      menuItem.setMnemonic (KeyEvent.VK_Q);
      menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q,keymask));
      menuItem.addActionListener (connectAction); 
      connectMenu.add (menuItem);
    } // if

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

    // Create server status panel
    // --------------------------
    statusPanel = new ServerStatusPanel();
    this.getContentPane().add (statusPanel, BorderLayout.NORTH);
    statusPanel.setBorder (new TitledBorder (new EtchedBorder (), 
      "Server Status"));
    statusPanel.setOperator (operator);

    // Create split pane
    // -----------------
    JSplitPane splitPane = new JSplitPane (JSplitPane.VERTICAL_SPLIT, false);
    splitPane.setOneTouchExpandable (true);
    splitPane.setResizeWeight (0.5);
    this.getContentPane().add (splitPane, BorderLayout.CENTER);

    // Create online data table
    // ------------------------
    passTable = new SatellitePassTable();
    JScrollPane scrollPane = new JScrollPane (passTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JPanel listPanel = new JPanel (new BorderLayout());
    listPanel.add (scrollPane, BorderLayout.CENTER);
    splitPane.setTopComponent (listPanel);
    listPanel.setBorder (new TitledBorder (new EtchedBorder (), 
      "Online Data"));
    scrollPane.setPreferredSize (new Dimension (700, 220));
    passTable.getSelectionModel().addListSelectionListener (
      new PassSelected());

    // Create data coverage panel
    // --------------------------
    JPanel bottomPanel = new JPanel (new GridLayout (1, 2));
    splitPane.setBottomComponent (bottomPanel);

    JPanel coveragePanelContainer = new JPanel (new BorderLayout());
    bottomPanel.add (coveragePanelContainer);
    coveragePanelContainer.setBorder (new TitledBorder (new EtchedBorder (), 
      "Data Coverage"));
    coveragePanelContainer.setPreferredSize (new Dimension (220, 220));
    coveragePanel = new SatellitePassCoveragePanel();
    coveragePanelContainer.add (coveragePanel, BorderLayout.CENTER);
    bottomPanel.add (coveragePanelContainer);

    // Create data preview panel
    // -------------------------
    previewPanel = new SatellitePassPreviewPanel();
    previewPanel.setPreferredSize (new Dimension (220, 220));
    bottomPanel.add (previewPanel);
    previewPanel.setBorder (new TitledBorder (new EtchedBorder (), 
      "Data Preview"));

    // Create update timer
    // -------------------
    timer = new Timer (UPDATE_DELAY, new UpdateAction());
    timer.setInitialDelay (0);

    // Set initial server
    // ------------------
    if (host != null) setServer (protocol, host);
    
    // Set minimized window icon
    // -------------------------
    setIconImage (GUIServices.getIcon ("tools.cwstatus").getImage());

  } // cwstatus constructor

  ////////////////////////////////////////////////////////////

  /** Sets the server to get status information from. */
  private void setServer (
    String protocol,
    String host
  ) {

    timer.stop();
    this.protocol = protocol;
    this.host = host;
    statusPanel.setSource (protocol, host, path);
    ((SatellitePassTableModel) passTable.getModel()).setSource (protocol, host, path);
    coveragePanel.setPass (null);
    previewPanel.setPass (null);
    timer.restart();

  } // setServer

  ////////////////////////////////////////////////////////////

  /** Updates the server status and pass table. */
  private void update () {

    // Get selected pass
    // -----------------
    int row = passTable.getSelectedRow();
    SatellitePassTableModel model = (SatellitePassTableModel) passTable.getModel();
    selectedPassID = (row == -1 ? null : model.getPass(row).getPassID());

    // Update status and table
    // -----------------------
    statusPanel.update();
    model.update();

  } // update

  ////////////////////////////////////////////////////////////

  /** Handles update events. */
  private class UpdateAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) { update(); }

  } // UpdateAction

  //////////////////////////////////////////////////////////////////////

  /** 
   * Updates the pass coverage and preview for the currently selected
   * pass.
   */
  private void updatePass () {

    // Get selected row
    // ----------------
    int row = passTable.getSelectedRow();

    // Restore selected pass
    // ---------------------
    if (row == -1 && selectedPassID != null) {
      int newRow = ((SatellitePassTableModel) 
        passTable.getModel()).getPassIndex (selectedPassID);
      if (newRow != -1) {
        passTable.setRowSelectionInterval (newRow, newRow);
        return;
      } // if
    } // if

    // Update coverage and preview displays
    // ------------------------------------
    if (row == -1) return;
    SatellitePassInfo pass = ((SatellitePassTableModel) 
      passTable.getModel()).getPass (row);
    if (pass != coveragePanel.getPass()) coveragePanel.setPass (pass);
    if (pass != previewPanel.getPass()) previewPanel.setPass (pass);

  } // updatePass

  //////////////////////////////////////////////////////////////////////

  /** Handles the pass table selection. */
  class PassSelected
    implements ListSelectionListener {

    public void valueChanged (ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) return;
      updatePass();
    } // valueChanged 

  } // PassSelected class

  ////////////////////////////////////////////////////////////

  /** Handles help operations. */
  private class HelpAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Show help dialog
      // ----------------
      String command = event.getActionCommand();
      if (command.equals (HELP_COMMAND)) {
        URL helpIndex = cwstatus.this.getClass().getResource (HELP_INDEX);
        HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
        helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
        helpPanel.showDialog (cwstatus.this, "Help");
      } // if

      // Show about dialog
      // -----------------
      else if (command.equals (ABOUT_COMMAND)) {
        JOptionPane.showMessageDialog (cwstatus.this, 
          ToolServices.getAbout (LONG_NAME), "About", 
          JOptionPane.INFORMATION_MESSAGE);
      } // else if

    } // actionPerformed

  } // HelpAction class

  ////////////////////////////////////////////////////////////

  /** Handles connect operations. */
  private class ConnectAction
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();
      
      // Set new server
      // --------------
      if (command.equals (SERVER_COMMAND)) {

        JPanel panel = new JPanel();
        panel.setLayout (new BoxLayout (panel, BoxLayout.PAGE_AXIS));
        panel.add (new JLabel ("Enter the new server host name or IP address:"));
        panel.add (Box.createRigidArea (new Dimension (0, 5)));
        JTextField serverField = new JTextField (cwstatus.this.host);
        panel.add (serverField);
        panel.add (Box.createRigidArea (new Dimension (0, 5)));
        JCheckBox sslCheckBox = new JCheckBox ("Use secure connection", cwstatus.this.protocol.equals ("https"));
        sslCheckBox.setSelected (cwstatus.this.protocol.equals ("https"));
        panel.add (sslCheckBox);

        Action okAction = GUIServices.createAction ("OK", () -> {
          String text = serverField.getText();
          boolean flag = sslCheckBox.isSelected();
          if (text != null && !text.trim().equals ("")) {
            setServer ((flag ? "https" : "http"), text);
          } // if
        });
        Action cancelAction = GUIServices.createAction ("Cancel", null);
        JDialog dialog = GUIServices.createDialog (
          cwstatus.this,
          "New server",
          true,
          panel,
          null,
          new Action[] {okAction, cancelAction},
          new boolean[] {true, true},
          true
        );
        dialog.setVisible (true);

      } // if

      // Quit program
      // ------------
      if (command.equals (QUIT_COMMAND)) {
        System.exit (0);
      } // else if

    } // actionPerformed

  } // ConnectAction class

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
    Option scriptOpt = cmd.addStringOption ('c', "script");
    Option operatorOpt = cmd.addBooleanOption ('o', "operator");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option sslOpt = cmd.addBooleanOption ('s', "ssl");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage();
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
      usage();
      System.exit (1);
    } // if
    final String host = (remain.length == 0 ? null : remain[0]);

    // Set defaults
    // ------------
    final String script = (String) cmd.getOptionValue (scriptOpt);
    final boolean operator = (cmd.getOptionValue (operatorOpt) != null);    
    final boolean ssl = (cmd.getOptionValue (sslOpt) != null);

    // Create frame
    // ------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        JFrame frame = new cwstatus ((ssl ? "https" : "http"), host, script, operator);
        frame.addWindowListener (new WindowMonitor());
        frame.addWindowListener (new UpdateAgent (PROG));
        frame.pack();
        GUIServices.createErrorDialog (frame, "Error", ToolServices.ERROR_INSTRUCTIONS);
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
"Usage: cwstatus [OPTIONS]\n" +
"       cwstatus [OPTIONS] host\n" +
"Shows the status of a CoastWatch data server using a graphical user\n" +
"interface.\n" +
"\n" +
"Main parameters:\n" +
"  host                       The CoastWatch server host name.\n" +
"\n" +
"Options:\n" +
"  -c, --script=PATH          Set host query script path (advanced users).\n" +
"  -h, --help                 Show this help message.\n" +
"  -o, --operator             Show operator error messages.\n" +
"  -s, --ssl                  Use SSL-encrypted connection to server.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

} // cwstatus class

////////////////////////////////////////////////////////////////////////
