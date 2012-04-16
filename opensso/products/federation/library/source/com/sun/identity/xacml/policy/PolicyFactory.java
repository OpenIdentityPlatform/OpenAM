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
 * $Id: PolicyFactory.java,v 1.2 2008/06/25 05:48:14 qcheng Exp $
 *
 */


package com.sun.identity.xacml.policy;

import org.w3c.dom.Element;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.policy.impl.ObligationImpl;
import com.sun.identity.xacml.policy.impl.ObligationsImpl;

/**
 * This is the factory class to obtain instances of the objects defined
 * in xacml context schema.
 * There are three ways to obtain an instance of a object type:
 * with no parameters, with a DOM tree element, or with an XML String.
 *
 * @supported.all.api
 */
public class PolicyFactory {

    private static PolicyFactory instance = new PolicyFactory();

    /**
     * Sole Constructor.
     */
    private PolicyFactory() {
    }

    /**
     * Returns the instance of <code>ContextSchemaFactory</code>.
     * 
     * @return <code>ContextSchemaFactory</code>.
     * 
     */
    public static PolicyFactory getInstance() {
        return instance;
    }

    /**
     * Returns a new instance of <code>Obligation</code>.
     *
     * @return a new instance of <code>Obligation</code>
     * 
     */
    public Obligation createObligation() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.REQUEST);
        if (obj == null) {
            return new ObligationImpl();
        } else {
            return (Obligation) obj;
        }
    }

    /**
     * Returns a new instance of <code>Obligation</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Obligation</code>
     * @return a new instance of <code>Obligation</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Obligation createObligation(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.REQUEST, elem);
        if (obj == null) {
            return new ObligationImpl(elem);
        } else {
            return (Obligation) obj;
        }
    }

    /**
     * Returns a new instance of <code>Obligation</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Obligation</code>
     * @return a new instance of <code>Resource</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Obligation createObligation(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.REQUEST, xml);
        if (obj == null) {
            return new ObligationImpl(xml);
        } else {
            return (Obligation) obj;
        }
    }

    
    /**
     * Returns a new instance of <code>Obligations</code>.
     *
     * @return a new instance of <code>Obligations</code>
     * 
     */
    public Obligations createObligations() {
        Object obj = XACMLSDKUtils.getObjectInstance(XACMLConstants.RESOURCE);
        if (obj == null) {
            return new ObligationsImpl();
        } else {
            return (Obligations) obj;
        }
    }

    /**
     * Returns a new instance of <code>Obligations</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Obligations</code>
     * @return a new instance of <code>Obligations</code>
     * @throws XACMLException if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Obligations createObligations(Element elem)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.RESOURCE, elem);
        if (obj == null) {
            return new ObligationsImpl(elem);
        } else {
            return (Obligations) obj;
        }
    }

    /**
     * Returns a new instance of <code>Obligations</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Obligations</code>
     * @return a new instance of <code>Obligations</code>
     * @throws XACMLException if error occurs while processing the 
     *    XML string
     * 
     */
    public Obligations createObligations(String xml)
        throws XACMLException {
        Object obj = XACMLSDKUtils.getObjectInstance(
            XACMLConstants.RESOURCE, xml);
        if (obj == null) {
            return new ObligationsImpl(xml);
        } else {
            return (Obligations) obj;
        }
    }

}
