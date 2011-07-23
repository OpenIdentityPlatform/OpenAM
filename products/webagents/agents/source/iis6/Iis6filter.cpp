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

#include "Iis6filter.h"

//-------------------------------------
//StringA Implementation

StringA::StringA(void)
{
    InitDefault();
}

StringA::StringA(StringA &str)
{
    InitDefault();
    *this = str;
}

StringA::StringA(char *pszValue)
{
    InitDefault();
    *this = pszValue;
}

StringA::~StringA(void)
{
    FreeBuffer();
}

void StringA::InitDefault()
{
    //Initialize - Important, do not remove
    pszDataBuffer = NULL;
    dwBufferLength = 0;

    //Init to prealloc size
    ResizeBuffer(DEFAULT_STRINGA_BUFFER_SIZE);
}

//Allocation / Free
void StringA::ResizeBuffer(size_t dwSize)
{
    if (dwSize > dwBufferLength)
    {
        char* pszExistingBuffer = NULL;
        size_t lastSize = 0;

        if (pszDataBuffer != NULL)
        {
            pszExistingBuffer = pszDataBuffer;
            lastSize = dwBufferLength;
        }

        //Create a New Buffer (original Pointer is held for cleanup)
        pszDataBuffer = new char[dwSize];
        memset(pszDataBuffer, 0, dwSize);
        dwBufferLength = dwSize;

        //Free the existing Buffer...
        if (pszExistingBuffer != NULL) 
        {
            //Copy the original data into the new Buffer...
            memcpy(pszDataBuffer, pszExistingBuffer, lastSize);
            delete[] pszExistingBuffer;
        }
    }
}

void StringA::FreeBuffer(void)
{
    if (pszDataBuffer != NULL)
    {
        delete[] pszDataBuffer;
        dwBufferLength = 0;
    }
}

void StringA::Empty(void)
{
    if (pszDataBuffer != NULL && dwBufferLength > 0)
    {
        memset(pszDataBuffer, 0, dwBufferLength);
    }
}

//--------------------------------------
// Attach
//
// Method attaches (copies) an existing buffer
// and frees the buffer passed as well
//
void StringA::Attach(char* pszData)
{
    if (NULL != pszData)
    {
        this->operator = (pszData);
	//Fix me
        //delete[] pszData;
    }
}

//Formatting
void StringA::PrintFormat(const char* szFormat, ...)
{
    char    buffer[MAX_PATH + 1];
    memset(buffer, 0, MAX_PATH + 1);

    //Perform the Format
    va_list args;
    va_start(args, szFormat);
    vsprintf(buffer, szFormat, args);
    va_end(args);

    //Zero Buffer
    Empty();

    //Make Sure there's room
    size_t length = strlen(buffer) + 1;
    ResizeBuffer(length);

    //Copy the formatted buffer
    memcpy(pszDataBuffer, buffer, length);
}

// Assignment operators
StringA& StringA::operator= ( StringA& pStr )
{
    if (&pStr == this) return *this;

    size_t dwLength = pStr.BufferLength();
    Empty();
    ResizeBuffer(dwLength);
    memcpy(pszDataBuffer, pStr.Data(), pStr.Length());

    return *this;
}

StringA& StringA::operator= ( const char* pChars )
{
    if (pChars != NULL)
    {
        size_t length = strlen(pChars);
        Empty();
        ResizeBuffer(length + 1);   //Extra Null Term...
        memcpy(pszDataBuffer, pChars, length);
    }

    return *this;
}

//Concat Operators
StringA& StringA::operator+= ( StringA& pStr )
{
    //This (string) length + that (string) length + 1
    size_t newlen = Length() + pStr.Length() + 1;
    ResizeBuffer(newlen);
    strcat(pszDataBuffer, pStr.Data());
    return *this;
}

StringA& StringA::operator+= ( const char* pChars )
{
    size_t newlen = Length() + strlen( pChars ) + 1;
    ResizeBuffer(newlen);
    strcat(pszDataBuffer, pChars);
    return *this;
}

// End StringA Implementation
//-------------------------------------


//-------------------------------------
// FilterRequest Implementation

FilterRequest::FilterRequest(void)
{
    //Initialize NULL buffers etc.
    instanceId = "";
}

FilterRequest::~FilterRequest(void)
{
    //FreeResources();
}

DWORD FilterRequest::FreeResources()
{
    const char* thisfunc = "FilterRequest::FreeResources";

    //NOTE: StringA instances are stack based and will free themselves
    return IIS_RESULT_OK;
}

