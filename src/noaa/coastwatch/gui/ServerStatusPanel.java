////////////////////////////////////////////////////////////////////////
/*
     FILE: ServerStatusPanel.java
  PURPOSE: To show the server processing status.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/14
  CHANGES: 2003/03/26, PFH, modified to use new ServerQuery
           2003/07/28, PFH, added operator mode
           2004/05/22, PFH, modified to use GUIServices.getIcon()
           2004/06/01, PFH, added setSource() method
           2004/11/14, PFH, fixed event dispatch lockup bug in update()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.net.ServerQuery;

/**
 * The server status panel queries the status of a CoastWatch data
 * server and displays the current activity including incoming,
 * unprocessed, and processing files.  The status monitor may be used
 * in an operator mode in which case significant error conditions
 * are reported in the form of a message dialog box.
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class ServerStatusPanel
  extends JPanel {

  // Constants
  // ---------

  /** The error audio clip. */
  private static final String ERROR_CLIP = "doorbell.wav";

  /** The network error message. */
  private static final String NETWORK_ERROR = 
    "A network connection error has occurred.  A problem may exist\n" +
    "in the physical network between the status monitoring machine\n" +
    "and the host machine or the host machine may not be responding\n" +
    "to status requests.  Please contact the system administrator\n" + 
    "of the host machine or wait for the network error to clear.";

  // Variables
  // ---------
  /** The server host and query path. */
  private String host, path;

  /** The current status labels. */
  private JLabel hostLabel;
  private JLabel idleLabel;
  private JLabel incomingLabel;
  private JLabel processingLabel;
  private JLabel errorLabel;
  private JLabel activityLabel;

  /** The current status text areas. */
  private JTextArea incomingArea;
  private JTextArea unprocessedArea;

  /** The operator flag, true to show error dialogs. */
  private boolean operator;

  /** The error condition flag, true if an error dialog is deployed. */
  private boolean errorState;

  /** The error condition sound effect. */
  private AudioClip errorSound;

  /** The current update worker thread. */
  private Thread worker;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the operator mode to show dialog panels when an error
   * occurs. By default operator mode is off.
   */
  public void setOperator (boolean flag) { operator = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new status panel with no server.  The
   * <code>setSource()</code> method should be called prior to
   * performing any status updates.
   */
  public ServerStatusPanel () {

    this ("", "");

  } // ServerStatusPanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the panel to query the status from a new data source.  The
   * current status is cleared.
   *
   * @param host the server host.
   * @param path the query script path.
   */
  public synchronized void setSource (
    String host,
    String path
  ) {

    // Set host and path
    // -----------------
    this.host = host;
    this.path = path;

    // Clear icons
    // -----------
    idleLabel.setEnabled (false);
    incomingLabel.setEnabled (false);
    processingLabel.setEnabled (false);
    errorLabel.setEnabled (false);

    // Clear labels
    // ------------
    activityLabel.setText ("");
    hostLabel.setText (host);

    // Clear lists
    // -----------
    incomingArea.setText ("");
    unprocessedArea.setText ("");

  } // setSource

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new status panel using the specified server.
   *
   * @param host the server host.
   * @param path the query script path.
   *
   * @see ServerQuery
   */
  public ServerStatusPanel (
    String host,
    String path
  ) {

    // Initialize
    // ----------
    super (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets (2,2,2,2);

    // Create host line
    // ----------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 0, 0);
    add (new JLabel ("Host:"), gc);
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.BOTH, 1, 0);
    hostLabel = new JLabel();
    add (hostLabel, gc);

    // Create activity line
    // --------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 0, 0);
    add (new JLabel ("Activity:"), gc);

    JPanel activityBar = new JPanel();
    activityBar.setLayout (new BoxLayout (activityBar, BoxLayout.X_AXIS));
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.BOTH, 1, 0);
    add (activityBar, gc);

    idleLabel = new JLabel (GUIServices.getIcon ("status.idle"));
    idleLabel.setBorder (new EtchedBorder ());
    idleLabel.setToolTipText ("Idle");
    activityBar.add (idleLabel);

    incomingLabel = new JLabel (GUIServices.getIcon ("status.incoming"));
    incomingLabel.setBorder (new EtchedBorder ());
    incomingLabel.setToolTipText ("Receiving");
    activityBar.add (incomingLabel);

    processingLabel = new JLabel (GUIServices.getIcon ("status.processing"));
    processingLabel.setBorder (new EtchedBorder ());
    processingLabel.setToolTipText ("Processing");
    activityBar.add (processingLabel);

    errorLabel = new JLabel (GUIServices.getIcon ("status.error"));
    errorLabel.setBorder (new EtchedBorder ());
    errorLabel.setToolTipText ("Error");
    activityBar.add (errorLabel);

    activityLabel = new JLabel();
    activityLabel.setBorder (new EmptyBorder (0, 5, 0, 5));
    activityBar.add (activityLabel);

    // Create incoming / unprocessed text areas
    // ----------------------------------------
    JPanel labelPanel = new JPanel (new GridLayout (1, 2));
    GUIServices.setConstraints (gc, 0, 2, 2, 1, GridBagConstraints.BOTH, 1, 0);
    add (labelPanel, gc);

    labelPanel.add (new JLabel ("Incoming:"));
    labelPanel.add (new JLabel ("Waiting:"));

    JPanel areaPanel = new JPanel (new GridLayout (1, 2));
    GUIServices.setConstraints (gc, 0, 3, 2, 1, GridBagConstraints.BOTH, 1, 1);
    add (areaPanel, gc);

    incomingArea = new JTextArea();
    incomingArea.setEditable (false);
    incomingArea.setRows (3);
    JScrollPane scrollPane = new JScrollPane (incomingArea,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    areaPanel.add (scrollPane);

    unprocessedArea = new JTextArea();
    unprocessedArea.setEditable (false);
    unprocessedArea.setRows (3);
    scrollPane = new JScrollPane (unprocessedArea,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    areaPanel.add (scrollPane);

    // Get error sound effect
    // ----------------------
    errorSound = Applet.newAudioClip (getClass().getResource (ERROR_CLIP));

    // Set initial source
    // ------------------
    setSource (host, path);

  } // ServerStatusPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the status elements by contacting the server for a status
   * report.  This method always returns immediately and the server
   * query is performed asynchronously.  If there is currently an
   * update running, no operation is performed.
   */
  public synchronized void update () {

    // Check for update worker
    // -----------------------
    if (worker != null && worker.isAlive()) return;

    // Create worker thread
    // --------------------
    worker = new Thread() {
      public void run() {

        // Perform query
        // -------------
        ServerQuery tmpQuery;
        HashMap queryKeys = new HashMap();
        queryKeys.put ("query", "serverStatus");
        try { tmpQuery = new ServerQuery (host, path, queryKeys); }
        catch (IOException e) { tmpQuery = null; }
        final ServerQuery query = tmpQuery;

        // Update status elements
        // ----------------------
        SwingUtilities.invokeLater (new Runnable() {
            public void run() { 
              updateElements (query);
            } // run
          });

      } // run
    };
    worker.start();

  } // update

  ////////////////////////////////////////////////////////////

  /** Updates the status elements using the query results. */
  private void updateElements (
    ServerQuery query
  ) {

    // Report error condition
    // ----------------------
    if (query == null) {
      idleLabel.setEnabled (false);
      incomingLabel.setEnabled (false);
      processingLabel.setEnabled (false);
      errorLabel.setEnabled (true);
      activityLabel.setText ("(Error)");
    } // if

    else {

      // Set error to false
      // ------------------
      errorLabel.setEnabled (false);

      // Report date
      // -----------
      hostLabel.setText (host + " updated at " + (String) query.getValue (0, 
        "date"));

      // Report incoming
      // ---------------
      int count;
      Vector activityLog = new Vector();
      try { 
        count = Integer.parseInt ((String) query.getValue (0, 
          "incoming_count")); 
      } catch (NumberFormatException e) { count = 0; }
      if (count != 0) {
        incomingLabel.setEnabled (true);
        String files = (String) query.getValue (0, "incoming_files");
        incomingArea.setText (files.replace (' ', '\n'));
        activityLog.add ("Receiving data");
      } // if
      else {
        incomingLabel.setEnabled (false);
        incomingArea.setText ("");
      } // else

      // Report unprocessed
      // ------------------
      try { 
        count = Integer.parseInt ((String) query.getValue (0, 
          "unprocessed_count"));
      } catch (NumberFormatException e) { count = 0; }
      if (count != 0) {
        String files = (String) query.getValue (0, "unprocessed_files");
        unprocessedArea.setText (files.replace (' ', '\n'));
      } // if
      else {
        unprocessedArea.setText ("");
      } // else

      // Report processing
      // -----------------
      try { 
        count = Integer.parseInt ((String) query.getValue (0, 
          "processing_count"));
      } catch (NumberFormatException e) { count = 0; }
      if (count != 0) {
        processingLabel.setEnabled (true);
        String files = (String) query.getValue (0, "processing_files");
        activityLog.add ("Processing " + files);
      } // if
      else {
        processingLabel.setEnabled (false);
      } // else

      // Report activity
      // ---------------
      if (activityLog.size() == 0) {
        idleLabel.setEnabled (true);
        activityLabel.setText ("(Idle)");
      } // if
      else {
        idleLabel.setEnabled (false);
        String activityText = "(";
        int entries = activityLog.size();
        for (int i = 0; i < entries; i++)
          activityText += activityLog.get(i) + (i < entries-1 ? ", " : "");
        activityText += ")";
        activityLabel.setText (activityText);
      } // else

    } // else

    // Warn user in operator mode
    // --------------------------
    if (operator && !errorState) {

      // Set network connection error
      // ----------------------------
      String message = null;
      if (query == null) { message = NETWORK_ERROR; }

      // Processing system error
      // -----------------------
      else {
        String errorMessage = (String) query.getValue (0, "error_message");
        if (!errorMessage.equals ("")) {
          String errorURLString = (String) query.getValue (0, "error_url");
          try {
            URL errorURL = new URL (errorURLString);
            BufferedReader buf = new BufferedReader (
              new InputStreamReader (errorURL.openStream()));
            String line;
            message = 
              "A CoastWatch processing system error has occurred:\n\n  " +
              errorMessage + "\n\n";
            while ((line = buf.readLine()) != null)
              message += line + "\n";
            message +=
              "Once the error has been resolved, click 'OK' below to clear\n" +
              "this message box.\n";          
          } // try
          catch (Exception e) { message = NETWORK_ERROR; }
        } // if
      } // else

      // Show dialog
      // -----------
      if (message != null) {
        errorState = true;
        errorSound.play();
        JOptionPane.showMessageDialog (this, message, "Error", 
          JOptionPane.ERROR_MESSAGE);
        errorState = false;
      } // if

    } // if

  } // updateElements

  ////////////////////////////////////////////////////////////

} // ServerStatusPanel class

////////////////////////////////////////////////////////////////////////

