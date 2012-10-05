////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataReaderFactory.java
  PURPOSE: A class to act as a factory for Earth data readers.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/04/21, MSR, added implementation
           2002/05/21, PFH, added javadoc, package, revised code
           2003/02/13, PFH, added NOAA1b readers
           2004/04/10, PFH, modified to use reader list
           2004/09/09, PFH, renamed SatelliteDataReaderFactory to 
             EarthDataReaderFactory
           2005/02/15, PFH, added NOAA 1b V3 reader to default list
           2005/07/03, PFH, added check for network file name
           2005/07/04, PFH, added CWNCReader
           2005/08/05, PFH, added GenericNCReader
           2006/01/27, PFH, added NOAA 1b V4 reader to default list
           2006/05/29, PFH, modified to use reader name strings
           2006/09/01, PFH, added extensions
           2006/11/15, PFH, added NOAA 1b V5 reader to default list
           2007/09/20, PFH, added ACSPOHDFReader class
           2007/11/05, PFH, added NOAA1bFileReader for non-AVHRR data

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
//import ncsa.hdf.hdf5lib.H5;

/**
 * The Earth data reader factory class creates an appropriate
 * Earth data reader object based on a file name.  The default
 * list of readers is as follows:
 * <ul>
 *   <li> {@link noaa.coastwatch.io.CWFReader} </li>
 *   <li> {@link noaa.coastwatch.io.CWHDFReader} </li>
 *   <li> {@link noaa.coastwatch.io.TSHDFReader} </li>
 *   <li> {@link noaa.coastwatch.io.NOAA1bV1Reader} </li>
 *   <li> {@link noaa.coastwatch.io.NOAA1bV2Reader} </li>
 *   <li> {@link noaa.coastwatch.io.NOAA1bV3Reader} </li>
 *   <li> {@link noaa.coastwatch.io.NOAA1bV4Reader} </li>
 *   <li> {@link noaa.coastwatch.io.NOAA1bV5Reader} </li>
 *   <li> {@link noaa.coastwatch.io.CWNCReader} </li>
 *   <li> {@link noaa.coastwatch.io.GenericNCReader} </li>
 *   <li> {@link noaa.coastwatch.io.ACSPOHDFReader} </li>
 *   <li> {@link noaa.coastwatch.io.noaa1b.NOAA1bFileReader} </li>
 * </ul>
 * Additional readers may be appended to the list using the
 * <code>addReader()</code> method, or by adding the class name
 * to the <code>reader.properties</code> resource file.
 */
public class EarthDataReaderFactory {

  // Constants
  // ---------

  /** The resource file for reader extensions. */
  private static final String READER_PROPERTIES = "reader.properties";

  // Variables
  // ---------

  /** The verbose flag, true to print all error messages. */
  private static boolean verbose;

  /** The list of available reader classes as strings. */
  private static List readerList;
  
  /** The list of available reader classes as strings. */
  private static List h5ReaderList;

  ////////////////////////////////////////////////////////////

