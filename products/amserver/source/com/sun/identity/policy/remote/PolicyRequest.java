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
 * $Id: PolicyRequest.java,v 1.5 2008/08/19 19:09:19 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import org.w3c.dom.Node;

/**
 * This <code>PolicyRequest</code> class represents a PolicyRequest
 * XML document. The PolicyRequest DTD is defined as the following:
 * <p>
 * <pre>
 *  <!-- PolicyRequest element is used by the client to request the
 *    policy evaluation decisions or to add/remove a policy listener.
 *    The attribute appSSOToken provides the SSO token of the client
 *    as its identity that can be used by the server to check if the
 *    client can receive the requested information.
 *    The attribute requestId specifies the id of the request.
 *  -->
 *
 *  <!ELEMENT    PolicyRequest    ( GetResourceResults
 *                                | AddPolicyListener
 *                                | RemovePolicyListener
 *                                | AdvicesHandleableByAMRequest ) >
 *  <!ATTLIST    PolicyRequest
 *      appSSOToken    CDATA        #REQUIRED
 *      requestId      CDATA        #REQUIRED
 *  >
 * </pre>
 * <p>
 */
public class PolicyRequest {
    /**
     * Policy Request - Get Resource Results Id.
     */
    public static final int POLICY_REQUEST_GET_RESOURCE_RESULTS = 1;

    /**
     * Policy Request - Add Policy Listener Id.
     */
    public static final int POLICY_REQUEST_ADD_POLICY_LISTENER = 2;

    /**
     * Policy Request - Remove Policy Listener Id.
     */
    public static final int POLICY_REQUEST_REMOVE_POLICY_LISTENER = 3;

    /**
     * Policy Request - Advices handleable by OpenSSO Id.
     */
    public static final int POLICY_REQUEST_ADVICES_HANDLEABLE_BY_AM_REQUEST = 4;

    static final String POLICY_REQUEST = PolicyService.POLICY_REQUEST;
    static final String GET_RESOURCE_RESULTS = "GetResourceResults";
    static final String ADD_POLICY_LISTENER = "AddPolicyListener";
    static final String REMOVE_POLICY_LISTENER = "RemovePolicyListener";
    static final String ADVICES_HANDLEABLE_BY_AM_REQUEST 
            = "AdvicesHandleableByAMRequest";
    static final String APP_SSOTOKEN = "appSSOToken";
    static final String REQUEST_ID = "requestId";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug;

    private ResourceResultRequest resourceResultReq = null;
    private PolicyListenerRequest policyListenerReq = null;
    private RemoveListenerRequest removeListenerReq = null;
    private AdvicesHandleableByAMRequest advicesHandleableByAMRequest = null;
    private String appSSOToken = null;
    private String requestId = null;
    private int methodID = 0; 


    /** 
     * Default constructor for <code>PolicyRequest</code>.
     */
    public PolicyRequest() {
    }

    /**
     * Returns the method Id of the Policy Request.
     *
     * @return the method Id.
     */
    public int getMethodID() {
        return methodID;
    }

    /**
     * Sets the method Id of the Policy Request.
     *
     * @param id the method Id.
     */
    public void setMethodID(int id) {
        methodID = id;
    }

    /**
     * Returns the single sign on token of the application who sends the
     * request.
     *
     * @return the single sign on token of the application.
     */
    public String getAppSSOToken() {
        return appSSOToken;
    }

    /**
     * Sets the single sign on token of the application who sends the request.
     *
     * @param ssoToken the single sign on token of the application.
     */
    public void setAppSSOToken(String ssoToken) {
        appSSOToken = ssoToken;
    }

    /**
     * Returns the request Id.
     *
     * @return the request Id.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request Id.
     *
     * @param reqId the Id of the request.
     */
    public void setRequestId(String reqId) {
        requestId = reqId;
    }

    /**
     * Returns the resource result request object.
     *
     * @return the resource result request.
     */
    public ResourceResultRequest getResourceResultRequest() {
        return resourceResultReq;
    }

    /**
     * Sets the resource result request object.
     *
     * @param req the resource result request.
     */
    public void setResourceResultRequest(ResourceResultRequest req) {
        resourceResultReq = req;
    }

    /**
     * Returns the <code>PolicyListenerRequest</code> object.
     *
     * @return the <code>PolicyListenerRequest</code> object.
     */
    public PolicyListenerRequest getPolicyListenerRequest() {
        return policyListenerReq;
    }

    /**
     * Sets the <code>PolicyListenerRequest</code> of the Policy Service.
     *
     * @param req the <code>PolicyListenerRequest</code> of this Policy Service
     */
    public void setPolicyListenerRequest(PolicyListenerRequest req) {
        policyListenerReq = req;
    }

    /**
     * Returns the <code>RemoveListenerRequest</code> object.
     *
     * @return the <code>RemoveListenerRequest</code> object
     */
    public RemoveListenerRequest getRemoveListenerRequest() {
        return removeListenerReq;
    }

