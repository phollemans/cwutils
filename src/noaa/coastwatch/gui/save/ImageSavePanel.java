////////////////////////////////////////////////////////////////////////
/*

     File: ImageSavePanel.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.save.GIFSavePanel;
import noaa.coastwatch.gui.save.GeoTIFFSavePanel;
import noaa.coastwatch.gui.save.JPEGSavePanel;
import noaa.coastwatch.gui.save.PDFSavePanel;
import noaa.coastwatch.gui.save.PNGSavePanel;
import noaa.coastwatch.gui.save.SavePanel;
import noaa.coastwatch.io.EarthImageWriter;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.IconElementFactory;
import noaa.coastwatch.util.EarthDataInfo;

/** 
 * The <code>ImageSavePanel</code> class is the abstract parent of all
 * image save panels, which allow the user to save earth data to an
 * image file format.  To save a view as an image, create a new panel
 * with the <code>create()</code> method, then call the
 * <code>write()</code> method.  Generally, the save panel is enclosed
 * in a dialog with OK and Cancel options.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class ImageSavePanel
  extends SavePanel {

  // Variables 
  // ---------

  /** The view to save as an image. */
  protected EarthDataView view;

  /** The earth data information to use for legends. */
  protected EarthDataInfo info;

  /** The indexable flag, true if we can limit the colors to an index model. */
  protected boolean isIndexable;

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new panel appropriate for the specified format. 
   *
   * @param view the earth data view to save.
   * @param info the earth data information to use for the
   * legends.
   * @param format the file format.
   */
  public static ImageSavePanel create (
    EarthDataView view,
    EarthDataInfo info,
    String format
  ) {

    // Create new panel
    // ----------------
    ImageSavePanel panel;
    if (format.equals ("png")) 
      panel = new PNGSavePanel (view, info);
    else if (format.equals ("jpg"))
      panel = new JPEGSavePanel (view, info);
    else if (format.equals ("tif"))
      panel = new GeoTIFFSavePanel (view, info);
    else if (format.equals ("pdf"))
      panel = new PDFSavePanel (view, info);
    else if (format.equals ("gif"))
      panel = new GIFSavePanel (view, info);
    else 
      throw new IllegalArgumentException ("Unsupported format: " + format);

    return (panel);

  } // create

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the image using the specified parameters.
   *
   * @param hasLegends the legends flag, true to draw color scale and
   * information legends.
   * @param isAntialiased the antialias flag, true to antialias fonts
   * and lines.
   * @param writeWorld the world file write flag, true to write a
   * world file.  The output file name extension is replaced with
   * '.wld' to create the world file name.
   * @param tiffComp the TIFF compression algorithm: 'none', 'deflate', 'pack',
   * 'lzw', or 'jpeg'.
   * @param colors the number of image colors for an indexed color
   * image, &lt;= 256 or 0 for no color limit.
   * @param format the output file format: 'png', 'gif', 'jpg', 'tif', or
   * 'pdf'.
   * @param file the file to write.
   *
   * @throws IOException if an error occurred writing the data.
   */
  protected void write (
    final boolean hasLegends,
    final boolean isAntialiased,
    final boolean writeWorld,
    final String tiffComp,
    final int colors,
    final String format,
    final File file
  ) throws IOException {

    // Create progress message dialog
    // ------------------------------
    final JDialog messageDialog = GUIServices.createMessageDialog (
      this, "Progress ...", "Writing image data ...");

    // Create task to show progress message
    // ------------------------------------
    ActionListener task = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          messageDialog.setVisible (true);
        } // actionPerformed
      };
    final Timer timer = new Timer (500, task);
    timer.setRepeats (false);

    // Write file
    // ----------
    Thread writeThread = new Thread () {
        public void run () {

          // Attempt write
          // -------------
          try {
            IconElement logoIcon = 
              IconElementFactory.create (IconElement.NOAA3D);
            EarthDataView viewClone = (EarthDataView) view.clone();
            String worldFile = (writeWorld ? 
              file.getPath().replaceFirst ("\\.[^.]*$", ".wld") : null);
            EarthImageWriter.getInstance().write (viewClone, info, false, hasLegends, 
              logoIcon, isAntialiased, file, format, worldFile, tiffComp, 
              colors);
          } // try

          // Show error message
          // ------------------
          catch (Exception e) {
            timer.stop();
            final String errorMessage = 
              "An error occurred writing the file:\n" +
              e.toString() + "\n" + 
              "Please choose another file or format and try again.";
            GUIServices.invokeAndWait (new Runnable() {
                public void run() {
                  messageDialog.dispose();
                  JOptionPane.showMessageDialog (ImageSavePanel.this, 
                    errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                } // run
              });
            return;
          } // catch

          // Stop timer and dispose message dialog
          // -------------------------------------
          timer.stop();
          SwingUtilities.invokeLater (new Runnable () {
              public void run () {
                messageDialog.dispose();
              } // run
            });

        } // run
      };
    writeThread.start();
    timer.start();

  } // write

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new image save panel.
   *
   * @param view the earth data view to save.
   * @param info the earth data information to use for the
   * legends.
   */
  protected ImageSavePanel (
    EarthDataView view,
    EarthDataInfo info
  ) {

    this.view = view;
    this.info = info;
    isIndexable = (view instanceof ColorEnhancement);
    
  } // ImageSavePanel constructor

  ////////////////////////////////////////////////////////////

} // ImageSavePanel class

////////////////////////////////////////////////////////////////////////
