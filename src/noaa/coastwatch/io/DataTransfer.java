////////////////////////////////////////////////////////////////////////
/*
     FILE: DataTransfer.java
  PURPOSE: To perform data transfers between input and output streams.
   AUTHOR: Peter Hollemans
     DATE: 2002/02/16
  CHANGES: 2003/10/26, PFH, added to the Coastwatch package
           2004/10/10, PFH, added setupIO(), modified to use ArrayList
           2006/01/13, PFH, updated close() to check for null references

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import java.util.*;

/**
 * The data transfer class allows for the generic connection between
 * input and output streams.  Data is copied from the input stream to
 * the output stream until no more data is available.  The progress of
 * data copying may be monitored with a data transfer listener.
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public class DataTransfer
  implements Runnable {

  // Variables
  // ---------
  /** The input stream for reading. */
  private InputStream input;
 
  /** The output stream for writing. */
  private OutputStream output;

  /** The data buffer size. */
  private int bufferSize;

  /** The total number of bytes transferred. */
  private int transferred;

  /** The transfer start time. */
  private long startTime;

  /** The list of event listeners. */
  private List listeners;

  /** The transfer abort flag. */
  private boolean abort;

  ////////////////////////////////////////////////////////////

  /** Gets the current count of transferred data in bytes. */
  public int getTransferred () { return (transferred); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the transfer start time in milliseconds.  If the data
   * transfer is not yet running, the start time is -1.
   */
  public long getStartTime () { return (startTime); }

  ////////////////////////////////////////////////////////////

  /** Gets the average transfer rate in kilobytes per second. */
  public double getRate () { 
  
    return ((transferred / 1024.0) / 
      ((System.currentTimeMillis() - startTime) / 1000.0)); 

  } // getRate

  ////////////////////////////////////////////////////////////

  /** Adds a data transfer listener to the list. */
  public void addDataTransferListener (
    DataTransferListener listener
  ) {

    if (!listeners.contains (listener)) listeners.add (listener);

  } // addDataTransferListener

  ////////////////////////////////////////////////////////////

  /** Removes a data transfer listener from the list. */
  public void removeDataTransferListener (
    DataTransferListener listener
  ) {

    listeners.remove (listener);

  } // removeDataTransferListener

  ////////////////////////////////////////////////////////////

  /** Creates a new empty transfer. */
  protected DataTransfer () {

    listeners = new ArrayList();
    startTime = -1;
    transferred = 0;
    abort = false;

  } // DataTransfer constructor

  ////////////////////////////////////////////////////////////

  /** Initializes a transfer with new input and output streams. */
  protected void setStreams (
    InputStream input,
    OutputStream output,
    int bufferSize
  ) {

    this.input = input;
    this.output = output;
    this.bufferSize = bufferSize;

  } // setStreams

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new data transfer with the specified parameters.
   * 
   * @param input the input stream to read.
   * @param output the output stream to write.
   * @param bufferSize the size of the buffer to use for each transfer.
   */
  public DataTransfer (
    InputStream input,
    OutputStream output,
    int bufferSize
  ) {

    this();
    setStreams (input, output, bufferSize);

  } // DataTransfer

  ////////////////////////////////////////////////////////////

  /** 
   * Performs any setup necessary before I/O actually occurs.  This
   * method is called from <code>run()</code> before any actual data
   * is transferred.  By default, it does nothing unless overridden in
   * the child class.  The state after this method runs should be that
   * the input and output streams are assigned valid stream values.
   */
  protected void setupIO () 
    throws IOException { 

    // do nothing

  } // setupIO

  ////////////////////////////////////////////////////////////

  /**
   * Starts the transfer of data from the input stream to the output
   * stream.
   */
  public void run () {

    // Initialize
    // ----------
    DataTransferEvent event = new DataTransferEvent (this);
    startTime = System.currentTimeMillis();

    try {

      // Start transfer
      // --------------
      for (Iterator iter = listeners.iterator(); iter.hasNext();)
        ((DataTransferListener) iter.next()).transferStarted (event);

      // Call setup method
      // -----------------
      setupIO();

      // Create buffer
      // -------------
      byte[] buffer = new byte[bufferSize];

      // Read until end of file
      // ----------------------
      boolean eof = false;
      while (!eof) {

        // Read input
        // ----------
        int bytesRead = input.read (buffer);
        eof = (bytesRead == -1);

        // Write output
        // ------------
        if (!eof) {
          output.write (buffer, 0, bytesRead);
          transferred += bytesRead;
          for (Iterator iter = listeners.iterator(); iter.hasNext();)
            ((DataTransferListener) iter.next()).transferProgress (event);
        } // if

        // Check abort flag
        // ----------------
        if (abort) throw new IOException ("Transfer aborted");
      
      } // while

      // Send transfer ended events
      // --------------------------
      for (Iterator iter = listeners.iterator(); iter.hasNext();)
        ((DataTransferListener) iter.next()).transferEnded (event);

    } // try

    // Sent transfer error events
    // --------------------------
    catch (IOException e) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();)
        ((DataTransferListener) iter.next()).transferError (event);
    } // catch

  } // run

  ////////////////////////////////////////////////////////////

  /** 
   * Aborts a transfer in progress.  This is only useful if the transfer
   * is taking place in a separate thread.
   */
  public void abort () { abort = true; }

  ////////////////////////////////////////////////////////////

  /** Closes the input and output streams. */
  public void close () throws IOException {

    if (input != null) input.close();
    if (output != null) output.close();

  } // close

  ////////////////////////////////////////////////////////////

} // DataTransfer class

////////////////////////////////////////////////////////////////////////
