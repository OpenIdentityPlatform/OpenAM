/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: XACMLAuthzDecisionQueryHandler.java,v 1.6 2008/06/25 05:50:16 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.xacml.plugins;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResourceResult;

import com.sun.identity.policy.PolicyEvaluator;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.soapbinding.RequestHandler;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionStatement;

import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ProtocolFactory;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.context.StatusDetail;

import com.sun.identity.xacml.spi.ActionMapper;
import com.sun.identity.xacml.spi.EnvironmentMapper;
import com.sun.identity.xacml.spi.ResourceMapper;
import com.sun.identity.xacml.spi.ResultMapper;
import com.sun.identity.xacml.spi.SubjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is an implementation of SAML2 query RequestHandler to handle
 * XACMLAuthzDecisionQuery
 * 
 */
public class XACMLAuthzDecisionQueryHandler implements RequestHandler {

    /**
     * This class is an implementation of SAML2 query RequestHandler to handle
     * XACMLAuthzDecisionQuery
     * 
     */
    public XACMLAuthzDecisionQueryHandler() {
    }
    
    /**
     * Processes an XACMLAuthzDecisionQuery and retruns a SAML2 Response.
     *
     * @param pdpEntityId EntityID of PDP
     * @param pepEntityId EntityID of PEP
     * @param samlpRequest SAML2 Request, an XAMLAuthzDecisionQuery
     * @param soapMessage SOAPMessage that carried the SAML2 Request
     * @return SAML2 Response with an XAMLAuthzDecisionStatement
     * @exception SAML2Exception if the query can not be handled
     */
    public com.sun.identity.saml2.protocol.Response handleQuery(
            String pdpEntityId, String pepEntityId, 
            RequestAbstract samlpRequest, SOAPMessage soapMessage) 
            throws SAML2Exception {

        //TODO: logging, i18n
        //TODO: long term, allow different mapper impls for  different
        //combination of pdp, pep

        SubjectMapper subjectMapper = new FMSubjectMapper();
        subjectMapper.initialize(pdpEntityId, pepEntityId, null);
        ResourceMapper resourceMapper = new FMResourceMapper();
        resourceMapper.initialize(pdpEntityId, pepEntityId, null);
        ActionMapper actionMapper = new FMActionMapper();
        actionMapper.initialize(pdpEntityId, pepEntityId, null);
        EnvironmentMapper environmentMapper = new FMEnvironmentMapper();
        environmentMapper.initialize(pdpEntityId, pepEntityId, null);
        ResultMapper resultMapper = new FMResultMapper();
        resultMapper.initialize(pdpEntityId, pepEntityId, null);

        boolean evaluationFailed = false;
        String statusCodeValue = null;

        if (XACMLSDKUtils.debug.messageEnabled()) {
            XACMLSDKUtils.debug.message(
                    "XACMLAuthzDecisionQueryHandler.handleQuery(), entering"
                    + ":pdpEntityId=" + pdpEntityId
                    + ":pepEntityId=" + pepEntityId
                    + ":samlpRequest=\n" + samlpRequest.toXMLString(
                        true, true)
                    + ":soapMessage=\n" + soapMessage);
        }

        Request xacmlRequest 
                = ((XACMLAuthzDecisionQuery)samlpRequest).getRequest();
        boolean returnContext 
                = ((XACMLAuthzDecisionQuery)samlpRequest).getReturnContext();

        SSOToken ssoToken = null;
        String resourceName = null;
        String serviceName = null;
        String actionName = null;
        Map environment = null;
        boolean booleanDecision = false;

        try {
            //get native sso token
            ssoToken =
                    (SSOToken)subjectMapper.mapToNativeSubject(
                    xacmlRequest.getSubjects()); 

            if (ssoToken == null) {
                //TODO: log message and fill missing attribute details 
                statusCodeValue = XACMLConstants.STATUS_CODE_MISSING_ATTRIBUTE;
                evaluationFailed = true;
            } else {
                if (XACMLSDKUtils.debug.messageEnabled()) {
                    XACMLSDKUtils.debug.message(
                            "XACMLAuthzDecisionQueryHandler.handleQuery(),"
                            + "created ssoToken");
                }
            }

            if (ssoToken != null) {
                //get native service name, resource name 
                List resources = xacmlRequest.getResources();
                Resource resource = null;
                if (!resources.isEmpty()) {
                    //We deal with only one resource for now
                    resource = (Resource)resources.get(0);
                }


                if (resource != null) {
                    String[] resourceService 
                            = resourceMapper.mapToNativeResource(resource);
                    if (resourceService != null) {
                        if (resourceService.length > 0) {
                            resourceName = resourceService[0];
                        }
                        if (resourceService.length > 1) {
                            serviceName = resourceService[1];
                        }
                    }
                }

                if (resourceName == null) {
                    //TODO: log message and fill missing attribute details 
                    statusCodeValue 
                            = XACMLConstants.STATUS_CODE_MISSING_ATTRIBUTE;
                    evaluationFailed = true;
                }

                if (serviceName == null) {
                    //TODO: log message and fill missing attribute details
                    throw new SAML2Exception(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "missing_attribute"));
                }
            }

            if (serviceName != null) {
                //get native action name
                if (serviceName != null) {
                    actionName = actionMapper.mapToNativeAction(
                            xacmlRequest.getAction(), serviceName);
                }

                if (actionName == null) {
                    //TODO: log message and fill missing attribute details
                    statusCodeValue = XACMLConstants.STATUS_CODE_MISSING_ATTRIBUTE;
                    evaluationFailed = true;
                }
            }

            //get environment map
            /*
            environment = environmentMapper.mapToNativeEnvironment(
                    xacmlRequest.getEnvironment(), 
                    xacmlRequest.getSubjects());
            */
        } catch (XACMLException xe) {
            statusCodeValue = XACMLConstants.STATUS_CODE_MISSING_ATTRIBUTE;
            evaluationFailed = true;
            if (XACMLSDKUtils.debug.warningEnabled()) {
                XACMLSDKUtils.debug.warning(
                        "XACMLAuthzDecisionQueryHandler.handleQuery(),"
                        + "caught exception", xe);
            }
        }


