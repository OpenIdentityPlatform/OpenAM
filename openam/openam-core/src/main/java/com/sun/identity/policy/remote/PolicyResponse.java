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
 * $Id: PolicyResponse.java,v 1.8 2008/12/04 00:38:52 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ResourceResult;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Node;

/**
 * This <code>PolicyResponse</code> represents a PolicyResponse
 * XML document. The PolicyResponse DTD is defined as the following:
 * <p>
 * <pre>
 *   <!-- PolicyResponse element is used by the server to return a
 *    response to the client.
 *    If the client request is a policy evaluation request, then the
 *    policy decision is sent back using ResourceResult.
 *    If the client request is a policy listener addition, then an
 *    acknowledgement is sent back using AddPolicyListenerResponse.
 *    If the client request is a policy listener removal, then an
 *    acknowledgement is sent back using RemovePolicyListenerResponse.
 *    If anything wrong happens during the request processing, an
 *    error message is sent back to the client using Exception.
 *    The attribute requestId specifies the id of the request to
 *    which this response is regarding.
 *   -->
 *
 *   <!ELEMENT    PolicyResponse    ( ResourceResult+
 *                                    | AddPolicyListenerResponse
 *                                    | RemovePolicyListenerResponse
 *                                    | AdvicesHandleableByAMResponse
 *                                    | Exception ) >
 *   <!ATTLIST    PolicyResponse
 *       requestId      CDATA        #REQUIRED
 *   >
 * </pre>
 * <p>
 */
public class PolicyResponse {
    /**
     * Policy Response Resource Result Id.
     */
    public static final int POLICY_RESPONSE_RESOURCE_RESULT = 1;

    /**
     * Add Policy Listener Resource Id.
     */
    public static final int POLICY_ADD_LISTENER_RESPONSE = 2;

    /**
     * Remove Policy Listener Resource Id.
     */
    public static final int POLICY_REMOVE_LISTENER_RESPONSE = 3;

    /**
     * Policy Exception Id.
     */
    public static final int POLICY_EXCEPTION = 4;

    /**
     * Policy Advices Handleable by OpenSSO Response Id.
     */
    public static final int POLICY_ADVICES_HANDLEABLE_BY_AM_RESPONSE = 5;

    /**
     * Exception message if Application SSO Token is ivalid
     */
    public static final String APP_SSO_TOKEN_INVALID 
            = "Application sso token is invalid";

    static final String POLICY_RESPONSE = PolicyService.POLICY_RESPONSE;
    static final String REQUEST_ID = "requestId";
    static final String ISSUE_INSTANT = "issueInstant";
    static final String RESOURCE_RESULT = "ResourceResult";
    static final String ADD_LISTENER_RESPONSE = "AddPolicyListenerResponse";
    static final String REMOVE_LISTENER_RESPONSE =
        "RemovePolicyListenerResponse";
    static final String ADVICES_HANDLEABLE_BY_AM_RESPONSE 
            = "AdvicesHandleableByAMResponse";
    static final String EXCEPTION_RESPONSE = "Exception";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug;
    
    private int methodID = 0;
    private long issueInstant = 0;
    private String requestId = null;
    private Set resourceResults = null;
    private AdvicesHandleableByAMResponse advicesHandleableByAMResponse = null;
    private String exceptionMsg = null;

    /**
     * Default constructor for <code>PolicyResponse</code>.
     */
    public PolicyResponse() {
    }

    /**
     * Returns the method Id of the Policy Response.
     *
     * @return the method Id.
     */
    public int getMethodID() {
        return methodID;
    }

