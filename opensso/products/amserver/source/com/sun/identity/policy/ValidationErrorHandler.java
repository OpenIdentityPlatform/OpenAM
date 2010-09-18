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
 * $Id: ValidationErrorHandler.java,v 1.2 2008/06/25 05:43:45 qcheng Exp $
 *
 */

package com.sun.identity.policy;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Class to take care of XML validation errors.
 */
class ValidationErrorHandler implements ErrorHandler {
    /**
     * Handles fatal error.
     *
     * @param spe SAX Parse Exception.
     */
    public void fatalError(SAXParseException spe)
        throws SAXParseException {
        PolicyManager.debug.error(spe.getMessage() +
            "\nLine Number in XML file : " + spe.getLineNumber() +
            "\nColumn Number in XML file : " + spe.getColumnNumber());
    }

    /**
     * Handles error.
     *
     * @param spe SAX Parse Exception.
     */
    public void error(SAXParseException spe)
        throws SAXParseException {
        PolicyManager.debug.error(spe.getMessage() +
            "\nLine Number in XML file : " + spe.getLineNumber() +
            "\nColumn Number in XML file : " + spe.getColumnNumber());
        throw spe;
    }

    /**
     * Handles warning.
     *
     * @param spe SAX Parse Exception.
     */
    public void warning(SAXParseException spe)
        throws SAXParseException {
        if (PolicyManager.debug.warningEnabled()) {
            PolicyManager.debug.warning(spe.getMessage() +
                "\nLine Number in XML file : " + spe.getLineNumber() +  
                "\nColumn Number in XML file : " + spe.getColumnNumber());
        }
    }
}
