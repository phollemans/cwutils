////////////////////////////////////////////////////////////////////////
/*

     File: URLTransfer.java
   Author: Peter Hollemans
     Date: 2002/02/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.net;

// Imports
// -------
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import noaa.coastwatch.io.DataTransfer;

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
