////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractReaderList.java
  PURPOSE: Holds a set of related data readers.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/22
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

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
