////////////////////////////////////////////////////////////////////////
/*
     FILE: FullScreenWindow.java
  PURPOSE: Displays a full screen panel and menu.
   AUTHOR: Peter Hollemans
     DATE: 2007/07/20
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
import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * The <code>FullScreenWindow</code> class display a full screen
 * component with optional tool bar.  The displayed component is taken
 * from its parent component (if it exists) and displayed in full
 * screen mode by the {@link #start} method.  The component is
 * restored to its parent by the {@link #stop} method.  If a parent
 * component exists, it should have a <code>BorderLayout</code> layout
 * manager for the component to be restored correctly.<p>
 *
 * Java support for full screen mode is not required for a
 * component to be displayed full screen, but some performance
 * degradation can occur if no Java support exists.  The {@link
 * #isFullScreenSupported} method checks for Java full screen
 * mode support.  Applications can safely ignore the return value
 * of this method if needed.
 *
 * @see FullScreenToolBar
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class FullScreenWindow {

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

  /** The default graphics device. */
  private static GraphicsDevice device = 
    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

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

  /** The constraint object for the component in its parent. */
  private Object constraints;

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
   *
   * @throws UnsupportedOperationException if full screen mode is not
   * supported.
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
    GraphicsConfiguration config = device.getDefaultConfiguration();
    frame = new JFrame (config);
    frame.setUndecorated (true);
    frame.setResizable (false);

    // Get full screen window bounds
    // -----------------------------
    bounds = config.getBounds();

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

    // Check for full screen mode
    // --------------------------
    Window window = device.getFullScreenWindow();
    if (window != null)
      throw new IllegalStateException ("Already in full screen mode");

    // Save component parent and layout constraints
    // --------------------------------------------
    parent = (Container) component.getParent();
    if (parent != null) {
      constraints = 
        ((BorderLayout) parent.getLayout()).getConstraints (component);
    } // if
    else
      constraints = null;

    // Add component to frame
    // ----------------------
    JLayeredPane layeredPane = new JLayeredPane();
    layeredPane.add (component, new Integer (0));
    component.setBounds (bounds);
    layeredPane.setOpaque (true);
    frame.setContentPane (layeredPane);

    // Add toolbar to frame
    // --------------------
    if (toolbar != null) {
      layeredPane.add (toolbar, new Integer (1));
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

    // Check for full screen mode
    // --------------------------
    if (!isFullScreen())
      throw new IllegalStateException ("Not in full screen mode");

    // Stop the toolbar timer
    // ----------------------
    toolbarTimer.stop();

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

    // Restore component to parent
    // ---------------------------
    if (parent != null) {
      parent.add (component, constraints);
      parent.validate();
    } // if

  } // stop

  ////////////////////////////////////////////////////////////

  /** Determines if this window is currently displayed full screen. */
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
  public static boolean isFullScreenSupported () {

    return (device.isFullScreenSupported());

  } // isFullScreenSupported
  
  ////////////////////////////////////////////////////////////

} // FullScreenWindow class

////////////////////////////////////////////////////////////////////////
