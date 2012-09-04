/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyService.java,v 1.5 2008/06/25 05:43:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This <code>PolicyService</code> class repolicyResents a PolicyService
 * XML document. The PolicyService DTD is defined as the following:
 * <p>
 * <pre>
 *   <!-- PolicyService element is the root element for remote policy
 *    service. This XML will be typically used between the client
 *    and the server. The client uses the PolicyRequest element to
 *    request information from the server and the server would respond
 *    using the PolicyResponse element. The server would use
 *    PolicyNotification to send policy or subject change notification
 *    to the client.
 *   -->
 *
 *   <!ELEMENT    PolicyService    ( PolicyRequest
 *                                   | PolicyResponse
 *                                   | PolicyNotification ) >
 *   <!ATTLIST    PolicyService
 *       version    CDATA    "1.0"
 *   >
 * </pre>
 */

public class PolicyService {
    static final String POLICY_SERVICE_ROOT = "PolicyService";
    static final String POLICY_REQUEST = "PolicyRequest";
    static final String POLICY_RESPONSE = "PolicyResponse";
    static final String POLICY_NOTIFICATION = "PolicyNotification";
    static final String POLICYSERVICE_VERSION = "version";
    static final String POLICYSERVICE_REVISION = "revisionNumber";
    static final String CRLF = "\r\n";

    /**
     * Policy Service name.
     */
    public static final String POLICY_SERVICE = "policy";

    /**
     * Policy Request ID.
     */
    public static final int POLICY_REQUEST_ID = 1;

    /**
     * Policy Response ID.
     */
    public static final int POLICY_RESPONSE_ID = 2;

    /**
     * Policy Notification ID.
     */
    public static final int POLICY_NOTIFICATION_ID = 3;

    static Debug debug = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    /**
     * implies server was not able to get the revision number from
     *  service schema
     */
    static final String ON_ERROR_REVISION_NUMBER = "0";

    private String version = "1.0";
    private int methodID = 0;

    private PolicyRequest policyReq;
    private PolicyResponse policyRes;
    private PolicyNotification policyNotification;
    private String revision;

    /**
     * Default Constructor for <code>PolicyService</code>.
     */
    public PolicyService() {
    }

    /**
     * Returns the service revision number of the Policy Service.
     *
     * @return the service revision number of the Policy Service.
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Sets the service revision number for the Policy Service.
     *
     * @param revision the revision
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * Returns the version number of the Policy Service.
     *
     * @return the version of the Policy Service.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version for the Policy Service.
     *
     * @param ver the version.
     */
    public void setVersion(String ver) {
        version = ver;
    }

    /**
     * Returns the method id of the Policy Service.
     *
     * @return the method id of the Policy Service.
     */
    public int getMethodID() {
        return methodID;
    }

    /**
     * Sets the method id of the Policy Service.
     *
     * @param id the method id of this Policy Service
     */
    public void setMethodID(int id) {
        methodID = id;
    }

    /**
     * Returns the <code>PolicyRequest</code> object.
     *
     * @return the <code>PolicyRequest</code> object.
     */
    public PolicyRequest getPolicyRequest() {
        return policyReq;
    }

    /**
     * Sets the <code>PolicyRequest</code> of the Policy Service.
     *
     * @param req the <code>PolicyRequest</code> of this Policy Service
     */
    public void setPolicyRequest(PolicyRequest req) {
        policyReq = req;
    }

    /**
     * Returns the <code>PolicyResponse</code> object.
     *
     * @return the <code>PolicyResponse</code> object.
     */
    public PolicyResponse getPolicyResponse() {
        return policyRes;
    }

    /**
     * Sets the <code>PolicyResponse</code> of the Policy Service.
     *
     * @param res the <code>PolicyResponse</code> of this Policy Service.
     */
    public void setPolicyResponse(PolicyResponse res) {
        policyRes = res;
    }

    /**
     * Returns the <code>PolicyNotification</code> object.
     *
     * @return the PolicyNotification object
     */
    public PolicyNotification getPolicyNotification() {
        return policyNotification;
    }

