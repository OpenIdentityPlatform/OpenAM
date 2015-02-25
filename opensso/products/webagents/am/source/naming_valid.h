/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

#ifndef __NAMING_VALID_H__
#define __NAMING_VALID_H__

#include "am_types.h"
#include "am_web.h"


const char *req_one = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"\
"<RequestSet vers=\"1.0\" svcid=\"auth\" reqid=\"1\">"\
"<Request><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><AuthContext version=\"1.0\">"\
"<Request authIdentifier=\"0\"><NewAuthContext orgName=\"%s\"/></Request>"\
"</AuthContext>]]></Request></RequestSet>";

const char *req_two = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"\
"<RequestSet vers=\"1.0\" svcid=\"auth\" reqid=\"2\">"\
"<Request><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><AuthContext version=\"1.0\">"\
"<Request authIdentifier=\"%s\"><Login>"\
"<IndexTypeNamePair indexType=\"moduleInstance\"><IndexName>%s</IndexName>"\
"</IndexTypeNamePair></Login></Request></AuthContext>]]></Request></RequestSet>";

const char *req_three = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"\
"<RequestSet vers=\"1.0\" svcid=\"auth\" reqid=\"3\">"\
"<Request><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><AuthContext version=\"1.0\">"\
"<Request authIdentifier=\"%s\"><SubmitRequirements>"\
"<Callbacks length=\"2\"><NameCallback><Prompt>Enter application name.</Prompt>"\
"<Value>%s</Value>"\
"</NameCallback><PasswordCallback echoPassword=\"true\"><Prompt>Enter secret string.</Prompt>"\
"<Value>%s</Value>"\
"</PasswordCallback></Callbacks></SubmitRequirements></Request></AuthContext>]]></Request></RequestSet>";

const char *req_logout = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"\
"<RequestSet vers=\"1.0\" svcid=\"auth\" reqid=\"3\">"\
"<Request><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?><AuthContext version=\"1.0\">"\
"<Request authIdentifier=\"%s\">"\
"<Logout/></Request></AuthContext>]]></Request></RequestSet>";

BEGIN_PRIVATE_NAMESPACE

class NamingValidateHttp : public BaseService {
private:
    ServiceInfo service;
    const Properties &properties;
    
public:

    NamingValidateHttp(const char *url, const Properties &props) : BaseService("NamingValidateHttp",
    props),
    service(url), properties(props) {
    }

    virtual ~NamingValidateHttp() {
    }

    am_status_t validate_url(int *hsts) {
        am_status_t status = AM_FAILURE;
        Http::Response response;
        status = doHttpGet(service, "", Http::CookieList(), response);
        *hsts = response.getStatus();
        return status;
    }
};

class NamingValidateHttpLogin : public BaseService {
private:
    ServiceInfo service;
    const Properties &properties;
    std::string uid;
    std::string passwd;
    std::string org;
    std::string module;

public:

    NamingValidateHttpLogin(const char *url, const Properties &props) : BaseService("NamingValidateHttpLogin",
    props),
    service(url), properties(props) {
        const std::string namingservice("/namingservice");
        const std::string authservice("/authservice");
        std::string authURL(url);
        std::size_t pos = 0;
        pos = authURL.find(namingservice, pos);
        while (pos != std::string::npos) {
            authURL.replace(pos, namingservice.size(), authservice);
            pos = authURL.find(namingservice, pos + 1);
        }
        service.clear();
        service.addServer(authURL);
        uid = properties.get(AM_POLICY_USER_NAME_PROPERTY, "");
        passwd = properties.get(AM_POLICY_PASSWORD_PROPERTY, "");
        org = properties.get(AM_POLICY_ORG_NAME_PROPERTY, "/");
        module = properties.get(AM_POLICY_MODULE_NAME_PROPERTY, "Application");
    }

    virtual ~NamingValidateHttpLogin() {
    }

    am_status_t validate_url(int *hsts) {
        am_status_t status = AM_FAILURE;
        Http::Response response;
        char req[8192];
        BodyChunkList bodyChunkList;
        size_t pos = 0, qpos = 0;
        std::string authIdentifier, tmp;
        memset(req, 0, sizeof (req));
        snprintf(req, sizeof (req), req_one, org.c_str());
        /* post for NewAuthContext */
        bodyChunkList.push_back(BodyChunk(std::string(req)));
        status = doHttpPost(service, "", Http::CookieList(), bodyChunkList, response);
        if (AM_SUCCESS == status && Http::OK == response.getStatus()) {
            tmp.assign(response.getBodyPtr());
            pos = tmp.find("authIdentifier=\"", pos);
            if (pos != std::string::npos) {
                qpos = tmp.find("\"", pos + 16);
                authIdentifier = tmp.substr(pos + 16, qpos - pos - 16);
            }
            if (!authIdentifier.empty()) {
                /* initiate Login */
                bodyChunkList.clear();
                memset(req, 0, sizeof (req));
                snprintf(req, sizeof (req), req_two, authIdentifier.c_str(), module.c_str());
                bodyChunkList.push_back(BodyChunk(std::string(req)));
                status = doHttpPost(service, "", Http::CookieList(), bodyChunkList, response);
                if (AM_SUCCESS == status && Http::OK == response.getStatus()) {
                    tmp.assign(response.getBodyPtr());
                    if (tmp.find("Callbacks length=\"2\"") != std::string::npos) {
                        /* submit user name and password */
                        bodyChunkList.clear();
                        memset(req, 0, sizeof (req));
                        snprintf(req, sizeof (req), req_three, authIdentifier.c_str(), uid.c_str(), passwd.c_str());
                        bodyChunkList.push_back(BodyChunk(std::string(req)));
                        status = doHttpPost(service, "", Http::CookieList(), bodyChunkList, response);
                        if (AM_SUCCESS == status && Http::OK == response.getStatus()) {
                            tmp.assign(response.getBodyPtr());
                            if (tmp.find("LoginStatus status=\"success\"") != std::string::npos) {
                                /* do logout, ignore response */
                                bodyChunkList.clear();
                                memset(req, 0, sizeof (req));
                                snprintf(req, sizeof (req), req_logout, authIdentifier.c_str());
                                bodyChunkList.push_back(BodyChunk(std::string(req)));
                                doHttpPost(service, "", Http::CookieList(), bodyChunkList, response);
                            } else {
                                status = AM_AUTH_FAILURE;
                                am_web_log_always("NamingValidateHttpLogin() response:\n%s", response.getBodyPtr());
                            }
                        }
                    } else {
                        status = AM_AUTH_CTX_INIT_FAILURE;
                        am_web_log_always("NamingValidateHttpLogin() response:\n%s", response.getBodyPtr());
                    }
                }
            } else {
                status = AM_INVALID_APP_SSOTOKEN;
                am_web_log_always("NamingValidateHttpLogin() response:\n%s", response.getBodyPtr());
            }
        }
        *hsts = response.getStatus();
        return status;
    }
};

END_PRIVATE_NAMESPACE

#endif