        //get native policy deicison using native policy evaluator
        if (!evaluationFailed) {
            try {
                PolicyEvaluator pe = new PolicyEvaluator(serviceName);
                booleanDecision = pe.isAllowed(ssoToken, resourceName,
                        actionName, environment);
            } catch (SSOException ssoe) {
                if (XACMLSDKUtils.debug.warningEnabled()) {
                    XACMLSDKUtils.debug.warning(
                            "XACMLAuthzDecisionQueryHandler.handleQuery(),"
                            + "caught exception", ssoe);
                }
                evaluationFailed = true;
            } catch (PolicyException pe) {
                if (XACMLSDKUtils.debug.warningEnabled()) { 
                    XACMLSDKUtils.debug.warning(
                            "XACMLAuthzDecisionQueryHandler.handleQuery(),"
                            + "caught exception", pe);
                }
                evaluationFailed = true;
            }
        }

        //decision: Indeterminate, Deny, Permit, NotApplicable
        //status code: missing_attribute, syntax_error, processing_error, ok

        Decision decision = ContextFactory.getInstance().createDecision();
        Status status = ContextFactory.getInstance().createStatus();
        StatusCode code = ContextFactory.getInstance().createStatusCode();
        StatusMessage message 
                = ContextFactory.getInstance().createStatusMessage();
        StatusDetail detail 
                = ContextFactory.getInstance().createStatusDetail();
        detail.getElement().insertBefore(detail.getElement().cloneNode(true), 
                null);
        if (evaluationFailed) {
            decision.setValue(XACMLConstants.INDETERMINATE);
            if (statusCodeValue == null) {
                statusCodeValue = XACMLConstants.STATUS_CODE_PROCESSING_ERROR;    
            }
            code.setValue(statusCodeValue);
            message.setValue("processing_error"); //TODO: i18n
        } else if (booleanDecision) {
            decision.setValue(XACMLConstants.PERMIT);
            code.setValue(XACMLConstants.STATUS_CODE_OK);
            message.setValue("ok"); //TODO: i18n
        } else {
            decision.setValue(XACMLConstants.DENY);
            code.setValue(XACMLConstants.STATUS_CODE_OK);
            message.setValue("ok"); //TODO: i18n
        }

