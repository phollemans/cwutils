////////////////////////////////////////////////////////////////////////
/*
     FILE: HTTPDirectoryLister.java
  PURPOSE: Lists directories over an HTTP connection.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/26
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
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.DateFormatter;
import com.braju.format.Format;

/**
 * The <code>HTTPDirectoryLister</code> lists directory contents over
 * an HTTP connection.  The directory name used must be a valid HTTP
 * protocol URL (ie: start with <code>http://</code>).  The URL
 * contents must be an HTTP server directory listing rather than a
 * normal web page.  The HTML content of the directory listing is
 * parsed to extract the file and directory names.  A custom filter
 * may be set to filter the directory entries returned by the lister.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class HTTPDirectoryLister 
  extends AbstractDirectoryLister {

  // Constants
  // ---------

  /** The date and time format for the HTTP server. */
  private static final SimpleDateFormat DATE_FORMAT = 
    new SimpleDateFormat ("dd-MMM-yyyy HH:mm");

  // Variables
  // ---------

  /** The custom filter for A tag references. */
  private StringFilter filter;

  /** The set of ignored A tags. */
  private static Set ignoredTags;

  /** The cache of HTTP directory entries. */
  private static Map entryListCache;

  ////////////////////////////////////////////////////////////

  static {

    // Create ignored tags set
    // -----------------------
    ignoredTags = new HashSet();
    ignoredTags.add ("name");
    ignoredTags.add ("last modified");
    ignoredTags.add ("size");
    ignoredTags.add ("description");
    ignoredTags.add ("parent directory");

    // Create entry list cache
    // -----------------------
    entryListCache = Collections.synchronizedMap (new HashMap());

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Sets the directory entry filter.  If set, each A tag HREF
   * attribute that is to be added to the list of directory entries is
   * filtered through the string filter before creating the directory
   * entry.  If the filter returns null for a given HREF attribute,
   * the corresponding directory entry is skipped.
   * 
   * @param filter the new filter or null for no filtering (the
   * default).
   */
  public void setRefFilter (
    StringFilter filter
  ) {

    this.filter = filter;

  } // setRefFilter

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>Callback</code> class parses HTTP directory listings in
   * HTML and builds the file entry list accordingly.
   */
  private class Callback
    extends HTMLEditorKit.ParserCallback {

    // Variables
    // ---------

    /** The tag flag, true if we are in an HTML A tag. */
    private boolean inTag = false;

    /** The text flag, true if we are expecting the text after an A tag. */
    private boolean getTrailingText = false;

    /** The current A tag text element. */
    private String aText;

    /** The current A tag reference. */
    private String aRef;

    /** The list of entries to build. */
    private List entryList;

    ////////////////////////////////////////////////////////

    /** Creates a new callback that outputs to the specified list. */
    public Callback (List entryList) { this.entryList = entryList; }

    ////////////////////////////////////////////////////////

    /** Handles a start tag. */
    public void handleStartTag (
      HTML.Tag tag,
      MutableAttributeSet atts,
      int pos
    ) {

      // Check for start of A tag
      // ------------------------
      if (tag == HTML.Tag.A && !inTag) {
        inTag = true;
        aRef = ((String) atts.getAttribute (HTML.Attribute.HREF)).trim();
        getTrailingText = false;
      } // if

    } // handleStartTag

    ////////////////////////////////////////////////////////

    /** Handles text within a tag. */
    public void handleText (
      char[] data, 
      int pos
    ) {

      // Save text from A tag
      // --------------------
      if (inTag) aText = new String (data).trim();

      // Create directory entry
      // ----------------------
      else if (getTrailingText) {
        getTrailingText = false;

        // Check for parent directory
        // --------------------------
        if (ignoredTags.contains (aText.toLowerCase())) return;

        // Check for filtering
        // -------------------
        if (filter != null) {
          String newRef = filter.filter (aRef);
          if (newRef == null) return;
          else aRef = newRef;
        } // if
        
        // Parse date
        // ----------
        String[] array = new String (data).trim().split (" +");
        Date date;
        try { date = DATE_FORMAT.parse (array[0] + " " + array[1]); }
        catch (ParseException e) { date = new Date (0); }

        // TODO: There is a problem here in that the new OPeNDAP
        // server HTML output for directories is now different,
        // and uses a table structure rather than preformatted
        // text. So we get an index out of bounds exeception when
        // trying to get the file size.  Here's a snippet of the
        // new scheme:
        //
        // <td align="right">26-Apr-2006 14:23  </td><td align="right">8.1M</td>
        // Parse size
        // ----------
        String sizeField = array[2];
        long multiplier = 1;
        if (!sizeField.equals ("")) {
          char multChar = sizeField.charAt (sizeField.length()-1);
          switch (multChar) {
          case 'k': case 'K': multiplier = 1024; break;
          case 'm': case 'M': multiplier = 1048576; break;
          case 'g': case 'G': multiplier = 1073741824L; break;
          } // switch
        } // if
        sizeField = sizeField.replaceAll ("[^0-9.]", "");
        float sizeFloat;
        try { sizeFloat = Float.parseFloat (sizeField); }
        catch (NumberFormatException e) { sizeFloat = 0; }
        long size = (long) (sizeFloat*multiplier);

        // Get name and directory flag
        // ---------------------------
        boolean isDir = aRef.endsWith ("/");
        String name = aRef.replaceFirst ("^.*/([^/]+)/?$", "$1");

        // Add entry to list
        // -----------------
        entryList.add (new Entry (name, date, size, isDir));

      } // if

    } // handleText

    ////////////////////////////////////////////////////////

    /** Handles an end of tag. */
    public void handleEndTag (
      HTML.Tag tag, 
      int pos
    ) {

      // Check for end of A tag
      // ----------------------
      if (tag == HTML.Tag.A && inTag) {
        inTag = false;

        // Signal start of file attribute text
        // -----------------------------------
        getTrailingText = true;

      } // if
      
    } // handleEndTag

    ////////////////////////////////////////////////////////

  } // Callback class

  ////////////////////////////////////////////////////////////

  public String getParent (
    String name
  ) {

    return (name.replaceFirst ("^(http://.+)/[^/]+/?$", "$1")); 

  } // getParent

  ////////////////////////////////////////////////////////////

  public String getChild (
    String parent,
    String child
  ) {

    if (!parent.endsWith ("/")) parent += "/";
    return (parent + child);

  } // getChild

  ////////////////////////////////////////////////////////////

  protected List buildEntryList (String name) throws IOException {

    // Get connection
    // --------------
    URL url = new URL (name);
    if (!url.getProtocol().equals ("http"))
      throw new IOException ("Only HTTP protocol supported");
    HttpURLConnection connect = (HttpURLConnection) url.openConnection();

    // Check cache first
    // -----------------
    if (entryListCache.containsKey (name)) {
      return ((List) entryListCache.get (name));
    } // if

    // Check response code and content type
    // ------------------------------------
    int response = connect.getResponseCode();
    String type;
    if (response == -1) {
      type = connect.guessContentTypeFromStream (connect.getInputStream());
    } // if
    else if (response != HttpURLConnection.HTTP_OK) {
      throw new IOException (response + " " + connect.getResponseMessage());
    } // else if
    else {
      type = connect.getContentType();
    } // else
    if (type.indexOf ("text/html") == -1)
      throw new IOException ("Invalid content type: " + type);

    // Create reader
    // -------------
    BufferedReader reader = new BufferedReader (
      new InputStreamReader (connect.getInputStream()));

    // Parse content
    // -------------
    List entryList = new ArrayList();
    HTMLEditorKit.ParserCallback callback = new Callback (entryList);
    new ParserDelegator().parse (reader, callback, false);

    // Add list to cache
    // -----------------
    entryListCache.put (name, entryList);

    return (entryList);

  } // buildEntryList

  ////////////////////////////////////////////////////////////

  public void refresh () throws IOException {

    String name = getDirectory();
    if (name != null) {
      entryListCache.remove (name);
      setDirectory (name);
    } // if

  } // refresh

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    DirectoryLister lister = new HTTPDirectoryLister();
    lister.setDirectory (argv[0]);
    List entryList = lister.getEntries();
    Collections.sort (entryList);
    Format.printf ("%5s %-10s %-18s %s\n", 
      new Object[] {"", "Size", "Modified", "Name"});
    TimeZone zone = TimeZone.getDefault();
    for (Iterator iter = entryList.iterator(); iter.hasNext();) {
      Entry entry = (Entry) iter.next();
      String dir = (entry.isDirectory() ? "[DIR]" : "");
      String name = entry.getName();
      String date = DateFormatter.formatDate (entry.getModified(), 
        "yyyy/MM/dd HH:mm", zone);
      Long size = new Long (entry.getSize());
      Format.printf ("%5s %-10d %-18s %s\n", 
        new Object[] {dir, size, date, name});
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // HTTPDirectoryLister class

////////////////////////////////////////////////////////////////////////
