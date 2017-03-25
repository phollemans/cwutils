////////////////////////////////////////////////////////////////////////
/*

     File: TextFieldVerifierImp.java
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
import javax.swing.JTextField;

/**
 * The <code>TextFieldVerifierImp</code> interface specifies methods
 * that a {@link TextFieldVerifier} requires to perform the verification.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public interface TextFieldVerifierImp {

  ////////////////////////////////////////////////////////////

  /**
   * Verifies that the text in the field is valid.
   *
   * @param field the text field to check.
   *
   * @return true if the text is valid or false if not.
   */
  public boolean verifyText (JTextField field);

  ////////////////////////////////////////////////////////////
  
  /**
   * Resets the text in the field to a valid value.
   *
   * @param field the text field to check.
   */
  public void resetText (JTextField field);

  ////////////////////////////////////////////////////////////
  
  /**
   * Handles a change in text in the field.
   * 
   * @param field the text field to take action on.
   */
  public void textChanged (JTextField field);

  ////////////////////////////////////////////////////////////

} // TextFieldVerifierImp class

////////////////////////////////////////////////////////////////////////
