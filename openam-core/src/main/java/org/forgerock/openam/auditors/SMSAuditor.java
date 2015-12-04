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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.auditors;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;

import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Responsible for publishing audit config events for an SMS operation.
 *
 * @since 13.0.0
 */
public class SMSAuditor extends ConfigAuditor {

    /**
     * Creates the base for an SMSAuditor
     * @param debug The debugger
     * @param auditEventPublisher The publisher being used
     * @param auditEventFactory The factory used to create the EventBuilder
     * @param runAs The user that the configuration was run as
     * @param realm The realm the configuration takes place in
     * @param objectId The id (e.g. dn) of the object being configured
     * @param initialState The initialState of the object being configured
     * @param filters The filters used to determine if the event should be audited
     */
    @Inject
    public SMSAuditor(@Named("amSMS") Debug debug, AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory, @Assisted SSOToken runAs, @Assisted("realm") @Nullable String realm,
            @Assisted("objectId") String objectId, @Assisted Map<String, Object> initialState, Set<SMSAuditFilter> filters) {
        super(debug, auditEventPublisher, auditEventFactory, runAs, realm, objectId, initialState, filters);
    }

    /**
     * Audits modify operations, using the structure passed in to the SMS operation to establish the final state.
     * @param mods The list of modifications being applied
     */
    public void auditModify(ModificationItem[] mods) {
        if (shouldAudit(AuditConstants.ConfigOperation.UPDATE)) {
            Map<String, Object> finalState = deriveFinalState(mods);
            String[] fieldList = new String[mods.length];
            for(int i = 0; i < mods.length; i++) {
                fieldList[i] = mods[i].getAttribute().getID();
            }

            super.auditModify(finalState, fieldList);
        }
    }

    /**
     * Uses a series of modifications to establish the final state of a set of attributes, based on their original
     * value.
     * @param mods The list of modifications made
     * @return A map representing the final state
     */
    private Map<String, Object> deriveFinalState(ModificationItem[] mods) {
        Map<String, Object> finalState = new CaseInsensitiveHashMap<>(getInitialState());

        for(int i = 0; i < mods.length; i++) {
            Attribute currentAttr = mods[i].getAttribute();
            if(mods[i].getModificationOp() == DirContext.REMOVE_ATTRIBUTE) {
                finalState.remove(currentAttr.getID());
            } else {
                HashSet values = new HashSet();
                for(int j = 0; j < currentAttr.size(); j++) {
                    try {
                        values.add(currentAttr.get(j));
                    } catch(NamingException e) {
                        //no operation here
                    }
                }
                finalState.put(currentAttr.getID(), values);
            }
        }
        return finalState;
    }

    /**
     * Extract the realm from the LDAP DN
     * @param dn The DN from which to extract the realm
     * @return The extracted realm's name
     */
    public static String getRealmFromDN(String dn) {
        Stack stack = new Stack();
        for (Iterator<RDN> itr = DN.valueOf(dn).iterator(); itr.hasNext();) {
            RDN rdn = itr.next();
            if (LDAPUtils.rdnType(rdn).equals("ou") && LDAPUtils.rdnValue(rdn).equals("GlobalConfig")) {
                return null;
            } else if (LDAPUtils.rdnType(rdn).equals("o")) {
                stack.push(LDAPUtils.rdnValue(rdn));
            }
        }

        StringBuilder builder = new StringBuilder();
        while(!stack.empty()) {
            builder.append("/");
            builder.append(stack.pop());
        }

        if(builder.length() > 0) {
            return builder.toString();
        }
        return "/";
    }
}
