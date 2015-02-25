------------------------------------------------------------------------------
README file for Open Web Single Sign-On - Web Agents
------------------------------------------------------------------------------
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
# $Id: README.txt,v 1.3 2008/06/25 05:54:29 qcheng Exp $
#
# Portions Copyright 2013 ForgeRock AS
#
%% Contents:
    %% 1. Build requirements
    %% 2. Library dependencies
        %% 2.1 Sun Studio 11 Compiler for Solaris x86
        %% 2.2 Obtaining make-3.80
        %% 2.3 Obtaining LIBXML2 2.6.23
        %% 2.4 Obtaining NSS 3.11
        %% 2.5 Obtaining NSPR 4.6.1
	%% 2.6 Obtaining libpthread, libCstd, and libCrun
	%% 2.7 Download SJS Web Server 7.0 Header files 
        %% 2.8 Obtaining OpenAM Agents Common Installer libraries
    %% 3. Building the workspace
    %% 4. Building 64-bit agent (optional)

%% 1. Build requirements

The OpenAM - Web Agents workspace uses GNU Make as the build tool. You must 
have GNU Make version 3.80 or above installed and available in your system path.

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

        - Sun Studio 11 Compiler for Solaris x86
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

%% 2.1 Sun Studio 11 Compiler for Solaris x86

