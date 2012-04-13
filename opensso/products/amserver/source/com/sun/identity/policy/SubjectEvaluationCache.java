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
 * $Id: SubjectEvaluationCache.java,v 1.4 2008/06/25 05:43:45 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import com.sun.identity.sm.ServiceManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;

/* 
 * This class maintains the Subject Evaluation Cache
 * with respect to the membersip of a user to the
 * <code>Subjects</code> specified in a policy. Also maintains a cahce
 * of membership of user across <code>Subject</code>s
 * occuring in different policies.
 */

public class SubjectEvaluationCache {

    /*
    * Cache for subject evaluation indexed on token ID
    * This cache is different from resultCache in Subjects.java.
    * resultsCache is maintained per policy to avoid subject evaluation
    * for a subject while accessing another/same resource protected
    * by an already evaluated policy, while subjectEvaluation Cache
    * is a cache maintaining membership status of a subject to a 
    * LDAPRoles/LDAPGroups/LDAPUsers/DSAMERole/Organization subjects,
    * irrespective of  policy, so if a subject  appear in different
    * policies more than once, this cache is relied on for subject evaluation.
    * key for subject evaluation cache would be token ID and 
    * value would be Map of SubjectId (concatenation of ldap server and
    * subjectDN to an array of 2 Objects, one being the isMember status
    * 2 being the timetoLive
    * tokenID ----> subjectId1 --> [timeToLive, isMember]
    *               subjectId2 --> [timeToLive, isMember]
    *                ....
    *                ....
    */

    public static Map subjectEvaluationCache = new HashMap();
    public  static long subjectEvalCacheTTL = 0; // milliseconds

    //in milliseconds
    private static final long DEFAULT_SUBJECT_EVAL_CACHE_TTL = 600000;
    private static final Debug DEBUG = PolicyManager.debug;

    /**
     * Initializes the <code>SubjectEvaluationCache</code>.
     * Uses configuration specified in Policy Configuration Service.
     */
    static void initSubjectEvalTTLFromPolicyConfig() {
        if (subjectEvalCacheTTL == 0) {
            String orgName = ServiceManager.getBaseDN();
            try {
                Map pConfigValues = PolicyConfig.getPolicyConfig(orgName);
                subjectEvalCacheTTL =
                     PolicyConfig.getSubjectsResultTtl(pConfigValues);
                if (subjectEvalCacheTTL <= 0) {
                    subjectEvalCacheTTL = DEFAULT_SUBJECT_EVAL_CACHE_TTL;
                    if (DEBUG.warningEnabled()) {
                        DEBUG.warning("Invalid Subject TTL got from "
                            + "configuration. Set TTL to default:"
                            + subjectEvalCacheTTL);
                    }
                }
            } catch ( PolicyException pe ) {
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("Could not read Policy Config data"
                        + ". Set TTL to default:" + subjectEvalCacheTTL, pe);
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("subjectEvalCacheTTL="
                               + subjectEvalCacheTTL);
            }
        }
    }

    /**
     * Returns the duration for which subject evaluation results would be cached
     * @return the duration for which subject evaluation results would be cached
     * from the time of evaluation,  expressed in milliseconds
     */
    public static long getSubjectEvalTTL() {
        initSubjectEvalTTLFromPolicyConfig();
        return subjectEvalCacheTTL;
    }

    /**
     * Adds a new entry to <code>SubjectEvaluationCache</code>.
     * @param tokenID sesstion token id of user.
     * @param ldapServer ldap server having the entry corresponding to 
     * <code>Subject</code> name value.
     * @param valueDN subject name value.
     * @param member result of membership evaluation.
     */
    public static void addEntry(
        String tokenID, 
        String ldapServer,
        String valueDN, 
        boolean member
    ) {
        String subjectId = ldapServer+":"+valueDN;
        Map subjectEntries = null;
        long[] elem = new long[2];
        synchronized (subjectEvaluationCache) {
            elem[0] = System.currentTimeMillis() + getSubjectEvalTTL();
            elem[1] = (member == true) ? 1:0;
            if ((subjectEntries = (Map)subjectEvaluationCache.get(tokenID))
                != null) 
            {
                subjectEntries.put(subjectId, elem);
            } else {
                subjectEntries = Collections.synchronizedMap(new HashMap());
                subjectEntries.put(subjectId, elem);
                subjectEvaluationCache.put(tokenID,subjectEntries);
            }
        }
    }

    /**
     * Checks whether the user identified by session token id is 
     * a member of a <code>Subject</code> name value
     * @param tokenID sesstion token id of user
     * @param ldapServer ldap server having the entry corresponding to 
     * <code>Subject</code> name value
     * @param valueDN subject name value
     * @return cached result of membership evaluation
     */
    public static Boolean isMember(String tokenID, 
        String ldapServer, String valueDN ) 
    {
        Boolean member = null;
        String subjectId = ldapServer+":"+valueDN;
        Map subjectEntries = null;
        if ((subjectEntries = (Map)subjectEvaluationCache.get(tokenID))
            != null) 
        {
            long[] element = (long[])subjectEntries.get(subjectId);
            long timeToLive = 0;
            if (element != null) {
                timeToLive = (long)element[0];
                long currentTime = System.currentTimeMillis();
                if (timeToLive > currentTime) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("SubjectEvaluationCache.isMember():"
                          + " get the membership result from cache.\n");
                    }
                    member = (element[1] == 1) ? Boolean.TRUE: 
                        Boolean.FALSE;
                }
            }
        }
        return member; 
    }

    /**
     * Records number of cached entries in <code>Stats</code> object
     * @param policyStats policy <code>Stats</code> object
     */
    static void printStats(Stats policyStats) {

        /* record stats for subjectEvaluationCache */

        int cacheSize = 0;
        synchronized(subjectEvaluationCache) {
            cacheSize = subjectEvaluationCache.size();
        }
        policyStats.record("SubjectEvaluationCache: Number of entries in"
                + " cache : " + cacheSize);
    }

}
