/* The contents of this file are subject to the terms
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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 *
 */

#ifdef IIS6AGENTEXT_EXPORTS
#define IIS6AGENTEXT_API __declspec(dllexport)
#else
#define IIS6AGENTEXT_API __declspec(dllimport)
#endif

#include <windows.h>
#include <tchar.h>
#include <httpfilt.h>
#include <stdio.h>
#include <time.h>
#include <nspr.h>
#include <string.h>
#include <nss.h>
#include <pk11func.h>

#include <am_sso.h>
#include <am_web.h>

#pragma once

#include <windows.h>

//-----------------------------------------
// StringA
//
// Purpose: Implements a simple ANSI Character
// buffer
//
#define DEFAULT_STRINGA_BUFFER_SIZE     MAX_PATH

class StringA
{
public:
    StringA(void);
    StringA(StringA &str);
    StringA(char *pszValue);
    virtual ~StringA(void);

    //Inlines
    inline size_t       BufferLength() { return dwBufferLength;}
    inline size_t       Length() { return (pszDataBuffer != NULL ?
            strlen(pszDataBuffer) : 0); } 
    inline char *       Data() { return pszDataBuffer;}
    inline BOOL         IsEmpty() { return pszDataBuffer == NULL || Length()
        == 0; }

    //Buffer Delegation / Assignment
    void    Attach(char* pszData);

    //Allocation / Free
    void    ResizeBuffer(size_t dwSize);
    void    ResizeBuffer32(DWORD dwSize);
    void    FreeBuffer(void);
    void    Empty(void);

    //Formatting
    void    PrintFormat(const char* szFormat, ...);

    //Cast Operators
    inline operator const char*(void) const { return (const
            char*)pszDataBuffer;} 
    inline operator LPSTR(void) const { return (LPSTR)pszDataBuffer;}

    //Assignment Operators
    StringA&    operator= ( StringA& pStr );
    StringA&    operator= ( const char* pChars );

    //Concat Operators
    StringA& operator+= ( StringA& pStr );
    StringA& operator+= ( const char* pChars );

    BOOL    Contains(const char* substr) { return (NULL != pszDataBuffer &&
            strstr(pszDataBuffer, substr) != NULL ? TRUE : FALSE);}

    //Comparison Operators
    int operator== ( const StringA& s ) const { return (strcmp(pszDataBuffer,
                s) == 0); } 

    int operator== ( const char* c ) const { return strcmp(pszDataBuffer, c)
        == 0; }

    int operator!= ( const StringA& s ) const { return (strcmp(pszDataBuffer,
                s) != 0); } 

    int operator!= ( const char* c ) const { return strcmp(pszDataBuffer, c)
        != 0; }

    int operator> ( const StringA& s ) const { return strcmp(pszDataBuffer, s)
        > 0; } 

    int operator> ( const char* c ) const { return strcmp(pszDataBuffer, c) >
        0; }

    int operator< ( const StringA& s ) const { return strcmp(pszDataBuffer, s)
        < 0; } 

    int operator< ( const char* c ) const { return strcmp(pszDataBuffer, c) <
        0; }

    int operator>= ( const StringA& s ) const { return strcmp(pszDataBuffer,
            s) >= 0; } 

    int operator>= ( const char* c ) const { return strcmp(pszDataBuffer, c)
        >= 0; }

    int operator<= ( const StringA& s ) const { return strcmp(pszDataBuffer,
            s) <= 0; }

    int operator<= ( const char* c ) const { return strcmp(pszDataBuffer, c)
        <= 0; }

private:
    void InitDefault();

    char*   pszDataBuffer;
    size_t  dwBufferLength;
};

/*
Name:           IIsapiFilterCore
Description:    Defines an abstract contract for encapsulating the required
                ISAPI Filter Methods for a given Filter implementation
*/
class IIsapiFilterCore
{
    virtual DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD
            NotificationType, VOID *pvData) = 0;
};

//-------------------------------------
// FilterRequest
//
// Implements a safe wrapper for gathering
// request information for this ISAPI Filter
//
class FilterRequest
{
public:
    FilterRequest(void);
    virtual ~FilterRequest(void);

    //Initialization
    DWORD       GetRequest(HTTP_FILTER_CONTEXT *pContext);
    
    DWORD       GetCookieValue(HTTP_FILTER_CONTEXT *pContext, const char*
            cookieName, StringA& cookieValue);

    DWORD       GetServerVariable(HTTP_FILTER_CONTEXT *pContext, LPSTR
            varName, StringA& string);

    DWORD       ValidateRequest(HTTP_FILTER_CONTEXT *pContext,
            HTTP_FILTER_PREPROC_HEADERS* pHeaders);
    
    DWORD       DoRedirect(HTTP_FILTER_CONTEXT *pContext, BOOL *pbRedirected);
    
    DWORD       ExecuteRedirect(HTTP_FILTER_CONTEXT *pContext, am_status_t
            amStatus, BOOL sessionTimeout);

    DWORD       GetCookies(HTTP_FILTER_CONTEXT *pContext);

