////////////////////////////////////////////////////////////////////////
/*

     File: AbstractVisualObject.java
   Author: Peter Hollemans
     Date: 2004/02/29

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
import javax.swing.event.SwingPropertyChangeSupport;

import noaa.coastwatch.gui.visual.VisualObject;
import noaa.coastwatch.gui.visual.ComponentProducer;

/**
 * The <code>AbstractVisualObject</code> class implements default versions
 * of the following interface methods:
 * <ul>
 *   <li>{@link ComponentProducer#refreshComponent} (no operation)</li>
 *   <li>{@link VisualObject#addPropertyChangeListener}</li>
 *   <li>{@link VisualObject#firePropertyChange}</li>
 *   <li>{@link VisualObject#showChooser} (no operation)</li>
 *   <li>{@link VisualObject#hasChooser} (returns false)</li>
 *   <li>{@link VisualObject#setRestrictions} (no operation)</li>
 * </ul>
 * Child classes must implement:
 * <ul>
 *   <li>{@link ComponentProducer#getComponent}</li>
 *   <li>{@link VisualObject#getValue}</li>
 * </ul>
 * and override any default behaviours needed for the methods provided here.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class AbstractVisualObject
  implements VisualObject {
  
  // Constants
  // ---------

  /** The value property. */
  public static final String VALUE_PROPERTY = "value";

  // Variables
  // ---------

  /** The property change support object used for firing events. */
  private SwingPropertyChangeSupport changeSupport;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new abstract visual object and initializes the property
   * change support.
   */
  protected AbstractVisualObject () {

    changeSupport = new SwingPropertyChangeSupport (this);

  } // AbstractVisualObject constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void addPropertyChangeListener (
    PropertyChangeListener listener
  ) {

    changeSupport.addPropertyChangeListener (listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////

  /** 
   * Fires a property change event for this object's value.  The old
   * property value is set to null, and the new value is set to the
   * value returned by <code>getValue()</code>.
   */
  @Override
  public void firePropertyChange () {

    changeSupport.firePropertyChange (VALUE_PROPERTY, null, getValue());

  } // firePropertyChange

  ////////////////////////////////////////////////////////////

  /** 
   * Fires a property change event for this object's value using the
   * specified old and new values.
   *
   * @param oldValue the old value for the property change event.
   * @param newValue the new value for the property change event.
   */
  public void firePropertyChange (
    Object oldValue,
    Object newValue
  ) {

    changeSupport.firePropertyChange (VALUE_PROPERTY, oldValue, newValue);

  } // firePropertyChange

  ////////////////////////////////////////////////////////////

  @Override
  public void showChooser () { } // no operation

  ////////////////////////////////////////////////////////////

  @Override
  public boolean hasChooser () { return (false); }

  ////////////////////////////////////////////////////////////

  @Override
  public void setRestrictions (Object restrict) { } // no operation

  ////////////////////////////////////////////////////////////

  /** 
   * Refreshes the component display to show the contents of the
   * current object.  This method does nothing unless overridden in
   * the child class.
   */
  public void refreshComponent () { } // no operation

  ////////////////////////////////////////////////////////////

} // AbstractVisualObject class

////////////////////////////////////////////////////////////////////////
