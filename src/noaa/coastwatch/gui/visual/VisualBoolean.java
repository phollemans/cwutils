////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualBoolean.java
  PURPOSE: Defines a visual interface for a Boolean.
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

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
