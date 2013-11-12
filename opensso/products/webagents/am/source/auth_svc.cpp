/*
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
 * $Id: auth_svc.cpp,v 1.7 2009/12/10 00:01:43 robertis Exp $
 *
 */ 

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#include <iostream>
#include <string.h>
#include <am.h>
#include <am_string_set.h>
#include "connection.h"
#include "auth_svc.h"
#include "xml_tree.h"
#include "log.h"
#include "utils.h"
#include "auth_svc.h"
#include "url.h"
#include <cstring>

USING_PRIVATE_NAMESPACE

#define AUTH_SVC_MODULE "AuthService"

/*
 * Must match enumeration of am_auth_index_t.
 */
const char *index_type_string[] = {
    "authLevel",
    "role",
    "user",
    "moduleInstance",
    "service"
};

/*
 * XML elements and attributes.
 */
const char *QUERY_RESULT ="QueryResult";
const char *GET_REQUIREMENTS ="GetRequirements";
const char *LOGIN_STATUS ="LoginStatus";
const char *EXCEPTION ="Exception";
const char *AUTH_IDENTIFIER ="authIdentifier";
const char *SUCCESS ="success";
const char *FAILED ="failed";
const char *COMPLETED = "completed";
const char *IN_PROGRESS = "in_progress";
const char *SUBJECT = "Subject";
const char *MESSAGE = "message";
const char *TOKEN_ID = "tokenId";
const char *ERROR_CODE = "errorCode";
const char *STATUS = "status";
const char *TEMPLATE_NAME = "templateName";
const char *CALLBACKS = "Callbacks";
const char *LENGTH = "length";
const char *CHOICE_CALLBACK = "ChoiceCallback";
const char *HTTP_CALLBACK = "HttpCallback";
const char *REDIRECT_CALLBACK = "RedirectCallback";
const char *CONFIRMATION_CALLBACK = "ConfirmationCallback";
const char *LANGUAGE_CALLBACK = "LanguageCallback";
const char *NAME_CALLBACK = "NameCallback";
const char *PASSWORD_CALLBACK = "PasswordCallback";
const char *TEXT_INPUT_CALLBACK = "TextInputCallback";
const char *TEXT_OUTPUT_CALLBACK = "TextOutputCallback";
const char *PAGE_PROPERTIES_CALLBACK = "PagePropertiesCallback";
const char *CUSTOM_CALLBACK = "CustomCallback";
const char *LOCALE = "Locale";
const char *LANGUAGE = "language";
const char *COUNTRY = "country";
const char *VARIANT_AM = "variant";
const char *PROMPT = "Prompt";
const char *ECHO_PASSWORD = "echoPassword";
const char *SSO_TOKEN = "ssoToken";
const char *MULTIPLE_SELECTIONS_ALLOWED = "multipleSelectionsAllowed";
const char *CHOICE_VALUE = "ChoiceValue";
const char *CHOICE_VALUES = "ChoiceValues";
const char *MODULE_INSTANCE_NAMES = "ChoiceValues";
const char *IS_DEFAULT = "isDefault";
const char *MESSAGE_TYPE = "messageType";
const char *OPTION_TYPE = "optionType";
const char *OPTION_VALUE = "OptionValue";
const char *OPTION_VALUES = "OptionValues";
const char *DEFAULT_OPTION_VALUE = "DefaultOptionValue";
const char *DEFAULT_VALUE = "DefaultValue";

const char *HTTP_HEADER = "HttpHeader";
const char *HTTP_NEGO = "Negotiation";
const char *HTTP_CODE = "HttpErrorCode";
const char *HTTP_TOKEN = "HttpToken";

const char *REDIRECT_URL = "RedirectUrl";
const char *REDIRECT_DATA = "RedirectData";
const char *REDIRECT_STATUS = "Status";
const char *REDIRECT_STATUS_PARAM = "RedirectStatusParam";
const char *REDIRECT_BACK_URL_COOKIE = "RedirectBackUrlCookie";
const char *REDIRECT_METHOD = "method";
const char *REDIRECT_NAME = "Name";
const char *REDIRECT_VALUE = "Value";


const char *REDIRECT_CALLBACK_BEGIN = "<RedirectCallback";
const char *REDIRECT_CALLBACK_END = "</RedirectCallback>";
const char *REDIRECT_URL_BEGIN = "<RedirectUrl>";
const char *REDIRECT_URL_END = "</RedirectUrl>";
const char *REDIRECT_DATA_BEGIN = "<RedirectData>";
const char *REDIRECT_DATA_END = "</RedirectData>";
const char *REDIRECT_STATUS_BEGIN = "<Status>";
const char *REDIRECT_STATUS_END = "</Status>";
const char *REDIRECT_STATUS_PARAM_BEGIN = "<RedirectStatusParam>";
const char *REDIRECT_STATUS_PARAM_END = "</RedirectStatusParam>";
const char *REDIRECT_BACK_URL_COOKIE_BEGIN = "<RedirectBackUrlCookie>";
const char *REDIRECT_BACK_URL_COOKIE_END = "</RedirectBackUrlCookie>";
const char *REDIRECT_NAME_BEGIN = "<Name>";
const char *REDIRECT_NAME_END = "</Name>";
const char *REDIRECT_VALUE_BEGIN = "<Value>";
const char *REDIRECT_VALUE_END = "</Value>";

// VALUE is defined in utils.cpp
// const char *VALUE = "Value";
const char *QUOTE = "\"";
const char *ELEMENT_CLOSE = ">";

/*
 * namespace
 */
