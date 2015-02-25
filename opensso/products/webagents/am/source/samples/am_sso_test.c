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
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <am_sso.h>
#include <am_auth.h>
#include <am_notify.h>
#include <am_utils.h>
#include <am_log.h>
#include <am_web.h>

static const char *test_notif_msg = 
"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
"<NotificationSet vers=\"1.0\" svcid=\"session\" notid=\"215\">"
"<Notification><![CDATA[<SessionNotification vers=\"1.0\" notid=\"215\">"
"<Session sid=\"%s\" "
"stype=\"user\" cid=\"uid=test,ou=People,dc=iplanet,dc=com\" "
"cdomain=\"dc=iplanet,dc=com\" maxtime=\"120\" maxidle=\"30\" "
"maxcaching=\"3\" timeidle=\"1\" timeleft=\"7197\" state=\"destroyed\">"
"<Property name=\"authInstant\" value=\"2003-04-12T06:06:53Z\"></Property>"
"<Property name=\"clientType\" value=\"genericHTML\"></Property>"
"<Property name=\"CharSet\" value=\"UTF-8\"></Property>"
"<Property name=\"Locale\" value=\"en_US\"></Property>"
"<Property name=\"UserToken\" value=\"test\"></Property>"
"<Property name=\"MyPropName\" value=\"MyPropVal\"></Property>"
"<Property name=\"Host\" value=\"\"></Property>"
"<Property name=\"AuthType\" value=\"LDAP\"></Property>"
"<Property name=\"Principals\" "
"value=\"uid=test,ou=People,dc=iplanet,dc=com\"></Property>"
"<Property name=\"cookieSupport\" value=\"true\"></Property>"
"<Property name=\"Organization\" value=\"dc=iplanet,dc=com\"></Property>"
"<Property name=\"AuthLevel\" value=\"0\"></Property>"
"<Property name=\"UserId\" value=\"test\"></Property>"
"<Property name=\"Principal\" "
"value=\"uid=test,ou=People,dc=iplanet,dc=com\"></Property>"
"</Session>"
"<Type>5</Type>"
"<Time>1050127615852</Time>"
"</SessionNotification>]]></Notification>"
"</NotificationSet>";


typedef struct listener_info {
    char *name;
    int done;
} listener_info_t;

void
listener_func(am_sso_token_handle_t sso_token_handle,
              const am_sso_token_event_type_t event_type,
              const time_t event_time,
              void *opaque) 
{
    listener_info_t *info = NULL;
    if (!opaque) {
        printf("opaque is null!");
    }
    else {
        info = (listener_info_t *)opaque;
    
        if (sso_token_handle == NULL) {
            printf("Listener %s: sso token handle is null!", info->name);
        }
	else {
	    const char *sso_token_id = 
			am_sso_get_sso_token_id(sso_token_handle);
	    const char *propVal = am_sso_get_property(sso_token_handle, 
						      "MyPropName", B_FALSE);
	    boolean_t is_valid = am_sso_is_valid_token(sso_token_handle);
	    printf("%s sso token id is %s.\n", info->name,
		   sso_token_id==NULL?"NULL":sso_token_id);
	    printf("%s sso is %s.\n", info->name,
		   is_valid == B_TRUE ? "valid":"invalid");
	    printf("%s event type %d.\n", info->name, event_type);
	    printf("%s event time %d.\n", info->name, event_time);
	    printf("%s prop val is %s\n", 
			info->name, propVal==NULL?"Null":propVal);
	    printf("%s sso_token_handle is 0x%x.\n", 
			info->name, sso_token_handle);
	    info->done = 1;
	}
    }
    return;
}


void
listener_func_one(am_sso_token_handle_t sso_token_handle,
                  const am_sso_token_event_type_t event_type,
                  const time_t event_time,
                  void *opaque) 
{
    listener_func(sso_token_handle, event_type, event_time, opaque);
}

void
listener_func_two(am_sso_token_handle_t sso_token_handle,
                  const am_sso_token_event_type_t event_type,
                  const time_t event_time,
                  void *opaque) 
{
    listener_func(sso_token_handle, event_type, event_time, opaque);
}

void
listener_func_glob_one(am_sso_token_handle_t sso_token_handle,
                  const am_sso_token_event_type_t event_type,
                  const time_t event_time,
                  void *opaque) 
{
    listener_func(sso_token_handle, event_type, event_time, opaque);
}

void
listener_func_glob_two(am_sso_token_handle_t sso_token_handle,
                  const am_sso_token_event_type_t event_type,
                  const time_t event_time,
                  void *opaque) 
{
    listener_func(sso_token_handle, event_type, event_time, opaque);
}


