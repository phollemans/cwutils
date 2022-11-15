////////////////////////////////////////////////////////////////////////
/*

     File: VisualOverlay.java
   Author: Peter Hollemans
     Date: 2004/03/01

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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;
import noaa.coastwatch.gui.visual.GenericOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooserFactory;
import noaa.coastwatch.gui.visual.VisualObject;
import noaa.coastwatch.gui.visual.VisualObjectFactory;
import noaa.coastwatch.gui.visual.VisualServices;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.MultilayerBitmaskOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.MultiPointFeatureOverlay;

import java.util.logging.Logger;


/**
 * The <code>VisualOverlay</code> class represents an
 * <code>EarthDataOverlay</code> object as a panel with modification
 * components.
 *
 * @see noaa.coastwatch.render.EarthDataOverlay
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualOverlay 
  extends AbstractVisualObject {

  private static final Logger LOGGER = Logger.getLogger (VisualOverlay.class.getName());

  // Variables
  // ---------

  /** The panel holding the control components. */
  private JPanel panel;

  /** The overlay object. */
  private EarthDataOverlay overlay;

  /** The current chooser panel or null if none is showing. */
  private OverlayPropertyChooser chooserPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new visual overlay object using the specified
   * overlay. 
   */
  public VisualOverlay (
    EarthDataOverlay overlay
  ) {                     

    // Create panel
    // ------------
    panel = new JPanel (new FlowLayout (FlowLayout.LEFT, 4, 0));

    // Set overlay
    // -----------
    setOverlay (overlay);

  } // VisualOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Adds a visual property component to the panel.  The new property
   * component is created using the <code>VisualObjectFactory</code>,
   * so it already automatically modifies the overlay itself.
   * Optionally, another property change listener is added so that
   * this visual overlay object itself fires property changes when one
   * of the overlay properties is changed.
   *
   * @param target the target to create a visual property.
   * @param property the property to add a component.
   * @param needsListener the listener flag, true if a listener should
   * be attached to the property component that simply fires a
   * property change event from this visual overlay object.
   */
  private void addVisualPropertyComponent (
    Object target,
    String property,
    boolean needsListener
  ) {

    VisualObject visual = VisualObjectFactory.create (target, property);
    if (needsListener) {
      visual.addPropertyChangeListener (new PropertyChangeListener () {
          public void propertyChange (PropertyChangeEvent event) {
            firePropertyChange();
          } // propertyChange
        });
    } // if
    Component component = visual.getComponent();
    if (component instanceof JComponent) {
      ((JComponent) component).setToolTipText (
        GenericOverlayPropertyChooser.getLabel (property));
    } // if
    panel.add (component);

  } // addVisualPropertyComponent

  ////////////////////////////////////////////////////////////

  /** Gets the panel used to represent the overlay. */
  public Component getComponent () { return (panel); }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay value. */
  public Object getValue () { return (overlay); }

  ////////////////////////////////////////////////////////////
  
  /** Returns true as this visual object has a chooser. */
  public boolean hasChooser () { return (true); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the active overlay from the currently displayed
   * chooser.
   *
   * @return true if the operation was successful, or false if
   * not.
   */
  private boolean setOverlayFromChooser () {

    try {
      var newOverlay = (EarthDataOverlay) chooserPanel.getOverlay().clone();
      setOverlay (newOverlay);
      return (true);
    } // try
    catch (IllegalStateException e) {
      String errorMessage = 
        "An error occurred checking the input:\n" +
        e.getMessage() + "\n" + 
        "Please correct the problem and try again.";
      JOptionPane.showMessageDialog (
        SwingUtilities.getWindowAncestor (chooserPanel), errorMessage,
        "Error", JOptionPane.ERROR_MESSAGE);
      return (false);
    } // catch

  } // setOverlayFromChooser

  ////////////////////////////////////////////////////////////

  /** Shows the overlay property chooser. */
  public void showChooser () {

    // Check if we are already showing a chooser
    // -----------------------------------------
    if (chooserPanel != null) return;

    // Create chooser panel
    // --------------------
    chooserPanel = OverlayPropertyChooserFactory.create (
      (EarthDataOverlay) overlay.clone());

    // Create auto-apply checkbox
    // --------------------------
    final JCheckBox autoApplyCheckBox = new JCheckBox ("Auto-apply");
    autoApplyCheckBox.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          if (autoApplyCheckBox.isSelected()) {
            if (!setOverlayFromChooser()) 
              autoApplyCheckBox.setSelected (false);
          } // if
        } // actionPerformed
      });
    chooserPanel.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          String property = event.getPropertyName();
          if (!property.equals (OverlayPropertyChooser.OVERLAY_PROPERTY)) 
            return;
          if (autoApplyCheckBox.isSelected()) setOverlayFromChooser();
        } // propertyChange
      });
    
    // Create dialog
    // -------------
    final JDialog[] dialog = new JDialog[1];
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
        public void run () { 
          if (setOverlayFromChooser()) dialog[0].dispose();
        } // run
      });
    Action cancelAction = GUIServices.createAction ("Cancel", null);
    dialog[0] = GUIServices.createDialog (
      panel, "Select the overlay properties", false, chooserPanel,
      new Component[] {autoApplyCheckBox, Box.createHorizontalGlue()}, 
      new Action[] {okAction, cancelAction}, new boolean[] {false, true},
      true);

    // Set chooser flag on window closed
    // ---------------------------------
    dialog[0].addWindowListener (new WindowAdapter () {
        public void windowClosed (WindowEvent e) { 
          chooserPanel = null;
        } // windowClosed
      });

    // Show dialog
    // -----------
    dialog[0].setVisible (true);

  } // showChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Refreshes the component display to show the contents of the
   * current overlay.
   */
  public void refreshComponent () {

    setOverlay (overlay);
                                
  } // refreshComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the overlay displayed in the panel.  A property change
   * event is also fired. 
   */
  private void setOverlay (
    EarthDataOverlay newOverlay
  ) {

    // Set overlay
    // -----------
    EarthDataOverlay oldOverlay = overlay;
    overlay = newOverlay;

    LOGGER.fine ("Old overlay is " + oldOverlay);
    LOGGER.fine ("New overlay is " + newOverlay);

    // Clear panel contents
    // --------------------
    panel.removeAll();

    // Add property components
    // -----------------------
    addVisualPropertyComponent (overlay, "visible", true);
    addVisualPropertyComponent (overlay, "name", false);
    if (!(overlay instanceof MultilayerBitmaskOverlay) && !(overlay instanceof MultiPointFeatureOverlay))
      addVisualPropertyComponent (overlay, "color", true);
    if (VisualServices.hasProperty (overlay, "stroke") && !(overlay instanceof PointFeatureOverlay))
      addVisualPropertyComponent (overlay, "stroke", true);
    if (overlay instanceof PointFeatureOverlay)
      addVisualPropertyComponent (((PointFeatureOverlay) overlay).getSymbol(), "plotSymbol", true);
    if (VisualServices.hasProperty (overlay, "fillColor"))
      addVisualPropertyComponent (overlay, "fillColor", true);

    // Perform layout if needed
    // ------------------------
    if (panel.isShowing()) { panel.revalidate(); }

    // Fire property change event
    // --------------------------
    /** 
     * We have to fire two property change events here because the new
     * and old overlays may have the same object reference but contain
     * different values.  If they have the same object reference, the
     * property change event is not propagated to the listener by the
     * Swing change support object.  Another way to do this would be
     * to give overlays a more sophisticated equals() method.
     */
    firePropertyChange (oldOverlay, null);
    firePropertyChange (null, newOverlay);

  } // setOverlay

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    panel.add (new VisualOverlay (new CoastOverlay (
      Color.WHITE)).getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualOverlay class

////////////////////////////////////////////////////////////////////////
