////////////////////////////////////////////////////////////////////////
/*
     FILE: JPEGSavePanel.java
  PURPOSE: Allows the user to choose JPEG save options.
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
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import noaa.coastwatch.gui.save.ImageSavePanel;
import noaa.coastwatch.gui.save.RenderOptionPanel;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>JPEGSavePanel</code> class allows the user to select save
 * options for JPEG image files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class JPEGSavePanel
  extends ImageSavePanel {

  // Variables 
  // ---------

  /** The rendering option panel. */
  private RenderOptionPanel renderPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image as a JPEG.
   *  
   * @param file the file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    write (renderPanel.getLegends(), renderPanel.getSmooth(), 
      renderPanel.getWorld(), null, 0, "jpg", file);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new JPEG save panel.
   *
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
   * legends.
   */
  public JPEGSavePanel (
    EarthDataView view,
    EarthDataInfo info
  ) {

    // Initialize
    // ----------
    super (view, info);
    setLayout (new BorderLayout());

    // Create render panel
    // -------------------
    renderPanel = new RenderOptionPanel (true, true, true, false, false);
    this.add (renderPanel, BorderLayout.CENTER);

  } // JPEGSavePanel constructor

  ////////////////////////////////////////////////////////////

} // JPEGSavePanel class

////////////////////////////////////////////////////////////////////////
