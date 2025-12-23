////////////////////////////////////////////////////////////////////////
/*

     File: SerializedObjectManager.java
   Author: Peter Hollemans
     Date: 2004/04/04

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
package noaa.coastwatch.io;

// Imports
// -------
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Collections;

/** 
 * The <code>SerializedObjectManager</code> class can be used to save,
 * load, delete, and get a list of serialized objects.  The objects
 * are stored as GZIP compressed files in a user-specified directory.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SerializedObjectManager {

  // Constants
  // ---------

  /** The serialized object file extension. */
  private static final String FILE_EXTENSION = ".jso";

  // Variables
  // ---------

  /** The object directory. */
  private File objectDir;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new manager to handle serialized objects in the
   * specified directory.
   *
   * @param objectDir the directory used to perform all serialized
   * object operations.
   */
  public SerializedObjectManager (
    File objectDir
  ) {

    this.objectDir = objectDir;

  } // SerializedObjectManager constructor

  ////////////////////////////////////////////////////////////

  /** Gets the list of object names available. */
  public List<String> getObjectNames () {
    
    // List the object file names, strip the file extension, and 
    // sort in alphabetic order.
    File[] files = objectDir.listFiles ((dir, name) -> name.endsWith (FILE_EXTENSION));
    List<String> objectList = new ArrayList<>();
    for (var file : files) {
      var fileName = file.getName();
      var objectName = fileName.substring (0, fileName.lastIndexOf (FILE_EXTENSION));
      objectList.add (objectName);
    } // for
    Collections.sort (objectList);

    return (objectList);

  } // getObjectNames

  ////////////////////////////////////////////////////////////

  /** 
   * Loads the specified serialized object.
   *
   * @param objectName the object name, which must be a valid name
   * obtained from the <code>getObjectNames()</code> method.
   *
   * @return the deserialized object read.
   *
   * @throws IOException if an error occurred reading the object file.
   * @throws ClassNotFoundException if the object class read is unknown.
   */
  public Object loadObject (
    String objectName
  ) throws IOException, ClassNotFoundException {

    // Create object input stream
    // --------------------------
    File file = new File (objectDir, objectName + FILE_EXTENSION);
    FileInputStream fileInput = new FileInputStream (file);
    GZIPInputStream zipInput = new GZIPInputStream (fileInput);
    ObjectInputStream objectInput = new ObjectInputStream (zipInput);

    // Read object
    // -----------
    try {
      Object object = objectInput.readObject();
      return (object);
    } // try
    finally {
      objectInput.close();
      zipInput.close();
      fileInput.close();
    } // finally
 
  } // loadObject

  ////////////////////////////////////////////////////////////

  /** 
   * Serializes and saves the specified object.
   *
   * @param object the object to save.
   * @param objectName the object name.  This is the name that may be
   * used later to retrieve the object.
   *
   * @throws IOException if an error occurred writing the object file.
   */
  public void saveObject (
    Object object,
    String objectName
  ) throws IOException {

    // Create object output stream
    // ---------------------------
    File file = new File (objectDir, objectName + FILE_EXTENSION);
    FileOutputStream fileOutput = new FileOutputStream (file);
    GZIPOutputStream zipOutput = new GZIPOutputStream (fileOutput);
    ObjectOutputStream objectOutput = new ObjectOutputStream (zipOutput);

    // Write object
    // ------------
    try {
      objectOutput.writeObject (object);
      objectOutput.flush();
    } // try
    finally {
      objectOutput.close();
      zipOutput.close();
      fileOutput.close();
    } // finally

  } // saveObject

  ////////////////////////////////////////////////////////////

  /** 
   * Deletes the specified object.  A subsequent call to
   * <code>getObjectNames()</code> will not include thie specified
   * name in the list.
   *
   * @throws IOException if an error occurred deleting the object file.
   */
  public void deleteObject (
    String objectName
  ) throws IOException {

    File file = new File (objectDir, objectName + FILE_EXTENSION);
    boolean deleted = file.delete();
    if (!deleted) throw new IOException ("Cannot delete object file " + file);

  } // deleteObject

  ////////////////////////////////////////////////////////////

} // SerializedObjectManager class

////////////////////////////////////////////////////////////////////////
