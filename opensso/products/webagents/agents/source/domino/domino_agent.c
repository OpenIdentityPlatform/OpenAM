/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: domino_agent.c,v 1.4 2010/03/10 05:08:11 dknab Exp $
 *
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */

#include <stdio.h>
#include <string.h>
#include <global.h>
#include <dsapi.h>
#include <osmem.h>
#include <osmisc.h>
#include <lookup.h>
#include <nsfdata.h>
#include <names.h>
#include <miscerr.h>
#include <am_types.h>
#include <am_properties.h>
#include <am_sso.h>
#include "am_web.h"
#include <am_utils.h>
#include <bsafe.h>

#define AGENT_CONFIG_FILE "Agent_Config_File="
#define AGENT_BOOTSTRAP_FILE "Agent_Bootstrap_File="

typedef struct {
    char *properties_file;
    char *bootstrap_file;
} agent_config_rec_t;

static agent_config_rec_t agent_config;

boolean_t agentInitialized = B_FALSE;

#ifdef WINNT
#include <windows.h>
#define strcasecmp stricmp
#define strncasecmp strnicmp
#endif

#define HTTP_COOKIE "HTTP_COOKIE"
#define HTTP_QUERY_STRING "QUERY_STRING"
#define HTTP_REMOTE_ADDR "REMOTE_ADDR"
#define HTTP_PATH_INFO "PATH_INFO"
#define HTTP_CONTENT_LENGTH "CONTENT_LENGTH"
#define HTTPS_HEADER "HTTPS"
#define SERVER_NAME "SERVER_NAME"
#define HOST "HOST"
#define PORT "SERVER_PORT"
#define PROTOCOL "SERVER_PROTOCOL"

#define SET_COOKIE_HEADER_NAME "Set-Cookie"

#define MAX_BUF_LEN 1500
#define MAX_VAR_LEN 500
#define EXT_BUF_LEN 50

#define HTTP_200_OK 200
#define HTTP_302_REDIRECT 302
#define HTTP_403_FORBIDDEN 403
#define HTTP_500_INT_ERROR 500

#define REASON_200_OK "OK"
#define REASON_302_FOUND "Found"
#define REASON_403_FORBIDDEN "Forbidden"
#define REASON_500_INT_ERROR "Internal Server Error"

#define CONTENT_TYPE_HEADER "content-type:"
#define CONTENT_TYPE_TEXT_PLAIN "text/plain"
#define CONTENT_TYPE_TEXT_HTML "text/html"
#define CONTENT_LENGTH_HEADER "content-length:"
#define LOCATION_HEADER "Location:"

#define FORBIDDEN_TEXT "Access Denied."
#define INTERNAL_ERROR_TEXT "Internal Error Encountered."

#define DOUBLE_SLASH "//"

char DOMAIN_STRING[] = ";domain=";
char PATH_STRING[] = ";Path=/";
char SECURE_ONLY[] = ";secure";

/**
 * The filter response headers to use for forbidden or internal error.
 */

/* The headerText must end with "\r\n\r\n" for the HTTP reason code to
 * render correctly, even if there are no response headers to be set.
 * If set to NULL, "OK" is rendered even if th reason code is something else.
 * Each header must be separated by "\r\n"
 */
#define HEADER_SEPARATOR "\r\n"
#define HEADER_TEXT_END "\r\n\r\n"

#if defined(_WINDOWS)
__declspec(dllexport) unsigned int FilterInit(FilterInitData *);
__declspec(dllexport) unsigned int HttpFilterProc(FilterContext *,
        unsigned int, void *);
__declspec(dllexport) unsigned int TerminateFilter(unsigned int);
#endif

static am_status_t
check_domino_access(void **args, const char *userName, char **fullName);

typedef struct private_context {
    char *user;
    char *ltpaTokenName;
    char *ltpaTokenValue;
    am_web_result_t result;
    am_map_t response_headers_to_set;
} private_context_t;

void init_at_request();

// maps domino request method number to string and
// to am_web's request method number.

static char *
map_method(unsigned int methodNum, am_web_req_method_t *am_web_method_num) {
    char *method = "UNKNOWN";
    am_web_req_method_t am_num = AM_WEB_REQUEST_UNKNOWN;

    switch (methodNum) {
        case kRequestNone:
            method = "NONE";
            am_num = AM_WEB_REQUEST_UNKNOWN;
            break;
        case kRequestHEAD:
            method = "HEAD";
            am_num = AM_WEB_REQUEST_HEAD;
            break;
        case kRequestGET:
            method = "GET";
            am_num = AM_WEB_REQUEST_GET;
            break;
        case kRequestPOST:
            method = "POST";
            am_num = AM_WEB_REQUEST_POST;
            break;
        case kRequestPUT:
            method = "PUT";
            am_num = AM_WEB_REQUEST_PUT;
            break;
        case kRequestDELETE:
            method = "DELETE";
            am_num = AM_WEB_REQUEST_DELETE;
            break;
        case kRequestTRACE:
            method = "TRACE";
            am_num = AM_WEB_REQUEST_TRACE;
            break;
        case kRequestCONNECT:
            method = "CONNECT";
            am_num = AM_WEB_REQUEST_UNKNOWN;
            break;
        case kRequestOPTIONS:
            method = "OPTIONS";
            am_num = AM_WEB_REQUEST_OPTIONS;
            break;
        case kRequestUNKNOWN:
            method = "UNKNOWN";
            am_num = AM_WEB_REQUEST_UNKNOWN;
            break;
        case kRequestBAD:
            method = "BAD";
            am_num = AM_WEB_REQUEST_UNKNOWN;
            break;
        default:
            method = "UNKNOWN";
            am_num = AM_WEB_REQUEST_UNKNOWN;
            break;
    }
    if (am_web_method_num)
        *am_web_method_num = am_num;
    return method;
}

// map domino's request method number to string

static char *
method_num_to_str(unsigned int methodNum) {
    return map_method(methodNum, NULL);
}

// map domino request method number to am_web's request number

static am_web_req_method_t
method_to_am_web_num(unsigned int methodNum) {
    am_web_req_method_t method;
    (void) map_method(methodNum, &method);
    return method;
}

// map method string to domino method number.
// not currently used.

static unsigned int
method_str_to_num(const char *methodStr) {
    unsigned int retVal = kRequestNone;
    if (strcasecmp(methodStr, "HEAD") == 0)
        retVal = kRequestHEAD;
    else if (strcasecmp(methodStr, "GET") == 0)
        retVal = kRequestGET;
    else if (strcasecmp(methodStr, "POST") == 0)
        retVal = kRequestPOST;
    else if (strcasecmp(methodStr, "PUT") == 0)
        retVal = kRequestPUT;
    else if (strcasecmp(methodStr, "DELETE") == 0)
        retVal = kRequestDELETE;
    else if (strcasecmp(methodStr, "TRACE") == 0)
        retVal = kRequestTRACE;
    else if (strcasecmp(methodStr, "CONNECT") == 0)
        retVal = kRequestCONNECT;
    else if (strcasecmp(methodStr, "OPTIONS") == 0)
        retVal = kRequestOPTIONS;
    else if (strcasecmp(methodStr, "UNKNOWN") == 0)
        retVal = kRequestUNKNOWN;
    else if (strcasecmp(methodStr, "BAD") == 0)
        retVal = kRequestBAD;
    return retVal;
}

// map domino error to am_status_t

static am_status_t
map_domino_error(unsigned int dominoErrID) {
    am_status_t status = AM_FAILURE;
    switch (dominoErrID) {
        case DSAPI_BUFFER_TOO_SMALL:
            status = AM_BUFFER_TOO_SMALL;
            break;
        case DSAPI_INVALID_ARGUMENT:
            status = AM_INVALID_ARGUMENT;
            break;
        case DSAPI_MEMORY_ERROR:
            status = AM_NO_MEMORY;
            break;
        case DSAPI_INTERNAL_ERROR:
            break;
        case DSAPI_REQUEST_ALREADY_OWNED:
            break;
        case DSAPI_ERROR_MESSAGES:
            break;
        default:
            break;
    }
    return status;
}

// map domino error ID to the name as a string.

static const char *
domino_error_to_str(unsigned int dominoErrID) {
    char *errName = "UNKNOWN";
    switch (dominoErrID) {
        case DSAPI_BUFFER_TOO_SMALL:
            errName = "DSAPI_BUFFER_TOO_SMALL";
            break;
        case DSAPI_INVALID_ARGUMENT:
            errName = "DSAPI_INVALID_ARGUMENT";
            break;
        case DSAPI_MEMORY_ERROR:
            errName = "DSAPI_MEMORY_ERROR";
            break;
        case DSAPI_INTERNAL_ERROR:
            errName = "DSAPI_INTERNAL_ERROR";
            break;
        case DSAPI_REQUEST_ALREADY_OWNED:
            errName = "DSAPI_REQUEST_ALREADY_OWNED";
            break;
        case DSAPI_ERROR_MESSAGES:
            errName = "DSAPI_ERROR_MESSAGES";
            break;
        default:
            errName = "UNKNOWN";
            break;
    }
    return errName;
}

// map domino return code to the name as a string.
static const char *filterReturnCodeStr[] = {
    "kFilterNotHandled",
    "kFilterHandledRequest",
    "kFilterHandledEvent",
    "kFilterError",
    "Unknown Code"
};

static const char *
domino_return_code_to_str(unsigned int code) {
    const char *retVal = filterReturnCodeStr[4];
    if (code < 4) {
        retVal = filterReturnCodeStr[code];
    }
    return retVal;
}

// map domino event type to the name as a string.

char *getEventTypeStr(int eventType) {
    char *retStr = "unknown event type";
    switch (eventType) {
        case kFilterRawRequest:
            retStr = "kFilterRawRequest";
            break;
        case kFilterAuthenticate:
            retStr = "kFilterAuthenticate";
            break;
        case kFilterAuthorized:
            retStr = "kFilterAuthorized";
            break;
        case kFilterResponse:
            retStr = "kFilterResponse";
            break;
            // ones below are currently not used.
        case kFilterParsedRequest:
            retStr = "kFilterParsedRequest";
            break;
        case kFilterAuthUser:
            retStr = "kFilterAuthUser";
            break;
        case kFilterUserNameList:
            retStr = "kFilterUserNameList";
            break;
        case kFilterMapURL:
            retStr = "kFilterMapURL";
            break;
        case kFilterRawWrite:
            retStr = "kFilterRawWrite";
            break;
        case kFilterEndRequest:
            retStr = "kFilterEndRequest";
            break;
        case kFilterStartRequest:
            retStr = "kFilterStartRequest";
            break;
        case kFilterPostTranslate:
            retStr = "kFilterPostTranslate";
            break;
        case kFilterProcessRequest:
            retStr = "kFilterProcessRequest";
            break;
        case kFilterRewriteURL:
            retStr = "kFilterRewriteURL";
            break;
        default:
            break;
    }
    return retStr;
}

/**
 * This function is logs the domino errors with with domino error name.
 * It also maps the Domino error code to AM error codes and returns them.
 *
 * Parameter:
 *       func:  The function name where this error occured.
 *  dsap_call:  The DSAPI function that was called, which returned this
 *                 this error code.
 *      errID: The error code itself.
 *
 * Returns:
 *      am_status_t
 *              The mapped error code, which makes sense to our APIs.
 */
