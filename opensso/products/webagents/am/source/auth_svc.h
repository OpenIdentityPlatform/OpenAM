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
 * $Id: auth_svc.h,v 1.4 2008/06/25 08:14:31 qcheng Exp $
 *
 */ 

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#ifndef __AUTH_SVC_H__
#define __AUTH_SVC_H__
#include <string>
#include <vector>
#include <stdexcept>
#include <am_auth.h>
#include "internal_macros.h"
#include "internal_exception.h"
#include "service_info.h"
#include "base_service.h"
#include "auth_context.h"
#include "xml_element.h"

BEGIN_PRIVATE_NAMESPACE
void auth_cleanup(); 
#define AUTH_CTX_NULL static_cast<AuthContext *>(NULL)


class AuthService: public BaseService {
 public:
    /* all functions throw InternalException upon error. */

    AuthService(const Properties &config); 
    void getModuleInstanceNames(AuthContext &, am_string_set_t**); 

    void create_auth_context(AuthContext &); 
    void create_auth_context_cac(AuthContext &); 

    void login(AuthContext &,
	       am_auth_index_t index_type,
	       const std::string &index_name); 

    void logout(AuthContext &); 

    void abort(AuthContext &); 

    inline am_auth_status_t getStatus(AuthContext &auth_ctx) {
	return auth_ctx.authStatus;
    }

    inline std::vector<am_auth_callback_t> &getRequirements(AuthContext &auth_ctx) {
	return auth_ctx.callbacks;
    }

    inline const std::size_t getNumRequirements(AuthContext &auth_ctx) {
	return auth_ctx.callbacks.size();
    }

    inline bool hasMoreRequirements(AuthContext &auth_ctx) {
	return auth_ctx.callbacks.size() != 0;
    }

    inline const std::string &getOrganizationName(AuthContext &auth_ctx) {
	return auth_ctx.orgName;
    }

    inline const std::string &getSSOToken(AuthContext &auth_ctx) {
	    return auth_ctx.ssoToken;
    }

    void submitRequirements(AuthContext &auth_ctx);

    virtual ~AuthService() {}

 private:
    friend void PRIVATE_NAMESPACE_NAME::auth_cleanup(); 
    Log::ModuleId logID;
    std::string orgName; // read from properties
    std::string namingURL; // read from properties
    Http::CookieList cookieList; // read from auth context requests

    static const BodyChunk xmlRequestPrefixChunk;
    static const BodyChunk xmlRequestSuffixChunk;
    static const BodyChunk authContextPrefixChunk;
    static const BodyChunk authContextSuffixChunk;

    static const BodyChunk newAuthContextPrefixChunk;
    static const BodyChunk newAuthContextSuffixChunk;
    static const BodyChunk loginPrefixChunk;
    static const BodyChunk loginSuffixChunk;
    static const BodyChunk indexTypeNamePairPrefixChunk;
    static const BodyChunk indexTypeNamePairSuffixChunk;
    static const BodyChunk indexNamePrefixChunk;
    static const BodyChunk indexNameSuffixChunk;
    static const BodyChunk submitRequirementsPrefixChunk;
    static const BodyChunk submitRequirementsSuffixChunk;
    static const BodyChunk callbacksPrefixChunk;
    static const BodyChunk callbacksSuffixChunk;
    static const BodyChunk choiceCallbackPrefixChunk;
    static const BodyChunk choiceCallbackSuffixChunk;
    static const BodyChunk confirmationCallbackPrefixChunk;
    static const BodyChunk confirmationCallbackSuffixChunk;
    static const BodyChunk languageCallbackPrefixChunk;
    static const BodyChunk languageCallbackSuffixChunk;
    static const BodyChunk nameCallbackPrefixChunk;
    static const BodyChunk nameCallbackSuffixChunk;
    static const BodyChunk passwordCallbackPrefixChunk;
    static const BodyChunk passwordCallbackSuffixChunk;
    static const BodyChunk textInputCallbackPrefixChunk;
    static const BodyChunk textInputCallbackSuffixChunk;

    static const BodyChunk httpCallbackPrefixChunk;
    static const BodyChunk httpCallbackSuffixChunk;


