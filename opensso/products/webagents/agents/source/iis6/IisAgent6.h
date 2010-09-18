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

/* OS specific */
#ifdef XP_UNIX  /* For XP_UNIX */
#elif defined(XP_WIN32)   /* For XP_WIN32 */
#define strcasecmp stricmp
#endif /* !XP_WIN32 */

/* Agent configuration initialized from properties file.
 * agentConfigFree() MUST be kept in lock step with this structure
 *   to prevent memory leaks. The initialization of the static structure
 *   immediately following it's instantiation in IisAgent.c also needs
 *   to stay in lock step with this.
 */
typedef struct AgentConfig {
	BOOL            bAgentInitSuccess; // For disabling IIS if init fails.
} tAgentConfig;
void agentConfigFree(void);

// Limit in the amount of memory allocated based on a HTTP request.
// This is to avoid easy Denial Of Service attacks.
//   0x100000 = 1MB
#define IISA_REQUEST_MALLOC_MAX 0x100000

// Maximum malloc allowed for the POST /UpdateAgentCacheServlet body
#define IISA_UPDATE_CACHE_BODY_MALLOC_MAX IISA_REQUEST_MALLOC_MAX

#define URL_SIZE_MAX (20*1024)

//   Size from minimum maximum of cookies per host in section 5.3 of:
//   http://www.ietf.org/rfc/rfc2965.txt
#define COOKIES_SIZE_MAX (20*4096)

// Tcp port numbers are 16 bits or 5 ascii decimal digits.
// See http://www.ietf.org/rfc/rfc793.txt
#define TCP_PORT_ASCII_SIZE_MAX 5

// Domain name length limit See: http://www.ietf.org/rfc/rfc1035.txt
#define DOMAIN_NAME_SIZE_MAX 255

// HTTP version length limit, allows for multiple digits.
// See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.1
#define HTTP_VERSION_SIZE_MAX 255

// TCP IP length limit xxx.xxx.xxx.xxx
#define TCP_IP_SIZE_MAX 15

#define PATH_INFO_SIZE_MAX 1000

// ISAPI extension methods
BOOL WINAPI GetExtensionVersion(HSE_VERSION_INFO *pVer);
DWORD WINAPI HttpExtensionProc(EXTENSION_CONTROL_BLOCK *pECB);
BOOL WINAPI TerminateExtemsion(DWORD dwFlags);

// Method to execute original url
VOID WINAPI ExecUrlCompletion (
    EXTENSION_CONTROL_BLOCK *   pECB,
    PVOID                       pContext,
    DWORD                       cbIO,
    DWORD                       dwError
    );

// Get the full path to the properties file... from registry.
// Returns TRUE for success, FALSE for an error.
// pszPropertiesFileFullPath is malloc'd
// in this iisaPropertiesFilePathGet(). MUST free it later...
BOOL iisaPropertiesFilePathGet(CHAR** propertiesFileFullPath, char *instanceId);

void logPrimitive(CHAR *message);

char* string_case_insensitive_search(char *HTTPHeaders, char *KeY);

am_status_t get_header_value(EXTENSION_CONTROL_BLOCK *pECB, char* header_name, 
        char* header_value);

tAgentConfig agentConfig;

// Agent error codes to return to IIS on failure via SetLastError() in
// GetExtensionVersion() .
// See WINERROR.H for format.

// Error | Customer code flag
#define IISA_ERROR_BASE (3 << 30 | 1 << 29)

#define IISA_ERROR_GET_EXTENSION_VERSION    (IISA_ERROR_BASE | 1 << 15)

#define IISA_ERROR_PROPERTIES_FILE_PATH_GET (IISA_ERROR_GET_EXTENSION_VERSION | 1)
#define IISA_ERROR_INIT_POLICY              (IISA_ERROR_GET_EXTENSION_VERSION | 2)
#define IISA_ERROR_SEE_DEBUG_LOG            (IISA_ERROR_GET_EXTENSION_VERSION | 3)
