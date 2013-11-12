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

#Portions Copyrighted 2013 ForgeRock, AS.

SED=`which sed`
UNZIP=`which unzip`
ZIP=`which zip`
ZIP_FILE=opendj.zip
LIST=opendj_inclusion_list
LDIF=template/ldif/
LDIF_FILE=openam_suffix.ldif.template
CONFIG=template/config/config.ldif
CONFIG_UPGRADE_DIR=./template/config/upgrade/

if [ -z ${@} ] ; then
       echo "Error! No command line argument supplied"
       echo "Usage: ./create_opendj_zip.sh OPENDJ_FOLDER"
       exit -1;
fi

PWD=`pwd`
cd "${@}"
cp ../${LDIF_FILE} ${LDIF}
for i in ${CONFIG} template/config/config.ldif ; do
        ${SED} -i -e '/dn: cn=SNMP/,/^$/d' $i
done

#strip out the HTTP Connection Handler class so we don't get a classloader issues with tomcat 6
#and we can use a smaller set of jars
for i in ${CONFIG} template/config/config.ldif ; do
        ${SED} -i -e '/dn: cn=HTTP/,/^$/d' $i
done

#add a config.ldif.${VERSION_NO} file to the upgrade
#directory for easy upgrading from Pre-OPENDJ2.4.5 versions
VERSION_NO=`ls ${CONFIG_UPGRADE_DIR} | sed s/"[a-z/.]*//"`

cp ${CONFIG} ${CONFIG_UPGRADE_DIR}"config.ldif."$VERSION_NO

${ZIP} -r -i@../${LIST} ../${ZIP_FILE} .
cd ${PWD}