static am_status_t
log_domino_error(const char *func, const char *dsapi_call, unsigned int errID) {
    const char fmt[] = {"%s: Error while calling %s. Reason: %s[%d]."};
    const char *errName = domino_error_to_str(errID);
    am_web_log_error(fmt, func, dsapi_call, errName, errID);
    return map_domino_error(errID);
}

static am_status_t
allocateVariableInDominoContext(FilterContext *context, char *buf,
        size_t len, char **var) {
    const char *thisfunc = "allocateVariableInDominoContext()";
    am_status_t sts = AM_SUCCESS;
    unsigned int errID = 0;
    if ((buf == NULL) || (len == 0)) {
        am_web_log_warning("%s: Buffer is null or empty", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    if (sts == AM_SUCCESS) {
        *var = (char *) context->AllocMem(context, len + 1, 0, &errID);
        if (*var == NULL) {
            am_web_log_error("%s: memory alloc of %d bytes failed: %s.",
                    thisfunc, len, domino_error_to_str(errID));
            sts = map_domino_error(errID);
        } else {
            strcpy(*var, buf);
        }
    }
    return sts;
}

/**
 *  Gets a HTTP server variable, allocate memory from the domino context
 *  for the result, and copy result to the allocated memory.
 *  So don't use this if you don't need memory allocated.
 *  input: context - domino context
 *         varName - variable name
 *  output: value - pointer to contain a string.
 *                  If success this will never be NULL.
 *  returns: AM_SUCCESS upon success,
 *           other am_status_t values upon failure.
 */
static am_status_t
getServerVariable(FilterContext *context, const char *originalVarName,
        char **varValue, boolean_t isRequired,
        boolean_t addHTTPPrefix) {
    const char *thisfunc = "getServerVariable()";
    am_status_t sts = AM_SUCCESS;
    char* varName = NULL;
    char* buf = NULL;
    size_t buffer_size;

    if (context == NULL || originalVarName == NULL || varValue == NULL) {
        am_web_log_debug("%s: Invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    // Add HTTP in front of the header name if requested
    if (addHTTPPrefix == TRUE) {
        varName = malloc(strlen("HTTP_") +
                strlen(originalVarName) + 1);
        if (varName != NULL) {
            strcpy(varName, "HTTP_");
            strcat(varName, originalVarName);
            strcat(varName, "\0");
        } else {
            am_web_log_error("%s: Not enough memory to allocate varName",
                    thisfunc);
            sts = AM_NO_MEMORY;
        }
    } else {
        varName = (char*) originalVarName;
    }
    // Store the value of the header in a buffer large enough to hold it
    // (the exact size is not known)
    if (sts == AM_SUCCESS) {
        unsigned int errID = 0;
        *varValue = NULL;
        buffer_size = MAX_BUF_LEN;

        while (1) {
            buf = realloc(buf, buffer_size);
            if (buf == NULL) {
                am_web_log_error("%s: Could not allocate %i bytes of memory "
                        "for buffer_size", thisfunc, buffer_size);
                sts = AM_NO_MEMORY;
                break;
            } else {
                memset(buf, 0, buffer_size);
                // leave last byte for null terminating char.
                if (!context->GetServerVariable(context, (char *) varName,
                        (void *) buf, buffer_size - 1, &errID)) {
                    if (errID == DSAPI_INVALID_ARGUMENT) {
                        if (isRequired == B_TRUE) {
                            am_web_log_error("%s: Could not get a value for "
                                    "header %s.",
                                    thisfunc, *varName);
                            sts = AM_FAILURE;
                        }
                    } else {
                        sts = log_domino_error(thisfunc,
                                "context->GetServerVariable()", errID);
                        sts = AM_FAILURE;
                    }
                    break;
                } else {
                    if (strlen(buf) > buffer_size - 3) {
                        buffer_size = buffer_size + MAX_BUF_LEN;
                        am_web_log_warning("%s: Current buffer size for %s "
                                "is insufficient. Increasing to %i.",
                                thisfunc, varName, buffer_size);
                    } else {
                        //am_web_log_info("%s: Max buffer size is %i, "
                        // "no buffer size increase required for %s (length=%i).",
                        // thisfunc, buffer_size-3, varName, strlen(buf));
                        break;
                    }
                }
            }
        }
        // Store the header in memory allocated from the domino context
        if ((sts == AM_SUCCESS) && (buf != NULL)) {
            if (strlen(buf) == 0) {
                *varValue = NULL;
            } else {
                size_t len = 0;
                if (strchr(buf, ' ') != NULL) {
                    len = strlen(buf) + EXT_BUF_LEN;
                } else {
                    len = strlen(buf) + 1;
                }
                sts = allocateVariableInDominoContext(context, buf,
                        len, &(*varValue));
            }
        }
        if (sts == AM_SUCCESS) {
            if (*varValue != NULL) {
                am_web_log_debug("%s: %s = %s", thisfunc,
                        varName, *varValue);
            } else {
                am_web_log_debug("%s: %s =", thisfunc, varName);
            }
        }
        if (buf != NULL) {
            free(buf);
            buf = NULL;
        }
    }
    if ((addHTTPPrefix == TRUE) && (varName != NULL)) {
        free(varName);
        varName = NULL;
    }
    return sts;
}

#define HTTP_STR "http://"
#define HTTP_STR_LEN 7
#define HTTPS_STR "https://"
#define HTTPS_STR_LEN 8

/**
 * Construct the request URL from the HOST, PORT, HTTPS headers.
 * This function should be called if the URL in the filterRequest is null or
 * not a full URL.
 * Arguments:
 * context - the domino request context
 * rel_url - the empty or relative url from domino request
 * request_url - container for the constructed full request url
 */
static am_status_t
construct_request_url(FilterContext *context, char *rel_url, char **request_url) {
    const char *thisfunc = "construct_request_url()";
    am_status_t sts = AM_SUCCESS;
    char https_hdr[MAX_VAR_LEN];
    char host[MAX_VAR_LEN];
    char port[MAX_VAR_LEN];
    unsigned int port_num = 0;
    char *protocol = HTTPS_STR;
    int request_url_len = 0;
    unsigned int errID;

    // initialize to '\0'
    host[0] = '\0';
    https_hdr[0] = '\0';
    port[0] = '\0';

    // Get HTTPS header, does not have to exist - protocol defaults to http
    if (!context->GetServerVariable(context, HTTPS_HEADER,
            (void *) https_hdr, MAX_VAR_LEN - 1, &errID) &&
            errID != DSAPI_INVALID_ARGUMENT) {
        // error encountered other than variable not exist
        am_web_log_error("%s: Error when getting HTTPS header: %s.",
                thisfunc, domino_error_to_str(errID));
        sts = map_domino_error(errID);

        // Get the host from HOST header
    } else if (!context->GetServerVariable(context, HOST,
            (void *) host, MAX_VAR_LEN - 1, &errID) &&
            errID != DSAPI_INVALID_ARGUMENT) {
        // error encountered other than variable not exist
        am_web_log_error("%s: Error when getting HOST header: %s.",
                thisfunc, domino_error_to_str(errID));
        sts = map_domino_error(errID);

        // if HOST header is not there, get host from SERVER_NAME header.
        // this must exist if we couldn't get host from the HOST header.
    } else if (host[0] == '\0' &&
            !context->GetServerVariable(context, SERVER_NAME,
            (void *) host, MAX_VAR_LEN - 1, &errID)) {
        am_web_log_error("%s: Error when getting SERVER_NAME header: %s.",
                thisfunc, domino_error_to_str(errID));
        if (errID == DSAPI_INVALID_ARGUMENT) {
            sts = AM_NOT_FOUND;
        } else {
            sts = map_domino_error(errID);
        }

        // if SERVER_NAME header also missing, then return error.
    } else if (host[0] == '\0') {
        am_web_log_error("%s: Could not get host from HOST or "
                "SERVER_NAME header", thisfunc);
        sts = AM_NOT_FOUND;

        // Get the port, must exist.
    } else if (!context->GetServerVariable(context, PORT,
            (void *) port, MAX_VAR_LEN - 1, &errID)) {
        am_web_log_error("%s: Error when getting PORT header: %s.",
                thisfunc, domino_error_to_str(errID));
        if (errID == DSAPI_INVALID_ARGUMENT) {
            sts = AM_NOT_FOUND;
        } else {
            sts = map_domino_error(errID);
        }

        // Check if port value is valid.
    } else if (sscanf(port, "%u", &port_num) != 1) {
        am_web_log_error("%s: Invalid port header value %s.", thisfunc, port);
        sts = AM_NOT_FOUND;

        // Got everything OK, now construct the URL.
    } else {
        // set protocol to https if HTTPS header was ON, else default to http.
        if (!strcmp(https_hdr, "ON")) {
            protocol = HTTPS_STR;
        } else {
            protocol = HTTP_STR;
        }
        // now allocate memory for our constructed url.
        request_url_len = strlen(protocol) + strlen(host) + strlen(port) +
                (rel_url == NULL ? 0 : strlen(rel_url)) +
                2; /* colon + terminating null char */
        *request_url = context->AllocMem(context, request_url_len, 0, &errID);
        if (*request_url == NULL) {
            am_web_log_error("%s: context->AllocMem(%d) failed with %s.",
                    thisfunc, request_url_len,
                    domino_error_to_str(errID));
            sts = map_domino_error(errID);
        } else {
            // everything OK - fill in URL.
            // check if the relative URL has a leading slash first.
            char *leading_slash = "";
            if (rel_url == NULL) {
                leading_slash = "";
                rel_url = "";
            } else if (rel_url[0] != '\0' && rel_url[0] != '/') {
                leading_slash = "/";
            }
            sprintf(*request_url, "%s%s:%u%s%s",
                    protocol, // protocol includes the :// (colon slash slash)
                    host, port_num,
                    leading_slash, rel_url);
            am_web_log_debug("%s: successfully constructed URL %s.",
                    thisfunc, *request_url);
            sts = AM_SUCCESS;
        }
    }
    return sts;
}

/**
 * Get request url and method.
 * Method will be set only upon success.
 * Memory allocated for request url is from context and does not need to be
 * freed.
 *
 * Returns:
 *     AM_SUCCESSS if successful, others if not.
 */
static am_status_t
get_request_url(FilterContext *context,
        char **request_url, am_web_req_method_t *request_method) {
    const char *thisfunc = "get_request_url()";
    FilterRequest fRequest;
    unsigned int errID = 0;
    char *url = NULL;
    am_status_t sts = AM_SUCCESS;
    boolean_t is_full_url = B_FALSE;

    // Check args first
    if (context == NULL || request_url == NULL || request_method == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;

        // Get request info from context.
    } else if (!context->GetRequest(context, &fRequest, &errID)) {
        sts = log_domino_error(thisfunc, "context->GetRequest()", errID);

        // Check request method
    } else if (fRequest.method != kRequestGET &&
            fRequest.method != kRequestPOST) {
        am_web_log_error("%s: Unsupported request method: %s",
                thisfunc, method_num_to_str(fRequest.method));
        sts = AM_FEATURE_UNSUPPORTED;

        // now get or form URL from request
    } else {
        url = fRequest.URL;
        // check if URL is a full URL (begins with http[s]://).
        is_full_url = url != NULL && url[0] != '\0' &&
                (!strncasecmp(url, HTTP_STR, HTTP_STR_LEN) ||
                !strncasecmp(url, HTTPS_STR, HTTPS_STR_LEN));
        // if URL is a full URL, we're done.
        if (is_full_url) {
            *request_url = context->AllocMem(
                    context, strlen(url) + 1, 0, &errID);
            if (*request_url == NULL) {
                am_web_log_error("%s: could not allocate memory for "
                        "full url [%s]. Error %s.",
                        thisfunc, url, domino_error_to_str(errID));
                sts = map_domino_error(errID);
            } else {
                strcpy(*request_url, url);
                sts = AM_SUCCESS;
            }
        } else {
            // if URL is not full url then construct it from headers.
            // error and debug logging is done in the function.
            sts = construct_request_url(context, url, request_url);
        }
        // set method only on success.
        if (sts == AM_SUCCESS) {
            *request_method = method_to_am_web_num(fRequest.method);
            am_web_log_debug("%s: Request url = %s", thisfunc, *request_url);
            am_web_log_debug("%s: Request method = %s", thisfunc,
                    method_num_to_str(fRequest.method));

        } else {
            am_web_log_error("%s: Could not get request URL. Error %s",
                    thisfunc, am_status_to_string(sts));
        }
    }
    return sts;
}

/**
 * Sets a header in the response.
 * This only sets the header in the header map of the private context.
 * The header is really set in either filterResponse when the request
 * is allowed or in render_response when the request is forbidden or
 * internal error. This is because filterResponse is called with a
 * different context when it is forbidden or error, so the private context
 * does not get passed in those cases (sigh).
 * Parameter:
 *       key: The name of the header to be set.
 *     value: The value of the header to be set.
 *      args: void ptr to FilterContext structure.
 * Return: AM_SUCCESS if successful, other am_status_t otherwise.
 */
static am_status_t
add_header_in_response(void **args, const char *key, const char *value) {
    const char *thisfunc = "add_header_in_response()";
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;
    private_context_t *priv_ctx = NULL;

    if (key == NULL || value == NULL || args == NULL ||
            (context = (FilterContext *) args[0]) == NULL) {
        am_web_log_error("%s: invalid argument passed", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if ((priv_ctx = (private_context_t *) context->privateContext) == NULL) {
        am_web_log_error("%s: missing private context", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }// create map for response headers if it did not exist before.
    else if (priv_ctx->response_headers_to_set == NULL &&
            (sts = am_map_create(
            &priv_ctx->response_headers_to_set)) != AM_SUCCESS) {
        am_web_log_error("%s: am_map_create failed with error: %s",
                thisfunc, am_status_to_string(sts));
    }// insert header into map.
    else if ((sts = am_map_insert(priv_ctx->response_headers_to_set,
            key, value, B_FALSE)) != AM_SUCCESS) {
        am_web_log_error("%s: am_map_insert failed with error: %s",
                thisfunc, am_status_to_string(sts));
    } else {
        sts = AM_SUCCESS;
        am_web_log_max_debug("%s: added response header %s:%s",
                thisfunc, key, value);
    }
    return sts;
}

static am_status_t
get_post_data(void **args, char **data) {
    const char *thisfunc = "get_post_data()";
    FilterContext *context = (FilterContext *) args[0];
    unsigned int errID = 0;
    char *cLen = NULL;
    size_t len = 0;
    am_status_t sts = AM_SUCCESS;

    sts = getServerVariable(context, HTTP_CONTENT_LENGTH, &cLen,
            B_TRUE, B_FALSE);
    if (sts != AM_SUCCESS) {
        am_web_log_error("%s: Could not get content length.", sts);
    } else if (sscanf(cLen, "%u", &len) != 1) {
        am_web_log_error("%s: invalid content length string: %s",
                thisfunc, cLen);
        sts = AM_INVALID_ARGUMENT;
    } else if (!context->GetRequestContents(context, data, &errID)) {
        am_web_log_error("%s: Error getting request contents: %s.",
                thisfunc, domino_error_to_str(errID));
        sts = map_domino_error(errID);
    } else if (*data == NULL || (*data)[0] == '\0') {
        am_web_log_debug("%s: No contents recieved.", thisfunc);
        *data = NULL;
    } else {
        // content must be null terminated (for the rest of processing)
        // Note: hopefully data long enough for the null terminating char ?
        (*data)[len] = '\0';
        am_web_log_max_debug("%s: Contents received:\n %s",
                thisfunc, *data);
    }
    return sts;
}

static am_status_t
set_user(void **args, const char *user) {
    const char *thisfunc = "set_user()";
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;
    private_context_t *priv_ctx = NULL;
    char *fullname = NULL;

    am_web_log_max_debug("%s: set_user entered.", thisfunc);
    // Check arguments
    if (args == NULL || (context = (FilterContext *) args[0]) == NULL) {
        am_web_log_error("%s: Error - context is NULL.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if ((priv_ctx =
            (private_context_t *) context->privateContext) == NULL) {
        am_web_log_error("%s: Could not set user %s - request_info missing "
                "from request context.", thisfunc, user);
        sts = AM_INVALID_ARGUMENT;
    } else if (user == NULL || user[0] == '\0') {
        // if user is null, set it in filterAuthenticate and let domino
        // decide whether or not to allow it.
        priv_ctx->user = "";
        am_web_log_warning("%s: Remote user is null. Setting user to empty "
                "string in private context.", thisfunc);
        sts = AM_SUCCESS;
        // Find user in domino's database and get the full name
    } else if ((sts = check_domino_access(args, user,
            (char **) &fullname)) != AM_SUCCESS) {
        // On RH AS 2.1 with gcc 3.3.x optimized mode, calling a function here
        // that is non-trivial (like atoi or puts or printf), causes a segv.
        // So the following log is omitted to get around this weird prob,
        // until further analysis sheds more info.
        // am_web_log_error("%s: check domino access failed with %s.",
        // thisfunc, am_status_to_string(sts));
    } else if (fullname == NULL || fullname[0] == '\0') {
        if (sts == AM_SUCCESS) {
            sts = AM_NOT_FOUND;
        }
        am_web_log_error("%s: Check domino access failed with %s.",
                thisfunc, am_status_to_string(sts));
    } else {
        // all OK - set user in the private context to be set later
        // in filterAuthenticate.
        priv_ctx->user = fullname;
        am_web_log_debug("%s: User in private context set to: %s", thisfunc, fullname);
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
set_method(void **args, am_web_req_method_t method) {
    const char *thisfunc = "set_method()";
    FilterContext *context = NULL;
    FilterRawRequest *rawRequest = NULL;
    am_status_t sts;
    unsigned int errID;

    if (args == NULL ||
            (context = args[0]) == NULL || (rawRequest = args[1]) == NULL) {
        am_web_log_error("%s: one of input argument is NULL.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if (method != AM_WEB_REQUEST_GET && method != AM_WEB_REQUEST_POST) {
        am_web_log_error("%s: invalid request method %s.",
                thisfunc, method);
        sts = AM_INVALID_ARGUMENT;
    } else {
        am_web_log_debug("%s: request method before set %s.",
                thisfunc,
                rawRequest->requestMethod == kRequestGET ?
                "GET" : "POST");
        if (method == AM_WEB_REQUEST_GET)
            rawRequest->requestMethod = kRequestGET;
        else
            rawRequest->requestMethod = kRequestPOST;
        am_web_log_debug("%s: request method after set %s.",
                thisfunc,
                rawRequest->requestMethod == kRequestGET ?
                "GET" : "POST");
        if (!rawRequest->SetHeader(context, "REQUEST_METHOD",
                method == AM_WEB_REQUEST_GET ? "GET" : "POST",
                &errID)) {
            am_web_log_error("%s: Could not set request method header. "
                    "Error %s.",
                    thisfunc, errID);
            sts = map_domino_error(errID);
        } else {
            am_web_log_debug("%s: method changed.", thisfunc);
        }
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t
get_header_length(const char *key, const char *val, void **args) {
    const char *thisfunc = "get_header_length()";
    am_status_t sts = AM_SUCCESS;
    size_t totalLength = *(size_t *) args[0];
    size_t val_len = 0;
    size_t sep_len = strlen(HEADER_SEPARATOR);

    if (key == NULL) {
        am_web_log_error("%s: Invalid input argument.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        val_len = val == NULL ? 0 : strlen(val);
        totalLength = totalLength + strlen(key) + val_len + sep_len + 2; // colon + null term char
        *(size_t *) args[0] = totalLength;
    }

    return sts;
}

static am_status_t
add_headers(const char *key, const char *val, void **args) {
    const char *thisfunc = "add_headers()";
    am_status_t sts = AM_SUCCESS;
    char *header_buf = NULL;
    size_t max_len = 0;
    size_t n_written = 0;
    size_t add_len = 0;
    size_t val_len = 0;
    size_t sep_len = strlen(HEADER_SEPARATOR);

    if (key == NULL || args == NULL || args[0] == NULL ||
            args[1] == NULL || args[2] == NULL) {
        am_web_log_error("%s: Invalid input argument.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        am_web_log_max_debug("%s: Adding header:%s=[%s].",
                thisfunc, key, val);
        header_buf = (char *) args[0];
        max_len = *(size_t *) args[1];
        n_written = *(size_t *) args[2];
        val_len = val == NULL ? 0 : strlen(val);

        if (header_buf[0] != '\0')
            add_len = sep_len;
        add_len += strlen(key) + val_len + 2; // colon + null term char
        if (n_written + add_len > max_len) {
            am_web_log_warning("%s: Could not write response header %s: "
                    "headerText buffer too small. ",
                    thisfunc, key, val == NULL ? "NULL" : val);
            sts = AM_BUFFER_TOO_SMALL;
        } else {
            // Don't put seperator if it's the first header.
            if (header_buf[0] != '\0')
                strcat(header_buf, HEADER_SEPARATOR);
            strcat(header_buf, key);
            strcat(header_buf, ":");
            strcat(header_buf, val);
            *(size_t *) args[2] = n_written + add_len;
            sts = AM_SUCCESS;
        }
    }
    return sts;
}

/*
 * Called by render_response.
 * form headers from :
 *	- the headers map in the private context,
 *	- redirect Location header.
 *	- and set content type (always text/plain) and length.
 *  Then destroy the headers map in the private context,
 *  All input args assumed to be correct.
 */
static am_status_t
fill_header_text(char *headers_buf, size_t buf_size,
        unsigned int reason_code,
        const char *redirect_url,
        size_t content_len,
        private_context_t *priv_ctx,
        const char *data) {
    const char *thisfunc = "fill_header_text()";
    am_status_t sts = AM_SUCCESS;
    am_status_t tmp_sts = AM_SUCCESS;
    size_t max_len = buf_size - strlen(HEADER_TEXT_END) - 1; // null term. char.
    size_t n_written = 0;
    size_t add_len = 0;
    void *args[] = {headers_buf, &max_len, &n_written};

    am_web_log_max_debug("%s: Entered.", thisfunc);
    // put header for redirect_url first if reason code is redirect.
    if (reason_code == HTTP_302_REDIRECT) {
        add_len = strlen(LOCATION_HEADER) + strlen(redirect_url) + 1;
        if (add_len > buf_size) {
            am_web_log_warning("%s: Buffer too small for header "
                    "[Location:%s].", thisfunc, redirect_url);
            sts = AM_BUFFER_TOO_SMALL;
        } else {
            strcat(headers_buf, LOCATION_HEADER);
            strcat(headers_buf, redirect_url);
            n_written += strlen(headers_buf);
        }
    } else {
        // else set content-type and content-length headers first
        if ((data != NULL) && (strlen(data) > 5) &&
                (strncasecmp(data, "<HTML>", 6) == 0)) {
            sprintf(headers_buf, "%s%s%s%s%d%s",
                    CONTENT_TYPE_HEADER, CONTENT_TYPE_TEXT_HTML,
                    HEADER_SEPARATOR, CONTENT_LENGTH_HEADER, content_len,
                    HEADER_SEPARATOR);
        } else {
            sprintf(headers_buf, "%s%s%s%s%d%s",
                    CONTENT_TYPE_HEADER, CONTENT_TYPE_TEXT_PLAIN,
                    HEADER_SEPARATOR, CONTENT_LENGTH_HEADER, content_len,
                    HEADER_SEPARATOR);
        }
        n_written += strlen(headers_buf);
    }

    // headers from private context
    // errors setting these are logged as warning but ignored.
    if (priv_ctx == NULL) {
        am_web_log_warning("%s: private context is null. "
                "no response headers found and set.", thisfunc);
    } else if (priv_ctx->response_headers_to_set != NULL) {
        am_web_log_max_debug("%s:Response headers present.", thisfunc);
        if (am_map_size(priv_ctx->response_headers_to_set) > 0) {
            tmp_sts = am_map_for_each(priv_ctx->response_headers_to_set,
                    add_headers, args);
            if (tmp_sts != AM_SUCCESS) {
                am_web_log_warning("%s: Error [%s] adding header from "
                        "private context.", thisfunc,
                        am_status_to_string(sts));
            }
        }
        am_map_destroy(priv_ctx->response_headers_to_set);
        priv_ctx->response_headers_to_set = NULL;
        am_web_log_debug("%s: destroyed headers map.", thisfunc);
    }
    // fill with header text end
    strcat(headers_buf, HEADER_TEXT_END);
    return sts;
}

/**
 *  Render a response - ok, redirect, forbidden or internal error.
 *  In all cases, the responseFilter event will not called with the same
 *  context, so any response headers will be set here, and
 *  the headersMap in the context's private context must be destroyed/freed
 *  since we will not see it again. If any error was encountered setting
 *  headers from the private context, it is logged as warning but ignored.
 *  If the local buffer is not large enough for all headers in the
 *  private context, a warning is logged but result is still rendered.
 *
 *  input args:
 *     context - the request context
 *     response_code - 200, 500, 403 or 302.
 *     headers - array of "key:value" strings of extra headers to be set.
 *               the element in array should be null.
 *               The headers content type and length will always be set
 *               so these should not be included.
 *     data - redirect url if response code is 302 or content to be
 *            rendered if response code is 200 (OK).
 *  output args:
 *     retVal - filled with the result of rendering the response.
 *              kFilterHandledRequest on success and
 *              kFilterError on failure.
 */
static am_status_t
render_response(FilterContext *context, unsigned int response_code,
        const char *data, unsigned int *retVal) {
    const char *thisfunc = "render_response()";
    private_context_t *priv_ctx = NULL;
    am_status_t sts = AM_SUCCESS;
    unsigned int errID;
    char headers_buf[MAX_BUF_LEN]; // must be enough for >= content type + len.
    FilterResponseHeaders response;
    const char *content = NULL;
    size_t content_len = 0;
    const char *redirect_url = NULL;

    am_web_log_max_debug("%s: Entered.", thisfunc);
    memset(headers_buf, 0, sizeof (headers_buf));

    if (context == NULL) {
        am_web_log_error("%s: one of input arguments is NULL", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // fill filterResponseHeader struct & set response content accordingly.
        switch (response_code) {
            case HTTP_200_OK:
                response.responseCode = HTTP_200_OK;
                response.reasonText = REASON_200_OK;
                content = data;
                break;
            case HTTP_403_FORBIDDEN:
                response.responseCode = HTTP_403_FORBIDDEN;
                response.reasonText = REASON_403_FORBIDDEN;
                content = FORBIDDEN_TEXT;
                break;
            case HTTP_302_REDIRECT:
                response.responseCode = HTTP_302_REDIRECT;
                response.reasonText = REASON_302_FOUND;
                redirect_url = data;
                break;
            case HTTP_500_INT_ERROR:
            default:
                response.responseCode = HTTP_500_INT_ERROR;
                response.reasonText = REASON_500_INT_ERROR;
                content = INTERNAL_ERROR_TEXT;
                break;
        }
        response.headerText = headers_buf;

        // form headers from :
        // - the headers map in the private context,
        // - location headers from redirect urls.
        // - and set content type (always text/plain) and length.
        // Then destroy the headers map in the private context,
        priv_ctx = (private_context_t *) context->privateContext;
        content_len = content == NULL ? 0 : strlen(content);
        sts = fill_header_text(headers_buf, sizeof (headers_buf),
                response_code, redirect_url,
                content_len, priv_ctx, data);
        if (sts != AM_SUCCESS) {
            am_web_log_error("%s: Error forming response headers: %s",
                    thisfunc, am_status_to_string(sts));
        } else if (!context->ServerSupport(context, kWriteResponseHeaders,
                &response, NULL, 0, &errID)) {
            // now write the response headers
            am_web_log_error("%s: Could not render response %s",
                    thisfunc, response.reasonText,
                    domino_error_to_str(errID));
            sts = map_domino_error(errID);
        } else if (content != NULL && *content != '\0' &&
                !context->WriteClient(context, (char *) content,
                content_len, 0, &errID)) {
            // write the content.
            am_web_log_error("%s: Could not write content [%s], error %s.",
                    thisfunc, data, domino_error_to_str(errID));
            sts = map_domino_error(errID);
        } else {
            // done - log debug msg.
            am_web_log_debug("%s: Rendered response header [%s] "
                    "and content [%s].",
                    thisfunc, response.headerText,
                    content == NULL ? "" : content);
            sts = AM_SUCCESS;
        }
    }
    if (retVal != NULL) {
        *retVal = sts == AM_SUCCESS ? kFilterHandledRequest : kFilterError;
    }
    return sts;
}

static am_status_t
render_result(void **args, am_web_result_t result, char *data) {
    const char *thisfunc = "render_result()";
    FilterContext *context = (FilterContext *) args[0];
    am_status_t sts = AM_SUCCESS;

    am_web_log_debug("%s: rendering result %s, data [%s]",
            thisfunc, am_web_result_num_to_str(result),
            data == NULL ? "NULL" : data);
    // Check arguments
    if (context == NULL) {
        am_web_log_error("%s: No context passed in.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if (result == AM_WEB_RESULT_REDIRECT &&
            (data == NULL || data[0] == '\0')) {
        am_web_log_error("%s: Result is redirect but redirect_url is %s.",
                thisfunc, data == NULL ? "NULL" : "empty");
        sts = AM_INVALID_ARGUMENT;
    }// now take care of result.
    else {
        switch (result) {
            case AM_WEB_RESULT_OK:
                sts = AM_SUCCESS;
                break;
            case AM_WEB_RESULT_OK_DONE:
                sts = render_response(context, HTTP_200_OK, data, NULL);
                break;
            case AM_WEB_RESULT_REDIRECT:

                sts = render_response(context, HTTP_302_REDIRECT, data, NULL);
                break;
            case AM_WEB_RESULT_FORBIDDEN:
                sts = render_response(context, HTTP_403_FORBIDDEN, NULL, NULL);
                break;
            case AM_WEB_RESULT_ERROR:
            default:
                sts = render_response(context, HTTP_500_INT_ERROR, NULL, NULL);
                break;
        }
    }
    return sts;
}

static char *
make_header(FilterContext *context, const char *key, const char *val,
        unsigned int *errID) {
    const char *thisfunc = "make_header()";
    char *header = NULL;
    unsigned int len = 0;

    if (context == NULL || key == NULL || errID == NULL) {
        am_web_log_error("%s: one of input arguments is null.", thisfunc);
    } else {
        len = strlen(key) + 2;
        if (val != NULL) {
            len += strlen(val); // for the : separator and null terminating char
        }
        header = context->AllocMem(context, len, 0, errID);
        if (header != NULL) {
            sprintf(header, "%s:%s", key, val == NULL ? "" : val);
        }
    }
    return header;
}

static am_status_t
set_header_in_request(void **args, const char *key, const char *val) {
    const char *thisfunc = "set_header_in_request()";
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;
    FilterRawRequest *rawRequest = NULL;
    am_map_t request_hdr_map = NULL;

    // For some headers such as cookie, this can be called many times
    // with the same header name. This causes a bug/problem in domino
    // for CGI's since apparently only the first set call gets reflected
    // in the CGI's http_cookie environment variable.
    // Workaround is to put this into a map and set it later at the end :(

    // Check arguments
    if (key == NULL || key[0] == '\0' || args == NULL ||
            (context = (FilterContext *) args[0]) == NULL ||
            (rawRequest = (FilterRawRequest *) args[1]) == NULL ||
            (request_hdr_map = (am_map_t) args[2]) == NULL) {
        if (key == NULL)
            am_web_log_error("%s: key is NULL.", thisfunc);
        if (key[0] == '\0')
            am_web_log_error("%s: key[0] is NULL.", thisfunc);
        if (args == NULL)
            am_web_log_error("%s: args is NULL.", thisfunc);
        if (args[0] == NULL)
            am_web_log_error("%s: args[0] is NULL.", thisfunc);
        if (args[1] == NULL)
            am_web_log_error("%s: args[1] is NULL.", thisfunc);
        if (args[2] == NULL)
            am_web_log_error("%s: args[2] is NULL.", thisfunc);
        am_web_log_error("%s: Error setting header in request. "
                "One or more input arguments is NULL or empty.",
                thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // the last arg B_TRUE indicates to replace any existing key.
        //Added by bn152013 for 6728280
        if (val == '\0')
            sts = am_map_insert(request_hdr_map, key, NULL, B_TRUE);
        else
            sts = am_map_insert(request_hdr_map, key, val, B_TRUE);
        //End

        am_web_log_debug("%s: inserted request header %s:%s into map.",
                thisfunc, key, val == NULL ? "NULL" : val);
    }
    return sts;
}

static am_status_t
really_set_header_in_request(const char *key, const char *val, void **args) {
    const char *thisfunc = "really_set_header_in_request()";
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;
    FilterRawRequest *rawRequest = NULL;
    char *header = NULL;
    unsigned int errID;

    if (key == NULL || key[0] == '\0' || args == NULL ||
            (context = (FilterContext *) args[0]) == NULL ||
            (rawRequest = (FilterRawRequest *) args[1]) == NULL) {
        am_web_log_error("%s: Error setting header in request. "
                "One or more input arguments is NULL or empty.",
                thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    if (sts == AM_SUCCESS) {
        if ((val == NULL) || strlen(val) == 0) {
            am_web_log_debug("%s: Header %s is empty and will not be set.",
                    thisfunc, key);
        } else {
            if ((header = make_header(context, key, val, &errID)) == NULL) {
                am_web_log_error("%s: Could not allocate memory for setting "
                        "header %s:%s, error %s",
                        thisfunc, key, val, domino_error_to_str(errID));
                sts = map_domino_error(errID);
            } else if (!rawRequest->AddHeader(context, header, &errID)) {
                am_web_log_error("%s: Error setting header [%s: %s], "
                        "Domino error %s.",
                        thisfunc, key, val, domino_error_to_str(errID));
                sts = map_domino_error(errID);
            } else {
                am_web_log_debug("%s: Header set %s.", thisfunc, header);

                // Gross hack to work around weird domino bug.
                // HTTP_COOKIE must also be set for cookie for it to show up
                // in the CGI environment variable.
                if (!strcasecmp(key, "cookie")) {
                    (void) really_set_header_in_request("HTTP_COOKIE", val, args);
                }
            }
        }
    }

    return sts;
}

// set all request headers that was saved in the request header map.

static am_status_t
set_all_request_headers(void **args) {
    const char *thisfunc = "set_all_request_headers()";
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;
    FilterRawRequest *rawRequest = NULL;
    am_map_t request_headers_map = NULL;

    if ((context = (FilterContext *) args[0]) == NULL ||
            (rawRequest = (FilterRawRequest *) args[1]) == NULL ||
            (request_headers_map = (am_map_t) args[2]) == NULL) {
        am_web_log_error("%s: Error setting all request headers: "
                "One or more input arguments is NULL or empty.",
                thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if (am_map_size(request_headers_map) == 0) {
        am_web_log_debug("%s: No request headers to set.", thisfunc);
        sts = AM_SUCCESS;
    } else {
        sts = am_map_for_each(request_headers_map,
                really_set_header_in_request, args);
        am_web_log_debug("%s: setting all request headers result: %s",
                thisfunc, am_status_to_string(sts));
    }
    return sts;
}

am_status_t validate_domino_sso(char *ssoOrgName, char *ssoCfgName, char *ltpaTokenValue,
        char *fullName) {
    char *thisfunc = "validate_domino_sso";
    am_status_t retVal = AM_SUCCESS;
    char user[MAXUSERNAME];
    TIMEDATE retCreation;
    TIMEDATE retExpiration;
    STATUS error = NOERROR;

    error = SECTokenValidate(NULL, ssoOrgName, ssoCfgName, ltpaTokenValue,
            (char *) &user, &retCreation, &retExpiration,
            (DWORD) 0, (void *) NULL);
    if (error != NOERROR) {
        switch (error) {
            case ERR_LTPA_TOKEN_EXPIRED:
                retVal = AM_INVALID_SESSION;
                break;
            case ERR_LTPA_TOKEN_INVALID:
                retVal = AM_INVALID_SESSION;
                break;
            default:
                retVal = AM_FAILURE;
        }
    } else if (strcmp(user, fullName) != 0) {
        am_web_log_debug("%s: User from LtpaToken (%s) doesn't correspond to "
                "authenticated user (%s)", thisfunc, user, fullName);
        retVal = AM_INVALID_SESSION;
    } else {
        am_web_log_debug("%s: Validation successful for user %s.",
                thisfunc, user);
    }
    am_web_log_info("%s: Validation returns:%s.", thisfunc,
            am_status_to_string(retVal));
    return retVal;
}

am_status_t extract_token(FilterContext *context, char *httpCookieValue,
        const char *token, char **value) {
    am_status_t retVal = AM_SUCCESS;
    unsigned int errID = 0;
    const char *thisfunc = "extract_token";
    if (context == NULL || httpCookieValue == NULL || token == NULL ||
            value == NULL) {
        am_web_log_error("%s: Invalid argument(s) to parameter.", thisfunc);
        retVal = AM_FAILURE;
    } else {
        size_t len = strlen(httpCookieValue) + 1;
        am_web_log_max_debug("%s: Extracting token: %s", thisfunc, token);
        *value = (char *) context->AllocMem(context, len, 0, &errID);
        if (*value != NULL) {
            char *tmpPtr = strstr(httpCookieValue, token);
            if (tmpPtr != NULL) {
                strcpy(*value, tmpPtr);
                tmpPtr = strchr(*value, ';');
                if (tmpPtr != NULL) {
                    tmpPtr[0] = '\0';
                }
                *value += strlen(token) + 1;
                am_web_log_max_debug("%s: Extracted cookie value for %s=[%s]",
                        thisfunc, token, *value);
            } else retVal = AM_NOT_FOUND;
        } else retVal = AM_NO_MEMORY;
    }
    return retVal;
}

static am_status_t
set_cookie(FilterContext *context, void **args, const char* tokenName, char* tokenValue) {
    const char *thisfunc = "set_cookie()";
    am_status_t sts = AM_SUCCESS;
    unsigned int errID = 0;

    sts = set_header_in_request(args, tokenName, tokenValue);
    if (sts != AM_SUCCESS) {
        am_web_log_error("%s: Failed to set header %s in the request.",
                thisfunc, tokenName);
    }
    if (sts == AM_SUCCESS) {
        char *value = NULL;
        size_t len = strlen(tokenName);
        len += strlen(tokenValue) + 100;
        if ((value = context->AllocMem(context,
                len, 0, &errID)) != NULL) {
            size_t x = strlen(tokenValue);
            strcpy(value, tokenName);
            strcat(value, "=");
            strncat(value, tokenValue, x);
            sts = add_header_in_response(args, SET_COOKIE_HEADER_NAME,
                    value);
            if (sts != AM_SUCCESS) {
                am_web_log_error("%s: Failed to add %s cookie "
                        "in the response.", thisfunc, tokenName);
            }
        } else {
            am_web_log_error("%s: Not enough memory to allocate value.");
            sts = AM_NO_MEMORY;
        }
    }
    return sts;
}

am_status_t create_domino_sso(FilterContext *context, void **args,
        char *pName, char *ssoOrgName, char *ssoCfgName) {
    am_status_t sts = AM_SUCCESS;
    MEMHANDLE mhToken;
    char *thisfunc = "create_domino_sso";
    unsigned int errID = 0;
    STATUS error = NOERROR;
    SSO_TOKEN *pToken = NULL;
    char *tokenName = NULL;
    char *tokenValue = NULL;
    char *pDomainList = NULL;
    char *ltpaTokenValue = NULL;
    WORD numDomains = 0;
    size_t valueLen = 0;

    const char *ltpaTokenName = NULL;
    void* agent_config = NULL;

    agent_config = am_web_get_agent_configuration();
    ltpaTokenName = am_web_domino_ltpa_token_name(agent_config);

    am_web_log_max_debug("%s: PNAME=%s SSOConfigName=%s",
            thisfunc, pName, ssoCfgName);
    if ((ssoOrgName != NULL) && (strlen(ssoOrgName) == 0)) {
        ssoOrgName = NULL;
    }
    // Generate the LtpaToken
    if ((error = SECTokenGenerate(NULL, ssoOrgName, ssoCfgName, pName, 0,
            0, &mhToken,
            (DWORD) 0, (void *) NULL)) != NOERROR) {
        am_web_log_error("%s: SECTokenGenerate failed (ssoOrgName=%s, "
                "ssoCfgName=%s, pName=%s)",
                thisfunc, ssoOrgName, ssoCfgName, pName);
        sts = AM_FAILURE;
    }
    // Lock the token structure and its member data.
    if (sts == AM_SUCCESS) {
        am_web_log_max_debug("%s: SECTokenGenerate successful.", thisfunc);
        pToken = (SSO_TOKEN*) OSMemoryLock(mhToken);
        if (pToken == NULL) {
            am_web_log_error("%s: SSO_TOKEN is NULL after "
                    "dereferencing from MEMHANDLE.", thisfunc);
            sts = AM_FAILURE;
        }
    }
    // Get the token name
    if (sts == AM_SUCCESS) {
        if (pToken->mhName != NULL) {
            tokenName = (char *) OSMemoryLock(pToken->mhName);
            am_web_log_max_debug("%s: Token name returned by Domino server: %s", thisfunc, tokenName);
            OSMemoryUnlock(pToken->mhName);
        } else {
            am_web_log_error("%s: pToken->mhName is NULL.", thisfunc);
            sts = AM_FAILURE;
        }
    }
    // Get the list of null-delimited DNS domains
    if (sts == AM_SUCCESS) {
        if (pToken->mhDomainList != NULL) {
            pDomainList = (char*) OSMemoryLock(pToken->mhDomainList);
            am_web_log_max_debug("%s: First domain of the list returned by Domino server: %s",
                    thisfunc, pDomainList);
            OSMemoryUnlock(pToken->mhDomainList);
        } else {
            am_web_log_error("%s: pToken->mhDomainList is NULL.", thisfunc);
            sts = AM_FAILURE;
        }
    }
    // Get the total number of domains contained in the mhDomainList
    if (sts == AM_SUCCESS) {
        if (pToken->wNumDomains > 0) {
            numDomains = pToken->wNumDomains;
            am_web_log_max_debug("%s: Number of domains returned by Domino server: %i",
                    thisfunc, numDomains);
        } else {
            am_web_log_error("%s: pToken->wNumDomains is not a "
                    "positive number.", thisfunc);
            sts = AM_FAILURE;
        }
    }
    // Get the token value
    if (sts == AM_SUCCESS) {
        if (pToken->mhData != NULL) {
            tokenValue = (char*) OSMemoryLock(pToken->mhData);
            am_web_log_max_debug("%s: Token value returned by Domino server: %s", thisfunc,
                    tokenValue);
            OSMemoryUnlock(pToken->mhData);
        } else {
            am_web_log_error("%s: pToken->mhData is NULL.", thisfunc);
            sts = AM_FAILURE;
        }
    }

    if (sts == AM_SUCCESS) {
        char * pNullChar = NULL;
        char * pDomain = NULL;
        int iDomain = 0;

        if (strcmp(tokenName, ltpaTokenName) != 0) {
            am_web_log_warning("%s: Domino server token name (%s) doesn't match "
                    "token name configured in agent (%s). "
                    "%s will be used to set the cookie.",
                    thisfunc, tokenName, ltpaTokenName,
                    ltpaTokenName);
        }
        // Build and set the LtpaToken cookie in the all the domains 
        // listed in pDomainList
        pNullChar = pDomainList - 1;
        do {
            // Extract the null-delimited domains from the list
            pDomain = pNullChar + 1;
            pNullChar = strchr(pNullChar + 1, '\0');
            if ((pNullChar == NULL) || (strlen(pDomain) < 2)) {
                am_web_log_error("%s: Could not find %i domains in "
                        "the domain list.", thisfunc, numDomains);
                sts = AM_FAILURE;
            }
            // Build and set the cookie in the domain
            if (sts == AM_SUCCESS) {
                valueLen = strlen(tokenValue) + sizeof (DOMAIN_STRING) +
                        strlen(pDomain) + sizeof (PATH_STRING) +
                        sizeof (SECURE_ONLY) + 1;
                ltpaTokenValue = realloc(ltpaTokenValue, valueLen);
                if (ltpaTokenValue == NULL) {
                    am_web_log_error("%s: Not enought memory to allocate "
                            "ltpaTokenValue at iteration %i.",
                            thisfunc, iDomain);
                    sts = AM_NO_MEMORY;
                } else {
                    memset(ltpaTokenValue, 0, valueLen);
                    strcpy(ltpaTokenValue, tokenValue);
                    strcat(ltpaTokenValue, DOMAIN_STRING);
                    strcat(ltpaTokenValue, pDomain);
                    strcat(ltpaTokenValue, PATH_STRING);
                    if (pToken->bSecureOnly) {
                        strcat(ltpaTokenValue, SECURE_ONLY);
                    }
                    am_web_log_debug("%s: Setting %s cookie in domain %s:",
                            thisfunc, ltpaTokenName, pDomain);
                    sts = set_cookie(context, args, ltpaTokenName,
                            ltpaTokenValue);
                    if (sts != AM_SUCCESS) {
                        am_web_log_error("%s: Failed to set %s cookie "
                                "in domain %s.", thisfunc,
                                ltpaTokenName, pDomain);
                    }
                }
            }
            iDomain++;
        } while ((iDomain < numDomains) && (sts == AM_SUCCESS));
    }
    // Clean up
    if (pToken != NULL) {
        OSMemoryUnlock(mhToken);
        SECTokenFree(&mhToken);
    }
    if (ltpaTokenValue != NULL) {
        free(ltpaTokenValue);
        ltpaTokenValue = NULL;
    }
    return sts;
}

/*
 * Authenticates user in Domino address book using very domino
 * proprietary calls.
 *
 * Input:  userName as present in the request
 * Output: fullName as present in Domino address book
 * Return: AM_SUCCESS if operation was successful.
 */
static am_status_t
check_domino_access(void **args, const char *userName, char **fullName) {
    const char *thisfunc = "check_domino_access()";
    STATUS error = NOERROR;
    HANDLE hLookup = NULLHANDLE;
    WORD Matches = 0;
    char *pLookup = NULL;
    char *pName = NULL;
    char *pMatch = NULL;
    WORD ValueLength, DataType;
    char *ValuePtr = NULL;
    unsigned int errID;
    am_status_t sts = AM_SUCCESS;
    FilterContext *context = NULL;

    const char *ltpaTokenName = NULL;
    const char *ssoConfigName = NULL;
    const char *ssoOrgName = NULL;
    void* agent_config = NULL;


    agent_config = am_web_get_agent_configuration();
    ltpaTokenName = am_web_domino_ltpa_token_name(agent_config);
    ssoConfigName = am_web_domino_ltpa_config_name(agent_config);
    ssoOrgName = am_web_domino_ltpa_org_name(agent_config);

    // Check arguments
    if (args == NULL || (context = (FilterContext *) args[0]) == NULL ||
            userName == NULL || fullName == NULL) {
        am_web_log_error("%s: Error - context is NULL.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if (am_web_is_domino_check_name_database(agent_config) == B_FALSE) {
        // Check if checkNameDatabase is false. If so, just use ruser as
        // the authenticated name.
        if ((*fullName = context->AllocMem(
                context, strlen(userName) + 1, 0, &errID)) == NULL) {
            am_web_log_error("%s: could not allocate memory for [%s].",
                    thisfunc, userName);
            sts = map_domino_error(errID);
        } else {
            strcpy(*fullName, userName);
            am_web_log_max_debug("%s: Skipping name database lookup. "
                    "Setting full name to %s",
                    thisfunc, *fullName);
            sts = AM_SUCCESS;
        }
    } else if (sts == AM_SUCCESS && (error = NAMELookup(NULL,
            0, 1, "$Users", 1, (char *) userName,
            2, "FullName\0ShortName", &hLookup)) != NOERROR) {
        am_web_log_error("%s: Call to NAMELookup failed with %d.",
                thisfunc, error);
        sts = AM_NOT_FOUND;
    } else if (sts == AM_SUCCESS &&
            (pLookup = (char *) OSLockObject(hLookup)) == NULL) {
        am_web_log_error("%s: Call to OSLockObject returned NULL.", thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && (pName = (char *) NAMELocateNextName(
            pLookup, NULL, &Matches)) == NULL) {
        am_web_log_error("%s: Call to NAMELocateNextName returned NULL.",
                thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && Matches <= 0) {
        am_web_log_error("%s: Call to NAMELocateNextName returned no matches.",
                thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && (pMatch = (char *) NAMELocateNextMatch(
            pLookup, pName, NULL)) == NULL) {
        am_web_log_error("%s: Call to NAMELocateNextMatch failed.", thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && (ValuePtr = (char *) NAMELocateItem(pMatch, 0,
            &DataType, &ValueLength)) == NULL) {
        am_web_log_error("%s: Call to NAMELocateItem returned null.", thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && ValueLength <= 0) {
        am_web_log_error("%s: Call to NAMELocateItem returned <=0 length.",
                thisfunc);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && (*fullName = context->AllocMem(
            context, ValueLength + 1, 0, &errID)) == NULL) {
        // now allocate memory for user's full name.
        am_web_log_error("%s: Could not allocate memory of length %d.",
                thisfunc, ValueLength + 1);
        sts = map_domino_error(errID);
    } else if (sts == AM_SUCCESS && (error = NAMEGetTextItem(
            pMatch, 0, 0, *fullName, ValueLength + 1)) != NOERROR) {
        // Get user's full name.
        am_web_log_error("%s: NAMEGetTextItem (Get user's full name) "
                "failed with %d.", thisfunc, error);
        sts = AM_FAILURE;
    } else if (sts == AM_SUCCESS && (am_web_is_domino_ltpa_enable(agent_config) == B_TRUE)) {
        // Get or create LTPA Token
        char httpCookieValue[MAX_VAR_LEN];
        boolean_t createSSOToken = B_FALSE;
        httpCookieValue[0] = '\0';
        am_web_log_debug("%s: Domino full name obtained using remote user %s: %s",
                thisfunc, userName, *fullName);
        am_web_log_max_debug("%s: Check if LTPAToken cookie is in the headers.",
                thisfunc);
        context->GetServerVariable(context, HTTP_COOKIE,
                (void *) httpCookieValue, MAX_VAR_LEN - 1,
                &errID);
        am_web_log_max_debug("%s: Cookie `Header value: Cookie=[%s]", thisfunc,
                httpCookieValue);
        if (httpCookieValue[0] != '\0') {
            am_status_t lclStatus = AM_SUCCESS;
            char *ltpaTokenValue = NULL;
            if ((lclStatus = extract_token(context, httpCookieValue,
                    ltpaTokenName,
                    &ltpaTokenValue)) != AM_SUCCESS) {
                if (lclStatus == AM_NOT_FOUND) {
                    sts = AM_SUCCESS;
                    am_web_log_max_debug("%s: LTPAToken not found in the headers.",
                            thisfunc);
                    createSSOToken = B_TRUE;
                } else {
                    sts = lclStatus;
                    am_web_log_error("%s: Error while extracting LTPA token.",
                            thisfunc);
                }
            } else {
                am_web_log_max_debug("%s: Found LTPA Token:%s",
                        thisfunc, ltpaTokenValue);
                lclStatus = validate_domino_sso((char *) ssoOrgName, (char *) ssoConfigName, ltpaTokenValue,
                        *fullName);
                if (lclStatus == AM_INVALID_SESSION) {
                    am_web_log_debug("%s: Outdated LTPA Token:%s.",
                            thisfunc, ltpaTokenValue);
                    createSSOToken = B_TRUE;
                } else {
                    am_web_log_max_debug("%s: LTPA Token has been validated.",
                            thisfunc);
                    sts = lclStatus;
                }
            }
        } else {
            createSSOToken = B_TRUE;
        }
        // Create and set the LtapToken cookie(s)
        if (sts == AM_SUCCESS && createSSOToken == B_TRUE) {
            am_web_log_max_debug("%s: Creating LTPA Token.", thisfunc);
            sts = create_domino_sso(context, args, *fullName, (char *) ssoOrgName,
                    (char *) ssoConfigName);
        }
        if (sts != AM_SUCCESS) {
            am_web_log_error("%s: Error while generating or validating token.",
                    thisfunc);
        }
    }
    // clean up
    if (pLookup && hLookup) {
        OSUnlock(hLookup);
    }
    if (NULLHANDLE != hLookup) {
        OSMemFree(hLookup);
    }

    return sts;
}

static private_context_t *
create_private_context(FilterContext *context) {
    const char *thisfunc = "create_private_context()";
    private_context_t *new_ctx = NULL;
    unsigned int errID;

    am_web_log_max_debug("%s: creating new private context", thisfunc);

    if (context == NULL) {
        am_web_log_error("%s: input argument context is NULL.", thisfunc);
        new_ctx = NULL;
    } else if ((new_ctx = (private_context_t *)
            context->AllocMem(context, sizeof (private_context_t),
            0, &errID)) == NULL) {
        am_web_log_error("%s: context->AllocMem(%d) failed with %s.",
                thisfunc, sizeof (private_context_t),
                domino_error_to_str(errID));
    } else {
        // success.
        // the map for headers will be allocated the first time
        // add_header_in_response is called.
    }
    return new_ctx;
}

static am_status_t
encode_space(char *pInfo) {

    int pinfo_len = strlen(pInfo);
    char *space_ptr = NULL;
    char tmp_buf[MAX_VAR_LEN];

    am_web_log_error("pinfo_len:[%d]", pinfo_len);


    memset(tmp_buf, 0, MAX_VAR_LEN);
    if ((space_ptr = strchr(pInfo, ' ')) != NULL) {
        strncat(tmp_buf, pInfo, space_ptr - pInfo);
        strcat(tmp_buf, "%20");
        strcat(tmp_buf, space_ptr + 1);
        strcpy(pInfo, tmp_buf);
    }

    return AM_SUCCESS;

}

/*
 * Domino Server modifies the value of the path_info
 * server variable in a way that is difficult to predict.
 * This causes problem for the agent because the path_info
 * cannot be found in the url. To resolve this issue get the 
 * path info directly from the url instead of the server
 * variable.
 */
static am_status_t
get_path_info(FilterContext *context,
        char *request_url, char **path_info) {
    const char *thisfunc = "get_path_info()";
    am_status_t sts = AM_SUCCESS;
    char *original_path_info = NULL;
    char *constructed_path_info = NULL;
    char *pStart = NULL;
    char *pEnd = NULL;
    size_t len = 0;

    // Check args first
    if (context == NULL || request_url == NULL || path_info == NULL) {
        am_web_log_error("%s: Invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    // Get the path_info server variable
    if (sts == AM_SUCCESS) {
        sts = getServerVariable(context, HTTP_PATH_INFO,
                &original_path_info, B_FALSE, B_FALSE);
    }
    // Extract the path info from the request url.
    // In Domino server the path info is everything between
    // the hostname and the query.
    if ((sts == AM_SUCCESS) && (original_path_info != NULL)) {
        pStart = strstr(request_url, DOUBLE_SLASH);
        if ((pStart != NULL) && (strlen(pStart) > strlen(DOUBLE_SLASH))) {
            pStart += strlen(DOUBLE_SLASH);
            pStart = strstr(pStart, "/");
            if (pStart != NULL) {
                // Remove the query if there is one
                pEnd = strstr(pStart, "?");
                if (pEnd != NULL) {
                    len = pEnd - pStart;
                } else {
                    len = strlen(pStart);
                }
                if (len > 0) {
                    constructed_path_info = (char *) malloc(len + 1);
                    if (constructed_path_info != NULL) {
                        memset(constructed_path_info, 0, len + 1);
                        strncpy(constructed_path_info, pStart, len);
                    } else {
                        am_web_log_error("%s: Unable to allocate memory for "
                                "constructed_path_info variable.", thisfunc);
                        sts = AM_NO_MEMORY;
                    }
                } else {
                    am_web_log_error("%s: Could not get path info from "
                            "request url %s.",
                            thisfunc, request_url);
                    sts = AM_FAILURE;
                }
            } else {
                am_web_log_error("%s: Request url %s is malformed ",
                        thisfunc, request_url);
                sts = AM_FAILURE;
            }
        } else {
            am_web_log_error("%s: Request url %s is malformed ",
                    thisfunc, request_url);
            sts = AM_FAILURE;
        }
    }
    // Allocate the path in the Domino context so it has not to 
    // be free.
    if ((sts == AM_SUCCESS) && (original_path_info != NULL)) {
        sts = allocateVariableInDominoContext(context, constructed_path_info,
                strlen(constructed_path_info),
                &(*path_info));
        if (strcmp(original_path_info, constructed_path_info) != 0) {
            am_web_log_debug("%s: Path info changed to: %s",
                    thisfunc, constructed_path_info);
        }
    }
    if (constructed_path_info != NULL) {
        free(constructed_path_info);
        constructed_path_info = NULL;
    }
    return sts;
}

static unsigned int
filterRawRequest(FilterContext *context, FilterRawRequest *rawRequest) {
    const char *thisfunc = "filterRawRequest()";
    unsigned int retVal = kFilterError;
    am_status_t sts = AM_SUCCESS;
    private_context_t *priv_ctx = NULL;
    char *request_url = NULL;
    char *path_info = NULL;
    char *requestIP = NULL;
    char *cookie_header_val = NULL;
    const char *clientIP_hdr_name = NULL;
    char *clientIP_hdr = NULL;
    char *clientIP = NULL;
    const char *clientHostname_hdr_name = NULL;
    char *clientHostname_hdr = NULL;
    char *clientHostname = NULL;
    void* agent_config = NULL;
    am_web_request_params_t params;
    am_web_req_method_t method;
    am_status_t render_sts = AM_FAILURE;
    am_web_request_params_t req_params;
    am_web_request_func_t req_funcs;
    am_web_result_t result = AM_WEB_RESULT_ERROR;
    am_map_t request_headers_map = NULL;


    memset(&params, 0, sizeof (params));

    // check if agent is initialized.
    // if not initialized, then call agent init function
    // TODO: This needs to be synchronized as only one time agent
    // initialization needs to be done.
    if (agentInitialized != B_TRUE) {
        am_web_log_debug("%s: Will call init", thisfunc);
        init_at_request();
        if (agentInitialized != B_TRUE) {
            am_web_log_error("%s: Agent is still not intialized", thisfunc);
            //deny the access
            sts = AM_FAILURE;
        } else {
            am_web_log_debug("%s: Agent intialized", thisfunc);
        }
    }
    // Get agent configuration
    if (sts == AM_SUCCESS) {
        agent_config = am_web_get_agent_configuration();
    }
    // Check arguments
    if (context == NULL) {
        am_web_log_error("%s: No context found.", thisfunc);
        sts = AM_FAILURE;
    }
    // Check if there is an existing context
    if (sts == AM_SUCCESS) {
        if ((priv_ctx = (private_context_t *) context->privateContext) != NULL) {
            am_web_log_error("%s: Unexpected private context found.", thisfunc);
            // don't set any headers from the unexpected private context.
            context->privateContext = NULL;
            sts = AM_FAILURE;
        }
    }
    // Create private context for passing info between HttpFilterProc calls.
    if (sts == AM_SUCCESS) {
        if ((context->privateContext = (priv_ctx = create_private_context(context))) == NULL) {
            am_web_log_error("%s: Failed to create request private context.",
                    thisfunc);
            sts = AM_FAILURE;
        }
    }
    // Get the URL and method (method will be either GET or POST)
    if (sts == AM_SUCCESS) {
        sts = get_request_url(context, &request_url, &method);
    }
    // Get path info 
    if (sts == AM_SUCCESS) {
        sts = get_path_info(context, request_url, &path_info);
    }
    // Get cookie header val 
    if (sts == AM_SUCCESS) {
        sts = getServerVariable(context, HTTP_COOKIE, &cookie_header_val,
                B_FALSE, B_FALSE);
    }
    // If there is a proxy in front of the agent, the user can set 
    // in AMAgent.properties the name of the headers that the proxy 
    // is using to set the real client IP and host name. 
    // The agent should then use the IP and host name obtained from 
    // these headers to process the request

    // Get the client IP address header set by the proxy, if there is one
    if (sts == AM_SUCCESS) {
        clientIP_hdr_name = am_web_get_client_ip_header_name(agent_config);
        if (clientIP_hdr_name != NULL) {
            sts = getServerVariable(context, clientIP_hdr_name,
                    &clientIP_hdr, B_FALSE, B_TRUE);
        }
    }
    // Get the client host name header set by the proxy, if there is one
    if (sts == AM_SUCCESS) {
        clientHostname_hdr_name = am_web_get_client_hostname_header_name(agent_config);
        if (clientHostname_hdr_name != NULL) {
            sts = getServerVariable(context, clientHostname_hdr_name,
                    &clientHostname_hdr, B_FALSE, B_TRUE);
        }
    }
    // If the client IP and host name headers contain more than one
    // value, take the first value.
    if (sts == AM_SUCCESS) {
        if ((clientIP_hdr != NULL) || (clientHostname_hdr != NULL)) {
            sts = am_web_get_client_ip_host(clientIP_hdr, clientHostname_hdr,
                    &clientIP, &clientHostname);
        }
    }
    // Set the IP address that will be used for the evaluation
    if (sts == AM_SUCCESS) {
        if (clientIP != NULL) {
            // The ip address should be allocated from the Domino context
            // like it would have been using getServerVariable().
            sts = allocateVariableInDominoContext(context, clientIP,
                    strlen(clientIP), &requestIP);
        } else {
            sts = getServerVariable(context, HTTP_REMOTE_ADDR,
                    &requestIP, B_TRUE, B_FALSE);
        }
    }
    if (sts == AM_SUCCESS) {
        am_web_log_debug("%s: IP address used for request evaluation: %s",
                thisfunc, requestIP);
        sts = am_map_create(&request_headers_map);
    }
    if (sts == AM_SUCCESS) {
        void *args[3] = {context, rawRequest, request_headers_map};
        // Get the query
        char *query = strchr(request_url, '?');
        if (query != NULL) {
            query++;
            am_web_log_debug("%s: QUERY = %s", thisfunc, query);
        }
        // Set request info needed for processing access check.
        req_params.url = request_url;
        req_params.query = query;
        req_params.method = method;
        req_params.client_ip = requestIP;
        req_params.client_hostname = clientHostname;
        req_params.path_info = path_info;
        req_params.cookie_header_val = cookie_header_val;
        // Set functions for processing access check result.
        req_funcs.get_post_data.func = get_post_data;
        req_funcs.get_post_data.args = args;
        // no need to free post data
        req_funcs.free_post_data.func = NULL;
        req_funcs.free_post_data.args = NULL;
        req_funcs.set_user.func = set_user;
        req_funcs.set_user.args = args;
        req_funcs.set_method.func = set_method;
        req_funcs.set_method.args = args;
        req_funcs.render_result.func = render_result;
        req_funcs.render_result.args = args;
        req_funcs.set_header_in_request.func = set_header_in_request;
        req_funcs.set_header_in_request.args = args;
        // not currently used
        req_funcs.add_header_in_response.func = add_header_in_response;
        req_funcs.add_header_in_response.args = args;
        // Process request access check
        result = am_web_process_request(&req_params, &req_funcs,
                &render_sts, agent_config);
        if (render_sts != AM_SUCCESS) {
            retVal = kFilterError;
        } else {
            switch (result) {
                case AM_WEB_RESULT_OK:
                    // see comments in set_header_in_request for why
                    // this is needed.
                    (void) set_all_request_headers(args);
                    // done
                    retVal = kFilterHandledEvent;
                    break;
                case AM_WEB_RESULT_OK_DONE:
                case AM_WEB_RESULT_FORBIDDEN:
                case AM_WEB_RESULT_REDIRECT:
                    retVal = kFilterHandledRequest;
                    break;
                case AM_WEB_RESULT_ERROR:
                default:
                    retVal = kFilterError;
                    break;
            }
        }
    }
    // Cleaning
    if (clientIP != NULL) {
        am_web_free_memory(clientIP);
    }
    if (clientHostname != NULL) {
        am_web_free_memory(clientHostname);
    }
    if (request_headers_map != NULL) {
        am_map_destroy(request_headers_map);
    }
    // Handle error status
    if (sts != AM_SUCCESS) {
        (void) render_response(context, HTTP_500_INT_ERROR, NULL, &retVal);
    }

    return retVal;
}

/**
 * Called on the kFilterAuthenticate event.
 * Set domino user to the user authenticated in filterRawRequest.
 */
static unsigned int
filterAuthenticate(FilterContext *context, FilterAuthenticate *fAuthN) {
    const char *thisfunc = "filterAuthenticate()";
    unsigned int retVal = kFilterError;
    private_context_t *priv_ctx = NULL;

    // Check input arguments
    if (context == NULL || fAuthN == NULL) {
        am_web_log_error("%s: One of input arguments is NULL.", thisfunc);
        if (fAuthN != NULL) {
            fAuthN->authType = kNotAuthentic;
            fAuthN->authName = "";
        }
        (void) render_response(context, HTTP_500_INT_ERROR, NULL, &retVal);

        // Get the result and user that was authenticated in filterRawRequest
    } else if ((priv_ctx = (private_context_t *) context->privateContext) == NULL ||
            priv_ctx->result != AM_WEB_RESULT_OK) {
        // if we get to this function, result should've been ok,
        // and private context should not be null.
        // however, if it is for some reason, render internal error.
        // Note that user could be null if there was no sso token and
        // the url is not enforced. in that case let domino decide
        // whether or not to allow access.
        am_web_log_error("%s: Error encountered: private context is NULL or "
                "result %s is not 200 OK.", thisfunc,
                priv_ctx == NULL ? "" :
                am_web_result_num_to_str(priv_ctx->result));
        fAuthN->authType = kNotAuthentic;
        fAuthN->authName = "";
        (void) render_response(context, HTTP_500_INT_ERROR, NULL, &retVal);

        // everything OK
    } else {
        if (priv_ctx->user == NULL || priv_ctx->user[0] == '\0') {
            am_web_log_warning("%s: setting user to empty string in private context.",
                    thisfunc);
            priv_ctx->user = "";
        }
        am_web_log_debug("%s: setting user to %s.",
                thisfunc, priv_ctx->user);
        fAuthN->authName = priv_ctx->user;
        fAuthN->authType = kAuthenticBasic;
        retVal = kFilterHandledEvent;
    }
    return retVal;
}

/**
 * Gets called on kFilterAuthorize event.
 * Returns isAuthorized if user access was OK in filterRawRequest().
 */
static unsigned int
filterAuthorize(FilterContext *context, FilterAuthorize *fAuthZ) {
    const char *thisfunc = "filterAuthorize()";
    unsigned int retVal = kFilterError;
    private_context_t *priv_ctx = NULL;

    // Check arguments
    if (context == NULL || fAuthZ == NULL) {
        am_web_log_error("%s: One of input arguments is NULL.", thisfunc);
        if (fAuthZ != NULL)
            fAuthZ->isAuthorized = 0;
        (void) render_response(context, HTTP_500_INT_ERROR, NULL, &retVal);
    }// Get the result from filterRawRequest
    else if ((priv_ctx = (private_context_t *) context->privateContext) == NULL ||
            priv_ctx->result != AM_WEB_RESULT_OK) {
        // if we get to this function, result should've been ok,
        // but if it isn't for some reason, render internal error.
        am_web_log_error("%s: Error encountered: result %s is not 200 OK, "
                "or private context is NULL.",
                thisfunc,
                priv_ctx == NULL ? "<no-result-found>" :
                am_web_result_num_to_str(priv_ctx->result));
        fAuthZ->isAuthorized = 0;
        (void) render_response(context, HTTP_500_INT_ERROR, NULL, &retVal);
    } else {
        fAuthZ->isAuthorized = 1;
        am_web_log_debug("%s: request authorized.", thisfunc);
        retVal = kFilterHandledEvent;
    }

    return retVal;
}


#include <sys/types.h> 
#include <sys/stat.h>
#define AGENT_CONFIG_PATH_FILE "dsame.conf"

/*
 * Description:  This routine is called to get paths of agent's 
 *               config and bootstrap files from file dsame.conf.
 *
 */
static am_status_t get_properties_file_path() {
    FILE *fp;
    struct stat stat_buf;
    char *read_str;
    char *__return_str;
    long read_buf_sz = 0;
    int continue_loop = 1;
    int err_id, read_len;
    am_status_t sts = AM_FAILURE;

    err_id = stat(AGENT_CONFIG_PATH_FILE, &stat_buf);

    if (err_id) {
        return (sts);
    }

    read_buf_sz = stat_buf.st_size;
    ++read_buf_sz;

    fp = fopen(AGENT_CONFIG_PATH_FILE, "r");
    if (!fp) {
        return (sts);
    }

    read_str = (char *) malloc(read_buf_sz * sizeof (char));
    if (!read_str) {
        return (sts);
    }

    while (continue_loop) {
        memset(read_str, 0, read_buf_sz);
        if (!fgets(read_str, read_buf_sz, fp)) {
            break;
        }
        if (strstr(read_str, AGENT_CONFIG_FILE)) {
            read_len = strlen(read_str) + 1;
            __return_str = (char *) malloc(read_len * sizeof (char));
            if (!__return_str) {
                free(read_str);
                return (sts);
            }
            strcpy(__return_str, read_str + strlen(AGENT_CONFIG_FILE));
            __return_str[strlen(__return_str) - 1] = 0;
            agent_config.properties_file = __return_str;
        } else if (strstr(read_str, AGENT_BOOTSTRAP_FILE)) {
            read_len = strlen(read_str) + 1;
            __return_str = (char *) malloc(read_len * sizeof (char));
            if (!__return_str) {
                free(read_str);
                if (!agent_config.properties_file) {
                    free(agent_config.properties_file);
                }
                return (sts);
            }
            strcpy(__return_str, read_str + strlen(AGENT_BOOTSTRAP_FILE));
            __return_str[strlen(__return_str) - 1] = 0;
            agent_config.bootstrap_file = __return_str;
        }

    }
    fclose(fp);
    free(read_str);
    return (AM_SUCCESS);
}

/**
 * Gets called on the kFilterResponse event.
 * This is where response headers (such as Set-Cookie) is really set but
 * ONLY when the request is allowed. When it is forbidden or internal
 * error, kFilterResponse is called with a different context so we
 * cannot use private context to pass response headers to be set.
 * After response headers are set the headers map is destroyed.
 */
static unsigned int
filterResponse(FilterContext *context, FilterResponse *fResp) {
    const char *thisfunc = "filterResponse()";
    unsigned int retVal = kFilterHandledEvent;
    am_map_t headersMap = NULL;
    am_status_t sts = AM_SUCCESS;
    unsigned int max_buf_len = 0;

    if (context == NULL || fResp == NULL) {
        am_web_log_error("%s: Invalid parameters passed.", thisfunc);
        retVal = kFilterError;
    } else if (context->privateContext == NULL) {
        am_web_log_warning("%s: No private context found.", thisfunc);
        retVal = kFilterHandledEvent;
    } else {
        private_context_t *
                priv_ctx = (private_context_t *) context->privateContext;
        headersMap = priv_ctx->response_headers_to_set;
        if (headersMap == NULL) {
            am_web_log_max_debug("%s: No headers handle found.", thisfunc);
            retVal = kFilterHandledEvent;
        } else if (am_map_size(headersMap) == 0) {
            am_web_log_max_debug("%s: No headers to be set.", thisfunc);
            retVal = kFilterHandledEvent;
        } else {
            // Calculate the length of the buffer required to store the headers
            void *args[] = {&max_buf_len};
            sts = am_map_for_each(headersMap, get_header_length, args);
            am_web_log_max_debug("%s: Header buffer length: %i", thisfunc, max_buf_len);
        }

        if (max_buf_len > 0) {
            char* headers_buf = NULL;
            size_t max_len = 0;
            size_t n_written = 0;
            void *args[3];
            unsigned int errID;

            headers_buf = malloc(max_buf_len);
            if (headers_buf == NULL) {
                am_web_log_error("%s: Could not allocate memory for headers_buf", thisfunc);
                retVal = kFilterError;
            } else {
                memset(headers_buf, 0, sizeof (headers_buf));
                max_len = sizeof (headers_buf) - strlen(HEADER_TEXT_END) - 1;
                args[0] = headers_buf;
                args[1] = &max_len;
                args[2] = &n_written;

                // response->AddHeader and response->SetHeader will both replace
                // a header if one is already there, and this does not work for
                // headers like "Set-Cookie" which can have multiple entries.
                // The following was found to work around this "feature" -
                // put all headers into the buffer passed to AddHeader(),
                // each seperated by \r\n.
                // The following call fills a buffer with a list of header:value
                // pairs seperated by a \r\n.
                sts = am_map_for_each(headersMap, add_headers, args);
                if (sts != AM_SUCCESS) {
                    am_web_log_error("%s: Error while "
                            "iterating over the header values: %s",
                            thisfunc, am_status_to_string(sts));
                } else {
                    am_web_log_max_debug("%s: Successfully set all headers.",
                            thisfunc);
                }

                // Now call AddHeader with list of headers.
                if (fResp->AddHeader(context, headers_buf, &errID) == FALSE) {
                    am_web_log_warning("%s: Error adding header %s: %s",
                            thisfunc, headers_buf,
                            domino_error_to_str(errID));
                } else {
                    am_web_log_debug("%s: Headers set: %s", thisfunc, headers_buf);
                }

                if (headers_buf != NULL) {
                    free(headers_buf);
                    headers_buf = NULL;
                }
                // whether or not response headers were set successfully,
                // the filter is handled.
                retVal = kFilterHandledEvent;
            }
        }

        if (headersMap != NULL) {
            am_map_destroy(headersMap);
            priv_ctx->response_headers_to_set = NULL;
            am_web_log_debug("%s: Destroyed response headers map", thisfunc);
        }
    }
    return retVal;
}

static am_status_t
init_agent() {
    const char *thisfunc = "init_agent()";
    am_status_t sts = AM_SUCCESS;

    //The following lines are added for the server restart bug
#if defined(WINNT)
    LoadLibrary("libnspr4.dll");
#endif
    //End of modifications for the server restart bug


    if ((sts = get_properties_file_path()) != AM_SUCCESS) {
        am_web_log_error(
                "%s: getting agent's config/bootstrap files' paths failed!",
                thisfunc);
    } else if ((sts = am_web_init(agent_config.bootstrap_file,
            agent_config.properties_file)) != AM_SUCCESS) {
        am_web_log_error("%s: am_web_init failed with %s.",
                thisfunc, am_status_to_string(sts));
    }
    if (sts == AM_SUCCESS) {
        am_web_log_debug("%s: successfully initialized.", thisfunc);
    }
    return sts;
}

/**
 * This function is invoked to initialize the agent
 * during the first request.
 */
void init_at_request() {
    am_status_t status;
    status = am_agent_init(&agentInitialized);
    if (status != AM_SUCCESS) {
        am_web_log_debug("Initialization of the agent failed: "
                "status = %s (%d)", am_status_to_string(status), status);
    }
}

unsigned int
FilterInit(FilterInitData *initData) {
    am_status_t sts = AM_FAILURE;
    unsigned int retVal = kFilterError;

    /*Required*/
    initData->appFilterVersion = kInterfaceVersion;

    /* Modify the following code to set the flags you want */
    initData->eventFlags = kFilterRawRequest |
            kFilterAuthenticate |
            kFilterAuthorized |
            kFilterResponse;

    /* Set a short description for your filter */
    strcpy(initData->filterDesc,
            "IS Authentication Filter for Lotus Domino Server R8");

    // initialize agent.
    sts = init_agent();
    if (sts != AM_SUCCESS) {
        am_web_log_debug("FilterInit(): init_agent failed with error %s",
                am_status_to_string(sts));
        retVal = kFilterError;
    } else {
        retVal = kFilterHandledEvent;
    }

    return retVal;
}

unsigned int
HttpFilterProc(FilterContext *context, unsigned int eventType,
        void *pEventData) {
    const char *thisfunc = "HttpFilterProc()";
    FilterReturnCode retVal = kFilterError;

    am_web_log_info("%s: Plugin called for event %s.",
            thisfunc, getEventTypeStr(eventType));
    if (context == NULL || pEventData == NULL) {
        am_web_log_error("%s: One of input arguments is NULL.",
                thisfunc);
        retVal = kFilterError;
    } else {
        switch (eventType) {
            case kFilterRawRequest:
                // where all of request access check takes place.
                retVal = filterRawRequest(context, (FilterRawRequest *) pEventData);
                break;
            case kFilterAuthenticate:
                // returns authenticated user name and type to domino
                retVal = filterAuthenticate(context,
                        (FilterAuthenticate *) pEventData);
                break;
            case kFilterAuthorized:
                // returns whether request is authorized to domino.
                retVal = filterAuthorize(context, (FilterAuthorize *) pEventData);
                break;
            case kFilterResponse:
                // set response headers
                retVal = filterResponse(context, (FilterResponse *) pEventData);
                break;
            default:
                am_web_log_error("%s: Unregistered event type invoked.", thisfunc);
                break;
        }
    }
    am_web_log_info("%s: Plugin call ended for event %s, "
            "return val %s.", thisfunc, getEventTypeStr(eventType),
            domino_return_code_to_str(retVal));
    return retVal;
}

unsigned int
TerminateFilter(unsigned int reserved) {
    if (agent_config.properties_file) {
        free(agent_config.properties_file);
        agent_config.properties_file = NULL;
    }
    if (agent_config.bootstrap_file) {
        free(agent_config.bootstrap_file);
        agent_config.bootstrap_file = NULL;
    }
    am_cleanup();
    return kFilterHandledEvent;
}

