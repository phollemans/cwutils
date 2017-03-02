////////////////////////////////////////////////////////////////////////
/*

     File: DataViewOverlayControl.java
   Author: Peter Hollemans
     Date: 2006/12/07

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.awt.Shape;
import java.beans.PropertyChangeListener;

/**
 * The <code>DataViewOverlayControl</code> class is an interface
 * that can be implemented by any class that acts to control the
 * overlay content of an {@link EarthDataViewPanel}.  The control
 * signals a change in the overlays that should be displayed on
 * the view using {@link #OVERLAY_LIST_PROPERTY}, and a change in
 * the operation mode using {@link #OPERATION_MODE_PROPERTY}.
 * The operation mode property controls what drawing mode the
 * view should use, and uses mode constants from the {@link
 * LightTable} class.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public interface DataViewOverlayControl {

  // Constants
  // ---------

  /** The overlay list property. */
  public static final String OVERLAY_LIST_PROPERTY = "overlayList";

  /** The operation mode property. */
  public static final String OPERATION_MODE_PROPERTY = "operationMode";

  ////////////////////////////////////////////////////////////

  /**
   * Deactivates the control.  This is used to signal to the
   * control that some other control has been set active.  The
   * control unclicks any buttons that indicate the operation
   * mode.
   */
  public void deactivate ();

  ////////////////////////////////////////////////////////////

  /**
   * Adds a change listener to the list.  The control signals
   * changes in its properties using {@link
   * #OVERLAY_LIST_PROPERTY} and {@link #OPERATION_MODE_PROPERTY}.
   *
   * @param propertyName the property to listen for.
   * @param listener the listener to call when a property change
   * occurs.
   */
  public void addPropertyChangeListener (
    String propertyName,
    PropertyChangeListener listener
  );

  ////////////////////////////////////////////////////////////

  /**
   * Performs a view control operation using the specified shape.
   * The shape should correspond to the current operation mode.
   * Generally, this method is called when the user performs some
   * interactive operation on the view and the resulting shape is
   * made available.
   *
   * @param shape the shape to perform the operation for.
   */
  public void performOperation (
    Shape shape
  );

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the control activity state.
   *
   * @return true if the control is in an active state, or false
   * if not.  An active control is a control that is ready to
   * perform an operation using {@link #performOperation}.
   */
  public boolean isActive ();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current operation mode.
   *
   * @return the current operation mode as a mode from the {@link
   * LightTable} class.
   */
  public int getOperationMode ();

  ////////////////////////////////////////////////////////////

} // DataViewOverlayControl class

////////////////////////////////////////////////////////////////////////
