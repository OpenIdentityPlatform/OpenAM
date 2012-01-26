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
 * $Id: RequestSet.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.iplanet.services.comm.share;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This <code>RequestSet</code> class represents a RequestSet XML document.
 * The RequestSet DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *    &lt;?xml version=&quot;1.0&quot;&gt;
 *    &lt; !-- This DTD is used by PLL --&gt;
 *    &lt; !DOCTYPE RequestSet [
 *    &lt; !ELEMENT RequestSet(Request)+&gt;
 *    &lt; !ATTLIST RequestSet
 *       vers  CDATA #REQUIRED
 *       svcid CDATA #REQUIRED
 *       reqid CDATA #REQUIRED&gt;
 *    &lt; !ELEMENT Request(#PCDATA)*&gt;
 *    &lt; !ATTLIST Request
 *       dtdid CDATA #IMPLIED
 *       sid   CDATA #IMPLIED&gt;
 *    ]&gt;
 * </pre>
 * 
 * </p>
 * Each RequestSet object contains a version, service ID, request set ID, and a
 * collection of Request objects. The RequestSet views each Request object as a
 * String. This makes it possible that the content of the Request object can be
 * another XML document. The PLL provides a reference Request DTD. Please see
 * class Request for details on the Request DTD. This class also provides a
 * method to aggregate each Request object and returns a RequestSet XML document
 * based on the RequestSet DTD mentioned above.
 * 
 * @see com.iplanet.services.comm.share.Request
 */

public class RequestSet {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    static final String BEGIN_CDATA = "<![CDATA[";

    static final String END_CDATA = "]]>";

    private String requestSetVersion = null;

    private String serviceID = null;

    private String requestSetID = null;

    private List<Request> requestSet = new ArrayList<Request>();

    private static int requestCount = 0;

    /**
     * This constructor is used primarily at the client side to construct a
     * RequestSet object for a given service. Individual request shall be added
     * to this object by calling addRequest method. service.
     * 
     * @param service
     *            The name of the service.
     */
    public RequestSet(String service) {
        serviceID = service;
        requestSetVersion = "1.0";
        requestSetID = Integer.toString(requestCount++);
    }

    /*
     * This constructor is used by RequestSetParser to reconstruct a RequestSet
     * object.
     */
    RequestSet() {
    }

    /**
     * This method is used primarily at the server side to reconstruct a
     * RequestSet object based on the XML document received from client. The DTD
     * of this XML document is described above.
     * 
     * @param xml
     *            The RequestSet XML document String.
     */
    public static RequestSet parseXML(String xml) {
        // Parse the XML document and extract the XML objects out of the
        // XML document
        RequestSetParser parser = new RequestSetParser(xml);
        return parser.parseXML();
    }

    /**
     * Gets the version of the RequestSet.
     *
     * @return The version of the request.
     */
    public String getRequestSetVersion() {
        return requestSetVersion;
    }

    /**
     * Gets the service ID of the RequestSet.
     * 
     * @return The service ID of the RequestSet.
     */
    public String getServiceID() {
        return serviceID;
    }

    /**
     * Gets the RequestSet ID for this object.
     * 
     * @return The RequestSet ID.
     */
    public String getRequestSetID() {
        return requestSetID;
    }

    /**
     * Gets the Request objects contained in this object.
     * 
     * @return A Vector of Request objects.
     */
    public List<Request> getRequests() {
        return requestSet;
    }

    /**
     * Adds a Request object to this object.
     * 
     * @param request
     *            A reference to a Request object.
     */
    public void addRequest(Request request) {
        requestSet.add(request);
    }

    /**
     * Returns an XML RequestSet document in String format. The returned String
     * is formatted based on the RequestSet DTD by aggregating each Request
     * object in this object.
     * 
     * @return An XML RequestSet document in String format.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(300);
        xml.append("<?xml version=").append(QUOTE).append("1.0").append(QUOTE)
                .append(" encoding=").append(QUOTE).append("UTF-8").append(
                        QUOTE).append(" standalone=").append(QUOTE).append(
                        "yes").append(QUOTE).append("?>").append(NL);

        xml.append("<RequestSet vers=").append(QUOTE).append(requestSetVersion)
                .append(QUOTE).append(" svcid=").append(QUOTE)
                .append(serviceID).append(QUOTE).append(" reqid=")
                .append(QUOTE).append(requestSetID).append(QUOTE).append('>')
                .append(NL);

        for (Request req : requestSet) {
            xml.append("<Request");
            if (req.getDtdID() != null) {
                xml.append(" dtdid=").append(QUOTE).append(req.getDtdID())
                        .append(QUOTE);
            }
            if (req.getSessionID() != null) {
                xml.append(" sid=").append(QUOTE).append(req.getSessionID())
                        .append(QUOTE);
            }
            xml.append('>');
            xml.append(BEGIN_CDATA).append(req.getContent()).append(END_CDATA);
            xml.append("</Request>").append(NL);
        }
        xml.append("</RequestSet>");
        return (xml.toString());
    }

    /*
     * The following methods are used by the RequestSetParser to reconstruct a
     * RequestSet object.
     */
    void setRequestSetVersion(String vers) {
        requestSetVersion = vers;
    }

    void setServiceID(String id) {
        serviceID = id;
    }

    void setRequestSetID(String id) {
        requestSetID = id;
    }
}
