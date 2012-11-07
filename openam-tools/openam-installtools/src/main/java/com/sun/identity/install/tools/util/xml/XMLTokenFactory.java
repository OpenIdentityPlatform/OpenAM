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
 * $Id: XMLTokenFactory.java,v 1.2 2008/06/25 05:51:32 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * A token factory is responsible for the creation of the associated token
 * handlers or <code>Token</code> objects given string fragments as parsed
 * from the XML document source. This factory is used by the parser to generate
 * the tokens when strings matching the syntactic rules of XML are identified
 * during the first parsing or scanning phase. The token factory is also
 * responsible for assigning unique identification index numbers to tokens that
 * is used by the parser and later by the document itself in order to create 
 * new elements for addition as necessary.
 */
public class XMLTokenFactory implements IXMLUtilsConstants {

    /**
     * Returns a <code>Token</code> handler which can be used to model the
     * given token string.
     * 
     * @param tokenString
     *            the string fragment that needs to be processed for the second
     *            phase of parsing
     * 
     * @return a <code>Token</code> object which is nothing but a handler for
     *         the given string fragment which can be used to further handle 
     *         the given string framgment.
     * 
     * @throws Exception
     *             if the identification of the appropriate token handler
     *             fails.
     */
    Token getToken(String tokenString) throws Exception {
        Token result = null;
        if (tokenString != null) {
            if (tokenString.startsWith("<") && tokenString.endsWith(">")) {
                if (tokenString.startsWith("<!")) {
                    if (tokenString.startsWith("<!--")
                            && tokenString.endsWith("-->")) {
                        result = new CommentToken(tokenString);
                    } else {
                        String innerString = tokenString.substring(2,
                                tokenString.length() - 1);
                        if (innerString.trim().startsWith(DOCTYPE)) {
                            result = new DoctypeToken(tokenString);
                        } else {
                            result = new MetaToken(tokenString);
                        }
                    }
                } else if (tokenString.startsWith("<?")) {
                    result = new MetaToken(tokenString);
                } else {
                    result = new BoundedToken(tokenString);
                }
            } else if (!tokenString.startsWith("<")
                    && !tokenString.endsWith(">")) {
                if (tokenString.trim().length() == 0) {
                    result = new WhiteSpaceToken(tokenString);
                } else {
                    result = new UnboundedToken(tokenString);
                }
            } else {
                throw new Exception("Invalid token String: " + tokenString);
            }
        } else {
            throw new Exception("Null token string");
        }

        if (result != null) {
            result.setTokenIndex(getNextTokenIndex());
        }
        return result;
    }

    /**
     * Assigns a unique index number for the next token handler.
     * 
     * @return a unique index used for identifying the token in the actual
     *         source document.
     */
    int getNextTokenIndex() {
        return tokenIndex++;
    }

    private int tokenIndex = 0;
}
