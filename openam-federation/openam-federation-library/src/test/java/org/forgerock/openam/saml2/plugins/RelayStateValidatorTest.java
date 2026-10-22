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
package org.forgerock.openam.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.configuration.FedLibSystemProperties;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.forgerock.openam.saml2.plugins.RelayStateValidator.HostedEntities;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.saml2.plugins.ValidRelayStateExtractor.SAMLEntityInfo;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the RelayState validation used by the SAML2 JSPs.
 *
 * <p>The static entry points are exercised for a hosted entity with no <code>relayStateUrlList</code>, which needs
 * no configuration to be present. Everything that does reach the configuration runs against the instance, which the
 * test builds with a stub extractor and a stub entity resolver in place of the real ones.
 */
public class RelayStateValidatorTest {

    /**
     * Records the entity the whitelist was asked for and hands back whatever patterns the test configured.
     */
    private static final class StubExtractor
            implements ValidDomainExtractor<ValidRelayStateExtractor.SAMLEntityInfo> {

        private ValidRelayStateExtractor.SAMLEntityInfo requestedEntity;
        private Collection<String> patterns;

        @Override
        public Collection<String> extractValidDomains(ValidRelayStateExtractor.SAMLEntityInfo entityInfo) {
            requestedEntity = entityInfo;
            return patterns;
        }
    }

    /**
     * Resolves the metaAlias the way the SAML2 meta manager would, or fails the way it does on unreadable metadata.
     */
    private static final class StubEntityResolver implements RelayStateValidator.EntityResolver {

        private String requestedMetaAlias;
        private String entityId;
        private boolean fail;

        @Override
        public String getEntityByMetaAlias(String metaAlias) throws SAML2Exception {
            requestedMetaAlias = metaAlias;
            if (fail) {
                throw new SAML2Exception("metadata is not readable");
            }
            return entityId;
        }
    }

    /**
     * The origin fallback counts the URL this server was installed under as one of its own, so these are pinned
     * rather than left to whatever else ran in this JVM first - an ambient value would silently add an accepted
     * origin and make the assertions below depend on test ordering.
     */
    private static final String[] INSTALLATION = {
        Constants.AM_SERVER_PROTOCOL,
        Constants.AM_SERVER_HOST,
        Constants.AM_SERVER_PORT,
    };

    private final Map<String, String> previousValues = new HashMap<String, String>();

    private HttpServletRequest request;
    private StubExtractor extractor;
    private StubEntityResolver entityResolver;
    private RelayStateValidator validator;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
        extractor = new StubExtractor();
        entityResolver = new StubEntityResolver();
        validator = new RelayStateValidator(
                new FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo>(extractor),
                entityResolver);

