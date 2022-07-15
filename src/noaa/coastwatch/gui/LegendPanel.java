////////////////////////////////////////////////////////////////////////
/*

     File: LegendPanel.java
   Author: Peter Hollemans
     Date: 2006/04/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import noaa.coastwatch.render.Legend;

/**
 * The <code>LegendPanel</code> class displays a {@link
 * noaa.coastwatch.render.Legend} graphic in a panel.  The size of the
 * legend may be scaled depending on the panel size.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class LegendPanel 
  extends JPanel {

  // Variables
  // ---------

  /** The legend to display. */
  private Legend legend;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new panel for the specified legend.
   *
   * @param legend the legend to display.
   */
  public LegendPanel (
    Legend legend
  ) {

    this.legend = legend;

  } // LegendPanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Check if we are showing
    // -----------------------
    if (!isShowing()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Render legend
    // -------------
    if (legend != null) {

      // Get legend size
      // ---------------
      Dimension preferred = getSize();
      legend.setPreferredSize (preferred);
      Dimension actual = legend.getSize ((Graphics2D) g);

      // Dim disabled legend
      // -------------------
      if (!isEnabled()) {
        ((Graphics2D) g).setComposite (AlphaComposite.getInstance (
          AlphaComposite.SRC_OVER, 0.4f));
      } // if

      // Render legend
      // -------------
      Graphics2D g2d = (Graphics2D) g;
      Object textHint = g2d.getRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING);
      g2d.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      legend.setForeground (getForeground());
      legend.setBackground (null);
      legend.render (g2d, preferred.width/2 - actual.width/2,
        preferred.height/2 - actual.height/2);
      g2d.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, textHint);

    } // if

  } // paintComponent

  ////////////////////////////////////////////////////////////

  /** Sets the legned for this panel. */
  public void setLegend (Legend legend) { this.legend = legend; }

  ////////////////////////////////////////////////////////////

} // LegendPanel class

////////////////////////////////////////////////////////////////////////