DWORD FilterRequest::GetRequest(HTTP_FILTER_CONTEXT *pContext)
{
    const char* thisfunc = "FilterRequest::GetRequest";

    DWORD   dwStatus = IIS_RESULT_OK;
    StringA httpsProtocolValue;
    BOOL    isOwaEnabledChangeProtocol =
        ((am_web_is_owa_enabled_change_protocol()==B_TRUE)?TRUE:FALSE);

    //Step 1: Check the HTTPS State
    dwStatus = GetServerVariable(pContext, HTTPS_PROTOCOL_HEADER,
            httpsProtocolValue);

    if (IIS_SUCCESS(dwStatus))
    {
        if (httpsProtocolValue == "on" || TRUE == isOwaEnabledChangeProtocol)
        {
            ProtocolType = HTTPS_PROTOCOL;
            DefaultPort = HTTPS_DEFAULT_PORT;
        }
        else
        {
            ProtocolType = HTTP_PROTOCOL;
            DefaultPort = HTTP_DEFAULT_PORT;
        }
    }

    //Step 2: Get the Host Header
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "SERVER_NAME", HostName);
        am_web_log_debug("%s: Server name/HostName = %s", thisfunc, HostName.Data());
    }

    //Step 3: Get the Port
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "SERVER_PORT", RequestPort);
        am_web_log_debug("%s: Request port = %s", thisfunc, RequestPort.Data());
    }

    //Step 4: Assemble the URL
    if (IIS_SUCCESS(dwStatus))
    {
        VirtualUrl = ProtocolType;              //"http"
        VirtualUrl += HTTP_PROTOCOL_DELIM;      //"http://"
        VirtualUrl += HostName;                 //"http://somedomain.com"

        if (FALSE == HostName.Contains(HTTP_PORT_DELIM))
        {
            if (isOwaEnabledChangeProtocol)
            {
                VirtualUrl += HTTP_PORT_DELIM;
                VirtualUrl += DefaultPort;
            }
            else
            {
                VirtualUrl += HTTP_PORT_DELIM;
                VirtualUrl += RequestPort;
            }
        }
    }

    //Step 5: Get the Raw URL
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "URL", BaseUrl);     
        am_web_log_debug("%s: Base URL = %s", thisfunc, BaseUrl.Data());
    }

    //Step 6: Get the QUERY_STRING
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "QUERY_STRING", QueryString);
        if (IIS_SUCCESS(dwStatus) && QueryString.Length() > 0)
        {
	    char *query_str = QueryString.Data();
	    char *asynch1_ptr = strstr(query_str,OWA_ASYNCH1);
	    char *asynch2_ptr = strstr(query_str,OWA_ASYNCH2);
	    if( asynch1_ptr != NULL && asynch2_ptr != NULL)
	    {
		IsAsyncOwaReq = TRUE;
		am_web_log_debug("%s: Owa Asynchronous request received.", 
			thisfunc);
	    }
	    am_web_log_debug("%s: Query String = %s", thisfunc, QueryString.Data());
            BaseUrl += "?";
            BaseUrl += QueryString;         
        }
        VirtualUrl += BaseUrl;
        am_web_log_debug("%s: Virtual Url = %s", thisfunc, VirtualUrl.Data());
    }

    //Step 7: Get the REQUEST_METHOD
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "REQUEST_METHOD", Method);

        if (IIS_SUCCESS(dwStatus))
        {
            IsValidRequestMethod = (Method == HTTP_POST || Method == HTTP_GET
                    ? TRUE : FALSE); if (FALSE == IsValidRequestMethod)
            {
                am_web_log_debug("%s: Invalid method : Method other than "
		       "GET or POST received : %s ", thisfunc, Method.Data());
                IsValidRequestMethod = FALSE;
            }
        }
    }

    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "PATH_INFO", PathInfo);
        am_web_log_debug("%s: PathInfo = %s", thisfunc, PathInfo.Data());
    }
    
    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetServerVariable(pContext, "REMOTE_ADDR", clientIP);
        am_web_log_debug("%s: Remote_Address = %s", thisfunc, clientIP.Data());
        finalclientIP = clientIP;
    }

    if (IIS_SUCCESS(dwStatus))
    {
        const char *client_ip_header_name;
        client_ip_header_name = am_web_get_client_ip_header_name();
        if(client_ip_header_name != NULL) {
            char* http_header = (char*)malloc(strlen("HTTP_")+
                                 strlen(client_ip_header_name)+1);
            strcpy(http_header,"HTTP_");
            strcat(http_header,client_ip_header_name);
            strcat(http_header,"\0");
            am_web_log_debug("%s: Header to be retrived : %s",
                             thisfunc, http_header);
            DWORD tmpStatus = GetServerVariable(pContext, (LPSTR)http_header,
                                                clientIPfromHeader);
            const char *ip_header = clientIPfromHeader.Data();
            if ((ip_header != NULL) && (ip_header[0] != '\0')) {
                char *client_ip_from_ip_header;
                char *nullPtr = NULL;
                am_web_get_client_ip_host(ip_header, NULL,
                                 &client_ip_from_ip_header, &nullPtr);
                if(client_ip_from_ip_header != NULL &&
                   client_ip_from_ip_header[0] != '\0') 
                {
                    finalclientIP = client_ip_from_ip_header;
                    am_web_log_debug("%s: IP recovered from the Header : %s ",
                                     thisfunc, finalclientIP.Data());
                }
            } else {
                am_web_log_debug("%s: IP Header value not found", thisfunc);
            }
            if(http_header != NULL) {
                free(http_header);
                http_header = NULL;
            }
        }
    }

    return dwStatus;
}



DWORD FilterRequest::GetCookies(HTTP_FILTER_CONTEXT *pContext)
{
    //A part of this function will be doing what getIsInvalidAuthSession does
    //Sets IsInvalidAuthSession to true if dpro cookie is NOT valid
    const char* thisfunc = "FilterRequest::GetCookies";
    DWORD dwStatus = IIS_RESULT_OK;
    IsInvalidAuthSession = FALSE;
    IsInvalidOwaCookie = FALSE;
    OwaCookieFound = FALSE;
    am_status_t amStatus;
    StringA dpCookieName;
    am_sso_token_handle_t   ssoTokenHandle = NULL;
    
    if (IIS_SUCCESS(dwStatus))
    {       
        dpCookieName = (char*)am_web_get_cookie_name();
        dwStatus = GetCookieValue(pContext, dpCookieName, DirectoryProCookie);
    }

    if (IIS_SUCCESS(dwStatus))
    {
        dwStatus = GetCookieValue(pContext, OWA_COOKIE, OWACookie);
    }

    if (!OWACookie.IsEmpty()) 
    {
        OwaCookieFound = TRUE;
    } 
    
    if (DirectoryProCookie.IsEmpty()) 
    {
        IsInvalidAuthSession = TRUE;        
    } 
    else //dpro cookie found but may or may not be valid 
    {
        // Since dpro cookie is present, user has authenticated with AM, 
        // check for the presence of OWA Cookie
        if(IsAsyncOwaReq)
        {
            amStatus = am_sso_create_sso_token_handle(&ssoTokenHandle,
                    DirectoryProCookie, B_FALSE); 
        }
        else
        {
            amStatus = am_sso_create_sso_token_handle(&ssoTokenHandle,
                    DirectoryProCookie, B_TRUE); 

        }
        
        if (amStatus == AM_INVALID_SESSION) 
        {
            IsInvalidAuthSession = TRUE;
            if(!OWACookie.IsEmpty()){
                IsInvalidOwaCookie = TRUE;
                am_web_log_debug("%s: Invalid Owa cookie set to true ", 
				thisfunc);
            }
        }

        //destroy the token handle created in validating the user session.
        if (ssoTokenHandle != NULL) 
        {           
            amStatus = am_sso_destroy_sso_token_handle(ssoTokenHandle);
            if (amStatus != AM_SUCCESS) 
            {
                am_web_log_error("%s: am_sso_destroy_sso_token_handle() " 
			"returned %s ", thisfunc, am_status_to_name(amStatus));
            }
        }
        
    }

    return dwStatus;
}


