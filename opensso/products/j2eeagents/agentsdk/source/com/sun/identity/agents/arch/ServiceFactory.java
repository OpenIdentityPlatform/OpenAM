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
 * $Id: ServiceFactory.java,v 1.4 2008/06/25 05:51:37 qcheng Exp $
 *
 */
 /*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.agents.arch;

import com.sun.identity.agents.common.IPDPCache;
import com.sun.identity.agents.common.IPDPCacheEntry;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.IAmFilter;
import com.sun.identity.agents.filter.IAmFilterResultHandler;
import com.sun.identity.agents.filter.IAmFilterTaskHandler;
import com.sun.identity.agents.filter.IAmSSOCache;
import com.sun.identity.agents.filter.ICDSSOContext;
import com.sun.identity.agents.filter.ISSOContext;
import com.sun.identity.agents.log.IAmAgentLocalLog;
import com.sun.identity.agents.log.IAmAgentLog;
import com.sun.identity.agents.log.IAmAgentRemoteLog;
import com.sun.identity.agents.policy.IAmWebPolicy;
import com.sun.identity.agents.policy.IAmWebPolicyAppSSOProvider;
import com.sun.identity.agents.realm.IAmRealm;

/**
 * Helper Factory to instantiate service classes.
 */
public class ServiceFactory {
    
    public static ArrayList getFilterInboundTaskHandlers(Manager manager, 
           ISSOContext context, AmFilterMode mode, boolean cdssoEnabled) 
    throws AgentException 
    {
        ArrayList result = new ArrayList();
        ArrayList impls = getResolver().getFilterInboundTaskHandlerImpls(
                mode, cdssoEnabled);
        if (impls != null && impls.size() > 0) {
            try {
                Iterator it = impls.iterator();
                    while (it.hasNext()) {
                        String nextImpl = (String) it.next();
                        IAmFilterTaskHandler instance = (IAmFilterTaskHandler)
                                getServiceInstance(manager, nextImpl);
                        instance.initialize(context, mode);
                        result.add(instance);
                    }
            } catch (Exception ex) {
                throw new AgentException(
                        "Unable to load filter inbound task handlers", ex);
            }
        }
        
        return result;
    }
        
    public static ArrayList getFilterSelfRedirectTaskHandlers(Manager manager, 
            ISSOContext context, AmFilterMode mode, boolean cdssoEnabled) 
    throws AgentException 
    {
        ArrayList result = new ArrayList();
        ArrayList impls = getResolver().getFilterSelfRedirectTaskHandlerImpls(
                mode, cdssoEnabled);
        if (impls != null && impls.size() > 0) {
            try {
                Iterator it = impls.iterator();
                    while (it.hasNext()) {
                        String nextImpl = (String) it.next();
                        IAmFilterTaskHandler instance = (IAmFilterTaskHandler)
                                getServiceInstance(manager, nextImpl);
                        instance.initialize(context, mode);
                        result.add(instance);
                    }
            } catch (Exception ex) {
                throw new AgentException(
                       "Unable to load filter self-redirect task handlers", ex);
            }
        }
        
        return result;
    }
    
    public static ArrayList getFilterResultHandlers(Manager manager,
            ISSOContext context, AmFilterMode mode, boolean cdssoEnabled) 
    throws AgentException 
    {
        ArrayList result = new ArrayList();
        ArrayList impls = getResolver().getFilterResultHandlerImpls(
            mode, cdssoEnabled);
        if (impls != null && impls.size() > 0) {
            try {
                Iterator it = impls.iterator();
                    while (it.hasNext()) {
                        String nextImpl = (String) it.next();
                        IAmFilterResultHandler instance = 
                            (IAmFilterResultHandler)
                            getServiceInstance(manager, nextImpl);
                        instance.initialize(context, mode);
                        result.add(instance);
                    }
            } catch (Exception ex) {
                throw new AgentException(
                        "Unable to load filter result handlers", ex);
            }
        }
        return result; 
    }    
    
