/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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

#include <windows.h>
#include <httpext.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>
#include "am_web.h"
#include <nspr.h>
#include "IisAgent6.h"

const char REDIRECT_TEMPLATE[] = {
    "Location: %s\r\n"
    "Content-Length: 0\r\n"
    "\r\n"
};

const char REDIRECT_COOKIE_TEMPLATE[] = {
    "Location: %s\r\n"
    "%s"
    "Content-Length: 0\r\n"
    "\r\n"
};


/* Comment for both FORBIDDEN_MSG and INTERNAL_SERVER_ERROR_MSG:
 * Both these have string messages only because Netscape browsers
 * can be happy.  Otherwise, they throw up a dialog box saying
 * document contains no data.  Netscape 7 does not say anything
 * at all.
 */
const char FORBIDDEN_MSG[] = {
    "HTTP/1.1 403 Forbidden\r\n"
    "Content-Length: 13\r\n"
    "Content-Type: text/plain\r\n"
    "\r\n"
    "403 Forbidden"
};

const char INTERNAL_SERVER_ERROR_MSG[] = {
    "HTTP/1.1 500 Internal Server Error\r\n"
    "Content-Length: 25\r\n"
    "Content-Type: text/html\r\n"
    "\r\n"
    "500 Internal Server Error"
};

#define AGENT_DESCRIPTION   "Sun OpenSSO Policy Agent 3.0 for Microsoft IIS 6.0"
const CHAR agentDescription[]   = { AGENT_DESCRIPTION };
#define	MAGIC_STR		"sunpostpreserve"
#define	POST_PRESERVE_URI	"/dummypost/"MAGIC_STR
#define   EMPTY_STRING	""

// actually const. But API prototypes don't alow.
CHAR httpOk[]                   = "200 OK";
CHAR httpRedirect[]             = "302 Found";
CHAR httpBadRequest[]           = "400 Bad Request";
CHAR httpForbidden[]            = "403 Forbidden";
CHAR httpServerError[]          = "500 Internal Server Error";

const CHAR httpProtocol[]       = "http";
const CHAR httpVersion1_1[]     = "HTTP/1.1";
const CHAR httpsProtocol[]      = "https";
const CHAR httpProtocolDelimiter[]  = "://";
const CHAR pszLocalHost[]       = "localhost";
// Do not change. Used to see if port number needed to reconstructing URL.
const CHAR httpPortDefault[]        = "80";
const CHAR httpsPortDefault[]       = "443";
const CHAR httpPortDelimiter[]      = ":";
const CHAR pszLocation[]        = "Location: ";
const CHAR pszContentLengthNoBody[] = "Content-length: 0\r\n";
const CHAR pszCrlf[]            = "\r\n";
const CHAR pszEntityDelimiter[]     = "\r\n\r\n";
// Response to cache invalidation notification request.
//   I.e. UpdateAgentCacheServlet
const CHAR HTTP_RESPONSE_OK[]     = {
    "HTTP/1.1 200 OK\r\n"
    "Content-length: 2\r\n"
    "Content-type: text/plain\r\n\r\n"
    "OK" // case etc. CRITICAL, Exact match in Access Manager.
};
const CHAR contentLength[]      = "Content-Length:";
const DWORD cbCrlfLen           = 2; // strlen("\r\n")
      CHAR  pszHttpAuthHeaderName[] = "Authorization:"; // Really const.
const CHAR refererServlet[]     = "refererservlet";

// Responses the agent uses to requests.
typedef enum {aaDeny, aaAllow, aaLogin} tAgentAction;

tAgentConfig agentConfig;

BOOL readAgentConfigFile = FALSE;
CRITICAL_SECTION initLock;

typedef struct OphResources {
    CHAR* cookies;      // cookies in the request
    DWORD cbCookies;
    CHAR *url;          // Requested URL
    size_t cbUrl;
    am_policy_result_t result;
} tOphResources;

#define RESOURCE_INITIALIZER \
    { NULL, 0, NULL, 0, AM_POLICY_RESULT_INITIALIZER }


BOOL WINAPI GetExtensionVersion(HSE_VERSION_INFO * pVer)
{
   HMODULE      nsprHandle = NULL;

   // Initialize NSPR library
   PR_Init(PR_SYSTEM_THREAD, PR_PRIORITY_NORMAL, 0);
   nsprHandle = LoadLibrary("libnspr4.dll");


   pVer->dwExtensionVersion = MAKELONG(0, 1);   // Version 1.0

   // A brief one line description of the ISAPI extension
   strncpy(pVer->lpszExtensionDesc, agentDescription, HSE_MAX_EXT_DLL_NAME_LEN);

   InitializeCriticalSection(&initLock);

   return TRUE;
}

BOOL loadAgentPropertyFile(EXTENSION_CONTROL_BLOCK *pECB)
{
    BOOL  gotInstanceId = FALSE;
    CHAR  *instanceId =  NULL;
    DWORD instanceIdSize = 0;
    CHAR* propertiesFileFullPath  = NULL;
    am_status_t status = AM_SUCCESS;
    am_status_t polsPolicyStatus = AM_SUCCESS;
    BOOL         statusContinue      = FALSE;
    CHAR         debugMsg[2048]   = "";
    CHAR* agent_bootstrap_file  = NULL;
    CHAR* agent_config_file = NULL;    
    boolean_t agentInitialized = B_FALSE;

    // Init to NULL values until we read properties file.
    agentConfig.bAgentInitSuccess = FALSE; // assume Failure until success

    if ( pECB->GetServerVariable(pECB->ConnID, "INSTANCE_ID", NULL,
                                 &instanceIdSize) == FALSE ) {
       instanceId = malloc(instanceIdSize);
       if (instanceId != NULL) {
           gotInstanceId = pECB->GetServerVariable(pECB->ConnID,
                                   "INSTANCE_ID",
                               instanceId,
                               &instanceIdSize);
           if ((gotInstanceId == FALSE) || (instanceIdSize <= 0)) {
               sprintf(debugMsg,
                       "%d: Invalid Instance Id received",
                       instanceIdSize);
           status = AM_FAILURE;
           }
       } else {
            sprintf(debugMsg,
                    "%d: Invalid Instance Id received",
                    instanceIdSize);
            status = AM_NO_MEMORY;
       }
    }

    if (status == AM_SUCCESS) {
       if (iisaPropertiesFilePathGet(&agent_bootstrap_file, instanceId, TRUE)
                                     == FALSE){ 
            sprintf(debugMsg,"%s: iisaPropertiesFilePathGet() failed.", 
                    agentDescription);
            logPrimitive(debugMsg);
            free(agent_bootstrap_file);
            agent_bootstrap_file = NULL;
            SetLastError(IISA_ERROR_PROPERTIES_FILE_PATH_GET);
            return FALSE;
        }
    }


    if (iisaPropertiesFilePathGet(&agent_config_file, instanceId, FALSE)
                                     == FALSE) {
           sprintf(debugMsg, "%s: iisaPropertiesFilePathGet() returned failure",
                   agentDescription);
            logPrimitive(debugMsg);
            free(agent_config_file);
            agent_config_file = NULL;
           SetLastError(IISA_ERROR_PROPERTIES_FILE_PATH_GET);
           return FALSE;
       }

       // Initialize the OpenSSO Policy API
        polsPolicyStatus = am_web_init(agent_bootstrap_file, agent_config_file);
        free(agent_bootstrap_file);
        agent_bootstrap_file = NULL;
        free(agent_config_file);
        agent_config_file = NULL;

       if (AM_SUCCESS != polsPolicyStatus) {
         // Use logPrimitive() AND am_web_log_error() here since a policy_init()
         //   failure could mean am_web_log_error() isn't initialized.
         sprintf(debugMsg,
                 "%s: Initialization of the agent failed: status = %s (%d)",
                 agentDescription, am_status_to_string(polsPolicyStatus),
         polsPolicyStatus);
         logPrimitive(debugMsg);
         SetLastError(IISA_ERROR_INIT_POLICY);
         return FALSE;
       }

    status = am_agent_init(&agentInitialized);
       if (AM_SUCCESS != polsPolicyStatus) {
         sprintf(debugMsg, "%s: Initialization of the agent(am_agent_init) failed: status = %s (%d)",
                 agentDescription, am_status_to_string(polsPolicyStatus),
         polsPolicyStatus);
         logPrimitive(debugMsg);
         SetLastError(IISA_ERROR_INIT_POLICY);
         return FALSE;
       }

    if (instanceId != NULL) {
       free(instanceId);
    }

    // Record success initializing agent.
    agentConfig.bAgentInitSuccess = TRUE;
    return TRUE;
}

// Method to register POST data in agent cache
static am_status_t register_post_data(EXTENSION_CONTROL_BLOCK *pECB, 
                         char *url, const char *key, char* response,
                         void* agent_config)
{
    const char *thisfunc = "register_post_data()";
    am_web_postcache_data_t post_data;
    am_status_t status = AM_SUCCESS;

    post_data.value = response;
    post_data.url = url;
    am_web_log_debug("%s: Register POST data key :%s", thisfunc, key);
    if (am_web_postcache_insert(key,&post_data,agent_config) == B_FALSE){
        am_web_log_error("Register POST data insert into"
                         " hash table failed:%s",key);
        status = AM_FAILURE;
    }
    
    return status;
}

// Method to check and create post page
static am_status_t check_for_post_data(EXTENSION_CONTROL_BLOCK *pECB,
                                       char *requestURL, char **page,
                                       void *agent_config)
{
    const char *thisfunc = "check_for_post_data()";
    const char *post_data_query = NULL;
    am_web_postcache_data_t get_data = {NULL, NULL};
    const char *actionurl = NULL;
    const char *postdata_cache = NULL;
    am_status_t status = AM_SUCCESS;
    CHAR* buffer_page = NULL;
    *page = NULL;

    // Check if magic URI is present in the URL
    if(requestURL != NULL) {
        post_data_query = strstr(requestURL, POST_PRESERVE_URI);
        if (post_data_query != NULL) {
            post_data_query += strlen(POST_PRESERVE_URI);
       }
    }
    // If magic uri present search for corresponding value in hashtable
    if(post_data_query != NULL && strlen(post_data_query) > 0) {
        am_web_log_debug("%s: POST Magic Query Value: %s", 
                         thisfunc, post_data_query);
        if(am_web_postcache_lookup(post_data_query, &get_data, agent_config) == B_TRUE) {
            postdata_cache = get_data.value;
            actionurl = get_data.url;
            am_web_log_debug("%s: POST hashtable actionurl: %s", 
                             thisfunc, actionurl);
            // Create the post page
            buffer_page = am_web_create_post_page(post_data_query,
                                   postdata_cache,actionurl, agent_config);
            *page = strdup(buffer_page);
            if (*page == NULL) {
                am_web_log_error("%s: Not enough memory to allocate page");
                status = AM_NO_MEMORY;
            }
            am_web_postcache_data_cleanup(&get_data);
            if (buffer_page != NULL) {
                am_web_free_memory(buffer_page);
            }
        } else {
            am_web_log_error("%s: Found magic URI (%s) but entry is not in POST"
                           " hash table",thisfunc, post_data_query);
            status = AM_FAILURE;
       }
    }
    return status;
}

