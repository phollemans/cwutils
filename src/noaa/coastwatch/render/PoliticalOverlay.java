////////////////////////////////////////////////////////////////////////
/*

     File: PoliticalOverlay.java
   Author: Peter Hollemans
     Date: 2004/03/26

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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import noaa.coastwatch.render.feature.BinnedGSHHSLineReader;
import noaa.coastwatch.render.feature.BinnedGSHHSReaderFactory;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LineOverlay;
import noaa.coastwatch.util.EarthArea;

/**
 * The <code>PoliticalOverlay</code> class annotates an
 * <code>EarthDataView</code> with political border lines.  Both
 * state borders and international borders may be selected but by
 * default, only international borders are rendered.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PoliticalOverlay 
  extends LineOverlay {
  
  // Constants
  // ---------

  /** The serialization constant. */
  private static final long serialVersionUID = 2995481037661135344L;

  // Variables
  // ---------

  /** The state flag, true to show state borders. */
  private boolean showState;

  /** The international flag, true to show international borders. */
  private boolean showInternational = true;

  /** The binned GSHHS reader factory used for border data. */
  private transient BinnedGSHHSReaderFactory readerFactory =
    BinnedGSHHSReaderFactory.getInstance();

  /** The current GSHHS reader used for border data. */
  private transient BinnedGSHHSLineReader border;

  ////////////////////////////////////////////////////////////

  /** Sets the read factory for new border readers. */
  public void setReaderFactory (BinnedGSHHSReaderFactory factory) {

    this.readerFactory = factory;

  } // setReaderFactory

  ////////////////////////////////////////////////////////////

  /** Reads the object data from the input stream. */
  private void readObject (
    ObjectInputStream in
  ) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    readerFactory = BinnedGSHHSReaderFactory.getInstance();

  } // readObject

  ////////////////////////////////////////////////////////////

  /** Gets the state borders flag. */
  public boolean getState () { return (showState); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the state borders flag.  By default, no state borders are
   * drawn.
   */
  public void setState (boolean flag) { 

    showState = flag; 

  } // setState

  ////////////////////////////////////////////////////////////

  /** Gets the international borders flag. */
  public boolean getInternational () { return (showInternational); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the international borders flag.  By default, international
   * borders are drawn.
   */
  public void setInternational (boolean flag) { 

    showInternational = flag; 

  } // setInternational

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new political overlay with the specified color.  By
   * default, only international borders are shown.  The layer number
   * is initialized to 0, and the stroke to the default
   * <code>BasicStroke</code>
   *
   * @param color the overlay color.
   */
  public PoliticalOverlay (
    Color color
  ) { 

    super (color);
    border = null;

  } // PoliticalOverlay

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check for any borders showing
    // -----------------------------
    if (!showState && !showInternational) { border = null; return; }
 
    // Get required parameters
    // -----------------------
    EarthArea viewArea = view.getArea();
    double res = view.getResolution();
    String viewDatabase = readerFactory.getDatabaseName (
      BinnedGSHHSReaderFactory.BORDER, 
      BinnedGSHHSReaderFactory.getDatabaseLevel (res)
    );

    // Check if border reload required
    // -------------------------------
    if (border != null && prepared) {
      EarthArea selectedArea = border.getArea();
      String selectedDatabase = border.getDatabase();
      if (viewDatabase.equals (selectedDatabase)) {
        if (viewArea.equals (selectedArea))
          return;
      } // if
      else border = null;
    } // if

    // Load border data
    // ----------------
    try {
      if (border == null) 
        border = readerFactory.getLineReader (viewDatabase);
      border.select (viewArea);
    } // try
    catch (Exception e) { e.printStackTrace(); border = null; }

  } // prepare

  ////////////////////////////////////////////////////////////

  protected void draw (
    Graphics2D g,
    EarthDataView view
  ) {

    // Check border
    // ------------
    if (border == null) return;

    // Draw borders
    // ------------
    Color color = getColorWithAlpha();
    if (color != null) {
      g.setColor (color);
      g.setStroke (getStroke());
      int minLevel = (showInternational ? 1 : 2);
      int maxLevel = (showState ? 2 : 1);
      border.setLevelRange (minLevel, maxLevel);
      border.render (g, view.getTransform());
    } // if

  } // draw

  ////////////////////////////////////////////////////////////

} // PoliticalOverlay class

////////////////////////////////////////////////////////////////////////