    /**
     * Sets the <code>RemoveListenerRequest</code> of the Policy Service.
     *
     * @param req the <code>RemoveListenerRequest</code> of this Policy Service
     */
    public void setRemoveListenerRequest(RemoveListenerRequest req) {
        removeListenerReq = req;
    }

    /**
     * Sets the <code>AdvicesHandleableByAMRequest</code> sub element of
     * <code>PolicyRequest</code>.
     *
     * @param advicesHandleableByAMRequest the
     *        <code>AdvicesHandleableByAMRequest</code> sub element of
     *        <code>PolicyRequest</code>.
     */
    public void setAdvicesHandleableByAMRequest(
        AdvicesHandleableByAMRequest advicesHandleableByAMRequest)
    {
        this.advicesHandleableByAMRequest = advicesHandleableByAMRequest;
    }

    /**
     * Returns a <code>PolicyRequest</code> object constructed from a XML.
     *
     * @param pNode the XML DOM node for the <code>PolicyRequest</code> object.
     * @return constructed <code>PolicyRequest</code> object
     */
    public static PolicyRequest parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        PolicyRequest preq = new PolicyRequest();

        String attr = XMLUtils.getNodeAttributeValue(pNode, APP_SSOTOKEN);
        if (attr == null) {
            debug.error("PolicyRequestparseXML(Node): missing attribute " +
                APP_SSOTOKEN);
            String objs[] = { APP_SSOTOKEN };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        preq.setAppSSOToken(attr);
        attr = XMLUtils.getNodeAttributeValue(pNode, REQUEST_ID);

        if (attr == null) {
            debug.error("PolicyRequest.parseXML(Node): missing attribute " +
                REQUEST_ID);
            String objs[] = { REQUEST_ID };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        preq.setRequestId(attr);
        
        Node node = XMLUtils.getChildNode(pNode, GET_RESOURCE_RESULTS);
        if (node != null) {
            ResourceResultRequest resourceResultReq = null;
            try {
                resourceResultReq = ResourceResultRequest.parseXML(node);
            } catch (PolicyEvaluationException pe) {
                throw new PolicyEvaluationException(pe, preq.getRequestId());
            }
            preq.setResourceResultRequest(resourceResultReq);
            preq.setMethodID(POLICY_REQUEST_GET_RESOURCE_RESULTS);
            return preq; 
        }

        node = XMLUtils.getChildNode(pNode, ADD_POLICY_LISTENER);
        if (node != null) {
            PolicyListenerRequest plr = null;
            try {
                plr = PolicyListenerRequest.parseXML(node);
            } catch (PolicyEvaluationException pe) {
                throw new PolicyEvaluationException(pe, preq.getRequestId());
            }
            preq.setPolicyListenerRequest(plr);
            preq.setMethodID(POLICY_REQUEST_ADD_POLICY_LISTENER);
            return preq;
        }

        node = XMLUtils.getChildNode(pNode, REMOVE_POLICY_LISTENER);
        if (node != null) {
            RemoveListenerRequest rmListenerReq = null;
            try {
                rmListenerReq = RemoveListenerRequest.parseXML(node);
            } catch (PolicyEvaluationException pe) {
                throw new PolicyEvaluationException(pe, preq.getRequestId());
            }
            preq.setRemoveListenerRequest(rmListenerReq);
            preq.setMethodID(POLICY_REQUEST_REMOVE_POLICY_LISTENER);
            return preq;
        }

        node = XMLUtils.getChildNode(pNode, ADVICES_HANDLEABLE_BY_AM_REQUEST);
        if (node != null) {
            preq.setAdvicesHandleableByAMRequest(
                    new AdvicesHandleableByAMRequest());
            preq.setMethodID(POLICY_REQUEST_ADVICES_HANDLEABLE_BY_AM_REQUEST);
            return preq;
        }

        /*
         * We reach here, there is no valid method name specified in
         * the xml docuemnt. Throw exception.
         */
        debug.error("PolicyRequest: invalid method specified");
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "invalid_policy_request_method", null, null);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(1000);
        xmlsb.append("<")
             .append(POLICY_REQUEST)
             .append(" ")
             .append(APP_SSOTOKEN)
             .append("=\"")
             .append(appSSOToken)
             .append("\" ")
             .append(REQUEST_ID)
             .append("=\"")
             .append(requestId)
             .append("\">")
             .append(CRLF);

        if (methodID == POLICY_REQUEST_GET_RESOURCE_RESULTS) {
            xmlsb.append(resourceResultReq.toXMLString());
        } else if (methodID == POLICY_REQUEST_ADD_POLICY_LISTENER) {
            xmlsb.append(policyListenerReq.toXMLString());
        } else if (methodID == POLICY_REQUEST_REMOVE_POLICY_LISTENER) {
            xmlsb.append(removeListenerReq.toXMLString());
        } else if (methodID 
                == POLICY_REQUEST_ADVICES_HANDLEABLE_BY_AM_REQUEST) {
            xmlsb.append(advicesHandleableByAMRequest.toXMLString());
        }

        xmlsb.append("</")
             .append(POLICY_REQUEST)
             .append(">")
             .append(CRLF);
        return xmlsb.toString();
    } 
} 
