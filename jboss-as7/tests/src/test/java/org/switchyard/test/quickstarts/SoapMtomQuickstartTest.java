/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.switchyard.test.quickstarts;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;

import org.custommonkey.xmlunit.XMLAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.switchyard.common.xml.XMLHelper;
import org.switchyard.component.soap.util.SOAPUtil;
import org.switchyard.test.ArquillianUtil;

/*
 *  MTOM/XOP test.
 */
@RunWith(Arquillian.class)
public class SoapMtomQuickstartTest {

    private static final String SWITCHYARD_WEB_SERVICE = "http://localhost:8080/soap-mtom/ImageServiceService";

    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        return ArquillianUtil.createJarQSDeployment("switchyard-quickstart-soap-mtom");
    }

    @Test
    public void imageService() throws Exception {
        SOAPMessage response = sendMessage();
        XMLAssert.assertXMLEqual(RESPONSE, XMLHelper.toString(response.getSOAPPart().getDocumentElement()));
        Assert.assertTrue(response.getAttachments().hasNext());
        AttachmentPart attachment = (AttachmentPart)response.getAttachments().next();
        Assert.assertNotNull(attachment);
        Assert.assertEquals("image/jpeg", attachment.getContentType());
        Assert.assertEquals(16384, attachment.getSize());
    }

    public SOAPMessage sendMessage() throws Exception {
        SOAPConnectionFactory conFactory = SOAPConnectionFactory.newInstance();

        SOAPConnection connection = conFactory.createConnection();
        MessageFactory msgFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage msg = msgFactory.createMessage();
        SOAPBodyElement bodyElement = msg.getSOAPBody().addBodyElement(new QName("urn:switchyard-quickstart:soap-mtom:1.0", "image"));
        bodyElement.setTextContent(imageData);

        return connection.call(msg, new URL(SWITCHYARD_WEB_SERVICE));
    }

    private static String imageData = "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwg"
                            + "JC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyM"
                            + "jIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAA"
                            + "AAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRV"
                            + "S0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJi"
                            + "pKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8Q"
                            + "AHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhc"
                            + "RMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN"
                            + "0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6"
                            + "Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDFs0162zJdeLvEepNFcxW0ywahLFDDKTyrsWLlSeAwC5wcHPTRbxbrd3r"
                            + "994UfVbiJA8trDdwXEwkRo8hX3b9zcrzk89z1qjDoXi6W5i1nStG1KC8uI83dnc28iLIQRvB3/LsLYwpbdgAjkcJP4"
                            + "I8S2fjE6yul3osBdi4JaFpZQrNkrtjBLMMkZAxxngVtotjHV77n/9k=";

    private static String RESPONSE = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                            + "<SOAP-ENV:Header/>"
                            + "<SOAP-ENV:Body>"
                            + "<ns2:imageResponse xmlns:ns2=\"urn:switchyard-quickstart:soap-mtom:1.0\">"
                            + "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:internal-resized-switchyard.jpeg\"/>"
                            + "</ns2:imageResponse>"
                            + "</SOAP-ENV:Body>"
                            + "</SOAP-ENV:Envelope>";
}
