#
# DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2012 ForgeRock Inc. All rights reserved.
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# http://forgerock.org/license/CDDLv1.0.html
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at http://forgerock.org/license/CDDLv1.0.html
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions copyright [year] [name of copyright owner]"
#

#
# Portions copyright 2012-2013 ForgeRock Inc
#

Description:
This document explains how to setup an OAuth 2.0 authorization server and how to setup the demo client to interact with
that authorization server.

Building:
 Build the OAuth 2.0 module
    1. Change directory into the openam-oauth2 folder.
    2. Run the command "mvn clean install"

 This will build the OAuth 2 demo war in openam-oauth2/openam-oauth2-demo/target folder.

Install:
 Install an OAuth 2 authorization server.
    1. Login to an OpenAM instance.
    2. Click Configure OAuth2.
    3. Set realm to "/".
    4. Fill in the desired token lifetimes.
    5. Check issue refresh tokens.
    6. Navigate to Access Control->top level realm->Agents->OAuth 2.0 Client and create an OAuth 2.0 client.
 Install the demo client.
    1. Put the war file located in openam-oauth2/openam-oauth2-demo/target in tomcats webapps directory.
    2. Edit the WEB-INF/lib/classes/AMConfig.properties file.
        a. Change all the URLs to your domain name and tomcat port for your authorizaiton server.
        b. Set org.forgerock.openam.oauth2.client_id and org.forgerock.openam.oauth2.client_secret to the OAuth 2.0
           Client created when setting up the authorization server.
        c. Set org.forgerock.openam.oauth2.endpoint.redirection=<url of a redirection end point>
3. Open <hostname>:<port>/<name of war file> in your web browser.