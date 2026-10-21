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

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
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
        // ValidRelayStateExtractor reads the IdP configuration for anything that is not SPRole. That contract is
        // its own, so this must not quietly substitute IDPRole here and hide a caller that names no role.
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
}
