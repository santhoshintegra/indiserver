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
package laazotea.indi.driver.examples;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.Constants.SwitchStatus;
import laazotea.indi.INDIBLOBValue;
import laazotea.indi.driver.*;

/**
 * A small example Driver that uses the INDI Driver for Java library. It defines
 * two BLOB Properties, two Text Properties and a Switch One. The BLOB
 * Properties will have two images about the weather in Spain and Europe
 * (dinamically downloaded from http://eltiempo.es), and the Text ones will
 * contain the names of them. It will check for updated images every 15 minutes.
 * The Switch Property can be used to ask for the current images (for example
 * once the client connects to the driver).
 *
 * @author S. Alonso (Zerjillo) [zerjio at zerjio.com]
 * @version 1.00, March 19, 2012
 */
public class INDIElTiempoDriver extends INDIDriver implements Runnable {

  /*
   * The properties
   */
  private INDISwitchElement sendImage;
  private INDISwitchProperty send;
  private INDIBLOBProperty spainImageProp;
  private INDIBLOBElement spainImageElem;
  private INDITextProperty spainImageNameProp;
  private INDITextElement spainImageNameElem;
  private INDIBLOBProperty europeImageProp;
  private INDIBLOBElement europeImageElem;
  private INDITextProperty europeImageNameProp;
  private INDITextElement europeImageNameElem;

  /**
   * Initializes the driver. It creates the Proerties and its Elements.
   *
   * @param inputStream The input stream from which the Driver will read.
   * @param outputStream The output stream to which the Driver will write.
   */
  public INDIElTiempoDriver(InputStream inputStream, OutputStream outputStream) {
    super(inputStream, outputStream);

    // We add the default CONNECTION Property
    addConnectionProperty();

    // We create the Switch Property with only one Switch Element
    sendImage = new INDISwitchElement("SEND", "Send Image", SwitchStatus.OFF);
    send = new INDISwitchProperty(this, "SEND", "Send Image", "Main Control", PropertyStates.IDLE, PropertyPermissions.RW, 3, SwitchRules.AT_MOST_ONE);
    send.addElement(sendImage);

    addProperty(send);

    // We create the BLOB Property for the Spain satellite image
    spainImageElem = new INDIBLOBElement("SPAIN_SATELLITE_IMAGE", "Spain Image");
    spainImageProp = new INDIBLOBProperty(this, "SPAIN_SATELLITE_IMAGE", "Spain Image", "Main Control", PropertyStates.IDLE, PropertyPermissions.RO, 0);
    spainImageProp.addElement(spainImageElem);

    addProperty(spainImageProp);

    // We create the Text Property for the Spain image name
    spainImageNameElem = new INDITextElement("SPAIN_IMAGE_NAME", "Spain Image Name", "");
    spainImageNameProp = new INDITextProperty(this, "SPAIN_IMAGE_NAME", "Spain Image Name", "Main Control", PropertyStates.IDLE, PropertyPermissions.RO, 3);
    spainImageNameProp.addElement(spainImageNameElem);

    addProperty(spainImageNameProp);

    // We create the BLOB Property for the Europe satellite image
    europeImageElem = new INDIBLOBElement("EUROPE_SATELLITE_IMAGE", "Europe Image");
    europeImageProp = new INDIBLOBProperty(this, "EUROPE_SATELLITE_IMAGE", "Europe Image", "Main Control", PropertyStates.IDLE, PropertyPermissions.RO, 0);
    europeImageProp.addElement(europeImageElem);

    addProperty(europeImageProp);

    // We create the Text Property for the Europe image name
    europeImageNameElem = new INDITextElement("EUROPE_IMAGE_NAME", "Europe Image Name", "");
    europeImageNameProp = new INDITextProperty(this, "EUROPE_IMAGE_NAME", "Europe Image Name", "Main Control", PropertyStates.IDLE, PropertyPermissions.RO, 3);
    europeImageNameProp.addElement(europeImageNameElem);

    addProperty(europeImageNameProp);
    
    Thread t = new Thread(this);
    t.start();
  }

  /**
   * Gets the name of the Driver
   */
  @Override
  public String getName() {
    return "El Tiempo INDI Driver";
  }

  /**
   * Nothing happens as there are no writable Text Properties
   *
   * @param property
   * @param timestamp
   * @param elementsAndValues
   */
  @Override
  public void processNewTextValue(INDITextProperty property, Date timestamp, INDITextElementAndValue[] elementsAndValues) {
  }

  /**
   * If we receive the Switch Value ON of the property "SEND" we check for new
   * images in the web, download them and send them to the client.
   *
   * @param property
   * @param timestamp
   * @param elementsAndValues
   */
  @Override
  public void processNewSwitchValue(INDISwitchProperty property, Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
    if (property == send) {
      if (elementsAndValues.length > 0) {
        SwitchStatus stat = elementsAndValues[0].getValue();

        if (stat == SwitchStatus.ON) {
          property.setState(PropertyStates.OK);
          updateProperty(property, "Checking images");

          checksForSpainImage(true);

          checksForEuropeImage(true);
        }
      }
    }
  }

  /**
   * Checks for the Spain Image and, if new, sends it to the clients.
   *
   * @param alwaysSend if
   * <code>true</code> the image is sended to the client. If not, it is only
   * sended if it is new.
   */
  private void checksForSpainImage(boolean alwaysSend) {
    boolean newImage = checkForImage("http://www.eltiempo.es/satelite", "SPAIN");
    INDIBLOBValue v = spainImageElem.getValue();

    if (v.getSize() > 0) {
      if (newImage || alwaysSend) {
        spainImageProp.setState(PropertyStates.OK);

        updateProperty(spainImageProp);

        spainImageNameProp.setState(PropertyStates.OK);

        updateProperty(spainImageNameProp);
      }
    }
  }

