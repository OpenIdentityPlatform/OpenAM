/**
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
 * $Id: IUMSConstants.java,v 1.3 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

/**
 * This interface represents a collection of common constants used by the
 * classes in UMS. Classes implementing this interface can use these constants.
 * The constants pertain to messages defined in properties files.
 */

public interface IUMSConstants {
    public static final String UMS_PKG = "amSDK";

    public static final String UMS_DEBUG = "amSDK";

    public static final String UMS_BUNDLE_NAME = "amSDK";

    public static final String ENTRY_NOT_FOUND = "ums-entry_not_found";

    public static final String UNABLE_TO_READ_ENTRY = 
        "ums-unable_to_read_entry";

    public static final String ENTRY_ALREADY_EXISTS = 
        "ums-entry_already_exists";

    public static final String INSUFFICIENT_ACCESS_ADD = 
        "ums-insufficient_access_add";

    public static final String INSUFFICIENT_ACCESS_DELETE = 
        "ums-insufficient_access_delete";

    public static final String UNABLE_TO_ADD_ENTRY = "ums-unable_to_add_entry";

    public static final String UNABLE_TO_DELETE_ENTRY =
        "ums-unable_to_delete_entry";

    public static final String NULL_TOKEN = "ums-null-token";

    public static final String BAD_TOKEN_HDL = "ums-badsesshdl";

    public static final String INVALID_TOKEN = "ums-invalidssotoken";

    public static final String BAD_TEMPLATE = "ums-bad-template";

    public static final String BAD_PRINCIPAL_HDL = "ums-bad-principal-hdl";

    public static final String BAD_ATTRNAMES = "ums-badattrnames";

    public static final String NULL_GUIDS = "ums-nullguids";

    public static final String USER_NOT_IN_GROUP_SCOPE = 
        "ums-usernotingroupscope";

    public static final String ILLEGAL_ADGROUP_SCOPE = 
        "ums-illegaladgroupscope";

    public static final String ERROR_CM_INITIATE = "ums-cminitiate";

    public static final String ERROR_CM = "ums-cm";

    public static final String NO_VALUE = "ums-novalue";

    public static final String ATTR_NOT_ALLOWED = "ums-attrnotallowed";

    public static final String TEMPLATE_NO_ATTR = "ums-templatenoattribute";

    public static final String SEARCH_FAILED = "ums-searchfailed";

    public static final String BAD_ID = "ums-badid";

    public static final String NEW_INSTANCE_FAILED = "ums-newinstancefailed";

    public static final String ILLEGAL_GROUP_SCOPE = "ums-illegalgroupscope";

    public static final String INSTANCE_FAILED = "ums-instancefailed";

    public static final String PERSISTENT_OBJECT_PARAM_NULL = 
        "ums-badpersistentobject";

    public static final String OBJECT_NOT_PERSISTENT = 
        "ums-objectnotpersistent";

    public static final String CONFIG_MGR_ERROR = "ums-configmanagererror";

    public static final String STRUCTURE_TEMPLATE_ATTRSET_NULL = 
        "ums-structuretemplateattrsetnull";

    public static final String BAD_STRUCTURE_TEMPLATE_PRIORITY = 
        "ums-badstructuretemplatepriority";

    public static final String NULL_SESSION = "ums-nullsession";

    public static final String ROLE_CONTAINED = "ums-rolecontained";

    public static final String BAD_OBJ_TO_ADD = "ums-badobjtoadd";

    public static final String BAD_GUID = "ums-badguid";

    public static final String NO_REQUIRED = "ums-norequired";

    public static final String ADD_NULL_OBJ = "ums-addnullobj";

    public static final String COMPOSE_GUID_FAILED = "ums-composeguidfailed";

    public static final String DEL_NULL_OBJ = "ums-delnullobj";

    public static final String BAD_CHILD_OBJ = "ums-badchildobj";

    public static final String NO_NAMING_ATTR = "ums-nonamingattr";

    public static final String BAD_NAMING_ATTR = "ums-badnamingattr";

