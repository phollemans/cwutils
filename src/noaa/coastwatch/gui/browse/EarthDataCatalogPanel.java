////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataCatalogPanel.java
  PURPOSE: Shows catalog views of a set of reader objects.
   AUTHOR: Peter Hollemans
     DATE: 2006/04/06
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
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.net.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.net.CatalogQueryAgent.Entry;

/**
 * The <code>EarthDataCatalogPanel</code> allows the user to
 * search an earth data catalog based on temporal and spatial
 * constraints using the services of a {@link
 * noaa.coastwatch.net.CatalogQueryAgent}.  When searching, the
 * <code>SEARCH_PROPERTY</code> is used to signal when in a
 * searching mode, true for when searching for new catalog
 * entries, or false when idle.  When the user double-clicks an
 * entry in the catalog search results table, the
 * <code>ENTRY_PROPERTY</code> is used to signal the event and
 * the value is the new {@link
 * noaa.coastwatch.net.CatalogQueryAgent.Entry} object.  When the
 * user simply selects a new entry from the list (by
 * single-clicking), the <code>CHANGED_PROPERTY</code> signals
 * that the entry was changed.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class EarthDataCatalogPanel
  extends JPanel {

  // Constants
  // ---------

  /** The date format for input parsing. */
  private static final String DATE_FMT = "yyyy/MM/dd HH:mm";

  /** The search property for property change events. */
  public static final String SEARCH_PROPERTY = "searchStatus";

  /** The entry property for property change events. */
  public static final String ENTRY_PROPERTY = "entrySelected";

  /** The changed property for property change events. */
  public static final String CHANGED_PROPERTY = "entryChanged";

  // Variables
  // ---------

  /** The catalog query agent. */
  private CatalogQueryAgent agent;

  /** The time search check box. */
  private JCheckBox timeCheck;

  /** The date form line. */
  private JPanel dateLine;

  /** The days form line. */
  private JPanel daysLine;

  /** The date/time radio button. */
  private JRadioButton dateRadio;

  /** The start date spinner. */
  private JSpinner startSpin;

  /** The end date spinner. */
  private JSpinner endSpin;

  /** The days radio button. */
  private JRadioButton daysRadio;

  /** The number of days spinner. */
  private JSpinner daysSpin;

  /** The coverage search check box. */
  private JCheckBox coverCheck;

  /** The cover percent spinner. */
  private JSpinner coverSpin;

  /** The cover region combo. */
  private JComboBox coverCombo;

  /** The search button. */
  private JButton searchButton;

  /** The table model for catalog entries. */
  private CatalogEntryTableModel entryModel;

  /** The table of matching catalog entries. */
  private SortedTable entryTable;
  
  /** The label for displaying matching entry count. */
  private JLabel entryLabel;

  /** The current search worker thread. */
  private Thread worker;

  /** The table of visual entry details. */
  private VisualEntryTable visualTable;

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the catalog query agent to be used for searching.
   *
   * @param agent the new catalog query agent.
   */
  public void setQueryAgent (
    CatalogQueryAgent agent
  ) {

    this.agent = agent;

  } // setQueryAgent

  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of available subregions for searching.
   *
   * @param subregionList a list of {@link
   * noaa.coastwatch.render.Subregion} objects.
   */
  public void setSubregionList (
    List subregionList
  ) {

    Subregion selected = (Subregion) coverCombo.getSelectedItem();
    coverCombo.removeAllItems();
    for (Iterator iter = subregionList.iterator(); iter.hasNext();) {
      Subregion subregion = (Subregion) iter.next();
      coverCombo.addItem (subregion);
      if (subregion.equals (selected))
        coverCombo.setSelectedItem (subregion);
    } // for

  } // setSubregions

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new catalog panel.  To perform searches, a query
   * agent must first be set by calling {@link #setQueryAgent},
   * and a subregion list using {@link #setSubregionList}.
   *
   * @param controlBar the control bar to but at the bottom of
   * the catalog results, or null for none.
   */
  public EarthDataCatalogPanel (
    JComponent controlBar
  ) {

    super (new BorderLayout());

    // Create search panel
    // -------------------
    JPanel searchPanel = new JPanel (new BorderLayout());
    searchPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Search Parameters"));
    this.add (searchPanel, BorderLayout.NORTH);
    Box form = Box.createVerticalBox();
    searchPanel.add (form);

    // Add region selection
    // --------------------
    JPanel regionLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    regionLine.setAlignmentX (0);
    form.add (regionLine);

    regionLine.add (new JLabel ("Search for data coverage of the"));
    coverCombo = new JComboBox();
    regionLine.add (coverCombo);
    regionLine.add (new JLabel ("region with:"));

    // Add coverage search controls
    // ----------------------------
    JPanel coverLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    coverLine.setAlignmentX (0);
    form.add (coverLine);

    coverCheck = new JCheckBox ("At least");
    coverCheck.addChangeListener (new ChangeListener () {
        public void stateChanged (ChangeEvent event) {
          GUIServices.setContainerEnabled (coverSpin, coverCheck.isSelected());
        } // stateChanged
      });
    coverLine.add (coverCheck);

    SpinnerNumberModel coverModel = new SpinnerNumberModel (
      new Integer (0),
      new Integer (0),
      new Integer (100),
      new Integer (1)
    );
    coverSpin = new JSpinner (coverModel);
    coverLine.add (coverSpin);
    coverLine.add (new JLabel ("percent coverage"));

    // Add date/time search controls
    // -----------------------------
    JPanel timeLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    timeLine.setAlignmentX (0);
    form.add (timeLine);

    timeCheck = new JCheckBox ("Date and time:");
    timeCheck.addChangeListener (new ChangeListener () {
        public void stateChanged (ChangeEvent event) {
          GUIServices.setContainerEnabled (dateLine, timeCheck.isSelected());
          GUIServices.setContainerEnabled (daysLine, timeCheck.isSelected());
        } // stateChanged
      });
    timeLine.add (timeCheck);

    dateLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    dateLine.setAlignmentX (0);
    form.add (dateLine);

    dateLine.add (Box.createHorizontalStrut (10));
    dateRadio = new JRadioButton ("From");
    ButtonGroup group = new ButtonGroup();
    group.add (dateRadio);
    dateLine.add (dateRadio);
    startSpin = new JSpinner (new SpinnerDateModel());
    startSpin.setEditor (new JSpinner.DateEditor (startSpin, DATE_FMT));
    dateLine.add (startSpin);
    dateLine.add (new JLabel ("to"));
    endSpin = new JSpinner (new SpinnerDateModel());
    endSpin.setEditor (new JSpinner.DateEditor (endSpin, DATE_FMT));
    dateLine.add (endSpin);

    daysLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    daysLine.setAlignmentX (0);
    form.add (daysLine);

    daysLine.add (Box.createHorizontalStrut (10));
    daysRadio = new JRadioButton ("During the past");
    group.add (daysRadio);
    daysLine.add (daysRadio);

    SpinnerNumberModel daysModel = new SpinnerNumberModel (
      new Integer (1),
      new Integer (1),
      new Integer (365),
      new Integer (1)
    );
    daysSpin = new JSpinner (daysModel);
    daysLine.add (daysSpin);
    daysLine.add (new JLabel ("days"));

    // Add button control box
    // ----------------------
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setAlignmentX (0);
    form.add (buttonBox);
    buttonBox.add (Box.createHorizontalGlue());

    searchButton = new JButton ("Search");
    searchButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          performSearch();
        } // actionPerformed
      });
    buttonBox.add (searchButton);

    JButton resetButton = new JButton ("Reset");
    resetButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          resetSearch();
        } // actionPerformed
      });
    buttonBox.add (resetButton);

    // Reset search values
    // -------------------
    resetSearch();

    // Create results panel
    // --------------------
    JPanel resultsPanel = new JPanel (new BorderLayout());
    resultsPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Catalog Results"));
    this.add (resultsPanel, BorderLayout.CENTER);

    JTabbedPane tabbedPane = new JTabbedPane();
    resultsPanel.add (tabbedPane, BorderLayout.CENTER);

    // Create visual panel
    // -------------------
    visualTable = new VisualEntryTable();
    tabbedPane.addTab ("Icon View", GUIServices.getIcon ("iconview.tab"),
      visualTable);
    visualTable.addPropertyChangeListener (
      VisualEntryTable.ENTRY_INDEX_PROPERTY, new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent evt) {
          int index = ((Integer) evt.getNewValue()).intValue();
          setSelectedIndex (index);
        } // propertyChange
      });
    visualTable.addPropertyChangeListener (
      VisualEntryTable.ENTRY_ACTIVATE_PROPERTY, new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent evt) {
          firePropertyChange (ENTRY_PROPERTY, null, getSelectedEntry());
        } // propertyChange
      });

    // Create details panel
    // --------------------
    JPanel detailsPanel = new JPanel (new BorderLayout());
    tabbedPane.addTab ("List View", GUIServices.getIcon ("listview.tab"),
      detailsPanel);
    entryModel = new CatalogEntryTableModel();
    entryTable = new SortedTable (entryModel);
    entryTable.setShowGrid (false);
    entryTable.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    entryTable.setIntercellSpacing (new Dimension (0,0));
    entryTable.setColumnSelectionAllowed (false);
    entryTable.getSelectionModel().addListSelectionListener (
      new EntryChanged());
    entryTable.addMouseListener (new EntrySelected());
    JScrollPane scrollpane = new JScrollPane (entryTable);
    detailsPanel.add (scrollpane, BorderLayout.CENTER);

    // Create entry count label
    // ------------------------
    JPanel labelPanel = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    resultsPanel.add (labelPanel, BorderLayout.SOUTH);
    entryLabel = new JLabel (" ");
    labelPanel.add (entryLabel);

    // Add control bar
    // ---------------
    if (controlBar != null)
      this.add (controlBar, BorderLayout.SOUTH);

  } // EarthDataCatalogPanel constructor

  ////////////////////////////////////////////////////////////

  /** Performs a search based on the current parameters. */
  public synchronized void performSearch () {

    // Check for valid agent
    // ---------------------
    if (agent == null) return;

    // Check for search worker
    // -----------------------
    if (worker != null && worker.isAlive()) return;

    // Set time search parameters
    // --------------------------
    boolean useTime = timeCheck.isSelected();
    agent.setSearchByTime (useTime);
    if (useTime) {
      if (dateRadio.isSelected()) {
        Date startDate = (Date) startSpin.getValue();
        Date endDate = (Date) endSpin.getValue();
        agent.setTimeByDate (startDate, endDate);
      } // if
      else {
        int days = ((Integer) daysSpin.getValue()).intValue();
        agent.setTimeByAge (days);
      } // else
    } // if

    // Set coverage search parameters
    // ------------------------------
    boolean useCoverage = coverCheck.isSelected();
    agent.setSearchByCoverage (true);
    String region = ((Subregion)coverCombo.getSelectedItem()).getShortName();
    if (useCoverage) {
      int coverage = ((Integer) coverSpin.getValue()).intValue();
      agent.setCoverageByRegion (region, coverage);
    } // if
    else {
      agent.setCoverageByRegion (region, -1);
    } // else

    // Setup for search
    // ----------------
    searchButton.setEnabled (false);
    entryLabel.setText ("Searching");
    firePropertyChange (SEARCH_PROPERTY, false, true);

    // Perform search
    // --------------
    worker = new Thread() {
      public void run() {

        // Get entry list and report results
        // ---------------------------------
        try { 

          // TODO: Need to enclose this in a timeout to really
          // make it foolproof against network problems.

          final List entryList = agent.getEntries();
          SwingUtilities.invokeLater (new Runnable () {
              public void run () {
                entryModel.setEntryList (entryList);
                visualTable.setEntryList (entryList);
                int entries = entryList.size();
                String text = entries + " matching " +
                  (entries == 1 ? "entry" : "entries") + " found";
                entryLabel.setText (text);
                searchButton.setEnabled (true);
                firePropertyChange (SEARCH_PROPERTY, true, false);
              } // run
            });
        } // try

        // Catch error and report
        // ----------------------
        catch (IOException e) { 
          SwingUtilities.invokeLater (new Runnable () {
              public void run () {
                entryLabel.setText ("Search failed");
                searchButton.setEnabled (true);
                firePropertyChange (SEARCH_PROPERTY, true, false);
                JOptionPane.showMessageDialog (EarthDataCatalogPanel.this, 
                  "A network error has occurred.\n" + 
                  "Please check your network connection and try again.", 
                  "Error", JOptionPane.ERROR_MESSAGE);
              } // run
            });
        } // catch

      } // run
    };
    worker.start();

  } // performSearch

  ////////////////////////////////////////////////////////////

  /** Resets the search parameters to default values. */
  private void resetSearch () {

    // Reset time values
    // -----------------
    timeCheck.setSelected (true);
    Date endDate = new Date();
    Date startDate = new Date (endDate.getTime() - 86400L*1000*2);
    startSpin.setValue (startDate);
    endSpin.setValue (endDate);
    daysRadio.setSelected (true);
    daysSpin.setValue (new Integer (2));

    // Reset coverage values
    // ---------------------
    coverCheck.setSelected (true);
    coverSpin.setValue (new Integer (50));
    if (coverCombo.getItemCount() != 0) coverCombo.setSelectedIndex (0);

  } // resetSearch

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the selected entry index in the entry table.
   *
   * @param index the entry model index to set selected.
   */
  private void setSelectedIndex (
    int index
  ) {

    index = entryTable.convertRowIndexToView (index);
    entryTable.setRowSelectionInterval (index, index);

  } // setSelectedIndex

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected catalog model index or -1 if none. */
  private int getSelectedCatalogIndex () {

    int row = entryTable.getSelectedRow();
    if (row == -1) return (-1);
    row = entryTable.convertRowIndexToModel (row);
    return (row);

  } // getSelectedCatalogIndex

  ////////////////////////////////////////////////////////////

  /**
   * Gets the selected catalog entry.
   *
   * @return the catalog entry, or null if none is selected.
   */
  public Entry getSelectedEntry () {

    int row = getSelectedCatalogIndex();
    if (row == -1) return (null);
    else return (entryModel.getEntry (row));

  } //  getSelectedEntry

  //////////////////////////////////////////////////////////////////////

  /** Fires a property change when an entry is selected by a double-click. */
  private class EntrySelected extends MouseAdapter {
    public void mouseClicked (MouseEvent evt) {

      if (entryTable.getSelectionModel().getValueIsAdjusting()) return;
      if (evt.getClickCount() == 2) {
        Entry entry = getSelectedEntry();
        if (entry != null)
          firePropertyChange (ENTRY_PROPERTY, null, entry);
      } // if

    } // mouseClicked
  } // EntrySelected class

  //////////////////////////////////////////////////////////////////////

  /** Modifies the visual table selection when the entry is changed. */
  private class EntryChanged implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent e) {

      if (e.getValueIsAdjusting()) return;
      int index = getSelectedCatalogIndex();
      if (visualTable.getSelectedIndex() != index) {
        visualTable.setSelectedIndex (index);
        if (index != -1) visualTable.scrollToIndex (index);
      } // if
      firePropertyChange (CHANGED_PROPERTY, null, getSelectedEntry());

    } // valueChanged 
  } // EntryChanged class

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    JFrame frame = new JFrame();
    EarthDataCatalogPanel panel = new EarthDataCatalogPanel (null);
    List subregionList = new ArrayList();
    subregionList.add (new Subregion (new EarthLocation(), 0, "East Coast", 
      "xx"));
    panel.setSubregionList (subregionList);
    panel.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent evt) {
          String name = evt.getPropertyName();
          if (name.equals (SEARCH_PROPERTY) ||
              name.equals (ENTRY_PROPERTY))
            System.out.println ("Got change event = " + name + ", value = " +
                                evt.getNewValue());
        } // propertyChange
      });
    CatalogQueryAgent agent = new OpendapQueryAgent (new URL (argv[0]));
    panel.setQueryAgent (agent);
    frame.getContentPane().add (panel, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible (true);

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataCatalogPanel class

////////////////////////////////////////////////////////////////////////


