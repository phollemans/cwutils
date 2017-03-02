////////////////////////////////////////////////////////////////////////
/*

     File: DataTransferListener.java
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
package noaa.coastwatch.io;

// Imports
// -------
import java.util.EventListener;
import noaa.coastwatch.io.DataTransferEvent;

/**
 * A data transfer listener receives data transfer events and performs
 * some appripriate action in response.  Data transfer events are used
 * to signal the details of a data transfer, such as starting,
 * transfer progress, and ending.
 *
 * @author Peter Hollemans
 * @since 3.1.5
 */
public interface DataTransferListener
  extends EventListener {

  ////////////////////////////////////////////////////////////

  /** Responds to a data transfer starting. */
  public void transferStarted (
    DataTransferEvent event
  );

  ////////////////////////////////////////////////////////////

  /** Responds to a data transfer in progress. */
  public void transferProgress (
    DataTransferEvent event
  );

  ////////////////////////////////////////////////////////////

  /** Responds to a data transfer ending. */
  public void transferEnded (
    DataTransferEvent event
  );

  ////////////////////////////////////////////////////////////

  /** Responds to a data transfer error. */
  public void transferError (
    DataTransferEvent event
  );

  ////////////////////////////////////////////////////////////

} // DataTransferListener class

////////////////////////////////////////////////////////////////////////
