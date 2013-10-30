////////////////////////////////////////////////////////////////////////
/*
     FILE: URLTransfer.java
  PURPOSE: To perform data transfers from a URL to a local file.
   AUTHOR: Peter Hollemans
     DATE: 2002/02/16
  CHANGES: 2003/10/26, PFH
           - added to CoastWatch package
           - modified constructor
           2003/11/22, PFH, fixed Javadoc comments
           2004/10/10, PFH, added setupIO()

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.net;

// Imports
// -------
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import noaa.coastwatch.io.*;

/**
 * The <code>URLTransfer</code> class initiates a connection to an
 * Internet host specified by a URL and downloads the content provided
 * by the URL.  The URL may contain username and password information,
 * in which case the URL is opened using a
 * <code>PasswordAuthentication</code> object as the default
 * authenticator for the connection.
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public class URLTransfer
  extends DataTransfer {

  // Variables
  // ---------

  /** The input URL to read. */
  private URL inputUrl;

  /** The output stream to write. */
  private OutputStream output;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new URL transfer with the specified parameters.
   *
   * @param inputUrl the URL to access for content.
   * @param output the output stream for the URL contents.
   */   
  public URLTransfer (
    URL inputUrl,
    OutputStream output
  ) {

    // Check for authentication 
    // ------------------------
    String userInfo = inputUrl.getUserInfo();
    if (userInfo != null) {
      String[] array = userInfo.split (":");
      final String user = array[0];
      final String pass = array[1];
      Authenticator.setDefault (new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication () {
          return (new PasswordAuthentication (user, pass.toCharArray()));
        } // getPasswordAuthentication
      });
    } // if

    // Store values to use later
    // -------------------------
    this.inputUrl = inputUrl;
    this.output = output;

  } // URLTransfer constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Overrides the parent to setup I/O streams.
   *
   * @throws IOException if an error occurred accessing the URL stream.
   */
  protected void setupIO () 
    throws IOException {

    // Set input and output streams
    // ----------------------------
    InputStream input = inputUrl.openStream();
    setStreams (input, output, 8192);

  } // setupIO

  ////////////////////////////////////////////////////////////

} // URLTransfer class

////////////////////////////////////////////////////////////////////////
