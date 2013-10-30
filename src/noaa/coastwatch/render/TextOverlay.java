////////////////////////////////////////////////////////////////////////
/*
     FILE: TextOverlay.java
  PURPOSE: An overlay for text annotation elements.
   AUTHOR: Peter Hollemans
     DATE: 2004/04/24
  CHANGES: 2005/04/04, PFH, added transparency handling
           2006/12/26, PFH, modified to use dynamic shadow color

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * The <code>TextOverlay</code> class may be used to render a list of
 * text annotation elements.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class TextOverlay 
  extends EarthDataOverlay {

  // Variables
  // ---------

  /** The list of text elements to draw. */
  private List elementList;

  /** The drop shadow flag, true to draw a drop shadow. */
  private boolean textDropShadow;
  
  ////////////////////////////////////////////////////////////

  /** Gets the text drop shadow flag. */
  public boolean getTextDropShadow () { return (textDropShadow); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the text drop shadow flag.  When drop shadow mode is on, a shadow
   * is drawn behind the text.  By default, drop shadow mode
   * is off.
   */
  public void setTextDropShadow (boolean flag) { textDropShadow = flag; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the text font.  The font is set for each text element in the
   * list.
   */
  public void setFont (Font font) { 

    for (Iterator iter = elementList.iterator(); iter.hasNext(); ) {
      TextElement element = (TextElement) iter.next();
      element.setFont (font);
    } // for

  } // setFont

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the text font.  We assume that all text elements have the
   * same font, and that there is at least one text element.
   */
  public Font getFont () { 

    return (((TextElement) elementList.get (0)).getFont()); 

  } // getFont

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new text element to the list.  The base point coordinates
   * are considered to be in the data coordinate reference frame.
   *
   * @param element the new text element to add to the list.
   */
  public void addElement (
    TextElement element
  ) {

    elementList.add (element);

  } // addElement

  ////////////////////////////////////////////////////////////

  /** Gets an iterator over the list of text elements. */
  public Iterator getElementIterator () { return (elementList.iterator()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Removes an element from the list.
   *
   * @param element the element to remove from the list.
   */
  public void removeElement (
    TextElement element
  ) {

    elementList.remove (element);

  } // removeElement

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new text overlay with the specified color.  The layer
   * number is initialized to 0.
   */
  public TextOverlay (
    Color color
  ) {

    // Initialize
    // ----------
    super (color);
    elementList = new LinkedList();

  } // TextOverlay constructor

  ////////////////////////////////////////////////////////////

  /** Returns false as this class needs no preparation. */
  protected boolean needsPrepare () { return (false); }

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // do nothing

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Setup for drawing
    // -----------------
    AffineTransform at = view.getTransform().getImageTransform().getAffine();
    Color fore = getColorWithAlpha();
    if (fore == null) return;
    Color back = (textDropShadow ? 
      getAlphaVersion (LineOverlay.getShadowColor (fore)) : null);

    // Draw text elements
    // ------------------
    for (Iterator iter = elementList.iterator(); iter.hasNext(); ) {
      TextElement element = (TextElement) iter.next();
      Point2D base = element.getBasePoint();
      Point2D newBase = at.transform (base, null);
      element.setBasePoint (newBase);
      element.render (g, fore, back);
      element.setBasePoint (base);
    } // for

  } // draw

  ////////////////////////////////////////////////////////////

} // TextOverlay class

////////////////////////////////////////////////////////////////////////
