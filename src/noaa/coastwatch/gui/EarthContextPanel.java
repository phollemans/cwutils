////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthContextPanel.java
  PURPOSE: To display an Earth context element.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/15
  CHANGES: 2005/05/27, PFH, modified to use getFastMode() to save/restore mode

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import javax.swing.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;

/**
 * The Earth context panel displays an Earth context element.
 *
 * @see EarthContextElement
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class EarthContextPanel
  extends JPanel {

  // Variables
  // ---------
  /** The context element to display. */
  protected EarthContextElement element;

  /** The panel size at last painting time. */
  private Dimension panelSize;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new Earth context panel showing the entire globe.
   */
  public EarthContextPanel () {

    this (new EarthContextElement (new EarthLocation (0, 0)));

  } // EarthContextPanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new Earth context panel.
   *
   * @param element the Earth context element for display.
   */
  public EarthContextPanel (
    EarthContextElement element
  ) {

    // Initialize
    // ----------
    this.element = element;

  } // EarthContextPanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Check showing
    // -------------
    if (!isShowing()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Set element size and position
    // -----------------------------
    Dimension size = getSize();
    if (panelSize == null || !panelSize.equals (size)) {
      element.setPreferredSize (size);
      Rectangle bounds = element.getBounds (null);
      element.setPosition (new Point (size.width/2 - bounds.width/2, 
        size.height/2 - bounds.height/2));
      panelSize = size;
    } // if

    // Render element
    // --------------
    boolean savedMode = LineFeature.getFastMode();
    LineFeature.setFastMode (true);
    element.render ((Graphics2D) g, getForeground(), getBackground());
    LineFeature.setFastMode (savedMode);

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // EarthContextPanel class

////////////////////////////////////////////////////////////////////////
