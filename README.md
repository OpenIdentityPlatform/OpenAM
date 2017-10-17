# OpenAM
[![Build Status](https://travis-ci.org/OpenIdentityPlatform/OpenAM.svg)](https://travis-ci.org/OpenIdentityPlatform/OpenAM)

## How-to build

```bash
git clone --recursive  https://github.com/OpenIdentityPlatform/OpenAM.git
mvn clean install -f forgerock-parent
mvn clean install -f OpenDJ-SDK
mvn clean install -f OpenDJ
mvn clean install -f OpenAM
```

About
==========

OpenAM is an "all-in-one" access management solution that provides the following features in a single unified project:

+ Authentication
    - Adaptive 
    - Strong  
+ Single sign-on (SSO)
+ Authorization
+ Entitlements
+ Federation 
+ Web Services Security

OpenAM provides mobile support out of the box, with full OAuth 2.0 and OpenID Connect support - modern protocols that 
provide the most efficient method for developing secure native or HTML5 mobile applications optimized for bandwidth and 
CPU.

## License

This project is licensed under the Common Development and Distribution License (CDDL). 
