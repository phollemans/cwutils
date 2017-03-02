////////////////////////////////////////////////////////////////////////
/*

     File: GhostButton.java
   Author: Peter Hollemans
     Date: 2007/07/12

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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import noaa.coastwatch.gui.TestContainer;

/**
 * The <code>GhostButton</code> class is a button that displays a
 * "ghostly" looking version of a standard button icon for use in
 * full screen mode toolbar menus.  The icon is modified so that
 * it appears in grayscale and inverted.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class GhostButton
  extends JButton {

  // Variables
  // ---------

  /** The ghostly icon derived from the user's icon. */
  private Icon ghostIcon;

  /** The highlighted version of the ghost icon. */
  private Icon ghostIconBright;

  /** The size of the button. */
  private Dimension buttonSize;

  /** The pressed flag, true if we should paint the button as pressed. */
  private boolean isPressed;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new button using an icon and text.  
   *
   * @param text the button text label.
   * @param icon the button icon.
   */
  public GhostButton (
    String text,
    Icon icon
  ) {

    // Initialize
    // ----------
    super (text, icon);

    // Create icons
    // ------------
    Icon[] icons = getGhostIcons (icon);
    ghostIcon = icons[0];
    ghostIconBright = icons[1];

    // Set button properties
    // ---------------------
    buttonSize = new Dimension (icon.getIconWidth(), icon.getIconHeight());
    setOpaque (false);
    setBorder (null);

    // Add pressed listener
    // --------------------
    addMouseListener (new MouseInputAdapter() { 
        public void mousePressed (MouseEvent e) { isPressed = true; }
        public void mouseReleased (MouseEvent e) { isPressed = false; }
      });

  } // GhostButton

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>GhostImageFilter</code> converts normal color
   * pixels to ghostly inverted grayscale pixels.
   */
  public static class GhostImageFilter extends RGBImageFilter {

    public GhostImageFilter () { canFilterIndexColorModel = true; }

    public int filterRGB(int x, int y, int rgb) {
      int r = (rgb & 0xff0000) >> 16;
      int g = (rgb & 0x00ff00) >> 8;
      int b = (rgb & 0x0000ff);
      int gray = (int) Math.min (255, r*0.3 + g*0.59 + b*0.11);
      gray = 255-gray;
      int newRgb = (rgb & 0xff000000) | (gray << 16) | (gray << 8) | 
        gray;
      return (newRgb);
    } // filterRGB

  } // GhostImageFilter class

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>FadeImageFilter</code> converts normal color
   * pixels to either brigher or darker versions using a fade
   * factor.
   */
  public static class FadeImageFilter extends RGBImageFilter {

    // Variables
    // ---------

    /** The fade facter for each color component. */
    private double factor;

    ////////////////////////////////////////////////////////

    /**
     * Creates a new fade filter using the specified factor.  The
     * factor is multiplied by each pixel color component to
     * compute the new component value.
     *
     * @param factor the multiplicative fade factor.
     */
    public FadeImageFilter (double factor) { 

      canFilterIndexColorModel = true; 
      this.factor = factor;

    } // FadeImageFilter constructor

    ////////////////////////////////////////////////////////

    public int filterRGB(int x, int y, int rgb) {

      int r = (rgb & 0xff0000) >> 16;
      r = (int) Math.max (0, Math.min (r*factor, 255));
      int g = (rgb & 0x00ff00) >> 8;
      g = (int) Math.max (0, Math.min (g*factor, 255));
      int b = (rgb & 0x0000ff);
      b = (int) Math.max (0, Math.min (b*factor, 255));
      int newRgb = (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
      return (newRgb);

    } // filterRGB

    ////////////////////////////////////////////////////////

  } // FadeImageFilter class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a ghostly looking version of an icon.
   *
   * @param icon the original icon to use as a source.
   *
   * @return the ghostly looking icons derived from the source as
   * [normal, highlighted].
   */
  public static Icon[] getGhostIcons (
    Icon icon
  ) {

    // Create an image for the icon
    // ----------------------------
    int width = icon.getIconWidth();
    int height = icon.getIconHeight();
    BufferedImage image = new BufferedImage (width, height, 
      BufferedImage.TYPE_INT_ARGB);
    
    // Render the icon
    // ---------------
    Graphics2D graphics = image.createGraphics();
    icon.paintIcon (new JLabel(), graphics, 0, 0);

    // Modify the rendered image
    // -------------------------
    Image ghostImage = Toolkit.getDefaultToolkit().createImage (
      new FilteredImageSource (image.getSource(), new GhostImageFilter()));
    Image ghostImageBright = Toolkit.getDefaultToolkit().createImage (
      new FilteredImageSource (ghostImage.getSource(), 
      new FadeImageFilter (1.2)));
    Image ghostImageDark = Toolkit.getDefaultToolkit().createImage (
      new FilteredImageSource (ghostImage.getSource(), 
      new FadeImageFilter (0.8)));

    return (new ImageIcon[] {
      new ImageIcon (ghostImageDark),
      new ImageIcon (ghostImageBright)
    });

  } // getGhostIcon

  ////////////////////////////////////////////////////////////

  public Dimension getMinimumSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  public Dimension getMaximumSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  public Dimension getPreferredSize () { return (buttonSize); }

  ////////////////////////////////////////////////////////////

  protected void paintComponent (Graphics g) {

    Icon icon = (isPressed ? ghostIconBright : ghostIcon);
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
    panel.add (new GhostButton ("GhostButton", new ImageIcon (argv[0])));
    panel.setBorder (new CompoundBorder (
      new EmptyBorder (10, 10, 10, 10),
      new LineBorder (new Color (1.0f, 1.0f, 1.0f, 0.2f), 2, true)
    ));

    TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // GhostButton class

////////////////////////////////////////////////////////////////////////
