////////////////////////////////////////////////////////////////////////
/*

     File: StringValuePanel.java
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
 * A <code>StringValuePanel</code> holds a String value and allows the user
 * to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class StringValuePanel
  extends ParsableValuePanel<String> {

  ////////////////////////////////////////////////////////////

  protected String getInitialValue() { return (""); }

  ////////////////////////////////////////////////////////////

  @Override
  protected String parseText (
    JTextField field
  ) {

    return (field.getText());

  } // parseText

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    StringValuePanel panel = new StringValuePanel();
    panel.addPropertyChangeListener (VALUE_PROPERTY, event -> {
      System.out.println ("[" + new Date().getTime() + "] event.getNewValue() = " + event.getNewValue());
    });
    panel.add (new JButton ("OK"));
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // StringValuePanel class

////////////////////////////////////////////////////////////////////////

