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
 * $Id: EmailValidator.java,v 1.2 2008/06/25 05:44:04 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * The class <code>EmailValidator</code> is used to check if the Email address
 * is syntactically correct according to valid format defined in RFC 822. Email
 * address must contain an @ symbol as well as syntactically correct username
 * and domain information e.g. name@domain.com
 */
public class EmailValidator implements ServiceAttributeValidator {

    /**
     * Default Constructor.
     */
    public EmailValidator() {
    }

    /**
     * Validates a set of email address.
     * 
     * @param values
     *            the set of string email address to validate
     * @return true if all of the email addresses are valid; false otherwise
     */
    public boolean validate(Set values) {

        Iterator it = values.iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            if (!validate(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates an email address.
     * 
     * @param value
     *            an email address
     * @return true if the email address is valid; false otherwise
     */
    public boolean validate(String value) {

        int index = value.indexOf('@');

        if (index == -1) {
            return false;
        }

        String name = value.substring(0, index);
        String domain = value.substring(index + 1, value.length());

        if (validateName(name) && validateDomain(domain)) {
            return true;
        } else {
            return false;
        }

    }

    private boolean validateName(String name) {

        StringTokenizer tok = new StringTokenizer(name, ".");

        if (tok.countTokens() < 1) {
            return false;
        }

        while (tok.hasMoreTokens()) {
            if (!isValidNamePart(tok.nextToken())) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidNamePart(String name) {

        int length = name.length();

        for (int i = 0; i < length; i++) {

            char ch = name.charAt(i);
            int val = ch;
            Character character = new Character(ch);

            // The character can be any ASCII character except:
            // a) ASCII control character and DEL
            // b) space
            // c) RFC 822 specials
            if (val < 33 || val > 126
                    || inValidChars.contains(character.toString())) {
                return false;
            }
        }

        return true;
    }

    private boolean validateDomain(String domain) {

        StringTokenizer tok = new StringTokenizer(domain, ".");
        if (tok.countTokens() <= 1) {
            return false;
        }

        while (tok.hasMoreTokens()) {
            if (!isValidDomainPart(tok.nextToken())) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidDomainPart(String domain) {

        int length = domain.length();

        // subdomain can not start or end with a hyphen
        if (domain.charAt(0) == '-' || domain.charAt(length - 1) == '-') {
            return false;
        }

        // subdomain or "label" can not be longer than 63 chars
        if (length > 63) {
            return false;
        }

        for (int i = 0; i < length; i++) {

            char ch = domain.charAt(i);

            if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '-')
            {
                return false;
            }
        }

        return true;
    }

    private static Vector inValidChars = new Vector();

    static {
        // RFC 822 special characters
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
    }

}
