////////////////////////////////////////////////////////////////////////
/*

     File: MultiPointFeatureOverlayPropertyChooser.java
   Author: Peter Hollemans
     Date: 2017/02/12

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.beans.PropertyChangeEvent;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.Box;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.io.IOException;

import noaa.coastwatch.gui.AbstractOverlayListPanel;
import noaa.coastwatch.gui.SelectionRuleFilterChooser;
import noaa.coastwatch.gui.FeatureGroupFilterChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.PointFeatureOverlayPropertyChooser;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.MultiPointFeatureOverlaySymbolPanel;
import noaa.coastwatch.gui.MultiPointFeatureOverlayStatsPanel;

import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.NumberRule;
import noaa.coastwatch.render.feature.DateRule;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.FeatureGroupFilter;
import noaa.coastwatch.render.feature.TimeWindow;
import noaa.coastwatch.render.feature.TimeWindowRule;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.PlotSymbolFactory;

import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.EarthArea;

/** 
 * A <code>MultiPointFeatureOverlayPropertyChooser</code> is an
 * {@link OverlayPropertyChooser} that handles {@link MultiPointFeatureOverlay}
 * objects.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class MultiPointFeatureOverlayPropertyChooser
  extends OverlayPropertyChooser<MultiPointFeatureOverlay<SimpleSymbol>> {

  // Constants
  // ---------

  /** The title used for the panel border. */
  private static String PANEL_TITLE = "Multi Point";

  // Variables
  // ---------
  
  /** The list panel of point overlays. */
  private PointFeatureOverlayListPanel listPanel;

  /** The buttons for manipulating entries in the list. */
  private JButton addButton;
  private JButton editButton;
  private JButton dupButton;
  private JButton removeButton;

  /** The source for point data. */
  private PointFeatureSource source;
  
  /** The chooser for group filter. */
  private FeatureGroupFilterChooser groupFilterChooser;
  
  /** The check box indicating that the group filter is active/inactive. */
  private JCheckBox groupCheckBox;
  
  /** The panel showing the results of feature filtering. */
  private MultiPointFeatureOverlaySymbolPanel featurePanel;

  /** The panel showing the results of feature statistics calculations. */
  private MultiPointFeatureOverlayStatsPanel statsPanel;
  
  ////////////////////////////////////////////////////////////

  /**
   * Create a chooser object based on the current source and overlay.
   *
   * @return the chooser to use.
   */
  private SelectionRuleFilterChooser createRuleFilterChooser() {
  
    TimeWindow window = overlay.getTimeWindowHint();
    if (window == null) window = new TimeWindow (new Date(), 0);
    SelectionRuleFilterChooser ruleFilterChooser = new SelectionRuleFilterChooser (source.getAttributes(),
      source.getAttributeNameMap(), window);

    return (ruleFilterChooser);

  } // createRuleFilterChooser

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new chooser panel.
   *
   * @param overlay the overlay to manipulate.
   */
  public MultiPointFeatureOverlayPropertyChooser (
    MultiPointFeatureOverlay<SimpleSymbol> overlay
  ) {

    super (overlay);

    // Create tabbed pane
    // ------------------
    setLayout (new BorderLayout());
    JTabbedPane tabbedPane = new JTabbedPane();
    this.add (tabbedPane, BorderLayout.CENTER);

    // Create filter panel
    // -------------------
    JPanel filterPanel = new JPanel (new BorderLayout());
    tabbedPane.add ("Filters", filterPanel);

    // Add top panel
    // -------------
    JPanel topPanel = new JPanel (new BorderLayout());
    filterPanel.add (topPanel, BorderLayout.NORTH);

    // Add selection rule panel
    // ------------------------
    List<PointFeatureOverlay<SimpleSymbol>> overlayList = overlay.getOverlayList();
    if (overlayList.size() == 0) throw new RuntimeException ("Required point data source not available");
    source = overlayList.get(0).getSource();
    SelectionRuleFilterChooser ruleFilterChooser = createRuleFilterChooser();
    ruleFilterChooser.setFilter (overlay.getGlobalFilter());
    ruleFilterChooser.setBorder (new TitledBorder (new EtchedBorder(), "Feature Selection Rules"));
    ruleFilterChooser.addPropertyChangeListener (SelectionRuleFilterChooser.FILTER_PROPERTY, event -> signalOverlayChanged());
    topPanel.add (ruleFilterChooser, BorderLayout.NORTH);

    // Add grouping rule panel
    // -----------------------
    JPanel groupFilterPanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.NORTH;
    int xPos = 0;
    groupFilterPanel.setBorder (new TitledBorder (new EtchedBorder(), "Grouping Rules"));
    topPanel.add (groupFilterPanel, BorderLayout.SOUTH);
    
    groupCheckBox = new JCheckBox();
    groupCheckBox.setSelected (overlay.getGroupFilterActive());
    groupCheckBox.addActionListener (event -> groupCheckChanged());
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    groupFilterPanel.add (groupCheckBox, gc);

    TimeWindow windowHint = overlay.getTimeWindowHint();
    Date defaultDate = (windowHint != null ? windowHint.getCentralDate() : new Date(0));
    groupFilterChooser = new FeatureGroupFilterChooser (source.getAttributes(),
      source.getAttributeNameMap(), defaultDate);
    FeatureGroupFilter groupFilter = overlay.getGroupFilter();
    if (groupFilter != null)
      groupFilterChooser.setFilter (groupFilter);
    groupFilterChooser.addPropertyChangeListener (FeatureGroupFilterChooser.FILTER_PROPERTY, event -> groupFilterChanged());
    GUIServices.setContainerEnabled (groupFilterChooser, groupCheckBox.isSelected());
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
    groupFilterPanel.add (groupFilterChooser, gc);

    // Add center panel
    // ----------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    centerPanel.setBorder (new TitledBorder (new EtchedBorder(), "Symbols"));

    listPanel = new PointFeatureOverlayListPanel (overlay.getOverlayList());
    listPanel.addPropertyChangeListener (PointFeatureOverlayListPanel.SELECTION_PROPERTY, event -> selectionChanged());
    listPanel.addPropertyChangeListener (PointFeatureOverlayListPanel.OVERLAY_PROPERTY, event -> signalOverlayChanged());
    centerPanel.add (listPanel, BorderLayout.CENTER);
    filterPanel.add (centerPanel, BorderLayout.CENTER);

    // Add side button panel
    // ---------------------
    JPanel sidePanel = new JPanel (new GridBagLayout());
    int yPos = 0;

    addButton = GUIServices.getTextButton ("Add Symbol");
    addButton.addActionListener (event -> listPanel.addSymbol());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (addButton, gc);
    
    editButton = GUIServices.getTextButton ("Edit");
    editButton.addActionListener (event -> listPanel.editSymbol());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (editButton, gc);
    
    dupButton = GUIServices.getTextButton ("Duplicate");
    dupButton.addActionListener (event -> listPanel.dupSymbol());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (dupButton, gc);
    
    removeButton = GUIServices.getTextButton ("Remove");
    removeButton.addActionListener (event -> listPanel.removeSymbol());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (removeButton, gc);
    
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.BOTH, 0, 1);
    sidePanel.add (new JPanel(), gc);

