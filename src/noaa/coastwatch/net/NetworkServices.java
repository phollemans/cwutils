////////////////////////////////////////////////////////////////////////
/*

     File: NetworkServices.java
   Author: Peter Hollemans
     Date: 2019/06/21

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <b>NetworkServices</b> class defines various static methods relating
 * to network operations.
 *
 * @author Peter Hollemans
 * @since 3.5.1
 */
public class NetworkServices {

  private static final Logger LOGGER = Logger.getLogger (NetworkServices.class.getName());

  ////////////////////////////////////////////////////////////

  /**
   * Sets up the <b>javax.net.ssl.HttpsURLConnection</b> class with a default
   * SSL socket factory and host name verifier that accepts SSL certificate
   * chains without validating them.  This should only be done as a last
   * resort override if the existing system fails to validate the certificate.
   */
  public static void setupTrustingSSLManager () {

    try {

      // Install trust manager that doesn't validate certificate
      // -------------------------------------------------------
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return (null);
          } // getAcceptedIssuers
          public void checkClientTrusted (X509Certificate[] certs, String authType) { }
          public void checkServerTrusted (X509Certificate[] certs, String authType) { }
        }
      };

      SSLContext sc = SSLContext.getInstance ("SSL");
      sc.init (null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory (sc.getSocketFactory());

      // Install host name verifier that doesn't check the name
      // ------------------------------------------------------
      HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify (String hostname, SSLSession session) {
          return (true);
        } // verify
      };

      HttpsURLConnection.setDefaultHostnameVerifier (allHostsValid);

    } // try

    catch (Exception e) {
      LOGGER.log (Level.FINE, "Exception when modifying SSL configuration", e);
      throw new RuntimeException ("Error in trust setup");
    } // catch

  } // setupTrustingSSLManager

  ////////////////////////////////////////////////////////////

  private NetworkServices () { }

  ////////////////////////////////////////////////////////////

} // NetworkServices class

////////////////////////////////////////////////////////////////////////

