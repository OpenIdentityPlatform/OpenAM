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
 * $Id: ClearTextTransform.java,v 1.2 2008/06/25 05:41:57 qcheng Exp $
 *
 */


package com.sun.identity.authentication.modules.jdbc;

import com.sun.identity.authentication.spi.AuthLoginException;
   
/**
 * A very simple test implementation of the JDBC Password Syntax Transform.
 */
public class ClearTextTransform implements JDBCPasswordSyntaxTransform  {
    /** 
     * Creates a new instance of <code>ClearTextTransform</code>. 
     */
    public ClearTextTransform() {
    }
    
    /** 
     * This simply returns the clear text format of the password. 
     *
     * @param input Password before transform
     * @return Password after transform in this case the same thing.
     * @throws AuthLoginException
     */  
    public String transform(String input) throws AuthLoginException {
        if (input == null) {
            throw new AuthLoginException(
                "No input to the Clear Text Transform!");
        }
        return input;
    }
}
