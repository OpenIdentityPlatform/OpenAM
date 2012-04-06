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
 * $Id: ResponseSet.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.share;

import java.util.Vector;

/**
 * This <code>ResponseSet</code> class represents a ResponseSet XML document.
 * The ResponseSet DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !-- This DTD is used by PLL --&gt;
 *     &lt; !DOCTYPE ResponseSet [
 *     &lt; !ELEMENT ResponseSet(Response)+&gt;
 *     &lt; !ATTLIST ResponseSet
 *       vers  CDATA #REQUIRED
 *       svcid CDATA #REQUIRED
 *       reqid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Response(#PCDATA)*&gt;
 *     &lt; !ATTLIST Response
 *       dtdid CDATA #IMPLIED&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 * Each ResponseSet object contains a version, service ID, response set ID, the
 * original request set ID, and a collection of Response objects. The
 * ResponseSet views each Response object as a String. This makes it possible
 * that the content of the Response object can be another XML document. The PLL
 * provides a reference Response DTD. Please see class Response for details on
 * the Response DTD. This class also provides a method to aggregate each
 * Response object and returns a ResponseSet XML document based on the
 * ResponseSet DTD mentioned above.
 * 
 * @see com.iplanet.services.comm.share.Response
 */

public class ResponseSet {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    static final String BEGIN_CDATA = "<![CDATA[";

    static final String END_CDATA = "]]>";

    private String responseSetVersion = null;

    private String serviceID = null;

    private String requestSetID = null;

    private Vector responseVector = new Vector();

    /**
     * This constructor is used primarily at the server side to construct a
     * ResponseSet object for a given service. Individual response shall be
     * added to this object by calling addResponse method.
     * 
     * @param service
     *            The name of the service.
     */
    public ResponseSet(String service) {
        serviceID = service;
        responseSetVersion = "1.0";
    }

    /*
     * This constructor is used by ResponseSetParser to reconstruct a
     * ResponseSet object.
     */
    ResponseSet() {
    }

    /**
     * This method is used primarily at the client side to reconstruct a
     * ResponseSet object based on the XML document received from server. The
     * DTD of this XML document is described above.
     * 
     * @param xml
     *            The ResponseSet XML document String.
     */
    public static ResponseSet parseXML(String xml) {
        // Parse the XML document and extract the XML objects out of the
        // XML document
        ResponseSetParser parser = new ResponseSetParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the original RequestSet ID for this object.
     * 
     * @param id The original RequestSet ID.
     */
    public void setRequestSetID(String id) {
        requestSetID = id;
    }

    /**
     * Gets the Response objects contained in this object.
     * 
     * @return A Vector of Response objects.
     */
    public Vector getResponses() {
        return responseVector;
    }

    /**
     * Adds a Response object to this object.
     * 
     * @param response A reference to a Response object.
     */
    public void addResponse(Response response) {
        responseVector.addElement(response);
    }

    /**
     * Returns an XML ResponseSet document in String format. The returned String
     * is formatted based on the ResponseSet DTD by aggregating each Response
     * object in this object.
     * 
     * @return An XML ResponseSet document in String format.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(300);
        xml.append("<?xml version=").append(QUOTE).append("1.0").append(QUOTE)
                .append(" encoding=").append(QUOTE).append("UTF-8").append(
                        QUOTE).append(" standalone=").append(QUOTE).append(
                        "yes").append(QUOTE).append("?>").append(NL);

        xml.append("<ResponseSet vers=").append(QUOTE).append(
                responseSetVersion).append(QUOTE).append(" svcid=").append(
                QUOTE).append(serviceID).append(QUOTE).append(" reqid=")
                .append(QUOTE).append(requestSetID).append(QUOTE).append('>')
                .append(NL);

        for (int i = 0; i < responseVector.size(); i++) {
            Response res = (Response) responseVector.elementAt(i);
            xml.append("<Response");
            if (res.getDtdID() != null) {
                xml.append(" dtdid=").append(QUOTE).append(res.getDtdID())
                        .append(QUOTE);
            }
            xml.append('>');
            xml.append(BEGIN_CDATA).append(res.getContent()).append(END_CDATA);
            xml.append("</Response>").append(NL);
        }
        xml.append("</ResponseSet>");
        return (xml.toString());
    }

    /*
     * The following methods are used by ResponseParser to reconstruct a
     * ResponseSet object.
     */
    void setResponseSetVersion(String ver) {
        responseSetVersion = ver;
    }

    void setServiceID(String id) {
        serviceID = id;
    }
}
