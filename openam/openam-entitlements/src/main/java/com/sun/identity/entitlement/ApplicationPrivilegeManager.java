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
 * $Id: ApplicationPrivilegeManager.java,v 1.5 2010/01/07 00:19:10 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This class manages the delegation of application privileges.
 */
public abstract class ApplicationPrivilegeManager {
    public static final String ACTION_READ = 
        ApplicationPrivilege.Action.READ.toString();
    public static final String ACTION_MODIFY = 
        ApplicationPrivilege.Action.MODIFY.toString();
    public static final String ACTION_DELEGATE =
        ApplicationPrivilege.Action.DELEGATE.toString();

    // RFE: implementation class to be pluggable
    private static String DEFAULT_IMPL_CLASS =
        "com.sun.identity.entitlement.opensso.OpenSSOApplicationPrivilegeManager";
    private static Class DEFAULT_IMPL;

    static {
        try {
            DEFAULT_IMPL = Class.forName(DEFAULT_IMPL_CLASS);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("ApplicationPrivilegeManager.<init>",
                ex);
        }
    }

    protected ApplicationPrivilegeManager() {
    }

    /**
     * Adds a delegation privilege.
     *
     * @param appPrivilege Application Privilege to be added
     * @throws EntitlementException if privilege cannot be added.
     */
    public abstract void addPrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException;

    /**
     * Replaces (or modifies) an existing privilege.
     *
     * @param appPrivilege New privilege.
     * @throws EntitlementException if privilege cannot be replaced.
     */
    public abstract void replacePrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException;

    /**
     * Removes a privilege.
     *
     * @param name Name of privilege to remove
     * @throws EntitlementException if privilege cannot be removed.
     */
    public abstract void removePrivilege(String name)
        throws EntitlementException;

    /**
     * Returns a set of privilege names that the administrator can delegate.
     *
     * @param filters Set of search filters.
     * @return Set of privilege names that the administrator can delegate.
     */
    public abstract Set<String> search(Set<SearchFilter> filters);

    /**
     * Returns the application privilege object.
     *
     * @param name Name of application privilege.
     * @return the application privilege object.
     * @throws EntitlementException if privilege cannot be returned.
     */
    public abstract ApplicationPrivilege getPrivilege(String name)
        throws EntitlementException;

    /**
     * Returns <code>true</code> if the subject has permission to a
     * privilege of a given action.
     *
     * @param p Privilege.
     * @param action Privilege action.
     * @return <code>true</code> if the subject has permission to a
     * privilege of a given action.
     */
    public abstract boolean hasPrivilege(Privilege p,
        ApplicationPrivilege.Action action
    ) throws EntitlementException;


    public abstract boolean hasPrivilege(
        Application app,
        ApplicationPrivilege.Action action
    ) throws EntitlementException;

    /**
     * Returns <code>true</code> if the subject has permission to a
     * referral privilege of a given action.
     *
     * @param p Referral Privilege.
     * @param action Privilege action.
     * @return <code>true</code> if the subject has permission to a
     * referral privilege of a given action.
     */
    public abstract boolean hasPrivilege(ReferralPrivilege p,
        ApplicationPrivilege.Action action
    ) throws EntitlementException;

    /**
     * Returns a set of resources for an application and an action.
     *
     * @param applicationName Application name.
     * @param action Privilege action.
     * @return set of resources for an application and an action.
     */
    public abstract Set<String> getResources(String applicationName,
        ApplicationPrivilege.Action action);

    /**
     * Returns application names for a given action.
     *
     * @param action Privilege action.
     * @return application names for a given action.
     */
    public abstract Set<String> getApplications(
        ApplicationPrivilege.Action action);

    /**
     * Returns <code>true</code> if subject can create application.
     *
     * @param realm Realm where application is to be created.
     */
    public abstract boolean canCreateApplication(String realm);

    /**
     * Returns an instance of application privilege manager.
     *
     * @param realm Realm name.
     * @param caller Administrator subject.
     * @return an instance of application privilege manager.
     */
    public static ApplicationPrivilegeManager getInstance(
        String realm, Subject caller) {
        Class[] parameterTypes = {String.class, Subject.class};
        Constructor c;
        ApplicationPrivilegeManager instance = null;
        try {
            c = DEFAULT_IMPL.getConstructor(parameterTypes);
            instance =(ApplicationPrivilegeManager)
                c.newInstance(realm, caller);
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        } catch (IllegalArgumentException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        } catch (InvocationTargetException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        } catch (NoSuchMethodException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        } catch (SecurityException ex) {
            PrivilegeManager.debug.error(
                "ApplicationPrivilegeManager.getInstance", ex);
        }
        return instance;
    }
}

