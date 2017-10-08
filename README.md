# OpenAM

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

The project is led by ForgeRock who integrate the OpenAM, OpenIDM, OpenDJ, OpenICF, and OpenIG open source projects to 
provide a quality-assured Identity Platform. Support, professional services, and training are available for the Identity
 Platform, providing stability and safety for the management of your digital identities. 

To find out more about the services ForgeRock provides, visit [www.forgerock.com][commercial_site].

To view the OpenAM project page, which also contains all of the documentation, visit
 [https://forgerock.org/openam/][project_page]. 

For a great place to start, take a look at [Getting Started With OpenAM]
(https://forgerock.org/openam/doc/bootstrap/getting-started/index.html "Getting Started With OpenAM").

For further help and discussion, visit the [community forums][community_forum].

# Getting the OpenAM Application

You can obtain the OpenAM Web Application Archive (WAR) file in the following ways:

## Download It 

The easiest way to try OpenAM is to download the WAR file and follow the [Getting Started With OpenAM](https://forgerock.org/openam/doc/bootstrap/getting-started/index.html "Getting Started With OpenAM") guide. 

You can download either:

1. An [enterprise release build][enterprise_builds].
2. The [nightly build][nightly_builds] which contains the latest features and bug fixes, but may also contain 
_in progress_ unstable features.

## Build The Source Code

In order to build the project from the command line follow these steps:

### Prepare your Environment

You will need the following software to build your code.

Software               | Required Version
---------------------- | ----------------
Java Development Kit   | 1.7 and above
Maven                  | 3.1.0 and above
Git                    | 1.7.6 and above

The following environment variables should be set:

- `JAVA_HOME` - points to the location of the version of Java that Maven will use.
- `MAVEN_OPTS` - sets some options for the jvm when running Maven.

For example your environment variables might look something like this:

```
JAVA_HOME=/usr/jdk/jdk1.7.0_79.jdk
MAVEN_OPTS='-Xmx2g -Xms2g -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512m'
```

### Getting the Code

The central project repository lives on the ForgeRock Bitbucket Server at 
[https://stash.forgerock.org/projects/OPENAM][central_repo].

Mirrors exist elsewhere (for example GitHub) but all contributions to the project are managed by using pull requests 
to the central repository.

There are two ways to get the code - if you want to run the code unmodified you can simply clone the central repo (or a 
reputable mirror):

```
git clone https://stash.forgerock.org/scm/openam/openam.git
```

If, however, you are considering contributing bug fixes, enhancements, or modifying the code you should fork the project
 and then clone your private fork, as described below:

1. Create an account on [BackStage][backstage] - You can use these credentials to create pull requests, report bugs, and
 download the enterprise release builds.
2. Log in to the Bitbucket Server using your BackStage account credentials. 
3. Fork the `openam` project. This will create a fork for you in your own area of Bitbucket Server. Click on your 
profile icon then select 'view profile' to see all your forks. 
4. Clone your fork to your machine.

Obtaining the code this way will allow you to create pull requests later. 

### Building the Code

The OpenAM build process and dependencies are managed by Maven. The first time you build the project, Maven will pull 
down all the dependencies and Maven plugins required by the build, which can take a significant amount of time. 
Subsequent builds will be much faster!

```
cd openam
mvn clean install
```

Maven builds the binary in `openam/openam-server/target`. The file name format is `OpenAM-<nextversion>-SNAPSHOT.war`, 
for example "OpenAM-14.0.0-SNAPSHOT.war".

## Tests

ForgeRock undertake functional, performance, and security testing on the Identity Platform, including OpenAM. To license
 a fully tested build of the Identity Platform check out 
[how to buy][how_to_buy]. 

### Checkstyle Tests

ForgeRock adhere to a set of [coding standards][coding_standards]. A set of checkstyle rules enforce these standards, 
and may be run by building using the `precommit` Maven profile:

```
mvn clean install -P precommit
```

Some legacy code will fail, so if you are modifying an existing module you should run this profile before modifying the 
code, and then run the profile again after modifications to ensure the number of reported issues has not increased. 

### Unit Tests

Unit tests are provided with the project and are run by Maven as part of the build. OpenAM uses the "_TestNG_" 
framework. Unit tests should be written for all new code.

You can run just the tests:

`mvn test`

Or build without running tests:

`mvn clean install -DskipTests`

All new code and modifications should be covered by unit tests.

## Getting Started With OpenAM

ForgeRock provide a comprehensive set of documents for OpenAM, including getting started and installation guides:

- [Documentation for enterprise builds][enterprise_docs].
- [Draft docs for nightly builds and self built code][nightly_docs]

## Contributing

There are many ways to contribute to the OpenAM project. You can contribute to the [OpenAM Docs Project][docs_project], 
report or [submit bug fixes][issue_tracking], or [contribute extensions][contribute] such as custom authentication 
modules, authentication scripts, policy scripts, dev ops scripts, and more.

## Versioning

ForgeRock produce an enterprise point release build. These builds use the versioning format X.0.0 (for example 12.0.0, 
13.0.0) and are produced yearly. These builds are free to use for trials, proof of concept projects and so on. A license
 is required to use these builds in production.

Those with support contracts have access to sustaining releases that contain bug and security fixes. These builds use 
the versioning format 13.0.X (for example 13.0.1, 13.0.2). Those with support contracts also get access to 
quality-assured interim releases, such as OpenAM 13.5.0. 

## Authors

See the list of [contributors][contributors] who participated in this project.

## License

This project is licensed under the Common Development and Distribution License (CDDL). The following text applies to 
both this file, and should also be included in all files in the project:

> The contents of this file are subject to the terms of the Common Development and  Distribution License (the License). 
> You may not use this file except in compliance with the License.  
>   
> You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the specific language governing 
> permission and limitations under the License.  
>  
> When distributing Covered Software, include this CDDL Header Notice in each file and include the License file at 
> legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header, with the fields enclosed by brackets [] 
> replaced by your own identifying information: "Portions copyright [year] [name of copyright owner]".  
>   
> Copyright 2016 ForgeRock AS.    

## All the Links!
To save you sifting through the readme looking for 'that link'...

- [ForgeRock's commercial website][commercial_site]
- [ForgeRock's community website][community_site]
- [ForgeRock's BackStage server][backstage] 
- [OpenAM Project Page][project_page]
- [Community Forums][community_forum]
- [Enterprise Build Downloads][enterprise_builds]
- [Enterprise Documentation][enterprise_docs]
- [Nightly Build Downloads][nightly_builds]
- [Nightly Documentation][nightly_docs]
- [Central Project Repository][central_repo]
- [Issue Tracking][issue_tracking]
- [Contributors][contributors]
- [Coding Standards][coding_standards]
- [Contributions][contribute]
- [How to Buy][how_to_buy]

[commercial_site]: https://www.forgerock.com
[community_site]: https://www.forgerock.org
[backstage]: https://backstage.forgerock.com
[project_page]: https://forgerock.org/openam/
[community_forum]: https://forgerock.org/forum/fr-projects/openam/
[enterprise_builds]: https://backstage.forgerock.com/#!/downloads/OpenAM/OpenAM%20Enterprise#browse
[enterprise_docs]: https://backstage.forgerock.com/#!/docs/openam
[nightly_builds]: https://forgerock.org/downloads/openam-builds/
[nightly_docs]: https://forgerock.org/documentation/openam/
[central_repo]: https://stash.forgerock.org/projects/OPENAM
[issue_tracking]: http://bugster.forgerock.org/
[docs_project]: https://stash.forgerock.org/projects/OPENAM/repos/openam-docs/browse
[contributors]: https://stash.forgerock.org/plugins/servlet/graphs?graph=contributors&projectKey=OPENAM&repoSlug=openam&refId=all-branches&type=c&group=weeks
[coding_standards]: https://wikis.forgerock.org/confluence/display/devcom/Coding+Style+and+Guidelines
[how_to_buy]: https://www.forgerock.com/platform/how-buy/
[contribute]: https://forgerock.org/projects/contribute/

## Acknowledgments

* Sun Microsystems.
* The founders of ForgeRock.
* The good things in life.