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
 * $Id: PolicyDecisionCacheListener.java,v 1.4 2009/03/18 17:51:31 dillidorai Exp $
 *
 */



package com.sun.identity.policy;

import java.util.*;
import com.sun.identity.policy.interfaces.PolicyListener;
import com.sun.identity.policy.interfaces.ResourceName;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>PolicyDecisionCacheListener</code> implements
 * PolicyListener interface. It listens on for changes in the
 * policies as sent out by the policy framework and cleans up the
 * policy decision cache for affected resources.
 */

public class PolicyDecisionCacheListener 
             implements PolicyListener {

    private static Map resultsCache = PolicyEvaluator.policyResultsCache;
    private static Debug debug = PolicyManager.debug;

    private static final String resourceWildcard = "*";
    private static final String resourceDelimiter = "/";
    private static final String resourceCase = "false";
    private ResourceName resourceNameUtil = null;
    private String serviceName = null;

    /**
     * Constructor of <code>PolicyDecisionCacheListener</code>
     * @param  serviceName name of the service for which this object is being
     *         created.
     */

    public PolicyDecisionCacheListener(String serviceName) {

        this.serviceName = serviceName;

        Map resourceMap = null;
        String className = null;

        try {
            // get resource comparator configuration information
            resourceMap = PolicyConfig.getResourceCompareConfig(serviceName);
            if (resourceMap != null) {
                className = (String)resourceMap.get(PolicyConfig.
                    RESOURCE_COMPARATOR_CLASS);
            } else {
                resourceMap = new HashMap();
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_DELIMITER, 
                    resourceDelimiter);
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_WILDCARD, 
                    resourceWildcard);
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_CASE_SENSITIVE,
                     resourceCase);
            }
            if (className != null) {
                Class resourceClass = Class.forName(className);
                resourceNameUtil = (ResourceName) resourceClass.newInstance();
                resourceNameUtil.initialize(resourceMap);
            }
        } catch (Exception e) {
            debug.error("PolicyDecisionCacheListener: failed to get a resource "
                +"comparator", e);
        }
    }



    /**
     * Sets the service name on which this listener listens
     * @param name the service name
     */

    public void setServiceTypeName(String name)
    {
        serviceName = name;
    }


    /**
     * The implementation for the PolicyListener interface method
     * gets the service name on which the listener listens.
     * @return service name
     */

    public String getServiceTypeName()
    {
        return serviceName;
    }


   
    /**
     * This is the callback implementation for the PolicyListener
     * which gets called when a policy changes. On getting called
     * this method drops the dirty cache for the affected resources.
     * @param evt <code>PolicyEvent</code> indicating the resource
     *        names which got affected.
     */
    public synchronized void policyChanged(PolicyEvent evt)
    {
        if (debug.messageEnabled()) {
            debug.message("PolicyDecisionCacheListener.policyChanged()");
        }
        if (evt == null) {
            debug.error("PolicyDecisionCacheListener.policyChanged(): "
                +"invalid policy event");
            return;
        }

        /* get the resource names from the event */
        Set resourceNames = evt.getResourceNames();
        if (debug.messageEnabled()) {
            debug.message("PolicyDecisionCacheListener.policyChanged(): "
                +"resource names from the policy event :" 
                + resourceNames.toString());
        }

        // update the policy decision cache
        if (!(resultsCache.isEmpty()) && !(resourceNames.isEmpty())) {
            Map svcValue = (Map)resultsCache.get(serviceName);
            if (svcValue != null) { 
                Set rscInCache = svcValue.keySet();
                Iterator rscInCacheIter = rscInCache.iterator();
                while (rscInCacheIter.hasNext()) {
                    String rscValueInCache = (String)rscInCacheIter.next();
                    Iterator resourceNamesIter = resourceNames.iterator();
                    while (resourceNamesIter.hasNext()) {
                        String resourceNameValue = 
                            (String)resourceNamesIter.next();
                        ResourceMatch match =
                            resourceNameUtil.compare(rscValueInCache,
                                                   resourceNameValue,
                                                   true);
                        if (!(match.equals(ResourceMatch.NO_MATCH))) {
                            // wipe out the cache for this resource
                            rscInCacheIter.remove();
                            if (debug.messageEnabled()) {
                                debug.message("PolicyDecisionCacheListener."
                                    +"policyChanged(): cache wiped out for " 
                                    + rscValueInCache);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
