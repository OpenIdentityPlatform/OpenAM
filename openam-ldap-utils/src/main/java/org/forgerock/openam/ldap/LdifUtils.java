//@Checkstyle:off
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPUtils.java,v 1.9 2009/08/05 20:39:01 hengming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */
//@Checkstyle:on

package org.forgerock.openam.ldap;

import com.forgerock.opendj.ldap.controls.TransactionIdControl;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyDNRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldif.ChangeRecord;
import org.forgerock.opendj.ldif.ChangeRecordVisitor;
import org.forgerock.opendj.ldif.LDIFChangeRecordReader;

/**
 * Utility class for LDIF operations.
 */
public final class LdifUtils {

    private static final Debug DEBUG = Debug.getInstance("amUtil");

    private LdifUtils() {
    }

    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param file file containing LDIF entries.
     * @param ld LDAP Connection.
     * @throws IOException If an error occurs when reading the LDIF file.
     */
    public static void createSchemaFromLDIF(String file, Connection ld) throws IOException {
        try (InputStream fileInput = new FileInputStream(file)) {
            createSchemaFromLDIF(fileInput, ld);
        }
    }


    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param stream Data input stream containing LDIF entries.
     * @param ld LDAP Connection.
     * @throws IOException If an error occurs when reading the LDIF file.
     */
    public static void createSchemaFromLDIF(InputStream stream, Connection ld) throws IOException {
        createSchemaFromLDIF(new LDIFChangeRecordReader(new BufferedInputStream(stream)), ld);
    }

    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param ldif LDIF object.
     * @param ld LDAP Connection.
     * @throws IOException If an error occurs when reading the LDIF file.
     */
    public static void createSchemaFromLDIF(LDIFChangeRecordReader ldif, final Connection ld) throws IOException {
        while (ldif.hasNext()) {
            final ChangeRecord changeRecord = ldif.readChangeRecord();
            changeRecord.accept(new ChangeRecordVisitor<Void, Void>() {
                @Override
                public Void visitChangeRecord(Void aVoid, AddRequest change) {
                    try {
                        change.addControl(TransactionIdControl.newControl(
                                AuditRequestContext.createSubTransactionIdValue()));
                        ld.add(change);
                    } catch (LdapException e) {
                        if (ResultCode.ENTRY_ALREADY_EXISTS.equals(e.getResult().getResultCode())) {
                            for (Attribute attr : change.getAllAttributes()) {
                                ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(change.getName());
                                modifyRequest.addModification(new Modification(ModificationType.ADD, attr));
                                try {
                                    ld.modify(modifyRequest);
                                } catch (LdapException ex) {
                                    DEBUG.warning("LDAPUtils.createSchemaFromLDIF - Could not modify schema: {}",
                                            modifyRequest, ex);
                                }
                            }
                        } else {
                            DEBUG.warning("LDAPUtils.createSchemaFromLDIF - Could not add to schema: {}", change, e);
                        }
                    }
                    return null;
                }

                @Override
                public Void visitChangeRecord(Void aVoid, ModifyRequest change) {
                    try {
                        change.addControl(TransactionIdControl.newControl(
                                AuditRequestContext.createSubTransactionIdValue()));
                        ld.modify(change);
                    } catch (LdapException e) {
                        DEBUG.warning("LDAPUtils.createSchemaFromLDIF - Could not modify schema: {}", change, e);
                    }
                    return null;
                }

                @Override
                public Void visitChangeRecord(Void aVoid, ModifyDNRequest change) {
                    return null;
                }

                @Override
                public Void visitChangeRecord(Void aVoid, DeleteRequest change) {
                    DEBUG.message("Delete request ignored: {}", changeRecord);
                    return null;
                }

            }, null);
        }
    }

}
