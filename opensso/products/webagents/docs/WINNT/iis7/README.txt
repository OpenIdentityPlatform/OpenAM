------------------------------------------------------------------------------
README file for Open Web Single Sign-On - Web Agents
------------------------------------------------------------------------------
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
#
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
# $Id: README.txt,v 1.1 2009/02/13 23:39:53 robertis Exp $
#
# Portions Copyright 2013 ForgeRock AS
#
%% Contents:
    %% 1. Build requirements
    %% 2. Library dependencies
        %% 2.1 Obtaining Visual Studio 6 or 7, MKS Tool Kit
        %% 2.2 Obtaining LIBXML2 2.6.23
        %% 2.3 Obtaining NSS 3.11
        %% 2.4 Obtaining NSPR 4.6.1
	%% 2.5 Download IIS Header files
    %% 3. Building the workspace

%% 1. Build requirements

The OpenAM - Web Agents workspace uses GNU Make as the build tool. You must have GNU Make version 3.80 or above installed and available in your system path.

Also make sure that the gcc version is above 3.2.1 (you can find this thru 
gcc --version)

Also Apache Ant is required to build the agent installer.
The build scripts in this workspace use features not present in
releases of Ant prior to 1.6.5.  Thus, in order to build this workspace
you must have Ant version 1.6.5 or above installed and available in
your system path.

The sources in this workspace should be compiled using JDK 5.0 with
the source and target levels set to "1.4". In order to allow this, you
must ensure that JDK 5.0 is present in your system path and the JAVA_HOME
environment variable is setup correctly pointing to its location.


%% 2. Library dependencies

The OpenAM sources depend upon a few third-party libraries that are not
included as a part of the workspace and must be obtained directly from 
the library vendor or distributor. Some of these libraries are available in 
source distribution form and must be built locally before they can be used. 
The libraries needed for compiling OpenAM sources are:

        - Visual Studio 6 or 7, MKS Tool Kit
        - LIBXML2 2.6.23
        - NSS 3.11
	- NSPR 4.6.1

The following subsections briefly describe how these libraries may be 
obtained. These instructions are provided for your convenience only and
are not intended to replace or invalidate the procedures/guidelines set
by the library vendor or distributor for obtaining those libraries. Before
you follow these instructions, you must agree to abide by any terms and
conditions set forth by the library vendor or distributor. In case any of
these instructions are in contradiction to such terms or conditions, you 
must disregard those instructions and instead follow the ones provided by
the library vendor or distributor in accordance with their terms and 
conditions.

%% 2.1 Obtaining Visual Studio 6 or 7, MKS Tool Kit

Please install Visual Studio 6 or 7 and MKS Tool Kit before compiling the 
sources. Make sure that the PATH variable is updated with the locations of 
Visual Studio 6 or 7 and MKS Tool Kit

%% 2.2 Obtaining LIBXML2 2.6.23

Follow these steps to obtain the LIBXML2:

2.2.1 Open the web page: http://www.zlatkovic.com/pub/libxml/
2.2.2 Download libxml2-2.6.23+.win32.zip
2.2.3 Uncompress the zip file.
2.2.4 cp <libxml2-install-dir>\include\libxml\* <openam_webagent>\extlib\WINNT\libxml2\include\libxml2\libxml
2.2.5 cp <libxml2-install-dir>\lib\* <openam_webagent>\extlib\WINNT\libxml2\lib

%n2.3 Obtaining NSS 3.11 

Follow these steps to obtain the binaries:

2.3.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/security/nss/releases/NSS_3_11_RTM/WINNT5.0_OPT.OBJ/
2.3.2 Download nss-3.11.zip
2.3.3 Uncompress the zip file. 
2.3.4 cp nss-3.11\bin\* <openam_webagent>\extlib\WINNT\nss\bin
2.3.5 cp nss-3.11\include\* <openam_webagent>\extlib\WINNT\nss\include
2.3.6 cp nss-3.11\lib\* <openam_webagent>\extlib\WINNT\nss\lib


%% 2.4 Obtaining  NSPR 4.6.1

Follow these steps to obtain the binaries:

2.4.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/nspr/releases/v4.6.1/WINNT5.0_OPT.OBJ/
2.4.2 Download nspr-4.6.1.zip
2.4.3 Uncompress the zip file. 
2.4.4 cp <nspr-install-dir>\include\*.h <openam_webagent>\extlib\WINNT\nspr\include
2.4.5 cp -r <nspr-install-dir>\include\obsolete <openam_webagent>\extlib\WINNT\nspr\include
2.4.6 cp -r <nspr-install-dir>\include\private <openam_webagent>\extlib\WINNT\nspr\include
2.4.7 cp <nspr-install-dir>\lib\* <openam_webagent>\extlib\WINNT\nspr\lib

%% 2.5 Download IIS Header files

2.5.1 If building IIS 7 agent then 

    2.5.1.1 IIS7's httpserv.h is needed.
    2.5.1.2 cp httpserv.h.h <openam_webagent>\extlib\WINNT\iis7\include


%% 3. Building the workspace

3.1 cd <openam_webagent>

** Execute ant usage to get information about all the supported options.
** Make sure gmake is in the system PATH.

3.2 Building IIS 7 agent:

     - ant iis7 : builds IIS 7 agent. C code compiled in optimized mode.

3.4 Build output

Build generates agent installation bits in .zip format,
in the built/dist/ directory. 

IIS 7 agent: iis_v7_WINNT_agent_3.zip
