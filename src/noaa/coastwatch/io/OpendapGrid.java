////////////////////////////////////////////////////////////////////////
/*
     FILE: OpendapGrid.java
  PURPOSE: Reads CoastWatch-style data through the OPeNDAP interface.
   AUTHOR: Peter Hollemans
     DATE: 2006/05/25
  CHANGES: 2008/02/18, PFH, modified to use opendap.dap classes
           2016/03/16, PFH
           - Changes: Updated to use new opendap.dap.DConnect2 class and call
             DArray.getClearName().
           - Issue: The Java NetCDF library uses the newer OPeNDAP Java
             classes and they were conflicting with the older API that we were
             using, so we had to remove the old dap2 jar and conform to the 
             API found in the classes in the latest toolsUI jar file.

  CoastWatch Software Library and Utilities
  Copyright 2006-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.Grid;
import opendap.dap.DConnect2;
import opendap.dap.DVector;
import opendap.dap.DataDDS;
import opendap.dap.PrimitiveVector;

/** 
 * The <code>OpendapGrid</code> is an intelligent OPeNDAP client
 * that delivers data by listening for {@link
 * noaa.coastwatch.util.DataVariable#setAccessHint} calls and
 * only downloads the data which is specified within that call.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class OpendapGrid
  extends Grid {

  // Variables
  // ---------

  /** The OPeNDAP connection used for data. */
  private DConnect2 connect;

  /** The starting data coordinate from the last access hint. */
  private int[] start;

  /** The ending data coordinate from the last access hint. */
  private int[] end;

  /** The stride along each dimension from the last access hint. */
  private int[] stride;

  /** The actual number of columns from the last access hint. */
  private int dataCols;

  /** The bounding rectangle for this data. */
  private Rectangle dataRect;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new grid.
   *
   * @param grid the prototype grid upon which to base this one.
   * @param url the OPeNDAP dataset URL to use for data.
   */
  public OpendapGrid (
    Grid grid,
    String url
  ) throws IOException {

    // Initialize
    // ----------
    super (grid);
    connect = new DConnect2 (url, true);

    // Create zero size start and end
    // ------------------------------
    start = new int[] {0, 0};
    end = new int[] {-1, -1};
    stride = new int[] {1, 1};

  } // OpendapGrid constructor

  ////////////////////////////////////////////////////////////

  public void setAccessHint (
    int[] start,
    int[] end,
    int[] stride
  ) {

    // Check for subset
    // ----------------
    if (dataRect != null && Arrays.equals (this.stride, stride)) {
      if (dataRect.contains (start[0], start[1], end[0] - start[0] + 1, 
        end[1] - start[1] + 1))
        return;
    } // if

    // Create constraint expression
    // ----------------------------
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < start.length; i++) {
      buffer.append ("[");
      buffer.append (Integer.toString (start[i]));
      buffer.append (":");
      buffer.append (Integer.toString (stride[i]));
      buffer.append (":");
      buffer.append (Integer.toString (end[i]));
      buffer.append ("]");
    } // for
    String constraint = buffer.toString();

    // Read data using constraint
    // --------------------------
    try {
      DataDDS dds = connect.getData ("?" + getName() + constraint, null);
      PrimitiveVector vector = 
        ((DVector) dds.getVariable (getName())).getPrimitiveVector();
      Object data = vector.getInternalStorage();
      setData (data);
    } // try
    catch (Exception e) {
      throw new RuntimeException ("Error getting data: " + e.getMessage());
    } // catch
    
    // Save constraint values
    // ----------------------
    this.start = (int[]) start.clone();
    this.end = (int[]) end.clone();
    this.stride = (int[]) stride.clone();
    dataCols = 
      (int) Math.ceil (((end[COLS] - start[COLS] + 1)/(float)stride[COLS]));
    dataRect = new Rectangle (
      start[0], start[1], 
      end[0] - start[0] + 1, end[1] - start[1] + 1
    );

  } // setAccessHint

  ////////////////////////////////////////////////////////////

  public void setValue (
    int row,
    int col,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values");

  } // setValue

  ////////////////////////////////////////////////////////////

  public void setValue (
    DataLocation loc,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values");

  } // setValue

  ////////////////////////////////////////////////////////////

  public void setValue (
    int index,
    double val
  ) {

    throw new UnsupportedOperationException ("Cannot set values");

  } // setValue

  ////////////////////////////////////////////////////////////

  public double interpolate (
    DataLocation loc
  ) {

    throw new UnsupportedOperationException ("Cannot interpolate values");

  } // interpolate

  ////////////////////////////////////////////////////////////

  public Object getData () { 

    throw new UnsupportedOperationException ("Cannot get data");

  } // getData

  ////////////////////////////////////////////////////////////

  public Object getData (
    int[] start,
    int[] count
  ) {

    throw new UnsupportedOperationException ("Cannot get data");

  } // getData

  ////////////////////////////////////////////////////////////

  public double getValue (
    int row,
    int col
  ) {

    if (row < start[ROWS] || row > end[ROWS] ||
        col < start[COLS] || col > end[COLS]) return (Double.NaN);
    int index = ((row - start[ROWS])/stride[ROWS]) * dataCols + 
      ((col - start[COLS])/stride[COLS]);
    return (getValue (index, data));

  } // getValue

  ////////////////////////////////////////////////////////////

  public double getValue (
    int index
  ) {

    return (getValue (index/dims[COLS], index%dims[COLS]));

  } // getValue

  ////////////////////////////////////////////////////////////

} // OpendapGrid class

////////////////////////////////////////////////////////////////////////
