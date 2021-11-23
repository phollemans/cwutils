////////////////////////////////////////////////////////////////////////
/*

     File: HybridView.java
   Author: Peter Hollemans
     Date: 2021/11/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2021 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import noaa.coastwatch.util.DataLocation;

/**
 * The <b>HybridView</b> class is an earth data view that combines a set
 * of data views.  The hybrid view is a combination of data view layers with
 * masks that control the transparency of each layer.<p>
 *
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class HybridView extends EarthDataView {

  private static final Logger LOGGER = Logger.getLogger (HybridView.class.getName());

  // Variables
  // ---------

  /** The list of views in this hybrid. */
  private List<EarthDataView> viewList;

  /** The list of masks in this hybrid, one for each view. */
  private List<MaskOverlay> maskList;

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new hybrid view.
   *
   * @param viewList the list of views to create a hybrid.  The views
   * are composited starting with index 0.
   * @param maskList the list of mask overlays that determine which of the
   * view pixels should be used in the hybrid.  If a mask overlay pixel is
   * active, the corresponding view pixel is made transparent.  If a mask
   * overlay is null, the image view is not masked.
   *
   * @throws NoninvertibleTransformException if the resulting image
   * transform is not invertible.
   */
  public HybridView (
    List<EarthDataView> viewList,
    List<MaskOverlay> maskList
  ) {

    super (viewList.get (0));
    this.viewList = viewList;
    this.maskList = maskList;

  } // HybridView

  ////////////////////////////////////////////////////////////

  // These various methods override the functionality of the superclass
  // to pass on the changes to the various views in the list.

  @Override
  public void setImageAffine (
    AffineTransform affine
  ) {
    super.setImageAffine (affine);
    for (var view : viewList) view.setImageAffine (affine);
  }

  @Override
  public void setVerbose (boolean flag) {
    super.setVerbose (flag);
    for (var view : viewList) view.setVerbose (flag);
  }

  @Override
  public void invalidate () {
    super.invalidate();
    for (var view : viewList) view.invalidate();
  }

  @Override
  public void setCenter (
    DataLocation center
  ) throws NoninvertibleTransformException {
    super.setCenter (center);
    for (var view : viewList) view.setCenter (center);
  }

  @Override
  public void setCenterAndScale (
    DataLocation center,
    double scale
  ) throws NoninvertibleTransformException {
    super.setCenterAndScale (center, scale);
    for (var view : viewList) view.setCenterAndScale (center, scale);
  }

  @Override
  public void resize (
    double factor
  ) throws NoninvertibleTransformException {
    super.resize (factor);
    for (var view : viewList) view.resize (factor);
  }

  @Override
  public void reset () throws NoninvertibleTransformException {
    super.reset();
    for (var view : viewList) view.reset();
  }

  @Override
  public void setSize (
    Dimension imageDims
  ) throws NoninvertibleTransformException {
    super.setSize (imageDims);
    for (var view : viewList) view.setSize (imageDims);
  }

  // For a legend, we pass back the first legend encountered in a view,
  // or null if none of the views have legends.
  @Override
  public Legend getLegend () {
    Legend legend = null;
    for (var view : viewList) {
      var viewLegend = view.getLegend();
      if (viewLegend != null) { legend = viewLegend; break; }
    } // for
    return (legend);
  }

  @Override
  public Object clone() { return (null); }

  ////////////////////////////////////////////////////////////

  @Override
  protected void prepare (
    Graphics2D g
  ) {

    // Create a new image with 24-bit RGB color (ie: no alpha).  This
    // is the final image that results from the hybrid composite of all
    // view images with all masks.
    image = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_INT_RGB);
    var graph = image.createGraphics();

    // Create a new image that each mask can render to and that we can
    // then use to filter pixels in each hybrid layer.
    var maskImage = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_BYTE_BINARY);
    var maskGraph = maskImage.createGraphics();
    var maskRaster = maskImage.getRaster();
    
    // Create a new image with 24-bit ARGB color for rendering each view
    // prior to rendering to the final view image.
    var viewImage = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_INT_ARGB);
    var viewGraph = viewImage.createGraphics();
    
    // Loop over each mask/view combination and render them to the final image
    int[] rgbRow = new int[imageDims.width];
    byte[] byteRow = new byte[imageDims.width];

    for (int i = 0; i < viewList.size(); i++) {

      var view = viewList.get (i);
      var mask = maskList.get (i);

      // First render the view and mask overlay separately to temporary images
      viewGraph.setColor (Color.BLACK);
      viewGraph.fillRect (0, 0, imageDims.width, imageDims.height);
      view.render (viewGraph);

      if (mask != null) {
        maskGraph.setColor (Color.BLACK);
        maskGraph.fillRect (0, 0, imageDims.width, imageDims.height);
        mask.render (maskGraph, this);
      } // if

      // Next, make the view image transparent where the mask overlay is
      // active
      if (mask != null) {

        for (int y = 0; y < imageDims.height; y++) {
          viewImage.getRGB​ (0, y, imageDims.width, 1, rgbRow, 0, imageDims.width);
          maskRaster.getDataElements (0, y, imageDims.width, 1, byteRow);
          for (int x = 0; x < imageDims.width; x++) {
            if (byteRow[x] != 0) rgbRow[x] = 0;
          } // for
          viewImage.setRGB (0, y, imageDims.width, 1, rgbRow, 0, imageDims.width);
        } // for

      } // if

      // Finally, add the view image to the final hybrid image
      graph.drawImage (viewImage, 0, 0, null);

    } // for

  } // prepare

  ////////////////////////////////////////////////////////////

} // HybridView class

////////////////////////////////////////////////////////////////////////

