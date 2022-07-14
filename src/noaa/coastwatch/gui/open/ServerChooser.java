////////////////////////////////////////////////////////////////////////
/*

     File: ServerChooser.java
   Author: Peter Hollemans
     Date: 2005/06/30

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import noaa.coastwatch.gui.open.ServerTableModel;
import noaa.coastwatch.gui.open.ServerTableModel.Entry;
import noaa.coastwatch.gui.GUIServices;

/**
 * <p>The <code>ServerChooser</code> class allows the user to select from
 * and edit a list of network servers.  Each server has a name and
 * location value (for example, a URL).  A property change is used to
 * signal that the user has selected a new server and pressed the
 * connect button.  The server property value sent is a {@link
 * ServerTableModel.Entry} object from the list of server entries.</p>
 *
 * <p>Generally, it might be a good idea to save the list of servers
 * after using the chooser, in case the user has edited the server
 * names or locations.  The current server list is available from
 * {@link #getServerList}.</p>
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ServerChooser
  extends JPanel {

  // Constants
  // ---------

  /** The remove button command. */
  private static final String REMOVE_COMMAND = "Remove";

  /** The connect button command. */
  private static final String CONNECT_COMMAND = "Connect";

  /** The server selection property. */
  public static final String SERVER_PROPERTY = "server";

  // Variables
  // ---------

  /** The remove button. */
  private JButton removeButton;

  /** The connect button. */
  private JButton connectButton;

  /** The table model of server name and location. */
  private ServerTableModel serverModel;

  /** The table of servers. */
  private JTable serverTable;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser with the specified server list.
   */
  public ServerChooser (
    List serverList
  ) {

    super (new BorderLayout());

    // Create server table
    // -------------------
    serverModel = new ServerTableModel (serverList);
    serverModel.addTableModelListener (new ModelChanged());
    serverTable = new JTable (serverModel);
    serverTable.getSelectionModel().addListSelectionListener (
      new ServerChanged());
    JScrollPane scrollpane = new JScrollPane (serverTable);
    this.add (scrollpane, BorderLayout.CENTER);

    // Set server table size
    // ---------------------
    Dimension size = serverTable.getPreferredScrollableViewportSize();
    int rowHeight = serverTable.getRowHeight();
    size.height = rowHeight*6;
    serverTable.setPreferredScrollableViewportSize (size);

    // Create buttons
    // --------------
    Box buttonBar = Box.createHorizontalBox();
    this.add (buttonBar, BorderLayout.SOUTH);

    ActionListener buttonListener = new ButtonListener();
    removeButton = GUIServices.getTextButton (REMOVE_COMMAND);
    removeButton.addActionListener (buttonListener);
    removeButton.setEnabled (false);
    buttonBar.add (removeButton);
    
    connectButton = GUIServices.getTextButton (CONNECT_COMMAND);
    connectButton.addActionListener (buttonListener);
    connectButton.setEnabled (false);
    buttonBar.add (Box.createGlue());
    buttonBar.add (connectButton);

  } // ServerChooser constructor

  ////////////////////////////////////////////////////////////

  /** Handles a remove or connect button click. */
  private class ButtonListener extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();
      int[] rows = serverTable.getSelectedRows();

      // Remove servers from list
      // ------------------------
      if (command.equals (REMOVE_COMMAND)) {
        for (int i = rows.length-1; i >= 0; i--) {
          serverModel.removeRow (rows[i]);
        } // for
      } // if

      // Connect to server
      // -----------------
      else if (command.equals (CONNECT_COMMAND)) {
        Entry entry = serverModel.getServerEntry (rows[0]);
        if (entry != null) 
          ServerChooser.this.firePropertyChange (SERVER_PROPERTY, null, entry);
      } // else if

    } // actionPerformed
  } // ButtonListener class

  ////////////////////////////////////////////////////////////

  /** Responds to a table model event. */
  private class ModelChanged implements TableModelListener {
    public void tableChanged (TableModelEvent e) {

      updateButtonsEnabled();

    } // tableChanged
  } // ModelChanged class

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the button enabled/disabled status accordingly to the
   * selected table row or rows. 
   */
  private void updateButtonsEnabled () {

    int[] rows = serverTable.getSelectedRows();
    boolean removeEnabled = (rows.length != 0 &&
      !(rows.length == 1 && rows[0] == serverTable.getRowCount()-1));
    boolean connectEnabled = (rows.length == 1 && 
      rows[0] != serverTable.getRowCount()-1 &&
      !serverModel.getServerEntry (rows[0]).getLocation().trim().equals ("")
    );
    removeButton.setEnabled (removeEnabled);
    connectButton.setEnabled (connectEnabled);

  } // updateButtonsEnabled

  ////////////////////////////////////////////////////////////

  /** Responds to a new server selection. */
  private class ServerChanged implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      if (event.getValueIsAdjusting()) return;
      updateButtonsEnabled();

    } // valueChanged
  } // ServerChanged class

  ////////////////////////////////////////////////////////////

  /** Gets the current list of server entries. */
  public List getServerList () { return (serverModel.getServerList()); }

  ////////////////////////////////////////////////////////////

  /** Creates a new empty chooser. */
  public ServerChooser () { this (new LinkedList()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create chooser
    // --------------
    List serverList = new LinkedList();
    serverList.add (new Entry ("Server 1", "http://foo.bar.gov"));
    serverList.add (new Entry ("Server 2", "http://fie.ont.gov"));
    serverList.add (new Entry ("Server 3", "http://fiddle.sticks.gov"));
    ServerChooser chooser = new ServerChooser (serverList);
    chooser.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          String name = event.getPropertyName();
          Object value = event.getNewValue();
          System.out.println ("Property change: " + name + " = " + value);
        } // propertyChange
      });

    // Create frame
    // ------------
    final JFrame frame = new JFrame (ServerChooser.class.getName());
    frame.getContentPane().add (chooser);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // ServerChooser class

////////////////////////////////////////////////////////////////////////
