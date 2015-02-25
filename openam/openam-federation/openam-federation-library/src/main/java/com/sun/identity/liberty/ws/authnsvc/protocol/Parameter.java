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
 * $Id: Parameter.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;

/**
 * The <code>Parameter</code> class represents 'Parameter' element in
 * 'Transform' element in 'PasswordTransforms' element defined in
 * Authentication Service schema.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class Parameter {

    /**
     * Parameter name 'length'
     */
    public static final String NAME_LENGTH = "length";

    /**
     * Parameter name 'allowed'
     */
    public static final String NAME_ALLOWED = "allowed";

    private String name = null;
    private String value = null;

    /**
     * Constructor takes the value of 'name' attribute and value
     * of 'Transform' element.
     * @param name value of 'name' attribute
     * @param value value of 'Transform' element
     */
    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructor takes a <code>org.w3c.dom.Element</code>.
     * @param element a Parameter element
     * @exception AuthnSvcException if an error occurs while parsing
     *                      the Parameter element
     */
    Parameter(Element element) throws AuthnSvcException
    {
        name = XMLUtils.getNodeAttributeValue(element,
                                              AuthnSvcConstants.ATTR_NAME);
        if (name == null || name.length() == 0) {
            throw new AuthnSvcException("missingNamePM");
        }

        value = XMLUtils.getElementValue(element);
    }

    /**
     * Returns value of 'name' attribute.
     * @return value of 'name' attribute
     * @see #setName(String)
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns value of 'Parameter' element.
     * @return value of 'Parameter' element
     * @see #setValue(String)
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets value of 'name' attribute.
     * @param name value of 'name' attribute
     * @see #getName()
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets value of 'Parameter' element.
     * @param value value of 'Parameter' element
     * @see #getValue()
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Converts this to <code>org.w3c.dom.Element</code> and add to
     * parent Transform Element.
     * @param tfE parent Transform Element
     */
    void addToParent(Element tfE) throws AuthnSvcException
    {
        if (name == null || name.length() == 0) {
            throw new AuthnSvcException("missingNamePM");
        }

        Document doc = tfE.getOwnerDocument();
        Element pmE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                          AuthnSvcConstants.PTAG_PARAMETER);
        tfE.appendChild(pmE);

        pmE.setAttributeNS(null, AuthnSvcConstants.ATTR_NAME, name);

        if (value != null) {
            pmE.appendChild(doc.createTextNode(value));
        }
    }
}
