/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: XACMLQueryUtil.java,v 1.1 2009/09/22 22:50:14 madan_ranganath Exp $
 *
 */

/*
 * Portions copyright 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml2.profile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.context.Subject;

/**
 * This class provides methods to send or process <code>AttributeQuery</code>.
 *
 * @supported.api
 */

public class XACMLQueryUtil {

    static SessionProvider sessionProvider = null;

    static {
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            SAML2Utils.debug.error("Error retrieving session provider.", se);
        }
    }

    private XACMLQueryUtil() {
    }

    /**
     * Sends the XACML query to specifiied PDP, gets the policy decision
     * and sends it back to the Fedlet
     *
     * @param request HTTP Servlet Request
     * @param pepEntityID PEP entity ID
     * @param pdpEntityID PDP entity ID
     * @param nameIDValue  NameID value 
     * @param serviceName  Service Name
     * @param resource  Resource URL
     * @param action  Action
     *
     * @return the <code>String</code> object
     * @exception SAML2Exception if the operation is not successful
     *
     * @supported.api
     */

    public static String getPolicyDecisionForFedlet(HttpServletRequest request,
                                                    String pepEntityID,
                                                    String pdpEntityID,
                                                    String nameIDValue,
                                                    String serviceName,
                                                    String resource,
                                                    String action)
                                                    throws SAML2Exception {
        Request Xrequest = ContextFactory.getInstance().createRequest();
        Response xacmlResponse=null;

        try {            
            //Subject
            Subject subject = ContextFactory.getInstance().createSubject();
            subject.setSubjectCategory(new URI(XACMLConstants.ACCESS_SUBJECT));

	        //set subject id
            Attribute attribute = ContextFactory.getInstance().createAttribute();
            attribute.setAttributeId(new URI(XACMLConstants.SUBJECT_ID));
            attribute.setDataType(new URI(XACMLConstants.SAML2_NAMEID));
            List valueList = new ArrayList();
            valueList.add(nameIDValue);
            attribute.setAttributeStringValues(valueList);
            List attributeList = new ArrayList();
            attributeList.add(attribute);
            subject.setAttributes(attributeList);

            // Set Subject in Request
            List subjectList = new ArrayList();
            subjectList.add(subject);
            Xrequest.setSubjects(subjectList);

            // Resource
            Resource xacml_resource =
                                 ContextFactory.getInstance().createResource();

            // Set resource id
            attribute = ContextFactory.getInstance().createAttribute();
            attribute.setAttributeId(new URI(XACMLConstants.RESOURCE_ID));
            attribute.setDataType( new URI(XACMLConstants.XS_STRING));
            valueList = new ArrayList();
            valueList.add(resource);
            attribute.setAttributeStringValues(valueList);
            attributeList = new ArrayList();
            attributeList.add(attribute);

            // Set serviceName
            attribute = ContextFactory.getInstance().createAttribute();
            attribute.setAttributeId(new URI(XACMLConstants.TARGET_SERVICE));
            attribute.setDataType(new URI(XACMLConstants.XS_STRING));
            valueList = new ArrayList();
            valueList.add(serviceName);
            attribute.setAttributeStringValues(valueList);
            attributeList.add(attribute);
            xacml_resource.setAttributes(attributeList);

            // Set Resource in Request
            List resourceList = new ArrayList();
            resourceList.add(xacml_resource);
            Xrequest.setResources(resourceList);

            // Action
            Action xacml_action = ContextFactory.getInstance().createAction();
            attribute = ContextFactory.getInstance().createAttribute();
            attribute.setAttributeId(new URI(XACMLConstants.ACTION_ID));
            attribute.setDataType(new URI(XACMLConstants.XS_STRING));

            // Set actionID
            valueList = new ArrayList();
            valueList.add(action);
            attribute.setAttributeStringValues(valueList);
            attributeList = new ArrayList();
            attributeList.add(attribute);
            xacml_action.setAttributes(attributeList);

            // Set Action in Request
            Xrequest.setAction(xacml_action);

            Environment environment =
                    ContextFactory.getInstance().createEnvironment();
            Xrequest.setEnvironment(environment);

            xacmlResponse =
                    XACMLRequestProcessor.getInstance().processRequest(
                                         Xrequest, pdpEntityID, pepEntityID);
            if (xacmlResponse != null) {
                List results = xacmlResponse.getResults();
                if (results.size() > 0) {
                    Result policy_result = (Result)results.get(0);
                    if (policy_result != null) {
                        Decision decision =
                                (Decision)policy_result.getDecision();
                        if (decision != null) {
                            String policy_decision = decision.getValue();
                            if (policy_decision != null) {
                                return policy_decision;
                            }
                        }
                    }
                }
            }
        } catch (URISyntaxException uriexp){
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("XACMLQueryUtil." +
                   "getPolicyDecisionForFedlet: " +
                   "URI Exception while sending the XACML Request");
            }
        } catch (XACMLException xacmlexp){
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("XACMLQueryUtil." +
                   "getPolicyDecisionForFedlet: " +
                   "Error while processing the XACML Response");
            }
        }
        return null;
    }
}


