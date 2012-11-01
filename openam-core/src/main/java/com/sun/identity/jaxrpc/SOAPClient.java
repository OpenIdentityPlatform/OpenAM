/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: SOAPClient.java,v 1.9 2008/09/03 07:06:30 lakshman_abburi Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.jaxrpc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.iplanet.am.sdk.remote.AMRemoteException;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.entity.EntityException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import javax.xml.parsers.SAXParser;

/**
 * The class <code>SOAPClient</code> provides methods for SOAP and JAXRPC
 * client to send and receive messages. The method <code>call(..)</code> will
 * be used by SOAP client to send SOAP messages, and JAXRPC clients will use
 * <code>encodeMessage</code> and <code>send</code> to send JAXRPC requests.
 * The method <code>encodeMessage(String functionName,
 * Object[] args)</code>,
 * encodes the JAXRPC data in SOAP, which can then be sent using the <code>send(
 * String message, String cookies)</code>.
 * <p>
 * The <code>SOAPClient</code> can be initialized either with known SOAP
 * endpoint URLs or it will find an active server using Naming service. In the
 * case of JAXRPC, the SOAP response is decoded and returns a java
 * <code>Object</code>; else an exception is thrown.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.jaxrpc.SOAPClient}
 */
public class SOAPClient {
    
    // Debug file
    static final Debug debug = Debug.getInstance("amJAXRPC");
    
    // Instance variables
    String serviceName;
    
    String url;
    
    // Variables for direct URLs
    String urls[];
    
    // Variables for managing exceptions
    String exceptionClassName, exceptionMessage, exceptionCode,
        smsExceptionCode;
    
    int ldapErrorCode;
    
    String resourceBundleName, errorString;
    
    Set messageArgs;
    
    Exception exception;
    
    boolean isException;
    
    private static boolean useCache = Boolean.getBoolean(
        SystemProperties.get(Constants.URL_CONNECTION_USE_CACHE, "false"));
    
    /**
     * Constructor for applications that would like to dynamically set the SOAP
     * endponts using <code>
     * <code>setUrls(String[] urls)</code> before
     * invoking either <code>send()</code> or <code>
     * call()</code>.
     */
    public SOAPClient() throws IOException {
        // do nothing
    }
    
    /**
     * Constructor for services that use JAXRPC as their communication protocol.
     * The URL end points for these services will be obtained from Naming
     * service for jaxrpc service, and the service name will appended to it as
     * the JAXRPC interface name.
     */
    public SOAPClient(String serviceName) {
        this.serviceName = serviceName;
    }
    
    /**
     * Constructor for applications that have the list of end point URLs. The
     * <code>SOAPClient</code> will iterate through the URLs in case of server
     * failure.
     */
    public SOAPClient(String urls[]) {
        this.urls = urls;
    }
    
