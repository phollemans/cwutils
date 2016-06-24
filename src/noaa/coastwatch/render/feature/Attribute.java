////////////////////////////////////////////////////////////////////////
/*
     FILE: Attribute.java
  PURPOSE: Atribute values for FeatureSource objects.
   AUTHOR: Peter Hollemans
     DATE: 2016/06/23
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

/*
 * An <code>Attribute</code> acts as an annotation element for 
 * <code>Feature</code> objects produced by a <code>FeatureSource</code>.
 * All attributes have a name and data type.  Once created, attributes are
 * immutable.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class Attribute {

  // Variables
  // ---------

  /** The attribute name. */
  private String name;

  /** The attribute type. */
  private Class type;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new attribute.
   *
   * @param the attribute name.
   * @param the attribute data type.
   */
  public Attribute (
    String name,
    Class type
  ) {

    this.name = name;
    this.type = type;

  } // Attribute constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the attribute name.
   * 
   * @return the attribute name.
   */
  public String getName () { return (name); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the attribute data type.
   * 
   * @return the attribute data type.
   */
  public Class getType () { return (type); }

 ////////////////////////////////////////////////////////////

} // Attribute class

////////////////////////////////////////////////////////////////////////
