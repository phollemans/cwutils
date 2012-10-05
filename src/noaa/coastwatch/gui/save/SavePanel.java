////////////////////////////////////////////////////////////////////////
/*
     FILE: SavePanel.java
  PURPOSE: Defines the abstract save panel methods.
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
import java.io.*;
import javax.swing.*;

/** 
 * The <code>SavePanel</code> class is the abstract parent of all save
 * panels.  The panel has a <code>write()</code> method so that it may
 * be used to save data.  Generally, a save panel is supposed to be
 * used as a mechanism for setting various options before saving.
 */
public abstract class SavePanel
  extends JPanel {

  ////////////////////////////////////////////////////////////

  /** 
   * Saves data to a file.
   *
   * @param file the file to write.
   * 
   * @throws IOException if an error occurred writing to the file.
   */
  public abstract void write (File file) throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Checks the panel entries.  This method performs no operation
   * unless overridden in the child class.  Child classes should make
   * an effort to throw an exception here if a subsequent call to the
   * <code>write()</code> method would fail, and the problem can be
   * detected before writing the file.
   *
   * @throws Exception if the panel contents have an error.
   */
  public void check () { }

  ////////////////////////////////////////////////////////////

} // SavePanel class

////////////////////////////////////////////////////////////////////////
