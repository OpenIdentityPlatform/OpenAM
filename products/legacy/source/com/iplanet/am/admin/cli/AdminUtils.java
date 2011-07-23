/**
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
 * $Id: AdminUtils.java,v 1.3 2009/01/28 05:35:11 ww203982 Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;


/**
 * The <code>AdminUtils </code> class provides methods to print status messages,
 * error messages to the std out when -verbose option is given in the amadmin
 * CLI commandline. The <code>AdminUtils </code> class provides methods to write
 * status messages, error messages to the std out when -debug option is given in
 * the amadmin CLI commandline.
 */
class AdminUtils {
    public final static int VERBOSE = 1;
    public final static int DEBUG = 2;

    private static final String ACCESS_LOG = "amAdmin.access";
    private static final String ERROR_LOG = "amAdmin.error";

    private static int level;
    private static Debug debug = null;
    private static Logger logger = null;
    protected static boolean verboseEnabled = false;
    protected static boolean debugEnabled = false;
    private static boolean logEnabled = true;

    private static SSOToken ssot = null;
    static final String USER_SERVICE  = "iPlanetAMUserService";

    /*
     * To differentiate between the type of
     * logrecord  i.e ERROR or ACCESS
     */
    /**
     * Description of the Field
     */
    public final static int LOG_ACCESS = 0;
    /**
     * Description of the Field
     */
    public final static int LOG_ERROR = 1;

    //
    //  messageids
    //

    /**
     *  Service Not Found
     */
    public static final String SERVICE_NOT_FOUND = "SVC_NOT_FOUND";

    /**
     *  AdminException received
     */
    public static final String ADMIN_EXCEPTION = "ADMIN_EXCEPTION";

    /**
     *  Unsuccessful Login for User
     */
    public static final String LOGIN_FAIL = "LOGIN_FAIL";


    /**
     *  Loaded Service Schema "x"
     */
    public static final String LOAD_SERVICE= "LOAD_SERVICE";

    /**
     *  Deleted Service "x"
     */
    public static final String DELETE_SERVICE = "DELETE_SERVICE";

    /**
     *  Add Attributes
     */
    public static final String ADD_ATTRIBUTES = "ADD_ATTRS";

    /**
     *  No Policies For This Service 
     */
    public static final String NO_POLICY_PRIVILEGES = "NO_POLICY_PRIVS";

    /**
     *  Deleting Policies For Service "x"
     */
    public static final String START_DELETING_RULES = "START_DELETING_POLICIES";

    /**
     *  Done Deleting Policies For Service "x"
     */
    public static final String DONE_DELETING_RULES = "DONE_DELETING_POLICIES";

    /**
     *  Add Resource Bundle of Locale
     */
    public static final String ADD_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER =
        "ADD_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER";

    /**
     *  Add Default Resource Bundle
     */
    public static final String ADD_DEFAULT_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER=
        "ADD_DEFAULT_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER";

    /**
     *  Delete Resource Bundle of Locale
     */
    public static final String DELETE_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER =
        "DELETE_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER";

    /**
     *  Delete Default Resource Bundle
     */
    public static final String
        DELETE_DEFAULT_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER =
        "DELETE_DEFAULT_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER";

    /**
     *  Create Policy 
     */
    public static final String CREATE_POLICY = "CREATE_POLICY";

    /**
     *  Delete Policy 
     */
    public static final String DELETE_POLICY = "DELETE_POLICY";

    /**
     *  Modified SubConfiguration In Organization
     */
    public static final String MODIFY_SUB_CONFIG_IN_ORG =
        "MODIFY_SUB_CONFIG_IN_ORG";

    /**
     *  Added SubConfiguration In Organization
     */
    public static final String ADD_SUB_CONFIG_IN_ORG =
        "ADD_SUB_CONFIG_IN_ORG";

    /**
     *  Deleted SubConfiguration In Organization
     */
    public static final String DELETE_SUB_CONFIG_IN_ORG =
        "DELETE_SUB_CONFIG_IN_ORG";

    /**
     *  Created Remote Provider
     */
    public static final String CREATE_REMOTE_PROV = "CREATE_REMOTE_PROV";

    /**
     *  Modified Remote Provider
     */
    public static final String MODIFY_REMOTE_PROV = "MODIFY_REMOTE_PROV";

    /**
     *  Modified Hosted Provider
     */
    public static final String MODIFY_HOSTED_PROV = "MODIFY_HOSTED_PROV";

