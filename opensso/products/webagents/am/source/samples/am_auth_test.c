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

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

#include <stdio.h>
#include <am_auth.h>
#include <limits.h>
#include <ctype.h>
#if defined(SOLARIS)
#include <strings.h>
#elif defined(LINUX)
#include <string.h>
#elif defined(WINNT)
#endif

void
usage(char **argv)
{
    printf(
	"usage: %s\n"
	"       [-u user]\n"
	"       [-p password]\n"
	"       [-f bootstrap_properties_file]\n"
	"       [-r url]             (overrides property)\n"
	"       [-n cert_nick_name]  (overrides property)\n"
	"       [-o org_name]        (overrides property)\n"
	"       [-t 0                (for login based on AUTH_LEVEL)\n"
	"           1                (for login based on ROLE)\n"
	"           2                (for login based on USER)\n"
	"           3 (default)      (for login based on MODULE)\n"
	"           4]               (for login based on SERVICE)\n"
	"       [-m 0|1|2|...        (when used with option -t 0)\n"
	"           some_role        (when used with option -t 1)\n"
	"           some_user        (when used with option -t 2)\n"
	"           LDAP (default)   (when used with option -t 3)\n"
	"           some_service]    (when used with option -t 4)\n"
	"       [-R <#>]             (repeat)\n"
	"       [-V]                 (verbose)\n",
	argv[0]);
}

void process_login_callback_requirements(am_auth_context_t *p_auth_ctx,
	char *user, char *password);

void abort_login(am_auth_context_t *auth_ctx,
	const am_auth_status_t auth_status, const int count);

const char * get_status_name(const am_status_t s);

void verbose_message(const char *message);

void fail_on_error(am_status_t status, const char *method_name);

boolean_t Verbose_On = B_FALSE; /* verbose is off by default */

/*
 * main
 *         Login to Identity Server using the C API.
 */
int
main (int argc, char *argv[])
{
    const char* prop_file = "../../config/OpenSSOAgentBootstrap.properties";
    am_status_t status = AM_FAILURE;
    am_auth_status_t auth_status = AM_AUTH_STATUS_FAILED;
    am_properties_t prop = AM_PROPERTIES_NULL;
    am_auth_context_t auth_ctx = NULL;
    char* user = NULL;
    char* password = NULL;
    char* url = NULL;
    char* org_name = NULL;
    char* cert_nick_name = NULL;
    char* auth_module = "LDAP";
    am_auth_index_t auth_module_type = AM_AUTH_INDEX_MODULE_INSTANCE;
    long repeat_login_count = 1; /* default is one login */
    char c;
    boolean_t usage_error = B_FALSE;
    int i, j, k;
    const char* ssoTokenID = NULL;
    const char* organization = NULL;
    am_string_set_t* string_set;

    for (j = 1; j < argc; j++) {
	if (*argv[j] == '-') {
	    c = argv[j][1];
	    switch (c) {
	    case 'u':
		user = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'p':
		password = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'f':
		prop_file = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'r':
		url = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'n':
		cert_nick_name = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'o':
		org_name = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 'm':
		auth_module = (j < argc-1) ? argv[++j] : NULL;
		break;
	    case 't':
		if ((j < argc-1)) {
		    if (isdigit(argv[j+1][0]) &&
		    atoi (argv[j+1]) >= AM_AUTH_INDEX_AUTH_LEVEL &&
		    atoi (argv[j+1]) <= AM_AUTH_INDEX_SERVICE) {
			auth_module_type = (am_auth_index_t) atoi(argv[++j]);
		    }
		} else {
		    usage_error = B_TRUE;
		}
		break;
	    case 'R':
		if ((j < argc-1)) {
		    if (isdigit(argv[j+1][0])) {
			repeat_login_count = atoi(argv[++j]);
		    }
		} else {
		    repeat_login_count = LONG_MAX;
		}
		break;
	    case 'V':
		Verbose_On = B_TRUE;
		break;
	    default:
		usage_error = B_TRUE;
		break;
	    }
	    if (usage_error == B_TRUE)
		break;
	} else {
	    usage_error = B_TRUE;
	}
    }

    if (usage_error || (NULL==ssoTokenID && (NULL==org_name) && (NULL==user || NULL==password))) {
	usage(argv);
	exit(EXIT_FAILURE);
    }

    verbose_message("am_properties_create()");
    status = am_properties_create(&prop);
    fail_on_error(status, "am_properties_create()");

    verbose_message("am_properties_load()");
    status = am_properties_load(prop, prop_file);
    fail_on_error(status, "am_properties_load()");

    verbose_message("am_auth_init()");
    status = am_auth_init(prop);
    fail_on_error(status, "am_auth_init()");

    /* login and logout this many times */
    for (i = 0; i < repeat_login_count; i++) {

	if (i)
	    printf("\n");

	//get auth context 
	verbose_message("am_auth_create_auth_context()");
	status = am_auth_create_auth_context(
	    &auth_ctx, org_name, cert_nick_name, url);
	fail_on_error(status, "am_auth_create_auth_context()");

	// initiate login 
	verbose_message("am_auth_login()");
	status = am_auth_login(auth_ctx, auth_module_type, auth_module);
	fail_on_error(status, "am_auth_login()");

	process_login_callback_requirements(&auth_ctx, user, password);

	verbose_message("am_auth_get_status()");
	auth_status = am_auth_get_status(auth_ctx);
	if (auth_status == AM_AUTH_STATUS_SUCCESS) {
	    printf("    Login  %d Succeeded!\n", i+1);
	} else {
	    abort_login(&auth_ctx, auth_status, i+1);
	    continue;
	}

	verbose_message("am_auth_get_sso_token_id()");
	ssoTokenID = am_auth_get_sso_token_id(auth_ctx);
	if(ssoTokenID != NULL) {
	    printf("        SSOToken = %s\n", ssoTokenID);
	}

	verbose_message("am_auth_get_organization_name()");
	organization = am_auth_get_organization_name(auth_ctx);
	if(organization != NULL) {
	    printf("        Organization = %s\n", organization);
	}


	verbose_message("am_auth_get_module_instance_names()");
	status = am_auth_get_module_instance_names(auth_ctx, &string_set);
	fail_on_error(status, "am_auth_get_module_instance_names()");
	for(k = 0; k < string_set->size; k++) {
	    printf("        Module Instance Name [%d] = %s\n",
	    k,  string_set->strings[k]);
	}
	am_string_set_destroy(string_set);

	verbose_message("am_auth_logout()");
	status = am_auth_logout(auth_ctx);
	fail_on_error(status, "am_auth_logout()");

	verbose_message("am_auth_get_status()");
	auth_status = am_auth_get_status(auth_ctx);
	if (auth_status == AM_AUTH_STATUS_COMPLETED) {
	    printf("    Logout %d Succeeded!\n", i+1);
	} 

	verbose_message("am_auth_destroy_auth_context()");
	status = am_auth_destroy_auth_context(auth_ctx);
	fail_on_error(status, "am_auth_destroy_auth_context()");
	auth_ctx = NULL;

    } 

    verbose_message("am_cleanup()");
    status = am_cleanup();
    fail_on_error(status, "am_cleanup()");

    exit(EXIT_SUCCESS);

}  