void fail_on_status(am_status_t status, 
                    am_status_t expected_status, 
                    const char *method_name) 
{
    if (status != expected_status) {
	fprintf(stderr,"\n");
	fprintf(stderr,"ERROR: %s failed with status %s.\n",
		method_name, am_status_to_name(status));
	exit(EXIT_FAILURE);
    }
}

void fail_on_error(am_status_t status, const char *method_name) 
{
    fail_on_status(status, AM_SUCCESS, method_name);
}

void Usage(char **argv) {
    printf("Usage: %s"
           " [-u userid]"
           " [-p password]"
           " [-s sso token id]"
           " [-o org_name]"
           " [-m auth_module]"
           " [-f bootstrap_properties_file]"
           " [-c config_properties_file]"
	   " [-d]"
           "\n",
           argv[0]);
}

/*
 * login to get a session
 */
void
auth_login(am_properties_t prop, 
           const char *user,
           const char *pw,
           const char *org_name,
           am_auth_context_t *auth_ctx_ptr, 
           const char **ssoTokenID_ptr,
           const char *auth_module)
{
    am_status_t status;
    am_auth_context_t auth_ctx = NULL;
    am_auth_callback_t *callback;
    int i;

    status = am_auth_init(prop);
    fail_on_error(status, "am_auth_init");

    status = am_auth_create_auth_context(&auth_ctx, 
                                         org_name,
                                         NULL,
                                         NULL);
    fail_on_error(status, "am_auth_create_auth_context");

   status = am_auth_login(auth_ctx, AM_AUTH_INDEX_MODULE_INSTANCE, auth_module);
    fail_on_error(status, "am_auth_login");

    for (i = 0; i < am_auth_num_callbacks(auth_ctx); i++) {
       callback = am_auth_get_callback(auth_ctx, i);
       switch(callback->callback_type) {
       case NameCallback:
	   callback->callback_info.name_callback.response = user;
	   break;
       case PasswordCallback:
	   callback->callback_info.password_callback.response = pw;
	   break;
       default:
           printf("Warning: Unexpected callback type %d received.\n", 
                  callback->callback_type);
           break;
       }
    }
    status = am_auth_submit_requirements(auth_ctx);
    fail_on_error(status, "am_auth_submit_requirements");

    *ssoTokenID_ptr = am_auth_get_sso_token_id(auth_ctx);
    fail_on_error(status, "am_auth_login");

    *auth_ctx_ptr = auth_ctx;
}

/* 
 * test interfaces for sso token 
 */
void
test_interfaces(am_sso_token_handle_t sso_handle, const char *ssoTokenID) 
{
    am_string_set_t *principal_set = NULL;
    int i;
    char *propName = "MyPropName";
    char *propVal = "MyPropVal";
    am_status_t status;

    printf("Principal = %s.\n", am_sso_get_principal(sso_handle));

    principal_set = am_sso_get_principal_set(sso_handle);
    if (principal_set == NULL) {
        printf("ERROR: Principal set is NULL!\n");
    }
    else {
	printf("Principal set size %d.\n", principal_set->size);
        for (i = 0; i < principal_set->size; i++) {
            printf("Principal[%d] = %s.\n", i, principal_set->strings[i]);
        }
        am_string_set_destroy(principal_set);
    }

    printf("SSOToken = %s.\n", am_sso_get_sso_token_id(sso_handle));
    printf("Auth Level = %d.\n", (int)am_sso_get_auth_level(sso_handle));
    printf("Auth Type = %s.\n", am_sso_get_auth_type(sso_handle));
    printf("Host = %s.\n", am_sso_get_host(sso_handle));
    printf("Max Idle Time=%d.\n", (int)am_sso_get_max_idle_time(sso_handle));
    printf("Max Session Time=%d.\n", 
           (int)am_sso_get_max_session_time(sso_handle));
    printf("Organization=%s.\n", 
           am_sso_get_property(sso_handle, "Organization", B_TRUE));
    printf("Time Left=%d.\n", (int)am_sso_get_time_left(sso_handle));
    printf("Idle Time=%d.\n", (int)am_sso_get_idle_time(sso_handle));
    printf("IsValid=%s.\n", am_sso_is_valid_token(sso_handle)?"true":"false");
    printf("Validate Token returned %s.\n", 
           am_status_to_name(am_sso_validate_token(sso_handle)));
    printf("Refresh Token returned %s.\n", 
           am_status_to_name(am_sso_refresh_token(sso_handle)));
    printf("Time Left=%d.\n", (int)am_sso_get_time_left(sso_handle));
    printf("Idle Time=%d.\n", (int)am_sso_get_idle_time(sso_handle));

    // test set property.
    status = am_sso_set_property(sso_handle, propName, propVal);
    if (status != AM_SUCCESS)
        printf("am_sso_set_property returned %s.\n", am_status_to_name(status));
    else {
        const char *val = am_sso_get_property(sso_handle, propName, B_TRUE);
        printf("Set property done - MyPropName = %s\n", NULL==val?"NULL":val);
    }

}