am_status_t get_post_data(EXTENSION_CONTROL_BLOCK *pECB, char **body)
{
    const char *thisfunc = "get_post_data()";
    am_status_t status = AM_SUCCESS;
    
    int totalBytesRecvd = pECB->cbTotalBytes;
    if (totalBytesRecvd > 0) {
        *body = (char *) malloc(totalBytesRecvd + 1);
        if (*body != NULL) {
            memset(*body, 0, sizeof(char) * totalBytesRecvd + 1);
            strncpy(*body, pECB->lpbData, totalBytesRecvd);
        } else {
            am_web_log_error("%s: Not enough memory 0x%x bytes.",
                              thisfunc, pECB->cbTotalBytes);
            status = AM_NO_MEMORY;
        }
    } else {
        am_web_log_warning("%s: This POST request has no post data.", thisfunc);
    }
    
    return status;
}

DWORD send_error(EXTENSION_CONTROL_BLOCK *pECB) 
{
    const char *thisfunc = "send_error()";
    const char *data = INTERNAL_SERVER_ERROR_MSG;
    size_t data_len = sizeof(INTERNAL_SERVER_ERROR_MSG) - 1;
    if ((pECB->WriteClient(pECB->ConnID, (LPVOID)data,
                     (LPDWORD)&data_len, (DWORD) 0))==FALSE)
    {
        am_web_log_error("%s: WriteClient did not succeed: "
                     "Attempted message = %s ", thisfunc, data);
    }
    return HSE_STATUS_SUCCESS_AND_KEEP_CONN;
}

// This function is called when the preserve post data feature is enabled
// to send the original request with the original post data after the 
// iPlanetDirectoryPro value has been obtained from the CDC servlet
DWORD send_post_data(EXTENSION_CONTROL_BLOCK *pECB, char *page, 
                     char *set_cookies_list)
{
    const char *thisfunc = "send_post_data()";
    size_t page_len = 0;
    size_t headers_len = 0;
    char *headers = NULL;
    HSE_SEND_HEADER_EX_INFO sendHdr;
    DWORD returnValue = HSE_STATUS_SUCCESS;
    const char post_headers_template[] = {
             "Content-Length: %d\r\n"
             "Content-Type: text/html\r\n"
             "\r\n"
    };
    const char post_headers_template_with_cookie[] = {
             "Content-Length: %d\r\n"
             "Content-Type: text/html\r\n"
             "%s"
             "\r\n"
    };

    // Prepare the headers 
    headers_len = strlen(post_headers_template) + 3;
    if (set_cookies_list != NULL && strlen(set_cookies_list) > 0) {
        headers_len += strlen(set_cookies_list);
    }
    headers = (char *) malloc(headers_len);
    if (headers == NULL) {
        //Send internal error message
        am_web_log_error("%s: Not enough memory to allocate headers.",
                         thisfunc);
        returnValue = send_error(pECB);
    } else {
        page_len = strlen(page);
        memset(headers, 0, headers_len);
        if (set_cookies_list != NULL && strlen(set_cookies_list) > 0) {
            sprintf(headers, post_headers_template_with_cookie, 
                    page_len, set_cookies_list);
        } else {
            sprintf(headers, post_headers_template, page_len);
        }
        am_web_log_debug("%s: Headers sent with post form:\n%s",
                          thisfunc, headers);
        am_web_log_debug("%s: Post form:\n%s", thisfunc, page);
        sendHdr.pszStatus = httpOk;
        sendHdr.pszHeader = headers;
        sendHdr.cchStatus = strlen(httpOk);
        sendHdr.cchHeader = strlen(headers);
        sendHdr.fKeepConn = TRUE;
        //Send the headers
        pECB->ServerSupportFunction(pECB->ConnID,
                           HSE_REQ_SEND_RESPONSE_HEADER_EX,
                           &sendHdr,
                           NULL,
                           NULL);
        //Send the post page
        if ((pECB->WriteClient(pECB->ConnID, (LPVOID)page,
                               (LPDWORD)&page_len, (DWORD)0))==FALSE) {
            am_web_log_error("%s: WriteClient did not succeed: "
                             "Attempted message = %s ", thisfunc, page);
        }
    }
    if (headers != NULL) { 
        free(headers);
        headers = NULL;
    }
    return returnValue;
}

// Set attributes as HTTP headers
static am_status_t set_header(const char *key, const char *values, void **args)
{
     am_status_t status = AM_SUCCESS;
     CHAR** ptr = NULL;
     CHAR* set_headers_list = NULL;

     if (key != NULL && args != NULL ) {
        EXTENSION_CONTROL_BLOCK *pECB = (EXTENSION_CONTROL_BLOCK *) args[0];
        int cookie_length = 0;
        char* httpHeaderName = NULL;
        char* tmpHeader = NULL;
        size_t header_length = 0;

        ptr = (CHAR **) args[1];
        set_headers_list = *ptr;

        if (pECB != NULL) {
          header_length = strlen(key) + strlen("\r\n") + 1;
          if (values != NULL) {
             header_length += strlen(values);
          }
          httpHeaderName = (char *) malloc(header_length + 1);
        } else {
          am_web_log_error("set_header(): Invalid EXTENSION_CONTROL_BLOCK");
          status = AM_INVALID_ARGUMENT;
        }

       if (status == AM_SUCCESS) {
          if (httpHeaderName != NULL) {
             memset(httpHeaderName, 0, sizeof(char) * (header_length + 1));
             strcpy(httpHeaderName, key);
             strcat(httpHeaderName, ":");
             if (values != NULL) {
                strcat(httpHeaderName, values);
             }
             strcat(httpHeaderName, "\r\n");

             if (set_headers_list == NULL) {
                set_headers_list = (char *) malloc(header_length + 1);
                if (set_headers_list != NULL) {
                    memset(set_headers_list, 0, sizeof(char) *
                                                header_length + 1);
                    strcpy(set_headers_list, httpHeaderName);
                } else {
                    am_web_log_error("set_header():Not enough memory 0x%x "
                             "bytes.",header_length + 1);
                    status = AM_NO_MEMORY;
                }
             } else {
                 tmpHeader = set_headers_list;
                 set_headers_list = (char *) malloc(strlen(tmpHeader) +
                                                    header_length + 1);
                 if (set_headers_list == NULL) {
                    am_web_log_error("set_header():Not enough memory 0x%x "
                             "bytes.",header_length + 1);
                    status = AM_NO_MEMORY;
                 } else {
                    memset(set_headers_list, 0, sizeof(set_headers_list));
                    strcpy(set_headers_list, tmpHeader);
                    strcat(set_headers_list, httpHeaderName);
                 }
              }
              free(httpHeaderName);
              if (tmpHeader) {
                free(tmpHeader);
                tmpHeader = NULL;
              }
            } else {
               am_web_log_error("set_header():Not enough memory 0x%x bytes.",
                                 cookie_length + 1);
               status = AM_NO_MEMORY;
            }
         }
       } else {
          am_web_log_error("set_header(): Invalid arguments obtained");
          status = AM_INVALID_ARGUMENT;
     }

     if (set_headers_list && set_headers_list[0] != '\0') {
        am_web_log_info("set_header():set_headers_list = %s", set_headers_list);
        *ptr = set_headers_list;
     }

     return status;
}

// Append set-cookie header in set_cookies_list variable
static am_status_t set_cookie(const char *header, void **args)
{
     const char *thisfunc = "set_cookie()";
     am_status_t status = AM_SUCCESS;
     CHAR** ptr = NULL;
     CHAR* set_cookies_list = NULL;

     if (header != NULL && args != NULL ) {
        EXTENSION_CONTROL_BLOCK *pECB = (EXTENSION_CONTROL_BLOCK *) args[0];
        size_t cookie_length = 0;
        char* cookieValue = NULL;
        char* tmpStr = NULL;

        ptr = (CHAR **) args[2];
        set_cookies_list = *ptr;

        if (pECB != NULL) {
            cookie_length = strlen("Set-Cookie:") + strlen(header)
                                            + strlen("\r\n");
            cookieValue = (char *) malloc(cookie_length + 1);
        } else {
          am_web_log_error("set_cookie(): Invalid EXTENSION_CONTROL_BLOCK");
          status = AM_INVALID_ARGUMENT;
        }

       if (status == AM_SUCCESS) {
          if (cookieValue != NULL) {
             sprintf(cookieValue, "Set-Cookie:%s\r\n", header);

             if (set_cookies_list == NULL) {
                set_cookies_list = (char *) malloc(cookie_length + 1);
                if (set_cookies_list != NULL) {
                    memset(set_cookies_list, 0, sizeof(char) *
                                        cookie_length + 1);
                    strcpy(set_cookies_list, cookieValue);
                } else {
                    am_web_log_error("%s:Not enough memory 0x%x "
                                   "bytes.",thisfunc, cookie_length + 1);
                    status = AM_NO_MEMORY;
                }
             } else {
                  tmpStr = set_cookies_list;
                  set_cookies_list = (char *) malloc(strlen(tmpStr) +
                                                     cookie_length + 1);
                  if (set_cookies_list == NULL) {
                    am_web_log_error("%s: Not enough memory 0x%x "
                                      "bytes.", thisfunc, cookie_length + 1);
                    status = AM_NO_MEMORY;
                  } else {
                     memset(set_cookies_list,0,sizeof(set_cookies_list));
                     strcpy(set_cookies_list,tmpStr);
                     strcat(set_cookies_list,cookieValue);
                  }
            }
            am_web_log_info("%s: Following header added to "
                            "set_cookies_list:\n%s", 
                            thisfunc, cookieValue);
            free(cookieValue);

            if (tmpStr) {
                free(tmpStr);
                tmpStr = NULL;
            }
          } else {
            am_web_log_error("%s: Not enough memory 0x%x bytes.",
                                  thisfunc, cookie_length + 1);
             status = AM_NO_MEMORY;
          }
       }
     } else {
            am_web_log_error("%s: Invalid arguments obtained", thisfunc);
          status = AM_INVALID_ARGUMENT;
     }

    if (set_cookies_list && set_cookies_list[0] != '\0') {
        am_web_log_info("%s:set_cookies_list = %s", thisfunc, set_cookies_list);
        *ptr = set_cookies_list;
    }

     return status;
}

static am_status_t set_header_attr_as_cookie(const char *header, void **args)
{
  return AM_SUCCESS;
}

static am_status_t get_cookie_sync(const char *cookieName,
                                   char** dpro_cookie,
                                   void **args)
{
   am_status_t status = AM_SUCCESS;
   return status;
}

static void set_method(void ** args, char * orig_req)
{
}

VOID WINAPI execute_orig_request(EXTENSION_CONTROL_BLOCK *pECB,
                      PVOID pContext,
                      DWORD cbIO,
                      DWORD dwError)
{
    HSE_EXEC_URL_STATUS execUrlStatus;
    CHAR        szStatus[32] = "";
    CHAR        szWin32Error[32] = "";
    BOOL                result;
    HSE_EXEC_URL_INFO   *pExecUrlInfo;

    pExecUrlInfo = (HSE_EXEC_URL_INFO *)pContext;

    //
    // Get the results of the child request and report it.
    //
    result = pECB->ServerSupportFunction(pECB->ConnID,
                                HSE_REQ_GET_EXEC_URL_STATUS,
                                &execUrlStatus,
                                NULL,
                                NULL );
    if ( result )
    {
        if ( execUrlStatus.uHttpSubStatus != 0 )
        {
            _snprintf( szStatus,
                   32,
                       "Child Status=%d.%d",
                       execUrlStatus.uHttpStatusCode,
                       execUrlStatus.uHttpSubStatus );
        }
        else
        {
            _snprintf( szStatus,
                   32,
                       "%d",
                       execUrlStatus.uHttpStatusCode );
        }

        szStatus[31] = '\0';

        if ( execUrlStatus.dwWin32Error != ERROR_SUCCESS )
        {
            am_web_log_error( szWin32Error,
                       16,
                       "ErrorCode=%d, ",
                       execUrlStatus.dwWin32Error );

            szWin32Error[31] = '\0';
        }
    }

    //
    // Clean up the context pointer
    //

    if ( pExecUrlInfo != NULL )
    {
        if (pExecUrlInfo->pUserInfo != NULL) {
            free(pExecUrlInfo->pUserInfo);
            pExecUrlInfo->pUserInfo = NULL;
        }
        free(pExecUrlInfo);
        pExecUrlInfo = NULL;
    }


    //
    // Notify IIS that we are done with this request
    //

    pECB->ServerSupportFunction(
        pECB->ConnID,
        HSE_REQ_DONE_WITH_SESSION,
        NULL,
        NULL,
        NULL
        );
}

