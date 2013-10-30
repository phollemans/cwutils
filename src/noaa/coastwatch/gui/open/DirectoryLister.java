////////////////////////////////////////////////////////////////////////
/*
     FILE: DirectoryLister.java
  PURPOSE: Interface for listing directories.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/24
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.util.*;
import java.io.*;

/**
 * A <code>DirectoryLister</code> performs simple directory listing
 * services, providing the file names in a directory, their size, and
 * modification times.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface DirectoryLister extends Cloneable {

  ////////////////////////////////////////////////////////////

  /** Gets the directory name. */
  public String getDirectory();

  ////////////////////////////////////////////////////////////

  /** Gets an independent copy of this lister. */
  public Object clone();
  
  ////////////////////////////////////////////////////////////

  /** Sets the directory name. */
  public void setDirectory (String name) throws IOException;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the parent directory name for the specified directory. 
   *
   * @param name the name of the directory to get the parent.
   *
   * @return the parent directory.  If the directory has no parent,
   * the directory itself is returned.
   */
  public String getParent (String name);

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the full child directory name for the specified parent and
   * child.
   *
   * @param parent the parent base directory.
   * @param child the child directory within the parent.
   *
   * @return the full child directory as a concatentation of parent
   * and child.
   */
  public String getChild (String parent, String child);

  ////////////////////////////////////////////////////////////

  /** Refreshes the entry list based on the current directory name. */
  public void refresh () throws IOException;

  ////////////////////////////////////////////////////////////

  /** Clears the directory name and entry list. */
  public void clear();

  ////////////////////////////////////////////////////////////

  /** Gets the list of directory entries. */
  public List getEntries();

  ////////////////////////////////////////////////////////////

  /**
   * The <code>DirectoryLister.Entry</code> class may be used to
   * access one entry in the contents of a directory lister.
   */
  public static class Entry 
    implements Comparable {

    // Variables
    // ---------
    private String name;
    private Date modified;
    private long size;
    private boolean isDir;

    /** Creates a new entry. */
    protected Entry (String n, Date m, long s, boolean d) { 
      name = n;
      modified = m;
      size = s;
      isDir = d;
    } // Entry

    /** Gets the directory entry name. */
    public String getName() { return (name); }

    /** Get the date that the directory entry was last modified. */
    public Date getModified() { return (modified); } 

    /** Gets the size of the directory entry (if applicable). */
    public long getSize() { return (size); }

    /** Returns true if the entry is a subdirectory. */
    public boolean isDirectory() { return (isDir); }

    /** Compares this entry to another. */
    public int compareTo (Object o) {
      Entry other = (Entry) o;
      if (this.isDir && !other.isDir) return (-1);
      else if (!this.isDir && other.isDir) return (1);
      else return (this.name.compareTo (other.name));
    } // compareTo 

    /** Checks this entry against another for equality. */
    public boolean equals (Object o) {
      Entry other = (Entry) o;
      return (this.name.equals (other.name) && this.isDir == other.isDir);
    } // equals

    /** Converts this entry to a string. */
    public String toString () {
      return (this.getClass().getName() + "[" +
        "name=" + name + "," +
        "modified=" + modified + "," +
        "size=" + size + "," +
        "isDir=" + isDir + "]"
      );
    } // equals

  } // Entry class

  ////////////////////////////////////////////////////////////

} // DirectoryLister interface

////////////////////////////////////////////////////////////////////////