    StringA     BaseUrl;        //The Raw URL
    StringA     VirtualUrl;     //The redirection Url
    StringA     Cookies;        //The Raw Cookies
    StringA     Method;         //The Http Method as a String. e.g. POST, GET
    StringA     HostName;       //The Http Host Name
    StringA     PathInfo;       //The Path Info from the raw URL
    StringA     QueryString;    //The Query String
    StringA     DirectoryProCookie; //Not initialized right off the bat
    StringA     OWACookie;      //Not initialized immediately
    StringA     ProtocolType;   //URL Protocol prefix buffer (http, https etc)
    StringA     DefaultPort;
    StringA     RequestPort;
    StringA     clientIP;
    StringA     clientIPfromHeader;
    StringA     finalclientIP;

    //Flags
    BOOL        OWAEnabled;
    BOOL        IsValidRequestMethod;   //TRUE if a GET or POST
    BOOL        IsInvalidAuthSession; // TRUE if dpro is invalid
    //OWAcookie is used to distinguish between users authenticated by AM from
    //the n-e-l users
    BOOL        IsInvalidOwaCookie;     
    BOOL        OwaCookieFound;
    BOOL        IsAsyncOwaReq;

    //Proxy User
    StringA     ProxyUser;      //Proxy Login used for OWA, SPS etc.
    StringA     ProxyPassword;

    //AM Internals
    StringA                 instanceId;

private:
    DWORD       DecryptPassword(char* enc_passwd, StringA& password);
    DWORD       FreeResources();
};

class IIS6FilterCore : IIsapiFilterCore
{
public:
    IIS6FilterCore(void);
    ~IIS6FilterCore(void);

    DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD
            NotificationType, VOID *pvData);

private:
    //Private Buffers - Taken from the Original OpsResources - we only need to
    //store these values for the life of the request, so they can be allocated
    //on the Stack and initialized as needed, on a per-request basis (See
    //GetRequestUrl etc)
    FilterRequest   theRequest;
    BOOL            filterInitialized;
    StringA         instanceId;     //The Instance ID
    StringA         propertiesFilePath; //Configured Properties path for amweb

    //------------------------------
    //Private Helper Functions

    //Initialization / Cleanup
    DWORD       InitFilter(HTTP_FILTER_CONTEXT *pContent);
    DWORD       FreeRequestResources();

    //Core Notification Handlers
    DWORD       ProcessHeaders(HTTP_FILTER_CONTEXT *pContent,
            HTTP_FILTER_PREPROC_HEADERS* pHeaders);

    DWORD       OnSendResponse(HTTP_FILTER_CONTEXT *pfc,
            HTTP_FILTER_SEND_RESPONSE *pSendResponse);
    
    DWORD       OnSendRawData(HTTP_FILTER_CONTEXT *pfc, HTTP_FILTER_RAW_DATA
            *pRawData);
    
    DWORD       OnEndOfRequest(HTTP_FILTER_CONTEXT *pfc);

    //Helper Functions
    DWORD       GetPropertiesFilePath(StringA& filePath); 
};

#ifndef _IISAPI_LOGGER_INCLUDED__
#define _IISAPI_LOGGER_INCLUDED__

typedef enum
{
    Debug = 0,
    Error = 1
} LogEventType;

class ILogger
{
public:
    virtual void LogMessage(LogEventType eventType, const char* lpszFormat,
            ...)=0;
};

class EventLogger : public ILogger
{
public:
    EventLogger(void);
    ~EventLogger(void);

    void LogMessage(LogEventType eventType, const char* lpszFormat, ...);
    void LogWin32Error();

private:
    void LogToAmWeb(LogEventType eventType, const char* lpszFormat, ...);
    void LogEventLog(LogEventType eventType, const char* lpszFormat, ...);
};

#endif //#ifndef _IISAPI_LOGGER_INCLUDED__

#ifndef _IIS_AGENT_INCLUDED_
#define _IIS_AGENT_INCLUDED_

/*
Type reassignment defines for various version scenarios:
NOTE: Define any of these symbols in build or make files
to toggle the Type substitution for the extension core 

IIS6_EXCHANGE2007: Extension for IIS Version 6.0 and Exchange 2007
IIS7_EXCHANGE2007: Extension for IIS Version 7.0 and Exchange 2007
*/
#define IIS6_EXCHANGE2007   //TODO: Break this out into Build Configurations

#ifdef IIS6_EXCHANGE2007

#define FILTER_CORETYPE     IIS6FilterCore
#define LOGGER_TYPE         EventLogger
#define FILTER_DESC         "Authentication filter for IIS 6.0 Sun Policy Agent 2.2"

#elif IIS7_EXCHANGE2007

#define FILTER_CORETYPE     IIS7FilterCore
#define LOGGER_TYPE         EventLogger
#define FILTER_DESC         "Authentication filter for IIS 7.0 Sun Policy Agent 2.2"

#endif

#ifndef LOGGER_TYPE
#define LOGGER_TYPE         EventLogger
#endif

