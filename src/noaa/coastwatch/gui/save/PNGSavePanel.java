////////////////////////////////////////////////////////////////////////
/*
     FILE: PNGSavePanel.java
  PURPOSE: Allows the user to choose PNG save options.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/03
  CHANGES: 2006/11/09, PFH, changed write() to write(File)

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.*;
import javax.swing.*;
import java.io.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>PNGSavePanel</code> class allows the user to select save
 * options for PNG image files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PNGSavePanel
  extends ImageSavePanel {

  // Variables 
  // ---------

  /** The rendering option panel. */
  private RenderOptionPanel renderPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image as a PNG.
   *
   * @param file the file to write.
   *  
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    write (renderPanel.getLegends(), renderPanel.getSmooth(), 
      renderPanel.getWorld(), null, renderPanel.getColors(), "png", file);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new PNG save panel.
   *
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
   * legends.
   */
  public PNGSavePanel (
    EarthDataView view,
    EarthDataInfo info
  ) {

    // Initialize
    // ----------
    super (view, info);
    setLayout (new BorderLayout());

    // Create render panel
    // -------------------
    renderPanel = new RenderOptionPanel (true, true, true, isIndexable, false);
    this.add (renderPanel, BorderLayout.CENTER);

  } // PNGSavePanel constructor

  ////////////////////////////////////////////////////////////

} // PNGSavePanel class

////////////////////////////////////////////////////////////////////////