    /**
     * Sets the <code>PolicyNotification</code> of the Policy Service.
     *
     * @param noti The <code>PolicyNotification</code> of this Policy Service.
     */
    public void setPolicyNotification(PolicyNotification noti)
    {
        policyNotification = noti;
    }

    /**
     * Returns <code>PolicyService</code> object constructed from XML.
     *
     * @param xml the XML document for the <code>PolicyService</code> object.
     * @return constructed <code>PolicyService</code> object.
     */
    public static PolicyService parseXML(String xml)
        throws PolicyEvaluationException
    {
        Document doc = null;

        try {
            doc = XMLUtils.getXMLDocument(
                     new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (Exception xe) {
            debug.error("PolicyService.parseXML(String): XML parsing error");
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "xml_parsing_error", null, xe);
        }

        PolicyService ps = new PolicyService();
        
        Node rootNode = XMLUtils.getRootNode(doc, POLICY_SERVICE_ROOT);
        if (rootNode == null) {
            debug.error("PolicyServiceparseXML(String): " +
                "invalid root element specified in the request");
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "invalid_root_element", null, null);
        }

        String ver = XMLUtils.getNodeAttributeValue(
            rootNode, POLICYSERVICE_VERSION);
        if (ver != null) {
            ps.setVersion(ver);
        }

        String rev = XMLUtils.getNodeAttributeValue(
            rootNode, POLICYSERVICE_REVISION);
        if (rev != null) {
            ps.setRevision(rev);
        }

        // Check if this is a policy request.
        Node node = XMLUtils.getChildNode(rootNode, POLICY_REQUEST);
        if (node != null) {
            PolicyRequest preq = PolicyRequest.parseXML(node);
            ps.setPolicyRequest(preq); 
            ps.setMethodID(POLICY_REQUEST_ID);
            return ps;
        }
  
        // Check if this is a policy response.
        node = XMLUtils.getChildNode(rootNode, POLICY_RESPONSE);
        if (node != null) {
            PolicyResponse pres = PolicyResponse.parseXML(node);
            ps.setPolicyResponse(pres);
            ps.setMethodID(POLICY_RESPONSE_ID);
            return ps;
        }

        // Check if this is a policy notification to the client.
        node = XMLUtils.getChildNode(rootNode, POLICY_NOTIFICATION);
        if (node != null) {
            PolicyNotification pn = PolicyNotification.parseXML(node);
            ps.setPolicyNotification(pn);
            ps.setMethodID(POLICY_NOTIFICATION_ID);
            return ps;
        }

        /*
         * We reach here, there is no valid method name specified in
         * the xml docuemnt. Throw exception.
         */
        debug.error("PolicyService: invalid method specified");
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "invalid_policy_service_method", null, null);
    }
                    

    /**
     * Returns string representation of this object.
     *
     * @return string representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(1000);

        xmlsb.append("<")
             .append(POLICY_SERVICE_ROOT)
             .append(" ")
             .append(POLICYSERVICE_VERSION)
             .append("=")
             .append("\"")
             .append(version)
             .append("\"");

        if ((revision != null) && (revision.trim().length() != 0) ) {
            xmlsb.append(" ")
                 .append(POLICYSERVICE_REVISION)
                 .append("=")
                 .append("\"")
                 .append(revision)
                 .append("\"");
        }
        xmlsb.append(">" + CRLF);

        switch(methodID) {
            case POLICY_REQUEST_ID:
                if (policyReq != null) {
                    xmlsb.append(policyReq.toXMLString());
                }
                break;
            case POLICY_RESPONSE_ID:
                if (policyRes != null) {
                    xmlsb.append(policyRes.toXMLString());
                }
                break;
            case POLICY_NOTIFICATION_ID:
                if (policyNotification != null) {
                    xmlsb.append(policyNotification.toXMLString());
                }
                break;
            default:
                break;
        }

        xmlsb.append("</")
             .append(POLICY_SERVICE_ROOT)
             .append(">")
             .append(CRLF);
        return xmlsb.toString(); 
    }

} 