    /**
     *  Created Hosted Provider
     */
    public static final String CREATE_HOSTED_PROV = "CREATE_HOSTED_PROV";

    /**
     *  Delete Remote Provider
     */
    public static final String DELETE_PROV = "DELETE_PROV";

    /**
     *  Created Authentication Domain
     */
    public static final String CREATE_COT = "CREATE_COT";

    /**
     *  Deleted Authentication Domain
     */
    public static final String DELETE_COT = "DELETE_COT";

    /**
     *  Modified Authentication Domain
     */
    public static final String MODIFY_COT = "MODIFY_COT";

    /**
     *  Modified Service Schema
     */
    public static final String MODIFY_SERVICE_SCHEMA =
        "MODIFY_SERVICE_SCHEMA";

    /**
     *  Deleted Service Sub Schema
     */
    public static final String DELETE_SERVICE_SUBSCHEMA =
        "DELETE_SERVICE_SUBSCHEMA";

    /**
     *  Added Service Sub Schema
     */
    public static final String ADD_SERVICE_SUBSCHEMA =
        "ADD_SERVICE_SUBSCHEMA";

    /**
     *  Added Sub Configuration of Service
     */
    public static final String ADD_SUB_CONFIGURATION_TO_DEFAULT =
        "ADD_SUB_CONFIGURATION_TO_DEFAULT";

    /**
     *  Modified Sub Configuration of Service
     */
    public static final String MODIFY_SUB_CONFIGURATION_TO_DEFAULT =
        "MODIFY_SUB_CONFIGURATION_TO_DEFAULT";

    /**
     *  Deleted Sub Configuration of Service
     */
    public static final String DELETE_SUB_CONFIGURATION_TO_DEFAULT =
        "DELETE_SUB_CONFIGURATION_TO_DEFAULT";

    /**
     *  Deleted All Service Configurations of Service
     */
    public static final String DELETE_ALL_CONFIGURATIONS =
        "DELETE_ALL_CONFIGURATIONS";

    /**
     * Get Sub Configuration of a service.
     */
    public static final String GET_SUB_CONFIGURATION = "GET_SUB_CONFIGURATION";

    /**
     *  Modified Service Template
     */
    public static final String MODIFY_SERVTEMPLATE_ATTEMPT =
        "MODIFY_SERVTEMPLATE_ATTEMPT";
    public static final String MODIFY_SERVTEMPLATE = "MODIFY_SERVTEMPLATE";

    /**
     *  Added Service Template
     *  This is really a ModifyServiceTemplate request, but...
     */
    public static final String ADD_SERVTEMPLATE_ATTEMPT =
        "ADD_SERVTEMPLATE_ATTEMPT";
    public static final String ADD_SERVTEMPLATE = "ADD_SERVTEMPLATE";

    /**
     *  Removed Service Template
     *  This is really a ModifyServiceTemplate request, but...
     */
    public static final String REMOVE_SERVTEMPLATE_ATTEMPT =
        "REMOVE_SERVTEMPLATE_ATTEMPT";
    public static final String REMOVE_SERVTEMPLATE = "REMOVE_SERVTEMPLATE";

    /**
     *  Added Nested Groups
     */
    public static final String ADD_NESTED_GROUPS_ATTEMPT =
        "ADD_NESTED_GROUPS_ATTEMPT";
    public static final String ADD_NESTED_GROUPS = "ADD_NESTED_GROUPS";

    /**
     *  Add User to Group
     */
    public static final String ADD_USER_ATTEMPT = "ADD_USER_ATTEMPT";
    public static final String ADD_USER = "ADD_USER";

    /**
     *  Create Entity
     */
    public static final String CREATE_ENTITY_ATTEMPT = "CREATE_ENTITY_ATTEMPT";
    public static final String CREATE_ENTITY = "CREATE_ENTITY";

    /**
     *  Create Role
     */
    public static final String CREATE_ROLE_ATTEMPT = "CREATE_ROLE_ATTEMPT";
    public static final String CREATE_ROLE = "CREATE_ROLE";

    /**
     *  Create Group Container
     */
    public static final String CREATE_GROUP_CONTAINER_ATTEMPT =
        "CREATE_GROUP_CONTAINER_ATTEMPT";
    public static final String CREATE_GROUP_CONTAINER =
        "CREATE_GROUP_CONTAINER";

