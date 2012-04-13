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
 * $Id: ResourceResultRequest.java,v 1.3 2008/06/25 05:43:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;


/**
 * This <code>ResourceResultRequest</code> class represents a
 * GetResourceResults XML document. The GetResourceResults
 * DTD is defined as the following:
 * <p>
 * <pre>
 * <!-- GetResourceResults element is used by the client to request
 *      the policy evaluation decisions for a particular user regarding
 *      a given service and a resource and possibly its sub resources.
 *      The attribute userSSOToken provides the identity of the user.
 *      The attribute serviceName specifies the service name and
 *      resourceName specifies the name of the resource for which the
 *      policy is evaluated.
 *      The attribute resourceScope gives the scope of the resources in
 *      terms of the policy evaluation. The value of the attribute could
 *      be "self", "strict-subtree", or "subtree". Value "self" means the
 *      resource node itself only; value "strict-subtree" means the strict
 *      subtree of the root, no wildcard matching subtree included; value
 *      "subtree" means to get all the subtrees with the resource node being
 *      the root of one of the subtree, the other subtree roots are those that
 *      match the resourceName by wildcard.
 *      The sub-element EnvParameters provides the environment
 *      information which may be useful during the policy evaluation.
 *      The sub-element GetResponseDecisions requests for the values for
 *      a set of user response attributes.
 * -->
 *
 * <!ELEMENT    GetResourceResults   ( EnvParameters?, GetResponseDecisions? ) >
 * <!ATTLIST    GetResourceResults
 *     userSSOToken    CDATA        #IMPLIED
 *     serviceName     NMTOKEN      #REQUIRED
 *     resourceName    CDATA        #REQUIRED
 *     resourceScope   (self | strict-subtree | subtree)  "strict-subtree"
 * >
 * </pre>
 * <p>
 */

public class ResourceResultRequest {

    static final String GET_RESOURCE_RESULTS =
        PolicyRequest.GET_RESOURCE_RESULTS;
    static final String USER_SSOTOKEN = "userSSOToken";
    static final String SERVICE_NAME = "serviceName";
    static final String RESOURCE_NAME = "resourceName";
    static final String RESOURCE_SCOPE = "resourceScope";
    static final String ENV_PARAMETERS = "EnvParameters";
    static final String GET_RESPONSE_DECISIONS = "GetResponseDecisions";
    static final String RESOURCE_SCOPE_SELF = "self";
    static final String RESOURCE_SCOPE_STRICT_SUBTREE = "strict-subtree";
    static final String RESOURCE_SCOPE_SUBTREE = "subtree";
    static final String RESPONSE_ATTRIBUTES_ONLY = "response-attributes-only";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug;

    private String userSSOToken = null;
    private String serviceName = null;
    private String resourceName = null;
    private String resourceScope = null; 
    private Map envParms = null;
    private Set respAttributes = null;
   
 
    /** 
     * Default constructor for <code>ResourceResultRequest</code>.
     */
    public ResourceResultRequest() {
    }

    /**
     * Returns the user's single sign-on token.
     *
     * @return user's single sign-on token.
     */
    public String getUserSSOToken() {
        return userSSOToken;
    }

    /**
     * Sets the user's single sign-on token.
     *
     * @param ssoToken user's single sign-on token.
     */
    public void setUserSSOToken(String ssoToken) {
        userSSOToken = ssoToken;
    }

    /**
     * Returns the service name of the request.
     *
     * @return the service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name of the request.
     *
     * @param name the service name.
     */
    public void setServiceName(String name) {
        serviceName = name;
    }

    /**
     * Returns the resource name of the request.
     *
     * @return the resource name.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets the resource name of the request.
     *
     * @param name Resource name.
     */
    public void setResourceName(String name) {
        resourceName = name;
    }

    /**
     * Returns the resource scope of the request.
     *
     * @return the resource scope.
     */
    public String getResourceScope() {
        return resourceScope;
    }

    /**
     * Sets the resource scope of the request.
     *
     * @param scope Resource scope.
     */
    public void setResourceScope(String scope) {
        resourceScope = scope;
    }

    /**
     * Returns the environment parameters.
     *
     * @return the environment parameters.
     */
    public Map getEnvParms() {
        return envParms;
    }

    /**
     * Sets the environment parameters.
     *
     * @param envs the environment parameters.
     */
    public void setEnvParms(Map envs) {
        envParms = envs;
    }

    /**
     * Returns the response attributes.
     *
     * @return the response attributes.
     */
    public Set getResponseAttributes() {
        return respAttributes;
    }

