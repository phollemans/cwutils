////////////////////////////////////////////////////////////////////////
/*

     File: SavePanel.java
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
import java.io.File;
import java.io.IOException;
import javax.swing.JPanel;

/** 
 * The <code>SavePanel</code> class is the abstract parent of all save
 * panels.  The panel has a <code>write()</code> method so that it may
 * be used to save data.  Generally, a save panel is supposed to be
 * used as a mechanism for setting various options before saving.
 *
 * @author Peter Hollemans
 * @since 3.1.7
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
  public void check () throws Exception { }

  ////////////////////////////////////////////////////////////

} // SavePanel class

////////////////////////////////////////////////////////////////////////
