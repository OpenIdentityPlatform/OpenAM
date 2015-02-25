/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;

import java.util.Set;

/**
 * Defines Create Read Update Delete operations for implementation of IPrivilege.
 */
public interface IPrivilegeManager<T extends IPrivilege> {

    /**
     * Checks if a privilege with the specified name can be found.
     *
     * @param name name of the privilege.
     * @throws com.sun.identity.entitlement.EntitlementException if search failed.
     */
    boolean canFindByName(String name) throws EntitlementException;

    /**
     * Finds a privilege by its unique name.
     *
     * @param name name of the privilege to be returned
     * @throws com.sun.identity.entitlement.EntitlementException if privilege is not found.
     */
    T findByName(String name) throws EntitlementException;

    /**
     * Add a privilege.
     *
     * @param privilege privilege to add.
     * @throws EntitlementException if privilege cannot be added.
     */
    void add(T privilege) throws EntitlementException;

    /**
     * Remove a privilege.
     *
     * @param name name of the privilege to be removed.
     * @throws EntitlementException if privilege cannot be removed.
     */
    void remove(String name) throws EntitlementException;

    /**
     * Modify a privilege.
     *
     * @param privilege the privilege to be modified
     * @throws com.sun.identity.entitlement.EntitlementException if privilege cannot be modified.
     */
    void modify(T privilege) throws EntitlementException;

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @param searchSizeLimit Search size limit.
     * @param searchTimeLimit Search time limit in seconds.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    Set<String> searchNames(Set<SearchFilter> filter, int searchSizeLimit, int searchTimeLimit)
            throws EntitlementException;

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    Set<String> searchNames(Set<SearchFilter> filter) throws EntitlementException;
}
