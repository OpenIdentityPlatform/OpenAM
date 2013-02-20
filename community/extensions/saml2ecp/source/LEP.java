/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LEP.java,v 1.2 2008/03/17 03:11:05 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.soap.*;

import org.w3c.dom.*;

import com.sun.identity.federation.message.*;
import com.sun.identity.federation.services.*;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.saml2.ecp.ECPRequest;
import com.sun.identity.saml2.ecp.ECPResponse;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.xml.XMLUtils;

public class LEP extends Thread {
    private static Hashtable cookieCache = null;
    private static Hashtable ecpCache = null;
    private static String IDPS_FILE = "config/idps.properties";
    private static String HTTP_HEADERS_FILE = "config/httpHeaders.properties";
    private static Properties idpsConfig = new Properties();
    private static Properties httpHeadersConfig = new Properties();
    private static String PROVIDER_URL = "/amserver/ssosoap";
    private static String VND_PAOS = "application/vnd.paos+xml";

    static {
        try {
            idpsConfig.load(new FileInputStream(IDPS_FILE));
            httpHeadersConfig.load(new FileInputStream(HTTP_HEADERS_FILE));
        } catch(IOException ie) {
            ie.printStackTrace();
            System.out.println("Cannot read configuration file:" +
                ie.getMessage());        
        }
    }

