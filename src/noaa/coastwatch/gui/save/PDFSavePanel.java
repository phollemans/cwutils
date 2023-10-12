////////////////////////////////////////////////////////////////////////
/*

     File: PDFSavePanel.java
   Author: Peter Hollemans
     Date: 2004/05/03

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
 * The <code>PDFSavePanel</code> class allows the user to select save
 * options for PDF image files.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PDFSavePanel
  extends ImageSavePanel {

  // Variables 
  // ---------

  /** The rendering option panel. */
  private RenderOptionPanel renderPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image as a PDF.
   *  
   * @param file the file to write.
   *
   * @throws IOException if an error occurred writing to the file.
   */
  public void write (File file) throws IOException {

    write (renderPanel.getLegends(), renderPanel.getInfoLegend(), false, false, null, 
      renderPanel.getColors(), "pdf", file);

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new PDF save panel.
   *
   * @param view the earth data view to save.
   * @param info the earth data information to use for the
   * legends.
   */
  public PDFSavePanel (
    EarthDataView view,
    EarthDataInfo info
  ) {

    // Initialize
    // ----------
    super (view, info);
    setLayout (new BorderLayout());

    // Create render panel
    // -------------------
    renderPanel = new RenderOptionPanel (true, false, false, isIndexable, 
      false);
    this.add (renderPanel, BorderLayout.CENTER);

  } // PDFSavePanel constructor

  ////////////////////////////////////////////////////////////

} // PDFSavePanel class

////////////////////////////////////////////////////////////////////////
