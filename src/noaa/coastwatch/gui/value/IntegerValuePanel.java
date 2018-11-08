////////////////////////////////////////////////////////////////////////
/*

     File: IntegerValuePanel.java
   Author: Peter Hollemans
     Date: 2017/03/24

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
import javax.swing.JTextField;
import noaa.coastwatch.gui.value.ParsableValuePanel;

/**
 * A <code>IntegerValuePanel</code> holds an Integer value and allows the user
 * to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class IntegerValuePanel
  extends ParsableValuePanel<Integer> {

  ////////////////////////////////////////////////////////////

  protected Integer getInitialValue() { return (0); }

  ////////////////////////////////////////////////////////////

  @Override
  protected Integer parseText (
    JTextField field
  ) {

    Integer parsedValue;
    try {
      parsedValue = Integer.valueOf (field.getText());
    } // try
    catch (NumberFormatException e) {
      parsedValue = null;
    } // catch

    return (parsedValue);

  } // parseText

  ////////////////////////////////////////////////////////////

} // IntegerValuePanel class

////////////////////////////////////////////////////////////////////////