DWORD FilterRequest::GetCookieValue(HTTP_FILTER_CONTEXT *pContext, const char*
        cookieName, StringA& cookieValue) 
{
    const char* thisfunc = "FilterRequest::GetCookieValue";
    DWORD dwStatus = IIS_RESULT_OK;

    int length = 0;
    int i = 0;

    // Get the cookie from header
    dwStatus = GetServerVariable(pContext, "HTTP_COOKIE", Cookies);
    if (IIS_SUCCESS(dwStatus))
    {
        char* pszValue = strstr((char*)Cookies, (char*)cookieName);
        while (pszValue) {
            char *marker = strstr(pszValue+1, cookieName);
            if (marker) {
               pszValue = marker;
            } 
            else {
               break;
            }
        }

        if (pszValue != NULL && (pszValue = strchr(pszValue ,'=')) != NULL) 
        {
            pszValue = &pszValue[1]; // 1 vs 0 skips over '='
            
            // find the end of the cookie
            length = 0;
            for (i=0;(pszValue[i] != ';') &&
                (pszValue[i] != '\0'); i++) {
                      length++;
            }
            pszValue[length]='\0';

            if (length < URL_SIZE_MAX-1) 
            {
                if (length > 0) 
                {
                    cookieValue = pszValue;
                 }
             }
         }
    }
    
    if (!IIS_SUCCESS(dwStatus))
    {
        am_web_log_warning("%s: Could not get the Cookie Value for "
                "Cookie %s", thisfunc, cookieName);
    }

    return dwStatus;
}

DWORD FilterRequest::GetServerVariable(HTTP_FILTER_CONTEXT *pContext, LPSTR
        varName, StringA& string) 
{
    const char* thisfunc = "FilterRequest::GetServerVariable";

    DWORD dwSize = 0;

    //Step 1: First get the Url 
    if (!(pContext->GetServerVariable(pContext, varName, NULL, &dwSize)))
    {
        //Resize the Buffer, if needed
        string.ResizeBuffer(dwSize);

        //Clear the Contents...
        string.Empty();

        if (pContext->GetServerVariable(pContext, varName, string.Data(),
                    &dwSize)) 
        {
            return IIS_RESULT_OK;
        }
        else
        {
            am_web_log_debug("%s: Unable to get %s Header.",
                     thisfunc, varName);
        }
    }
    else
    {
        am_web_log_debug("%s: Error calling GetServerVariable.");
    }

    //Fix me
    //logger.LogWin32Error();
    return IIS_RESULT_FAIL;
}

DWORD FilterRequest::ValidateRequest(HTTP_FILTER_CONTEXT *pContext,
        HTTP_FILTER_PREPROC_HEADERS* pHeaders) 
{
    //ignore n-e-l and owa cookie for a while
    const char* thisfunc = "FilterRequest::ValidateRequest";
    DWORD       dwStatus = IIS_RESULT_OK;
    StringA     DecryptedPassword;
    am_status_t amStatus;
    am_status_t amRefStatus;
    const char* enc_passwd = NULL;
    const CHAR *sunIdentityUserPassword="sunIdentityUserPassword";
    char tmp_password1[256];
    am_sso_token_handle_t   SSOToken = NULL;
       
    
    if(!IsInvalidAuthSession)
    {
        //This call doesnt go to the server. It fetches from the agent cache.
        //Just before this call, there is a call in GetCookies function.
        amStatus = am_sso_create_sso_token_handle(&SSOToken, 
                    DirectoryProCookie, B_FALSE);

        if (amStatus != AM_SUCCESS) 
        {
            am_web_log_error("%s: am_sso_create_sso_token_handle()"
                    "returned %s ", thisfunc, am_status_to_name(amStatus));
            dwStatus = IIS_RESULT_FAIL;
            return dwStatus;
        }

        // First check for the sharepoint login attribute value
        if (!this->OWAEnabled) 
        {
            ProxyUser = am_sso_get_property(SSOToken, SPS_LOGIN_ATTR_VAL,
            B_TRUE); 
        }

        if (ProxyUser.IsEmpty()) 
        {
            StringA userIdParam;
            userIdParam = am_web_get_user_id_param();
            ProxyUser = am_sso_get_property(SSOToken, userIdParam, B_TRUE);
            
            if (ProxyUser.IsEmpty()) 
            {
                am_web_log_error("%s:  Username is null or session is"
                        "invalid. Redirecting to logging page", thisfunc);
                dwStatus = IIS_RESULT_FAIL;
            }
        }

        enc_passwd = am_sso_get_property(SSOToken, sunIdentityUserPassword,
                B_TRUE); 

        if((enc_passwd == NULL) || (strlen(enc_passwd) == 0))
        {
            am_web_log_error("%s: Password is null or session is"
                    "invalid. ", thisfunc);
            dwStatus = IIS_RESULT_FAIL;

        }
        else
        {
            memset(tmp_password1,0,256);
            strncpy(tmp_password1,enc_passwd,strlen(enc_passwd)-1);
            strcat(tmp_password1,"\0");
        }

        if (IIS_SUCCESS(dwStatus)) 
        {                
            dwStatus = DecryptPassword(tmp_password1, DecryptedPassword);

            if (!IIS_SUCCESS(dwStatus) || DecryptedPassword.IsEmpty()) 
            {
                am_web_log_error("%s: decrypt_passwd returned an empty"
                        "value", thisfunc);
                dwStatus = IIS_RESULT_FAIL;
            }
        }

        if (IIS_SUCCESS(dwStatus)) 
        {
            // Construct "u:p" plain text in httpAuthorityValue
            StringA httpAuthorityValue = ProxyUser;
            httpAuthorityValue += ":";
            httpAuthorityValue += DecryptedPassword;

            // Encode "u:p"
            StringA httpCipherText;
            httpCipherText.ResizeBuffer(512);

            encode_base64(httpAuthorityValue, httpAuthorityValue.Length(),
                    httpCipherText.Data());

            // Prepare the authorization header: "Basic base64(user:pass)"
            // Store it in httpAuthorityValue
            httpAuthorityValue = HTTP_AUTH_TYPE;    //"Basic "
            httpAuthorityValue += httpCipherText;

            //---------------------------------------
            // Set the Authorization header
            pHeaders->SetHeader(pContext, HTTP_AUTH_HEADER_NAME,
                    httpAuthorityValue); 
            am_web_log_debug("%s: Authorization header successfully set using "
                    "user %s for request %s.", thisfunc, ProxyUser.Data(),
                    VirtualUrl.Data());
            //---------------------------------------
        }
    }
    if (SSOToken != NULL) 
        {           
            amStatus = am_sso_destroy_sso_token_handle(SSOToken);
            if (amStatus != AM_SUCCESS) 
            {
                am_web_log_error("%s:"
                        "am_sso_destroy_sso_token_handle() returned %s ",
                        thisfunc, am_status_to_name(amStatus));
            }
        }
    
    return dwStatus;
}

