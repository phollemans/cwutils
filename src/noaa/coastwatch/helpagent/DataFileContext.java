/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.io.ReaderSummaryProducer;
import noaa.coastwatch.util.MetadataServices;

/**
 * Captures JSON context describing an earth data source for use by the
 * CoastWatch help agent.  The context contains structured summary information
 * produced from the interpreted reader properties, and detailed metadata
 * obtained from the reader's raw global and variable metadata maps.
 *
 * <p>Construction from an {@link EarthDataReader} reads all context data
 * immediately but does not retain or close the reader.  The {@link
 * #create(String)} convenience method opens and closes its own reader.</p>
 *
 * @author Peter Hollemans
 * @since 4.2.5
 */
public final class DataFileContext {

  private static final Logger LOGGER = Logger.getLogger (DataFileContext.class.getName());

  /** The mapper used to format context as JSON. */
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  /** The data source described by this context. */
  private final String source;

  /** The structured reader summary. */
  private final Map<String, Object> summary;

  /** The structured detailed raw metadata, possibly null. */
  private final Map<String, Object> detailedMetadata;

  /** The complete context formatted as JSON. */
  private final String json;

  ////////////////////////////////////////////////////////////

  /**
   * Creates context for a data source by opening and closing a reader.
   *
   * @param source the local file name or network location to read.
   *
   * @return the data file context.
   *
   * @throws IOException if the source cannot be opened or its summary cannot
   * be read.
   */
  public static DataFileContext create (
    String source
  ) throws IOException {

    EarthDataReader reader = EarthDataReaderFactory.create (source);
    try {
      return (new DataFileContext (reader));
    } // try
    finally {
      try { reader.close(); }
      catch (IOException e) {
        LOGGER.log (Level.WARNING, "Error closing attached data source " + source, e);
      } // catch
    } // finally

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates context from an open reader.  The reader is not retained or
   * closed by this object.
   *
   * @param reader the reader supplying summary and detailed metadata.
   *
   * @throws IOException if the reader summary cannot be created.
   */
  public DataFileContext (
    EarthDataReader reader
  ) throws IOException {

    this.source = reader.getSource();
    this.summary = createSummary (reader);
    this.detailedMetadata = createDetailedMetadata (reader);

    var context = new LinkedHashMap<String, Object>();
    context.put ("source", source);
    context.put ("summary", summary);
    context.put ("detailedMetadata", detailedMetadata);
    this.json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString (context);

  } // DataFileContext constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data source described by this context.
   *
   * @return the source file name or network location.
   */
  public String getSource () { return (source); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the structured interpreted reader summary.
   *
   * @return the summary data.
   */
  public Map<String, Object> getSummary () { return (summary); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the structured detailed raw metadata.
   *
   * @return the detailed metadata, or null if no detailed metadata was
   * available.
   */
  public Map<String, Object> getDetailedMetadata () { return (detailedMetadata); }

  ////////////////////////////////////////////////////////////

  /**
   * Tests if any detailed raw metadata was available.
   *
   * @return true if detailed metadata is available.
   */
  public boolean hasDetailedMetadata () { return (detailedMetadata != null); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the complete context formatted as JSON.
   *
   * @return the JSON context text.
   */
  public String getText () { return (json); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the complete context formatted as JSON.
   *
   * @return the JSON context text.
   */
  public String getJson () { return (json); }

  ////////////////////////////////////////////////////////////

  /** Creates the structured interpreted reader summary. */
  private static Map<String, Object> createSummary (
    EarthDataReader reader
  ) throws IOException {

    var producer = ReaderSummaryProducer.getInstance();
    var readerSummary = producer.create (reader);
    var summary = new LinkedHashMap<String, Object>();
    summary.put ("global", readerSummary.global);
    summary.put ("variables", createTableRecords (
      readerSummary.variable.columnNames,
      readerSummary.variable.rowList
    ));
    summary.put ("earthLocation", readerSummary.transform);
    summary.put ("coordinateSystems", readerSummary.coordinate);
    return (summary);

  } // createSummary

  ////////////////////////////////////////////////////////////

  /** Converts a summary table to records named by the table columns. */
  private static List<Map<String, String>> createTableRecords (
    String[] columns,
    List<String[]> rows
  ) {

    var records = new ArrayList<Map<String, String>>();
    for (var row : rows) {
      var record = new LinkedHashMap<String, String>();
      for (int i = 0; i < columns.length; i++) record.put (columns[i], row[i]);
      records.add (record);
    } // for
    return (records);

  } // createTableRecords

  ////////////////////////////////////////////////////////////

  /** Creates the structured detailed raw metadata. */
  private static Map<String, Object> createDetailedMetadata (
    EarthDataReader reader
  ) {

    var detailed = new LinkedHashMap<String, Object>();

    var globalMetadata = createMetadataAttributes (reader.getRawMetadata());
    if (!globalMetadata.isEmpty()) detailed.put ("global", globalMetadata);

    var variables = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < reader.getVariables(); i++) {
      try {
        var attributes = createMetadataAttributes (reader.getRawMetadata (i));
        if (!attributes.isEmpty()) {
          var variable = new LinkedHashMap<String, Object>();
          variable.put ("name", reader.getName (i));
          variable.put ("attributes", attributes);
          variables.add (variable);
        } // if
      } // try
      catch (IOException e) {
        LOGGER.warning (
          "Error reading metadata at variable index " + i + ": " + e.toString()
        );
      } // catch
    } // for

    if (!variables.isEmpty()) detailed.put ("variables", variables);
    return (detailed.isEmpty() ? null : detailed);

  } // createDetailedMetadata

  ////////////////////////////////////////////////////////////

  /** Converts a raw metadata map to structured attributes. */
  private static Map<String, Object> createMetadataAttributes (
    Map<?, ?> metadata
  ) {

    var attributes = new LinkedHashMap<String, Object>();
    metadata.forEach ((key, value) -> {
      var attribute = new LinkedHashMap<String, Object>();
      attribute.put ("type", getType (value));
      attribute.put ("value", getJsonValue (value));
      attributes.put (String.valueOf (key), attribute);
    });
    return (attributes);

  } // createMetadataAttributes

  ////////////////////////////////////////////////////////////

  /** Converts a metadata value to a value supported directly by JSON. */
  private static Object getJsonValue (
    Object value
  ) {

    Object jsonValue;
    if (value == null || value instanceof String || value instanceof Number ||
      value instanceof Boolean) {
      jsonValue = value;
    } // if
    else if (value instanceof Character) {
      jsonValue = value.toString();
    } // else if
    else if (value.getClass().isArray()) {
      var values = new ArrayList<Object>();
      int length = Array.getLength (value);
      for (int i = 0; i < length; i++) values.add (getJsonValue (Array.get (value, i)));
      jsonValue = values;
    } // else if
    else {
      jsonValue = MetadataServices.toString (value);
    } // else

    return (jsonValue);

  } // getJsonValue

  ////////////////////////////////////////////////////////////

  /** Gets the metadata type description used by the metadata panel. */
  private static String getType (
    Object value
  ) {

    String type;
    if (value == null) {
      type = "unknown";
    } // if
    else {
      var valueClass = value.getClass();
      var isArray = valueClass.isArray();
      var className = (isArray ? valueClass.getComponentType().toString() : valueClass.toString());
      type = className.substring (className.lastIndexOf ('.')+1).toLowerCase();
      if (type.equals ("integer")) type = "int";
      if (isArray) type += "[]";
    } // else

    return (type);

  } // getType

  ////////////////////////////////////////////////////////////

  /**
   * Runs a test program that prints context for a data source.
   *
   * @param args the command line arguments.  The first argument is the local
   * file name or network location to read.
   *
   * @throws IOException if the source cannot be opened or read.
   */
  public static void main (String[] args) throws IOException {

    if (args.length == 0) {
      System.err.println ("Usage: DataFileContext <data-source>");
      return;
    } // if

    var context = DataFileContext.create (args[0]);
    System.out.println (context.getJson());

  } // main

  ////////////////////////////////////////////////////////////

} // DataFileContext
