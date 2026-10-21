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
package com.sun.identity.wsfederation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the wreply validation used by the WS-Federation logout JSPs.
 *
 * <p>The static entry points are exercised for a hosted entity with no <code>wreplyList</code>, which needs no
 * configuration to be present. Everything that does reach the configuration runs against the instance, which the
 * test builds with a stub extractor and a stub entity resolver in place of the real ones.
 */
public class WReplyValidatorTest {

    /**
     * Records the entities the whitelist was asked for and hands back whatever patterns the test configured.
     */
    private static final class StubExtractor
            implements ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo> {

        private final List<String> requestedEntities = new ArrayList<String>();
        private Collection<String> patterns;
        private String patternHolder;

        @Override
        public Collection<String> extractValidDomains(ValidWReplyExtractor.WSFederationEntityInfo entityInfo) {
            requestedEntities.add(String.valueOf(entityInfo));
            if (patternHolder != null && !String.valueOf(entityInfo).contains(patternHolder)) {
                return null;
            }
            return patterns;
        }
    }

    /**
     * Resolves the metaAlias the way the WS-Federation meta manager would, or fails the way it does on unreadable
     * metadata.
     */
    private static final class StubEntityResolver implements WReplyValidator.EntityResolver {

        private String requestedMetaAlias;
        private String entityId;
        private String role = SAML2Constants.UNKNOWN_ROLE;
        private boolean fail;

        @Override
        public String getEntityByMetaAlias(String metaAlias) throws WSFederationMetaException {
            requestedMetaAlias = metaAlias;
            if (fail) {
                throw new WSFederationMetaException("metadata is not readable");
            }
            return entityId;
        }

        @Override
        public String getRoleByMetaAlias(String metaAlias) throws WSFederationMetaException {
            if (fail) {
                throw new WSFederationMetaException("metadata is not readable");
            }
            return role;
        }
    }

