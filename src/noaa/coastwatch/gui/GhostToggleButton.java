////////////////////////////////////////////////////////////////////////
/*

     File: GhostToggleButton.java
   Author: Peter Hollemans
     Date: 2007/07/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2007 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import noaa.coastwatch.gui.GhostButton;
import noaa.coastwatch.gui.TestContainer;

/**
 * The <code>GhostToggleButton</code> class is a button that displays
 * a "ghostly" looking version of a standard button icon for use in
 * full screen mode toolbar menus.  The icon is modified so that it
 * appears in grayscale and inverted.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class GhostToggleButton
  extends JToggleButton {

  // Variables
  // ---------

  /** The ghostly icon derived from the user's icon. */
  private Icon ghostIcon;

  /** The selected version of the ghost icon. */
  private Icon ghostIconBright;

  /** The size of the button. */
  private Dimension buttonSize;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new button using an icon and text.  
   *
   * @param text the button text label.
   * @param icon the button icon.
   */
  public GhostToggleButton (
    String text,
    Icon icon
  ) {

    // Initialize
    // ----------
    super (text, icon);

    // Create icons
    // ------------
    Icon[] icons = GhostButton.getGhostIcons (icon);
    ghostIcon = icons[0];
    ghostIconBright = icons[1];

    // Set button properties
    // ---------------------
    buttonSize = new Dimension (icon.getIconWidth(), icon.getIconHeight());
    setOpaque (false);
    setBorder (null);

  } // GhostToggleButton

  ////////////////////////////////////////////////////////////

  public Dimension getMinimumSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  public Dimension getMaximumSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  public Dimension getPreferredSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  protected void paintComponent (Graphics g) {

    Icon icon = (isSelected() ? ghostIconBright : ghostIcon);
    icon.paintIcon (this, g, 0, 0);

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {

    // Create panel
    // ------------
    JPanel panel = new JPanel();
    panel.setLayout (new BoxLayout (panel, BoxLayout.X_AXIS));
    panel.setBackground (Color.BLACK);
    panel.add (new GhostToggleButton ("GhostToggleButton", 
      new ImageIcon (argv[0])));
    panel.setBorder (new CompoundBorder (
      new EmptyBorder (10, 10, 10, 10),
      new LineBorder (new Color (1.0f, 1.0f, 1.0f, 0.2f), 2, true)
    ));

    TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // GhostToggleButton class

////////////////////////////////////////////////////////////////////////
