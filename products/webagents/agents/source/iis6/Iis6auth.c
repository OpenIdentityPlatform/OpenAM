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
#include <httpfilt.h>
#include <stdio.h>
#include <time.h>
#include <nspr.h>
#include <string.h>
#include <nss.h>
#include <pk11func.h>

#include <am_sso.h>
#include <am_web.h>

// Agent error codes to return to IIS on failure via SetLastError() in
// See WINERROR.H for format.
#define IISA_ERROR_BASE (3 << 30 | 1 << 29)
#define IISA_ERROR_GET_FILTER_VERSION (IISA_ERROR_BASE | 1 << 15)
#define IISA_ERROR_PROPERTIES_FILE_PATH_GET (IISA_ERROR_GET_FILTER_VERSION | 1)

#define TCP_PORT_ASCII_SIZE_MAX 5
#define URL_SIZE_MAX (20*1024)
#define NAME_SIZE_MAX (50)
#define COOKIES_SIZE_MAX (20*4096)
#define HTTP_UNAUTHORIZED 401

#define RESET_FILTER 0
#define REDIRECT_FORBIDDEN 1
#define REMOVE_DATA 2

typedef struct OphResources {
    CHAR* cookies;		// cookies in the request
    DWORD cbCookies;
    CHAR *url;			// Requested URL
    DWORD cbUrl;
} tOphResources;

#define RESOURCE_INITIALIZER \
    { NULL, 0, NULL, 0 }

typedef enum{
    FAILED = 0,
    SUCCESS = 1
} Status;

const CHAR agentDescription[] = "Authentication filter for IIS 6.0 Sun Policy Agent 2.2";
BOOL filterInitialized = FALSE;
CRITICAL_SECTION initLock;
CHAR debugMsg[2048] = "";

// actually const. But API prototypes don't alow.
CHAR httpOk[] = "200 OK";
CHAR httpRedirect[] = "302 Found";
CHAR httpBadRequest[] = "400 Bad Request";
CHAR httpForbidden[] = "403 Forbidden";
CHAR httpServerError[] = "500 Internal Server Error";

const CHAR httpProtocol[] = "http";
const CHAR httpVersion1_1[] = "HTTP/1.1";
const CHAR httpsProtocol[] = "https";
const CHAR httpProtocolDelimiter[] = "://";
const CHAR pszLocalHost[] = "localhost";
// Do not change. Used to see if port number needed to reconstructing URL.
const CHAR httpPortDefault[] = "80";
const CHAR httpsPortDefault[] = "443";
const CHAR httpPortDelimiter[] = ":";
const CHAR pszLocation[] = "Location: ";
const CHAR pszContentLengthNoBody[] = "Content-length: 0\r\n";
const CHAR pszCrlf[] = "\r\n";
const CHAR postMethod[] = "POST";
const CHAR getMethod[] = "GET";
const CHAR pszEntityDelimiter[] = "\r\n\r\n";
const CHAR exchangeSuffix[]="/exchange";
const CHAR owa_cookie[]="owaAuthCookie";
const CHAR *sharepoint_login_attr_value="sharepoint_login_attr_value";
const CHAR *sunIdentityUserPassword="sunIdentityUserPassword";

const char REDIRECT_TEMPLATE[] = {
    "Location: %s\r\n"
    "Content-Length: 0\r\n"
    "\r\n"
};

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

// ISAPI filter methods
BOOL WINAPI GetFilterVersion(HTTP_FILTER_VERSION *pVer);
DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc,
                            DWORD NotificationType, VOID *pvData);
BOOL WINAPI TerminateFilter(DWORD dwFlags);

// Other methods
DWORD OnPreprocHeaders(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_PREPROC_HEADERS *pvData);

DWORD OnSendResponse(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_SEND_RESPONSE *pHeaders);
DWORD OnSendRawData(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_RAW_DATA *pHeaders);
DWORD OnEndOfRequest(HTTP_FILTER_CONTEXT *pfc);

DWORD init_filter(PHTTP_FILTER_CONTEXT pfc);
am_status_t get_request_url(HTTP_FILTER_CONTEXT *pfc, tOphResources* pOphResources);
CHAR* get_cookie_value(HTTP_FILTER_CONTEXT *pfc,tOphResources* pOphResources,CHAR* cookieName);
DWORD do_redirect(HTTP_FILTER_CONTEXT *pfc, am_status_t status, const char *original_url, const char *method, BOOL sessionTimeOut);
void logMessage(CHAR *message);
void OphResourcesFree(tOphResources* pOphResources);
CHAR* get_decrypt_passwd(const char *encrypt_key, char *encrypt_passwd);


BOOL WINAPI GetFilterVersion(HTTP_FILTER_VERSION *pVer)
{
    HMODULE      nsprHandle = NULL;

    // Version 1.0
    // Initialize NSPR library
    PR_Init(PR_SYSTEM_THREAD, PR_PRIORITY_NORMAL, 0);
    nsprHandle = LoadLibrary("libnspr4.dll");

    pVer->dwFilterVersion = MAKELONG(0, 1);

    // Specify the types and order of notification
    pVer->dwFlags = (SF_NOTIFY_PREPROC_HEADERS | 
                     SF_NOTIFY_ORDER_HIGH | 
                     SF_NOTIFY_SEND_RESPONSE | 
                     SF_NOTIFY_SEND_RAW_DATA | 
                     SF_NOTIFY_END_OF_REQUEST);
	
    sprintf(pVer->lpszFilterDesc, agentDescription);
	InitializeCriticalSection(&initLock);
    return TRUE;
}

BOOL WINAPI TerminateFilter(DWORD dwFlags)
{
    am_web_cleanup();
    DeleteCriticalSection(&initLock);
    return TRUE;
}

DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD NotificationType,
                            VOID *pvData)
{
    DWORD dwRet;

    tOphResources OphResources = RESOURCE_INITIALIZER;
    tOphResources* pOphResources = &OphResources;
    CHAR* owa_auth_cookie=NULL;
    BOOL owaCookieFound=FALSE;
    BOOL isOwaEnabled = FALSE;
	isOwaEnabled = ((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
	
    switch (NotificationType) {
        case SF_NOTIFY_PREPROC_HEADERS:
            dwRet = OnPreprocHeaders(pfc,(PHTTP_FILTER_PREPROC_HEADERS) pvData);
            break;

        case SF_NOTIFY_SEND_RESPONSE:
            dwRet = OnSendResponse(pfc,(PHTTP_FILTER_SEND_RESPONSE) pvData);
            break;

        case SF_NOTIFY_SEND_RAW_DATA:
            dwRet = OnSendRawData(pfc,(PHTTP_FILTER_RAW_DATA) pvData);
            break;

        case SF_NOTIFY_END_OF_REQUEST:
            dwRet = OnEndOfRequest(pfc);
            break;
        default:            
            owa_auth_cookie = get_cookie_value(pfc,pOphResources,owa_cookie);
            // Check for the presence of owa cookie
            if (owa_auth_cookie == NULL){			 
                owaCookieFound=FALSE;
            } else {
                owaCookieFound=TRUE;
            }
            if (owaCookieFound && isOwaEnabled) {
                dwRet =  SF_STATUS_REQ_HANDLED_NOTIFICATION;
            } else {
                dwRet = SF_STATUS_REQ_NEXT_NOTIFICATION;
            }
            break;
    }
    if (owa_auth_cookie != NULL) {
        free(owa_auth_cookie);
        owa_auth_cookie = NULL;
    }	
    OphResourcesFree(pOphResources);
    return dwRet;
}

/**
 * OnPreprocHeaders checks if there is a iPlanetDirectoryProCookie.
 * If not, redirect the request to the AM login page.
 * Otherwise get the remote user and the password from the session
 * information and use them to set the Authorization header.
*/
DWORD OnPreprocHeaders(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_PREPROC_HEADERS *pHeaders)
{
    const char *thisfunc = "OnPreprocHeaders";
    am_status_t amStatus = AM_SUCCESS;
    DWORD returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION;
    Status status = SUCCESS;
    BOOL isInvalidSession = FALSE;
    CHAR* dpro_cookie = NULL;
    CHAR* owa_auth_cookie = NULL;
    CHAR *requestMethod = NULL;
    DWORD requestMethodSize = 0;
    BOOL gotRequestMethod = FALSE;
    BOOL invalidMethod = FALSE;
    BOOL gotPathInfo = FALSE;
    CHAR pathInfo[1000] = "";
    DWORD pathInfoSize = sizeof pathInfo;
    const CHAR  httpAuthType[] = "Basic ";
    CHAR  httpAuthHeaderName[] = "Authorization:";
    DWORD httpCipherTextSize = 0;
    CHAR* httpCipherText = NULL;
    DWORD httpAuthorityValueSize = 0;
    CHAR* httpAuthorityValue = NULL;
    const char *username = NULL;
    const char *password = NULL;
    const char *encrypt_key = NULL;
    char *decrypt_passwd = NULL;
    char tmp_password1[256];
	am_sso_token_handle_t sso_handle = NULL;
    tOphResources OphResources = RESOURCE_INITIALIZER;
    tOphResources* pOphResources = &OphResources;
    BOOL isInvalidAuthSession = FALSE;
    BOOL owaCookieFound = FALSE;
    BOOL isOwaEnabled = FALSE;
    CHAR* dpro_cookieName = NULL;
    BOOL isInvalidOwaCookie = FALSE;
    char *session_timeout_url = NULL;
    const char *userIdParam = NULL;
		
    if (!filterInitialized) {
		EnterCriticalSection(&initLock);
        // Initialize the filter and the amsdk
		if (!filterInitialized) {
			if (init_filter(pfc) == FAILED) {
				sprintf(debugMsg,"%s: ERROR - init_filter() failed. "
						"Filter is not active", thisfunc);
				logMessage(debugMsg);
				status = FAILED;
			} else {
				filterInitialized = TRUE;
			}
		}
		LeaveCriticalSection(&initLock);
    }

    if (status == SUCCESS) {
        isOwaEnabled = ((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
        // Get the request url
        amStatus = get_request_url(pfc,pOphResources);
        if (amStatus != AM_SUCCESS) {
            am_web_log_error("%s: get_request_url() failed. "
                             "Filter is not active.", thisfunc);
            status = FAILED;
        } 
    }

    if (status == SUCCESS) {
        // Get the request method
        if (pfc->GetServerVariable(pfc, "REQUEST_METHOD", NULL,
		                   &requestMethodSize ) == FALSE ) {
           requestMethod =(CHAR *) malloc(requestMethodSize);
           if (requestMethod != NULL) {
               gotRequestMethod = pfc->GetServerVariable(pfc,
					                "REQUEST_METHOD",
					                 requestMethod,
				                         &requestMethodSize);
               if ( !gotRequestMethod || (requestMethodSize <= 0)) {
                   am_web_log_error("%s: Unable to get request "
                                    "method. GetHeader(method) = %d, "
                                    "requestMethodSize = %d. "
                                    "Filter is not active.", thisfunc,
                                    gotRequestMethod, requestMethodSize);
                   status = FAILED;
               } else {
                     am_web_log_debug("%s: Request method is %s.", thisfunc, 
                                      requestMethod);
               }
           } else {
             am_web_log_error("%s: ERROR - : "
                              "Not enough memory for "
                              "requestMethod (0x%x bytes). "
                              "Filter is not active.",
                               thisfunc, requestMethodSize);
              status = FAILED;
           }
         } else {
             am_web_log_error("%s: Could not get requestMethodSize.", thisfunc);
             status = FAILED;
        }
    }

    // check whether the method name is GET or POST, if neither
    // it is an invalid method
    if ((requestMethod!=NULL) && (strcmp(requestMethod, "GET")) 
		                               && (strcmp(requestMethod, "POST"))) {
      am_web_log_debug("%s: Method other than GET or POST received,"
                        "method obtained is %s.", thisfunc, requestMethod);
      invalidMethod = TRUE;
    } 

    if (status == AM_SUCCESS) {
           gotPathInfo = pfc->GetServerVariable(pfc,
                                               "PATH_INFO",
                                               pathInfo,
                                               &pathInfoSize);
           if ( !gotPathInfo || (pathInfoSize <= 0)) {
               am_web_log_error("OnPreProcHeaders(): "
                                "Unable to get Path info. gotPathInfo= %d, "
                                "pathInfoSize = %d", gotPathInfo, pathInfoSize);
               status = FAILED;
           }
        }

	if (status == SUCCESS) {
        dpro_cookieName=am_web_get_cookie_name();
        dpro_cookie = get_cookie_value(pfc,pOphResources,dpro_cookieName);
        owa_auth_cookie=get_cookie_value(pfc,pOphResources,owa_cookie);
        isInvalidAuthSession = getIsInvalidAuthSession(pfc,&isInvalidOwaCookie,invalidMethod);
        if (isInvalidAuthSession) {				
            if (am_web_is_in_not_enforced_list(pOphResources->url,pathInfo)) {
                am_web_log_debug("%s: Incoming URL %s is in not enforced "
                                    "list,allowing access", thisfunc, 
                                    pOphResources->url);
                isInvalidSession = FALSE;
                session_timeout_url = am_web_is_owa_enabled_session_timeout_url();
                if ((owa_auth_cookie != NULL) && (session_timeout_url != NULL) &&
                    strcmp(pOphResources->url, session_timeout_url)) {
                        isInvalidSession = TRUE;
                    }
                }else if (!am_web_is_in_not_enforced_list(pOphResources->url,pathInfo)){
                    isInvalidSession = TRUE;
                }else if (status == SUCCESS) {
                    am_web_log_debug("%s: iPlanetDirectoryPro cookie null. "
				    "Redirecting to logging page.",thisfunc);
                    // Redirect to AM login page
                    isInvalidSession = TRUE;
                }
            } else {
                if (!isInvalidAuthSession) {
                    // Found iPlanetDirectoryPro cookie.
                    // Build new Authorization header.
                    am_web_log_debug("%s: iPlanetDirectoryPro cookie found: %s.",
                                thisfunc, dpro_cookie);

                    // Create sso token handle
                    amStatus = am_sso_create_sso_token_handle(&sso_handle,
                                                     dpro_cookie, B_FALSE);
			   
                    if (amStatus != AM_SUCCESS) {
                        if (amStatus == AM_INVALID_SESSION) {
                         am_web_log_error("%s: am_sso_create_sso_token_handle()"
                                  "returned invalid session. Redirecting to the "
                                  "login page : %s", thisfunc, 
                                  am_status_to_name(amStatus));
                            isInvalidSession = TRUE;
                    } else {
                        am_web_log_error("%s: am_sso_create_sso_token_handle() "
                                        "failed. %s", thisfunc, 
                                        am_status_to_name(amStatus));
                    }
                    status = FAILED;
                }
                if (status == SUCCESS) {
                    const char *userIdParam = NULL;

                    // First check for the sharepoint login attribute value
                    if (!isOwaEnabled) {
                        username = am_sso_get_property(sso_handle,
                                  sharepoint_login_attr_value,B_TRUE);
                    }
                    if (username == NULL) {
                        // Get the default value
                        userIdParam = am_web_get_user_id_param();

                        // Get username and password
                        username = am_sso_get_property(sso_handle, userIdParam, 
                                                  B_TRUE);
                        if ((username == NULL) || (strlen(username) == 0)) {
                            am_web_log_error("%s:  Username is null or "
                                       "session is invalid. Redirecting to "
                                       "logging page", thisfunc);
                            isInvalidSession = TRUE;
                            status = FAILED;
                        }
                    }
                    password = am_sso_get_property(sso_handle,
                                           sunIdentityUserPassword,B_TRUE);
                    if ((password == NULL) || (strlen(password) == 0)) {
                        am_web_log_error("%s: Password is null or "
                                         "session is invalid. ", thisfunc);
                        status = FAILED;
                    }
                }
                if (status == SUCCESS) {
                    memset(tmp_password1,0,256);
                    strncpy(tmp_password1,password,strlen(password)-1);
                    strcat(tmp_password1,"\0");
                    encrypt_key = am_web_get_iis6_replaypasswd_key();

                    decrypt_passwd = get_decrypt_passwd(encrypt_key,tmp_password1);
                    if (decrypt_passwd != NULL) {
                        // Allocate memory for the encrypted string 
                        // "username:password"
                        httpCipherTextSize = (strlen(username) + 1 + 
                                          strlen(decrypt_passwd))* 2 + 2 + 1;
                        httpCipherText =(CHAR *) malloc(httpCipherTextSize);
                        if (httpCipherText == NULL) {
                            am_web_log_error("%s: ERROR - Not enough memory "
                                        "for httpCipherText (%d bytes). ",
                                        thisfunc, httpCipherTextSize+1);
                            status = FAILED;
                        }
                    } else {
                        am_web_log_error("%s: decrypt_passwd returned an "
                                      "empty value", thisfunc);
                        status = FAILED;
                    }
                }
                if (status == SUCCESS) {
                    // Allocate memory for Authorization header
                    httpAuthorityValueSize = strlen(httpAuthType) + 
                                           httpCipherTextSize + 1;
                    httpAuthorityValue = (CHAR *) malloc(httpAuthorityValueSize);
                    if (httpAuthorityValue == NULL) {
                        am_web_log_error("%s: Not enough memory "
                                      "for httpAuthorityValue (%d bytes).",
                                      thisfunc, httpAuthorityValueSize+1);
                        status = FAILED;
                    }
                }
                if (status == SUCCESS) {
                    // Construct "u:p" plain text in httpAuthorityValue
                    sprintf(httpAuthorityValue, username);
                    strcat(httpAuthorityValue, ":");
                    strcat(httpAuthorityValue, decrypt_passwd);
                    // Encode "u:p"
                    encode_base64(httpAuthorityValue, strlen(httpAuthorityValue), httpCipherText);
                    // Prepare the authorization header: "Basic base64(user:pass)"
                    // Store it in httpAuthorityValue
                    sprintf(httpAuthorityValue, httpAuthType);
                    strcat(httpAuthorityValue, httpCipherText);
                    // Set the Authorization header
                    pHeaders->SetHeader(pfc, httpAuthHeaderName, 
                                           httpAuthorityValue);
                    am_web_log_debug("%s: Authorization header successfully set "
                                   "using user %s for request %s.",
                                   thisfunc,username,pOphResources->url);
                }
                // Clean the sso handle
                if (sso_handle != NULL) {
                    amStatus = am_sso_destroy_sso_token_handle(sso_handle);
                    if (amStatus != AM_SUCCESS) {
                        am_web_log_error("%s: am_sso_destroy_sso_token_handle() "
                                     "returned %s ", thisfunc, am_status_to_name(status));
                    }
                }
            }
        }
    }

    if ( isInvalidSession && !invalidMethod ) {
        // Redirect to AM login page
        // redirects to FQDN/exchange when the user was timed out in OWA
        // the 3rd condition verifies if the request is from a session timed out OWA-user
        if(isInvalidAuthSession && isOwaEnabled && (isInvalidOwaCookie)){
            returnValue = do_redirect(pfc, AM_INVALID_SESSION,
                                      pOphResources->url, requestMethod, TRUE);
        }
        else {
            returnValue = do_redirect(pfc, AM_INVALID_SESSION,
                                      pOphResources->url, requestMethod, FALSE);
        }		
    }

    // Free memory
    if (requestMethod != NULL) {
        free(requestMethod);
        requestMethod = NULL;
    }
    if (httpCipherText != NULL) {
        free(httpCipherText);
        httpCipherText = NULL;
    }
    if (httpAuthorityValue != NULL) {
        free(httpAuthorityValue);
        httpAuthorityValue = NULL;
    }
    if (dpro_cookie != NULL) {
        free(dpro_cookie);
        dpro_cookie = NULL;
    }
    
    // Check for the presence of owa cookie
    if(owa_auth_cookie == NULL){			 
        owaCookieFound=FALSE;
    } else {
        owaCookieFound=TRUE;
    }
	
    if(owaCookieFound && isOwaEnabled) {
        if(!isInvalidAuthSession) {
            if(returnValue==SF_STATUS_REQ_NEXT_NOTIFICATION) {
                returnValue=SF_STATUS_REQ_HANDLED_NOTIFICATION;				
            }
        }
        else {
            if(returnValue==SF_STATUS_REQ_HANDLED_NOTIFICATION){
                returnValue=SF_STATUS_REQ_NEXT_NOTIFICATION;				
            }
        }				
    }
	
    if (owa_auth_cookie != NULL) {
        free(owa_auth_cookie);
        owa_auth_cookie = NULL;
    }

    if (decrypt_passwd != NULL) {
        free(decrypt_passwd);
        decrypt_passwd = NULL;
    }

    OphResourcesFree(pOphResources);

    return returnValue;
}

/**
 * Initialize the filter
**/

DWORD init_filter(PHTTP_FILTER_CONTEXT pfc)
{
	const char *thisfunc = "init_filter";
	CHAR* propertiesFileFullPath = NULL;
	CHAR *instanceId = NULL;
	DWORD instanceIdSize = 0;
	BOOL gotInstanceId = FALSE;
	Status status = SUCCESS;
	am_properties_t prop = AM_PROPERTIES_NULL;

	am_status_t amStatus = AM_FAILURE;
	am_sso_token_handle_t sso_handle = NULL;

	//Get the instance ID for the agent
	if ( pfc->GetServerVariable(pfc, "INSTANCE_ID",
	                            NULL, &instanceIdSize) == FALSE ) {
                instanceId = (CHAR *)malloc(instanceIdSize);
                if (instanceId == NULL) {
                    sprintf(debugMsg,"%s: ERROR - "
                            "Not enough memory for instanceId (%d bytes)",
                             thisfunc, instanceIdSize);
                    logMessage(debugMsg);
                    status = FAILED;
		} else {
                  gotInstanceId = pfc->GetServerVariable(pfc,"INSTANCE_ID",
                                                         instanceId,
                                                         &instanceIdSize);
                  if ( !gotInstanceId || (instanceIdSize <= 0)) {
                       sprintf(debugMsg,"%s: ERROR - "
                               "Could not get Instance_ID variable.",
                                thisfunc);
                       logMessage(debugMsg);
                       status = FAILED;
                  }
		}
        } else {
           sprintf(debugMsg,"%s: ERROR - "
                   "Could not get Instance_ID variable size.",thisfunc);
           logMessage(debugMsg);
           status = FAILED;
	}

	// Get the location of the OpenSSOAgentBootstrap.properties file from the registry
	if (status == SUCCESS) {
           if (iisaPropertiesFilePathGet(&propertiesFileFullPath, instanceId) 
                                                   == FALSE) {
              sprintf(debugMsg,"%s: ERROR - iisaPropertiesFilePathGet() failed",
                               thisfunc);
              logMessage(debugMsg);
              SetLastError(IISA_ERROR_PROPERTIES_FILE_PATH_GET);
              status = FAILED;
           }
	}

	if (status == SUCCESS) {
		// Initialize amsdk
		amStatus = am_web_init(propertiesFileFullPath);
		if (AM_SUCCESS != amStatus) {
			sprintf(debugMsg, "%s: ERROR - am_web_init failed "
			         "with status = %s (%d)",
			         thisfunc, am_status_to_string(amStatus), 
                                 amStatus);
			logMessage(debugMsg);
			status = FAILED;
		}
	}

	if (instanceId != NULL) {
		free(instanceId);
		instanceId = NULL;
	}
	if (propertiesFileFullPath != NULL) {
		free(propertiesFileFullPath);
		propertiesFileFullPath = NULL;
	}

	return status;
}


BOOL iisaPropertiesFilePathGet(CHAR** propertiesFileFullPath,char *instanceId)
{
    // Max WINAPI path
    const DWORD dwPropertiesFileFullPathSize = MAX_PATH + 1;
    const CHAR  szPropertiesFileName[]       = "OpenSSOAgentBootstrap.properties";
    CHAR agentApplicationSubKey[1000] = "";
    const CHAR agentDirectoryKeyName[]       = "Path";
    DWORD dwPropertiesFileFullPathLen        = dwPropertiesFileFullPathSize;
    HKEY hKey                                = NULL;
    LONG lRet                                = ERROR_SUCCESS;

    strcpy(agentApplicationSubKey,
      "Software\\Sun Microsystems\\Access Manager IIS6 Agent\\Identifier_");
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
        logMessage(debugMsg);
        return FALSE;
    }

    // free'd by caller, even when there's an error.
    *propertiesFileFullPath =(CHAR *) malloc(dwPropertiesFileFullPathLen);
    if (*propertiesFileFullPath == NULL) {
        sprintf(debugMsg,
              "%s(%d) Insufficient memory for propertiesFileFullPath %d bytes",
             __FILE__, __LINE__, dwPropertiesFileFullPathLen);
        logMessage(debugMsg);
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
        logMessage(debugMsg);
        return FALSE;
    }
    if (*propertiesFileFullPath &&
        (**propertiesFileFullPath == '\0')) {
        sprintf(debugMsg,
                "%s(%d) Properties file directory path is NULL.",
                __FILE__, __LINE__);
        logMessage(debugMsg);
        return FALSE;
    }
    if (*(*propertiesFileFullPath + dwPropertiesFileFullPathLen - 1) !=
        '\0') {
        sprintf(debugMsg,
             "%s(%d) Properties file directory path missing NULL termination.",
             __FILE__, __LINE__);
        logMessage(debugMsg);
        return FALSE;
    }
    // closes system registry
    RegCloseKey(hKey);
    if ((strlen(*propertiesFileFullPath) + 2 /* size of \\ */ +
         strlen(szPropertiesFileName) + 1) > dwPropertiesFileFullPathSize) {
        sprintf(debugMsg,
              "%s(%d) Properties file directory path exceeds Max WINAPI path.",
              __FILE__, __LINE__);
        logMessage(debugMsg);
        return FALSE;
    }
    strcat(*propertiesFileFullPath, "\\");
    strcat(*propertiesFileFullPath, szPropertiesFileName);
    return TRUE;
}


am_status_t get_request_url(HTTP_FILTER_CONTEXT *pfc, tOphResources* pOphResources)
{
    const char *thisfunc = "get_request_url";
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
    CHAR  *queryString = NULL;
    DWORD queryStringSize = 0;
    BOOL  gotQueryString = FALSE;
    CHAR* baseUrl = NULL;
    CHAR* colon_ptr = NULL;
    DWORD baseUrlLength = 0;
    BOOL  gotUrl = FALSE;
    am_status_t status = AM_SUCCESS;
    BOOL isOwaEnabledChangeProtocol =
        ((am_web_is_owa_enabled_change_protocol()==B_TRUE)?TRUE:FALSE);
    CHAR *token_ptr = NULL;

    // Check whether the request is http or https
    if ( pfc->GetServerVariable(pfc, "HTTPS", NULL,
                &requestProtocolTypeSize ) == FALSE ) {
        if (requestProtocolTypeSize > 0) {
            requestProtocolType = (CHAR *) malloc(requestProtocolTypeSize);
            if (requestProtocolType != NULL) {
                gotRequestProtocol = pfc->GetServerVariable(pfc,
                                     "HTTPS",
                                      requestProtocolType,
                                      &requestProtocolTypeSize);
                if (!gotRequestProtocol ||
                      (requestProtocolTypeSize <= 0)) {
                    am_web_log_error("%s: Unable to get "
                                            "protocol type, "
                                            "gotRequestProtocol = %d, "
                                            "requestProtocolType = %s, "
                                            "requestProtocolTypeSize = %d",
                                            thisfunc,
                                            gotRequestProtocol, requestProtocolType,
                                            requestProtocolTypeSize);
                    status = AM_FAILURE;
                }
            } 
            else {
                am_web_log_error("%s: Not enough memory 0x%x"
				   "bytes.",thisfunc, requestProtocolTypeSize);
                status = AM_NO_MEMORY;
            }
        }
    }

    if (status == AM_SUCCESS) {
        // Default Protocol and port number needs to be set as follows:
        // 1. When the incoming request is already https and port is 443
        // 2. isOwaEnabledConvertFlag set to "true" (this is a special 
        //    requirement, where both http and https should result in 
        //    https and port 80 and 443 should result in 443.
        if ((strncmp(requestProtocolType,"on", 2) == 0) || (isOwaEnabledChangeProtocol)) {
            requestProtocol = httpsProtocol;
            strcpy(defaultPort, httpsPortDefault);
        } 
        else if(strncmp(requestProtocolType,"off", 3) == 0) {
            requestProtocol = httpProtocol;
            strcpy(defaultPort, httpPortDefault);
        }

        // Get the host name
        if ( pfc->GetServerVariable( pfc, "HEADER_Host", NULL,
                            &requestHostHeaderSize ) == FALSE ) {
            requestHostHeader =(CHAR *) malloc(requestHostHeaderSize);
            if (requestHostHeader != NULL) {
                gotRequestHost = pfc->GetServerVariable(pfc,
                                                "HEADER_Host",
                                             requestHostHeader,
                                        &requestHostHeaderSize);
                if (!gotRequestHost ||(requestHostHeaderSize <= 0)) {
                    am_web_log_error("%s: Unable to get "
                                    "Host name of request. "
                                    "errorHost = %d, "
                                    "RequestHostHeaderSize = %d",
                                    thisfunc, gotRequestHost,
                                    requestHostHeaderSize);
                    status = AM_FAILURE;
                }
            }
        } 
        else {
            am_web_log_error("%s: Not enough memory 0x%x"
                                "bytes.",thisfunc, requestHostHeaderSize);
            status = AM_NO_MEMORY;
        }
    }

    if ((status == AM_SUCCESS) && (requestHostHeader != NULL)) {
        colon_ptr = strchr(requestHostHeader, ':');
        if (colon_ptr != NULL) {
            strncpy(requestPort, colon_ptr + 1, strlen(colon_ptr)-1);
        } 
        else {
            // Get the port number from Server variable
            gotRequestPort = pfc->GetServerVariable(pfc,
                                                    "SERVER_PORT",
                                                    requestPort,
                                                    &requestPortSize);
            if ( !gotRequestPort || (requestPortSize <= 0)) {
                am_web_log_error("%s: Unable to get TCP port "
                    "GetServerVariable(SERVER_PORT) = %d, "
                    "requestPortSize = %d",
                    thisfunc, gotRequestPort, requestPortSize);
                    status = AM_FAILURE;
            }
        }
    }

    if ((status == AM_SUCCESS) && isOwaEnabledChangeProtocol) {
        // If the requestHostHeader already has a port number(80)
        // need to extract the same and replace it with 443
        token_ptr = strtok(requestHostHeader, ":");

        if (token_ptr != NULL) {
            strncpy(requestHostHeader, token_ptr, strlen(token_ptr));
        }
        memset(requestPort,0,requestPortSize);
        strncpy(requestPort,"443",3);
    }

    if (status == AM_SUCCESS) {
        pOphResources->cbUrl =  strlen(requestProtocol)          +
                                strlen(httpProtocolDelimiter)    +
                                strlen(requestHostHeader)        +
                                strlen(httpPortDelimiter)        +
                                strlen(requestPort)              +
                                URL_SIZE_MAX;

        pOphResources->url =(CHAR *) malloc(pOphResources->cbUrl);
        if (pOphResources->url == NULL) {
            am_web_log_error("%s: Not enough memory"
                    "pOphResources->cbUrl", thisfunc);
            status = AM_NO_MEMORY;
        }
    }
	
    // Store request url info in pOphResources
    if (status == AM_SUCCESS) {
        strcpy(pOphResources->url, requestProtocol);
        strcat(pOphResources->url, httpProtocolDelimiter);
        strcat(pOphResources->url, requestHostHeader);
        
        if (strstr(requestHostHeader, httpPortDelimiter) == NULL) {
            if (strcmp(requestPort, defaultPort) != 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, requestPort);
            } else if (strcmp(requestProtocol, httpProtocol) == 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, httpPortDefault);
            } else if (strcmp(requestProtocol, httpsProtocol) == 0) {
                strcat(pOphResources->url, httpPortDelimiter);
                strcat(pOphResources->url, httpsPortDefault);
            }
        }
			
        //Get the base url

        if ( pfc->GetServerVariable( pfc, "URL", NULL,
                                    &baseUrlLength ) == FALSE ) {
            if (baseUrlLength > 0) {
                baseUrl = (CHAR *) malloc(baseUrlLength);
                if (baseUrl != NULL) {
                    gotUrl = pfc->GetServerVariable(pfc, "URL",
                                        baseUrl, &baseUrlLength );
                    if ( !gotUrl ||(baseUrlLength <= 0)) {
                        am_web_log_error("%s: Unable to get "
                                        "base URL gotUrl = %d", 
                                        "baseUrlLength = %d",
                                        thisfunc, gotUrl, baseUrlLength);
                        status = AM_FAILURE;
                    }
                } 
                else {
                    am_web_log_error("%s: Not enough memory 0x%x"
                        "bytes.", thisfunc, baseUrlLength);
                    status = AM_NO_MEMORY;
                }
            }
        }
    }

    if (status == AM_SUCCESS) {
        strcat(pOphResources->url, baseUrl);

        // Get the Query string
        if ( pfc->GetServerVariable( pfc, "QUERY_STRING", NULL,
                                       &queryStringSize ) == FALSE ) {
            queryString = (CHAR *) malloc(queryStringSize);
            if (queryString != NULL) {
                gotQueryString = pfc->GetServerVariable(pfc,
                                                "QUERY_STRING",
                                                queryString,
                                                &queryStringSize);
                if (queryString != NULL && strlen(queryString) > 0) {
                    strcat(pOphResources->url, "?");
                    strcat(pOphResources->url, queryString);
                }
            } 
            else {
                am_web_log_error("%s:  get_request_url():Not enough "
                    "memory 0x%x bytes.", thisfunc, queryStringSize);
                    status = AM_NO_MEMORY;
            }
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
    if (queryString != NULL) {
        free(queryString);
        queryString = NULL;		
    }	

    return status;
}


/**
 * Check if there is a cookie with the cookie name, which is supplied as 
 * in argument,
 * in the request.
**/
CHAR* get_cookie_value(HTTP_FILTER_CONTEXT *pfc, tOphResources* pOphResources, 
		       CHAR *cookie_name) 
{
    const char *thisfunc = "get_cookie_value";
    Status status = FALSE;
    CHAR* dpro_cookie = NULL;
    BOOL fCookie = FALSE;
    DWORD cbCookiesLength = 0;
    CHAR* cookieValue = NULL;
    int length = 0;
    int i = 0;

    // Get the cookie from header
    pOphResources->cbCookies = COOKIES_SIZE_MAX + 1;
    pOphResources->cookies = (CHAR *) malloc(pOphResources->cbCookies);
    if (pOphResources->cookies != NULL) {
        memset(pOphResources->cookies,0,pOphResources->cbCookies);
        cbCookiesLength = pOphResources->cbCookies;
        fCookie = pfc->GetServerVariable(pfc, "HTTP_COOKIE",
		              pOphResources->cookies, &cbCookiesLength);
        if (fCookie  &&  cbCookiesLength > 0) {
            const char *cookieName =(char *) malloc(1024);
            if (cookieName!=NULL) {
               strcpy(cookieName,cookie_name);
            } else {
               am_web_log_error("%s: Unable to allocate memory for cookieName",
                                thisfunc);
            }			   
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
                         dpro_cookie = (CHAR *) malloc(length+1);
                         if (dpro_cookie != NULL) {
                            strncpy(dpro_cookie, cookieValue, length);
                            dpro_cookie[length] = '\0';
                         } else {
                            am_web_log_error("%s: Not enough memory for "
                                             "for cookie, size = %u",
                                              thisfunc, length);
                             status = AM_NO_MEMORY;
                         }
                     }
                 }
             }
             if (cookieName != NULL) {
                 free(cookieName);
                 cookieName = NULL;
             }
          }
    } else {
        am_web_log_error("%s: Not enough memory for "
		         "pOphResources->cbCookies", thisfunc);
        status = AM_NO_MEMORY;
    }

    if (pOphResources->cookies != NULL) {
        free(pOphResources->cookies);
        pOphResources->cookies = NULL;
        pOphResources->cbCookies    = 0;
    }
	
    return dpro_cookie;
}