        previousValues.clear();
        for (String property : INSTALLATION) {
            previousValues.put(property, SystemPropertiesManager.get(property));
        }
        setInstallation("https", "installed.example.com", "443");
    }

    @AfterMethod
    public void tearDown() {
        Properties properties = new Properties();
        for (String property : INSTALLATION) {
            String previous = previousValues.get(property);
            properties.setProperty(property, previous == null ? "" : previous);
        }
        new FedLibSystemProperties().initializeProperties(properties);
    }

    private static void setInstallation(String protocol, String host, String port) {
        Properties properties = new Properties();
        properties.setProperty(Constants.AM_SERVER_PROTOCOL, protocol);
        properties.setProperty(Constants.AM_SERVER_HOST, host);
        properties.setProperty(Constants.AM_SERVER_PORT, port);
        new FedLibSystemProperties().initializeProperties(properties);
    }

    @DataProvider(name = "rejectedRelayStates")
    public Object[][] rejectedRelayStates() {
        return new Object[][] {
            { null },
            { "" },
            { "https://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "javascript:alert(1)" },
            { "/\\attacker.example.com" },
        };
    }

    @Test(dataProvider = "rejectedRelayStates")
    public void shouldRejectRelayStateLeavingThisServer(String relayState) {
        assertThat(RelayStateValidator.isValidForEntity(request, null, null, relayState, SAML2Constants.SP_ROLE))
                .isFalse();
    }

    @DataProvider(name = "acceptedRelayStates")
    public Object[][] acceptedRelayStates() {
        return new Object[][] {
            { "/openam/saml2/jsp/default.jsp" },
            { "https://openam.example.com/openam/" },
            { "https://OPENAM.EXAMPLE.COM:443/openam/" },
            // The JSPs used to run the RelayState through the ESAPI "HTTPQueryString" type, which
            // demands a sequence of name=value pairs and so rejected every one of these. That check
            // is gone; these have to stay accepted or the RelayState feature remains dead.
            { "/openam/console?realm=%2Fsub&locale=en" },
            { "/openam/XUI/#login" },
            { "https://openam.example.com/openam/XUI/?goto=%2Fhome#login" },
        };
    }

    @Test(dataProvider = "acceptedRelayStates")
    public void shouldAcceptRelayStatePointingBackAtThisServer(String relayState) {
        assertThat(RelayStateValidator.isValidForEntity(request, null, null, relayState, SAML2Constants.SP_ROLE))
                .isTrue();
    }

    @Test
    public void shouldTakeTheMetaAliasBeforeTheRelayStateOnTheMetaAliasEntryPoint() {
        // The static entry points are what the JSPs call, and every argument of these two is a String, so a
        // wrapper handing them on in the wrong order still compiles - and no JSP is compiled by the build to
        // catch it. The metaAlias resolution itself cannot be asserted here, since the static goes through the
        // real SAML2 meta manager; a request naming no hosted entity never reaches it, and that is enough to
        // pin the order. Swap metaAlias and relayState and the first line judges a null RelayState; swap
        // relayState and role and the second judges the role name, which is relative and would be accepted.
        // The first line is absolute on purpose: nothing sets com.iplanet.am.server.* in this module, so it is
        // accepted only while the request itself is handed on to be measured against.
        assertThat(RelayStateValidator.isValidForMetaAlias(request, null, "https://openam.example.com/openam/",
                SAML2Constants.SP_ROLE)).isTrue();
        assertThat(RelayStateValidator.isValidForMetaAlias(request, null, "https://attacker.example.com/",
                SAML2Constants.SP_ROLE)).isFalse();
    }

    @Test
    public void shouldTakeTheRelayStateBeforeTheRoleOnTheRequestEntryPoint() {
        // Same hazard on the entry point most of the JSPs call. The URI names no metaAlias, so the hosted entity
        // is never resolved and the RelayState is measured against the origin of this server.
        when(request.getRequestURI()).thenReturn("/openam/saml2/jsp/spSingleLogoutInit.jsp");

        assertThat(RelayStateValidator.isValidForRequest(request, "https://openam.example.com/openam/",
                SAML2Constants.SP_ROLE)).isTrue();
        assertThat(RelayStateValidator.isValidForRequest(request, "https://attacker.example.com/",
                SAML2Constants.SP_ROLE)).isFalse();
    }

    @Test
    public void shouldNotTouchTheConfigurationWithoutARealmAndEntity() {
        assertThat(validator.validate(request, null, null, "/openam/", SAML2Constants.SP_ROLE)).isTrue();
        assertThat(validator.validate(request, "/sub", null, "/openam/", SAML2Constants.SP_ROLE)).isTrue();
        assertThat(validator.validate(request, null, "sp", "/openam/", SAML2Constants.SP_ROLE)).isTrue();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldReadTheWhitelistOfTheHostedEntity() {
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");

        assertThat(validator.validate(request, "/sub", "https://sp.example.com", "https://partner.example.com/acs",
                SAML2Constants.SP_ROLE)).isTrue();

        assertThat(extractor.requestedEntity).isNotNull();
        assertThat(extractor.requestedEntity.toString())
                .contains("realm=/sub")
                .contains("entityID=https://sp.example.com")
                .contains("role=" + SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldRejectRelayStateMissingFromTheWhitelistOfTheHostedEntity() {
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        assertThat(validator.validate(request, "/sub", "https://sp.example.com", "https://attacker.example.com/",
                SAML2Constants.SP_ROLE)).isFalse();
    }

    @Test
    public void shouldAskForTheEntityOfTheRoleItWasGiven() {
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        validator.validate(request, "/sub", "https://idp.example.com", "https://partner.example.com/slo",
                SAML2Constants.IDP_ROLE);
        assertThat(extractor.requestedEntity.toString()).contains("role=" + SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldResolveTheHostedEntityFromTheMetaAlias() {
        entityResolver.entityId = "https://sp.example.com";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");

        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "https://partner.example.com/acs",
                SAML2Constants.SP_ROLE)).isTrue();

        assertThat(entityResolver.requestedMetaAlias).isEqualTo("/sub/sp");
        assertThat(extractor.requestedEntity.toString())
                .contains("realm=/sub")
                .contains("entityID=https://sp.example.com");
    }

    @Test
    public void shouldRejectTheRelayStateWhenTheHostedEntityCannotBeResolved() {
        // The whitelist can not be read, so there is nothing left to validate against.
        entityResolver.fail = true;
        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "/openam/", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldRejectTheRelayStateWhenResolvingTheEntityThrowsAtRuntime() {
        // Metadata that is present but incomplete arrives here as an unchecked exception. It leaves as little to
        // validate against as the checked one, and must not escape as a 500 either.
        RelayStateValidator.EntityResolver breaking = new RelayStateValidator.EntityResolver() {

            @Override
            public String getEntityByMetaAlias(String metaAlias) {
                throw new NullPointerException("the entity has no configuration");
            }
        };
        RelayStateValidator breakable = new RelayStateValidator(
                new FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo>(extractor), breaking);

        assertThat(breakable.validateForMetaAlias(request, "/sub/sp", "/openam/", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldTakeTheMetaAliasFromTheRequestUri() {
        entityResolver.entityId = "https://sp.example.com";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        when(request.getRequestURI()).thenReturn("/openam/saml2/jsp/spSingleLogoutInit.jsp/metaAlias/sub/sp");

        assertThat(validator.validateForRequest(request, "https://partner.example.com/slo", SAML2Constants.SP_ROLE))
                .isTrue();
        assertThat(entityResolver.requestedMetaAlias).isEqualTo("/sub/sp");
    }

    @Test
    public void shouldIgnoreTheMetaAliasRequestParameter() {
        // The parameter used to stand in for a URI carrying no metaAlias, which let an unauthenticated caller
        // name whichever hosted entity has a relayStateUrlList holding the URL they want to be sent to - the
        // entries of such a list are legitimately off-origin, so any realm's could be borrowed by any other
        // realm's endpoint. The endpoint's own path is now the only source.
        entityResolver.entityId = "https://sp.example.com";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        when(request.getRequestURI()).thenReturn("/openam/saml2/jsp/spSingleLogoutInit.jsp");
        when(request.getParameter("metaAlias")).thenReturn("/sub/sp");

        assertThat(validator.validateForRequest(request, "https://partner.example.com/slo", SAML2Constants.SP_ROLE))
                .isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldFallBackToTheOriginWhenTheRequestNamesNoEntity() {
        when(request.getRequestURI()).thenReturn("/openam/saml2/jsp/spSingleLogoutInit.jsp");

        assertThat(validator.validateForRequest(request, "/openam/", SAML2Constants.SP_ROLE)).isTrue();
        assertThat(validator.validateForRequest(request, "https://attacker.example.com/", SAML2Constants.SP_ROLE))
                .isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
    }

    @Test
    public void shouldNotReadTheMetadataWhenThereIsNoRelayStateToValidate() {
        // A request carrying no RelayState is answered without reading the metadata of the hosted entity, which
        // has nothing to say about a URL that is not there. The metaAlias itself is still taken from the URI on
        // the entry point below - it is the argument of the call - so what the early return saves is the reads,
        // not the parse. The two verdicts hold either way, and so does the assertion on the extractor: an empty
        // URL is refused by the validator before it collects any patterns. The assertion on the resolver is what
        // pins the early return.
        entityResolver.entityId = "https://sp.example.com";

        assertThat(validator.validateForMetaAlias(request, "/sub/sp", null, SAML2Constants.SP_ROLE)).isFalse();
        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldNotReadTheMetadataOfTheAddressedEntityWhenThereIsNoRelayStateToValidate() {
        // The same on the entry point the JSPs call, where the metaAlias comes from the URI: a request naming a
        // hosted entity but carrying no RelayState leaves its metadata unread.
        entityResolver.entityId = "https://sp.example.com";
        when(request.getRequestURI()).thenReturn("/openam/SPSloRedirect/metaAlias/sub/sp");

        assertThat(validator.validateForRequest(request, null, SAML2Constants.SP_ROLE)).isFalse();
        assertThat(validator.validateForRequest(request, "", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
        assertThat(extractor.requestedEntity).isNull();
    }

    @Test
    public void shouldRefuseToLeaveThisServerForAHostedEntityWhoseWhitelistCanNotBeRead() {
        // The cases above name no hosted entity, so they short circuit before ValidRelayStateExtractor is asked
        // for anything. This one does name one - there is no configuration store behind it here, which is exactly
        // the case that has to stay fail safe rather than fail open. It does not pin the role selection: both
        // verdicts are settled before the role is ever read, which is what entityInfo() below is for.
        assertThat(RelayStateValidator.isValidForEntity(request, "/", "https://sp.example.com",
                "https://attacker.example.com/", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(RelayStateValidator.isValidForEntity(request, "/", "https://sp.example.com",
                "https://openam.example.com/openam/XUI/", SAML2Constants.SP_ROLE)).isTrue();
    }

    // The default ValidRelayStateExtractor builds its own SAML2MetaManager, so no verdict of the static entry
    // points can depend on which configuration it would have read. The selection that decides it is a pure
    // function, and these pin it. The fields are asserted through toString() because SAMLEntityInfo keeps them
    // to itself.

    @Test
    public void shouldNameTheConfigurationOfTheRoleTheEndpointWasReachedFor() {
        assertThat(RelayStateValidator.entityInfo("/", "https://sp.example.com", SAML2Constants.SP_ROLE).toString())
                .contains("realm=/")
                .contains("entityID=https://sp.example.com")
                .contains("role=" + SAML2Constants.SP_ROLE);
        assertThat(RelayStateValidator.entityInfo("/", "https://idp.example.com", SAML2Constants.IDP_ROLE)
                .toString())
                .contains("realm=/")
                .contains("entityID=https://idp.example.com")
                .contains("role=" + SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldCarryAnUnknownRoleThroughRatherThanNameOneOfItsOwn() {
        // ValidRelayStateExtractor reports a role that is neither SPRole nor IDPRole as a list it could not read.
        // That contract is its own, so this must not quietly substitute IDPRole here and hide a caller that names
        // no role.
        assertThat(RelayStateValidator.entityInfo("/", "https://sp.example.com", null).toString())
                .contains("role=null");
    }

    @Test
    public void shouldNameNoConfigurationWithoutAHostedEntity() {
        // Without a whitelist to read, the origin of this server is all that is left, which is what the URL cases
        // above exercise.
        assertThat(RelayStateValidator.entityInfo(null, "https://sp.example.com", SAML2Constants.SP_ROLE)).isNull();
        assertThat(RelayStateValidator.entityInfo("/", null, SAML2Constants.SP_ROLE)).isNull();
        assertThat(RelayStateValidator.entityInfo("", "", SAML2Constants.SP_ROLE)).isNull();
    }

    @Test
    public void shouldAcceptTheUrlThisServerWasInstalledUnder() {
        // The container reports openam.example.com for this request while the installation names another host;
        // both are origins of this server, which is what keeps a proxied deployment working.
        assertThat(isValid("https://installed.example.com/openam/")).isTrue();
        assertThat(isValid("https://installed.example.com:8443/openam/")).isFalse();
    }

    @Test
    public void shouldAcceptAnyHttpUrlWhenTheLegacyPropertyIsSet() {
        // The escape hatch for a deployment that cannot populate the relayStateUrlList of every hosted entity.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            assertThat(isValid("https://partner.example.com/consumer")).isTrue();
            // It restores the old behaviour whole, protocol relative URLs included.
            assertThat(isValid("//partner.example.com/consumer")).isTrue();
            // Not a licence to redirect to anything at all.
            assertThat(isValid("javascript:alert(1)")).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    /*
     * The relayStateUrlList of a hosted entity below. An entity can be configured for both roles, each with a list
     * of its own, so which of the two decides is the role the redirect belongs to.
     */

    private static final String SP_RELAY_STATE = "https://sp.example.com/*";

    private static final String IDP_RELAY_STATE = "https://idp.example.com/*";

    @Test
    public void shouldJudgeTheRelayStateByTheListOfTheRoleInPlay() {
        RelayStateValidator validator = new RelayStateValidator(new PerRoleExtractor(), new HostedEntities());

        assertThat(validator.validate(request, "/", "entity", "https://sp.example.com/consumer",
                SAML2Constants.SP_ROLE)).isTrue();
        assertThat(validator.validate(request, "/", "entity", "https://idp.example.com/consumer",
                SAML2Constants.SP_ROLE)).isFalse();
        assertThat(validator.validate(request, "/", "entity", "https://idp.example.com/consumer",
                SAML2Constants.IDP_ROLE)).isTrue();
    }

    @Test
    public void shouldNotAcceptAnyUrlWhenTheRelayStateListCouldNotBeReadEvenWithTheLegacyPropertySet() {
        // ValidRelayStateExtractor swallows the exception the configuration store throws and answers null, which
        // used to be indistinguishable from an entity without a list: a store that blinked handed the escape
        // hatch an entity that did populate its relayStateUrlList.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            RelayStateValidator validator = new RelayStateValidator(UNREADABLE_LIST, new HostedEntities());

            assertThat(validator.validate(request, "/", "entity", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
            assertThat(validator.validate(request, "/", "entity", "https://openam.example.com/openam/",
                    SAML2Constants.SP_ROLE)).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldKeepAPopulatedRelayStateListAuthoritativeWhenTheLegacyPropertyIsSet() {
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            RelayStateValidator validator = new RelayStateValidator(new PerRoleExtractor(), new HostedEntities());

            assertThat(validator.validate(request, "/", "entity", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldRefuseTheRelayStateWhenTheEntityBehindTheMetaAliasCannotBeResolvedEvenWithTheLegacyPropertySet() {
        // The whitelist of the hosted entity can not be read, so there is nothing left to validate against - not
        // even a RelayState pointing back at this server, as shouldRejectTheRelayStateWhenTheHostedEntityCannotBeResolved
        // pins for the stubbed resolver. What matters here is that the escape hatch does not reopen it.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            RelayStateValidator validator = new RelayStateValidator(new PerRoleExtractor(), new HostedEntities() {
                @Override
                String entityId(String metaAlias) throws SAML2Exception {
                    throw new SAML2Exception("the configuration store is unreachable");
                }
            });

            assertThat(validator.validateForMetaAlias(request, "/sp", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
            assertThat(validator.validateForMetaAlias(request, "/sp", "https://openam.example.com/openam/",
                    SAML2Constants.SP_ROLE)).isFalse();
            assertThat(validator.validateForMetaAlias(request, "/sp", "/openam/saml2/jsp/default.jsp",
                    SAML2Constants.SP_ROLE)).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldOnlyAcceptOwnOriginWhenNoEntityIsDeployedUnderTheMetaAlias() {
        // SAML2MetaManager.getEntityByMetaAlias answers null for a metaAlias it does not know and throws only when
        // the store fails. A metaAlias no hosted entity is deployed under must not read as an endpoint without a
        // list and reach the escape hatch.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            RelayStateValidator validator = new RelayStateValidator(new PerRoleExtractor(), new HostedEntities() {
                @Override
                String entityId(String metaAlias) {
                    return null;
                }
            });

            assertThat(validator.validateForMetaAlias(request, "/bogus", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
            assertThat(validator.validateForMetaAlias(request, "/bogus", "https://openam.example.com/openam/",
                    SAML2Constants.SP_ROLE)).isTrue();
            assertThat(validator.validateForMetaAlias(request, "/bogus", "/openam/saml2/jsp/default.jsp",
                    SAML2Constants.SP_ROLE)).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    /*
     * The static entry points below are what the JSPs and SAML2 classes call. The tests above that go through them
     * pass no realm, so nothing is read behind them; these put a recording validator in place and check what the
     * entry points hand on, so a wrapper that swapped the realm and the entity or dropped the role would show.
     */

    @Test
    public void shouldForwardTheRealmEntityAndRoleThroughTheStaticEntryPoint() {
        RecordingExtractor extractor = new RecordingExtractor();
        RelayStateValidator.setInstance(new RelayStateValidator(extractor, new HostedEntities()));
        try {
            RelayStateValidator.isValidForEntity(request, "/sub", "https://sp.example.com", "https://partner.example.com/",
                    SAML2Constants.SP_ROLE);
        } finally {
            RelayStateValidator.setInstance(null);
        }

        assertThat(extractor.asked).hasSize(1);
        assertThat(extractor.asked.get(0).getRealm()).isEqualTo("/sub");
        assertThat(extractor.asked.get(0).getEntityID()).isEqualTo("https://sp.example.com");
        assertThat(extractor.asked.get(0).getRole()).isEqualTo(SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldForwardTheMetaAliasOfTheRequestUriThroughTheStaticEntryPoint() {
        RecordingExtractor extractor = new RecordingExtractor();
        RecordingEntities entities = new RecordingEntities();
        RelayStateValidator.setInstance(new RelayStateValidator(extractor, entities));
        try {
            when(request.getRequestURI()).thenReturn("/openam/SPSloRedirect/metaAlias/sub/sp");
            RelayStateValidator.isValidForRequest(request, "https://partner.example.com/", SAML2Constants.SP_ROLE);
        } finally {
            RelayStateValidator.setInstance(null);
        }

        assertThat(entities.asked).containsExactly("/sub/sp");
        assertThat(extractor.asked).hasSize(1);
        assertThat(extractor.asked.get(0).getRealm()).isEqualTo("/sub");
        assertThat(extractor.asked.get(0).getEntityID()).isEqualTo("entity:/sub/sp");
        assertThat(extractor.asked.get(0).getRole()).isEqualTo(SAML2Constants.SP_ROLE);
    }

    /** Records the entity each list is read for, and holds no list. */
    private static final class RecordingExtractor implements ValidDomainExtractor<SAMLEntityInfo> {

        private final List<SAMLEntityInfo> asked = new ArrayList<SAMLEntityInfo>();

        @Override
        public Collection<String> extractValidDomains(SAMLEntityInfo entityInfo) {
            asked.add(entityInfo);
            return Collections.emptyList();
        }
    }

    /** Records the metaAlias each entity is resolved for, and names the entity after it. */
    private static final class RecordingEntities extends HostedEntities {

        private final List<String> asked = new ArrayList<String>();

        @Override
        String entityId(String metaAlias) {
            asked.add(metaAlias);
            return "entity:" + metaAlias;
        }
    }

    /**
     * A hosted entity configured for both roles, each with a relayStateUrlList of its own.
     */
    private static final class PerRoleExtractor implements ValidDomainExtractor<SAMLEntityInfo> {

        @Override
        public Collection<String> extractValidDomains(SAMLEntityInfo entityInfo) {
            if (SAML2Constants.SP_ROLE.equals(entityInfo.getRole())) {
                return Collections.singletonList(SP_RELAY_STATE);
            }
            if (SAML2Constants.IDP_ROLE.equals(entityInfo.getRole())) {
                return Collections.singletonList(IDP_RELAY_STATE);
            }
            // The role decides which list applies, so an unresolved one leaves the list unknown.
            return null;
        }
    }

    /** Stands for an entity whose configuration could not be read at all. */
    private static final ValidDomainExtractor<SAMLEntityInfo> UNREADABLE_LIST =
            new ValidDomainExtractor<SAMLEntityInfo>() {
                @Override
                public Collection<String> extractValidDomains(SAMLEntityInfo entityInfo) {
                    return null;
                }
            };

    private boolean isValid(String relayState) {
        return RelayStateValidator.isValidForEntity(request, null, null, relayState, SAML2Constants.SP_ROLE);
    }

    private static void setProperty(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        new FedLibSystemProperties().initializeProperties(properties);
    }
}