    /**
     * Sets the method Id of the Policy Response.
     *
     * @param id the method Id.
     */
    public void setMethodID(int id) {
        methodID = id;
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
     * Returns the issue instant
     *
     * @return the issue instant
     */
    public long getIssueInstant() {
        return issueInstant;
    }

    /**
     * Sets the issue instant
     *
     * @param issueInst issue instant
     */
    public void setIssueInstant(long issueInst) {
        issueInstant = issueInst;
    }

    /**
     * Returns the set of the sub resource results.
     *
     * @return the sub resource result set.
     */
    public Set getResourceResults() {
        return resourceResults;
    }

    /**
     * Sets the set of the sub resource results.
     *
     * @param set the sub resource result set.
     */
    public void setResourceResults(Set set) {
        resourceResults = set;
    }

    /**
     * Returns the <code>AdvicesHandleableByAMResponse</code> object.
     *
     * @return <code>AdvicesHandleableByAMResponse</code> object.
     */
    public AdvicesHandleableByAMResponse getAdvicesHandleableByAMResponse() {
        return advicesHandleableByAMResponse;
    }

    /**
     * Set the <code>AdvicesHandleableByAMResponse</code>.
     *
     * @param advicesHandleableByAMResponse the
     *        <code>AdvicesHandleableByAMResponse</code>.
     */
    public void setAdvicesHandleableByAMResponse(AdvicesHandleableByAMResponse 
            advicesHandleableByAMResponse)
    {
        this.advicesHandleableByAMResponse = advicesHandleableByAMResponse;
    }

    /**
     * Returns the exception response.
     *
     * @return the exception response.
     */
    public String getExceptionMsg() {
        return exceptionMsg;
    }

    /**
     * Sets the exception response.
     *
     * @param exMsg the exception response.
     */
    public void setExceptionMsg(String exMsg) {
        exceptionMsg = exMsg;
    }

    /**
     * Returns <code>PolicyResponse</code> object constructed from XML.
     *
     * @param pNode the XML DOM node for the <code>PolicyResponse</code> object.
     * @return constructed <code>PolicyResponse</code> object.
     */
    public static PolicyResponse parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        PolicyResponse pres = new PolicyResponse();

        Node node = null;
        String attr = XMLUtils.getNodeAttributeValue(pNode, REQUEST_ID);
        if (attr == null) {
            debug.error("PolicyResponse: missing attribute " + REQUEST_ID);
            String objs[] = { REQUEST_ID };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        pres.setRequestId(attr);

        String issueInst = XMLUtils.getNodeAttributeValue(pNode, ISSUE_INSTANT);
        if ((issueInst != null) && (issueInst.length() != 0)) {
            try {
                pres.setIssueInstant(Long.parseLong(issueInst));
            } catch(NumberFormatException nfe) {
                //This should never happen 
                if (debug.warningEnabled()) {
                    debug.message("PolicyResponse: invald value for attribute:" 
                            + ISSUE_INSTANT + ":" + issueInst);
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("PolicyResponse: missing attribute: " 
                        + ISSUE_INSTANT);
            }
        }

        Set nodeSet = XMLUtils.getChildNodes(pNode, RESOURCE_RESULT);
        if ((nodeSet != null) && (!nodeSet.isEmpty())) {
            Set resResults = new HashSet();
            Iterator nodes = nodeSet.iterator();
            while (nodes.hasNext()) {
                node = (Node)nodes.next();
                ResourceResult rRes = null;
                try {
                    rRes = ResourceResult.parseResourceResult(node);
                } catch (Exception e) {
                    debug.error("PolicyResponse: XML parsing error");
                    throw new PolicyEvaluationException(
                        ResBundleUtils.rbName, "xml_parsing_error", null, e);
                }
                resResults.add(rRes); 
            }
            pres.setResourceResults(resResults);
            pres.setMethodID(POLICY_RESPONSE_RESOURCE_RESULT); 
    
            return pres;
        }

        node = XMLUtils.getChildNode(pNode, ADD_LISTENER_RESPONSE);
        if (node != null) {
            pres.setMethodID(POLICY_ADD_LISTENER_RESPONSE);
            return pres;
        }

        node = XMLUtils.getChildNode(pNode, REMOVE_LISTENER_RESPONSE);
        if (node != null) {
            pres.setMethodID(POLICY_REMOVE_LISTENER_RESPONSE);
            return pres;
        }

        node = XMLUtils.getChildNode(pNode, ADVICES_HANDLEABLE_BY_AM_RESPONSE);
        if (node != null) { 
            pres.setAdvicesHandleableByAMResponse(AdvicesHandleableByAMResponse
                    .parseXML(node));
            pres.setMethodID(POLICY_ADVICES_HANDLEABLE_BY_AM_RESPONSE);
            return pres;
        }

        node = XMLUtils.getChildNode(pNode, EXCEPTION_RESPONSE);
        if (node != null) {
            String eMsg = XMLUtils.getValueOfValueNode(node);
            pres.setExceptionMsg(eMsg);
            pres.setMethodID(POLICY_EXCEPTION);

            return pres;
        }

        /* We reach here, there is no valid method name specified in
           the xml docuemnt. Throw exception.
         */
        debug.error("PolicyResponse: invalid method specified");
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "invalid_policy_response_method", null, null);
    }

    /**
     * Returns string representation of this object.
     *
     * @return string representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(1000);

        xmlsb.append("<")
             .append(POLICY_RESPONSE)
             .append(" ")
             .append(REQUEST_ID)
             .append("=\"").append(requestId).append("\" ");
        if (issueInstant != 0) {
            xmlsb.append(ISSUE_INSTANT)
                .append("=\"") .append(issueInstant).append("\" ");
        }
        xmlsb.append(">")
            .append(CRLF);

        if (methodID == POLICY_RESPONSE_RESOURCE_RESULT) { 
            Iterator itr = resourceResults.iterator();
            while (itr.hasNext()) {
                ResourceResult rRes = (ResourceResult)itr.next();
                xmlsb.append(rRes.toXML());
            }
        } else if (methodID == POLICY_ADD_LISTENER_RESPONSE) {
            xmlsb.append("<")
                 .append(ADD_LISTENER_RESPONSE)
                 .append("/>")
                 .append(CRLF);
        } else if (methodID == POLICY_REMOVE_LISTENER_RESPONSE) {
            xmlsb.append("<")
                 .append(REMOVE_LISTENER_RESPONSE)
                 .append("/>")
                 .append(CRLF);
        } else if (methodID == POLICY_ADVICES_HANDLEABLE_BY_AM_RESPONSE) {
              xmlsb.append(advicesHandleableByAMResponse.toXMLString());
        } else if (methodID == POLICY_EXCEPTION) {
            xmlsb.append("<")
                 .append(EXCEPTION_RESPONSE)
                 .append(">")
                 .append(CRLF);
            xmlsb.append(exceptionMsg).append(CRLF);
            xmlsb.append("</")
                 .append(EXCEPTION_RESPONSE)
                 .append(">")
                 .append(CRLF);
        }

        xmlsb.append("</")
             .append(POLICY_RESPONSE)
             .append(">")
             .append(CRLF);
        return xmlsb.toString();
    }
}

 
