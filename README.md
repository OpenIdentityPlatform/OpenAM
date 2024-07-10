## <img alt="OpenAM Logo" src="https://github.com/OpenIdentityPlatform/OpenAM/raw/master/logo.png" width="300"/>
[![Latest release](https://img.shields.io/github/release/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/releases)
[![Build](https://github.com/OpenIdentityPlatform/OpenAM/actions/workflows/build.yml/badge.svg)](https://github.com/OpenIdentityPlatform/OpenAM/actions/workflows/build.yml)
[![Deploy](https://github.com/OpenIdentityPlatform/OpenAM/actions/workflows/deploy.yml/badge.svg)](https://github.com/OpenIdentityPlatform/OpenAM/actions/workflows/deploy.yml)
[![Issues](https://img.shields.io/github/issues/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/issues)
[![Last commit](https://img.shields.io/github/last-commit/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM/commits/master)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md)
[![Downloads](https://img.shields.io/github/downloads/OpenIdentityPlatform/OpenAM/total.svg)](https://github.com/OpenIdentityPlatform/OpenAM/releases)
[![Docker](https://img.shields.io/docker/pulls/openidentityplatform/openam.svg)](https://hub.docker.com/r/openidentityplatform/openam)
[![Top language](https://img.shields.io/github/languages/top/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)
[![Code size in bytes](https://img.shields.io/github/languages/code-size/OpenIdentityPlatform/OpenAM.svg)](https://github.com/OpenIdentityPlatform/OpenAM)

Open Access Management (OpenAM) is an access management solution that includes Authentication, SSO, Authorization, Federation, Entitlements, and Web Services Security.

Cross Domain Single Sign On (CDSSO), SAML 2.0, OAuth 2.0 & OpenID Connect ensure that OpenAM integrates easily with legacy, custom, and cloud applications without requiring any modifications. 
It's a developer-friendly, open-source control solution that allows you to own and protect your user's digital identities.

## Key Features
### Authentication Management
With OpenAM you can set up complex authentication processes using various authentication methods,
such as login and password, OTP, saved cookie, QR authentication, and more...
OpenAM also supports third-party identity providers using SAML, OAuth2, NTLM, and Kerberos protocols.
### Access Management
Integrations with [OpenIG](https://github.com/OpenIdentityPlatform/OpenIG) or OpenAM Policy Agent allow you to set up flexible access policies to your resources.
There could be role-based, authentication level-based, or attribute-based and, if you need flexible logic, you can script access policy.
### Cross Domain Single Sign-On
After a single authentication, a user gets access to all resources protected by OpenAM. So, there is no need to authenticate at other services.
### Federation
OpenAM supports OAuth2/OIDC and SAMLv2 Federation protocols, so OpenAM can act as both Identity and Service Provider.
### Extensibility
If you have to extend OpenAM functionality, it is relatively easy to do. OpenAM pluggable architecture allows modification relatively easy.
You can implement your custom authentication module, user data source, session data source, post-authentication process logic, and more...

## License
This project is licensed under the [Common Development and Distribution License (CDDL)](https://github.com/OpenIdentityPlatform/OpenAM/blob/master/LICENSE.md). 

## Downloads 
* [OpenAM Distribution Packages](https://github.com/OpenIdentityPlatform/OpenAM/releases) (All OS)
* [OpenAM Docker Image](https://hub.docker.com/r/openidentityplatform/openam/) (All OS)

### Download OpenAM Policy Agents:
* [OpenIG gateway/reverse proxy/authorization endpoint](https://github.com/OpenIdentityPlatform/OpenIG/releases)
* [OpenAM Java Policy Agent](https://github.com/OpenIdentityPlatform/OpenAM-JEE-Agents#downloads) (All OS)
* [OpenAM .Net/Mono Policy Agents](https://github.com/OpenIdentityPlatform/OpenAM-.Net-Agent#install-binary-distribution) (Windows/Linux)
* [OpenAM Web Policy Agent](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents)
  * [Apache 2.2 x64](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents#downloads) 
  * [Apache 2.4 x64](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents#downloads) 
  * [IIS  Windows x32/x64](https://github.com/OpenIdentityPlatform/OpenAM-Web-Agents/releases)

## How-to Build OpenAM from Source
To build OpenAM from source you should use JDK 8 or higher

For Windows users before clone and build run the following command:
```bash
git config --system core.longpaths true
```

```bash
git clone https://github.com/OpenIdentityPlatform/OpenAM.git
mvn install -f OpenAM
```

## How-to Run After the Build
Add FQDN host name in `/etc/hosts` (Windows `c:\windows\systems32\drivers\etc\hosts`) file: 

```bash
127.0.0.1 login.domain.com
```

Run OpenAM from source:

```bash
mvn cargo:run -f OpenAM/openam-server
```

The next step is then to go to [http://login.domain.com:8080/openam](http://login.domain.com:8080/openam) where you'll see the OpenAM welcome 


## Support
* OpenAM Community [documentation](https://github.com/OpenIdentityPlatform/OpenAM/wiki)
* OpenAM Community [discussions](https://github.com/OpenIdentityPlatform/OpenAM/discussions)
* OpenAM Community [issues](https://github.com/OpenIdentityPlatform/OpenAM/issues)
* OpenAM [commercial support](https://github.com/OpenIdentityPlatform/.github/wiki/Approved-Vendor-List)

## Contributing
Please, make [Pull request](https://github.com/OpenIdentityPlatform/OpenAM/pulls)

## Thanks 🥰
* Sun Access Manager/OpenSSO
* Oracle OpenSSO
* Forgerock OpenAM