/*
 * process_login_callback_requirements
 *         Fulfill login callback requirements.
 */
void
process_login_callback_requirements(am_auth_context_t *p_auth_ctx,
	char *user, char *password)
{
    am_status_t status = AM_FAILURE;
    am_auth_callback_t *callback;
    am_auth_name_callback_t *name_cb;
    am_auth_text_input_callback_t *text_input_cb;
    char input[80];
    char usr[80];
    char pw[80];
    char text[80];
    char choice[80];
    char *choices[1]; /* this sample supports single selection */
    char option[80];
    char language[80];
    char country[80];
    char variant[80];
    am_auth_locale_t locale; 
    size_t i, j, k;
    am_auth_context_t auth_ctx= *p_auth_ctx;

    /* satisfy login requirements */
    while (am_auth_has_more_requirements(auth_ctx) == B_TRUE) {
	verbose_message("am_auth_has_more_requirements()");

	for (i = 0; i < am_auth_num_callbacks(auth_ctx); i++) {
	    verbose_message("am_auth_get_callback()");
	    callback = am_auth_get_callback(auth_ctx, i);
	    switch(callback->callback_type) {
	    case ChoiceCallback:
		verbose_message("ChoiceCallback");
		choice[0] = '\0';
		for (j = 0;
		j < callback->callback_info.choice_callback.choices_size;
		j++) {
		    printf("[%d] %s\n",
		    j,
		    callback->callback_info.choice_callback.choices[j]);
		}
		strcpy(choice, "0");
		printf("Please enter selection [%s]: ", choice);
		scanf("%s", input);
		if (strlen (input) != 0)
		    strcpy (choice, input);
		choices[0] = choice;
		callback->callback_info.choice_callback.response =
		    (const char**) choices;
		callback->callback_info.choice_callback.response_size = 1;
	    break;
	    case ConfirmationCallback:
		verbose_message("ConfirmationCallback");
		for (k = 0;
		k < callback->callback_info.confirmation_callback.options_size;
		k++) {
		    printf("[%d] %s\n",
		    k,
		    callback->callback_info.confirmation_callback.options[k]);
		}
		strcpy(option, "0");
		printf("Please enter selection [%s]: ", option);
		scanf("%s", input);
		if (strlen (input) != 0)
		    strcpy (option, input);
		callback->callback_info.confirmation_callback.response = option;
	    break;
	    case LanguageCallback:
		verbose_message("LanguageCallback");
		language[0] = '\0';
		country[0] = '\0';
		variant[0] = '\0';
		if (callback->callback_info.language_callback.locale->language
		!= NULL) {
		    strcpy (language,
		    callback->callback_info.language_callback.locale->language);
		} else {
		    strcpy (language, "en");
		}
		printf("Please enter language [%s]: ", language);
		scanf("%s", input);
		if (strlen (input) != 0)
		    strcpy (language, input);
		locale.language = language;
		if (callback->callback_info.language_callback.locale->country
		!= NULL) {
		    strcpy (country,
		    callback->callback_info.language_callback.locale->country);
		} else {
		    strcpy (country, "US");
		}
		printf("Please enter country  [%s]: ", country);
		scanf("%s", input);
		if (strlen (input) != 0)
		    strcpy (country, input);
		locale.country = country;
		if (callback->callback_info.language_callback.locale->variant
		!= NULL) {
		    strcpy (variant,
		    callback->callback_info.language_callback.locale->variant);
		}
		printf("Please enter variant  [%s]: ", variant);
		scanf("%s", input);
		if (strlen (input) != 0)
		    strcpy (variant, input);
		locale.variant = variant;
		callback->callback_info.language_callback.response = &locale;
	    break;
	    case NameCallback:
		verbose_message("NameCallback");
		name_cb = &(callback->callback_info.name_callback);
		usr[0] = '\0';
		if (user != NULL && strlen(user) != 0) {
		    strcpy (usr, user);
		} else {
		    if (name_cb->default_name != NULL) {
			printf("%s [%s] ",
			    name_cb->prompt, name_cb->default_name);
		    } else {
			printf(name_cb->prompt);
		    }
		    scanf("%s", input);
		    if (strlen (input) != 0) {
			strcpy (usr, input);
		    } else {
			if (name_cb->default_name != NULL &&
			    strlen (name_cb->default_name) != 0) {
			    strcpy (usr, name_cb->default_name);
			}
		    }
		}
		callback->callback_info.name_callback.response = usr;
	    break;
	    case PasswordCallback:
		verbose_message("PasswordCallback");
		pw[0] = '\0';
		if (password != NULL && strlen(password) != 0) {
		    strcpy (pw, password);
		} else {
		    printf(callback->callback_info.password_callback.prompt);
		    scanf("%s", input);
		    if (strlen (input) != 0)
			strcpy (pw, input);
		}
		callback->callback_info.password_callback.response = pw;
	    break;
	    case TextInputCallback:
		verbose_message("TextInputCallback");
		text_input_cb = &(callback->callback_info.text_input_callback);
		text[0] = '\0';
		if (text_input_cb->default_text != NULL) {
		    printf("%s [%s] ",
			text_input_cb->prompt, text_input_cb->default_text);
		} else {
		    printf(text_input_cb->prompt);
		}
		scanf("%s", input);
		if (strlen (input) != 0) {
		    strcpy (text, input);
		} else {
		    if (text_input_cb->default_text != NULL &&
			strlen (text_input_cb->default_text) != 0) {
			strcpy (text, text_input_cb->default_text);
		    }
		}
		callback->callback_info.text_input_callback.response = text;
	    break;
	    case TextOutputCallback:
		verbose_message("TextOutputCallback");
		printf("Message Type: %s\n",
		callback->callback_info.text_output_callback.message_type);
		printf("     Message: %s\n",
		callback->callback_info.text_output_callback.message);
	    break;
            case HTTPCallback:
                printf("In HTTPCallback");
                callback->callback_info.http_callback.authToken = "AUTHTOKEN";
            break;
	    default: 
		printf("Warning: Unexpected callback type %d received.\n", 
		callback->callback_type);
	    break;
	    }
	} /* for callbacks */

	verbose_message("am_auth_submit_requirements()");
	status = am_auth_submit_requirements_and_update_authctx(&auth_ctx);
	fail_on_error(status, "am_auth_submit_requirements()");

    } /* while login requirements */

    return;
}


