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
 * $Id: AuthXMLTags.java,v 1.11 2009/06/19 17:54:14 ericow Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.authentication.share;

public interface AuthXMLTags {    
    // Environment attributes
    public static final String AUTH_SERVICE = "auth";
    public static final String SERVER_PASS_FILE =
        "com.iplanet.am.admin.cli.certdb.passfile";
    public static final String SERVER_DEPLOY_URI = 
        "com.iplanet.am.services.deploymentDescriptor";

    /**
     * XML elements and attributes
     */
    public static final String REQUEST = "Request";
    public static final String RESPONSE = "Response";
    public static final String AUTH_ID_HANDLE = "authIdentifier";
    public static final String LOGIN_STATUS = "LoginStatus";
    public static final String EXCEPTION = "Exception";
    public static final String QUERY_INFO = "QueryInformation";
    public static final String QUERY_RESULT = "QueryResult";
    public static final String LOGIN = "Login";
    public static final String LOGOUT = "Logout";
    public static final String ABORT = "Abort";
    public static final String SUBMIT_REQ = "SubmitRequirements";
    public static final String GET_REQ = "GetRequirements";
    public static final String INDEX_TYPE_PAIR = "IndexTypeNamePair";
    public static final String INDEX_NAME = "IndexName";
    public static final String INDEX_TYPE = "indexType";
    public static final String LOCALE = "Locale";
    public static final String SUBJECT = "Subject";
    public static final String PASSWORD = "Password";
    public static final String PRINCIPAL = "Principal";
    public static final String CALLBACKS = "Callbacks";
    public static final String NAME_CALLBACK = "NameCallback";
    public static final String PASSWORD_CALLBACK = "PasswordCallback";
    public static final String CHOICE_CALLBACK = "ChoiceCallback";
    public static final String CONFIRMATION_CALLBACK = "ConfirmationCallback";
    public static final String TEXT_INPUT_CALLBACK = "TextInputCallback";
    public static final String TEXT_OUTPUT_CALLBACK = "TextOutputCallback";
    public static final String PAGE_PROPERTIES_CALLBACK =
        "PagePropertiesCallback";
    public static final String LANGUAGE_CALLBACK = "LanguageCallback";
    public static final String SAML_CALLBACK = "SAMLCallback";
    public static final String X509CERTIFICATE_CALLBACK =
        "X509CertificateCallback";
    public static final String CUSTOM_CALLBACK = "CustomCallback";
    public static final String PROMPT = "Prompt";
    public static final String ECHO_PASSWORD = "echoPassword";
    public static final String MULTI_SELECT_ALLOWED =
        "multipleSelectionsAllowed";
    public static final String VALUE = "Value";
    public static final String SELECTED_VALUE = "SelectedValue";
    public static final String DEFAULT_OPTION_VALUE = "DefaultOptionValue";
    public static final String DEFAULT_VALUE = "DefaultValue";
    public static final String CHOICE_VALUE = "ChoiceValue";
    public static final String CHOICE_VALUES = "ChoiceValues";
    public static final String OPTION_VALUES = "OptionValues";
    public static final String SELECTED_VALUES = "SelectedValues";
    public static final String ORG_NAME_ELEMENT = "OrganizationName";
    public static final String ORG_NAME_ATTR = "orgName";
    public static final String HOST_NAME_ATTR = "hostName";
    public static final String FORCE_AUTH_ATTR = "forceAuth";
    public static final String REQUESTED_INFO = "requestedInformation";
    public static final String MODULE_INSTANCE = "moduleInstanceNames";
    public static final String STATUS = "status";
    public static final String SSOTOKEN = "ssoToken";
    public static final String MESSAGE = "message";
    public static final String TOKEN_ID = "tokenId";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_STATE = "isErrorState";
    public static final String TEMPLATE_NAME = "templateName";
    public static final String LENGTH = "length";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String OPTION_TYPE = "optionType";
    public static final String IS_DEFAULT = "isDefault";
    public static final String IS_PCOOKIE = "isPCookie";
    public static final String INDEX_TYPE_SVC_ATTR = "service";
    public static final String INDEX_TYPE_ROLE_ATTR = "role";
    public static final String INDEX_TYPE_MODULE_ATTR = "moduleInstance";
    public static final String INDEX_TYPE_LEVEL_ATTR = "authLevel";
    public static final String INDEX_TYPE_USER_ATTR = "user";
    public static final String INDEX_TYPE_RESOURCE = "resource";
    public static final String INDEX_TYPE_COMPOSITE_ADVICE_ATTR =
        "compositeAdvice";
    public static final String SUCCESS_URL="successURL";
    public static final String FAILURE_URL="failureURL";
    public static final String EQUAL= "=";
    public static final String SPACE= " ";
    public static final String QUOTE = "\"";
    public static final String HTTP_CALLBACK = "HttpCallback";
    public static final String HTTP_CALLBACK_BEGIN = "<HttpCallback>";
    public static final String HTTP_CALLBACK_END = "</HttpCallback>";
    public static final String HTTP_HEADER = "HttpHeader";
    public static final String HTTP_HEADER_BEGIN = "<HttpHeader>";
    public static final String HTTP_HEADER_END = "</HttpHeader>";
    public static final String HTTP_NEGO = "Negotiation";
    public static final String HTTP_NEGO_BEGIN = "<Negotiation>";
    public static final String HTTP_NEGO_END = "</Negotiation>";
    public static final String HTTP_CODE = "HttpErrorCode";
    public static final String HTTP_CODE_BEGIN = "<HttpErrorCode>";
    public static final String HTTP_CODE_END = "</HttpErrorCode>";
    public static final String HTTP_TOKEN = "HttpToken";
    public static final String HTTP_TOKEN_BEGIN = "<HttpToken>";
    public static final String HTTP_TOKEN_END = "</HttpToken>";
    public static final String SIGN_REQUIRED = "SignatureRequired";
    public static final String X509CERTIFICATE = "X509Certificate";

