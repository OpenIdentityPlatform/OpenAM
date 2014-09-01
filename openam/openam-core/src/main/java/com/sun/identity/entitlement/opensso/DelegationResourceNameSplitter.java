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
 * $Id: DelegationResourceNameSplitter.java,v 1.2 2009/11/19 00:08:51 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement.opensso;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.util.ResourceNameSplitter;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.SMSEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DelegationResourceNameSplitter extends
    ResourceNameSplitter {
    private final static Pattern PATTERN =
        Pattern.compile("(sms://)(.*?)(/.*)");

    @Override
    public ResourceSearchIndexes getIndexes(String resource, String realm) {
        Matcher match = PATTERN.matcher(resource);
        if (!match.matches()) {
            return super.getIndexes(resource, realm);
        }

        String rootSuffix = SMSEntry.getRootSuffix();
        String dn = match.group(2);

        if (dn.trim().length() == 0) {
            dn = rootSuffix;
        }

        String prefix = match.group(1);
        String suffix = match.group(3);

        if (DN.isDN(dn)) {
            DN rootDN = new DN(rootSuffix);

            if (rootDN.equals(new DN(dn))) {
                return super.getIndexes(resource, realm);
            } else {
                ResourceSearchIndexes indexes = null;
                String[] comp = LDAPDN.explodeDN(dn, false);
                StringBuilder buff = new StringBuilder();
                boolean start = false;

                for (int i = comp.length - 1; i >= 0; --i) {
                    if (buff.length() > 0) {
                        buff.insert(0, ",");
                    }
                    buff.insert(0, comp[i]);

                    if (!start) {
                        start = rootDN.equals(new DN(buff.toString()));
                        if (start) {
                            indexes = super.getIndexes(
                                prefix + buff.toString() + suffix, realm);
                        }
                    } else {
                        ResourceSearchIndexes idx = super.getIndexes(
                            prefix + buff.toString() + suffix, realm);
                        indexes.addAll(idx);
                    }
                }
                return indexes;
            }
        } else {
            return super.getIndexes(resource, realm);
        }
    }
}