    /**
     *  Create Group
     */
    public static final String CREATE_GROUP_ATTEMPT = "CREATE_GROUP_ATTEMPT";
    public static final String CREATE_GROUP = "CREATE_GROUP";

    /**
     *  Create People Container
     */
    public static final String CREATE_PC_ATTEMPT = "CREATE_PC_ATTEMPT";
    public static final String CREATE_PC = "CREATE_PC";

    /**
     *  Create Service Template
     */
    public static final String CREATE_SERVTEMPLATE_ATTEMPT =
        "CREATE_SERVTEMPLATE_ATTEMPT";
    public static final String CREATE_SERVTEMPLATE = "CREATE_SERVTEMPLATE";

    /**
     *  Create Container
     */
    public static final String CREATE_CONTAINER_ATTEMPT =
        "CREATE_CONTAINER_ATTEMPT";
    public static final String CREATE_CONTAINER = "CREATE_CONTAINER";

    /**
     *  Create User
     */
    public static final String CREATE_USER_ATTEMPT = "CREATE_USER_ATTEMPT";
    public static final String CREATE_USER = "CREATE_USER";

    /**
     *  Delete Entity
     */
    public static final String DELETE_ENTITY_ATTEMPT = "DELETE_ENTITY_ATTEMPT";
    public static final String DELETE_ENTITY = "DELETE_ENTITY";

    /**
     *  Delete People Container
     */
    public static final String DELETE_PC_ATTEMPT = "DELETE_PC_ATTEMPT";
    public static final String DELETE_PC = "DELETE_PC";

    /**
     *  Delete Role
     */
    public static final String DELETE_ROLE_ATTEMPT = "DELETE_ROLE_ATTEMPT";
    public static final String DELETE_ROLE = "DELETE_ROLE";

    /**
     *  Delete Service Template
     */
    public static final String DELETE_SERVTEMPLATE_ATTEMPT =
        "DELETE_SERVTEMPLATE_ATTEMPT";
    public static final String DELETE_SERVTEMPLATE = "DELETE_SERVTEMPLATE";

    /**
     *  Delete Container
     */
    public static final String DELETE_CONTAINER_ATTEMPT =
        "DELETE_CONTAINER_ATTEMPT";
    public static final String DELETE_CONTAINER = "DELETE_CONTAINER";

    /**
     *  Modify Entity
     */
    public static final String MODIFY_ENTITY_ATTEMPT = "MODIFY_ENTITY_ATTEMPT";
    public static final String MODIFY_ENTITY = "MODIFY_ENTITY";

    /**
     *  Modify People Container
     */
    public static final String MODIFY_PC_ATTEMPT = "MODIFY_PC_ATTEMPT";
    public static final String MODIFY_PC = "MODIFY_PC";

    /**
     *  Modify Container
     */
    public static final String MODIFY_SUBCONT_ATTEMPT =
        "MODIFY_SUBCONT_ATTEMPT";
    public static final String MODIFY_SUBCONT = "MODIFY_SUBCONT";

    /**
     *  Register Service "x"
     */
    public static final String REGISTER_SERVICE_ATTEMPT =
        "REGISTER_SERVICE_ATTEMPT";
    public static final String REGISTER_SERVICE = "REGISTER_SERVICE";

    /**
     *  Unregister Service "x"
     */
    public static final String UNREGISTER_SERVICE_ATTEMPT =
        "UNREGISTER_SERVICE_ATTEMPT";
    public static final String UNREGISTER_SERVICE = "UNREGISTER_SERVICE";

    /**
     *  Modify Group
     */
    public static final String MODIFY_GROUP_ATTEMPT = "MODIFY_GROUP_ATTEMPT";
    public static final String MODIFY_GROUP = "MODIFY_GROUP";

    /**
     *  Remove Nested Group From Group
     */
    public static final String REMOVE_NESTED_GROUP_FROM_GROUP_ATTEMPT =
        "REMOVE_NESTED_GROUP_FROM_GROUP_ATTEMPT";
    public static final String REMOVE_NESTED_GROUP_FROM_GROUP =
        "REMOVE_NESTED_GROUP_FROM_GROUP";

    /**
     *  Delete Group
     */
    public static final String DELETE_GROUP_ATTEMPT = "DELETE_GROUP_ATTEMPT";
    public static final String DELETE_GROUP = "DELETE_GROUP";

