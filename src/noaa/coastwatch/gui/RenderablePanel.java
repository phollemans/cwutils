////////////////////////////////////////////////////////////////////////
/*
     FILE: RenderablePanel.java
  PURPOSE: A class to display a renderable.
   AUTHOR: Peter Hollemans
     DATE: 2002/11/28
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

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

/**
 * The renderable panel displays the output from a renderable object.
 *
 * @see noaa.coastwatch.render.Renderable
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
