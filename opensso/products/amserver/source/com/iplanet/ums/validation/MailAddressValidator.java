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
 * $Id: MailAddressValidator.java,v 1.3 2008/06/25 05:41:48 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums.validation;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Validates mail address This class is constructed using default(noarguments)
 * constructor and mail address is passed to validate function with optional
 * rules The passed mail address is validated for authenticity and boolean value
 * is returned accordingly.
 *
 * @supported.all.api
 */
public class MailAddressValidator implements IValidator {

    /**
     * Determines if the value is a valid email address string
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if the value represents a valid email address string
     */
    public boolean validate(String value, String rule) {
        return validate(value);
    }

    /**
     * Check if the given email address is valid
     * 
     * @param addr
     *            value to test
     * @return true if the string contains valid email characters
     */
    public boolean validate(String addr) {
        String namePart;
        String domainPart;
        StringTokenizer tok;

        int endindex = addr.indexOf('@');

        if (endindex == -1) {
            return false;
        }

        namePart = addr.substring(0, endindex);
        domainPart = addr.substring(endindex + 1, addr.length());

        tok = new StringTokenizer(namePart, ".");
        while (tok.hasMoreTokens()) {
            if (!isValidLocalPart(tok.nextToken())) {
                return false;
            }
        }

        DomainValidator validator = new DomainValidator();
        if (!validator.validate(domainPart)) {
            return false;
        }

        return true;
    }

    /**
     * Determines if the character is in inValidChars array
     */
    private boolean isValidChar(String value) {
        if (inValidChars.contains(value)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if the given name is valid
     */
    private boolean isValidLocalPart(String atom) {
        char ch;
        int val;
        Character character;

        StringBuilder buf = new StringBuilder(atom);

        for (int i = 0; i < buf.length(); i++) {

            ch = buf.charAt(i);
            val = ch;
            character = new Character(ch);

            if (val < 33 || val > 126 || !isValidChar(character.toString())) {
                return false;
            }
        }

        return true;
    }

    private static Vector inValidChars = new Vector();

    static {
        // rfc822 special
        inValidChars.addElement("(");
        inValidChars.addElement(")");
        inValidChars.addElement("<");
        inValidChars.addElement(">");
        inValidChars.addElement("@");
        inValidChars.addElement(",");
        inValidChars.addElement(";");
        inValidChars.addElement(":");
        inValidChars.addElement("\\");
        inValidChars.addElement("\"");
        inValidChars.addElement(".");
        inValidChars.addElement("[");
        inValidChars.addElement("]");
        inValidChars.addElement(" ");
        inValidChars.addElement("\t");
        // dangerous characters
        inValidChars.addElement("!");
        inValidChars.addElement("%");
        inValidChars.addElement("+");
        inValidChars.addElement("/");
        // annoying/confusing characters
        inValidChars.addElement("=");
        inValidChars.addElement("{");
        inValidChars.addElement("}");
        inValidChars.addElement("#");
        // unix shell met-characters
        inValidChars.addElement("$");
        inValidChars.addElement("&");
        inValidChars.addElement("*");
        inValidChars.addElement("?");
        inValidChars.addElement("|");
        inValidChars.addElement("~");
    }

}
