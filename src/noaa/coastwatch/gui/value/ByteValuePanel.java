////////////////////////////////////////////////////////////////////////
/*

     File: ByteValuePanel.java
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
import javax.swing.JTextField;
import noaa.coastwatch.gui.value.ParsableValuePanel;

/**
 * A <code>ByteValuePanel</code> holds a Byte value and allows the user
 * to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class ByteValuePanel
  extends ParsableValuePanel<Byte> {

  ////////////////////////////////////////////////////////////

  protected Byte getInitialValue() { return ((byte) 0); }

  ////////////////////////////////////////////////////////////

  @Override
  protected Byte parseText (
    JTextField field
  ) {

    Byte parsedValue;
    try {
      parsedValue = Byte.valueOf (field.getText());
    } // try
    catch (NumberFormatException e) {
      parsedValue = null;
    } // catch

    return (parsedValue);

  } // parseText

  ////////////////////////////////////////////////////////////

} // ByteValuePanel class

////////////////////////////////////////////////////////////////////////

