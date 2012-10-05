////////////////////////////////////////////////////////////////////////
/*
     FILE: CWF.java
  PURPOSE: To allow access to CoastWatch IMGMAP format files from Java
           via the native CWF library in C.
   AUTHOR: Mark Robinson
     DATE: 2002/03/19
  CHANGES: 2002/03/30, MSR, added projection functions, more constants
           2002/05/13, PFH, added javadoc, package, and reformatted
           2002/11/25, PFH, added updateNavigation
           2003/11/22, PFH, fixed Javadoc comments
           2004/06/09, PFH, moved updateNavigation() to CWFReader

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.geom.*;
import java.io.*;

/**
 * The static routines in the CoastWatch file class handle the reading
 * and writing of data for a CoastWatch IMGMAP format file.  A CWF
 * file stores a 2D grid of satellite data pixels as well as
 * associated metadata such as date, time, projection information, and
 * so on.  The class is simply a Java wrapper for the CWF library in C.
 */
public class CWF {

  // Shared library load
  // -------------------
  static {
    System.loadLibrary ("CWF");
  } // static

  // Type constants
  // --------------
  /** CWF type constant for byte data. */
  public static final int CW_BYTE = 0;

  /** CWF type constant for character data. */
  public static final int CW_CHAR = 1;

  /** CWF type constant for short (16-bit) data. */
  public static final int CW_SHORT = 2;

  /** CWF type constant for float (32-bit) data. */
  public static final int CW_FLOAT = 3;

  // File constants
  // --------------
  /** Overwrite if the file already exists. */
  public static final int CW_CLOBBER = 0;

  /** Do not overwrite if the file already exists. */
  public static final int CW_NOCLOBBER = 1;

  /** Open with read permissions only. */
  public static final int CW_NOWRITE = 0;

  /** Open with read/write permissions. */
  public static final int CW_WRITE = 1;

  // Projection constants
  // --------------------
  /** Unmapped satellite swath projection. */
  public static final int UNMAPPED = 0;

  /** Mercator projection. */
  public static final int MERCATOR = 1;

  /** Polar stereographic projection. */
  public static final int POLAR = 2;

  /** Linear latitude/longitude projection. */
  public static final int LINEAR = 3;

  /** Nothern hemisphere. */
  public static final int NORTH = 1;

  /** Southern hemisphere. */
  public static final int SOUTH = -1;

  /** CWF Earth radius in kilometers. */
  public static final double EARTH_RADIUS = 6371.2;

  // Data constants
  // --------------
  /** Value signifying missing or invalid float data. */
  public final static float CW_BADVAL = -999.0f;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new CWF file and assigns it an ID.  The new file is
   * created with the given name and creation mode.  The creation mode
   * is used to determine if an existing file with the same name
   * should be overwritten.
   * 
   * @param path the full path and name for the file to create.
   * @param create_mode the creation mode used to create the file.
   * The mode may have the value <code>CW_CLOBBER</code> (overwrite an
   * existing file with the same name) or <code>CW_NOCLOBBER</code>
   * (do not overwrite).
   *
   * @return the integer ID of the file.  The ID may be used in
   * subsequent calls to routines to uniquely identify the file.
   *
   * @see #open
   * @see #close
   */
  public static native int create (
    String path,
    int create_mode
  ); // create

  ////////////////////////////////////////////////////////////

  /**
   * Opens an existing CWF file and assigns it an ID.  
   * 
   * @param path the full path and name for the file to open.
   * @param open_mode the access mode to use in opening the file.  The
   * access mode determines whether the file is opened for reading
   * only or for reading and updating.  The mode may have the value
   * <code>CW_NOWRITE</code> (read only) or <code>CW_WRITE</code>
   * (read and write).
   *
   * @return the integer ID of the file.  The ID may be used in
   * subsequent calls to routines to uniquely identify the file.
   *
   * @see #create
   * @see #close
   */
  public static native int open (
    String path,
    int open_mode
  ); // open

