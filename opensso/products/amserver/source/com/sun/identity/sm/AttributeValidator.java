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
 * $Id: AttributeValidator.java,v 1.10 2009/11/03 00:06:31 hengming Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.AMEncryption;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.validation.BooleanValidator;
import com.iplanet.ums.validation.DNValidator;
import com.iplanet.ums.validation.FloatValidator;
import com.iplanet.ums.validation.MailAddressValidator;
import com.iplanet.ums.validation.NumberValidator;
import com.iplanet.ums.validation.URLValidator;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class <code> AttributeValidator </code> provides methods by which
 * ServiceConfig data to be stored in the Directory, can be validated against
 * the relevant Service Schema. The validator needs to check against the
 * relevant Schema to validate the attribute syntax and type.
 * 
 */
class AttributeValidator {
    // Static variables
    static final MailAddressValidator mailValidator = 
        new MailAddressValidator();

    static final BooleanValidator boolValidator = new BooleanValidator();

    static final NumberValidator numberValidator = new NumberValidator();

    static final URLValidator urlValidator = new URLValidator();

    static final FloatValidator floatValidator = new FloatValidator();

    static final DNValidator dnValidator = new DNValidator();

    static Debug debug = SMSEntry.debug;

    // Instance variables
    private AttributeSchemaImpl as;

    /**
     * Constructor
     * 
     * @param as
     *            the service schema which will be used to validate the
     *            attribute values
     */
    AttributeValidator(AttributeSchemaImpl as) {
        this.as = as;
    }

