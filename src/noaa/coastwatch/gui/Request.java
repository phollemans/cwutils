/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

/**
 *
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public interface Request {

  String getTypeID();
  Object getContent();

} // Request interface