DWORD process_original_url(EXTENSION_CONTROL_BLOCK *pECB,
               CHAR* requestURL,
               CHAR* orig_req_method,
               CHAR* request_hdrs,
			   tOphResources* pOphResources,
               void* agent_config)
{
    CHAR* authtype = NULL;

    HSE_EXEC_URL_INFO  *execUrlInfo  = NULL;
    execUrlInfo = (HSE_EXEC_URL_INFO *) malloc(sizeof(HSE_EXEC_URL_INFO));

    if (execUrlInfo == NULL) {
        am_web_log_error("process_original_url(): Error %d occurred during "
                         "creating HSE_EXEC_URL_INFO context. \r\n",
                         GetLastError());
        return HSE_STATUS_ERROR;
    } else {
        memset(execUrlInfo, 0, sizeof(execUrlInfo));
        execUrlInfo->pszUrl = NULL;          // Use original request URL
        if (orig_req_method != NULL) {
            //CDSSO mode(restore orig method)
            execUrlInfo->pszMethod = orig_req_method;
            //Remove the entity-body sent by the CDC servlet
            execUrlInfo->pEntity = "\0";
            am_web_log_debug("process_original_url(): CDSSO Mode - "
                             "method set back to original method (%s), "
                             "CDC servlet content deleted",orig_req_method);
        } else {
            execUrlInfo->pszMethod = NULL;       // Use original request method
            execUrlInfo->pEntity = NULL;         // Use original request entity
        }
        if (request_hdrs != NULL) {
           am_web_log_debug("process_original_url(): request_hdrs = %s",
                                                     request_hdrs);
           execUrlInfo->pszChildHeaders = request_hdrs; // Original and custom
                                                   // headers
        } else {
            execUrlInfo->pszChildHeaders = NULL;// Use original request headers
        }
        execUrlInfo->pUserInfo = NULL;       // Use original request user info
        if (pOphResources->result.remote_user != NULL) {
           // Set the remote user
           execUrlInfo->pUserInfo = malloc(sizeof(HSE_EXEC_URL_USER_INFO));
           if (execUrlInfo->pUserInfo != NULL) {
               memset(execUrlInfo->pUserInfo,0,sizeof(execUrlInfo->pUserInfo));
               execUrlInfo->pUserInfo->hImpersonationToken = NULL;
               execUrlInfo->pUserInfo->pszCustomUserName =
                                 (LPSTR)pOphResources->result.remote_user;
               authtype = am_web_get_authType(agent_config);
               if (authtype != NULL)
                   execUrlInfo->pUserInfo->pszCustomAuthType = authtype;
               else
                   execUrlInfo->pUserInfo->pszCustomAuthType = "dsame";
               am_web_log_debug("process_original_url(): Auth-Type set to %s",
                        execUrlInfo->pUserInfo->pszCustomAuthType);
           }
        }
    }

    //
    // Need to set the below flag to avoid recursion
    //

    execUrlInfo->dwExecUrlFlags = HSE_EXEC_URL_IGNORE_CURRENT_INTERCEPTOR;

    //
    // Associate the completion routine and the current URL with
    // this request.
    //

    if ( pECB->ServerSupportFunction( pECB->ConnID,
                                      HSE_REQ_IO_COMPLETION,
                                      execute_orig_request,
                                      0,
                                      (LPDWORD)execUrlInfo) == FALSE )
    {
        am_web_log_error("process_original_url(): Error %d occurred setting "
             "I/O completion.\r\n", GetLastError());

        if (execUrlInfo->pUserInfo != NULL) {
            free(execUrlInfo->pUserInfo);
            execUrlInfo->pUserInfo = NULL;
        }
        return HSE_STATUS_ERROR;
    }

    //
    // Execute child request
    //

    if ( pECB->ServerSupportFunction( pECB->ConnID,
                                      HSE_REQ_EXEC_URL,
                                      execUrlInfo,
                                      NULL,
                                      NULL ) == FALSE )
    {
        am_web_log_error("process_original_url(): Error %d occurred calling "
                     "HSE_REQ_EXEC_URL.\r\n", GetLastError() );

        if (execUrlInfo->pUserInfo != NULL) {
            free(execUrlInfo->pUserInfo);
            execUrlInfo->pUserInfo = NULL;
        }
        return HSE_STATUS_ERROR;
    }

    //
    // Return pending and let the completion clean up.
    //
    return HSE_STATUS_PENDING;
}

static DWORD do_redirect(EXTENSION_CONTROL_BLOCK *pECB,
             am_status_t status,
             am_policy_result_t *policy_result,
             const char *original_url,
             const char *method,
			 void** args,
             void* agent_config)
{
    const char *thisfunc = "do_redirect()";
    char *redirect_header = NULL;
    size_t redirect_hdr_len = 0;
    char *redirect_status = httpServerError;
    char *redirect_url = NULL;
    DWORD redirect_url_len = 0;
    HSE_SEND_HEADER_EX_INFO sendHdr;
    size_t advice_headers_len = 0;
    char *advice_headers = NULL;
    const char advice_headers_template[] = {
             "Content-Length: %d\r\n"
             "Content-Type: text/html\r\n"
             "\r\n"
    };

    am_status_t ret = AM_SUCCESS;
    const am_map_t advice_map = policy_result->advice_map;

    ret = am_web_get_url_to_redirect(status, advice_map, original_url,
				      method, AM_RESERVED,&redirect_url, agent_config);

    // Compute the length of the redirect response.  Using the size of
    // the format string overallocates by a couple of bytes, but that is
    // not a significant issue given the short life span of the allocation.
    switch(status) {
        case AM_ACCESS_DENIED:
        case AM_INVALID_SESSION:
        case AM_INVALID_FQDN_ACCESS:

        //Check whether policy advices exist. If exists send
        //the advice back to client
            if ((ret == AM_SUCCESS)  && (redirect_url != NULL) &&
                (policy_result->advice_string != NULL)) {
                
                // Composite advice is sent as a POST
                char *advice_txt = NULL;
                ret = am_web_build_advice_response(policy_result, redirect_url,
                         &advice_txt);
                am_web_log_debug("%s: policy status=%s, response[%s]", 
                           thisfunc, am_status_to_string(status), advice_txt);

                if(ret == AM_SUCCESS) {
                    size_t data_length = (advice_txt != NULL)?strlen(advice_txt):0;
                    if(data_length > 0) {
                        //Send the headers
                        advice_headers_len = strlen(advice_headers_template) + 3;
                        advice_headers = (char *) malloc(advice_headers_len);
                        if (advice_headers != NULL) {
                            memset(advice_headers, 0, advice_headers_len);
                            sprintf(advice_headers, advice_headers_template, data_length);
                            sendHdr.pszStatus = httpOk;
                            sendHdr.pszHeader = advice_headers;
                            sendHdr.cchStatus = strlen(httpOk);
                            sendHdr.cchHeader = strlen(advice_headers);
                            sendHdr.fKeepConn = FALSE;
                            pECB->ServerSupportFunction(pECB->ConnID,
                                HSE_REQ_SEND_RESPONSE_HEADER_EX,
                                &sendHdr,
                                NULL,
                                NULL);
                            //Send the advice
                            if ((pECB->WriteClient( pECB->ConnID, (LPVOID)advice_txt,
                                    (LPDWORD)&data_length, (DWORD)0))==FALSE) {
                                am_web_log_error("%s: WriteClient did not "
                                      "succeed sending policy advice: "
                                      "Attempted message = %s ", thisfunc, advice_txt);
                            }
                        } else {
                             am_web_log_error("%s: Not enough memory 0x%x bytes.",
                             thisfunc, advice_headers_len);
                        }
                    }

                } else {
                    am_web_log_error("%s: Error while building "
                                     "advice response body:%s",
                                     thisfunc, am_status_to_string(ret));
                }
                //no policy advices exist. proceed normally.
                    } else {
                if (ret == AM_SUCCESS && redirect_url != NULL) {
                    CHAR* set_cookies_list = *((CHAR**) args[2]);
                    am_web_log_debug("%s: policy status = %s, "
                           "redirection URL is %s",
                           thisfunc, am_status_to_string(status),
                           redirect_url);
                    if (set_cookies_list == NULL) {
                        redirect_hdr_len = sizeof(REDIRECT_TEMPLATE) +
                                                strlen(redirect_url);
                    } else {
                        redirect_hdr_len = sizeof(REDIRECT_COOKIE_TEMPLATE) +
                                strlen(redirect_url) +
                                strlen(set_cookies_list);
                    }

                    redirect_header = malloc(redirect_hdr_len + 1);
                    if (redirect_header != NULL) {
                        redirect_status = httpRedirect;
                        if (set_cookies_list == NULL) {
                            _snprintf(redirect_header, redirect_hdr_len,
                                   REDIRECT_TEMPLATE, redirect_url);
                        } else {
                            _snprintf(redirect_header, redirect_hdr_len,
                                 REDIRECT_COOKIE_TEMPLATE, redirect_url,
                                 set_cookies_list);
                            free(set_cookies_list);
                            set_cookies_list = NULL;
                        }
                        am_web_log_info("%s: redirect_header = %s",
                                        thisfunc, redirect_header);
                    } else {
                        am_web_log_error("%s: unable to allocate "
                                "%u bytes", thisfunc, redirect_hdr_len);
                    }
                } else {
                    if(status == AM_ACCESS_DENIED) {
                        // Only reason why we should be sending 403 forbidden.
                        // All other cases are non-deterministic.
                        redirect_status = httpForbidden;
                    }
                    am_web_log_error("%s: Error while calling "
                                "am_web_get_redirect_url(): status = %s",
                                thisfunc, am_status_to_string(ret));
                }

                if (redirect_status == httpRedirect) {
                    sendHdr.pszStatus = httpRedirect;
                    sendHdr.pszHeader = redirect_header;
                    sendHdr.cchStatus = strlen(httpRedirect);
                    sendHdr.cchHeader = strlen(redirect_header);
                    sendHdr.fKeepConn = FALSE;

                    pECB->ServerSupportFunction(pECB->ConnID,
                                        HSE_REQ_SEND_RESPONSE_HEADER_EX,
                                        &sendHdr,
                                        NULL,
                                        NULL);
                } else {
                    size_t data_len = sizeof(FORBIDDEN_MSG) - 1;
                    const char *data = FORBIDDEN_MSG;
                    if (redirect_status == httpServerError) {
                        data = INTERNAL_SERVER_ERROR_MSG;
                        data_len = sizeof(INTERNAL_SERVER_ERROR_MSG) - 1;
                    }
                    if ((pECB->WriteClient(pECB->ConnID, (LPVOID)data,
                                (LPDWORD)&data_len, (DWORD) 0))==FALSE) {
                        am_web_log_error("do_redirect() WriteClient did not "
                                 "succeed: Attempted message = %s ", data);
                    }
                }
                free(redirect_header);
            }

            if (redirect_url) {
                am_web_free_memory(redirect_url);
            }
            if (advice_headers) {
                free(advice_headers);
            }
            break;

        default:
            // All the default values are set to send 500 code.
            break;
    }
    return HSE_STATUS_SUCCESS;
}


