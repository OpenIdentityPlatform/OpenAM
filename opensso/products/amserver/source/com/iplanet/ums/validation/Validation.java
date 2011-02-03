/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Validation.java,v 1.4 2009/01/28 05:34:52 ww203982 Exp $
 *
 */

package com.iplanet.ums.validation;

import java.util.Enumeration;

import com.sun.identity.shared.ldap.LDAPModification;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;
import com.iplanet.ums.CreationTemplate;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.TemplateManager;
import com.iplanet.ums.UMSException;

/**
 * Validation handles all validation routines. This class is constructed using
 * default constructor (no argument constructor) and validateAttribute function
 * is used to validate set of attributes against optional rules passed and
 * validator specific rules. UMSException is throw if there is instanciation
 * problems DataConstraintException is thrown when the attributes fail to
 * validations.
 *
 * @supported.all.api
 */
public class Validation {

    /**
     * Determines whether a specific attribute is valid. Called by
     * validateAttribute(Attr, Class). This method calls the validation method
     * for this attribute.
     * 
     * @param attr
     *            attribute to test
     * @param validatorClass
     *            the validator class name
     * @param rule
     *            optional rule applies to the validator
     * @exception UMSException
     *                failure
     * @exception DataConstraintException
     *                data validation failure
     */
    public static void validateAttribute(Attr attr, String validatorClass,
            String rule) throws UMSException, DataConstraintException {

        if (attr != null) {
            String[] values = attr.getStringValues();
            for (int i = 0; i < values.length; i++) {
                String aValue = values[i];

                if ((aValue != null) && (!aValue.equalsIgnoreCase(""))
                        && (validatorClass != null)) {

                    IValidator validator = null;
                    try {
                        Class theClass = Class.forName(validatorClass);
                        validator = (IValidator) theClass.newInstance();
                    } catch (Exception e) {
                        throw new UMSException(i18n
                                .getString(IUMSConstants.INSTANCE_FAILED), e);
                    }

                    if (!validator.validate(aValue, rule)) {
                        String msg = i18n
                                .getString(IUMSConstants.DATA_CONSTRAINT);
                        throw new DataConstraintException(msg + ": "
                                + "{ type=" + attr.getName() + ", value="
                                + aValue + " }");
                    }
                }
            }
        }
    }

    /**
     * Determines whether attribute is valid. Check the attribute if there is a
     * validation method that needs to execute.
     * 
     * @param attr
     *            attribute to test
     * @param cls
     *            Class associatd with this attribute
     * @param guid
     *            the guid of the Organization where the config data is stored
     * @exception UMSException
     *                failure
     * @exception DataConstraintException
     *                data validation failure
     */
    public static void validateAttribute(Attr attr, Class cls, Guid guid)
            throws UMSException, DataConstraintException {

        if (attr == null) {
            return;
        }

        String validatorClass = null;
        String rule = null;
        String attrName = attr.getName();

        // Gets the Template associates with the Class
        CreationTemplate ct = TemplateManager.getTemplateManager()
                .getCreationTemplate(cls, guid);
        if (ct != null) {
            // Gets an enumeration of ValidationElements of this attriubte
            Enumeration en = ct.getValidation(attrName);

            while (en.hasMoreElements()) {
                ValidationElement vElement = (ValidationElement) en
                        .nextElement();
                validatorClass = vElement.getValidator();
                rule = vElement.getRule();
                if (validatorClass != null) {
                    validateAttribute(attr, validatorClass, rule);

                }
            }
        }
    }

    /**
     * Determines whether each attribute in the attribute set is valid. Iterates
     * though the set checking each element to see if there is a validation
     * method that needs to execute.
     * 
     * @param attrSet
     *            attribute set to test
     * @param cls
     *            Class associated with these attribute set
     * @param guid
     *            the guid of the Organization where the config data is stored
     * @exception UMSException
     *                failure
     * @exception DataConstraintException
     *                data validation failure
     */
    public static void validateAttributes(AttrSet attrSet, Class cls, Guid guid)
            throws UMSException, DataConstraintException {

        if (attrSet == null) {
            return;
        }

        String[] attrNames = attrSet.getAttributeNames();
        for (int i = 0; i < attrNames.length; i++) {
            Attr attr = attrSet.getAttribute(attrNames[i]);
            validateAttribute(attr, cls, guid);
        }
    }

    /**
     * Determines whether each attribute in the ModSet is valid.
     * 
     * @param modSet
     *            modSet to test
     * @param cls
     *            Class associated with these attributes
     * @param guid
     *            the guid of the Organization where the config data is stored
     * @exception UMSException
     *                failure
     * @exception DataConstraintException
     *                data validation failure
     */
    public static void validateAttributes(ModSet modSet, Class cls, Guid guid)
            throws UMSException, DataConstraintException {

        if (modSet == null) {
            return;
        }

        for (int i = 0; i < modSet.size(); i++) {
            LDAPModification ldapMod = modSet.elementAt(i);
            if (ldapMod.getOp() != LDAPModification.DELETE) {
                Attr attr = new Attr(ldapMod.getAttribute());
                validateAttribute(attr, cls, guid);
            }
        }
    }

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
}
