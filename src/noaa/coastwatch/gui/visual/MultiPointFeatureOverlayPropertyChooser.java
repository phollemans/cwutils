////////////////////////////////////////////////////////////////////////
/*
     FILE: MultiPointFeatureOverlayPropertyChooser.java
  PURPOSE: Allows the user to edit overlay properties for multi-point feature overlays.
   AUTHOR: Peter Hollemans
     DATE: 2017/02/12
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2017, USDOC/NOAA/NESDIS CoastWatch

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
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.Box;
import javax.swing.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.io.IOException;

import noaa.coastwatch.gui.AbstractOverlayListPanel;
import noaa.coastwatch.gui.SelectionRuleFilterChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.PointFeatureOverlayPropertyChooser;
import noaa.coastwatch.gui.GUIServices;

import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.NumberRule;
import noaa.coastwatch.render.feature.DateRule;
import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.PlotSymbolFactory;

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

    // Setup layout
    // ------------
    setLayout (new BorderLayout());

    // Add top panel
    // -------------

// TODO: what if there are no overlays yet?  where can we get the source?

    source = overlay.getOverlayList().get(0).getSource();
    SelectionRuleFilterChooser filterChooser = new SelectionRuleFilterChooser (source.getAttributes(),
      source.getAttributeNameMap());
    filterChooser.setFilter (overlay.getGlobalFilter());
    filterChooser.setBorder (new TitledBorder (new EtchedBorder(), "Feature Selection Rules"));
    filterChooser.addPropertyChangeListener (SelectionRuleFilterChooser.FILTER_PROPERTY, event -> signalOverlayChanged());
    this.add (filterChooser, BorderLayout.NORTH);

    // Add center panel
    // ----------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    centerPanel.setBorder (new TitledBorder (new EtchedBorder(), "Symbols"));
    listPanel = new PointFeatureOverlayListPanel (overlay.getOverlayList());
    listPanel.addPropertyChangeListener (PointFeatureOverlayListPanel.SELECTION_PROPERTY, event -> selectionChanged());
    listPanel.addPropertyChangeListener (PointFeatureOverlayListPanel.OVERLAY_PROPERTY, event -> signalOverlayChanged());
    centerPanel.add (listPanel, BorderLayout.CENTER);
    this.add (centerPanel, BorderLayout.CENTER);

    // Add side button panel
    // ---------------------
    JPanel sidePanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
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
    
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (Box.createVerticalStrut (4*addButton.getMinimumSize().height), gc);
    
    this.add (sidePanel, BorderLayout.EAST);

    // Fire a selection change to initialize the buttons
    // -------------------------------------------------
    selectionChanged();

  } // MultiPointFeatureOverlayPropertyChooser constructor
  
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
    SelectionRuleFilterChooser chooser = new SelectionRuleFilterChooser (source.getAttributes(),
      source.getAttributeNameMap());
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
      overlayList.getElement (index).showChooser();
    
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

    PointFeatureOverlay pointOverlay = new PointFeatureOverlay (symbol, source);
    pointOverlay.setFilter (filter);
    pointOverlay.setName ("CRW");
    
    MultiPointFeatureOverlay multiOverlay = new MultiPointFeatureOverlay();
    multiOverlay.getOverlayList().add (pointOverlay);
    multiOverlay.getGlobalFilter().add (new DateRule ("time", attNameMap, new Date()));

    MultiPointFeatureOverlayPropertyChooser chooser = new MultiPointFeatureOverlayPropertyChooser (multiOverlay);
    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (multiOverlay)));

  } // main

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////

  
  
