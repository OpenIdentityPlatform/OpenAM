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
 * $Id: LogUtil.java,v 1.12 2010/01/23 00:07:41 exu Exp $
 *
 */


package com.sun.identity.saml2.logging;

import java.util.Map;
import java.util.logging.Level;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogManager;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * The <code>LogUtil</code> class defines methods which are used by
 * SAML2 compoment to write logs.
 */
public class LogUtil {

    /* Log Constants */
    public static final String INVALID_SP="INVALID_SP";
    public static final String INVALID_IDP="INVALID_IDP";
    public static final String INVALID_SSOTOKEN="INVALID_SSOTOKEN";
    public static final String MISSING_ENTITY="MISSING_ENTITY";
    public static final String MISSING_META_ALIAS="MISSING_META_ALIAS";
    public static final String METADATA_ERROR="METADATA_ERROR";
    public static final String SP_METADATA_ERROR="SP_METADATA_ERROR";
    public static final String IDP_METADATA_ERROR="IDP_METADATA_ERROR";
    public static final String SSO_NOT_FOUND="SSO_NOT_FOUND";
    public static final String SLO_NOT_FOUND="SLO_NOT_FOUND";
    public static final String REDIRECT_TO_SP="REDIRECT_TO_SP";
    public static final String REDIRECT_TO_IDP="REDIRECT_TO_IDP";
    public static final String REDIRECT_TO_AUTH="REDIRECT_TO_AUTH";