    /**
     * Performs a raw SOAP call with "message" as the SOAP data and response is
     * returned as <code>StringBuffer</code>
     */
    public InputStream call(String message, String cookies) throws Exception {
        if (debug.messageEnabled()) {
            debug.message("SOAP Client: Message being sent:" + message);
        }
        // Setup the connection, support for failover
        InputStream in_buf = null;
        boolean done = false;
        int urlIndex = 0;
        while (!done) {
            // Check for a valid URL, if not find one
            if (url == null) {
                // Check if URLs are provided at the time of
                // constructor, else get it from JAXRPCUtils
                if (urls != null) {
                    if (urlIndex >= urls.length) {
                        // All the URLs have been checked
                        // throw RemoteException
                        if (debug.warningEnabled()) {
                            debug.warning("SOAPClient: No vaild server found");
                        }
                        throw (new RemoteException("no-server-found"));
                    }
                    url = urls[urlIndex++];
                } else {
                    // This function throws RemoteException
                    // if no servers are found
                    url = JAXRPCUtil.getValidURL(serviceName);
                }
            }
            
            URL endpoint = new URL(url);
            HttpURLConnection connection = 
                HttpURLConnectionManager.getConnection(endpoint);
            connection.setDoOutput(true);
            connection.setUseCaches(useCache);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                "text/xml; charset=\"utf-8\"");
            connection.setRequestProperty("SOAPAction", "\"\"");
            if (cookies != null) {
                connection.setRequestProperty("Cookie", cookies);
            }
            String userInfo = endpoint.getUserInfo();
            if (userInfo != null) {
                connection.setRequestProperty("Authorization", "Basic "
                    + Base64.encode(userInfo.getBytes("UTF-8")));
            }
            
            // Output
            byte[] data = message.getBytes("UTF-8");
            int requestLength = data.length;
            connection.setRequestProperty("Content-Length", Integer
                .toString(requestLength));
            OutputStream out = null;
            try {
                out = connection.getOutputStream();
            } catch (ConnectException ce) {
                // Debug the exception
                if (debug.warningEnabled()) {
                    debug.warning("SOAP Client: Connection Exception: " + url,
                        ce);
                }
                // Server may be down, try the next server
                JAXRPCUtil.serverFailed(url);
                url = null;
                continue;
            }
            // Write out the message
            out.write(data);
            out.flush();
            
            // Get the response
            try {
                in_buf = connection.getInputStream();
            } catch (IOException ioe) {
                // Could be receiving SOAP fault
                // Debug the exception
                if (debug.messageEnabled()) {
                    debug.message("SOAP Client: READ Exception", ioe);
                }
                in_buf = connection.getErrorStream();
                isException = true; // Used by send(...)
            } finally {
                done = true;
            }
            
        }
        
