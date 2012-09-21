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
 * $Id: ThreadLocalService.java,v 1.9 2009/11/16 21:52:59 mallas Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import com.iplanet.sso.SSOToken;

/**
 *
 * ThreadLocalservice is a convenient utility class file to store
 * thread local state variables.
 */
public class ThreadLocalService {
    
    private static ThreadLocal ssoToken = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };

    private static ThreadLocal serviceName = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };

    private static ThreadLocal subj = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };

    private static ThreadLocal clientCert = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };
    
    static  synchronized String getServiceName() {
        return (String)serviceName.get();
    }

    static synchronized void setServiceName(String sName) {
        serviceName.set(sName);
    }
    
    static synchronized void removeServiceName(String sName) {
        serviceName.remove();
    }
    
    static  synchronized Object getSSOToken() {
       return ssoToken.get();
    }

    public static synchronized void setSSOToken(Object sToken) {
        ssoToken.set(sToken);
    }
    
    public static synchronized void removeSSOToken(Object sToken) {
        ssoToken.remove();
    }
    
    public static synchronized Object getSubject() {
        return subj.get();
    }

    public static synchronized void setSubject(Object subject) {
        subj.set(subject);
    }
    
    public static synchronized void removeSubject() {
        subj.remove();
    }

    public static synchronized Object getClientCertificate() {
        return clientCert.get();
    }

    public static synchronized void setClientCertificate(Object cert) {
        clientCert.set(cert);
    }

    public static synchronized void removeClientCertificate() {
        clientCert.remove();
    }
}
