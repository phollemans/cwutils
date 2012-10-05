////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualEntryTable.java
  PURPOSE: Shows a set of catalog entries as a table of images.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/16
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.browse;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.util.List;
import java.net.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.net.CatalogQueryAgent.Entry;

/**
 * The <code>VisualEntryTable</code> shows a grid of images to
 * represent catalog entry data.  When the user selects an entry,
 * the <code>ENTRY_INDEX_PROPERTY</code> is fired with the index
 * selected.  When a user activates an entry by double-clicking,
 * the <code>ENTRY_ACTIVATE_PROPERTY</code> is fired with the
 * newly activated entry.
 */
public class VisualEntryTable 
  extends JPanel {

  // Constants
  // ---------

  /** The format for date strings. */
  private static final String DATE_FMT = "yyyy/MM/dd HH:mm";

  /** The maximum number of cached image icons. */
  private static final int MAX_ICONS = 200;

  /** The entry index property for property change events. */
  public static final String ENTRY_INDEX_PROPERTY = "catalogEntryIndex";

  /** The entry activated property for property change events. */
  public static final String ENTRY_ACTIVATE_PROPERTY = "catalogEntryActive";

  // Variables
  // ---------

  /** The list of entries to display. */
  private List entryList;

  /** The list of containers for each visual entry. */
  private List containerList;

  /** The grid panel for visual entries. */
  private JPanel gridPanel;

  /** The scroll pane that contains the grid panel. */
  private JScrollPane scrollPane;

  /** The height of each row in pixels. */
  private int rowHeight;

  /** The cache of image icons by URL string. */
  private Map iconCache;
  
  /** The currently selected entry index, or -1 for none. */
  private int selectedIndex = -1;

  /** The normal border for entry containers. */
  private Border normalBorder;

  /** The selected border for entry containers. */
  private Border selectedBorder;

  /** The border for icon image labels. */
  private Border imageBorder;

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>ScrollPanel</code> class adds a
   * <code>javax.swing.Scrollable</code> interface to a regular
   * <code>JPanel</code> so that it can more easily be used in a
   * <code>JScrollPane</code>.  In this implementation, the scrollable
   * may scroll vertically, but not horizontally.
   */
  private class ScrollPanel extends JPanel implements Scrollable {

    ////////////////////////////////////////////////////////

    /**
     * Creates a new panel with a left-aligned {@link
     * noaa.coastwatch.gui.browse.WrapLayout} and component spacing of
     * 5 pixels in both directions.
     */
    public ScrollPanel () { 

      super (new WrapLayout (WrapLayout.LEFT, 5, 5));

    } // ScrollPanel constructor

    ////////////////////////////////////////////////////////

    /** Gets the preferred viewport size as the size of the panel. */
    public Dimension getPreferredScrollableViewportSize () {

      return (getPreferredSize());

    } // getPreferredScrollableViewportSize

    ////////////////////////////////////////////////////////

    /** Gets the scrolling unit increment (depends on row height). */
    public int getScrollableUnitIncrement (
      Rectangle visibleRect,
      int orientation,
      int direction
    ) {

      return (rowHeight/4);

    } // getScrollableUnitIncrement

    ////////////////////////////////////////////////////////

    /** Gets the scrolling block increment (depends on row height). */
    public int getScrollableBlockIncrement (
      Rectangle visibleRect,
      int orientation,
      int direction
    ) {

      return (rowHeight/4);

    } // getScrollableBlockIncrement

    ////////////////////////////////////////////////////////

    /** Returns false as we want this panel to scroll vertically. */
    public boolean getScrollableTracksViewportHeight () { return (false); }

    ////////////////////////////////////////////////////////

    /** Returns false as we never want this panel to scroll horizontally. */
    public boolean getScrollableTracksViewportWidth () { return (true); }

    ////////////////////////////////////////////////////////

  } // ScrollPanel constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new empty table. */
  public VisualEntryTable () {

    super (new BorderLayout());

    // Initialize
    // ----------
    entryList = new ArrayList();
    containerList = new ArrayList();
    iconCache = new LinkedHashMap (MAX_ICONS/4, .75f, true) {
      public boolean removeEldestEntry (Map.Entry eldest) {
        if (size() > MAX_ICONS) return (true);
        else return (false);
      } // removeEldestEntry
    }; 

    // Create grid panel
    // -----------------
    gridPanel = new ScrollPanel();
    scrollPane = new JScrollPane (gridPanel);
    add (scrollPane, BorderLayout.CENTER);
    scrollPane.getViewport().setBackground (gridPanel.getBackground());

    // Add mouse listener to grid panel
    // --------------------------------
    gridPanel.addMouseListener (new MouseAdapter () {
        public void mouseClicked (MouseEvent event) {
          Component component = gridPanel.getComponentAt (event.getPoint());
          if (containerList.contains (component)) {
            int clicks = event.getClickCount();
            int index = containerList.indexOf (component);
            if (clicks == 1) {
              setSelectedIndex (index);
              firePropertyChange (ENTRY_INDEX_PROPERTY, -1, index);
            } // if
            else if (clicks == 2) {
              firePropertyChange (ENTRY_ACTIVATE_PROPERTY, -1, index);
            } // else if
          } // if
        } // mouseClicked
      });

    // Set row height
    // --------------
    rowHeight = 100 + new JLabel ("XXX").getPreferredSize().height*2;

    // Create borders
    // --------------
    normalBorder = new CompoundBorder (
      new LineBorder (new Color (0x00000000, true), 2),
      new EmptyBorder (5, 5, 5, 5)
    );

    Color foreground = (Color) UIManager.get ("Label.foreground");
    int rgba = foreground.getRGB() & 0x50ffffff;
    Color lineColor = new Color (rgba, true);
    selectedBorder = new CompoundBorder (
      new LineBorder (lineColor, 2),
      new EmptyBorder (5, 5, 5, 5)
    );

    imageBorder = new EtchedBorder();

  } // VisualEntryTable constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected catalog entry index.
   * 
   * @return the entry index or -1 if no entry is selected.
   */
  public int getSelectedIndex () { return (selectedIndex); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the index of the selected (highlighted) entry.
   *
   * @param index the new index to select, or -1 for none.
   */
  public void setSelectedIndex (
    int index
  ) {

    if (index == selectedIndex) return;

    // Unhighlight the old selection
    // -----------------------------
    if (selectedIndex != -1) {
      JComponent component = (JComponent) containerList.get (selectedIndex);
      component.setBorder (normalBorder);
    } // if

    // Set and highlight the new selection
    // -----------------------------------
    selectedIndex = index;
    if (index != -1) {
      JComponent component = (JComponent) containerList.get (index);
      component.setBorder (selectedBorder);
    } // if

  } // setSelectedIndex

  ////////////////////////////////////////////////////////////

  /**
   * Scrolls the table view so that the specified entry is visible.
   *
   * @param index the new index to scroll to.
   */
  public void scrollToIndex (
    int index
  ) {

    JComponent component = (JComponent) containerList.get (index);
    Rectangle rect = component.getBounds();
    scrollPane.getViewport().setViewPosition (new Point (0, rect.y));

  } // scrollToIndex

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the catalog entry list.
   *
   * @param entryList the new entry list.
   */
  public void setEntryList (
    List entryList
  ) {

    this.entryList = entryList;
    selectedIndex = -1;
    updateGridPanel();

  } // setEntryList

  ////////////////////////////////////////////////////////////

  /**
   * Loads an image icon for a label in an asychronous thread.
   *
   * @param urlString the image URL to load.
   * @param label the label to set the icon for after loading is complete.
   */
  private void loadIcon (
    final String urlString,
    final JLabel imageLabel
  ) {

    imageLabel.setText ("[Loading]");
    Thread worker = new Thread () {
      public void run() {

        // Load icon
        // ---------
        ImageIcon icon;
        try { icon = new ImageIcon (new URL (urlString)); }
        catch (Exception e) { icon = null; }

        // Update GUI
        // ----------
        final ImageIcon fIcon = icon;
        SwingUtilities.invokeLater (new Runnable () {
            public void run () {
              if (fIcon == null) 
                imageLabel.setText ("[Error]");
              else {
                iconCache.put (urlString, fIcon);
                imageLabel.setText ("");
                imageLabel.setIcon (fIcon);
              } // else
            } // run
          });

      } // run
      };
    worker.start();

  } // loadIcon

  ////////////////////////////////////////////////////////////

  /** Updates the contents of the grid panel based on the entry list. */
  private void updateGridPanel () {

    // Remove old panels
    // -----------------
    gridPanel.removeAll();
    containerList.clear();

    // Add new panels
    // --------------
    for (Iterator iter = entryList.iterator(); iter.hasNext();) {

      // Get next entry
      // --------------
      Entry entry = (Entry) iter.next();

      // Create box and add to panel
      // ---------------------------
      Box box = Box.createVerticalBox();
      box.setBorder (normalBorder);
      containerList.add (box);
      gridPanel.add (box);

      // Add image
      // ---------
      JLabel imageLabel = new JLabel();
      imageLabel.setBorder (imageBorder);
      ImageIcon icon = (ImageIcon) iconCache.get (entry.previewUrl);
      if (icon == null)
        loadIcon (entry.previewUrl, imageLabel);
      else
        imageLabel.setIcon (icon);
      imageLabel.setAlignmentX (0.5f);
      box.add (imageLabel);

      // Add date string
      // ---------------
      String dateStr = DateFormatter.formatDate (entry.startDate, DATE_FMT);
      JLabel dateLabel = new JLabel (dateStr);
      dateLabel.setAlignmentX (0.5f);
      box.add (dateLabel);

      // Add data source
      // ---------------
      JLabel sourceLabel = new JLabel (entry.dataSource);
      sourceLabel.setAlignmentX (0.5f);
      box.add (sourceLabel);

    } // for

    // Force grid to revalidate
    // ------------------------
    gridPanel.revalidate();

  } // updateGridPanel

  ////////////////////////////////////////////////////////////

} // VisualEntryTable

////////////////////////////////////////////////////////////////////////
