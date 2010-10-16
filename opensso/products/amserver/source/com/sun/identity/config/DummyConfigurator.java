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
 * $Id: DummyConfigurator.java,v 1.3 2008/06/25 05:42:31 qcheng Exp $
 *
 */
package com.sun.identity.config;

import com.sun.identity.config.pojos.*;
import com.sun.identity.config.pojos.condition.Condition;
import net.sf.click.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * Just a dummy class for testing.
 *
 * @author Les Hazlewood
 */
public class DummyConfigurator implements Configurator {

    private static final List agentGroups = new ArrayList();
    private static final List agentProfiles = new ArrayList();
    private static final List urlPatterns = new ArrayList();
    private static final List conditions = new ArrayList();
    private static final List circlesOfTrust = new ArrayList();
    private static final Map serviceProviders = new HashMap();
    private static final Map identityProviders = new HashMap();


    public static final String PASSWORD_SET_KEY = "passwordSet";

    static {
        UrlPattern pattern = new UrlPattern();
        pattern.setId(new Integer(1));
        pattern.setPattern("http://someoneserver.com/admin");
        urlPatterns.add(pattern);
        pattern = new UrlPattern();
        pattern.setId(new Integer(2));
        pattern.setPattern("http://someoneserver.com/HR");
        urlPatterns.add(pattern);

        Condition condition = new Condition();
        condition.setId(new Integer(1));
        condition.setName("some condition");
        conditions.add(condition);
        condition = new Condition();
        condition.setId(new Integer(2));
        condition.setName("another condition");
        conditions.add(condition);
    }

    private Page page = null;

    public DummyConfigurator( Page page ) {
        this.page = page;
    }

    public boolean isNewInstall() {
        //for now simulate with url param - if it doesn't exist, assume new install, if it does, assume upgrade:
        return page.getContext().getRequest().getParameter( "upgrade" ) == null;
    }

    public boolean isPasswordUpdateRequired() {
        return page.getContext().getSessionAttribute( PASSWORD_SET_KEY ) == null;
    }

    public void setPassword( String username, String password ) {
        //simulate call w/ back end for now:
        page.getContext().setSessionAttribute( PASSWORD_SET_KEY, "true" );
    }

    public void testHost( LDAPStore store ) {
    }

    public void testBaseDN( LDAPStore store ) {
    }

    public void testLoginId( LDAPStore store ) {
    }

    public void testLoadBalancer( String host, int port ) {
    }

    public void writeConfiguration() {
    }

    public void writeConfiguration( String newInstanceUrl, LDAPStore configStor, LDAPStore userStore, String loadBalancerHost, int loadBalancerPort ) {
    }

    public List getExistingConfigurations() {
        List existing = new ArrayList(3);
        existing.add( "http://fam.company.com:8080/fam/");
        existing.add( "http://fam.sun.com:8080/fam/");
        existing.add( "http://some.other.server.com:8080/opensso/" );
        return existing;
    }

    public void writeConfiguration( List configStringUrls ) {
    }

    public void testNewInstanceUrl( String url ) {
    }

    public void pushConfiguration( String instanceUrl ) {
    }

    public void upgrade() {
    }

    public void coexist() {
    }

    public void olderUpgrade() {
    }

    public List getRealms() {
        List realms = new ArrayList();
        for (int i = 0; i < 8; i++) {
            Realm realm = new Realm();
            realm.setName("Realm_" + (i + 1));
            realms.add(realm);
        }
        return realms;
    }


    public Realm getRealm(String name) {
        Realm realm = null;
        if (!name.equals("Jeff realm")) {
            realm = new Realm();
            realm.setName("Realm_1");
        }
        return realm;
    }

    public List getUsers(Realm realm, String filter) {
        List users = new ArrayList();
        int maxUsers = filter.equals("*") ? 503 : 7;
        for (int i = 0; i < maxUsers; i++) {
            RealmUser realmUser = new RealmUser();
            realmUser.setFirstName("FirstName" + (i + 1));
            realmUser.setLastName("LastName" + (i + 1));
            users.add(realmUser);
        }

        return users;
    }

    public List getAdministrators(Realm realm, RealmRole role) {
        List realmAdmins = new ArrayList();

        for (int i = 0; i < 5; i++) {
            RealmUser realmUser = new RealmUser();
            realmUser.setFirstName("FirstName" + (i + 1));
            realmUser.setLastName("LastName" + (i + 1));
            realmUser.setRealmRole(new RealmRole());
            realmAdmins.add(realmUser);
        }

        return realmAdmins;
    }

    public void assignAdministrators(Realm realm, List administrators) {
    }

    public void removeAdministrators(Realm realm, List administrators) {
    }

    public void addAuthenticationStore(AuthenticationStore authenticationStore) {
        page.getContext().setSessionAttribute("AuthenticationStore", authenticationStore);
    }

    public List getAgentGroups() {
        return agentGroups;
    }

    public void deleteAgentGroup( String group ) {
        agentGroups.remove( group );
    }

    public void createAgentGroup( String group ) {
        if ( !agentGroups.contains( group ) ) {
            agentGroups.add( group );
        }
    }

    public List getAgentProfiles() {
        return agentProfiles;
    }

    public void deleteAgentProfile( String profile ) {
        agentProfiles.remove( profile );
    }

    public void createAgentProfile( String profile ) {
        if ( !agentProfiles.contains( profile ) ) {
            agentProfiles.add( profile );
        }
    }

    public void createRole(RealmRole realmRole) {
        page.getContext().setSessionAttribute("RealmRole", realmRole);
    }

