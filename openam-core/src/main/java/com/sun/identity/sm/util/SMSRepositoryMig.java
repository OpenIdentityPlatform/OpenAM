/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSRepositoryMig.java,v 1.4 2009/01/28 05:35:04 ww203982 Exp $
 *
 * Portions Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.sm.util;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;

import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.flatfile.SMSFlatFileObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;

/**
 * Migrates a SMS in LDAP to flat file structure usable # by SMSFlatFileObject
 * class.
 */
public class SMSRepositoryMig {

    static private void createSMSEntry(SMSFlatFileObject smsFlatFileObject,
            String dn, Iterable<Attribute> attrs) throws Exception {
        // Convert attrs from LDAPAttributeSet to a Map needed by SMSObject.
        Map<String, Set<String>> attrsMap = new HashMap<>();

        for (Attribute attribute : attrs) {
            String attrName = attribute.getAttributeDescriptionAsString();
            Set<String> attrVals = new HashSet<>();
            for (ByteString value : attribute) {
                attrVals.add(value.toString());
            }
            attrsMap.put(attrName, attrVals);
        }
        try {
            smsFlatFileObject.create(null, dn, attrsMap);
        } catch (ServiceAlreadyExistsException e) {
            System.out.println("Warning: '" + dn + "' already exists.");
        }
    }

    static private void migrate(ConnectionFactory factory, String host, int port, String binddn,
            String pw, String basedn, String flatfiledir) throws Exception {
        // check args
        if (port < 0 || binddn == null || binddn.length() == 0 || pw == null
                || pw.length() == 0 || basedn == null || basedn.length() == 0
                || flatfiledir == null || flatfiledir.length() == 0) {
            throw new IllegalArgumentException(
                    "SMSRepositoryMig: One or more invalid " +
                    "arguments in constructor");
        }

        // Create the SMSFlatFileObject
        SMSFlatFileObject smsFlatFileObject = new SMSFlatFileObject();
        try (Connection conn = factory.getConnection()) {
            // Loop through LDAP attributes, create SMS object for each.
            ConnectionEntryReader res = conn.search(LDAPRequests.newSearchRequest("ou=services," + basedn,
                    SearchScope.BASE_OBJECT, "(objectclass=*)", "*"));
            while (res.hasNext()) {
                if (res.isReference()) {
                    //ignore
                    res.readReference();
                    System.out.println("ERROR: LDAP Referral not supported.");
                    System.out.println("LDAPReferralException received");
                } else {
                    SearchResultEntry entry;
                    try {
                        entry = res.readEntry();
                        createSMSEntry(smsFlatFileObject, entry.getName().toString(), entry.getAllAttributes());
                    } catch (LdapException e) {
                        System.out.println("ERROR: LDAP Exception encountered: " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static ConnectionFactory getConnectionFactory(String hostname, int port, String bindDN, char[] bindPassword) {
        Options options = Options.defaultOptions()
                .set(AUTHN_BIND_REQUEST,
                        LDAPRequests.newSimpleBindRequest(bindDN, bindPassword));
        return new LDAPConnectionFactory(hostname, port, options);
    }

    static private void usage() {
        System.out
                .println("Usage: "
                        + SMSRepositoryMig.class.getName()
                        + " <host> <port> <binddn> <password> " +
                                "<orgdn> <flat-file directory>");
    }

    static public void main(String[] args) throws Exception {
        String host, binddn, pw, basedn, flatfiledir;
        int port;
        if (args.length < 6) {
            usage();
            System.exit(0);
        }

        host = args[0];
        port = Integer.parseInt(args[1]);
        binddn = args[2];
        pw = args[3];
        basedn = args[4];
        flatfiledir = args[5];

        try (ConnectionFactory factory = getConnectionFactory(host, port, basedn, pw.toCharArray())) {
            // do the migration
            migrate(factory, host, port, binddn, pw, basedn, flatfiledir);
        }
    }
}
