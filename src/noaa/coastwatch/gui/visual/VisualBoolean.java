////////////////////////////////////////////////////////////////////////
/*

     File: VisualBoolean.java
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;

/**
 * The <code>VisualBoolean</code> class represents a Boolean as a
 * checkbox button.  When the button is pressed, the Boolean changes
 * value.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualBoolean 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The boolean component button. */
  private JCheckBox checkBox;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual Boolean object using the specified Boolean. */
  public VisualBoolean (
    Boolean value
  ) {                     

    // Create button
    // -------------
    checkBox = new JCheckBox();
    checkBox.setSelected (value.booleanValue());
    checkBox.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          firePropertyChange();
        } // actionPerformed
      });

  } // VisualBoolean constructor

  ////////////////////////////////////////////////////////////

  /** Gets the button used to represent the Boolean. */
  public Component getComponent () { return (checkBox); }

  ////////////////////////////////////////////////////////////

  /** Gets the Boolean value. */
  public Object getValue () { return (new Boolean (checkBox.isSelected())); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualBoolean (new Boolean (true)).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualBoolean class

////////////////////////////////////////////////////////////////////////