/*
 * test listeners and notification
 */
void
test_listeners(am_sso_token_handle_t sso_handle, 
               const char *ssoTokenID, boolean_t dispatch_listener)
{
    am_status_t status;
    char *notif_msg_buf = NULL;
    char buf[1024];
    listener_info_t one, two, glob_one, glob_two;
    one.name = "one";
    two.name = "two";
    glob_one.name = "glob_one";
    glob_two.name = "glob_two";
    one.done = two.done = glob_one.done = glob_two.done = 0;

    // always decode ssoTokenID in case it was encoded when -s is used for testing.
    // this only needs to be done because of the simulated notification response.
    // real responses from server have session ID already decoded.
    
    if (strchr(ssoTokenID, '%') != NULL) {
        am_http_cookie_decode(ssoTokenID, buf, 1024);
        printf("decoded SSO Token is %s\n", buf);
    } else {
        strcpy(buf, ssoTokenID);
    }

    notif_msg_buf = (char *)malloc(strlen(test_notif_msg)+strlen(buf)+1);
    sprintf(notif_msg_buf, test_notif_msg, buf);

    // add first listener function.
    status = am_sso_add_sso_token_listener(sso_handle, 
                                           listener_func_one, 
                                           &one, 
					   dispatch_listener);
    if (status != AM_NOTIF_NOT_ENABLED) {
        printf("sso_handle is 0x%x.\n", sso_handle);
        fail_on_error(status, "am_sso_add_sso_token_listener() listener one");

	// add second listener function.
	status = am_sso_add_sso_token_listener(sso_handle, 
					       listener_func_two, 
					       &two,
					       dispatch_listener);
	fail_on_error(status, "am_sso_add_sso_token_listener() listener two");

	// remove first listener.
	status = am_sso_remove_sso_token_listener(sso_handle, 
		     (const am_sso_token_listener_func_t)listener_func_one);
	fail_on_error(status, "am_sso_remove_sso_token_listener one");

	// remove listener one again - this should return AM_NOT_FOUND.
	status = am_sso_remove_sso_token_listener(sso_handle, 
						  listener_func_one);
	fail_on_status(status, AM_NOT_FOUND,
		       "am_sso_remove_sso_token_listener remove one");

	// add listener one again.
	status = am_sso_add_sso_token_listener(sso_handle, 
					       listener_func_one, 
					       &one,
					       dispatch_listener);
        fail_on_error(status, 
                      "am_sso_add_sso_token_listener() listener one again");

	// add general listeners
	status = am_sso_add_listener(listener_func_glob_one, 
				     &glob_one,
				     dispatch_listener);
	fail_on_error(status, "am_sso_add_listener() listener glob one");
	
	status = am_sso_add_listener(listener_func_glob_two, 
				     &glob_two,
				     dispatch_listener);
	fail_on_error(status, "am_sso_add_listener() listener glob two");

	// remove glob one
	status = am_sso_remove_listener(listener_func_glob_one);
	fail_on_error(status, "am_sso_remove_listener glob one");

	// remove listener glob one again - this should return AM_NOT_FOUND.
	status = am_sso_remove_listener(listener_func_glob_one);
	fail_on_status(status, AM_NOT_FOUND,
		       "am_sso_remove_listener remove glob one");

	// add listener glob one again.
	status = am_sso_add_listener(listener_func_glob_one, 
				     &glob_one,
				     dispatch_listener);
        fail_on_error(status, 
                      "am_sso_add_listener() listener glob one again");

	// simulate notification
	status = am_notify(notif_msg_buf, NULL);
	fail_on_error(status, "am_notify()");

	printf("Waiting for listeners to finish..");
        fflush(stdout);
	while (!one.done || !two.done || !glob_one.done || !glob_two.done) {
            printf(".");
	}
	printf("Listeners returned.\n");
    }
    else {
        printf("Notification not enabled, listener test not performed.\n");
    }

    if (notif_msg_buf != NULL) 
        free(notif_msg_buf);
       
}


