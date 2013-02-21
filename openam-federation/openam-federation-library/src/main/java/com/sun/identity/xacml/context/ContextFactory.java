/**
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
 * $Id: ContextFactory.java,v 1.3 2008/06/25 05:48:11 qcheng Exp $
 *
 */


package com.sun.identity.xacml.context;

import org.w3c.dom.Element;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.context.impl.ActionImpl;
import com.sun.identity.xacml.context.impl.AttributeImpl;
import com.sun.identity.xacml.context.impl.EnvironmentImpl;
import com.sun.identity.xacml.context.impl.RequestImpl;
import com.sun.identity.xacml.context.impl.ResourceImpl;
import com.sun.identity.xacml.context.impl.SubjectImpl;
import com.sun.identity.xacml.context.impl.DecisionImpl;
import com.sun.identity.xacml.context.impl.StatusCodeImpl;
import com.sun.identity.xacml.context.impl.StatusDetailImpl;
import com.sun.identity.xacml.context.impl.StatusImpl;
import com.sun.identity.xacml.context.impl.StatusMessageImpl;
import com.sun.identity.xacml.context.impl.ResponseImpl;
import com.sun.identity.xacml.context.impl.ResultImpl;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;
import com.sun.identity.xacml.saml2.impl.XACMLAuthzDecisionQueryImpl;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionStatement;
import com.sun.identity.xacml.saml2.impl.XACMLAuthzDecisionStatementImpl;

/**
 * This is the factory class to obtain instances of the objects defined
 * in xacml context schema.
 * There are three ways to obtain an instance of a object type:
 * with no parameters, with a DOM tree element, or with an XML String.
 *
 * @supported.all.api
 */
public class ContextFactory {

    private static ContextFactory instance = new ContextFactory();

    /**
     * Sole Constructor.
     */
    private ContextFactory() {
    }

    /**
     * Returns the instance of <code>ContextSchemaFactory</code>.
     * 
     * @return <code>ContextSchemaFactory</code>.
     * 
     */
    public static ContextFactory getInstance() {
        return instance;
    }