    public static final String UNMATCHED_CLASS = "ums-unmatchedclass";

    public static final String NULL_PRINCIPAL = "ums-null-principal";

    public static final String ATTRIBUTETYPE_NOT_FOUND = 
        "ums-attributetypenotfound";

    public static final String OBJECTCLASS_NOT_FOUND = 
        "ums-objectclassnotfound";

    public static final String READING_LDIF_FAILED = "ums-readingldiffailed";

    public static final String MULTIPLE_ENTRY = "ums-multipleentries";

    public static final String NO_RECURSION_ALLOW = "ums-recursionnotallow";

    public static final String MISSING_TEMPL_NAME = "ums-missingtemplname";

    public static final String BAD_CLASS = "ums-badclass";

    public static final String NEXT_ENTRY_FAILED = "ums-nextentryfailed";

    public static final String READ_ATTRIBUTES_ERROR =
        "ums-readattributeserror";

    public static final String REPLACE_DEFINITION_NOT_PERSISTENT = 
        "cos-replace_definition_not_persistent";

    public static final String COS_DEF_OR_TARGET_OBJECT_NULL = 
        "cos-cos_def_or_target_object_null";

    public static final String COS_TARGET_OBJECT_NOT_PERSISTENT = 
        "cos-cos_target_object_not_persistent";

    public static final String INVALID_COS_ATTRIBUTE_QUALIFIER = 
        "cos-cos_invalid_cos_attribute_qualifier";

    public static final String INVALID_COSDEFINITION = 
        "cos-invalid_cosdefinition";

    public static final String COS_DEFINITION_NOT_FOUND = 
        "cos-cos_definition_not_found";

    public static final String COS_DEFINITION_NOT_PERSISTENT = 
        "cos-cos_definition_not_persistent";

    public static final String COS_TARGET_OBJECT_DIFFERENT_TREE = 
        "cos-cos_target_object_different_tree";

    public static final String COS_TEMPLATE_NOT_FOUND = 
        "cos-cos_template_not_found";

    public static final String DEFINITION_NOT_PERSISTENT = 
        "cos-definition_not_persistent";

    public static final String BAD_COS_ATTR_QUALIFIER = 
        "cos-bad_cos_attr_qualifier";

    public static final String DATA_CONSTRAINT = 
        "validation-dataconstraint";

    public static final String NO_POLICY_DOMAIN = "policy-nopolicydomain";

    public static final String POLICY_ROOT_NOT_FOUND = 
        "policy-policy_root_not_found";

    public static final String POLICY_DOMAIN_NOT_FOUND = 
        "policy-policy_domain_not_found";

    public static final String POLICY_NOT_FOUND = "policy-policy_not_found";

    public static final String POLICY_EXISTS_FOR_NAME = 
        "policy-policy_exists_for_policy_name";

    public static final String POLICY_EXISTS_FOR_RESOURCE_ACTION = 
        "policy-policy_exists_for_resource_action";

    public static final String POLICIES_DO_NOT_MATCH_BY_NAME_RESOURCE_ACTION = 
        "policy-policies_do_not_match_by_name_resource_action";

    public static final String SSO_NOPROVIDERPROPERTY = 
        "sso-noproviderproperty";

    public static final String SSO_NOPROVIDERCLASS = "sso-noproviderclass";

    public static final String SSO_NOPROVIDERINSTANCE = 
        "sso-noproviderinstance";

    public static final String SSO_ILLEGALACCESS = "sso-illegalaccess";

    public static final String DSCFG_DIRSERVER_NODE_EXPECTED = 
        "dscfg-dirserver_node_expected";

    public static final String DSCFG_INVALID_BASE_DN = 
        "dscfg-invalid_base_dn";

    public static final String DSCFG_SERVERGROUP_NODE_EXPECTED =
        "dscfg-servergroup_node_expected";

    public static final String DSCFG_NOCFGMGR = "dscfg-nocfgmgr";

    public static final String DSCFG_CONNECTFAIL = "dscfg-connectFail";

    public static final String DSCFG_UNSUPPORTEDSERVERCTRL = 
        "dscfg-unsupportedServerCtrl";

