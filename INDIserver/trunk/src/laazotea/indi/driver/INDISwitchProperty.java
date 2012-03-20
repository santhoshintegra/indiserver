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

import java.util.List;
import laazotea.indi.Constants;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.INDIDateFormat;

/**
 * A class representing a INDI Switch Property.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDISwitchProperty extends INDIProperty {

  /**
   * The current Rule for this Switch Property.
   */
  private SwitchRules rule;

  /**
   * Constructs an instance of <code>INDISwitchProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>group</code>, <code>state</code>, <code>permission</code>, <code>timeout</code> and <code>rule</code>.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param group The group of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @param timeout The timeout of the Property 
   * @param rule The rule of the Switch Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDISwitchProperty(INDIDriver driver, String name, String label, String group, PropertyStates state, PropertyPermissions permission, int timeout, SwitchRules rule) throws IllegalArgumentException {
    super(driver, name, label, group, state, permission, timeout);

    if (permission == PropertyPermissions.WO) {
      throw new IllegalArgumentException("Switch Properties cannot be Write Only");
    }

    this.rule = rule;
  }

  /**
   * Constructs an instance of <code>INDISwitchProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>group</code>, <code>state</code>, <code>permission</code> and a 0 timeout.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param group The group of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @param rule The rule of the Switch Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDISwitchProperty(INDIDriver driver, String name, String label, String group, PropertyStates state, PropertyPermissions permission, SwitchRules rule) throws IllegalArgumentException {
    super(driver, name, label, group, state, permission, 0);

    if (permission == PropertyPermissions.WO) {
      throw new IllegalArgumentException("Switch Properties cannot be Write Only");
    }

    this.rule = rule;
  }

  /**
   * Constructs an instance of <code>INDISwitchProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>label</code>, <code>state</code>, <code>permission</code> and a 0 timeout and default group.
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param label The label of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @param rule The rule of the Switch Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDISwitchProperty(INDIDriver driver, String name, String label, PropertyStates state, PropertyPermissions permission, SwitchRules rule) throws IllegalArgumentException {
    super(driver, name, label, null, state, permission, 0);

    if (permission == PropertyPermissions.WO) {
      throw new IllegalArgumentException("Switch Properties cannot be Write Only");
    }

    this.rule = rule;
  }

  /**
   * Constructs an instance of <code>INDISwitchProperty</code> with a particular <code>Driver</code>, <code>name</code>, <code>state</code>, <code>permission</code> and a 0 timeout, a default group and a label equal to its <code>name</code>
   * @param driver The Driver to which this property is associated.
   * @param name The name of the Property
   * @param state The initial state of the Property
   * @param permission The permission of the Property
   * @param rule The rule of the Switch Property
   * @throws IllegalArgumentException 
   * @see INDIProperty
   */
  public INDISwitchProperty(INDIDriver driver, String name, PropertyStates state, PropertyPermissions permission, SwitchRules rule) throws IllegalArgumentException {
    super(driver, name, null, null, state, permission, 0);

    if (permission == PropertyPermissions.WO) {
      throw new IllegalArgumentException("Switch Properties cannot be Write Only");
    }

    this.rule = rule;
  }

  /**
   * Gets the current Rule for this Switch Property
   *
   * @return the current Rule for this Switch Property
   */
  public SwitchRules getRule() {
    return rule;
  }

  /**
   * Checks if the Rule of this Switch property holds.
   *
   * @return
   * <code>true</code> if the values of the Elements of this Property comply
   * with the Rule.
   * <code>false</code> otherwise.
   */
  private boolean checkCorrectValues() {
    if (getState() == PropertyStates.OK) {

      int selectedCount = getSelectedCount();

      if ((rule == SwitchRules.ONE_OF_MANY) && (selectedCount != 1)) {
        return false;
      }

      if ((rule == SwitchRules.AT_MOST_ONE) && (selectedCount > 1)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Gets the number of selected Switch Elements.
   *
   * @return the number of selected Elements.
   */
  private int getSelectedCount() {
    int selectedCount = 0;

    List<INDIElement> list = getElementsAsList();

    for (int i = 0 ; i < list.size() ; i++) {
      INDISwitchElement el = (INDISwitchElement) list.get(i);
      //     System.out.println("-->" + el.getName() + el.getValue());
      if (el.getValue() == SwitchStatus.ON) {
        selectedCount++;
      }
    }

    return selectedCount;
  }

  @Override
  protected String getXMLPropertyDefinitionInit() {
    String xml = "<defSwitchVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" label=\"" + getLabel() + "\" group=\"" + getGroup() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" perm=\"" + Constants.getPropertyPermissionAsString(getPermission()) + "\" rule=\"" + Constants.getSwitchRuleAsString(getRule()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertyDefinitionInit(String message) {
    String xml = "<defSwitchVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" label=\"" + getLabel() + "\" group=\"" + getGroup() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" perm=\"" + Constants.getPropertyPermissionAsString(getPermission()) + "\" rule=\"" + Constants.getSwitchRuleAsString(getRule()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\" message=\"" + message + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertyDefinitionEnd() {
    String xml = "</defSwitchVector>";

    return xml;
  }

  @Override
  protected String getXMLPropertySetInit() {
    String xml = "<setSwitchVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertySetInit(String message) {
    String xml = "<setSwitchVector device=\"" + getDriver().getName() + "\" name=\"" + getName() + "\" state=\"" + Constants.getPropertyStateAsString(getState()) + "\" timeout=\"" + getTimeout() + "\" timestamp=\"" + INDIDateFormat.getCurrentTimestamp() + "\" message=\"" + message + "\">";

    return xml;
  }

  @Override
  protected String getXMLPropertySetEnd() {
    String xml = "</setSwitchVector>";

    return xml;
  }
}
