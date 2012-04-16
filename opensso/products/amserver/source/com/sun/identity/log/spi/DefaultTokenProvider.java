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
 * $Id: DefaultTokenProvider.java,v 1.3 2008/06/25 05:43:40 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

/**
 * This class implements <code>ITokenProvider</code> interface and
 * representing default sso token provider.
 */
public class DefaultTokenProvider implements ITokenProvider{
    /**
     * Construct the <code>DefaultTokenProvider</code>
     */
    public DefaultTokenProvider(){
    }
    
    /**
     * Return <code>Object</code> 
     * Given paramers are not used.
     * @param name user name
     * @param Password password for user
     */
    public Object createToken(String name, String Password){
        return new Object();
    }
}

