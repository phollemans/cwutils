////////////////////////////////////////////////////////////////////////
/*
     FILE: ImageSavePanel.java
  PURPOSE: Allows the user to choose save options for rendered images.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/03
  CHANGES: 2005/01/30, PFH, modified write() method to catch more exceptions
           2005/02/14, PFH, modified to handle different logos
           2005/03/26, PFH, modified to clone view before writing with legends
           2005/03/28, PFH, added GIF saving
           2005/05/30, PFH, modified for world files
           2006/07/07, PFH, modified for image colors
           2006/11/14, PFH, updated write() to handle more options
           2006/11/16, PFH, added isIndexable

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.gui.*;

/** 
 * The <code>ImageSavePanel</code> class is the abstract parent of all
 * image save panels, which allow the user to save Earth data to an
 * image file format.  To save a view as an image, create a new panel
 * with the <code>create()</code> method, then call the
 * <code>write()</code> method.  Generally, the save panel is enclosed
 * in a dialog with OK and Cancel options.
 */
public abstract class ImageSavePanel
  extends SavePanel {

  // Variables 
  // ---------

  /** The view to save as an image. */
  protected EarthDataView view;

  /** The Earth data information to use for legends. */
  protected EarthDataInfo info;

  /** The indexable flag, true if we can limit the colors to an index model. */
  protected boolean isIndexable;

  ////////////////////////////////////////////////////////////

  /* 
   * Creates a new panel appropriate for the specified format. 
   *
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
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
   * @param tiffComp the TIFF compression algorithm: 'none',
   * 'deflate', or 'pack'.
   * @param colors the number of image colors for an indexed color
   * image, <= 256 or 0 for no color limit.
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
            EarthImageWriter.write (viewClone, info, false, hasLegends, 
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
   * @param view the Earth data view to save.
   * @param info the Earth data information to use for the
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
