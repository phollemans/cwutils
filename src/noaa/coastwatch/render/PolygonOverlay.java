////////////////////////////////////////////////////////////////////////
/*
     FILE: PolygonOverlay.java
  PURPOSE: An overlay for vector polygon data with filling.
   AUTHOR: Peter Hollemans
     DATE: 2002/09/06
  CHANGES: 2005/03/21, PFH, added transparency handling
           2006/07/07, PFH, added getColors()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Stroke;
import java.util.List;
import noaa.coastwatch.render.LineOverlay;

/**
 * A polygon overlay annotes a data view with vector-specified polygon
 * lines and shapes.  The polygon overlay adds the concept of a fill
 * color to the parent class.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public abstract class PolygonOverlay 
  extends LineOverlay {

  // Constants
  // ---------

  /** The serialization constant. */
  static final long serialVersionUID = -8535512620300938216L;

  // Variables
  // ---------
  /** The fill color to use for polygon fills. */
  private Color fillColor;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the polygon fill color.  Only opaque colors are allowed.  If
   * the color has an alpha component, the alpha value is set to 255
   * and the overlay transparency is set using the new alpha value.
   * This behaviour may be changed in the future to ignore the color's
   * alpha value entirely -- overlay transparency should really be set
   * by calling {@link EarthDataOverlay#setTransparency}.
   *
   * @param fillColor the new opaque polygon fill color, or null to
   * perform no filling.
   */
  public void setFillColor (
    Color fillColor
  ) { 

    if (fillColor != null && fillColor.getAlpha() != 255) {
      this.fillColor = new Color (fillColor.getRGB());
      this.alpha = fillColor.getAlpha();
    } // if
    else
      this.fillColor = fillColor; 
  
  } // setFillColor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the polygon fill color.  The polygon fill color is always
   * opaque, even if a call to {@link #setFillColor} specifies a color
   * with an alpha component.  The overlay transparency may be
   * accessed through {@link #setTransparency} or {@link
   * EarthDataOverlay#getTransparency}.
   *
   * @return the polygon fill color.
   */
  public Color getFillColor() { return (fillColor); }

  ////////////////////////////////////////////////////////////

  /** Gets the polygon fill color with alpha component. */
  public Color getFillColorWithAlpha () { 

    return (getAlphaVersion (fillColor));

  } // getFillColorWithAlpha

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new polygon overlay.
   * 
   * @param color the overlay color.
   * @param layer the overlay layer number.  
   * @param stroke the stroke to use for vector paths.
   * @param fillColor the fill color to use for polygon fills.
   */
  protected PolygonOverlay (
    Color color,
    int layer,
    Stroke stroke,
    Color fillColor
  ) { 

    super (color, layer, stroke);
    setFillColor (fillColor);

  } // PolygonOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new polygon overlay.  The layer number is initialized
   * to 0, the stroke to the default <code>BasicStroke</code>, and the
   * fill color to the overlay color.
   * 
   * @param color the overlay color.
   */
  protected PolygonOverlay (
    Color color
  ) { 

    super (color);
    setFillColor (fillColor);

  } // PolygonOverlay constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of all the colors used by this overlay.
   * 
   * @return the list of colors.
   */
  public List getColors () {

    List colorList = super.getColors();
    colorList.add (fillColor);
    return (colorList);

  } // getColors

  ////////////////////////////////////////////////////////////

} // PolygonOverlay class

////////////////////////////////////////////////////////////////////////
