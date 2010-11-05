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
 * $Id: auth_context.cpp,v 1.3 2008/06/25 08:14:31 qcheng Exp $
 *
 */ 

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

#include "auth_context.h"
#include "log.h"
#include "am_sso.h"

USING_PRIVATE_NAMESPACE

#define AUTH_CONTEXT_MODULE "AuthService"

/*
 * cleanupCallbacks
 *             Clean up memory allocated for callback data.
 */
void
AuthContext::cleanupCallbacks() {

    for(std::size_t i=0; i < callbacks.size(); i++) {
	am_auth_callback_t &callback = callbacks[i];
	switch(callback.callback_type) {
	case ChoiceCallback:
	    {
		// clean prompt
		cleanupCharArray (
		    callback.callback_info.choice_callback.prompt);

		// clean choices
		cleanupStringList (
		    callback.callback_info.choice_callback.choices,
		    callback.callback_info.choice_callback.choices_size);
	    }
            break;
	case ConfirmationCallback:
	    {
		// clean prompt
		cleanupCharArray (
		    callback.callback_info.confirmation_callback.prompt);

		// clean message type
		cleanupCharArray (
		    callback.callback_info.confirmation_callback.message_type);

		// clean option type
		cleanupCharArray (
		    callback.callback_info.confirmation_callback.option_type);

		// clean options
		cleanupStringList (
		    callback.callback_info.confirmation_callback.options,
		    callback.callback_info.confirmation_callback.options_size);

		// clean default option
		cleanupCharArray (
		    callback.callback_info.confirmation_callback.default_option);

	    }
	    break;
	case LanguageCallback:
	    {
		// clean locale
		am_auth_locale_t* &locale =
		    callback.callback_info.language_callback.locale;
		if (locale != NULL) {
		    if (locale->language != NULL) {
			free ((void *) locale->language);
		    }
		    if (locale->country != NULL) {
			free ((void *) locale->country);
		    }
		    if (locale->variant != NULL) {
			free ((void *) locale->variant);
		    }
		    free ((void *) locale);
		    callback.callback_info.language_callback.locale = NULL;
		}
	    }
	    break;
	case NameCallback:
	    {
		// clean prompt
		cleanupCharArray (
		    callback.callback_info.name_callback.prompt);

		// clean default value
		cleanupCharArray (
		    callback.callback_info.name_callback.default_name);
	    }
	    break;
	case PasswordCallback:
	    {
		// clean prompt
		cleanupCharArray (
		    callback.callback_info.password_callback.prompt);
	    }
	    break;
	case HTTPCallback:
	    {
		// clean prompt  Insert cleanup for HTTP
		cleanupCharArray (
		    callback.callback_info.http_callback.tokenHeader);
		cleanupCharArray (
		    callback.callback_info.http_callback.negoHeader);
		cleanupCharArray (
		    callback.callback_info.http_callback.negoValue);
		cleanupCharArray (
		    callback.callback_info.http_callback.negoErrorCode);

	    }
	    break;
	case RedirectCallback:
	    {
		// clean prompt Insert cleanup for Redirect

	    }
	    break;
	case TextInputCallback:
	    {
		// clean prompt
		cleanupCharArray (
		    callback.callback_info.text_input_callback.prompt);

		// clean default value
		cleanupCharArray (
		    callback.callback_info.text_input_callback.default_text);
	    }
	    break;
	case TextOutputCallback:
	    {
		// clean message
		cleanupCharArray (
		    callback.callback_info.text_output_callback.message);

		// clean message type
		cleanupCharArray (
		    callback.callback_info.text_output_callback.message_type);
	    }
	    break;
	default:
	    break;
	}
    }
    callbacks.resize(0);
    return;
}


/*
 * cleanupCharArray
 *             Clean up memory allocated for a character array.
 */
void
AuthContext::cleanupCharArray(const char* &char_array) {

    if (char_array != NULL) {
	free ((void *) char_array);
	char_array = NULL;
    }

    return;
}


/*
 * cleanupStringList
 *             Clean up memory allocated for a list of strings.
 */
void
AuthContext::cleanupStringList(const char** &string_list, size_t list_size) {

    if (string_list != NULL) {
	std::size_t i;
	for (i = 0; i < list_size; i++) {
	    cleanupCharArray(string_list[i]);
	}
	free ((void **) string_list);
	string_list = NULL;
    }

    return;
}