    /**
     *  Added Identity to Identity in Realm
     */
    public static final String ADD_MEMBER_IDENTITY_ATTEMPT =
        "ADD_MEMBER_IDENTITY_ATTEMPT";
    public static final String ADD_MEMBER_IDENTITY = "ADD_MEMBER_IDENTITY";

    /**
     *  Assigned Service to Identity in Realm
     */
    public static final String ASSIGN_SERVICE_IDENTITY_ATTEMPT =
        "ASSIGN_SERVICE_IDENTITY_ATTEMPT";
    public static final String ASSIGN_SERVICE_IDENTITY =
        "ASSIGN_SERVICE_IDENTITY";

    /**
     *  Created Identities of type in Realm
     */
    public static final String CREATE_IDENTITIES_ATTEMPT =
        "CREATE_IDENTITIES_ATTEMPT";
    public static final String CREATE_IDENTITIES = "CREATE_IDENTITIES";

    /**
     *  Created Identity of type in Realm
     */
    public static final String CREATE_IDENTITY_ATTEMPT =
        "CREATE_IDENTITY_ATTEMPT";
    public static final String CREATE_IDENTITY = "CREATE_IDENTITY";

    /**
     *  Deleted Identity of type in Realm
     */
    public static final String DELETE_IDENTITY_ATTEMPT =
        "DELETE_IDENTITY_ATTEMPT";
    public static final String DELETE_IDENTITY = "DELETE_IDENTITY";

    /**
     *  Modify Service for an Identity in a Realm
     */
    public static final String MODIFY_SERVICE_IDENTITY_ATTEMPT =
        "MODIFY_SERVICE_IDENTITY_ATTEMPT";
    public static final String MODIFY_SERVICE_IDENTITY =
        "MODIFY_SERVICE_IDENTITY";

    /**
     *  Remove Service for an Identity in a Realm
     */
    public static final String REMOVE_MEMBER_IDENTITY_ATTEMPT =
        "REMOVE_MEMBER_IDENTITY_ATTEMPT";
    public static final String REMOVE_MEMBER_IDENTITY =
        "REMOVE_MEMBER_IDENTITY";

    /**
     *  Set Attributes for Service for Identity
     */
    public static final String SET_ATTRIBUTES_IDENTITY_ATTEMPT =
        "SET_ATTRIBUTES_IDENTITY_ATTEMPT";
    public static final String SET_ATTRIBUTES_IDENTITY =
        "SET_ATTRIBUTES_IDENTITY";

    /**
     *  Unassign Service from Identity in Realm
     */
    public static final String UNASSIGN_SERVICE_IDENTITY_ATTEMPT =
        "UNASSIGN_SERVICE_IDENTITY_ATTEMPT";
    public static final String UNASSIGN_SERVICE_IDENTITY =
        "UNASSIGN_SERVICE_IDENTITY";

    /**
     *  Create Sub Organization
     */
    public static final String CREATE_SUBORG_ATTEMPT = "CREATE_SUBORG_ATTEMPT";
    public static final String CREATE_SUBORG = "CREATE_SUBORG";

    /**
     *  Delete Sub Organization
     */
    public static final String DELETE_SUBORG_ATTEMPT = "DELETE_SUBORG_ATTEMPT";
    public static final String DELETE_SUBORG = "DELETE_SUBORG";

    /**
     *  Modify Role
     */
    public static final String MODIFY_ROLE_ATTEMPT = "MODIFY_ROLE_ATTEMPT";
    public static final String MODIFY_ROLE = "MODIFY_ROLE";

    /**
     *  Modify Sub Organization
     */
    public static final String MODIFY_SUBORG_ATTEMPT = "MODIFY_SUBORG_ATTEMPT";
    public static final String MODIFY_SUBORG = "MODIFY_SUBORG";

    /**
     *  Delete User
     */
    public static final String DELETE_USER_ATTEMPT = "DELETE_USER_ATTEMPT";
    public static final String DELETE_USER = "DELETE_USER";

    /**
     *  Modify User
     */
    public static final String MODIFY_USER_ATTEMPT = "MODIFY_USER_ATTEMPT";
    public static final String MODIFY_USER = "MODIFY_USER";

    /**
     *  Added Values to Service Attribute in a Realm
     */
    public static final String ADD_ATTRVALS_REALM_ATTEMPT =
        "ADD_ATTRVALS_REALM_ATTEMPT";
    public static final String ADD_ATTRVALS_REALM = "ADD_ATTRVALS_REALM";