    public static final String REDIRECT_CALLBACK = "RedirectCallback";
    public static final String REDIRECT_URL = "RedirectUrl";
    public static final String REDIRECT_DATA = "RedirectData";
    public static final String REDIRECT_STATUS = "Status";
    public static final String REDIRECT_STATUS_PARAM = "RedirectStatusParam";
    public static final String REDIRECT_BACK_URL_COOKIE =
        "RedirectBackUrlCookie";
    public static final String REDIRECT_METHOD = "method";
    public static final String REDIRECT_NAME = "Name";
    public static final String REDIRECT_VALUE = "Value";
    public static final String REDIRECT_CALLBACK_BEGIN = "<RedirectCallback";
    public static final String REDIRECT_CALLBACK_END = "</RedirectCallback>";
    public static final String REDIRECT_URL_BEGIN = "<RedirectUrl>";
    public static final String REDIRECT_URL_END = "</RedirectUrl>";
    public static final String REDIRECT_DATA_BEGIN = "<RedirectData>";
    public static final String REDIRECT_DATA_END = "</RedirectData>";
    public static final String REDIRECT_STATUS_BEGIN = "<Status>";
    public static final String REDIRECT_STATUS_END = "</Status>";
    public static final String REDIRECT_STATUS_PARAM_BEGIN =
        "<RedirectStatusParam>";
    public static final String REDIRECT_STATUS_PARAM_END =
        "</RedirectStatusParam>";
    public static final String REDIRECT_BACK_URL_COOKIE_BEGIN =
        "<RedirectBackUrlCookie>";
    public static final String REDIRECT_BACK_URL_COOKIE_END =
        "</RedirectBackUrlCookie>";
    public static final String REDIRECT_NAME_BEGIN = "<Name>";
    public static final String REDIRECT_NAME_END = "</Name>";
    public static final String REDIRECT_VALUE_BEGIN = "<Value>";
    public static final String REDIRECT_VALUE_END = "</Value>";
    public static final String REMOTE_REQUEST_RESPONSE = "RemoteRequestResponse";
    public static final String HTTP_SERVLET_REQUEST = "HttpServletRequest";
    public static final String HTTP_SERVLET_RESPONSE = "HttpServletResponse";
    
