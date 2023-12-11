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
import java.awt.Cursor;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.Icon;

import java.io.File;
import java.io.IOException;

import java.util.List;

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

  @Override
  public boolean canImport (TransferHandler.TransferSupport info) {

    boolean importable = false;

    if (info.isDrop() && info.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
      importable = true;
    } // if

    return (importable);

  } // canImport

  ////////////////////////////////////////////////////////////

  @Override
  public boolean importData (TransferHandler.TransferSupport info) {

    boolean imported = false;

    if (canImport (info)) {
      try {
        fileList = (List) info.getTransferable().getTransferData (DataFlavor.javaFileListFlavor);
        imported = true;
      }  // try
      catch (UnsupportedFlavorException e) { }
      catch (IOException e) { }
    } // if

    if (imported) runnable.run();
    return (imported);

  } // importData

  ////////////////////////////////////////////////////////////

} // FileTransferHandler class

////////////////////////////////////////////////////////////////////////
