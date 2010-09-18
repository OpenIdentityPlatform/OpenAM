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
 * $Id: LogUtil.java,v 1.4 2008/12/19 06:50:45 exu Exp $
 *
 */

package com.sun.identity.federation.common;

import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.LogManager;
import java.util.logging.Level;

/**
 * The <code>LogUtil</code> class provides methods which are used by
 * federation compoment to write logs.
 */
public class LogUtil {

    /**
     * Write Account Federation information
     */
    public static final String WRITE_ACCOUNT_FED_INFO =
        "WRITE_ACCOUNT_FED_INFO";

    /**
     * Remove Account Federation information.
     */
    public static final String REMOVE_ACCOUNT_FED_INFO =
        "REMOVE_ACCOUNT_FED_INFO";

    /**
     * Create Authentication Domain.
     */
    public static final String CREATE_AUTH_DOMAIN = "CREATE_AUTH_DOMAIN";

    /**
     * Delete Authentication Domain.
     */
    public static final String DELETE_AUTH_DOMAIN = "DELETE_AUTH_DOMAIN";

    /**
     * Modify Authentication Domain .
     */
    public static final String MODIFY_AUTH_DOMAIN = "MODIFY_AUTH_DOMAIN";

    /**
     * Create Remote Provider.
     */
    public static final String CREATE_REMOTE_PROVIDER =
        "CREATE_REMOTE_PROVIDER";

   /**
    *  Create Hosted Provider.
    */
    public static final String CREATE_HOSTED_PROVIDER =
        "CREATE_HOSTED_PROVIDER";

    /**
     * Delete Affliation.
     */
    public static final String DELETE_AFFILIATION = "DELETE_AFFILIATION";

    /**
     * Delete Entity.
     */
    public static final String DELETE_ENTITY = "DELETE_ENTITY";

    /**
     * Delete Provider.
     */
    public static final String DELETE_PROVIDER = "DELETE_PROVIDER";

    /**
     * Modify Entity.
     */
    public static final String MODIFY_ENTITY = "MODIFY_ENTITY";

    /**
     * Modify Affliation.
     */
    public static final String MODIFY_AFFILIATION = "MODIFY_AFFILIATION";

    /**
     * Modify Provider.
     */
    public static final String MODIFY_PROVIDER = "MODIFY_PROVIDER";


    /**
     * Create Entity.
     */
    public static final String CREATE_ENTITY = "CREATE_ENTITY";

    /**
     * Create Affliation.
     */
    public static final String CREATE_AFFILIATION = "CREATE_AFFILIATION";

    /**
     * No SAML Response.
     */
    public static final String MISSING_RESPONSE = "MISSING_RESPONSE";

    /**
     * Create SAML Assertion .
     */
    public static final String CREATE_ASSERTION = "CREATE_ASSERTION";

    /**
     * Federation Management Disabled.
     */
    public static final String LIBERTY_NOT_ENABLED = "LIBERTY_NOT_ENABLED";

    /**
     * Logout Request processing failed.
     */
    public static final String LOGOUT_REQUEST_PROCESSING_FAILED = 
        "LOGOUT_REQUEST_PROCESSING_FAILED";

    /**
     *  No SOAP Message Factory.
     */
    public static final String MISSING_SOAP_MSG_FACTORY = 
        "MISSING_SOAP_MSG_FACTORY";

    /**
     * SOAP URL End Point Creation Failed.
     */
    public static final String FAILED_SOAP_URL_END_POINT_CREATION = 
        "FAILED_SOAP_URL_END_POINT_CREATION";

    /**
     * Mismatch AuthType and the protocol (based on SOAPUrl).
     */
    public static final String MISMATCH_AUTH_TYPE_AND_PROTOCOL = 
       "MISMATCH_AUTH_TYPE_AND_PROTOCOL";

