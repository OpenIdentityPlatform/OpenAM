/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrivilegeEvaluator.java,v 1.2 2009/10/07 06:36:40 veiming Exp $
 *
 * Portions copyright 2010-2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.IThreadPool;
import com.sun.identity.entitlement.util.NetworkMonitor;

import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.security.auth.Subject;
import org.forgerock.openam.session.util.AppTokenHandler;
import org.forgerock.openam.entitlement.PrivilegeEvaluatorContext;

/**
 * This class evaluates entitlements of a subject for a given resource
 * and a environment parameters.
 */
class PrivilegeEvaluator {
    private String realm = "/";
    private Subject adminSubject;
    private Subject subject;
    private String applicationName;
    private String resourceName;
    private Map<String, Set<String>> envParameters;
    private ResourceSearchIndexes indexes;
    private List<List<Entitlement>> resultQ = new
        LinkedList<List<Entitlement>>();
    private Application application;
    private Set<String> actionNames;
    private EntitlementCombiner entitlementCombiner;
    private boolean recursive;
    private EntitlementException eException;
    private final Lock lock = new ReentrantLock();
    private Condition hasResults = lock.newCondition();
    private final static String PRIVILEGE_EVALUATION_CONTEXT =
            "org.forgerock.openam.entitlement.context";

    // Static variables
    // TODO determine number of tasks per thread
    private static int evalThreadSize = Evaluator.DEFAULT_POLICY_EVAL_THREAD;
    private static int tasksPerThread = 5;

    private static IThreadPool threadPool;
    private static boolean isMultiThreaded;
    
