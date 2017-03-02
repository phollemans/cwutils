////////////////////////////////////////////////////////////////////////
/*

     File: VisualObject.java
   Author: Peter Hollemans
     Date: 2004/02/23

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.beans.PropertyChangeListener;
import noaa.coastwatch.gui.visual.ComponentProducer;

/**
 * The <code>VisualObject</code> interface defines the methods
 * required for objects to have a visual onscreen representation.  The
 * onscreen version may be used to manipulate the properties of the
 * object.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface VisualObject
  extends ComponentProducer {

  /** 
   * Shows the chooser dialog used to manipulate the object's
   * properties.  In some cases, a visual object may not have a
   * chooser, in which case <code>hasChooser()</code> should return
   * false and this method has no effect.
   */
  public void showChooser ();

  /** Returns true if this object has a chooser, or false if not. */
  public boolean hasChooser ();

  /** Gets the object value. */
  public Object getValue ();

  /** 
   * Adds a listener to receive property change events for when this
   * object's value changes.
   *
   * @param listener the listener to add.
   */
  public void addPropertyChangeListener (PropertyChangeListener listener);

  /** Fires a property change event for this object's value. */
  public void firePropertyChange ();

  /** Sets restrictions on the allowed object values. */
  public void setRestrictions (Object restrict);
  
} // VisualObject interface

////////////////////////////////////////////////////////////////////////