    /**
     * Incorrect Authentication type.
     */
    public static final String WRONG_AUTH_TYPE = "WRONG_AUTH_TYPE";

    /**
     * SAML SOAP Receiver URL
     */
    public static final String SOAP_RECEIVER_URL = "SOAP_RECEIVER_URL";

    /**
     * SOAP Response is invalid.
     */
    public static final String INVALID_SOAP_RESPONSE = "INVALID_SOAP_RESPONSE";

    /**
     * Response is invalid.
     */
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";

    /**
     * Assertion is invalid.
     */
    public static final String INVALID_ASSERTION = "INVALID_ASSERTION";

    /**
     * Single Signon failed
     */
    public static final String SINGLE_SIGNON_FAILED = "SINGLE_SIGNON_FAILED";

    /**
     * Access granted, redirecting to url.
     */
    public static final String ACCESS_GRANTED_REDIRECT_TO = 
        "ACCESS_GRANTED_REDIRECT_TO";

    /**
     * Input parameter is null.
     */
    public static final String NULL_INPUT_PARAMETER = "NULL_INPUT_PARAMETER";

    /**
     * No Authentication Response
     */
    public static final String MISSING_AUTHN_RESPONSE = 
        "MISSING_AUTHN_RESPONSE";

    /**
     * Account federation failed.
     */
    public static final String ACCOUNT_FEDERATION_FAILED =
        "ACCOUNT_FEDERATION_FAILED";

    /**
     * Failed to generation SSOToken
     */
    public static final String FAILED_SSO_TOKEN_GENERATION = 
        "FAILED_SSO_TOKEN_GENERATION";

    /**
     * Invalid Authentication Response
     */
    public static final String INVALID_AUTHN_RESPONSE = 
        "INVALID_AUTHN_RESPONSE";

    /**
     * Authentication Request processing failed.
     */
    public static final String AUTHN_REQUEST_PROCESSING_FAILED =
        "AUTHN_REQUEST_PROCESSING_FAILED";
        
    /**
     * Signature verification failed
     */
    public static final String SIGNATURE_VERIFICATION_FAILED = 
        "SIGNATURE VERIFICATION FAILED";
    
    /**
     * Error building response
     */
    public static final String CANNOT_BUILD_RESPONSE = "CANNOT_BUILD_RESPONSE";

    /**
     * Create SAML Response
     */
    public static final String CREATE_SAML_RESPONSE = "CREATE_SAML_RESPONSE";

    /**
     *  Redirect to URL
     */
    public static final String REDIRECT_TO = "REDIRECT_TO";

    /**
     * Common Domain Service Information not found.
     */
    public static final String COMMON_DOMAIN_META_DATA_NOT_FOUND = 
        "COMMON_DOMAIN_META_DATA_NOT_FOUND";

    /**
     * Invalid Request ID
     */
    public static final String INVALID_REQUEST_ID = "INVALID_REQUEST_ID";


    /**
     * Provider is not trusted.
     */
    public static final String PROVIDER_NOT_TRUSTED = "PROVIDER_NOT_TRUSTED";


    /**
     * Authentication Request is invalid.
     */
    public static final String INVALID_AUTHN_REQUEST = "INVALID_AUTHN_REQUEST";

    /**
     * Account Federation Information not found for user.
     */
    public static final String USER_ACCOUNT_FEDERATION_INFO_NOT_FOUND = 
        "USER_ACCOUNT_FEDERATION_INFO_NOT_FOUND";

    /**
     * Authentication Request is invalid.
     */
    public static final String INVALID_AUTHN_REQUEST_EXCEPTION = 
        "INVALID_AUTHN_REQUEST_EXCEPTION";

    /**
     * User not found.
     */
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    /**
     * Logout profile not supported.
     */
    public static final String LOGOUT_PROFILE_NOT_SUPPORTED = 
        "LOGOUT_PROFILE_NOT_SUPPORTED";

    /**
     * Logout Success
     */
    public static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";