    /**
     * Predefined xml tags and statements.
     */
    public static final String XML_REQUEST_PREFIX =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<AuthContext version=\"1.0\"><Request authIdentifier=\"{0}\">";
    public static final String QUERY_INFO_BEGIN = "<QueryInformation";
    public static final String QUERY_INFO_END = "</QueryInformation>";
    public static final String NEW_AUTHCONTEXT_BEGIN = "<NewAuthContext";
    public static final String NEW_AUTHCONTEXT_END = "</NewAuthContext>";
    public static final String LOGIN_BEGIN = "<Login";
    public static final String LOGIN_END = "</Login>";
    public static final String LOGOUT_BEGIN = "<Logout>";
    public static final String LOGOUT_END = "</Logout>";
    public static final String APPSSOTOKEN_BEGIN = "<AppSSOToken>";
    public static final String APPSSOTOKEN_END = "</AppSSOToken>";
    public static final String ABORT_BEGIN = "<Abort>";
    public static final String ABORT_END = "</Abort>";
    //public static final String ORG_NAME_BEGIN = "<OrganizationName>";
    //public static final String ORG_NAME_END = "</OrganizationName>";
    public static final String INDEX_TYPE_PAIR_BEGIN = "<IndexTypeNamePair";
    public static final String INDEX_TYPE_PAIR_END = "</IndexTypeNamePair>";
    public static final String INDEX_NAME_BEGIN = "<IndexName>";
    public static final String INDEX_NAME_END = "</IndexName>";
    public static final String GET_REQS_BEGIN = "<GetRequirements>";
    public static final String GET_REQS_END = "</GetRequirements>";
    public static final String SUBMIT_REQS_BEGIN = "<SubmitRequirements>";
    public static final String SUBMIT_REQS_END = "</SubmitRequirements>";
    public static final String CALLBACKS_BEGIN = "<Callbacks";
    public static final String CALLBACKS_END = "</Callbacks>";
    public static final String SUBJECT_BEGIN = "<Subject>";
    public static final String SUBJECT_END = "</Subject>";
    public static final String PARAMS_BEGIN = "<Params>";
    public static final String PARAMS_END = "</Params>";
    public static final String PARAMS = "Params";
    public static final String ENV_BEGIN = "<Environment>";
    public static final String ENV_END = "</Environment>";
    public static final String ENVIRONMENT = "Environment";
    public static final String ENV_AV_BEGIN = "<EnvValue>";
    public static final String ENV_AV_END = "</EnvValue>";
    public static final String ENV_VALUE = "EnvValue";
    public static final String ELEMENT_END = ">";
    public static final String XML_REQUEST_SUFFIX =
        "</Request></AuthContext>";
    
    public static final String XML_RESPONSE_PREFIX =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<AuthContext version=\"1.0\"><Response authIdentifier=\"{0}\">";
    public static final String XML_RESPONSE_SUFFIX =
        "</Response></AuthContext>";
    public static final String EXCEPTION_BEGIN="<Exception";
    public static final String EXCEPTION_END="</Exception>";
    public static final String LOGIN_STATUS_BEGIN="<LoginStatus";
    public static final String LOGIN_STATUS_END="</LoginStatus>";
    public static final String ELEMENT_CLOSE=">";
    public static final String NAME_CALLBACK_BEGIN="<NameCallback>";
    public static final String NAME_CALLBACK_END="</NameCallback>";
    public static final String PROMPT_BEGIN = "<Prompt>";
    public static final String PROMPT_END = "</Prompt>";
    public static final String VALUE_BEGIN = "<Value>";
    public static final String VALUE_END = "</Value>";
    public static final String PASSWORD_CALLBACK_BEGIN="<PasswordCallback";
    public static final String PASSWORD_CALLBACK_END="</PasswordCallback>";
    public static final String CERT_CALLBACK_BEGIN="<X509CertificateCallback";
    public static final String CERT_CALLBACK_END="</X509CertificateCallback>";
    public static final String CHOICE_CALLBACK_BEGIN="<ChoiceCallback";
    public static final String CHOICE_CALLBACK_END="</ChoiceCallback>";
    public static final String CHOICE_VALUES_BEGIN="<ChoiceValues>";
    public static final String CHOICE_VALUES_END="</ChoiceValues>";
    public static final String CHOICE_VALUE_BEGIN="<ChoiceValue";
    public static final String CHOICE_VALUE_END ="</ChoiceValue>";
    public static final String SELECTED_VALUES_BEGIN="<SelectedValues>";
    public static final String SELECTED_VALUES_END="</SelectedValues>";
    public static final String CONFIRMATION_CALLBACK_BEGIN =
        "<ConfirmationCallback";
    public static final String CONFIRMATION_CALLBACK_END =
        "</ConfirmationCallback>";
    public static final String CUSTOM_CALLBACK_BEGIN ="<CustomCallback";
    public static final String CUSTOM_CALLBACK_END ="</CustomCallback>";
    public static final String OPTION_VALUES_BEGIN ="<OptionValues>";
    public static final String OPTION_VALUES_END   ="</OptionValues>";
    public static final String OPTION_VALUE_BEGIN ="<OptionValue>";
    public static final String OPTION_VALUE_END ="</OptionValue>";
    public static final String DEFAULT_OPTION_VALUE_BEGIN =
        "<DefaultOptionValue>";
    public static final String DEFAULT_OPTION_VALUE_END="</DefaultOptionValue>";
    public static final String DEFAULT_VALUE_BEGIN ="<DefaultValue>";
    public static final String DEFAULT_VALUE_END="</DefaultValue>";
    public static final String SELECTED_VALUE_BEGIN="<SelectedValue>";
    public static final String SELECTED_VALUE_END="</SelectedValue>";
    public static final String TEXTINPUT_CALLBACK_BEGIN="<TextInputCallback>";
    public static final String TEXTINPUT_CALLBACK_END="</TextInputCallback>";
    public static final String TEXTOUTPUT_CALLBACK_BEGIN="<TextOutputCallback";
    public static final String TEXTOUTPUT_CALLBACK_END="</TextOutputCallback>";
    public static final String PAGEP_CALLBACK_BEGIN="<PagePropertiesCallback";
    public static final String PAGEP_CALLBACK_END="</PagePropertiesCallback>";
    public static final String LANGUAGE_CALLBACK_BEGIN="<LanguageCallback>";
    public static final String LANGUAGE_CALLBACK_END="</LanguageCallback>";
    public static final String LOCALE_BEGIN="<Locale>";
    public static final String LOCALE_END ="</Locale>";

