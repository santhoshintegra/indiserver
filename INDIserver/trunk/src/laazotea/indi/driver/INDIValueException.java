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
 * A class representing an exception on the value of a
 * <code>INDIElement</code>.
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
@SuppressWarnings("serial")
public class INDIValueException extends Exception {

  /**
   * The element that produced the exception.
   */
  private INDIElement element;

  /**
   * Constructs an instance of
   * <code>INDIValueException</code> with the specified detail message.
   *
   * @param element The element that produced the error.
   * @param msg the detail message.
   */
  public INDIValueException(INDIElement element, String msg) {
    super(msg);
    this.element = element;
  }

  /**
   * Gets the
   * <code>INDIElement</code> that produced the exception.
   *
   * @return the
   * <code>INDIElement</code> that produced the exception
   */
  public INDIElement getINDIElement() {
    return element;
  }
}