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
 * $Id: SearchResultIterator.java,v 1.2 2009/04/02 20:22:43 veiming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.sm.ldap;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSUtils;
import java.util.Iterator;
import java.util.Set;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * This class iterates through the <code>LDAPSearchResults</code> and converts the
 * LDAPEntry to a <code>SMSDataEntry</code> object.
 */
public class SearchResultIterator implements Iterator<SMSDataEntry> {
    private final Debug debug = Debug.getInstance("amSMSLdap");
    private final Connection conn;
    private ConnectionEntryReader results;
    private Set<String> excludeDNs;
    private boolean hasExcludeDNs;
    private SMSDataEntry current;

    /**
     * Constructs a <code>SearchResultIterator</code>
     *  @param results LDAP Search Results object.
     * @param excludeDNs a set of distinguished names to be excluded
     * @param conn
     */
    public SearchResultIterator(ConnectionEntryReader results, Set<String> excludeDNs, Connection conn) {
        this.results = results;
        this.excludeDNs = excludeDNs;
        hasExcludeDNs = (excludeDNs != null) && !excludeDNs.isEmpty();
        this.conn = conn;
    }

    public boolean hasNext() {
        try {
            if (results.hasNext()) {
                if (current == null) {
                    if (results.isReference()) {
                        debug.warning("SearchResultIterator: ignoring reference: {}", results.readReference());
                        return hasNext();
                    }
                    SearchResultEntry entry = results.readEntry();
                    String dn = entry.getName().toString();
                    if (hasExcludeDNs && excludeDNs.contains(dn)) {
                        return hasNext();
                    }

                    current = new SMSDataEntry(dn, SMSUtils.convertEntryToAttributesMap(entry));
                }
                return true;
            }
        } catch (LdapException e) {
            ResultCode errorCode = e.getResult().getResultCode();
            if (errorCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                debug.message("SearchResultIterator: size limit exceeded");
            } else {
                debug.error("SearchResultIterator.hasNext", e);
            }
        } catch (SearchResultReferenceIOException e) {
            debug.error("SearchResultIterator.hasNext: reference should be already handled", e);
            return hasNext();
        }
        conn.close();
        return false;
    }

    public SMSDataEntry next() {
        SMSDataEntry tmp = current;
        current = null;
        return tmp;
    }

    public void remove() {
        //not supported.
    }

}
