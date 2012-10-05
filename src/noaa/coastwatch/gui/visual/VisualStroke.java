////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualStroke.java
  PURPOSE: Defines a visual interface for a line stroke.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/25
  CHANGES: 2006/03/19, PFH, modified setMargin() call for better LAF behaviour
           
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
import java.beans.*;
import noaa.coastwatch.gui.*;

/**
 * The <code>VisualStroke</code> class represents a line stroke as a
 * button with an icon of the stoke pattern.  When the button is
 * pressed, a <code>StrokeChooser</code> appears that allows the user
 * to select a new stroke.
 */
public class VisualStroke 
  extends AbstractVisualObject {

  // Constants 
  // ---------

  /** The size of the stroke icon. */
  private static final int ICON_SIZE = 10;

  // Variables
  // ---------

  /** The stroke object. */
  private Stroke stroke;

  /** The stroke component button. */
  private JButton button;

  /** The stroke swatch icon. */
  private StrokeSwatch swatch;

  /** The stroke chooser panel. */
  private static StrokeChooser chooserPanel = new StrokeChooser();

  ////////////////////////////////////////////////////////////

  /** Creates a new visual stroke object using the specified stroke. */
  public VisualStroke (
    Stroke stroke
  ) {                     

    // Create swatch
    // -------------
    this.stroke = stroke;
    swatch = new StrokeSwatch (stroke, ICON_SIZE*3, ICON_SIZE);

    // Create button
    // -------------
    button = new JButton (swatch);
    button.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          showDialog();
        } // actionPerformed
      });
    Insets margin = button.getMargin();
    margin.left = margin.top;
    margin.right = margin.top;
    button.setMargin (margin);

  } // VisualStroke constructor

  ////////////////////////////////////////////////////////////

  /** Shows the font chooser dialog. */
  private void showDialog () {

    // Set stroke in panel
    // -------------------
    chooserPanel.setStroke (stroke);

    // Create dialog
    // -------------
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
        public void run () {
          setStroke (chooserPanel.getStroke());
        } // run
      });
    Action cancelAction = GUIServices.createAction ("Cancel", null);
    JDialog dialog = GUIServices.createDialog (
      button, "Select a line style", true, chooserPanel, 
      null, new Action[] {okAction, cancelAction}, null, true);

    // Show dialog
    // -----------
    dialog.setVisible (true);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** Gets the button used to represent the stroke. */
  public Component getComponent () { return (button); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the stroke and fires a property change event if the new
   * stroke is different.  The new stroke may be null.
   */
  private void setStroke (
    Stroke newStroke
  ) {

    if (newStroke != null && !newStroke.equals (stroke)) {
      stroke = newStroke;
      swatch.setStroke (stroke);
      button.repaint();
      firePropertyChange();
    } // if

  } // setStroke

  ////////////////////////////////////////////////////////////

  /** Gets the stroke value. */
  public Object getValue () { return (stroke); }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    Stroke stroke = StrokeChooser.getBasicStroke (2, 
      StrokeChooser.DASH_PATTERNS[1]);
    Component comp =  new VisualStroke (stroke).getComponent();
    panel.add (comp);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualStroke class

////////////////////////////////////////////////////////////////////////
