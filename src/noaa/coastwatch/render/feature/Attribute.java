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

  /** The attribute type. */
  private String units;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new attribute.
   *
   * @param name the attribute name.
   * @param type the attribute data type.
   * @param units the attribute units or null for no units.
   */
  public Attribute (
    String name,
    Class type,
    String units
  ) {

    this.name = name;
    this.type = type;
    this.units = units;

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

  /** 
   * Gets the attribute units.
   * 
   * @return the attribute or null if the attribute has no units.
   */
  public String getUnits () { return (units); }

 ////////////////////////////////////////////////////////////

} // Attribute class

////////////////////////////////////////////////////////////////////////
