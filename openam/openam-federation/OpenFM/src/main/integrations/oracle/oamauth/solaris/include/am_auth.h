/* -*- Mode: C -*- */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: am_auth.h,v 1.2 2008/06/25 05:48:54 qcheng Exp $
 */

#ifndef __AM_AUTH_H__
#define __AM_AUTH_H__

#include <stdlib.h>
#include <am.h>
#include <am_properties.h>
#include <am_string_set.h>

AM_BEGIN_EXTERN_C

typedef struct am_auth_context *am_auth_context_t;

/*
 * Different types of authentication parameters.
 */
typedef enum am_auth_idx {
    AM_AUTH_INDEX_AUTH_LEVEL = 0,
    AM_AUTH_INDEX_ROLE,
    AM_AUTH_INDEX_USER,
    AM_AUTH_INDEX_MODULE_INSTANCE,
    AM_AUTH_INDEX_SERVICE
} am_auth_index_t;

/*
 * Enumeration of authentication statuses.
 */
typedef enum am_auth_status {
    AM_AUTH_STATUS_SUCCESS = 0,
    AM_AUTH_STATUS_FAILED,
    AM_AUTH_STATUS_NOT_STARTED,
    AM_AUTH_STATUS_IN_PROGRESS,
    AM_AUTH_STATUS_COMPLETED
} am_auth_status_t;

/*
 * Language locale structure.
 */
typedef struct am_auth_locale {
    const char *language;
    const char *country;
    const char *variant;
} am_auth_locale_t;

/*
 * Enumeration of types of callbacks.
 */
typedef enum am_auth_callback_type {
    ChoiceCallback = 0,
    ConfirmationCallback,
    LanguageCallback,
    NameCallback,
    PasswordCallback,
    TextInputCallback,
    TextOutputCallback
} am_auth_callback_type_t;

/*
 * Choice callback structure.
 */
typedef struct am_auth_choice_callback {
    const char *prompt;
    boolean_t allow_multiple_selections;
    const char **choices;
    size_t choices_size;
    size_t default_choice;
    const char **response; /* selected indexes */
    size_t response_size;
} am_auth_choice_callback_t;

/*
 * Confirmation callback structure.
 */
typedef struct am_auth_confirmation_callback_info {
    const char *prompt;
    const char *message_type;
    const char *option_type;
    const char **options;
    size_t options_size;
    const char *default_option;
    const char *response; /* selected index */
} am_auth_confirmation_callback_t;

/*
 * Language callback structure.
 */
typedef struct am_auth_language_callback_info {
    am_auth_locale_t *locale;
    am_auth_locale_t *response; /* locale */
} am_auth_language_callback_t;

/*
 * Name callback structure.
 */
typedef struct am_auth_name_callback_info {
    const char *prompt;
    const char *default_name;
    const char *response; /* name */
} am_auth_name_callback_t;

/*
 * Password callback structure.
 */
typedef struct am_auth_password_callback_info {
    const char *prompt;
    boolean_t echo_on;
    const char *response; /* password */
} am_auth_password_callback_t;

/*
 * Text Input callback structure.
 */
typedef struct am_auth_text_input_callback_info {
    const char *prompt;
    const char *default_text;
    const char *response; /* text */
} am_auth_text_input_callback_t;

/*
 * Text Output callback structure.
 */
typedef struct am_auth_text_output_callback_info {
    const char *message;
    const char *message_type;
} am_auth_text_output_callback_t;

/*
 * Primary callback structure.  The callback_type field
 * represents which type of callback this instance of callback
 * is representing.  Based on the type, the user must use the
 * appropriate member of the union.
 */
typedef struct am_auth_callback {
    am_auth_callback_type_t callback_type;
    union am_auth_callback_info {
	am_auth_choice_callback_t choice_callback;
	am_auth_confirmation_callback_t	confirmation_callback;
	am_auth_language_callback_t language_callback;
	am_auth_name_callback_t name_callback;
	am_auth_password_callback_t password_callback;
	am_auth_text_input_callback_t text_input_callback;
	am_auth_text_output_callback_t text_output_callback;
    } callback_info;
} am_auth_callback_t;

/*
 * Initialize the authentication modules.
 *
 * Parameters:
 *   auth_init_params  The property handle to the property file which
 *                     contains the properties to initialize the
 *                     authentication library.
 *
 * Returns:
 *   AM_SUCCESS
 *             If the initialization of the library is successful.
 *
 *   AM_NO_MEMORY
 *             If unable to allocate memory during initialization.
 *
 *   AM_INVALID_ARGUMENT
 *             If auth_init_params is NULL.
 *
 *   Others (Please refer to am_types.h)
 *             If the error was due to other causes.
 */
AM_EXPORT am_status_t
am_auth_init(const am_properties_t auth_init_params);

/*
 * Create a new auth context and returns the handle.
 *
 * Parameters:
 *   auth_ctx	Pointer to the handle of the auth context.
 *
 *   org_name   Organization name to authenticate to.
 *              May be NULL to use value in property file.
 *
 *   cert_nick_name
 *              The alias of the certificate to be used if
 *              the client is connecting securely.  May be
 *              NULL in case of non-secure connection.
 *              
 *   url        Service URL, for example:
 *              "http://pride.red.iplanet.com:58080/amserver".
 *              May be NULL, in which case the naming service
 *              URL property is used.
 *
 * Returns:
 *   AM_SUCCESS
 *		If auth context was successfully created.
 *
 *   AM_NO_MEMORY
 *		If unable to allocate memory for the handle.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 *   AM_AUTH_CTX_INIT_FAILURE
 *              If the authentication initialization failed.
 */