    /**
     * Logout failed to redirect due to incorrect URL.
     */
    public static final String LOGOUT_REDIRECT_FAILED = 
        "LOGOUT_REDIRECT_FAILED";

    /**
     * Logout request not formed properly.
     */
    public static final String LOGOUT_FAILED_REQUEST_IMPROPER = 
        "LOGOUT_FAILED_REQUEST_IMPROPER";

    /**
     * Logout Failed, Invalid Handler
     */
    public static final String LOGOUT_FAILED_INVALID_HANDLER = 
        "LOGOUT_FAILED_INVALID_HANDLER";

    /**
     * Logout Failed , no Provider
     */
    public static final String INVALID_PROVIDER = "INVALID_PROVIDER";

    /**
     * Logout Failed
     */
    public static final String LOGOUT_FAILED = "LOGOUT_FAILED";

    /**
     * Create SPProvidedNameIdentifier failed.
     */
    public static final String REGISTRATION_FAILED_SP_NAME_IDENTIFIER = 
        "REGISTRATION_FAILED_SP_NAME_IDENTIFIER";

    /**
     * Invalid Signature for Registration.
     */
    public static final String INVALID_SIGNATURE =
        "INVALID_SIGNATURE";

    /**
     * Federation Termination failed locally.
     */
    public static final String TERMINATION_FAILED = 
        "TERMINATION_FAILED";

    /**
     * Termination success.
     */
    public static final String TERMINATION_SUCCESS = 
        "TERMINATION_SUCCESS";

    /**
     * Termination Request Processing Failed.
     */
    public static final String TERMINATION_REQUEST_PROCESSING_FAILED =
        "TERMINATION_REQUEST_PROCESSING_FAILED";
   
    /**
     * Unable to get configuration instance for IDFF Meta Service.
     */
    public static final String ERROR_GET_IDFF_META_INSTANCE = 
        "ERROR_GET_IDFF_META_INSTANCE";

    /**
     * Null Entity Descriptor.
     */
    public static final String NULL_ENTITY_DESCRIPTOR = 
        "NULL_ENTITY_DESCRIPTOR";

   
    /**
     * Null Entity ID.
     */
    public static final String NULL_ENTITY_ID = "NULL_ENTITY_ID";

    /**
     * Entity created successfully. 
     */
    public static final String CREATE_ENTITY_SUCCEEDED = 
        "CREATE_ENTITY_SUCCEEDED";

    /**
     * Unable to create entity. 
     */
    public static final String CREATE_ENTITY_FAILED = "CREATE_ENTITY_FAILED";

    /**
     * Unsupported operation.
     */
    public static final String UNSUPPORTED_OPERATION = "UNSUPPORTED_OPERATION";

    /**
     * Invalid Entity Descriptor. 
     */
    public static final String INVALID_ENTITY_DESCRIPTOR = 
        "INVALID_ENTITY_DESCRIPTOR";

    /**
     * Failed to get Entity. 
     */
    public static final String GET_ENTITY_FAILED = "GET_ENTITY_FAILED";

    /**
     * Entity returned successfully. 
     */
    public static final String GET_ENTITY_SUCCEEDED = "GET_ENTITY_SUCCEEDED";

    /**
     * Entity modified successfully. 
     */
    public static final String SET_ENTITY_SUCCEEDED = 
        "SET_ENTITY_SUCCEEDED";

    /**
     * Failed to modify Entity. 
     */
    public static final String SET_ENTITY_FAILED = "SET_ENTITY_FAILED";

    /**
     * Entity deleted succcessfully.  
     */
    public static final String DELETE_ENTITY_SUCCEEDED = 
        "DELETE_ENTITY_SUCCEEDED";

    /**
     * Entity does not exist.  
     */
    public static final String ENTITY_DOES_NOT_EXISTS = 
        "ENTITY_DOES_NOT_EXISTS";

