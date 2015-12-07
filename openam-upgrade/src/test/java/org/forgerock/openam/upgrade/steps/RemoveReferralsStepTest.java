/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.OpenSSOPrivilege;
import com.sun.identity.shared.Constants;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.entitlement.service.PrivilegeManagerFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeParser;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Set;

/**
 * Unit test for {@link RemoveReferralsStep}.
 *
 * @since 13.0.0
 */
public final class RemoveReferralsStepTest {

    @Mock
    private ApplicationManagerWrapper applicationService;
    @Mock
    private ResourceTypeService resourceTypeService;
    @Mock
    private PrivilegeManagerFactory policyServiceFactory;
    @Mock
    private PrivilegeManager policyService;
    @Mock
    private ConnectionFactory<Connection> connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private ConnectionEntryReader entryReader;
    @Mock
    private SearchResultEntry resultEntry;
    @Mock
    private PrivilegedAction<SSOToken> privilegedAction;

    @Captor
    private ArgumentCaptor<ResourceType> resourceTypeCaptor;
    @Captor
    private ArgumentCaptor<Application> applicationCaptor;
    @Captor
    private ArgumentCaptor<Privilege> policyCaptor;
    @Captor
    private ArgumentCaptor<DeleteRequest> deleteRequestCaptor;

    private UpgradeStep testStep;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        System.setProperty("com.iplanet.am.version", "12.0.0");

        SSOToken token = mock(SSOToken.class);
        given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("abc");
        given(privilegedAction.run()).willReturn(token);

        testStep = new RemoveReferralsStep(applicationService, resourceTypeService,
                policyServiceFactory, connectionFactory, privilegedAction, "ou=forgerock,ou=org");
    }

    @Test
    public void simpleSuccessfulPassThrough() throws Exception {
        // Given
        given(connectionFactory.create()).willReturn(connection);
        given(connection.search(isA(SearchRequest.class))).willReturn(entryReader);
        given(entryReader.hasNext()).willReturn(true).willReturn(false);
        given(entryReader.readEntry()).willReturn(resultEntry);
        given(resultEntry.getName()).willReturn(DN.valueOf("ou=test,ou=forgerock,ou=org"));

        JsonValue jsonValue = json(
                object(
                        field("name", "ref"),
                        field("mapApplNameToResources", object(field("app1", array("*://*:*/*")))),
                        field("realms", array("/a"))));

        Set<String> values = singleton("serializable=" + jsonValue.toString());

        Attribute attribute = new LinkedAttribute("ou", values);
        AttributeParser attributeParser = AttributeParser.parseAttribute(attribute);
        given(resultEntry.parseAttribute("sunKeyValue")).willReturn(attributeParser);

        Application app1 = new Application();
        app1.setName("app1");
        app1.addAllResourceTypeUuids(singleton("123"));

        given(applicationService.getApplication(isA(Subject.class), eq("/"), eq("app1"))).willReturn(app1);
        given(policyServiceFactory.get(eq("/a"), isA(Subject.class))).willReturn(policyService);

        Privilege policy1 = new OpenSSOPrivilege();
        policy1.setName("pol1");

        given(policyService.findAllPoliciesByApplication("app1")).willReturn(singletonList(policy1));

        ResourceType resourceType1 = ResourceType
                .builder()
                .setName("resourceType1")
                .setUUID("123")
                .build();

        given(resourceTypeService.getResourceType(isA(Subject.class), eq("/"), eq("123"))).willReturn(resourceType1);

        // When
        testStep.initialize();
        boolean isApplicable = testStep.isApplicable();
        testStep.perform();
        String shortReport = testStep.getShortReport("");
        String longReport = testStep.getDetailedReport("");

        // Then
        assertThat(isApplicable).isTrue();
        assertThat(shortReport).containsSequence("applications to be cloned", "Referrals found");
        assertThat(longReport).containsSequence("app1", "ou=test,ou=forgerock,ou=org");

        verify(resourceTypeService).saveResourceType(isA(Subject.class), eq("/a"), resourceTypeCaptor.capture());
        verify(applicationService).saveApplication(isA(Subject.class), eq("/a"), applicationCaptor.capture());
        verify(policyService).modify(policyCaptor.capture());

        ResourceType clonedResourceType = resourceTypeCaptor.getValue();
        assertThat(clonedResourceType).isNotEqualTo(resourceType1);
        assertThat(clonedResourceType.getName()).isEqualTo("resourceType1");

        Application clonedApplication = applicationCaptor.getValue();
        assertThat(clonedApplication).isNotEqualTo(app1);
        assertThat(clonedApplication.getName()).isEqualTo("app1");
        assertThat(clonedApplication.getResourceTypeUuids()).containsExactly(clonedResourceType.getUUID());

        Privilege modifiedPolicy = policyCaptor.getValue();
        assertThat(modifiedPolicy).isEqualTo(modifiedPolicy);
        assertThat(modifiedPolicy.getResourceTypeUuid()).isEqualTo(clonedResourceType.getUUID());

        verify(connection).delete(deleteRequestCaptor.capture());
        DeleteRequest request = deleteRequestCaptor.getValue();
        assertThat(request.getName().toString()).isEqualTo("ou=test,ou=forgerock,ou=org");
    }

}