////////////////////////////////////////////////////////////////////////
/*
     FILE: DynamicExtensionChooser.java
  PURPOSE: Allows the user to choose a file using various extensions.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/29
  CHANGES: 2006/11/07, PFH, added check for null extension on filter change

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.beans.*;
import java.io.*;
import java.util.*;

/** 
 * The <code>DynamicExtensionFileChooser</code> class allows the user
 * to select a file.  The chooser sets up various extra features in a
 * normal file chooser to filter for certain file types, and changes
 * the file name according to the selected file type.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class DynamicExtensionFileChooser
  extends JFileChooser {

  // Variables
  // ---------

 /** The last selected file name. */
  private String fileName;

  /** The map of filters to extensions. */
  private Map extensionMap;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser with specified filters.
   *
   * @param filterMap the map of filter descriptions to filter
   * extensions.  Each key in the map is a <code>String</code>
   * respresenting a filter description, for example "JPEG file".
   * Each value in the map is the corresponding list of extensions to
   * use for files as a <code>String[]</code> object, for example "jpg".
   */
  public DynamicExtensionFileChooser (
    Map filterMap
  ) {

    // Initialize
    // ----------
    setAcceptAllFileFilterUsed (false);
    extensionMap = new HashMap();

    // Create file filters
    // -------------------
    Set keys = filterMap.keySet();
    FileFilter firstFilter = null;
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String description = (String) iter.next();
      String[] extensionArray = (String[]) filterMap.get (description);
      FileFilter filter = new SimpleFileFilter (extensionArray, description);
      if (firstFilter == null) firstFilter = filter;
      addChoosableFileFilter (filter);
      extensionMap.put (filter, extensionArray[0]);
    } // for
    setFileFilter (firstFilter);

    // Add filter change listener
    // --------------------------
    addPropertyChangeListener (FILE_FILTER_CHANGED_PROPERTY, 
      new FilterChangedListener());

    // Add listener to save file name
    // ------------------------------
    addPropertyChangeListener (SELECTED_FILE_CHANGED_PROPERTY,
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          File file = (File) event.getNewValue();
          if (file != null) {
            fileName = file.getName();
          } // if
        } // propertyChange
      });

  } // DynamicExtensionFileChooser constructor

  ////////////////////////////////////////////////////////////

  /** Modifies the file name when the filter is changed. */
  private class FilterChangedListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      // Get new file extension
      // ----------------------
      FileFilter filter = (FileFilter) event.getNewValue();
      String extension = (String) extensionMap.get (filter);
      if (extension == null) return;

      // Update file name
      // ----------------
      if (fileName != null) {
        String newName = fileName.replaceFirst ("\\.[^.]*$", "." + extension);
        /** 
         * We set the file in a delayed runnable because of a little
         * quirk in the file chooser that erases the file name after
         * this point because a new filter has been selected.
         */
        final File newFile = new File (newName);
        SwingUtilities.invokeLater (new Runnable() {
            public void run () {
              setSelectedFile (newFile);
            } // run
          });
      } // if

    } // propertyChange
  } // PropertyChangeListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    Map filterMap = new TreeMap();
    filterMap.put ("JPEG image", new String[] {"jpg", "jpeg"});
    filterMap.put ("PNG image", new String[] {"png"});
    filterMap.put ("PDF document", new String[] {"pdf"});
    filterMap.put ("GeoTIFF image", new String[] {"tif", "tiff"});
    DynamicExtensionFileChooser chooser = 
      new DynamicExtensionFileChooser (filterMap);
    JPanel panel = new JPanel (new BorderLayout());
    panel.add (chooser, BorderLayout.CENTER);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // DynamicExtensionFileChooser

////////////////////////////////////////////////////////////////////////


