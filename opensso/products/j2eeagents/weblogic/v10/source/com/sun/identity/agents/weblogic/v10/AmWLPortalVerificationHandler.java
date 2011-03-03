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
 * $Id: AmWLPortalVerificationHandler.java,v 1.2 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.sun.identity.agents.util.TransportToken;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;

/**
 * Class AmWLPortalVerificationHandler
 *
 * @see This class will be used to verify the presence of an user in
 *      WL10.0 portal repository.
 *
 */
public class AmWLPortalVerificationHandler extends
        GenericExternalVerificationHandler {
    
    /**
     * Method userExists
     *
     * @param userName User trying to authenticate
     * @param tt Transport Token
     * @param data
     *
     * @return true if the user exists, otherwise it's false.
     */
    public boolean verify(String userName, TransportToken tt, Object data) {
        
        boolean result = false;
        IModuleAccess modAccess = AmRealmManager.getModuleAccess();
        
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage("AmWLPortalVerificationHandler.verify - " +
                    "for User Name = " + userName);
        }
        
        try {
            // get and invoke AtnSecurityMgmtHelper.getDefaultProvider();
            Class[] parameterClasses = {};
            Method method = AmWLAgentUtils.getClassMethod(
                    modAccess,
                    "com.bea.p13n.security.management.authentication." +
                    "AtnSecurityMgmtHelper",
                    "getDefaultProvider",
                    parameterClasses);
            Object[] parameters = {};
            Object providerDescription = method.invoke(null, parameters );
            
            // get and invoke new AtnManagerProxy(AtnProviderDescription).
            Class clazz = AmWLAgentUtils.getClass(modAccess,
                    "com.bea.p13n.security.management.authentication." +
                    "AtnProviderDescription");
            parameterClasses = new Class[] { clazz };
            parameters = new Object[] { providerDescription };
            Constructor constructor = AmWLAgentUtils.getConstructorMethod(
                    modAccess,
                    "com.bea.p13n.security.management.authentication." +
                    "AtnManagerProxy",
                    parameterClasses);
            Object managerProxy= constructor.newInstance(parameters);
            
            // get and invoke AtnManagerProxy.userExists().
            parameterClasses = new Class[] { String.class };
            parameters = new Object[] { userName };
            method = AmWLAgentUtils.getClassMethod(
                    modAccess,
                    "com.bea.p13n.security.management.authentication." +
                    "AtnManagerProxy",
                    "userExists",
                    parameterClasses);
            Boolean resultBoolean = 
                    (Boolean) method.invoke(managerProxy, parameters);
            result = resultBoolean.booleanValue();
            
        } catch (Exception ex) {
            if (modAccess.isLogWarningEnabled()) {
                modAccess.logWarning("AmWLPortalVerificationHandler.verify - "
                        + "Programmatic verify Failed! " + ex);
            }
        }
        
        if(result) {
            if (modAccess.isLogMessageEnabled()) {
                modAccess.logMessage(
                        "AmWLPortalVerificationHandler.verify - " +
                        "succeeded to verify User Name = " + userName + 
                        " ,returned result = " +
                        result);
            }
        } else {
            if (modAccess.isLogWarningEnabled()) {
                modAccess.logWarning(
                        "AmWLPortalVerificationHandler.verify - " +
                        "failed to verify User Name = " + userName +
                        " ,returned result = " + result);
                
            }
        }
        
        return result;
    }
    
}
