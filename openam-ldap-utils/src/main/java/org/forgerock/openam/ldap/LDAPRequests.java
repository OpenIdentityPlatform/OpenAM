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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.ldap;

import com.forgerock.opendj.ldap.controls.TransactionIdControl;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyDNRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.requests.SimpleBindRequest;

/**
 * Wrapper around DJ SDK {@link Requests} static factory methods to add audit transaction id propagation and any
 * other common features. This class should be used in preference to the raw DJ SDK class wherever possible.
 */
public final class LDAPRequests {
    private LDAPRequests() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a simple LDAP bind request that will propagate the common audit transaction id if the target LDAP
     * server supports it (i.e., is OpenDJ).
     *
     * @see Requests#newSimpleBindRequest(String, char[])
     * @param name the user DN.
     * @param password the password.
     * @return the bind request.
     */
    public static SimpleBindRequest newSimpleBindRequest(final String name, final char[] password) {
        return Requests.newSimpleBindRequest(name, password)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Creates a simple LDAP modify request that will propagate the common audit transaction id if the target LDAP
     * server supports it (i.e., is OpenDJ).
     *
     * @see Requests#newModifyRequest(String)
     * @param name the DN of the entry to modify.
     * @return the modify request.
     */
    public static ModifyRequest newModifyRequest(final String name) {
        return Requests.newModifyRequest(name)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Creates a simple LDAP modify request that will propagate the common audit transaction id if the target LDAP
     * server supports it (i.e., is OpenDJ).
     *
     * @see Requests#newModifyRequest(String)
     * @param dn the DN of the entry to modify.
     * @return the modify request.
     */
    public static ModifyRequest newModifyRequest(final DN dn) {
        return Requests.newModifyRequest(dn)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));

    }

    /**
     * Creates an LDAP search request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newSearchRequest(DN, SearchScope, Filter, String...)
     * @param name the search base DN.
     * @param scope the search scope.
     * @param filter the search filter.
     * @param attributeDescriptions the attributes.
     * @return the search request.
     */
    public static SearchRequest newSearchRequest(final DN name, final SearchScope scope, final Filter filter,
            final String... attributeDescriptions) {
        return Requests.newSearchRequest(name, scope, filter, attributeDescriptions)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));

    }

    /**
     * Creates an LDAP search request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newSearchRequest(DN, SearchScope, Filter, String...)
     * @param name the search base DN.
     * @param scope the search scope.
     * @param filter the search filter.
     * @param attributeDescriptions the attributes.
     * @return the search request.
     */
    public static SearchRequest newSearchRequest(final String name, final SearchScope scope, final String filter,
            final String... attributeDescriptions) {
        return Requests.newSearchRequest(name, scope, filter, attributeDescriptions)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));

    }

    /**
     * Returns a single-entry search request for the given entry DN and attributes.
     *
     * @see Connection#readEntry(DN, String...)
     * @param dn the dn of the entry to read.
     * @param attributeDescriptions the attributes to read.
     * @return the search request for the given entry.
     */
    public static SearchRequest newSingleEntrySearchRequest(final DN dn, String... attributeDescriptions) {
        return Requests.newSingleEntrySearchRequest(dn, SearchScope.BASE_OBJECT, Filter.objectClassPresent(),
                attributeDescriptions)
                       .addControl(TransactionIdControl.newControl(
                               AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Returns a single-entry search request for the given entry DN and attributes.
     *
     * @see Connection#readEntry(String, String...)
     * @param dn the dn of the entry to read.
     * @param attributeDescriptions the attributes to read.
     * @return the search request for the given entry.
     */
    public static SearchRequest newSingleEntrySearchRequest(final String dn, String... attributeDescriptions) {
        return Requests.newSingleEntrySearchRequest(dn, SearchScope.BASE_OBJECT, Filter.objectClassPresent().toString(),
                attributeDescriptions)
                       .addControl(TransactionIdControl.newControl(
                               AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Returns a single-entry search request.
     *
     * @see Connection#readEntry(String, String...)
     * @param dn the dn of the entry to read.
     * @param scope the search scope.
     * @param filter the search filter.
     * @param attributeDescriptions the attributes to read.
     * @return the search request for the given entry.
     */
    public static SearchRequest newSingleEntrySearchRequest(final String dn, final SearchScope scope,
            final String filter, final String... attributeDescriptions) {
        return Requests.newSingleEntrySearchRequest(dn, scope, filter, attributeDescriptions)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Creates an LDAP add request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newAddRequest(String)
     * @param name the DN of the entry to add.
     * @return the add request.
     */
    public static AddRequest newAddRequest(final String name) {
        return Requests.newAddRequest(name)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));

    }

    /**
     * Creates an LDAP add request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newAddRequest(String)
     * @param name the DN of the entry to add.
     * @return the add request.
     */
    public static AddRequest newAddRequest(final DN name) {
        return Requests.newAddRequest(name)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));

    }


    /**
     * Creates an LDAP add request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newAddRequest(Entry)
     * @param entry the entry to add.
     * @return the add request.
     */
    public static Entry newAddRequest(final Entry entry) {
        return Requests.newAddRequest(entry)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }


    /**
     * Creates an LDAP delete request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newDeleteRequest(String)
     * @param name the DN of the entry to delete.
     * @return the delete request.
     */
    public static DeleteRequest newDeleteRequest(final String name) {
        return Requests.newDeleteRequest(name)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Creates an LDAP delete request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newDeleteRequest(DN)
     * @param dn the DN of the entry to delete.
     * @return the delete request.
     */
    public static DeleteRequest newDeleteRequest(final DN dn) {
        return Requests.newDeleteRequest(dn)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

    /**
     * Creates an LDAP modify DN request that will propagate the common audit transaction id if the target LDAP server
     * supports it (i.e., is OpenDJ).
     *
     * @see Requests#newModifyDNRequest(String, String)
     * @param name the DN of the entry to modify.
     * @param newName the new DN of the entry.
     * @return the modify DN request.
     */
    public static ModifyDNRequest newModifyDNRequest(final String name, final String newName) {
        return Requests.newModifyDNRequest(name, newName)
                       .addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()));
    }

}
