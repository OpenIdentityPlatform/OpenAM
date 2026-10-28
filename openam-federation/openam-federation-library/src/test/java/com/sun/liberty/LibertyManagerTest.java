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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.liberty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.servlet.http.HttpServletRequest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.MetaManagerFailureFixture;
import com.sun.identity.federation.jaxb.entityconfig.AttributeElement;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaManager;

/**
 * {@code LibertyManager} used to capture the meta manager in a static initialiser. When the
 * SMS was unreachable at the moment this class was first touched the captured manager stayed
 * null for the life of the JVM, so every provider lookup here - including the home URL
 * {@code FSPostLogin} falls back to when a RelayState is refused - failed until a restart.
 * Each lookup reads the manager on its own, so each is a case of its own.
 */
public class LibertyManagerTest {

    private static final String REALM = IFSConstants.ROOT_REALM;
    private static final String META_ALIAS = "/sp3";
    private static final String SP = "https://sp3.example.com/idff";
    private static final String IDP = "https://idp3.example.com/idff";
    private static final String HOME_URL = "https://sp3.example.com/home";
    private static final String FEDERATION_DONE_URL = "https://sp3.example.com/federated";
    private static final String TERMINATION_DONE_URL = "https://sp3.example.com/terminated";
    private static final String ERROR_PAGE_URL = "https://sp3.example.com/error";
    private static final String REGISTRATION_DONE_URL = "https://sp3.example.com/registered";

    private IDFFMetaManager originalMetaInstance;
    private long originalFailedAt;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalFailedAt = MetaManagerFailureFixture.failedAt();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            MetaManagerFailureFixture.setFailedAt(originalFailedAt);
        } finally {
            FederationStaticsLock.release();
        }
    }

    @DataProvider(name = "lookups")
    public Object[][] lookups() {
        final HttpServletRequest request = request();
        return new Object[][] {
            { "getAllIDPList", (Callable<Object>) () -> list(LibertyManager.getAllIDPList(REALM)),
                Collections.singletonList(IDP) },
            { "getAllSPList", (Callable<Object>) () -> list(LibertyManager.getAllSPList(REALM)),
                Collections.singletonList(SP) },
            { "getIDPList", (Callable<Object>) () -> list(LibertyManager.getIDPList(REALM, SP)),
                Collections.singletonList(IDP) },
            { "getHomeURL",
                (Callable<Object>) () -> LibertyManager.getHomeURL(REALM, SP, IFSConstants.SP),
                HOME_URL },
            { "getEntityID", (Callable<Object>) () -> LibertyManager.getEntityID(META_ALIAS), SP },
            { "getMetaAlias",
                (Callable<Object>) () -> LibertyManager.getMetaAlias(REALM, SP, IFSConstants.SP),
                META_ALIAS },
            { "getFederationDonePageURL",
                (Callable<Object>) () -> LibertyManager.getFederationDonePageURL(
                    REALM, SP, IFSConstants.SP, request),
                FEDERATION_DONE_URL },
            { "getTerminationDonePageURL",
                (Callable<Object>) () -> LibertyManager.getTerminationDonePageURL(
                    REALM, SP, IFSConstants.SP, request),
                TERMINATION_DONE_URL },
            { "getErrorPageURL",
                (Callable<Object>) () -> LibertyManager.getErrorPageURL(
                    REALM, SP, IFSConstants.SP, request),
                ERROR_PAGE_URL },
            { "getNameRegistrationDonePageURL",
                (Callable<Object>) () -> LibertyManager.getNameRegistrationDonePageURL(
                    REALM, SP, IFSConstants.SP, request),
                REGISTRATION_DONE_URL },
            { "getProviderRole",
                (Callable<Object>) () -> LibertyManager.getProviderRole(META_ALIAS),
                IFSConstants.SP }
        };
    }

    /**
     * The expected values are this provider's alone: a manager latched from any other test
     * answers none of them.
     */
    @Test(dataProvider = "lookups")
    public void shouldRecoverWhenTheMetaManagerBecomesAvailableAfterClassLoad(
        String lookup, Callable<Object> call, Object expected) throws Exception {

        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        call.call();

        FSUtils.metaInstance = provider();

        assertThat(call.call()).isEqualTo(expected);
    }

    private static IDFFMetaManager provider() throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.PROVIDER_HOME_PAGE_URL, HOME_URL));
        attributes.add(attribute(IFSConstants.FEDERATION_DONE_URL, FEDERATION_DONE_URL));
        attributes.add(attribute(IFSConstants.TERMINATION_DONE_URL, TERMINATION_DONE_URL));
        attributes.add(attribute(IFSConstants.ERROR_PAGE_URL, ERROR_PAGE_URL));
        attributes.add(attribute(IFSConstants.REGISTRATION_DONE_URL, REGISTRATION_DONE_URL));

        List<AttributeElement> elements = new ArrayList<AttributeElement>();
        for (AttributeType attribute : attributes) {
            elements.add(new AttributeElement(attribute));
        }
        BaseConfigType baseConfig = mock(BaseConfigType.class);
        when(baseConfig.getAttribute()).thenReturn(elements);
        when(baseConfig.getMetaAlias()).thenReturn(META_ALIAS);
        SPDescriptorConfigElement config = new SPDescriptorConfigElement(baseConfig);

        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getSPDescriptorConfig(REALM, SP)).thenReturn(config);
        when(metaManager.getEntityIDByMetaAlias(META_ALIAS)).thenReturn(SP);
        when(metaManager.getProviderRoleByMetaAlias(META_ALIAS)).thenReturn(IFSConstants.SP);
        when(metaManager.getAllHostedIdentityProviderIDs(REALM))
            .thenReturn(Collections.singletonList(IDP));
        when(metaManager.getAllHostedServiceProviderEntities(REALM))
            .thenReturn(Collections.singletonList(SP));
        when(metaManager.getAllTrustedProviders(META_ALIAS))
            .thenReturn(Collections.singleton(IDP));
        return metaManager;
    }

    private static AttributeType attribute(String name, String value) {
        AttributeType attribute = mock(AttributeType.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getValue()).thenReturn(new ArrayList<String>(Arrays.asList(value)));
        return attribute;
    }

    /** Where the done pages are looked for when no provider configures them. */
    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/federation"));
        return request;
    }

    private static List<Object> list(Iterator<?> iterator) {
        List<Object> list = new ArrayList<Object>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
