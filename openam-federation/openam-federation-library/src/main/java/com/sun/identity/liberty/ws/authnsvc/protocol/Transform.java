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
 * $Id: Transform.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;

/**
 * The <code>Transform</code> class represents 'Transform' element in
 * 'PasswordTransforms' element defined in Authentication Service schema.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public abstract class Transform {

    /**
     * Truncation Transform name.
     */
    public static final String TRUNCATION_URI = "urn:liberty:sa:pw:truncate";

    /**
     * Lowercase Transform name.
     */
    public static final String LOWERCASE_URI = "urn:liberty:sa:pw:lowercase";

    /**
     * Uppercase Transform name.
     */
    public static final String UPPERCASE_URI = "urn:liberty:sa:pw:uppercase";

    /**
     * Select Transform name.
     */
    public static final String SELECT_URI = "urn:liberty:sa:pw:select";

    private static final String TRANSFORM_CLASSES =
                      "com.sun.identity.liberty.ws.authnsvc.transformClasses";

    private static Map transformClasses = new HashMap();

    protected String name = null;
    protected String id = null;
    protected List parameters = null;

    static {
        String tmpstr = SystemPropertiesManager.get(TRANSFORM_CLASSES);
        if (tmpstr != null && tmpstr.length() > 0) {
            StringTokenizer stz = new StringTokenizer(tmpstr, ",");
            while(stz.hasMoreTokens()) {
                String token = stz.nextToken().trim();
                int index = token.indexOf('|');
                if (index != -1 && index != 0 && index != token.length() - 1) {
                    String name = token.substring(0, index);
                    String className = token.substring(index + 1);
                    if (AuthnSvcUtils.debug.messageEnabled()) {
                        AuthnSvcUtils.debug.message(
                                      "Transform.static: add " + token);
                    }
                    transformClasses.put(name, className);
                } else {
                    if (AuthnSvcUtils.debug.warningEnabled()) {
                        AuthnSvcUtils.debug.warning(
                                      "Transform.static: Invalid syntax " +
                                      "for Transform Classes List: " +
                                      token);
                    }
                }
            }          
        }

    }

    static Transform getTransform(Element element) throws AuthnSvcException {

        String name = XMLUtils.getNodeAttributeValue(element,
                                                  AuthnSvcConstants.ATTR_NAME);
        if (name == null || name.length() == 0) {
            throw new AuthnSvcException("missingNameTF");
        }

        Transform tf = null;
        String className = (String)transformClasses.get(name);
        if (className != null) {
            try {
                tf = (Transform)Class.forName(className).newInstance();
            } catch (Throwable t) {
                if (AuthnSvcUtils.debug.warningEnabled()) {
                    AuthnSvcUtils.debug.warning(
                            "Transform.getTransform class = " + className, t);
                }

                transformClasses.remove(name);
            }
        }

        if (tf == null) {
            if (name.equals(TRUNCATION_URI)) {

                tf = new TruncationTransform();
            } else if (name.equals(LOWERCASE_URI)) {

                tf = new LowercaseTransform();
            } else if (name.equals(UPPERCASE_URI)) {

                tf = new UppercaseTransform();
            } else if (name.equals(SELECT_URI)) {

                tf = new SelectTransform();
            } else {

                tf = new GenericTransform(name);
            }
        }

        String id =  XMLUtils.getNodeAttributeValue(element,
                                                    AuthnSvcConstants.ATTR_id);
        tf.setId(id);

        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        List parameters = null;
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();

                if (AuthnSvcConstants.NS_AUTHN_SVC.equals(namespaceURI) &&
                    AuthnSvcConstants.TAG_PARAMETER.equals(localName)) {

                    Parameter parameter = new Parameter(childElement);
                    if (parameters == null) {
                        parameters = new ArrayList();
                    }
                    parameters.add(parameter);
                } else {
                    throw new AuthnSvcException("invalidChildTF");
                }
            }
        }

        tf.setParameters(parameters);

        return tf;
    }


    /**
     * Transforms password.
     * @param password original password
     * @return transformed password
     */
    public abstract String transform(String password);

    /**
     * Returns value of 'name' attribute.
     * @return value of 'name' attribute
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns value of 'id' attribute.
     * @return value of 'id' attribute
     * @see #setId(String)
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns a list of 'Parameter' child element.
     * @return a list of 'Parameter' child element
     * @see #setParameters(List)
     */
    public List getParameters()
    {
        return parameters;
    }

    /**
     * Sets value of 'id' attribute.
     * @param id value of 'id' attribute
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Sets a list of 'Parameter' child element.
     * @param parameters a list of 'Parameter' child element
     * @see #getParameters()
     */
    public void setParameters(List parameters)
    {
        this.parameters = parameters;
    }

    /**
     * Converts this to <code>org.w3c.dom.Element</code> and add to
     * parent PasswordTransforms Element.
     * @param ptE parent PasswordTransforms Element
     * @exception AuthnSvcException if there is 'name' attribute is empty
     */
    void addToParent(Element ptE) throws AuthnSvcException
    {
        if (name == null || name.length() == 0) {
            throw new AuthnSvcException("missingNameTF");
        }

        Document doc = ptE.getOwnerDocument();
        Element tfE = doc.createElementNS(
                            AuthnSvcConstants.NS_AUTHN_SVC,
                            AuthnSvcConstants.PTAG_TRANSFORM);
        ptE.appendChild(tfE);

        tfE.setAttributeNS(null, AuthnSvcConstants.ATTR_NAME, name);

        if (id != null) {
            tfE.setAttributeNS(null, AuthnSvcConstants.ATTR_id, id);
        }

        if (parameters != null && !parameters.isEmpty()) {
            for(Iterator iter = parameters.iterator(); iter.hasNext(); ) {
                Parameter parameter = (Parameter)iter.next();
                parameter.addToParent(tfE);
            }
        }

    }
}
