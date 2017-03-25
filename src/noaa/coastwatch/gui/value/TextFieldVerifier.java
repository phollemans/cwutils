////////////////////////////////////////////////////////////////////////
/*

     File: TextFieldVerifier.java
   Author: Peter Hollemans
     Date: 2017/03/15

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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A <code>TextFieldVerifier</code> attaches to a JTextField and verifies 
 * that the string conforms to a certain format.  The verifier uses a 
 * {@link TextFieldVerifierImp} object to determine correctness and to 
 * detect changes in the field value.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class TextFieldVerifier
  extends InputVerifier
  implements ActionListener {

  // Variables
  // ---------
  
  /** The verifier implementor for this object. */
  private TextFieldVerifierImp verifierImp;

  ////////////////////////////////////////////////////////////

  @Override
  public boolean verify (JComponent input) {

    boolean isValid = verifierImp.verifyText ((JTextField) input);
    return (isValid);

  } // verify

  ////////////////////////////////////////////////////////////

  @Override
  public boolean shouldYieldFocus (JComponent input) {

    boolean isValid = verify (input);
    JTextField field = (JTextField) input;
    if (!isValid) {
      Toolkit.getDefaultToolkit().beep();
      verifierImp.resetText (field);
      field.selectAll();
    } // if
    verifierImp.textChanged (field);

    return (isValid);

  } // shouldYieldFocus

  ////////////////////////////////////////////////////////////

  @Override
  public void actionPerformed (ActionEvent event) {

    JTextField field = (JTextField) event.getSource();
    shouldYieldFocus (field);

  } // actionPerformed

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new verifier for the specified field and implementor.
   *
   * @param field the text field to monitor and verify.
   * @param imp the verifier implementor to use.
   */
  public TextFieldVerifier (
    JTextField field,
    TextFieldVerifierImp imp
  ) {
  
    field.setInputVerifier (this);
    field.addActionListener (this);
    this.verifierImp = imp;
  
  } // TextFieldVerifier constructor

  ////////////////////////////////////////////////////////////

} // TextFieldVerifier class

////////////////////////////////////////////////////////////////////////