        Result result = ContextFactory.getInstance().createResult();
        String resourceId = resourceName; 
        List resources = xacmlRequest.getResources();
        Resource resource = null;
        if (!resources.isEmpty()) {
            //We deal with only one resource for now
            resource = (Resource)resources.get(0);
            if (resource != null) {
                List attributes = resource.getAttributes();
                if (attributes != null) {
                    for (int count = 0; count < attributes.size(); count++) {
                        Attribute attr = (Attribute) attributes.get(count);
                        if (attr != null) {
                            URI tmpURI = attr.getAttributeId();
                            if (tmpURI.toString().equals(XACMLConstants.
                                RESOURCE_ID)) {
                                    Element element 
                                        = (Element)attr.getAttributeValues().get(0);
                                    resourceId = XMLUtils.getElementValue(element);
                                    break;
                            }
                        }
                    }
                }
            }
        }
        result.setResourceId(resourceId);
        result.setDecision(decision);

        status.setStatusCode(code);
        status.setStatusMessage(message);
        status.setStatusDetail(detail);
        result.setStatus(status);

        Response response = ContextFactory.getInstance().createResponse();
        response.addResult(result);

        XACMLAuthzDecisionStatement statement = ContextFactory.getInstance()
                .createXACMLAuthzDecisionStatement();
        statement.setResponse(response);
        if (returnContext) {
            statement.setRequest(xacmlRequest);
        }

        com.sun.identity.saml2.protocol.Response samlpResponse
                = createSamlpResponse(statement, 
                status.getStatusCode().getValue());

        if (XACMLSDKUtils.debug.messageEnabled()) {
            XACMLSDKUtils.debug.message(
                    "XACMLAuthzDecisionQueryHandler.handleQuery(), returning"
                    + ":samlResponse=\n" 
                    + samlpResponse.toXMLString(true, true));
        }

        return samlpResponse;
    }


    private com.sun.identity.saml2.protocol.Response createSamlpResponse(
            XACMLAuthzDecisionStatement statement, String statusCodeValue) 
            throws XACMLException, SAML2Exception {

        com.sun.identity.saml2.protocol.Response samlpResponse
                = ProtocolFactory.getInstance().createResponse();
        samlpResponse.setID("response-id:1");
        samlpResponse.setVersion("2.0");
        samlpResponse.setIssueInstant(newDate());

        com.sun.identity.saml2.protocol.StatusCode samlStatusCode
                = ProtocolFactory.getInstance().createStatusCode();
        samlStatusCode.setValue(statusCodeValue);
        com.sun.identity.saml2.protocol.Status samlStatus
                = ProtocolFactory.getInstance().createStatus();
        samlStatus.setStatusCode(samlStatusCode);
        samlpResponse.setStatus(samlStatus );

        Assertion assertion = AssertionFactory.getInstance().createAssertion();
        assertion.setVersion("2.0");
        assertion.setID("response-id:1");
        assertion.setIssueInstant(newDate());
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue("issuer-1");
        assertion.setIssuer(issuer);
        List statements = new ArrayList();
        statements.add(
                statement.toXMLString(true, true)); //add decisionstatement
        assertion.setStatements(statements);
        List assertions = new ArrayList();
        assertions.add(assertion);
        samlpResponse.setAssertion(assertions);
        return samlpResponse;
    }

}

