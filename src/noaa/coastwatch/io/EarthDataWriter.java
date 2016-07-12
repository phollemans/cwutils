////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataWriter.java
  PURPOSE: Abstract class to setup the functionality of all data
           writer subclasses.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/06/25, PFH, added javadoc, package
           2002/07/12, PFH, added close
           2004/03/23, PFH, modified to use ArrayList rather than Vector
           2004/05/06, PFH, added tracking of flush progress
           2004/09/09, PFH, renamed SatelliteDataWriter to EarthDataWriter
           2014/08/26, PFH
           - Changes: Cleaned up implementation of finalize() to call
             super no matter what happens.
           - Issue: The original finalize() wasn't written correctly.

  CoastWatch Software Library and Utilities
  Copyright 2004-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;

/**
 * All earth data writers obtain earth data from Earth
 * data info and variable objects and format to a data destination.
 * A earth data writer should do the following:
 * <ul>
 *   <li> construct from some type of file or data stream, and a
 *        {@link noaa.coastwatch.util.EarthDataInfo} object </li>
 *   <li> add {@link noaa.coastwatch.util.DataVariable}
 *        objects to the contents </li>
 *   <li> flush current contents to destination </li>
 *   <li> close the destination when no longer needed </li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class EarthDataWriter {

  // Variables
  // ---------
  /** Earth data info object. */
  protected EarthDataInfo info;

  /** Earth data variables. */
  protected List<DataVariable> variables;

  /** The data destination. */
  private String destination;

  /** 
   * The number of variables written so far during the current flush
   * operation.
   */
  protected int writeVariables;

  /** The current variable progress as a value in the range [0..100]. */
  protected int writeProgress;

  /** The name of the variable currently being written. */
  protected String writeVariableName;

  /** The canceled flag, true if the current flush is canceled. */
  protected boolean isCanceled;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the name of the current variable being written in a flush
   * operation.
   *
   * @return the variable name, or null if no variable is being
   * written.
   */
  public String getProgressVariable () { return (writeVariableName); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the progress length.  This may be used in a progress monitor
   * to measure the progress of a flush operation.
   *
   * @return the progress length.
   */
  public int getProgressLength () {

    return (variables.size() * 100);

  } // getProgressLength

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current flush progress.  This may be used in a progress
   * monitor to measure the progress of a flush operation.
   *
   * @return the progress so far in the current flush operation.
   *
   * @see #getProgressLength
   */
  public int getProgress () {

    int progress;
    synchronized (this) {
      progress = writeVariables*100 + writeProgress;
    } // synchronized
    return (progress);

  } // getProgressLength

  ////////////////////////////////////////////////////////////

  /** 
   * Cancels a flush operation in progress.  When a flush is canceled,
   * the writer is no longer usable and subsequent calls to the
   * <code>flush()</code> method will not work.
   */
  public void cancel () {

    isCanceled = true;

  } // cancel

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new earth data writer and initializes the
   * variables to an empty list.
   * 
   * @param destination the data destination.
   */
  protected EarthDataWriter (
    String destination
  ) {
    
    this.destination = destination;
    variables = new ArrayList<DataVariable>();

  } // EarthDataWriter constructor

  ////////////////////////////////////////////////////////////

  /** Gets the earth data destination. */
  public String getDestination () { return (destination); }

  ////////////////////////////////////////////////////////////

  /**
   * Adds a data variable to the writer.  The variable is
   * added to the list of variables to write, but no data is actually
   * written until a flush is performed.
   *
   * @param var the data variable to add.
   * 
   * @see #flush
   */
  public void addVariable (
    DataVariable var
  ) {

    variables.add (var);

  } // addVariable

  ////////////////////////////////////////////////////////////

  /**
   * Flushes all unwritten data to the destination.
   *
   * @throws IOException if the data destination had I/O errors.
   */
  public abstract void flush () throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Closes the writer and frees any resources.  The <code>flush</code>
   * method is called prior to closing.
   *
   * @throws IOException if the data destination had I/O errors.
   */
  public abstract void close () throws IOException;

  ////////////////////////////////////////////////////////////

  /**
   * Closes the resources associated with the data destination.
   */
  @Override
  protected void finalize () throws Throwable {

    try { close(); }
    finally { super.finalize(); }

  } // finalize

  ////////////////////////////////////////////////////////////

} // EarthDataWriter class

////////////////////////////////////////////////////////////////////////
