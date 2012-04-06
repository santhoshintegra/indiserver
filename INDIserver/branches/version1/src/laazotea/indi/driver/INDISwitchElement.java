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
import laazotea.indi.Constants.SwitchStatus;
import org.w3c.dom.Element;

/**
 * A class representing a INDI Switch Element.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDISwitchElement extends INDIElement {

  /**
   * Current Status value for this Switch Element.
   */
  private SwitchStatus status;

  /**
   * Constructs an instance of a <code>INDISwitchElement</code> with a <code>name</code>, a <code>label</code> and its initial <code>status</code>.
   * @param name The name of the Element.
   * @param label The label of the Element.
   * @param status The initial status of the Element
   */
  public INDISwitchElement(String name, String label, SwitchStatus status) throws IllegalArgumentException {
    super(name, label);

    this.status = status;
  }

  /**
   * Constructs an instance of a <code>INDISwitchElement</code> with a <code>name</code>, a <code>label</code> and its initial <code>status</code>. The label of the Element will be a copy of the <code>name</code>.
   * @param name The name of the Element.
   * @param status The initial state of the Element.
   * @throws IllegalArgumentException
   */
  public INDISwitchElement(String name, SwitchStatus status) throws IllegalArgumentException {
    super(name);

    this.status = status;
  }  

  @Override
  public SwitchStatus getValue() {
    return status;
  }


  @Override
  public void setValue(Object newValue) throws IllegalArgumentException {
    SwitchStatus ss = null;
    try {
      ss = (SwitchStatus)newValue;
    } catch(ClassCastException e) {
      throw new IllegalArgumentException("Value for a Switch Element must be a SwitchStatus");
    }

    this.status = ss;
  }



  @Override
  public String getXMLOneElement() {
    String stat = Constants.getSwitchStatusAsString(status);

    String xml = "<oneSwitch name=\"" + this.getName() + "\">" + stat + "</oneSwitch>";

    return xml;
  }
  
    @Override
  public String getNameAndValueAsString() {
    return getName() + " - " + getValue();
  }

  @Override
  protected String getXMLDefElement() {
    String stat = Constants.getSwitchStatusAsString(status);

    String xml = "<defSwitch name=\"" + this.getName() + "\" label=\"" + getLabel() + "\">" + stat + "</defSwitch>";

    return xml;
  }

  @Override
  public Object parseOneValue(Element xml) {
    return Constants.parseSwitchStatus(xml.getTextContent().trim());
  }
}
