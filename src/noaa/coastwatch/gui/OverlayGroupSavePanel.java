////////////////////////////////////////////////////////////////////////
/*
     FILE: OverlayGroupSavePanel.java
  PURPOSE: Allows the user to select an overlay group name to save.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/04
  CHANGES: n/a
           
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
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import noaa.coastwatch.render.*;

/**
 * The <code>OverlayGroupSavePanel</code> shows a list of overlays
 * that are to be saved as a group, and allows the user to choose the
 * group name or use an existing name.
 */
public class OverlayGroupSavePanel 
  extends JPanel {

  // Variables
  // ---------

  /** The group name text field. */
  private JTextField groupField;

  /** The group name list. */
  private JList groupList;

  ////////////////////////////////////////////////////////////

  /** Gets the selected group name. */
  public String getGroup () { return (groupField.getText()); }

  ////////////////////////////////////////////////////////////

  /** Returns true if the specified group name exists in the list. */
  public boolean contains (
    String group
  ) {

    return (((DefaultListModel) groupList.getModel()).contains (group));

  } // contains

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new save panel.
   * 
   * @param overlayList the overlay list to be saved as a group.
   * @param groupNameList the group name list to choose a name from.
   */
  public OverlayGroupSavePanel (
    List overlayList,
    List groupNameList
  ) {

    super (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, GridBagConstraints.RELATIVE, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Overlay Group Save"),
      new EmptyBorder (4, 4, 4, 4)
    ));

    // Add initial label
    // -----------------
    StringBuffer buffer = new StringBuffer();
    buffer.append ("<html>");
    buffer.append ("The following overlays will be saved to the group:");
    buffer.append ("<ul>");
    for (Iterator iter = overlayList.iterator(); iter.hasNext(); )
      buffer.append ("<li>"+ ((EarthDataOverlay) iter.next()).getName() 
        + "</li>");
    buffer.append ("</ul>");
    buffer.append ("</html>");
    add (new JLabel (buffer.toString()), gc);

    // Add group list
    // --------------
    add (new JLabel ("Overlay groups:"), gc);
    DefaultListModel model = new DefaultListModel();
    for (Iterator iter = groupNameList.iterator(); iter.hasNext(); )
      model.addElement (iter.next());
    groupList = new JList (model);
    groupList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    groupList.setVisibleRowCount (6);
    groupList.addListSelectionListener (new ListSelectionListener () {
        public void valueChanged (ListSelectionEvent e) {
          groupField.setText ((String) groupList.getSelectedValue());
        } // valueChanged
      });
    add (new JScrollPane (groupList), gc);

    // Add group field
    // ---------------
    add (new JLabel ("New group name:"), gc);
    groupField = new JTextField();
    add (groupField, gc);

  } // OverlayGroupSavePanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Shows a modal save dialog and blocks until the dialog is hidden.
   *
   * @param parent the component to use for the dialog parent.
   * @param title the dialog title string.
   * @param overlayList the overlay list to be saved as a group.
   * @param groupList the group list to choose a name from.
   *
   * @return the new overlay group name, or null if the save was
   * cancelled.
   */
  public static String showDialog (
    Component parent, 
    String title, 
    List overlayList,
    List groupList
  ) {

    // Create panel
    // ------------
    final OverlayGroupSavePanel savePanel = 
      new OverlayGroupSavePanel (overlayList, groupList);

    // Create actions
    // --------------
    final String[] returnValue = new String[1];
    final JDialog[] dialog = new JDialog[1];
    Action saveAction = GUIServices.createAction ("Save", new Runnable() {
        public void run () {

          // Get group name
          // --------------
          String name = savePanel.getGroup();
          boolean save = true;

          // Check if group name is empty
          // ----------------------------
          if (name == null || name.equals ("")) {
            JOptionPane.showMessageDialog (dialog[0],
              "You must enter a group name.",
              "Error", JOptionPane.ERROR_MESSAGE);
            save = false;
          } // if

          // Check if group exists
          // ---------------------
          if (savePanel.contains (name)) {
            String question = 
              "The selected overlay group already exists.\n" +
              "The existing group will be overwritten.  Continue?";
            int result = JOptionPane.showConfirmDialog (
              dialog[0], question, "Confirmation", 
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
              save = (result == JOptionPane.YES_OPTION);
          } // if

          // Save group
          // ----------
          if (save) {
            returnValue[0] = name;
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