    /* Log Constants for SP Assertion Consumer Service */
    public static final String RESPONSE_NOT_FOUND_FROM_CACHE =
                                "RESPONSE_NOT_FOUND_FROM_CACHE";
    public static final String MISSING_ARTIFACT = "MISSING_ARTIFACT";
    public static final String RECEIVED_ARTIFACT = "RECEIVED_ARTIFACT";
    public static final String IDP_META_NOT_FOUND = "IDP_META_NOT_FOUND";
    public static final String CANNOT_CREATE_ARTIFACT_RESOLVE =
                                "CANNOT_CREATE_ARTIFACT_RESOLVE";
    public static final String CANNOT_GET_SOAP_RESPONSE =
                                "CANNOT_GET_SOAP_RESPONSE";
    public static final String GOT_RESPONSE_FROM_ARTIFACT =
                                "GOT_RESPONSE_FROM_ARTIFACT";
    public static final String IDP_NOT_FOUND ="IDP_NOT_FOUND";
    public static final String ARTIFACT_RESOLUTION_URL_NOT_FOUND =
                                "ARTIFACT_RESOLUTION_URL_NOT_FOUND";
    public static final String SOAP_ERROR = "SOAP_ERROR";
    public static final String SOAP_FAULT = "SOAP_FAULT";
    public static final String TOO_MANY_ARTIFACT_RESPONSE =
                                "TOO_MANY_ARTIFACT_RESPONSE";
    public static final String CANNOT_INSTANTIATE_ARTIFACT_RESPONSE =
                                "CANNOT_INSTANTIATE_ARTIFACT_RESPONSE";
    public static final String MISSING_ARTIFACT_RESPONSE =
                                "MISSING_ARTIFACT_RESPONSE";
    public static final String ARTIFACT_RESPONSE_INVALID_SIGNATURE =
                                "ARTIFACT_RESPONSE_INVALID_SIGNATURE";
    public static final String ARTIFACT_RESPONSE_INVALID_INRESPONSETO =
                                "ARTIFACT_RESPONSE_INVALID_INRESPONSETO";
    public static final String ARTIFACT_RESPONSE_INVALID_ISSUER =
                                "ARTIFACT_RESPONSE_INVALID_ISSUER";
    public static final String ARTIFACT_RESPONSE_INVALID_STATUS_CODE =
                                "ARTIFACT_RESPONSE_INVALID_STATUS_CODE";
    public static final String CANNOT_INSTANTIATE_RESPONSE_ARTIFACT =
                                "CANNOT_INSTANTIATE_RESPONSE_ARTIFACT";
    public static final String MISSING_SAML_RESPONSE_FROM_POST =
                                "MISSING_SAML_RESPONSE_FROM_POST";
    public static final String CANNOT_INSTANTIATE_RESPONSE_POST =
                                "CANNOT_INSTANTIATE_RESPONSE_POST";
    public static final String CANNOT_DECODE_RESPONSE =
                                "CANNOT_DECODE_RESPONSE";
    public static final String CANNOT_DECODE_REQUEST =
                                "CANNOT_DECODE_REQUEST";
    public static final String GOT_RESPONSE_FROM_POST =
                                "GOT_RESPONSE_FROM_POST";
    public static final String SUCCESS_FED_SSO = "SUCCESS_FED_SSO";
    public static final String FED_INFO_WRITTEN = "FED_INFO_WRITTEN";
    public static final String INVALID_INRESPONSETO_RESPONSE =
                                "INVALID_INRESPONSETO_RESPONSE";
    public static final String INVALID_ISSUER_RESPONSE =
                                "INVALID_ISSUER_RESPONSE";
    public static final String INVALID_ISSUER_REQUEST =
                                "INVALID_ISSUER_REQUEST";
    public static final String WRONG_STATUS_CODE = "WRONG_STATUS_CODE";
    public static final String ASSERTION_NOT_ENCRYPTED =
                                "ASSERTION_NOT_ENCRYPTED";
    public static final String MISSING_ASSERTION = "MISSING_ASSERTION";
    public static final String INVALID_ISSUER_ASSERTION =
                                "INVALID_ISSUER_ASSERTION";
    public static final String MISMATCH_ISSUER_ASSERTION =
                                "MISMATCH_ISSUER_ASSERTION";
    public static final String INVALID_SIGNATURE_ASSERTION =
                                "INVALID_SIGNATURE_ASSERTION";
    public static final String MISSING_SUBJECT_COMFIRMATION_DATA =
                                "MISSING_SUBJECT_COMFIRMATION_DATA";
    public static final String MISSING_RECIPIENT = "MISSING_RECIPIENT";
    public static final String WRONG_RECIPIENT = "WRONG_RECIPIENT";
    public static final String INVALID_TIME_SUBJECT_CONFIRMATION_DATA =
                                "INVALID_TIME_SUBJECT_CONFIRMATION_DATA";
    public static final String CONTAINED_NOT_BEFORE =
                                "CONTAINED_NOT_BEFORE";
    public static final String WRONG_INRESPONSETO_ASSERTION =
                                "WRONG_INRESPONSETO_ASSERTION";
    public static final String MISSING_CONDITIONS = "MISSING_CONDITIONS";
    public static final String MISSING_AUDIENCE_RESTRICTION =
                                "MISSING_AUDIENCE_RESTRICTION";
    public static final String WRONG_AUDIENCE = "WRONG_AUDIENCE";
    public static final String FOUND_AUTHN_ASSERTION =
                                "FOUND_AUTHN_ASSERTION";

