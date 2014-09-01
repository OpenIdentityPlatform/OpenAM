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
 * $Id: NamingRequest.java,v 1.4 2008/06/25 05:41:40 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.naming.share;

/**
 * This <code>NamingRequest</code> class represents a NamingRequest XML
 * document. The NamingRequest DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE NamingRequest [
 *     &lt; !ELEMENT NamingRequest (GetNamingProfile)&gt;
 *     &lt; !ATTLIST NamingRequest
 *       vers   CDATA #REQUIRED
 *       reqid  CDATA #REQUIRED
 *       sessid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetNamingProfile EMPTY&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */

public class NamingRequest {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    private String requestVersion = null;

    private String requestID = null;

    private String sessionId = null;

    private String preferredNamingURL = null;

    private static int requestCount = 0;

    public static final String reqVersion = "3.0";

    /*
     * Constructors
     */

    /**
     * This constructor shall only be used at the client side to construct a
     * NamingRequest object.
     * 
     * @param ver
     *            The naming request version.
     */
    public NamingRequest(String ver) {
        float version = Float.valueOf(ver).floatValue();
        requestVersion = (version <= 1.0) ? reqVersion : ver;
        requestID = Integer.toString(requestCount++);
    }

    /*
     * This constructor is used by NamingRequestParser to reconstruct a
     * NamingRequest object.
     */
    NamingRequest() {
    }

    /**
     * This method is used primarily at the server side to reconstruct a
     * NamingRequest object based on the XML document received from client. The
     * DTD of this XML document is described above.
     * 
     * @param xml
     *            The NamingRequest XML document String.
     * @return <code>NamingRequest</code> object.
     */
    public static NamingRequest parseXML(String xml) {
        NamingRequestParser parser = new NamingRequestParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the request version.
     * 
     * @param version
     *            A string representing the request version.
     */
    public void setRequestVersion(String version) {
        requestVersion = version;
    }

    /**
     * Gets the request version.
     * 
     * @return The request version.
     */
    public String getRequestVersion() {
        return requestVersion;
    }

    /**
     * Sets the request ID.
     * 
     * @param id
     *            A string representing the request ID.
     */
    public void setRequestID(String id) {
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
     * Sets the session ID.
     * 
     * @param id
     *            A string representing the session ID.
     */
    public void setSessionId(String id) {
        sessionId = id;
    }

    /**
     * Gets the session ID.
     * 
     * @return The session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the PreferredNamingURL.
     * @param url A string representing preferred NamingURL by client.
     */
    public void setPreferredNamingURL(String url){
         preferredNamingURL = url;
    }

    /**
     * Gets the PreferredNamingURL.
     * @return The PreferredNamingURL.
     */
    public String getPreferredNamingURL(){
        return preferredNamingURL;
    }

    /**
     * This method translates the request to an XML document String based on the
     * NamingRequest DTD described above.
     * 
     * @return An XML String representing the request.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(150);
        xml.append("<NamingRequest vers=").append(QUOTE).append(requestVersion)
           .append(QUOTE).append(" reqid=").append(QUOTE).append(requestID);
        if (sessionId != null) {
            xml.append(QUOTE).append(" sessid=")
               .append(QUOTE).append(sessionId);
        }
        if (preferredNamingURL != null) {
            xml.append(QUOTE).append(" preferredNamingURL=");
            xml.append(QUOTE).append(preferredNamingURL);
        }

        xml.append(QUOTE).append('>').append(NL);
        xml.append("<GetNamingProfile>").append(NL);
        xml.append("</GetNamingProfile>").append(NL);
        xml.append("</NamingRequest>");

        return xml.toString();
    }
}
