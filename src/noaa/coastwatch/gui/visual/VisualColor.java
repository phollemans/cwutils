////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualColor.java
  PURPOSE: Defines a visual interface for a color.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/23
  CHANGES: 2006/03/19, PFH, modified to use GUIServices.setSquare()
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// TODO: When the color chooser is opened, it should show the current
// drawing color in the top of the color preview window, rather than
// the first color that happened to be selected when the color chooser
// was created.

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.border.EmptyBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;
import noaa.coastwatch.gui.visual.ColorSwatch;
import noaa.coastwatch.gui.visual.SimpleColorChooser;

/**
 * The <code>VisualColor</code> class represents a color as a button
 * with an icon of the color.  When the button is pressed, a
 * <code>JColorChooser</code> appears that allows the user to select
 * a color.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualColor 
  extends AbstractVisualObject {

  // Constants 
  // ---------

  /** The size of the color icon. */
  private static final int ICON_SIZE = 10;

  // Variables
  // ---------

  /** The color object. */
  private Color color;

  /** The color component button. */
  private JButton button;

  /** The color swatch icon. */
  private ColorSwatch swatch;

  /** The popup menu used to show the simple color chooser. */
  private JPopupMenu popup;

  /** The simple color chooser panel. */
  private SimpleColorChooser chooser;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual color object using the specified color. */
  public VisualColor (
    Color color
  ) {                     

    // Create swatch
    // -------------
    this.color = color;
    swatch = new ColorSwatch (color, ICON_SIZE, ICON_SIZE);

    // Create simple color chooser
    // ---------------------------
    chooser = new SimpleColorChooserMenuItem();
    chooser.addPropertyChangeListener (SimpleColorChooser.COLOR_PROPERTY, 
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          setColor ((Color) event.getNewValue());
        } // propertyChange
      });

    // Create popup menu
    // -----------------
    popup = new JPopupMenu();
    popup.add (chooser);

    // Create button
    // -------------
    button = new JButton (swatch);
    button.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          popup.show (button, 0, button.getHeight());
        } // actionPerformed
      });
    GUIServices.setSquare (button);

  } // VisualColor constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Extends the <code>SimpleColorChooser</code> class to be a menu
   * item.  The simple color chooser panel is set to a small 5x7
   * subset of the total color palette.  The chooser is modified so
   * that it works with the popup menu.
   */
  private class SimpleColorChooserMenuItem 
    extends SimpleColorChooser 
    implements MenuElement {

    ////////////////////////////////////////////////////////

    /** Creates a new chooser. */
    public SimpleColorChooserMenuItem () {

      super (5, 7, color);
      this.setBorder (new EmptyBorder (2, 2, 2, 2));
      chooserButton.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          popup.setVisible (false);
        } // actionPerformed
      });
            
    } // SimpleColorChooserMenuItem constuctor

    ////////////////////////////////////////////////////////

    public void processMouseEvent (
      MouseEvent e, 
      MenuElement path[],
      MenuSelectionManager manager
    ) {}

    ////////////////////////////////////////////////////////

    public void processKeyEvent (
      KeyEvent e, 
      MenuElement path[],
      MenuSelectionManager manager
    ) {}

    ////////////////////////////////////////////////////////

    public void menuSelectionChanged (boolean isIncluded) {}
    
    ////////////////////////////////////////////////////////

    public MenuElement[] getSubElements()  {return (new MenuElement[0]); }

    ////////////////////////////////////////////////////////
 
    public Component getComponent() { return (this); }

    ////////////////////////////////////////////////////////

    /** 
     * Sets the color to the specified color and sets the popup menu
     * to invisible.
     */
    public void setColor (Color color) {

      super.setColor (color);
      popup.setVisible (false);

    } // setColor

    ////////////////////////////////////////////////////////

  } // SimpleColorChooserMenuItem class

  ////////////////////////////////////////////////////////////

  /** Gets the button used to represent the color. */
  public Component getComponent () { return (button); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the color and throws a property change event if the new
   * color is different.  The new color may be null.
   */
  private void setColor (
    Color newColor
  ) {

    if ((newColor == null && color != null) || 
      (newColor != null && !newColor.equals (color))) {
      color = newColor;
      swatch.setColor (color);
      button.repaint();
      firePropertyChange();
    } // if

  } // setColor

  ////////////////////////////////////////////////////////////

  /** Gets the color value. */
  public Object getValue () { return (color); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    Component comp =  new VisualColor (Color.WHITE).getComponent();
    panel.add (comp);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualColor class

////////////////////////////////////////////////////////////////////////