        // Debug the input/output messages
        if (debug.messageEnabled()) {
            StringBuffer inbuf = new StringBuffer();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                in_buf, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                inbuf.append(line).append("\n");
            }
            String data = new String(inbuf);
            debug.message("SOAP Client: Input: " + message + "\nOutput: "
                + data);
            in_buf = new ByteArrayInputStream(data.getBytes("UTF-8"));
        }
        return (in_buf);
    }
    
    /**
     * Performs a JAXRPC method call. The parameter <code>
     * functionName</code>
     * is the JAXRPC function to be called with parameters <code>params</code>.
     * Returns an object on success, else throws an <code>Exception
     * </code>.
     */
    public synchronized Object send(String functionName, Object params[],
        String cookies) throws Exception {
        return (send(encodeMessage(functionName, params), cookies));
    }
    
    /**
     * Performs a JAXRPC method call. The parameter <code>
     * functionName</code>
     * is the JAXRPC function to be called with parameter <code>param</code>.
     * Returns an object on success, else throws an <code>Exception
     * </code>.
     */
    public synchronized Object send(String functionName, Object param,
        String cookies) throws Exception {
        return (send(encodeMessage(functionName, param), cookies));
    }
    
    /**
     * Performs a JAXRPC method call. The parameter <code>
     * message</code>
     * contains SOAP encoded function call obtained from
     * <code>encodeMessage</code>. Returns an object on success, else throws
     * an <code>Exception
     * </code>.
     */
    public synchronized Object send(String message, String cookies)
    throws Exception {
        // Initialize variables
        exceptionClassName = exceptionMessage = null;
        resourceBundleName = exceptionCode = null;
        smsExceptionCode = null;
        messageArgs = null;
        errorString = null;
        ldapErrorCode = 0;
        isException = false;
        
        // Send the SOAP request and get the response
        InputStream in_buf = call(message, cookies);
        
        // Decode the output. Parse the document using SAX
        SOAPContentHandler handler = new SOAPContentHandler();
        try {
            SAXParser saxParser;
            if (debug.warningEnabled()) {
                saxParser = XMLUtils.getSafeSAXParser(true);
            } else {
                saxParser = XMLUtils.getSafeSAXParser(false);
            }
            XMLReader parser = saxParser.getXMLReader();
            parser.setContentHandler(handler);
            parser.setErrorHandler(new SOAPErrorHandler());
            parser.parse(new InputSource(in_buf));
        } catch (ParserConfigurationException pce) {
            if (debug.warningEnabled()) {
                debug.warning("SOAPClient:send parser config exception", pce);
            }
        } catch (SAXException saxe) {
            if (debug.warningEnabled()) {
                debug.warning("SOAPClient:send SAX exception", saxe);
            }
        }
        
        // Check for exceptions
        if (isException) {
            throw (exception);
        }
        
        return (handler.getObject());
    }
    
    public void setURL(String url) {
        this.url = url;
    }
    
    void setURLs(String[] urls) {
        this.urls = urls;
    }
    
    String encodeString(String str) {
        return (encodeString("String_1", str));
    }
    
    String encodeInt(String name, Integer i) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"xsd:int\">");
        sb.append(i).append("</").append(name).append(">");
        return (sb.toString());
    }
    
    String encodeInt(Integer i) {
        return (encodeInt("int_1", i));
    }
    
    String encodeBoolean(String name, Boolean b) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"xsd:boolean\">");
        sb.append(b).append("</").append(name).append(">");
        return (sb.toString());
    }
    
    String encodeBoolean(Boolean b) {
        return (encodeBoolean("boolean_1", b));
    }
    
    String encodeString(String name, String str) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"xsd:string\">");
        // Make string to be XML compliant
        String data = SMSSchema.escapeSpecialCharacters(str);
        sb.append(data).append("</").append(name).append(">");
        return (sb.toString());
    }
    
    String encodeSet(Set set) {
        return (encodeSet("Set_1", set));
    }
    
    String encodeSet(String name, Set set) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"ns1:hashSet\" enc:arrayType=\"xsd:anyType[");
        sb.append(set.size());
        sb.append("]\">");
        for (Iterator items = set.iterator(); items.hasNext();) {
            sb.append(encodeString("item", items.next().toString()));
        }
        sb.append("</").append(name).append(">");
        return (sb.toString());
    }
    
    String encodeList(List list) {
        return (encodeList("List_1", list));
    }
    
    String encodeList(String name, List list) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"ns1:linkedList\" enc:arrayType=\"xsd:anyType[");
        sb.append(list.size());
        sb.append("]\">");
        for (Iterator items = list.iterator(); items.hasNext();) {
            sb.append(encodeString("item", items.next().toString()));
        }
        sb.append("</").append(name).append(">");
        return (sb.toString());
    }
    
    public String encodeMap(Map map) {
        return (encodeMap("Map_1", map));
    }
    
    String encodeByteArrayArray(String name, byte[][] data) {
        return (null);
    }
    
    public String encodeMap(String name, Map map) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("<").append(name);
        sb.append(" xsi:type=\"ns1:hashMap\" enc:arrayType=\"ns1:mapEntry[");
        sb.append(map.size()).append("]\">");
        for (Iterator items = map.entrySet().iterator(); items.hasNext();) {
            Map.Entry entry = (Map.Entry) items.next();
            sb.append("<item xsi:type=\"ns1:mapEntry\">");
            sb.append(encodeString("key", entry.getKey().toString()));
            Object value = entry.getValue();
            if (value instanceof java.util.Set) {
                sb.append(encodeSet("value", (Set) value));
            } else if (value instanceof java.util.Map) {
                sb.append(encodeMap("value", (Map) value));
            } else if (value instanceof java.util.List) {
                sb.append(encodeList("value", (List) value));
            } else if (value instanceof java.lang.String) {
                sb.append(encodeString("value", (String) value));
            } else if (value instanceof byte[][]) {
                sb.append(encodeByteArrayArray("value", (byte[][]) value));
            }
            sb.append("</item>");
        }
        sb.append("</").append(name).append(">");
        return (sb.toString());
    }
    
    public Map decodeMap(String xmlMap) {
        if (xmlMap == null || xmlMap.length() == 0) {
            return (Collections.EMPTY_MAP);
        }
        // Add prefix and suffix to the xmlMap
        StringBuilder sb = new StringBuilder(200);
        sb.append(DECODE_HEADER);
        sb.append(xmlMap);
        sb.append(DECODE_FOOTER);
        SOAPContentHandler handler = new SOAPContentHandler();
        try {
            SAXParser saxParser = XMLUtils.getSafeSAXParser(false);
            XMLReader parser = saxParser.getXMLReader();
            parser.setContentHandler(handler);
            parser.setErrorHandler(new SOAPErrorHandler());
            parser.parse(new InputSource(new ByteArrayInputStream(sb.toString()
            .getBytes("UTF-8"))));
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("SOAPClient::decodeMap Exception", e);
            }
            return (Collections.EMPTY_MAP);
        }
        return ((Map) handler.getObject());
    }
    
    /**
     * Returns a SOAP request compliant with JAXRPC for the provide function
     * name <code>function</code> that takes the parameter <code>param</code>
     * as the only argument.
     */
    public String encodeMessage(String function, Object param) {
        Object params[] = null;
        if (param != null) {
            params = new Object[1];
            params[0] = param;
        }
        return (encodeMessage(function, params));
    }
    
    /**
     * Returns a SOAP request compliant with JAXRPC for the provide function
     * name <code>function</code> that takes the parameters
     * <code>params</code> as its arguments.
     */
    public synchronized String encodeMessage(String function, Object[] params) {
        int index = 1;
        StringBuilder sb = new StringBuilder(1000);
        sb.append(ENVELOPE).append(HEADSTART).append(HEADEND).append(ENV_BODY);
        sb.append("<ans1:").append(function).append(
            " xmlns:ans1=\"http://isp.com/wsdl\">");
        for (int i = 0; (params != null) && (i < params.length); i++) {
            if (params[i] instanceof java.lang.String) {
                sb
                    .append(encodeString("String_" + index++,
                    (String) params[i]));
            } else if (params[i] instanceof java.util.Set) {
                sb.append(encodeSet("Set_" + index++, (Set) params[i]));
            } else if (params[i] instanceof java.util.Map) {
                sb.append(encodeMap("Map_" + index++, (Map) params[i]));
            } else if (params[i] instanceof java.util.List) {
                sb.append(encodeList("List_" + index++, (List) params[i]));
            } else if (params[i] instanceof Integer) {
                sb.append(encodeInt("int_" + index++, (Integer) params[i]));
            } else if (params[i] instanceof Boolean) {
                sb.append(encodeBoolean("boolean_" + index++,
                    (Boolean) params[i]));
            } else if (params[i] == null) {
                index++;
            } else {
                debug.error("SOAPClient: Unknown class: "
                    + params.getClass().getName());
            }
        }
        sb.append("</ans1:").append(function).append(">").append(SUFFIX);
        return (sb.toString());
    }
    
    class SOAPContentHandler implements org.xml.sax.ContentHandler {
        Locator locator;
        
        boolean started;
        
        List types, maps, keys;
        
        Object answer;
        
        String currentType, type;
        
        StringBuffer currentString;
        
        Set set, currentSet;
        
        Map map;
        
        List list, currentList;
        
        protected SOAPContentHandler() {
            types = new LinkedList();
            maps = new LinkedList();
            keys = new LinkedList();
        }
        
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        
        public void startElement(String namespaceURI, String localName,
            String rawName, Attributes attrs) throws SAXException {
            if (!started && localName.equalsIgnoreCase(BODY)) {
                started = true;
                return;
            }
            if (!isException) {
                if (localName.equalsIgnoreCase(RESULT)) {
                    // Determine the object type
                    type = currentType = attrs.getValue(attrs.getIndex(TYPE));
                    if (type.equalsIgnoreCase(SET)) {
                        set = currentSet = new OrderedSet();
                    } else if (type.equalsIgnoreCase(TREESET)) {
                        set = currentSet = new TreeSet();
                    } else if (type.equalsIgnoreCase(MAP)) {
                        map = new HashMap();
                        maps.add(0, map);
                    } else if (type.equalsIgnoreCase(LIST)) {
                        list = currentList = new LinkedList();
                    }
                } else if (localName.equalsIgnoreCase(ITEM)) {
                    // Get the type, could be String, Set or MapEntry
                    types.add(0, currentType);
                    currentType = attrs.getValue(attrs.getIndex(TYPE));
                } else if (localName.equalsIgnoreCase(VALUE)) {
                    types.add(0, currentType);
                    currentType = attrs.getValue(attrs.getIndex(TYPE));
                    if (currentType.equalsIgnoreCase(SET)) {
                        currentSet = new OrderedSet();
                    } else if (currentType.equalsIgnoreCase(MAP)) {
                        maps.add(0, new HashMap());
                    } else if (currentType.equalsIgnoreCase(LIST)) {
                        currentList = new LinkedList();
                    }
                }
            } else {
                // Check for ArrayOfanyType and reset the set
                if (localName.equals(ARRAY_OF_ANY_TYPE)) {
                    messageArgs = new HashSet();
                }
            }
            
            // Initialize currentString
            if (currentString == null) {
                currentString = new StringBuffer();
            }
        }
        
        public void endElement(String namespaceURI, String localName,
            String rawName) throws SAXException {
            // Determine the current type and copy elements to answer
            if (!started) {
                return;
            }
            // Process results if exception is not returned
            if (!isException) {
                if (localName.equalsIgnoreCase(ITEM)) {
                    // End of an item
                    if (currentType.equalsIgnoreCase(STRING)) {
                        if (currentSet != null) {
                            currentSet.add(currentString.toString());
                        } else {
                            currentList.add(currentString.toString());
                        }
                    }
                    currentType = (String) types.remove(0);
                } else if (localName.equalsIgnoreCase(VALUE)) {
                    if (currentType.equalsIgnoreCase(SET) ||
                        currentType.equalsIgnoreCase(TREESET)) {
                        Map map1 = (Map) maps.get(0);
                        map1.put(keys.remove(0), currentSet);
                    } else if (currentType.equalsIgnoreCase(MAP)) {
                        Map map1 = (Map) maps.remove(0);
                        Map map2 = (Map) maps.get(0);
                        map2.put(keys.remove(0), map1);
                    } else if (currentType.equalsIgnoreCase(LIST)) {
                        Map map1 = (Map) maps.get(0);
                        map1.put(keys.remove(0), currentList);
                    } else if (currentType.equalsIgnoreCase(STRING)
                    && ((String) types.get(0))
                    .equalsIgnoreCase(MAPENTRY)) {
                        map.put(keys.remove(0), currentString.toString());
                    }
                    currentType = (String) types.remove(0);
                } else if (localName.equals(KEY)) {
                    // End if key value for Map
                    keys.add(0, currentString.toString());
                } else if (localName.equalsIgnoreCase(RESULT)) {
                    // End of results
                    if (type.equalsIgnoreCase(SET) ||
                        type.equalsIgnoreCase(TREESET)) {
                        answer = set;
                    } else if (type.equalsIgnoreCase(MAP)) {
                        answer = map;
                    } else if (type.equalsIgnoreCase(STRING)) {
                        answer = currentString.toString();
                    } else if (type.equalsIgnoreCase(INTEGER)) {
                        try {
                            answer = new Integer(currentString.toString());
                        } catch (NumberFormatException nfe) {
                            answer = new Integer(0);
                        }
                    } else if (type.equalsIgnoreCase(BOOLEAN)) {
                        answer = Boolean.valueOf(currentString.toString());
                    } else if (type.equalsIgnoreCase(LIST)) {
                        answer = list;
                    }
                }
            } else {
                // Exception was thrown
                if (localName.equals(FAULT_STRING)) {
                    exceptionClassName = currentString.toString();
                } else if (localName.equals(MESSAGE)) {
                    exceptionMessage = currentString.toString();
                } else if (localName.equals(ERROR_CODE)) {
                    exceptionCode = currentString.toString();
                } else if (localName.equals(EXCEPTION_CODE)) {
                    smsExceptionCode = currentString.toString();
                } else if (localName.equals(RESOURCE_BUNDLE_NAME)) {
                    resourceBundleName = currentString.toString();
                } else if (localName.equals(ITEM)) {
                    messageArgs.add(currentString.toString());
                }
                if (localName.equals(LDAP_ERROR_CODE)) {
                    ldapErrorCode = Integer.parseInt(currentString.toString());
                }
                if (localName.equals(ERROR_STRING)) {
                    errorString = currentString.toString();
                }
            }
            // Set currentString to null
            currentString = null;
        }
        
        public void characters(char[] ch, int start, int length)
        throws SAXException {
            currentString.append(ch, start, length);
        }
        
        Object getObject() {
            return (answer);
        }
        
        public void startDocument() throws SAXException {
            // Ignore
        }
        
        public void endDocument() throws SAXException {
            // If exception is thrown, construct the exception
            if (isException) {
                if (exceptionClassName.equals(SSOEXCEPTION)) {
                    if (resourceBundleName != null) {
                        exception = new SSOException(resourceBundleName,
                            exceptionCode, messageArgs.toArray());
                    } else if (exceptionMessage != null) {
                        exception = new SSOException(exceptionMessage);
                    } else {
                        exception = new SSOException("no message");
                    }
                } else if (exceptionClassName.equals(SMSEXCEPTION)) {
                    if (resourceBundleName != null) {
                        exception = new SMSException(resourceBundleName,
                            exceptionCode, (messageArgs == null) ? null
                            : messageArgs.toArray());
                    } else if (exceptionMessage != null
                        && exceptionCode != null) {
                        try {
                            exception = new SMSException(Integer
                                .parseInt(exceptionCode), exceptionMessage);
                        } catch (NumberFormatException nfe) {
                            // Ignore
                            exception = new SMSException(exceptionMessage);
                        }
                    } else if (smsExceptionCode != null) {
                        try {
                            exception = new SMSException(Integer
                                .parseInt(smsExceptionCode), null);
                        } catch (NumberFormatException nfe) {
                            // Ignore
                            exception = new SMSException(exceptionCode);
                        }
                    } else if (exceptionMessage != null) {
                        exception = new SMSException(exceptionMessage);
                    } else {
                        // Throw generic SMSException
                        exception = new SMSException();
                    }
                } else if (exceptionClassName.equals(IDREPOEXCEPTION)) {
                    if (resourceBundleName != null) {
                        exception = new IdRepoException(
                            resourceBundleName, exceptionCode,
                            (messageArgs == null) ? null :
                                messageArgs.toArray());
                    } else if (exceptionCode != null) {
                        exception = new IdRepoException(exceptionMessage,
                            exceptionCode);
                    } else if (exceptionMessage != null) {
                        exception = new IdRepoException(exceptionMessage);
                    } else {
                        // Throw generic SMSException
                        exception = new SMSException();
                    }
                } else if (exceptionClassName.equals(AMREMOTEEXCEPTION)) {
                    exception = new AMRemoteException(exceptionMessage,
                        exceptionCode, ldapErrorCode,
                        (messageArgs != null) ? (String[]) messageArgs
                        .toArray() : null);
                } else if (exceptionClassName.equals(ENTITYEXCEPTION)) {
                    if (messageArgs != null) {
                        exception = new EntityException(exceptionMessage,
                            exceptionCode, messageArgs.toArray());
                    } else {
                        exception = new EntityException(exceptionMessage,
                            exceptionCode);
                    }
                } else if (exceptionClassName.equals(SAMLEXCEPTION)) {
                    if (exceptionMessage != null) {
                        exception = new SOAPClientException(exceptionClassName,
                            exceptionMessage);
                    } else {
                        exception = new SOAPClientException(exceptionClassName);
                    }
                } else if (exceptionClassName.indexOf(RMIREMOTEEXCEPTION) != -1) {
                    exception = new RemoteException(exceptionClassName);
                } else {
                    // Catch all for remaing exception
                    if (exceptionMessage != null) {
                        exception = new Exception(exceptionMessage);
                    } else {
                        exception = new Exception("unknown-exception");
                    }
                }
            }
        }
        
        public void processingInstruction(String target, String data)
        throws SAXException {
            // Ignore
        }
        
        public void startPrefixMapping(String prefix, String url) {
            // Ignore
        }
        
        public void endPrefixMapping(String prefix) {
            // Ignore
        }
        
        public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
            // Ignore white spaces
        }
        
        public void skippedEntity(String name) throws SAXException {
            // Ignore skipped entry
        }
    }
    
    class SOAPErrorHandler implements ErrorHandler {
        
        SOAPErrorHandler() {
            // do nothing
        }
        
        public void fatalError(SAXParseException spe) throws SAXParseException {
            debug.error("SOAPClient:PARSER.fatalError", spe);
        }
        
        public void error(SAXParseException spe) throws SAXParseException {
            // do nothing, error can be ignored
        }
        
        public void warning(SAXParseException spe) throws SAXParseException {
            // Parser warning can be ignored
        }
    }
    
    // Static variables
    static final String ENVELOPE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xmlns:enc=\"http://schemas.xmlsoap.org/soap/encoding/\" "
        + "xmlns:ns0=\"http://isp.com/types\" "
        + "xmlns:ns1=\"http://java.sun.com/jax-rpc-ri/internal\" "
        + "env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">";
    
    static final String HEADSTART = "<env:Header>";
    
    static final String HEADEND = "</env:Header>";
    
    static final String ENV_BODY = "<env:Body>";
    
    static final String SUFFIX = "</env:Body></env:Envelope>\n";
    
    static final String BODY = "body";
    
    static final String RESULT = "result";
    
    static final String TYPE = "xsi:type";
    
    static final String STRING = "xsd:string";
    
    static final String INTEGER = "xsd:int";
    
    static final String BOOLEAN = "xsd:boolean";
    
    static final String SET = "ns1:hashSet";
    
    static final String TREESET = "ns1:treeSet";
    
    static final String MAP = "ns1:hashMap";
    
    static final String LIST = "ns1:linkedList";
    
    static final String MAPENTRY = "ns1:mapEntry";
    
    static final String ITEM = "item";
    
    static final String KEY = "key";
    
    static final String VALUE = "value";
    
    static final String FAULT_STRING = "faultstring";
    
    static final String MESSAGE = "message";
    
    static final String ERROR_CODE = "errorCode";
    
    static final String EXCEPTION_CODE = "exceptionCode";
    
    static final String LDAP_ERROR_CODE = "LDAPErrorCode";
    
    static final String ERROR_STRING = "errorString";
    
    static final String RMIREMOTEEXCEPTION = "java.rmi.RemoteException";
    
    static final String SSOEXCEPTION = "com.iplanet.sso.SSOException";
    
    static final String AMREMOTEEXCEPTION =
        "com.iplanet.am.sdk.remote.AMRemoteException";
    
    static final String SMSEXCEPTION = "com.sun.identity.sm.SMSException";
    
    static final String IDREPOEXCEPTION =
        "com.sun.identity.sm.IdRepoException";
    
    static final String ENTITYEXCEPTION =
        "com.sun.identity.entity.EntityException";
    
    static final String SAMLEXCEPTION = "com.sun.identity.common.SAMLException";
    
    static final String RESOURCE_BUNDLE_NAME = "resourceBundleName";
    
    static final String MESSAGE_ARGS = "messageArgs";
    
    static final String HREF = "href";
    
    static final String ARRAY_OF_ANY_TYPE = "ArrayOfanyType";
    
    static final String ID = "id";
    
    static final String DECODE_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xmlns:enc=\"http://schemas.xmlsoap.org/soap/encoding/\" "
        + "xmlns:ns0=\"http://isp.com/types\" "
        + "xmlns:ns1=\"http://java.sun.com/jax-rpc-ri/internal\" "
        + "env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "<env:Body><ans1:readResponse xmlns:ans1=\"http://isp.com/wsdl\">";
    
    static final String DECODE_FOOTER =
        "</ans1:readResponse></env:Body></env:Envelope>";
}