    public static void processURL(String url) {
        HttpURLConnection conn = null;
        try {
            System.out.println("URL: " + url);

            conn = openConnection(new URL(url));
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);
            conn.addRequestProperty("Accept", "test/html; " +
                ECPUtils.PAOS_MIME_TYPE_VAL);
            conn.addRequestProperty(ECPUtils.PAOS_HEADER_TYPE,
                ECPUtils.PAOS_HEADER_VAL);

            conn.setDoOutput(false);
            conn.connect();

            sendRequestToSP(conn);
        } catch (Exception e) {
            e.printStackTrace ();
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private static String findIDPEndpoint(String spEntityID, IDPList idpList)
        throws Exception {

        String idpEndpoint = null;
        idpEndpoint = (String)idpsConfig.get(spEntityID);
        if (idpEndpoint != null) {
            System.out.println("Got idp endpoint from idps.properties = " +
                idpEndpoint);
            return idpEndpoint;
        }

        if (idpList != null) {
            List idps = idpList.getIDPEntries();
            if ((idps != null) && (!idps.isEmpty())) {
                for(Iterator iter = idps.iterator(); iter.hasNext();) {
                    IDPEntry idpEntry = (IDPEntry)iter.next();
                    idpEndpoint = idpEntry.getLoc();
                    if (idpEndpoint != null) {
                        System.out.print("Got idp endpoint from ecp Request = "
                            + idpEndpoint);
                        return idpEndpoint;
                    }
                }
            }
        }

        throw new Exception("Unable to determine idp endpoint.");
    }

    /**
     * Forward the <AuthnRequest> SOAP message received from the SP to the
     * IDP.
     */
    private static SOAPMessage sendAuthnRequestToIDP (String idpEndpoint,
        SOAPMessage msg) throws Exception {

        SOAPMessage response = null;
        // remove SOAP headers received from SP before forwarding.
        SOAPHeader header = msg.getSOAPHeader();
        header.detachNode();

        System.out.println("Sending the following MSG to provider:" + idpEndpoint);
        msg.writeTo(System.out);
        System.out.println("");

        response = postSOAPMsgToProvider(idpEndpoint, msg);
        if(response != null) {
            System.out.println("Got Response from IDP: " );
            response.writeTo(System.out);
        } else {
            throw new Exception("Response from IDP is not a SOAP message.");
        }
            
        return response;
    }

    /**
     * This method posts a SOAP Authn message to the IDP using HttpURLConnection
     * and processes either a SOAP response or a non-SOAP response in case
     * the IDP requires a login.
     */
    public static SOAPMessage postSOAPMsgToProvider(String providerEndpoint,
        SOAPMessage msg)
        throws IOException, SOAPException {

        SOAPMessage response = null;

        URL url = null;
        try {
            url = new URL(providerEndpoint);
        } catch (MalformedURLException me) {
            throw new IOException(me.getMessage());
        }

        HttpURLConnection conn = openConnection(url);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Content-type", "text/xml");
        conn.setRequestProperty("SOAPAction", "\"\"");


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.writeTo(baos);
        StringBuffer postBody  = new StringBuffer(baos.toString());

        System.out.println("\n\nSending SOAPMessage to " + providerEndpoint);
        System.out.println(postBody.toString());
        System.out.println("\n\n");

        conn.setDoOutput(true);
        conn.connect();
        PrintWriter writer = new PrintWriter(conn.getOutputStream());
        writer.print(postBody);
        writer.close();

        BufferedInputStream bin =
            new BufferedInputStream(conn.getInputStream());

        String contentType = conn.getContentType();
        if(contentType.startsWith("text/xml"))  {
            // SOAP message response
            MessageFactory mf = MessageFactory.newInstance();
            MimeHeaders mimeHdrs = new MimeHeaders();
            mimeHdrs.addHeader("Content-type", "text/xml");
            response = mf.createMessage(mimeHdrs, bin);
        } else {
            // Non-SOAP message response
            printContent(conn, bin);
        }
        return response;
    }

    
    private static void sendRequestToSP(HttpURLConnection conn)
        throws Exception {

        BufferedInputStream bin =
            new BufferedInputStream(conn.getInputStream());

        String contentType = conn.getContentType();
        System.out.println("Response content type = " + contentType);
        if (contentType == null) {
            printContent(conn, bin);
        } else if(contentType.startsWith(VND_PAOS)) {
            String authnMsgId = null;
            String msgId = null;
            String acURL = null;
            AuthnRequest ar = null;
            PAOSRequest pr = null;
            ECPRelayState ers = null;

            SOAPMessage m = readSOAPMessageFromStream(bin);
            SOAPHeader hdrs = m.getSOAPHeader();

            SOAPBody body = m.getSOAPBody();
            Iterator it = body.getChildElements();
            while(it.hasNext()) {
                SOAPElement se = (SOAPElement) it.next();
                Name n = se.getElementName();
                String localName = n.getLocalName();
                System.out.println("LOCAL NAME:" + localName);
                if(localName.equals("AuthnRequest")) {
                    ar = ProtocolFactory.getInstance()
                        .createAuthnRequest(se);
                    authnMsgId = ar.getID();
                    System.out.println("AUTHN MESSAGE ID:" + authnMsgId);
                }
            }

            if (ar == null) {
                throw new Exception("AuthnRequest not found.");
            }

            Iterator li = hdrs.examineAllHeaderElements();

            IDPList idpList = null;
            String spEntityID = null;
            while(li.hasNext()) {
                SOAPHeaderElement e = (SOAPHeaderElement) li.next();
                String tagName = e.getLocalName();
                String namespace = e.getNamespaceURI();
                if(namespace.equals(ECPUtils.PAOSNAMESPACE) && 
                        tagName.equals("Request")) {
                    pr = new PAOSRequest(e);
                    msgId = pr.getMessageID();
                    System.out.println("PAOSRequest:" +
                        pr.toXMLString(true, true));

                } else if(namespace.equals(ECPUtils.ECPNAMESPACE) && 
                            tagName.equals("Request")) {
                    ECPRequest ecpReq =
                        ECPFactory.getInstance().createECPRequest(e);
                    idpList = ecpReq.getIDPList();
                    spEntityID = ecpReq.getIssuer().getValue();
                    System.out.println("ECPRequest:" +
                        ecpReq.toXMLString(true, true));
                } else if(namespace.equals(ECPUtils.ECPNAMESPACE) && 
                        tagName.equals("RelayState")) {
                    ers = ECPFactory.getInstance().createECPRelayState(e);
                    System.out.println("ECPRelayState:" +
                        ers.toXMLString(true, true));
                }
            }

            System.out.println("SP Provider:" + spEntityID);
            System.out.println("IDP Providers from IDPList:" + idpList);
            String idpEndpoint = findIDPEndpoint(spEntityID, idpList);
            SOAPMessage response = sendAuthnRequestToIDP(idpEndpoint, m);

            SOAPHeader soapHeaders = response.getSOAPHeader();
            if (soapHeaders != null) {
                for(Iterator iter= soapHeaders.examineAllHeaderElements();
                    iter.hasNext();) {
                    Element e = (Element)iter.next();
                    String tagName = e.getLocalName();
                    String namespace = e.getNamespaceURI();
                    if (namespace.equals(ECPUtils.ECPNAMESPACE) && 
                        tagName.equals("Response")) {

                        ECPResponse ecpResp =
                            ECPFactory.getInstance().createECPResponse(e);
                        acURL = ecpResp.getAssertionConsumerServiceURL();
                    }
                }
            }

            body = response.getSOAPBody();
            List soapBodies = new ArrayList();
            for(Iterator iter = body.getChildElements();iter.hasNext();) {
                soapBodies.add(iter.next());
            }

            postSAMLResponseToSP(acURL, soapBodies, pr, ers);
        } else {
            // Plain HTML response. Send back to browser.
            printContent(conn, bin);
        }
    }

    /**
     * Post a SOAP message to the service provider.
     */
    private static void postSAMLResponseToSP(String acURL, List soapBodies,
        PAOSRequest pr, ECPRelayState ers) throws Exception {
        // Send PAOS response header in message to SP.
        SOAPMessage respMsg = null;
        if (acURL != null) {
            System.out.println ("AssertionConsumerURL: " + acURL);
            PAOSResponse paosResp = new PAOSResponse(pr.getMessageID(),
                Boolean.TRUE, ECPUtils.ACTOR);

            String header = paosResp.toXMLString(true, true) + 
                ((ers == null) ? "" : ers.toXMLString(true, true));
            
            StringBuffer bodySB = new StringBuffer();
            for(Iterator iter = soapBodies.iterator(); iter.hasNext();) {
                Element childElem = (Element)iter.next();
                bodySB.append(XMLUtils.print(childElem));
            }

            System.out.println("Sending following msg to SP");
            SOAPMessage msg = createSOAPMessage(header, bodySB.toString());
            msg.writeTo(System.out);
            respMsg = postSOAPMsgToProvider(acURL, msg);
        } else {
            throw new Exception("Unable to post SAML response. " +
                "No SP info found");
        }
    }

    /**
     * Read the contents of the SOAP message from the specified input stream.
     */
    private static SOAPMessage readSOAPMessageFromStream(InputStream is) 
        throws SOAPException, IOException {
        MessageFactory mf = MessageFactory.newInstance();
        MimeHeaders mimeHdrs = new MimeHeaders();
        mimeHdrs.addHeader("Content-type", "text/xml");
        SOAPMessage msg = mf.createMessage(mimeHdrs, is);
        return msg;
    }

    /**
     * Read the contents of the response from the URL connection
     * and return to browser.
     */
    private static void printContent(HttpURLConnection conn,
        BufferedInputStream bin) throws IOException {

        System.out.println("URL = " + conn.getURL());
        System.out.println("Response code = " + conn.getResponseCode());
        System.out.println("Response message = " + conn.getResponseMessage());
        int contentLength = conn.getContentLength();
        System.out.println("Content length = " + contentLength);
        String contentType = conn.getContentType();
        System.out.println("Content type = " + contentType);

        if (!contentType.startsWith("text/html")) {
            return;
        }

        StringBuffer contentSB = new StringBuffer();
        byte content[] = new byte[2048];

        if (contentLength != -1) {
            int read = 0, totalRead = 0;
            int left;
            while (totalRead < contentLength) {
                left = contentLength - totalRead;
                read = bin.read(content, 0,
                    left < content.length ? left : content.length);
                if (read == -1) {
                    // We need to close connection !!
                    break;
                } else {
                    if (read > 0) {
                        totalRead += read;
                        contentSB.append(new String(content, 0, read));
                    }
                }
            }
        } else {
            int numbytes;
            int totalRead = 0;

            while (true) {
                numbytes = bin.read(content);
                if (numbytes == -1) {
                    break;
                }

                totalRead += numbytes;

                contentSB.append(new String(content, 0, numbytes));
            }
        }

        System.out.println("Content = \n" + contentSB.toString());
    }

    /**
     * Add the cookie to the cache to used later by the ECP.
     */
    private void addCookieToCache(String cookieHeader) {
        if(cookieHeader != null) {
            int nameIdx = cookieHeader.indexOf("=");
            if(nameIdx != -1){
                String cookieKey = cookieHeader.substring(0, nameIdx);
                String cookieVal = cookieHeader.substring(nameIdx+1, 
                                    cookieHeader.length());
                int valIdx = cookieVal.indexOf(";");
                if(valIdx != -1) {
                    cookieVal = cookieVal.substring(0, valIdx);
                }
                System.out.println("COOKIE:" +cookieKey + " VAL=" +cookieVal);
                cookieCache.put(cookieKey, cookieVal);
            }
        } 
    }

    private static HttpURLConnection openConnection(URL url)
        throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        // Add addtional HTTP headers, for example, X-MSISDN header
        for(Enumeration e = httpHeadersConfig.keys(); e.hasMoreElements() ;) {
            String key = (String) e.nextElement();
            String val = (String) httpHeadersConfig.get(key);
            if (key.equals("Cookie")) {
                int index = val.indexOf("=");
                if (index != -1) {
                    val = val.substring(0, index + 1) +
                    val.substring(index + 1).replaceAll("\\+", "%2B");
                    // URLEncoder.encode(val.substring(index + 1), "UTF-8");
                }
            }
            System.out.println("Setting HTTP request header:" + key + ":" +
                val);
            conn.setRequestProperty(key, val);
        }
        return conn;
    }