//    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
//    sidePanel.add (Box.createVerticalStrut (4*addButton.getMinimumSize().height), gc);
    
    centerPanel.add (sidePanel, BorderLayout.EAST);

    // Fire a selection change to initialize the buttons
    // -------------------------------------------------
    selectionChanged();

    // Add feature panel
    // -----------------
    EarthArea area = overlay.getEarthAreaHint();
    if (area == null) {
      area = new EarthArea();
      area.addAll();
    } // if
    featurePanel = new MultiPointFeatureOverlaySymbolPanel (overlay, area);
    tabbedPane.add ("Features", featurePanel);

    // Add stats panel
    // -----------------
    statsPanel = new MultiPointFeatureOverlayStatsPanel (overlay, area);
    tabbedPane.add ("Statistics", statsPanel);
    List<String> expressionListHint = overlay.getExpressionListHint();
    if (expressionListHint != null) {
      expressionListHint.forEach (expression -> statsPanel.addExpression (expression));
    } // if

  } // MultiPointFeatureOverlayPropertyChooser constructor
  
  ////////////////////////////////////////////////////////////

  /** Handles the group filter changing state. */
  private void groupFilterChanged () {
  
    overlay.setGroupFilter (groupFilterChooser.getFilter());
    signalOverlayChanged();

  } // groupFilterChanged

  ////////////////////////////////////////////////////////////

  /** Handles the group filter check box changing state. */
  private void groupCheckChanged () {

    boolean isGroupFilterActive = groupCheckBox.isSelected();
    GUIServices.setContainerEnabled (groupFilterChooser, isGroupFilterActive);
    overlay.setGroupFilterActive (isGroupFilterActive);
    signalOverlayChanged();
  
  } // groupCheckChanged

  ////////////////////////////////////////////////////////////

  /** Handles a change in list element selection. */
  private void selectionChanged () {

    int[] indices = listPanel.getSelectedIndices();
    if (indices.length == 0) {
      addButton.setEnabled (true);
      editButton.setEnabled (false);
      dupButton.setEnabled (false);
      removeButton.setEnabled (false);
    } // if
    else if (indices.length == 1) {
      addButton.setEnabled (true);
      editButton.setEnabled (true);
      dupButton.setEnabled (true);
      removeButton.setEnabled (true);
    } // else if
    else {
      addButton.setEnabled (true);
      editButton.setEnabled (false);
      dupButton.setEnabled (false);
      removeButton.setEnabled (true);
    } // else
  
  } // selectionChanged

  ////////////////////////////////////////////////////////////

  /** Signals that the overlay contained in the chooser has been altered. */
  private void signalOverlayChanged() {
  
    firePropertyChange (OVERLAY_PROPERTY, null, overlay);
    featurePanel.overlayChanged();
    statsPanel.overlayChanged();

  } // signalOverlayChanged
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Creates a default overlay for use when a new symbol is needed.
   *
   * @return the new default point overlay.
   */
  private PointFeatureOverlay<SimpleSymbol> createDefaultOverlay () {
  
    SimpleSymbol symbol = new SimpleSymbol (PlotSymbolFactory.create ("Square"));
    symbol.setBorderColor (Color.WHITE);
    symbol.setFillColor (Color.RED);

    PointFeatureOverlay<SimpleSymbol> pointOverlay = new PointFeatureOverlay<> (symbol, source);
    SelectionRuleFilterChooser chooser = createRuleFilterChooser();
    pointOverlay.setFilter (chooser.getFilter());

    return (pointOverlay);

  } // createDefaultOverlay

  ////////////////////////////////////////////////////////////

  /** 
   * Implements a list panel of {@link PointFeatureOverlay} objects.
   * The list has a simple look with no extra buttons, because in this
   * design we put those buttons to the right side as part of another button
   * panel.
   */
  private class PointFeatureOverlayListPanel
    extends AbstractOverlayListPanel {

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new list panel with all extra buttons hidden.
     *
     * @param overlayList the initial list of overlays to show.
     */
    public PointFeatureOverlayListPanel (
      List<PointFeatureOverlay<SimpleSymbol>> overlayList
    ) {

      super (false, false, false, true, false);
      int i = overlayList.size();
      for (PointFeatureOverlay overlay : overlayList) overlay.setLayer (i--);
      addOverlays (overlayList);

    } // PointFeatureOverlayListPanel constructor

    ////////////////////////////////////////////////////////

    @Override
    protected List getAddButtons () { return (null); }

    ////////////////////////////////////////////////////////

    @Override
    protected String getTitle () { return (null); }

    ////////////////////////////////////////////////////////

    /**
     * Gets the index of the selected rows in the symbol list.
     *
     * @return the selected row indices (possibly empty).
     */
    public int[] getSelectedIndices() { return (overlayList.getSelectedIndices()); }

    ////////////////////////////////////////////////////////

    /**
     * Shows a dialog with a chooser in it.
     *
     * @param chooser the overlay chooser to show.
     * @param task the task to perform when the OK button on the dialog is clicked.
     */
    private void showDialog (
      OverlayPropertyChooser chooser,
      Runnable task
    ) { 

      JDialog dialog = GUIServices.createDialog (
        this,
        "Select the point symbol properties",
        true,
        chooser,
        null,
        new Action[] {
          GUIServices.createAction ("OK", () -> task.run()),
          GUIServices.createAction ("Cancel", null)
        },
        null,
        true);

      dialog.setVisible (true);

    } // showDialog

    ////////////////////////////////////////////////////////

    /** 
     * Reorders the layer values of the list of symbol overlays
     * according to their order in the multipoint list and updates the
     * visual list.
     */
    private void reorderSymbols () {
    
      List<PointFeatureOverlay<SimpleSymbol>> list = overlay.getOverlayList();
      int i = list.size();
      for (PointFeatureOverlay overlay : list) overlay.setLayer (i--);
      removeOverlays();
      addOverlays (list);

    } // reorderSymbols

    ////////////////////////////////////////////////////////

    /** Adds a new symbol to the overlay list. */
    public void addSymbol () {

      PointFeatureOverlay<SimpleSymbol> pointOverlay = createDefaultOverlay();
      pointOverlay.setName ("Symbol " + (overlayList.getElements()+1));
      PointFeatureOverlayPropertyChooser chooser = new PointFeatureOverlayPropertyChooser (pointOverlay);
      showDialog (chooser, () -> {
        overlay.getOverlayList().add (pointOverlay);
        reorderSymbols();
        signalOverlayChanged();
      });
    
    } // addSymbol

    ////////////////////////////////////////////////////////

    /** Edits the selected symbol in the overlay list. */
    public void editSymbol () {

      int index = overlayList.getSelectedIndices()[0];
      List<PointFeatureOverlay<SimpleSymbol>> list = overlay.getOverlayList();
      PointFeatureOverlay<SimpleSymbol> pointOverlay = list.get (index);
      PointFeatureOverlay<SimpleSymbol> copy = (PointFeatureOverlay<SimpleSymbol>) pointOverlay.clone();
      PointFeatureOverlayPropertyChooser chooser = new PointFeatureOverlayPropertyChooser (copy);
      showDialog (chooser, () -> {
        list.remove (pointOverlay);
        list.add (index, copy);
        reorderSymbols();
        signalOverlayChanged();
      });
    
    } // editSymbol

    ////////////////////////////////////////////////////////

    /** Edits the selected symbol in the overlay list. */
    public void dupSymbol () {
    
      int index = overlayList.getSelectedIndices()[0];
      PointFeatureOverlay<SimpleSymbol> pointOverlay = overlay.getOverlayList().get (index);
      PointFeatureOverlay<SimpleSymbol> copy = (PointFeatureOverlay<SimpleSymbol>) pointOverlay.clone();
      copy.setName ("Copy of " + pointOverlay.getName());
      PointFeatureOverlayPropertyChooser chooser = new PointFeatureOverlayPropertyChooser (copy);
      showDialog (chooser, () -> {
        overlay.getOverlayList().add (copy);
        reorderSymbols();
        signalOverlayChanged();
      });
    
    } // dupSymbol

    ////////////////////////////////////////////////////////

    /** Removes the selected symbols in the overlay list. */
    public void removeSymbol () {
    
      int[] indices = overlayList.getSelectedIndices();
      for (int i = indices.length-1; i >= 0; i--)
        overlay.getOverlayList().remove (indices[i]);
      reorderSymbols();
      signalOverlayChanged();
    
    } // removeSymbol

    ////////////////////////////////////////////////////////

  } // PointFeatureOverlayListPanel

  ////////////////////////////////////////////////////////////

  @Override
  protected String getTitle () { return (PANEL_TITLE); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a simple overlay for testing.
   *
   * @return the test overlay.
   */
  public static MultiPointFeatureOverlay createTestOverlay () {
  
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
      protected void select () throws IOException {
        if (featureList.size() == 0) {
          for (int i = 0; i < 100; i++) {
            EarthLocation earthLoc = new EarthLocation (
              -90 + Math.random()*180,
              -180 + Math.random()*360
            );
            Object[] attArray = new Object[] {
              (byte) (Math.round (Math.random()*8)),
              new String (new char[] {
                (char) ('A' + Math.round (Math.random()*25)),
                (char) ('A' + Math.round (Math.random()*25)),
                (char) ('0' + Math.round (Math.random()*9)),
                (char) ('0' + Math.round (Math.random()*9)),
                (char) ('0' + Math.round (Math.random()*9)),
                (char) ('0' + Math.round (Math.random()*9))
              }),
              Math.random()*35,
              (int) Math.round (Math.random()*2),
              new Date (System.currentTimeMillis() - (long) Math.round (Math.random()*2592e6))
            };
            featureList.add (new PointFeature (earthLoc, attArray));
          } // for
        } // if
      } // select
    };

    int[] platformValues = new int[] {0,1,2,3,4,5,6,7,8};
    String[] platformNames = new String[] {
      "Unknown",
      "Ship",
      "Drifter",
      "T-Mooring",
      "C-Mooring",
      "Argo",
      "HR-Drifter",
      "IMOS",
      "CRW"
    };
    Color[] platformColors = new Color[] {
      new Color (128, 128, 128),    // unknown
      new Color (20, 150, 20),      // ship
      new Color (0, 0, 180),        // drifter
      new Color (240, 0, 0),        // t-mooring
      new Color (170, 0, 170),      // c-mooring
      new Color (0, 170, 170),      // argo
      new Color (0, 90, 240),       // hr-drifter
      new Color (0, 230, 0),        // imos
      new Color (145, 22, 22)       // crw
    };
    String[] platformSymbols = new String[] {
      "X",             // unknown
      "Diamond",       // ship
      "Diamond",       // drifter
      "Triangle Up",   // t-mooring
      "Triangle Down", // c-mooring
      "Circle",        // argo
      "Square",        // hr-drifter
      "Square",        // imos
      "Circle"         // crw
    };
    
    Map<String, Integer> attNameMap = source.getAttributeNameMap();
    MultiPointFeatureOverlay multiOverlay = new MultiPointFeatureOverlay();
    for (int i = 0; i < platformValues.length; i++) {

      NumberRule platformRule = new NumberRule ("platform_type", attNameMap, (byte) platformValues[i]);
      platformRule.setOperator (NumberRule.Operator.IS_EQUAL_TO);
      SelectionRuleFilter filter = new SelectionRuleFilter();
      filter.add (platformRule);

      SimpleSymbol symbol = new SimpleSymbol (PlotSymbolFactory.create (platformSymbols[i]));
      symbol.setBorderColor (Color.BLACK);
      symbol.setFillColor (platformColors[i]);

      PointFeatureOverlay pointOverlay = new PointFeatureOverlay (symbol, source);
      pointOverlay.setFilter (filter);
      pointOverlay.setName (platformNames[i]);

      multiOverlay.getOverlayList().add (pointOverlay);

    } // for

    TimeWindow window = new TimeWindow (new Date(), 2592000000L/2);
    multiOverlay.getGlobalFilter().add (new TimeWindowRule ("time", attNameMap, window));
  
    FeatureGroupFilter groupFilter = new FeatureGroupFilter ("platform_id",
      attNameMap, "time", new Date());
    multiOverlay.setGroupFilter (groupFilter);
    multiOverlay.setGroupFilterActive (false);
  
    return (multiOverlay);
  
  } // createTestOverlay

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    MultiPointFeatureOverlay overlay = createTestOverlay();
    MultiPointFeatureOverlayPropertyChooser chooser = new MultiPointFeatureOverlayPropertyChooser (overlay);
    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (overlay)));

  } // main

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////

  
  
