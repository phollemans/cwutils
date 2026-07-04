/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

/**
 * The <code>Request</code> interface defines a typed request with
 * content.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public interface Request {

  /**
   * Gets the request type ID.
   *
   * @return the request type ID.
   */
  String getTypeID();

  /**
   * Gets the request content.
   *
   * @return the request content.
   */
  Object getContent();

} // Request interface
