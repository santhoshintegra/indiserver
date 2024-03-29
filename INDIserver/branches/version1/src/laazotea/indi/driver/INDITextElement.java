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

import org.w3c.dom.Element;

/**
 * A class representing a INDI Text Element.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDITextElement extends INDIElement {

  /**
   * The current value of the Text Element
   */
  private String value;

  /**
   * Constructs an instance of a <code>INDITextElement</code> with a <code>name</code>, a <code>label</code> and its initial <code>value</code>.
   * @param name The name of the Element.
   * @param label The label of the Element.
   * @param value The initial value of the Element
   */
  public INDITextElement(String name, String label, String value) {
    super(name, label);

    this.value = value.trim();
  }
  
  /**
   * Constructs an instance of a <code>INDITextElement</code> with a <code>name</code>, a <code>label</code> and its initial <code>value</code>. The label of the Element will be a copy of the <code>name</code>.
   * @param name The name of the Element.
   * @param value The initial value of the Element
   */
  public INDITextElement(String name, String value) throws IllegalArgumentException {
    super(name);

    this.value = value.trim();
  }  

  @Override
  public String getValue() {
    return value;
  }


  @Override
  public void setValue(Object newValue) throws IllegalArgumentException {
    String v = null;

    try {
      v = (String)newValue;
    } catch(ClassCastException e) {
      throw new IllegalArgumentException("Value for a Text Element must be a String");
    }
    
    this.value = v;
  }




  @Override
  public String getXMLOneElement() {
    String xml = "<oneText name=\"" + this.getName() + "\">" + value + "</oneText>";

    return xml;
  }

  @Override
  public String getNameAndValueAsString() {
    return getName() + " - " + getValue();
  }

  @Override
  protected String getXMLDefElement() {
    String xml = "<defText name=\"" + this.getName() + "\" label=\"" + getLabel() + "\">" + value + "</defText>";

    return xml;
  }

  @Override
  public String parseOneValue(Element xml) {
    return xml.getTextContent().trim();
  }
}
