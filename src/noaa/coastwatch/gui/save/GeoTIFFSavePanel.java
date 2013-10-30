////////////////////////////////////////////////////////////////////////
/*
     FILE: GeoTIFFSavePanel.java
  PURPOSE: Allows the user to choose GeoTIFF save options.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/03
  CHANGES: 2006/11/09, PFH, changed write() to write(File)
           2006/11/14, PFH, added extra rendering options

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
 * The <code>GeoTIFFSavePanel</code> class allows the user to select save
 * options for GeoTIFF image files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class GeoTIFFSavePanel
  extends ImageSavePanel {

  // Variables 
  // ---------

  /** The rendering option panel. */
  private RenderOptionPanel renderPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image as a GeoTIFF.
   *  
   * @param file the file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    write (false, renderPanel.getSmooth(), false, renderPanel.getCompress(),
      renderPanel.getColors(), "tif", file);

  } // write

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new GeoTIFF save panel.
   *
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
   * legends.
   */
  public GeoTIFFSavePanel (
    EarthDataView view,
    EarthDataInfo info
  ) {

    // Initialize
    // ----------
    super (view, info);
    setLayout (new BorderLayout());

    // Create render panel
    // -------------------
    renderPanel = new RenderOptionPanel (false, true, false, isIndexable, 
      true);
    this.add (renderPanel, BorderLayout.CENTER);

  } // GeoTIFFSavePanel constructor

  ////////////////////////////////////////////////////////////

} // GeoTIFFSavePanel class

////////////////////////////////////////////////////////////////////////
