## <img alt="OpenAM Logo" src="https://github.com/OpenIdentityPlatform/OpenAM/raw/master/logo.png" width="300"/>
[![Build Status](https://travis-ci.org/OpenIdentityPlatform/OpenAM.svg)](https://travis-ci.org/OpenIdentityPlatform/OpenAM)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/license/license.txt)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](http://gitter.im/OpenIdentityPlatform)

Open Access Management (OpenAM) is an access management solution that includes Authentication, SSO, Authorization, Federation, Entitlements and Web Services Security.

## License
This project is licensed under the Common Development and Distribution License (CDDL). 

## How-to build
```bash
git clone --recursive  https://github.com/OpenIdentityPlatform/OpenAM.git
mvn clean install -f OpenAM/OpenDJ/forgerock-parent
mvn clean install -f OpenAM/OpenDJ
mvn clean install -f OpenAM
```

## How-to run after build
```bash
mvn tomcat7:run-war -f OpenAM/openam-server
```

## Support and Mailing List Information
* Community Mailing List: open-identity-platform-openam@googlegroups.com
* Community Archive: https://groups.google.com/d/forum/open-identity-platform-openam
* Commercial support RFP: support@openam.org.ru (English, Russian)

## Contributing
Please, make [Pull request](https://github.com/OpenIdentityPlatform/OpenAM/pulls)

## Thanks
* Sun Access Manager
* Sun OpenSSO
* Oracle OpenSSO
* Forgrock OpenAM
