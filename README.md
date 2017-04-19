# OpenAM Community Edition 11.0.3

Traditionally delivered as six different products — SSO, adaptive authentication, strong authentication, federation, web services security and fine-grained entitlement enforcement — OpenAM now provides all this in a single, unified cross platform offering deployed as a .war file into a Java Servlet container such as Tomcat.

### About the Community Version

Originally based on Sun MicroSystem's OpenSSO, ForgeRock have been developing and commercially supporting OpenAM since 2010. This version was originally released to ForgeRock customers in March 2015, and is now being released as our Community Edition without the ForgeRock binary licensing restrictions. It is well tested an has managed millions of identities in its lifetime.

To find out about the enterprise release of the ForgeRock platform [here][ForgeRock Identity Platform].

## How do I build it?

Best built on linux or OS X. Builds are possible on Windows, but more of a challenge. 

####Environment (Pre-requisites)

Software          | Version
------------------|--------
Apache Maven      | 3.0.5  
JDK version       | Oracle JDK 1.6 or 1.7
Git               | 1.7.6 or above


The Community Edition Releases are built using Oracle JDK 1.7.0_80

1. Clone the repository, or Fork it and clone your Fork if you want to create pull requests:
`git clone https://github.com/ForgeRock/openam-community-edition-11.0.3.git`
2. `cd openam-community-edition-11.0.3`
3. `mvn clean install`



### Modifying the GitHub Project Page

The OpenAM Community Edition project pages are published via the `gh-pages` branch, which contains all the usual artifacts to create the web page. The GitHub page is served up directly from this branch by GitHub.

## Getting Started with OpenAM

ForgeRock provide a comprehensive set of documents for OpenAM. They maybe found [here][OpenAM 11.0.3 Docs] and [here][OpenAM 11 Docs].

## Issues

Issues are handled via the [GitHub issues page for the project][GitHub Issues].

## How to Collaborate

Collaborate by:

- [Reporting an issue][GitHub Issues]
- [Fixing an issue][Help Wanted Issues]
- [Contributing to the Wiki][Project Wiki]

Code collaboration is done by creating an issue, discussing the changes in the issue. When the issue's been agreed then, fork, modify, test and submit a pull request. 

## Licensing

The Code and binaries are covered under the [CDDL 1.0 license](https://forgerock.org/cddlv1-0/). Essentially you may use the release binaries in production at your own risk. 

#### Legal Disclaimer Bit
All components herein are provided AS IS and without a warranty of any kind by ForgeRock or any licensors of such code.  ForgeRock and all licensors of such code disclaims all liability or obligations of such code and any use, distribution or operation of such code shall be exclusively subject to the licenses contained in each file and recipient is obligated to read and understand each such license before using the Software.  Notwithstanding anything to the contrary, ForgeRock has no obligations for the use or support of such code under any ForgeRock license agreement.


# All the Links

- [GitHub Project]
- [Project Wiki]
- [GitHub Issues]
- [Help Wanted Issues]
- [OpenAM 11.0.3 Docs]
- [OpenAM 11 Docs]
- [ForgeRock Identity Platform]

[GitHub Project]:https://github.com/ForgeRock/openam-community-edition-11.0.3
[GitHub Issues]:https://github.com/ForgeRock/openam-community-edition-11.0.3/issues
[Help Wanted Issues]:https://github.com/ForgeRock/opendj-community-edition-11.0.3/labels/help%20wanted
[Project Wiki]:https://github.com/ForgeRock/openam-community-edition-11.0.3/wiki
[ForgeRock Identity Platform]:https://www.forgerock.com/platform/
[OpenAM 11 Docs]:https://backstage.forgerock.com/docs/openam/11.0.0
[OpenAM 11.0.3 Docs]:https://backstage.forgerock.com/docs/openam/11.0.3