    static const BodyChunk localePrefixChunk;
    static const BodyChunk localeSuffixChunk;
    static const BodyChunk promptPrefixChunk;
    static const BodyChunk promptSuffixChunk;
    static const BodyChunk valuePrefixChunk;
    static const BodyChunk valueSuffixChunk;
    static const BodyChunk moduleInstanceNamesPrefixChunk;
    static const BodyChunk moduleInstanceNamesSuffixChunk;
    static const BodyChunk logoutChunk;
    static const BodyChunk abortChunk;
    static const BodyChunk choiceValuesPrefixChunk;
    static const BodyChunk choiceValuesSuffixChunk;
    static const BodyChunk choiceValuePrefixChunk;
    static const BodyChunk choiceValueSuffixChunk;
    static const BodyChunk selectedValuesPrefixChunk;
    static const BodyChunk selectedValuesSuffixChunk;
    static const BodyChunk optionValuesPrefixChunk;
    static const BodyChunk optionValuesSuffixChunk;
    static const BodyChunk optionValuePrefixChunk;
    static const BodyChunk optionValueSuffixChunk;
    static const BodyChunk defaultOptionValuePrefixChunk;
    static const BodyChunk defaultOptionValueSuffixChunk;
    static const BodyChunk selectedValuePrefixChunk;
    static const BodyChunk selectedValueSuffixChunk;
    static const BodyChunk isDefaultAttributeChunk;
    static const BodyChunk messageTypeAttributeChunk;
    static const BodyChunk optionTypeAttributeChunk;
    static const BodyChunk languageAttributeChunk;
    static const BodyChunk countryAttributeChunk;
    static const BodyChunk variantAttributeChunk;
    static const BodyChunk quoteChunk;
    static const BodyChunk endElementChunk;
    static const BodyChunk quoteEndElementChunk;

    static const BodyChunk httpHeaderPrefixChunk;

    static const BodyChunk httpHeaderSuffixChunk;
    static const BodyChunk httpNegoPrefixChunk;
    static const BodyChunk httpNegoSuffixChunk;
    static const BodyChunk httpErrorCodePrefixChunk;
    static const BodyChunk httpErrorCodeSuffixChunk;
    static const BodyChunk httpTokenPrefixChunk;
    static const BodyChunk httpTokenSuffixChunk;

    void setAuthSvcInfo(AuthContext &); 

    void processResponse(AuthContext &,
				const XMLElement &); 

    void processLoginStatus(AuthContext &,
				const XMLElement &);

    void logAndThrowException(const std::string& method, 
                                const XMLElement &) ; 

    void processGetRequirements(AuthContext &,
				const XMLElement &); 

    XMLElement validateResponse(const XMLElement &); 

    void processIndividualCallbacks(AuthContext &,
				const XMLElement &); 

    void processChoiceCallback(am_auth_choice_callback_t &,
				const XMLElement &); 

    void processConfirmationCallback(am_auth_confirmation_callback_t &,
				const XMLElement &); 

    void processHTTPCallback(am_auth_http_callback_t &,
				const XMLElement &);
    
    void processRedirectCallback(am_auth_redirect_callback_t &,
				const XMLElement &);

    void processLanguageCallback(am_auth_language_callback_t &,
				const XMLElement &); 

    void processNameCallback(am_auth_name_callback_t &,
				const XMLElement &); 

    void processPasswordCallback(am_auth_password_callback_t &,
				const XMLElement &); 

    void processTextInputCallback(am_auth_text_input_callback_t &,
				const XMLElement &); 

    void processTextOutputCallback(am_auth_text_output_callback_t &,
				const XMLElement &); 
    void processGetModuleInstanceNames(AuthContext &,
       const XMLElement &, am_string_set_t**); 
    const std::size_t validateAndCountRequirements(AuthContext &);

    void addCallbackRequirements(AuthContext &, BodyChunkList &); 

    void addChoiceCallbackRequirements(am_auth_choice_callback_t &,
				BodyChunkList &);

    void addConfirmationCallbackRequirements(am_auth_confirmation_callback_t &,
				BodyChunkList &);

    void addLanguageCallbackRequirements(am_auth_language_callback_t &,
				BodyChunkList &);

    void addNameCallbackRequirements(am_auth_name_callback_t &,
				BodyChunkList &);

    void addPasswordCallbackRequirements(am_auth_password_callback_t &,
				BodyChunkList &);

    void addTextInputCallbackRequirements(am_auth_text_input_callback_t &,
				BodyChunkList &);
    void addHttpCallbackRequirements(am_auth_http_callback_t &,
				BodyChunkList &);
    void addRedirectCallbackRequirements(am_auth_redirect_callback_t &,
				BodyChunkList &);


};

END_PRIVATE_NAMESPACE
#endif
