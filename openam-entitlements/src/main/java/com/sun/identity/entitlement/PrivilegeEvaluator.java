/*
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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.IThreadPool;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
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

import org.forgerock.openam.entitlement.PrivilegeEvaluatorContext;
import org.forgerock.openam.session.util.AppTokenHandler;
import org.forgerock.openam.utils.CollectionUtils;

/**
 * This class evaluates entitlements of a subject for a given resource
 * and a environment parameters.
 */
class PrivilegeEvaluator {
    private String realm = "/";
    private Subject adminSubject;
    private Subject subject;
    private String applicationName;
    private String normalisedResourceName;
    private String requestedResourceName;
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
    private final Condition hasResults = lock.newCondition();

    // Static variables
    // TODO determine number of tasks per thread
    private static int evalThreadSize = Evaluator.DEFAULT_POLICY_EVAL_THREAD;
    private static final int TASKS_PER_THREAD = 5;

    private static final IThreadPool threadPool;
    private static final boolean isMultiThreaded;

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
        isMultiThreaded = evalThreadSize > 1;
        threadPool = isMultiThreaded ? new EntitlementThreadPool(evalThreadSize) : new SequentialThreadPool();
    }

    /**
     * Initializes the evaluator.
     *
     * @param adminSubject Administrator subject which is used fo evaluation.
     * @param subject Subject to be evaluated.
     * @param realm Realm Name
     * @param applicationName Application Name.
     * @param normalisedResourceName The normalised resource name.
     * @param requestedResourceName The requested resource name.
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
        String normalisedResourceName,
        String requestedResourceName,
        Set<String> actions,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {
        this.adminSubject = adminSubject;
        this.subject = subject;
        this.realm = realm;
        this.applicationName = applicationName;
        this.normalisedResourceName = normalisedResourceName;
        this.requestedResourceName = requestedResourceName;
        this.envParameters = envParameters;
        this.actionNames = new HashSet<String>();
        if (CollectionUtils.isNotEmpty(actions)) {
            this.actionNames.addAll(actions);
        }

        Application appl = getApplication();
        entitlementCombiner = appl.getEntitlementCombiner();
        entitlementCombiner.init(realm, applicationName, normalisedResourceName, requestedResourceName,
                this.actionNames, recursive);
        this.recursive = recursive;

        if (PrivilegeManager.debug.messageEnabled()) {
            Debug debug = PrivilegeManager.debug;
            debug.message("[PolicyEval] PrivilegeEvaluator:init()");
            debug.message("[PolicyEval] subject: " + getPrincipalId(subject));
            debug.message("[PolicyEval] realm: " + realm);
            debug.message("[PolicyEval] applicationName: " + applicationName);
            debug.message("[PolicyEval] normalisedResourceName: " + this.normalisedResourceName);
            debug.message("[PolicyEval] requestedResourceName: " + this.requestedResourceName);
            debug.message("[PolicyEval] actions: " + actionNames);
            if ((envParameters != null) && !envParameters.isEmpty()) {
                debug.message("[PolicyEval] envParameters: " +
                    envParameters.toString());
            }
        }

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
     * @param adminSubject Administrator subject which is used for evaluation.
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
            entitlement.getResourceName(), entitlement.getRequestedResourceName(),
            entitlement.getActionValues().keySet(), envParameters, false);
        entitlement.setApplicationName(applicationName);

        indexes = entitlement.getResourceSearchIndexes(adminSubject, realm);

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
     * @param normalisedResourceName The normalised resource name.
     * @param requestedResourceName The requested resource name.
     * @param envParameters Environment parameters.
     * @param recursive <code>true</code> for sub tree evaluation.
     * @return <code>true</code> if the subject has privilege to have the
     * given entitlement.
     * @throws com.sun.identity.entitlement.EntitlementException if
     * evaluation fails.
     */
    public List<Entitlement> evaluate(
        String realm,
        Subject adminSubject,
        Subject subject,
        String applicationName,
        String normalisedResourceName,
        String requestedResourceName,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {


        init(adminSubject, subject, realm, applicationName,
            normalisedResourceName, requestedResourceName, null, envParameters, recursive);
        indexes = getApplication().getResourceSearchIndex(normalisedResourceName, realm);

        return evaluate(realm);



    }

    /**
     * Responsible for the core evaluation of policies associated with the request resource.
     *
     * @param realm
     *         the evaluation realm
     *
     * @return a list of applicable entitlements
     *
     * @throws EntitlementException
     */
    private List<Entitlement> evaluate(String realm) throws EntitlementException {
        final Debug debug = PrivilegeManager.debug;

        // Search for relevant policies.
        final SubjectAttributesManager sam = SubjectAttributesManager.getInstance(adminSubject, realm);
        final Set<String> subjectIndexes = sam.getSubjectSearchFilter(subject, applicationName);
        final PrivilegeIndexStore indexStore = PrivilegeIndexStore.getInstance(adminSubject, realm);
        final Iterator<IPrivilege> policyIterator = indexStore.search(realm, indexes, subjectIndexes, recursive);

        int totalCount = 0;
        IPrivilege policy;

        // First collect policies to be evaluated locally.
        final Set<IPrivilege> localBatch = new HashSet<IPrivilege>(2 * TASKS_PER_THREAD);
        while (totalCount < TASKS_PER_THREAD && policyIterator.hasNext()) {
            policy = policyIterator.next();

            if (policy instanceof ReferralPrivilege) {
                // We want to ignore referrals - deprecated.
                continue;
            }

            if (debug.messageEnabled()) {
                debug.message("[PolicyEval] PolicyEvaluator.evaluate");
                debug.message("[PolicyEval] search result: privilege=" + policy.getName());
            }

            localBatch.add(policy);
            totalCount++;
        }

        // Define an evaluation context.
        final PrivilegeEvaluatorContext context =
                new PrivilegeEvaluatorContext(realm, normalisedResourceName, applicationName);
        final Object appToken = AppTokenHandler.getAndClear();

        // Submit additional policies to be executed by worker threads.
        final Set<IPrivilege> threadBatch = new HashSet<IPrivilege>(2 * TASKS_PER_THREAD);
        boolean tasksSubmitted = false;

        while (policyIterator.hasNext()) {
            tasksSubmitted = true;
            policy = policyIterator.next();

            if (policy instanceof ReferralPrivilege) {
                // We want to ignore referrals - deprecated.
                continue;
            }

            if (debug.messageEnabled()) {
                debug.message("[PolicyEval] PolicyEvaluator.evaluate");
                debug.message("[PolicyEval] search result: privilege=" + policy.getName());
            }

            threadBatch.add(policy);
            totalCount++;

            if (threadBatch.size() == TASKS_PER_THREAD) {
                final Set<IPrivilege> copiedBatch = new HashSet<IPrivilege>(threadBatch);
                threadPool.submit(new PrivilegeTask(this, copiedBatch, isMultiThreaded, appToken, context));
                threadBatch.clear();
            }
        }

        if (!threadBatch.isEmpty()) {
            // Submit any remaining policies.
            threadPool.submit(new PrivilegeTask(this, threadBatch, isMultiThreaded, appToken, context));
        }

        // Submit the local policies.
        final Runnable localTask = new PrivilegeTask(this, localBatch, tasksSubmitted, appToken, context);
        localTask.run();

        // Wait for submitted threads to complete evaluation.
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

        if (eException != null) {
            // Throw caught exception.
            throw eException;
        }

        return entitlementCombiner.getResults();
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
        private final Set<IPrivilege> privileges;
        private final boolean isThreaded;
        private final Object context;
        private final PrivilegeEvaluatorContext ctx;

        PrivilegeTask(PrivilegeEvaluator parent, Set<IPrivilege> privileges,
            boolean isThreaded, Object context, PrivilegeEvaluatorContext ctx) {
            this.parent = parent;
            this.privileges = privileges;
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
                                            parent.applicationName, parent.normalisedResourceName,
                                            parent.requestedResourceName,
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
