////////////////////////////////////////////////////////////////////////
/*
     FILE: OverlayGroupManager.java
  PURPOSE: Allows the user to load and save overlay groups.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/03
  CHANGES: 2006/11/17, PFH, updated to detect GridContainerOverlay
           2006/12/29, PFH, added change listener support
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.event.*;
import noaa.coastwatch.io.*;

/** 
 * The <code>OverlayGroupManager</code> class can be used to save,
 * load, delete, and get a list of overlay groups.  Bitmask overlays
 * need special treatment in order to correctly restore the data
 * source used for bitmask data.  The {@link #setDataSource} method
 * should be called to set the bitmask data source prior to loading
 * any overlay groups that contain bitmask overlays.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class OverlayGroupManager {

  // Constants
  // ---------

  /** The overlay groups property. */
  public static final String OVERLAY_GROUPS_PROPERTY = "overlayGroups";

  // Variables
  // ---------

  /** The object manager. */
  private SerializedObjectManager manager;

  /** The reader to use for deserializing grid container overlays. */
  private EarthDataReader reader;

  /** The variable list to use for deserializing grid container overlays. */
  private List<String> variableList;

  /** The change support object for changes in overlay groups. */
  private static SwingPropertyChangeSupport changeSupport = 
    new SwingPropertyChangeSupport (new Object());

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data source for bitmask grid data.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public void setDataSource (
    EarthDataReader reader,
    List<String> variableList
  ) {

    this.reader = reader;
    this.variableList = variableList;

  } // setDataSource

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new manager to handle overlay groups in the specified
   * directory.
   *
   * @param groupDir the group directory used to perform all
   * group-related operations.
   */
  public OverlayGroupManager (
    File groupDir
  ) {

    manager = new SerializedObjectManager (groupDir);

  } // OverlayGroupManager constructor

  ////////////////////////////////////////////////////////////

  /**
   * Adds a listener for changes to the group list.
   * 
   * @param listener the listener to add to the list.
   */
  public void addPropertyChangeListener (
    PropertyChangeListener listener
  ) {

    changeSupport.addPropertyChangeListener (OVERLAY_GROUPS_PROPERTY, 
      listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////

  /**
   * Removes a listener for changes to the group list.
   * 
   * @param listener the listener to remove from the list.
   */
  public void removePropertyChangeListener (
    PropertyChangeListener listener
  ) {

    changeSupport.removePropertyChangeListener (OVERLAY_GROUPS_PROPERTY,
      listener);

  } // removePropertyChangeListener

  ////////////////////////////////////////////////////////////

  /** Gets the list of group names available. */
  public List<String> getGroups () { 

    return (manager.getObjectNames());

  } // getGroups

  ////////////////////////////////////////////////////////////

  /** 
   * Loads the specified group of overlays.
   *
   * @param group the group name, which must be a valid name obtained
   * from the {@link #getGroups} method.
   *
   * @return the list of overlays in the group.
   *
   * @throws IOException if an error occurred reading the group file.
   */
  public List<EarthDataOverlay> loadGroup (
    String group
  ) throws IOException {

    // Get list of overlays
    // --------------------
    List<EarthDataOverlay> overlayList;
    try {
      overlayList = (List<EarthDataOverlay>) manager.loadObject (group);
    } // try
    catch (ClassNotFoundException e) {
      throw new IOException (e.getMessage());
    } // catch

    // Fix overlays that need extra data
    // ---------------------------------
    for (EarthDataOverlay overlay : overlayList) {
      if (overlay instanceof GridContainerOverlay)
        ((GridContainerOverlay) overlay).setDataSource (reader, variableList);
    } // for

    return (overlayList);
      
  } // loadGroup

  ////////////////////////////////////////////////////////////

  /** 
   * Saves the specified overlay group.
   *
   * @param group the overlay group to save.
   * @param name the group name.  This is the name that may be used
   * later to retrieve the group.
   *
   * @throws IOException if an error occurred writing the group file.
   */
  public void saveGroup (
    List<EarthDataOverlay> group,
    String name
  ) throws IOException {

    List<String> groups = getGroups();
    manager.saveObject (group, name);
    if (!groups.contains (name)) {
      changeSupport.firePropertyChange (OVERLAY_GROUPS_PROPERTY,
        null, getGroups());
    } // if

  } // saveGroup

  ////////////////////////////////////////////////////////////

  /** 
   * Deletes the specified group.  A subsequent call to {@link
   * #getGroups} will not include thie specified name in the list.
   *
   * @throws IOException if an error occurred deleting the group file.
   */
  public void deleteGroup (
    String group
  ) throws IOException {

    manager.deleteObject (group);
    changeSupport.firePropertyChange (OVERLAY_GROUPS_PROPERTY,
      null, getGroups());

  } // deleteGroup

  ////////////////////////////////////////////////////////////

} // OverlayGroupManager class

////////////////////////////////////////////////////////////////////////