/**
 * Redirect the request to the appropriate URL
 * depending on the status
 */
static DWORD do_redirect(HTTP_FILTER_CONTEXT *pfc,
			 am_status_t status,
			 const char *original_url,
			 const char *method,
			 BOOL sessionTimeout)
{
    const char *thisfunc = "do_redirect";
    char *redirect_header = NULL;
    size_t redirect_hdr_len = 0;
    char *redirect_status = httpServerError;
    char *redirect_url = NULL;
    char local_redirect_url[1024] = {'\0'};
    char tmpResetCookie[256] = {'\0'};
    am_status_t ret = AM_SUCCESS;
    am_map_t advice_map = AM_MAP_NULL;
    const char *session_timeout_url = NULL;
	                                       
    if (!sessionTimeout) {
        am_web_log_debug("%s:  sessionTimeout is false", thisfunc);
        ret = am_web_get_url_to_redirect(status, advice_map, original_url,
                                         method, AM_RESERVED, &redirect_url);
    } else {
        session_timeout_url = am_web_is_owa_enabled_session_timeout_url();
        if (session_timeout_url != NULL) {
            strcpy(local_redirect_url, session_timeout_url);
            if (local_redirect_url != NULL) {
               strcat(local_redirect_url, "?owagoto=");
               strcat(local_redirect_url, original_url);
            } 
            am_web_log_debug("%s:sessionTimeout is true, value of "
                 "local_redirect_url => %s", thisfunc,
                 local_redirect_url);
        }
    }

    // Compute the length of the redirect response.  Using the size of
    // the format string overallocates by a couple of bytes, but that is
    // not a significant issue given the short life span of the allocation.
    switch(status) {
        case AM_ACCESS_DENIED:
        case AM_INVALID_SESSION:
        case AM_INVALID_FQDN_ACCESS:
        if (ret == AM_SUCCESS && ((redirect_url != NULL) || 
            local_redirect_url != NULL)) {
            if (sessionTimeout && (local_redirect_url != NULL)) {
                redirect_hdr_len = sizeof(REDIRECT_TEMPLATE) + 
                                    strlen(local_redirect_url);
            } 
            else {
                redirect_hdr_len = sizeof(REDIRECT_TEMPLATE) + 
                                    strlen(redirect_url);
            }
            redirect_header =(char *) malloc(redirect_hdr_len);
            if (redirect_header != NULL) {
                redirect_status = httpRedirect;
                if (sessionTimeout) {
                    _snprintf(redirect_header, redirect_hdr_len, 
                              REDIRECT_TEMPLATE, local_redirect_url);
                } 
                else {
                    _snprintf(redirect_header, redirect_hdr_len, 
                              REDIRECT_TEMPLATE, redirect_url);
                }
            } else {
                am_web_log_error("%s: Unable to allocate %u bytes",
                                 thisfunc, redirect_hdr_len);
            }
        } else {
            if (status == AM_ACCESS_DENIED) {
                redirect_status = httpForbidden;
            }
            am_web_log_error("%s:  Error while calling "
                              "am_web_get_url_to_redirect(): "
                              "status = %s", thisfunc, 
                              am_status_to_string(ret));
        }
        break;

        default:
        // All the default values are set to send 500 code.
        break;
    }

    if (redirect_status == httpRedirect &&
       pfc->ServerSupportFunction(pfc, SF_REQ_SEND_RESPONSE_HEADER,
                        redirect_status, (DWORD) redirect_header, 0)) {
        if (redirect_header != NULL) {
            am_web_log_debug("%s: ServerSupportFunction"
                 "succeed: Attempted status = %s ",
                 thisfunc, redirect_status);
        } else {
                   am_web_log_error("%s: ServerSupportFunction did not "
                       "reply with status message = %s "
                       "and extra headers = %s",
                 thisfunc, redirect_status, redirect_header);
        }
    } else {
        size_t data_len = sizeof(FORBIDDEN_MSG) - 1;
        const char *data = FORBIDDEN_MSG;
        if(redirect_status == httpServerError) {
            data = INTERNAL_SERVER_ERROR_MSG;
            data_len = sizeof(INTERNAL_SERVER_ERROR_MSG) - 1;
        }
        if(pfc->WriteClient(pfc, (LPVOID)data,
               (LPDWORD)&data_len, (DWORD) 0)) {
            am_web_log_error("%s: WriteClient did not "
                         "succeed: Attempted message = %s ",
                         thisfunc, data);
        }
    }
	
    if (redirect_header != NULL) {
        free(redirect_header);
        redirect_header = NULL;
    }

    if (redirect_url != NULL) {
        free(redirect_url);
        redirect_url = NULL;
    }

    return SF_STATUS_REQ_FINISHED_KEEP_CONN;
}

