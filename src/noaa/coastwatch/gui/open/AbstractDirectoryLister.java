////////////////////////////////////////////////////////////////////////
/*

     File: AbstractDirectoryLister.java
   Author: Peter Hollemans
     Date: 2005/06/26

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.gui.open.DirectoryLister;

/**
 * The <code>AbstractDirectoryLister</code> is an abstract helper that
 * implements most of the {@link DirectoryLister} methods.  Child
 * classes need only implement:
 * <ul>
 *   <li>{@link DirectoryLister#getParent}</li>
 *   <li>{@link DirectoryLister#getChild}</li>
 *   <li>{@link #buildEntryList}</li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class AbstractDirectoryLister 
  implements DirectoryLister, Cloneable {

  // Variables
  // ---------

  /** The current directory name. */
  private String name;

  /** The list of entries for the current directory. */
  private List entryList;

  ////////////////////////////////////////////////////////////

  /** Creates a new lister with empty directory name. */
  public AbstractDirectoryLister () {

    entryList = new ArrayList();

  } // AbstractDirectoryLister constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the directory name. 
   * 
   * @return name the directory name or null if no directory is set.
   */
  public String getDirectory () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Gets a copy of this object. */
  public Object clone () {

    AbstractDirectoryLister copy;
    try { copy = (AbstractDirectoryLister) super.clone(); }
    catch (CloneNotSupportedException e) { return (null); }
    copy.entryList = new ArrayList (entryList);
    return (copy);

  } // clone

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the directory name. 
   *
   * @param name the new directory name.
   *
   * @throws IOException if an error occurred getting the entries for
   * the new directory.
   */
  public void setDirectory (
    String name
  ) throws IOException {

    name = name.trim();
    List newEntryList = buildEntryList (name);
    this.name = name;
    this.entryList = newEntryList;

  } // setDirectory

  ////////////////////////////////////////////////////////////

  /** 
   * Builds the list of directory entries. 
   *
   * @throws IOException if an error occurred getting the entries for
   * the new directory.
   */
  protected abstract List buildEntryList (String name) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Refreshes the entry list based on the current directory name. 
   *
   * @throws IOException if an error occurred getting the entries for
   * the new directory.
   */
  public void refresh () throws IOException {

    if (name != null) setDirectory (name);

  } // refresh

  ////////////////////////////////////////////////////////////

  /** Clears the directory name and entry list. */
  public void clear () {

    name = null;
    entryList.clear();

  } // clear

  ////////////////////////////////////////////////////////////

  /** Gets the list of directory entries. */
  public List getEntries () { return (new ArrayList (entryList)); }

  ////////////////////////////////////////////////////////////

} // AbstractDirectoryLister class

////////////////////////////////////////////////////////////////////////