DWORD FilterRequest::DoRedirect(HTTP_FILTER_CONTEXT *pContext, BOOL
        *pbRedirected) 
{
    DWORD dwStatus = IIS_RESULT_OK;
    if (NULL != pbRedirected) *pbRedirected = FALSE;
    
    //These 2 conditions on the 'if' can be removed as they are checked while
    //calling this function.
    if (this->IsInvalidAuthSession && this->IsValidRequestMethod)
    {
        //IsOwaEnabled can also replace IsInvalidOwaCookie, as this means
        //redirect to local timeout URL if OWA is enabled in session timeout.
        //Maybe not if it is backdoor session??
        if(IsInvalidOwaCookie){
            dwStatus = ExecuteRedirect(pContext, AM_INVALID_SESSION, TRUE);
        }
        else{
            dwStatus = ExecuteRedirect(pContext, AM_INVALID_SESSION, FALSE);
        }
        if(dwStatus == IIS_RESULT_OK){
            *pbRedirected = TRUE; //this flag might also be removed
        }
    }

    //If No Redirection should take place, then this is a fall through, and
    //thus dwStatus must init to IIS_RESULT_OK
    return dwStatus;    
}

DWORD FilterRequest::ExecuteRedirect(HTTP_FILTER_CONTEXT *pContext,
        am_status_t amStatus, BOOL sessionTimeout) 
{
    const char *thisfunc = "FilterRequest::ExecuteRedirect";

    DWORD dwStatus = IIS_RESULT_OK;

    StringA redirectHeader;
    StringA redirectStatus; redirectStatus = HTTP_SERVER_ERROR;
    StringA localRedirectUrl;
    StringA tempResetCookie;
    StringA sessionTimeoutUrl;
    const char *accessdenied_url = NULL;
    BOOL isOwaEnabled=((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);

    am_status_t thisStatus = AM_SUCCESS;
    am_map_t advice_map = AM_MAP_NULL;
    
    char* redirectBuffer = NULL;
    size_t redirect_hdr_len = 0;
    
    //construct redirection buffer for AM Login
    if ((!sessionTimeout) && (!IsAsyncOwaReq))
    {
        am_web_log_debug("%s: sessionTimeout is false.", thisfunc);
        thisStatus = am_web_get_url_to_redirect(amStatus, advice_map,
                VirtualUrl.Data(), Method.Data(), AM_RESERVED,
                &redirectBuffer);
    } 
    //local timeout redirection url
    else if((sessionTimeout) && (!IsAsyncOwaReq) && (isOwaEnabled))
    {
        sessionTimeoutUrl = am_web_is_owa_enabled_session_timeout_url();
        if (FALSE == sessionTimeoutUrl.IsEmpty()) 
        {
            localRedirectUrl = sessionTimeoutUrl;
            localRedirectUrl += "?owagoto=";
            localRedirectUrl += VirtualUrl;
            am_web_log_debug("%s : localRedirectUrl = %s", thisfunc,
		   localRedirectUrl.Data());
        }
    }
    //redirect buffer for Sharepoint
    else if ((!isOwaEnabled))
    {
        thisStatus = am_web_get_url_to_redirect(amStatus, advice_map,
                VirtualUrl.Data(), Method.Data(), AM_RESERVED,
                &redirectBuffer);
    } 

    // Compute the length of the redirect response.  Using the size of
    // the format string overallocates by a couple of bytes, but that is
    // not a significant issue given the short life span of the allocation.
    switch(amStatus) 
    {
        case AM_ACCESS_DENIED:
        case AM_INVALID_SESSION:
        case AM_INVALID_FQDN_ACCESS:
            if (thisStatus == AM_SUCCESS) 
            {
                //AM login redirection
                if (!sessionTimeout && (redirectBuffer != NULL)
				&& (!IsAsyncOwaReq))
                {
                    _snprintf(redirectHeader.Data(), strlen(redirectBuffer)+
                            sizeof(REDIRECT_TEMPLATE), REDIRECT_TEMPLATE,
                            redirectBuffer); 
                    redirectStatus = HTTP_REDIRECT;
                }
                //local timeout redirection in OWA
                else if(sessionTimeout && !localRedirectUrl.IsEmpty() 
				&& (!IsAsyncOwaReq))
                {
                    _snprintf(redirectHeader.Data(),
                            localRedirectUrl.Length()+
                            sizeof(REDIRECT_TEMPLATE), REDIRECT_TEMPLATE,
                            localRedirectUrl.Data()); 
                    redirectStatus = HTTP_REDIRECT;
                }
                //if sessiontimeout and ajax post request
                //construct 440 Login timeout
                else if((sessionTimeout) && (IsAsyncOwaReq))
                {
                    _snprintf(redirectHeader.Data(),
                            sizeof(LOGIN_TIMEOUT_TEMPLATE), LOGIN_TIMEOUT_TEMPLATE); 
                    redirectStatus = HTTP_LOGIN_TIMEOUT;

                }
                //timeout redirection for Sharepoint
                if (sessionTimeout && (redirectBuffer != NULL)
				&& (!isOwaEnabled))
                {
                    _snprintf(redirectHeader.Data(), strlen(redirectBuffer)+
                            sizeof(REDIRECT_TEMPLATE), REDIRECT_TEMPLATE,
                            redirectBuffer); 
                    redirectStatus = HTTP_REDIRECT;
                }
                am_web_log_debug("%s: Redirect Header = %s", thisfunc, 
				redirectHeader.Data());

             } 
            else 
            {
                if (amStatus == AM_ACCESS_DENIED) {
                    am_web_log_error("%s:  Error while calling "
                            "am_web_get_url_to_redirect(): status = %s",
                             thisfunc, am_status_to_string(amStatus));
                    accessdenied_url = am_web_get_accessdenied_url();
                    if (accessdenied_url == NULL) {
                        am_web_log_debug("%s: Access is denied. Browser will "
                                     "display forbidden message as "
                                     "accessdenied url is null.", thisfunc);
                        redirectStatus = HTTP_FORBIDDEN;
                    } else {
                        am_web_log_debug("%s: Access is denied. Redirecting "
                                         "to accessdenied url %s.",
                                         thisfunc, accessdenied_url);
                        _snprintf(redirectHeader.Data(), 
                                  sizeof(REDIRECT_TEMPLATE) +
                                  strlen(accessdenied_url), 
                                  REDIRECT_TEMPLATE, accessdenied_url);
                        redirectStatus = HTTP_REDIRECT;
                    }
                }
            }
            break;

        default:
        // All the default values are set to send 500 code.
        break;
    }

    am_web_log_debug("%s :  redirectStatus = %s", thisfunc,redirectStatus.Data());
    am_web_log_debug("%s :  amStatus = %s", thisfunc,am_status_to_string(amStatus));
    am_web_log_debug("%s :  thisStatus = %s", thisfunc,am_status_to_string(thisStatus));
    
    if (redirectStatus == HTTP_REDIRECT && 
        pContext->ServerSupportFunction(pContext, SF_REQ_SEND_RESPONSE_HEADER,
            redirectStatus, (DWORD)redirectHeader.Data(), 0)) 
    {
        if (!redirectHeader.IsEmpty()) 
        {
            am_web_log_debug("%s: ServerSupportFunction succeed: %s",thisfunc,
			    redirectStatus.Data());
        }
        else 
        {
            am_web_log_error("%s: ServerSupportFunction did not reply"
                    "with status message = %s  and extra headers = %s",
                    thisfunc, redirectStatus.Data(), redirectHeader.Data());
        }
    } 

    else if (redirectStatus == HTTP_LOGIN_TIMEOUT && 
        pContext->ServerSupportFunction(pContext, SF_REQ_SEND_RESPONSE_HEADER,
            redirectStatus, (DWORD)redirectHeader.Data(), 0)) 
    {
        if (!redirectHeader.IsEmpty()) 
        {
            am_web_log_debug("%s: ServerSupportFunction succeed: %s",thisfunc,redirectStatus.Data());
        }
        else 
        {
            am_web_log_error("%s: ServerSupportFunction did not reply"
                    "with status message = %s  and extra headers = %s",
                    thisfunc, redirectStatus.Data(), redirectHeader.Data());
        }
    }

    else 
    {
        StringA bufferData; 
        bufferData = FORBIDDEN_MSG;

        if(redirectStatus == HTTP_SERVER_ERROR) 
        {
            bufferData = INTERNAL_SERVER_ERROR_MSG;
        }

        size_t data_len = bufferData.Length();
        if(pContext->WriteClient(pContext, (LPVOID)bufferData.Data(),
                    (LPDWORD)&data_len, (DWORD) 0)) 
        {
            am_web_log_error("%s: WriteClient did not succeed:"
                    "Attempted message = %s ", thisfunc, bufferData.Data());
        }
    }
    
    if(redirectBuffer != NULL)
    {
        am_web_free_memory(redirectBuffer);
        redirectBuffer = NULL;
    }
    
    return dwStatus;
}

DWORD FilterRequest::DecryptPassword(char* enc_passwd, StringA& password)
{
    const char* thisfunc = "FilterRequest::DecryptPassword";

    DWORD               dwStatus = IIS_RESULT_OK;
    PK11SlotInfo*       slot = NULL;
    CK_MECHANISM_TYPE   cipherMech;
    PK11SymKey*         SymKey = NULL;
    SECItem*            SecParam = NULL;
    PK11Context*        EncContext = NULL;
    SECItem             keyItem;
    SECStatus           rv1, rv2;
    unsigned char       buf1[1024], buf2[1024];
    int                 result_len=0;
    int                 tmp1_outlen, tmp2_outlen;
    unsigned char       gKey[256] = {'\0'};

    am_status_t status = AM_SUCCESS;

    //if (encryptedPwd.IsEmpty()) return IIS_RESULT_FAIL;
    if (strlen(enc_passwd)==0) return IIS_RESULT_FAIL;

    //Get Encryption Key from AMSDK
    StringA encrypt_key;
    encrypt_key = am_web_get_iis6_replaypasswd_key();

    if (IIS_SUCCESS(dwStatus)) 
    {
        cipherMech = CKM_DES_ECB;
        slot = PK11_GetBestSlot(cipherMech, NULL);
        if (slot == NULL) 
        {
            am_web_log_error("%s, Unable to find security device (err"
                "%d)\n", thisfunc, PR_GetError());
            dwStatus = IIS_RESULT_FAIL;
        }
    }

    if (IIS_SUCCESS(dwStatus)) 
    {
        memset(gKey,0,256);
        decode_base64(encrypt_key, (char*)gKey);

        keyItem.data = (unsigned char*)gKey;
        keyItem.len = 8;

        SymKey = PK11_ImportSymKey(slot, cipherMech, PK11_OriginUnwrap,
                CKA_DECRYPT, &keyItem, NULL); 
        if (SymKey == NULL) 
        {
            am_web_log_error("%s: Failure to import key into NSS (err"
                "%d)\n", thisfunc, PR_GetError());
            dwStatus = IIS_RESULT_FAIL;
        }
    }

    if (IIS_SUCCESS(dwStatus))
    {
        SecParam = PK11_ParamFromIV(cipherMech, NULL);
        if (SecParam == NULL) 
        {
            am_web_log_error("%s: Failure to set up PKCS11 param (err"
                "%d)\n", thisfunc, PR_GetError()); 
            dwStatus = IIS_RESULT_FAIL;
        }
    }

    if (status == AM_SUCCESS) 
    {
        memset(buf1, 0, 1024);
        //decode_base64(encryptedPwd.Data(), (char*)buf1);
        decode_base64(enc_passwd, (char*)buf1);

        //DECRYPT buf1 into buf2. buf2 len must be atleast buf1 len
        tmp1_outlen = tmp2_outlen = 0;
        memset(buf2, 0, 1024);
        result_len = 16;

        /* Create cipher context */
        EncContext = PK11_CreateContextBySymKey(cipherMech, CKA_DECRYPT,
                SymKey, SecParam);

        rv1 = PK11_CipherOp(EncContext, buf2, &tmp1_outlen, sizeof(buf2),
                buf1, result_len); 
        rv2 = PK11_DigestFinal(EncContext,
                    buf2+tmp1_outlen, (unsigned int*)&tmp2_outlen,
                    result_len-tmp1_outlen);
        
        PK11_DestroyContext(EncContext, PR_TRUE);

        result_len = tmp1_outlen + tmp2_outlen;
        if (rv1 != SECSuccess || rv2 != SECSuccess) 
        {
            am_web_log_error("%s: Data decryption failure (err %d)\n",
                    thisfunc, PR_GetError()); 
            dwStatus = IIS_RESULT_FAIL;
        }
    }

    if (IIS_SUCCESS(dwStatus)) 
    {
        password = (char*)buf2;
        if (password.IsEmpty())
        {
            am_web_log_error("%s: Password is Empty after acquiring"
                    "encryption context.", thisfunc); 
            dwStatus = IIS_RESULT_FAIL;
        }
    }

  //Free up nss specfic data types
  if (SymKey) PK11_FreeSymKey(SymKey);
  if (SecParam) SECITEM_FreeItem(SecParam, PR_TRUE);

  return dwStatus;  
}

// End FilterRequest Implementation
//-------------------------------------

//-------------------------------------
// EventLogger Implementation

EventLogger::EventLogger(void)
{
}

EventLogger::~EventLogger(void)
{
}

void EventLogger::LogMessage(LogEventType eventType, const CHAR* lpszFormat,
        ...) 
{
    if (NULL == lpszFormat) return;

    char    buffer[MAX_PATH + 1];
    memset(buffer, 0, MAX_PATH + 1);

    //Perform the Format
    va_list args;
    va_start(args, lpszFormat);
    vsprintf(buffer, lpszFormat, args);
    va_end(args);

    //Log to am_web
    switch(eventType)
    {
    case Debug:
        am_web_log_debug(buffer);
        break;
    case Error:
        am_web_log_error(buffer);
        break;
    }
}

void EventLogger::LogWin32Error()
{
    DWORD dwErr = GetLastError();

    if (dwErr)
    {
        LPVOID lpBuffer;
        FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                NULL, GetLastError(), 0, // Default language
            (LPTSTR) &lpBuffer, 0, NULL);
        
        StringA msg;

        if (NULL == lpBuffer)
        {
            msg.PrintFormat("Win32 Error: %d", dwErr);
        }
        else
        {
            msg.PrintFormat("Win32 Error: %d, %s", dwErr, lpBuffer);
            LocalFree(lpBuffer);
        }

        LogMessage(Error, msg);
    }
}