am_status_t get_request_url(EXTENSION_CONTROL_BLOCK *pECB,
                      CHAR** requestURL, CHAR** pathInfo,
                      tOphResources* pOphResources,
                      void* agent_config)
{
    const char *thisfunc = "get_request_url()";

    CHAR *requestHostHeader = NULL;
    DWORD requestHostHeaderSize	= 0;
    BOOL gotRequestHost = FALSE;

    const CHAR* requestProtocol = NULL;
    CHAR  *requestProtocolType  = NULL;
    DWORD requestProtocolTypeSize = 0;
    BOOL  gotRequestProtocol = FALSE;

    CHAR  defaultPort[TCP_PORT_ASCII_SIZE_MAX + 1] = "";
    CHAR  requestPort[TCP_PORT_ASCII_SIZE_MAX + 1] = "";
    DWORD requestPortSize = sizeof requestPort;
    BOOL  gotRequestPort = FALSE;
    size_t portNumber = 0;

    CHAR* queryString = NULL;
    DWORD queryStringSize = 0;
    BOOL  gotQueryString = FALSE;

    CHAR* baseUrl = NULL;
    CHAR* colon_ptr = NULL;
    DWORD baseUrlLength = 0;
    BOOL  gotUrl = FALSE;
    CHAR* fullBaseUrl = NULL;
    size_t fullBaseUrlLength = 0;
    
    CHAR* path_info = NULL;
    DWORD pathInfoSize = 0;
    BOOL gotPathInfo = FALSE;
    CHAR* newPathInfo = NULL;
    CHAR* tmpPath = NULL; 

    BOOL gotScriptName = FALSE;
    CHAR *scriptName = NULL;
    DWORD scriptNameSize = 0;
    
    am_status_t status = AM_SUCCESS;

    // Check whether the request is http or https
    if ( pECB->GetServerVariable( pECB->ConnID, "HTTPS", NULL,
                                  &requestProtocolTypeSize ) == FALSE ) {
        if (requestProtocolTypeSize > 0) {
            requestProtocolType = malloc(requestProtocolTypeSize);
            if (requestProtocolType != NULL) {
                gotRequestProtocol = pECB->GetServerVariable(pECB->ConnID,
                                                 "HTTPS",
                                                 requestProtocolType,
                                                 &requestProtocolTypeSize);
                if ((gotRequestProtocol == FALSE) ||
                       (requestProtocolTypeSize <= 0)) {
                    am_web_log_error("%s: Unable to get protocol"
                                " type, gotRequestProtocol = %d, "
                                "requestProtocolType = %s, "
                                "requestProtocolTypeSize = %d",
                                thisfunc, gotRequestProtocol, 
                                requestProtocolType,
                                requestProtocolTypeSize);
                    status = AM_FAILURE;
                }
            } else {
                am_web_log_error("%s: Not enough memory 0x%x"
                    "bytes.", thisfunc, requestProtocolTypeSize);
                status = AM_NO_MEMORY;
            }
        }
    }

    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: requestProtocolType = %s",
                        thisfunc, requestProtocolType);
        if(strncmp(requestProtocolType,"on", 2) == 0) {
            requestProtocol = httpsProtocol;
            strcpy(defaultPort, httpsPortDefault);
        } else if(strncmp(requestProtocolType,"off", 3) == 0) {
            requestProtocol = httpProtocol;
            strcpy(defaultPort, httpPortDefault);
        }

        // Get the host name
        if ( pECB->GetServerVariable( pECB->ConnID, "HEADER_Host", NULL,
                                      &requestHostHeaderSize ) == FALSE ) {
            requestHostHeader = malloc(requestHostHeaderSize);
            if (requestHostHeader != NULL) {
                gotRequestHost = pECB->GetServerVariable(pECB->ConnID,
                                                      "HEADER_Host",
                                                      requestHostHeader,
                                                      &requestHostHeaderSize);
                if ((gotRequestHost == FALSE) || (requestHostHeaderSize <= 0)) {
                    am_web_log_error("%s: Unable to get Host name "
                                 "of request. errorHost = %d, "
                                 "RequestHostHeaderSize = %d",
                                 thisfunc, gotRequestHost, 
                                 requestHostHeaderSize);
                    status = AM_FAILURE;
                }
            } else {
                am_web_log_error("%s: Not enough memory 0x%x bytes.", 
                              thisfunc, requestHostHeaderSize);
                status = AM_NO_MEMORY;
            }
        }
    }

    if ((status == AM_SUCCESS) && (requestHostHeader != NULL)) {
        am_web_log_debug("%s: HEADER_Host = %s",
                         thisfunc, requestHostHeader);
        colon_ptr = strchr(requestHostHeader, ':');
        if (colon_ptr != NULL) {
            strncpy(requestPort, colon_ptr + 1, strlen(colon_ptr)-1);
        } else {
            // Get the port number from Server variable
            gotRequestPort = pECB->GetServerVariable(pECB->ConnID,
                                                    "SERVER_PORT",
                                                    requestPort,
                                                    &requestPortSize);
            if ((gotRequestPort == FALSE) || (requestPortSize <= 0)) {
                am_web_log_error("%s: Unable to get TCP port "
                                "GetServerVariable(SERVER_PORT) = %d, "
                                "requestPortSize = %d", thisfunc,
                                gotRequestPort, requestPortSize);
                status = AM_FAILURE;
            }
        }
    }

    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: SERVER_PORT = %s", thisfunc, requestPort);

        pOphResources->cbUrl = strlen(requestProtocol)          +
                               strlen(httpProtocolDelimiter)    +
                               strlen(requestHostHeader)        +
                               strlen(httpPortDelimiter)        +
                               strlen(requestPort)              +
                               URL_SIZE_MAX;
        pOphResources->url = malloc(pOphResources->cbUrl);
        if (pOphResources->url == NULL) {
            am_web_log_error("%s: Not enough memory"
                     "pOphResources->cbUrl", thisfunc);
            status = AM_NO_MEMORY;
        }
    }

    if (status == AM_SUCCESS) {
        strcpy(pOphResources->url, requestProtocol);
        strcat(pOphResources->url, httpProtocolDelimiter);
        strcat(pOphResources->url, requestHostHeader);

        // Add the port number if it's not the default HTTP(S) port and
        // there's no port delimiter in the Host: header indicating
        // that the port is not present in the Host: header.
        if (strstr(requestHostHeader, httpPortDelimiter) == NULL) {
            if (strcmp(requestPort, defaultPort) != 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, requestPort);
            // following 2 "else if" were added based on
            // instruction that port number has to be added for IIS
            } else if (strcmp(requestProtocol, httpProtocol) == 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, httpPortDefault);
            } else if (strcmp(requestProtocol, httpsProtocol) == 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, httpsPortDefault);
            }
        }

        //Get the base url
        if ( pECB->GetServerVariable( pECB->ConnID, "URL", NULL,
                                      &baseUrlLength ) == FALSE ) {
            if (baseUrlLength > 0) {
                baseUrl = malloc(baseUrlLength);
                if ( baseUrl != NULL ) {
                    gotUrl = pECB->GetServerVariable(pECB->ConnID, "URL",
                                          baseUrl, &baseUrlLength );
                    if ((gotUrl == FALSE) || (baseUrlLength <= 0)) {
                        am_web_log_error("%s: Unable to get base URL, "
                                         "gotUrl = %d, baseUrlLength = %d",
                                         thisfunc, gotUrl, baseUrlLength);
                        status = AM_FAILURE;
                    }
                } else {
                    am_web_log_error("%s: Not enough memory 0x%x"
                             "bytes.", thisfunc, baseUrlLength);
                    status = AM_NO_MEMORY;
                }
            }
        }
    }

    if (status == AM_SUCCESS) {
         am_web_log_debug("%s: URL = %s", thisfunc, baseUrl);
        
        // Get the path info .
        if (pECB->GetServerVariable(pECB->ConnID, "PATH_INFO", NULL,
                                     &pathInfoSize) == FALSE ) {
            path_info = malloc(pathInfoSize);
            if (path_info != NULL) {
                gotPathInfo = pECB->GetServerVariable(pECB->ConnID,
                                            "PATH_INFO",
                                            path_info,
                                            &pathInfoSize);
                if ((gotPathInfo == FALSE) || (pathInfoSize <= 0)) {
                    am_web_log_error("%s: Unable to get PATH_INFO,  "
                              "gotPathInfo= %d, pathInfoSize = %d",
                              thisfunc, gotPathInfo, pathInfoSize);
                    status = AM_FAILURE;
                }
            } else {
                am_web_log_error("%s: Unable to allocate memory for "
                                 "path_info.", thisfunc);
                status = AM_NO_MEMORY;
            }
        }
    }

    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: PATH_INFO = %s", thisfunc, path_info);

        // Get the script name
        if (pECB->GetServerVariable(pECB->ConnID, "SCRIPT_NAME", NULL,
                                     &scriptNameSize) == FALSE ) {
            scriptName = malloc(scriptNameSize);
            if (scriptName != NULL) {
                gotScriptName = pECB->GetServerVariable(pECB->ConnID,
                                                       "SCRIPT_NAME",
                                                       scriptName,
                                                       &scriptNameSize);
                if ((gotScriptName == FALSE) || (scriptNameSize <= 0)) {
                    am_web_log_error("%s: Unable to get SCRIPT_NAME, "
                                    "gotScriptName= %d, scriptNameSize = %d",
                                    thisfunc, gotScriptName, scriptNameSize);
                    status = AM_FAILURE;
                }
            } else {
                am_web_log_error("%s: Unable to allocate memory for "
                                 "scriptName.", thisfunc);
                status = AM_NO_MEMORY;
            }
        }
    }

    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: SCRIPT_NAME = %s",thisfunc, scriptName);
        
        //Remove the script name from path_info to get the real path info
        if (path_info != NULL && scriptName != NULL) {
            tmpPath = path_info + strlen(scriptName);
            newPathInfo = strdup(tmpPath);
            if (newPathInfo != NULL) {
                *pathInfo = newPathInfo;
                am_web_log_debug("%s: Reconstructed path info = %s",
                                 thisfunc, *pathInfo );
            } else {
               am_web_log_error("%s: Unable to allocate newPathInfo.",
                                   thisfunc);
               status = AM_NO_MEMORY;
            }
        }
    }
    
    if (status == AM_SUCCESS) {
        strcat(pOphResources->url, baseUrl);
        // Add the path info to the base url
        if ((newPathInfo != NULL) && (strlen(newPathInfo) > 0)) {
            strcat(pOphResources->url, newPathInfo);
        }

        // Get the query string
        if ( pECB->GetServerVariable( pECB->ConnID, "QUERY_STRING", NULL,
                                      &queryStringSize ) == FALSE ) {
            queryString = malloc(queryStringSize+1);
            if (queryString != NULL) {
                gotQueryString = pECB->GetServerVariable(pECB->ConnID,
                                                       "QUERY_STRING",
                                                       queryString,
                                                       &queryStringSize);
                if (queryString != NULL && strlen(queryString) > 0) {
                    am_web_log_debug("%s: QUERY_STRING = %s",
                                     thisfunc, queryString);
                    queryString[queryStringSize] = '\0';
                    // Add the query string to the url
                    strcat(pOphResources->url, "?");
                    strcat(pOphResources->url, queryString);
                }
            } else {
                am_web_log_error("%s: Not enough memory 0x%x"
                        "bytes.", thisfunc, queryStringSize);
                status = AM_NO_MEMORY;
            }
        }
        portNumber = atoi(requestPort);
        // Check if we have to add path info to the base url
        if ((newPathInfo != NULL) && (strlen(newPathInfo) > 0)) {
            fullBaseUrlLength = baseUrlLength + strlen(newPathInfo);
            fullBaseUrl = (char *) malloc(fullBaseUrlLength + 1);
            if(fullBaseUrl != NULL) {
               memset(fullBaseUrl, 0, sizeof(char) * (fullBaseUrlLength + 1));
               strcpy(fullBaseUrl, baseUrl);
               strcat(fullBaseUrl, newPathInfo); 
               status = am_web_get_request_url(requestHostHeader, requestProtocol,
                       NULL, portNumber, fullBaseUrl, queryString, requestURL,
                       agent_config);
            } else {
               am_web_log_error("%s: Unable to allocate memory for "
                                "fullBaseUrl.", thisfunc);
               status = AM_NO_MEMORY;
            }
        } else {
            status = am_web_get_request_url(requestHostHeader, requestProtocol,
                       NULL, portNumber, baseUrl, queryString, requestURL,
                       agent_config);
        }
        if(status == AM_SUCCESS) {
            am_web_log_debug("%s: Constructed request url: %s", thisfunc, *requestURL);
        } else {
            am_web_log_error("%s: Failed with error: %s.",
                        thisfunc, am_status_to_string(status)); 
        }
    }

    if (requestProtocolType != NULL) {
        free(requestProtocolType);
        requestProtocolType = NULL;
    }
    if (requestHostHeader != NULL) {
        free(requestHostHeader);
        requestHostHeader = NULL;
    }
    if (baseUrl != NULL) {
        free(baseUrl);
        baseUrl = NULL;
    }
    if (path_info != NULL) {
        free(path_info);
        path_info = NULL;
    }
    if (scriptName != NULL) {
        free(scriptName);
        scriptName = NULL;
    }
    if (queryString != NULL) {
        free(queryString);
        queryString = NULL;
    }
    if (fullBaseUrl != NULL) {
        free(fullBaseUrl);
        fullBaseUrl = NULL;
    }

    return status;
}