    /**
     * Failed to delete Entity. 
     */
    public static final String DELETE_ENTITY_FAILED = 
        "DELETE_ENTITY_FAILED";

    /**
     * Null Entity configuration. 
     */
    public static final String NULL_ENTITY_CONFIG = 
        "NULL_ENTITY_CONFIG";

    /**
     * No entity configuration to delete.
     */
    public static final String NO_ENTITY_CONFIG_TO_DELETE =
        "NO_ENTITY_CONFIG_TO_DELETE";

    /**
     * Failed to delete entity configuration. 
     */
    public static final String DELETE_ENTITY_CONFIG_FAILED =
        "DELETE_ENTITY_CONFIG_FAILED";

    /**
     * Entity configuration deleted successfully.
     */
    public static final String DELETE_ENTITY_CONFIG_SUCCEEDED =
        "DELETE_ENTITY_CONFIG_SUCCEEDED";

    /**
     * Unable to find entity configuration. 
     */
    public static final String ENTITY_CONFIG_NOT_FOUND = 
        "ENTITY_CONFIG_NOT_FOUND";

    /**
     * Entity configuration already exists. 
     */
    public static final String ENTITY_CONFIG_EXISTS = 
        "ENTITY_CONFIG_EXISTS";

    /**
     * Entity configuration modified successfully. 
     */
    public static final String SET_ENTITY_CONFIG_SUCCEEDED = 
        "SET_ENTITY_CONFIG_SUCCEEDED";

    /**
     * Failed to modify Entity configuration. 
     */
    public static final String SET_ENTITY_CONFIG_FAILED = 
        "SET_ENTITY_CONFIG_FAILED";

    /**
     * Entity configuration created successfully. 
     */
    public static final String CREATE_ENTITY_CONFIG_SUCCEEDED = 
        "CREATE_ENTITY_CONFIG_SUCCEEDED";

    /**
     * Failed to create Entity configuration. 
     */
    public static final String CREATE_ENTITY_CONFIG_FAILED = 
        "CREATE_ENTITY_CONFIG_FAILED";

    /**
     * Invallid Entity configuration. 
     */
    public static final String INVALID_ENTITY_CONFIG = "INVALID_ENTITY_CONFIG";

    /**
     * Failed to get Entity configuration. 
     */
    public static final String GET_ENTITY_CONFIG_FAILED = 
        "GET_ENTITY_CONFIG_FAILED";

    /**
     * Entity configuration returned successfully. 
     */
    public static final String GET_ENTITY_CONFIG_SUCCEEDED = 
        "GET_ENTITY_CONFIG_SUCCEEDED";

    /**
     * All Entities returned successfully. 
     */
    public static final String GET_ALL_ENTITIES_SUCCEEDED = 
        "GET_ALL_ENTITIES_SUCCEEDED";

    /**
     * Failed to get all Entities. 
     */
    public static final String GET_ALL_ENTITIES_FAILED = 
        "GET_ALL_ENTITIES_FAILED";

    /**
     * All Entity names returned successfully. 
     */
    public static final String GET_ENTITY_NAMES_SUCCEEDED = 
        "GET_ENTITY_NAMES_SUCCEEDED";

    /**
     * Failed to get all Entity names. 
     */
    public static final String GET_ENTITY_NAMES_FAILED = 
        "GET_ENTITY_NAMES_FAILED";

    /**
     * All hosted Entities returned successfully. 
     */
    public static final String GET_HOSTED_ENTITIES_SUCCEEDED = 
        "GET_HOSTED_ENTITIES_SUCCEEDED";

    /**
     * Failed to get all hosted Entities. 
     */
    public static final String GET_HOSTED_ENTITIES_FAILED = 
        "GET_HOSTED_ENTITIES_FAILED";

    /**
     * All remote Entities returned successfully. 
     */
    public static final String GET_REMOTE_ENTITIES_SUCCEEDED = 
        "GET_REMOTE_ENTITIES_SUCCEEDED";

