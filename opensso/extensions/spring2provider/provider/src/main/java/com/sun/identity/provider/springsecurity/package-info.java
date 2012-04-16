/**
 *
 * A Spring 2 Security provider for OpenSSO.
 * <br/>
 * <p>
 * Provides authentication and authorization plugins for the Spring 2
 * Security framework. For an example of how to configure this module
 * refer to the <a href="https://opensso.dev.java.net/source/browse/opensso/extensions/spring2provider/example/src/">
 * OpenSSO / Spring example</a>
 * </p>
 *
 * <h3>Authentication</h3>
 * <p>
 * The provider delegates authentication to the OpenSSO instance
 * configured in the applications AMConfig.properties. When a user tries to access an
 * application web page, the spring provider will check for a valid SSOToken. If the user
 * is not authenticated they will be redirected to OpenSSO. Once authentication
 * is complete, OpenSSO will redirect the user back to the application.
 * </p>
 * <p>
 * Upon authentication, a Spring UserDetails object is created for the user and
 * placed in the session.
 * This can be used by the application to query for the user principal and other
 * information. The spring security authentication tags can be used within a JSP,
 * as shown in the following example:
 * 
 * </p>
 * <pre>
 *  {@code
 *   The Logged on Principal is <security:authentication property="principal.username"/>
 *  }
 * </pre>
 * <h3>Authorization - Web URL Policy</h3>
 * <p>
 * The provider delegates URL policy decisions to OpenSSO. This is
 * different than most Spring 2 providers where the URL policy is configured
 * in the application using annotations or spring XML configuration.
 * </p>
 * <p>
 * OpenSSO is queried for URL policy decisions, and will return
 * ALLOW, DENY or null. A null return means that OpenSSO does not have a policy for the requested
 * URL. The provider will return an ABSTAIN vote if the OpenSSO policy decision
 * is null. If you wish to implement a policy of "Deny that which is not explicity
 * permitted" you will want to use Springs
 * <a href="http://static.springsource.org/spring-security/site/apidocs/org/springframework/security/vote/AffirmativeBased.html">
 * AffirmativeBased</a> voter in your security configuration. This ensures that at least
 * one voter must "ALLOW" the request.
 * </p>
 *
 * <h3>Authorization - Roles</h3>
 * Spring Security uses the concept of <i>GrantedAuthorities</i> which
 * are analagous to roles in OpenSSO.  This provider converts
 * OpenSSO group (role) membership into Spring GrantedAuthorities. The current
 * implementation converts an OpenSSO group membership (for example "staff") into
 * a GrantedAuthority by concatenating the prefix "ROLE_" with
 * the upper cased group name.
 * For
 * example, if a user belongs to the OpenSSO groups "staff" and "admins", they
 * will be granted "ROLE_STAFF" and "ROLE_ADMINS" authorizations.
 * </p>
 * <p>
 * Authorizations can be used in JSPs using the Spring security tags. For
 * example, the following JSP snippet will output different results depending
 * on whether the user belongs to the staff group or not:
 *  <pre>
 *
{@code
<security:authorize ifAllGranted="ROLE_STAFF">
    <div align="left"><h2>Congrats!! You have the Staff role</h2></div>
</security:authorize>
<security:authorize ifNotGranted="ROLE_STAFF">
    <div align="left"><h2>TOO BAD SO SAD - You do NOT have the Staff role</h2></div>
</security:authorize>
 * }
 * </pre>
 * <p>
 * Authorizations can also be used to protect methods using Spring pointcuts or
 * annotations. The example below demonstrates using JSR security annotations:
 * </p>
 * 
 * {@code  @RolesAllowed("ROLE_ADMIN")
public String sayHello() {
      return "Hello"
}
 * <p>
 * The above method will throw a Spring Security AccessException if the
 * user is not in the admin group.
 * </p>
 * <h2>Logging</h2>
 * <p>
 * Logging is integrated with the OpenSSO clientsdk logging mechanism. The
 * log output will go the directory configured in your AMConfig.properties file.
 * All logging related to this module is placed in a log file called "amSpring".
 * </p>
 *
 * 
 */
package com.sun.identity.provider.springsecurity;