  private void checksForEuropeImage(boolean alwaysSend) {
    boolean newImage = checkForImage("http://www.eltiempo.es/europa/satelite/", "EUROPE");
    INDIBLOBValue v = europeImageElem.getValue();

    if (v.getSize() > 0) {
      if (newImage || alwaysSend) {
        europeImageProp.setState(PropertyStates.OK);

        updateProperty(europeImageProp);

        europeImageNameProp.setState(PropertyStates.OK);
        
        updateProperty(europeImageNameProp);
      }
    }
  }

  /**
   * Nothing happens as there are no writable Number Properties
   *
   * @param property
   * @param timestamp
   * @param elementsAndValues
   */
  @Override
  public void processNewNumberValue(INDINumberProperty property, Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
  }

  /**
   * Nothing happens as there are no writable BLOB Properties
   *
   * @param property
   * @param timestamp
   * @param elementsAndValues
   */
  @Override
  public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp, INDIBLOBElementAndValue[] elementsAndValues) {
  }

  /**
   * Checks for a new image in the
   * <code>url</code> and if it has changed, saves it to the appropriate BLOB
   * Property (data) and Text Property (name) - according to the
   * <code>imagePrefix</code>.
   */
  private boolean checkForImage(String url, String imagePrefix) {
    File webpage = new File("web.html");
    String text;

    try {
      downloadAndSave(url, webpage);

      text = readFile(webpage);
    } catch (IOException e) {
      e.printStackTrace();

      return false;
    }

    // We look for the URL of the image to be downloaded
    String searchString = "<img id=\"imgmap\" src=\"";
    int start = text.indexOf(searchString);

    if (start == -1) {  // Not found
      return false;
    }

    start += searchString.length();

    int stop = text.indexOf("\"", start + 1);

    if (stop == -1) {
      return false;
    }

    String imgURL = text.substring(start, stop);

    int lastBar = imgURL.lastIndexOf("/");

    String fileName = imgURL.substring(lastBar + 1);

    File image = new File(fileName);

    if (!image.exists()) {  // Download the image
      try {
        downloadAndSave(imgURL, image);
      } catch (IOException e) {
        e.printStackTrace();

        return false;
      }
    }

    byte[] imageBytes;

    try {
      imageBytes = readBinaryFile(image);
    } catch (IOException e) {
      e.printStackTrace();

      return false;
    }

    // Define
    INDIBLOBProperty pim = (INDIBLOBProperty) getProperty(imagePrefix + "_SATELLITE_IMAGE");
    INDIBLOBElement eim = (INDIBLOBElement) pim.getElement(imagePrefix + "_SATELLITE_IMAGE");

    if (Arrays.equals(imageBytes, eim.getValue().getBLOBData())) {
      return false;  // The same image as the one in the property
    }

    eim.setValue(new INDIBLOBValue(imageBytes, "jpg"));

    int pos1 = fileName.lastIndexOf("-");

    String name = fileName.substring(pos1, pos1 + 5) + "/" + fileName.substring(pos1 + 5, pos1 + 7) + "/" + fileName.substring(pos1 + 7, pos1 + 9) + " " + fileName.substring(pos1 + 9, pos1 + 11) + ":" + fileName.substring(pos1 + 11, pos1 + 13);

    INDITextProperty pn = (INDITextProperty) getProperty(imagePrefix + "_IMAGE_NAME");
    INDITextElement en = (INDITextElement) pn.getElement(imagePrefix + "_IMAGE_NAME");

    en.setValue(imagePrefix + " Satellite " + name + " UTC");

    return true;
  }

  /**
   * Reads a text file and returns its contents as a String.
   *
   * @param file
   * @return
   * @throws IOException
   */
  private String readFile(File file) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    StringBuilder stringBuilder = new StringBuilder();
    String ls = System.getProperty("line.separator");

    while ((line = reader.readLine()) != null) {
      stringBuilder.append(line);
      stringBuilder.append(ls);
    }

    return stringBuilder.toString();
  }

  /**
   * Reads a binary file and returns its contents as a byte[]
   *
   * @param file
   * @return
   * @throws IOException
   */
  private byte[] readBinaryFile(File file) throws IOException {
    int fileSize = (int) file.length();
    FileInputStream reader = new FileInputStream(file);

    byte[] buffer = new byte[fileSize];

    int totalRead = 0;

    while (totalRead < fileSize) {
      int readed = reader.read(buffer, totalRead, fileSize - totalRead);

      if (readed == -1) {
        return null; // Unexpected end of file 
      }

      totalRead += readed;
    }

    return buffer;
  }

  /**
   * Downloads a
   * <code>url</code> and saves its contents to
   * <code>file</code>.
   *
   * @param url
   * @param file
   * @throws IOException
   * @throws MalformedURLException
   */
  private void downloadAndSave(String url, File file) throws IOException, MalformedURLException {
    int bufsize = 65536;
    byte[] buffer = new byte[bufsize];

    URL u = new URL(url);
    InputStream is = u.openStream();  // throws an IOException
    BufferedInputStream bis = new BufferedInputStream(is);
    FileOutputStream fos = new FileOutputStream(file);

    int readed = 0;

    while (readed != -1) {
      readed = bis.read(buffer);

      if (readed > 0) {
        fos.write(buffer, 0, readed);
      }
    }

    bis.close();
    fos.close();
  }

  /**
   * The thread that every 15 minutes checks for new images and, if they have
   * changed, downloads them and sends them back to the clients.
   */
  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(15 * 60 * 1000);
      } catch (InterruptedException e) {
      }
      
      checksForSpainImage(false);

      checksForEuropeImage(false);
    }
  }
}