    public static final String NO_ACS_URL = "NO_ACS_URL";
    public static final String NO_RETURN_BINDING = "NO_RETURN_BINDING";
    public static final String POST_TO_TARGET_FAILED = "POST_TO_TARGET_FAILED";
    public static final String CANNOT_CREATE_ARTIFACT = 
                                  "CANNOT_CREATE_ARTIFACT";
    public static final String RECEIVED_AUTHN_REQUEST =
                                  "RECEIVED_AUTHN_REQUEST";
    public static final String POST_RESPONSE = "POST_RESPONSE";
    public static final String SEND_ARTIFACT = "SEND_ARTIFACT";
    public static final String SEND_ECP_RESPONSE = "SEND_ECP_RESPONSE";
    public static final String SEND_ECP_RESPONSE_FAILED =
                                "SEND_ECP_RESPONSE_FAILED";
    public static final String CANNOT_INSTANTIATE_SOAP_MESSAGE_ECP =
                                "CANNOT_INSTANTIATE_SOAP_MESSAGE_ECP";
    public static final String RECEIVE_SOAP_FAULT_ECP =
                                "RECEIVE_SOAP_FAULT_ECP";
    public static final String CANNOT_INSTANTIATE_SAML_RESPONSE_FROM_ECP =
                                "CANNOT_INSTANTIATE_SAML_RESPONSE_FROM_ECP";
    public static final String ECP_ASSERTION_NOT_SIGNED =
                                "ECP_ASSERTION_NOT_SIGNED";
    public static final String ECP_ASSERTION_INVALID_SIGNATURE =
                                "ECP_ASSERTION_INVALID_SIGNATURE";
    public static final String RECEIVED_AUTHN_REQUEST_ECP =
                                "RECEIVED_AUTHN_REQUEST_ECP";
    public static final String RECEIVED_HTTP_REQUEST_ECP =
                                "RECEIVED_HTTP_REQUEST_ECP";
    public static final String SEND_ECP_PAOS_REQUEST =
                                "SEND_ECP_PAOS_REQUEST";
    public static final String SEND_ECP_PAOS_REQUEST_FAILED =
                                "SEND_ECP_PAOS_REQUEST_FAILED";
    public static final String INVALID_SOAP_MESSAGE = "INVALID_SOAP_MESSAGE";
    public static final String ARTIFACT_RESPONSE = "ARTIFACT_RESPONSE";
    public static final String GOT_ENTITY_DESCRIPTOR = "GOT_ENTITY_DESCRIPTOR";
    public static final String INVALID_REALM_GET_ENTITY_DESCRIPTOR =
                                "INVALID_REALM_GET_ENTITY_DESCRIPTOR";
    public static final String GOT_INVALID_ENTITY_DESCRIPTOR =
                                "GOT_INVALID_ENTITY_DESCRIPTOR";
    public static final String CONFIG_ERROR_GET_ENTITY_DESCRIPTOR =
                                "CONFIG_ERROR_GET_ENTITY_DESCRIPTOR";
    public static final String NO_ENTITY_ID_SET_ENTITY_DESCRIPTOR =
                                "NO_ENTITY_ID_SET_ENTITY_DESCRIPTOR";
    public static final String INVALID_REALM_SET_ENTITY_DESCRIPTOR =
                                "INVALID_REALM_SET_ENTITY_DESCRIPTOR";
    public static final String NO_ENTITY_DESCRIPTOR_SET_ENTITY_DESCRIPTOR =
                                "NO_ENTITY_DESCRIPTOR_SET_ENTITY_DESCRIPTOR";
    public static final String SET_ENTITY_DESCRIPTOR = "SET_ENTITY_DESCRIPTOR";
    public static final String CONFIG_ERROR_SET_ENTITY_DESCRIPTOR =
                                "CONFIG_ERROR_SET_ENTITY_DESCRIPTOR";
    public static final String SET_INVALID_ENTITY_DESCRIPTOR =
                                "SET_INVALID_ENTITY_DESCRIPTOR";
    public static final String NO_ENTITY_ID_CREATE_ENTITY_DESCRIPTOR =
                                "NO_ENTITY_ID_CREATE_ENTITY_DESCRIPTOR";
    public static final String INVALID_REALM_CREATE_ENTITY_DESCRIPTOR =
                                "INVALID_REALM_CREATE_ENTITY_DESCRIPTOR";
    public static final String ENTITY_DESCRIPTOR_EXISTS =
                                "ENTITY_DESCRIPTOR_EXISTS";
    public static final String ENTITY_DESCRIPTOR_CREATED =
                                "ENTITY_DESCRIPTOR_CREATED";
    public static final String CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR =
                                "CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR";
    public static final String CREATE_INVALID_ENTITY_DESCRIPTOR =
                                "CREATE_INVALID_ENTITY_DESCRIPTOR";
    public static final String INVALID_REALM_DELETE_ENTITY_DESCRIPTOR =
                                "INVALID_REALM_DELETE_ENTITY_DESCRIPTOR";
    public static final String NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_DESCRIPTOR =
                               "NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_DESCRIPTOR";
    public static final String ENTITY_DESCRIPTOR_DELETED =
                                "ENTITY_DESCRIPTOR_DELETED";
    public static final String CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR =
                                "CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR";
    public static final String GOT_ENTITY_CONFIG = "GOT_ENTITY_CONFIG";
    public static final String INVALID_REALM_GET_ENTITY_CONFIG =
                                "INVALID_REALM_GET_ENTITY_CONFIG";
    public static final String GOT_INVALID_ENTITY_CONFIG =
                                "GOT_INVALID_ENTITY_CONFIG";
    public static final String CONFIG_ERROR_GET_ENTITY_CONFIG =
                                "CONFIG_ERROR_GET_ENTITY_CONFIG";
    public static final String NO_ENTITY_ID_SET_ENTITY_CONFIG =
                                "NO_ENTITY_ID_SET_ENTITY_CONFIG";
    public static final String INVALID_REALM_SET_ENTITY_CONFIG =
                                "INVALID_REALM_SET_ENTITY_CONFIG";
    public static final String NO_ENTITY_DESCRIPTOR_SET_ENTITY_CONFIG =
                                "NO_ENTITY_DESCRIPTOR_SET_ENTITY_CONFIG";
    public static final String SET_ENTITY_CONFIG = "SET_ENTITY_CONFIG";
    public static final String CONFIG_ERROR_SET_ENTITY_CONFIG =
                                "CONFIG_ERROR_SET_ENTITY_CONFIG";
    public static final String SET_INVALID_ENTITY_CONFIG =
                                "SET_INVALID_ENTITY_CONFIG";
    public static final String NO_ENTITY_ID_CREATE_ENTITY_CONFIG =
                                "NO_ENTITY_ID_CREATE_ENTITY_CONFIG";
    public static final String INVALID_REALM_CREATE_ENTITY_CONFIG =
                                "INVALID_REALM_CREATE_ENTITY_CONFIG";
    public static final String NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG =
                                "NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG";
    public static final String ENTITY_CONFIG_EXISTS = "ENTITY_CONFIG_EXISTS";
    public static final String ENTITY_CONFIG_CREATED =
                                "ENTITY_CONFIG_CREATED";
    public static final String CONFIG_ERROR_CREATE_ENTITY_CONFIG =
                                "CONFIG_ERROR_CREATE_ENTITY_CONFIG";
    public static final String CREATE_INVALID_ENTITY_CONFIG =
                                "CREATE_INVALID_ENTITY_CONFIG";
    public static final String INVALID_REALM_DELETE_ENTITY_CONFIG =
                                "INVALID_REALM_DELETE_ENTITY_CONFIG";
    public static final String NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG =
                                "NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG";
    public static final String NO_ENTITY_CONFIG_DELETE_ENTITY_CONFIG =
                                "NO_ENTITY_CONFIG_DELETE_ENTITY_CONFIG";
    public static final String ENTITY_CONFIG_DELETED =
                                "ENTITY_CONFIG_DELETED";
    public static final String CONFIG_ERROR_DELETE_ENTITY_CONFIG =
                                "CONFIG_ERROR_DELETE_ENTITY_CONFIG";
    public static final String INVALID_REALM_GET_ALL_HOSTED_ENTITIES =
                                "INVALID_REALM_GET_ALL_HOSTED_ENTITIES";
    public static final String CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES =
                                "CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES";
    public static final String GOT_ALL_HOSTED_ENTITIES =
                                "GOT_ALL_HOSTED_ENTITIES";
    public static final String INVALID_REALM_GET_ALL_REMOTE_ENTITIES =
                                "INVALID_REALM_GET_ALL_REMOTE_ENTITIES";
    public static final String CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES =
                                "CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES";
    public static final String GOT_ALL_REMOTE_ENTITIES =
                                "GOT_ALL_REMOTE_ENTITIES";
    public static final String CANNOT_INSTANTIATE_MNI_RESPONSE =
                "CANNOT_INSTANTIATE_MNI_RESPONSE";
    public static final String CANNOT_INSTANTIATE_MNI_REQUEST =
                "CANNOT_INSTANTIATE_MNI_REQUEST";
    public static final String CANNOT_INSTANTIATE_SLO_RESPONSE =
                "CANNOT_INSTANTIATE_MNI_RESPONSE";
    public static final String CANNOT_INSTANTIATE_SLO_REQUEST =
                "CANNOT_INSTANTIATE_MNI_REQUEST";
    public static final String MNI_REQUEST_INVALID_SIGNATURE =
                "MNI_REQUEST_INVALID_SIGNATURE";
    public static final String MNI_RESPONSE_INVALID_SIGNATURE =
                "MNI_RESPONSE_INVALID_SIGNATURE";
    public static final String SLO_REQUEST_INVALID_SIGNATURE =
                "SLO_REQUEST_INVALID_SIGNATURE";
    public static final String SLO_RESPONSE_INVALID_SIGNATURE =
                "SLO_RESPONSE_INVALID_SIGNATURE";
    public static final String NAMEID_INVALID_ENCRYPTION =
                "NAMEID_INVALID_ENCRYPTION";
    public static final String INVALID_MNI_RESPONSE =
                "INVALID_MNI_RESPONSE";
    public static final String INVALID_SLO_RESPONSE =
                "INVALID_SLO_RESPONSE";
    public static final String MISSING_ENTITY_ROLE =
                "MISSING_ENTITY_ROLE";
    public static final String INVALID_REALM_GET_ALL_ENTITIES =
                                "INVALID_REALM_GET_ALL_ENTITIES";
    public static final String CONFIG_ERROR_GET_ALL_ENTITIES =
                                "CONFIG_ERROR_GET_ALL_ENTITIES";
    public static final String GOT_ALL_ENTITIES = "GOT_ALL_ENTITIES";
    public static final String NAME_ID = "NameID";

