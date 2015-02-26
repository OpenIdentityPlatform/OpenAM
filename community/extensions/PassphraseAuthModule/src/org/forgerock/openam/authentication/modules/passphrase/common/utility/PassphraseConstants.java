/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.common.utility;

public interface PassphraseConstants {
	public static final String USER_PASSWORD_ATTRIBUTE = "USER_PASSWORD_ATTRIBUTE";	
	public static final String ED_USER_PASSWORD_ATTRIBUTE = "ED_USER_PASSWORD_ATTRIBUTE";
	public static final String USER_PASSPHRASE_ATTRIBUTE = "USER_PASSPHRASE_ATTRIBUTE";
	public static final String USER_PASSWORD_MIN_LENGTH_ATTRIBUTE = "USER_PASSWORD_MIN_LENGTH_ATTRIBUTE"; 
	public static final String USER_PASSPHRASE_MIN_LENGTH_ATTRIBUTE = "USER_PASSPHRASE_MIN_LENGTH_ATTRIBUTE"; 
	public static final String USER_PASSWORD_RESET_FLAG_ATTRIBUTE = "USER_PASSWORD_RESET_FLAG_ATTRIBUTE";
	public static final String USER_PASSPHRASE_RESET_FLAG_ATTRIBUTE = "USER_PASSPHRASE_RESET_FLAG_ATTRIBUTE";
	public static final String USER_RESET_QUESTION_ANSWER_ATTRIBUTE = "USER_RESET_QUESTION_ANSWER_ATTRIBUTE";
	public static final String RESET_QUESTIONS_ATTRIBUTE = "RESET_QUESTIONS_ATTRIBUTE";

	public static final String USER_IDENTIFIER = "USER_IDENTIFIER";
	public static final String RESET_ANSWERS_MIN_LENGTH = "RESET_ANSWERS_MIN_LENGTH";
	public static final String PASSWORD_EXPIRY_DAYS = "PASSWORD_EXPIRY_DAYS";
	public static final String ADMIN_PASSWORD_EXPIRY_DAYS = "ADMIN_PASSWORD_EXPIRY_DAYS";
	public static final String ED_SCHEMA_EXPIRY_DAYS = "ED_SCHEMA_EXPIRY_DAYS";
	public static final String ADMIN_GROUP = "ADMIN_GROUP";
	
	public static final String PROPERTY_FILE_PATH = "org/forgerock/openam/authenticaiton/modules/passphrase/security/properties/CustomModule";
	public static final String MAIL_TEMPLATE_PATH_PASSWORD = "org/forgerock/openam/authenticaiton/modules/passphrase/mailtemplate/passwordTemplate.vm";
	public static final String MAIL_TEMPLATE_PATH_PASSPHRASE = "org/forgerock/openam/authenticaiton/modules/passphrase/mailtemplate/passphraseTemplate.vm";
	
	public static final String SCREEN_NAME_ATTRIBUTE = "SCREEN_NAME_ATTRIBUTE";
	public static final String FIRST_NAME_ATTRIBUTE = "FIRST_NAME_ATTRIBUTE";
	public static final String LAST_NAME_ATTRIBUTE = "LAST_NAME_ATTRIBUTE";
	
	public static final String SERVER_HOST = "iplanet-am-auth-ldap-server";
	public static final String BASE_DN = "iplanet-am-auth-ldap-base-dn";
	public static final String BIND_DN = "iplanet-am-auth-ldap-bind-dn";
	public static final String BIND_PWD = "iplanet-am-auth-ldap-bind-passwd";
	public static final String USER_NAMING_ATTR = "iplanet-am-auth-ldap-user-search-attributes";
	public static final String SEARCH_FILTER = "iplanet-am-auth-ldap-search-filter";
	public static final String SSL = "iplanet-am-auth-ldap-ssl-enabled";
	public static final String SEARCH_SCOPE = "iplanet-am-auth-ldap-search-scope";
	public static final String OBJECT = "OBJECT";
	public static final String ONELEVEL = "ONELEVEL";
	public static final String HTML = "html";
	public static final String LDAPUSER_SEARCH_ATTRIBUTE = "iplanet-am-auth-ldap-user-search-attributes";
	public static final String ID = "@forgerock.com";
	public static final String CLEARNET_ID = "@forgerock.com";
	public static final String COPY_MNEMONIC_ATTRS = "COPY_MNEMONIC_ATTRS";
	public static final String IGNORE_USER_ATTRS = "IGNORE_USER_ATTRS";
	public static final String MANDATORY_USER_ATTRS = "MANDATORY_USER_ATTRS";
	public static final String COUNTRY = "GB";	
		
	public static final String ED_EXTERNAL_INSTANCE_NAME = "ED_EXTERNAL_INSTANCE_NAME";
	public static final String ED_INTERNAL_INSTANCE_NAME = "ED_INTERNAL_INSTANCE_NAME";
		
	public static final String ED_EXTERNAL_DATASTORE_NAME = "ED_EXTERNAL_DATASTORE_NAME";
	public static final String ED_INTERNAL_DATASTORE_NAME = "ED_INTERNAL_DATASTORE_NAME";
	
	public static final String ED_EXTERNAL_REALM = "ED_EXTERNAL_REALM";
	public static final String ED_INTERNAL_REALM = "ED_INTERNAL_REALM";
	
	public static final String JNDI_NAME = "JNDI_NAME";	
	public static final int MODULE_ENTRY_POINT = 1; 
	public static final int MODULE_PASSWORD_RESET = 2; 
	public static final int MODULE_PASSPHRASE_ENTRY = 3; 
	public static final int MODULE_PASSPHRASE_AUTHENTICATION = 4; 
	public static final int MODULE_PASSPHRASE_RESET = 5; 
	public static final int MODULE_RESET_ANSWERS_ENTRY = 6;
	public static final int MODULE_PROFILE_UPDATED = 7;
	public static final int MODULE_SUCCESS = -1;
	
	public static final int AUTH_MODULE_ERROR_STATE = 2; 
	
	public static final int TYPE_INVALID_PASSWORD = 1;
	public static final int TYPE_INVALID_PASSPHRASE = 2;
	public static final int SERVER_PORT = 50389;
}