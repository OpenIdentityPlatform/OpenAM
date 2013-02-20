ScriptedCondition - JavaScript policy condition
by Robert Meakins 19/02/2013


Description
============
This custom condition allows the administrator to write JavaScript to obtain a
ConditionResult when a policy is evaluated.

Once installed, you can add this condition to any policy and specify a
different script for each one.

In sample benchmarks using a virtual machine running on a laptop, a simple test
case showed the averaged JavaScript execution time was 35 milliseconds,
compared to an equivalent Java class which took less than 1 ms to execute.

Instructions
============

To install this plugin, there is a 3 step process:
1. Modify files inside the "classes" directory in the deployed OpenAM war:
2. Run ssoadm (or ssoadm.jsp) commands to register the plugin
3. Restart OpenAM



1. Files to edit:

amPolicy.properties:
scripted-policy-condition-name=ScriptedCondition


amPolicyConfig.properties:
x101=ScriptedCondition


2: ssoadm commands:

add-plugin-schema:
Name of service: iPlanetAMPolicyService
Name of interface: Condition
Name of Plug-in: ScriptedCondition
Plug-in I18n Key: scripted-policy-condition-name
Plug-in I18n Name: amPolicy
Name of the Plugin Schema class implementation: com.sun.identity.policy.plugins.ScriptedCondition


set-attr-choicevals:
Name of service: iPlanetAMPolicyConfigService
Type of schema: Organization
Name of attribute: iplanet-am-policy-selected-conditions
Set this flag to append the choice values to existing ones: true
Name of sub schema: 
Choice value e.g. o102=Inactive: x101=ScriptedCondition


add-attr-defs:
Name of service: iPlanetAMPolicyConfigService
Type of schema: Organization
Attribute values e.g. homeaddress=here: iplanet-am-policy-selected-conditions=ScriptedCondition
Name of sub schema:




Usage examples
==============


Example 1:

env.get('requestIp').startsWith('192.168.');



Example 2:

if (token.get('Principal').endsWith('dc=forgerock,dc=org')) {
    'true';
} else {
    'false,AuthenticateToRealmConditionAdvice,ForgeRockRealm';
}


Available Properties
====================
It's advised to test this yourself since the available properties may vary.

The env.get() method has these properties available:

{
 sun.am.requestedResource=[http://openam.example.com:80]
 invocatorPrincipalUuid=[id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org]
 requestIp=192.168.1.10
 sun.am.requestedActions=[POST, GET]
 sun.am.policyConfig={iplanet-am-policy-config-connection_pool_min_size=1
 iplanet-am-policy-config-is-roles-base-dn=dc=openam,dc=forgerock,dc=org
 iplanet-am-policy-config-search-limit=100
 iplanet-am-policy-selected-referrals=[SubOrgReferral, PeerOrgReferral]
 iplanet-am-policy-config-ldap-ssl-enabled=false
 iplanet-am-policy-config-ldap-roles-search-scope=SCOPE_SUB
 iplanet-am-policy-config-ldap-users-base-dn=dc=openam,dc=forgerock,dc=org
 iplanet-am-policy-config-ldap-roles-search-attribute=cn
 iplanet-am-policy-config-ldap-organizations-search-scope=SCOPE_SUB
 iplanet-am-policy-config-ldap-roles-search-filter=(&(objectclass=ldapsubentry)(objectclass=nsroledefinition))
 iplanet-am-policy-config-subjects-result-ttl=10
 iplanet-am-policy-config-search-timeout=5
 iplanet-am-policy-config-ldap-organizations-search-filter=(objectclass=sunismanagedorganization)
 iplanet-am-policy-selected-subjects=[AuthenticatedUsers, WebServicesClients, AMIdentitySubject]
 iplanet-am-policy-config-ldap-users-search-filter=(objectclass=inetorgperson)
 iplanet-am-policy-config-ldap-server=localhost:50389
 iplanet-am-policy-config-ldap-bind-dn=cn=Directory Manager
 iplanet-am-policy-config-ldap-groups-search-scope=SCOPE_SUB
 iplanet-am-policy-config-connection_pool_max_size=10
 iplanet-am-policy-config-ldap-users-search-scope=SCOPE_SUB
 OrganizationName=[o=Example,ou=services,dc=openam,dc=forgerock,dc=org]
 iplanet-am-policy-config-ldap-groups-search-attribute=cn
 sun-am-policy-selected-responseproviders=[IDRepoResponseProvider]
 iplanet-am-policy-config-ldap-users-search-attribute=uid
 iplanet-am-policy-selected-conditions=[LEAuthLevelCondition, SessionPropertyCondition, SessionCondition, LDAPFilterCondition, AMIdentityMembershipCondition, IPCondition, ScriptedCondition, ResourceEnvIPCondition, AuthLevelCondition, AuthenticateToRealmCondition, AuthenticateToServiceCondition, SimpleTimeCondition AuthSchemeCondition]
 iplanet-am-policy-config-ldap-base-dn=dc=openam,dc=forgerock,dc=org
 iplanet-am-policy-config-ldap-organizations-search-attribute=o
 orgDN=o=example,ou=services,dc=openam,dc=forgerock,dc=org,
 iplanet-am-policy-config-user-alias-enabled=false
 iplanet-am-policy-config-ldap-bind-password=AQICGp+4uAV0aUfYTdCuu/O9s2ZVQVLfRkgU
 iplanet-am-policy-config-is-roles-search-scope=SCOPE_SUB
 iplanet-am-policy-config-ldap-groups-search-filter=(objectclass=groupOfUniqueNames)}
 sun.am.requestedOriginalResource=[http://openam.example.com:80]
}

And the token.get() method (for the "demo" user):

{
 CharSet=UTF-8
 UserId=demo
 FullLoginURL=/openam/UI/Login?realm=Example&goto=http%3A%2F%2Fopenam.example.com%2F
 successURL=/openam/console
 cookieSupport=true
 AuthLevel=0
 SessionHandle=shandle:AQIC5wM2LY4SfcxJ18vM4nejMRpwKWeqSkht2uSVR-rsG6U.*AAJTSQACMDEAAlNLABM1MDU1Njk4MzI5MTg3MDM0MTg5*
 UserToken=demo
 loginURL=/openam/UI/Login
 Principals=demo
 Service=ldapService
 sun.am.UniversalIdentifier=id=demo,ou=user,o=example,ou=services,dc=openam,dc=forgerock,dc=org
 amlbcookie=01
 am.protected.test1=demo
 Organization=o=example,ou=services,dc=openam,dc=forgerock,dc=org
 Locale=en_US
 HostName=192.168.1.10
 AuthType=DataStore
 Host=192.168.1.10
 UserProfile=Required
 clientType=genericHTML
 AMCtxId=5d027d1be6c0386b01
 authInstant=2013-02-01T09:17:33Z
 Principal=id=demo,ou=user,o=example,ou=services,dc=openam,dc=forgerock,dc=org
}

The "am.protected.test1" entry was added to the session properties by setting the "User Attribute Mapping to Session Attribute" (under the Authentication -> All Core Settings area in realm settings) to include "uid|test1"