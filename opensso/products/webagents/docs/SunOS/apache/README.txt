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
# $Id: README.txt,v 1.8 2008/06/25 05:54:29 qcheng Exp $
#
# Portions Copyright 2013 ForgeRock AS
#
%% Contents:
    %% 1. Build requirements
    %% 2. Library dependencies
        %% 2.1 Sun Studio 11 Compiler for Solaris Sparc
        %% 2.2 Obtaining make-3.80
        %% 2.3 Obtaining LIBXML2 2.6.23
        %% 2.4 Obtaining NSS 3.11
        %% 2.5 Obtaining NSPR 4.6.1
	%% 2.6 Obtaining libpthread, libCstd, and libCrun
	%% 2.7 Download Apache Header files (if using Apache Agent)
        %% 2.8 Obtaining OpenAM Agents Common Installer libraries
    %% 3. Building the workspace

%% 1. Build requirements

The OpenAM - Web Agents workspace uses GNU Make as the build tool. You must have GNU Make version 3.80 or above installed and available in your system path.

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

        - Sun Studio 11 Compiler for Solaris Sparc
        - make 3.80
        - LIBXML2 2.6.23
        - NSS 3.11
	- NSPR 4.6.1
        - OpenAM Agents Common Installer libraries

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

%% 2.1 Sun Studio 11 Compiler for Solaris Sparc

The Sun Studio 11 for Solaris Sparc is freely available from Sun downloads. 
Please check the below url:
http://developers.sun.com/prodtech/cc/downloads/index.jsp

Click on "Sun Studio 11 - Electronic Software Download".

Install the compiler and update the PATH with the location of "cc".

%% 2.2 Obtaining GNU Make

Follow these steps to obtain the GNU Make:

2.2.1 Open the web page: http://ftp.gnu.org/pub/gnu/make/
2.2.2 Download make-3.80.tar.gz. 
2.2.3 Uncompress the the archive.

Compile the source:
2.2.4 cd make-3.80
2.2.5 ./configure --prefix=<make-3.80-install-dir>
2.2.6 make
2.2.7 make install
2.2.8 Update the PATH variable with <make-3.80-install-dir>/bin

%% 2.3 Obtaining LIBXML2 2.6.23

Follow these steps to obtain the LIBXML2: 