void EventLogger::LogEventLog(LogEventType eventType, const CHAR* lpszFormat,
        ...) 
{
    if (NULL == lpszFormat) return;

    HANDLE hes= NULL;

    WORD dwEventType = (eventType == Error ? EVENTLOG_ERROR_TYPE :
            EVENTLOG_INFORMATION_TYPE);

    char    buffer[MAX_PATH + 1];
    memset(buffer, 0, MAX_PATH + 1);

    //Perform the Format
    va_list args;
    va_start(args, lpszFormat);
    vsprintf(buffer, lpszFormat, args);
    va_end(args);

    hes = RegisterEventSource(0, "Filter Event Source");
    
    if (hes) 
    {
       ReportEvent(hes, dwEventType, 0, 0, 0, 1, 0, (LPCSTR*)&buffer, 0);
       DeregisterEventSource(hes);
    }
}

void EventLogger::LogToAmWeb(LogEventType eventType, const CHAR* lpszFormat,
        ...) 
{
    char    buffer[MAX_PATH + 1];
    memset(buffer, 0, MAX_PATH + 1);

    //Perform the Format
    va_list args;
    va_start(args, lpszFormat);
    vsprintf(buffer, lpszFormat, args);
    va_end(args);

    //Log to am_web
    switch(eventType)
    {
    case Debug:
        am_web_log_debug(buffer);
        break;
    case Error:
        am_web_log_error(buffer);
        break;
    }
}