  ////////////////////////////////////////////////////////////

  /** 
   * Takes the CWF file out of define mode.  When a new CWF file
   * is created, it is in a definition mode while the various
   * attributes such as dimensions, variable, date, time, and so on
   * are defined and written to the file.  Once define mode has
   * ended, file data may be read or written.
   *
   * @param cw_id the CWF file ID.
   */
  public static native void enddef (
    int cw_id
  ); // enddef

  ////////////////////////////////////////////////////////////

  /**
   * Closes a CWF file.  Once the file is closed, the CWF file ID is
   * no longer valid.  The file must be re-opened with a call to
   * <code>open</code>.
   *
   * @param cw_id the CWF file ID.
   *
   * @see #open
   * @see #create
   */
  public static native void close (
    int cw_id
  ); // close

  ////////////////////////////////////////////////////////////

  /**
   * Defines a new dimension.  The dimensions determine the size of
   * the 2D data grid that the file contains.
   * 
   * @param cw_id the CWF file ID.
   * @param dimension_name the name of the dimension.  Only the names
   * <code>rows</code> and <code>columns</code> are accepted.
   * @param size the length of the dimension.  The length is a
   * positive integer specifying the size of the 2D grid in the named
   * dimension, for example a common length is 512.
   * 
   * @return the integer dimension ID. The ID may be used in
   * subsequent calls to routines to uniquely identify the dimension.
   */
  public static native int define_dimension (
    int cw_id,
    String dimension_name,
    int size
  ); // define_dimension

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the dimension ID.  The routine translates a string name into
   * a unique dimension ID for the CWF file.
   * 
   * @param cw_id the CWF file ID.
   * @param dimension_name the name of the dimension.  Only the names
   * <code>rows</code> and <code>columns</code> are accepted.
   * 
   * @return the integer dimension ID. The ID may be used in
   * subsequent calls to routines to uniquely identify the dimension.
   */
  public static native int inquire_dimension_id (
    int cw_id,
    String dimension_name
  ); // inquire_dimension_id

  ////////////////////////////////////////////////////////////

  /**
   * Gets the dimension length.
   * 
   * @param cw_id the CWF file ID.
   * @param dimension_id the dimension ID.  Dimension IDs are attained
   * from a call to <code>inquire_dimension_id</code> or
   * <code>define_dimension</code>.
   *
   * @return the dimension length.  
   *
   * @see #inquire_dimension_id
   * @see #define_dimension
   */
  public static native int inquire_dimension_length (
    int cw_id,
    int dimension_id
  ); // inquire_dimension_length

  ////////////////////////////////////////////////////////////

  /**
   * Gets the dimension name.
   * 
   * @param cw_id the CWF file ID.
   * @param dimension_id the dimension ID.  Dimension IDs are attained
   * from a call to <code>inquire_dimension_id</code> or
   * <code>define_dimension</code>.
   *
   * @return the dimension name.
   *
   * @see #inquire_dimension_id
   * @see #define_dimension
   */
  public static native String inquire_dimension_name (
    int cw_id,
    int dimension_id
  ); // inquire_dimension_name

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new variable in the CWF file.  A variable may only be
   * created in define mode.
   * 
   * @param cw_id the CWF file ID.
   * @param variable_name the name of the variable to create.
   * @param dimension_id an array of dimension IDs to use for the variable.
   *
   * @return the ID of the newly created variable.
   *
   * @see #enddef
   */
  public static native int define_variable (
    int cw_id,
    String variable_name,
    int dimension_id[]
  ); // define_variable

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable ID.  
   *
   * @param cw_id the CWF file ID.
   * @param variable_name the name of the variable.
   * 
   * @return the integer variable ID. The ID may be used in
   * subsequent calls to routines to uniquely identify the variable.
   */
  public static native int inquire_variable_id (
    int cw_id,
    String variable_name
  ); // inquire_variable_id

  ////////////////////////////////////////////////////////////

