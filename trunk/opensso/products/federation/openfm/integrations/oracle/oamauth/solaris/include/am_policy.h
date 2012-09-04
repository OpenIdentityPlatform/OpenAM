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
 * $Id: am_policy.h,v 1.4 2008/08/19 19:11:37 veiming Exp $
 */

/*
 * Abstract:
 *
 * Types and functions for using OpenSSO Access 
 * Management SDK policy objects.
 *
 */

#ifndef AM_POLICY_H
#define AM_POLICY_H

#include <stdlib.h>
#include <am.h>
#include <am_properties.h>
#include <am_map.h>

AM_BEGIN_EXTERN_C

typedef struct am_policy_result {
    const char *remote_user;
    const char *remote_user_passwd;
    const char *remote_IP;
    am_map_t advice_map;
    am_map_t attr_profile_map;
    am_map_t attr_session_map;
    am_map_t attr_response_map;
    const char *advice_string;
} am_policy_result_t;
#define AM_POLICY_RESULT_INITIALIZER \
{ \
    NULL, \
    NULL, \
    NULL, \
    AM_MAP_NULL, \
    AM_MAP_NULL, \
    AM_MAP_NULL, \
    AM_MAP_NULL, \
    NULL \
} \

/**
 * For explaination on each of these values see the comments in the
 * function am_policy_compare_urls.
 */
typedef enum am_resource_match {
    AM_SUB_RESOURCE_MATCH,
    AM_EXACT_MATCH,
    AM_SUPER_RESOURCE_MATCH,
    AM_NO_MATCH,
    AM_EXACT_PATTERN_MATCH
} am_resource_match_t;

typedef struct am_resource_traits {
    am_resource_match_t (*cmp_func_ptr)(const struct am_resource_traits *rsrc_traits,
				      const char *policy_res_name,
				      const char *resource_name,
				      boolean_t use_patterns);
    boolean_t (*has_patterns)(const char *resource_name);
    boolean_t (*get_resource_root)(const char *resource_name,
				    char *root_resource_name,
				    size_t buflength);
    boolean_t ignore_case;
    char separator;
    void (*canonicalize)(const char *resource, char **c_resource);
    void (*str_free)(void *resource_str);
} am_resource_traits_t;


/*
 * Opaque handle for a policy evaluation object.
 */
typedef unsigned int am_policy_t;

/**
 * Destroy am_policy_result internal structures.
 */
AM_EXPORT void am_policy_result_destroy(am_policy_result_t *result);


/*
 * Method to initialize the policy evaluation engine.
 */
AM_EXPORT am_status_t
am_policy_init(am_properties_t policy_config_properties);

/*
 * Method to initialize one specific instance of service for
 * policy evaluation.
 */
AM_EXPORT am_status_t
am_policy_service_init(const char *service_name,
		       const char *instance_name,
		       am_resource_traits_t rsrc_traits,
		       am_properties_t service_config_properties,
		       am_policy_t *policy_handle_ptr);

/*
 * Method to close an initialized policy evaluator
 */
AM_EXPORT am_status_t
am_policy_destroy(am_policy_t policy);

/*
 * Method to evaluate a non-boolean policy question for a resource.
 */
AM_EXPORT am_status_t
am_policy_evaluate_ignore_url_notenforced(am_policy_t policy_handle,
		      const char *sso_token,
		      const char *resource_name,
		      const char *action_name,
		      const am_map_t env_parameter_map,
		      am_map_t policy_response_map_ptr,
		      am_policy_result_t *policy_result,
		      am_bool_t ignorePolicyResult,
		      char **am_revision_number);

/*
 * Method to evaluate a non-boolean policy question for a resource.
 */
AM_EXPORT am_status_t
am_policy_evaluate(am_policy_t policy_handle,
		      const char *sso_token,
		      const char *resource_name,
		      const char *action_name,
		      const am_map_t env_parameter_map,
		      am_map_t policy_response_map_ptr,
		      am_policy_result_t *policy_result);

/*
 * Method to check if notification is enabled in the SDK.
 *
 * Returns:
 *  If notification is enabled returns non-zero, otherwise zero.
 */
AM_EXPORT boolean_t
am_policy_is_notification_enabled(am_policy_t policy_handle);


/*
 * Method to refresh policy cache when a policy notification is received
 * by the client.
 */
AM_EXPORT am_status_t
am_policy_notify(am_policy_t policy_handle,
		    const char *notification_data,
		    size_t notification_data_len);

/**
 * Method will take two url resources compare and return an appropriate
 * result.  The use_patterns is AM_TRUE, this method will consider occurances
 * of '*' in the policy resource name as wildcards.  If usePatterns is
 * AM_FALSE, '*' occurances are taken as a literal characters.
 * Returns:
 * EXACT_MATCH - If both the resource names exactly matched.
 * SUB_RESOURCE_MATCH - If the resourceName is a sub-resource to the resource
 *                      name defined in the policy.
 * SUPER_RESOURCE_MATCH - If the resourcName is a ancestor of the policy
 *                        resource name.
 * NO_MATCH - If the there is no kind of match between the policy resource
 *            and the requested resource name.
 * EXACT_PATTERN_MATCH - This result will be returned only if the policy is
 *                       matches resource name.  Distinction is not made
 *                       whether it was a EXACT_MATCH or a pattern match.
 * Note: In cases of SUB/SUPER_RESOURCE_MATCH, if the usePatterns is
 * AM_TRUE, the patterns are sub/super matching patterns.
 */
AM_EXPORT am_resource_match_t
am_policy_compare_urls(const am_resource_traits_t *rsrc_traits,
		       const char *policy_resource_name,
		       const char *resource_name,
		       boolean_t use_patterns);

/**
 * Given a url resource name, this method will populate the pointer
 * resourceRoot with the resource root.
 * Returns:
 * Successful root extraction will return AM_TRUE and AM_FALSE otherwise.
 */
AM_EXPORT boolean_t
am_policy_get_url_resource_root(const char *resource_name,
				   char *resource_root, size_t length);

AM_EXPORT boolean_t
am_policy_resource_has_patterns(const char *resource_name);

AM_EXPORT void
am_policy_resource_canonicalize(const char *, char **);

AM_EXPORT am_status_t
am_policy_invalidate_session(am_policy_t policy_handle,
                             const char *ssoTokenId);


AM_END_EXTERN_C

#endif	/* not AM_POLICY_H */