    /**
     * This method validates the syntax of the Attribute values against what it
     * is supposed to be in the ServiceSchema.
     * 
     * @param values
     *            Set of all the values for this attribute.
     * @param encodePassword
     *            if true, the values will be encrypted if the attribute's
     *            syntax is password
     * @return boolean true or false depending on whether the values are valid.
     * @throws com.sun.identity.sms.SMSException
     */
    private boolean validateSyntax(Set values, boolean encodePassword)
            throws SMSException {
        AttributeSchema.Syntax syntax = as.getSyntax();
        if (syntax == null)
            return (true);
        if ((syntax.equals(AttributeSchema.Syntax.STRING))
                || (syntax.equals(AttributeSchema.Syntax.PARAGRAPH))
                || (syntax.equals(AttributeSchema.Syntax.URL))
                || (syntax.equals(AttributeSchema.Syntax.XML))
                || (syntax.equals(AttributeSchema.Syntax.BOOLEAN))
                || (syntax.equals(AttributeSchema.Syntax.DATE))) {

            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.EMAIL)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                String val = ((String) it.next()).trim();
                /**
                 * This condition is required because console is
                 * passing a set of empty string. Without this check, 
                 * mailValidator will validate empty string for email 
                 * address and fail
                 */
                if ( (values.size() == 1) && (val.length() == 0) ) {
                    break;
                }
                if (!mailValidator.validate(val)) {
                    return (false);
                }
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.PASSWORD)
                || syntax.equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD)) {
            if (encodePassword) {
                // Encrypt the passwords
                Set encValues = new HashSet();
                Set remValues = new HashSet();
                for (Iterator it = values.iterator(); it.hasNext();) {
                    String value = (String) it.next();
                    try {
                        encValues.add(AccessController
                                .doPrivileged(new EncodeAction(value)));
                    } catch (Throwable e) {
                        debug.error("AttributeValidator: Unable to encode", e);
                        encValues.add(value);
                    }
                    remValues.add(value);
                }
                values.removeAll(remValues);
                values.addAll(encValues);
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.NUMERIC)
                || syntax.equals(AttributeSchema.Syntax.NUMBER)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                if (!numberValidator.validate((String) it.next())) {
                    return (false);
                }
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.PERCENT)
                || syntax.equals(AttributeSchema.Syntax.DECIMAL_NUMBER)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                if (!floatValidator.validate((String) it.next())) {
                    return (false);
                }
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.NUMBER_RANGE)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                int i, start, end;
                try {
                    i = Integer.parseInt(s);
                    String startRange = as.getStartRange();
                    String endRange = as.getEndRange();
                    if ((startRange == null) && (endRange == null)) {
                        return (true);
                    }
                    start = Integer.parseInt(startRange);
                    end = Integer.parseInt(endRange);
                } catch (Exception e) {
                    return (false);
                }

                if ((i < start) || (i > end)) {
                    return (false);
                }
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.DECIMAL_RANGE)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                float f, start, end;
                try {
                    f = Float.parseFloat(s);
                    String startRange = as.getStartRange();
                    String endRange = as.getEndRange();
                    if ((startRange == null) && (endRange == null)) {
                        return (true);
                    }
                    start = Float.parseFloat(startRange);
                    end = Float.parseFloat(endRange);
                } catch (Exception e) {
                    return (false);
                }

                if ((f < start) || (f > end)) {
                    return (false);
                }
            }
            return (true);
        }
        if (syntax.equals(AttributeSchema.Syntax.DN)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                if (!dnValidator.validate((String) it.next()))
                    return (false);
            }
            return (true);
        }

        // Doesn't fit any of these supported syntax??
        String[] args = { as.getName() };
        throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-invalid_attribute_syntax", args);
    }

    /**
     * This method validates the type of the Attribute values against what it is
     * supposed to be in the ServiceSchema.
     * 
     * @param values
     *            Set of all the values for this attribute.
     * @return boolean true or false depending on whether the values are valid.
     * @throws com.sun.identity.sms.SMSException
     */
    private boolean validateType(Set values, Map env) throws SMSException {
        String installTime = SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME, "false");
        String[] array;
        AttributeSchema.Type type = as.getType();
        if (type == null)
            return (true);
        if (type.equals(AttributeSchema.Type.SINGLE)) {
            if (values.size() > 1) {
                return (false);
            } else {
                return (true);
            }
        }
        if (type.equals(AttributeSchema.Type.LIST)) {
            int size = values.size();
            int minValue = as.getMinValue();
            int maxValue = as.getMaxValue();
            if (!(minValue == -1 || maxValue == -1)) {
                if (size < minValue || size > maxValue) {
                    return (false);
                }
            }
            return (true);
        }
        if (type.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            if (values.size() > 1) {
                return (false);
            } else {
                // we may not be able validate choice type attribute values
                // correctly during installation time or when importing
                // service configuration.
                if (installTime.equalsIgnoreCase("true")) {
                    return true;
                }

                array = as.getChoiceValues(env);
                Iterator it = values.iterator();
                String val = (it.hasNext()) ? (String) it.next() : null;
                if (val == null) {
                    return (true);
                }
                for (int i = 0; i < array.length; i++) {
                    if (array[i].equalsIgnoreCase(val)) {
                        return (true);
                    }
                }
                return (false);
            }
        }
        if (type.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            // we may not be able validate choice type attribute values
            // correctly during installation time or when importing
            // service configuration.
            if (installTime.equalsIgnoreCase("true")) {
                return true;
            }

            array = as.getChoiceValues(env);
            int size = values.size();
            int minValue = as.getMinValue();
            int maxValue = as.getMaxValue();
            if (!(minValue == -1 || maxValue == -1)) {
                if (size < minValue || size > maxValue) {
                    return (false);
                }
            }
            if (size == 0) {
                return (true);
            }

            if ((array == null) || (array.length == 0)) {
                return false;
            }

            Iterator it = values.iterator();
            int arraySize = array.length;
            while (it.hasNext()) {
                boolean match = false;
                String value = (String) it.next();
                for (int i = 0; i < arraySize; i++) {
                    if (array[i].equalsIgnoreCase(value)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return (false);
                }
            }
            return (true);
        }
        if (type.equals(AttributeSchema.Type.VALIDATOR)) {
            return (true);
        }
        if (type.equals(AttributeSchema.Type.SIGNATURE)) {
            return (true);
        }

        // Doesn't fit any of these supported type??
        String[] args = { as.getName() };
        throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-invalid_attribute_type", args);
    }

    /**
     * Validates a map of Attributes and values against Service Schema
     * definition.
     * 
     * @param attrVals
     *            a Set of attribute values
     * @param i18nFileName
     *            Resource bundle file name
     * @param encodePassword
     *            if true, the values will be encrypted if the attribute's
     *            syntax is password
     * @return boolean true or false depending on whether the values are valid.
     * @throws com.sun.identity.sms.SMSException
     *             if values is invalidate.
     */
    boolean validate(Set attrVals, String i18nFileName, boolean encodePassword)
            throws SMSException {
        return validate(attrVals, i18nFileName, encodePassword,
                Collections.EMPTY_MAP);
    }

    /**
     * Validates a map of Attributes and values against Service Schema
     * definition.
     * 
     * @param attrVals
     *            a Set of attribute values
     * @param i18nFileName
     *            Resource bundle file name
     * @param encodePassword
     *            if true, the values will be encrypted if the attribute's
     *            syntax is password
     * @param envParam
     *            a Map of environment parameters
     * @return boolean true or false depending on whether the values are valid.
     * @throws com.sun.identity.sms.SMSException
     *             if values is invalidate.
     */
    boolean validate(Set attrVals, String i18nFileName, boolean encodePassword,
            Map envParam) throws SMSException {
        // removing old values, no need to validate
        if ((attrVals == null) || (attrVals.isEmpty())){
            return true;
        }

        if (!validateType(attrVals, envParam)
                || !validateSyntax(attrVals, encodePassword)) {
            if (debug.messageEnabled()) {
                debug.message("Validation Failed for attribute: "
                        + as.getName() + " value:" + attrVals + " Env Map: "
                        + envParam);
            }

            if (i18nFileName != null) {
                String[] args = { as.getName(), i18nFileName, as.getI18NKey() };
                throw (new InvalidAttributeValueException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-attribute-values-does-not-match-schema", args));
            } else {
                String[] args = { as.getName() };
                throw (new InvalidAttributeValueException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-attribute-values-does-not-match-schema", args));
            }
        }
        return (true);
    }

    /**
     * This method checks if the attribute name (as given by the
     * AttributeSchema) is present, and if missings adds the defaults values.
     * 
     * @param attrs
     *            A map of the attributes and their values
     * @return A map which is a union of the attributes provided and default
     *         attribute values
     */
    Map inheritDefaults(Map attrs) {
        Set values = (Set) attrs.get(as.getName());
        if (values == null) {
            // Inherit the default values
            attrs.put(as.getName(), as.getDefaultValues());
        } else if (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD)
                || as.getSyntax().equals(
                        AttributeSchema.Syntax.ENCRYPTED_PASSWORD)) {
            // Decrypt the password
            Set vals = new HashSet();
            for (Iterator items = values.iterator(); items.hasNext();) {
                String tString = (String) items.next();
                try {
                    vals.add(AccessController.doPrivileged(new DecodeAction(
                            tString)));
                } catch (Throwable e) {
                    debug.error("AttributeValidator: Unable to decode", e);
                    vals.add(tString);
                }
            }
            attrs.put(as.getName(), vals);
        }
        return (attrs);
    }

    /**
     * This method checks if attribute schema is of syntax password or
     * encoded_password, if so it decrypts the password when it is stored in the
     * cache.
     * 
     * @param attrs
     *            a Map of the attributes and their values
     * @return A map which is has replaced encrypted values with decrypted ones.
     */
    Map decodeEncodedAttrs(Map attrs) {
        Set values = (Set) attrs.get(as.getName());
        if (values == null) {
            return attrs;
        }
        if (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD)
                || as.getSyntax().equals(
                        AttributeSchema.Syntax.ENCRYPTED_PASSWORD)) {
            // Decrypt the password
            Set vals = new HashSet();
            for (Iterator items = values.iterator(); items.hasNext();) {
                String tString = (String) items.next();
                try {
                    vals.add(AccessController.doPrivileged(new DecodeAction(
                            tString)));
                } catch (Throwable e) {
                    debug.error("AttributeValidator: Unable to decode", e);
                    vals.add(tString);
                }
            }
            attrs.put(as.getName(), vals);
        }
        return (attrs);
    }

    /**
     * Encodes attribute value if it is of syntax password or encoded_password.
     * 
     * @param attrs Map of the attributes and their values
     * @param encryptObj Encryptor
     * @return A map which is has replaced values with encrypted ones.
     */
    Map encodedAttrs(Map attrs, AMEncryption encryptObj) {
        Set values = (Set) attrs.get(as.getName());
        if (values == null) {
            return attrs;
        }
        if (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD)
                || as.getSyntax().equals(
                        AttributeSchema.Syntax.ENCRYPTED_PASSWORD)) {
            // Encrypt the password
            Set vals = new HashSet();
            for (Iterator items = values.iterator(); items.hasNext();) {
                String tString = (String) items.next();
                try {
                    vals.add(AccessController.doPrivileged(new EncodeAction(
                        tString, encryptObj)));
                } catch (Throwable e) {
                    debug.error(
                        "AttributeValidator.encodedAttrs: Unable to encode", e);
                    vals.add(tString);
                }
            }
            attrs.put(as.getName(), vals);
        }
        return (attrs);
    }
}