    /**
     * Sets the response attributes.
     *
     * @param attrs response attributes.
     */
    public void setResponseAttributes(Set attrs) {
        respAttributes = attrs;
    }

    /**
     * Returns <code>ResourceResultRequest</code> object constructed from a
     * XML.
     *
     * @param pNode the XML DOM node for the <code>ResourceResultRequest</code>
     *        object
     * @return constructed <code>ResourceResultRequest</code> object.
     */
    public static ResourceResultRequest parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        ResourceResultRequest resResultReq = new ResourceResultRequest();

        String attr = null;

        attr = XMLUtils.getNodeAttributeValue(pNode, USER_SSOTOKEN);
        if ((attr == null) ||
            (attr.trim().equals(PolicyUtils.EMPTY_STRING))
        ) {
            if (debug.messageEnabled()) {
                debug.error("ResourceResultRequest: user sso toekn is null"); 
            }
            attr = PolicyUtils.EMPTY_STRING;
        }
        resResultReq.setUserSSOToken(attr);

        attr = XMLUtils.getNodeAttributeValue(pNode, SERVICE_NAME);
        if (attr == null) {
            debug.error("ResourceResultRequest: missing attribute " +
                SERVICE_NAME);
            String objs[] = { SERVICE_NAME };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }

        resResultReq.setServiceName(attr);
       
        attr = XMLUtils.getNodeAttributeValue(pNode, RESOURCE_NAME);
        if (attr == null) {
            debug.error("ResourceResultRequest: missing attribute " +
                RESOURCE_NAME);
            String objs[] = { RESOURCE_NAME };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute",objs, null);
        }

        resResultReq.setResourceName(attr);

        attr = XMLUtils.getNodeAttributeValue(pNode, RESOURCE_SCOPE);
        if (attr == null) {
            /* if the resource scope is not specified in the request,
             * we take the default value RESOURCE_SCOPE_STRICT_SUBTREE
             */
            resResultReq.setResourceScope(RESOURCE_SCOPE_STRICT_SUBTREE);
        }
        else {
            if (attr.equals(RESOURCE_SCOPE_SUBTREE) 
                 || attr.equals(RESOURCE_SCOPE_STRICT_SUBTREE) 
                 || attr.equals(RESOURCE_SCOPE_SELF) 
                 || attr.equals(RESPONSE_ATTRIBUTES_ONLY)) {
                resResultReq.setResourceScope(attr);
            }
            else {
                debug.error("ResourceResultRequest: invalid value " 
                     + attr + " set for attribute " + RESOURCE_SCOPE);
                String objs[] = { attr, RESOURCE_SCOPE };
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "invalid_value_for_attribute", objs, null);
            }
        } 

        Node node = XMLUtils.getChildNode(pNode, ENV_PARAMETERS);     
        if (node != null) {
            try {
                resResultReq.setEnvParms(
                               PolicyUtils.parseEnvParameters(node));
            } catch (PolicyException pe) {
                throw new PolicyEvaluationException(pe);
            }
        }

        node = XMLUtils.getChildNode(pNode, GET_RESPONSE_DECISIONS);
        if (node != null) {
            try {
                resResultReq.setResponseAttributes(
                           PolicyUtils.parseResponseAttributes(node));
            } catch (PolicyException pe) {
                throw new PolicyEvaluationException(pe);
            }
        }

        return resResultReq;
    }

    /**
     * Returns a XML representation of this object.
     *
     * @return a XML representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(1000);

        xmlsb.append("<")
             .append(GET_RESOURCE_RESULTS);
        xmlsb.append(" ")
             .append(USER_SSOTOKEN)
             .append("=\"")
             .append((userSSOToken != null)
                ? userSSOToken : PolicyUtils.EMPTY_STRING)
             .append("\"");

        xmlsb.append(" ")
             .append(SERVICE_NAME)
             .append("=\"")
             .append(serviceName)
             .append("\"");
        xmlsb.append(" ")
             .append(RESOURCE_NAME)
             .append("=\"")
             .append(XMLUtils.escapeSpecialCharacters(resourceName))
             .append("\"");
        xmlsb.append(" ")
             .append(RESOURCE_SCOPE)
             .append("=\"")
             .append(resourceScope)
             .append("\">")
             .append(CRLF);

        if (envParms != null) {
            xmlsb.append(PolicyUtils.envParametersToXMLString(envParms));
        } 

        if (respAttributes != null) {
            xmlsb.append(PolicyUtils.responseAttributesToXMLString(
                respAttributes));
        } 

        xmlsb.append("</")
             .append(GET_RESOURCE_RESULTS)
             .append(">")
             .append(CRLF);
        return xmlsb.toString();                
    }
} 