    public static final String DSCFG_UNSUPPORTEDLSTNRTYPE = 
        "dscfg-unsupportedLstnrType";

    public static final String DSCFG_CTRLERROR = "dscfg-ctrlError";

    public static final String DSCFG_SERVER_NOT_FOUND = "dscfg-serverNotFound";

    public static final String DSCFG_NO_FILE_PATH = 
        "dscfg-no-file-path-specified";

    public static final String DSCFG_JSSSFFAIL = "dscfg-jssSockFactoryFail";

    public static final String SMSSCHEMA_SERVICE_NOTFOUND = 
        "sms-SMSSchema_service_notfound";

    public static final String SMS_INVALID_ATTR_NAME = "sms-INVALID_ATTR_NAME";

    public static final String SMS_INVALID_SEARCH_PATTERN = 
        "sms-invalid-search-filter";

    public static final String SMS_READONLY_OBJ = "sms-readonly_obj";

    public static final String SMS_INVALID_METHOD = "sms-invalidMethod";

    public static final String SMS_INVALID_PARAMETERS = 
        "sms-INVALID_PARAMETERS";

    public static final String SMS_INVALID_OP_VALUE = "sms-INVALID_OP_VALUE";

    public static final String SMS_INVALID_ATTR_ENTRY =
        "sms-INVALID_ATTR_ENTRY";

    public static final String SMS_INVALID_CLASS_NAME = 
        "sms-INVALID_CLASS_NAME";

    public static final String SMS_INVALID_DN = "sms-INVALID_DN";

    public static final String SMS_CANNOT_CREATE_INSTANCE =
        "sms-CANNOT_CREATE_INSTANCE";

    public static final String SMS_ATTR_OR_VAL_EXISTS = 
        "sms-ATTR_OR_VAL_EXISTS";

    public static final String SMS_NODE_ALREADY_EXISTS = 
        "sms-NODE_ALREADY_EXISTS";

    public static final String SMS_NO_SUCH_ATTRIBUTE = "sms-NO_SUCH_ATTRIBUTE";

    public static final String SMS_NO_ATTRIBUTE_IN_ENTRY = 
        "sms-NO_ATTRIBUTE_IN_ENTRY";

    public static final String SMS_INVALID_SEARCH_ORDER_PARAMETER = 
        "sms-INVALID_SEARCH_ORDER_PARAMETER";

    public static final String SMS_INVALID_SEARCH_BASE =
        "sms-INVALID_SEARCH_BASE";

    public static final String SMS_ATTR_LIST_NEEDED = "sms-ATTR_LIST_NEEDED";

    public static final String SMS_CANNOT_CREATE_PLACE_HOLDER_NODE = 
        "sms-CANNOT_CREATE_PLACE_HOLDER_NODE";

    public static final String SMS_SERVER_DOWN = "sms-SERVER_DOWN";

    public static final String SMS_LDAP_NOT_SUPPORTED = 
        "sms-LDAP_NOT_SUPPORTED";

    public static final String SMS_LDAP_SERVER_BUSY = "sms-LDAP_SERVER_BUSY";

    public static final String SMS_INSUFFICIENT_ACCESS_RIGHTS = 
        "sms-INSUFFICIENT_ACCESS_RIGHTS";

    public static final String SMS_ADMIN_LIMIT_EXCEEDED = 
        "sms-ADMIN_LIMIT_EXCEEDED";

    public static final String SMS_TIME_LIMIT_EXCEEDED = 
        "sms-TIME_LIMIT_EXCEEDED";

    public static final String SMS_LDAP_REFERRAL_EXCEPTION = 
        "sms-LDAP_REFERRAL_EXCEPTION";

    public static final String SMS_LDAP_OPERATION_FAILED = 
        "sms-LDAP_OPERATION_FAILED";

    public static final String SMS_UNEXPECTED_LDAP_EXCEPTION = 
        "sms-UNEXPECTED_LDAP_EXCEPTION";

