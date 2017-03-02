////////////////////////////////////////////////////////////////////////
/*

     File: AbstractReaderList.java
   Author: Peter Hollemans
     Date: 2006/05/22

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
import java.util.Date;
import noaa.coastwatch.io.ReaderList;

/**
 * The <code>AbstractReaderList</code> class implements some of the
 * more universal methods for a <code>ReaderList</code>.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public abstract class AbstractReaderList 
  implements ReaderList {

  ////////////////////////////////////////////////////////////

  public int getClosestIndex (
    Date date
  ) {

    long targetTime = date.getTime();
    long minDiff = Long.MAX_VALUE;
    int closestIndex = -1;
    for (int i = 0; i < size(); i++) {
      long diff = Math.abs (getStartDate (i).getTime() - targetTime);
      if (diff < minDiff) {
        minDiff = diff;
        closestIndex = i;
      } // if
    } // for

    return (closestIndex);

  } // getClosestIndex

  ////////////////////////////////////////////////////////////

} // AbstractReaderList class

////////////////////////////////////////////////////////////////////////
