/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServiceTypeManager.java,v 1.6 2009/06/30 17:46:02 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */


package com.sun.identity.policy;

import java.util.*;
import java.security.AccessController;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.Constants;

/**
 * The class <code>ServiceTypeManager</code> provides methods
 * to determine the services that have policy privileges and
 * and interfaces to instantiate <code>ServiceType</code>
 * objects. This is a singleton class.
 */
public class ServiceTypeManager {

    private static ServiceTypeManager svtm = null;

    private SSOToken token;
    private Map serviceTypes = Collections.synchronizedMap(new HashMap());

    // static variables
    private static Random random = new Random();

    /**
     * Returns an instance of <code>ServiceTypeManager</code>
     * @return an instance of <code>ServiceTypeManager</code>
     */
    static ServiceTypeManager getServiceTypeManager() 
            throws SSOException {
        if ( svtm == null ) {
            svtm = new ServiceTypeManager();
        }
        return svtm;
    }

    /**
     * Constructs an instance of <code>ServiceTypeManager</code>
     */
    private ServiceTypeManager() throws SSOException {
        token = getSSOToken();
    }

    /**
     * Constructs an instance of <code>ServiceTypeManager</code>
     * @param pm <code>PolicyManager</code> to initialize the
     * <code>ServiceTypeManager</code> with 
     */
    private ServiceTypeManager(PolicyManager pm) {
        token = pm.token;
    }

    /**
     * Constructor to obtain an instance of <code>ServiceTypeManager</code>
     * using single-sign-on token <code>SSOToken</code>. If the
     * single-sign-on token is invalid or has expired an <code>
     * SSOException</code> will be thrown.
     *
     * @param token single-sign-on token of the user
     *
     * @exception SSOException single-sign-on token has either expired or
     * is invalid
     */
    public ServiceTypeManager(SSOToken token) throws SSOException {
        SSOTokenManager.getInstance().validateToken(token);
        this.token = token;
    }

    /**
     * Returns a set of service names that have policy privileges.
     *
     * @return set of service type names that have policy privileges
     *
     * @exception SSOException single-sign-on token has either expired
     * or is invalid
     * @exception NoPermissionException user does not have privileges
     * to access service names
     */
    public Set getServiceTypeNames() throws SSOException,
            NoPermissionException {
        SSOTokenManager.getInstance().validateToken(token);
        try {
            ServiceManager sm = new ServiceManager(token);
            Iterator items = sm.getServiceNames().iterator();
            // Check if the service names have policy schema
            HashSet answer = new HashSet();
            while (items.hasNext()) {
            String serviceName = (String) items.next();
            try {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    serviceName, token);
                if (ssm.getPolicySchema() != null)
                    answer.add(serviceName);
            } catch (Exception e) {
                        PolicyManager.debug.error(
                            "ServiceTypeManager.getServiceTypeNames:", e);
            }
            }
            return (answer);
        } catch (SMSException se) {
            throw (new NoPermissionException(se));
        }
    }

    /**
     * Returns a <code>ServiceType</code> object for the given
     * service name. If the service does not exist, the exception
     * <code>NameNotFoundException</code> is thrown.
     *
     * @param serviceTypeName name of the service
     *
     * @return <code>ServiceType</code> object for the given service name
     *
     * @exception SSOException single-sign-on token has either expired
     * or is invalid
     * @exception NameNotFoundException service for the given <code>
     * serviceTypeName</code> does not exist
     */
    public ServiceType getServiceType(String serviceTypeName)
            throws SSOException, NameNotFoundException {
        ServiceType st = (ServiceType) serviceTypes.get(serviceTypeName);
        if (st == null) {
            try {
                ServiceSchema policySchema = null;
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    serviceTypeName, token);
                if ((ssm == null) ||
                    ((policySchema = ssm.getPolicySchema()) == null)) {
                    if (PolicyManager.debug.messageEnabled()) {
                            PolicyManager.debug.message(
                            "ServiceTypeManager::getServiceType " +
                            serviceTypeName 
                            + " not found with policy privileges");
                    }
                    String objs[] = { serviceTypeName };
                    throw (new NameNotFoundException(
                            ResBundleUtils.rbName,
                            "service_name_not_found", objs, 
                            serviceTypeName, PolicyException.SERVICE));
                }
                st =  new ServiceType(serviceTypeName, ssm, policySchema);
                serviceTypes.put(serviceTypeName, st);
            } catch (SMSException se) {
                PolicyManager.debug.error(
                        "In ServiceTypeManager::getServiceType " +
                        serviceTypeName + " got SMS exception: ", se);
                throw (new NameNotFoundException(se, serviceTypeName,
                        PolicyException.SERVICE));
            }
        }
        return st;
    }

    /**
     * Returns the <code>SSOToken</code> of the admininistrator 
     * configured in serverconfig.xml 
     * @return the <code>SSOToken</code> of the admininistrator 
     * configured in serverconfig.xml 
     */
    static SSOToken getSSOToken() throws SSOException {
        SSOToken token = AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if (token == null) {
            throw (new SSOException(new PolicyException(
            ResBundleUtils.rbName, "invalid_admin", null, null)));
        }
        return token;
    }

    /**
     * Generates a random name for use in some policy elements.
     *
     * Note on using Random instead of SecureRandom in generating 
     * random name: 
     * names generated in this method are used for 
     * policy elements Conditions, Referrals, Subjects, Rule 
     * and Subjects if a name is not supplied by the caller. 
     * Random is used to come up with a name that is not likely 
     * to be used in the same policy. 
     * These names are not meant to be secrets. 
     * So, not a security problem. 
     * @return the generated random name
     */
    static String generateRandomName() {
        StringBuilder sb = new StringBuilder(30);
        byte[] keyRandom = new byte[5];
        random.nextBytes(keyRandom);
        sb.append(System.currentTimeMillis()).toString();
        return (sb.append(Base64.encode(keyRandom)).toString());
    }

    /** 
     * Returns service revision number of policy service 
     * @return service revision number of policy service 
     */
    public static int getPolicyServiceRevisionNumber() 
            throws PolicyException, SSOException, SMSException {
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                "ServiceTypeManager.getPolicyServiceRevisionNumber:"
                + "entering");
        }
        SSOToken token = getSSOToken();
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            PolicyManager.POLICY_SERVICE_NAME, token);
        int revision = ssm.getRevisionNumber();
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                "ServiceTypeManager.getPolicyServiceRevisionNumber:"
                + "returning revision=" + revision);
        }
        return revision;
    }

}    
