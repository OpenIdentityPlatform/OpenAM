/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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

/*
 * Portions Copyrighted 2011 TOOLS.LV SIA
 */

#pragma once

#include <string>
#include <list>
#include <climits>
#include <sstream>
#include <cctype>
#include <stdlib.h>

#ifdef _MSC_VER
#include <windows.h>
#include <process.h>
#include <io.h>
#define getpid _getpid
#define snprintf _snprintf
#define sleep(interval) Sleep(interval * 1000)
#endif

namespace sdk {

    namespace utils {

        std::string format(const char *fmt, ...);
        inline std::string trim(std::string &str);
        void stringtokenize(std::string &str, std::string separator, std::list<std::string>* results);
        std::string timestamp(const long sec);
        std::string timestamp(const char *sec);

        struct url {

            url(const std::string & url_s) {
                parse(url_s);
            }

            const std::string & protocol() const {
                return protocol_;
            }

            const std::string & host() const {
                return host_;
            }

            const std::string & domain() const {
                return domain_;
            }

            const std::string & path() const {
                return path_;
            }

            const std::string & query() const {
                return query_;
            }

            const std::string & uri() const {
                return uri_;
            }

            const int port() const {
                int st = atoi(port_.c_str());
                if (st == INT_MAX || st == INT_MIN) {
                    return 0;
                }
                return st;
            }

            const std::string URL() const {
                std::string sc(":");
                std::string qm("?");
                std::string rv;
                if (protocol_.empty() || host_.empty()) {
                    return rv;
                }
                rv.append(protocol_).append("://").append(host_)
                        .append(port_.empty() ? "" : sc.append(port_))
                        .append(path_.empty() ? "/" : path_)
                        .append(query_.empty() ? "" : qm.append(query_));
                return rv;
            }
        private:
            void parse(const std::string & url_s);
        private:
            std::string protocol_, host_, port_, path_, query_, domain_, uri_;
        };

        int validate_agent_credentials(url *u, const char *aname, const char *apwd,
                const char *arealm, const char *ssldb, const char *sslpwd, int init);


    }

}