    /**
     * Failed to get all remote Entities. 
     */
    public static final String GET_REMOTE_ENTITIES_FAILED = 
        "GET_REMOTE_ENTITIES_FAILED";

    /**
     * All hosted service providers returned successfully. 
     */
    public static final String GET_HOSTED_SERVICE_PROVIDERS_SUCCEEDED = 
        "GET_HOSTED_SERVICE_PROVIDERS_SUCCEEDED";

    /**
     * All remote service providers returned successfully. 
     */
    public static final String GET_REMOTE_SERVICE_PROVIDERS_SUCCEEDED = 
        "GET_REMOTE_SERVICE_PROVIDERS_SUCCEEDED";

    /**
     * All hosted identity providers returned successfully. 
     */
    public static final String GET_HOSTED_IDENTITY_PROVIDERS_SUCCEEDED = 
        "GET_HOSTED_IDENTITY_PROVIDERS_SUCCEEDED";

    /**
     * All remote identity providers returned successfully. 
     */
    public static final String GET_REMOTE_IDENTITY_PROVIDERS_SUCCEEDED = 
        "GET_REMOTE_IDENTITY_PROVIDERS_SUCCEEDED";

    /**
     * Checking affiliation member returned successfully. 
     */
    public static final String IS_AFFILIATE_MEMBER_SUCCEEDED = 
        "IS_AFFILIATE_MEMBER_SUCCEEDED";

    /**
     * Created Authn Response.
     */
    public static final String CREATE_AUTHN_RESPONSE = "CREATE_AUTHN_RESPONSE";

    /**
     * Sent Authn Response.
     */
    public static final String SENT_AUTHN_RESPONSE = "SENT_AUTHN_RESPONSE";

    private static Logger logger = null;

    static {
        try {
            logger = LogManager.getLogger(IFSConstants.IDFF);
        } catch (LogException le) {
           FSUtils.debug.message("Error getting logger:", le);
        }
    }

    /**
     * Logs message to ID-FF access logs.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     */
    public static void access(Level level, String msgid, String data[]) {
        access(level, msgid, data, null);
    }

    /**
     * Logs message to ID-FF access logs.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's session object
     */
    public static void access(
        Level level, String msgid, String data[], Object session) 
    {
        if (logger != null) {
            try {
                logger.access(level, msgid, data, session);
            } catch (LogException le) {
                FSUtils.debug.error("LogUtil.access: Couldn't write log:", le);
            }
        }
    }
    
    /**
     * Logs error messages to ID-FF error log.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     */
     public static void error(Level level, String msgid, String data[]) {
         error(level,msgid,data,null);
     }

     /** 
     * Logs error messages to ID-FF error log.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's Session object.
      */
    public static void error(
        Level level, String msgid, String data[], Object session) 
    {
        if (logger != null) {
            try {
                logger.error(level, msgid, data, session);
            } catch (LogException le) {
                FSUtils.debug.error("LogUtil.error: Couldn't write log:", le);
            }
        } 
    }

    /**
     * Checks if the logging is enabled.
     *
     * @return true if logging is enabled.
     */
    public boolean isLogEnabled() {
        if (logger == null) {
            return false;
        } else {
            return logger.isLogEnabled();
        }
    }
    /**
     * Checks if an access message of the given level would actually be logged
     * by this logger. This check is based on the Loggers effective level.
     * @param level a message logging level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isAccessLoggable(Level level) {
        if (logger == null) {
            return false;
        } else {
            return logger.isAccessLoggable(level);
        }
    }

    /**
     * Checks if an error message of the given level would actually be logged
     * by this logger. This check is based on the Loggers effective level.
     * @param level a message logging level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isErrorLoggable(Level level) {
        if (logger == null) {
            return false;
        } else {
            return logger.isErrorLoggable(level);
        }
    }
}
