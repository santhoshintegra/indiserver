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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A class representing the main Thread of the Driver. It reads from the Drivers
 * input Stream and does the initial parsing of the received information.
 * Drivers usually should not directly instantiate this class nor directly start
 * this Thread (it is done via the
 * <code>startListening</code> method of the
 * <code>INDIDriver</code>).
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.10, March 19, 2012
 */
public class INDIDriverThread extends Thread {

  INDIDriver driver;

  protected INDIDriverThread(INDIDriver driver) {
    this.driver = driver;
  }

  /**
   * Implements the function to read the Driver input stream and parsing of the
   * received messages. It is called by
   * <code>startListening</code> and should not be directly called by the
   * Drivers.
   *
   * @see INDIDriver#startListening
   */
  @Override
  public void run() {
    DocumentBuilderFactory docBuilderFactory;

    DocumentBuilder docBuilder;
    try {
      docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilder = docBuilderFactory.newDocumentBuilder();
      docBuilder.setErrorHandler(new ErrorHandler() {

        @Override
        public void warning(SAXParseException e) throws SAXException {
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    int BUFFER_SIZE = 1000000;

    StringBuffer bufferedInput = new StringBuffer();

    char[] buffer = new char[BUFFER_SIZE];

    boolean connected = true;
    BufferedReader in = driver.getIn();

    try {
      while (connected) {
        int nReaded = in.read(buffer, 0, BUFFER_SIZE);

        if (nReaded != -1) {
          bufferedInput.append(buffer, 0, nReaded);  // Appending to the buffer

          boolean errorParsing = false;

          try {
            Document doc = docBuilder.parse(new InputSource(new StringReader("<INDI>" + bufferedInput + "</INDI>")));

            driver.parseXML(doc);
          } catch (SAXException e) {
            errorParsing = true;
          }

          if (!errorParsing) {
            bufferedInput.setLength(0);  // Empty the buffer because it has been already parsed
          }
        } else {  // If -1 readed, end
          connected = false;
        }

      }
    } catch (IOException e) {
      //   e.printStackTrace();
    }

    driver.disconnect();
  }
}
