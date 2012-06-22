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
 * $Id: DomainValidator.java,v 1.4 2008/06/25 05:41:48 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums.validation;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Validates domain
 *
 * @supported.all.api
 */
public class DomainValidator implements IValidator {

    /**
     * Determines if the value is a valid domain string
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if the value represents a valid domain string
     */
    public boolean validate(String value, String rule) {
        return validate(value);
    }

    /**
     * Determines if the domain is valid
     * 
     * @param domain
     *            domain string to test
     * @return true if the domain string is valid.
     */
    public boolean validate(String domain) {
        StringTokenizer tok;

        tok = new StringTokenizer(domain, ".");
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

    /**
     * Check if the given domain part is valid
     */
    private boolean isValidDomainPart(String dp) {
        char ch;
        Character character;

        StringBuilder buf = new StringBuilder(dp);

        // subdomain can not start or end with a hyphen
        if (buf.charAt(0) == '-' || buf.charAt(buf.length() - 1) == '-') {
            return false;
        }

        // subdomain or "label" can not be longer than 63 chars
        if (buf.length() > 63) {
            return false;
        }

        for (int i = 0; i < buf.length(); i++) {

            ch = buf.charAt(i);
            character = new Character(ch);

            if (!validDomainChars.contains(character.toString())) {
                return false;
            }
        }
        return true;
    }

    private static Vector validDomainChars = new Vector();

    static {
        validDomainChars.addElement("a");
        validDomainChars.addElement("b");
        validDomainChars.addElement("c");
        validDomainChars.addElement("d");
        validDomainChars.addElement("e");
        validDomainChars.addElement("f");
        validDomainChars.addElement("g");
        validDomainChars.addElement("h");
        validDomainChars.addElement("i");
        validDomainChars.addElement("j");
        validDomainChars.addElement("k");
        validDomainChars.addElement("l");
        validDomainChars.addElement("m");
        validDomainChars.addElement("n");
        validDomainChars.addElement("o");
        validDomainChars.addElement("p");
        validDomainChars.addElement("q");
        validDomainChars.addElement("r");
        validDomainChars.addElement("s");
        validDomainChars.addElement("t");
        validDomainChars.addElement("u");
        validDomainChars.addElement("v");
        validDomainChars.addElement("w");
        validDomainChars.addElement("x");
        validDomainChars.addElement("y");
        validDomainChars.addElement("z");
        validDomainChars.addElement("A");
        validDomainChars.addElement("B");
        validDomainChars.addElement("C");
        validDomainChars.addElement("D");
        validDomainChars.addElement("E");
        validDomainChars.addElement("F");
        validDomainChars.addElement("G");
        validDomainChars.addElement("H");
        validDomainChars.addElement("I");
        validDomainChars.addElement("J");
        validDomainChars.addElement("K");
        validDomainChars.addElement("L");
        validDomainChars.addElement("M");
        validDomainChars.addElement("N");
        validDomainChars.addElement("O");
        validDomainChars.addElement("P");
        validDomainChars.addElement("Q");
        validDomainChars.addElement("R");
        validDomainChars.addElement("S");
        validDomainChars.addElement("T");
        validDomainChars.addElement("U");
        validDomainChars.addElement("V");
        validDomainChars.addElement("W");
        validDomainChars.addElement("X");
        validDomainChars.addElement("Y");
        validDomainChars.addElement("Z");
        validDomainChars.addElement("-");
        validDomainChars.addElement("_");
        validDomainChars.addElement("0");
        validDomainChars.addElement("1");
        validDomainChars.addElement("2");
        validDomainChars.addElement("3");
        validDomainChars.addElement("4");
        validDomainChars.addElement("5");
        validDomainChars.addElement("6");
        validDomainChars.addElement("7");
        validDomainChars.addElement("8");
        validDomainChars.addElement("9");
    }

}
