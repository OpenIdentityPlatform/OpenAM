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
 *
 */ 
#include <stdio.h>
#include <am_web.h>
#include <am_policy.h>

void fail_on_error(am_status_t status, const char *name);

int main(int argc, char **argv)
{
    am_status_t status;
    am_policy_t hdl;
    const char *ssoToken = NULL;
    am_map_t env = NULL, response = NULL;
    am_policy_result_t result = AM_POLICY_RESULT_INITIALIZER;
    am_properties_t properties = AM_PROPERTIES_NULL;
    const char *action = NULL;
    const char *resName = NULL;
    boolean_t agentInitialized = B_FALSE;
    am_map_value_iter_t iter;

   /*
     * resource abstraction structure.  Pass actual implementations of
     * the required abstract routines for the kind of resource name you
     * have implemented.  The am library internally implements URL
     * resource names.  So, we pass references to the resource comparisons
     * inside am library.
     */
    am_resource_traits_t rsrc = {am_policy_compare_urls,
                                    am_policy_resource_has_patterns,
                                    am_policy_get_url_resource_root,
                                    B_FALSE,
                                    '/',
                                    am_policy_resource_canonicalize,
                                    free};

    if(argc < 6) {
	printf("Usage: %s "
               "<bootstrap_properties_file> <config_properties_file> <sso_token> <resource_name> <action>\n",
               argv[0]);
	return 0;
    }

    ssoToken = argv[3];
    resName = argv[4];
    action = argv[5];

    am_web_init(argv[1], argv[2]);

    am_agent_init(&agentInitialized);
    
    status = am_properties_create(&properties);
    fail_on_error(status, "am_properties_create");

   /*
     * load the properties file.  This file is the properties file that is
     * used during agent initialization.  If you have installed Identity
     * server or one of its agents, you can pass the path to
     * OpenSSOAgentBootstrap.properties of that installation. Make sure that 
     * your test program has permissions to write to the log directory specified
     * in the properties file.
     */
    status = am_properties_load(properties, argv[1]);
    fail_on_error(status, "am_properties_load");

    status = am_policy_init(properties);
    fail_on_error(status, "am_init");

    status = am_policy_service_init("iPlanetAMWebAgentService",
                                    "UNUSED PARAMETER",
                                    rsrc,
                                    properties, &hdl);
    fail_on_error(status, "am_policy_init");


    am_map_create(&env);
    am_map_create(&response);



    /*
     * Acutal evaluation routine invoked.
     */
    status = am_policy_evaluate(hdl, ssoToken, resName, action, env, response, &result);
    fail_on_error(status, "am_policy_evaluate");

    /*
     * Policy evaluation is a success.  Now we need to see what is
     * the result of the subject (user) being able to perform the
     * action on the resource (object).
     */

    printf("Policy evalutation successful.\n");
    status = am_map_find(response, action, &iter);
    fail_on_error(status, "am_map_find");

    printf("Remote user = %s\n", result.remote_user);
    printf("Values for the action %s are: ", action);
    while(B_TRUE==am_map_value_iter_is_value_valid(iter)) {
        if(status == AM_SUCCESS) {
            printf("%s ", am_map_value_iter_get(iter)); 
        }
        if(B_TRUE==am_map_value_iter_next(iter)) break;
    }
    printf("\n");

    am_map_value_iter_destroy(iter);

    /* destroy the map. */
    am_map_destroy(env);
    am_map_destroy(response);

    /*
     * Clean up the results structure allocated internally by the
     * evaluation engine.
     */
    am_policy_result_destroy(&result);


    /*
     * Destroy the policy handle.
     */
    am_policy_destroy(hdl);

    /*
     * Library cleanup: last thing to do before exit.
     */
    status = am_cleanup();
    fail_on_error(status, "am_cleanup");

    am_properties_destroy(properties);

    return EXIT_SUCCESS;
}

void fail_on_error(am_status_t status, const char *method_name) {
    if (status != AM_SUCCESS) {
        fprintf(stderr,"\n\n");
        fprintf(stderr,"*** \n");
        fprintf(stderr,"*** ERROR: %s failed with status code=%u\n", method_name, status);
        fprintf(stderr,"*** \n");
        fprintf(stderr,"\n\n");
        exit(EXIT_FAILURE);
    }
}