    public static final String MODULE_NAME_BEGIN="<ModuleName>";
    public static final String MODULE_NAME_END="</ModuleName>";
    public static final String HEADER_VALUE_BEGIN="<HeaderValue>";
    public static final String HEADER_VALUE_END  ="</HeaderValue>";
    public static final String IMAGE_NAME_BEGIN="<ImageName>";
    public static final String IMAGE_NAME_END="</ImageName>";
    public static final String REQUIRED_LIST_BEGIN="<RequiredList>";
    public static final String REQUIRED_LIST_END="</RequiredList>";
    public static final String ATTRIBUTE_LIST_BEGIN="<AttributeList>";
    public static final String ATTRIBUTE_LIST_END="</AttributeList>";
    public static final String INFOTEXT_LIST_BEGIN="<InfoTextList>";
    public static final String INFOTEXT_LIST_END="</InfoTextList>";
    public static final String PAGE_TIMEOUT_BEGIN="<PageTimeOutValue>";
    public static final String PAGE_TIMEOUT_END  ="</PageTimeOutValue>";
    public static final String TEMPLATE_NAME_BEGIN="<TemplateName>";
    public static final String TEMPLATE_NAME_END ="</TemplateName>";
    public static final String PAGE_STATE_BEGIN ="<PageState>";
    public static final String PAGE_STATE_END ="</PageState>";
    public static final String QUERY_RESULT_BEGIN = "<QueryResult";
    public static final String QUERY_RESULT_END = "</QueryResult>";
    public static final String X509CERTIFICATE_BEGIN = "<X509Certificate>";
    public static final String X509CERTIFICATE_END = "</X509Certificate>";

    public static final String ATTRIBUTE_VALUE_PAIR = "AttributeValuePair";
    public static final String ATTRIBUTE_VALUE_PAIR_BEGIN =
        "<AttributeValuePair>";
    public static final String ATTRIBUTE_VALUE_PAIR_END =
        "</AttributeValuePair>";
    public static final String ATTRIBUTE="Attribute";
    public static final String ATTRIBUTE_BEGIN="<Attribute";
    public static final String ATTRIBUTE_END="</Attribute>";
    public static final String ATTRIBUTE_NAME="name";
    public static final String ATTRIBUTE_CLASS_NAME="className";
    public static final String ATTRIBUTE_LANG="language";
    public static final String ATTRIBUTE_COUNTRY="country";
    public static final String ATTRIBUTE_VARIANT="variant";

    public static final String HTTP_SERVLET_REQUEST_START ="<HttpServletRequest>";
    public static final String HTTP_SERVLET_REQUEST_END ="</HttpServletRequest>";
    public static final String HTTP_SERVLET_RESPONSE_START ="<HttpServletResponse>";
    public static final String HTTP_SERVLET_RESPONSE_END ="</HttpServletResponse>";
    public static final String REMOTE_REQUEST_RESPONSE_START = "<RemoteRequestResponse>";
    public static final String REMOTE_REQUEST_RESPONSE_END = "</RemoteRequestResponse>";
}
