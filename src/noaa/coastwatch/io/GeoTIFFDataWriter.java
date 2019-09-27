////////////////////////////////////////////////////////////////////////
/*

     File: GeoTIFFDataWriter.java
   Author: Peter Hollemans
     Date: 2019/09/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.RenderedImage;
import java.awt.Dimension;
import java.awt.geom.NoninvertibleTransformException;
import javax.imageio.stream.FileImageOutputStream;

import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.io.GeoTIFFWriter;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.tools.ToolServices;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>GeoTIFFDataWriter</code> encodes a set of data variables
 * as a 32-bit floating-point value multiband TIFF file with GeoTIFF
 * georeferencing tags.  The resulting image file is not suitable for display,
 * but can be imported into GIS systems.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.1
 */
public class GeoTIFFDataWriter extends EarthDataWriter {

  // Variables
  // ---------

  /** The writer to use for encoding image data. */
  private GeoTIFFWriter writer;

  /** The image dimensions. */
  private Dimension imageDims;

  /** The output stream for encoding data. */
  private FileImageOutputStream outputStream;
  
  /** The list of float data arrays to write. */
  private List<float[]> floatArrayList;
  
  /** The list of variable names corresponding to the float data. */
  private List<String> varNameList;
  
  /** Flag to signify that the file is closed. */
  private boolean closed;

  /** The missing data value. */
  private float missing = Float.NaN;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new writer.
   *
   * @param info the info object to use for earth transform and other attributes.
   * @param filename the new GeoTIFF file name.
   * @param compress the TIFF compression type, either COMP_NONE or COMP_DEFLATE
   * (see {@link GeoTIFFWriter}).
   *
   * @throws IOException if an error occurred  the file.
   */
  public GeoTIFFDataWriter (
    EarthDataInfo info,
    String filename,
    int compress
  ) throws IOException {

    super (filename);

    this.info = info;
    this.floatArrayList = new ArrayList<>();
    this.varNameList = new ArrayList<>();

    // Create writer
    // -------------
    closed = true;

    File file = new File (filename);
    if (file.exists()) file.delete();

    this.outputStream = new FileImageOutputStream (file);

    EarthTransform earthTrans = info.getTransform();
    int[] dims = earthTrans.getDimensions();
    this.imageDims = new Dimension (dims[COL], dims[ROW]);

    ImageTransform imageTrans;
    try {
      imageTrans = new ImageTransform (
        imageDims,
        new DataLocation ((dims[ROW]-1)/2.0, (dims[COL]-1)/2.0),
        1.0
      );
    } // try
    catch (NoninvertibleTransformException e) { throw new IOException (e); }
    EarthImageTransform trans = new EarthImageTransform (earthTrans, imageTrans);

    this.writer = new GeoTIFFWriter (outputStream, trans, compress);

    closed = false;

    // Set extra attributes
    // --------------------
    writer.setArtist (System.getProperty ("user.name"));
    writer.setSoftware (ToolServices.getToolVersion ("") + ToolServices.getCommandLine());
    writer.setComputer (ToolServices.getJavaVersion());

  } // GeoTIFFDataWriter

  ////////////////////////////////////////////////////////////

  /**
   * Sets the missing value.
   *
   * @param missing the missing value.  The missing value is used to
   * represent missing or out of range data.  By default, Float.NaN is used
   * as the missing value.
   */
  public void setMissing (float missing) {

    this.missing = missing;

  } // setMissing

  ////////////////////////////////////////////////////////////

  @Override
  public void flush () throws IOException {
  
    // Check for canceled
    // ------------------
    if (isCanceled) return;

    // Initialize progress counters
    // ----------------------------
    synchronized (this) {
      writeProgress = 0;
      writeVariables = 0;
    } // synchronized

    // Loop over each variable
    // -----------------------
    int pixels = imageDims.width * imageDims.height;
    while (variables.size() != 0) {

      // Convert to float data
      // ---------------------
      Grid grid = (Grid) variables.remove (0);
      writeVariableName = grid.getName();
      varNameList.add (writeVariableName);

      float[] floatArray = new float[pixels];
      for (int i = 0; i < imageDims.height; i++) {
        for (int j = 0; j < imageDims.width; j++) {

          int index = i*imageDims.width + j;
          float val = (float) grid.getValue (i, j);
          if (Float.isNaN (val)) floatArray[index] = missing;
          else floatArray[index] = val;
          
        } // for
        writeProgress = ((i*imageDims.width)*100)/pixels;
      } // for
      floatArrayList.add (floatArray);

      // Update progress
      // ---------------
      synchronized (this) {
        writeProgress = 0;
        writeVariables++;
      } // synchronized

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // while
  
  } // flush

  ////////////////////////////////////////////////////////////

  @Override
  public void close () throws IOException {

    // Check for already closed
    // ------------------------
    if (!closed) {

      flush();

      // Write image description
      // -----------------------
      StringBuffer desc = new StringBuffer();
      for (String name : varNameList) {
        if (desc.length() != 0) desc.append ("|");
        desc.append (name);
      } // while
      writer.setDescription (desc.toString());

      // Write image
      // -----------
      RenderedImage image = GeoTIFFWriter.createImageForData (
        imageDims.width, imageDims.height, floatArrayList);
      writer.encode (image);
      outputStream.close();

      // Mark as closed
      // --------------
      closed = true;
      
    } // if

  } // close

  ////////////////////////////////////////////////////////////

} // GeoTIFFDataWriter class

////////////////////////////////////////////////////////////////////////
