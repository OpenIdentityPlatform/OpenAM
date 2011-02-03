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

    if(argc < 5) {
	printf("Usage: test_service <init property file> "
	       "<ssoToken> <resource name> <action>\n");
	return 0;
    }

    ssoToken = argv[2];
    resName = argv[3];
    action = argv[4];

    status = am_web_init(argv[1]);
    fail_on_error(status, "am_web_init");

    status = am_map_create(&env);
    fail_on_error(status, "am_map_create");

    /*
     * Acutal evaluation routine invoked.
     */
    status = am_web_is_access_allowed(ssoToken, resName, action,
				      "10.10.88.1" , env, &result);
    fail_on_error(status, "am_web_is_access_allowed");

    printf("Remote user = %s\n", result.remote_user);

    /* destroy the map. */
    am_map_destroy(env);
    am_map_destroy(response);

    am_policy_result_destroy(&result);

    status = am_web_cleanup(hdl);
    fail_on_error(status, "am_web_cleanup");

    am_properties_destroy(properties);

    return EXIT_SUCCESS;
}

void fail_on_error(am_status_t status, const char *method_name) {
    if (status != AM_SUCCESS) {
        fprintf(stderr,"\n\n");
        fprintf(stderr,"*** \n");
        fprintf(stderr,"*** ERROR: %s failed with error=%s\n", method_name, am_status_to_string(status));
        fprintf(stderr,"*** \n");
        fprintf(stderr,"\n\n");
        exit(EXIT_FAILURE);
    }
}