    /**
     *  Assigned a Service to a Realm
     */
    public static final String ASSIGN_SERVICE_TO_REALM_ATTEMPT =
        "ASSIGN_SERVICE_TO_REALM_ATTEMPT";
    public static final String ASSIGN_SERVICE_TO_REALM =
        "ASSIGN_SERVICE_TO_REALM";
    public static final String ASSIGN_SERVICE_TO_REALM_NOTINLIST =
        "ASSIGN_SERVICE_TO_REALM_NOTINLIST";

    /**
     *  Assigned a Service to an Organization Configuration
     */
    public static final String ASSIGN_SERVICE_TO_ORGCONFIG_ATTEMPT =
        "ASSIGN_SERVICE_TO_ORGCONFIG_ATTEMPT";
    public static final String ASSIGN_SERVICE_TO_ORGCONFIG =
        "ASSIGN_SERVICE_TO_ORGCONFIG";
    public static final String ASSIGN_SERVICE_TO_ORGCONFIG_NOTINLIST =
        "ASSIGN_SERVICE_TO_ORGCONFIG_NOTINLIST";

    /**
     *  Create Realm
     */
    public static final String CREATE_REALM_ATTEMPT = "CREATE_REALM_ATTEMPT";
    public static final String CREATE_REALM = "CREATE_REALM";

    /**
     *  Delete Realm
     */
    public static final String DELETE_REALM_ATTEMPT = "DELETE_REALM_ATTEMPT";
    public static final String DELETE_REALM = "DELETE_REALM";

    /**
     *  Modified a Service in a Realm
     */
    public static final String MODIFY_SERVICE_REALM_ATTEMPT =
        "MODIFY_SERVICE_REALM_ATTEMPT";
    public static final String MODIFY_SERVICE_REALM = "MODIFY_SERVICE_REALM";

    /**
     *  Modified a Service in an Organization Configuration
     */
    public static final String MODIFY_SERVICE_ORGCONFIG_ATTEMPT =
        "MODIFY_SERVICE_ORGCONFIG_ATTEMPT";
    public static final String MODIFY_SERVICE_ORGCONFIG =
        "MODIFY_SERVICE_ORGCONFIG";
    public static final String MODIFY_SERVICE_NOTIN_ORGCONFIG_OR_REALM =
        "MODIFY_SERVICE_NOTIN_ORGCONFIG_OR_REALM";

    /**
     *  Remove an Attribute from a Service in a Realm
     */
    public static final String REMOVE_ATTRIBUTE_FROM_SERVICE_ATTEMPT =
        "REMOVE_ATTRIBUTE_FROM_SERVICE_ATTEMPT";
    public static final String REMOVE_ATTRIBUTE_FROM_SERVICE =
        "REMOVE_ATTRIBUTE_FROM_SERVICE";

    /**
     *  Removed Values from an Attribute from a Service in a Realm
     */
    public static final String REMOVE_ATTRVALS_REALM_ATTEMPT =
        "REMOVE_ATTRVALS_REALM_ATTEMPT";
    public static final String REMOVE_ATTRVALS_REALM =
        "REMOVE_ATTRVALS_REALM";

    /**
     *  Set Attributes for a Service in a Realm
     */
    public static final String SET_ATTRS_REALM_ATTEMPT =
        "SET_ATTRS_REALM_ATTEMPT";
    public static final String SET_ATTRS_REALM = "SET_ATTRS_REALM";

    /**
     *  Unassign a Service from a Realm
     */
    public static final String UNASSIGN_SERVICE_FROM_REALM_ATTEMPT =
        "UNASSIGN_SERVICE_FROM_REALM_ATTEMPT";
    public static final String UNASSIGN_SERVICE_FROM_REALM =
        "UNASSIGN_SERVICE_FROM_REALM";

    /**
     *  Unassign a Service from an Organization Configuration
     */
    public static final String UNASSIGN_SERVICE_FROM_ORGCONFIG_ATTEMPT =
        "UNASSIGN_SERVICE_FROM_ORGCONFIG_ATTEMPT";
    public static final String UNASSIGN_SERVICE_FROM_ORGCONFIG =
        "UNASSIGN_SERVICE_FROM_ORGCONFIG";
    public static final String UNASSIGN_SERVICE_NOTIN_ORGCONFIG_OR_REALM =
        "UNASSIGN_SERVICE_NOTIN_ORGCONFIG_OR_REALM";

