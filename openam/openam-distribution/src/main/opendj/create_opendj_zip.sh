#!/bin/bash

# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010-2011 ForgeRock AS. All Rights Reserved
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
# "Portions Copyrighted [year] [name of copyright owner]"

# This script creates the cut down OpenDJ.zip for inclusion in the build
# OpenDJ libraries must be copied into extlib manually

SED=`which sed`
UNZIP=`which unzip`
ZIP=`which zip`
ZIP_FILE=opendj.zip
LIST=opendj_inclusion_list
LDIF=ldif
LDIF_FILE=openam_suffix.ldif.template
CONFIG=config/config.ldif

if [ -z ${@} ] ; then
       echo "Error! No command line argument supplied"
       echo "Usage: ./create_opendj_zip.sh OPENDJ_FOLDER"
       exit -1;
fi

PWD=`pwd`
cd "${@}"
cp ../${LDIF_FILE} ${LDIF}
for i in ${CONFIG} config/upgrade/config.ldif.* ; do
        ${SED} -i -e '/dn: cn=SNMP/,/^$/d' $i
done
${ZIP} -r -i@../${LIST} ../${ZIP_FILE} .
cd ${PWD}