// End EventLogger Implementation
//-------------------------------------

//-------------------------------------
// IIS6FilterCore Implementation

IIS6FilterCore::IIS6FilterCore(void)
{
    filterInitialized = FALSE;
}

IIS6FilterCore::~IIS6FilterCore(void)
{
}

DWORD WINAPI IIS6FilterCore::HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD
        NotificationType, VOID *pvData) 
{
    DWORD dwStatus = IIS_RESULT_OK;
    DWORD iisStatus;

    StringA OWACookieValue;
    BOOL    isOwaEnabled = FALSE;
    isOwaEnabled = ((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);

    switch (NotificationType) 
    {
        case SF_NOTIFY_PREPROC_HEADERS:
            dwStatus = ProcessHeaders(pfc,
                    (PHTTP_FILTER_PREPROC_HEADERS)pvData); 
            break;

        case SF_NOTIFY_SEND_RESPONSE:
            dwStatus = OnSendResponse(pfc,
                    (PHTTP_FILTER_SEND_RESPONSE)pvData); 
            break;

        case SF_NOTIFY_SEND_RAW_DATA:
            dwStatus = OnSendRawData(pfc, (PHTTP_FILTER_RAW_DATA)pvData);
            break;

        case SF_NOTIFY_END_OF_REQUEST:
            dwStatus = OnEndOfRequest(pfc);
            break;

        default:            
            theRequest.GetCookieValue(pfc, OWA_COOKIE, OWACookieValue);
            
            // Check for the presence of owa cookie
            if (OWACookieValue.IsEmpty() && isOwaEnabled) 
            {
                iisStatus =  SF_STATUS_REQ_HANDLED_NOTIFICATION;
            } 
            else 
            {
                iisStatus = SF_STATUS_REQ_NEXT_NOTIFICATION;
            }
            break;
    }

    //return iisStatus;
    return dwStatus;
}

DWORD IIS6FilterCore::ProcessHeaders(HTTP_FILTER_CONTEXT *pContext,
        HTTP_FILTER_PREPROC_HEADERS* pHeaders) 
{
    DWORD       dwStatus = IIS_RESULT_OK;
    DWORD       dwCookieStatus = IIS_RESULT_OK;
    BOOL        redirected = FALSE;
    //A short note on SF_STATUS_REQ_HANDLED_NOTIFICATION : the request is not
    //to be handled by any filters so IIS processes it.
    DWORD       iisStatus = SF_STATUS_REQ_HANDLED_NOTIFICATION;
    
    //empty the FilterRequest.
    if (FALSE == filterInitialized) {
        EnterCriticalSection(&initLock);
        //Initialize the filter and the amsdk
        if (FALSE == filterInitialized) {
            if (!IIS_SUCCESS(InitFilter(pContext))){
                return IIS_RESULT_FAIL;
            }
        }
        LeaveCriticalSection(&initLock);
    }

    theRequest.OWAEnabled = ((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
    theRequest.IsAsyncOwaReq = FALSE; 
    //Have the Request do all the state gathering...
    dwStatus = theRequest.GetRequest(pContext);
    
    //set the proper flags depending on the validity of cookies
    if (IIS_SUCCESS(dwStatus))
    {
        dwCookieStatus = theRequest.GetCookies(pContext);       
    }

    //IsInvalidOwaCookie differentiates the user timed out from an AM session
    //from the one who came accessing an n-e-l the if condition below is for
    //the users accessing n-e-l/backdoor users and the else condition for the
    //regular owa users authenticated through AM

    if((theRequest.IsInvalidAuthSession) &&
        ((am_web_is_in_not_enforced_list(theRequest.VirtualUrl.Data(),
            theRequest.PathInfo.Data()) == B_TRUE)||
                (am_web_is_in_not_enforced_ip_list(theRequest.finalclientIP.Data()) == B_TRUE))
                    && !(theRequest.IsInvalidOwaCookie)) 
    {
        //the return flag is set to SF_REQ_NEXT_NOTIFICATION as we want to
        //invoke the other ISAPI filter like owaauth.dll
        am_web_log_debug(" ProcessHeaders : invoking the OWA DLL .");
        iisStatus = SF_STATUS_REQ_NEXT_NOTIFICATION;        
    }
    else{
        //the IsValidRequestMethod condition is so that it is not redirected
        //for AJAX request when the user is timed out
        if (theRequest.IsInvalidAuthSession)
        {
            // Redirect to AM login page
            if(theRequest.IsValidRequestMethod)
            {
                dwStatus = theRequest.DoRedirect(pContext, &redirected);
            }
            iisStatus = SF_STATUS_REQ_FINISHED_KEEP_CONN;
        }
        if (IIS_SUCCESS(dwStatus))
        {
            dwStatus = theRequest.ValidateRequest(pContext, pHeaders);
        }       
    }           
    return iisStatus;
}

DWORD IIS6FilterCore::OnSendResponse(HTTP_FILTER_CONTEXT *pfc,
        HTTP_FILTER_SEND_RESPONSE *pSendResponse) 
{
    DWORD  dwStatus = IIS_RESULT_OK;

    BOOL isOwaEnabled=((am_web_is_owa_enabled()==B_TRUE)?TRUE:FALSE);
    
    am_web_log_debug("OnSendResponse(): HTTP Status code is %d",
            pSendResponse->HttpStatus);

    //When IIS reponds with 401, redirect to 403 iff owa is not enabled or 
    //owa is enabled but the request comes thru backdoor access.
    if (pSendResponse->HttpStatus == HTTP_UNAUTHORIZED && !isOwaEnabled) 
    {
        pfc->pFilterContext = (VOID*)REDIRECT_FORBIDDEN;                
    }
    
    return (IIS_SUCCESS(dwStatus) ? SF_STATUS_REQ_NEXT_NOTIFICATION :
            SF_STATUS_REQ_ERROR); 
}

DWORD IIS6FilterCore::OnSendRawData(HTTP_FILTER_CONTEXT *pfc,
                      HTTP_FILTER_RAW_DATA *pRawData)
{
    DWORD state = (DWORD)pfc->pFilterContext; 
    DWORD returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION; 
    
    switch (state) 
    { 
        case RESET_FILTER: 
            break;

        case REDIRECT_FORBIDDEN: 
            returnValue = theRequest.ExecuteRedirect(pfc, AM_ACCESS_DENIED,
                    FALSE); 
            pfc->pFilterContext = (VOID*)REMOVE_DATA; 
            break;

        case REMOVE_DATA: 
            pRawData->cbInData = 0; 
            break;
    } 

    return returnValue; 
}


DWORD IIS6FilterCore::OnEndOfRequest(HTTP_FILTER_CONTEXT *pfc)
{
    DWORD returnValue = SF_STATUS_REQ_NEXT_NOTIFICATION;
    pfc->pFilterContext = (VOID*)RESET_FILTER;
    return returnValue;
}


//--------------------------------------------
//Private Helper Functions

DWORD IIS6FilterCore::InitFilter(HTTP_FILTER_CONTEXT *pContext)
{
    const char *thisfunc = "IIS6FilterCore::InitFilter";

    //NOTE: instanceId and propertiesFilePath are now class members of type
    //StringA

    DWORD           dwStatus = IIS_RESULT_OK;

    //Get the Instance ID (NOTE: GetServerVariable will log failures etc)
    dwStatus = theRequest.GetServerVariable(pContext, "INSTANCE_ID",
                instanceId);

    // Get the location of the AMAgent.properties file from the registry
    if (IIS_SUCCESS(dwStatus)) 
    {
        dwStatus = GetPropertiesFilePath(propertiesFilePath);
    }

    if (IIS_SUCCESS(dwStatus)) 
    {
        //Initialize Access Manager SDK
        am_status_t amStatus = am_web_init(propertiesFilePath);
        if (AM_SUCCESS != amStatus) 
        {
            am_web_log_error("%s: ERROR - am_web_init failed with"
                    "status = %s (%d)", thisfunc,
                    am_status_to_string(amStatus), amStatus); 
            dwStatus = IIS_RESULT_FAIL;
        }
    }

    //IMPORTANT: Set this boolean to insure that this filter instance
    //doesn't initialize on every request
    if (IIS_SUCCESS(dwStatus)) filterInitialized = TRUE;

    return dwStatus;
}

DWORD IIS6FilterCore::GetPropertiesFilePath(StringA& filePath)
{
    // Max WINAPI path
    StringA     agentApplicationSubKey;
    HKEY        hKey                         = NULL;
    LONG        lRet                         = ERROR_SUCCESS;

    agentApplicationSubKey = AGENT_APPLICATION_SUBKEY;
    if (!instanceId.IsEmpty()) 
    {
       agentApplicationSubKey += instanceId;
    }

    ///////////////////////////////////////////////////////////////////
    //  get the location of the properties file from the registry
    lRet = RegOpenKeyEx(HKEY_LOCAL_MACHINE, agentApplicationSubKey, 0,
            KEY_READ, &hKey); 
    if(lRet != ERROR_SUCCESS) 
    {
        am_web_log_debug("%s(%d) Opening registry key %s%s failed with"
                "error code %d", __FILE__, __LINE__, "HKEY_LOCAL_MACHINE\\",
                AGENT_APPLICATION_SUBKEY, lRet);
        return IIS_RESULT_FAIL;
    }

    //Query the Key Value
    DWORD dwDataLen = MAX_PATH + 1;
    lRet = RegQueryValueEx(hKey, AGENT_DIRECTORY_KEY, NULL, NULL,
            (LPBYTE)filePath.Data(), &dwDataLen); 
    if (lRet != ERROR_SUCCESS) 
    {
        am_web_log_error("%s(%d) Reading registry value %s\\%s\\%s"
                "failed with error code %d", __FILE__, __LINE__,
                "HKEY_LOCAL_MACHINE\\", AGENT_APPLICATION_SUBKEY,
                AGENT_DIRECTORY_KEY, lRet);
        return IIS_RESULT_FAIL;
    }

    //Check for Non-Null but Empty String Buffer
    if (filePath.IsEmpty()) 
    {
        am_web_log_error("%s(%d) Properties file directory path is"
                "NULL or an empty string.",   __FILE__, __LINE__); 
        return IIS_RESULT_FAIL;
    }

    // Close Registry Key - this is very important - failing to do so will
    // cause a system memory leak (handle leak) and will cause the IIS Machine
    // to fallover in a matter of hours, depending on traffic
    RegCloseKey(hKey);

    filePath += "\\";
    filePath += PROPERTY_FILE_NAME;

    return IIS_RESULT_OK;
}

// End IIS6FilterCore Implementation
//-------------------------------------

//-------------------------------------
// DLL Main Entry Point 
// and Exported Isapi Filter Functions

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
                     )
{
    switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH:
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
        break;
    }
    return TRUE;
}

#ifdef _MANAGED
#pragma managed(pop)
#endif

//The one and only stateless Extension Core
//Instantiate it inside the HttpFilterProc instead
//FILTER_CORETYPE theExtension;

//Type of the one and only Logger
LOGGER_TYPE     logger;

/*
IIS Filter Contract Functions NOTE: These functions forward calls to the
appropriate implementation of the IIsapiFilterCore interface contract. This
pattern is meant to provide define-based version support for various versions
of IIS, Exchange / OWA and SharePoint / MOSS versions currently supported, as
well as a consistent compilation model for future versions of the same (or
possibly other) commerical products.
*/

/*
Function:       GetExtensionVersion
Description:    Allows IIS to query the version of the ISAPI Filter itself
*/
BOOL WINAPI GetFilterVersion(HTTP_FILTER_VERSION *pVer)
{
    // Version 1.0
    // Initialize NSPR library
    PR_Init(PR_SYSTEM_THREAD, PR_PRIORITY_NORMAL, 0);
    //Question: Why is this being loaded? Does this cause something to
    //initialize?
    HMODULE nsprHandle = LoadLibrary("libnspr4.dll");   

    pVer->dwFilterVersion = MAKELONG(0, 1);

    // Specify the types and order of notification
    pVer->dwFlags = (SF_NOTIFY_PREPROC_HEADERS | 
                     SF_NOTIFY_ORDER_HIGH | 
                     SF_NOTIFY_SEND_RESPONSE | 
                     SF_NOTIFY_SEND_RAW_DATA | 
                     SF_NOTIFY_END_OF_REQUEST);
    
    //This is non-Unicode buffer
    strcpy(pVer->lpszFilterDesc, FILTER_DESC);
    InitializeCriticalSection(&initLock);
    return TRUE;
}

/*
Function:       HttpFilterProc
Description:    Processes the request from ISS - all filter work is done here
*/
DWORD WINAPI HttpFilterProc(HTTP_FILTER_CONTEXT *pfc, DWORD NotificationType,
        VOID *pvData) 
{
    //Instantiate an object of IISFilterCore theExtension here. The stateless
    //object will not work.
    FILTER_CORETYPE theExtension;
    return theExtension.HttpFilterProc(pfc, NotificationType, pvData);
}

/*
Function:       TerminateFilter
Description:    Is called when IIS terminates the instance of the filter
*/
BOOL WINAPI TerminateFilter(DWORD dwFlags)
{
    am_web_cleanup();
    DeleteCriticalSection(&initLock);
    return TRUE;
}

// End DLL Implementation
//-------------------------------------



