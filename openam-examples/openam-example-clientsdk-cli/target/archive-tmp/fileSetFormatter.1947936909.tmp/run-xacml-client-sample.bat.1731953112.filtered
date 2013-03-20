@echo off
: 
: DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
:
: Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
:
: The contents of this file are subject to the terms
: of the Common Development and Distribution License
: (the License). You may not use this file except in
: compliance with the License.
:
: You can obtain a copy of the License at
: https://opensso.dev.java.net/public/CDDLv1.0.html or
: opensso/legal/CDDLv1.0.txt
: See the License for the specific language governing
: permission and limitations under the License.
:
: When distributing Covered Code, include this CDDL
: Header Notice in each file and include the License file
: at opensso/legal/CDDLv1.0.txt.
: If applicable, add the following below the CDDL Header,
: with the fields enclosed by brackets [] replaced by
: your own identifying information:
: "Portions Copyrighted [year] [name of copyright owner]"
:
: $Id: run-xacml-client-sample.bat,v 1.4 2008/08/19 19:11:25 veiming Exp $
:
: Portions Copyrighted 2013 ForgeRock, Inc.
: 
: Runs the xacml client sample program
: 
: constructs a xacml-context:Request 
: makes XACMLAuthzDecisionQuery to PDP,
: receives XACMLAuthzDecisionStatement
: prints out xacml-context:Response
: 
: Requires one parameter: the name of the  resource file that defines
: property values used by the sample
: Default is xacmlClientSamples
: The corresponding file that would be read from classpath 
: is xacmlClientSample.properites 
: from classpath.
: A default template is included 
: at ../resources/xacmlClientSample.properties
: See the template for more information on the properties
: Please update it to match your deployment
: You have to create user and policy at PDP to get right policy decision
: see ../resources/xacmlClientSample.properties for more information
: 
: Requires ../resources/AMConfig.properties 
: Must run "setup.sh" once to configure the client to find the OpenSSO server, this
: is referred as PEP host below. Modify AMConfig.properties, set value of
: "com.sun.identity.agents.app.username" property to "amadmin", set value of 
: "com.iplanet.am.service.password" property to the amadmin password if it is 
: different from the password entered when running setup.sh command. 
: 
: Setting up PDP OpenSSO and PEP OpenSSO
: 
:  At PDP host, that is the host that would run the OpenSSO acting as PDP.
:  We would call this PDP OpenSSO. At PDP host, do the following:
: 
:  deploy opensso.war  and configure it on a supported java ee container
:  using OpenSSO console, Configuration > SAMLv2 SOAP Binding,set soap handler, 
:  key=/xacmlPdp|class=com.sun.identity.xacml.plugins.XACMLAuthzDecisionQueryHandler
: 
:  unzip ssoAdminTools.zip and setup OpenSSO admin tools
:  opensso/bin/ssoadm create-cot -t xacml-pdp-cot -u amadmin -f <password_file>
:  opensso/bin/ssoadm create-metadata-templ -y xacmlPdpEntity -p /xacmlPdp -m xacmlPdp.xml -x xacmlPdp-x.xml -u amadmin -f <password_file>
:  opensso/bin/ssoadm import-entity -t xacml-pdp-cot -m xacmlPdp.xml -x xacmlPdp-x.xml -u amadmin -f <password_file>
: 
: 
:  At PEP host, that is the host that would run the OpenSSO acting as PEP metadata
:  repository, do
: 
:  deploy opensso.war  and configure it on a supported java ee container
: 
:  unzip ssoAdminTools.zip and setup OpenSSO admin tools 
:  opensso/bin/ssoadm create-cot -t xacml-pep-cot -u amadmin -f <password_file>
:  opensso/bin/ssoadm create-metadata-templ -y xacmlPepEntity -e /xacmlPep -m xacmlPep.xml -x xacmlPep-x.xml -u amadmin -f <password_file>
:  opensso/bin/ssoadm import-entity -t xacml-pep-cot -m xacmlPep.xml -x xacmlPep-x.xml -u amadmin -f <password_file>
: 
:  copy xacmlPdp.xml from PDP host as  xacmlPdp-r.xml to PEP host, do
:  opensso/bin/ssoadm import-entity -t xacml-pep-cot -m xacmlPdp-r.xml -u amadmin -f <password_file>
: 
: 
:  At PDP host, do the following:
:  copy xacmlPep.xml from PEP host as xacmlPep-r.xml to PDP host
:  opensso/bin/ssoadm import-entity -t xacml-pdp-cot -m xacmlPep-r.xml -u amadmin -f <password_file>
: 
:  Then, run this script
java -classpath resources;lib/openam-clientsdk-10.2.0-SNAPSHOT.jar;lib/openam-example-clientsdk-cli-10.2.0-SNAPSHOT.jar;lib/servlet-api-2.5.jar;lib/jaxb-libs-1.0.6.jar;lib/jaxb-impl-1.0.6.jar samples.xacml.XACMLClientSample xacmlClientSample

