/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.List;
import java.util.ArrayList;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.RenderingHints;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.GeneralPath;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.Action;
import javax.swing.UIManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.logging.Logger;

/**
 *
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class DropDownButton extends JButton {

  private static final Logger LOGGER = Logger.getLogger (DropDownButton.class.getName());    

  private static final int extra = 12;
  private static final int arrowSize = 6;    

  private JPopupMenu menu;
  private List<ActionListener> actionListenerList;

  ////////////////////////////////////////////////////////////

  private static class DropDownIcon implements Icon {

    // The issue with this method is that when the button is disabled,
    // the icon doesn't appear disabled.  So we need to create some sort
    // of disabled version of the icon, or set some type of image composite 
    // on the graphics context.

    private Icon icon;
    private int width, height;
    private GeneralPath arrow;

    public DropDownIcon (Icon icon) { 

      this.icon = icon;
      this.width = icon.getIconWidth() + extra;
      this.height = icon.getIconHeight();

      arrow = new GeneralPath();
      arrow.moveTo (0, 0);
      arrow.lineTo (arrowSize, 0);
      arrow.lineTo (arrowSize/2, arrowSize);
      arrow.lineTo (0, 0);

    } // DropDownIcon

    @Override
    public int getIconHeight () { return (height); }

    @Override
    public int getIconWidth () { return (width); }

    @Override
    public void paintIcon (Component c, Graphics g, int x, int y) {

      icon.paintIcon (c, g, x, y);
      var g2d = (Graphics2D) g.create();
      g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setColor (c.getForeground());
      g2d.translate (x + width - extra/2 - arrowSize/2, y + height/2 - arrowSize/2);
      g2d.fill (arrow);

      g2d.dispose();

    } // paintIcon

  } // DropDownIcon class

  ////////////////////////////////////////////////////////////

  private Icon createDropDownIcon (Icon icon) {

    // The issue with this method is that on high resolution displays, the
    // icon is only rendered at the lower resolution.  We would need to
    // perform some type of scaling using the affine transform for the display.

    int width = icon.getIconWidth() + extra;
    int height = icon.getIconHeight();

    var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    var gd = ge.getDefaultScreenDevice();
    var gc = gd.getDefaultConfiguration();

// var transform = gc.getDefaultTransform();
// LOGGER.fine ("scaleX = " + transform.getScaleX());
// LOGGER.fine ("scaleY = " + transform.getScaleY());

    var image = gc.createCompatibleImage (width, height, Transparency.TRANSLUCENT);
    Graphics2D g2d = image.createGraphics();
    icon.paintIcon (null, g2d, 0, 0);

    var arrow = new GeneralPath();
    arrow.moveTo (0, 0);
    arrow.lineTo (arrowSize, 0);
    arrow.lineTo (arrowSize/2, arrowSize);
    arrow.lineTo (0, 0);
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor (this.getForeground());
    g2d.translate (width - extra/2 - arrowSize/2, height/2 - arrowSize/2);
    g2d.fill (arrow);

    g2d.dispose();

    return (new ImageIcon (image));

  } // createDropDownIcon

  ////////////////////////////////////////////////////////////

  @Override
  protected void paintComponent (Graphics g) {

    super.paintComponent (g);

    var arrow = new GeneralPath();
    arrow.moveTo (0, 0);
    arrow.lineTo (arrowSize, 0);
    arrow.lineTo (arrowSize/2, arrowSize);
    arrow.lineTo (0, 0);

    var g2d = (Graphics2D) g.create();
    g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor (isEnabled() ? getForeground() : (Color) UIManager.get ("Button.disabledText"));
    var size = getSize();
    var insets = getInsets();
    var iconHeight = getIcon().getIconHeight();
    var iconWidth = getIcon().getIconWidth();
    g2d.translate ((size.width/2 + iconWidth/2 + size.width)/2 - arrowSize/2, size.height/2 - arrowSize/2);
    g2d.fill (arrow);
    g2d.dispose();

  } // paintComponent

  ////////////////////////////////////////////////////////////

  // @Override
  // public Dimension getMinimumSize() {

  //   var size = super.getMinimumSize();
  //   size.width += extra;
  //   return (size);

  // } // getMinimumSize

  ////////////////////////////////////////////////////////////

  @Override
  public Dimension getPreferredSize() {

    var size = super.getPreferredSize();
    size.width += extra;
    return (size);

  } // getPreferredSize

  ////////////////////////////////////////////////////////////

  public DropDownButton (Action action) { 

    this.menu = new JPopupMenu();
    super.addActionListener (event -> buttonClickEvent());
    this.actionListenerList = new ArrayList<>();
    setAction (action);

//    super (text, new DropDownIcon (icon));

//    super (text);
//    setIcon (createDropDownIcon (icon));

  } // DropDownButton

  ////////////////////////////////////////////////////////////

  private void buttonClickEvent () {

    menu.show (this, 0, this.getHeight());

  } // buttonClickEvent

  ////////////////////////////////////////////////////////////

  public void addItem (Action action) {

    var item = new JMenuItem (action);
    item.addActionListener (event -> itemClickedEvent (event));
    menu.add (item);

  } // addItem

  ////////////////////////////////////////////////////////////

  public void addItem (String text, Icon icon, String command) {

    var item = new JMenuItem (text, icon);
    item.setActionCommand (command);
    item.addActionListener (event -> itemClickedEvent (event));
    menu.add (item);

  } // addItem

  ////////////////////////////////////////////////////////////

  private void itemClickedEvent (ActionEvent event) {

    for (var listener : actionListenerList)
      listener.actionPerformed (event);

  } // itemClickedEvent

  ////////////////////////////////////////////////////////////

  @Override
  public void addActionListener (ActionListener listener) {

    actionListenerList.add (listener);

  }  // addActionListener

  ////////////////////////////////////////////////////////////

} // DropDownButton class
