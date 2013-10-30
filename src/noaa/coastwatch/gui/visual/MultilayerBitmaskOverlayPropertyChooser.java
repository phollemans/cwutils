////////////////////////////////////////////////////////////////////////
/*
     FILE: MultilayerBitmaskOverlayPropertyChooser.java
  PURPOSE: Allows the user to edit overlay properties for multilayer
           bitmasks.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/06
  CHANGES: 2006/11/02, PFH, modified to remove use of getOverlay()
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.beans.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.io.*;

/** 
 * The <code>MultilayerBitmaskOverlayPropertyChooser</code> class is
 * an implementation of an <code>OverlayPropertyChooser</code> that
 * allows the user to edit the properties of a
 * <code>MultilayerBitmaskOverlay</code>.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class MultilayerBitmaskOverlayPropertyChooser 
  extends OverlayPropertyChooser {

  // Constants
  // ---------

  /** The title used for the panel border. */
  private static String PANEL_TITLE = "Multilayer";

  /** The mask size labels. */
  private static String[] SIZE_LABELS = new String[] {
    "8 bits",
    "16 bits",
    "24 bits",
    "32 bits"
  };

  /** The default bitmask colors. */
  private static Color[] BITMASK_COLORS = new Color[] {
    new Color (0,102,255),
    new Color (102,0,255),
    new Color (255,0,204),
    new Color (255,0,0),
    new Color (255,204,0),
    new Color (102,255,0),
    new Color (153,153,0),
    new Color (153,0,153)
  };

  // Variables
  // ---------

  /** The list used for storing all bitmask overlays. */
  private BitmaskListPanel listPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new generic overlay property chooser panel.  The
   * assumption is that the number of bitmasks in the list is a
   * multiple of 8.
   */
  public MultilayerBitmaskOverlayPropertyChooser (
    MultilayerBitmaskOverlay multilayer
  ) {

    super (multilayer);

    // Setup layout
    // ------------
    setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    // Add mask variable combo
    // -----------------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Mask variable:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);

    JComboBox variableCombo = 
      new JComboBox (multilayer.getGridNameValues().toArray());
    variableCombo.setSelectedItem (multilayer.getGridName());
    variableCombo.addActionListener (new VariableSelectionListener());
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.NONE, 1, 0);
    this.add (variableCombo, gc);

    // Add mask size combo
    // -------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Mask size:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);

    JComboBox sizeCombo = new JComboBox (SIZE_LABELS);
    List overlayList = multilayer.getOverlays();
    sizeCombo.setSelectedIndex (overlayList.size()/8 - 1);
    sizeCombo.addActionListener (new SizeSelectionListener());
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.NONE, 1, 0);
    this.add (sizeCombo, gc);

    // Add overlay list chooser
    // ------------------------
    GUIServices.setConstraints (gc, 0, 2, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (new JLabel ("Layers:"), gc);

    listPanel = new BitmaskListPanel (overlayList);
    listPanel.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          ((MultilayerBitmaskOverlay) overlay).updateBitmasks();
          firePropertyChange (OVERLAY_PROPERTY, null, overlay);
        } // propertyChange
      });
    listPanel.setVisibleRowCount (8);
    GUIServices.setConstraints (gc, 0, 3, 2, 1, 
      GridBagConstraints.BOTH, 1, 1);
    this.add (listPanel, gc);

  } // MultilayerBitmaskOverlayPropertyChooser constructor

  ////////////////////////////////////////////////////////////

  /** Changes the active mask size. */
  private class SizeSelectionListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Set grid name
      // -------------
      int maskSize = (((JComboBox) event.getSource()).getSelectedIndex()+1)*8;
      MultilayerBitmaskOverlay multilayer = (MultilayerBitmaskOverlay) overlay;
      List variableList = multilayer.getGridNameValues();
      EarthDataReader reader = multilayer.getReader();
      String gridName = multilayer.getGridName();
      multilayer.clearOverlays();
      List overlayList = createBitmaskList (1, maskSize, reader, variableList, 
        gridName);
      multilayer.addOverlays (overlayList);
      listPanel.setOverlays (overlayList);

      // Fire change event
      // -----------------
      firePropertyChange (OVERLAY_PROPERTY, null, multilayer);

    } // actionPerformed
  } // SizeSelectionListener class

  ////////////////////////////////////////////////////////////

  /** Changes the active variable. */
  private class VariableSelectionListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Set grid name
      // -------------
      String name = (String) ((JComboBox) event.getSource()).getSelectedItem();
      ((MultilayerBitmaskOverlay) overlay).setGridName (name);

      // Fire change event
      // -----------------
      firePropertyChange (OVERLAY_PROPERTY, null, overlay);

    } // actionPerformed
  } // VariableSelectionListener class

  ////////////////////////////////////////////////////////////

  /** Implements a list panel of bitmask overlays. */
  private class BitmaskListPanel 
    extends AbstractOverlayListPanel {

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new list panel with hide, show and rearrange
     * buttons. 
     *
     * @param overlayList the initial list of overlays to show.
     */
    public BitmaskListPanel (
      List overlayList
    ) {

      super (false, false, true, true, false);
      addOverlays (overlayList);

    } // BitmaskListPanel

    ////////////////////////////////////////////////////////

    /** 
     * Sets the list of bitmask overlays to the specified list.  All
     * previous bitmask overlays are removed.  No property change
     * events are fired from the list.
     */
    public void setOverlays (
      List overlayList
    ) {

      removeOverlays();
      addOverlays (overlayList);

    } // setOverlays

    ////////////////////////////////////////////////////////

    /** Returns null so that no add buttons are used. */
    protected List getAddButtons () { return (null); }

    ////////////////////////////////////////////////////////

    /** Returns null so that no title is used. */
    protected String getTitle () { return (null); }

    ////////////////////////////////////////////////////////

  } // BitmaskListPanel

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a list of bitmask overlays using the specified starting
   * bit and length.  Each bitmask is given a unique name according to
   * the bit number it represents, as well as a unique color from an
   * arrangement of unique predefined colors.  The overlays are
   * initially assigned layer values such that the lowest bit value is
   * rendered last.  Each bitmask is given the integer mask that
   * coincides with its bit number, for example, bit 1 will have 0x01
   * as a mask, bit 2 will have 0x02, bit 3 0x04, and so on.
   *
   * @param startBit the starting bit value, in the range [1..32].
   * @param length the number of desired bitmask overlays.
   * @param reader the reader to use for bitmask data.
   * @param variableList the list of variables to use for bitmask data.
   * @param gridName the initial bitmask grid name.
   */
  public static List createBitmaskList (
    int startBit,
    int length,
    EarthDataReader reader,
    List variableList,
    String gridName
  ) {

    // Loop over each bit value
    // ------------------------
    List bitmaskList = new ArrayList();
    int endBit = startBit+length-1;
    for (int i = startBit; i <= endBit; i++) {

      // Create bitmask overlay
      // ----------------------
      int mask = 0x01 << (i-1);
      BitmaskOverlay overlay = new BitmaskOverlay (BITMASK_COLORS[(i-1)%8], 
        reader, variableList, gridName, mask);

      // Set attributes
      // --------------
      overlay.setName ("Bit " + i);
      overlay.setLayer (endBit-(i-startBit));

      // Add to list
      // -----------
      bitmaskList.add (overlay);

    } // for

    return (bitmaskList);

  } // createBitmaskList

  ////////////////////////////////////////////////////////////

  /** Gets the chooser panel title. */
  protected String getTitle () {

    return (PANEL_TITLE);

  } // getTitle

  ////////////////////////////////////////////////////////////

} // MultilayerBitmaskOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////
