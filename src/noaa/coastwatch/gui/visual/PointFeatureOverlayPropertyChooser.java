////////////////////////////////////////////////////////////////////////
/*

     File: PointFeatureOverlayPropertyChooser.java
   Author: Peter Hollemans
     Date: 2017/02/09

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Box;

import noaa.coastwatch.gui.SelectionRuleFilterChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.VisualObject;

import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.NumberRule;
import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.PlotSymbolFactory;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.SelectionRuleFilter.FilterMode;

/** 
 * A <code>PointFeatureOverlayPropertyChooser</code> is an
 * {@link OverlayPropertyChooser} that handles {@link PointFeatureOverlay} 
 * objects.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class PointFeatureOverlayPropertyChooser
  extends OverlayPropertyChooser<PointFeatureOverlay<SimpleSymbol>> {

  // Constants
  // ---------

  /** The title used for the panel border. */
  private static String PANEL_TITLE = "Point";

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new chooser panel.
   *
   * @param overlay the overlay to manipulate.
   */
  public PointFeatureOverlayPropertyChooser (
    PointFeatureOverlay<SimpleSymbol> overlay
  ) {

    super (overlay);

    // Setup layout
    // ------------
    setLayout (new BorderLayout());

    // Add description label and field
    // -------------------------------
    JPanel topPanel = new JPanel (new FlowLayout (FlowLayout.LEFT, 2, 2));
    topPanel.add (new JLabel ("Description:"));
    JTextField field = (JTextField) VisualObjectFactory.create (overlay, "name").getComponent();
    field.setColumns (20);
    topPanel.add (field);
    this.add (topPanel, BorderLayout.NORTH);
    
    // Add filter chooser
    // ------------------
    PointFeatureSource source = overlay.getSource();
    if (source.getAttributeCount() != 0) {
      SelectionRuleFilterChooser filterChooser =
        new SelectionRuleFilterChooser (source.getAttributes(), source.getAttributeNameMap());
      var filter = overlay.getFilter();
      if (filter != null) filterChooser.setFilter (filter);
      this.add (filterChooser, BorderLayout.CENTER);
    } // if

    // Add symbol chooser
    // ------------------
    Box bottomPanel = Box.createVerticalBox();
    bottomPanel.add (Box.createVerticalStrut (4));
    JLabel label = new JLabel ("Use the following to display the data points:");
    label.setAlignmentX (0);
    bottomPanel.add (label);
    bottomPanel.add (Box.createVerticalStrut (2));
    JPanel symbolPanel = new JPanel (new FlowLayout (FlowLayout.LEFT, 2, 0));
    symbolPanel.setAlignmentX (0);
    symbolPanel.add (Box.createHorizontalStrut (20));
    symbolPanel.add (new JLabel ("Symbol:"));
    var visualList = new ArrayList<VisualObject>();
    var visual = VisualObjectFactory.create (overlay.getSymbol(), "plotSymbol");
    visualList.add (visual);
    symbolPanel.add (visual.getComponent());
    symbolPanel.add (Box.createHorizontalStrut (5));
    symbolPanel.add (new JLabel ("Drawing color:"));
    visual = VisualObjectFactory.create (overlay, "color");
    visualList.add (visual);
    symbolPanel.add (visual.getComponent());
    symbolPanel.add (Box.createHorizontalStrut (5));
    symbolPanel.add (new JLabel ("Fill color:"));
    visual = VisualObjectFactory.create (overlay, "fillColor");
    visualList.add (visual);
    symbolPanel.add (visual.getComponent());
    bottomPanel.add (symbolPanel);
    this.add (bottomPanel, BorderLayout.SOUTH);
  
    // Add listeners to the visual objects so that they signal a
    // property change when they're updated.
    visualList.forEach (obj -> obj.addPropertyChangeListener (
      event -> firePropertyChange (OVERLAY_PROPERTY, null, overlay)));

  } // PointFeatureOverlayPropertyChooser constructor
  
  ////////////////////////////////////////////////////////////

  @Override
  protected String getTitle () { return (PANEL_TITLE); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    SimpleSymbol symbol = new SimpleSymbol (PlotSymbolFactory.create ("Circle"));
    symbol.setBorderColor (Color.WHITE);
    symbol.setFillColor (new Color (145, 22, 22));

    final List<Attribute> attList = new ArrayList<Attribute>();
    attList.add (new Attribute ("platform_type", Byte.class, ""));
    attList.add (new Attribute ("platform_id", String.class, ""));
    attList.add (new Attribute ("sst", Double.class, ""));
    attList.add (new Attribute ("quality_level", Byte.class, ""));
    attList.add (new Attribute ("time", Date.class, ""));
    PointFeatureSource source = new PointFeatureSource () {
      {
        setAttributes (attList);
      }
      protected void select () throws IOException { }
    };

    Map<String, Integer> attNameMap = source.getAttributeNameMap();
    NumberRule platformRule = new NumberRule ("platform_type", attNameMap, (byte) 8);
    platformRule.setOperator (NumberRule.Operator.IS_EQUAL_TO);
    SelectionRuleFilter filter = new SelectionRuleFilter();
    filter.add (platformRule);

    PointFeatureOverlay overlay = new PointFeatureOverlay (symbol, source);
    overlay.setFilter (filter);
    overlay.setName ("CRW");
    PointFeatureOverlayPropertyChooser chooser = new PointFeatureOverlayPropertyChooser (overlay);

    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (overlay)));

  } // main

  ////////////////////////////////////////////////////////////

} // PointFeatureOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////

  
  
  
  
