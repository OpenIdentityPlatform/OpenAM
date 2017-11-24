/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMSTSAuthorizationProvider.java,v 1.3 2008/08/27 19:05:53 mrudul_uchil Exp $
 *
 */
 
package com.sun.identity.wss.sts.spi;

import com.sun.xml.wss.SubjectAccessor;

import java.util.*;

import javax.security.auth.Subject;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.client.PolicyEvaluator;

import com.sun.xml.ws.api.security.trust.STSAuthorizationProvider;

public class FAMSTSAuthorizationProvider implements STSAuthorizationProvider {

    private static SSOToken getSSOToken(Subject subject) {
        Set pc = subject.getPublicCredentials();
        if (pc == null){
            pc = SubjectAccessor.getRequesterSubject().getPublicCredentials();
        }
       
        if (pc != null){
            Iterator ite = pc.iterator();
            while (ite.hasNext()){
                Object obj = ite.next();
                if (obj instanceof com.iplanet.sso.SSOToken){
                    return (SSOToken)obj;
                }
            }
        }
        return null;
    }
    
    public boolean isAuthorized(Subject subject, String appliesTo, 
                                String tokenType, String keyType) {
        return true;
    }  
}
