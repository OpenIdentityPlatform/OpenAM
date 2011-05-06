/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Configurator.java,v 1.5 2008/08/19 19:09:02 veiming Exp $
 *
 */
package com.sun.identity.config;

import com.sun.identity.config.pojos.*;
import com.sun.identity.config.pojos.condition.Condition;

import java.util.List;

/**
 * Interface encapsulating all back-end functionality to support the front-end page operations.
 *
 * @author Les Hazlewood
 */
public interface Configurator {

    boolean isPasswordUpdateRequired();

    /**
     * @return If <tt>true</tt>, the options.htm page will be cusotmized for a new installation, otherwise it will be
     * customized for an upgrade.
     */
    boolean isNewInstall();

    /**
     * Sets the system account password for the given username with the specified password.
     * @param username the system account username for which to set the password.
     * @param password the verified password to associate with the given username.
     */
    void setPassword( String username, String password );

    /**
     * Tests the hostname, port and secure properties of the specified LDAP store configuration - i.e. if they are
     * valid as well as if the host is online and can be accessed.
     *
     * <p>If it is not valid, an exception should be thrown.  That exception's getMessage() will be shown to the user
     * explaining why the host is not valid or online.
     *
     * @param store the store config with the host to test
     */
    void testHost( LDAPStore store );

    /**
     * Tests the Base DN of the specified LDAPStore configuration.
     *
     * <p>If it is not valid or accessible, an exception should be thrown.  That exception's getMessage() will be
     * shown to the user explaining why it is not valid or accessible.
     *
     * @param store the store config with the Base DN to test
     */
    void testBaseDN( LDAPStore store );

    /**
     * Tests the Login ID and credentials of specified LDAPStore configuration.
     *
     * <p>If the id and/or password are not valid, an exception should be thrown.  That exception's getMessage() will
     * be shown to the user explaining why it is not valid or accessible.
     *
     * @param store the store config with the Base DN to test
     */
    void testLoginId( LDAPStore store );

    /**
     * Tests the hostname and port for a user-specified load balancer to ensure it is valid and online/accessible.
     *
     * <p>If it is not valid or unreachable, an exception should be thrown.  That exception's getMessage() will be
     * shown to the user explaining why the host is not valid or online.
     *
     * @param host - the host name of the load balancer to test
     * @param port - the port of the load balancer to test.  If non-positive (i.e. 0 or less), then the user did not
     * specify a port and the argument can be ignored.
     */
    void testLoadBalancer( String host, int port );


    /**
     * When a new instance is deployed, and all defaults will be used, this method executes the default
     * installation configuration (e.g. demo environment);
     */
    void writeConfiguration();

    /**
     * Writes a user-specified custom configuration.  A null value in any argument means that it was not specified by
     * the user during the config process and the system defaults should come in to effect for that argument.
     *
     * @param newInstanceUrl
     * @param configStor
     * @param userStore
     * @param loadBalancerHost
     * @param loadBalancerPort
     */
    void writeConfiguration( String newInstanceUrl, LDAPStore configStor, LDAPStore userStore,
                             String loadBalancerHost, int loadBalancerPort );

    /**
     * Returns a list of URL {@link java.lang.String strings}, one for each each OpenSSO instance whose configuration
     * could be copied over to this instance.  This method supports the first step (of two) in Wireframes
     * Flow 3E.
     *
     * @return a list of URL strings, one for each OpenSSO instance whose config could be copied to this instance.
     */
    List getExistingConfigurations();


    /**
     * Sets this instance's configuration based on the specified URL strings of the corresponding OpenSSO instances.
     * This method supports the second step (of two) in Wireframes Flow 3E.
     *
     * @param configStringUrls URL strings, one for each OpenSSO instance whose config should be copied to this instance.
     */
    void writeConfiguration( List configStringUrls );


    /**
     * Tests that the path specified by the user is accessible and represents a currently on-line other instance in a
     * multi-instance set-up.
     *
     * <p>If it is not valid, an exception should be thrown.  That exception's getMessage() will be shown to the user
     * explaining why the instance url is not valid or online.
     *
     * @param url - user specified new instance url/path to test for online validation.
     */
    void testNewInstanceUrl( String url );

    /**
     * In a multi-instance configuration, this method pushes the configuration to the instance with the specified
     * url/path.
     * @param instanceUrl the user-specified and previously validated url/path to push the configuration to.
     */
    void pushConfiguration( String instanceUrl );

