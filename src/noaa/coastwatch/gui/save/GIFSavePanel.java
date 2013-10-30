////////////////////////////////////////////////////////////////////////
/*
     FILE: GIFSavePanel.java
  PURPOSE: Allows the user to choose GIF save options.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/28
  CHANGES: 2006/11/09, PFH, changed write() to write(File)
           2006/11/14, PFH, added extra rendering options

  CoastWatch Software Library and Utilities
  Copyright 1998-2005 USDOC/NOAA/NESDIS CoastWatch

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
 * The <code>GIFSavePanel</code> class allows the user to select save
 * options for GIF image files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class GIFSavePanel
  extends ImageSavePanel {

  // Variables 
  // ---------

  /** The rendering option panel. */
  private RenderOptionPanel renderPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image as a GIF.
   *  
   * @param file the file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    write (renderPanel.getLegends(), renderPanel.getSmooth(), 
      renderPanel.getWorld(), null, renderPanel.getColors(), "gif", file);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new GIF save panel.
   *
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
   * legends.
   */
  public GIFSavePanel (
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

  } // GIFSavePanel constructor

  ////////////////////////////////////////////////////////////

} // GIFSavePanel class

////////////////////////////////////////////////////////////////////////
