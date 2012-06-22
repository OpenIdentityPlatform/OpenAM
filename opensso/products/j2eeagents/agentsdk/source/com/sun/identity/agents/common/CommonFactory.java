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
 * $Id: CommonFactory.java,v 1.6 2009/05/26 22:47:57 leiming Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.common;


import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.ServiceResolver;
import java.util.Set;


/**
 * Class CommonFactory is a factory class to get different types of providers
 * based on the configured <code>ServiceResolver</code>.
 */
public class CommonFactory {
    
    public CommonFactory(Module module) {
        setModule(module);
    }
    
    public IApplicationSSOTokenProvider newApplicationSSOTokenProvider()
    throws AgentException {
        IApplicationSSOTokenProvider result = null;
        String className = getResolver().getApplicationSSOTokenProviderImpl();
        try {
            result = (IApplicationSSOTokenProvider) getObject(className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IApplicationSSOTokenProvider: "
                    + className, ex);
        }
        return result;
    }
    
    public IProfileAttributeHelper newProfileAttributeHelper()
    throws AgentException {
        IProfileAttributeHelper result = null;
        String className = getResolver().getProfileAttributeHelperImpl();
        try {
            result = (IProfileAttributeHelper) getObject(className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IProfileAttributeHelper: "
                    + className, ex);
        }
        return result;
    }
    
    public ISSOTokenValidator newSSOTokenValidator()
            throws AgentException {
        ISSOTokenValidator result = null;
        String className = getResolver().getSSOTokenValidatorImpl();
        try {
            result = (ISSOTokenValidator) getObject(className);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize ISSOTokenValidator: "
                    + className, ex);
        }
        return result;
    }

    public ILibertyAuthnResponseHelper newLibertyAuthnResponseHelper(
            int skewFactor) throws AgentException 
    {
        ILibertyAuthnResponseHelper result = null;
        String className = getResolver().getLibertyAuthnResponseHelperImpl();
        try {
            result = (ILibertyAuthnResponseHelper) getObject(className);
            result.initialize(skewFactor);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IURLFailoverHelper: "
                    + className, ex);
        }        
        return result;
    }
    

    public IURLFailoverHelper newURLFailoverHelper(
            boolean probeEnabled,
            boolean isPrioritized, 
            long timeout, 
            String[] urlList,
            Map<String, Set<String>> conditionalUrls)
    	throws AgentException
    {
        IURLFailoverHelper result = null;
        String className = getResolver().getURLFailoverHelperImpl();
        try {
            result = (IURLFailoverHelper) getObject(className);
            result.initialize(probeEnabled, isPrioritized, timeout, urlList, conditionalUrls);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IURLFailoverHelper: "
                    + className, ex);
        }
        return result;
    }
    
    
    public IHttpServletRequestHelper newServletRequestHelper(
            String dateFormatString, Map attributeMap)
            throws AgentException {
        return newServletRequestHelper(dateFormatString, attributeMap, null);
    }
    
    public IHttpServletRequestHelper newServletRequestHelper(
            String dateFormatString, Map attributeMap,
            ServletInputStream inputStream)
            throws AgentException {
        IHttpServletRequestHelper result = null;
        String className = getResolver().getHttpServletRequestHelperImpl();
        try {
            result = (IHttpServletRequestHelper) getObject(className);
            result.initialize(dateFormatString, attributeMap, inputStream);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IHttpServletRequestHelper: "
                    + className, ex);
        }
        return result;
    }
    
    public IFQDNHelper newFQDNHelper(String defaultFQDN, Map fqdnMap)
    throws AgentException {
        IFQDNHelper result = null;
        String className = getResolver().getFQDNHelperImpl();
        try {
            result = (IFQDNHelper) getObject(className);
            result.initialize(defaultFQDN, fqdnMap);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize IFQDNHelper: "
                    + className, ex);
        }
        return result;
    }
    
    public INotenforcedURIHelper newNotenforcedURIHelper(
            boolean isInverted, boolean cacheEnabled,
            int maxSize, String[] notenforcedURIEntries) throws AgentException {
        INotenforcedURIHelper result = null;
        String className = getResolver().getNotenforcedURIHelperImpl();
        try {
            result = (INotenforcedURIHelper) getObject(className);
            result.initialize(isInverted, cacheEnabled,
                    maxSize, notenforcedURIEntries);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize INotenforcedURIHelper: "
                    + className, ex);
        }
        return result;
    }
    
    public ICookieResetHelper newCookieResetHelper(
            ICookieResetInitializer cookieResetInitializer) 
            throws AgentException {
        ICookieResetHelper result = null;
        String className = getResolver().getCookieResetHelperImpl();
        try {
            result = (ICookieResetHelper) getObject(className);
            result.initialize(cookieResetInitializer);
        } catch (Exception ex) {
            throw new AgentException("Unable to initialize ICookieResetHelper: "
                    + className, ex);
        }
        return result;
    }
    
    public IPatternMatcher newPatternMatcher(String[] patternList)
    throws AgentException {
        IPatternMatcher result = null;
        String className = getResolver().getPatternMatcherImpl();
        try {
            result = (IPatternMatcher) getObject(className);
            result.initialize(patternList);
        } catch (Exception ex) {
            throw new AgentException("Unable to initialize IPatternMatcher: "
                    + className, ex);
        }
        return result;
    }

    public IPatternMatcher newURLPatternMatcher(String[] patternList)
    throws AgentException {
        IPatternMatcher result = null;
        String className = getResolver().getURLPatternMatcherImpl();
        try {
            result = (IPatternMatcher) getObject(className);
            result.initialize(patternList);
        } catch (Exception ex) {
            throw new AgentException("Unable to initialize IPatternMatcher: "
                    + className, ex);
        }
        return result;
    }

    public INotenforcedIPHelper newNotenforcedIPHelper(
            boolean cacheEnabled, int maxSize,
            boolean invertList, String[] notenforcedIPs)
            throws AgentException {
        INotenforcedIPHelper result = null;
        String className = getResolver().getNotenforcedIPHelperImpl();
        try {
            result = (INotenforcedIPHelper) getObject(className);
            result.initialize(cacheEnabled, maxSize, invertList,
                    notenforcedIPs);
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize INotenforcedIPHelper: "
                    + className, ex);
        }
        return result;
    }
    
    private Object getObject(String className) throws Exception {
        Constructor c = getConstructor(className);
        return c.newInstance(new Object[] { getModule() });
    }
    
    private Constructor getConstructor(String className) throws Exception {
        Constructor c = (Constructor) getConstructorCache().get(className);
        if (c == null) {
            c = getConstructorInternal(className);
        }
        return c;
    }
    
    private synchronized Constructor getConstructorInternal(String className)
    throws Exception {
        Constructor c = (Constructor) getConstructorCache().get(className);
        if (c == null) {
            Class xclass = Class.forName(className);
            c = xclass.getConstructor(new Class[] { Module.class });
            if (c == null) {
                throw new Exception("Unable to find appropriate constructor: "
                        + className);
            }
            getConstructorCache().put(className, c);
        }
        
        return c;
    }
    
    private void setModule(Module module) {
        _module = module;
    }
    
    private Module getModule() {
        return _module;
    }
    
    private ServiceResolver getResolver() {
        return _resolver;
    }
    
    private HashMap getConstructorCache() {
        return _constructorCache;
    }
    
    private Module _module;
    private HashMap _constructorCache = new HashMap();;
    private ServiceResolver _resolver = AgentConfiguration.getServiceResolver();
}
