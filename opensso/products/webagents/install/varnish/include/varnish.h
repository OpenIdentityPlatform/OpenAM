/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
#ifndef VARNISH_H
#define	VARNISH_H

const char VARNISH_VCL[] =
        "import am;\n"
        "\n"
        "#backend default {\n"
        "#     .host = \"127.0.0.1\";\n"
        "#     .port = \"8080\";\n"
        "#}\n"
        "\n"
        "##\n"
        "# OpenAM Varnish Web Policy Agent configuration\n"
        "# Do not modify below this line\n"
        "##\n\n"
        "sub vcl_init {\n"
        "    am.init(\"%s\", \"%s\");\n"
        "    return (ok);\n"
        "}\n"
        "\n"
        "sub vcl_fini {\n"
        "    am.cleanup();\n"
        "    return (ok);\n"
        "}\n"
        "\n"
        "sub vcl_recv {\n"
        "    if(!am.authenticate(req.request, \"%s\", req.http.host, server.port, req.url, client.ip)) {\n"
        "	error 800;\n"
        "    }\n"
        "    return (lookup);\n"
        "}\n"
        "\n"
        "sub vcl_error {\n"
        "    if(obj.status == 800 || obj.status == 801) {\n"
        "        am.done();\n"
        "        return (deliver);\n"
        "    }\n"
        "}\n"
        "\n"
        "sub vcl_deliver {\n"
        "    am.ok();\n"
        "}\n";

#endif
