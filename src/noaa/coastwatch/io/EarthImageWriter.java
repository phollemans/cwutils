////////////////////////////////////////////////////////////////////////
/*

     File: EarthImageWriter.java
   Author: Peter Hollemans
     Date: 2006/11/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
// --------
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import edu.wlu.cs.levy.CG.KDTree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import noaa.coastwatch.io.GIFWriter;
import noaa.coastwatch.io.GeoTIFFWriter;
import noaa.coastwatch.io.WorldFileWriter;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataPlot;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.GammaEnhancement;
import noaa.coastwatch.render.Renderable;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>EarthImageWriter</code> has a single static method that writes
 * image data from an {@link EarthDataView} to one of various formats.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class EarthImageWriter {

  private static final Logger LOGGER = Logger.getLogger (EarthImageWriter.class.getName());
  private static final Logger VERBOSE = Logger.getLogger (EarthImageWriter.class.getName() + ".verbose");

  // Variables
  // ---------

  /** The single instance of this class. */
  private static EarthImageWriter instance;
  
  /** The plot legend background. */
  private Color backgroundColor = new Color (237, 238, 207);

  /** The plot legend foreground. */
  private Color foregroundColor = Color.BLACK;

  /** The plot fill. */
  private Color fillColor = Color.WHITE;

  /** The default plot font. */
  private Font font = new Font (null, Font.PLAIN, 9);

  ////////////////////////////////////////////////////////////

  /**
   * Creates an instance of this object.
   *
   * @return the object instance.
   */
  private EarthImageWriter () { }

  ////////////////////////////////////////////////////////////
  
  /**
   * Gets the singleton instance of this class.
   *
   * @return the singeton instance.
   */
  public static EarthImageWriter getInstance () {
  
    if (instance == null) instance = new EarthImageWriter();
    return (instance);
    
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Sets the font used for legends.
   *
   * @param font the new legend font.
   */
  public void setFont (Font font) { this.font = font; }

  ////////////////////////////////////////////////////////////

  /** 
   * Writes the specified view to an image file.  Note that if the
   * image file is to contain legends, the view itself may be modified
   * in size slightly to match the legend size.  If this effect is
   * undesired, the view should be cloned prior to passing to this
   * routine.
   *
   * @param view the earth data view to write.
   * @param info the earth data information to use for the
   * legends.
   * @param isVerbose the verbose flag, true to print verbose messages.
   * @param hasLegends the legends flag, true to draw color scale and
   * information legends.
   * @param logo the logo to use for the legend.  Ignored if
   * hasLegends is false.
   * @param isAntialiased the antialias flag, true to antialias fonts
   * and lines.
   * @param file the output file to write.
   * @param format the output file format: 'png', 'jpg', 'tif',
   * 'gif', or 'pdf'.
   * @param worldFile the world file to write, or null for no world
   * file.  Correct world files can only be written if no legends are
   * used and only for image formats, not PDF.
   * @param tiffComp the TIFF compression type: 'none', 'deflate', 'pack',
   * 'lzw', or 'jpeg'.
   * @param imageColors the number of image colors to use or 0 to
   * not restrict the image colors.  If &gt; 0, an indexed color
   * model will be used for TIFF, PNG, GIF, and PDF output and
   * antialiasing of lines turned off.
   *
   * @return a renderable object that may be used to show a preview of
   * the written image file.
   *
   * @throws IllegalArgumentException if the specified format is not
   * supported.
   * @throws IOException if an error occurred writing the image data.
   */
  public Renderable write (
    EarthDataView view,
    EarthDataInfo info,
    boolean isVerbose,
    boolean hasLegends,
    IconElement logo,
    boolean isAntialiased,
    File file,
    String format,
    String worldFile,
    String tiffComp,
    int imageColors
  ) throws IllegalArgumentException, IOException {

    if (isVerbose) VERBOSE.setLevel (Level.INFO);

    // Create renderable object
    // ------------------------
    Renderable renderable;
    if (hasLegends) {
      renderable = new EarthDataPlot (view, info, logo, foregroundColor, backgroundColor, font);
    } // if
    else {
      renderable = view;
    } // else
    
    // Detect indexable image format
    // -----------------------------
    boolean isIndexable = (
      format.equals ("tif") ||
      format.equals ("gif") ||
      format.equals ("png") ||
      format.equals ("pdf")
    );
    
    // We discovered that the TIFF writer doesn't like to JPEG compress
    // indexed images, so we keep them unindexed here in that case.
    if (format.equals ("tif") && tiffComp.equals ("jpeg")) isIndexable = false;

    // Get visible overlay list
    // ------------------------
    List overlayList = view.getOverlays();
    for (Iterator iter = overlayList.iterator(); iter.hasNext();) {
      EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
      if (!overlay.getVisible()) iter.remove();
    } // for
    int overlays = overlayList.size();

    // Get indexed color model
    // -----------------------
    boolean isIndexed = false;
    IndexColorModel colorModel = null;
    if (isIndexable && view instanceof ColorEnhancement && 
      ((!hasLegends && overlays == 0) || imageColors != 0)) {

      // Set the indexed flag
      // --------------------
      isIndexed = true;

      // Get simple model when no overlays are present
      // ---------------------------------------------
      if (overlays == 0 && imageColors == 0) {
        colorModel = ((ColorEnhancement) view).getColorModel();
      } // if

      // Get model from view and add overlay colors
      // ------------------------------------------
      else {

        // Get overlay colors
        // ------------------
        List overlayColorList = new ArrayList();
        if (!format.equals ("pdf")) {
          for (Iterator iter = overlayList.iterator(); iter.hasNext();) {
            EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
            overlayColorList.addAll (overlay.getColors());
          } // for
          while (overlayColorList.remove (null)) ;
        } // if
        int overlayColors = overlayColorList.size();

        LOGGER.fine ("Using " + overlayColors + " overlay colours");

        // Get legend colors
        // -----------------
        int legendColors = 0;
        if (hasLegends && !format.equals ("pdf")) {
          legendColors += 3;
        } // if

        LOGGER.fine ("Using " + legendColors + " legend colours");

        // Check image colors
        // ------------------
        int extraColors = overlayColors + legendColors;
        if (imageColors > 256) {
          throw new IOException ("Image colors must be <= 256");
        } // if
        else if ((imageColors - extraColors) < 16) {
          throw new IOException ("Too few colors for overlays and legends");
        } // if

        LOGGER.fine ("Using " + imageColors + " image colours and " +
          extraColors + " extra colours");

        // Set colors for data view
        // ------------------------
        ((ColorEnhancement) view).setColors (imageColors - extraColors);
        colorModel = ((ColorEnhancement) view).getColorModel();
        
        // Modify index model if needed
        // ----------------------------
        if (extraColors != 0) {
          isAntialiased = false;

          // Get current colors
          // ------------------
          byte[] red = new byte[imageColors];
          byte[] green = new byte[imageColors];
          byte[] blue = new byte[imageColors];
          colorModel.getReds (red);
          colorModel.getGreens (green);
          colorModel.getBlues (blue);

          // Add overlay colors
          // ------------------
          int colorIndex = (imageColors-1) - extraColors;
          for (Iterator iter = overlayColorList.iterator(); iter.hasNext();) {
            Color color = (Color) iter.next();
            red[colorIndex] = (byte) color.getRed();
            green[colorIndex] = (byte) color.getGreen();
            blue[colorIndex] = (byte) color.getBlue();
            colorIndex++;
          } // for

          // Add legend colors
          // -----------------
          if (hasLegends) { 

            red[colorIndex] = (byte) fillColor.getRed();
            green[colorIndex] = (byte) fillColor.getGreen();
            blue[colorIndex] = (byte) fillColor.getBlue();
            colorIndex++;

            red[colorIndex] = (byte) foregroundColor.getRed();
            green[colorIndex] = (byte) foregroundColor.getGreen();
            blue[colorIndex] = (byte) foregroundColor.getBlue();
            colorIndex++;

            red[colorIndex] = (byte) backgroundColor.getRed();
            green[colorIndex] = (byte) backgroundColor.getGreen();
            blue[colorIndex] = (byte) backgroundColor.getBlue();
            colorIndex++;

          } // if

          // Create new model
          // ----------------
          colorModel = new IndexColorModel (8, imageColors, red, green, blue);

        } // if
        
      } // else
    } // if

    // Modify color map to be 256 colors
    // ---------------------------------
    /**
     * Why do this?  It turns out some image writers don't like
     * writing an image whose 8-bit color map is not 256 colors
     * exactly, like the TIFF writer.  So we accommodate them
     * here.
     */
    if (isIndexed && colorModel.getMapSize() != 256) {
      if (colorModel.getMapSize() > 256)
        throw new IOException ("Index color model has too many colors");
      byte[] red = new byte[256];
      byte[] green = new byte[256];
      byte[] blue = new byte[256];
      colorModel.getReds (red);
      colorModel.getGreens (green);
      colorModel.getBlues (blue);
      colorModel = new IndexColorModel (8, 256, red, green, blue);
    } // if    

    // Get total rendered size
    // -----------------------
    BufferedImage tmpImage = new BufferedImage (1, 1, 
      BufferedImage.TYPE_INT_RGB);
    Graphics2D tmpGraphics = tmpImage.createGraphics();
    Dimension renderSize = renderable.getSize (tmpGraphics);
    tmpGraphics.dispose();

    // Render to raster
    // ----------------
    if (format.equals ("png") || format.equals ("jpg") || 
      format.equals ("tif") || format.equals ("gif")) {

      // Create buffered image
      // ---------------------
      BufferedImage image = new BufferedImage (renderSize.width, 
        renderSize.height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();

      // Set antialiasing
      // ----------------
      if (isAntialiased) {
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
          RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, 
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      } // if
      else {
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
          RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, 
          RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      } // else

      // Render
      // ------
      g.setColor (fillColor);
      g.fillRect (0, 0, renderSize.width, renderSize.height); 
      renderable.render (g);
      g.dispose();

      // Convert to indexed
      // ------------------
      if (isIndexed) {
        image = quantizeToIndex (image, colorModel);
      } // if

      // Print writing message
      // ---------------------
      VERBOSE.info ("Writing output " + file);

      // Write GeoTIFF file
      // ------------------
      if (format.equals ("tif")) {

        // Determine compression
        // ---------------------
        int compress;
        if (tiffComp.equals ("none")) 
          compress = GeoTIFFWriter.COMP_NONE;
        else if (tiffComp.equals ("deflate")) 
          compress = GeoTIFFWriter.COMP_DEFLATE;
        else if (tiffComp.equals ("pack"))
          compress = GeoTIFFWriter.COMP_PACK;
        else if (tiffComp.equals ("lzw"))
          compress = GeoTIFFWriter.COMP_LZW;
        else if (tiffComp.equals ("jpeg"))
          compress = GeoTIFFWriter.COMP_JPEG;
        else {
          throw new IllegalArgumentException ("Unsupported TIFF " +
            "compression: " + tiffComp);
        } // else

        // Create TIFF writer
        // ------------------
        if (file.exists()) file.delete();
        FileImageOutputStream outputStream = new FileImageOutputStream (file);
        GeoTIFFWriter writer = new GeoTIFFWriter (outputStream,
          view.getTransform(), compress);

        writer.setArtist (System.getProperty ("user.name"));
        writer.setSoftware (ToolServices.getToolVersion ("") + ToolServices.getCommandLine());
        writer.setComputer (ToolServices.getJavaVersion());

        // Write the palette equation to the description
        // ---------------------------------------------
        if (isIndexed) {
          ColorEnhancement enhancement = (ColorEnhancement) view;
          EnhancementFunction func = enhancement.getFunction();
          double min = func.getInverse(0);
          double max = func.getInverse(1);
          int colors = enhancement.getColors();
          String equation = enhancement.getGrid().getName() + " = ";
          if (func instanceof LinearEnhancement) {

            /**
             * For a linear enhancement, we reverse the y = mx + b.
             * m = 1/(max-min)
             * b = -m*min
             * Then we have y = (x-min) / (max-min)
             * Rearranging, x = y*(max-min) + min
             * For example: sst = color_index*0.117647 + 0
             * where max = 30, min = 0, colors = 256, y = color_index/(colors-1)
             */
            equation += "color_index*" + (max-min)/(colors-1) + " + " + min; 

          } // if
          else if (func instanceof LogEnhancement) {

            /**
             * For a log enhancement, we reverse the y = mlogx + b.  Since
             * m = 1/(log(max) - log(min) and
             * b = -m * log(min)
             * Then we have y = [logx - log(min)]/[log(max) - log(min)]
             * Rearranging, x = min * (max/min)^y
             * For example: chlor_a = 0.01 * (6500^(color_index/255))
             * where max = 65, min = 0.01, colors = 256, y = color_index/(colors-1)
             */

            equation += min + "*(" + (max/min) + "^(color_index/" +(colors-1) + "))";

          } // else if

          else if (func instanceof GammaEnhancement) {
          
            /**
             * For a gamma enhancement, we reverse the y = (mx + b)^g.  Since
             * similarly to the linear case:
             * m = 1/(max-min)
             * b = -m*min
             * Then we have y = [(x-min)/(max-min)]^g
             * Rearranging, x = y^(1/g) * (max - min) + min
             * For example: EV_BandM5 = 4.35 * color_index^0.4545 0.01*(6500^(color_index/255))
             * where max = 55, min = 1, gamma = 1/2.2, colors = 256, y = color_index/(colors-1)
             */
            double gamma = ((GammaEnhancement) func).getGamma();
            equation += "((color_index/" + (colors-1) + ")^" + gamma + ")*" + (max-min) + " + " + min;
              
          } // else if
          
          else {
            throw new IOException ("Unknown enhancement function: " + func.getClass());
          } // else

          writer.setDescription (equation);

        } // if

        // Encode the image
        // ----------------
        writer.encode (image);
        outputStream.close();

      } // if

      // Write JPEG, GIF or PNG file
      // ---------------------------
      else {

        // Get default metadata
        // --------------------
        ImageWriter writer = ImageIO.getImageWritersByFormatName (format).next();
        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType (BufferedImage.TYPE_INT_RGB);
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata (imageType, writeParam);

        // Add metadata
        // ------------
        String metadataFormat = "javax_imageio_1.0";

        /**
         * Here we create a set of metadata keyword/value pairs whose keywords 
         * are predefined for PNG files here:
         *
         * http://dev.exiv2.org/projects/exiv2/wiki/The_Metadata_in_PNG_files
         *
         * We use them for GIF and JPEG files as well.
         */

        Map<String, String> keyValueMap = new LinkedHashMap<String, String>();
        keyValueMap.put ("Author", System.getProperty ("user.name"));
        String dateTime =  new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss z").format (new Date());
        keyValueMap.put ("Creation Time", dateTime);
        keyValueMap.put ("Software", ToolServices.PACKAGE + " version " +
          ToolServices.getVersion());
        keyValueMap.put ("Source", ToolServices.getJavaVersion());
        keyValueMap.put ("Comment", "Command line was " + ToolServices.getCommandLine());

        IIOMetadataNode root = new IIOMetadataNode (metadataFormat);
        IIOMetadataNode textNode = new IIOMetadataNode ("Text");
        root.appendChild (textNode);

        boolean supportsKeywords = (format.equals ("png"));
        for (String keyword : keyValueMap.keySet()) {
          IIOMetadataNode entry = new IIOMetadataNode ("TextEntry");
          if (supportsKeywords) {
            entry.setAttribute ("keyword", keyword);
            entry.setAttribute ("value", keyValueMap.get (keyword));
          } // if
          else {
            entry.setAttribute ("value", keyword + ": " + keyValueMap.get (keyword));
          } // else
          textNode.appendChild (entry);
        } // for

        metadata.mergeTree (metadataFormat, root);
        
        // Quantize image for GIF encoding
        // -------------------------------
        if (format.equals ("gif")) {
          if (GIFWriter.needsQuantization (image))
            image = GIFWriter.getQuantizedImage (image);
        } // if

        // Write file
        // ----------

        // We found that if we don't have this next line and the file exists,
        // the data is written to the image at the beginning of the file
        // and the rest of the file remains untouched.  So if the existing
        // file is much larger than the new file, the new file data is written
        // to the beginning, and the rest of the larger file (garbage at this
        // point) remains.  The other image file writers work differently,
        // and seem to delete the larger file's previous contents, maybe
        // because they're created using FileOutputStream rather than
        // FileImageOutputStream.  This probably started happening when we
        // began writing extra metadata into the file headers.
        
        if (file.exists()) file.delete();

        FileImageOutputStream outputStream = new FileImageOutputStream (file);
        writer.setOutput (outputStream);
        IIOImage imageData = new IIOImage (image, null, metadata);
        writer.write (metadata, imageData, writeParam);
        outputStream.close();
      
      } // else

      // Write world file for PNG, GIF, JPEG
      // -----------------------------------
      if (worldFile != null && !format.equals ("tif")) {
        WorldFileWriter worldWriter = new WorldFileWriter (
          new FileOutputStream (worldFile), view.getTransform());
        VERBOSE.info ("Writing world file " + worldFile);
        worldWriter.write();
      } // if

    } // if

    // Render to PDF
    // -------------
    else if (format.equals ("pdf")) {

      // Create PDF file
      // ---------------
      com.lowagie.text.Rectangle page = PageSize.LETTER;
      if (renderSize.width > renderSize.height) page = page.rotate();
      Document document = new Document (page);
      PdfWriter writer;
      try {
        writer = PdfWriter.getInstance (document, new FileOutputStream (file));
      } // try
      catch (DocumentException e) {
        throw new IOException (e.getMessage());
      } // catch
      
      // Add metadata
      // ------------
      document.addAuthor (System.getProperty ("user.name"));
      document.addCreator (ToolServices.getToolVersion ("") + ToolServices.getCommandLine());
      document.open();

      // Compute scale factor
      // --------------------
      int maxWidth = (int) page.width() - 72;
      int maxHeight = (int) page.height() - 72;
      double scale = 1;
      if (renderSize.width > maxWidth || renderSize.height > maxHeight) {
        scale = Math.min ((double) maxWidth/renderSize.width,
          (double) maxHeight/renderSize.height);
        renderSize.width = (int) (scale*renderSize.width);
        renderSize.height = (int) (scale*renderSize.height);
      } // if

      // Create template
      // ---------------
      PdfContentByte content = writer.getDirectContent();
      PdfTemplate template = content.createTemplate (renderSize.width, 
        renderSize.height);
      Graphics2D g = template.createGraphics (renderSize.width, 
        renderSize.height);
      template.setWidth (renderSize.width);
      template.setHeight (renderSize.height);

      // Render
      // ------
      g.scale (scale, scale);
      renderable.render (g);
      g.dispose();

      // Add template and close
      // ----------------------
      int xoffset = (int) page.width() / 2 - renderSize.width / 2;
      int yoffset = (int) page.height() / 2 - renderSize.height / 2;
      VERBOSE.info ("Writing output " + file);
      content.addTemplate (template, xoffset, yoffset);
      document.close();

    } // else if

    // Unsupported format
    // ------------------
    else throw new IllegalArgumentException ("Unsupported format: " + format);

    return (renderable);

  } // write

  ////////////////////////////////////////////////////////////

  /**
   * Performs a color quantization of an image to a specific
   * index color model.
   *
   * @param image the image to quantize.
   * @param colorModel the index color model to quantize the
   * image with size &lt;= 256 colors.
   *
   * @return the output quantized image.
   */
  public static BufferedImage quantizeToIndex (
    BufferedImage image,
    IndexColorModel colorModel
  ) {

    // Create hash map and color tree
    // ------------------------------
    HashMap colorMap = new HashMap();
    KDTree colorTree = new KDTree (3);
    int colors = colorModel.getMapSize();
    for (int i = 0; i < colors; i++) {

      // Check for duplicate color
      // -------------------------
      int colorRGB = colorModel.getRGB (i);
      Integer mapKey = Integer.valueOf (colorRGB);
      if (colorMap.containsKey (mapKey)) continue;

      // Add entry to hash map
      // ---------------------
      Byte colorIndex = Byte.valueOf ((byte) i);
      colorMap.put (mapKey, colorIndex);

      // Add entry to color tree
      // -----------------------
      double[] treeKey = new double[] {
        (colorRGB & 0xff0000) >>> 16,
        (colorRGB & 0xff00) >>> 8,
        colorRGB & 0xff
      };
      try { colorTree.insert (treeKey, colorIndex); }
      catch (Exception e) {
        throw new RuntimeException ("Error in k-d tree insert call");
      } // catch

    } // for

    // Create new image
    // ----------------
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage newImage = new BufferedImage (width, height, 
      BufferedImage.TYPE_BYTE_INDEXED, colorModel);

    // Loop over each color pixel
    // --------------------------
    WritableRaster raster = newImage.getRaster();
    byte[] byteRow = new byte[width];
    double[] treeKey = new double[3];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        // Get color index from hash map
        // -----------------------------
        int colorRGB = image.getRGB (x, y);
        Integer mapKey = Integer.valueOf (colorRGB);
        Byte colorIndex = (Byte) colorMap.get (mapKey);

        // If that fails, get closest index from color tree
        // ------------------------------------------------
        if (colorIndex == null) {
          treeKey[0] = (colorRGB & 0xff0000) >>> 16;
          treeKey[1] = (colorRGB & 0xff00) >>> 8;
          treeKey[2] = colorRGB & 0xff;
          try { colorIndex = (Byte) colorTree.nearest (treeKey); }
          catch (Exception e) { 
            throw new RuntimeException ("Error in k-d tree nearest call");
          } // catch
        } // if

        // Set color index in new image
        // ----------------------------
        byteRow[x] = colorIndex.byteValue();

      } // for
      raster.setDataElements (0, y, width, 1, byteRow);
    } // for

    return (newImage);

  } // quantizeToIndex

  ////////////////////////////////////////////////////////////

} // EarthImageWriter class

////////////////////////////////////////////////////////////////////////

