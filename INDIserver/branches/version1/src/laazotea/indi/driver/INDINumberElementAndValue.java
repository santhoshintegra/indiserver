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

/**
 * A class representing a pair of a <code>INDINumberElement</code> and a <code>Double</code>. 
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDINumberElementAndValue implements INDIElementAndValue {
  private final INDINumberElement element;
  private final Double value;

  /**
   * Constructs an instance of a <code>INDINumberElementAndValue</code>.  This class should not usually be instantiated by specific Drivers.
   * @param element The Number Element
   * @param value The number
   */
  public INDINumberElementAndValue(INDINumberElement element, Double value) {
    this.element = element;
    this.value = value;
  }

  @Override
  public INDINumberElement getElement() {
    return element;
  }

  @Override
  public Double getValue() {
    return value;
  }
}
