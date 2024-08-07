////////////////////////////////////////////////////////////////////////
/*

     File: VisualFont.java
   Author: Peter Hollemans
     Date: 2004/02/27

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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;
import noaa.coastwatch.gui.visual.FontChooser;

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
    button = GUIServices.getTextButton (getFontDescription (font));
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