/* 
 * This function checks if the profile attribute key is in the original 
 * request headers. If it is remove it in order to avoid tampering.
 */
am_status_t remove_key_in_headers(char* key, char** httpHeaders)
{
    const char *thisfunc = "remove_custom_attribute_in_header()";
    am_status_t status = AM_SUCCESS;
    CHAR* pStartHdr =NULL;
    CHAR* pEndHdr =NULL;    
    CHAR* tmpHdr=NULL;  
    size_t len;
    
    pStartHdr = string_case_insensitive_search(*httpHeaders,key);
    if (pStartHdr != NULL) {
        tmpHdr = malloc(strlen(*httpHeaders) + 1);
        if (tmpHdr != NULL) {
            memset(tmpHdr,0,strlen(*httpHeaders) + 1);
            len = strlen(*httpHeaders) - strlen(pStartHdr);
            strncpy(tmpHdr,*httpHeaders,len);
            pEndHdr = strstr(pStartHdr,pszCrlf);
            if (pEndHdr != NULL) {
                pEndHdr = pEndHdr + 2;
                strcat(tmpHdr,pEndHdr);
            }
            am_web_log_info("%s: Attribute %s was found and removed from "
                                "the original request headers.",thisfunc, key);
        } else {
            am_web_log_error("%s: Not enough memory to allocate tmpHdr.",
                             thisfunc);
            status = AM_NO_MEMORY;
        }
    }
    if (tmpHdr != NULL) {
        memset(*httpHeaders,0,strlen(*httpHeaders) + 1);
        strcpy(*httpHeaders,tmpHdr);
        free(tmpHdr);
        tmpHdr = NULL;
    }
    
    return status;
}

am_status_t set_request_headers(EXTENSION_CONTROL_BLOCK *pECB,
                                void** args, BOOL addOriginalHeaders)
{
    const char *thisfunc = "set_request_headers()";
    am_status_t status = AM_SUCCESS;
    am_status_t status1 = AM_SUCCESS;
    am_status_t status2 = AM_SUCCESS;
    CHAR* httpHeaders = NULL;
    BOOL gotHttpHeaders = FALSE;
    DWORD httpHeadersSize = 0;
    size_t http_headers_length = 0;
    CHAR* key = NULL;
    CHAR* pkeyStart = NULL;
    CHAR* tmpAttributeList = NULL;
    CHAR* pTemp = NULL;
    int i, j;
    int iKeyStart, keyLength;
    int iValueStart, iValueEnd, iHdrStart;
    BOOL isEmptyValue = FALSE;
    char *temp = NULL;

    if (pECB != NULL) {
        CHAR* set_headers_list = *((CHAR**) args[1]);
        CHAR* set_cookies_list = *((CHAR**) args[2]);
        CHAR** ptr = (CHAR **) args[3];
        CHAR* request_hdrs = *ptr;

        if (addOriginalHeaders == TRUE) {
        //Get the original headers from the request
        if (pECB->GetServerVariable(pECB->ConnID, "ALL_RAW", NULL,
                                      &httpHeadersSize) == FALSE ) {
            httpHeaders = malloc(httpHeadersSize);
            if (httpHeaders != NULL) {
                gotHttpHeaders = pECB->GetServerVariable(pECB->ConnID,
                                                          "ALL_RAW",
                                                          httpHeaders,
                                                          &httpHeadersSize);
                if (httpHeaders == NULL) {
                    am_web_log_error("%s: Unable to get http headers",thisfunc);
                    status = AM_FAILURE;
                } else {
                    am_web_log_debug("%s: Original headers:\n%s",
                                 thisfunc, httpHeaders);
                }
            } else {
                am_web_log_error("%s: Not enough memory "
                              "to allocate httpHeaders.", thisfunc);
                status = AM_NO_MEMORY;
            }
        } else {
             am_web_log_error("%s: Unable to get http "
                            "headers size", thisfunc);
             status = AM_FAILURE;
        }
        
        //Remove profile attributes from original request headers, if any,
        //to avoid tampering
        if ((status == AM_SUCCESS) && (set_headers_list != NULL)) {
            pkeyStart = set_headers_list;
            iKeyStart=0;
            for (i = 0; i < strlen(set_headers_list); ++i) {
                if (set_headers_list[i] == ':') {
                    keyLength = i + 1 - iKeyStart;
                    key = malloc(keyLength + 1);
                    if (key != NULL) {
                        memset(key,0,keyLength + 1);
                        strncpy (key,pkeyStart,keyLength);
                        if (strlen(key) > 0) {
                            status = remove_key_in_headers(key, &httpHeaders);
                        }
                        if((strchr(key, '-'))&&(strlen(key) > 0)) {
                            while(temp = strchr(key, '-')) {
                                if(temp == NULL) {
                                    break;
       			        }
			        key[temp-key]='_';
		            }
			    status1 = remove_key_in_headers(key, &httpHeaders);
                        }
                        if((strchr(key, '_'))&&(strlen(key) > 0)) {
			    while(temp = strchr(key, '_')) {
  			        if(temp==NULL) {
				    break;
				}
		                key[temp-key]='-';
			    }
			    status2 = remove_key_in_headers(key, &httpHeaders);
		        }
			if(status == AM_NO_MEMORY || status1 == AM_NO_MEMORY ||
			    status2 == AM_NO_MEMORY) {
			    status = AM_NO_MEMORY;
			}

                        free(key);
                        key = NULL;
                    } else {
                        am_web_log_error("%s:Not enough memory "
                                "to allocate key variable", thisfunc);
                        status = AM_NO_MEMORY;
                        break;
                    }
                    pkeyStart = set_headers_list;
                }
                if ((set_headers_list[i] == '\r') &&
                    (set_headers_list[i+1] == '\n')) {
                    iKeyStart = i+2;
                    pkeyStart = pkeyStart + i + 2;
                }
            }
        }
        //Remove empty values from set_headers_list 
        if ((status == AM_SUCCESS) && (set_headers_list != NULL)) {
            tmpAttributeList = malloc(strlen(set_headers_list)+1);
            if (tmpAttributeList != NULL) {
                memset(tmpAttributeList,0,strlen(set_headers_list)+1);
                strcpy(tmpAttributeList,set_headers_list);
                memset(set_headers_list,0,strlen(tmpAttributeList)+1);
                iValueStart = 0;
                iValueEnd = 0;
                for (i = 0; i < strlen(tmpAttributeList); ++i) {
                    if (tmpAttributeList[i] == ':') {
                        iValueStart = i + 1;
                    }
                    if ((tmpAttributeList[i] == '\r') &&
                         (tmpAttributeList[i+1] == '\n')) {
                        iHdrStart = iValueEnd;
                        iValueEnd = i;
                        isEmptyValue = TRUE;
                        if ((iValueStart > 0 ) && (iValueEnd > iValueStart)) {
                            for (j=iValueStart ; j < iValueEnd ; j++) {
                                if (tmpAttributeList[j] != ' ') {
                                    isEmptyValue = FALSE;
                                    break;
                                }
                            }
                        }
                        if (isEmptyValue == FALSE) {
                            for (j=iHdrStart ; j<iValueEnd ; j++) {
                                if ((tmpAttributeList[j] != '\r') &&
                                        (tmpAttributeList[j] != '\n')) {
                                    pTemp = tmpAttributeList + j;
                                    strncat(set_headers_list, pTemp, 1);
                                }
                            }
                            strcat(set_headers_list,pszCrlf);
                        }
                    }
                }
            } else {
                   am_web_log_error("%s:Not enough memory to allocate "
                                 "tmpAttributeList.", thisfunc);
                   status = AM_NO_MEMORY;
            }
            if (tmpAttributeList != NULL) {
                free(tmpAttributeList);
                tmpAttributeList = NULL;
            }
        }
        }

        //Add custom headers and/or set_cookie header to original headers
        if (status == AM_SUCCESS) {
            if (addOriginalHeaders == TRUE) {
                http_headers_length = strlen(httpHeaders);
            }
            if (set_headers_list != NULL) {
                http_headers_length = http_headers_length + 
                                strlen(set_headers_list);
            }
            if (set_cookies_list != NULL) {
                http_headers_length = http_headers_length +
                               strlen(set_cookies_list);
            }
            if (http_headers_length > 0) {
            request_hdrs = (char *)malloc(http_headers_length + 1);
            if (request_hdrs != NULL) {
                memset(request_hdrs,0, http_headers_length + 1);
                if (httpHeaders != NULL) {
                    strcpy(request_hdrs, httpHeaders);
                }
                if (set_headers_list != NULL) {
                    strcat(request_hdrs,set_headers_list);
                }
                if (set_cookies_list != NULL) {
                    strcat(request_hdrs,set_cookies_list);
                }
                *ptr = request_hdrs;
                    am_web_log_debug("%s: Final headers: %s",
                                     thisfunc, request_hdrs);
            } else {
                    am_web_log_error("%s: Not enough memory to allocate "
                                   "request_hdrs", thisfunc);
                status = AM_NO_MEMORY;
            }
        }
        }
        if (httpHeaders != NULL) {
            free(httpHeaders);
            httpHeaders = NULL;
        }
        if (set_headers_list != NULL) {
            free(set_headers_list);
            set_headers_list = NULL;
        }
    }
    
    return status;
}

void OphResourcesFree(tOphResources* pOphResources)
{
    if (pOphResources->cookies != NULL) {
        free(pOphResources->cookies);
        pOphResources->cookies   = NULL;
        pOphResources->cbCookies    = 0;
    }

    if (pOphResources->url != NULL) {
        free(pOphResources->url);
        pOphResources->url       = NULL;
        pOphResources->cbUrl        = 0;
    } 

    am_web_clear_attributes_map(&pOphResources->result);
    am_policy_result_destroy(&pOphResources->result);
    return;
}