    public List getRoles() {
        List result = new ArrayList();
        for (int i = 0; i < 5; i++) {
            RealmRole realmRole = new RealmRole();
            realmRole.setName("Role_" + (i + 1));
            result.add(realmRole);
        }
        return result;
    }

    public List getAgentTypes() {
        List agentTypes = new ArrayList();
        AgentType agentType = new AgentType();
        agentType.setId(new Integer(1));
        agentType.setName("Local File");
        agentTypes.add(agentType);
        agentType = new AgentType();
        agentType.setId(new Integer(2));
        agentType.setName("FAM Server");
        agentTypes.add(agentType);
        return agentTypes;
    }

    public boolean checkFAMServerURL(String url) {
        return url != null;
    }

    public boolean checkCredentials(String profileName, String profilePassword) {
        return profileName != null && profilePassword != null;
    }

    public List getUrlPatterns() {
        return urlPatterns;
    }

    public void removeUrlPattern(UrlPattern urlPattern) {
        for(Iterator it = urlPatterns.iterator(); it.hasNext();) {
            UrlPattern up = (UrlPattern) it.next();
            if (urlPattern.equals(up)) {
                urlPatterns.remove(urlPattern);
                break;
            }
        }
    }

    public void removeUrlPatterns(UrlPattern[] urlPatterns) {
        for(int idx = 0; idx < urlPatterns.length; idx++) {
            removeUrlPattern(urlPatterns[idx]);
        }
    }

    public void addUrlPattern(UrlPattern urlPattern) {
        urlPattern.setId(new Integer(urlPatterns.size() + 1));
        urlPatterns.add(urlPattern);
    }

    public List getExistentConditions() {
        return conditions;
    }

    public void removeCondition(Condition condition) {
        for(Iterator it = conditions.iterator(); it.hasNext();) {
            Condition c = (Condition) it.next();
            if (condition.equals(c)) {
                conditions.remove(condition);
                break;
            }
        }
    }

    public void removeConditions(Condition[] conditions) {
        for(int idx = 0; idx < conditions.length; idx++) {
            removeCondition(conditions[idx]);
        }
    }

    public void addCondition(Condition condition) {
        condition.setId(new Integer(conditions.size() + 1));
        conditions.add(condition);
    }

    public List getFederalProtocols() {
        List result = new ArrayList();
        result.add(new FederalProtocol("SAML 1.0"));
        result.add(new FederalProtocol("SAML 1.1"));
        result.add(new FederalProtocol("SAML 2.0"));
        result.add(new FederalProtocol("ID-FF"));
        result.add(new FederalProtocol("WSFederation"));
        return result;
    }

    public List getCirclesOfTrust() {
        /**
         *
						<option>cot1 in sun > us</option>
						<option>cot1 in sun > uk</option>
						<option>cot1 in sun > asia</option>
         */
        if (circlesOfTrust.isEmpty()){
            circlesOfTrust.add(new CircleTrust("sun > us", getRealm("cot1")));
            circlesOfTrust.add(new CircleTrust("sun > uk", getRealm("cot2")));
            circlesOfTrust.add(new CircleTrust("sun > asia", getRealm("cot3")));
        }

        return circlesOfTrust;
    }

    public void createServiceProvider(ServiceProvider serviceProvider) {
        int serviceProviderId = (int) Math.random() * 100;
        serviceProvider.setServiceProviderId(serviceProviderId);
        serviceProviders.put(new Integer(serviceProviderId), serviceProvider);
    }

    public void createServiceProvider(IdentityProvider identityProvider, ServiceProvider serviceProvider) {
        identityProvider.setRemoteServiceProvider(serviceProvider);
    }


    public ServiceProvider getServiceProvider(int serviceProviderId){
        return (ServiceProvider)serviceProviders.get(new Integer(serviceProviderId));
    }

    public void createCircleOfTrust(CircleTrust circleTrust) {
        List circlesOfTrust = getCirclesOfTrust();
        circlesOfTrust.add(circleTrust);
    }
    public void deleteCircleOfTrust(String circleName){
        CircleTrust circleTrust = getCircleOfTrust(circleName);
        if (circleTrust != null){
            getCirclesOfTrust().remove(circleTrust);
        }
    }

    public CircleTrust getCircleOfTrust(String circleName){
        CircleTrust circleTrust = null;
        boolean found = false;
        List circlesOfTrust = getCirclesOfTrust();
        for(int i=0; i < circlesOfTrust.size(); i++){
            circleTrust = (CircleTrust)circlesOfTrust.get(i);
            found = circleTrust.getName().equals(circleName);
            if(found){
                break;
            }
        }

        if(!found){
            circleTrust = null;
        }

        return circleTrust;
    }

    public FederalProtocol getFederalProtocol(String protocolName){
        return  new FederalProtocol(protocolName);
    }


    public void createIdentityProvider(ServiceProvider serviceProvider, IdentityProvider identityProvider){
        ServiceProvider storedServiceProvider = getServiceProvider(serviceProvider.getServiceProviderId());
        storedServiceProvider.setRemoteIdentityProvider(identityProvider);
    }

    public void createIdentityProvider(IdentityProvider identityProvider){
        int providerId = (int) Math.random() * 100;
        identityProvider.setIdentityProviderId(providerId);
        identityProviders.put(new Integer(providerId), identityProvider);
    }

    public IdentityProvider getIdentityProvider(int providerId){
        return (IdentityProvider)identityProviders.get(new Integer(providerId));
    }


    public boolean validateHostName(String hostName) {
        return (hostName.equals("hostname"));
    }

    public boolean resolveHostName(String hostName) {
        return validateHostName(hostName);
    }
}