    /**
     * Supports Wireframes "3G Upgrade Case".
     */
    void upgrade();

    /**
     * Supports wireframes "3G Alternate Flow | Co-existence"
     */
    void coexist();

    /**
     * Supports wireframes "3G Alternate Flow | Upgrade from older version."
     */
    void olderUpgrade();

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.Realm} objects.
     * @return the list available Realms
     */
    List getRealms();

    /**
     * Returns a Realm object corresponding to the realm name passed as parameter or null if doesn't exist a realm with the specified name.
     * @param name is the name of the realm we want to get from the back-end.
     * @return a realm object corresponding to the passed name, or null if such not exists
     */
    Realm getRealm(String name);

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.RealmUser} objects for a specified Realm.
     * @param realm
     * @param filter
     * @return the list Realm Users for the Realm provided as parameter
     */
    List getUsers(Realm realm, String filter);

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.RealmUser} objects with a RealmRole assigned (wich mean they have some specific administrative charge) for a specified Realm and RealmRole.
     * @param realm
     * @param role
     * @return the list Realm users for the Realm and RealmRole provided as parameters
     */
    List getAdministrators(Realm realm, RealmRole role);

    void assignAdministrators(Realm realm, List administrators);

    void removeAdministrators(Realm realm, List administrators);

    /**
     * Saves the AuthenticationStore object passed as parameter.
     * @param authenticationStore the object to save
     */
    void addAuthenticationStore(AuthenticationStore authenticationStore);


    /**
     * Returns agent groups in the system.
     * This method supports wireframes page 124.
     *
     * @return a collection of String group names.
     */
    List getAgentGroups();
    void deleteAgentGroup( String group );
    void createAgentGroup( String group );

    List getAgentProfiles();
    void deleteAgentProfile( String profile );
    void createAgentProfile( String profile );

    /**
     * Saves the RealRole object passed as paremeter.
     * @param realmRole RealRole object
     */
    void createRole(RealmRole realmRole);

    List getRoles();

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.AgentType} objects.
     * @return agent type list.
     */
    List getAgentTypes();

    /**
     * Checks whether the OpenSSO Server URL is valid
     * @param url text representing the URL
     * @return true if it's valid
     */
    boolean checkFAMServerURL(String url);

    /**
     * Checks whether the Profile name and password are valid
     * @param profileName profile name
     * @param profilePassword profile password
     * @return true it they're valid
     */
    boolean checkCredentials(String profileName, String profilePassword);

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.UrlPattern} objects.
     * @return url pattern list
     */
    List getUrlPatterns();

    /**
     * Deletes a {@link com.sun.identity.config.pojos.UrlPattern} object array.
     * @param urlPatterns url pattern
     */
    void removeUrlPatterns(UrlPattern[] urlPatterns);

    /**
     * Adds a {@link com.sun.identity.config.pojos.UrlPattern} object.
     * @param urlPattern url pattern to add
     */
    void addUrlPattern(UrlPattern urlPattern);

    /**
     * Returns a list of {@link com.sun.identity.config.pojos.condition.Condition} objects.
     * @return existent conditions list
     */
    List getExistentConditions();
    /**
     * Deletes a {@link com.sun.identity.config.pojos.condition.Condition} object array.
     * @param conditions conditions
     */
    void removeConditions(Condition[] conditions);

    /**
     * Adds a {@link com.sun.identity.config.pojos.condition.Condition} object.
     * @param condition condition to add
     */
    void addCondition(Condition condition);

    List getFederalProtocols();
    List getCirclesOfTrust();
    void createServiceProvider(ServiceProvider serviceProvider);

    void createCircleOfTrust(CircleTrust circleTrust);

    void deleteCircleOfTrust(String circleName);

    CircleTrust getCircleOfTrust(String circleName);

    FederalProtocol getFederalProtocol(String protocolName);

    boolean validateHostName(String hostName);

    boolean resolveHostName(String hostName);

    ServiceProvider getServiceProvider(int serviceProviderId);

    void createIdentityProvider(ServiceProvider serviceProvider, IdentityProvider identityProvider);

    void createIdentityProvider(IdentityProvider identityProvider);

    IdentityProvider getIdentityProvider(int providerId);

    void createServiceProvider(IdentityProvider identityProvider, ServiceProvider serviceProvider);
}