namespace {

/*
 * Predefined XML tags and statements.
 */
#define XML_REQUEST_PREFIX_DATA \
	    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" \
	    "<RequestSet vers=\"1.0\" svcid=\"auth\" reqid=\""

#define XML_REQUEST_SUFFIX_DATA "</RequestSet>\n"

#define AUTH_CONTEXT_VERSION "1.0"

#define	AUTH_CONTEXT_PREFIX_DATA "\">\n" \
	    "<Request><![CDATA[" \
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" \
	    "<AuthContext version=\"" AUTH_CONTEXT_VERSION "\">\n" \
	    "<Request authIdentifier=\""

#define AUTH_CONTEXT_SUFFIX_DATA "</AuthContext>]]></Request>\n"

#define NEW_AUTH_CONTEXT_PREFIX_DATA \
	    "\">" \
	    "<NewAuthContext orgName=\""

#define NEW_AUTH_CONTEXT_SUFFIX_DATA "\"/></Request>"

#define LOGIN_PREFIX_DATA "\"><Login>"

#define LOGIN_SUFFIX_DATA "</Login></Request>"

#define INDEX_TYPE_NAME_PAIR_PREFIX_DATA "<IndexTypeNamePair indexType=\""

#define INDEX_TYPE_NAME_PAIR_SUFFIX_DATA "</IndexTypeNamePair>"

#define INDEX_NAME_PREFIX_DATA "\"><IndexName>"

#define INDEX_NAME_SUFFIX_DATA "</IndexName>"

#define SUBMIT_REQUIREMENTS_PREFIX_DATA "\"><SubmitRequirements>"

#define SUBMIT_REQUIREMENTS_SUFFIX_DATA "</SubmitRequirements></Request>"

#define CALLBACKS_PREFIX_DATA "<Callbacks length=\""

#define CALLBACKS_SUFFIX_DATA "</Callbacks>"

#define CHOICE_CALLBACK_PREFIX_DATA \
	    "<ChoiceCallback multipleSelectionsAllowed=\""

#define CHOICE_CALLBACK_SUFFIX_DATA "</ChoiceCallback>"

#define CONFIRMATION_CALLBACK_PREFIX_DATA "<ConfirmationCallback"

#define CONFIRMATION_CALLBACK_SUFFIX_DATA "</ConfirmationCallback>"

#define LANGUAGE_CALLBACK_PREFIX_DATA "<LanguageCallback"

#define LANGUAGE_CALLBACK_SUFFIX_DATA "</LanguageCallback>"

#define NAME_CALLBACK_PREFIX_DATA "<NameCallback>"

#define NAME_CALLBACK_SUFFIX_DATA "</NameCallback>"

#define PASSWORD_CALLBACK_PREFIX_DATA "<PasswordCallback echoPassword=\""

#define PASSWORD_CALLBACK_SUFFIX_DATA "</PasswordCallback>"

#define TEXT_INPUT_CALLBACK_PREFIX_DATA "<TextInputCallback>"

#define TEXT_INPUT_CALLBACK_SUFFIX_DATA "</TextInputCallback>"

#define TEXT_OUTPUT_CALLBACK_PREFIX_DATA "<TextOutputCallback>"

#define TEXT_OUTPUT_CALLBACK_SUFFIX_DATA "</TextOutputCallback>"


#define HTTP_CALLBACK_PREFIX_DATA "<HttpCallback>"
#define HTTP_CALLBACK_SUFFIX_DATA "</HttpCallback>"

#define HTTP_HEADER_CALLBACK_PREFIX_DATA "<HttpHeader>"
#define HTTP_HEADER_CALLBACK_SUFFIX_DATA "</HttpHeader>"

#define HTTP_NEGO_CALLBACK_PREFIX_DATA "<Negotiation>"
#define HTTP_NEGO_CALLBACK_SUFFIX_DATA "</Negotiation>"

#define HTTP_CODE_CALLBACK_PREFIX_DATA "<HttpErrorCode>"
#define HTTP_CODE_CALLBACK_SUFFIX_DATA "</HttpErrorCode>"

#define HTTP_TOKEN_CALLBACK_PREFIX_DATA "<HttpToken>"
#define HTTP_TOKEN_CALLBACK_SUFFIX_DATA "</HttpToken>"




#define LOCALE_PREFIX_DATA "<Locale>"

#define LOCALE_SUFFIX_DATA "</Locale>"

#define PROMPT_PREFIX_DATA "<Prompt>"

#define PROMPT_SUFFIX_DATA "</Prompt>"

#define VALUE_PREFIX_DATA "<Value>"

#define VALUE_SUFFIX_DATA "</Value>"

#define LOGOUT_DATA "\"><Logout/></Request>"

#define ABORT_DATA "\"><Abort/></Request>"

#define MODULE_INSTANCE_NAMES_PREFIX_DATA "\">" \
            "<QueryInformation requestedInformation=\"" \
            "moduleInstanceNames\">" 
#define MODULE_INSTANCE_NAMES_SUFFIX_DATA "</QueryInformation></Request>"

#define CHOICE_VALUES_PREFIX_DATA "<ChoiceValues>"

#define CHOICE_VALUES_SUFFIX_DATA "</ChoiceValues>"

#define CHOICE_VALUE_PREFIX_DATA "<ChoiceValue" // may have attribute added

#define CHOICE_VALUE_SUFFIX_DATA "</ChoiceValue>"

#define SELECTED_VALUES_PREFIX_DATA "<SelectedValues>"

#define SELECTED_VALUES_SUFFIX_DATA "</SelectedValues>"

#define OPTION_VALUES_PREFIX_DATA "<OptionValues>"

#define OPTION_VALUES_SUFFIX_DATA "</OptionValues>"

#define OPTION_VALUE_PREFIX_DATA "<OptionValue>"

#define OPTION_VALUE_SUFFIX_DATA "</OptionValue>"

#define DEFAULT_OPTION_VALUE_PREFIX_DATA "<DefaultOptionValue>"

#define DEFAULT_OPTION_VALUE_SUFFIX_DATA "</DefaultOptionValue>"

#define SELECTED_VALUE_PREFIX_DATA "<SelectedValue>"

#define SELECTED_VALUE_SUFFIX_DATA "</SelectedValue>"

#define IS_DEFAULT_ATTRIBUTE_DATA " isDefault=\"yes\">"

#define MESSAGE_TYPE_ATTRIBUTE_DATA " messageType=\""

#define OPTION_TYPE_ATTRIBUTE_DATA " optionType=\""

#define LANGUAGE_ATTRIBUTE_DATA " language=\""

#define COUNTRY_ATTRIBUTE_DATA " country=\""

#define VARIANT_ATTRIBUTE_DATA " variant=\""

#define QUOTE_DATA "\""

#define END_ELEMENT_DATA ">"

#define QUOTE_END_ELEMENT_DATA "\">"

/*
 * Constants to be used to create BodyChunks.
 */
const char xmlRequestPrefix[] = {
    XML_REQUEST_PREFIX_DATA
};

const char xmlRequestSuffix[] = {
    XML_REQUEST_SUFFIX_DATA
};

const char authContextPrefix[] = {
    AUTH_CONTEXT_PREFIX_DATA
};

const char authContextSuffix[] = {
    AUTH_CONTEXT_SUFFIX_DATA
};

const char newAuthContextPrefix[] = {
    NEW_AUTH_CONTEXT_PREFIX_DATA
};

const char newAuthContextSuffix[] = {
    NEW_AUTH_CONTEXT_SUFFIX_DATA
};

const char loginPrefix[] = {
    LOGIN_PREFIX_DATA
};

const char loginSuffix[] = {
    LOGIN_SUFFIX_DATA
};

const char indexTypeNamePairPrefix[] = {   /* optional element of login */
    INDEX_TYPE_NAME_PAIR_PREFIX_DATA
};

const char indexTypeNamePairSuffix[] = {
    INDEX_TYPE_NAME_PAIR_SUFFIX_DATA
};

const char indexNamePrefix[] = {
    INDEX_NAME_PREFIX_DATA
};

const char indexNameSuffix[] = {
    INDEX_NAME_SUFFIX_DATA
};

const char submitRequirementsPrefix[] = {
    SUBMIT_REQUIREMENTS_PREFIX_DATA
};

const char submitRequirementsSuffix[] = {
    SUBMIT_REQUIREMENTS_SUFFIX_DATA
};

const char callbacksPrefix[] = {
    CALLBACKS_PREFIX_DATA
};

const char callbacksSuffix[] = {
    CALLBACKS_SUFFIX_DATA
};

const char choiceCallbackPrefix[] = {
    CHOICE_CALLBACK_PREFIX_DATA
};

const char choiceCallbackSuffix[] = {
    CHOICE_CALLBACK_SUFFIX_DATA
};

const char confirmationCallbackPrefix[] = {
    CONFIRMATION_CALLBACK_PREFIX_DATA
};

const char confirmationCallbackSuffix[] = {
    CONFIRMATION_CALLBACK_SUFFIX_DATA
};

const char languageCallbackPrefix[] = {
    LANGUAGE_CALLBACK_PREFIX_DATA
};

const char languageCallbackSuffix[] = { 
    LANGUAGE_CALLBACK_SUFFIX_DATA 
}; 

const char nameCallbackPrefix[] = {
    NAME_CALLBACK_PREFIX_DATA
};

const char nameCallbackSuffix[] = { 
    NAME_CALLBACK_SUFFIX_DATA 
}; 

const char passwordCallbackPrefix[] = {
    PASSWORD_CALLBACK_PREFIX_DATA
};

const char passwordCallbackSuffix[] = { 
    PASSWORD_CALLBACK_SUFFIX_DATA 
};

const char textInputCallbackPrefix[] = {
    TEXT_INPUT_CALLBACK_PREFIX_DATA
};

const char textInputCallbackSuffix[] = { 
    TEXT_INPUT_CALLBACK_SUFFIX_DATA 
};


const char httpCallbackPrefix[] = {
    HTTP_CALLBACK_PREFIX_DATA
};
const char httpCallbackSuffix[] = {
    HTTP_CALLBACK_SUFFIX_DATA
};

const char httpHeaderCallbackPrefix[] = {
    HTTP_HEADER_CALLBACK_PREFIX_DATA
};
const char httpHeaderCallbackSuffix[] = {
    HTTP_HEADER_CALLBACK_SUFFIX_DATA
};

const char httpNegoCallbackPrefix[] = {
    HTTP_NEGO_CALLBACK_PREFIX_DATA
};
const char httpNegoCallbackSuffix[] = {
    HTTP_NEGO_CALLBACK_SUFFIX_DATA
};

const char httpCodeCallbackPrefix[] = {
    HTTP_CODE_CALLBACK_PREFIX_DATA
};
const char httpCodeCallbackSuffix[] = {
    HTTP_CODE_CALLBACK_SUFFIX_DATA
};

const char httpTokenCallbackPrefix[] = {
    HTTP_TOKEN_CALLBACK_PREFIX_DATA
};
const char httpTokenCallbackSuffix[] = {
    HTTP_TOKEN_CALLBACK_SUFFIX_DATA
};



const char localePrefix[] = {
    LOCALE_PREFIX_DATA
};

const char localeSuffix[] = {
    LOCALE_SUFFIX_DATA
};

const char promptPrefix[] = {
    PROMPT_PREFIX_DATA
};

const char promptSuffix[] = {
    PROMPT_SUFFIX_DATA
};

const char valuePrefix[] = {
    VALUE_PREFIX_DATA
};

const char valueSuffix[] = {
    VALUE_SUFFIX_DATA
};

const char logoutStr[] = {
    LOGOUT_DATA
};

const char abortStr[] = {
    ABORT_DATA
};

const char choiceValuesPrefix[] = {
    CHOICE_VALUES_PREFIX_DATA
};

const char choiceValuesSuffix[] = {
    CHOICE_VALUES_SUFFIX_DATA
};

const char choiceValuePrefix[] = {
    CHOICE_VALUE_PREFIX_DATA
};

const char choiceValueSuffix[] = {
    CHOICE_VALUE_SUFFIX_DATA
};

const char moduleInstanceNamesPrefix[] = {
    MODULE_INSTANCE_NAMES_PREFIX_DATA
};

const char moduleInstanceNamesSuffix[] = {
    MODULE_INSTANCE_NAMES_SUFFIX_DATA
};

const char selectedValuesPrefix[] = {
    SELECTED_VALUES_PREFIX_DATA
};

const char selectedValuesSuffix[] = {
    SELECTED_VALUES_SUFFIX_DATA
};

const char optionValuesPrefix[] = {
    OPTION_VALUES_PREFIX_DATA
};

const char optionValuesSuffix[] = {
    OPTION_VALUES_SUFFIX_DATA
};

const char optionValuePrefix[] = {
    OPTION_VALUE_PREFIX_DATA
};

const char optionValueSuffix[] = {
    OPTION_VALUE_SUFFIX_DATA
};

const char defaultOptionValuePrefix[] = {
    DEFAULT_OPTION_VALUE_PREFIX_DATA
};

const char defaultOptionValueSuffix[] = {
    DEFAULT_OPTION_VALUE_SUFFIX_DATA
};

const char selectedValuePrefix[] = {
    SELECTED_VALUE_PREFIX_DATA
};

const char selectedValueSuffix[] = {
    SELECTED_VALUE_SUFFIX_DATA
};

const char isDefaultAttribute[] = { 
    IS_DEFAULT_ATTRIBUTE_DATA 
};

const char messageTypeAttribute[] = { 
    MESSAGE_TYPE_ATTRIBUTE_DATA 
};

const char optionTypeAttribute[] = { 
    OPTION_TYPE_ATTRIBUTE_DATA 
};

const char languageAttribute[] = { 
    LANGUAGE_ATTRIBUTE_DATA 
};

const char countryAttribute[] = { 
    COUNTRY_ATTRIBUTE_DATA 
};

const char variantAttribute[] = { 
    VARIANT_ATTRIBUTE_DATA 
};

const char quote[] = { 
    QUOTE_DATA 
};

const char endElementDt[] = { 
    END_ELEMENT_DATA 
};

const char quoteEndElement[] = { 
    QUOTE_END_ELEMENT_DATA 
};

} // namespace

/*
 * XML pre-initialized chunks.
 */
const AuthService::BodyChunk
AuthService::xmlRequestPrefixChunk(xmlRequestPrefix,
				    sizeof(xmlRequestPrefix) - 1);

const AuthService::BodyChunk
AuthService::xmlRequestSuffixChunk(xmlRequestSuffix,
				    sizeof(xmlRequestSuffix) - 1);

const AuthService::BodyChunk
AuthService::authContextPrefixChunk(authContextPrefix,
				    sizeof(authContextPrefix) - 1);

const AuthService::BodyChunk
AuthService::authContextSuffixChunk(authContextSuffix,
				    sizeof(authContextSuffix) - 1);

const AuthService::BodyChunk
AuthService::newAuthContextPrefixChunk(newAuthContextPrefix,
				    sizeof(newAuthContextPrefix) - 1);

const AuthService::BodyChunk
AuthService::newAuthContextSuffixChunk(newAuthContextSuffix,
				    sizeof(newAuthContextSuffix) - 1);

const AuthService::BodyChunk
AuthService::loginPrefixChunk(loginPrefix,
				    sizeof(loginPrefix) - 1);

const AuthService::BodyChunk
AuthService::loginSuffixChunk(loginSuffix,
				    sizeof(loginSuffix) - 1);

const AuthService::BodyChunk
AuthService::indexTypeNamePairPrefixChunk(indexTypeNamePairPrefix,
				    sizeof(indexTypeNamePairPrefix) - 1);

const AuthService::BodyChunk
AuthService::indexTypeNamePairSuffixChunk(indexTypeNamePairSuffix,
				    sizeof(indexTypeNamePairSuffix) - 1);

const AuthService::BodyChunk
AuthService::indexNamePrefixChunk(indexNamePrefix,
				    sizeof(indexNamePrefix) - 1);

const AuthService::BodyChunk
AuthService::indexNameSuffixChunk(indexNameSuffix,
				    sizeof(indexNameSuffix) - 1);

const AuthService::BodyChunk
AuthService::submitRequirementsPrefixChunk(submitRequirementsPrefix,
				    sizeof(submitRequirementsPrefix) - 1);

const AuthService::BodyChunk
AuthService::submitRequirementsSuffixChunk(submitRequirementsSuffix,
				    sizeof(submitRequirementsSuffix) - 1);

const AuthService::BodyChunk
AuthService::callbacksPrefixChunk(callbacksPrefix,
				    sizeof(callbacksPrefix) - 1);

const AuthService::BodyChunk
AuthService::callbacksSuffixChunk(callbacksSuffix,
				    sizeof(callbacksSuffix) - 1);

