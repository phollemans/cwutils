/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

/**
 * The <code>RequestHandler</code> interface defines methods for
 * checking and handling typed requests.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public interface RequestHandler {

  /**
   * Handles a request.
   *
   * @param request the request to handle.
   */
  void handleRequest (Request request);

  /**
   * Determines if this handler can handle a request.
   *
   * @param request the request to check.
   *
   * @return true if this handler can handle the request, or false otherwise.
   */
  boolean canHandleRequest (Request request);

} // RequestHandler interface
