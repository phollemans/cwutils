////////////////////////////////////////////////////////////////////////
/*

     File: LongValuePanel.java
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
// --------
import javax.swing.JTextField;
import noaa.coastwatch.gui.value.ParsableValuePanel;

// Testing
import javax.swing.JButton;
import java.util.Date;

/**
 * A <code>LongValuePanel</code> holds a Long value and allows the user
 * to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class LongValuePanel
  extends ParsableValuePanel<Long> {

  ////////////////////////////////////////////////////////////

  protected Long getInitialValue() { return (0L); }

  ////////////////////////////////////////////////////////////

  @Override
  protected Long parseText (
    JTextField field
  ) {

    Long parsedValue;
    try {
      parsedValue = new Long (field.getText());
    } // try
    catch (NumberFormatException e) {
      parsedValue = null;
    } // catch

    return (parsedValue);

  } // parseText

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    LongValuePanel panel = new LongValuePanel();
    panel.addPropertyChangeListener (VALUE_PROPERTY, event -> {
      System.out.println ("[" + new Date().getTime() + "] event.getNewValue() = " + event.getNewValue());
    });
    panel.add (new JButton ("OK"));
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // LongValuePanel class

////////////////////////////////////////////////////////////////////////

