#!/bin/bash
#
# Script used within Jenkins Build Process for OpenAM
# Prepare Artifacts for versioning on Download Site.
#
OPENAM_VERSION="10.1.0-SNAPSHOT";
mv "$WORKSPACE"/openam-server/target/openam-server-${OPENAM_VERSION}.war \
    "$WORKSPACE"/openam-server/target/openam-server-${OPENAM_VERSION}_`date "+%Y%m%d"`.war

mv openam-distribution/openam-distribution-ssoadmintools/target/openam-distribution-ssoAdminTools-${OPENAM_VERSION}.zip \
   openam-distribution/openam-distribution-ssoadmintools/target/openam-distribution-ssoAdminTools-${OPENAM_VERSION}_`date "+%Y%m%d"`.zip

mv openam-distribution/openam-distribution-ssoconfiguratortools/target/openam-distribution-ssoconfiguratortools-${OPENAM_VERSION}.zip \
   openam-distribution/openam-distribution-ssoconfiguratortools/target/openam-distribution-ssoconfiguratortools-${OPENAM_VERSION}_`date "+%Y%m%d"`.zip

mv openam-distribution/openam-distribution-kit/target/openam-distribution-kit-${OPENAM_VERSION}.zip \
   openam-distribution/openam-distribution-kit/target/openam-distribution-kit-${OPENAM_VERSION}_nightly_`date "+%Y%m%d"`.zip

