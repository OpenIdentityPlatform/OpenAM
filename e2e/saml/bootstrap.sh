#!/bin/bash
set -e

OPENAM_IMAGE=${OPENAM_IMAGE:-"openidentityplatform/openam"}

echo "Using docker image: ${OPENAM_IMAGE}"

docker network create openam-saml 2>/dev/null || true

echo "running OpenAM IDP container..."

docker run --rm -it -d -h idp.acme.org -p 8080:8080 -p 8000:8000 --network openam-saml --name openam-idp \
  -e JPDA_ADDRESS=*:8000 \
  -e JPDA_TRANSPORT=dt_socket \
  ${OPENAM_IMAGE} catalina.sh jpda run

echo "waiting for OpenAM IDP to be alive..."

timeout 3m bash -c 'until docker inspect --format="{{json .State.Health.Status}}" openam-idp | grep -q \"healthy\"; do sleep 10; done'


echo "Running OpenAM IDP setup"
          
docker exec -w '/usr/openam/ssoconfiguratortools' openam-idp bash -c \
'echo "ACCEPT_LICENSES=true
SERVER_URL=http://idp.acme.org:8080
DEPLOYMENT_URI=/$OPENAM_PATH
BASE_DIR=$OPENAM_DATA_DIR
locale=en_US
PLATFORM_LOCALE=en_US
AM_ENC_KEY=
ADMIN_PWD=passw0rd
AMLDAPUSERPASSWD=p@passw0rd
COOKIE_DOMAIN=idp.acme.org
ACCEPT_LICENSES=true
DATA_STORE=embedded
DIRECTORY_SSL=SIMPLE
DIRECTORY_SERVER=idp.acme.org
DIRECTORY_PORT=50389
DIRECTORY_ADMIN_PORT=4444
DIRECTORY_JMX_PORT=1689
ROOT_SUFFIX=dc=openam,dc=example,dc=org
DS_DIRMGRDN=cn=Directory Manager
DS_DIRMGRPASSWD=passw0rd" > conf.file && java -jar openam-configurator-tool*.jar --file conf.file'

echo "Setup ssoadm tools for OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools' openam-idp bash -c './setup -p /usr/openam/config --acceptLicense'

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c 'echo passw0rd > pwd.txt && chmod 400 pwd.txt'

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
  --entityid http://idp.acme.org:8080/openam \
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

echo "Create hosted identity provider for OpenAM IDP"

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-idp bash -c \
'./ssoadm import-entity \
  --adminid amadmin \
  --password-file pwd.txt \
  --realm / \
  --cot MYSAML \
  --meta-data-file idp-metadata.xml \
  --extended-data-file idp-extended.xml'

echo "Running OpenAM SP setup"

docker run --rm -it -d -h sp.mycompany.org -p 8081:8080 -p 8001:8000 --network openam-saml --name openam-sp \
  -e JPDA_ADDRESS=*:8000 \
  -e JPDA_TRANSPORT=dt_socket \
  ${OPENAM_IMAGE} catalina.sh jpda run

echo "waiting for OpenAM SP to be alive..."

timeout 3m bash -c 'until docker inspect --format="{{json .State.Health.Status}}" openam-sp | grep -q \"healthy\"; do sleep 10; done'

echo "Running OpenAM SP setup"
          
docker exec -w '/usr/openam/ssoconfiguratortools' openam-sp bash -c \
'echo "ACCEPT_LICENSES=true
SERVER_URL=http://sp.mycompany.org:8080
DEPLOYMENT_URI=/$OPENAM_PATH
BASE_DIR=$OPENAM_DATA_DIR
locale=en_US
PLATFORM_LOCALE=en_US
AM_ENC_KEY=
ADMIN_PWD=passw0rd
AMLDAPUSERPASSWD=p@passw0rd
COOKIE_DOMAIN=sp.mycompany.org
ACCEPT_LICENSES=true
DATA_STORE=embedded
DIRECTORY_SSL=SIMPLE
DIRECTORY_SERVER=sp.mycompany.org
DIRECTORY_PORT=50389
DIRECTORY_ADMIN_PORT=4444
DIRECTORY_JMX_PORT=1689
ROOT_SUFFIX=dc=openam,dc=example,dc=org
DS_DIRMGRDN=cn=Directory Manager
DS_DIRMGRPASSWD=passw0rd" > conf.file && java -jar openam-configurator-tool*.jar --file conf.file'


echo "Setup ssoadm tools for OpenAM SP"

docker exec -w '/usr/openam/ssoadmintools' openam-sp bash -c './setup -p /usr/openam/config --acceptLicense'

docker exec -w '/usr/openam/ssoadmintools/openam/bin' openam-sp bash -c 'echo passw0rd > pwd.txt && chmod 400 pwd.txt'

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