    // Log constants (message id) for SAE
    public static final String SAE_IDP_SUCCESS = "SAE_IDP_SUCCESS";
    public static final String SAE_IDP_ERROR = "SAE_IDP_ERROR";
    public static final String SAE_IDP_ERROR_NODATA = "SAE_IDP_ERROR_NODATA";
    public static final String SAE_IDP_AUTH = "SAE_IDP_AUTH";
/*
    public static final String SAE_IDP_LOGOUT_SUCCESS = 
        "SAE_IDP_LOGOUT_SUCCESS";
    public static final String SAE_IDP_LOGOUT_ERROR = "SAE_IDP_LOGOUT_ERROR";
*/
    public static final String SAE_SP_SUCCESS = "SAE_SP_SUCCESS";
    public static final String SAE_SP_ERROR = "SAE_SP_ERROR";
    //public static final String SAE_SP_LOGOUT_SUCCESS = "SAE_SP_LOGOUT_SUCCESS";
    //public static final String SAE_SP_LOGOUT_ERROR = "SAE_SP_LOGOUT_ERROR";
    
    public static final String INVALID_PEP_ID="INVALID_PEP_ID";
    public static final String INVALID_PDP_ID="INVALID_PDP_ID";
    
    //LogConstants for SAMLv2 SOAPBinding 
    public static final String REQUEST_MESSAGE="REQUEST_MESSAGE";
    public static final String NULL_PDP_SIGN_CERT_ALIAS =
            "NULL_PDP_SIGN_CERT_ALIAS";
    public static final String NULL_PEP_SIGN_CERT_ALIAS =
            "NULL_PEP_SIGN_CERT_ALIAS";
    public static final String INVALID_SIGNATURE_QUERY=
            "INVALID_SIGNATURE_QUERY";
    public static final String VALID_SIGNATURE_QUERY="VALID_SIGNATURE_QUERY";
    public static final String PEP_METADATA_ERROR="PEP_METADATA_ERROR";
    public static final String PDP_METADATA_ERROR="PDP_METADATA_ERROR";