    /**
     * Creates <code>SOAPMessage</code> with the input XML String
     * as message header and body.
     * @param header XML string to be put into <code>SOAPMessage</code> header.
     * @param body XML string to be put into <code>SOAPMessage</code> body.
     * @return newly created <code>SOAPMessage</code>.
     * @exception SOAPException if it cannot create the
     *     <code>SOAPMessage</code>.
     */
    private static SOAPMessage createSOAPMessage(String header, String body)
        throws Exception {

        SOAPMessage msg = null;

        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addHeader("Content-Type", "text/xml");
            
        StringBuffer sb = new StringBuffer(500);
        sb.append("<").append(SAMLConstants.SOAP_ENV_PREFIX)
          .append(":Envelope").append(SAMLConstants.SPACE)
          .append("xmlns:").append(SAMLConstants.SOAP_ENV_PREFIX)
          .append("=\"").append(SAMLConstants.SOAP_URI).append("\">");
        if (header != null) {
            sb.append("<")
              .append(SAMLConstants.SOAP_ENV_PREFIX).append(":Header>")
              .append(header)
              .append(SAMLConstants.START_END_ELEMENT)
              .append(SAMLConstants.SOAP_ENV_PREFIX)
              .append(":Header>");
        }
        if (body != null) {
            sb.append("<")
              .append(SAMLConstants.SOAP_ENV_PREFIX).append(":Body>")
              .append(body)
              .append(SAMLConstants.START_END_ELEMENT)
              .append(SAMLConstants.SOAP_ENV_PREFIX)
              .append(":Body>");
        }
        sb.append(SAMLConstants.START_END_ELEMENT)
          .append(SAMLConstants.SOAP_ENV_PREFIX)
          .append(":Envelope>").append(SAMLConstants.NL);
            
        return MessageFactory.newInstance().createMessage(mimeHeaders,
            new ByteArrayInputStream(sb.toString().getBytes(
            SAML2Constants.DEFAULT_ENCODING)));
    }
}