AM_EXPORT am_status_t
am_auth_create_auth_context(am_auth_context_t *auth_ctx,
			    const char *org_name,
			    const char *cert_nick_name,
			    const char *url);

/*
 * Destroys the given auth context handle.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context to be destroyed.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the auth context was successfully destroyed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 */
AM_EXPORT am_status_t
am_auth_destroy_auth_context(am_auth_context_t auth_ctx);

/*
 * Starts the login process given the index type and its value.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 *   auth_idx   Index type to be used to initiate the login process.
 *
 *   value      Value corresponding to the index type.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the login process was successfully completed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx or value parameter is NULL.
 *
 *   AM_FEATURE_UNSUPPORTED
 *              If the auth_idx parameter is invalid.
 *
 */
AM_EXPORT am_status_t
am_auth_login(am_auth_context_t auth_ctx, am_auth_index_t auth_idx,
	      const char *value);

/*
 * Logout the user.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the logout process was successfully completed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 */
AM_EXPORT am_status_t
am_auth_logout(am_auth_context_t auth_ctx);

/*
 * Abort the authentication process.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the abort process was successfully completed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 */
AM_EXPORT am_status_t
am_auth_abort(am_auth_context_t auth_ctx);

/*
 * Checks to see if there are requirements to be supplied to
 * complete the login process. This call is invoked after
 * invoking the login() call. If there are requirements to
 * be supplied, then the caller can retrieve and submit the
 * requirements in the form of callbacks.
 *
 * The number of callbacks may be retrieved with a call to
 * am_auth_num_callbacks() and each callback may be retrieved
 * with a call to am_auth_get_callback(). Once the requirements
 * for each callback are set, am_auth_submit_requirements() is
 * called.
 *
 * Repeat until done.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *   B_TRUE
 *		If there are more requirements.
 *   B_FALSE
 *		If there are no more requirements.
 *
 */
AM_EXPORT boolean_t
am_auth_has_more_requirements(am_auth_context_t auth_ctx);

/*
 * Gets the number of callbacks.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *	Number of callbacks.
 *
 */
AM_EXPORT size_t
am_auth_num_callbacks(am_auth_context_t auth_ctx);

/*
 * Gets the n-th callback structure.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 *   index      The index into the callback array.
 *
 * Returns:
 *      Returns a pointer to the am_auth_callback_t structure
 *      which the caller needs to populate.
 *
 */
AM_EXPORT am_auth_callback_t *
am_auth_get_callback(am_auth_context_t auth_ctx, size_t index);

/*
 * Submits the responses populated in the callbacks to the server.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the submitted requirements were processed
 *		successfully.
 *
 *   AM_AUTH_FAILURE
 *		If the authentication process failed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 */
AM_EXPORT am_status_t
am_auth_submit_requirements(am_auth_context_t auth_ctx);

/*
 * 
 * Get the status of the authentication process.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *
 *   AM_AUTH_STATUS_FAILED
 *              The login process has failed.
 *
 *   AM_AUTH_STATUS_NOT_STARTED,
 *              The login process has not started.
 *
 *   AM_AUTH_STATUS_IN_PROGRESS,
 *              The login is in progress.
 *
 *   AM_AUTH_STATUS_COMPLETED,
 *              The user has been logged out.
 *
 *   AM_AUTH_STATUS_SUCCESS
 *              The user has logged in.
 *
 */
AM_EXPORT am_auth_status_t
am_auth_get_status(am_auth_context_t auth_ctx);

/*
 * Get the sso token id of the authenticated user.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *	A zero terminated string representing the sso token,
 *	NULL if there was an error or the user has not
 *	successfully logged in.
 *
 */
AM_EXPORT const char *
am_auth_get_sso_token_id(am_auth_context_t auth_ctx);

/*
 * Gets the organization to which the user is authenticated.
 *
 * Parameters:
 *   auth_ctx	Handle of the auth context.
 *
 * Returns:
 *	A zero terminated string representing the organization,
 *	NULL if there was an error or the user has not
 *	successfully logged in.
 *
 */
AM_EXPORT const char *
am_auth_get_organization_name(am_auth_context_t auth_ctx);

/*
 * Gets the authentication module/s instances (or plugins) configured
 * for an organization, or sub-organization name that was set during the
 * creation of the auth context.
 *
 * Supply the address of a pointer to a structure of type am_string_set_t.
 * Module instance names are returned in am_string_set_t. Free the memory
 * allocated for this set by calling am_string_set_destroy().
 *
 * Returns NULL if the number of modules configured is zero.
 *
 * Parameters:
 *   auth_ctx               Handle of the auth context.
 *   module_inst_names_ptr  Address of a pointer to am_string_set_t.
 *
 * Returns:
 *   AM_SUCCESS
 *		If the submitted requirements were processed
 *		successfully.
 *
 *   AM_AUTH_FAILURE
 *		If the authentication process failed.
 *
 *   AM_INVALID_ARGUMENT
 *		If the auth_ctx parameter is NULL.
 *
 *   AM_SERVICE_NOT_INITIALIZED
 *		If the auth service is not initialized.
 *
 */
AM_EXPORT am_status_t
am_auth_get_module_instance_names(am_auth_context_t auth_ctx,
				  am_string_set_t** module_inst_names_ptr);

AM_END_EXTERN_C
#endif
