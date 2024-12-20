////////////////////////////////////////////////////////////////////////
/*

     File: FullScreenWindow.java
   Author: Peter Hollemans
     Date: 2007/07/20

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

import noaa.coastwatch.gui.FullScreenToolBar;
import noaa.coastwatch.gui.GUIServices;

import java.util.logging.Logger;

/**
 * <p>The <code>FullScreenWindow</code> class display a full screen
 * component with optional tool bar.  The displayed component is taken
 * from its parent component (if it exists) and displayed in full
 * screen mode by the {@link #start} method.  The caller must restore the 
 * component to its parent after calling the {@link #stop} method.</p>
 *
 * <p>Java support for full screen mode is not required for a
 * component to be displayed full screen, but some performance
 * degradation can occur if no Java support exists.  The {@link
 * #isFullScreenSupported} method checks for Java full screen
 * mode support.  Applications can safely ignore the return value
 * of this method if needed.</p>
 *
 * @see FullScreenToolBar
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class FullScreenWindow {

  private static final Logger LOGGER = Logger.getLogger (FullScreenWindow.class.getName());

  // Constants
  // ---------

  /** 
   * The time delay before the toolbar disappears after mouse movement
   * (milliseconds).
   */
  private static final int DISAPPEAR_DELAY = 2000;

  /** The time step delay for toolbar disappear fade (milliseconds). */
  private static final int FADE_DELAY = 50;

  // Variables
  // ---------

  /** The graphics device for the component. */
  private GraphicsDevice device;

  /** The rectangle bounds for the full screen window. */
  private Rectangle bounds;

  /** The component to use for the window. */
  private Component component;

  /** The frame to use as a full screen window. */
  private JFrame frame;

  /** The toolbar to use for the window (possibly null). */
  private FullScreenToolBar toolbar;

  /** The parent container to use for restoring the full screen component. */
  private Container parent;

  /** The disappearance timer for the toolbar. */
  private Timer toolbarTimer;

  /** The fade timer for the toolbar. */
  private Timer fadeTimer;

  /** The toolbar active flag, true if the toolbar should not be hidden. */
  private boolean isToolbarActive;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new window with the specified component and toolbar.
   *
   * @param component the component to use as the full screen
   * contents.
   * @param toolbar the toolbar to use for the component, or null for
   * no toolbar.
   */
  public FullScreenWindow (
    Component component,
    FullScreenToolBar toolbar
  ) {

    // Initialize
    // ----------
    this.component = component;
    this.toolbar = toolbar;

    // Create frame
    // ------------
    GraphicsConfiguration config = component.getGraphicsConfiguration();
    this.device = config.getDevice();
    frame = new JFrame (config);
    frame.setUndecorated (true);
    frame.setResizable (false);

    // Get full screen window bounds
    // -----------------------------
    bounds = config.getBounds();
    bounds.x = bounds.y = 0;  // These could be non-zero in a virtual device

    if (toolbar != null) {
      
      // Create toolbar timer
      // --------------------
      toolbarTimer = new Timer (DISAPPEAR_DELAY, new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            if (!isToolbarActive) fadeTimer.restart();
          } // actionPerformed
        });
      toolbarTimer.setRepeats (false);

      // Create fade timer
      // -----------------
      fadeTimer = new Timer (FADE_DELAY, new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            float alpha = FullScreenWindow.this.toolbar.getAlpha();
            if (alpha < (FADE_DELAY/1000f)) {
              fadeTimer.stop();
              FullScreenWindow.this.toolbar.setVisible (false);
              FullScreenWindow.this.toolbar.setAlpha (1f);
            } // if
            else {
              FullScreenWindow.this.toolbar.setAlpha (alpha - 
                FADE_DELAY/1000f); 
              FullScreenWindow.this.toolbar.repaint();
            } // else
          } // actionPerformed
        });

      // Add property listener to toolbar
      // --------------------------------
      toolbar.addPropertyChangeListener (
        FullScreenToolBar.MOUSE_ACTIVITY_PROPERTY,
        new PropertyChangeListener () {
          public void propertyChange (PropertyChangeEvent event) {
            isToolbarActive = (Boolean) event.getNewValue();
          } // propertyChange
        });

      // Add mouse listener to component and toolbar
      // -------------------------------------------
      component.addMouseMotionListener (new MouseInputAdapter() {
          public void mouseMoved (MouseEvent e) { showToolbar(); }
          public void mouseDragged (MouseEvent e) { mouseMoved (e); }
        });
      toolbar.addMouseMotionListener (new MouseInputAdapter() {
          public void mouseMoved (MouseEvent e) { showToolbar(); }
          public void mouseDragged (MouseEvent e) { mouseMoved (e); }
        });

    } // if

  } // FullScreenWindow constructor

  ////////////////////////////////////////////////////////////

  /** Shows the toolbar and resets the toolbar timer. */
  private void showToolbar () {

    if (isFullScreen()) {
      if (fadeTimer.isRunning()) {
        fadeTimer.stop();
        if (toolbar.isVisible() && toolbar.getAlpha() != 1f) { 
          toolbar.setAlpha (1f); 
          toolbar.repaint(); 
        } // if
      } // if
      else if (!toolbar.isVisible()) {
        toolbar.setVisible (true);
      } // else
      toolbarTimer.restart();
    }// if

  } // showToolbar

  ////////////////////////////////////////////////////////////

  /** 
   * Starts full screen mode with this window.
   *
   * @throws IllegalStateException if the graphics device is already in
   * full screen mode.
   */
  public void start () {

    LOGGER.fine ("Entering full screen mode");

    // Check for full screen mode
    // --------------------------
    if (isFullScreen())
      throw new IllegalStateException ("Already in full screen mode");

    // Save component parent
    // ---------------------
    parent = (Container) component.getParent();

    // Add component to frame
    // ----------------------
    JLayeredPane layeredPane = new JLayeredPane();
    layeredPane.add (component, Integer.valueOf (0));
    component.setBounds (bounds);
    layeredPane.setOpaque (true);
    frame.setContentPane (layeredPane);

    // Add toolbar to frame
    // --------------------
    if (toolbar != null) {
      layeredPane.add (toolbar, Integer.valueOf (1));
      Dimension size = toolbar.getPreferredSize();
      toolbar.setBounds (
        bounds.width/2 - size.width/2, 
        bounds.height - size.height - size.height/2,
        size.width, size.height);
      toolbar.setBackground (new Color (0, 0, 0, 0));
      toolbar.setOpaque (false);
      toolbar.setVisible (false);
    } // if
    
    // Enter full screen mode
    // ----------------------

    /**
     * This is a workaround to hide the top level window from the
     * Windows full screen mode bug: 
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435887
     */
    if (parent != null && GUIServices.IS_WIN) {
      Window topWindow = SwingUtilities.getWindowAncestor (parent);
      topWindow.setVisible (false);
    } // if
    
    device.setFullScreenWindow (frame);
    frame.setBounds (bounds);
    frame.setVisible (true);
    frame.validate();

  } // start

  ////////////////////////////////////////////////////////////

  /** 
   * Ends full screen mode with this window.
   *
   * @throws IllegalStateException if the graphics device is not in
   * full screen mode.
   */
  public void stop () {

    LOGGER.fine ("Exiting full screen mode");

    // Check for full screen mode
    // --------------------------
    if (!isFullScreen())
      throw new IllegalStateException ("Not in full screen mode");

    // Stop the toolbar timer
    // ----------------------
    if (toolbar != null) toolbarTimer.stop();

    // Stop full screen mode
    // ---------------------

    /**
     * This is a workaround to show the top level window after hiding
     * it from the Windows full screen mode bug:
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435887
     */
    if (parent != null && GUIServices.IS_WIN) {
      Window topWindow = SwingUtilities.getWindowAncestor (parent);
      topWindow.setVisible (true);
    } // if

    device.setFullScreenWindow (null);
    frame.setVisible (false);

  } // stop

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if this window is currently displayed full screen.
   *
   * @return the fullscreen mode flag, true if displayed fullscreen or false 
   * if not.
   */
  public boolean isFullScreen () {

    Window window = device.getFullScreenWindow();
    return (window == frame);

  } // isFullScreen

  ////////////////////////////////////////////////////////////

  /** 
   * Determines if full screen mode is supported on the default
   * graphics device.
   *
   * @return true if full screen mode is supported or false if not.
   */
  @Deprecated
  public static boolean isFullScreenSupported () {

//    return (device.isFullScreenSupported());
    return (true);

  } // isFullScreenSupported
  
  ////////////////////////////////////////////////////////////

} // FullScreenWindow class

////////////////////////////////////////////////////////////////////////
