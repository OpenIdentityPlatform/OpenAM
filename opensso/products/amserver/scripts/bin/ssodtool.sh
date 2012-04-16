#!/bin/sh
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
# $Id: ssodtool.sh,v 1.3 2009/08/18 01:02:53 ak138937 Exp $
#

# Portions Copyrighted 2011 ForgeRock AS

if [ -z "${JAVA_HOME}" ]; then
  echo "Please define JAVA_HOME environment variable before running this program"
  exit 1
fi

if [ ! -x "$JAVA_HOME"/bin/java ]; then
  echo "The defined JAVA_HOME environment variable is not correct"
  exit 1
fi

AWK=`which awk`
if [ -z $AWK ]; then
    echo "ssodtool fails because awk is not found"
    exit 1
fi

JAVA_VER=`${JAVA_HOME}/bin/java -version 2>&1 | $AWK -F'"' '{print $2}'`

case $JAVA_VER in
1.0* | 1.1* | 1.2* | 1.3* | 1.4*)
  echo "This program is designed to work with 1.5 or newer JRE."
  exit 0
  ;;
esac

HOME="`which $0`"
TOOLS_HOME="`dirname ${HOME}`"

DEBUG_FLAGS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=n"

${JAVA_HOME}/bin/java -cp ${TOOLS_HOME}:${TOOLS_HOME}/config:${TOOLS_HOME}/lib/locale.jar:${TOOLS_HOME}/lib/opensso-sharedlib.jar:${TOOLS_HOME}/lib/amserver.jar:${TOOLS_HOME}/lib/OpenDJ.jar:${TOOLS_HOME}/lib/jaxb-impl.jar:${TOOLS_HOME}/lib/jaxb-api.jar:${TOOLS_HOME}/lib/xsdlib.jar:${TOOLS_HOME}/lib/toolbase.jar:${TOOLS_HOME}/lib/webservices-rt.jar com.sun.identity.diagnostic.base.core.DiagnosticToolMain "$@"
