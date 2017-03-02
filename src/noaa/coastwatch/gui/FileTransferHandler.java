////////////////////////////////////////////////////////////////////////
/*

     File: FileTransferHandler.java
   Author: Peter Hollemans
     Date: 2006/03/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
// --------
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * The <code>FileTransferHandler</code> class is used with the
 * <code>JComponent.setTransferHandler()</code> method to handle one
 * or more <code>java.io.File</code> objects during a drag and drop
 * operation.  The user must specify a <code>Runnable</code> to call
 * when drag and drop of file information occurs.  If the drag and
 * drop operation is not for file information, then no action is
 * performed.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class FileTransferHandler
  extends TransferHandler {

  // Variables
  // ---------

  /** The list of files from the last drop operation. */
  private List fileList;

  /** The runnable object for drop operations. */
  private Runnable runnable;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new handler that calls the specified runnable.
   *
   * @param runnable the runnable object whose <code>run()</code> will
   * be called when a drop operation occurs.
   */
  public FileTransferHandler (
    Runnable runnable
  ) {

    this.runnable = runnable;
    fileList = null;

  } // TransferHandler constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the latest list of files from a drop operation.
   * 
   * @return the list of files, or null if no drop operation has
   * occurred.
   */
  public List getFileList () {

    return (fileList);

  } // getFileList

  ////////////////////////////////////////////////////////////

  /**
   * Gets the latest file from a drop operation.  If a list of files
   * is available, only the first file is returned.
   * 
   * @return the file, or null if no drop operation has occurred.
   */
  public File getFile () {

    if (fileList == null || fileList.size() == 0) return (null);
    else return ((File) fileList.get (0));

  } // getFile

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the data flavour contains a
   * <code>DataFlavour.javaFileListFlovor</code> object.  Users should
   * not need to call this method -- it is called by AWT when a drag
   * event occurs over the component.  See the Java API documentation
   * for <code>javax.swing.TransferHandler</code> for more info.
   */
  public boolean canImport (
    JComponent comp, 
    DataFlavor[] flavors
  ) {

    // Find file list flavor
    // ---------------------
    for (int i = 0; i < flavors.length; i++) {
      if (flavors[i].equals (DataFlavor.javaFileListFlavor))
        return (true);
    } // for

    return (false);

  } // canImport

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the dropped data is imported successfully.  Users
   * should not need to call this method -- it is called by AWT when a
   * drop event occurs on the component.  See the Java API
   * documentation for <code>javax.swing.TransferHandler</code> for
   * more info.
   */
  public boolean importData (
    JComponent comp, 
    Transferable t
  ) {
          
    // Get file list
    // -------------
    try {
      fileList = (List) t.getTransferData (DataFlavor.javaFileListFlavor);
    } // try
    catch (UnsupportedFlavorException e1) { return (false); }
    catch (IOException e2) { return (false); }

    // Call runnable
    // -------------
    runnable.run();

    return (true);

  } // importData

  ////////////////////////////////////////////////////////////

  /** 
   * Returns an icon that represents the file transfer.  Users should
   * not need to call this method -- it is called by AWT when a drop
   * event occurs on the component.  See the Java API documentation
   * for <code>javax.swing.TransferHandler</code> for more info.
   */

  /*
   * TODO: Currently, we can't find a JVM that supports changing the
   * cursor for a drop operation using the return value from this
   * method, so for now we don't override it.

  public Icon getVisualRepresentation (Transferable t) {

    return (GUIServices.getIcon ("file.open"));

  } // getVisualRepresentation

   */

  ////////////////////////////////////////////////////////////

} // FileTransferHandler class

////////////////////////////////////////////////////////////////////////
