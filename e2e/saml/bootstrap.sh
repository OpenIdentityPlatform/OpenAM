#!/bin/bash

# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2026 3A Systems, LLC.


set -e


echo "Setup COT for OpenAM IDP"
docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c \
'./ssoadm create-cot \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML'

echo "Create Metadata Template for OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c \
'./ssoadm create-metadata-templ \
  --adminid amadmin \
  --password-file pwd.txt \
  --entityid http://openam.example.org:8080/openam \
  --identityprovider /idp \
  --idpscertalias test \
  --meta-data-file idp-metadata.xml \
  --extended-data-file idp-extended.xml'

echo "modify idp-extended.xml"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c '
sed -i "
  /<Attribute name=\"attributeMap\">/{
    N
    s|<Attribute name=\"attributeMap\">\s*</Attribute>|<Attribute name=\"attributeMap\">\n  <Value>uid=uid</Value>\n</Attribute>|
  }
" idp-extended.xml'

echo "Create a hosted identity provider for OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c \
'./ssoadm import-entity \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML \
  --meta-data-file idp-metadata.xml \
  --extended-data-file idp-extended.xml'

echo "Running OpenAM SP setup"

echo "Setup COT for OpenAM SP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
'./ssoadm create-cot \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML'

echo "Create Metadata Template for OpenAM SP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
'./ssoadm create-metadata-templ \
  --adminid amadmin \
  --password-file pwd.txt \
  --entityid http://sp.mycompany.org:8081/openam \
  --serviceprovider /sp \
  --spscertalias test \
  --meta-data-file sp-metadata.xml \
  --extended-data-file sp-extended.xml'

echo "modify sp-metadata.xml"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
  "sed -i 's/http:\/\/sp.mycompany.org:8080/http:\/\/sp.mycompany.org:8081/g' sp-metadata.xml"

echo "modify sp-extended.xml"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c '
sed -i "
  /<Attribute name=\"attributeMap\">/{
    N
    s|<Attribute name=\"attributeMap\">\s*</Attribute>|<Attribute name=\"attributeMap\">\n  <Value>\*=\*</Value>\n</Attribute>|
  }
" sp-extended.xml'  

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp \
  sed -i '/name="autofedEnabled"/{n;s/false/true/}' sp-extended.xml

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp \
  sed -i '/name="autofedAttribute"/{n;s|<Value></Value>|<Value>uid</Value>|}' sp-extended.xml


echo "Create hosted identity provider for OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
'./ssoadm import-entity \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML \
  --meta-data-file sp-metadata.xml \
  --extended-data-file sp-extended.xml'

echo "Exchange providers metadata between contaners"

docker exec openam-sp cat /usr/openam/ssoadmintools/openam/bin/sp-metadata.xml | docker exec -i openam-idp bash -c 'cat > /usr/openam/ssoadmintools/openam/bin/remote-sp-metadata.xml'

docker exec openam-idp cat /usr/openam/ssoadmintools/openam/bin/idp-metadata.xml | docker exec -i openam-sp bash -c 'cat > /usr/openam/ssoadmintools/openam/bin/remote-idp-metadata.xml'

echo "Create remote SP in OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c \
'./ssoadm import-entity \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML \
  --meta-data-file remote-sp-metadata.xml'

echo "Create remote IDP in OpenAM SP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
'./ssoadm import-entity \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML \
  --meta-data-file remote-idp-metadata.xml'

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c \
'./ssoadm set-realm-svc-attrs \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --servicename iPlanetAMAuthService \
  --attributevalues iplanet-am-auth-dynamic-profile-creation=ignore'