const AuthService::BodyChunk
AuthService::choiceCallbackPrefixChunk(choiceCallbackPrefix,
				    sizeof(choiceCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::choiceCallbackSuffixChunk(choiceCallbackSuffix,
				    sizeof(choiceCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::confirmationCallbackPrefixChunk(confirmationCallbackPrefix,
				    sizeof(confirmationCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::confirmationCallbackSuffixChunk(confirmationCallbackSuffix,
				    sizeof(confirmationCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::languageCallbackPrefixChunk(languageCallbackPrefix,
				    sizeof(languageCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::languageCallbackSuffixChunk(languageCallbackSuffix,
				    sizeof(languageCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::nameCallbackPrefixChunk(nameCallbackPrefix,
				    sizeof(nameCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::nameCallbackSuffixChunk(nameCallbackSuffix,
				    sizeof(nameCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::passwordCallbackPrefixChunk(passwordCallbackPrefix,
				    sizeof(passwordCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::passwordCallbackSuffixChunk(passwordCallbackSuffix,
				    sizeof(passwordCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::textInputCallbackPrefixChunk(textInputCallbackPrefix,
				    sizeof(textInputCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::textInputCallbackSuffixChunk(textInputCallbackSuffix,
				    sizeof(textInputCallbackSuffix) - 1);


const AuthService::BodyChunk
AuthService::localePrefixChunk(localePrefix,
				    sizeof(localePrefix) - 1);

const AuthService::BodyChunk
AuthService::localeSuffixChunk(localeSuffix,
				    sizeof(localeSuffix) - 1);

const AuthService::BodyChunk
AuthService::promptPrefixChunk(promptPrefix,
				    sizeof(promptPrefix) - 1);

const AuthService::BodyChunk
AuthService::promptSuffixChunk(promptSuffix,
				    sizeof(promptSuffix) - 1);

const AuthService::BodyChunk
AuthService::valuePrefixChunk(valuePrefix,
				    sizeof(valuePrefix) - 1);

const AuthService::BodyChunk
AuthService::valueSuffixChunk(valueSuffix,
				    sizeof(valueSuffix) - 1);

const AuthService::BodyChunk
AuthService::logoutChunk(logoutStr, sizeof(logoutStr) - 1);

const AuthService::BodyChunk
AuthService::abortChunk(abortStr, sizeof(abortStr) - 1);

const AuthService::BodyChunk
AuthService::choiceValuesPrefixChunk(choiceValuesPrefix,
				    sizeof(choiceValuesPrefix) - 1);

const AuthService::BodyChunk
AuthService::choiceValuesSuffixChunk(choiceValuesSuffix,
				    sizeof(choiceValuesSuffix) - 1);

const AuthService::BodyChunk
AuthService::moduleInstanceNamesPrefixChunk(moduleInstanceNamesPrefix,
				    sizeof(moduleInstanceNamesPrefix) - 1);

const AuthService::BodyChunk
AuthService::moduleInstanceNamesSuffixChunk(moduleInstanceNamesSuffix,
				    sizeof(moduleInstanceNamesSuffix) - 1);
const AuthService::BodyChunk
AuthService::choiceValuePrefixChunk(choiceValuePrefix,
				    sizeof(choiceValuePrefix) - 1);

const AuthService::BodyChunk
AuthService::choiceValueSuffixChunk(choiceValueSuffix,
				    sizeof(choiceValueSuffix) - 1);

const AuthService::BodyChunk
AuthService::selectedValuesPrefixChunk(selectedValuesPrefix,
				    sizeof(selectedValuesPrefix) - 1);

const AuthService::BodyChunk
AuthService::selectedValuesSuffixChunk(selectedValuesSuffix,
				    sizeof(selectedValuesSuffix) - 1);

const AuthService::BodyChunk
AuthService::optionValuesPrefixChunk(optionValuesPrefix,
				    sizeof(optionValuesPrefix) - 1);

const AuthService::BodyChunk
AuthService::optionValuesSuffixChunk(optionValuesSuffix,
				    sizeof(optionValuesSuffix) - 1);

const AuthService::BodyChunk
AuthService::optionValuePrefixChunk(optionValuePrefix,
				    sizeof(optionValuePrefix) - 1);

const AuthService::BodyChunk
AuthService::optionValueSuffixChunk(optionValueSuffix,
				    sizeof(optionValueSuffix) - 1);

const AuthService::BodyChunk
AuthService::defaultOptionValuePrefixChunk(defaultOptionValuePrefix,
				    sizeof(defaultOptionValuePrefix) - 1);

const AuthService::BodyChunk
AuthService::defaultOptionValueSuffixChunk(defaultOptionValueSuffix,
				    sizeof(defaultOptionValueSuffix) - 1);

const AuthService::BodyChunk
AuthService::selectedValuePrefixChunk(selectedValuePrefix,
				    sizeof(selectedValuePrefix) - 1);

const AuthService::BodyChunk
AuthService::selectedValueSuffixChunk(selectedValueSuffix,
				    sizeof(selectedValueSuffix) - 1);

const AuthService::BodyChunk
AuthService::isDefaultAttributeChunk(isDefaultAttribute,
				    sizeof(isDefaultAttribute) - 1);

const AuthService::BodyChunk
AuthService::messageTypeAttributeChunk(messageTypeAttribute,
				    sizeof(messageTypeAttribute) - 1);

const AuthService::BodyChunk
AuthService::optionTypeAttributeChunk(optionTypeAttribute,
				    sizeof(optionTypeAttribute) - 1);

const AuthService::BodyChunk
AuthService::languageAttributeChunk(languageAttribute,
				    sizeof(languageAttribute) - 1);

const AuthService::BodyChunk
AuthService::countryAttributeChunk(countryAttribute,
				    sizeof(countryAttribute) - 1);

const AuthService::BodyChunk
AuthService::variantAttributeChunk(variantAttribute,
				    sizeof(variantAttribute) - 1);

const AuthService::BodyChunk
AuthService::quoteChunk(quote,
				    sizeof(quote) - 1);

const AuthService::BodyChunk
AuthService::endElementChunk(endElementDt,
				    sizeof(endElementDt) - 1);

const AuthService::BodyChunk
AuthService::quoteEndElementChunk(quoteEndElement,
				    sizeof(quoteEndElement) - 1);

/* Now we add in the defines for the HTTP callback  */


const AuthService::BodyChunk
AuthService::httpCallbackPrefixChunk(httpCallbackPrefix,
				    sizeof(httpCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::httpCallbackSuffixChunk(httpCallbackSuffix,
				    sizeof(httpCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::httpHeaderPrefixChunk(httpHeaderCallbackPrefix,
				    sizeof(httpHeaderCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::httpHeaderSuffixChunk(httpHeaderCallbackSuffix,
				    sizeof(httpHeaderCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::httpNegoPrefixChunk(httpNegoCallbackPrefix,
				    sizeof(httpNegoCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::httpNegoSuffixChunk(httpNegoCallbackSuffix,
				    sizeof(httpNegoCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::httpErrorCodePrefixChunk(httpCodeCallbackPrefix,
				    sizeof(httpCodeCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::httpErrorCodeSuffixChunk(httpCodeCallbackSuffix,
				    sizeof(httpCodeCallbackSuffix) - 1);

const AuthService::BodyChunk
AuthService::httpTokenPrefixChunk(httpTokenCallbackPrefix,
				    sizeof(httpTokenCallbackPrefix) - 1);

const AuthService::BodyChunk
AuthService::httpTokenSuffixChunk(httpTokenCallbackSuffix,
				    sizeof(httpTokenCallbackSuffix) - 1);


/*
 * AuthService constructor.
 * Throws: InternalException upon error
 */
AuthService::AuthService(const Properties &config) 
    : BaseService(AUTH_SVC_MODULE, config),
    logID(Log::addModule(AUTH_SVC_MODULE)),
    orgName(""),
    namingURL(config.get(AM_COMMON_NAMING_URL_PROPERTY, "")),
    cookieList()
{

} // constructor


/*
 * create_auth_context
 * Throws: InternalException upon error
 */
void
AuthService::create_auth_context(AuthContext &auth_ctx)
{

    am_status_t status;

    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
		    10, false);

    Http::Response response;

    BodyChunkList &bodyChunkList = request.getBodyChunkList();
    BodyChunk firstAuthIdentifier("0", 1);

    if(auth_ctx.orgName.size() == 0) {
	auth_ctx.orgName = orgName; // no org passed in so use property
    }

    // Do entity Reference conversions
    Utils::expandEntityRefs(auth_ctx.orgName);

    if(auth_ctx.orgName.size() == 0) {
	Log::log(logID, Log::LOG_ERROR,
		"AuthService::create_auth_context() "
		"No org name specified in properties file or input parameter.");
	throw InternalException("AuthService::create_auth_context() ",
		"No org name specified in properties file or input parameter.",
		AM_AUTH_FAILURE);
    }

    if(auth_ctx.namingURL.size() == 0) {
	auth_ctx.namingURL = namingURL; // no url passed in so use property
    }

    // Do entity Reference conversions
    Utils::expandEntityRefs(auth_ctx.namingURL);

    if(auth_ctx.namingURL.size() == 0) {
	Log::log(logID, Log::LOG_ERROR,
		"AuthService::create_auth_context() "
		"No Naming URL specified in properties file or "
		"input parameter.");
	throw InternalException("AuthService::create_auth_context() ",
		"No Naming URL specified in properties file or "
		"input parameter.",
		AM_AUTH_FAILURE);
    }

    setAuthSvcInfo(auth_ctx);

    BodyChunk orgNameBodyChunk(auth_ctx.orgName);
    bodyChunkList.push_back(firstAuthIdentifier);
    bodyChunkList.push_back(newAuthContextPrefixChunk);
    bodyChunkList.push_back(orgNameBodyChunk);
    bodyChunkList.push_back(newAuthContextSuffixChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);
    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), Http::CookieList(),
			bodyChunkList, response);
    if(status != AM_SUCCESS) {
	throw InternalException("AuthService::create_auth_context()",
		"Error sending request for authentication context from server.",
		status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
					    request.getGlobalId());

    if(authCtxResponses.empty()) {
	throw InternalException("AuthService::create_auth_context()",
				"Received empty response set from server.",
				AM_AUTH_CTX_INIT_FAILURE);
    }

    Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[0].c_str());
    XMLTree::Init xt;
    XMLTree authCtxTree(false, authCtxResponses[0].c_str(),
			authCtxResponses[0].size());

    XMLElement rootElem = authCtxTree.getRootElement();
    processResponse(auth_ctx, rootElem);
    cookieList = response.getCookieList();

    return;
} // create_auth_context


/*
 * setAuthSvcInfo
 * Throws: InternalException upon error
 */
void
AuthService::setAuthSvcInfo(AuthContext &auth_ctx)
{

    // The IS Naming Service returns Naming Service Table
    // URLs without replacing %protocol://%host:%port
    // when the request does not have an SSO token.
    // The Naming Service is not currently contacted
    // in this code to obtain the Auth Service URL.
    // It may be desirable to do so in the future.

    const std::string namingservice("/namingservice");
    const std::string authservice("/authservice");
    std::string authURL;
    std::size_t pos = 0;

    authURL = auth_ctx.namingURL;

    pos = authURL.find (namingservice, pos);
    while (pos != std::string::npos) {
	authURL.replace (pos, namingservice.size(), authservice);
	pos = authURL.find (namingservice, pos + 1);
    }

    try {
	URL verifyURL(authURL);
    } catch (InternalException &iex) {
	throw InternalException("AuthService::setAuthSvcInfo()",
				"Malformed URL.",
				AM_AUTH_FAILURE);
    }

    auth_ctx.authSvcInfo.setFromString(authURL);
    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "Number of servers in service:%d, '%s'.",
	     auth_ctx.authSvcInfo.getNumberOfServers(), authURL.c_str());

    return;
} // setAuthSvcInfo


/*
 * login
 * Throws: InternalException upon error
 */
void
AuthService::login(AuthContext &auth_ctx,
		   am_auth_index_t index_type,
		   const std::string &index_name) 
{

    am_status_t status = AM_FAILURE;
    std::string idxName(index_name);

    // check supported index types
    if (index_type != AM_AUTH_INDEX_AUTH_LEVEL &&
	index_type != AM_AUTH_INDEX_ROLE &&
	index_type != AM_AUTH_INDEX_USER &&
	index_type != AM_AUTH_INDEX_MODULE_INSTANCE &&
	index_type != AM_AUTH_INDEX_SERVICE) {
	std::string msg("Error: unsupported index type ");
	msg.append(index_type_string[index_type]);
	throw InternalException("AuthService::login()",
				msg.c_str(),
				AM_INVALID_ARGUMENT);
    }

    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
                    10, false);

    Http::Response response;

    // Do entity Reference conversions
    Utils::expandEntityRefs(idxName);

    BodyChunkList &bodyChunkList = request.getBodyChunkList();
    BodyChunk authIdentifierChunk(auth_ctx.authIdentifier);
    BodyChunk indexTypeNamePairBodyChunk(index_type_string[index_type],
					 strlen(index_type_string[index_type]));
    BodyChunk indexNameBodyChunk(idxName);

    bodyChunkList.push_back(authIdentifierChunk);
    bodyChunkList.push_back(loginPrefixChunk);
    bodyChunkList.push_back(indexTypeNamePairPrefixChunk);
    bodyChunkList.push_back(indexTypeNamePairBodyChunk);
    bodyChunkList.push_back(indexNamePrefixChunk);
    bodyChunkList.push_back(indexNameBodyChunk);
    bodyChunkList.push_back(indexNameSuffixChunk);
    bodyChunkList.push_back(indexTypeNamePairSuffixChunk);
    bodyChunkList.push_back(loginSuffixChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);

    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), cookieList,
			bodyChunkList, response);
    if(status != AM_SUCCESS) {
        throw InternalException("AuthService::login()",
		"Error sending login request to server.",
		status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
                                            request.getGlobalId());

    if(authCtxResponses.empty()) {
        throw InternalException("AuthService::login()",
				"Received empty response set from server.",
                                AM_AUTH_FAILURE);
    }

    std::size_t i = 0;
    XMLTree::Init xt;
    for(i = 0; i < authCtxResponses.size(); i++) {
	Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[i].c_str());
	XMLTree authCtxTree(false, authCtxResponses[i].c_str(),
			    authCtxResponses[i].size());
	XMLElement rootElem = authCtxTree.getRootElement();
	processResponse(auth_ctx, rootElem);
    }

    return;
} // login


/*
 * logout
 * Throws: InternalException upon error
 */
void
AuthService::logout(AuthContext &auth_ctx) 
{

    am_status_t status = AM_FAILURE;

    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
		    10, false);

    Http::Response response;

    BodyChunkList &bodyChunkList = request.getBodyChunkList();

    BodyChunk authIdentifierChunk(auth_ctx.authIdentifier);

    bodyChunkList.push_back(authIdentifierChunk);
    bodyChunkList.push_back(logoutChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);

    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), cookieList,
			bodyChunkList, response);
    if(status != AM_SUCCESS) {
        throw InternalException("AuthService::logout()",
		"Error sending logout request to server.",
		status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
                                            request.getGlobalId());

    if(authCtxResponses.empty()) {
        throw InternalException("AuthService::logout()",
				"Received empty response set from server.",
                                AM_AUTH_FAILURE);
    }

    XMLTree::Init xt;
    for(std::size_t i = 0; i < authCtxResponses.size(); i++) {
	Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[i].c_str());
	XMLTree authCtxTree(false, authCtxResponses[i].c_str(),
			    authCtxResponses[i].size());
	XMLElement rootElem = authCtxTree.getRootElement();
	processResponse(auth_ctx, rootElem);
    }

    return;
} // logout


/*
 * abort
 * Throws: InternalException upon error
 */
void
AuthService::abort(AuthContext &auth_ctx) 
{

    am_status_t status = AM_FAILURE;

    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
		    10, false);

    Http::Response response;

    BodyChunkList &bodyChunkList = request.getBodyChunkList();
    BodyChunk authIdentifierChunk(auth_ctx.authIdentifier);

    bodyChunkList.push_back(authIdentifierChunk);
    bodyChunkList.push_back(abortChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);

    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), cookieList,
			bodyChunkList, response);
    if(status != AM_SUCCESS) {
        throw InternalException("AuthService::abort()",
		"Error sending abort request to server.",
		status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
                                            request.getGlobalId());

    if(authCtxResponses.empty()) {
        throw InternalException("AuthService::abort()",
				"Received empty response set from server.",
                                AM_AUTH_FAILURE);
    }

    XMLTree::Init xt;
    for(std::size_t i = 0; i < authCtxResponses.size(); i++) {
	Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[i].c_str());
	XMLTree authCtxTree(false, authCtxResponses[i].c_str(),
			    authCtxResponses[i].size());
	XMLElement rootElem = authCtxTree.getRootElement();
	processResponse(auth_ctx, rootElem);
    }

    return;
} // abort


/*
 * getModuleInstanceNames
 * Throws: InternalException upon error
 */
void
AuthService::getModuleInstanceNames(AuthContext &auth_ctx, 
				    am_string_set_t** module_inst_names) 
{

    am_status_t status = AM_FAILURE;
 
    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
			10, false);

    Http::Response response;
    
    BodyChunkList &bodyChunkList = request.getBodyChunkList();
    BodyChunk authIdentifierChunk(auth_ctx.authIdentifier);
    bodyChunkList.push_back(authIdentifierChunk);
    bodyChunkList.push_back(moduleInstanceNamesPrefixChunk);
    bodyChunkList.push_back(moduleInstanceNamesSuffixChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);

    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), cookieList,
			    bodyChunkList, response);
    if(status != AM_SUCCESS) {
	throw InternalException("AuthService::getModuleInstanceNames()",
				"Error sending getModuleInstanceNames request",
				status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
						request.getGlobalId());

    if(authCtxResponses.empty()) {
	throw InternalException("AuthService::getModuleInstanceNames()",
				"Received empty response set from server.",
				AM_AUTH_FAILURE);
    }

    XMLTree::Init xt;
    for(std::size_t i = 0; i < authCtxResponses.size(); i++) {
	Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[i].c_str());
	XMLTree authCtxTree(false, authCtxResponses[i].c_str(),
				authCtxResponses[i].size());
	XMLElement rootElem = authCtxTree.getRootElement();
	XMLElement validResp = validateResponse(rootElem);
	if (validResp.isNamed(QUERY_RESULT)) {
	    processGetModuleInstanceNames(auth_ctx, validResp, 
					     module_inst_names);
	}
    }

    return;
} // getModuleInstanceNames


/*
 * processResponse
 *             Process response from server.
 * Throws: InternalException upon error
 */
void
AuthService::processResponse(AuthContext &auth_ctx,
			     const XMLElement &authCtxNode)
{

    XMLElement validResp = validateResponse(authCtxNode);

    if(validResp.isNamed(LOGIN_STATUS)) {
	processLoginStatus(auth_ctx, authCtxNode.getFirstSubElement());
    }

    else if(validResp.isNamed(GET_REQUIREMENTS)) {
	processGetRequirements(auth_ctx, validResp);
    }

    else if(validResp.isNamed(EXCEPTION)) {
	logAndThrowException("AuthService::processResponse()", validResp);
    }

    return;
} // processResponse


/*
 *  processGetModuleInstanceNames
 *             Process get requirements from server.
 * Throws: InternalException upon error
 */
void
AuthService::processGetModuleInstanceNames(AuthContext &auth_ctx,
					   const XMLElement &getReqNode,
					   am_string_set_t** module_inst_names)
{
    am_string_set_t *string_set;
    XMLElement valueNode;
    int num_instances = 0;
    std::string value;
    if (getReqNode.getSubElement(VALUE, valueNode)) {
       
	if(valueNode.getValue(value) && value.length() > 0) {
	    num_instances++;
	    while(valueNode.nextSibling(VALUE)) {
		num_instances++;
	    }
	}
       
	// allocate memory
	if (num_instances != 0) {
	    *module_inst_names = am_string_set_allocate(num_instances);
	    string_set = *module_inst_names;
	    if (string_set == NULL) {
		Log::log(logID, Log::LOG_WARNING,
		    "AuthService::processGetModuleInstanceNames() "
		    "Cannot allocate memory.");
	        *module_inst_names = NULL;
		throw InternalException(
		"AuthService::processGetModuleInstanceNames()",
				   "Cannot allocate memory.",
				    AM_NO_MEMORY);
	    }
	} else { // really shouldn't happen
	    Log::log(logID, Log::LOG_WARNING,
		   "AuthService::processGetModuleInstanceNames() "
		   "No modules found.");
	    *module_inst_names = NULL;
	}
	num_instances = 0;
	
	if(getReqNode.getSubElement(VALUE, valueNode)) {
	    if(valueNode.getValue(value) && value.length() > 0) {
		string_set->strings[num_instances] = 
		    ( char *) strdup(value.c_str());    
	    }
	    num_instances++; 
	    while(valueNode.nextSibling(VALUE)) {
		if(valueNode.getValue(value) && value.length() > 0) {
		    string_set->strings[num_instances] = 
			( char *) strdup(value.c_str());
		    num_instances++;
		}
	    }
	    string_set->size = num_instances;
	}
	
    }
    
    return;
} //  processGetModuleInstanceNames


/*
 * processGetRequirements
 *             Process get requirements from server.
 * Throws: InternalException upon error
 */
void
AuthService::processGetRequirements(AuthContext &auth_ctx,
				    const XMLElement &getReqNode)
{
    XMLElement callbacksNode;

    if(getReqNode.getSubElement(CALLBACKS, callbacksNode)) {

	std::string lengthStr;
	std::size_t numCallbackNodes = 0;
	if(callbacksNode.getAttributeValue(LENGTH, lengthStr)) {
	    try {
		numCallbackNodes = Utils::getNumber(lengthStr);
		auth_ctx.callbacks.reserve(numCallbackNodes);
	    }
	    catch (std::exception &ex) {
		throw InternalException(
			"AuthService::processGetRequirements()",
			ex.what(),
			AM_INVALID_ARGUMENT);
	    }
	    catch (...) {
		throw InternalException(
			"AuthService::processGetRequirements",
			"Error parsing length.",
			AM_INVALID_ARGUMENT);
	    }
	}

	for(XMLElement oneClbkNode = callbacksNode.getFirstSubElement();
	    oneClbkNode.isValid(); oneClbkNode.nextSibling()) {
	    processIndividualCallbacks(auth_ctx, oneClbkNode);
	}

	// The number of callbacks processed depends on whether
	// the Page Callback is received (which is ignored).
	if(numCallbackNodes != auth_ctx.callbacks.size() &&
	numCallbackNodes != (auth_ctx.callbacks.size() - 1)) {
	    Log::log(logID, Log::LOG_WARNING,
		"AuthService::processGetRequirements() "
		"The number of callbacks received was %lu. "
		"The number of callbacks processed was %lu. ",
		numCallbackNodes, auth_ctx.callbacks.size());
	}
    }

    return;
} // processGetRequirements


/*
 * processIndividualCallbacks
 *             Process individual callbacks from server.
 * Throws: InternalException upon error
 */
void
AuthService::processIndividualCallbacks(AuthContext &auth_ctx,
					const XMLElement &callbackNode)
{

    am_auth_callback_t callback;
    std::memset(&callback, 0, sizeof(callback));

    if(callbackNode.isNamed(NAME_CALLBACK)) {

	// process NameCallback

	am_auth_name_callback_t &name_cb =
	    callback.callback_info.name_callback;
	callback.callback_type = NameCallback;
	processNameCallback(name_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(PASSWORD_CALLBACK)) {

	// process PasswordCallback

	am_auth_password_callback_t &password_cb =
	    callback.callback_info.password_callback;
	callback.callback_type = PasswordCallback;
	processPasswordCallback(password_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(CHOICE_CALLBACK)) {

	// process ChoiceCallback

	am_auth_choice_callback_t &choice_cb =
	    callback.callback_info.choice_callback;
	callback.callback_type = ChoiceCallback;
	processChoiceCallback(choice_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(CONFIRMATION_CALLBACK)) {

	// process ConfirmationCallback

	am_auth_confirmation_callback_t &confirmation_cb =
	    callback.callback_info.confirmation_callback;
	callback.callback_type = ConfirmationCallback;
	processConfirmationCallback(confirmation_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(HTTP_CALLBACK)) {

	// process HTTPCallback  OPENAM-45

	am_auth_http_callback_t &http_cb =
	    callback.callback_info.http_callback;
	callback.callback_type = HTTPCallback;
	processHTTPCallback(http_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(REDIRECT_CALLBACK)) {

	// process RedirectCallback OPENAM-45

	am_auth_redirect_callback_t &redirect_cb =
	    callback.callback_info.redirect_callback;
	callback.callback_type = RedirectCallback;
	processRedirectCallback(redirect_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(LANGUAGE_CALLBACK)) {

	// process LanguageCallback

	am_auth_language_callback_t &language_cb =
	    callback.callback_info.language_callback;
	callback.callback_type = LanguageCallback;
	processLanguageCallback(language_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(TEXT_INPUT_CALLBACK)) {

	// process TextInputCallback

	am_auth_text_input_callback_t &text_input_cb =
	    callback.callback_info.text_input_callback;
	callback.callback_type = TextInputCallback;
	processTextInputCallback(text_input_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(TEXT_OUTPUT_CALLBACK)) {

	// process TextOutputCallback

	am_auth_text_output_callback_t &text_output_cb =
	    callback.callback_info.text_output_callback;
	callback.callback_type = TextOutputCallback;
	processTextOutputCallback(text_output_cb, callbackNode);

	// add to the list of callbacks
	auth_ctx.callbacks.push_back(callback);

    } else if(callbackNode.isNamed(PAGE_PROPERTIES_CALLBACK)) {
	Log::log(logID, Log::LOG_WARNING,
		 "AuthService::processIndividualCallbacks() "
		 "Ignoring PagePropertiesCallback.");
    } else {
	std::string nodeName;
	callbackNode.getName(nodeName);
	std::string msg("Unsupported node encountered=");
	msg.append(nodeName);

	Log::log(logID, Log::LOG_ERROR,
		 "AuthService::processIndividualCallbacks() "
		 "Unsupported node encountered [%s].", nodeName.c_str());
	throw InternalException("AuthService::processIndividualCallbacks",
				msg.c_str(),
				AM_AUTH_FAILURE);
    }

    return;
} // processIndividualCallbacks


/*
 * processChoiceCallback
 *             Process choice callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processChoiceCallback(am_auth_choice_callback_t &choice_cb,
			const XMLElement &callbackNode)
{

    // set "allow multiple selections"
    choice_cb.allow_multiple_selections = B_FALSE;
    std::string multipleSelectionsAllowedValueStr;
    if(callbackNode.getAttributeValue(MULTIPLE_SELECTIONS_ALLOWED,
				  multipleSelectionsAllowedValueStr)) {
	if(strcasecmp(multipleSelectionsAllowedValueStr.c_str(), "true") == 0) {
	    choice_cb.allow_multiple_selections = B_TRUE;
	}
    }

    // set prompt
    XMLElement promptNode;
    if(callbackNode.getSubElement(PROMPT, promptNode)) {
	std::string prompt;
	if(promptNode.getValue(prompt) && prompt.length() > 0) {
	    choice_cb.prompt = (const char *) strdup(prompt.c_str());
	}
    }

    std::size_t num_choices = 0;
    XMLElement choiceValuesNode;
    XMLElement choiceValueNode;
    std::string value;

    // count choices
    num_choices = 0;
    if (callbackNode.getSubElement(CHOICE_VALUES, choiceValuesNode)) {
	if (choiceValuesNode.getSubElement(CHOICE_VALUE, choiceValueNode)) {
	    if(choiceValueNode.getValue(value) && value.length() > 0) {
		num_choices++;
		while(choiceValueNode.nextSibling(CHOICE_VALUE)) {
		    num_choices++;
		}
	    }
	}
    }

    // allocate memory
    if (num_choices != 0) {
	choice_cb.choices =
	    (const char**) calloc (num_choices, sizeof (const char*));
    } else { // really shouldn't happen
	Log::log(logID, Log::LOG_WARNING,
	    "AuthService::processChoiceCallback() "
	    "No choices found.");
    }

    std::string defaultChoiceStr;
    std::string isDefaultStr;

    // assign choices
    num_choices = 0;
    choice_cb.default_choice = 0;
    if (callbackNode.getSubElement(CHOICE_VALUES, choiceValuesNode)) {
	if (choiceValuesNode.getSubElement(CHOICE_VALUE, choiceValueNode)) {
	    if(choiceValueNode.getValue(value) && value.length() > 0) {
		choice_cb.choices[num_choices] =
		    (const char *)strdup(value.c_str());
		// check if this is the default choice
		if(choiceValueNode.getAttributeValue(IS_DEFAULT,
					  isDefaultStr)) {
		    if(strcasecmp(isDefaultStr.c_str(), "yes") == 0) {
			choice_cb.default_choice = num_choices;
		    }
		}
		num_choices++;
		while(choiceValueNode.nextSibling(CHOICE_VALUE)) {
		    if(choiceValueNode.getValue(value) && value.length() > 0) {
			choice_cb.choices[num_choices] =
			    (const char *)strdup(value.c_str());
			// check if this is the default choice
			if(choiceValueNode.getAttributeValue(IS_DEFAULT,
					  isDefaultStr)) {
			    if(strcasecmp(isDefaultStr.c_str(), "yes") == 0) {
				choice_cb.default_choice = num_choices;
			    }
			}
			num_choices++;
		    }
		}
	    }
	}
    }
    choice_cb.choices_size = num_choices;

    return;
} // processChoiceCallback


/*
 * processConfirmationCallback
 *             Process confirmation callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processConfirmationCallback(
			am_auth_confirmation_callback_t &confirmation_cb,
			const XMLElement &callbackNode)
{

    // set message type
    std::string messageTypeStr;
    confirmation_cb.message_type = NULL;
    if(callbackNode.getAttributeValue(MESSAGE_TYPE, messageTypeStr)) {
	confirmation_cb.message_type =
	    (const char *) strdup(messageTypeStr.c_str());
    }

    // set option type
    std::string optionTypeStr;
    confirmation_cb.option_type = NULL;
    if(callbackNode.getAttributeValue(OPTION_TYPE, optionTypeStr)) {
	confirmation_cb.option_type =
	    (const char *) strdup(optionTypeStr.c_str());
    }

    std::size_t num_options;
    XMLElement optionValuesNode;
    XMLElement optionValueNode;
    std::string value;

    // count options
    num_options = 0;
    if(callbackNode.getSubElement(OPTION_VALUES, optionValuesNode)) {
	if(callbackNode.getSubElement(OPTION_VALUE, optionValueNode)) {
	    if(optionValueNode.getValue(value) && value.length() > 0) {
		num_options++;
		while(optionValueNode.nextSibling(OPTION_VALUE)) {
		    if(optionValueNode.getValue(value) && value.length() > 0) {
			num_options++;
		    }
		}
	    }
	}
    }

    // allocate memory
    if (num_options != 0) {
	confirmation_cb.options =
	    (const char**) calloc (num_options, sizeof (const char*));
    } else { // really shouldn't happen
	Log::log(logID, Log::LOG_WARNING,
	    "AuthService::processConfirmationCallback() "
	    "No options found.");
    }

    // assign options
    num_options = 0;
    if(callbackNode.getSubElement(OPTION_VALUES, optionValuesNode)) {
	if(callbackNode.getSubElement(OPTION_VALUE, optionValueNode)) {
	    if(optionValueNode.getValue(value) && value.length() > 0) {
		confirmation_cb.options[num_options] =
		    (const char *) strdup(value.c_str());
		num_options++;
		while(optionValueNode.nextSibling(OPTION_VALUE)) {
		    if(optionValueNode.getValue(value) && value.length() > 0) {
			confirmation_cb.options[num_options] =
			    (const char *) strdup(value.c_str());
			num_options++;
		    }
		}
	    }
	}
    }
    confirmation_cb.options_size = num_options;

    // set default option
    XMLElement defaultOptionNode;
    confirmation_cb.default_option = NULL;
    if(callbackNode.getSubElement(DEFAULT_OPTION_VALUE, defaultOptionNode)) {
	if(defaultOptionNode.getValue(value) && value.length() > 0) {
	    confirmation_cb.default_option =
		(const char *) strdup(value.c_str());
	}
    }

    return;
} // processConfirmationCallback


/*
 * processLanguageCallback
 *             Process language callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processLanguageCallback(am_auth_language_callback_t &language_cb,
			const XMLElement &callbackNode)
{

    language_cb.locale =
	(am_auth_locale_t *) calloc (1, sizeof (am_auth_locale_t));

    XMLElement localeNode;
    if(callbackNode.getSubElement(LOCALE, localeNode)) {
	std::string languageValueStr;
	if(localeNode.getAttributeValue(LANGUAGE, languageValueStr)) {
	    if(languageValueStr.length() > 0) {
		language_cb.locale->language =
		(const char *) strdup(languageValueStr.c_str());
	    }
	}
	std::string countryValueStr;
	if(localeNode.getAttributeValue(COUNTRY, countryValueStr)) {
	    if(countryValueStr.length() > 0) {
		language_cb.locale->country =
		(const char *) strdup(countryValueStr.c_str());
	    }
	}
	std::string variantValueStr;
	if(localeNode.getAttributeValue(VARIANT_AM, variantValueStr)) {
	    if(variantValueStr.length() > 0) {
		language_cb.locale->variant =
		(const char *) strdup(variantValueStr.c_str());
	    }
	}
    }

    return;
} // processLanguageCallback


/*
 * processNameCallback
 *             Process name callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processNameCallback(am_auth_name_callback_t &name_cb,
			const XMLElement &callbackNode)
{

    XMLElement promptNode;
    if(callbackNode.getSubElement(PROMPT, promptNode)) {
	std::string prompt;
	if(promptNode.getValue(prompt) && prompt.length() > 0) {
	    name_cb.prompt = (const char *) strdup(prompt.c_str());
	}
    }

    XMLElement defaultValueNode;
    if(callbackNode.getSubElement(DEFAULT_VALUE, defaultValueNode)) {
	std::string defaultValue;
	if(defaultValueNode.getValue(defaultValue) && defaultValue.length() > 0)
	{
	    name_cb.default_name = (const char *) strdup(defaultValue.c_str());
	}
    }

    return;
} // processNameCallback

/*
 * processHTTPCallback
 *             Process HTTP callback from server.
 * Throws: InternalException upon error
 */


void
AuthService::processHTTPCallback(am_auth_http_callback_t &http_cb,
			const XMLElement &callbackNode)
{

    XMLElement headerNode;
    if(callbackNode.getSubElement(HTTP_HEADER, headerNode)) {
	std::string auth_header;
	if(headerNode.getValue(auth_header) && auth_header.length() > 0) {
	    http_cb.tokenHeader = (const char *) strdup(auth_header.c_str());
	}
    }

    XMLElement negoNode;
    if(callbackNode.getSubElement(HTTP_NEGO, negoNode)) {
	std::string negotiation;
	if(negoNode.getValue(negotiation) && negotiation.length() > 0)
	{
            size_t colon = negotiation.find ( ':' );
            if(colon != std::string::npos) {
                http_cb.negoHeader = (const char *) strdup(negotiation.substr(0,colon).c_str());
                http_cb.negoValue  = (const char *) strdup(negotiation.substr(colon+1).c_str());

            } else {
                http_cb.negoHeader = (const char *) strdup(negotiation.c_str());
                http_cb.negoValue  = (const char *) NULL;
            }
	}
    }

    XMLElement errorCodeNode;
    if(callbackNode.getSubElement(HTTP_CODE, errorCodeNode)) {
	std::string errorCode;
	if(errorCodeNode.getValue(errorCode) && errorCode.length() > 0)
	{
	    http_cb.negoErrorCode = (const char *) strdup(errorCode.c_str());
	}
    }

    XMLElement authTokenNode;
    if(callbackNode.getSubElement(HTTP_CODE, authTokenNode)) {
	std::string authToken;
	if(authTokenNode.getValue(authToken) && authToken.length() > 0)
	{
	    http_cb.response = (const char *) strdup(authToken.c_str());
	}
    }

    return;
} // processHTTPCallback


/*
 * processRedirectCallback
 *             Process redirect callback from server.
 * Throws: InternalException upon error
 **
 * <RedirectCallBack method=XXX>
 * <RedirectURL>
 *     magicEncoded URL
 * </RedirectURL>
 * <RedirectData>
 *    <RedirectName>
 *         Name
 *    </RedirectName>
 *    <RedirectValue>
 *         value
 *    </RedirectValue>
 * </RedirectData>
 * <RedirectStatusParam>
 *     RedirectStatus
 * </RedirectStatusParam>
 * <RedirectBackUrlCookie>
 *     RedirectBackURL
 * </RedirectBackUrlCookie>
 * <Status>
 *     status
 * </Status>

 */



void
AuthService::processRedirectCallback(am_auth_redirect_callback_t &redirect_cb,
			const XMLElement &callbackNode)
{

    std::string redirectMethodStr;
    redirect_cb.method = NULL;
    if(callbackNode.getAttributeValue(REDIRECT_METHOD, redirectMethodStr)) {
	redirect_cb.method = (const char *) strdup(redirectMethodStr.c_str());
    } else {
        redirect_cb.method = (const char *) strdup("get");
    }

    
    XMLElement redirectURLNode;
    if(callbackNode.getSubElement(REDIRECT_URL, redirectURLNode)) {
	std::string redirectURL;
	if(redirectURLNode.getValue(redirectURL) && redirectURL.length() > 0) {
	    redirect_cb.redirectUrl = (const char *) strdup(redirectURL.c_str());
	}
    }

    XMLElement redirectDataNode;
    if(callbackNode.getSubElement(REDIRECT_DATA, redirectDataNode)) {
        // Process all redirect data
    }

    XMLElement redirectStatusNode;
    if(callbackNode.getSubElement(REDIRECT_STATUS, redirectStatusNode)) {
	std::string redirectStatus;
	if(redirectStatusNode.getValue(redirectStatus) && redirectStatus.length() > 0) {
	    redirect_cb.statusParameter = (const char *) strdup(redirectStatus.c_str());
	} else {
	    redirect_cb.statusParameter = (const char *) strdup("AM_AUTH_SUCCESS_PARAM");
        }
    }

    XMLElement redirectCookieNode;
    if(callbackNode.getSubElement(REDIRECT_BACK_URL_COOKIE, redirectCookieNode)) {
	std::string redirectCookie;
	if(redirectCookieNode.getValue(redirectCookie) && redirectCookie.length() > 0) {
	    redirect_cb.redirectBackUrlCookie = (const char *) strdup(redirectCookie.c_str());
	} else {
            redirect_cb.redirectBackUrlCookie = (const char *) strdup("AM_REDIRECT_BACK_SERVER_URL");
        }
    }

    return;
} // processRedirectCallback


/*
 * processPasswordCallback
 *             Process password callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processPasswordCallback(am_auth_password_callback_t &password_cb,
			const XMLElement &callbackNode)
{

    std::string echoPasswdValueStr;
    password_cb.echo_on = B_FALSE;
    if(callbackNode.getAttributeValue(ECHO_PASSWORD, echoPasswdValueStr)) {
	if(strcasecmp(echoPasswdValueStr.c_str(), "true") == 0) {
	    password_cb.echo_on = B_TRUE;
	}
    }

    XMLElement promptNode;
    if(callbackNode.getSubElement(PROMPT, promptNode)) {
	std::string prompt;
	if(promptNode.getValue(prompt) && prompt.length() > 0) {
	    password_cb.prompt = (const char *) strdup(prompt.c_str());
	}
    }

    return;
} // processPasswordCallback


/*
 * processTextInputCallback
 *             Process text input callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processTextInputCallback(
			am_auth_text_input_callback_t &text_input_cb,
			const XMLElement &callbackNode)
{

    XMLElement promptNode;
    if(callbackNode.getSubElement(PROMPT, promptNode)) {
	std::string prompt;
	if(promptNode.getValue(prompt) && prompt.length() > 0) {
	    text_input_cb.prompt = (const char *) strdup(prompt.c_str());
	}
    }

    XMLElement defaultValueNode;
    if(callbackNode.getSubElement(DEFAULT_VALUE, defaultValueNode)) {
	std::string defaultValue;
	if(defaultValueNode.getValue(defaultValue) && defaultValue.length() > 0)
	{
	    text_input_cb.default_text =
		(const char *) strdup(defaultValue.c_str());
	}
    }

    return;
} // processTextInputCallback


/*
 * processTextOutputCallback
 *             Process text output callback from server.
 * Throws: InternalException upon error
 */
void
AuthService::processTextOutputCallback(
			am_auth_text_output_callback_t &text_output_cb,
			const XMLElement &callbackNode)
{

    // set message type
    std::string messageTypeStr;
    text_output_cb.message_type = NULL;
    if(callbackNode.getAttributeValue(MESSAGE_TYPE, messageTypeStr)) {
	text_output_cb.message_type =
	    (const char *) strdup(messageTypeStr.c_str());
    }

    XMLElement valueNode;
    if(callbackNode.getSubElement(VALUE, valueNode)) {
	std::string value;
	if(valueNode.getValue(value) && value.length() > 0) {
	    text_output_cb.message = (const char *) strdup(value.c_str());
	}
    }

    return;
} // processTextOutputCallback


/*
 * processLoginStatus
 * Throws: InternalException upon error
 */
void
AuthService::processLoginStatus(AuthContext &auth_ctx,
				const XMLElement &responseNode)
{

    XMLElement loginStatusNode;
    if(responseNode.getSubElement(LOGIN_STATUS, loginStatusNode)) {
	std::string status;
	if(loginStatusNode.getAttributeValue(STATUS, status)) {
	    const char *statusStr = status.c_str();
	    if(strcmp(statusStr, FAILED) == 0) {
            // process AUTH_STATUS_FAILED
            XMLElement exceptionNode;
            auth_ctx.authStatus = AM_AUTH_STATUS_FAILED;
            if(responseNode.getSubElement(EXCEPTION, exceptionNode)) {
                logAndThrowException("AuthService::processLoginStatus()",
                                             exceptionNode);
            } else {
                throw InternalException("AuthService::processLoginStatus()",
                            "Login failed.",
                            AM_AUTH_FAILURE);
            }
	    } else if(strcmp(statusStr, COMPLETED) == 0) {
            // process AUTH_STATUS_COMPLETED
            auth_ctx.authStatus = AM_AUTH_STATUS_COMPLETED;
            Log::log(logID, Log::LOG_INFO,
                 "AuthService::procesLoginStatus() "
                 "Login completed.");
	    } else if(strcmp(statusStr, SUCCESS) == 0) {
            // process AUTH_STATUS_SUCCESS
            auth_ctx.authStatus = AM_AUTH_STATUS_SUCCESS;
            XMLElement subjectNode;
            if(loginStatusNode.getSubElement(SUBJECT, subjectNode)) {
                if(subjectNode.getValue(auth_ctx.subject)) {
                    Log::log(logID, Log::LOG_INFO,
                     "AuthService::processLoginStatus(): "
                     "Successful login of subject %s",
                     auth_ctx.subject.c_str());
                }
            }
            if(loginStatusNode.getAttributeValue(SSO_TOKEN,
						     auth_ctx.ssoToken)) {
                Log::log(logID, Log::LOG_INFO,
                     "AuthService::processLoginStatus() "
                     "Successful login of ssoToken %s",
                     auth_ctx.ssoToken.c_str());
            }

            // the auth identifier is checked again because a new one
            // is generated after the successful authentication.
            if(responseNode.getAttributeValue(AUTH_IDENTIFIER,
						  auth_ctx.authIdentifier)) {
                Log::log(logID, Log::LOG_INFO,
			     "AuthService::processLoginStatus() "
			     "Auth Identifier =%s",
			     auth_ctx.authIdentifier.c_str());
            } else {
                Log::log(logID, Log::LOG_ERROR,
                    "AuthService::processLoginStatus() "
                    "Auth Identifier not found in server response.");
                throw InternalException("AuthService::processLoginStatus()",
                "No Auth Identifier found in server response.",
                AM_AUTH_FAILURE);
            }

	    } else if(strcmp(statusStr, IN_PROGRESS) == 0) {
            // process AUTH_STATUS_IN_PROGRESS
            auth_ctx.authStatus = AM_AUTH_STATUS_IN_PROGRESS;
            Log::log(logID, Log::LOG_DEBUG,
                 "AuthService::processLoginStatus() "
                 "Login is in progress.");
            // there could be an exception after status
            // throw an error if so.
            XMLElement excNode;
            if (responseNode.getSubElement(EXCEPTION, excNode)) {
                std::string errorCode;
                if (excNode.getAttributeValue(ERROR_CODE, errorCode)) {
                    Log::log(logID, Log::LOG_ERROR,
                        "AuthService::processLoginStatus() "
                        "Exception fround in server response: "
                        "error code '%s'.", errorCode.c_str());
                        throw InternalException(
                            "AuthService::processLoginStatus()",
                            "Exception found in server response.",
                            AM_AUTH_FAILURE);
                }
                else {
                    Log::log(logID, Log::LOG_ERROR,
                        "AuthService::processLoginStatus() "
                        "Exception (with no error code) fround in "
                        "server response.");
                    throw InternalException(
                        "AuthService::processLoginStatus()",
                        "Exception (with no error code) found in "
                        "server response.", AM_AUTH_FAILURE);
                }
            }
            // check for auth identifier.
            if(responseNode.getAttributeValue(AUTH_IDENTIFIER,
						  auth_ctx.authIdentifier)) {
                Log::log(logID, Log::LOG_INFO,
			     "AuthService::processLoginStatus() "
			     "Auth Identifier =%s",
			     auth_ctx.authIdentifier.c_str());
            } else {
                //exception
                Log::log(logID, Log::LOG_ERROR,
                    "AuthService::processLoginStatus() "
                    "Auth Identifier not found in server response.");
                throw InternalException("AuthService::processLoginStatus()",
                "No Auth Identifier found in server response.",
                AM_AUTH_FAILURE);
            }
	    }
	}
    } else {
	//exception
	Log::log(logID, Log::LOG_ERROR,
			"AuthService::processLoginStatus() "
			"Login status not found in server response.");
	throw InternalException("AuthService::processLoginStatus()",
				"No login status found in server response.",
				AM_AUTH_FAILURE);
    }
    return;
} // processLoginStatus


/*
 * logAndThrowException
 * Throws: InternalException upon error
 */
void
AuthService::logAndThrowException(const std::string& method, 
                                  const XMLElement &exceptionNode)
{

    std::string value;
    std::string msg = "Exception";
    if(exceptionNode.getAttributeValue(MESSAGE, value)) {
	msg.append(" message=[");
	msg.append(value);
	msg.append("]");
    }
    if(exceptionNode.getAttributeValue(TOKEN_ID, value)) {
	msg.append(" tokenId=");
	msg.append(value);
    }
    if(exceptionNode.getAttributeValue(ERROR_CODE, value)) {
	msg.append(" errorCode='");
	msg.append(value);
	msg.append("'");
    }
    if(exceptionNode.getAttributeValue(TEMPLATE_NAME, value)) {
	msg.append(" templateName=");
	msg.append(value);
    }
    Log::log(logID, Log::LOG_ERROR, 
             "%s %s.", method.c_str(), msg.c_str());
    throw InternalException(method, msg, AM_AUTH_FAILURE);
} // logAndThrowException


/*
 * validateResponse
 *             Validate response from server.
 * Throws: InternalException upon error
 */
XMLElement
AuthService::validateResponse(const XMLElement &authCtxNode)
{

    std::string respName;
    XMLElement responseNode = authCtxNode.getFirstSubElement();
    if(responseNode.isNamed(RESPONSE)) {
	XMLElement respType = responseNode.getFirstSubElement();
	respType.getName(respName);

	if(respType.isNamed(QUERY_RESULT)) {

	    // process QueryResult
	   return respType;
	} else if(respType.isNamed(GET_REQUIREMENTS)) {
	
	    //process GetRequirements
	    return respType;
	} else if(respType.isNamed(LOGIN_STATUS)) {
	
	    //process LoginStatus
	    return respType;
	} else if(respType.isNamed(EXCEPTION)) {

	    //process Exception
	    throw InternalException("AuthService::validateResponse()",
				    "Received exception from server.",
				    AM_FAILURE);
	}
    }
    // Log error since no response is found.
    std::string msg("Invalid response from server.");
    msg.append(respName);
    throw InternalException("AuthService::validateResponse()",
			    msg, AM_FAILURE);
} // validateResponse


/*
 * submitRequirements
 *             Submit the callback requirements as filled in by client
 *             back to the server.
 */
void
AuthService::submitRequirements(AuthContext &auth_ctx) {

    am_status_t status;
    std::size_t i = 0;

    if(auth_ctx.authStatus != AM_AUTH_STATUS_IN_PROGRESS) {
	Log::log(logID, Log::LOG_ERROR, "AuthService::submitRequirements() "
		 "Invalid invocation of am_auth_submit_requirements().");
	throw InternalException(
	    "AuthService::submitRequirements()",
	    "The AuthContext status is not ready to submit requirements.",
	    AM_INVALID_ARGUMENT);
    }

    // start building XML to be submitted to server
    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
                    10, false);

    // get handle to XML so more elements and attributes can be appended
    BodyChunkList &bodyChunkList = request.getBodyChunkList();

    BodyChunk authIdentifierChunk(auth_ctx.authIdentifier);

    bodyChunkList.push_back(authIdentifierChunk);
    bodyChunkList.push_back(submitRequirementsPrefixChunk);

    bodyChunkList.push_back(callbacksPrefixChunk);

    // count requirements so the length attribute can be set
    std::size_t numCallbacks = 0;
    numCallbacks = validateAndCountRequirements(auth_ctx);

    char number[15];
    snprintf(number, 14, "%lu", numCallbacks);
    bodyChunkList.push_back(BodyChunk(number, strlen(number)));
    bodyChunkList.push_back(quoteEndElementChunk);

    // add all callback requirements to bodyChunkList
    addCallbackRequirements(auth_ctx, bodyChunkList);

    // done processing all callbacks
    bodyChunkList.push_back(callbacksSuffixChunk);
    bodyChunkList.push_back(submitRequirementsSuffixChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);

    // post callback requirements as filled in by client to server
    Http::Response response;
    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), cookieList,
                        bodyChunkList, response);

    auth_ctx.cleanupCallbacks();

    if(status != AM_SUCCESS) {
        throw InternalException("AuthService::submitRequirements()",
		"Error sending client submitted requirements to server.",
		status);
    }

    // parse response from server
    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
                                            request.getGlobalId());

    if(authCtxResponses.empty()) {
        throw InternalException("AuthService::submitRequirements()",
				"Received empty response set from server.",
				AM_AUTH_FAILURE);
    }

    XMLTree::Init xt;
    for(i = 0; i < authCtxResponses.size(); i++) {
	Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[i].c_str());
	XMLTree authCtxTree(false, authCtxResponses[i].c_str(),
			    authCtxResponses[i].size());
	XMLElement rootElem = authCtxTree.getRootElement();
	processResponse(auth_ctx, rootElem);
    }

    return;
} // submitRequirements


/*
 * validateAndCountRequirements
 *             Validates and counts requirements as set by client.
 * Throws: InternalException upon error
 */
const std::size_t
AuthService::validateAndCountRequirements(AuthContext &auth_ctx)
{

    std::size_t i = 0;
    std::size_t numCallbacks = 0;

    // validation of callbacks coming in.
    for(i = 0; i < auth_ctx.callbacks.size(); i++) {
	am_auth_callback_t &callback = auth_ctx.callbacks[i];
	switch(callback.callback_type) {
	case NameCallback:
	    if(callback.callback_info.name_callback.response == NULL) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthService::validateAndCountRequirements() "
			"NameCallback does not have a valid response.");
		throw InternalException(
			"AuthService::validateAndCountRequirements()",
			"NameCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case PasswordCallback:
	    if(callback.callback_info.password_callback.response == NULL) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthService::validateAndCountRequirements() "
			"PasswordCallback does not have a valid response.");
		throw InternalException(
			"AuthService::validateAndCountRequirements()",
			"PasswordCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case ChoiceCallback:
	    if( (callback.callback_info.choice_callback.response == NULL) ||
		(callback.callback_info.choice_callback.response_size == 0) ||
		(callback.callback_info.choice_callback.response[0] == NULL) ) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthContext::validateAndCountRequirements() "
			"ChoiceCallback does not have a valid response.");
		throw InternalException(
			"AuthContext::validateAndCountRequirements()",
			"ChoiceCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case ConfirmationCallback:
	    if(callback.callback_info.confirmation_callback.response == NULL) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthContext::validateAndCountRequirements() "
			"ConfirmationCallback does not have a valid response.");
		throw InternalException(
			"AuthContext::validateAndCountRequirements()",
			"ConfirmationCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case LanguageCallback:
	    if( (callback.callback_info.language_callback.response
		    == NULL) ||
		(callback.callback_info.language_callback.response->language[0]
		    == '\0') ||
		(callback.callback_info.language_callback.response->country[0]
		    == '\0') ) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthContext::validateAndCountRequirements() "
			"LanguageCallback does not have a valid response.");
		throw InternalException(
			"AuthContext::validateAndCountRequirements()",
			"LanguageCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case TextInputCallback:
	    if(callback.callback_info.text_input_callback.response == NULL) {
		Log::log(logID, Log::LOG_ERROR,
			"AuthService::validateAndCountRequirements() "
			"TextInputCallback does not have a valid response.");
		throw InternalException(
			"AuthService::validateAndCountRequirements()",
			"TextInputCallback does not have a valid response.",
			AM_INVALID_ARGUMENT);
	    }
	    ++numCallbacks;
	    break;
	case TextOutputCallback:
	    // nothing to do
	    break;
	default:
	    break;
	}
    }

    return numCallbacks;
} // validateAndCountRequirements


/*
 * addCallbackRequirements
 *             Use callback requirements received from client
 *             to generate XML for response to server.
 * Throws: InternalException upon error
 */
void
AuthService::addCallbackRequirements(AuthContext &auth_ctx,
		BodyChunkList &bodyChunkList) 
{

    std::size_t i = 0;

    for(i = 0; i < auth_ctx.callbacks.size(); i++) {
	// iterate through callbacks, add to BodyChunk
	am_auth_callback_t &callback = auth_ctx.callbacks[i];
	switch(callback.callback_type) {
	case NameCallback:
	    // if name callback
	    {
	    am_auth_name_callback_t &name_cb =
		callback.callback_info.name_callback;
	    addNameCallbackRequirements(name_cb, bodyChunkList);
	    }
	    break;
	case PasswordCallback:
	    // if password callback
	    {
	    am_auth_password_callback_t &password_cb =
		callback.callback_info.password_callback;
	    addPasswordCallbackRequirements(password_cb, bodyChunkList);
	    }
	    break;
	case ChoiceCallback:
	    // if choice callback
	    {
	    am_auth_choice_callback_t &choice_cb =
		callback.callback_info.choice_callback;
	    addChoiceCallbackRequirements(choice_cb, bodyChunkList);
	    }
	    break;
	case ConfirmationCallback:
	    // if confirmation callback
	    {
	    am_auth_confirmation_callback_t &confirmation_cb =
		callback.callback_info.confirmation_callback;
	    addConfirmationCallbackRequirements(confirmation_cb, bodyChunkList);
	    }
	    break;
	case LanguageCallback:
	    // if language callback
	    {
	    am_auth_language_callback_t &language_cb =
		callback.callback_info.language_callback;
	    addLanguageCallbackRequirements(language_cb, bodyChunkList);
	    }
	    break;
	case TextInputCallback:
	    // if text input callback
	    {
	    am_auth_text_input_callback_t &text_input_cb =
		callback.callback_info.text_input_callback;
	    addTextInputCallbackRequirements(text_input_cb, bodyChunkList);
	    }
	    break;
	case TextOutputCallback:
	    // if text output callback
	    {
		// nothing to do
	    }
	    break;
	case HTTPCallback:
	    // if HTTP callback
	    {
	    am_auth_http_callback_t &http_cb =
		callback.callback_info.http_callback;
	    addHttpCallbackRequirements(http_cb, bodyChunkList);
	    }
	    break;
	case RedirectCallback:
	    // if Redirect callback
	    {
	    am_auth_confirmation_callback_t &confirmation_cb =
		callback.callback_info.confirmation_callback;
	    addConfirmationCallbackRequirements(confirmation_cb, bodyChunkList);
	    }
	    break;
	default:
	    break;
	}
    }

    return;
} // addCallbackRequirements


/*
 * addChoiceCallbackRequirements
 *             Add choice callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addChoiceCallbackRequirements(am_auth_choice_callback_t &choice_cb,
		BodyChunkList &bodyChunkList) {

    bodyChunkList.push_back(choiceCallbackPrefixChunk);

    // add "multiple selections allowed" attribute value
    if(choice_cb.allow_multiple_selections == B_TRUE) {
	bodyChunkList.push_back(BodyChunk("true", 4));
    } else {
	bodyChunkList.push_back(BodyChunk("false", 5));
    }
    bodyChunkList.push_back(quoteEndElementChunk);

    // add prompt value
    bodyChunkList.push_back(promptPrefixChunk);
    bodyChunkList.push_back(BodyChunk(choice_cb.prompt,
	strlen(choice_cb.prompt)));
    bodyChunkList.push_back(promptSuffixChunk);

    // add choice values
    bodyChunkList.push_back(choiceValuesPrefixChunk);
    std::size_t i = 0;
    for(i = 0; i < choice_cb.choices_size; i++) {
	// add default choice attribute value
    	if(choice_cb.default_choice == i) {
	    bodyChunkList.push_back(choiceValuePrefixChunk);
	    bodyChunkList.push_back(isDefaultAttributeChunk);
	} else {
	    bodyChunkList.push_back(choiceValuePrefixChunk);
	    bodyChunkList.push_back(endElementChunk);
	}
    	bodyChunkList.push_back(valuePrefixChunk);
    	// add choice value
    	bodyChunkList.push_back(BodyChunk(choice_cb.choices[i],
	    strlen(choice_cb.choices[i])));
    	bodyChunkList.push_back(valueSuffixChunk);
    	bodyChunkList.push_back(choiceValueSuffixChunk);
    }
    bodyChunkList.push_back(choiceValuesSuffixChunk);

    // add selected values
    bodyChunkList.push_back(selectedValuesPrefixChunk);
    std::size_t j = 0;
    for (j = 0; j < choice_cb.response_size; j++) {
	bodyChunkList.push_back(valuePrefixChunk);
	{
	    std::string selectedIndex(choice_cb.response[j]);
	    // Do entity Reference conversions
	    Utils::expandEntityRefs(selectedIndex);
	    bodyChunkList.push_back(BodyChunk(selectedIndex));
	}
	bodyChunkList.push_back(valueSuffixChunk);
    }
    bodyChunkList.push_back(selectedValuesSuffixChunk);

    bodyChunkList.push_back(choiceCallbackSuffixChunk);

return;
} // addChoiceCallbackRequirements


/*
 * addConfirmationCallbackRequirements
 *             Add confirmation callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addConfirmationCallbackRequirements(
		am_auth_confirmation_callback_t &confirmation_cb,
		BodyChunkList &bodyChunkList) {

    std::string selectedIndex(confirmation_cb.response);

    bodyChunkList.push_back(confirmationCallbackPrefixChunk);

    // add messageType attribute value
    if (confirmation_cb.message_type != NULL) {
	bodyChunkList.push_back(messageTypeAttributeChunk);
	bodyChunkList.push_back(BodyChunk(confirmation_cb.message_type,
	    strlen(confirmation_cb.message_type)));
	bodyChunkList.push_back(quoteChunk);
    }

    // add optionType attribute value
    if (confirmation_cb.option_type != NULL) {
	bodyChunkList.push_back(optionTypeAttributeChunk);
	bodyChunkList.push_back(BodyChunk(confirmation_cb.option_type,
	    strlen(confirmation_cb.option_type)));
	bodyChunkList.push_back(quoteChunk);
    }

    bodyChunkList.push_back(endElementChunk);

    // add option values
    bodyChunkList.push_back(optionValuesPrefixChunk);

    std::size_t i = 0;
    for (i = 0; i < confirmation_cb.options_size; i++) {
	bodyChunkList.push_back(optionValuePrefixChunk);
	bodyChunkList.push_back(valuePrefixChunk);
	bodyChunkList.push_back(BodyChunk(confirmation_cb.options[i],
	    strlen(confirmation_cb.options[i])));
	bodyChunkList.push_back(valueSuffixChunk);
	bodyChunkList.push_back(optionValueSuffixChunk);
    }
    confirmation_cb.options_size = i;

    bodyChunkList.push_back(optionValuesSuffixChunk);

    // add default option value
    if (confirmation_cb.default_option != NULL) {
	bodyChunkList.push_back(defaultOptionValuePrefixChunk);
	bodyChunkList.push_back(valuePrefixChunk);
	bodyChunkList.push_back(BodyChunk(confirmation_cb.default_option,
	    strlen(confirmation_cb.default_option)));
	bodyChunkList.push_back(valueSuffixChunk);
	bodyChunkList.push_back(defaultOptionValueSuffixChunk);
    }

    // Do entity Reference conversions
    Utils::expandEntityRefs(selectedIndex);

    // add selected value
    bodyChunkList.push_back(selectedValuePrefixChunk);
    bodyChunkList.push_back(valuePrefixChunk);
    bodyChunkList.push_back(BodyChunk(selectedIndex));
    bodyChunkList.push_back(valueSuffixChunk);
    bodyChunkList.push_back(selectedValueSuffixChunk);

    bodyChunkList.push_back(confirmationCallbackSuffixChunk);

    return;
} // addConfirmationCallbackRequirements


/*
 * addLanguageCallbackRequirements
 *             Add language callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addLanguageCallbackRequirements(
		am_auth_language_callback_t &language_cb,
		BodyChunkList &bodyChunkList) {

    bodyChunkList.push_back(languageCallbackPrefixChunk);

    // add language
    if((language_cb.response->language != NULL) &&
    (language_cb.response->language[0] != '\0')) {
	std::string language(language_cb.response->language);
	// Do entity Reference conversions
	Utils::expandEntityRefs(language);
	bodyChunkList.push_back(languageAttributeChunk);
	bodyChunkList.push_back(BodyChunk(language));
	bodyChunkList.push_back(quoteChunk);
    }
    // add country
    if((language_cb.response->country != NULL) &&
    (language_cb.response->country[0] != '\0')) {
	std::string country(language_cb.response->country);
	// Do entity Reference conversions
	Utils::expandEntityRefs(country);
	bodyChunkList.push_back(countryAttributeChunk);
	bodyChunkList.push_back(BodyChunk(country));
	bodyChunkList.push_back(quoteChunk);
    }
    // add variant
    if((language_cb.response->variant != NULL) &&
    (language_cb.response->variant[0] != '\0')) {
	std::string variant(language_cb.response->variant);
	// Do entity Reference conversions
	Utils::expandEntityRefs(variant);
	bodyChunkList.push_back(variantAttributeChunk);
	bodyChunkList.push_back(BodyChunk(variant));
	bodyChunkList.push_back(quoteChunk);
    }

    bodyChunkList.push_back(endElementChunk);

    bodyChunkList.push_back(languageCallbackSuffixChunk);

    return;
} // addLanguageCallbackRequirements


/*
 * addNameCallbackRequirements
 *             Add name callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addNameCallbackRequirements(am_auth_name_callback_t &name_cb,
		BodyChunkList &bodyChunkList) {

    std::string name(name_cb.response);

    bodyChunkList.push_back(nameCallbackPrefixChunk);

    // add prompt value
    bodyChunkList.push_back(promptPrefixChunk);
    bodyChunkList.push_back(BodyChunk(name_cb.prompt, strlen(name_cb.prompt)));
    bodyChunkList.push_back(promptSuffixChunk);

    // Do entity Reference conversions
    Utils::expandEntityRefs(name);

    // add name value
    bodyChunkList.push_back(valuePrefixChunk);
    bodyChunkList.push_back(BodyChunk(name));
    bodyChunkList.push_back(valueSuffixChunk);

    bodyChunkList.push_back(nameCallbackSuffixChunk);

    return;
} // addNameCallbackRequirements


/*
 * addPasswordCallbackRequirements
 *             Add password callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addPasswordCallbackRequirements(
		am_auth_password_callback_t &password_cb,
		BodyChunkList &bodyChunkList) {

    std::string password(password_cb.response);

    bodyChunkList.push_back(passwordCallbackPrefixChunk);

    // add "is echo on" value
    if(password_cb.echo_on == B_TRUE) {
	bodyChunkList.push_back(BodyChunk("true", 4));
	bodyChunkList.push_back(quoteEndElementChunk);
    } else {
	bodyChunkList.push_back(BodyChunk("false", 5));
	bodyChunkList.push_back(quoteEndElementChunk);
    }

    // add prompt value
    bodyChunkList.push_back(promptPrefixChunk);
    bodyChunkList.push_back(BodyChunk(password_cb.prompt,
	strlen(password_cb.prompt)));
    bodyChunkList.push_back(promptSuffixChunk);

    // Do entity Reference conversions
    Utils::expandEntityRefs(password);

    // add password value
    bodyChunkList.push_back(valuePrefixChunk);
    bodyChunkList.push_back(BodyChunk(password, true)); // hide
    bodyChunkList.push_back(valueSuffixChunk);

    bodyChunkList.push_back(passwordCallbackSuffixChunk);

    return;
} // addPasswordCallbackRequirements


/*
 * addTextInputCallbackRequirements
 *             Add text input callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addTextInputCallbackRequirements(
		am_auth_text_input_callback_t &text_input_cb,
		BodyChunkList &bodyChunkList) {

    std::string text(text_input_cb.response);

    bodyChunkList.push_back(textInputCallbackPrefixChunk);

    // add prompt value
    bodyChunkList.push_back(promptPrefixChunk);
    bodyChunkList.push_back(BodyChunk(text_input_cb.prompt,
	strlen(text_input_cb.prompt)));
    bodyChunkList.push_back(promptSuffixChunk);

    // Do entity Reference conversions
    Utils::expandEntityRefs(text);

    // add text input value
    bodyChunkList.push_back(valuePrefixChunk);
    bodyChunkList.push_back(BodyChunk(text));
    bodyChunkList.push_back(valueSuffixChunk);

    bodyChunkList.push_back(textInputCallbackSuffixChunk);

    return;
} // addTextInputCallbackRequirements


/*
 * addHTTPCallbackRequirements
 *             Add name callback requirements received from client
 *             to XML server response being generated.
 */



void
AuthService::addHttpCallbackRequirements(am_auth_http_callback_t &http_cb,
		BodyChunkList &bodyChunkList) {

    std::string text(http_cb.authToken);

    bodyChunkList.push_back(httpCallbackPrefixChunk);

    // add Authorization Header
    bodyChunkList.push_back(httpHeaderPrefixChunk);
    bodyChunkList.push_back(BodyChunk(http_cb.tokenHeader, strlen(http_cb.tokenHeader)));
    bodyChunkList.push_back(httpHeaderSuffixChunk);

    // add Negotiation Header
    bodyChunkList.push_back(httpNegoPrefixChunk);
    bodyChunkList.push_back(BodyChunk(http_cb.negoHeader, strlen(http_cb.negoHeader)));
    bodyChunkList.push_back(BodyChunk(":",1));
    bodyChunkList.push_back(BodyChunk(http_cb.negoValue, strlen(http_cb.negoValue)));
    bodyChunkList.push_back(httpNegoSuffixChunk);

    // add Code Header
    bodyChunkList.push_back(httpErrorCodePrefixChunk);
    bodyChunkList.push_back(BodyChunk(http_cb.negoErrorCode, strlen(http_cb.negoErrorCode)));
    bodyChunkList.push_back(httpErrorCodeSuffixChunk);


    // Do entity Reference conversions
    Utils::expandEntityRefs(text);

    // add name value
    bodyChunkList.push_back(httpTokenPrefixChunk);
    bodyChunkList.push_back(BodyChunk(text));
    bodyChunkList.push_back(httpTokenSuffixChunk);

    bodyChunkList.push_back(httpCallbackSuffixChunk);

    return;
} // addNameCallbackRequirements


/*
 * addRedirectCallbackRequirements
 *             Add Redirect callback requirements received from client
 *             to XML server response being generated.
 */
void
AuthService::addRedirectCallbackRequirements(am_auth_redirect_callback_t &redirect_cb,
		BodyChunkList &bodyChunkList) {


    return;
} // addRedirectCallbackRequirements


/*
 * create_auth_context_cac
 * Throws: InternalException upon error
 */
void
AuthService::create_auth_context_cac(AuthContext &auth_ctx)
{

    am_status_t status;

    Request request(*this, xmlRequestPrefixChunk, authContextPrefixChunk,
                    10, false);

    Http::Response response;

    BodyChunkList &bodyChunkList = request.getBodyChunkList();
    BodyChunk firstAuthIdentifier("0", 1);

    if(auth_ctx.orgName.size() == 0) {
        auth_ctx.orgName = orgName; // no org passed in so use property
    }

    // Do entity Reference conversions
    Utils::expandEntityRefs(auth_ctx.orgName);

    if(auth_ctx.orgName.size() == 0) {
        Log::log(logID, Log::LOG_ERROR,
                "AuthService::create_auth_context_cac() "
                "No org name specified in properties file or input parameter.");
        throw InternalException("AuthService::create_auth_context_cac() ",
                "No org name specified in properties file or input parameter.",
                AM_AUTH_FAILURE);
    }

    if(auth_ctx.namingURL.size() == 0) {
        auth_ctx.namingURL = namingURL; // no url passed in so use property
    }

    // Do entity Reference conversions
    Utils::expandEntityRefs(auth_ctx.namingURL);

    if(auth_ctx.namingURL.size() == 0) {
        Log::log(logID, Log::LOG_ERROR,
                "AuthService::create_auth_context_cac() "
                "No Naming URL specified in properties file or "
                "input parameter.");
        throw InternalException("AuthService::create_auth_context_cac() ",
                "No Naming URL specified in properties file or "
                "input parameter.",
                AM_AUTH_FAILURE);
    }

    BodyChunk orgNameBodyChunk(auth_ctx.orgName);
    bodyChunkList.push_back(firstAuthIdentifier);
    bodyChunkList.push_back(newAuthContextPrefixChunk);
    bodyChunkList.push_back(orgNameBodyChunk);
    bodyChunkList.push_back(newAuthContextSuffixChunk);
    bodyChunkList.push_back(authContextSuffixChunk);
    bodyChunkList.push_back(xmlRequestSuffixChunk);
    status = doHttpPost(auth_ctx.authSvcInfo, std::string(), Http::CookieList(),
                        bodyChunkList, response);
    if(status != AM_SUCCESS) {
        throw InternalException("AuthService::create_auth_context_cac()",
                "Error sending request for authentication context from server.",
                status);
    }

    std::vector<std::string> authCtxResponses;
    authCtxResponses = parseGenericResponse(response,
                                            request.getGlobalId());

    if(authCtxResponses.empty()) {
        throw InternalException("AuthService::create_auth_context_cac()",
                                "Received empty response set from server.",
                                AM_AUTH_CTX_INIT_FAILURE);
    }

    Log::log(logID, Log::LOG_MAX_DEBUG, authCtxResponses[0].c_str());
    XMLTree::Init xt;
    XMLTree authCtxTree(false, authCtxResponses[0].c_str(),
                        authCtxResponses[0].size());

    XMLElement rootElem = authCtxTree.getRootElement();
    processResponse(auth_ctx, rootElem);
    cookieList = response.getCookieList();

    return;
} // create_auth_context_cac