/*
 * abort_login
 *         Abort login.
 */
void
abort_login(am_auth_context_t *auth_ctx, const am_auth_status_t auth_status,
	const int count)
{
    am_status_t status;

    switch (auth_status) {
	case AM_AUTH_STATUS_SUCCESS:
	    printf("    Login  %d Status is Success - Aborting!\n", count);
	    break;
	case AM_AUTH_STATUS_FAILED:
	    printf("    Login  %d Status is Failed - Aborting!\n", count);
	    break;
	case AM_AUTH_STATUS_NOT_STARTED:
	    printf("    Login  %d Status is Not Started - Aborting!\n", count);
	    break;
	case AM_AUTH_STATUS_IN_PROGRESS:
	    printf("    Login  %d Status is In Progress - Aborting!\n", count);
	    break;
	case AM_AUTH_STATUS_COMPLETED:
	    printf("    Login  %d Status is Completed - Aborting!\n", count);
	    break;
	default:
	    printf("Unrecognized Status Code");
    }

    verbose_message("am_auth_abort()");
    status = am_auth_abort(*auth_ctx);
    fail_on_error(status, "am_auth_abort()");
    verbose_message("am_auth_destroy_auth_context()");
    status = am_auth_destroy_auth_context(*auth_ctx);
    fail_on_error(status, "am_auth_destroy_auth_context()");
    *auth_ctx = NULL;
}