    /**
     * The Domain field. The Domain pertaining to the log record's
     * Data field.
     */
    public static final String DOMAIN = "Domain";
    /**
     * The LoginID field. The Login ID pertaining to the log record's
     * Data field.
     */
    public static final String LOGIN_ID = "LoginID";
    /**
     * The IPAddr field. The IP Address pertaining to the log record's
     * Data field.
     */
    public static final String IP_ADDR = "IPAddr";
    /**
     * The ModuleName field. The Module pertaining to the log record's
     * Data field.
     */
    public static final String MODULE_NAME = "ModuleName";

    public static final String INVALID_ISSUER_IN_PEP_REQUEST=
            "INVALID_ISSUER_IN_PEP_REQUEST";
    public static final String ASSERTION_FROM_PDP_NOT_ENCRYPTED=
            "ASSERTION_FROM_PDP_NOT_ENCRYPTED";
    public static final String MISSING_ASSERTION_IN_PDP_RESPONSE=
            "MISSING_ASSERTION_IN_PDP_RESPONSE";
    public static final String INVALID_ISSUER_IN_ASSERTION_FROM_PDP=
            "INVALID_ISSUER_IN_ASSERTION_FROM_PDP";
    public static final String MISMATCH_ISSUER_IN_ASSERTION_FROM_PDP=
            "MISMATCH_ISSUER_IN_ASSERTION_FROM_PDP";
    public static final String INVALID_SIGNATURE_ASSERTION_FROM_PDP=
            "INVALID_SIGNATURE_ASSERTION_FROM_PDP";
    public static final String SUCCESS_FED_TERMINATION = 
            "SUCCESS_FED_TERMINATION";
    public static final String SUCCESS_NEW_NAMEID = "SUCCESS_NEW_NAMEID";
    public static final String UNKNOWN_PRINCIPAL = "UNKNOWN_PRINCIPAL";
    public static final String UNABLE_TO_TERMINATE = "UNABLE_TO_TERMINATE";
    public static final String POST_RESPONSE_INVALID_SIGNATURE =
                                "POST_RESPONSE_INVALID_SIGNATURE";
    public static final String BINDING_NOT_SUPPORTED = "BINDING_NOT_SUPPORTED";
    public static final String SP_SSO_FAILED = "SP_SSO_FAILED";
                                          
