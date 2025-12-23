////////////////////////////////////////////////////////////////////////
/*

     File: PaletteChooser.java
   Author: Peter Hollemans
     Date: 2003/09/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.DropMode;
import javax.swing.DefaultListModel;
import javax.swing.Box;

import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.PalettePanel;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>A palette chooser is a panel that allows the user to pick a colour
 * palette from a selection of predefined palettes.  A color palette
 * is typically used in conjunction with a 2D data variable to map a
 * set of data values to a set of colours.</p>
 *
 * <p>The palette chooser signals a change in the selected palette by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>PaletterChooser.PALETTE_PROPERTY</code>, and new value
 * contains a palette name from the predefined palettes supplied by
 * the {@link noaa.coastwatch.render.Palette} class.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class PaletteChooser
  extends JPanel
  implements TabComponent {

  private static final Logger LOGGER = Logger.getLogger (PaletteChooser.class.getName());

  // Constants
  // ---------

  /** The height of the palette stripe. */
  private static final int PALETTE_HEIGHT = 40;

  /** The palette property. */
  public static final String PALETTE_PROPERTY = "palette";

  /** The palette tooltip. */
  private static final String PALETTE_TOOLTIP = "Color Palette";

  // The key for storing and recalling the favourite palette names. 
  private static final String FAVOURITES_KEY = "favourite.palettes";

  // Variables
  // ---------    

  // The list and buttons for the favourite palettes.
  private JList<String> favouritesList;
  private JLabel favLabel;
  private JButton favRemoveButton;
  private JButton favClearButton;

  /** The list of palette names. */
  private JList<String> paletteList;

  /** The palette display panel. */
  private PalettePanel palettePanel;

  /** The last selected palette. */
  private String selectedPalette;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new palette chooser panel.  Initially no palette is
   * selected.
   */  
  public PaletteChooser () {

    super (new BorderLayout());

    JPanel topPanel = new JPanel (new BorderLayout());
    JPanel bottomPanel = new JPanel (new BorderLayout());

    this.add (topPanel, BorderLayout.NORTH);
    this.add (bottomPanel, BorderLayout.CENTER);

    // Create the panel that shows the preview of the currently selected
    // palette.
    JPanel palettePanelContainer = new JPanel (new BorderLayout());
    palettePanelContainer.setBorder (new TitledBorder (new EtchedBorder(), 
      "Palette"));
    topPanel.add (palettePanelContainer, BorderLayout.NORTH);

    palettePanel = new PalettePanel();
    palettePanel.setPreferredSize (new Dimension (PALETTE_HEIGHT, 
      PALETTE_HEIGHT));
    palettePanelContainer.add (palettePanel, BorderLayout.CENTER);

    // We create a favourite palettes list here so that the user can store
    // their frequently used palettes.
    var favouritesPanel = new JPanel (new BorderLayout());
    favouritesPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Favorite Palettes"));

    var favModel = new DefaultListModel<String>();
    favouritesList = new JList<> (favModel);
    recallFavouritePalettes();
    favouritesList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    favouritesList.addListSelectionListener (event -> favouritesListSelectedEvent (event));

    var overlayPanel = new JPanel() {
      @Override
      public boolean isOptimizedDrawingEnabled() { return (false); }
    };
    overlayPanel.setLayout (new OverlayLayout (overlayPanel));

    var scroll = new JScrollPane (favouritesList);
    scroll.setAlignmentX (0.5f);
    scroll.setAlignmentY (0.5f);

    // We do this next part because we found that turning the visibility of
    // the label on and off changed the size of the favourites scroll window
    // by a tiny amount.  This way, it stays fixed.
    int height = GUIServices.getLabelHeight()*8;
    scroll.setMinimumSize (new Dimension (100, height));
    scroll.setMinimumSize (new Dimension (100, height));
    scroll.setPreferredSize (new Dimension (100, height));

    favLabel = new JLabel ("Drag palettes here to add");
    favLabel.setEnabled (false);
    favLabel.setAlignmentX (0.5f);
    favLabel.setAlignmentY (0.5f);
    favLabel.setVisible (favModel.isEmpty());

    overlayPanel.add (favLabel);
    overlayPanel.add (scroll);

//    overlayPanel.setPreferredSize (favouritesList.getPreferredSize());
//    favouritesPanel.add (new JScrollPane (favouritesList), BorderLayout.CENTER);

    favouritesPanel.add (overlayPanel, BorderLayout.CENTER);
    topPanel.add (favouritesPanel, BorderLayout.SOUTH);

    var buttonPanel = Box.createHorizontalBox();

    favRemoveButton = GUIServices.getIconButton ("list.delete");
    GUIServices.setSquare (favRemoveButton);
    favRemoveButton.addActionListener (event -> removeButtonEvent());
    favRemoveButton.setEnabled (favModel.size() != 0);
    favRemoveButton.setToolTipText ("Remove palette from favorites");
    buttonPanel.add (favRemoveButton);

    buttonPanel.add (Box.createHorizontalGlue());

    favClearButton = GUIServices.getIconButton ("list.clear");
    GUIServices.setSquare (favClearButton);
    favClearButton.addActionListener (event -> clearButtonEvent());
    favClearButton.setEnabled (favModel.size() != 0);
    favClearButton.setToolTipText ("Clear favorites list");
    buttonPanel.add (favClearButton);

    favouritesPanel.add (buttonPanel, BorderLayout.SOUTH);

    // Create the main list of palettes here with a searchable text field
    // at the top.
    JPanel paletteListContainer = new JPanel (new BorderLayout());
    paletteListContainer.setBorder (new TitledBorder (new EtchedBorder(), 
      "Palette List"));

    var searchPanel = new JPanel (new BorderLayout (5, 5));
    searchPanel.setBorder (BorderFactory.createEmptyBorder (5, 2, 5, 2));
    paletteListContainer.add (searchPanel, BorderLayout.NORTH);
    searchPanel.add (new JLabel ("Search:"), BorderLayout.WEST);
    var searchField = new JTextField (10);
    searchField.getDocument().addDocumentListener (new DocumentListener () {
      private void changed () { searchEvent (searchField.getText()); }
      public void insertUpdate (DocumentEvent e) { changed(); }
      public void removeUpdate(DocumentEvent e) { changed(); }
      public void changedUpdate(DocumentEvent e) { changed(); }
    });
    searchPanel.add (searchField, BorderLayout.CENTER);
    var clearButton = new JButton ("Clear");
    clearButton.addActionListener (event -> searchField.setText (""));
    searchPanel.add (clearButton, BorderLayout.EAST);
    bottomPanel.add (paletteListContainer, BorderLayout.CENTER);

    var palModel = new DefaultListModel<String>();
    palModel.addAll (PaletteFactory.getPredefined());
    paletteList = new JList<> (palModel);
    paletteList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    paletteList.addListSelectionListener (event -> paletteListSelectedEvent (event));
    paletteListContainer.add (new JScrollPane (paletteList), BorderLayout.CENTER);

    // Add drag and drop support so that users can save their favourite
    // palettes.
    paletteList.setDragEnabled (true);
    favouritesList.setDropMode (DropMode.INSERT);
    favouritesList.setTransferHandler (new FavouritesHandler());

  } // PaletteChooser constructor

  ////////////////////////////////////////////////////////////

  private void storeFavouritePalettes () {

    var model = (DefaultListModel<String>) favouritesList.getModel();
    String favourites = "";
    if (!model.isEmpty()) {
      var buffer = new StringBuffer();
      for (int i = 0; i < model.size(); i++) {
        buffer.append (model.get (i));
        if (i < model.size()-1) buffer.append (",");
      } // for
      favourites = buffer.toString();
    } // if
    GUIServices.storeStringSettingForClass (favourites, FAVOURITES_KEY, PaletteChooser.class);

    LOGGER.fine ("Stored " + model.size() + " favourite palettes '" + favourites + "'");

  } // storeFavouritePalettes

  ////////////////////////////////////////////////////////////

  private void recallFavouritePalettes () {

    var model = (DefaultListModel<String>) favouritesList.getModel();
    model.clear();
    String favourites = GUIServices.recallStringSettingForClass (null, FAVOURITES_KEY, PaletteChooser.class);
    if (favourites != null && !favourites.equals ("")) {
      for (String name : favourites.split (",")) model.addElement (name);
      LOGGER.fine ("Recalled " + model.size() + " favourite palettes '" + favourites + "'");
    } // if

  } // recallFavouritePalettes

  ////////////////////////////////////////////////////////////

  private void removeButtonEvent() { 

    var selected = favouritesList.getSelectedValue();
    if (selected != null) {
      var model = (DefaultListModel<String>) favouritesList.getModel();
      model.removeElement (selected);
      storeFavouritePalettes();
      if (model.getSize() == 0) {
        favLabel.setVisible (true);
        favRemoveButton.setEnabled (false);
        favClearButton.setEnabled (false);
      } // if
    } // if

  } // removeButtonEvent

  ////////////////////////////////////////////////////////////

  private void clearButtonEvent() { 

    var model = (DefaultListModel<String>) favouritesList.getModel();
    if (model.getSize() != 0) {
      model.clear();
      storeFavouritePalettes();
      favLabel.setVisible (true);
      favRemoveButton.setEnabled (false);
      favClearButton.setEnabled (false);
    } // if

  } // clearButtonEvent

  ////////////////////////////////////////////////////////////

  /** 
   * A class to handle the drop of an item from the palette list into
   * the favourites. 
   */
  private class FavouritesHandler extends TransferHandler {

    @Override
    public boolean canImport (TransferSupport support) {

      return (support.isDataFlavorSupported (DataFlavor.stringFlavor));

    } // canImport

    @Override
    public boolean importData (TransferSupport support) {

      boolean result = false;

      if (canImport (support)) {
        Transferable transferable = support.getTransferable();
        try {            
          String item = (String) transferable.getTransferData (DataFlavor.stringFlavor);
          if (item != null && PaletteFactory.getPredefined().contains (item)) {
            var model = (DefaultListModel<String>) favouritesList.getModel();
            if (!model.contains (item)) {
              var location = (JList.DropLocation) support.getDropLocation();
              int index = location.getIndex();
              model.add (index, item);
              updateFavouritesList();
              storeFavouritePalettes();
              favouritesList.ensureIndexIsVisible (index);
              favLabel.setVisible (false);
              favRemoveButton.setEnabled (true);
              favClearButton.setEnabled (true);
              result = true;
            } // if
          } // if
        } catch (Exception e) {
          LOGGER.log (Level.WARNING, "Failed to drop item into favourites list", e);
        } // catch
      } // if

      return (result);

    } // importData

  } // FavouritesHandler class

  ////////////////////////////////////////////////////////////

  private void searchEvent (String text) {

    var paletteNameList = new ArrayList<> (PaletteFactory.getPredefined());
    var textLower = text.toLowerCase();
    paletteNameList.removeIf (name -> !name.toLowerCase().contains (textLower));

    var model = (DefaultListModel<String>) paletteList.getModel();
    model.clear();
    model.addAll (paletteNameList);

    updatePaletteList();

  } // searchEvent

  ////////////////////////////////////////////////////////////

  private void paletteListSelectedEvent (ListSelectionEvent event) {

    if (!event.getValueIsAdjusting()) {
      updatePalette (paletteList.getSelectedValue());
    } // if

  } // paletteListSelectedEvent

  ////////////////////////////////////////////////////////////

  private void fireChangeEvent (Palette newPalette) {

    LOGGER.fine ("Firing palette change property event for " + newPalette.getName());
    PaletteChooser.this.firePropertyChange (PALETTE_PROPERTY, null, newPalette);

  } // fireChangeEvent

  ////////////////////////////////////////////////////////////

  private void favouritesListSelectedEvent (ListSelectionEvent event) {

    if (!event.getValueIsAdjusting()) {
      updatePalette (favouritesList.getSelectedValue());
    } // if

  } // favouritesListSelectedEvent

  ////////////////////////////////////////////////////////////

  private Palette getPaletteObject () {

    Palette obj = null;
    if (selectedPalette != null) obj = PaletteFactory.create (selectedPalette);

    return (obj);

  } // getPaletteObject

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected palette.
   * 
   * @return the selected palette, or if the selection is empty, the last
   * valid selected palette, or null if no palette has ever been selected.
   */
  public Palette getPalette () { 

    return (getPaletteObject());

  } // getPalette

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the selected palette.  The palette must have been obtained
   * using the {@link noaa.coastwatch.render.PaletteFactory#create(String)} method or 
   * be a value previously returned by {@link getPalette}.
   */
  public void setPalette (
    Palette palette
  ) { 

    setPalette (palette.getName());

  } // setPalette
  
  ////////////////////////////////////////////////////////////

  private void updatePaletteList () {

    var model = (DefaultListModel<String>) paletteList.getModel();
    if (model.contains (selectedPalette)) paletteList.setSelectedValue (selectedPalette, true);
    else paletteList.clearSelection();

  } // updatePaletteList

  ////////////////////////////////////////////////////////////

  private void updateFavouritesList () {

    var model = (DefaultListModel<String>) favouritesList.getModel();
    if (model.contains (selectedPalette)) favouritesList.setSelectedValue (selectedPalette, true);
    else favouritesList.clearSelection();

  } // updateFavouritesList

  ////////////////////////////////////////////////////////////

  private void updatePalette (String palette) { 

    if (palette != null && !palette.equals (selectedPalette)) {
      selectedPalette = palette;
      var paletteObj = getPaletteObject();
      palettePanel.setPalette (paletteObj);
      updatePaletteList();
      updateFavouritesList();
      fireChangeEvent (paletteObj);
    } // if

  } // updatePalette

  ////////////////////////////////////////////////////////////

  /** 
   * Set the selected palette by name.
   *
   * @param name the palette name.
   *
   * @see #setPalette(Palette)
   */
  public void setPalette (
    String name
  ) {

    LOGGER.fine ("Setting palette to " + name);
    updatePalette (name);

  } // setPalette

  ////////////////////////////////////////////////////////////

  @Override public Icon getIcon () { return (GUIServices.getIcon ("palette.tab")); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () { return (PALETTE_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

} // PaletteChooser class

////////////////////////////////////////////////////////////////////////
