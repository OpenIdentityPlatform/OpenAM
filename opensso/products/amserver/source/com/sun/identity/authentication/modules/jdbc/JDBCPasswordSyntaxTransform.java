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
 * $Id: JDBCPasswordSyntaxTransform.java,v 1.3 2008/06/25 05:41:57 qcheng Exp $
 *
 */


package com.sun.identity.authentication.modules.jdbc;

import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * This is the interface to implement if you don't store your password 
 * in clear text.
 */
public interface JDBCPasswordSyntaxTransform {
    /**
     * This is the only method to implement if you don't store your password in 
     * cleartext.
     * Take the string the user gave you, and hash it or whatever
     *
     * @param input Password before transform
     * @return Password after transform - in this case the same thing.
     * @throws AuthLoginException
     */
    String transform(String input)
        throws AuthLoginException;
}
