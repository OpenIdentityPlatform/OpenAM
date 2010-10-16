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
 * $Id: AuthenticationCallbackXMLHelper.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */


package com.sun.identity.authentication.share;

import javax.security.auth.callback.Callback;
import org.w3c.dom.Node;

/**
 * Defines the interface that allows Authentication service to implement the
 * XML processing of required Authentication Callback during Authentication
 * remote SDK execution.
 * The implementation of this interface is determined during runtime.
 *
 * @see AuthenticationCallbackXMLHelperFactory
 */

public interface AuthenticationCallbackXMLHelper {
    
    /**
     * Returns the XML string representing the Authentication Callback.
     *
     * @param callback Authentication <code>Callback</code>.
     * @return XML string representing the Authentication <code>Callback</code>.
     */
    String getAuthenticationCallbackXML(Callback callback);
    
    /**
     * Creates the Authentication <code>Callback</code>
     * from its XML document.
     *
     * @param childNode XML document node.
     * @param callback Authentication <code>Callback</code>.
     * @return Authentication <code>Callback</code> from its XML document
     * representation.
     */
    Callback createAuthenticationCallback(Node childNode, Callback callback);
    
    /**
     * Returns the Authentication <code>Callback</code> index in the 
     * <code>Callback</code> array.
     *
     * @param callbacks Array of <code>Callback</code>.
     * @param startIndex starting index in the <code>Callback</code> array.
     * @return integer as the Authentication <code>Callback</code> index in the 
     * <code>Callback</code> array.
     */
    int getAuthenticationCallbackIndex(Callback[] callbacks, int startIndex);
    
}