The Sun Studio 11 for Solaris x86 is freely available from Sun downloads.
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
2.3.2 Download the latest libxml2 source (e.g libxml2-2.6.23.tar.gz)
2.3.3 Uncompress the the archive.
2.3.4 cd libxml2-2.6.23
2.3.5 ./configure --prefix=<libxml2-install-dir>
2.3.6 make
2.3.7 make install
2.3.8 cp <libxml2-install-dir>/include/libxml2/libxml/* <openam_webagent>/extlib/SunOS_i86pc/libxml2/include/libxml2/libxml
2.3.9 cp <libxml2-install-dir>/lib/* <openam_webagent>/extlib/SunOS_i86pc/libxml2/lib

%n2.4 Obtaining NSS 3.11 

Follow these steps to obtain the binaries:

2.4.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/security/nss/releases/NSS_3_11_RTM/SunOS5.9_i86pc_OPT.OBJ/
2.4.2 Download nss-3.11.tar.gz.
2.4.3 Uncompress the the archive.
2.4.4 cp nss-3.11/bin/* <openam_webagent>/extlib/SunOS_i86pc/nss/bin
2.4.5 cp nss-3.11/include/* <openam_webagent>/extlib/SunOS_i86pc/nss/include
2.4.6 cp nss-3.11/lib/* <openam_webagent>/extlib/SunOS_i86pc/nss/lib


%% 2.5 Obtaining  NSPR 4.6.1

Follow these steps to obtain the binaries:

2.5.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/nspr/releases/v4.6.1/SunOS5.8_i86pc_OPT.OBJ/
2.5.2 Download nspr-4.6.1.tar.gz.
2.5.3 Uncompress the the archive.
2.5.4 cp <nspr-install-dir>/include/*.h <openam_webagent>/extlib/SunOS_i86pc/nspr/include
2.5.5 cp -r <nspr-install-dir>/include/obsolete <openam_webagent>/extlib/SunOS_i86pc/nspr/include
2.5.6 cp -r <nspr-install-dir>/include/private <openam_webagent>/extlib/SunOS_i86pc/nspr/include
2.5.7 cp <nspr-install-dir>/lib/* <openam_webagent>/extlib/SunOS_i86pc/nspr/lib

%% 2.6 Obtaining libpthread, libCstd, and libCrun

These will be available in /usr/lib in Solaris.

%% 2.7 Download SJS WS7.0 Header files

2.7.1 Open the web page: http://www.sun.com/download . Choose Web Server.
2.7.2 Download WS7.0 install bits.
2.7.3 Uncompress the the archive.
2.7.4 Install the server.
2.7.5 Goto server install directory.
2.7.6 cp <webserver-install-dir>/include/* <openam_webagent>/extlib/SunOS_i86pc/sjsws/include

%% 2.8 Obtaining OpenAM Agents Common Installer libraries

opensso/products/installtools source code needs to be available in the same workspace.
Building of OpenAM Agents Common installer libraries opensso-installtools.jar and
opensso-installtools-launcher.jar is integrated in the webagents/build.xml.
So no separate copy/build is required.

%% 3. Building the workspace

3.1 cd opensso/products/webagents
3.2 ant sjsws

** Execute ant usage to get information about all the supported options.
** Make sure gmake is in the system PATH.

Building SJS WS7 agent:

     - ant sjsws : builds SJS WS7 agent. C code compiled in optimized mode.
     - ant sjsws -Dbuild.debug=full     : builds SJS WS7 agent. C code compiled in debug mode.
     - ant sjsws -Dbuild.debug=optimize : builds SJS WS7 agent. C code compiled in optimized mode.
     - ant all    : builds all agents. C code compiled in optimized mode.

3.4 Build output

Build generates agent installation bits zip format,
in the built/dist/ directory. The agent installer archive
name is in this format: sjsws_v70_<OS>_agent.zip.

    - <OS> : SunOS, Linux, WINNT, SunOS_x86

Example: sjsws_v70_SunOS_x86_agent.zip


4. Building 64-bit agent. (Optional)

Web Server 7 also has 64-bit support on Solaris sparc and x86 platforms.
The above information helps in building 32-bit agent i.e. agent works with 32-bit Web Server7.

In order to build 64-bit agent, all the above information is applicable,
but 64-bit libxml2, nss and nspr libraries need to exist in extlib/ directory.

Also make sure that the Solaris system supports 64-bit kernel. Try the following command:
#isainfo -v (or isainfo -kv)
64-bit sparcv9 applications
32-bit sparc applications

If the result shows 64-bit, then 64-bit agent works.

Please follow these instructions:

%% 4.1 64-bit LIBXML2 2.6.23

4.1.1 Open the web page: ftp://xmlsoft.org/libxml2/
4.1.2 Download the latest libxml2 source (libxml2-2.6.23.tar.gz)
4.1.3 Uncompress the the archive.
4.1.4 cd libxml2-2.6.23
4.1.4 ./configure --prefix=<libxml2-install-dir> CC=<SunStudio c compiler path> CFLAGS="-fast -xarch=generic64"
4.1.5 make
4.1.6 make install
4.1.7 cp <libxml2-install-dir>/include/libxml2/libxml/* <openam_webagent>/extlib/SunOS_i86pc/libxml2_64/include/libxml2/libxml
4.1.8 cp <libxml2-install-dir>/lib/* <openam_webagent>/extlib/SunOS_i86pc/libxml2_64/lib

%%4.2 64-bit NSS 3.11

Follow these steps to obtain the binaries:

4.2.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/security/nss/releases/NSS_3_11_RTM/SunOS5.10_i86pc_64_OPT.OBJ/nss-3.11.tar.gz
4.2.2 Download nss-3.11.tar.gz.
4.2.3 Uncompress the the archive.
4.2.4 cp nss-3.11/bin/* <openam_webagent>/extlib/SunOS_i86pc/nss_64/bin
4.2.5 cp nss-3.11/include/* <openam_webagent>/extlib/SunOS_i86pc/nss_64/include
4.2.6 cp nss-3.11/lib/* <openam_webagent>/extlib/SunOS_i86pc/nss_64/lib


%% 4.3 64-bit  NSPR 4.6.1

Follow these steps to obtain the binaries:

4.3.1 Open the web page : ftp://ftp.mozilla.org/pub/mozilla.org/nspr/releases/v4.6.1/SunOS5.10_i86pc_64_OPT.OBJ/nspr-4.6.1.tar.gz
4.3.2 Download nspr-4.6.1.tar.gz.
4.3.3 Uncompress the the archive.
4.3.4 cp <nspr-install-dir>/include/*.h <openam_webagent>/extlib/SunOS_i86pc/nspr_64/include
4.3.5 cp -r <nspr-install-dir>/include/obsolete <openam_webagent>/extlib/SunOS_i86pc/nspr_64/include
4.3.6 cp -r <nspr-install-dir>/include/private <openam_webagent>/extlib/SunOS_i86pc/nspr_64/include
4.3.7 cp <nspr-install-dir>/lib/* <openam_webagent>/extlib/SunOS_i86pc/nspr_64/lib

%% 4.4 Building the 64-bit agent

4.4.1 ant sjsws -Dbuild.type=64

