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
 * $Id: EmbeddedSearchResultIterator.java,v 1.1 2009/05/13 21:27:25 hengming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.sm.ldap;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.sm.SMSDataEntry;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.ByteString;
import org.opends.server.types.Attribute;
import org.opends.server.types.SearchResultEntry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class iterates through the list of <code>SearchResultEntry</code> and
 * converts the <code>SearchResultEntry</code> to a <code>SMSDataEntry</code>
 * object.
 */
public class EmbeddedSearchResultIterator implements Iterator<SMSDataEntry> {
    private Iterator resultIter;
    private Set excludeDNs;
    private boolean hasExcludeDNs;
    private SMSDataEntry current;

    /**
     * Constructs a <code>SearchResultIterator</code>
     *
     * @param results LDAP Search Results object.
     * @param excludeDNs a set of distinguished names to be excluded
     */
    public EmbeddedSearchResultIterator(LinkedList results, Set excludeDNs) {
        resultIter = results.iterator();
        this.excludeDNs = excludeDNs;
        hasExcludeDNs = (excludeDNs != null) && !excludeDNs.isEmpty();
    }

    public boolean hasNext() {
        if (!resultIter.hasNext()) {
            return false;
        }

        SearchResultEntry entry = (SearchResultEntry)resultIter.next();
        String dn = entry.getName().toString();
        if (hasExcludeDNs) {
            while (excludeDNs.contains(dn)) {
                if (resultIter.hasNext()) {
                    entry = (SearchResultEntry)resultIter.next();
                    dn = entry.getName().toString();
                } else {
                    entry = null;
                    break;
                }
            }
        }

        current = (entry == null) ? null :  new SMSDataEntry(dn,
            convertLDAPAttributeSetToMap(entry.getAttributes()));
            
        return (current != null);
    }

    public SMSDataEntry next() {
        SMSDataEntry tmp = current;
        current = null;
        return tmp;
    }

    public void remove() {
        //not supported.
    }

    static Map<String, Set<String>> convertLDAPAttributeSetToMap(List<Attribute> attributes) {
        Map<String, Set<String>> answer = null;
        if (CollectionUtils.isNotEmpty(attributes)) {
            for (Attribute attr : attributes) {
                if (attr != null) {
                    Set<String> strValues = new HashSet<>();
                    for (ByteString anAttr : attr) {
                        strValues.add(anAttr.toString());
                    }
                    if (answer == null) {
                        answer = new CaseInsensitiveHashMap<>(10);
                    }
                    answer.put(attr.getName(), strValues);
                }
            }
        }
        return (answer);
    }
}