void logMessage(CHAR* message)
{
    HANDLE hes= NULL;
    const CHAR* rsz[]= {message};

    if (message == NULL) {
	return;
    }
    hes = RegisterEventSource(0, agentDescription);
    if (hes) {
       ReportEvent(hes, EVENTLOG_ERROR_TYPE, 0, 0, 0, 1, 0, rsz, 0);
       DeregisterEventSource(hes);
    }
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
    return;
}

CHAR* get_decrypt_passwd(const char *encrypt_key, char *encrypt_passwd)
{
  PK11SlotInfo*  slot = NULL;
  CK_MECHANISM_TYPE  cipherMech;
  PK11SymKey*        SymKey = NULL;
  SECItem*           SecParam = NULL;
  PK11Context*       EncContext = NULL;
  SECItem            keyItem;
  SECStatus          rv1, rv2;
  unsigned char      buf1[1024], buf2[1024];
  int                result_len=0, tmp1_outlen, tmp2_outlen;
  unsigned char      gKey[256] = {'\0'};
  const char *thisfunc = "get_decrypt_passwd";
  unsigned char*      decrypt_passwd = NULL;
  am_status_t status = AM_SUCCESS;


  if (status == AM_SUCCESS) {
     cipherMech = CKM_DES_ECB;
     slot = PK11_GetBestSlot(cipherMech, NULL);
     if (slot == NULL) {
        am_web_log_error("%s, Unable to find security device (err %d)\n", 
                         thisfunc, PR_GetError());
        status = AM_FAILURE;
     }
  }

  if (status == AM_SUCCESS) {
      memset(gKey,0,256);
      decode_base64(encrypt_key, gKey);
      keyItem.data = gKey;
      keyItem.len = 8;

      SymKey = PK11_ImportSymKey(slot, cipherMech, PK11_OriginUnwrap, 
                                 CKA_DECRYPT, &keyItem, NULL);
      if (SymKey == NULL) {
         am_web_log_error("%s: Failure to import key into NSS (err %d)\n", 
                          thisfunc,PR_GetError());
         status = AM_FAILURE;
      }
  }

  if (status == AM_SUCCESS) {

     SecParam = PK11_ParamFromIV(cipherMech, NULL);
     if (SecParam == NULL) {
        am_web_log_error("%s: Failure to set up PKCS11 param (err %d)\n", 
                         thisfunc, PR_GetError());
        status = AM_FAILURE;
     }
  }

  if (status == AM_SUCCESS) {
     memset(buf1,0,1024);
     decode_base64(encrypt_passwd, buf1);

     /* DECRYPT buf1 into buf2. buf2 len must be atleast buf1 len */
     tmp1_outlen = tmp2_outlen = 0;
     memset(buf2,0,1024);
     result_len = 16;
 
     /* Create cipher context */
     EncContext = PK11_CreateContextBySymKey(cipherMech, CKA_DECRYPT,
                                             SymKey, SecParam);
     rv1 = PK11_CipherOp(EncContext, buf2, &tmp1_outlen, sizeof(buf2),
                         buf1, result_len);
     rv2 = PK11_DigestFinal(EncContext, buf2+tmp1_outlen, &tmp2_outlen,
                            result_len-tmp1_outlen);
     PK11_DestroyContext(EncContext, PR_TRUE);
     result_len = tmp1_outlen + tmp2_outlen;
     if (rv1 != SECSuccess || rv2 != SECSuccess) {
        am_web_log_error("%s: Data decryption failure (err %d)\n", 
                         thisfunc, PR_GetError());
        status = AM_FAILURE;
     }
  }

  if (status == AM_SUCCESS) {
   decrypt_passwd = (char *) malloc(1024);
   if (decrypt_passwd == NULL) {
       am_web_log_error("%s: ERROR - : Not enough memory for "
                        "decrypt_passwd (1024 bytes). ", thisfunc);
   } else {
     memset(decrypt_passwd,0,1024);
     strncpy(decrypt_passwd,buf2,strlen(buf2));
  }
  }

  /* Free up nss specfic data types */
  if (SymKey)
    PK11_FreeSymKey(SymKey);
  if (SecParam)
    SECITEM_FreeItem(SecParam, PR_TRUE);

  return decrypt_passwd;
}

