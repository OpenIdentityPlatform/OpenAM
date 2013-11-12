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

# Portions Copyrighted 2011-2012 ForgeRock Inc

if [ -z "$JAVA_HOME" ]; then
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

JAVA_VER=`$JAVA_HOME/bin/java -version 2>&1 | $AWK -F'"' '{print $2}'`

case $JAVA_VER in
1.0* | 1.1* | 1.2* | 1.3* | 1.4*)
  echo "This program is designed to work with 1.5 or newer JRE."
  exit 0
  ;;
esac

HOME="`which $0`"
TOOLS_HOME="`dirname $HOME`"

EXT_CLASSPATH=$CLASSPATH

CLASSPATH="resources"
CLASSPATH="$CLASSPATH:lib/forgerock-util-${commons.forgerock-util.version}.jar"
CLASSPATH="$CLASSPATH:lib/openam-diagnostics-base-${project.version}.jar"
CLASSPATH="$CLASSPATH:lib/openam-diagnostics-schema-${project.version}.jar"
CLASSPATH="$CLASSPATH:lib/openam-core-${project.version}.jar"
CLASSPATH="$CLASSPATH:lib/openam-shared-${project.version}.jar"
CLASSPATH="$CLASSPATH:lib/jaxb-api-1.0.6.jar"
CLASSPATH="$CLASSPATH:lib/jaxb-impl-1.0.6.jar"
CLASSPATH="$CLASSPATH:lib/opendj-server-${opendj.server.version}.jar"
CLASSPATH="$CLASSPATH:lib/xsdlib-20060615.jar"
CLASSPATH="$CLASSPATH:lib/webservices-rt-2009-29-07.jar"

if [ -n "$EXT_CLASSPATH" ] ; then
        CLASSPATH=$EXT_CLASSPATH:$CLASSPATH
fi

DEBUG_FLAGS="-Xdebug -Xrunjdwp:transport=dt_socket,address=9009,server=y,suspend=y"

$JAVA_HOME/bin/java -cp $CLASSPATH \
   com.sun.identity.diagnostic.base.core.DiagnosticToolMain "$@"
