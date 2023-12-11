////////////////////////////////////////////////////////////////////////
/*

     File: OverlayGroupSavePanel.java
   Author: Peter Hollemans
     Date: 2004/04/04

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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.render.EarthDataOverlay;

/**
 * The <code>OverlayGroupSavePanel</code> shows a list of overlays
 * that are to be saved as a group, and allows the user to choose the
 * group name or use an existing name.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class OverlayGroupSavePanel 
  extends JPanel {

  // Variables
  // ---------

  /** The group name text field. */
  private JTextField groupField;

  /** The group overlay list. */
  private JList<EarthDataOverlay> overlayJList;

  /** The group name list. */
  private JComboBox<String> groupCombo;

  ////////////////////////////////////////////////////////////

  /** Gets the selected group name. */
  public String getGroupName () { return ((String) groupCombo.getSelectedItem()); }

  ////////////////////////////////////////////////////////////

  /** Gets the selected overlays. */
  public List<EarthDataOverlay> getSelectedOverlays () { 

    return (overlayJList.getSelectedValuesList()); 

  } // getSelectedOverlays

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new save panel.
   * 
   * @param overlayList the overlay list to selectively be saved to a group.
   * @param groupNameList the group name list to choose a name from.
   */
  public OverlayGroupSavePanel (
    List<EarthDataOverlay> overlayList,
    List<String> groupNameList
  ) {

    super (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, GridBagConstraints.RELATIVE, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Overlay Group Properties"),
      new EmptyBorder (4, 4, 4, 4)
    ));

    // Add a selectable list of overlays to save.
    add (new JLabel ("Select overlays to save to group:"), gc);
    overlayJList = new JList<> (overlayList.toArray (new EarthDataOverlay[0]));
    overlayJList.setVisibleRowCount (6);
    add (new JScrollPane (overlayJList), gc);

    // Add a dropdown of which overlay group to overwrite and make it editable
    // for the user to add their own name.
    add (new JLabel ("Specify an overlay group name:"), gc);
    groupCombo = new JComboBox<> (groupNameList.toArray (new String[0]));
    groupCombo.setEditable (true);
    add (groupCombo, gc);

  } // OverlayGroupSavePanel constructor

  ////////////////////////////////////////////////////////////

  /** Holds the results from an overlay group save operation. */
  public static class SaveData {

    /** The list of overlays to save. */
    public List<EarthDataOverlay> overlayList;

    /** The name of the group to save. */
    public String groupName;

  } // SaveData class

  ////////////////////////////////////////////////////////////

  /**
   * Shows a modal save dialog and blocks until the dialog is hidden.
   *
   * @param parent the component to use for the dialog parent.
   * @param title the dialog title string.
   * @param overlayList the overlay list to selectively be saved to a group.
   * @param groupList the group name list to choose a name from.
   *
   * @return the new overlay group name, or null if the save was
   * cancelled.
   */
  public static SaveData showDialog (
    Component parent, 
    String title, 
    List<EarthDataOverlay> overlayList,
    List<String> groupList
  ) {

    // Create panel
    // ------------
    final OverlayGroupSavePanel savePanel = 
      new OverlayGroupSavePanel (overlayList, groupList);

    // Create actions
    // --------------
    final SaveData[] returnValue = new SaveData[1];
    final JDialog[] dialog = new JDialog[1];
    Action saveAction = GUIServices.createAction ("Save", new Runnable() {
      public void run () {

        // Get group name
        // --------------
        var groupName = savePanel.getGroupName();
        var selectedOverlays = savePanel.getSelectedOverlays();
        boolean save = true;

        // Check if any overlays have been selected to include in the
        // new overlay group.
        if (selectedOverlays.size() == 0) {
          JOptionPane.showMessageDialog (dialog[0],
            "Select at least one overlay in the list to\n" +
            "create a new overlay group.",
            "Error", JOptionPane.ERROR_MESSAGE);
          save = false;
        } // if

        // Check if group name is empty
        // ----------------------------
        else if (groupName == null || groupName.trim().equals ("")) {
          JOptionPane.showMessageDialog (dialog[0],
            "Specify an overlay group name to create.",
            "Error", JOptionPane.ERROR_MESSAGE);
          save = false;
        } // if

        // Check if group exists
        // ---------------------
        if (save && groupList.contains (groupName)) {
          String question = 
            "The selected overlay group already exists.\n" +
            "The group will be overwritten with the\n" +
            "selected overlays.  Are you sure?";
          int result = JOptionPane.showConfirmDialog (
            dialog[0], question, "Confirmation", 
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            save = (result == JOptionPane.YES_OPTION);
        } // if

        // Save group
        // ----------
        if (save) {
          var data = new SaveData();
          data.overlayList = selectedOverlays;
          data.groupName = groupName;
          returnValue[0] = data;
          dialog[0].dispose();
        } // if

      } // run
    });

    Action cancelAction = GUIServices.createAction ("Cancel", null);

    // Create dialog
    // -------------
    dialog[0] = GUIServices.createDialog (parent, title, true, 
      savePanel, null, new Action[] {saveAction, cancelAction},
      new boolean[] {false, true}, true);

    // Show dialog
    // -----------
    dialog[0].setVisible (true);
    return (returnValue[0]);

  } // showDialog

  ////////////////////////////////////////////////////////////

} // OverlayGroupSavePanel class

////////////////////////////////////////////////////////////////////////