    private HttpServletRequest request;
    private StubExtractor extractor;
    private StubEntityResolver entityResolver;
    private WReplyValidator validator;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
        extractor = new StubExtractor();
        entityResolver = new StubEntityResolver();
        validator = new WReplyValidator(
                new FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo>(extractor),
                entityResolver);
    }

    @DataProvider(name = "rejectedWReplyUrls")
    public Object[][] rejectedWReplyUrls() {
        // Inside this deployment, so that nothing but the length limit refuses it.
        StringBuilder tooLong = new StringBuilder("https://openam.example.com/openam/");
        while (tooLong.length() <= org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator
                .getMaxUrlLength()) {
            tooLong.append('a');
        }
        return new Object[][] {
            { null },
            { "" },
            { "https://attacker.example.com/" },
            { "http://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com.attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "http://openam.example.com/openam/" },
            { "javascript:alert(1)" },
            { "data:text/html,<script>alert(1)</script>" },
            { "/\\attacker.example.com" },
            { "https:/\\attacker.example.com" },
            { " https://attacker.example.com/" },
            { tooLong.toString() },
        };
    }

    @Test(dataProvider = "rejectedWReplyUrls")
    public void shouldRejectWReplyUrlsLeavingThisServer(String wreply) {
        assertThat(WReplyValidator.pointsBackAtThisServer(request, wreply)).isFalse();
    }

    @DataProvider(name = "acceptedWReplyUrls")
    public Object[][] acceptedWReplyUrls() {
        return new Object[][] {
            { "/openam/XUI/#logout/" },
            { "openam/UI/Login" },
            { "https://openam.example.com/openam/" },
            { "https://openam.example.com:443/openam/" },
            { "https://OPENAM.EXAMPLE.COM/openam/" },
        };
    }

    @Test(dataProvider = "acceptedWReplyUrls")
    public void shouldAcceptWReplyUrlsPointingBackAtThisServer(String wreply) {
        assertThat(WReplyValidator.pointsBackAtThisServer(request, wreply)).isTrue();
    }

    // realmSelection.jsp and the deprecated WSFederationUtils.isWReplyURLValid reach the origin fallback
    // through the role-bearing overload instead, so it gets the same URL cases rather than relying on
    // pointsBackAtThisServer having been the only way in.

    @Test(dataProvider = "rejectedWReplyUrls")
    public void shouldRejectWReplyUrlsLeavingThisServerForAnEntityWithNoWhitelist(String wreply) {
        assertThat(WReplyValidator.isValidForEntity(request, null, null, wreply, SAML2Constants.SP_ROLE)).isFalse();
    }

    @Test(dataProvider = "acceptedWReplyUrls")
    public void shouldAcceptWReplyUrlsPointingBackAtThisServerForAnEntityWithNoWhitelist(String wreply) {
        assertThat(WReplyValidator.isValidForEntity(request, null, null, wreply, SAML2Constants.SP_ROLE)).isTrue();
    }

    // The tests above cover the endpoints whose hosted entity is named by the caller, where no whitelist is read
    // at all. The role selection that decides which whitelist would be read where the entity is trustworthy is a
    // pure function, and these pin it. The role is asserted through toString() because WSFederationEntityInfo
    // keeps its fields to itself.

    @Test
    public void shouldConsultOnlyTheIdpWhitelistForAnIdpEndpoint() {
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", SAML2Constants.IDP_ROLE);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldConsultOnlyTheSpWhitelistForAnSpEndpoint() {
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", SAML2Constants.SP_ROLE);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldConsultBothWhitelistsWhereTheRoleIsUnknown() {
        // multi.jsp carries no metaAlias, and the entity of a WS-Federation signout may be hosted as either role,
        // so both whitelists count rather than whichever one happens to be configured.
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", null);

        assertThat(infos).hasSize(2);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.IDP_ROLE);
        assertThat(infos.get(1).toString()).contains("role=" + SAML2Constants.SP_ROLE);
        assertThat(WReplyValidator.entityInfos("/", "urn:federation:example", "SomeOtherRole")).hasSize(2);
    }

    @Test
    public void shouldNameNoConfigurationWithoutAHostedEntity() {
        // Without a whitelist to read, the origin of this server is all that is left, which is what the URL cases
        // above exercise.
        assertThat(WReplyValidator.entityInfos(null, "urn:federation:example", SAML2Constants.IDP_ROLE)).isEmpty();
        assertThat(WReplyValidator.entityInfos("/", null, SAML2Constants.IDP_ROLE)).isEmpty();
        assertThat(WReplyValidator.entityInfos("", "", null)).isEmpty();
    }

    @Test
    public void shouldTakeTheWReplyBeforeTheRoleOnTheFiveArgumentEntryPoint() {
        // Both trailing parameters are Strings, so a swapped call compiles. Pin the order by behaviour: with the
        // arguments the other way round the value under test would be the role name, which is relative and would
        // be accepted.
        assertThat(WReplyValidator.isValidForEntity(request, null, null, "https://attacker.example.com/",
                SAML2Constants.SP_ROLE)).isFalse();
        assertThat(WReplyValidator.isValidForEntity(request, null, null, "/openam/XUI/", SAML2Constants.SP_ROLE))
                .isTrue();
    }

    @Test
    public void shouldTakeTheMetaAliasBeforeTheWReplyOnTheMetaAliasEntryPoint() {
        // The same hazard on the entry point WSFederationUtils calls: both trailing parameters are Strings. The
        // metaAlias resolution itself cannot be asserted through the static, which goes through the real
        // WS-Federation meta manager; a request naming no hosted entity never reaches it, and that is enough.
        // With the two the other way round this line would judge a null wreply and answer false. The URL is
        // absolute on purpose as well: nothing sets com.iplanet.am.server.* in this module, so it is accepted
        // only while the request itself is handed on to be measured against.
        assertThat(WReplyValidator.isValidForMetaAlias(request, null, "https://openam.example.com/openam/"))
                .isTrue();
        assertThat(WReplyValidator.isValidForMetaAlias(request, null, "https://attacker.example.com/")).isFalse();
    }

    @Test
    public void shouldMeasureAgainstThisServerOnTheRequestEntryPoint() {
        // WSFederationActionFactory asks about every request it serves through this one. The URI names no
        // metaAlias, so the hosted entity is never resolved and the wreply is measured against the origin of
        // this server - which only happens while the wrapper hands the request on.
        when(request.getRequestURI()).thenReturn("/openam/WSFederationServlet/sso");

        assertThat(WReplyValidator.isValidForRequest(request, "https://openam.example.com/openam/")).isTrue();
        assertThat(WReplyValidator.isValidForRequest(request, "https://attacker.example.com/")).isFalse();
    }

    @Test
    public void shouldNotTouchTheConfigurationWithoutARealmAndEntity() {
        assertThat(validator.validate(request, null, null, "/openam/", null)).isTrue();
        assertThat(validator.validate(request, "/sub", null, "/openam/", null)).isTrue();
        assertThat(validator.validate(request, null, "sp", "/openam/", null)).isTrue();
        assertThat(extractor.requestedEntities).isEmpty();
    }

    @Test
    public void shouldReadTheWhitelistOfTheHostedEntity() {
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");

        assertThat(validator.validate(request, "/sub", "urn:sp", "https://partner.example.com/logout", null)).isTrue();

        // Positional, because realm and entity ID are both Strings: a swap between them compiles, and asserting
        // only that each value appears somewhere would not notice it.
        assertThat(extractor.requestedEntities).isNotEmpty();
        assertThat(extractor.requestedEntities.get(0))
                .contains("realm=/sub, entityID=urn:sp")
                .contains(SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldRejectWReplyMissingFromTheWhitelistOfTheHostedEntity() {
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        assertThat(validator.validate(request, "/sub", "urn:sp", "https://attacker.example.com/", null)).isFalse();
    }

    @Test
    public void shouldFallBackToTheServiceProviderWhitelist() {
        // The endpoints are reachable for both roles, so the whitelist of whichever one carries it is honoured.
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        extractor.patternHolder = SAML2Constants.SP_ROLE;

        assertThat(validator.validate(request, "/sub", "urn:sp", "https://partner.example.com/logout", null)).isTrue();

        // The IDP whitelist is asked for first and holds nothing, so the SP whitelist decides.
        assertThat(extractor.requestedEntities.get(0)).contains(SAML2Constants.IDP_ROLE);
        assertThat(extractor.requestedEntities.get(1)).contains(SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldConsultOnlyTheRoleTheCallerNames() {
        // A wreplyList carried by the IdP side must not decide for an endpoint served by the SP side.
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        extractor.patternHolder = SAML2Constants.IDP_ROLE;

        assertThat(validator.validate(request, "/sub", "urn:sp", "https://partner.example.com/logout",
                SAML2Constants.SP_ROLE)).isFalse();

        assertThat(extractor.requestedEntities).hasSize(1);
        assertThat(extractor.requestedEntities.get(0)).contains(SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldTakeTheRoleFromTheMetaAlias() {
        entityResolver.entityId = "urn:sp";
        entityResolver.role = SAML2Constants.SP_ROLE;
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");

        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "https://partner.example.com/logout"))
                .isTrue();

        assertThat(extractor.requestedEntities).hasSize(1);
        assertThat(extractor.requestedEntities.get(0)).contains(SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldResolveTheHostedEntityFromTheMetaAlias() {
        entityResolver.entityId = "urn:sp";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");

        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "https://partner.example.com/logout")).isTrue();

        assertThat(entityResolver.requestedMetaAlias).isEqualTo("/sub/sp");
        assertThat(extractor.requestedEntities.get(0)).contains("realm=/sub, entityID=urn:sp");
    }

    @Test
    public void shouldRejectTheWReplyWhenTheHostedEntityCannotBeResolved() {
        // The whitelist can not be read, so there is nothing left to validate against.
        entityResolver.fail = true;
        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "/openam/")).isFalse();
        assertThat(extractor.requestedEntities).isEmpty();
    }

    @Test
    public void shouldRejectTheWReplyWhenResolvingTheEntityThrowsAtRuntime() {
        // WSFederationMetaManager.getRoleByMetaAlias dereferences the configuration of an entity that has none,
        // so incomplete metadata arrives here as an unchecked exception. It leaves as little to validate against
        // as the checked one, and must not escape as a 500 either.
        WReplyValidator.EntityResolver breaking = new WReplyValidator.EntityResolver() {

            @Override
            public String getEntityByMetaAlias(String metaAlias) {
                return "urn:sp";
            }

            @Override
            public String getRoleByMetaAlias(String metaAlias) {
                throw new NullPointerException("the entity has no configuration under either role");
            }
        };
        WReplyValidator breakable = new WReplyValidator(
                new FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo>(extractor), breaking);

        assertThat(breakable.validateForMetaAlias(request, "/sub/sp", "/openam/")).isFalse();
        assertThat(extractor.requestedEntities).isEmpty();
    }

    @Test
    public void shouldTakeBothRoleWhitelistsTogetherWhenTheCallerNamesNoRole() {
        // A dual role entity must not have the wreplyList of one role shadow the other's.
        ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo> byRole =
                new ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo>() {

                    @Override
                    public Collection<String> extractValidDomains(
                            ValidWReplyExtractor.WSFederationEntityInfo entityInfo) {
                        return Collections.singletonList(String.valueOf(entityInfo).contains(SAML2Constants.IDP_ROLE)
                                ? "https://idp-partner.example.com/*" : "https://sp-partner.example.com/*");
                    }
                };
        WReplyValidator unioning = new WReplyValidator(
                new FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo>(byRole),
                entityResolver);

        assertThat(unioning.validate(request, "/sub", "urn:both", "https://sp-partner.example.com/logout", null))
                .isTrue();
        assertThat(unioning.validate(request, "/sub", "urn:both", "https://idp-partner.example.com/logout", null))
                .isTrue();
        assertThat(unioning.validate(request, "/sub", "urn:both", "https://attacker.example.com/", null))
                .isFalse();
    }

    @Test
    public void shouldTakeTheMetaAliasFromTheRequestUri() {
        entityResolver.entityId = "urn:sp";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        when(request.getRequestURI()).thenReturn("/openam/RealmSelection/metaAlias/sub/sp");

        assertThat(validator.validateForRequest(request, "https://partner.example.com/logout")).isTrue();
        assertThat(entityResolver.requestedMetaAlias).isEqualTo("/sub/sp");
    }

    @Test
    public void shouldFallBackToTheOriginWhenTheRequestNamesNoEntity() {
        when(request.getRequestURI()).thenReturn("/openam/wsfederation/jsp/logout.jsp");

        assertThat(validator.validateForRequest(request, "/openam/")).isTrue();
        assertThat(validator.validateForRequest(request, "https://attacker.example.com/")).isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
    }

    @Test
    public void shouldNotReadTheMetadataWhenThereIsNoWReplyToValidate() {
        // WSFederationActionFactory asks about every WS-Federation request it serves and the overwhelming
        // majority carry no wreply. Those are answered without reading the metadata of the hosted entity, which
        // has nothing to say about a URL that is not there and cost three passes over the same configuration.
        // The metaAlias itself is still taken from the URI on the entry point below - it is the argument of the
        // call - so what the early return saves is the reads, not the parse. The two verdicts hold either way,
        // and so does the assertion on the extractor: an empty URL is refused by the validator before it collects
        // any patterns. The assertion on the resolver is what pins the early return.
        entityResolver.entityId = "urn:sp";

        assertThat(validator.validateForMetaAlias(request, "/sub/sp", null)).isFalse();
        assertThat(validator.validateForMetaAlias(request, "/sub/sp", "")).isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
        assertThat(extractor.requestedEntities).isEmpty();
    }

    @Test
    public void shouldNotReadTheMetadataOfTheAddressedEntityWhenThereIsNoWReplyToValidate() {
        // The same on the entry point WSFederationActionFactory actually calls, where the metaAlias comes from
        // the URI: a request naming a hosted entity but carrying no wreply leaves its metadata unread.
        entityResolver.entityId = "urn:sp";
        when(request.getRequestURI()).thenReturn("/openam/RealmSelection/metaAlias/sub/sp");

        assertThat(validator.validateForRequest(request, null)).isFalse();
        assertThat(validator.validateForRequest(request, "")).isFalse();
        assertThat(entityResolver.requestedMetaAlias).isNull();
        assertThat(extractor.requestedEntities).isEmpty();
    }

    @Test
    public void shouldValidateWithoutARequest() {
        // The deprecated WSFederationUtils.isWReplyURLValid(metaAlias, wreply, role) has no request to offer.
        entityResolver.entityId = "urn:sp";
        extractor.patterns = Collections.singletonList("https://partner.example.com/*");
        assertThat(validator.validateForMetaAlias(null, "/sub/sp", "https://partner.example.com/logout")).isTrue();
        assertThat(validator.validateForMetaAlias(null, "/sub/sp", "https://attacker.example.com/")).isFalse();
    }
}
