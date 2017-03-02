////////////////////////////////////////////////////////////////////////
/*

     File: RenderablePanel.java
   Author: Peter Hollemans
     Date: 2002/11/28

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import noaa.coastwatch.render.Renderable;

/**
 * The renderable panel displays the output from a renderable object.
 *
 * @see noaa.coastwatch.render.Renderable
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class RenderablePanel
  extends JPanel {

  // Variables
  // ---------
  /** The renderable to display. */
  protected Renderable renderable;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new renderable panel using the specified renderable.
   *
   * @param renderable the renderable to display.  The renderable is
   * used to create the graphics to display in the panel.
   */
  public RenderablePanel (
    Renderable renderable
  ) {

    // Initialize
    // ----------
    this.renderable = renderable;

  } // RenderablePanel constructor

  ////////////////////////////////////////////////////////////

  public void paintComponent (
    Graphics g
  ) {

    // Check showing
    // -------------
    if (!isShowing ()) return;

    // Call super
    // ----------
    super.paintComponent (g);

    // Render view
    // -----------
    Cursor cursor = getCursor();
    setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    renderable.render ((Graphics2D) g);
    setCursor (cursor);

  } // paintComponent

  ////////////////////////////////////////////////////////////

} // RenderablePanel class

////////////////////////////////////////////////////////////////////////
