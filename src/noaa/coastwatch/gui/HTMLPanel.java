////////////////////////////////////////////////////////////////////////
/*
     FILE: HTMLPanel.java
  PURPOSE: A class to display a basic HTML panel.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/28
  CHANGES: 2003/01/14, PFH, removed absolute path for icon files
           2003/11/22, PFH, fixed Javadoc comments
           2004/05/22, PFH, modified to use GUIServices.getIcon()
           2005/04/07, PFH, modified to hide refresh button on request
           2006/03/15, PFH, modified to use GUIServices.getIconButton()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;

/**
 * The HTML panel displays an HTML document in a scrollable window 
 * and responds to hyperlink clicks.
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class HTMLPanel 
  extends JPanel {

  // Constants
  // ---------
  /** The navigation commands. */
  private static final String FORWARD_COMMAND = "Forward";
  private static final String BACK_COMMAND = "Back";
  private static final String REFRESH_COMMAND = "Refresh";
  private static final String HOME_COMMAND = "Home";

  // Variables
  // ---------
  /** The editor pane. */
  private JEditorPane editor;

  /** The address text field. */
  private JTextField addressField;

  /** The current page index loaded. */
  private int currentPage;

  /** The history of URLs. */
  private LinkedList history;

  /** The home URL. */
  private URL homeURL;

  /** The forward navigation button. */
  private JButton forwardButton;

  /** The back navigation button. */
  private JButton backButton;

  ////////////////////////////////////////////////////////////

  /** Creates a new empty HTML panel with full network controls. */
  public HTMLPanel () { this (true); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new empty HTML panel.
   *
   * @param isNetworked the networked flag, true if the content of the
   * panel will be on a live network or false if the contents are from
   * a static source such as a directory or JAR file.  If true, then
   * full navigation controls are shown.  If false, simplified
   * navigation controls are shown with no address bar or refresh
   * button.
   */
  public HTMLPanel (
    boolean isNetworked
  ) {

    super (new BorderLayout());

    // Create editor
    // -------------
    editor = new JEditorPane();
    editor.setContentType ("text/html");
    editor.setEditable (false);
    editor.addHyperlinkListener (new LinkActivated());

    // Create scroll window
    // --------------------
    add (new JScrollPane (editor), BorderLayout.CENTER);

    // Create top panel
    // ----------------
    JPanel topPanel = new JPanel (new BorderLayout());
    add (topPanel, BorderLayout.NORTH);

    // Create navigation bar
    // ---------------------
    JToolBar navBar = new JToolBar();
    navBar.setFloatable (false);
    topPanel.add (navBar, BorderLayout.SOUTH);
    navBar.add (new JLabel ("Navigation: "));

    JButton button;
    ActionListener navListener = new Navigate();
    button = GUIServices.getIconButton ("navigation.back");
    button.setToolTipText (BACK_COMMAND);
    button.getModel().setActionCommand (BACK_COMMAND);
    button.addActionListener (navListener);
    navBar.add (button);
    backButton = button;

    button = GUIServices.getIconButton ("navigation.forward");
    button.setToolTipText (FORWARD_COMMAND);
    button.getModel().setActionCommand (FORWARD_COMMAND);
    button.addActionListener (navListener);
    navBar.add (button);
    forwardButton = button;

    if (isNetworked) {
      button = GUIServices.getIconButton ("navigation.refresh");
      button.setToolTipText (REFRESH_COMMAND);
      button.getModel().setActionCommand (REFRESH_COMMAND);
      button.addActionListener (navListener);
      navBar.add (button);
    } // if

    button = GUIServices.getIconButton ("navigation.home");
    button.setToolTipText (HOME_COMMAND);
    button.getModel().setActionCommand (HOME_COMMAND);
    button.addActionListener (navListener);
    navBar.add (button);

    // Create address bar
    // ------------------
    if (isNetworked) {
      JToolBar addressBar = new JToolBar();
      addressBar.setFloatable (false);
      topPanel.add (addressBar, BorderLayout.NORTH);
      addressBar.add (new JLabel ("Address: "));
      addressField = new JTextField();
      addressField.addActionListener (new AddressEntered());
      addressBar.add (addressField);
    } // if
    else addressField = null;

    // Create empty history
    // --------------------
    history = new LinkedList();
    currentPage = -1;
    homeURL = null;

    // Update navigation buttons
    // -------------------------
    updateNavigation();

  } // HTMLPanel constructor

  ////////////////////////////////////////////////////////////

  /** Updates the navigation buttons. */
  private void updateNavigation () {

    forwardButton.setEnabled (currentPage < history.size()-1);
    backButton.setEnabled (currentPage > 0);

  } // updateNavigation

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HTML page to display.
   *
   * @param url the URL for the page to display.
   */
  public void setPage (
    URL url
  ) {

    history = new LinkedList();
    currentPage = -1;
    setPage (url, true);

  } // setPage

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HTML page to display.
   *
   * @param url the URL for the page to display.
   * @param record the record flag, true to record the page in the
   * history list.
   */
  private void setPage (
    URL url,
    boolean record
  ) {

    // Set editor cursor
    // -----------------
    Cursor cursor = editor.getCursor();
    editor.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));

    // Set page
    // --------
    IOException error = null;
    try { 

      // Set editor page
      // ---------------
      editor.setPage (url); 

      // Update address field
      // --------------------
      if (addressField != null) { addressField.setText (url.toString()); }

      // Record history
      // --------------
      if (record) {
        while (currentPage < history.size()-1) history.removeLast();
        history.add (url);
        currentPage++;
      } // if

      // Update navigation
      // -----------------
      updateNavigation();

    } catch (IOException e) { error = e; }

    // Reset editor cursor
    // -------------------
    editor.setCursor (cursor);

    // Check for error
    // ---------------
    if (error != null) {
      JOptionPane.showMessageDialog (this,
        "A problem occurred accessing the URL.\n" + 
        error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    } // if

  } // setPage

  ////////////////////////////////////////////////////////////

  /** Sets the page forward in the history. */
  private void forward () {

    if (currentPage < history.size()-1) {
      currentPage++;
      setPage ((URL) history.get (currentPage), false);
    } // if
 
  } // forward

  ////////////////////////////////////////////////////////////

  /** Sets the page backward in the history. */
  private void back () {

    if (currentPage > 0) {
      currentPage--;
      setPage ((URL) history.get (currentPage), false);
    } // if
 
  } // back

  ////////////////////////////////////////////////////////////

  /** Refreshes the current page. */
  private void refresh () {

    if (currentPage >= 0) {
      editor.getDocument().putProperty (
        Document.StreamDescriptionProperty, null);
      setPage ((URL) history.get (currentPage), false);
    } // if
 
  } // refresh

  ////////////////////////////////////////////////////////////

  /**
   * Sets the page to the home page.  This is only valid if the HTML
   * panel was created with an initial URL.
   */
  private void home () {

    if (homeURL != null) {
      if (!((URL) history.get (currentPage)).equals (homeURL))
        setPage (homeURL, true);
    } // if

  } // home

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new HTML panel.
   *
   * @param url the home URL to load.
   * @param isNetworked the networked flag, true if the content of the
   * panel will be on a live network or false if the contents are from
   * a static source such as a directory or JAR file.  If true, then
   * full navigation controls are shown.  If false, simplified
   * navigation controls are shown with no address bar or refresh
   * button.
   */
  public HTMLPanel (
    URL url,
    boolean isNetworked
  ) {

    this (isNetworked);
    setPage (url);
    homeURL = url;

  } // HTMLPanel constructor

  ////////////////////////////////////////////////////////////

  /** Responds to a navigation command. */
  private class Navigate 
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {
      String command = event.getActionCommand();
      if (command.equals (FORWARD_COMMAND)) { forward(); }
      else if (command.equals (BACK_COMMAND)) { back(); }
      else if (command.equals (REFRESH_COMMAND)) { refresh(); }
      else if (command.equals (HOME_COMMAND)) { home(); }
    } // actionPerformed

  } // Navigate class

  ////////////////////////////////////////////////////////////

  /** Responds to an address change. */
  private class AddressEntered
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {

      // Get URL
      // -------
      URL url;
      try { 
        String address = addressField.getText();
        if (address.indexOf ("://") == -1) address = "http://" + address;
        url = new URL (address);
      } // try
      catch (MalformedURLException e) {
        JOptionPane.showMessageDialog (HTMLPanel.this,
          "A problem occurred using the address.\n" + 
          e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } // catch   

      // Set page
      // --------
      if (currentPage >= 0 && url.equals (history.get (currentPage)))
        refresh();
      else
        setPage (url, true);

    } // actionPerformed

  } // AddressEntered

  ////////////////////////////////////////////////////////////

  /** Responds to a hyperlink click. */
  private class LinkActivated
    implements HyperlinkListener {

    public void hyperlinkUpdate (HyperlinkEvent event) { 
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        setPage (event.getURL(), true);
      } // if
    } // hyperlinkUpdate

  } // LinkListener class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog showing the HTML panel and Close button.
   *
   * @param component the parent component for the dialog.
   * @param title the dialog window title.
   */
  public void showDialog (
    Component component,
    String title
  ) {

    // Create HTML dialog
    // ------------------
    JOptionPane optionPane = new JOptionPane (this,
      JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
      null, new String [] {"Close"});
    JDialog dialog = optionPane.createDialog (component, title);
    dialog.setResizable (true);
    dialog.setModal (false);
    dialog.setVisible (true);

  } // showDialog

  ////////////////////////////////////////////////////////////

} // HTMLPanel class

////////////////////////////////////////////////////////////////////////
