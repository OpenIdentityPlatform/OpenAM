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
 * $Id: PolicySSOTokenListener.java,v 1.4 2008/06/25 05:43:44 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenEvent;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.policy.plugins.LDAPRoles;

/**
 * The class <code>PolicySSOTokenListener</code> implements
 * SSOTokenListener interface and is used for maintaining the 
 * policy decision cache , subject evaluation cache, User
 * Role cache maintained by <code>LDAPRoles</code> subject,
 * as well as the user </code>nsRole</code> attribute values cache.
 */

public class PolicySSOTokenListener 
             implements SSOTokenListener {

    private static Map resultsCache = 
                              PolicyEvaluator.policyResultsCache;
    private static Debug debug = PolicyManager.debug;

    /**
     *  Constructor of <code>PolicySSOTokenListener</code>
     */

    public PolicySSOTokenListener() {
    }

    /**
     *  Callback for SSOTokenListener
     *  Cleans up the policy decision cache, subject evaluation cache ,
     *  user role cache of LDAPRoles and user <code>nsRole</code> attribute 
     *  values cache upon user's token expiration.
     *  @param evt <code>SSOTokenEvent</code> with details on the change
     *         which happened to the <code>SSOToken</code>
     */
    public void ssoTokenChanged(SSOTokenEvent evt)
    {
        try {
            SSOTokenID tokenId = evt.getToken().getTokenID();        
            String tokenIdStr = tokenId.toString();
            if (tokenIdStr == null) {
                debug.error("PolicySSOTokenListener: " 
                            + "token id string is null");
                return;
            }
        
            // update the policy decision cache
            synchronized(PolicyEvaluator.policyResultsCache) {
                if (!(resultsCache.isEmpty())) {
                    Set svcInCache = resultsCache.keySet(); 
                    Iterator svcInCacheIter = svcInCache.iterator();
                    while (svcInCacheIter.hasNext()) {
                        String svcName = (String)svcInCacheIter.next();
                        Map svcValue = (Map)resultsCache.get(svcName);
                        if ((svcValue != null)&&(!(svcValue.isEmpty()))) {
                            Set rscInCache = svcValue.keySet(); 
                            Iterator rscInCacheIter = rscInCache.iterator();
                            while (rscInCacheIter.hasNext()) {
                                String rscName = (String)rscInCacheIter.next();
                                Map rscValues = (Map)svcValue.get(rscName);
                                if ((rscValues != null)
                                    && (!(rscValues.isEmpty()))) {
                                    if ((rscValues.remove(tokenIdStr)) 
                                          != null) { 
                                        if (debug.messageEnabled()) {
                                            debug.message("cleaned up the "
                                                +"policy results for an "
                                                +"expired token " + tokenIdStr);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //clean up userNSRoleCache
            PolicyEvaluator.userNSRoleCache.remove(tokenIdStr);
            if (debug.messageEnabled()) {
                debug.message("PolicySSOTokenListener.ssoTokenChanged():"
                    +"cleaned up user nsRole cache for an expired token " 
                    + tokenIdStr);
            }

            // clean up the subject evaluation cache
            SubjectEvaluationCache.subjectEvaluationCache.remove(tokenIdStr);
            if (debug.messageEnabled()) {
                debug.message("PolicySSOTokenListener.ssoTokenChanged():"
                    +"cleaned up subject evaluation cache for an expired token" 
                    +" "+tokenIdStr);
            }

            // clean up the user role cache from LDAPRoles
            LDAPRoles.userLDAPRoleCache.remove(tokenIdStr);
            if (debug.messageEnabled()) {
                debug.message("PolicySSOTokenListener.ssoTokenChanged()cleaned "
                    +"up user role cache of LDAPRoles "
                    + "for an expired token "+tokenIdStr);
            }

            // clean up subject result cache inside  Policy objects
            if (evt.getType() == SSOTokenEvent.SSO_TOKEN_PROPERTY_CHANGED) {
                if (debug.messageEnabled()) {
                    debug.message("PolicySSOTokenListener.ssoTokenChanged():"
                        + " receieved sso token property change notification, "
                        + " clearing cached subject result cache "
                        + " for tokenIdStr XXXXXX");
                }
                PolicyCache.getInstance().clearSubjectResultCache(tokenIdStr);
            }

            PolicyEvaluator.ssoListenerRegistry.remove(tokenIdStr);
        } catch (Exception e ) {
            debug.error("PolicySSOTokenListener.ssoTokenChanged():policy sso "
                +"token listener", e);
        }
    }
}