  /**
   * Gets the variable name.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.  Variable IDs are attained
   * from a call to <code>inquire_variable_id</code> or
   * <code>define_variable</code>.
   *
   * @return the variable name.
   *
   * @see #inquire_variable_id
   * @see #define_variable
   */
  public static native String inquire_variable_name (
    int cw_id,
    int variable_id
  ); // inquire_variable_name

  ////////////////////////////////////////////////////////////

  /**
   * Gets the variable type.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.  Variable IDs are attained
   * from a call to <code>inquire_variable_id</code> or
   * <code>define_variable</code>.
   *
   * @return the variable type code (see the CWF type constants).
   *
   * @see #inquire_variable_id
   * @see #define_variable
   */
  public static native int inquire_variable_type (
    int cw_id,
    int variable_id
  ); // inquire_variable_type

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of attributes for the variable.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.  Variable IDs are attained
   * from a call to <code>inquire_variable_id</code> or
   * <code>define_variable</code>.
   *
   * @return the variable attribute count.
   *
   * @see #inquire_variable_id
   * @see #define_variable
   */
  public static native int inquire_variable_attributes (
    int cw_id,
    int variable_id
  ); // inquire_variable_attributes

  ////////////////////////////////////////////////////////////

  /**
   * Gets the variable dimension IDs.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.  Variable IDs are attained
   * from a call to <code>inquire_variable_id</code> or
   * <code>define_variable</code>.
   *
   * @return an array of variable dimension IDs.
   *
   * @see #inquire_variable_id
   * @see #define_variable
   */
  public static native int[] inquire_variable_dimension_ids (
    int cw_id,
    int variable_id
  ); // inquire_variable_dimension_ids

  ////////////////////////////////////////////////////////////

  /**
   * Writes float data values to the CWF file.  The data is written to
   * the CWF file at the specified starting point using values from
   * the data array.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param start_point array of integers to designate the origin in
   * the file for writing.
   * @param size array of integers to designate the dimensions of the
   * data array.
   * @param array array of values to write.
   *
   * @see #create
   * @see #define_variable
   * @see #enddef
   */
  public static native void put_variable (
    int cw_id,
    int variable_id,
    int start_point[],
    int size[],
    float array[][]
  ); // put_variable

  ////////////////////////////////////////////////////////////

  /**
   * Writes byte data values to the CWF file.  The data is written to
   * the CWF file at the specified starting point using values from
   * the data array.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param start_point array of integers to designate the origin in
   * the file for writing.
   * @param size array of integers to designate the dimensions of the
   * data array.
   * @param array array of values to write.
   *
   * @see #create
   * @see #define_variable
   * @see #enddef
   */
  public static native void put_variable (
    int cw_id,
    int variable_id,
    int start_point[],
    int size[],
    byte array[][]
  ); // put_variable

  ////////////////////////////////////////////////////////////

  /**
   * Reads float data values from the CWF file.  The data is read from 
   * the CWF file at the specified starting point.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param start_point array of integers to designate the origin in
   * the file for reading.
   * @param size array of integers to designate the dimensions of the
   * data to read.
   *
   * @return array array of values read.
   *
   * @see #open
   * @see #inquire_variable_id
   */
  public static native float[][] get_variable_float (
    int cw_id,
    int variable_id,
    int start_point[],
    int size[]
  ); // get_variable_float

  ////////////////////////////////////////////////////////////

