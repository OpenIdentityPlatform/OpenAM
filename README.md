## <img alt="OpenAM Logo" src="https://github.com/OpenIdentityPlatform/OpenAM/raw/master/logo.png" width="300"/>
[![Latest release](https://img.shields.io/github/release/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/releases)
[![Build Status](https://travis-ci.org/OpenIdentityPlatform/OpenAM.svg)](https://travis-ci.org/OpenIdentityPlatform/OpenAM)
[![Issues](https://img.shields.io/github/issues/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/issues)
[![Last commit](https://img.shields.io/github/last-commit/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/commits/master)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md)
[![Downloads](https://img.shields.io/github/downloads/OpenIdentityPlatform/OpenAM/total.svg)](https://github.com/OpenIdentityPlatform/OpenAM/releases)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/OpenIdentityPlatform/OpenAM)
[![Top language](https://img.shields.io/github/languages/top/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)
[![Code size in bytes](https://img.shields.io/github/languages/code-size/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)

Open Access Management (OpenAM) is an access management solution that includes Authentication, SSO, Authorization, Federation, Entitlements and Web Services Security.

Cross Domain Single Sign On (CDSSO), SAML 2.0, OAuth 2.0 & OpenID Connect ensure that OpenAM integrates easily with legacy, custom and cloud applications without requiring any modifications. It's a developer-friendly, open-source control solution that allows you to own and protect your users digital identities.

## License
This project is licensed under the [Common Development and Distribution License (CDDL)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md). 

## Downloads 
* [OpenAM ZIP](https://github.com/OpenIdentityPlatform/OpenAM/releases) (All OS)
* [OpenAM WAR](https://github.com/OpenIdentityPlatform/OpenAM/releases) (All OS)
* [OpenAM Docker](https://hub.docker.com/r/openidentityplatform/openam/) (All OS)
### Download OpenAM Policy Agents:
* [OpenAM Java Policy Agent](https://github.com/OpenIdentityPlatform/OpenAM-JEE-Agents#downloads) (All OS)
* [OpenAM .Net/Mono Policy Agents](https://github.com/OpenIdentityPlatform/OpenAM-.Net-Agent#Установка-файлов-бинарной-поставки) (Windows/Linux)
* [OpenAM Web Policy Agent Apache 2.2](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents#downloads) (Linux x64)
* [OpenAM Web Policy Agent Apache 2.4](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents#downloads) (Linux x64)
* [OpenAM Web Policy Agent (IIS  Windows x32/x64 ZIP)](https://ci.appveyor.com/api/buildjobs/cnebrw2f43my9vxr/artifacts/IIS_WINNT_4.1.0.zip)

## How-to build
For windows use:
```bash
git config --system core.longpaths true
```

```bash
git clone --recursive  https://github.com/OpenIdentityPlatform/OpenAM.git
mvn -DskipTests -Dmaven.javadoc.skip=true install -f OpenAM/OpenDJ/forgerock-parent
mvn -DskipTests -Dmaven.javadoc.skip=true install -f OpenAM/OpenDJ
mvn install -f OpenAM
```

## How-to run after build
```bash
mvn tomcat7:run-war -f OpenAM/openam-server
```
The next step is then to go to [http://localhost:8080/openam](http://localhost:8080/openam) where you'll see the OpenAM welcome page

## Support and Mailing List Information
* OpenAM Community Wiki: https://github.com/OpenIdentityPlatform/OpenAM/wiki
* OpenAM Community Mailing List: open-identity-platform-openam@googlegroups.com
* OpenAM Community Archive: https://groups.google.com/d/forum/open-identity-platform-openam
* OpenAM Community on Gitter: https://gitter.im/OpenIdentityPlatform/OpenAM
* OpenAM Commercial support RFP: support@openam.org.ru (English, Russian)

## Contributing
Please, make [Pull request](https://github.com/OpenIdentityPlatform/OpenAM/pulls)

## Thanks for OpenAM
* Sun Access Manager
* Sun OpenSSO
* Oracle OpenSSO
* Forgerock OpenAM
