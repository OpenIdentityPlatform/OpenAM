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
 * $Id: JavaOneDemoCondition.java,v 1.1 2009/08/19 05:40:35 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.LDAPDN;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class JavaOneDemoCondition extends EntitlementConditionAdaptor {

    public void setState(String state) {
        //DO NOTHING
    }

    public String getState() {
        return "";
    }

    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        try {
            String accountNumber = getAccountNumber(resourceName);
            if (accountNumber == null) {
                return new ConditionDecision(false, Collections.EMPTY_MAP);
            }
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            String uuid = getUUID(subject);
            AMIdentity amid = new AMIdentity(adminToken, uuid, IdType.USER,
                "/", null);
            Set<String> setHoH = amid.getAttribute(
                "head_of_household");
            if ((setHoH == null) || !setHoH.contains("1")) {
                return new ConditionDecision(false, Collections.EMPTY_MAP);
            }
            Set<String> account = amid.getAttribute("account_number");
            if ((account == null) || !account.contains(accountNumber)) {
                return new ConditionDecision(false, Collections.EMPTY_MAP);
            }
            return new ConditionDecision(true, Collections.EMPTY_MAP);
        } catch (IdRepoException ex) {
            return new ConditionDecision(false, Collections.EMPTY_MAP);
        } catch (SSOException ex) {
            return new ConditionDecision(false, Collections.EMPTY_MAP);
        }
    }

    private String getAccountNumber(String resourceName)
        throws EntitlementException {
        try {
            /*
             * Resource name can be of form
             * http://localhost:8080/C1DemoServer/resources/phones/1234567890
             * http://localhost:8080/C1DemoServer/resources/phones/1234567890/
             * http://localhost:8080/C1DemoServer/resources/phones/1234567890/accountNumber/
             * etc
             */
            String prefix = "/resources/phones/";
            String telNumber;
            int start = resourceName.indexOf(prefix) + prefix.length();
            if ( start >= resourceName.length() ) {
                return null;
            }
            int end = resourceName.indexOf("/", start);
            if ( end == -1 ) {
                // No trailing slash
                telNumber = resourceName.substring(start);
            } else {
                telNumber = resourceName.substring(start, end);
            }

            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            AMIdentity amid = new AMIdentity(adminToken, telNumber,
                IdType.USER, "/", null);
            Set<String> accountNumbers = amid.getAttribute("account_number");
            return ((accountNumbers != null) && !accountNumbers.isEmpty()) ?
                accountNumbers.iterator().next() : null;
        } catch (IdRepoException ex) {
            return null;
        } catch (SSOException ex) {
            return null;
        }
    }

    public static String getUUID(Subject subject) {
        Set<Principal> userPrincipals = subject.getPrincipals();
        String uid = ((userPrincipals != null) && !userPrincipals.isEmpty()) ?
            userPrincipals.iterator().next().getName() : null;
        String[] x = LDAPDN.explodeDN(uid, true);
        return x[0];
    }

}