    // 
    public static final String INVALID_REALM_FOR_SESSION = "INVALID_REALM_FOR_SESSION";
    private static final String SAML2_LOG = "SAML2";
    private static Logger logger = null;

    
    static {
        try {
            logger = LogManager.getLogger(SAML2_LOG);
        } catch (LogException le) {
            SAML2Utils.debug.error("LogUtil.static: Error getting logger:", le);
        }
    }
    
    /**
     * Logs message to SAML2 access logs.
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
        access(level, msgid, data, null, null);
    }

    /**
     * Logs message to SAML2 access logs.
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
        access(level, msgid, data, session, null);
    }

    /**
     * Logs message to SAML2 access logs.
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
     * @param props extra log fields
     */
    public static void access(
        Level level, String msgid, String data[],
        Object session, Map props) 
    {
        if (logger != null) {
            try {
                logger.access(level, msgid, data, session, props);
            } catch (LogException le) {
                SAML2Utils.debug.error(
                    "LogUtil.access: Couldn't write log:", le);
            }
        }
    }
    
    /**
     * Logs error messages to SAML2 error log.
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
         error(level,msgid,data,null, null);
     }

     /** 
     * Logs error messages to SAML2 error log.
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
        error(level, msgid, data, session, null);
    }


     /** 
     * Logs error messages to SAML2 error log.
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
     * @param props extra log fields
      */
    public static void error(
        Level level, String msgid, String data[],
        Object session, Map props) 
    {
        if (logger != null) {
            try {
                logger.error(level, msgid, data, session, props);
            } catch (LogException le) {
                SAML2Utils.debug.error("LogUtil.error:Couldn't write log:",le);
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