    public static IAmFilter getAmFilter(Manager manager, AmFilterMode mode) 
            throws AgentException
    {
        IAmFilter result = null;
        String className = 
            getResolver().getAmFilterImpl();
        try {
            result = (IAmFilter) getServiceInstance(manager, className);
            result.initialize(mode);
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmFilter: " 
                    + className, ex);
        }        
        return result;
    }
    
    public static IAmRealm getAmRealm(Manager manager) throws AgentException {
        IAmRealm result = null;
        String className = getResolver().getAmRealmImpl();
        try {
            result = (IAmRealm) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmRealm: " 
                    + className, ex);
        }
        return result;
    }

    public static ICDSSOContext getCDSSOContext(Manager manager, 
            AmFilterMode filterMode) throws AgentException 
    {
        ICDSSOContext result = null;
        String className = getResolver().getCDSSOContextImpl();
        try {
            result = (ICDSSOContext) getServiceInstance(manager, className);
            result.initialize(filterMode);
        } catch (Exception ex) {
            throw new AgentException("Unable to load ICDSSOContext: " 
                    + className, ex);
        }
        return result;
    }
    
    
    public static IAmSSOCache getAmSSOCache(Manager manager) 
                    throws AgentException {
        IAmSSOCache result = null;
        String className = getResolver().getAmSSOCacheImpl();
        try {
            result = (IAmSSOCache) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmSSOCache: " 
                    + className, ex);
        }
        return result;
    }

    public static IPDPCache getPDPCache(Manager manager)
                    throws AgentException {
        IPDPCache result = null;
        String className = getResolver().getPDPCacheImpl();
        try {
            result = (IPDPCache) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IPDPCache: "
                    + className, ex);
        }
        return result;
    }

    public static IPDPCacheEntry getPDPCacheEntry(Manager manager)
                    throws AgentException {
        IPDPCacheEntry result = null;
        String className = getResolver().getPDPCacheEntryImpl();
        try {
            result = (IPDPCacheEntry) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IPDPCacheEntry: "
                    + className, ex);
        }
        return result;
    }

    public static ISSOContext getSSOContext(Manager manager, 
            AmFilterMode filterMode) throws AgentException 
    {
        ISSOContext result = null;
        String className = getResolver().getSSOContextImpl();
        try {
            result = (ISSOContext) getServiceInstance(manager, className);
            result.initialize(filterMode);
        } catch (Exception ex) {
            throw new AgentException("Unable to load ISSOContext: " 
                    + className, ex);
        }
        return result;
    }
    
    public static IAmWebPolicyAppSSOProvider getAmWebPolicyAppSSOProvider(
            Manager manager) throws AgentException
        {
        IAmWebPolicyAppSSOProvider result = null;
        String className = getResolver().getAmWebPolicyAppSSOProviderImpl();
        try {
            result = (IAmWebPolicyAppSSOProvider) 
                                    getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to load IAmWebPolicyAppSSOProvider: "
                    + className, ex);
        }
        return result;
        }
    
  
    public static ICrypt getCryptProvider() throws AgentException {
            
        ICrypt result = null;
        String className = getResolver().getCryptImpl();
        try {
            result = (ICrypt) getServiceInstance(className);
        } catch (Exception ex) {
            throw new AgentException("Unable to load ICrypt: " 
                    + className, ex);
        }
        return result;
    }
    
    public static IAmWebPolicy getAmWebPolicy(Manager manager) 
    throws AgentException 
    {
        IAmWebPolicy result = null;
        String className = getResolver().getAmWebPolicyImpl();
        try {
            result = (IAmWebPolicy) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmWebPolicy: "
                    + className, ex);
        }
        return result;
    }
    
    public static IAmAgentLog getAmAgentLog(Manager manager) 
    throws AgentException 
    {
        IAmAgentLog result = null;
        String className = getResolver().getAmAgentLogImpl();
        try {
            result = (IAmAgentLog) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmAgentLog: "
                    + className, ex);
        }
        return result;        
    }
    
    public static IAmAgentRemoteLog getAmAgentRemoteLog(Manager manager) 
    throws AgentException
    {
        IAmAgentRemoteLog result = null;
        String className = getResolver().getAmAgentRemoteLogImpl();
        try {
            result = (IAmAgentRemoteLog) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmAgentRemoteLog: "
                    + className, ex);
        }
        return result;        
    }
    
    public static IAmAgentLocalLog getAmAgentLocalLog(Manager manager) 
    throws AgentException
    {
        IAmAgentLocalLog result = null;
        String className = getResolver().getAmAgentLocalLogImpl();
        try {
            result = (IAmAgentLocalLog) getServiceInstance(manager, className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Unable to load IAmAgentLocalLog: "
                    + className, ex);
        }
        return result;
    }
    
    public static Object getServiceInstance(Manager manager, String className) 
            throws Exception
    {
        Object result = null;
        manager.getModule().logMessage("ServiceFactory: Loading class: "
                                       + className);
        Constructor c = getConstructor(className);
        result = c.newInstance(new Object[] { manager });
        return result;
    }
    
    private static Constructor getConstructor(String className) 
    throws Exception 
    {
        Constructor c = (Constructor) getConstructorCache().get(className);
        if (c == null) {
            c = getConstructorInternal(className);
        }
        return c;
    }
   
    
    private static synchronized Constructor getConstructorInternal(
            String className) throws Exception
        {
        Constructor c = (Constructor) getConstructorCache().get(className);
            if (c == null) {
                Class xclass = Class.forName(className);
                c = xclass.getConstructor(new Class[] { Manager.class });
                if (c == null) {
                    throw new Exception(
                        "Unable to find appropriate constructor: "
                        + className);
                }
                getConstructorCache().put(className, c);
            }
    
            return c;
        }
    
    private static Object getServiceInstance(String className) 
                throws Exception
    {
            return getObject(className);
    }
    
    
    private static Object getObject(String className) 
            throws Exception 
    {
        Object o = (Object) getObjectCache().get(className);
        if (o == null) {
            o = getObjectInternal(className);
        }
        return o;
    }
    
    private static synchronized Object getObjectInternal(
            String className) throws Exception
    {
            Object o = Class.forName(className).newInstance();
            if (o == null) {
            throw new Exception("Unable to instantiate appropriate class: "
            + className);
            }
            getObjectCache().put(className, o);
   
            return o;
    }
    
    private static ServiceResolver getResolver() {
        return _resolver;
    }

    private static Hashtable getConstructorCache() {
        return _constructorCache;
    }
    
    private static Hashtable getObjectCache() {
        return _objectCache;
    }
    
    private static Hashtable _constructorCache = new Hashtable();
    private static Hashtable _objectCache = new Hashtable();
    private static ServiceResolver _resolver = 
    AgentConfiguration.getServiceResolver();
}
