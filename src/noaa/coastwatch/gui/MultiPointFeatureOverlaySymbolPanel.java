////////////////////////////////////////////////////////////////////////
/*

     File: MultiPointFeatureOverlaySymbolPanel.java
   Author: Peter Hollemans
     Date: 2017/05/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import java.util.Vector;
import java.util.List;

import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.gui.visual.SymbolSwatch;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.Attribute;






// Testing
import noaa.coastwatch.gui.visual.MultiPointFeatureOverlayPropertyChooser;

/** 
 * A <code>MultiPointFeatureOverlaySymbolPanel</code> shows a set of symbols
 * and an attribute value table for features displayed by a 
 * {@link MultiPointFeatureOverlay} object.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class MultiPointFeatureOverlaySymbolPanel
  extends JPanel {

  // Variables
  // ---------
  
  /** The overlay to display features from in this panel. */
  private MultiPointFeatureOverlay overlay;

  /** The list of symbol overlays. */
  private JList<PointFeatureOverlay> overlayList;

  /** The label displaying the number of matching features. */
  private JLabel matchingFeaturesLabel;

  /** The list of matching features. */
  private List<Feature> matchingFeatures;

  /** The list of feature attributes. */
  private List<Attribute> featureAttributes;







  ////////////////////////////////////////////////////////////

  /** Paints an icon as it would appear in a rendered PointFeatureOverlay. */
  private class OverlayIcon
    extends SymbolSwatch {

    private Color lineColor;
    private Color fillColor;

    public OverlayIcon (
      PointFeatureOverlay overlay,
      int size
    ) {

      super (overlay.getSymbol(), size);
      lineColor = overlay.getColor();
      fillColor = overlay.getFillColor();
    
    } // OverlayIcon constructor
  
    @Override
    public void paintIcon (Component c, Graphics g, int x, int y) {

      Graphics2D g2d = (Graphics2D) g;
      Stroke saved = g2d.getStroke();

      g2d.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
      g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      symbol.setSize (size - 4);
      symbol.setBorderColor (lineColor);
      symbol.setFillColor (fillColor);
      symbol.draw (g2d, x+size/2, y+size/2);

      g2d.setStroke (saved);

    } // paintIcon

  } // OverlayIcon class

  ////////////////////////////////////////////////////////////

  /** Renders the lines in the list of symbol overlays. */
  private class SymbolOverlayRenderer
    extends JLabel
    implements ListCellRenderer<PointFeatureOverlay> {
    
    @Override
    public Component getListCellRendererComponent (
      JList<? extends PointFeatureOverlay> list,
      PointFeatureOverlay value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      setText (value.getName() + "  ");
      setIcon (new OverlayIcon (value, list.getFont().getSize()));

      if (isSelected) {
        setBackground (list.getSelectionBackground());
        setForeground (list.getSelectionForeground());
      } // if
      else {
        setBackground (list.getBackground());
        setForeground (list.getForeground());
      } // else
      setEnabled (list.isEnabled());
      setFont (list.getFont());
      setOpaque (true);

      return (this);
      
    } // getListCellRendererComponent
     
  } // SymbolOverlayRenderer class

  ////////////////////////////////////////////////////////////

  /** Provides data for the feature table display. */
  private class FeatureTableModel
    extends AbstractTableModel {
    
    ////////////////////////////////////////////////////////

    @Override
    public int getRowCount() { return (matchingFeatures.size()); }

    ////////////////////////////////////////////////////////

    @Override
    public int getColumnCount () { return (featureAttributes.size()); }

    ////////////////////////////////////////////////////////

    @Override
    public Object getValueAt (int row, int column) {
    
      return (matchingFeatures.get (row).getAttribute (column));

    } // getValueAt

    ////////////////////////////////////////////////////////

    @Override
    public String getColumnName (int column) {
    
      return (featureAttributes.get (column).getName());
    
    } // getColumnName

    ////////////////////////////////////////////////////////

  } // FeatureTableModel class

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the matching features table based on the currently selected
   *
   */
  private void updateMatchingFeatures () {

    PointFeatureOverlay pointOverlay = overlayList.getSelectedValue();
    if (pointOverlay != null) {
    
    



    
    } // if
    else {
    

    
    
    } // else




  
  } // updateMatchingFeatures

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new symbol display panel.
   *
   * @param overlay the overlay to display.
   */
  public MultiPointFeatureOverlaySymbolPanel (
    MultiPointFeatureOverlay overlay
  ) {

    this.overlay = overlay;
    
    // Setup layout
    // ------------
    setLayout (new BorderLayout());

    // Create left panel
    // -----------------
    JPanel leftPanel = new JPanel (new BorderLayout());
    this.add (leftPanel, BorderLayout.WEST);
    leftPanel.setBorder (new TitledBorder (new EtchedBorder(), "Symbols"));

    Vector<PointFeatureOverlay> overlayVector = new Vector<> (overlay.getOverlayList());
    overlayList = new JList<PointFeatureOverlay> (overlayVector);
    overlayList.setCellRenderer (new SymbolOverlayRenderer());
    overlayList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    overlayList.addListSelectionListener (event -> updateMatchingFeatures());
    JScrollPane listScroller = new JScrollPane (overlayList);
    leftPanel.add (listScroller, BorderLayout.CENTER);

    // Create center panel
    // -------------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    this.add (centerPanel, BorderLayout.CENTER);
    centerPanel.setBorder (new TitledBorder (new EtchedBorder(), "Features"));

    JTable featureTable = new JTable (new FeatureTableModel());
    JScrollPane tableScroller = new JScrollPane (featureTable);
    centerPanel.add (tableScroller, BorderLayout.CENTER);

    matchingFeaturesLabel = new JLabel();
    centerPanel.add (matchingFeaturesLabel, BorderLayout.SOUTH);

    // Select first symbol
    // -------------------
    overlayList.setSelectionInterval (0, 0);

  } // MultiPointFeatureOverlaySymbolPanel constructor
  
  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    MultiPointFeatureOverlay overlay = MultiPointFeatureOverlayPropertyChooser.createTestOverlay();
    MultiPointFeatureOverlaySymbolPanel panel = new MultiPointFeatureOverlaySymbolPanel (overlay);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlaySymbolPanel class

////////////////////////////////////////////////////////////////////////