    /**
     *  Remove user from a Role
     */
    public static final String REMOVE_USER_FROM_ROLE_ATTEMPT =
        "REMOVE_USER_FROM_ROLE_ATTEMPT";
    public static final String REMOVE_USER_FROM_ROLE = "REMOVE_USER_FROM_ROLE";

    /**
     *  Remove user from a Group
     */
    public static final String REMOVE_USER_FROM_GROUP_ATTEMPT =
        "REMOVE_USER_FROM_GROUP_ATTEMPT";
    public static final String REMOVE_USER_FROM_GROUP =
        "REMOVE_USER_FROM_GROUP";

    /**
     *  Session Destroyed
     */
    public static final String STATUS_MSG36 = "STATUS_MSG36";


    private AdminUtils() {
    }

    static void setDebug(Debug theDebug) {
        debug = theDebug;
    }

    static void setDebugStatus(int debugStatus) {
        debug.setDebug(debugStatus);
    }

    static void setLog(boolean enabled) {
        logEnabled = enabled;
    }


    static void setSSOToken(SSOToken ssotoken) {
        ssot = ssotoken;
    }

    static void enableVerbose(boolean verbose) {
        verboseEnabled = verbose;
        level = VERBOSE;
    }

    static void enableDebug(boolean debug) {
        debugEnabled = debug;
        level = DEBUG;
    }


    /**
     * Method to write Debug messages given in the -debug/-verbose option of
     * amadmin CLI.
     *
     * @param message
     */
    public static void log(String message) {
        if (level == VERBOSE) {
            System.out.println(message);
        }
    }

    /**
     * Write to log.
     *
     * @param type of log message
     * @param message to log
     */
    public static void logOperation(int type, String message) {
        logOperation(type, message, ssot);
    }

    /**
     * Write to log.
     *
     * @param type of log message
     * @param message to log
     * @param ssoToken to do logging.
     */
    public static void logOperation(int type, String message, SSOToken ssoToken)
    {
        if (logEnabled && !Main.isInstallTime()) {
            LogRecord logRec = new LogRecord(java.util.logging.Level.INFO,
                message, ssoToken);

            try {
                logRec.addLogInfo(LogConstants.HOST_NAME,
                    ssoToken.getHostName());
                logRec.addLogInfo(LogConstants.LOGIN_ID,
                    ssoToken.getPrincipal().getName());
            } catch (SSOException ssoe) {
                debug.error("AdminUtils.logOperation", ssoe);
            }

            logRec.addLogInfo(LogConstants.LOGIN_ID_SID,
                ssoToken.getTokenID().toString());

            switch (type) {
            case LOG_ACCESS:
                logger = (com.sun.identity.log.Logger)
                    Logger.getLogger(ACCESS_LOG);
                logRec.addLogInfo(LogConstants.MODULE_NAME, ACCESS_LOG);
                break;
            case LOG_ERROR:
                logger = (com.sun.identity.log.Logger)
                    Logger.getLogger(ERROR_LOG);
                logRec.addLogInfo(LogConstants.MODULE_NAME, ERROR_LOG);
                break;
            default:
                logger = (com.sun.identity.log.Logger)
                    Logger.getLogger(ACCESS_LOG);
                logRec.addLogInfo(LogConstants.MODULE_NAME, ACCESS_LOG);
                break;
            }
        
            logger.log(logRec, ssoToken);
        }
    }


    /**
     * Write to log.
     *
     * @param type of log message
     * @param logging level of the message
     * @param message id for message
     * @param array of log message "data"
     */
    public static void logOperation(int type, Level level, String msgid,
        String[] msgdata)
    {
        if (level == null) {
            level = Level.INFO;
        }

        if (logEnabled) {
            switch (type) {
                case LOG_ACCESS:
                    logger = (com.sun.identity.log.Logger)
                        Logger.getLogger(ACCESS_LOG);
                    break;
                case LOG_ERROR:
                    logger = (com.sun.identity.log.Logger)
                        Logger.getLogger(ERROR_LOG);
                    break;
                default:
                    logger = (com.sun.identity.log.Logger)
                        Logger.getLogger(ACCESS_LOG);
            }

            if (logger.isLoggable(level)) {
                try {
                    LogMessageProvider msgProvider =
                        MessageProviderFactory.getProvider("Amadmin_CLI");

                    LogRecord logRec = msgProvider.createLogRecord(msgid,
                        msgdata, ssot);
                    if (logRec != null) {
                        logger.log(logRec, ssot);
                    }
                } catch (Exception ioe) {
                    debug.error("Error getting log message provider: " +
                        ioe.getMessage());
                }
            }
        }
    }

