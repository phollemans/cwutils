////////////////////////////////////////////////////////////////////////
/*
     FILE: FullScreenToolBar.java
  PURPOSE: Displays a tool bar of buttons designed for full screen viewing.
   AUTHOR: Peter Hollemans
     DATE: 2007/07/13
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2007, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.render.TextElement;

/**
 * The <code>FullScreenToolBar</code> class is a horizontal toolbar
 * for full screen modes.  The toolbar is semi-translucent, and each
 * button on the toolbar is represented by an icon and text label
 * which is displayed only when the mouse cursor is over the button.
 * Buttons may be simple buttons or toggle buttons.  Buttons may be
 * safely added to the toolbar without losing them in their original
 * layout since buttons are only used for their icons and text.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class FullScreenToolBar 
  extends JPanel {

  // Constants
  // ---------

  /** The border between buttons and edge of panel. */
  private static final int BORDER = 5;

  /** The margin between border and buttons. */
  private static final int MARGIN = 10;

  /** The space between adjacent buttons. */
  private static final int SPACE = 10;

  /** The arc on the corners of the rounded rectangle bounding the buttons. */
  private static final int ARC = 20;

  /** The font size for button labels. */
  private static final int FONT_SIZE = 14;

  /** The font to use for labeling buttons. */
  private static final Font LABEL_FONT = new Font (null, Font.BOLD, FONT_SIZE);

  /** The vertical space for the button labels. */
  private static final int LABEL_SPACE =  MARGIN + FONT_SIZE + 5;

  /** The total width of a separator. */
  private static final int SEPARATOR_WIDTH = SPACE + SPACE/2;

  /** The property name for mouse activity. */
  public static final String MOUSE_ACTIVITY_PROPERTY = "mouseActivity";

  // Variables
  // ---------

  /** The list of buttons to display. */
  private List<FullScreenButton> buttonList = 
    new ArrayList<FullScreenButton>();

  /** The rectangle enclosing all buttons. */
  private RectangularShape buttonBorder;

  /** The currently active button. */
  private FullScreenButton activeButton = null;

  /** 
   * The pressed flag, true if we should paint the active button as
   * pressed. 
   */
  private boolean isPressed;

  /** 
   * The mouse activity flag, true if mouse activity is detected
   * within the button border.
   */
  private boolean isMouseActive;

  /** The alpha value to use for painting the toolbar. */
  private float alpha = 1;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the alpha composite value currently set for painting
   * the toolbar. 
   *
   * @return the alpha value in the range [0..1].
   */
  public float getAlpha () { return (alpha); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the new alpha composite value for painting the toolbar.
   *
   * @param alpha the new alpha value in the range [0..1].
   */
  public void setAlpha (
    float alpha
  ) {

    this.alpha = alpha;

  } // setAlpha

  ////////////////////////////////////////////////////////////
  
  /** Holds information for one fullscreen button. */
  private static class FullScreenButton {

    /** The original source button passed to the toolbar. */
    public AbstractButton source;

    /** The dark and light icons to use for drawing the button. */
    public Icon[] icons;

    /** The bounds rectangle for drawing the button. */
    public Rectangle bounds;

    /** The text element for labelling the button. */
    public TextElement textElement;

    /** The flag to indicate a separator after this button. */
    public boolean hasSeparator;

  } // FullScreenButton class

  ////////////////////////////////////////////////////////////

  /** Creates a new toolbar. */
  public FullScreenToolBar () {

    // Add mouse events
    // ----------------
    this.addMouseListener (new MouseInputAdapter() {
        public void mousePressed (MouseEvent e) { 
          isPressed = true; 
          repaint(); 
        } // mousePressed
        public void mouseReleased (MouseEvent e) { 
          isPressed = false; 
          repaint();
        } // mouseReleased
        public void mouseClicked (MouseEvent e) { 
          if (activeButton != null) {
            activeButton.source.doClick();
            repaint();
          } // if
        } // mouseClicked
        public void mouseExited (MouseEvent e) {
          activeButton = null;
          repaint();
          updateMouseActivity (false);
        } // mouseExited
      });
    this.addMouseMotionListener (new MouseInputAdapter() {
        public void mouseMoved (MouseEvent e) { 
          Point p = e.getPoint();
          updateActiveButton (p);
          updateMouseActivity (buttonBorder.contains (p));
        } // mouseMoved
        public void mouseDragged (MouseEvent e) { mouseMoved (e); }
      });

  } // FullScreenToolBar constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the mouse activity flag and fires a property change
   * if necessary.
   *
   * @param flag the new mouse activity status.
   */
  private void updateMouseActivity (boolean flag) {

    if (isMouseActive != flag) {
      firePropertyChange (MOUSE_ACTIVITY_PROPERTY, isMouseActive, flag);
      isMouseActive = flag;
    } // if

  } // updateMouseActivity

  ////////////////////////////////////////////////////////////

  /** Updates the active button according to the mouse position. */
  private void updateActiveButton (
    Point p
  ) {

    // Find button under cursor
    // ------------------------
    FullScreenButton newActiveButton = null;
    for (FullScreenButton button : buttonList)
      if (button.bounds.contains (p)) { newActiveButton = button ; break; }

    // Update active button
    // --------------------
    if (activeButton != newActiveButton) {
      activeButton = newActiveButton;
      repaint();
    } // if

  } // updateActiveButton

  ////////////////////////////////////////////////////////////

  /** Adds a new separator after the current button. */
  public void addSeparator () {

    if (!buttonList.isEmpty()) 
      buttonList.get (buttonList.size()-1).hasSeparator = true;      

  } // addSeparator

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new button to the toolbar.
   *
   * @param button the new button to add.
   */
  public void addButton (
    AbstractButton button
  ) {

    addButton (button, button.getIcon());

  } // addButton

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new button to the toolbar.
   *
   * @param button the new button to add.
   * @param icon the icon to use, or null to use the button's own icon.
   */
  public void addButton (
    AbstractButton button,
    Icon icon
  ) {

    // Add button to list
    // ------------------
    FullScreenButton fsButton = new FullScreenButton();
    fsButton.source = button;
    if (icon == null) icon = button.getIcon();
    fsButton.icons = getGhostIcons (icon);
    int x;
    if (buttonList.isEmpty())
      x = BORDER + MARGIN;
    else {
      FullScreenButton lastButton = buttonList.get (buttonList.size()-1);
      x = lastButton.bounds.x + lastButton.bounds.width;
      if (lastButton.hasSeparator) x += SEPARATOR_WIDTH;
      else x += SPACE;
    } // else
    fsButton.bounds = new Rectangle (x, LABEL_SPACE + MARGIN, 
      icon.getIconWidth(), icon.getIconHeight());
    fsButton.textElement = new TextElement (button.getText(), LABEL_FONT, 
      new Point (fsButton.bounds.x + fsButton.bounds.width/2, 
      fsButton.bounds.y - 2*MARGIN), new double[] {0.5, 0}, 0);
    buttonList.add (fsButton);

    // Update button border
    // --------------------
    int maxButtonHeight = 0;
    for (FullScreenButton thisButton : buttonList)
      maxButtonHeight = Math.max (maxButtonHeight, thisButton.bounds.height);
    buttonBorder = new RoundRectangle2D.Double (
      BORDER,
      LABEL_SPACE,
      (fsButton.bounds.x + 1) + fsButton.bounds.width + MARGIN,
      maxButtonHeight + 2*MARGIN,
      ARC,
      ARC
    );
    setPreferredSize (new Dimension ((int) buttonBorder.getWidth() + 2*BORDER, 
      (int) buttonBorder.getHeight() + LABEL_SPACE + BORDER));

  } // addButton

  ////////////////////////////////////////////////////////////

  protected void paintComponent (Graphics g) {

    if (isOpaque()) super.paintComponent (g);

    // Set antialias hint
    // ------------------
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
      RenderingHints.VALUE_ANTIALIAS_ON);

    // Set alpha composite
    // -------------------
    if (alpha != 1) {
      g2.setComposite (AlphaComposite.getInstance (AlphaComposite.SRC_OVER, 
        alpha));
    } // if

    // Draw button border
    // ------------------
    g2.setColor (new Color (0, 0, 0, 128));
    g2.fill (buttonBorder);
    g2.setColor (new Color (128, 128, 128, 128));
    g2.setStroke (new BasicStroke (1.0f));
    g2.draw (buttonBorder);

    // Draw buttons
    // ------------
    for (FullScreenButton button : buttonList) {

      // Draw icon
      // ---------
      Icon icon = ((isPressed && button == activeButton) || 
        button.source.isSelected() ? button.icons[1] : button.icons[0]);
      icon.paintIcon (this, g2, button.bounds.x, button.bounds.y);

      // Draw separator
      // --------------
      if (button.hasSeparator) {
        int x = button.bounds.x + button.bounds.width + SEPARATOR_WIDTH/2;
        g2.setColor (Color.LIGHT_GRAY);
        g2.drawLine (x, button.bounds.y, x, button.bounds.y + 
          button.bounds.height);
      } // if

      // Draw label
      // ----------
      if (button == activeButton) {
        button.textElement.render (g2, Color.WHITE, Color.BLACK);
      } // if

    } // for

    g2.dispose();

  } // paintComponent

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

  } // getGhostIcons

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {

    // Create toolbar
    // --------------
    FullScreenToolBar toolbar = new FullScreenToolBar();
    for (int i = 0; i < argv.length; i++) {
      JButton button = new JButton ("Button " + (i+1), 
        new ImageIcon (argv[i]));
      button.setActionCommand ("Button " + (i+1));
      toolbar.addButton (button);
      if (i == argv.length-2) toolbar.addSeparator();
    } // for

    TestContainer.showFrame (toolbar);

  } // main

  ////////////////////////////////////////////////////////////

} // FullScreenToolBar class

////////////////////////////////////////////////////////////////////////
