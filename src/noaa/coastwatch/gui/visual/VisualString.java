////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualString.java
  PURPOSE: Defines a visual interface for a String.
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
import javax.swing.event.*;

/**
 * The <code>VisualString</code> class represents a string as a text
 * field.  When the text field is modified, the string changes value.
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

  /** Tests this class. */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualString (new String ("hello")).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualString class

////////////////////////////////////////////////////////////////////////
