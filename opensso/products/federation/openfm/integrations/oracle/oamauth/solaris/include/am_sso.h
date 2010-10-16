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
 * $Id: am_sso.h,v 1.2 2008/06/25 05:48:54 qcheng Exp $
 */

#ifndef __AM_SSO_H__
#define __AM_SSO_H__

#include <stdlib.h>
#include <am.h>
#include <am_types.h>
#include <am_properties.h>
#include <am_string_set.h>

AM_BEGIN_EXTERN_C

typedef struct am_sso_token_handle *am_sso_token_handle_t;

AM_EXPORT am_status_t
am_sso_init(am_properties_t property_map);

/*
 * Create an handle to session information.
 *
 * Parameters:
 *   sso_token_handle
 *		pointer to sso token handle which will be
 *		assigned an handle if the session validation is
 *		is successful.
 *   sso_token_id
 *		string representation session identifier.
 *
 *   reset_idle_timer
 *		When quering for session information should this
 *		query reset the idle timer on the server or not?
 * Returns:
 *   AM_SUCCESS
 *		if session validation was successful and
 *		a handle was successfully created.
 *
 *   AM_SERVICE_NOT_INITIALIZED
 *		if sso token service was not initialized.
 *              sso token service must be initialized by callling
 *              am_sso_init() any call to am_sso_* can be made.
 *
 *   AM_INVALID_ARGUMENT
 *		if the session_token_handle_ptr parameter is NULL
 *
 *   AM_NO_MEMORY
 *		if there was a memory allocation problem.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_create_sso_token_handle(am_sso_token_handle_t *sso_token_handle_ptr,
			       const char *sso_token_id,
			       boolean_t reset_idle_timer);

AM_EXPORT void
am_sso_set_initializeLog(boolean_t value);

/*
 * Destroy the handle to session information.  It does NOT log out the
 * user or invalidate the session.
 *
 * Parameters:
 *   sso_token_handle
 *		sso token handle to be deallocated.
 *
 * Returns:
 *   AM_SUCCESS
 *		if the memory release process was successful.
 *
 *   AM_INVALID_ARGUMENT
 *		if the session_token_handle parameter is NULL.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_destroy_sso_token_handle(am_sso_token_handle_t sso_token_handle);

/*
 * Invalidate/destroy the session on the server.
 * If successful the session handler in input argument will have state invalid
 * after this call.
 *
 * Note: Does not free the sso_token_handle input parameter.
 * Call am_sso_destroy_sso_token_handle() to free memory for the handle itself.
 *
 * Parameters:
 *   sso_token_handle
 *		sso token handle of session to be invalidated.
 *
 * Returns:
 *   AM_SUCCESS
 *		if session was successfully invalidated.
 *
 *   AM_INVALID_ARGUMENT
 *		if the sso_token_handle is invalid.
 *
 *   AM_SERVICE_NOT_INITIALIZED
 *		if the sso token service was not initialized with am_sso_init().
 *
 *   AM_SERVICE_NOT_AVAILABLE
 *		if server returned service not available.
 *
 *   AM_HTTP_ERROR
 *		if HTTP error encountered while communicating with server.
 *
 *   AM_ERROR_PARSING_XML
 *		if error parsing XML from server.
 *
 *   AM_ACCESS_DENIED
 *		if access denied while communicating with server.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_invalidate_token(const am_sso_token_handle_t sso_token_handle);

/*
 * Get the auth level for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The auth level of this session handle,
 *
 *      ULONG_MAX if there was any error.
 */
AM_EXPORT unsigned long
am_sso_get_auth_level(const am_sso_token_handle_t sso_token);

/*
 * Get the auth type for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The auth type of this session handle.
 *
 *      NULL if there was any error.
 *
 */
AM_EXPORT const char *
am_sso_get_auth_type(const am_sso_token_handle_t sso_token);

/*
 * Get the host address for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The host name of this session handle as given by the "Host" property.
 *
 *      NULL if the "Host" property is not set or does not have a value.
 *
 */
AM_EXPORT const char *
am_sso_get_host(const am_sso_token_handle_t sso_token);