    public static final String SMS_EVENT_NOTIFICATION_FAILED = 
        "sms-EVENT_NOTIFICATION_FAILED";

    public static final String SMS_UNKNOWN_EXCEPTION_OCCURRED = 
        "sms-UNKNOWN_EXCEPTION_OCCURED";

    public static final String SMS_XML_PARSER_EXCEPTION =
        "sms-XML_PARSER_EXCEPTION";

    public static final String SMS_SERVER_INSTANCE_NOT_FOUND = 
        "sms-SERVER_INSTANCE_NOT_FOUND";

    public static final String SMS_VALUE_DOES_NOT_EXIST = 
        "sms-VALUE_DOES_NOT_EXIST";

    public static final String SMS_SUB_CONFIG_DOES_NOT_EXIST = 
        "sms-SUB_CONFIG_DOES_NOT_EXIST";

    public static final String SMS_INVALID_CONFIG_NAME = 
        "sms-INVALID_CONFIG_NAME";

    public static final String SMS_ADD_SUB_CONFIG_FAILED = 
        "sms-ADD_SUB_CONFIG_FAILED";

    public static final String SMS_NO_SUCH_OBJECT = "sms-NO_SUCH_OBJECT";

    public static final String SMS_SERVICE_NODE_NOT_FOUND = 
        "sms-SERVICE_NODE_NOT_FOUND";

    public static final String SMS_SERVICE_NAME_NOT_FOUND = 
        "sms-SERVICE_NAME_NOT_FOUND";

    public static final String SMS_ATTR_NAME_NOT_FOUND = 
        "sms-ATTR_NAME_NOT_FOUND";

    public static final String SMS_ATTR_SYNTAX_NOT_FOUND = 
        "sms-ATTR_SYNTAX_NOT_FOUND";

    public static final String SMS_OC_NAME_NOT_FOUND = 
        "sms-OC_NAME_NOT_FOUND";

    public static final String SMS_AUTHENTICATION_ERROR = 
        "sms-AUTHENTICATION_ERROR";

    public static final String SMS_CAN_NOT_CONSTRUCT_SERVICE_MANAGER = 
        "sms-CAN_NOT_CONSTRUCT_SERVICE_MANAGER";

    public static final String SMS_SMSSchema_no_service_element = 
        "sms-SMSSchema_no_service_element";

    public static final String SMS_SMSSchema_no_schema_element = 
        "sms-SMSSchema_no_schema_element";

    public static final String SMS_SMSSchema_parser_error = 
        "sms-SMSSchema_parser_error";

    public static final String SMS_SMSSchema_exception_message = 
        "sms-SMSSchema_exception_message";

    public static final String SMS_SMSSchema_invalid_xml_document = 
        "sms-SMSSchema_invalid_xml_document";

    public static final String SMS_SMSSchema_invalid_input_stream = 
        "sms-SMSSchema_invalid_input_stream";

    public static final String SMS_SMSSchema_service_notfound = 
        "sms-SMSSchema_service_notfound";

    public static final String SMS_service_already_exists = 
        "sms-service_already_exists";

    public static final String SMS_service_already_exists_no_args = 
        "sms-service_already_exists1";

    public static final String SMS_organization_already_exists_no_args = 
        "sms-organization_already_exists1";

    public static final String SMS_services_node_does_not_exist = 
        "sms-services_node_does_not_exist";

    public static final String SMS_service_does_not_exist = 
        "sms-service_does_not_exist";

    public static final String SMS_failed_to_get_schema_manager = 
        "sms-failed_to_get_schema_manager";

    public static final String SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS = 
        "sms-validator_cannot_instantiate_class";

    public static final String SMS_xml_invalid_doc_type = "sms-invalid-doctype";

    public static final String services_validator_invalid_attr_name = 
        "services_validator_invalid_attr_name";

    public static final String services_validator_invalid_attr_schema = 
        "services_validator_invalid_attr_schema";

    public static final String services_validator_initialize_failed =
        "services_validator_initialize_failed";

    public static final String services_validator_schema_does_not_exist = 
        "services_validator_schema_does_not_exist";
}
