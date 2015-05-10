////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualInteger.java
  PURPOSE: Defines a visual interface for an Integer.
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
import java.awt.Component;
import java.lang.reflect.Array;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;

/**
 * The <code>VisualInteger</code> class represents an Integer as a
 * spinner.  When the spinner is modified, the Integer changes value.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualInteger 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The Integer spinner component. */
  private JSpinner spinner;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual Integer object using the specified Integer. */
  public VisualInteger (
    Integer value
  ) {                     

    // Create spinner
    // --------------
    spinner = new JSpinner (new SpinnerNumberModel (value.intValue(),
      Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
    JTextField field = 
      ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
    field.setColumns (5);
    spinner.addChangeListener (new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
          firePropertyChange();
        } // stateChanged
      });

  } // VisualInteger constructor

  ////////////////////////////////////////////////////////////

  /** Gets the button used to represent the Integer. */
  public Component getComponent () { return (spinner); }

  ////////////////////////////////////////////////////////////

  /** Gets the Integer value. */
  public Object getValue () { return (spinner.getValue()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets restrictions on the allowed integer values.
   * 
   * @param restrict the Integer[] restriction array as [min, max,
   * step].
   */
  public void setRestrictions (
    Object restrict
  ) {

    SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
    model.setMinimum ((Integer) Array.get (restrict, 0));
    model.setMaximum ((Integer) Array.get (restrict, 1));
    model.setStepSize ((Integer) Array.get (restrict, 2));

  } // setRestrictions

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualInteger (new Integer (5)).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualInteger class

////////////////////////////////////////////////////////////////////////
