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
    else {
	fprintf(stdout,"SUCCESS: %s passed with status %s.\n",
		method_name, am_status_to_name(status));
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
           const char *auth_module
	   )
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
test_interfaces(am_sso_token_handle_t sso_handle, const char *ssoTokenID , int sub_test_no) 
{
    am_string_set_t *principal_set = NULL;
    int i;
    char *propName = "MyPropName";
    char *propVal = "MyPropVal";
    am_status_t status;

	switch(sub_test_no){

	case 8 : {		 
		printf("am_sso_get_principal  %s \n" , am_sso_get_principal(sso_handle));
		}break;
	case 9 : {		
		printf("am_sso_get_principal_set  %d \n" , (int)am_sso_get_principal_set(sso_handle));
		}break;	
	case 10 : {		 
		printf("am_sso_get_sso_token_id  %s \n" , am_sso_get_sso_token_id(sso_handle));
		}break;
	case 11 : {		
		printf("am_sso_get_auth_level  %d \n" , (int)am_sso_get_auth_level(sso_handle));
		}break;
	case 12 : {		
		printf("am_sso_get_auth_type  %s \n" , am_sso_get_auth_type(sso_handle));
		}break;
	case 13 : {		
		printf("am_sso_get_host  %s \n" , am_sso_get_host(sso_handle));
		}break;
	case 14 : {		
		printf("am_sso_get_max_idle_time  %d \n" , (int)am_sso_get_max_idle_time(sso_handle));
		}break;
	case 15 : {		
		printf("am_sso_get_max_session_time  %d \n" , (int)am_sso_get_max_session_time(sso_handle));
		}break;
	case 16 : {		
		printf("am_sso_get_property %s \n" , am_sso_get_property(sso_handle, "Organization", B_TRUE));
		}break;
	case 17 : {		
		printf("am_sso_get_time_left  %d \n" , (int)am_sso_get_time_left(sso_handle));
		}break;
	case 18 : {		
		printf("am_sso_get_idle_time   %d \n" , (int)am_sso_get_idle_time(sso_handle));
		}break;
	case 19 : {
               boolean_t is_valid = am_sso_is_valid_token(sso_handle);
               if (is_valid) {
                  printf("am_sso_is_valid_token AM_SUCCESS \n");
               } else {
                  printf("am_sso_is_valid_token AM_FAILURE \n");
               }
		//printf(" am_sso_is_valid_token  %s \n" , am_status_to_name());
		}break;
	case 20 : {		
		printf("am_sso_validate_token  %s \n" , am_status_to_name(am_sso_validate_token(sso_handle)));
		}break;
	case 21 : {		
		printf("am_sso_refresh_token   %s \n" , am_status_to_name(am_sso_refresh_token(sso_handle)));
		}break;
	case 22 : {		
		printf("am_sso_get_time_left  %d \n" , (int)am_sso_get_time_left(sso_handle));
		}break;
	case 23 : {		
		printf("am_sso_get_idle_time  %d \n" ,am_sso_get_idle_time(sso_handle));
		}break;
	case 24 : {		
		printf("am_sso_set_property  %s \n" , am_status_to_name(am_sso_set_property(sso_handle, propName, propVal)));
		}break;
	}
}

/*
 * test listeners and notification
 */
void
test_listeners(am_sso_token_handle_t sso_handle, 
               const char *ssoTokenID, boolean_t dispatch_listener , int sub_test_no)
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

	switch(sub_test_no){

	case 25 : {		 
		printf("am_sso_get_principal  %s \n" ,  am_status_to_name(am_sso_add_sso_token_listener(sso_handle, 
                                           listener_func_one, 
                                           &one, 
					   dispatch_listener)));
		}break;	

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
    int test_no = 0;
	
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
	    case 't': 
		test_no = (j < argc-1) ? atoi(argv[++j]) : 0;

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
	
	
	switch(test_no){


	case 1 : {
		status = am_properties_create(&prop);
		printf("am_properties_create  %s \n" , am_status_to_name(status));
		}break;
	case 2 : {
		am_properties_create(&prop);
		status = am_properties_load( prop, prop_file );
		printf("am_properties_load %s \n" , am_status_to_name(status));
		}break;
	case 3 : {
		am_properties_create(&prop);
		 status = am_log_init(prop);
		printf("am_log_init  %s \n" , am_status_to_name(status));
		}break;
	case 4 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		status = am_sso_init(prop);
		printf("am_sso_init %s \n" , am_status_to_name(status));
		}break;

	case 5 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		am_sso_init(prop);
		status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, 
                                            B_FALSE);
		printf("am_sso_create_sso_token_handle %s \n", am_status_to_name(status));
		}break;
	case 6 : {				
		am_properties_create(&prop);		
		am_properties_load( prop, prop_file );		
		am_log_init(prop);		
		 am_sso_init(prop);		
		status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID,  B_FALSE);
                if(status == AM_SUCCESS){
			status = am_sso_destroy_sso_token_handle(sso_handle);	
			printf("am_sso_destroy_sso_token_handle  %s \n", am_status_to_name(status)); 
		}
		}break;
	case 7 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		am_sso_init(prop);
		status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, 
                                            B_FALSE);
                if(status == AM_SUCCESS){
			am_sso_destroy_sso_token_handle(sso_handle);
                        sso_handle = NULL;
		}
		//calling create ssso token again to check if it used the cache or went to  the server
		status = am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, 
                                            B_FALSE);		
		printf("am_sso_create_sso_token_handle (again)  %s \n", am_status_to_name(status));
		}break; 
	case 8 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 8);		
		}break; 
	case 9 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 9);		
		}break;  
	case 10 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 10);		
		}break;  
	case 11 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 11);		
		}break; 
	case 12 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 12);		
		}break; 
	case 13 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 13);		
		}break; 
	case 14 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 14);		
		}break; 
	case 15 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 15);		
		}break; 
	case 16: {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 16);		
		}break; 
	case 17 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 17);		
		}break; 
	case 18 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 18);		
		}break; 
	case 19 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 19);		
		}break; 
	case 20 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 20);		
		}break; 
	case 21 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 21);		
		}break; 
	case 22 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 22);		
		}break; 
	case 23 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 23);		
		}break; 
	case 24 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_interfaces(sso_handle, ssoTokenID , 24);		
		}break; 	
	case 25 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		test_listeners(sso_handle, ssoTokenID, dispatch_listener , 25);		
		}break; 	
	case 26 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		printf("am_sso_invalidate_token  %s \n", am_status_to_name(am_sso_invalidate_token(sso_handle)));			
		}break;
	case 27 : {
		am_properties_create(&prop);
		am_properties_load( prop, prop_file );
		am_log_init(prop);
		 am_sso_init(prop);
		am_sso_create_sso_token_handle(&sso_handle,
					    ssoTokenID, B_FALSE);
		printf("am_auth_destroy_auth_context  %s \n", am_status_to_name(am_auth_destroy_auth_context(auth_ctx)));			
		}break;
	}   

    // destroy auth context
    //STATus = am_auth_destroy_auth_context(auth_ctx);   
    // destroy sso token handle (free the memory)
    printf("Deleting token..\n");
    //status = am_sso_destroy_sso_token_handle(sso_handle);    
    printf("Cleaning up..\n");
    (void)am_cleanup();
    am_properties_destroy(prop);
    printf("Done.\n"); 

  return EXIT_SUCCESS;  
}  /* end of main procedure */