int
main(int argc, char *argv[])
{
    const char* prop_file = "../../config/OpenSSOAgentBootstrap.properties";
    const char* config_file = "../../config/OpenSSOAgentConfiguration.properties";
    am_status_t status = AM_FAILURE;
    am_properties_t prop = AM_PROPERTIES_NULL;
    am_auth_context_t auth_ctx = NULL;
    am_sso_token_handle_t sso_handle = NULL;
    const char *ssoTokenID = NULL;
    char *user = NULL;
    char* org_name = NULL;
    char* auth_module = "LDAP";
    char *pw = NULL;
    int j;
    char c;
    int usage = 0;
    boolean_t agentInitialized = B_FALSE; 
    boolean_t dispatch_listener = B_FALSE; /* dispatch listener in a */
					   /* seperate thread */

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];
            switch (c) {
	    case 'u':
                user = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'p':
                pw = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'o':
		org_name = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'f':
                prop_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 'c':
                config_file = (j <= argc-1) ? argv[++j] : NULL;
		break;
	    case 's':
                ssoTokenID = (j <= argc-1) ? argv[++j] : NULL;
		break;
            case 'm':
                auth_module = (j < argc-1) ? argv[++j] : NULL;
                break;
	    case 'd': 
		dispatch_listener = B_TRUE;
		break;
	    default:
		usage++;
		break;
	    }
	    if (usage)
		break;
        }
        else {
            usage++;
            break;
        }
    }

    if (usage || (NULL==ssoTokenID && (NULL==org_name) && (NULL==user || NULL==pw))) {
        Usage(argv);
        return EXIT_FAILURE;
    }

    am_web_init(prop_file, config_file);

    am_agent_init(&agentInitialized);

    // initialize sso
    status = am_properties_create(&prop);
    fail_on_error(status, "am_properties_create");

    status = am_properties_load( prop, prop_file );
    fail_on_error(status, "am_properties_load");

    status = am_log_init(prop);
    fail_on_error(status, "am_log_init");

    status = am_sso_init(prop);
    fail_on_error(status, "am_sso_init");

    // login to get a sso token ID
    if (NULL == ssoTokenID) {
        auth_login(prop, user, pw, org_name, &auth_ctx, &ssoTokenID, auth_module);
    }
    else {
        am_log_log(AM_LOG_ALL_MODULES, AM_LOG_INFO, 
                   "SSO Token ID is %s.", ssoTokenID);
    }

    // create sso token handle 
    status = AM_FAILURE;
    status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, 
                                            B_FALSE);
    fail_on_error(status, "am_sso_create_sso_token_handle");
    printf("Created sso token handle for %s.\n", ssoTokenID);

    status = am_sso_destroy_sso_token_handle(sso_handle);
    fail_on_error(status, "am_sso_destroy_sso_token_handle");
    sso_handle = NULL;

    // call it again to see if found in cache (check log)
    status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, 
                                            B_FALSE);
    fail_on_error(status, "am_sso_create_sso_token_handle (again)");
    printf("Created sso token handle again for %s.\n", ssoTokenID);

    // test interfaces for sso_handle
    test_interfaces(sso_handle, ssoTokenID);

    // test listeners and notification
    test_listeners(sso_handle, ssoTokenID, dispatch_listener);

    // test invalidate.
    printf("Invalidating token..\n");
    status = am_sso_invalidate_token(sso_handle);
    printf("am_sso_invalidate_token returned %s.\n", am_status_to_name(status));

    // session should now be invalid.
    printf("IsValid=%s.\n", am_sso_is_valid_token(sso_handle)?"true":"false");

    // add listener should now fail.
    status = am_sso_add_sso_token_listener(sso_handle, 
                                           listener_func_one, 
                                           NULL,
					   dispatch_listener);
    printf("am_sso_add_sso_token_listener() returned %s.\n",
           am_status_to_name(status));

    // destroy auth context
    status = am_auth_destroy_auth_context(auth_ctx);
    printf("am_auth_destroy_auth_context returned %s.\n", 
           am_status_to_name(status));

    // destroy sso token handle (free the memory)
    printf("Deleting token..\n");
    status = am_sso_destroy_sso_token_handle(sso_handle);
    printf("am_sso_destroy_sso_token_handle() returned %s.\n", 
           am_status_to_name(status));

    printf("Cleaning up..\n");
    (void)am_cleanup();
    am_properties_destroy(prop);

    printf("Done.\n");

    return EXIT_SUCCESS;
}  /* end of main procedure */

