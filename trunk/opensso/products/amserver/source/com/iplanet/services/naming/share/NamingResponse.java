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
 * $Id: NamingResponse.java,v 1.6 2008/11/12 08:56:52 mchlbgs Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.naming.share;

import java.util.Enumeration;
import java.util.Hashtable;


/**
 * This <code>NamingResponse</code> class represents a NamingResponse XML
 * document. The NamingResponse DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE NamingResponse [
 *     &lt; !ELEMENT NamingResponse (GetNamingProfile)&gt;
 *     &lt; !ATTLIST NamingResponse
 *       vers   CDATA #REQUIRED
 *       reqid  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetNamingProfile (Attribute*|Exception)&gt;
 *     &lt; !ELEMENT Attribute EMPTY&gt;
 *     &lt; !ATTLIST Attribute
 *       name   CDATA #REQUIRED
 *       value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Exception (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */

public class NamingResponse {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    private String responseVersion = "1.0";

    private String requestID = null;

    private Hashtable namingTable = new Hashtable();

    private String exception = null;

    /*
     * Constructors
     */

    /**
     * This constructor shall only be used at the server side to construct a
     * NamingResponse object.
     * 
     * @param reqid
     *            The original request ID.
     */
    public NamingResponse(String reqid) {
        requestID = reqid;
    }

    /*
     * This constructor is used by NamingResponseParser to reconstruct a
     * NamingResponse object.
     */
    NamingResponse() {
    }

    /**
     * This method is used primarily at the client side to reconstruct a
     * NamingResponse object based on the XML document received from server. The
     * DTD of this XML document is described above.
     * 
     * @param xml
     *            The NamingResponse XML document String.
     */
    public static NamingResponse parseXML(String xml) {
        NamingResponseParser parser = new NamingResponseParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the response version.
     * 
     * @param version
     *            A string representing the response version.
     */
    void setResponseVersion(String version) {
        responseVersion = version;
    }

    /**
     * Gets the response version.
     * 
     * @return The response version.
     */
    public String getResponseVersion() {
        return responseVersion;
    }

    /**
     * Sets the request ID.
     * 
     * @param id
     *            A string representing the original request ID.
     */
    void setRequestID(String id) {
        requestID = id;
    }

    /**
     * Gets the request ID.
     * 
     * @return The request ID.
     */
    public String getRequestID() {
        return requestID;
    }

    /**
     * Sets the naming attribute.
     * 
     * @param name
     *            attribute name.
     * @param value
     *            attribute value.
     */
    public void setAttribute(String name, String value) {
        namingTable.put(name, value);
    }

    /**
     * Gets the attribute.
     * 
     * @param name
     *            attribute name.
     * @return the attribute value.
     */
    public String getAttribute(String name) {
        return (String) namingTable.get(name);
    }

    /**
     * Gets the naming table.
     */
    public Hashtable getNamingTable() {
        return namingTable;
    }

    /**
     * Sets the naming table.
     */
    public void setNamingTable(Hashtable table) {
        namingTable = table;
    }

    /**
     * Sets the exception.
     * 
     * @param ex A string representing the exception.
     */
    public void setException(String ex) {
        exception = ex;
    }

    /**
     * Gets the exception.
     * 
     * @return The exception.
     */
    public String getException() {
        return exception;
    }
    
    /**
     * Replaces "%uri" with the acutal URI
     */
    public void replaceURI(String uri) {
        if ((namingTable != null) && !namingTable.isEmpty()) {
            Hashtable newNamingTable = new Hashtable();
            Enumeration e = namingTable.keys();
            while (e.hasMoreElements()) {
                String name = e.nextElement().toString();
                String value = namingTable.get(name).toString();
                if (value.indexOf("%uri") != -1) {
                    value = value.replaceAll("%uri", uri);
                } else {
                    // strip the URI if the entry is like
                    // 01=http://whatever.domain.com/opensso
                    // because previous releases of the agent
                    // do serverId lookup without the uri.
                    try {
                        Integer.parseInt(name);
                        int li = value.lastIndexOf(uri);
                        if (li != -1 && value.endsWith(uri)) { 
                             value = value.substring(0,li);
                        }
                    } catch (NumberFormatException ex) {
                        //ignore
                    }
                }
                newNamingTable.put(name, value);
            }
            namingTable = newNamingTable;
        }
    }

    /**
     * This method translates the response to an XML document String based on
     * the NamingResponse DTD described above.
     * 
     * @return An XML String representing the response.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(200);
        xml.append("<NamingResponse vers=").append(QUOTE).append(
                responseVersion).append(QUOTE).append(" reqid=").append(QUOTE)
                .append(requestID).append(QUOTE).append('>').append(NL);
        xml.append("<GetNamingProfile>").append(NL);
        if (exception != null) {
            xml.append("<Exception>").append(exception).append("</Exception>")
                    .append(NL);
        } else {
            Enumeration e = namingTable.keys();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String value = (String) namingTable.get(name);
                xml.append("<Attribute name=").append(QUOTE).append(name).append(QUOTE)
                        .append(" value=").append(QUOTE).append(value).append(
                                QUOTE).append('>').append(
                                "</Attribute>").append(NL);
            }

        }
        xml.append("</GetNamingProfile>").append(NL);
        xml.append("</NamingResponse>");

        return xml.toString();
    }
}