  /**
   * Reads byte data values from the CWF file.  The data is read from 
   * the CWF file at the specified starting point.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param start_point array of integers to designate the origin in
   * the file for reading.
   * @param size array of integers to designate the dimensions of the
   * data to read.
   *
   * @return array array of values read.
   *
   * @see #open
   * @see #inquire_variable_id
   */
  public static native byte[][] get_variable_byte (
    int cw_id,
    int variable_id,
    int start_point[],
    int size[]
  ); // get_variable_byte

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute name.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_id the attribute ID.
   *
   * @return the attribute name.
   */
  public static native String inquire_attribute_name (
    int cw_id,
    int variable_id,
    int attribute_id
  ); // inquire_attribute_name

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute type.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute the attribute name.
   *
   * @return the attribute type (see the CWF type constants).
   */
  public static native int inquire_attribute_type (
    int cw_id,
    int variable_id,
    String attribute
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of values stored for an attribute.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute the attribute name.
   *
   * @return the number of values stored for the attribute.
   */
  public static native int inquire_attribute_num (
    int cw_id,
    int variable_id,
    String attribute
  ); // inquire_attribute_num

  ////////////////////////////////////////////////////////////

  /**
   * Gets the attribute ID.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute the attribute name.
   *
   * @return the attribute ID.
   */
  public static native int inquire_attribute_id (
    int cw_id,
    int variable_id,
    String attribute
  ); // inquire_attribute_id

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the attribute value as a string.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   *
   * @return the attribute value as a string.
   */ 
  public static native String get_attribute_string (
    int cw_id,
    int variable_id,
    String attribute_name
  ); // get_attribute_string

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the attribute value as a float.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   *
   * @return the attribute value as a float.
   */ 
  public static native float get_attribute_float (
    int cw_id,
    int variable_id,
    String attribute_name
  ); // get_attribute_float

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the attribute value as a short.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   *
   * @return the attribute value as a short.
   */ 
  public static native short get_attribute_short (
    int cw_id,
    int variable_id,
    String attribute_name
  ); // get_attribute_short

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the attribute value as a short.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   * @param value the new attribute value.
   */ 
  public static native void put_attribute (
    int cw_id,
    int variable_id,
    String attribute_name,
    short value
  ); // put_attribute

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the attribute value as a string.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   * @param value the new attribute value.
   */ 
  public static native void put_attribute (
    int cw_id,
    int variable_id,
    String attribute_name,
    String value
  ); // put_attribute

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the attribute value as a float.
   *
   * @param cw_id the CWF file ID.
   * @param variable_id the variable ID.
   * @param attribute_name the attribute name.
   * @param value the new attribute value.
   */ 
  public static native void put_attribute (
    int cw_id,
    int variable_id,
    String attribute_name,
    float value
  ); // put_attribute

  ////////////////////////////////////////////////////////////

  /** 
   * Initializes projection calculations for a CWF file.  All
   * subsequent calls to projection routines will use the projection
   * information from the specified CWF file.
   * 
   * @param cw_id the CWF file ID.
   *
   * @see #projection_info
   * @see #get_latitiude_longitude
   * @see #get_pixel
   */
  public static native void init_projection (
    int cw_id
  ); // init_projection

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the projection information for the currently active
   * projection calculations.
   *
   * @return a <code>CWFProjectionInfo</code> object.
   *
   * @see #init_projection
   */
  public static native CWFProjectionInfo projection_info ();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the geographic coordinates of a data pixel.  The currently
   * active projection is used to perform the calculation.
   *
   * @param i the data pixel column using 1-relative indexing.
   * @param j the data pixel row using 1-relative indexing.
   *
   * @return an array containing the [latitude, longitude] of the data
   * pixel.
   *
   * @see #init_projection
   * @see #get_pixel
   */
  public static native double[] get_latitiude_longitude (
    double i,
    double j
  ); // get_latitiude_longitude

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data coordinates of a data pixel.  The currently
   * active projection is used to perform the calculation.
   *
   * @param latitude the data pixel latitude.
   * @param longitude the data pixel longitude.
   *
   * @return an array containing the [column, row] of the data
   * pixel.
   *
   * @see #init_projection
   * @see #get_latitiude_longitude
   */
  public static native double[] get_pixel (
    double latitude,
    double longitude
  ); // get_pixel

  ////////////////////////////////////////////////////////////

  private CWF () { }

  ////////////////////////////////////////////////////////////

} // CWF class

////////////////////////////////////////////////////////////////////////
