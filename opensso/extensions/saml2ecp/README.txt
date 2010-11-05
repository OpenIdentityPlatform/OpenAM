# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# $Id: README.txt,v 1.2 2008/03/17 03:12:07 hengming Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved

In this document,
SAML2ECP_HOME represent this directory.

This directory contains a sample ECP project.
To build this project, first build openfm. Then run
   ant build

To start ECP, 
   $SAML2ECP_HOME/startecp <url>

To test SAML2 ECP profile, you need at least one SP and one IDP.

Setup SP and IDP as usual with the following extra steps:
For SP:
1. Instead of deploying 
$SAML2ECP_HOME/../../product/federation/openfm/built/dist/opensso.war,
deploy $SAML2ECP_HOME/built/dist/opensso.war.
This opensso.war contains a filter ECPFilter. ECPFilter will let any request pass
through except the request that accesses ecp/*. When processing these requests,
ECPFilter will check if the user has valid session. If yes, it lets the request
throught. If not, it will forward request to "/SPECP" which is SP sso init
endpoint for ECP profile.
2. When generating SP metadata template, the sp metaAlias needs to be '/sp'
   because '/sp' is hardcoded in ECPFilter.
3. After generating SP metadata, the extended metadata can be customized.
   (optional):
   a. attribute 'ECPRequestIDPListFinderImpl'
      The implementation class to return a list IDP's that SP wish to sso with.
      The default implementation will reads from the following attributes.
   b. attribute 'ECPRequestIDPList'
      Specifies a list of IDP entity ID's
   c. attribute 'ECPRequestIDPListGetComplete'
      Specifies a URL that ECP can get complete IDP entity ID's from.
      At this point, we don't have this service. So our sample ECP won't
      get IDP entity ID's from this URL.
For IDP:
After generating IDP metadata, the extended metadata can be customized.
(optional):
attribute 'idpECPSessionMapper'
The implementation class will return session by calling
SessionManager.getProvider().getSession(httpRequest);
A customized implementation may read HTTP header X-MSISDN and invoke
MSISDN auth module.
For ECP:
1. Customize $SAML2ECP_HOME/config/idps.properties (optional if you configure
SP to send IDPLIst):
   The key is SP entity ID and the value is IDP endpoint for ECP profile.
   When ECP receives AuthnRequest from SP, it will use idps.properties to
   get IDP endpoint. If it is not found, ECP will try to get it from IDPList
   of ECP Request which comes with AuthnRequest. IDPList is what you specified
   on SP.
2. Customize $SAML2ECP_HOME/config/httpHeaders.properties (optional):
   key is HTTP header name, value is HTTP header value.
   ECP will add these HTTP headers to HTTPRequest before forwarding it.
   For example, you can add HTTP header X-MSISDN or Cookie.

To test ECP profile:
1. Start IDP, SP. and ECP.
2. $SAML2ECP_HOME/startecp http://<SP host>:<SP port>/<deploy URI>/ecp/index.html.
   If the user doesn't have SP session, ECP profile will be triggered.
   If idpECPSessionMapper is default implementation, you need to change
   $SAML2ECP_HOME/config/httpHeaders.properties to add Cookie header using
   correct IDP session ID. If everything is set up correctly, startecp will
   print the content of the url at the end. In above case, it will print

Content =
ECP test file

 

