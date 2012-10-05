////////////////////////////////////////////////////////////////////////
/*
     FILE: MultilayerBitmaskOverlay.java
  PURPOSE: An overlay for bitmasked variable data with multiple layers.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/05
  CHANGES: 2004/04/04, PFH, added special serialization methods
           2004/08/30, PFH, added special case for Java VMs that do
             not support binary image with transparent color
           2004/10/17, PFH
           - added invalidate(), getGrid()
           - changed to extend GridContainerOverlay
           - modified to use EarthDataView.hasCompatibleCaches()
           2006/01/16, PFH, added check for null color in bitmask overlays
           2006/07/10, PFH, added TransparentOverlay interface
           2007/11/05, PFH, modified getGridList() to return non-null

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>MultilayerBitmaskOverlay</code> class uses a set of
 * <code>BitmaskOverlay</code> objects to colour subsets of bits in
 * the bit mask with different colors.  The individual bit mask
 * overlay colors are used in rendering, but the main overlay color is
 * ignored.  The layer values and visibility of each bit mask overlay
 * are also taken into account when rendering, but the inverse flag is
 * ignored.  It is assumed that each <code>BitmaskOverlay</code>
 * object uses the same data grid variable.
 */
public class MultilayerBitmaskOverlay 
  extends EarthDataOverlay
  implements TransparentOverlay, GridContainerOverlay {

  // Constants
  // ---------

  /** The serialization constant. */
  static final long serialVersionUID = 1833669752906042660L;

  // Variables
  // ---------

  /** The list of overlays to use for rendering. */
  private LinkedList overlayList;

  /** The list of bit-valued images for each overlay. */
  private transient LinkedList imageList;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the data source for grid data.  The reader and variable list
   * must contain a data grid with the current grid name.
   *
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public void setDataSource (
    EarthDataReader reader,
    List variableList
  ) {

    for (Iterator iter = overlayList.iterator(); iter.hasNext(); )
      ((BitmaskOverlay) iter.next()).setDataSource (reader, variableList);

  } // setDataSource

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the reader used to fetch the data for the bitmasks, or null
   * if no reader was explicitly given to the bitmask constructor.
   */
  public EarthDataReader getReader () { 

    return (((BitmaskOverlay) overlayList.get (0)).getReader());

  } // getReader

  ////////////////////////////////////////////////////////////

  /** Gets the possible grid variable names. */
  public List getGridNameValues () { 

    return (((BitmaskOverlay) overlayList.get (0)).getGridNameValues());

  } // getGridNameValues

  ////////////////////////////////////////////////////////////

  /** Gets the grid variable name. */
  public String getGridName () { 

    return (((BitmaskOverlay) overlayList.get (0)).getGridName());

  } // getGridName

  ////////////////////////////////////////////////////////////

  /** Gets the active grid variable. */
  public Grid getGrid () { 

    return (((BitmaskOverlay) overlayList.get (0)).getGrid());

  } // getGrid

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the grid variable in each bitmask overlay based on the
   * name. 
   */
  public void setGridName (String name) { 

    for (Iterator iter = overlayList.iterator(); iter.hasNext(); ) {
      BitmaskOverlay overlay = (BitmaskOverlay) iter.next();
      overlay.setGridName (name);
    } // for
    invalidate();

  } // setGridName

  ////////////////////////////////////////////////////////////

  /** Clears the list of overlays. */
  public void clearOverlays () {

    overlayList.clear();
    invalidate();

  } // clearOverlays

  ////////////////////////////////////////////////////////////

  /** Adds a list of overlays to the list. */
  public void addOverlays (List overlays) {

    overlayList.addAll (overlays);
    invalidate();

  } // addOverlays

  ////////////////////////////////////////////////////////////
  
  /** Adds a new bit mask overlay to the list. */
  public void addOverlay (
    BitmaskOverlay overlay
  ) {

    overlayList.add (overlay);
    invalidate();

  } // addOverlay

  ////////////////////////////////////////////////////////////

  /** Removes an overlay from the list. */
  public void removeOverlay (
    BitmaskOverlay overlay
  ) {

    overlayList.remove (overlay);
    invalidate();

  } // removeOverlay

  ////////////////////////////////////////////////////////////

  /** Gets the current list of overlays. */
  public List getOverlays () { return ((List) overlayList.clone()); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new complex bitmask overlay.  The layer number is
   * initialized to 0.
   */
  public MultilayerBitmaskOverlay () {

    super (null);
    overlayList = new LinkedList();
    imageList = null;

  } // MultilayerBitmaskOverlay constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the internal image buffers to reflect any changes made in
   * the bitmasks.  Currently, only color changes in the bitmasks are
   * recognized.
   */
  public void updateBitmasks () { 

    // Check image list
    // ----------------
    if (imageList == null) return;

    // Loop over each overlay
    // ----------------------
    for (int i = 0; i < overlayList.size(); i++) {

      // Get overlay and image colors
      // ----------------------------
      BitmaskOverlay overlay = (BitmaskOverlay) overlayList.get(i);
      BufferedImage image = (BufferedImage) imageList.get (i);
      int modelRgb = ((IndexColorModel) image.getColorModel()).getRGB (1);
      Color overlayColor = overlay.getColor();
      int overlayRgb = (overlayColor == null ? ~modelRgb : 
        overlayColor.getRGB());

      // Replace color model if needed
      // -----------------------------
      if (modelRgb != overlayRgb) {
        BufferedImage newImage = new BufferedImage (
          BitmaskOverlay.createColorModel (overlay.getColor(), false),
          image.getRaster(), false, null);
        imageList.set (i, newImage);
      } // if

    } // for

  } // updateBitmasks

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Initialize image properties
    // ---------------------------
    ImageTransform imageTrans = view.getTransform().getImageTransform();
    Dimension imageDims = imageTrans.getImageDimensions();

    // Prepare for rendering
    // ---------------------
    imageList = new LinkedList();
    int overlays = overlayList.size();
    WritableRaster[] rasterArray = new WritableRaster[overlays];
    int[] maskArray = new int[overlays];
    boolean useBinary = GraphicsServices.supportsBinaryWithTransparency (g);
    for (int i = 0 ; i < overlays; i++) {

      // Create color model
      // ------------------
      BitmaskOverlay overlay = (BitmaskOverlay) overlayList.get (i);
      IndexColorModel colorModel = 
        BitmaskOverlay.createColorModel (overlay.getColor(), false);

      // Create 1-bit or 8-bit image
      // ---------------------------
      BufferedImage image;
      if (useBinary) {
        image = new BufferedImage (imageDims.width, 
          imageDims.height, BufferedImage.TYPE_BYTE_BINARY, colorModel);
      } // if
      else {
        image = new BufferedImage (imageDims.width, 
          imageDims.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
      } // else
      imageList.add (image);
      rasterArray[i] = image.getRaster();

      // Get mask value
      // --------------
      maskArray[i] = overlay.getMask();

    } // for

    // Create byte rows and get grid
    // -----------------------------
    byte[][] byteRows = new byte[overlays][imageDims.width];
    Grid grid = ((BitmaskOverlay) overlayList.get (0)).getGrid();
    int value;

    // Render using image transform
    // ----------------------------
    if (!view.hasCompatibleCaches (grid)) {
      Point point = new Point();
      for (point.y = 0; point.y < imageDims.height; point.y++) {

        // Render line
        // -----------
        for (point.x = 0; point.x < imageDims.width; point.x++) {
          value = (int) grid.getValue (imageTrans.transform (point));
          for (int k = 0; k < overlays; k++) {
            byteRows[k][point.x] = 
              (byte) ((value & maskArray[k]) == 0 ? 0 : 1);
          } // for
        } // for

        // Set data elements in raster
        // ---------------------------
        for (int k = 0; k < overlays; k++) {
          rasterArray[k].setDataElements (0, point.y, imageDims.width, 1, 
            byteRows[k]);
        } // for

      } // for
    } // if

    // Render using cached coordinates
    // -------------------------------
    else {
      int lastGridRow = Integer.MIN_VALUE;
      byte[] byteValues = new byte[overlays];
      for (int y = 0; y < imageDims.height; y++) {

        // Render line
        // -----------
        if (view.rowCache[y] != lastGridRow) {
          int lastGridCol = Integer.MIN_VALUE;
          for (int x = 0; x < imageDims.width; x++) {
            if (view.colCache[x] != lastGridCol) {
              value = (int) grid.getValue (view.rowCache[y], view.colCache[x]);
              for (int k = 0; k < overlays; k++) {
                byteValues[k] = (byte) ((value & maskArray[k]) == 0 ? 0 : 1);
              } // for
            } // if
            for (int k = 0; k < overlays; k++) {
              byteRows[k][x] = byteValues[k];
            } // for
          } // for
          lastGridRow = view.rowCache[y];
        } // if

        // Set data elements in raster
        // ---------------------------
        for (int k = 0; k < overlays; k++) {
          rasterArray[k].setDataElements (0, y, imageDims.width, 1, 
            byteRows[k]);
        } // for

      } // for
    } // else

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Sort visible overlays
    // ---------------------
    Map overlayMap = new HashMap();
    List visibleList = new ArrayList();
    int overlays = overlayList.size();
    for (int i = 0; i < overlays ; i++) {
      BitmaskOverlay overlay = (BitmaskOverlay) overlayList.get (i);
      if (overlay.getVisible() && overlay.getColor() != null) {
        visibleList.add (overlay);
        overlayMap.put (overlay, imageList.get (i));
      } // if
    } // for
    BitmaskOverlay[] overlayArray = 
      (BitmaskOverlay[]) visibleList.toArray (new BitmaskOverlay[]{});
    Arrays.sort (overlayArray);

    // Draw images
    // -----------
    for (int i = 0; i < overlayArray.length; i++) {
      Image image = (Image) overlayMap.get (overlayArray[i]);
      g.drawImage (image, 0, 0, null);
    } // for

  } // draw

  ////////////////////////////////////////////////////////////

  /** Creates and returns a copy of this object. */
  public Object clone () {

    MultilayerBitmaskOverlay overlay = 
      (MultilayerBitmaskOverlay) super.clone();
    overlay.overlayList = new LinkedList();
    for (Iterator iter = overlayList.iterator(); iter.hasNext(); ) 
      overlay.overlayList.add (((BitmaskOverlay) iter.next()).clone());
    if (imageList != null)
      overlay.imageList = (LinkedList) imageList.clone();
    return (overlay);

  } // clone

  ////////////////////////////////////////////////////////////

  /**
   * Invalidates the overlay.  This causes the bitmask graphics to be
   * completely reconstructed upon the next call to
   * <code>render()</code>.
   */
  public void invalidate () {

    prepared = false;
    imageList = null;

  } // invalidate

  ////////////////////////////////////////////////////////////

  public List<Grid> getGridList () { 

    return (Arrays.asList (new Grid[] {getGrid()})); 

  } // getGridList

  ////////////////////////////////////////////////////////////

} // MultilayerBitmaskOverlay class

////////////////////////////////////////////////////////////////////////
