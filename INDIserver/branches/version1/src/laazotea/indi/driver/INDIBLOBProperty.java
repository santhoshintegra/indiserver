/*
 *  This file is part of INDI Driver for Java.
 * 
 *  INDI Driver for Java is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation, either version 3 of 
 *  the License, or (at your option) any later version.
 * 
 *  INDI Driver for Java is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with INDI Driver for Java.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package laazotea.indi.driver;

import laazotea.indi.Constants;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.INDIDateFormat;

/**
 * A class representing a INDI BLOB Property.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDIBLOBProperty extends INDIProperty {

  /**
   * Constructs an instance of <code>INDIBLOBProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>group</code>, <code>state</code>, <code>permission</code> and <code>timeout</code>.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param group The group of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @param timeout The timeout of the Property 
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDIBLOBProperty(INDIDriver driver, String name, String label, String group, PropertyStates state, PropertyPermissions permission, int timeout) throws IllegalArgumentException {
    super(driver, name, label, group, state, permission, timeout);
  }

  /**
   * Constructs an instance of <code>INDIBLOBProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>group</code>, <code>state</code>, <code>permission</code> and a 0 timeout.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param group The group of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDIBLOBProperty(INDIDriver driver, String name, String label, String group, PropertyStates state, PropertyPermissions permission) throws IllegalArgumentException {
    super(driver, name, label, group, state, permission, 0);
  }

  /**
   * Constructs an instance of <code>INDIBLOBProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>state</code>, <code>permission</code> and a 0 timeout and default group.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDIBLOBProperty(INDIDriver driver, String name, String label, PropertyStates state, PropertyPermissions permission) throws IllegalArgumentException {
    super(driver, name, label, null, state, permission, 0);
  }

  /**
   * Constructs an instance of <code>INDIBLOBProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>state</code>, <code>permission</code> and a 0 timeout, a default group and a label equal to its <code>name</code>
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDIBLOBProperty(INDIDriver driver, String name, PropertyStates state, PropertyPermissions permission) throws IllegalArgumentException {
    super(driver, name, null, null, state, permission, 0);
  }

  @Override
  protected String getXMLPropertyDefinitionInit() {
    String xml = "<defBLOBVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" label=\"" + getLabel() + "\" group=\"" + getGroup() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" perm=\"" + Constants.getPropertyPermissionAsString(getPermission()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertyDefinitionInit(String message) {
    String xml = "<defBLOBVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" label=\"" + getLabel() + "\" group=\"" + getGroup() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" perm=\"" + Constants.getPropertyPermissionAsString(getPermission()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\" message=\"" + message + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertyDefinitionEnd() {
    String xml = "</defBLOBVector>";

    return xml;
  }

  @Override
  protected String getXMLPropertySetInit() {
    String xml = "<setBLOBVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertySetInit(String message) {
    String xml = "<setBLOBVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\" message=\"" + message + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertySetEnd() {
    String xml = "</setBLOBVector>";

    return xml;
  }
}
