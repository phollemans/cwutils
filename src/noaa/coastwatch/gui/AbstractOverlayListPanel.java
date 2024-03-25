////////////////////////////////////////////////////////////////////////
/*

     File: AbstractOverlayListPanel.java
   Author: Peter Hollemans
     Date: 2004/02/20

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

// TODO: Should dialog boxes appear over the overlay list chooser, or
// the center of the application?

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.OverlayGroupSavePanel;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.ComponentList;
import noaa.coastwatch.gui.visual.VisualOverlay;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.LatLonOverlay;
import noaa.coastwatch.render.OverlayGroupManager;
import noaa.coastwatch.tools.ResourceManager;

/** 
 * <p>The <code>AbstractOverlayListPanel</code> class is the abstract
 * parent of all overlay list panels.  It allows the user to add new
 * overlays via a set of buttons, remove overlays from the current
 * list, edit a specific overlay using an overlay property chooser,
 * and rearrange the overlay layers within the list.  Child classes
 * must implement the <code>getAddButtons()</code> method which
 * returns a list of the "add overlay" buttons, and the
 * <code>getTitle()</code> method which determine the title for parts
 * of the panel.  Child classes may also override the
 * <code>getCustomPanel()</code> method to supply a custom panel that
 * is inserted between the add button panel and the overlay list.</p>
 *
 * <p>The overlay list panel signals an add/remove in the overlay list by
 * firing a property change event whose property name is given by the
 * <code>OVERLAY_PROPERTY</code> constant, and old value (if non-null)
 * contains an overlay to remove from the view, and new value (if
 * non-null) contains an overlay to add to the view.  If the overlay
 * to add already exists in the view, then the overlay's internal
 * properties have changed and the view should be re-rendered.</p>
 *
 * <p>Additionally, a selection change in the overlay list is signaled by
 * firing a property change event whose property name is given by the
 * <code>SELECTION_PROPERTY</code> constant.  The selection value is
 * the currently selected <code>EarthDataOverlay</code> object, or
 * null if none is selected.  If multiple overlays are selected, no
 * event is fired.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class AbstractOverlayListPanel 
  extends JPanel {

  // Constants 
  // ---------

  /** The overlay property for change events. */
  public static String OVERLAY_PROPERTY = "overlay";

  /** The selection property for change events. */
  public static String SELECTION_PROPERTY = "selection";

  /** The overlay remove command. */
  private static final String REMOVE_COMMAND = "Remove layer(s) from list";

  /** The overlay clear command. */
  private static final String CLEAR_COMMAND = "Remove all layers";

  /** The overlay edit command. */
  private static final String EDIT_COMMAND = "Edit layer properties";

  /** The overlay hide command. */
  private static final String HIDE_COMMAND = "Hide all layers";

  /** The overlay show command. */
  private static final String SHOW_COMMAND = "Show all layers";

  /** The overlay up command. */
  private static final String UP_COMMAND = "Move layer up";

  /** The overlay down command. */
  private static final String DOWN_COMMAND = "Move layer down";

  /** The group load command. */
  private static final String LOAD_COMMAND = "Load saved overlay group";

  /** The group save command. */
  private static final String SAVE_COMMAND = "Create new overlay group";

  /** The group delete command. */
  private static final String DELETE_COMMAND = "Delete overlay group";

  /** The group restore command. */
  private static final String RESTORE_COMMAND = "Restore default overlay groups";

  // Variables
  // ---------

  /** The list of overlays. */
  protected ComponentList<VisualOverlay> overlayList;

  /** The edit button. */
  private JButton editButton;

  /** The remove button. */
  private JButton removeButton;

  /** The clear button. */
  private JButton clearButton;

  /** The hide button. */
  private JButton hideButton;

  /** The show button. */
  private JButton showButton;

  /** The up button. */
  private JButton upButton;

  /** The down button. */
  private JButton downButton;

  /** The group save button. */
  private JButton saveButton;

  /** The group load button. */
  private JButton loadButton;

  /** The group delete button. */
  private JButton deleteButton;

  /** The map of overlay class to counter value. */
  private Map counterMap;

  /** The base layer number for overlay layering. */
  private int baseLayer;

  /** The scroll pane used to hold the list of overlays. */
  private JScrollPane scrollPane;

  /** The group overlay list. */
  private JList groupList;

  /** The overlay group manager. */
  private OverlayGroupManager groupManager;

  /** The listener for group list changes. */
  private GroupListListener groupListener;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data source for bitmask grid data.  This method need
   * only be used if the overlay group feature is enabled, and the
   * group manager needs to load bitmask overlays.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   *
   * @see noaa.coastwatch.render.OverlayGroupManager
   */
  public void setDataSource (
    EarthDataReader reader,
    List variableList
  ) {

    groupManager.setDataSource (reader, variableList);

  } // setDataSource

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a custom panel to be placed between the add overlay button
   * panel and the overlay list.
   *
   * @return the custom panel or null for none (the default).
   */
  protected JPanel getCustomPanel () { return (null); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new list panel, showing the Remove/Edit/Up/Down buttons.
   */
  protected AbstractOverlayListPanel () {

    this (true, true, true, false, false);

  } // AbstractOverlayListPanel

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the preferred number of visible overlay list rows. 
   * 
   * @param rows the number of visible rows.
   */
  public void setVisibleRowCount (
    int rows
  ) {

    overlayList.setVisibleRowCount (rows);

  } // setVisibleRowCount

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new list panel with the specified buttons.
   *
   * @param showRemove if true, shows the remove overlay button.
   * @param showEdit if true, shows the edit selected overlay button.
   * @param showArrange if true, shows the up and down overlay buttons.
   * @param showVisibility if true, shows the visibility modifier buttons.
   * @param showGroup if true, shows the overlay group panel.
   */
  protected AbstractOverlayListPanel (
    boolean showRemove,
    boolean showEdit,
    boolean showArrange,
    boolean showVisibility,
    boolean showGroup
  ) {

    super (new BorderLayout());

    // Create top panel
    // ----------------
    JPanel topPanel = new JPanel (new BorderLayout());
    this.add (topPanel, BorderLayout.NORTH);

    // Create button panel
    // -------------------
    List buttons = getAddButtons();
    if (buttons != null) {
      JToolBar buttonPanel = new JToolBar();
      buttonPanel.setFloatable (false);
      for (Iterator iter = buttons.iterator(); iter.hasNext();) {
        AbstractButton button = (AbstractButton) iter.next();
        buttonPanel.add (button);
      } // for
      String buttonTitle = getButtonTitle();
      if (buttonTitle != null) {
        buttonPanel.setBorder (new TitledBorder (new EtchedBorder(), 
          buttonTitle));
      } // if
      topPanel.add (buttonPanel, BorderLayout.NORTH);
    } // if

    // Create custom panel
    // -------------------
    JPanel customPanel = getCustomPanel();
    if (customPanel != null) topPanel.add (customPanel, BorderLayout.SOUTH);

    // Create list panel
    // -----------------
    overlayList = new ComponentList();
    overlayList.addPropertyChangeListener (
      ComponentList.SELECTION_PROPERTY, new OverlaySelectionListener());
    JPanel listPanel = new JPanel (new BorderLayout());
    scrollPane = new JScrollPane (overlayList);
    listPanel.add (scrollPane, BorderLayout.CENTER);
    String listTitle = getListTitle();
    if (listTitle != null) {
      listPanel.setBorder (new TitledBorder (new EtchedBorder(), listTitle));
    } // if
    this.add (listPanel, BorderLayout.CENTER);

    // Create list button panel
    // ------------------------
    Box listButtonPanel = Box.createHorizontalBox();
    listPanel.add (listButtonPanel, BorderLayout.SOUTH);
    ActionListener overlayButtonListener = new ListButtonListener();

    // Create edit button
    // ------------------
    if (showEdit) {
      editButton = GUIServices.getIconButton ("list.edit");
      GUIServices.setSquare (editButton);
      editButton.setActionCommand (EDIT_COMMAND);
      editButton.addActionListener (overlayButtonListener);
      editButton.setEnabled (false);
      editButton.setToolTipText (EDIT_COMMAND);
      listButtonPanel.add (editButton);
      overlayList.addMouseListener (new MouseInputAdapter () {
          public void mouseClicked (MouseEvent e) {
            if (e.getClickCount() == 2 && editButton.isEnabled()) {
              editButton.doClick();
            } // if
          } // mouseClicked
        });
    } // if

    // Create remove button
    // --------------------
    if (showRemove) {

      removeButton = GUIServices.getIconButton ("list.delete");
      GUIServices.setSquare (removeButton);
      removeButton.setActionCommand (REMOVE_COMMAND);
      removeButton.addActionListener (overlayButtonListener);
      removeButton.setEnabled (false);
      removeButton.setToolTipText (REMOVE_COMMAND);
      listButtonPanel.add (removeButton);

      // clearButton = GUIServices.getIconButton ("list.clear");
      // GUIServices.setSquare (clearButton);
      // clearButton.setActionCommand (CLEAR_COMMAND);
      // clearButton.addActionListener (overlayButtonListener);
      // clearButton.setEnabled (false);
      // clearButton.setToolTipText (CLEAR_COMMAND);
      // listButtonPanel.add (clearButton);

    } // if

    // Create show all / hide all buttons
    // ----------------------------------
    if (showVisibility) {

      showButton = GUIServices.getIconButton ("list.show");
      GUIServices.setSquare (showButton);
      showButton.setActionCommand (SHOW_COMMAND);
      showButton.addActionListener (overlayButtonListener);
      showButton.setEnabled (false);
      showButton.setToolTipText (SHOW_COMMAND);
      listButtonPanel.add (showButton);

      hideButton = GUIServices.getIconButton ("list.hide");
      GUIServices.setSquare (hideButton);
      hideButton.setActionCommand (HIDE_COMMAND);
      hideButton.addActionListener (overlayButtonListener);
      hideButton.setEnabled (false);
      hideButton.setToolTipText (HIDE_COMMAND);
      listButtonPanel.add (hideButton);

    } // if

    // Add space component in middle
    // -----------------------------
    listButtonPanel.add (Box.createHorizontalGlue());

    // Create move up / move down buttons
    // ----------------------------------
    if (showArrange) {

      upButton = GUIServices.getIconButton ("list.up");
      GUIServices.setSquare (upButton);
      upButton.setActionCommand (UP_COMMAND);
      upButton.addActionListener (overlayButtonListener);
      upButton.setEnabled (false);
      upButton.setToolTipText (UP_COMMAND);
      listButtonPanel.add (upButton);

      downButton = GUIServices.getIconButton ("list.down");
      GUIServices.setSquare (downButton);
      downButton.setActionCommand (DOWN_COMMAND);
      downButton.addActionListener (overlayButtonListener);
      downButton.setEnabled (false);
      downButton.setToolTipText (DOWN_COMMAND);
      listButtonPanel.add (downButton);

    } // if

    // Create counter map
    // ------------------
    counterMap = new HashMap();

    // Create group panel
    // ------------------
    if (showGroup) {

      groupManager = ResourceManager.getOverlayManager();
      groupListener = new GroupListListener();
      groupManager.addPropertyChangeListener (groupListener);
      addHierarchyListener (new HierarchyListener () {
          public void hierarchyChanged (HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) 
                != 0 && !isDisplayable()) {
              groupManager.removePropertyChangeListener (groupListener);
            } // if
          } // hierarchyChanged
        });

      JPanel groupPanel = new JPanel (new BorderLayout());
      String groupTitle = getGroupTitle();
      if (groupTitle != null) {
        groupPanel.setBorder (new TitledBorder (new EtchedBorder(), 
          groupTitle));
      } // if
      this.add (groupPanel, BorderLayout.SOUTH);

      // Create group list
      // -----------------
      groupList = new JList (new Vector (groupManager.getGroups()));
      groupList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
      groupList.setVisibleRowCount (6);
      groupList.addListSelectionListener (new GroupSelectionListener());
      groupPanel.add (new JScrollPane (groupList), BorderLayout.CENTER);

      // Create group buttons
      // --------------------
      Box groupButtonPanel = Box.createHorizontalBox();
      groupPanel.add (groupButtonPanel, BorderLayout.SOUTH);

      saveButton = GUIServices.getIconButton ("list.group.save");
      GUIServices.setSquare (saveButton);
      saveButton.setActionCommand (SAVE_COMMAND);
      saveButton.addActionListener (overlayButtonListener);
      saveButton.setEnabled (false);
      saveButton.setToolTipText (SAVE_COMMAND);
      groupButtonPanel.add (saveButton);

      loadButton = GUIServices.getIconButton ("list.group.load");
      GUIServices.setSquare (loadButton);
      loadButton.setActionCommand (LOAD_COMMAND);
      loadButton.addActionListener (overlayButtonListener);
      loadButton.setEnabled (false);
      loadButton.setToolTipText (LOAD_COMMAND);
      groupButtonPanel.add (loadButton);

      deleteButton = GUIServices.getIconButton ("list.group.delete");
      GUIServices.setSquare (deleteButton);
      deleteButton.setActionCommand (DELETE_COMMAND);
      deleteButton.addActionListener (overlayButtonListener);
      deleteButton.setEnabled (false);
      deleteButton.setToolTipText (DELETE_COMMAND);
      groupButtonPanel.add (deleteButton);

      groupButtonPanel.add (Box.createHorizontalGlue());

      var restoreButton = GUIServices.getIconButton ("list.group.restore");
      GUIServices.setSquare (restoreButton);
      restoreButton.setActionCommand (RESTORE_COMMAND);
      restoreButton.addActionListener (overlayButtonListener);
      restoreButton.setToolTipText (RESTORE_COMMAND);
      groupButtonPanel.add (restoreButton);

      // Add a double click detector to the group list.  This is a shortcut
      // for opening the group.
      groupList.addMouseListener (new MouseInputAdapter () {
        public void mouseClicked (MouseEvent e) {
          if (e.getClickCount() == 2) loadButton.doClick();
        } // mouseClicked
      });

    } // if

  } // AbstractOverlayListPanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the base layer.  When overlays are added to the list, they
   * are given a layer number that is higher than any other overlay
   * currently in the list so that the new overlay is rendered on top
   * of existing overlays.  The base layer determines what layer value
   * the initially added overlay will have.  By default the base layer
   * is 0.
   * 
   * @param baseLayer the base layer for the initial overlay.
   */
  public void setBaseLayer (int baseLayer) { this.baseLayer = baseLayer; }

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the list buttons. */
  private class ListButtonListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Hide or show all overlays
      // -------------------------
      if (command.equals (HIDE_COMMAND) || command.equals (SHOW_COMMAND)) {
        boolean isVisible = command.equals (SHOW_COMMAND);
        for (int i = 0; i < overlayList.getElements(); i++) {
          VisualOverlay visual = (VisualOverlay) overlayList.getElement (i);
          EarthDataOverlay overlay = (EarthDataOverlay) visual.getValue();
          if (overlay.getVisible() != isVisible) {
            overlay.setVisible (isVisible);
            visual.refreshComponent();
          } // if
        } // for
        /**
         * Note that here, we only fire one overlay change event, the
         * same as if we moved one up or down.
         */
        firePropertyChange (OVERLAY_PROPERTY, null, getOverlay (0));
      } // if

      // Load selected overlay group
      // ---------------------------
      else if (command.equals (LOAD_COMMAND)) {
        try { 
          String name = (String) groupList.getSelectedValue();
          List overlayList = groupManager.loadGroup (name);
          for (int i = overlayList.size()-1; i >= 0; i--)
            addOverlay ((EarthDataOverlay) overlayList.get (i));
        } // try
        catch (Exception e) { 
          String message = 
            "Error loading overlay group:\n" + e;
          JOptionPane.showMessageDialog (AbstractOverlayListPanel.this, 
            message, "Error", JOptionPane.ERROR_MESSAGE);
        } // catch
      } // else if

      // Delete selected overlay group
      // -----------------------------
      else if (command.equals (DELETE_COMMAND)) {
        String name = (String) groupList.getSelectedValue();
        String question = 
          "The selected overlay group will be\n" +
          "deleted.  Are you sure?";
        int result = JOptionPane.showConfirmDialog (
          AbstractOverlayListPanel.this, question, "Confirmation", 
          JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
          try {
            groupManager.deleteGroup (name);
          } // try
          catch (Exception e) { 
            String message = 
              "Error deleting overlay group:\n" + e;
            JOptionPane.showMessageDialog (AbstractOverlayListPanel.this, 
              message, "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
        } // if
      } // else if

      // Restore the overlay groups from the default ones installed with the
      // software.  We check with the user first to make sure this is what
      // they want.
      else if (command.equals (RESTORE_COMMAND)) {
        String question = 
          "The default overlay groups will be restored.\n" +
          "This will overwrite changes made to any existing\n" +
          "group of the same name. Are you sure?";
        int result = JOptionPane.showConfirmDialog (
          AbstractOverlayListPanel.this, question, "Confirmation", 
          JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
          try {
            ResourceManager.restoreOverlays();
            groupManager.signalGroupsChanged();
          } // try
          catch (Exception e) { 
            String message = 
              "Error restoring default overlay groups:\n" + e;
            JOptionPane.showMessageDialog (AbstractOverlayListPanel.this, 
              message, "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
        } // if
      } // else if

      // Clear all the overlays.
      else if (command.equals (CLEAR_COMMAND)) {
        while (overlayList.getElements() != 0) {
          VisualOverlay visual = (VisualOverlay) overlayList.removeElement (0);
          firePropertyChange (OVERLAY_PROPERTY, visual.getValue(), null);
        } // for
      } // else if

      // Perform a command on selected overlays
      // --------------------------------------
      else {

        // Get selected rows
        // -----------------
        int[] indices = overlayList.getSelectedIndices();         
        if (indices.length == 0) return;

        // Remove overlays
        // ---------------
        if (command.equals (REMOVE_COMMAND)) {

          // TODO: Connect the delete key to the remove overlay button.
          
          /**
           * We assume here that the selected row indices are in
           * ascending order, so that removing them in reverse order
           * does not disturb the indicies of the preceding rows. 
           */
          for (int i = indices.length-1; i >= 0; i--) {
            VisualOverlay visual = 
              (VisualOverlay) overlayList.removeElement (indices[i]);
            firePropertyChange (OVERLAY_PROPERTY, visual.getValue(), null);
          } // for
          if (overlayList.getElements() != 0)
            overlayList.setSelectionInterval (0, 0);
        } // if

        // Edit overlay
        // ------------
        else if (command.equals (EDIT_COMMAND)) {
          VisualOverlay visual = 
            (VisualOverlay) overlayList.getElement (indices[0]);
          visual.showChooser();
        } // else if

        // Move overlay up
        // ---------------
        else if (command.equals (UP_COMMAND)) {
          int index = indices[0];
          if (index != 0) {
            overlayList.moveElement (index, index-1);
            overlayList.setSelectionInterval (index-1, index-1);
            swapLayers (index, index-1);
            /**
             * For now, we only fire one property change event even
             * though both layer numbers have been altered.  Strictly
             * speaking, we should fire two events but that would cause
             * rendering to occur twice which is not really necessary.
             * Similarly for moving an overlay down.
             */
            firePropertyChange (OVERLAY_PROPERTY, null, getOverlay (index-1));
          } // if
        } // else if

        // Move overlay down
        // -----------------
        else if (command.equals (DOWN_COMMAND)) {
          int index = indices[0];
          if (index != overlayList.getElements()-1) {
            overlayList.moveElement (index, index+1);
            overlayList.setSelectionInterval (index+1, index+1);
            swapLayers (index, index+1);
            firePropertyChange (OVERLAY_PROPERTY, null, getOverlay (index+1));
          } // if
        } // else if

        // Save selected overlays
        // ----------------------
        else if (command.equals (SAVE_COMMAND)) {
          List overlayList = getOverlayList();
          var data = OverlayGroupSavePanel.showDialog (
            AbstractOverlayListPanel.this, "Create new overlay group",
            (List<EarthDataOverlay>) overlayList, (List<String>) groupManager.getGroups());
          if (data != null) {
            try { 
              groupManager.saveGroup (data.overlayList, data.groupName);
            } // try
            catch (Exception e) {
              String message = "An error occurred while saving the overlay group:\n\n" + e;
              JOptionPane.showMessageDialog (AbstractOverlayListPanel.this, 
                message, "Error", JOptionPane.ERROR_MESSAGE);
            } // catch
          } // if
        } // else if

      } // else

    } // actionPerformed
  } // ListButtonListener class

  ////////////////////////////////////////////////////////////

  /** Swaps the specified overlay layer values. */
  private void swapLayers (
    int firstIndex,
    int secondIndex
  ) {

    EarthDataOverlay first = getOverlay (firstIndex);
    EarthDataOverlay second = getOverlay (secondIndex);
    int firstLayer = first.getLayer();
    first.setLayer (second.getLayer());
    second.setLayer (firstLayer);

  } // swapLayers

  ////////////////////////////////////////////////////////////

  /** Gets the overlay at the specified index. */
  private EarthDataOverlay getOverlay (
    int index
  ) {

    return ((EarthDataOverlay) ((VisualOverlay) overlayList.
      getElement (index)).getValue());

  } // getOverlay

  ////////////////////////////////////////////////////////////

  /** Handles group list change events from the group manager. */
  private class GroupListListener implements PropertyChangeListener {
    public void	propertyChange (PropertyChangeEvent event) {

      groupList.setListData (new Vector ((List)event.getNewValue()));

    } // propertyChange
  } // GroupListListener class

  ////////////////////////////////////////////////////////////

  /** Handles overlay selection changes. */
  private class OverlaySelectionListener implements PropertyChangeListener {
    public void	propertyChange (PropertyChangeEvent event) {

      // Get selected indices
      // --------------------
      int[] indices = overlayList.getSelectedIndices();

      // Enable the save button as long as there is at least one
      // overlay in the list to save.
      if (saveButton != null) saveButton.setEnabled (overlayList.getElements() != 0);

      // Disable all buttons
      // -------------------
      if (indices.length == 0) {
        if (editButton != null) editButton.setEnabled (false);
        if (removeButton != null) removeButton.setEnabled (false);
        if (clearButton != null) clearButton.setEnabled (false);
        if (upButton != null) upButton.setEnabled (false);
        if (downButton != null) downButton.setEnabled (false);
      } // if

      // Enable all buttons
      // ------------------
      else if (indices.length == 1) {
        if (editButton != null) editButton.setEnabled (true);
        if (removeButton != null) removeButton.setEnabled (true);
        if (clearButton != null) clearButton.setEnabled (true);
        if (upButton != null) upButton.setEnabled (true);
        if (downButton != null) downButton.setEnabled (true);
      } // else if

      // Enable some buttons
      // -------------------
      else {
        if (editButton != null) editButton.setEnabled (false);
        if (removeButton != null) removeButton.setEnabled (true);
        if (clearButton != null) clearButton.setEnabled (true);
        if (upButton != null) upButton.setEnabled (false);
        if (downButton != null) downButton.setEnabled (false);
      } // else

      // Check number of elements
      // ------------------------
      boolean isEnabled = (overlayList.getElements() != 0);
      if (hideButton != null) hideButton.setEnabled (isEnabled);
      if (showButton != null) showButton.setEnabled (isEnabled);

      // Fire selection change event
      // ---------------------------
      if (indices.length == 0) 
        firePropertyChange (SELECTION_PROPERTY, null, null);
      else if (indices.length == 1) {
        VisualOverlay visual =
          (VisualOverlay) overlayList.getElement (indices[0]);
        firePropertyChange (SELECTION_PROPERTY, null, visual.getValue());
      } // if

    } // propertyChange
  } // OverlaySelectionListener class

  ////////////////////////////////////////////////////////////

  /** Handles group selection changes. */
  private class GroupSelectionListener implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent e) {

      int[] indices = groupList.getSelectedIndices();
      loadButton.setEnabled (indices.length != 0);
      deleteButton.setEnabled (indices.length != 0);
      
    } // valueChanged
  } // GroupSelectionListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Removes all overlays from the list. No property change event is
   * fired.
   */
  protected void removeOverlays () {

    overlayList.clear();

  } // removeOverlays

  ////////////////////////////////////////////////////////////

  /**
   * Adds a set of overlays to the list.  This method should be called
   * by the child class to setup the initial list of overlays.  No
   * property change event is fired, and no layer numbers are
   * assigned, since we assume that the layers are set correctly.
   * Overlays are displayed in the order that the layer values
   * dictate.
   *
   * @param overlays the initial set of overlays.
   */
  protected void addOverlays (
    List overlays
  ) {

    // Sort overlays in layer order
    // ----------------------------
    List sortedOverlays = new LinkedList (overlays);
    Collections.sort (sortedOverlays);

    // Loop over each overlay
    // ----------------------
    for (Iterator iter = sortedOverlays.iterator(); iter.hasNext(); ) {

      // Create visual and add change listener
      // -------------------------------------
      EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
      VisualOverlay visual = new VisualOverlay (overlay);
      visual.addPropertyChangeListener (new PropertyChangeListener () {
          public void propertyChange (PropertyChangeEvent event) {
            firePropertyChange (OVERLAY_PROPERTY, event.getOldValue(), 
            event.getNewValue());
          } // propertyChange
        });

      // Add visual to list
      // ------------------
      overlayList.addElement (0, visual);

    } // for

  } // addOverlays

  ////////////////////////////////////////////////////////////

  /** 
   * Adds an overlay to the list.  This method should be called by the
   * child class when a new overlay is added.  A property change event
   * is fired to indicate that the overlay has been added.  The layer
   * number of the overlay is set to be larger than any existing layer
   * number in the list.  The new overlay is set to be selected in the
   * list.
   *
   * @param overlay the overlay to add.
   */
  protected void addOverlay (
    EarthDataOverlay overlay
  ) {

    // Set overlay layer
    // -----------------
    int maxLayer = baseLayer;
    for (Iterator iter = overlayList.iterator(); iter.hasNext();) {
      int layer = ((EarthDataOverlay) ((VisualOverlay) iter.next()).
        getValue()).getLayer();
      maxLayer = Math.max (maxLayer, layer);
    } // for
    overlay.setLayer (maxLayer + 1);

    // Create visual and add change listener
    // -------------------------------------
    VisualOverlay visual = new VisualOverlay (overlay);
    visual.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          firePropertyChange (OVERLAY_PROPERTY, event.getOldValue(), 
            event.getNewValue());
        } // propertyChange
      });

    // Add to list
    // -----------
    overlayList.addElement (0, visual);
    overlayList.setSelectionInterval (0, 0);

    // Fire change event that overlay is added
    // ---------------------------------------
    firePropertyChange (OVERLAY_PROPERTY, null, overlay);

  } // addOverlay

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets the list of add overlay buttons.  Generally, the list should
   * contain buttons of all one size with tool text and action
   * listeners attached.  In order to add an overlay when clicked, the
   * button listener should invoke the <code>addOverlay()</code>
   * method.
   *
   * @return the list of add buttons, or null to not create a button panel.
   */
  protected abstract List getAddButtons();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the title that will be used to annotate the button and list
   * panels.
   *
   * @return the title string, or null if no titles should be used.
   */
  protected abstract String getTitle();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the title that will be used to annotate the button panel.
   *
   * @return the title string, or null if no titles should be used.
   */
  protected String getButtonTitle() { 

    String title = getTitle();
    if (title == null) return (null);
    else return ("Add " + title);

  } // getButtonTitle

  ////////////////////////////////////////////////////////////

  /**
   * Gets the title that will be used to annotate the list panel.
   *
   * @return the title string, or null if no titles should be used.
   */
  protected String getListTitle() {

    String title = getTitle();
    if (title == null) return (null);
    else return (title + " List");

  } // getListTitle

  ////////////////////////////////////////////////////////////

  /**
   * Gets the title that will be used to annotate the groups panel.
   *
   * @return the title string, or null if no titles should be used.
   */
  protected String getGroupTitle() {

    String title = getTitle();
    if (title == null) return (null);
    else return (title + " Groups");

  } // getGroupTitle

  ////////////////////////////////////////////////////////////

  /** 
   * Gets and increments the current count of overlays created with
   * the specified key.  This method may be used by child classes to
   * help name new overlays as they are created.
   *
   * @param overlayKey the overlay key used to uniquely identify a
   * set of overlays.
   *
   * @return the total number of calls to this method with the
   * specified overlay key.
   */
  protected int getOverlayCount (
    Object overlayKey
  ) {

    Integer count = (Integer) counterMap.get (overlayKey);
    if (count == null) {
      count = Integer.valueOf (0);
    } // if
    count = Integer.valueOf (count.intValue() + 1);
    counterMap.put (overlayKey, count);
    return (count.intValue());

  } // getOverlayCount
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the list of overlays.
   *
   * @return the list of overlays.
   */
  public List getOverlayList() {

    List overlays = new ArrayList();
    int count = overlayList.getElements();
    for (int i = 0; i < count; i++)
      overlays.add (getOverlay (i));
    return (overlays);
    
  } // getOverlayList
  
  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class. 
   *
   * @param argv the array of arguments (ignored).
   */
  public static void main (String[] argv) {

    // Create panel
    // ------------
    AbstractOverlayListPanel panel = new AbstractOverlayListPanel () {
        protected List getAddButtons () {
          List list = new LinkedList();
          JButton button = new JButton ("Button1");
          button.addActionListener (new ActionListener() {
              public void actionPerformed (ActionEvent event) {
                addOverlay (new LatLonOverlay (Color.WHITE));
              } // actionPerformed
            });
          list.add (button);
          list.add (new JButton ("Button2"));
          list.add (new JButton ("Button3"));
          return (list);
        } // getAddButtons
        protected String getTitle() {
          return ("Test");
        } // getTitle
      };

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // AbstractOverlayListPanel class

////////////////////////////////////////////////////////////////////////