/*
 * Get the max idle time for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The max idle time for this session handle in seconds.
 *
 *      (time_t)-1 if there was any error.
 *
 */
AM_EXPORT time_t
am_sso_get_max_idle_time(const am_sso_token_handle_t sso_token);

/*
 * Get the max session time for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The max session time of this session handle in seconds.
 *
 *      (time_t)-1 if there was any error.
 *
 */
AM_EXPORT time_t
am_sso_get_max_session_time(const am_sso_token_handle_t sso_token);

/*
 * Get the value of a session property.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 *   property_key
 *		the name of property to get
 *
 *   check_if_session_valid
 *		whether to check if session is valid first.
 *              if true and session is invalid, NULL will always be returned.
 *
 * Returns:
 *	The value of the session property.
 *
 *      NULL if property is not set or does not have a value.
 *
 */
AM_EXPORT const char *
am_sso_get_property(const am_sso_token_handle_t sso_token,
		    const char *property_key, boolean_t check_if_session_valid);

/*
 * Get the sso token id for this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The sso token id of this session.
 *
 *      NULL if sso_token_handle is invalid or any other error occurred.
 *
 */
AM_EXPORT const char *
am_sso_get_sso_token_id(const am_sso_token_handle_t sso_token_handle);

/*
 * Get the principal of this session.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The principal of this session handle,
 *
 *      NULL if the sso_token handle is invalid or any other error occurred.
 *
 */
AM_EXPORT const char *
am_sso_get_principal(const am_sso_token_handle_t sso_token);

/*
 * Get the set of principals of this session.
 * A session can have more than one principal.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The set of principals of this session handle,
 *
 *      NULL if the principal property is not set or has no value,
 *
 */
AM_EXPORT am_string_set_t *
am_sso_get_principal_set(const am_sso_token_handle_t sso_token);

/*
 * Check if a token is valid.
 * This call looks in the passed sso_token_handle to check for validity,
 * it does NOT go to the server.
 *
 * Parameters:
 *   sso_token_handle
 *		sso token to check if valid.
 *
 * Returns:
 *   B_TRUE
 *	if sso token is valid.
 *
 *   B_FALSE
 *	if sso token is invalid or any other error occurred.
 */
AM_EXPORT boolean_t
am_sso_is_valid_token(const am_sso_token_handle_t sso_token_handle);

/*
 * Validate a sso token.
 * This call will go to the server to get the latest session info and
 * update the sso_token_handle input parameter.
 * The sso_token_handle input parameter is updated if the return
 * status is either AM_SUCCESS or AM_INVALID_SESSION.
 * This is different from am_sso_refresh_token() in that it does *not*
 * update the last access time on the server.
 *
 * Parameters:
 *   sso_token_handle
 *		sso token to validate.
 *
 * Returns:
 *   AM_SUCCESS
 *		if sso token is valid, session handle is updated.
 *
 *   AM_INVALID_SESSION
 *              if the session is invalid, session handle is updated.
 *
 *   AM_INVALID_ARGUMENT
 *              if the input parameter is invalid.
 *
 *   AM_SERVICE_NOT_INITIALIZED
 *		if sso token service is not initialized.
 *		sso token service must be initialized by calling
 *              am_sso_init() before any call to am_sso*.
 *
 *   AM_SERVICE_NOT_AVAILABLE
 *		if server returned service not available.
 *
 *   AM_HTTP_ERROR
 *		if HTTP error encountered while communicating with server.
 *
 *   AM_ERROR_PARSING_XML
 *		if error parsing XML from server.
 *
 *   AM_ACCESS_DENIED
 *		if access denied while communicating with server.
 *
 *   AM_FAILURE
 *		if any other er:744
ror occurred.
 */
AM_EXPORT am_status_t
am_sso_validate_token(const am_sso_token_handle_t sso_token_handle);