  /** Sets up the initial set of readers. */
  static {

    // Add standard readers
    // --------------------
    readerList = new ArrayList();
    h5ReaderList = new ArrayList();
    String thisPackage = EarthDataReaderFactory.class.getPackage().getName();
    readerList.add (thisPackage + ".CWFReader");
    readerList.add (thisPackage + ".CWHDFReader");
    readerList.add (thisPackage + ".TSHDFReader");
    readerList.add (thisPackage + ".NOAA1bV1Reader");
    readerList.add (thisPackage + ".NOAA1bV2Reader");
    readerList.add (thisPackage + ".NOAA1bV3Reader");
    readerList.add (thisPackage + ".NOAA1bV4Reader");
    readerList.add (thisPackage + ".NOAA1bV5Reader");
    readerList.add (thisPackage + ".ACSPOHDFReader");
    //readerList.add (thisPackage + ".ACSPONCReader");
    readerList.add (thisPackage + ".noaa1b.NOAA1bFileReader");
    h5ReaderList.add (thisPackage + ".L2PNCReader");
    h5ReaderList.add (thisPackage + ".ACSPONCReader");
    h5ReaderList.add (thisPackage + ".CWNCReader");
    h5ReaderList.add (thisPackage + ".CWNCCFReader");
    h5ReaderList.add (thisPackage + ".GenericNCReader");
    //readerList.add (thisPackage + ".VIIRSSDRReader");
    
    // Add extension readers
    // ---------------------
    InputStream stream = ClassLoader.getSystemResourceAsStream (
      READER_PROPERTIES);
    if (stream != null) {
      Properties props = new Properties();
      try { 
        props.load (stream); 
        for (Iterator iter = props.keySet().iterator(); iter.hasNext();)
          readerList.add ((String) iter.next());
      } // try
      catch (IOException e) { }
      finally { 
        try { stream.close(); }
        catch (IOException e) { }
      } // finally
    } // if

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new reader to the list.  When the factory creates a new
   * reader from a file name, the new reader will be among those tried
   * when opening the file.  The reader should throw a
   * <code>java.io.IOException</code> if an error is encountered
   * reading the initial parts of the file, and/or if the file format
   * is not recognized.  By convention, the reader should have a
   * constructor that takes a single <code>String</code> parameter as
   * the file name.
   *
   * @param readerClass the new reader class to add to the list.
   */
  public static void addReader (
    Class readerClass
  ) {

    readerList.add (readerClass.getName());

  } // addReader

  ////////////////////////////////////////////////////////////

  /**
   * Sets the verbose mode.  If verbose mode is on, the errors
   * encountered while trying to create the reader object are
   * printed.
   *
   * @param flag the verbose flag.
   */
  public static void setVerbose (boolean flag) { verbose = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an Earth data reader object.
   *
   * @param file the file name.  The file will be opened using the
   * different Earth data reader classes in the list until the
   * correct constructor is found.
   * 
   * @return an Earth data reader object specific to the file
   * format.
   *
   * @throws IOException if the reader could not be created.  Either
   * the file was not found, or the format was not recognized by any
   * reader.
   */
  public static EarthDataReader create (
    String file
  ) throws IOException {

    // Check for a network file
    // ------------------------
    boolean isNetwork = file.startsWith ("http://");

    // Check file exists and is readable
    // ---------------------------------
    if (!isNetwork) {
      File f = new File (file);
      if (!f.canRead ()) throw new FileNotFoundException ("Cannot open " + 
        file);
    } // if


// FIXME: This needs to be fixed so that the filename extensions
// are not relied upon for files to be recognized correctly.  There
// must be some other more reliable method!



    // Loop through each reader class
    // ------------------------------
    Class[] types = new Class[] {String.class};
    Object[] args = new Object[] {file};
    EarthDataReader reader = null;
    try{
    	//if(H5.H5Fis_hdf5(file.toString())){
    	boolean isNCfile = file.endsWith(".nc4") || file.endsWith(".nc");
      boolean isGribFile = file.endsWith(".grib") || file.endsWith(".grib2") || file.endsWith (".grb");
    	//boolean isNCfile = file.endsWith(".nc4");
    	if(isNCfile || isGribFile || isNetwork){


//System.out.println ("Trying to open as a NetCDF file");


    		for (Iterator iter = h5ReaderList.iterator(); iter.hasNext(); ) {
  		      Class readerClass = Class.forName ((String) iter.next());
  		      Constructor constructor = readerClass.getConstructor (types);
  		      try{
  		    	  reader = (EarthDataReader) constructor.newInstance (args);
  		      }
  		      catch(Exception e){
  		    	  reader = null;
  		      }
  		      if (reader != null) break;
    		} // for
    	}
    	else{


//System.out.println ("Trying to open as a non-NetCDF file");


    		for (Iterator iter = readerList.iterator(); iter.hasNext(); ) {
    		    Class readerClass = Class.forName ((String) iter.next());
    		    Constructor constructor = readerClass.getConstructor (types);
    		    try{
    		    	reader = (EarthDataReader) constructor.newInstance (args);
    		    }
    		    catch(Exception e){
    		    	  reader = null;
    		    }
    		    if (reader != null) break;
    		} // for
    	}
    }
    catch(Exception e){
    	e.printStackTrace();
    }
    /*
    for (Iterator iter = readerList.iterator(); iter.hasNext(); ) {
      try {
        Class readerClass = Class.forName ((String) iter.next());
        Constructor constructor = readerClass.getConstructor (types);
        reader = (EarthDataReader) constructor.newInstance (args);
      } // try
      catch (InvocationTargetException e1) {
        if (verbose) e1.getCause().printStackTrace();
      } // catch
      catch (Exception e2) {
        if (verbose) e2.printStackTrace();
      } // catch
      if (reader != null) break;
    } // for
    */
    // Give up and throw an error 
    // --------------------------
    if (reader == null) 
      throw new UnsupportedEncodingException (
        "Unable to recognize file format for " + file);      

    return (reader);

  } // create

  ////////////////////////////////////////////////////////////

} // EarthDataReaderFactory class

////////////////////////////////////////////////////////////////////////
