////////////////////////////////////////////////////////////////////////
/*

     File: HTMLPanel.java
   Author: Peter Hollemans
     Date: 2002/11/28

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.SwingUtilities;

import noaa.coastwatch.gui.GUIServices;

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
  private LinkedList<HistoryEntry> history;

  /** The home URL. */
  private URL homeURL;

  /** The forward navigation button. */
  private JButton forwardButton;

  /** The back navigation button. */
  private JButton backButton;

  /** The scroll pane used to hold the editor. */
  private JScrollPane scrollPane;

  ////////////////////////////////////////////////////////////

  private static class HistoryEntry {

    public URL url;
    public Point position;

    public static HistoryEntry create (URL url) { 
      var entry = new HistoryEntry();
      entry.url = url;
      entry.position = new Point();
      return (entry);
    } // create

  } // HistoryEntry class

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
    scrollPane = new JScrollPane (editor);
    add (scrollPane, BorderLayout.CENTER);

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
    history = new LinkedList<>();
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

    history = new LinkedList<>();
    currentPage = -1;
    setPage (url, null, true);

  } // setPage

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the HTML page to display.
   *
   * @param url the URL for the page to display.
   * @param position the new position to display the page at, or null to display
   * at the top of the page.
   * @param record the record flag, true to record the page in the history list.
   */
  private void setPage (
    URL url,
    Point position,
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
      if (position != null) {
        var listener = new PropertyChangeListener[1];
        listener[0] = event -> {
          scrollPane.getViewport().setViewPosition (position);
          editor.removePropertyChangeListener ("page", listener[0]);
        };
        editor.addPropertyChangeListener ("page", listener[0]);
      } // if
      editor.setPage (url);

      // Record history
      // --------------
      if (record) {
        while (currentPage < history.size()-1) history.removeLast();
        history.add (HistoryEntry.create (url));
        currentPage++;
      } // if

      // Update address field
      // --------------------
      if (addressField != null) { addressField.setText (url.toString()); }

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

  private void savePosition() {

    if (currentPage >= 0 && currentPage <= history.size()-1)
      history.get (currentPage).position = scrollPane.getViewport().getViewPosition();

  } // savePosition

  ////////////////////////////////////////////////////////////

  /** Sets the page forward in the history. */
  private void forward () {

    if (currentPage < history.size()-1) {
      savePosition();
      currentPage++;
      var entry = history.get (currentPage);
      setPage (entry.url, entry.position, false);
    } // if
 
  } // forward

  ////////////////////////////////////////////////////////////

  /** Sets the page backward in the history. */
  private void back () {

    if (currentPage > 0) {
      savePosition();
      currentPage--;
      var entry = history.get (currentPage);
      setPage (entry.url, entry.position, false);
    } // if
 
  } // back

  ////////////////////////////////////////////////////////////

  /** Refreshes the current page. */
  private void refresh () {

    if (currentPage >= 0) {
      editor.getDocument().putProperty (Document.StreamDescriptionProperty, null);
      var entry = history.get (currentPage);
      setPage (entry.url, entry.position, false);
    } // if
 
  } // refresh

  ////////////////////////////////////////////////////////////

  /**
   * Sets the page to the home page.  This is only valid if the HTML
   * panel was created with an initial URL.
   */
  private void home () {

    if (homeURL != null) {
      if (!(history.get (currentPage).url).equals (homeURL)) {
        savePosition();
        setPage (homeURL, null, true);
      } // if
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
  private class AddressEntered extends AbstractAction {

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
      if (currentPage >= 0 && url.equals (history.get (currentPage).url))
        refresh();
      else {
        savePosition();
        setPage (url, null, true);
      } // else

    } // actionPerformed

  } // AddressEntered

  ////////////////////////////////////////////////////////////

  /** Responds to a hyperlink click. */
  private class LinkActivated
    implements HyperlinkListener {

    public void hyperlinkUpdate (HyperlinkEvent event) { 
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        savePosition();
        setPage (event.getURL(), null, true);
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
    JDialog dialog = GUIServices.createDialog (
      component,
      title,
      false,
      this,
      null,
      new Action[] {GUIServices.createAction ("Close", null)},
      new boolean[] {true},
      true);
    dialog.setVisible (true);
    
  } // showDialog

  ////////////////////////////////////////////////////////////

} // HTMLPanel class

////////////////////////////////////////////////////////////////////////