/*
 * Refresh a sso token session.
 *
 * This goes to the server to get latest session info and update it
 * in the sso_token_handle input parameter like am_sso_validate_token().
 * However it also refreshes the last access time of the session.
 *
 * Parameters:
 *   sso_token_handle
 *		sso token to refresh.
 *
 * Returns:
 *   AM_SUCCESS
 *		if sso token could be refreshed with no errors.
 *
 *   AM_INVALID_ARGUMENT
 *              if the input parameter is invalid.
 *
 *   AM_SERVICE_NOT_INITIALIZED
 *		if sso token service is not initialized.
 *		sso token service must be initialized by calling
 *              am_sso_init() before any call to am_sso*.
 *
 *   AM_SERVICE_NOT_AVAILABLE
 *		if server returned service not available.
 *
 *   AM_HTTP_ERROR
 *		if HTTP error encountered while communicating with server.
 *
 *   AM_ERROR_PARSING_XML
 *		if error parsing XML from server.
 *
 *   AM_ACCESS_DENIED
 *		if access denied while communicating with server.
 *
 *   AM_SESSION_FAILURE
 *              if the session validation failed.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_refresh_token(const am_sso_token_handle_t sso_token_handle);

/*
 * Get the time left of this session handle
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The time left of this session handle in seconds.
 *
 *      (time_t)-1 if token is invalid or some error occurs.
 *      Detailed error is logged.
 */
AM_EXPORT time_t
am_sso_get_time_left(const am_sso_token_handle_t sso_token_handle);

/*
 * Get idle time associated with this session handle.
 *
 * Parameters:
 *   sso_token_handle
 *		the sso token handle
 *
 * Returns:
 *	The idle time of the session handle in seconds.
 *
 *      (time_t)-1 if token is invalid or some error occurs.
 *      Detailed error is logged.
 */
AM_EXPORT time_t
am_sso_get_idle_time(const am_sso_token_handle_t sso_token_handle);

/*
 * Set a property in the session.
 *
 * Note: session handle for this token ID obtained before this call
 * will not be current (not have the newly set property) after this call.
 * Call am_sso_validate_token() to update the handle with the new set of
 * properties.
 *
 * Parameters:
 *   sso_token_handle
 *		the session handle
 *
 *   name
 *		the property name
 *
 *   value
 *		the property value
 *
 * Returns:
 *   AM_SUCCESS
 *              if the property was successfully set.
 *
 *   AM_INVALID_ARGUMENT
 *		if the sso_token_handle is invalid.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_set_property(am_sso_token_handle_t sso_token_handle,
                    const char *name,
                    const char *value);


/*
 * Types needed by sso token listener related interfaces below.
 */
typedef enum {
    AM_SSO_TOKEN_EVENT_TYPE_UNKNOWN = 0,
    AM_SSO_TOKEN_EVENT_TYPE_IDLE_TIMEOUT = 1,
    AM_SSO_TOKEN_EVENT_TYPE_MAX_TIMEOUT = 2,
    AM_SSO_TOKEN_EVENT_TYPE_LOGOUT = 3,
    AM_SSO_TOKEN_EVENT_TYPE_DESTROY = 5
} am_sso_token_event_type_t;

typedef void (*am_sso_token_listener_func_t)(
                 const am_sso_token_handle_t sso_token_handle,
                 const am_sso_token_event_type_t event_type,
                 const time_t event_time,
                 void *args);

