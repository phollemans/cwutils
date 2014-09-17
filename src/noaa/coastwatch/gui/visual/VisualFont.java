////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualFont.java
  PURPOSE: Defines a visual interface for a font.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/27
  CHANGES: 2006/03/19, PFH, removed setMargin() call for better LAF behaviour
           
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
 * The <code>VisualFont</code> class represents a font as a
 * button with the name of the font.  When the button is
 * pressed, a <code>FontChooser</code> appears that allows the user
 * to select a new font.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualFont 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The font object. */
  private Font font;

  /** The font component button. */
  private JButton button;

  /** The font chooser panel. */
  private static FontChooser chooserPanel = new FontChooser();

  ////////////////////////////////////////////////////////////

  /** Creates a new visual font object using the specified font. */
  public VisualFont (
    Font font
  ) {                     

    // Initialize
    // ----------
    this.font = font;

    // Create button
    // -------------
    button = new JButton (getFontDescription (font));
    button.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          showDialog();
        } // actionPerformed
      });

  } // VisualFont constructor

  ////////////////////////////////////////////////////////////

  /** Shows the font chooser dialog. */
  private void showDialog () {

    // Set font in panel
    // -----------------
    chooserPanel.setFontSelection (font);

    // Create dialog
    // -------------
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
        public void run () {
          setFont (chooserPanel.getFontSelection());
        } // run
      });
    Action cancelAction = GUIServices.createAction ("Cancel", null);
    JDialog dialog = GUIServices.createDialog (
      button, "Select a text font", true, chooserPanel,
      null, new Action[] {okAction, cancelAction}, null, true);

    // Show dialog
    // -----------
    dialog.setVisible (true);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** Gets the button used to represent the font. */
  public Component getComponent () { return (button); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the font and fires a property change event if the new font
   * is different.  The new font may be null, in which case nothing
   * is changed.
   */
  private void setFont (
    Font newFont
  ) {

    if (newFont != null && !newFont.equals (font)) {
      font = newFont;
      button.setText (getFontDescription (font));
      firePropertyChange();
    } // if

  } // setFont

  ////////////////////////////////////////////////////////////

  /** Gets a font description. */
  public static String getFontDescription (
    Font font
  ) {

    String description = font.getFamily();
    if (font.isPlain()) description += " Plain";
    if (font.isBold()) description += " Bold";
    if (font.isItalic()) description += " Italic";
    description += " " + font.getSize();
    return (description);

  } // getFontDescription

  ////////////////////////////////////////////////////////////

  /** Gets the font value. */
  public Object getValue () { return (font); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    Font font = Font.decode (argv[0]);
    Component comp =  new VisualFont (font).getComponent();
    panel.add (comp);
    panel.add (new VisualFont (font).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualFont class

////////////////////////////////////////////////////////////////////////
