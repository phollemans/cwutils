////////////////////////////////////////////////////////////////////////
/*

     File: DataTransferAdapter.java
   Author: Peter Hollemans
     Date: 2002/02/17

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
package noaa.coastwatch.io;

// Imports
// -------
import noaa.coastwatch.io.DataTransferEvent;
import noaa.coastwatch.io.DataTransferListener;

/**
 * A data transfer adapter provides default implementations for a data
 * transfer listener.
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public class DataTransferAdapter
  implements DataTransferListener {

  ////////////////////////////////////////////////////////////

  public void transferStarted (DataTransferEvent event) { }

  ////////////////////////////////////////////////////////////

  public void transferProgress (DataTransferEvent event) { }

  ////////////////////////////////////////////////////////////

  public void transferEnded (DataTransferEvent event) { } 

  ////////////////////////////////////////////////////////////

  public void transferError (DataTransferEvent event) { }

  ////////////////////////////////////////////////////////////

} // DataTransferAdapter class

////////////////////////////////////////////////////////////////////////