DWORD OnSendResponse(HTTP_FILTER_CONTEXT *pfc,
                     HTTP_FILTER_SEND_RESPONSE *pSendResponse)
{
    DWORD  returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION; 
    BOOL isOwaEnabled=((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
	
    am_web_log_debug("OnSendResponse(): HTTP Status code is %d", 
                     pSendResponse->HttpStatus);

    if (pSendResponse->HttpStatus == HTTP_UNAUTHORIZED && !isOwaEnabled) { 
        pfc->pFilterContext = (VOID*)REDIRECT_FORBIDDEN; 
    }

    return returnValue;
}


DWORD OnSendRawData(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_RAW_DATA *pRawData)
{
    DWORD state = (DWORD)pfc->pFilterContext; 
    DWORD returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION; 
    CHAR* requestURL = NULL;
    CHAR *requestMethod = NULL;
    BOOL isOwaEnabled=((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
    
    switch (state) { 
        case RESET_FILTER: 
       	    break;

        case REDIRECT_FORBIDDEN: 
            returnValue = do_redirect(pfc, AM_ACCESS_DENIED,requestURL, 
                                      requestMethod, FALSE);
            pfc->pFilterContext = (VOID*)REMOVE_DATA; 
       	    break;

        case REMOVE_DATA: 
            pRawData->cbInData = 0; 
            break;
    } 

    return returnValue; 
}


DWORD OnEndOfRequest(HTTP_FILTER_CONTEXT *pfc)
{
    DWORD returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION;
    BOOL isOwaEnabled=((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
    pfc->pFilterContext = (VOID*)RESET_FILTER;
    return returnValue;
}

/*
 * This function returns true if there is no dpro cookie or,
 * if dpro is found and am session is invalid.
 */
BOOL getIsInvalidAuthSession(HTTP_FILTER_CONTEXT *pfc, 
                             BOOL* pIsInvalidOwaCookie, BOOL invalidMethod)
{
    BOOL isInvalidAuthSession = FALSE;
    BOOL owaCookieFound = FALSE;
    am_sso_token_handle_t sso_handle = NULL;
    CHAR* dpro_cookie = NULL;
    CHAR* owa_auth_cookie = NULL;
    tOphResources OphResources = RESOURCE_INITIALIZER;
    tOphResources* pOphResources = &OphResources;
    BOOL gotPathInfo = FALSE;
    CHAR pathInfo[1000] = "";
    DWORD pathInfoSize = sizeof pathInfo;
    am_status_t amStatus = AM_SUCCESS;
    const char *thisfunc = "getIsInvalidAuthSession";

    CHAR* dpro_cookieName = am_web_get_cookie_name();
    dpro_cookie = get_cookie_value(pfc,pOphResources,dpro_cookieName);
    owa_auth_cookie = get_cookie_value(pfc,pOphResources,owa_cookie);

    // Check for the presence of owa cookie
    if(owa_auth_cookie == NULL){			 
	   owaCookieFound=FALSE;
    } else {
	  owaCookieFound=TRUE;
    }

    amStatus = get_request_url(pfc, pOphResources);
    if (amStatus==AM_SUCCESS) {
        if (dpro_cookie == NULL) {
            if ( !owaCookieFound ) {
               // If user is coming without authenticating with AM 
               // Check for n-e-l and allow the request to be processed
               // comment this part as it will be set to true even 
               // when the user logs in the first time
               isInvalidAuthSession = TRUE;
			   *pIsInvalidOwaCookie = FALSE;               
            } else {
               // The owa user was timed out and owa cookie is still present.
               isInvalidAuthSession = TRUE;
               *pIsInvalidOwaCookie = TRUE;			   
           }
        } 
        // dpro cookie is found but the session may or may not be valid.
        else {
            // User has authenticated with AM, check for the presence of 
            // owaCookie.
            if (owaCookieFound) {
               // Validate the user session
				if(invalidMethod){
					//if the request method is other than GET or POST, 
					//do not reset the idle-timer
					amStatus = am_sso_create_sso_token_handle(&sso_handle, 
						    dpro_cookie, B_FALSE);
				}
				else{
					amStatus = am_sso_create_sso_token_handle(&sso_handle, 
						    dpro_cookie, B_TRUE);
				}
               if (amStatus != AM_SUCCESS) {
                   if (amStatus == AM_INVALID_SESSION) {
                      isInvalidAuthSession = TRUE;
					  *pIsInvalidOwaCookie = TRUE;                      
                   }
               }

               if (sso_handle != NULL) {
                   amStatus = am_sso_destroy_sso_token_handle(sso_handle);
                   if (amStatus != AM_SUCCESS) {
                      am_web_log_error("%s: am_sso_destroy_sso_token_handle() "
                         "returned %s ",thisfunc, am_status_to_name(amStatus));
                   }
               }
            }
        } 
    } else {
        am_web_log_error("%s: amStatus = %s", thisfunc, 
                         am_status_to_name(amStatus));
    }

    if (dpro_cookie != NULL) {
        free(dpro_cookie);
        dpro_cookie = NULL;
    }
	
    if (owa_auth_cookie != NULL) {
        free(owa_auth_cookie);
        owa_auth_cookie = NULL;
    }

    OphResourcesFree(pOphResources);
    
    return isInvalidAuthSession;
}