////-------------------------------------------------
////Type of the one and only stateless Extension Core
//extern FILTER_CORETYPE theExtension;

//-------------------------------------------------
//Type of the one and only Logger
extern LOGGER_TYPE      logger;

// ISAPI extension methods
BOOL WINAPI GetFilterVersion(HTTP_FILTER_VERSION *pVer);

DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD NotificationType,
        VOID *pvData);

BOOL WINAPI TerminateFilter(DWORD dwFlags);

// Agent error codes to return to IIS on failure via SetLastError() in
// See WINERROR.H for format.
#define IISA_ERROR_BASE (3 << 30 | 1 << 29)
#define IISA_ERROR_GET_FILTER_VERSION (IISA_ERROR_BASE | 1 << 15)
#define IISA_ERROR_PROPERTIES_FILE_PATH_GET (IISA_ERROR_GET_FILTER_VERSION | 1)

//Buffer Sizes / Limits
#define TCP_PORT_ASCII_SIZE_MAX     5
#define URL_SIZE_MAX                (20*1024)
#define NAME_SIZE_MAX               (50)
#define COOKIES_SIZE_MAX            (20*4096)
#define METHOD_MAX_SIZE             32
#define PORT_MAX_SIZE               16
#define HTTP_UNAUTHORIZED           401

#define RESET_FILTER 0
#define REDIRECT_FORBIDDEN 1
#define REMOVE_DATA 2

#define RESOURCE_INITIALIZER        { NULL, 0, NULL, 0 }

//Unified Result Codes and Testing Macros
#define IIS_RESULT_OK               0
#define IIS_RESULT_FAIL             1

#define IIS_SUCCESS(r)                  (r == IIS_RESULT_OK)
#define IIS_FAILED(r)                   (r != IIS_RESULT_OK)

#define FILTER_EVENT_SOURCE "Sun Policy Agent";
#define IIS6_FILTERCORE_NAME "Authentication filter for IIS 6.0 Sun Policy Agent 2.2";

//HTTP Result Statuses
#define HTTP_OK             "200 OK"
#define HTTP_REDIRECT       "302 Found"
#define HTTP_BADREQUEST     "400 Bad Request"
#define HTTP_FORBIDDEN      "403 Forbidden"
#define HTTP_SERVER_ERROR   "500 Internal Server Error"
#define HTTP_LOGIN_TIMEOUT  "440 Login Timeout"

#define HTTP_PROTOCOL       "http"
#define HTTP_VERSION11      "HTTP/1.1"
#define HTTPS_PROTOCOL      "https"
#define HTTP_PROTOCOL_DELIM "://"

#define LOCALHOST           "localhost"

// Do not change. Used to see if port number needed to reconstructing URL.
#define HTTP_DEFAULT_PORT   "80"
#define HTTPS_DEFAULT_PORT  "443"
#define HTTP_PORT_DELIM     ":"
#define HDR_LOCATION        "Location: "
#define CONTENT_LEN_NOBODY  "Content-length: 0\r\n"
#define CRLF                "\r\n"
#define HTTP_POST           "POST"
#define HTTP_GET            "GET"
#define ENTITY_DELIM        "\r\n\r\n"
#define EXCHANGE_SUFFIX     "/exchange"
#define OWA_COOKIE          "owaAuthCookie"
#define SPS_LOGIN_ATTR_VAL  "sharepoint_login_attr_value"
#define SUN_IDENTITY_USER_PWD   "sunIdentityUserPassword"

#define HTTPS_PROTOCOL_HEADER "HTTPS"

#define REDIRECT_TEMPLATE   "Location: %s\r\nContent-Length: 0\r\n\r\n"

#define LOGIN_TIMEOUT_TEMPLATE   "Content-Type: text/html\r\nConnection: close\r\nContent-Length: 154\r\n\r\n"

#define FORBIDDEN_MSG       "HTTP/1.1 403 Forbidden\r\nContent-Length: 13\r\nContent-Type: text/plain\r\n\r\n403 Forbidden"

#define INTERNAL_SERVER_ERROR_MSG   "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 25\r\nContent-Type: text/html\r\n\r\n500 Internal Server Error"

#define PROPERTY_FILE_NAME          "AMAgent.Properties"
#define AGENT_DIRECTORY_KEY         "Path"
#define AGENT_APPLICATION_SUBKEY    "Software\\Sun Microsystems\\Access Manager IIS6 Agent\\Identifier_"

#define HTTP_AUTH_TYPE              "Basic "
#define HTTP_AUTH_HEADER_NAME       "Authorization:"

#define HTTP_COOKIE                 "HTTP_COOKIE"

#define OWA_ASYNCH1                 "Notify"
#define OWA_ASYNCH2                 "Poll"

extern "C" int decrypt_base64(const char *, char *);
extern "C" int decode_base64(const char *, char *);
extern "C" void encode_base64(const char *, size_t, char *);
CRITICAL_SECTION initLock;


#endif  //#ifndef _IIS_AGENT_INCLUDED_