    /**
     * Returns a new instance of <code>Request</code>.
     *
     * @return a new instance of <code>Request</code>
     * 
     */
    public Request createRequest() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.REQUEST);
        if (obj == null) {
            return new RequestImpl();
        } else {
            return (Request) obj;
        }
    }

    /**
     * Returns a new instance of <code>Request</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Request</code>
     * @return a new instance of <code>Request</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Request createRequest(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.REQUEST, elem);
        if (obj == null) {
            return new RequestImpl(elem);
        } else {
            return (Request) obj;
        }
    }

    /**
     * Returns a new instance of <code>Request</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Request</code>
     * @return a new instance of <code>Resource</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Request createRequest(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.REQUEST, xml);
        if (obj == null) {
            return new RequestImpl(xml);
        } else {
            return (Request) obj;
        }
    }

    
    /**
     * Returns a new instance of <code>Resource</code>.
     *
     * @return a new instance of <code>Resource</code>
     * 
     */
    public Resource createResource() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.RESOURCE);
        if (obj == null) {
            return new ResourceImpl();
        } else {
            return (Resource) obj;
        }
    }

    /**
     * Returns a new instance of <code>Resource</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Resource</code>
     * @return a new instance of <code>Resource</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Resource createResource(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.RESOURCE, elem);
        if (obj == null) {
            return new ResourceImpl(elem);
        } else {
            return (Resource) obj;
        }
    }

    /**
     * Returns a new instance of <code>Resource</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Resource</code>
     * @return a new instance of <code>Resource</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Resource createResource(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.RESOURCE, xml);
        if (obj == null) {
            return new ResourceImpl(xml);
        } else {
            return (Resource) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     *
     * @return a new instance of <code>Subject</code>
     * 
     */
    public Subject createSubject() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.SUBJECT);
        if (obj == null) {
            return new SubjectImpl();
        } else {
            return (Subject) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Subject</code>
     * @return a new instance of <code>Subject</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Subject createSubject(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.SUBJECT, elem);
        if (obj == null) {
            return new SubjectImpl(elem);
        } else {
            return (Subject) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Subject</code>
     * @return a new instance of <code>Subject</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Subject createSubject(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.SUBJECT, xml);
        if (obj == null) {
            return new SubjectImpl(xml);
        } else {
            return (Subject) obj;
        }
    }
    
    /**
     * Returns a new instance of <code>Action</code>.
     *
     * @return a new instance of <code>Action</code>
     * 
     */
    public Action createAction() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.ACTION);
        if (obj == null) {
            return new ActionImpl();
        } else {
            return (Action) obj;
        }
    }

    /**
     * Returns a new instance of <code>Action</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Action</code>
     * @return a new instance of <code>Action</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Action createAction(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ACTION, elem);
        if (obj == null) {
            return new ActionImpl(elem);
        } else {
            return (Action) obj;
        }
    }

    /**
     * Returns a new instance of <code>Action</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Action</code>
     * @return a new instance of <code>Action</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Action createAction(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ACTION, xml);
        if (obj == null) {
            return new ActionImpl(xml);
        } else {
            return (Action) obj;
        }
    }
    
    /**
     * Returns a new instance of <code>Environment</code>.
     *
     * @return a new instance of <code>Environment</code>
     * 
     */
    public Environment createEnvironment() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.
            ENVIRONMENT);
        if (obj == null) {
            return new EnvironmentImpl();
        } else {
            return (Environment) obj;
        }
    }

    /**
     * Returns a new instance of <code>Environment</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Environment</code>
     * @return a new instance of <code>Environment</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Environment createEnvironment(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ENVIRONMENT, elem);
        if (obj == null) {
            return new EnvironmentImpl(elem);
        } else {
            return (Environment) obj;
        }
    }

    /**
     * Returns a new instance of <code>Environment</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Environment</code>
     * @return a new instance of <code>Environment</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Environment createEnvironment(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ENVIRONMENT, xml);
        if (obj == null) {
            return new EnvironmentImpl(xml);
        } else {
            return (Environment) obj;
        }
    }


    /**
     * Returns a new instance of <code>Attribute</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>Attribute</code>.
     * 
     */
    public Attribute createAttribute() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.
            ATTRIBUTE);
        if (obj == null) {
            return new AttributeImpl();
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>Attribute</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>Attribute</code>.
     * @return a new instance of <code>Attribute</code>.
     * @throws XACMLException if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public Attribute createAttribute(Element elem)
                throws XACMLException
    {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ATTRIBUTE, elem);
        if (obj == null) {
            return new AttributeImpl(elem);
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>Attribute</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>Attribute</code>.
     * @return a new instance of <code>Attribute</code>.
     * @throws XACMLException if error occurs while processing the XML string.
     * 
     */
    public Attribute createAttribute(String xml)
                throws XACMLException
    {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.ATTRIBUTE, xml);
        if (obj == null) {
            return new AttributeImpl(xml);
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * 
     */
    public XACMLAuthzDecisionQuery createXACMLAuthzDecisionQuery() {
        Object obj = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.XACML_AUTHZ_DECISION_QUERY);
        if (obj == null) {
            return new XACMLAuthzDecisionQueryImpl();
        } else {
            return (XACMLAuthzDecisionQuery) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionQuery</code>. 
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>XACMLAuthzDecisionQuery</code>.
     * @return a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * @throws XACMLException if error occurs while processing the
     *                <code>Element</code>.
     * @throws SAML2Exception if not able to create the base saml
     * <code>RequestAbstract</code>
     * 
     */
    public XACMLAuthzDecisionQuery createXACMLAuthzDecisionQuery(Element elem)
                throws XACMLException, SAML2Exception
    {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.XACML_AUTHZ_DECISION_QUERY, elem);
        if (obj == null) {
            return new XACMLAuthzDecisionQueryImpl(elem);
        } else {
            return (XACMLAuthzDecisionQuery) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionQuery</code>. 
     * The return object is immutable.
     *
     * @param xml an XML String representing 
     * <code>XACMLAuthzDecisionQuery</code>.
     * @return a new instance of <code>XACMLAuthzDecisionQuery</code>.
     * @throws XACMLException if error occurs while processing the XML string.
     * @throws SAML2Exception if not able to create the base saml
     * <code>RequestAbstract</code>
     * 
     */
    public XACMLAuthzDecisionQuery createXACMLAuthzDecisionQuery(String xml)
                throws XACMLException, SAML2Exception
    {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.XACML_AUTHZ_DECISION_QUERY, xml);
        if (obj == null) {
            return new XACMLAuthzDecisionQueryImpl(xml);
        } else {
            return (XACMLAuthzDecisionQuery) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionStatement</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>XACMLAuthzDecisionStatement</code>.
     * 
     */
    public XACMLAuthzDecisionStatement createXACMLAuthzDecisionStatement() {
        Object obj = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.XACML_AUTHZ_DECISION_STATEMENT);
        if (obj == null) {
            return new XACMLAuthzDecisionStatementImpl();
        } else {
            return (XACMLAuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionStatement</code>. 
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>XACMLAuthzDecisionStatement</code>.
     * @return a new instance of <code>XACMLAuthzDecisionStatement</code>.
     * @throws XACMLException if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public XACMLAuthzDecisionStatement createXACMLAuthzDecisionStatement(
            Element elem) throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.XACML_AUTHZ_DECISION_STATEMENT, elem);
        if (obj == null) {
            return new XACMLAuthzDecisionStatementImpl(elem);
        } else {
            return (XACMLAuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>XACMLAuthzDecisionStatement</code>. 
     * The return object is immutable.
     *
     * @param xml an XML String representing 
     * <code>XACMLAuthzDecisionStatement</code>.
     * @return a new instance of <code>XACMLAuthzDecisionStatement</code>.
     * @throws XACMLException if error occurs while processing the XML string.
     * 
     */
    public XACMLAuthzDecisionStatement createXACMLAuthzDecisionStatement(String xml)
            throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.XACML_AUTHZ_DECISION_STATEMENT, xml);
        if (obj == null) {
            return new XACMLAuthzDecisionStatementImpl(xml);
        } else {
            return (XACMLAuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>Response</code>.
     *
     * @return a new instance of <code>Response</code>
     * 
     */
    public Response createResponse() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESPONSE);
        if (object == null) {
            return new ResponseImpl();
        } else {
            return (Response)object;
        }
    }

    /**
     * Returns a new instance of <code>Response</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Response</code>
     * @return a new instance of <code>Response</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Response createResponse(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESPONSE, elem);
        if (object == null) {
            return new ResponseImpl(elem);
        } else {
            return (Response)object;
        }
    }

    /**
     * Returns a new instance of <code>Response</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Response</code>
     * @return a new instance of <code>Response</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Response createResponse(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESPONSE, xml);
        if (object == null) {
            return new ResponseImpl(xml);
        } else {
            return (Response)object;
        }
    }

    /**
     * Returns a new instance of <code>Result</code>.
     *
     * @return a new instance of <code>Result</code>
     * 
     */
    public Result createResult() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESULT);
        if (object == null) {
            return new ResultImpl();
        } else {
            return (Result)object;
        }
    }

    /**
     * Returns a new instance of <code>Result</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Result</code>
     * @return a new instance of <code>Result</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Result createResult(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESULT, elem);
        if (object == null) {
            return new ResultImpl(elem);
        } else {
            return (Result)object;
        }
    }

    /**
     * Returns a new instance of <code>Result</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Result</code>
     * @return a new instance of <code>Result</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Result createResult(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.RESULT, xml);
        if (object == null) {
            return new ResultImpl(xml);
        } else {
            return (Result)object;
        }
    }

    /**
     * Returns a new instance of <code>Decision</code>.
     *
     * @return a new instance of <code>Decision</code>
     * 
     */
    public Decision createDecision() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.DECISION);
        if (object == null) {
            return new DecisionImpl();
        } else {
            return (Decision)object;
        }
    }

    /**
     * Returns a new instance of <code>Decision</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Decision</code>
     * @return a new instance of <code>Decision</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Decision createDecision(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.DECISION, elem);
        if (object == null) {
            return new DecisionImpl(elem);
        } else {
            return (Decision)object;
        }
    }

    /**
     * Returns a new instance of <code>Decision</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Decision</code>
     * @return a new instance of <code>Decision</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Decision createDecision(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.DECISION, xml);
        if (object == null) {
            return new DecisionImpl(xml);
        } else {
            return (Decision)object;
        }
    }

    /**
     * Returns a new instance of <code>Status</code>.
     *
     * @return a new instance of <code>Status</code>
     * 
     */
    public Status createStatus() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS);
        if (object == null) {
            return new StatusImpl();
        } else {
            return (Status)object;
        }
    }

    /**
     * Returns a new instance of <code>Status</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Status</code>
     * @return a new instance of <code>Status</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Status createStatus(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS, elem);
        if (object == null) {
            return new StatusImpl(elem);
        } else {
            return (Status)object;
        }
    }

    /**
     * Returns a new instance of <code>Status</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Status</code>
     * @return a new instance of <code>Status</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Status createStatus(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS, xml);
        if (object == null) {
            return new StatusImpl(xml);
        } else {
            return (Status)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusCode</code>.
     *
     * @return a new instance of <code>StatusCode</code>
     * 
     */
    public StatusCode createStatusCode() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_CODE);
        if (object == null) {
            return new StatusCodeImpl();
        } else {
            return (StatusCode)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusCode</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>StatusCode</code>
     * @return a new instance of <code>StatusCode</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public StatusCode createStatusCode(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_CODE, elem);
        if (object == null) {
            return new StatusCodeImpl(elem);
        } else {
            return (StatusCode)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusCode</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>StatusCode</code>
     * @return a new instance of <code>StatusCode</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public StatusCode createStatusCode(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_CODE, xml);
        if (object == null) {
            return new StatusCodeImpl(xml);
        } else {
            return (StatusCode)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusMessage</code>.
     *
     * @return a new instance of <code>StatusMessage</code>
     * 
     */
    public StatusMessage createStatusMessage() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_MESSAGE);
        if (object == null) {
            return new StatusMessageImpl();
        } else {
            return (StatusMessage)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusMessage</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>StatusMessage</code>
     * @return a new instance of <code>StatusMessage</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public StatusMessage createStatusMessage(Element elem)
            throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_MESSAGE, elem);
        if (object == null) {
            return new StatusMessageImpl(elem);
        } else {
            return (StatusMessage)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusMessage</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>StatusMessage</code>
     * @return a new instance of <code>StatusMessage</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public StatusMessage createStatusMessage(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_MESSAGE, xml);
        if (object == null) {
            return new StatusMessageImpl(xml);
        } else {
            return (StatusMessage)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusDetail</code>.
     *
     * @return a new instance of <code>StatusDetail</code>
     * 
     */
    public StatusDetail createStatusDetail() throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_DETAIL);
        if (object == null) {
            return new StatusDetailImpl();
        } else {
            return (StatusDetail)object;
        }
    }
    /**
     * Returns a new instance of <code>StatusDetail</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>StatusDetail</code>
     * @return a new instance of <code>StatusDetail</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public StatusDetail createStatusDetail(Element elem)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_DETAIL, elem);
        if (object == null) {
            return new StatusDetailImpl(elem);
        } else {
            return (StatusDetail)object;
        }
    }

    /**
     * Returns a new instance of <code>StatusDetail</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>StatusDetail</code>
     * @return a new instance of <code>StatusDetail</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public StatusDetail createStatusDetail(String xml)throws XACMLException {
        Object object = XACMLSDKUtils.getObjectInstance(
                XACMLConstants.STATUS_DETAIL, xml);
        if (object == null) {
            return new StatusDetailImpl(xml);
        } else {
            return (StatusDetail)object;
        }
    }

}