    // Stats monitor
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_INIT =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorInit");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_RES_INDEX =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorResourceIndex");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_SUB_INDEX =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorSubjectIndex");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_SEARCH =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorSearch");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorSearchNext");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_SUBMIT =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorSubmit");
    private static final NetworkMonitor PRIVILEGE_EVAL_MONITOR_WAIT =
        NetworkMonitor.getInstance("PrivilegeEvaluatorMonitorCombineResults");

    static {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            PrivilegeManager.superAdminSubject, "/");
        Set<String> setPolicyEvalThread = ec.getConfiguration(
            EntitlementConfiguration.POLICY_EVAL_THREAD_SIZE);

        if ((setPolicyEvalThread != null) && !setPolicyEvalThread.isEmpty()) {
            try {
                evalThreadSize = Integer.parseInt(setPolicyEvalThread.
                    iterator().next());
            } catch (NumberFormatException e) {
                PrivilegeManager.debug.error(
                    "PrivilegeEvaluator.<init>: get evaluation thread pool size",
                    e);
            }
        }
        isMultiThreaded = (evalThreadSize > 1);
        threadPool = (isMultiThreaded) ?
            new EntitlementThreadPool(evalThreadSize) :
            new SequentialThreadPool();
    }

    /**
     * Initializes the evaluator.
     *
     * @param adminSubject Administrator subject which is used for evcaluation.
     * @param subject Subject to be evaluated.
     * @param realm Realm Name
     * @param applicationName Application Name.
     * @param resourceName Rsource name.
     * @param actions Action names.
     * @param envParameters Environment parameters.
     * @param recursive <code>true</code> for sub tree evaluation
     * @throws com.sun.identity.entitlement.EntitlementException if
     * initialization fails.
     */
    private void init(
        Subject adminSubject,
        Subject subject,
        String realm,
        String applicationName,
        String resourceName,
        Set<String> actions,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {
        long start = PRIVILEGE_EVAL_MONITOR_INIT.start();
        this.adminSubject = adminSubject;
        this.subject = subject;
        this.realm = realm;
        this.applicationName = applicationName;
        this.resourceName = resourceName;
        this.envParameters = envParameters;

        Application appl = getApplication();
        
        this.actionNames = new HashSet<String>();
        if ((actions == null) || actions.isEmpty()) {
            this.actionNames.addAll(appl.getActions().keySet());
        } else {
            this.actionNames.addAll(actions);
        }

        entitlementCombiner = appl.getEntitlementCombiner();
        entitlementCombiner.init(adminSubject, realm, applicationName,
            resourceName, this.actionNames, recursive);
        this.recursive = recursive;

        if (PrivilegeManager.debug.messageEnabled()) {
            Debug debug = PrivilegeManager.debug;
            debug.message("[PolicyEval] PrivilegeEvaluator:init()", null);
            debug.message("[PolicyEval] subject: " + getPrincipalId(subject), null);
            debug.message("[PolicyEval] realm: " + realm, null);
            debug.message("[PolicyEval] applicationName: " + applicationName, null);
            debug.message("[PolicyEval] resourceName: " + resourceName, null);
            debug.message("[PolicyEval] actions: " + actionNames, null);
            if ((envParameters != null) && !envParameters.isEmpty()) {
                debug.message("[PolicyEval] envParameters: " +
                    envParameters.toString(), null);
            }
        }

        PRIVILEGE_EVAL_MONITOR_INIT.end(start);
    }

    private static String getPrincipalId(Subject subject) {
        if (subject == null) {
            return "";
        }
        Set<Principal> userPrincipals = subject.getPrincipals();
        return ((userPrincipals != null) && !userPrincipals.isEmpty()) ?
            userPrincipals.iterator().next().getName() : null;
    }
    /**
     * Returrns <code>true</code> if the subject has privilege to have the
     * given entitlement.
     *
     * @param adminSubject Administrator subject which is used for evcaluation.
     * @param subject Subject to be evaluated.
     * @param applicationName Application Name.
     * @param entitlement Entitlement to be evaluated.
     * @param envParameters Environment parameters.
     * @return <code>true</code> if the subject has privilege to have the
     * given entitlement.
     * @throws com.sun.identity.entitlement.EntitlementException if
     * evaluation fails.
     */
    public boolean hasEntitlement(
        String realm,
        Subject adminSubject,
        Subject subject,
        String applicationName,
        Entitlement entitlement,
        Map<String, Set<String>> envParameters
    ) throws EntitlementException {
        init(adminSubject, subject, realm, applicationName,
            entitlement.getResourceName(), 
            entitlement.getActionValues().keySet(), envParameters, false);
        entitlement.setApplicationName(applicationName);

        long start = PRIVILEGE_EVAL_MONITOR_RES_INDEX.start();
        indexes = entitlement.getResourceSearchIndexes(adminSubject, realm);
        PRIVILEGE_EVAL_MONITOR_RES_INDEX.end(start);

        if (indexes.isEmpty()) {
            // No policies indexes retrieved, therefore return default behaviour to fail.
            return false;
        }

        List<Entitlement> results = evaluate(realm);
        Entitlement result = results.get(0);

        for (String action : entitlement.getActionValues().keySet()) {
            Boolean b = result.getActionValue(action);
            // TODO, use policy decision combining algorithm
            // Default is deny overrides
            if ((b == null) || !b.booleanValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns list of entitlements which is entitled to a subject.
     *
     * @param adminSubject Administrator subject which is used for evaluation.
     * @param subject Subject to be evaluated.
     * @param applicationName Application Name.
     * @param resourceName Resource name.
     * @param envParameters Environment parameters.
     * @param recursive <code>true</code> for sub tree evaluation.
     * @return <code>true</code> if the subject has privilege to have the
     * given entitlement.
     * @throws com.sun.identity.entitlement. EntitlementException if
     * evaluation fails.
     */
    public List<Entitlement> evaluate(
        String realm,
        Subject adminSubject,
        Subject subject,
        String applicationName,
        String resourceName,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {
        init(adminSubject, subject, realm, applicationName,
            resourceName, null, envParameters, recursive);        
        long start = PRIVILEGE_EVAL_MONITOR_RES_INDEX.start();
        indexes = getApplication().getResourceSearchIndex(resourceName, realm);
        PRIVILEGE_EVAL_MONITOR_RES_INDEX.end(start);

        List<Entitlement> entitlements = Collections.emptyList();

        if (!indexes.isEmpty()) {
            entitlements = evaluate(realm);
        }

        return entitlements;
    }
    
    private List<Entitlement> evaluate(String realm)
        throws EntitlementException {

        // Subject index
        long start = PRIVILEGE_EVAL_MONITOR_SUB_INDEX.start();
        SubjectAttributesManager sam = SubjectAttributesManager.getInstance(
            adminSubject, realm);
        PRIVILEGE_EVAL_MONITOR_SUB_INDEX.end(start);
        
        // Search for policies
        start = PRIVILEGE_EVAL_MONITOR_SEARCH.start();
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            adminSubject, realm);
        Iterator<IPrivilege> i = pis.search(realm, indexes,
            sam.getSubjectSearchFilter(subject, applicationName), recursive);
        PRIVILEGE_EVAL_MONITOR_SEARCH.end(start);
        
        // Submit the privileges for evaluation
        // First collect tasks to be evaluated locally
        Set<IPrivilege> localPrivileges = new HashSet<IPrivilege>(
            2*tasksPerThread);
        Debug debug = PrivilegeManager.debug;

        int totalCount = 0;
        while (totalCount != tasksPerThread) {
            start = PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT.start();
            if (i.hasNext()) {
                IPrivilege p = i.next();
                if (debug.messageEnabled()) {
                    debug.message("[PolicyEval] PolicyEvaluator.evaluate", null);
                    debug.message("[PolicyEval] search result: privilege=" +
                        p.getName(), null);
                }
                localPrivileges.add(p);
                totalCount++;
                PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT.end(start);
            } else {
                PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT.end(start);
                break;
            }
        }
        // Submit additional privilges to be executed by worker threads
        Set<IPrivilege> privileges = null;
        boolean tasksSubmitted = false;
        PrivilegeEvaluatorContext ctx =
                new PrivilegeEvaluatorContext(realm, resourceName, applicationName);
        Object appToken = AppTokenHandler.getAndClear();

        while (true) {
            start = PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT.start();
            if (!i.hasNext()) {
                break;
            }
            if (privileges == null) {
                privileges = new HashSet<IPrivilege>(2*tasksPerThread);
                tasksSubmitted = true;
            }
            IPrivilege p = i.next();
            if (debug.messageEnabled()) {
                debug.message("[PolicyEval] PolicyEvaluator.evaluate", null);
                debug.message("[PolicyEval] search result: privilege=" +
                    p.getName(), null);
            }
            privileges.add(p);
            PRIVILEGE_EVAL_MONITOR_SEARCH_NEXT.end(start);
            totalCount++;
            if ((totalCount % tasksPerThread) == 0) {
                start = PRIVILEGE_EVAL_MONITOR_SUBMIT.start();
                threadPool.submit(new PrivilegeTask(this, privileges,
                    isMultiThreaded, appToken, ctx));
                PRIVILEGE_EVAL_MONITOR_SUBMIT.end(start);
                privileges.clear();
            }
        }
        if ((privileges != null) && !privileges.isEmpty()) {
            start = PRIVILEGE_EVAL_MONITOR_SUBMIT.start();
            threadPool.submit(new PrivilegeTask(this, privileges,
                isMultiThreaded, appToken, ctx));
            PRIVILEGE_EVAL_MONITOR_SUBMIT.end(start);
        }
        // IPrivilege privileges locally
        (new PrivilegeTask(this, localPrivileges, tasksSubmitted, appToken, ctx)).run();

        // Wait for submitted threads to complete evaluation
        start = PRIVILEGE_EVAL_MONITOR_WAIT.start();
        if (tasksSubmitted) {
            if (isMultiThreaded) {
                receiveEvalResults(totalCount);
            } else {
                boolean isDone = false;
                while (!resultQ.isEmpty() && !isDone) {
                    entitlementCombiner.add(resultQ.remove(0));
                    isDone = entitlementCombiner.isDone();
                }
            }
        } else if (eException == null) {
            boolean isDone = false;
            while (!resultQ.isEmpty() && !isDone) {
                entitlementCombiner.add(resultQ.remove(0));
                isDone = entitlementCombiner.isDone();
            }
        }
        PRIVILEGE_EVAL_MONITOR_WAIT.end(start);
        
        if (eException != null) {
            throw eException;
        }

        List<Entitlement> ents = entitlementCombiner.getResults();
        return ents;
    }

    private void receiveEvalResults(int totalCount) {
        int counter = 0;
        lock.lock();
        boolean isDone = (eException != null);

        try {
            while (!isDone && (counter < totalCount)) {
                if (resultQ.isEmpty()) {
                    hasResults.await();
                }
                while (!resultQ.isEmpty() && !isDone) {
                    entitlementCombiner.add(resultQ.remove(0));
                    isDone = entitlementCombiner.isDone();
                    counter++;
                }
            }
        } catch (InterruptedException ex) {
            PrivilegeManager.debug.error("PrivilegeEvaluator.evaluate", ex);
        } finally {
            lock.unlock();
        }
    }
    
    private Application getApplication()
        throws EntitlementException {
        if (application == null) {
            application = ApplicationManager.getApplicationForEvaluation(
                realm, applicationName);
            // If application is still null, throw an exception
            if (application == null) {
                String[] params = { realm };
                throw (new EntitlementException(248, params));
            }
        }
        return application;
    }

    class PrivilegeTask implements Runnable {
        final PrivilegeEvaluator parent;
        private Set<IPrivilege> privileges;
        private boolean isThreaded;
        private Object context;
        private PrivilegeEvaluatorContext ctx;

        PrivilegeTask(PrivilegeEvaluator parent, Set<IPrivilege> privileges,
            boolean isThreaded, Object context, PrivilegeEvaluatorContext ctx) {
            this.parent = parent;
            this.privileges = new HashSet<IPrivilege>(privileges.size() *2);
            this.privileges.addAll(privileges);
            this.isThreaded = isThreaded;
            this.context = context;
            this.ctx = ctx;
        }

        public void run() {
            PrivilegeEvaluatorContext.setCurrent(ctx);
            
            try {
                for (final IPrivilege eval : privileges) {
                    List<Entitlement> entitlements = eval.evaluate(
                                            parent.adminSubject,
                                            parent.realm, parent.subject,
                                            parent.applicationName, parent.resourceName,
                                            parent.actionNames, parent.envParameters,
                                            parent.recursive, context);

                    if (entitlements != null) {
                        if (isThreaded) {
                            try {
                                parent.lock.lock();
                                parent.resultQ.add(entitlements);
                                parent.hasResults.signal();
                            } finally {
                                parent.lock.unlock();
                            }
                        } else {
                            parent.resultQ.add(entitlements);
                        }
                    }
                }
            } catch (EntitlementException ex) {
                if (isThreaded) {
                    try {
                        parent.lock.lock();
                        parent.eException = ex;
                        parent.hasResults.signal();
                    } finally {
                        parent.lock.unlock();
                    }
                } else {
                    parent.eException = ex;
                }
            }
        }
    }
}
