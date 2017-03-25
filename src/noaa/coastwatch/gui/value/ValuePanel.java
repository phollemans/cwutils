////////////////////////////////////////////////////////////////////////
/*

     File: ValuePanel.java
   Author: Peter Hollemans
     Date: 2017/03/14

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.value;

// Imports
// -------
import javax.swing.JPanel;

/**
 * A <code>ValuePanel</code> hold an object value and allows the user
 * to change it.  Property change events are fired when the object value is
 * changed by the user.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public abstract class ValuePanel<T> extends JPanel {

  // Constants
  // ---------

  /** The name for value property change events. */
  public static final String VALUE_PROPERTY = "value";

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the value in this panel.  Calling this method does not cause the
   * panel to fire a property change event.
   *
   * @param value the new object value.
   */
  public abstract void setValue (T value);
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the object value for this panel.
   *
   * @return the object value.
   */
  public abstract T getValue();

  ////////////////////////////////////////////////////////////
  
  /** Fires a property change event with a new value. */
  protected void signalValueChanged() {
    
    firePropertyChange (VALUE_PROPERTY, null, getValue());
  
  } // valueChanged

  ////////////////////////////////////////////////////////////

} // ValuePanel class

////////////////////////////////////////////////////////////////////////