    /**
     * Method to write Debug errors given in the -debug/-verbose option of
     * amadmin CLI.
     *
     * @param message
     * @param t
     */

    public static void log(String message, Throwable t) {
        if (level == VERBOSE) {
            System.err.println(message);
        } else if (level == DEBUG) {
            if (debug != null && debugEnabled) {
                debug.error(message, t);
            }
        }
    }

    /**
     * Description of the Method
     *
     * @return   Description of the Return Value
     */
    public static boolean logEnabled() {
        return verboseEnabled || debugEnabled;
    }

    static void printAttributeNameValuesMap(PrintWriter prnWriter,
        PrintUtils prnUtl, Map map)
    {
        Set set = map.keySet();

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            Object objAttribute = iter.next();
            prnWriter.println("  " + objAttribute.toString());
            prnUtl.printAVPairs((Map)map.get(objAttribute), 2);
        }
    }

    static void printAttributeNameValuesMap(
        PrintWriter prnWriter,
        PrintUtils prnUtl,
        SSOToken ssoToken,
        Map map,
        String serviceName,
        SchemaType schemaType
    ) {
        Set set = map.keySet();
        Map attrSchemas = null;

        try {
            if (schemaType == null) {
                attrSchemas = getAttributeSchemas(serviceName, ssoToken);
            } else {
                attrSchemas = getAttributeSchemas(
                    serviceName, schemaType, ssoToken);
            }
        } catch (AdminException ae) {
            debug.error("AdminUtils.printAttributeNameValuesMap", ae);
            attrSchemas = Collections.EMPTY_MAP;
        }

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            Object objAttribute = iter.next();
            prnWriter.println("  " + objAttribute.toString());
            Map values = maskPassword((Map)map.get(objAttribute), attrSchemas);
            prnUtl.printAVPairs(values, 2);
        }
    }



    private static Map maskPassword(Map map, Map attrSchemas) {
        Map masked = new HashMap(map.size() *2);

        for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            AttributeSchema as = (AttributeSchema)attrSchemas.get(
                attrName.toLowerCase());
            boolean isPassword = false;

            if (as != null) {
                AttributeSchema.Syntax syntax = as.getSyntax();
                isPassword = syntax.equals(AttributeSchema.Syntax.PASSWORD) ||
                    syntax.equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD);
            }

            if (isPassword) {
                Set set = new HashSet(2);
                set.add("********");
                masked.put(attrName, set);
            } else {
                masked.put(attrName, map.get(attrName));
            }
        }

        return masked;
    }

    /**
     * Returns true of an <code>AMObject</code> is a child of a <code>DN</code>.
     *
     * @param obj AMObject instance
     * @param parentDN parent/ancestor distinguished name
     * @return true of an <code>AMObject</code> is a child of a <code>DN</code>.
     */
    static boolean isChildOf(AMObject obj, String parentDN) {
        return isDescendantOf(obj, parentDN, AMConstants.SCOPE_ONE);
    }

    /**
     * Returns true of an <code>AMObject</code> is a descendant of a 
     * <code>DN</code>.
     *
     * @param obj AMObject instance
     * @param parentDN parent/ancestor distinguished name
     * @return true of an <code>AMObject</code> is a descendant of a 
     * <code>DN</code>.
     */
    static boolean isDescendantOf(AMObject obj, String parentDN) {
        return isDescendantOf(obj, parentDN, AMConstants.SCOPE_SUB);
    }

    /**
     * Returns true of an <code>AMObject</code> is a descendant of a 
     * <code>DN</code>.
     *
     * @param obj AMObject instance
     * @param parentDN parent/ancestor distinguished name
     * @param scope i.e. AMConstants.SCOPE_SUB or AMConstants.SCOPE_ONE.
     * @return true of an <code>AMObject</code> is a descendant of a 
     * <code>DN</code>.
     */
    static boolean isDescendantOf(AMObject obj, String parentDN, int scope) {
        boolean isDescendant = LDAPDN.equals(obj.getDN(), parentDN);

        if (!isDescendant) {
            if (scope == AMConstants.SCOPE_ONE) {
                isDescendant = LDAPDN.equals(obj.getParentDN(), parentDN);
            } else {
                DN dn = new DN(obj.getDN());
                isDescendant = dn.isDescendantOf(new DN(parentDN));
            }
        }
        return isDescendant;
    }

    /**
     * Returns true if an <code>AMEntity</code> is a child of a <code>DN</code>.
     *
     * @param obj <code>AMEntity</code> instance.
     * @param parentDN parent/ancestor distinguished name.
     * @param scope i.e. AMConstants.SCOPE_SUB or AMConstants.SCOPE_ONE.
     * @return true if an <code>AMEntity</code> is a child of a <code>DN</code>.
     */
    static boolean isDescendantOf(AMEntity obj, String parentDN, int scope) {
        boolean isDescendant = LDAPDN.equals(obj.getDN(), parentDN);

        if (!isDescendant) {
            if (scope == AMConstants.SCOPE_ONE) {
                isDescendant = LDAPDN.equals(obj.getParentDN(), parentDN);
            } else {
                DN dn = new DN(obj.getDN());
                isDescendant = dn.isDescendantOf(new DN(parentDN));
            }
        }

        return isDescendant;
    }

    /**
     * Returns People Container <code>DN</code> of an organization
     *
     * @param organization object
     * @return People Container <code>DN</code> of an organization
     * @throws AdminException if People Container <code>DN</code> cannot be
     * obtained.
     */
    static String getPeopleContainerDN(AMOrganization organization)
        throws AdminException
    {
        try {
            Set pcDNs = organization.getPeopleContainers(AMConstants.SCOPE_ONE);
            String peopleContainerDN = null;

            if ((pcDNs != null) && !pcDNs.isEmpty()) {
                peopleContainerDN = (String)pcDNs.iterator().next();
            }

            return peopleContainerDN;
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    /**
     * Returns People Container <code>DN</code> of an organization unit
     *
     * @param organization object
     * @return People Container <code>DN</code> of an organization unit
     * @throws AdminException if People Container <code>DN</code> cannot be
     *         obtained.
     */
    static String getPeopleContainerDN(AMOrganizationalUnit organizationalUnit)
        throws AdminException
    {
        try {
            Set pcDNs = organizationalUnit.getPeopleContainers(
                AMConstants.SCOPE_ONE);
            String peopleContainerDN = null;

            if ((pcDNs != null) && !pcDNs.isEmpty()) {
                peopleContainerDN = (String)pcDNs.iterator().next();
            }

            return peopleContainerDN;
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private static Map getAttributeSchemas(
        String serviceName,
        SchemaType schemaType,
        SSOToken ssoToken
    ) throws AdminException
    {
        Map map = Collections.EMPTY_MAP;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, ssoToken);
            map = getAttributeSchemas(mgr, schemaType);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }

        return map;
    }

    private static Map getAttributeSchemas(
        ServiceSchemaManager mgr,
        SchemaType schemaType
    ) throws SSOException, SMSException {
        Map map = Collections.EMPTY_MAP;
        ServiceSchema schema = mgr.getSchema(schemaType);
        Set attrSchemas = schema.getAttributeSchemas();

        if ((attrSchemas != null) && !attrSchemas.isEmpty()) {
            map = new HashMap(attrSchemas.size() *2);

            for (Iterator iter = attrSchemas.iterator(); iter.hasNext(); ) {
                 AttributeSchema as = (AttributeSchema)iter.next();
                 map.put(as.getName().toLowerCase(), as);
            }
        }

        return map;
    }


    private static Map getAttributeSchemas(
        String serviceName,
        SSOToken ssoToken
    ) throws AdminException {
        Map map = new HashMap();
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, ssoToken);
            Set types = mgr.getSchemaTypes();

            if ((types != null) && !types.isEmpty()) {
                for (Iterator iter = types.iterator(); iter.hasNext(); ) {
                    map.putAll(
                        getAttributeSchemas(mgr, (SchemaType)iter.next()));
                }
            }
        } catch (SSOException e) {
            throw new AdminException(e);
        } catch (SMSException e) {
            throw new AdminException(e);
        }

        return map;
    }

    static DelegationPrivilege getDelegationPrivilege(
        String name,
        Set privilegeObjects
    ) {
        DelegationPrivilege dp = null;
        for (Iterator i= privilegeObjects.iterator();
            i.hasNext() && (dp == null);
        ) {
            DelegationPrivilege p = (DelegationPrivilege)i.next();
            if (p.getName().equals(name)) {
                dp = p;
            }
        }
        return dp;
    }
}
