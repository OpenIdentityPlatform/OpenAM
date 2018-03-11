## <img alt="OpenAM Logo" src="https://github.com/OpenIdentityPlatform/OpenAM/raw/master/logo.png" width="300"/>
[![Latest release](https://img.shields.io/github/release/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/releases/latest)
[![Build Status](https://travis-ci.org/OpenIdentityPlatform/OpenAM.svg)](https://travis-ci.org/OpenIdentityPlatform/OpenAM)
[![Issues](https://img.shields.io/github/issues/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/issues)
[![Last commit](https://img.shields.io/github/last-commit/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/commits/master)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/OpenIdentityPlatform/OpenAM)
[![Top language](https://img.shields.io/github/languages/top/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)
[![Code size in bytes](https://img.shields.io/github/languages/code-size/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)

Open Access Management (OpenAM) is an access management solution that includes Authentication, SSO, Authorization, Federation, Entitlements and Web Services Security.

## License
This project is licensed under the [Common Development and Distribution License (CDDL)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md). 

## Downloads 
* [OpenAM ZIP](https://github.com/OpenIdentityPlatform/OpenAM/releases/latest) (All OS)
* [OpenAM WAR](https://github.com/OpenIdentityPlatform/OpenAM/releases/latest) (All OS)
* [OpenAM Docker](https://hub.docker.com/r/openidentityplatform/openam/) (All OS)

Java 1.8+ required

## How-to build
```bash
git clone --recursive  https://github.com/OpenIdentityPlatform/OpenAM.git
mvn -DskipTests -Dmaven.javadoc.skip=true install -f OpenDJ/forgerock-parent
mvn -DskipTests -Dmaven.javadoc.skip=true install -f OpenDJ -P '!man-pages,!distribution'
mvn install 
```

## How-to run after build
```bash
mvn tomcat7:run-war -f OpenAM/openam-server
```

## Support and Mailing List Information
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