/*
 * Add a SSO token listener for the sso token's change events.
 *
 * Caller must either provide a URL to this function or have notification
 * enabled with a valid notification URL in the properties file used to
 * initialize sso in am_sso_init(). The URL must point to a HTTP host and port
 * that listens for notification messages from the server.
 * Notification messages are in XML. XML Notification messages received
 * from the server should be passed to as a string (const char *) to
 * am_notify(), which will parse the message and invoke listeners accordingly.
 * See the C API documentation and samples for more information.
 *
 * When the listener is called, the sso_token_handle that is passed to the
 * listener is a temporary one containing the updated session information
 * from the server. (Note that it is not the original sso_token_handle
 * passed to am_sso_add_sso_token_listener()).
 *
 * Once a listener has been called it is removed from memory, in other words,
 * a listener is called only once.
 *
 * Parameters:
 *   sso_token_handle
 *		the session handle containing the sso token id to listen for.
 *		the handle will be filled with the session information from
 *              the notification message. any existing contents will be
 *              overwritten.
 *
 *   listener
 *              the token change event listener
 *
 *   args
 *              arguments to pass to the listener.
 *
 *   dispatch_to_sep_thread
 *              call the listener in a seperate thread from an internal
 *              thread pool. This allows am_notify to return immediately
 *              upon parsing the notification message rather than waiting
 *              for the listener function(s) to finish before returning.
 *
 * Returns:
 *   AM_SUCCESS
 *              if the listener was successfully added.
 *
 *   AM_INVALID_ARGUMENT
 *		if sso_token_handle or listener is invalid, or if
 *              notification_url is not set and no notification url
 *              is provided in the properties file.
 *
 *   AM_NOTIF_NOT_ENABLED
 *              if notification is not enabled and the notification_url
 *              input parameter is invalid.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_add_sso_token_listener(am_sso_token_handle_t sso_token_handle,
                              const am_sso_token_listener_func_t listener,
                              void  *args,
			      boolean_t dispatch_to_sep_thread);

/*
 * Remove a SSO token listener for the sso token's change events.
 *
 * If am_sso_token_add_listener() was called more than once with the
 * same listener function, all instances of the listener function will be
 * removed.
 *
 * Parameters:
 *   sso_token_handle
 *		the session handle containing the sso token id for the
 *		listener.
 *
 *   listener
 *              the token change event listener
 *
 * Returns:
 *   AM_SUCCESS
 *              if the listener was successfully removed.
 *
 *   AM_INVALID_ARGUMENT
 *		if sso_token_id or listener is invalid or NULL.
 *
 *   AM_NOT_FOUND
 *              if listener was not found for the sso token id.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_remove_sso_token_listener(
	const am_sso_token_handle_t sso_token_handle,
        const am_sso_token_listener_func_t listener);


/*
 * Add a listener for the ANY sso token's change events.
 *
 * Caller must either provide a URL to this function or have notification
 * enabled with a valid notification URL in the properties file used to
 * initialize sso in am_sso_init(). The URL must point to a HTTP host and port
 * that listens for notification messages from the server.
 * Notification messages are in XML. XML Notification messages received
 * from the server should be passed to as a string (const char *) to
 * am_notify(), which will parse the message and invoke listeners accordingly.
 * See the C API documentation and samples for more information.
 *
 * When the listener is called, the sso_token_handle that is passed to the
 * listener is a temporary one containing the updated session information
 * from the server. (Note that it is not the original sso_token_handle
 * passed to am_sso_add_sso_token_listener()).
 *
 * Once added the listener will be called for any and all session event change
 * notification. (It will not be removed after it is called once
 * like am_sso_add_sso_token_listener).
 *
 * Parameters:
 *   listener
 *              the token change event listener
 *
 *   args
 *		arguments to pass to the listener
 *
 *   dispatch_to_sep_thread
 *              call the listener in a seperate thread from an internal
 *              thread pool. This allows am_notify to return immediately
 *              upon parsing the notification message rather than waiting
 *              for the listener function(s) to finish before returning.
 *
 * Returns:
 *   AM_SUCCESS
 *              if the listener was successfully added.
 *
 *   AM_INVALID_ARGUMENT
 *		if sso_token_handle or listener is invalid, or if
 *              notification_url is not set and no notification url
 *              is provided in the properties file.
 *
 *   AM_NOTIF_NOT_ENABLED
 *              if notification is not enabled and the notification_url
 *              input parameter is invalid.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_add_listener(const am_sso_token_listener_func_t listener,
                    void  *args,
		    boolean_t dispatch_to_sep_thread);

/*
 * Remove a SSO token listener for any sso token's change events.
 *
 * If am_sso_add_listener() was called more than once with the
 * same listener function, all instances of the listener function will be
 * removed.
 *
 * Parameters:
 *   listener
 *              the change event listener
 *
 * Returns:
 *   AM_SUCCESS
 *              if the listener was successfully removed.
 *
 *   AM_INVALID_ARGUMENT
 *		if listener was NULL.
 *
 *   AM_NOT_FOUND
 *              if listener was not found.
 *
 *   AM_FAILURE
 *		if any other error occurred.
 */
AM_EXPORT am_status_t
am_sso_remove_listener(const am_sso_token_listener_func_t listener);


AM_END_EXTERN_C

#endif /*__AM_SSO_H__*/
