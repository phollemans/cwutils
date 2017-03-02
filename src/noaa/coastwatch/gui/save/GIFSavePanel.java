////////////////////////////////////////////////////////////////////////
/*

     File: GIFSavePanel.java
   Author: Peter Hollemans
     Date: 2004/03/28

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
   * @param view the earth data view to save.
   * @param info the earth data information to use for the
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
