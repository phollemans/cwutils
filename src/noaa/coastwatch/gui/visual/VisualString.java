////////////////////////////////////////////////////////////////////////
/*

     File: VisualString.java
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;

/**
 * The <code>VisualString</code> class represents a string as a text
 * field.  When the text field is modified, the string changes value.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualString 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The text field component. */
  private JTextField field;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual string object using the specified String. */
  public VisualString (
    String text
  ) {                     

    // Create text field
    // -----------------
    field = new JTextField();
    field.setEditable (true);
    field.setColumns (8);
    field.setText (text);
    field.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          firePropertyChange();
        } // actionPerformed
      });
    field.addFocusListener (new FocusAdapter() {
        public void focusLost (FocusEvent event) {
          firePropertyChange();
        } // focusLost
      });

  } // VisualString constructor

  ////////////////////////////////////////////////////////////

  /** Gets the field used to represent the string. */
  public Component getComponent () { return (field); }

  ////////////////////////////////////////////////////////////

  /** Gets the string value. */
  public Object getValue () { return (field.getText()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualString (new String ("hello")).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualString class

////////////////////////////////////////////////////////////////////////
