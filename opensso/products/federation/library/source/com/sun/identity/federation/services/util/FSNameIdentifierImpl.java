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
 * $Id: FSNameIdentifierImpl.java,v 1.2 2008/06/25 05:47:04 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.util;

import java.security.SecureRandom;
import com.sun.liberty.INameIdentifier;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.shared.encode.Base64;

/**
 * Default plugin for interface <code>INameIdentifier</code>.
 */
public class FSNameIdentifierImpl implements INameIdentifier {
    private static SecureRandom randomGenerator = new SecureRandom();
    
    /**
     * Default constructor.
     */
    public FSNameIdentifierImpl() {
    }

    /**
     * Creates a new name identifier string.
     * @return name identifier string; <code>null</code> if an error occurred
     *  during the creation process. 
     */
    public String createNameIdentifier(){
        try {
            FSUtils.debug.message(
                "NameIdentifierImpl.createNameIdentifier: Called");
            byte[] handleBytes = new byte[21];
            randomGenerator.nextBytes(handleBytes);
            if (handleBytes == null){
                FSUtils.debug.error("NameIdentifierImpl.createNameIdentifier:" 
                    + "Could not generate random handle");
                return null;
            }
            String handle = Base64.encode(handleBytes);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("NameIdentifierImpl.createNameIdentifier:"
                    + " String: " + handle);
            }
            return handle;
        } catch (Exception exp) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("NameIdentifierImpl.createNameIdentifier:"
                    + " Exception during proccessing request ", exp);
            }
            return null;
        }
    }
}