static DWORD redirect_to_request_url(EXTENSION_CONTROL_BLOCK *pECB,
                                  const char *redirect_url, 
                                  const char *set_cookies_list)
{
    const char *thisfunc = "redirect_to_request_url()";
    char *redirect_header = NULL;
    size_t redirect_hdr_len = 0;
    char *redirect_status = httpServerError;
    HSE_SEND_HEADER_EX_INFO sendHdr;
    DWORD returnValue = HSE_STATUS_SUCCESS;
    
    // Build the redirect header
    if (redirect_url == NULL) {
        am_web_log_error("%s: redirect_url is NULL", thisfunc);
    } else {
        if (set_cookies_list == NULL) {
            redirect_hdr_len = sizeof(REDIRECT_TEMPLATE) +
                                      strlen(redirect_url);
        } else {
            redirect_hdr_len = sizeof(REDIRECT_COOKIE_TEMPLATE) +
                                strlen(redirect_url) +
                                strlen(set_cookies_list);
        }
        redirect_header = malloc(redirect_hdr_len + 1);
        if (redirect_header == NULL) {
            am_web_log_error("%s: Not enough memory for redirect_header",
                              thisfunc);
        } else {
            redirect_status = httpRedirect;
            if (set_cookies_list == NULL) {
                _snprintf(redirect_header, redirect_hdr_len,
                          REDIRECT_TEMPLATE, redirect_url);
            } else {
                _snprintf(redirect_header, redirect_hdr_len,
                        REDIRECT_COOKIE_TEMPLATE, redirect_url,
                        set_cookies_list);
            }

        }
    }

    // Send the request
    if (redirect_status == httpRedirect) {
        am_web_log_info("%s: redirect_header = %s", 
	                             thisfunc, redirect_header);
        sendHdr.pszStatus = httpRedirect;
        sendHdr.pszHeader = redirect_header;
        sendHdr.cchStatus = strlen(httpRedirect);
        sendHdr.cchHeader = strlen(redirect_header);
        sendHdr.fKeepConn = FALSE;
	
        pECB->ServerSupportFunction(pECB->ConnID,
                        HSE_REQ_SEND_RESPONSE_HEADER_EX,
                        &sendHdr,
                        NULL,
                        NULL);
    } else {
        size_t data_len = sizeof(FORBIDDEN_MSG) - 1;
        const char *data = FORBIDDEN_MSG;
        data = INTERNAL_SERVER_ERROR_MSG;
        data_len = sizeof(INTERNAL_SERVER_ERROR_MSG) - 1;
        if ((pECB->WriteClient(pECB->ConnID, (LPVOID)data,
                       (LPDWORD)&data_len, (DWORD) 0))==FALSE) {
            am_web_log_error("%s: WriteClient did not "
                        "succeed. Attempted message = %s ", thisfunc, data);
            returnValue = HSE_STATUS_ERROR;
        }
    }

    if (redirect_header != NULL) {
        free(redirect_header);
        redirect_header = NULL;
    }
    
    return returnValue;
}

DWORD send_ok(EXTENSION_CONTROL_BLOCK *pECB) 
{
    const char *thisfunc = "send_ok()";
    const char *data = HTTP_RESPONSE_OK;
    size_t data_len = sizeof(HTTP_RESPONSE_OK) - 1;
    if ((pECB->WriteClient(pECB->ConnID, (LPVOID)data,
                     (LPDWORD)&data_len, (DWORD) 0))==FALSE)
    {
        am_web_log_error("%s: WriteClient did not succeed: "
                     "Attempted message = %s ", thisfunc, data);
    }
    return HSE_STATUS_SUCCESS_AND_KEEP_CONN;
}

