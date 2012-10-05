////////////////////////////////////////////////////////////////////////
/*
     FILE: ACSPOBuoyDataReader.java
  PURPOSE: Creates an overlay based on ACSPO HDF buoy data.
   AUTHOR: Peter Hollemans
     DATE: 2008/05/14
  CHANGES: 2010/03/30, PFH, modified constructor to close file on failure

  CoastWatch Software Library and Utilities
  Copyright 1998-2010, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import ncsa.hdf.hdflib.*;

/**
 * The <code>ACSPOBuoyDataReader</code> class reads buoy point data
 * features from ACSPO HDF data files and acts as a point data source.
 *
 * @see noaa.coastwatch.io.ACSPOHDFReader
 */
public class ACSPOBuoyDataReader 
  extends PointFeatureSource {

  // Constants
  // ---------
  
  /** The buoy ID variable. */
  private static final String BUOY_VAR = "Buoy_ID";

  /** The latitude variable. */
  private static final String LAT_VAR = "Latitude(degrees)";

  /** The longitude variable. */
  private static final String LON_VAR = "Longitude(degrees)";

  ////////////////////////////////////////////////////////////

  /** Creates a new reader using an HDF file name. */
  public ACSPOBuoyDataReader (
    String file
  ) throws IOException, HDFException, ClassNotFoundException {

    // Open HDF file
    // -------------
    File f = new File (file);
    if (!f.canRead ()) throw new FileNotFoundException ("Cannot open " + file);
    int sdid = HDFLibrary.SDstart (file, HDFConstants.DFACC_READ);

    try {

      // Check attributes
      // ----------------
      String processor = (String) HDFReader.getAttribute (sdid, "PROCESSOR");
      if (!processor.toLowerCase().matches (".*(clavr-x|acspo).*"))
        throw new IOException ("Failed to find correct PROCESSOR attribute");
    
      // Get buoy count
      // --------------
      int[] dims = HDFReader.getVariableDimensions (sdid, BUOY_VAR);
      int buoyCount = dims[0];
      
      // Get buoy data arrays
      // --------------------
      String[] varNameArray = HDFReader.getVariableNames (sdid);
      Map<String,Object> varDataMap = new HashMap<String,Object>();
      List<String> nameList = new ArrayList<String>();
      for (String name : varNameArray) {

        // Access variable
        // ---------------
        int sdsid = HDFLibrary.SDselect (sdid, HDFLibrary.SDnametoindex (sdid, 
          name));
        if (sdsid < 0)
          throw new HDFException ("Cannot access variable " + name);

        // Get variable info
        // -----------------
        String[] varName = new String[] {""};
        int varDims[] = new int[HDFConstants.MAX_VAR_DIMS];
        int varInfo[] = new int[3];
        if (!HDFLibrary.SDgetinfo (sdsid, varName, varDims, varInfo))
          throw new HDFException ("Cannot get variable info for " + name);
        int rank = varInfo[0];
        int varType = varInfo[1];
        
        // Check dimensions
        // ----------------
        if (varDims[0] != buoyCount) continue;
        if (rank > 2) continue;
        nameList.add (name);
        
        // Read variable data array
        // ------------------------
        Class varClass = HDFReader.getClass (varType);
        int values = (rank == 1 ? varDims[0] : varDims[0]*varDims[1]);
        Object data = Array.newInstance (varClass, values);
        int[] start = new int[varDims.length];
        Arrays.fill (start, 0);
        int[] stride = new int[varDims.length];
        Arrays.fill (stride, 1);
        if (!HDFLibrary.SDreaddata (sdsid, start, stride, dims, data))
          throw new HDFException ("Cannot read data for " + name);
        
        // End access
        // ----------
        HDFLibrary.SDendaccess (sdsid);

        // Add data to map
        // ---------------
        varDataMap.put (name, data);

      } // for

      // End access to file
      // ------------------
      HDFLibrary.SDend (sdid);

      // Create list of features
      // -----------------------
      int attCount = varDataMap.size();
      for (int i = 0; i < buoyCount; i++) {
        Object[] attArray = new Object[attCount];
        double lat = 0, lon = 0;
        for (int attIndex = 0; attIndex < attCount; attIndex++) {

          // Get attribute value
          // -------------------
          String name = nameList.get (attIndex);
          if (name.equals (BUOY_VAR)) {
            attArray[attIndex] = new String ((byte[]) varDataMap.get (name), 
              i*8, 8);
          } // if
          else {
            attArray[attIndex] = Array.get (varDataMap.get (name), attIndex);
          } // else

          // Get lat/lon values
          // ------------------
          if (name.equals (LAT_VAR)) lat = (Float) attArray[attIndex];
          else if (name.equals (LON_VAR)) lon = (Float) attArray[attIndex];
          
        } // for
        featureList.add (new PointFeature (new EarthLocation (lat,lon),
          attArray));
      } // for

      // Set attribute names
      // -------------------
      setAttributeNames (nameList.toArray (new String[0]));

    } // try

    // Catch exception and close file
    // ------------------------------
    catch (Exception e) {
      HDFLibrary.SDend (sdid);
      throw new IOException (e.getMessage());
    } // catch

  } // ACSPOBuoyDataReader constructor

  ////////////////////////////////////////////////////////////

  protected void select () throws java.io.IOException {

    // Do nothing ??

  } // select

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    ACSPOBuoyDataReader reader = new ACSPOBuoyDataReader (argv[0]);
    int i = 0;
    for (Iterator iter = reader.iterator(); iter.hasNext();) {
      PointFeature feature = (PointFeature) iter.next();
      System.out.println ("Feature #" + i++);
      System.out.println ("  " + feature.getPoint());
      for (int j = 0; j < reader.getAttributeCount(); j++) {
        System.out.println ("  " + reader.getAttributeName (j) + " = " +
          feature.getAttribute (j));
      } // for
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // ACSPOBuoyDataReader class

////////////////////////////////////////////////////////////////////////

