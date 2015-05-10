////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractVisualObject.java
  PURPOSE: Partially implements the visual object interface.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/29
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

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

/**
 * The <code>AbstractVisualObject</code> class implements property
 * change support, default chooser behaviour, and restrictions for
 * visual objects.
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

  /** 
   * Adds a listener to receive property change events when the object's
   * value changes.
   */
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
  public void firePropertyChange () {

    changeSupport.firePropertyChange (VALUE_PROPERTY, null, getValue());

  } // firePropertyChange

  ////////////////////////////////////////////////////////////

  /** 
   * Fires a property change event for this object's value using the
   * specified old and new values.
   */
  public void firePropertyChange (
    Object oldValue,
    Object newValue
  ) {

    changeSupport.firePropertyChange (VALUE_PROPERTY, oldValue, newValue);

  } // firePropertyChange

  ////////////////////////////////////////////////////////////

  /** 
   * Shows the chooser dialog used to manipulate the object's
   * properties.  This method has no effect unless overridden in the
   * child class.
   */
  public void showChooser () { }

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if this object has a chooser, or false if not.  This
   * method always returns false unless overridden in the child class.
   */
  public boolean hasChooser () { return (false); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets restrictions on the allowed object values.  This method does
   * nothing unless overridden in the child class.
   */
  public void setRestrictions (Object restrict) { }

  ////////////////////////////////////////////////////////////

  /** 
   * Refreshes the component display to show the contents of the
   * current object.  This method does nothing unless overridden in
   * the child class.
   */
  public void refreshComponent () { }

  ////////////////////////////////////////////////////////////

} // AbstractVisualObject class

////////////////////////////////////////////////////////////////////////