DWORD WINAPI HttpExtensionProc(EXTENSION_CONTROL_BLOCK *pECB)
{
    const char *thisfunc = "HttpExtensionProc()";
    CHAR* requestURL = NULL;
    CHAR* pathInfo = NULL;
    
    CHAR* dpro_cookie = NULL;
    am_status_t status = AM_SUCCESS;
    am_status_t status_tmp = AM_SUCCESS;
    DWORD returnValue = HSE_STATUS_SUCCESS;
    CHAR *set_cookies_list = NULL;
    CHAR *set_headers_list = NULL;
    CHAR *request_hdrs = NULL;

    void *args[] = {(void *) pECB, (void *) &set_headers_list,
                    (void *) &set_cookies_list, (void *) &request_hdrs };

    BOOL gotRequestMethod = FALSE;
    CHAR *requestMethod = NULL;
    DWORD requestMethodSize = 0;

    BOOL gotRequestClientIP = FALSE;
    CHAR *requestClientIP = NULL;
    DWORD requestClientIPSize = 0;
    
    am_map_t env_parameter_map = NULL;

    tOphResources OphResources = RESOURCE_INITIALIZER;
    tOphResources* pOphResources = &OphResources;

    CHAR* orig_req_method = NULL;
    CHAR* query = NULL;
    CHAR* response = NULL;
    BOOL fCookie = FALSE;
    DWORD cbCookiesLength = 0;
    CHAR* cookieValue = NULL;
    CHAR* post_page = NULL;    
    int length = 0;
    int i = 0;
    void *agent_config=NULL;
    BOOL isLocalAlloc = FALSE;
    BOOL redirectRequest = FALSE;
    char* logout_url = NULL;
    am_status_t cdStatus = AM_FAILURE; 
    char* cookie_name=NULL; 
    size_t cookie_header_len;
    char* cookie_header = NULL;

    char* ip_header = NULL;
    char* hostname_header = NULL;
    char* client_ip_from_ip_header = NULL;
    char* client_hostname_from_hostname_header = NULL;
    char* client_ip_header_name = NULL; 
    char* client_hostname_header_name = NULL; 
    DWORD client_ip_size=0;
    DWORD client_host_size=0;
    BOOL got_client_ip = FALSE;
    BOOL got_client_host = FALSE;

    // Load Agent Propeties file only once
    if (readAgentConfigFile == FALSE) {
        EnterCriticalSection(&initLock);
        if (readAgentConfigFile == FALSE) {
            loadAgentPropertyFile(pECB);
            readAgentConfigFile = TRUE;
        }
        LeaveCriticalSection(&initLock);
    }

    agent_config = am_web_get_agent_configuration();

    // Get ther request url and the path info
    status = get_request_url(pECB, &requestURL, &pathInfo, pOphResources, agent_config);

    // Check whether the url is a notification url
    if ((status == AM_SUCCESS) &&
         (B_TRUE == am_web_is_notification(requestURL, agent_config))) {
          const char* data = NULL;
          if (pECB->cbTotalBytes > 0) {
             data =  pECB->lpbData;
             am_web_handle_notification(data, pECB->cbTotalBytes, agent_config);
             OphResourcesFree(pOphResources);
             send_ok(pECB);
             return HSE_STATUS_SUCCESS_AND_KEEP_CONN;
          }
    }

    // Get the request method
    if (status == AM_SUCCESS) {
        if ( pECB->GetServerVariable( pECB->ConnID, "REQUEST_METHOD", NULL,
                                      &requestMethodSize ) == FALSE ) {

           requestMethod = malloc(requestMethodSize);
           if (requestMethod != NULL) {
              gotRequestMethod = pECB->GetServerVariable(pECB->ConnID,
                                                         "REQUEST_METHOD",
                                                         requestMethod,
                                                         &requestMethodSize);
              if ((gotRequestMethod == FALSE) || (requestMethodSize <= 0)) {
                 am_web_log_error("%s: Unable to get request "
                                   "method. GetHeader(method) = %d, "
                                   "requestMethodSize = %d", thisfunc, gotRequestMethod,
                                   requestMethodSize);
                  status = AM_FAILURE;
              }
           } else {
             am_web_log_error("%s:Not enough memory to allocate "
                                 "requestMethod.",thisfunc);
             status = AM_NO_MEMORY;
           }
        }
    }
    
    //Check if the SSO token is in the HTTP_COOKIE header
    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: requestMethod = %s",thisfunc, requestMethod);
        
        // Get the HTTP_COOKIE header
        pOphResources->cbCookies  = COOKIES_SIZE_MAX + 1;
        pOphResources->cookies = malloc(pOphResources->cbCookies);
        if (pOphResources->cookies != NULL) {
            memset(pOphResources->cookies,0,pOphResources->cbCookies);
            cbCookiesLength = pOphResources->cbCookies;
            fCookie = pECB->GetServerVariable(pECB->ConnID,
                                       "HTTP_COOKIE",
                                       pOphResources->cookies,
                                       &cbCookiesLength);
            if (fCookie  &&  cbCookiesLength > 0) {
                const char *cookieName = am_web_get_cookie_name(agent_config);
                
                // Look for the iPlanetDirectoryPro cookie
                if (cookieName != NULL) {
                    cookieValue = strstr(pOphResources->cookies, cookieName);
                    while (cookieValue) {
                        char *marker = strstr(cookieValue+1, cookieName);
                        if (marker) {
                            cookieValue = marker;
                        } else {
                            break;
                        }
                    }
                    if (cookieValue != NULL) {
                        cookieValue = strchr(cookieValue ,'=');
                        cookieValue = &cookieValue[1]; // 1 vs 0 skips over '='
                        // find the end of the cookie
                        length = 0;
                        for (i=0;(cookieValue[i] != ';') &&
                              (cookieValue[i] != '\0'); i++) {
                            length++;
                        }
                        cookieValue[length]='\0';
                        if (length < URL_SIZE_MAX-1) {
                            if (length > 0) {
                                dpro_cookie = malloc(length+1);
                                if (dpro_cookie != NULL) {
                                    strncpy(dpro_cookie, cookieValue, length);
                                    dpro_cookie[length] = '\0';
                                    isLocalAlloc = TRUE;
                                    am_web_log_debug("%s: SSO token found in "
                                          " cookie header.",
                                           thisfunc);
                                } else {
                                    am_web_log_error("%s: Unable to allocate "
                                            "memory for cookie, size = %u",
                                            thisfunc, length);
                                    status = AM_NO_MEMORY;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (status == AM_SUCCESS) {
        //Get the remote address
        if (pECB->GetServerVariable(pECB->ConnID, "REMOTE_ADDR", NULL,
                                     &requestClientIPSize ) == FALSE ) {
            requestClientIP = malloc(requestClientIPSize);
            if (requestClientIP != NULL) {
                gotRequestClientIP = pECB->GetServerVariable(pECB->ConnID,
                                                       "REMOTE_ADDR",
                                                       requestClientIP,
                                                       &requestClientIPSize);
                am_web_log_debug("%s: requestClientIP = %s",
                                 thisfunc, requestClientIP);
            } else {
                am_web_log_error("%s: Not enough memory 0x%x bytes.",
                         thisfunc, requestClientIPSize);
                status = AM_NO_MEMORY;
            }
        }
    }

    // If post preserve data is enabled, check if there is post data
    // in the post data cache
    if (status == AM_SUCCESS) {
        if (B_TRUE==am_web_is_postpreserve_enabled(agent_config)) {
            status = check_for_post_data(pECB, requestURL, &post_page, agent_config);
        }
    }

    //In CDSSO mode, check if the sso token is in the post data
    if (status == AM_SUCCESS) {
        if ((am_web_is_cdsso_enabled(agent_config) == B_TRUE) &&
                  (strcmp(requestMethod, REQUEST_METHOD_POST) == 0))
        {
            if (dpro_cookie == NULL &&
                 ((post_page != NULL) || 
                   am_web_is_url_enforced(requestURL, pathInfo, 
                   requestClientIP, agent_config) == B_TRUE))
            {
                status = get_post_data(pECB, &response);
                if (status == AM_SUCCESS) {
                        //Set original method to GET
                        orig_req_method = strdup(REQUEST_METHOD_GET);
                        if (orig_req_method != NULL) {
                            am_web_log_debug("%s: Request method set to GET.",
                                              thisfunc);
                        } else {
                            am_web_log_error("%s: Not enough memory to ",
                                    "allocate orig_req_method.", thisfunc);
                            status = AM_NO_MEMORY;
                        }
                    if (status == AM_SUCCESS) {
                        if(dpro_cookie != NULL) {
                            free(dpro_cookie);
                            dpro_cookie = NULL;
                        }
                        status = am_web_check_cookie_in_post(args, &dpro_cookie,
                                                &requestURL,
                                                &orig_req_method,
                                                requestMethod, response,
                                                B_FALSE, set_cookie,
                                                set_method, agent_config);
                        if (status == AM_SUCCESS) {
                            isLocalAlloc = FALSE;
                            am_web_log_debug("%s: SSO token found in "
                                             "assertion.",thisfunc);
                                redirectRequest = TRUE;
                        } else {
                            am_web_log_debug("%s: SSO token not found in "
                                   "assertion. Redirecting to login page.",
                                   thisfunc);
                            status = AM_INVALID_SESSION;
                        }
                    }
                }
            }
        }
    }

    if (status == AM_SUCCESS) {
        am_web_log_debug("%s: SSO token = %s", thisfunc, dpro_cookie);

        status = am_map_create(&env_parameter_map);
        am_web_log_debug("%s: status after "
                        "am_map_create = %s (%d)",thisfunc,
                        am_status_to_string(status),
                        status);
    }

    // Check if the user is authorized to access the resource.
    // This check is not necessary for the "/dummypost/sunpostpreserve" url.
    if ((status == AM_SUCCESS)) {
        
        client_ip_header_name = am_web_get_client_ip_header_name(agent_config);

        client_hostname_header_name = 
            am_web_get_client_hostname_header_name(agent_config);

        // If client ip header property is set, then try to
        // retrieve header value.
        if(client_ip_header_name != NULL && client_ip_header_name[0] != '\0') {

            get_header_value(pECB,client_ip_header_name, &ip_header);
            am_web_log_debug("%s: Header value retrived : %s",thisfunc, ip_header);
            am_web_get_client_ip(ip_header, &client_ip_from_ip_header);
        }

        // If client hostname header property is set, then try to retrieve header value.
        if(client_hostname_header_name != NULL && client_hostname_header_name[0] != '\0') {
            get_header_value(pECB,client_hostname_header_name, &hostname_header);
            am_web_get_client_hostname(hostname_header, 
                            &client_hostname_from_hostname_header);
        }

        // If client IP value is present from above processing, then
        // set it to env_param_map. Else use from request structure.
        if(client_ip_from_ip_header != NULL && client_ip_from_ip_header[0] != '\0') {
            am_web_set_host_ip_in_env_map(client_ip_from_ip_header,
                                  client_hostname_from_hostname_header,
                                  env_parameter_map,
                                  agent_config);

            status = am_web_is_access_allowed(dpro_cookie, requestURL,
                                        pathInfo, requestMethod,
                                        client_ip_from_ip_header,
                                        env_parameter_map,
                                        &OphResources.result,
                                        agent_config);
        } else {
            status = am_web_is_access_allowed(dpro_cookie, requestURL,
                                        pathInfo, requestMethod,
                                        (char *)requestClientIP,
                                        env_parameter_map,
                                        &OphResources.result,
                                        agent_config);
        }


        am_web_log_debug("%s: status after "
                         "am_web_is_access_allowed = %s (%d)",thisfunc,
                         am_status_to_string(status), status);
        am_map_destroy(env_parameter_map);
        am_web_free_memory(client_ip_from_ip_header);
        am_web_free_memory(client_hostname_from_hostname_header);

        if(ip_header !=NULL){
            free(ip_header);
            ip_header = NULL;
        }
        if(hostname_header != NULL){
            free(hostname_header);
            hostname_header = NULL;
        }
    }

    switch(status) {
        case AM_SUCCESS:
              if (am_web_is_logout_url(requestURL,agent_config) == B_TRUE) {
                 (void)am_web_logout_cookies_reset(set_cookie, args, agent_config);
            }
            // set user attributes to http header/cookies
            status_tmp = am_web_result_attr_map_set(&OphResources.result,
                                        set_header, set_cookie,
                                        set_header_attr_as_cookie,
                                        get_cookie_sync, args, agent_config);
            if (status_tmp == AM_SUCCESS) {
                // Set request headers
                if ((set_headers_list != NULL) || (set_cookies_list != NULL)) {
                    BOOL addOriginalHeaders;
                    if (redirectRequest == TRUE) {
                        addOriginalHeaders = FALSE;
                    } else {
                        addOriginalHeaders = TRUE;
                    }
                    status_tmp = set_request_headers(pECB, args, 
                                    addOriginalHeaders);
                }
            }
            if (status_tmp == AM_SUCCESS) {
                if (post_page != NULL) {
                    const char *lbCookieHeader = NULL;


                    // If post_ page is not null it means that the request 
                    // contains the "/dummypost/sunpostpreserve" string and
                    // that the data of the original request need to be posted
                    // If using the lb cookie, it needs to be reset to NULL there
                    status_tmp = am_web_get_postdata_preserve_lbcookie(
                                  &lbCookieHeader, B_TRUE, agent_config);
                    if (status_tmp == AM_SUCCESS) {
                        if (lbCookieHeader != NULL) {
                            am_web_log_debug("%s: Setting LB cookie for "
                                             "post data preservation to null",
                                             thisfunc);
                            set_cookie(lbCookieHeader, args);
                        }
                        returnValue = send_post_data(pECB, post_page, 
                                                 set_cookies_list);
                    } else {
                        am_web_log_error("%s: "
                              "am_web_get_postdata_preserve_lbcookie() "
                              "failed ", thisfunc);
                        returnValue = send_error(pECB);
                    }
                    if (lbCookieHeader != NULL) {
                        am_web_free_memory(lbCookieHeader);
                        lbCookieHeader = NULL;
                    }
                } else if (redirectRequest == TRUE) {
                    //If the property use.sunwmethod is set to false,
                    //the request method after authentication is GET
                    //The request can then be redirected to the original
                    //url instead of being handled by process_original_url().
                    //This avoids problems with ASP and perl.
                    am_web_log_debug("%s: Request redirected to orignal url after return "
                                     " from CDC servlet",thisfunc);
                    returnValue = redirect_to_request_url(pECB, 
                                  requestURL, request_hdrs);
                } else {
                    // If set_cookies_list is not empty, set the cookies 
                    // in the current response
                    if (set_cookies_list != NULL && strlen(set_cookies_list) > 0) {
                        HSE_SEND_HEADER_EX_INFO cookieResponseHdr;
                        cookieResponseHdr.pszStatus = NULL;
                        cookieResponseHdr.pszHeader = set_cookies_list;
                        cookieResponseHdr.cchStatus = 0;
                        cookieResponseHdr.cchHeader = strlen(set_cookies_list);
                        cookieResponseHdr.fKeepConn = TRUE;
                        pECB->ServerSupportFunction(pECB->ConnID,
                                          HSE_REQ_SEND_RESPONSE_HEADER_EX,
                                          &cookieResponseHdr,
                                          NULL,
                                          NULL);
                    }
                    returnValue = process_original_url(pECB, requestURL,
                                                       orig_req_method,
                                                        request_hdrs, pOphResources, 
                                                        agent_config);
                }
            }
            if (set_cookies_list != NULL) {
                free(set_cookies_list);
                set_cookies_list = NULL;
            }
            break;

        case AM_INVALID_SESSION:
            am_web_log_info("%s: Invalid session.",thisfunc);
            //first clear the stale cdsso cookies if any, in the browser
            if (am_web_is_cdsso_enabled(agent_config) == B_TRUE)
            {
                cdStatus = am_web_do_cookie_domain_set(set_cookie, args, EMPTY_STRING, agent_config);        
                if(cdStatus != AM_SUCCESS) {
                    am_web_log_error("%s : CDSSO reset cookie failed. ", thisfunc);
                }
            }

            am_web_do_cookies_reset(set_cookie, args, agent_config);
            // If the post data preservation feature is enabled
            // save the post data in the cache for post requests.
            if (strcmp(requestMethod, REQUEST_METHOD_POST) == 0 
                && B_TRUE==am_web_is_postpreserve_enabled(agent_config))
            {
                post_urls_t *post_urls = NULL;
                post_urls = am_web_create_post_preserve_urls(requestURL, agent_config);
                // In CDSSO mode, for a POST request, the post data have
                // already been saved in the response variable, so we need
                // to get them here only if response is NULL.
                if (response == NULL) {
                    status_tmp = get_post_data(pECB, &response);
                }                
                if (status_tmp == AM_SUCCESS) {
                    const char *lbCookieHeader = NULL;
                    if (response != NULL && strlen(response) > 0) {
                        if (AM_SUCCESS == register_post_data(pECB,post_urls->action_url,
                                          post_urls->post_time_key, response, agent_config)) 
                        {                            
                            // If using a LB in front of the agent, the LB cookie
                            // needs to be set there. The boolean argument allows
                            // to set the value of the cookie to the one defined in the
                            // properties file (B_FALSE) or to NULL (B_TRUE).
                            status_tmp = am_web_get_postdata_preserve_lbcookie(
                                                   &lbCookieHeader, B_FALSE, agent_config);
                            if (status_tmp == AM_SUCCESS) {
                                if (lbCookieHeader != NULL) {
                                    am_web_log_debug("%s: Setting LB cookie for post data "
                                             "preservation", thisfunc);
                                    set_cookie(lbCookieHeader, args);
                                }
                                returnValue = do_redirect(pECB, status, 
                                                      &pOphResources->result,
                                                      post_urls->dummy_url, 
                                                      requestMethod, args, 
                                                      agent_config);
                            } else {
                                am_web_log_error("%s: "
                                   "am_web_get_postdata_preserve_lbcookie() "
                                   "failed ", thisfunc);
                                returnValue = send_error(pECB);
                            }
                            if (lbCookieHeader != NULL) {
                                am_web_free_memory(lbCookieHeader);
                                lbCookieHeader = NULL;
                            }

                        } else {
                            returnValue = send_error(pECB);
                        }
                        if (post_urls != NULL) {
                            am_web_clean_post_urls(post_urls);
                            post_urls = NULL;
                        }
                    } else {
                        am_web_log_debug("%s: AM_INVALID_SESSION. This is a POST "
                                         "request with no post data => redirecting "
                                         "as a GET request.", thisfunc);
                        returnValue = do_redirect(pECB, status,
                                              &OphResources.result,
                                              requestURL, REQUEST_METHOD_GET, args,
                                              agent_config);
                    }
                } else {
                    returnValue = send_error(pECB);
                }
            } else {
                returnValue = do_redirect(pECB, status,
                                          &OphResources.result,
                                          requestURL, requestMethod, args,
                                          agent_config);
            }
            break;

        case AM_ACCESS_DENIED:
            am_web_log_info("%s: Access denied to %s",thisfunc,
                            OphResources.result.remote_user ?
                            OphResources.result.remote_user : "unknown user");
            //returnValue = do_redirect(pECB, status, &OphResources.result, requestURL, requestMethod, args, agent_config);

            if (strcmp(requestMethod, REQUEST_METHOD_POST) == 0 
                && B_TRUE==am_web_is_postpreserve_enabled(agent_config))
            {
                post_urls_t *post_urls = NULL;
                post_urls = am_web_create_post_preserve_urls(requestURL, agent_config);
                // In CDSSO mode, for a POST request, the post data have
                // already been saved in the response variable, so we need
                // to get them here only if response is NULL.
                if (response == NULL) {
                    status_tmp = get_post_data(pECB, &response);
                }                
                if (status_tmp == AM_SUCCESS) {
                    const char *lbCookieHeader = NULL;
                    if (response != NULL && strlen(response) > 0) {
                        if (AM_SUCCESS == register_post_data(pECB,post_urls->action_url,
                                          post_urls->post_time_key, response, agent_config)) 
                        {                            
                            // If using a LB in front of the agent, the LB cookie
                            // needs to be set there. The boolean argument allows
                            // to set the value of the cookie to the one defined in the
                            // properties file (B_FALSE) or to NULL (B_TRUE).
                            status_tmp = am_web_get_postdata_preserve_lbcookie(
                                                   &lbCookieHeader, B_FALSE, agent_config);
                            if (status_tmp == AM_SUCCESS) {
                                if (lbCookieHeader != NULL) {
                                    am_web_log_debug("%s: Setting LB cookie for post data "
                                             "preservation", thisfunc);
                                    set_cookie(lbCookieHeader, args);
                                }
                                returnValue = do_redirect(pECB, status, 
                                                      &pOphResources->result,
                                                      post_urls->dummy_url, 
                                                      requestMethod, args, 
                                                      agent_config);
                            } else {
                                am_web_log_error("%s: "
                                   "am_web_get_postdata_preserve_lbcookie() "
                                   "failed ", thisfunc);
                                returnValue = send_error(pECB);
                            }
                            if (lbCookieHeader != NULL) {
                                am_web_free_memory(lbCookieHeader);
                                lbCookieHeader = NULL;
                            }

                        } else {
                            returnValue = send_error(pECB);
                        }
                        if (post_urls != NULL) {
                            am_web_clean_post_urls(post_urls);
                            post_urls = NULL;
                        }
                    } else {
                        am_web_log_debug("%s: AM_INVALID_SESSION. This is a POST "
                                         "request with no post data => redirecting "
                                         "as a GET request.", thisfunc);
                        returnValue = do_redirect(pECB, status,
                                              &OphResources.result,
                                              requestURL, REQUEST_METHOD_GET, args,
                                              agent_config);
                    }
                } else {
                    returnValue = send_error(pECB);
                }
            } else {
                returnValue = do_redirect(pECB, status,
                                          &OphResources.result,
                                          requestURL, requestMethod, args,
                                          agent_config);
            }
            break;

        case AM_INVALID_FQDN_ACCESS:
            am_web_log_info("%s: Invalid FQDN access",thisfunc);
            returnValue = do_redirect(pECB, status,
                                        &OphResources.result,
                                        requestURL, requestMethod, args,
                                        agent_config);
           break;

        case AM_INVALID_ARGUMENT:
        case AM_NO_MEMORY:
        case AM_REDIRECT_LOGOUT:
            status = am_web_get_logout_url(&logout_url, agent_config);
            if(status == AM_SUCCESS) {
                returnValue = redirect_to_request_url(pECB, 
                                  logout_url, NULL);
            }
            else {
                returnValue = send_error(pECB);
                am_web_log_debug("validate_session_policy(): "
                    "am_web_get_logout_url failed. ");
            }
    break;
        case AM_FAILURE:
        default:
            am_web_log_error("%s: status: %s (%d)",thisfunc,
                              am_status_to_string(status), status);
            returnValue = send_error(pECB);
            break;
    }

    if(dpro_cookie != NULL) {
        if(isLocalAlloc) {
            free(dpro_cookie);
        } else {
            am_web_free_memory(dpro_cookie);
        }
        dpro_cookie = NULL;
    }
    if (orig_req_method != NULL) {
            free(orig_req_method);
    }
    if (pathInfo != NULL) {
        free(pathInfo);
        pathInfo = NULL;
    }
    if (requestMethod != NULL) {
        free(requestMethod);
        requestMethod = NULL;
    }
    if (requestClientIP != NULL) {
        free(requestClientIP);
        requestClientIP = NULL;
    }
    if (response != NULL) {
        free(response);
        response = NULL;
    }
    if (request_hdrs != NULL) {
        free(request_hdrs);
        request_hdrs = NULL;
    }
    if (post_page != NULL) {
        free(post_page);
        post_page = NULL;
    }
    
    am_web_free_memory(logout_url);
    am_web_free_memory(requestURL);

    OphResourcesFree(pOphResources);

    am_web_delete_agent_configuration(agent_config);

    return returnValue;
}


BOOL iisaPropertiesFilePathGet(CHAR** propertiesFileFullPath, char* instanceId,
        BOOL isBootStrapFile)
{
    // Max WINAPI path
    const DWORD dwPropertiesFileFullPathSize = MAX_PATH + 1;
    const CHAR  szPropertiesFileName[512]    = "";
    CHAR agentApplicationSubKey[1000]        = "";
    const CHAR agentDirectoryKeyName[]       = "Path";
    DWORD dwPropertiesFileFullPathLen        = dwPropertiesFileFullPathSize;
    HKEY hKey                                = NULL;
    LONG lRet                                = ERROR_SUCCESS;
    CHAR debugMsg[2048]                      = "";

    if(isBootStrapFile) {
        strcpy(szPropertiesFileName,"OpenSSOAgentBootstrap.properties");
    }
    else {
        strcpy(szPropertiesFileName,"OpenSSOAgentConfiguration.properties");
    }

    strcpy(agentApplicationSubKey,
        "Software\\Sun Microsystems\\OpenSSO IIS6 Agent\\Identifier_");
    if (instanceId != NULL) {
       strcat(agentApplicationSubKey,instanceId);
    }
    ///////////////////////////////////////////////////////////////////
    //  get the location of the properties file from the registry
    lRet = RegOpenKeyEx(HKEY_LOCAL_MACHINE, agentApplicationSubKey,
                        0, KEY_READ, &hKey);
    if(lRet != ERROR_SUCCESS) {
        sprintf(debugMsg,
                "%s(%d) Opening registry key %s%s failed with error code %d",
                __FILE__, __LINE__, "HKEY_LOCAL_MACHINE\\",
                agentApplicationSubKey, lRet);
        logPrimitive(debugMsg);
        return FALSE;
    }

    // free'd by caller, even when there's an error.
    *propertiesFileFullPath = (CHAR*) malloc(dwPropertiesFileFullPathLen);
    if (*propertiesFileFullPath == NULL) {
        sprintf(debugMsg,
              "%s(%d) Insufficient memory for propertiesFileFullPath %d bytes",
             __FILE__, __LINE__, dwPropertiesFileFullPathLen);
        logPrimitive(debugMsg);
        return FALSE;
    }
    lRet = RegQueryValueEx(hKey, agentDirectoryKeyName, NULL, NULL,
                           *propertiesFileFullPath,
                           &dwPropertiesFileFullPathLen);
    if (lRet != ERROR_SUCCESS || *propertiesFileFullPath == NULL ||
        (*propertiesFileFullPath)[0] == '\0') {
        sprintf(debugMsg,
          "%s(%d) Reading registry value %s\\%s\\%s failed with error code %d",
          __FILE__, __LINE__,
          "HKEY_LOCAL_MACHINE\\", agentApplicationSubKey,
          agentDirectoryKeyName, lRet);
        logPrimitive(debugMsg);
        return FALSE;
    }
    if (*propertiesFileFullPath &&
        (**propertiesFileFullPath == '\0')) {
        sprintf(debugMsg,
                "%s(%d) Properties file directory path is NULL.",
                __FILE__, __LINE__);
        logPrimitive(debugMsg);
        return FALSE;
    }
    if (*(*propertiesFileFullPath + dwPropertiesFileFullPathLen - 1) !=
        '\0') {
        sprintf(debugMsg,
             "%s(%d) Properties file directory path missing NULL termination.",
             __FILE__, __LINE__);
        logPrimitive(debugMsg);
        return FALSE;
    }
    // closes system registry
    RegCloseKey(hKey);
    if ((strlen(*propertiesFileFullPath) + 2 /* size of \\ */ +
         strlen(szPropertiesFileName) + 1) > dwPropertiesFileFullPathSize) {
        sprintf(debugMsg,
              "%s(%d) Properties file directory path exceeds Max WINAPI path.",
              __FILE__, __LINE__);
        logPrimitive(debugMsg);
        return FALSE;
    }
    strcat(*propertiesFileFullPath, "\\");
    strcat(*propertiesFileFullPath, szPropertiesFileName);
    return TRUE;
}

// Primitive error logger here that works before before policy_error() is
// initialized.
void logPrimitive(CHAR *message)
{
    HANDLE hes        = NULL;
    const CHAR* rsz[] = {message};

    if (message == NULL) {
    return;
    }

    hes = RegisterEventSource(0, agentDescription);
    if (hes) {
    ReportEvent(hes, EVENTLOG_ERROR_TYPE, 0, 0, 0, 1, 0, rsz, 0);
    DeregisterEventSource(hes);
    }
}

BOOL WINAPI TerminateExtension(DWORD dwFlags)
{
    am_status_t status = am_web_cleanup();
    DeleteCriticalSection(&initLock);
    return TRUE;
}

char* string_case_insensitive_search(char *HTTPHeaders, char *KeY)
{
    char *h, *n;
    if(!*KeY) {
        return HTTPHeaders;
    }
    for(; *HTTPHeaders; ++HTTPHeaders) {
        if(toupper(*HTTPHeaders) == toupper(*KeY)) {
            for(h=HTTPHeaders, n=KeY; *h && *n; ++h,++n) {
                if(toupper(*h)!=toupper(*n)) {
		    break;
		}
	    }
            if(!*n) {
	        return HTTPHeaders;
	    }
        }
    }
    return NULL;
}

/*
 * This function retrieves the value of a HTTP header. 
 * header_name is the name of the http header.
 * the value is assigned in header_value.
 * This value must be freed by the caller.
 * */
am_status_t get_header_value(EXTENSION_CONTROL_BLOCK *pECB, char* header_name, 
        char** header_value)
{
    const char *thisfunc = "get_header_value()";
    DWORD header_size = 0;
    BOOL got_header = FALSE;
    am_status_t status = AM_SUCCESS; 
    char* http_header = NULL;

    http_header = malloc(strlen("HTTP_")+strlen(header_name)+1);
    strcpy(http_header,"HTTP_");
    strcat(http_header,header_name);
    strcat(http_header,"\0");

    am_web_log_debug("%s: Header to be retrived : %s",thisfunc, http_header);

    if (pECB->GetServerVariable(pECB->ConnID, http_header, NULL, &header_size) == FALSE){
       *header_value = malloc(header_size);
       if (*header_value != NULL) {
           got_header = pECB->GetServerVariable(pECB->ConnID, http_header,
                               *header_value, &header_size);
           if ((got_header == FALSE) || (header_size <= 0)) {
                am_web_log_debug("%s: Invalid header received : %d",thisfunc,
                         header_size);
               status = AM_FAILURE;
           }
       } else {
            am_web_log_debug("%s: Header value alloc failed ",thisfunc);
            status = AM_NO_MEMORY;
       }
    }

    if(http_header != NULL){
        free(http_header);
        http_header = NULL;
    }
    return status;
}