2.3.1 Open the web page: ftp://xmlsoft.org/libxml2/
2.3.2 Download the latest libxml2 source (libxml2-2.6.23.tar.gz)
2.3.3 Uncompress the the archive.
2.3.4 cd libxml2-2.6.23
2.3.4 ./configure --prefix=<libxml2-install-dir>
2.3.5 make
2.3.6 make install
2.3.7 cp <libxml2-install-dir>/include/libxml2/libxml/* <openam_webagent>/extlib/SunOS/libxml2/include/libxml2/libxml
2.3.8 cp <libxml2-install-dir>/lib/* <openam_webagent>/extlib/SunOS/libxml2/lib

%n2.4 Obtaining NSS 3.11

Follow these steps to obtain the binaries:

2.4.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/security/nss/releases/NSS_3_11_RTM/SunOS5.8_OPT.OBJ/
2.4.2 Download nss-3.11.tar.gz.
2.4.3 Uncompress the the archive.
2.4.4 cp nss-3.11/bin/* <openam_webagent>/extlib/SunOS/nss/bin
2.4.5 cp nss-3.11/include/* <openam_webagent>/extlib/SunOS/nss/include
2.4.6 cp nss-3.11/lib/* <openam_webagent>/extlib/SunOS/nss/lib


%% 2.5 Obtaining  NSPR 4.6.1

Follow these steps to obtain the binaries:

2.5.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/nspr/releases/v4.6.1/SunOS5.8_OPT.OBJ/
2.5.2 Download nspr-4.6.1.tar.gz.
2.5.3 Uncompress the the archive. 
2.5.4 cp <nspr-install-dir>/include/*.h <openam_webagent>/extlib/SunOS/nspr/include
2.5.5 cp -r <nspr-install-dir>/include/obsolete <openam_webagent>/extlib/SunOS/nspr/include
2.5.6 cp -r <nspr-install-dir>/include/private <openam_webagent>/extlib/SunOS/nspr/include
2.5.7 cp <nspr-install-dir>/lib/* <openam_webagent>/extlib/SunOS/nspr/lib

%% 2.6 Obtaining libpthread, libCstd, and libCrun

These will be available in /usr/lib in Solaris.

%% 2.7 Download Apache Header files

2.7.1 If building 2.0.x agent then 

    2.7.1.1 Open the web page: http://httpd.apache.org/download.cgi
    2.7.1.2 Download httpd-2.0.55.tar.gz (or any other 2.0.x archive)
    2.7.1.3 Uncompress the the archive. 
    2.7.1.4 cd http-2.0.55
    2.7.1.5 ./configure --prefix=<apache-install-dir>
    2.7.1.6 make
    2.7.1.7 make install
    2.7.1.8 cp <apache-install-dir>/include/* <openam_webagent>/extlib/SunOS/apache/include

2.7.2 If building 2.2.x agent then 

    2.7.2.1 Open the web page: http://httpd.apache.org/download.cgi
    2.7.2.2 Download httpd-2.2.4.tar.gz (or any other 2.2.x archive)
    2.7.2.3 Uncompress the the archive. 
    2.7.2.4 cd http-2.2.4
    2.7.2.5 ./configure --prefix=<apache-install-dir>
    2.7.2.6 make
    2.7.2.7 make install
    2.7.2.8 cp <apache-install-dir>/include/* <openam_webagent>/extlib/SunOS/apache22/include

%% 2.8 Obtaining OpenAM Agents Common Installer libraries

The OpenAM Agents Common installer libraries opensso-installtools.jar and
opensso-installtools-launcher.jar can be built from the OpenAM Agents Common
Installer workspace.

Follow these steps to obtain the libraries:
2.8.1 Check out the OpenAM Agents Common installer workspace.
2.8.2 Set JAVA_HOME to the location of JDK 1.5.
2.8.3 At the root of the workspace, run: ant
2.8.4 Copy the libraries opensso-installtools.jar, and opensso-installtools-launcher.jar
from the dist directory of the OpenAM Agents Common installer workspace
into extlib directory


%% 3. Building the workspace

3.1 cd <openam_webagent>
3.2 ant <agent-name>

** Execute ant usage to get information about all the supported options.
** Make sure gmake is in the system PATH.

Building Apache 2.0.x agent:

     - ant apache : builds Apache 2.0.x agent. C code compiled in optimized mode.
     - ant apache -Dbuild.debug=full     : builds Apache 2.0.x agent. C code compiled in debug mode.
     - ant apache -Dbuild.debug=optimize : builds Apache 2.0.x agent. C code compiled in optimized mode.
     - ant all    : builds all agents. C code compiled in optimized mode.

Building Apache 2.2.x agent:

     - ant apache22 : builds Apache 2.2.x agent. C code compiled in optimized mode.
     - ant apache22 -Dbuild.debug=full     : builds Apache 2.2.x agent. C code compiled in debug mode.
     - ant apache22 -Dbuild.debug=optimize : builds Apache 2.2.x agent. C code compiled in optimized mode.
     - ant all    : builds all agents. C code compiled in optimized mode.

3.3 Creation of build output directories such as
        <openam_webagent>/built
        <openam_webagent>/built/dist
        <openam_webagent>/bin
        <openam_webagent>/drop
        <openam_webagent>/include
        <openam_webagent>/samples

3.4 Build output

Build generates agent installation bits in .zip format,
in the built/dist/ directory. 

Apache 2.0.x agent: apache_v20_SunOS_agent.zip
Apache 2.2.x agent: apache_v22_SunOS_agent.zip
