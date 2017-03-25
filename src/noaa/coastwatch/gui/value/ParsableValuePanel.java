////////////////////////////////////////////////////////////////////////
/*

     File: ParsableValuePanel.java
   Author: Peter Hollemans
     Date: 2017/03/23

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
// --------
import java.awt.FlowLayout;
import javax.swing.JTextField;

import noaa.coastwatch.gui.value.ValuePanel;
import noaa.coastwatch.gui.value.TextFieldVerifier;
import noaa.coastwatch.gui.value.TextFieldVerifierImp;

// Testing
import javax.swing.JButton;
import java.util.Date;

/**
 * A <code>ParsableValuePanel</code> holds a value that can be parsed from a 
 * text string and allows the user to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public abstract class ParsableValuePanel<T>
  extends ValuePanel<T>
  implements TextFieldVerifierImp {

  // Variables
  // ---------
  
  /** The value held by this object. */
  private T value;

  /** The text field to display the value. */
  private JTextField inputField;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the length of the text string.
   *
   * @param length the number of characters long that need to be parsed.
   */
  public void setTextLength (
    int length
  ) {
  
    inputField.setColumns (length);
    inputField.setMinimumSize (inputField.getPreferredSize());
    
  } // setTextLength

  ////////////////////////////////////////////////////////////

  /** Creates a new value panel.  The initial value is set to zero. */
  public ParsableValuePanel () {

    setLayout (new FlowLayout (FlowLayout.LEFT, 2, 0));
    inputField = new JTextField();
    inputField.setColumns (10);
    inputField.setMinimumSize (inputField.getPreferredSize());
    TextFieldVerifier verifier = new TextFieldVerifier (inputField, this);
    setValue (getInitialValue());
    this.add (inputField);

  } // ParsableValuePanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the initial value for this panel.
   * 
   * @return the initial value.
   */
  protected abstract T getInitialValue();

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (T value) {
  
    this.value = value;
    resetText (inputField);
    
  } // setValue
  
  ////////////////////////////////////////////////////////////

  @Override
  public T getValue() { return (value); }
  
  ////////////////////////////////////////////////////////////

  /**
   * Parses the text in the field to a value.
   *
   * @param field the text field to parse.
   *
   * @return the value parsed, or null if there was a
   * number format exception.
   */
  protected abstract T parseText (
    JTextField field
  );

  ////////////////////////////////////////////////////////////

  @Override
  public boolean verifyText (JTextField field) {
  
    boolean isValid = (parseText (field) != null);
    return (isValid);

  } // verifyText
  
  ////////////////////////////////////////////////////////////
  
  @Override
  public void resetText (JTextField field) {
  
    field.setText (value.toString());
    
  } // resetText

  ////////////////////////////////////////////////////////////
  
  @Override
  public void textChanged (JTextField field) {
  
    // Parse the text
    // --------------
    T newValue = parseText (field);
    // We should never have this next line happen
    if (newValue == null) throw new RuntimeException ("Error parsing text");

    // Check if we have a change in value
    // ----------------------------------
    if (!newValue.equals (value)) {
      value = newValue;
      signalValueChanged();
    } // if
  
  } // textChanged

  ////////////////////////////////////////////////////////////

} // ParsableValuePanel class

////////////////////////////////////////////////////////////////////////

