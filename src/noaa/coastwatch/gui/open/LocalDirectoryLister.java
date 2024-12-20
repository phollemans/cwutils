////////////////////////////////////////////////////////////////////////
/*

     File: LocalDirectoryLister.java
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import noaa.coastwatch.gui.open.AbstractDirectoryLister;
import noaa.coastwatch.gui.open.DirectoryLister;
import noaa.coastwatch.util.DateFormatter;
import com.braju.format.Format;

/**
 * The <code>LocalDirectoryLister</code> lists directory contents on
 * the local filesystem.  This class is mainly useful as a test
 * implementation of the {@link DirectoryLister} class, as the
 * <code>JFileChooser</code> provides more extensive directory listing
 * services in a GUI format.  The directory name must be an existing
 * local path convertible into a <code>java.io.File</code> object.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class LocalDirectoryLister 
  extends AbstractDirectoryLister {

  ////////////////////////////////////////////////////////////

  public String getParent (
    String name
  ) {

    String parent = new File (name).getParent();
    if (parent == null) parent = name;
    return (parent);

  } // getParent

  ////////////////////////////////////////////////////////////

  public String getChild (
    String parent,
    String child
  ) {

    if (!parent.endsWith (File.separator)) parent += File.separator;
    return (parent + child);

  } // getChild

  ////////////////////////////////////////////////////////////

  protected List buildEntryList (String name) throws IOException {

    // Get listing
    // -----------
    File file = new File (name);
    if (!file.exists()) 
      throw new IOException ("Directory does not exist");
    if (!file.isDirectory()) 
      throw new IOException ("Path name is not a directory");
    if (!file.canRead()) 
      throw new IOException ("Cannot read directory, access denied");
    String[] list = file.list();
    if (list == null) 
      throw new IOException ("Error getting directory listing");

    // Create list of entries
    // ----------------------
    List entryList = new ArrayList();
    for (int i = 0; i < list.length; i++) {
      File entryFile = new File (name, list[i]);
      entryList.add (new Entry (list[i], new Date (entryFile.lastModified()),
        entryFile.length(), entryFile.isDirectory()));
    } // for
    return (entryList);

  } // buildEntryList

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    DirectoryLister lister = new LocalDirectoryLister();
    lister.setDirectory (argv[0]);
    List entryList = lister.getEntries();
    Collections.sort (entryList);
    Format.printf ("%5s %-10s %-18s %s\n", 
      new Object[] {"", "Size", "Modified", "Name"});
    TimeZone zone = TimeZone.getDefault();
    for (Iterator iter = entryList.iterator(); iter.hasNext();) {
      Entry entry = (Entry) iter.next();
      String dir = (entry.isDirectory() ? "[DIR]" : "");
      String name = entry.getName();
      String date = DateFormatter.formatDate (entry.getModified(), 
        "yyyy/MM/dd HH:mm", zone);
      Long size = Long.valueOf (entry.getSize());
      Format.printf ("%5s %-10d %-18s %s\n", 
        new Object[] {dir, size, date, name});
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // LocalDirectoryLister class

////////////////////////////////////////////////////////////////////////