/*
 * get_status_name
 *         Get the status name.
 */
const char *
get_status_name(const am_status_t s)
{
    switch(s) {
	case AM_SUCCESS:
	    return "AM_SUCCESS";
	case AM_FAILURE:
	    return "AM_FAILURE";
	case AM_INIT_FAILURE:
	    return "AM_INIT_FAILURE";
	case AM_AUTH_FAILURE:
	    return "AM_AUTH_FAILURE";
	case AM_NAMING_FAILURE:
	    return "AM_NAMING_FAILURE";
	case AM_SESSION_FAILURE:
	    return "AM_SESSION_FAILURE";
	case AM_POLICY_FAILURE:
	    return "AM_POLICY_FAILURE";
	case AM_NO_POLICY:
	    return "AM_NO_POLICY";
	case AM_INVALID_ARGUMENT:
	    return "AM_INVALID_ARGUMENT";
	case AM_INVALID_VALUE:
	    return "AM_INVALID_VALUE";
	case AM_NOT_FOUND:
	    return "AM_NOT_FOUND";
	case AM_NO_MEMORY:
	    return "AM_NO_MEMORY";
	case AM_NSPR_ERROR:
	    return "AM_NSPR_ERROR";
	case AM_END_OF_FILE:
	    return "AM_END_OF_FILE";
	case AM_BUFFER_TOO_SMALL:
	    return "AM_BUFFER_TOO_SMALL";
	case AM_NO_SUCH_SERVICE_TYPE:
	    return "AM_NO_SUCH_SERVICE_TYPE";
	case AM_SERVICE_NOT_AVAILABLE:
	    return "AM_SERVICE_NOT_AVAILABLE";
	case AM_ERROR_PARSING_XML:
	    return "AM_ERROR_PARSING_XML";
	case AM_INVALID_SESSION:
	    return "AM_INVALID_SESSION";
	case AM_INVALID_ACTION_TYPE:
	    return "AM_INVALID_ACTION_TYPE";
	case AM_ACCESS_DENIED:
	    return "AM_ACCESS_DENIED";
	case AM_HTTP_ERROR:
	    return "AM_HTTP_ERROR";
	case AM_INVALID_FQDN_ACCESS:
	    return "AM_INVALID_FQDN_ACCESS";
	case AM_FEATURE_UNSUPPORTED:
	    return "AM_FEATURE_UNSUPPORTED";
	case AM_AUTH_CTX_INIT_FAILURE:
	    return "AM_AUTH_CTX_INIT_FAILURE";
	case AM_SERVICE_NOT_INITIALIZED:
	    return "AM_SERVICE_NOT_INITIALIZED";
	case AM_NOTIF_NOT_ENABLED:
	    return "AM_NOTIF_NOT_ENABLED";
	case AM_ERROR_DISPATCH_LISTENER:
	    return "AM_ERROR_DISPATCH_LISTENER";
	default:
	    return "Unrecognized Status Code";
    }
}


/*
 * verbose_message
 *         If Verbose is On then print message.
 */
void
verbose_message(const char *message)
{
    if (Verbose_On == B_TRUE) {
	if (message != NULL && strlen (message) != 0) {
	    fprintf(stderr,"VERBOSE: %s\n", message);
	    fflush (stderr);
	}
    }
}


/*
 * fail_on_error
 *         If ERROR then print status and exit.
 */
void
fail_on_error(am_status_t status, const char *method_name)
{
    if (status != AM_SUCCESS) {
	fprintf(stderr,"ERROR: %s failed with status code = %u (%s)\n",
	    method_name, status, get_status_name(status));
	fflush (stderr);
	exit(EXIT_FAILURE);
    }
}

