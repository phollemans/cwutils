////////////////////////////////////////////////////////////////////////
/*
     FILE: ServerTableModel.java
  PURPOSE: Models server information.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/24
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/** 
 * The <code>ServerTableModel</code> class models a mapping of simple
 * server names to URL strings.  New servers may be added by entering
 * valid data into the last row.  The table always holds one more row
 * than the actual number of server mappings so that users can use the
 * last row to add a new server.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class ServerTableModel
  extends AbstractTableModel {

  // Constants
  // ---------

  /** Column index of server name. */
  private static final int NAME_COLUMN = 0;

  /** Column index of server URL. */
  private static final int URL_COLUMN = 1;

  /** The server table DTD URL. */
  private static final String DTD_URL = 
    "http://coastwatch.noaa.gov/xml/servertable.dtd";

  /** The server table DTD local resource. */
  private static final String DTD_RESOURCE = "servertable.dtd";

  // Variables
  // ---------

  /** The list of server entries. */
  private List serverList;

  ////////////////////////////////////////////////////////////

  /** Holds a server table entry with server name and location. */
  public static class Entry {

    /** The server name. */
    private String name;

    /** The server location. */
    private String location;

    /** Creates a new server entry. */
    public Entry (String n, String l) { name = n; location = l; }

    /** Gets the server name. */
    public String getName () { return (name); }
    
    /** Gets the server location. */
    public String getLocation () { return (location); }

    /** Converts the entry to a string. */
    public String toString () {
      return (
        this.getClass().getName() + "[" +
        "name=" + name + "," +
        "location=" + location + "]"
      );
    } // toString

  } // Entry class

  ////////////////////////////////////////////////////////////

  /** Creates a new table model with no data. */
  public ServerTableModel () {

    this.serverList = new LinkedList();

  } // ServerTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Creates a new table model using the server list. */
  public ServerTableModel (List serverList) { 

    this.serverList = serverList;

  } // ServerTableModel constructor

  ////////////////////////////////////////////////////////////

  /** Gets the number of table rows. */
  public int getRowCount () { return (serverList.size()+1); }

  ////////////////////////////////////////////////////////////
    
  /** Gets the number of table columns. */
  public int getColumnCount () { return (2); }

  ////////////////////////////////////////////////////////////

  /** Gets the table data value. */
  public Object getValueAt (int row, int column) {

    // Get empty value
    // ---------------
    if (row == serverList.size()) return ("");

    // Get actual value
    // ----------------
    Entry entry = (Entry) serverList.get (row);
    switch (column) {
    case NAME_COLUMN: return (entry.getName());
    case URL_COLUMN: return (entry.getLocation());
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // getValueAt

  ////////////////////////////////////////////////////////////

  /** Gets the server list entry at the specified row. */
  public Entry getServerEntry (
    int row
  ) {

    if (row == serverList.size()) return (null);
    else return ((Entry) serverList.get (row));

  } // getServerEntry

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true except for the new entry because all the cells are
   * editable.
   */
  public boolean isCellEditable (int row, int column) { 

    if (row == serverList.size() && column == URL_COLUMN) return (false);
    return (true); 

  } // isCellEditable

  ////////////////////////////////////////////////////////////

  /** Sets the value at the specified row and column. */
  public void setValueAt (Object value, int row, int column) {

    // Check for invalid value
    // -----------------------
    String valueStr = ((String) value).trim();
    if (valueStr.equals ("") && column == NAME_COLUMN) return;

    // Set new value
    // -------------
    if (row == serverList.size()) {
      switch (column) {
      case NAME_COLUMN: 
        serverList.add (new Entry (valueStr, ""));
        int rows = serverList.size();
        fireTableRowsInserted (rows, rows);
        return;
      default: throw new IllegalArgumentException (
        "Column index " + column + " not allowed");
      } // switch
    } // if

    // Change existing value
    // ---------------------
    Entry entry = (Entry) serverList.get (row);
    switch (column) {
    case NAME_COLUMN: 
      serverList.set (row, new Entry (valueStr, entry.getLocation()));
      fireTableRowsUpdated (row, row);
      break;
    case URL_COLUMN: 
      serverList.set (row, new Entry (entry.getName(), valueStr));
      fireTableRowsUpdated (row, row);
      break;
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // setValueAt

  ////////////////////////////////////////////////////////////

  /** Gets the table column name. */
  public String	getColumnName (int column) { 

    switch (column) {
    case NAME_COLUMN: return ("Name");
    case URL_COLUMN: return ("Location");
    default: throw new IllegalArgumentException (
      "Column index " + column + " not allowed");
    } // switch

  } // getColumnName

  ////////////////////////////////////////////////////////////

  /** Gets the list of server entries. */
  public List getServerList () { return (new ArrayList (serverList)); }

  ////////////////////////////////////////////////////////////

  /** Removes the specified row from the table model. */
  public void removeRow (int row) {

    if (row == serverList.size()) return;
    serverList.remove (row);
    fireTableDataChanged();

  } // removeRow

  ////////////////////////////////////////////////////////////

  /** 
   * Reads a list of {@link Entry} objects from the specified stream
   * in XML format.  The XML format is as follows:
   * <pre>
   *   &lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;
   *   &lt;!DOCTYPE servertable SYSTEM "http://coastwatch.noaa.gov/xml/servertable.dtd"&gt;
   *   
   *   &lt;servertable&gt;
   *     &lt;server name="Server 1" location="http://foo.bar.gov" /&gt;
   *     &lt;server name="Server 2" location="http://fie.ont.gov" /&gt;
   *     &lt;server name="Server 3" location="http://fiddle.sticks.gov" /&gt;
   *     ...
   *   &lt;/servertable&gt;
   *</pre>
   *
   * @param stream the input stream to read.
   *
   * @return the list of {@link Entry} objects.
   *
   * @throws IOException if the input had format errors.
   */
  public static List readList (
    InputStream stream
  ) throws IOException {

    try {

      // Create parser
      // -------------
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating (true);
      SAXParser parser = factory.newSAXParser();
      ServerTableHandler handler = new ServerTableHandler();

      // Parse file
      // ----------
      parser.parse (stream, handler);
      return (handler.getList());

    } // try

    catch (Exception e) {
      throw new IOException (e.getMessage());
    } // catch

  } // readList

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a list of {@link Entry} objects to the specified stream in
   * XML format.  The XML format conforms to that of {@link
   * #readList}.
   *
   * @param stream the output stream to write.
   * @param serverList the list of {@link Entry} objects.
   */
  public static void writeList (
    OutputStream stream,
    List serverList
  ) {

    PrintStream printStream = new PrintStream (stream);

    // Write header
    // ------------
    printStream.println ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    printStream.println ("<!DOCTYPE servertable SYSTEM \"http://coastwatch.noaa.gov/xml/servertable.dtd\">\n");
    printStream.println ("<servertable>");

    // Write entries
    // -------------
    for (Iterator iter = serverList.iterator(); iter.hasNext();) {
      Entry entry = (Entry) iter.next();
      printStream.println ("  <server name=\"" + entry.getName() +
        "\" location=\"" + entry.getLocation() + "\" />");
    } // for

    // Write footer
    // ------------
    printStream.println ("</servertable>");
    printStream.println ("");
    printStream.println ("<!-- Last modified: " + new Date() + " -->");
    
  } // writeList

  ////////////////////////////////////////////////////////////

  /** Handles server table parsing events. */
  private static class ServerTableHandler 
    extends DefaultHandler {

    // Variables
    // ---------

    /** The server list. */
    private ArrayList serverList;

    ////////////////////////////////////////////////////////

    /** Gets the server list parsed by this handler. */
    public List getList () {

      return ((List) serverList.clone());

    } // getList

    ////////////////////////////////////////////////////////

    public InputSource resolveEntity (
      String publicId, 
      String systemId
    ) {

      if (systemId.equals (DTD_URL)) {
        InputStream stream = getClass().getResourceAsStream (DTD_RESOURCE);
        return (new InputSource (stream));
      }  // if

      return (null);

    } // resolveEntity

    ////////////////////////////////////////////////////////

    public void error (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // error

    ////////////////////////////////////////////////////////

    public void fatalError (
      SAXParseException e
    ) throws SAXException {
 
      throw (new SAXException ("Line " + e.getLineNumber() + ": " + 
        e.getMessage())); 

    } // fatalError

    ////////////////////////////////////////////////////////

    public void warning (
      SAXParseException e
    ) throws SAXException { 

      fatalError (e);

    } // warning

    ////////////////////////////////////////////////////////

    public void startElement (
        String uri,
        String localName,
        String qName,
        Attributes attributes
    ) throws SAXException {

      // Start server table
      // ------------------
      if (qName.equals ("servertable")) {
        serverList = new ArrayList();
      } // if

      // Add color to server table
      // -------------------------
      else if (qName.equals ("server")) {
        serverList.add (new Entry (attributes.getValue ("name"), 
          attributes.getValue ("location")));
      } // else if

    } // startElement

    ////////////////////////////////////////////////////////

  } // ServerTableHandler

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create table
    // ------------
    List serverList = new LinkedList();
    serverList.add (new Entry ("Server 1", "http://foo.bar.gov"));
    serverList.add (new Entry ("Server 2", "http://fie.ont.gov"));
    serverList.add (new Entry ("Server 3", "http://fiddle.sticks.gov"));
    AbstractTableModel model = new ServerTableModel (serverList);
    JTable table = new JTable (model);
    JScrollPane scrollpane = new JScrollPane (table);

    // Create frame
    // ------------
    final JFrame frame = new JFrame (ServerTableModel.class.getName());
    frame.getContentPane().add (scrollpane);
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // ServerTableModel

////////////////////////////////////////////////////////////////////////
