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

import com.sun.identity.configuration.FedLibSystemProperties;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.wsfederation.common.WReplyValidator.HostedEntities;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor.WSFederationEntityInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.AfterMethod;
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

    @Test
    public void shouldAcceptTheUrlThisServerWasInstalledUnder() {
        // The container reports openam.example.com for this request while the installation names another host;
        // both are origins of this server, which is what keeps a proxied deployment working.
        assertThat(isValid("https://installed.example.com/openam/")).isTrue();
        assertThat(isValid("https://installed.example.com:8443/openam/")).isFalse();
    }

    @Test
    public void shouldAcceptAnyHttpUrlWhenTheLegacyPropertyIsSet() {
        // The escape hatch for a deployment that cannot populate the wreplyList of every hosted entity.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            assertThat(isValid("https://partner.example.com/logout")).isTrue();
            // It restores the old behaviour whole, protocol relative URLs included.
            assertThat(isValid("//partner.example.com/logout")).isTrue();
            // Not a licence to redirect to anything at all.
            assertThat(isValid("javascript:alert(1)")).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    /*
     * The wreplyList of a hosted entity below. An entity can be configured for both roles, each with a list of its
     * own - the console offers one per role - so which of the two decides is the role the redirect belongs to.
     */

    private static final String SP_WREPLY = "https://sp.example.com/*";

    private static final String IDP_WREPLY = "https://idp.example.com/*";

    @Test
    public void shouldJudgeTheWreplyByTheListOfTheRoleInPlay() {
        PerRoleExtractor extractor = new PerRoleExtractor();
        WReplyValidator validator = new WReplyValidator(extractor, new HostedEntities());

        assertThat(validator.validate(request, "/", "entity", "https://sp.example.com/logout", SAML2Constants.SP_ROLE))
                .isTrue();
        assertThat(validator.validate(request, "/", "entity", "https://idp.example.com/logout", SAML2Constants.SP_ROLE))
                .isFalse();
        assertThat(validator.validate(request, "/", "entity", "https://idp.example.com/logout",
                SAML2Constants.IDP_ROLE)).isTrue();
        assertThat(validator.validate(request, "/", "entity", "https://sp.example.com/logout",
                SAML2Constants.IDP_ROLE)).isFalse();
        assertThat(extractor.asked).hasSize(4);
    }

    @Test
    public void shouldKeepAPopulatedWreplyListAuthoritativeWhenTheLegacyPropertyIsSet() {
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(new PerRoleExtractor(), new HostedEntities());

            assertThat(validator.validate(request, "/", "entity", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldNotAcceptAnyUrlWhenTheWreplyListCouldNotBeReadEvenWithTheLegacyPropertySet() {
        // ValidWReplyExtractor swallows the exception the configuration store throws and answers null, which used
        // to be indistinguishable from an entity without a list: a store that blinked handed the escape hatch an
        // entity that did populate its wreplyList.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(UNREADABLE_LIST, new HostedEntities());

            assertThat(validator.validate(request, "/", "entity", "https://attacker.example.com/",
                    SAML2Constants.SP_ROLE)).isFalse();
            assertThat(validator.validate(request, "/", "entity", "https://openam.example.com/openam/",
                    SAML2Constants.SP_ROLE)).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldAcceptAnyUrlWhenTheEntityHasNoWreplyListAndTheLegacyPropertyIsSet() {
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(NO_LIST, new HostedEntities());

            assertThat(validator.validate(request, "/", "entity", "https://partner.example.com/logout",
                    SAML2Constants.SP_ROLE)).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    /*
     * The metaAlias overload below is the one WSFederationActionFactory calls: the endpoint is deployed under a
     * metaAlias, so the entity and the role it is hosted for come from the request URI.
     */

    @Test
    public void shouldTakeTheEntityAndItsRoleFromTheMetaAlias() {
        WReplyValidator validator = new WReplyValidator(new PerRoleExtractor(), entitiesFor("entity",
                SAML2Constants.SP_ROLE));

        assertThat(validator.validateForRequest(metaAliasRequest("/sp"), "https://sp.example.com/logout")).isTrue();
        assertThat(validator.validateForRequest(metaAliasRequest("/sp"), "https://idp.example.com/logout")).isFalse();
    }

    @Test
    public void shouldRefuseTheWReplyWhenTheEntityBehindTheMetaAliasCannotBeResolvedEvenWithTheLegacyPropertySet() {
        // The whitelist of the hosted entity can not be read, so there is nothing left to validate against - not
        // even a wreply pointing back at this server, as shouldRejectTheWReplyWhenTheHostedEntityCannotBeResolved
        // pins for the stubbed resolver. What matters here is that the escape hatch does not reopen it.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(new PerRoleExtractor(), new HostedEntities() {
                @Override
                String entityId(String metaAlias) throws WSFederationMetaException {
                    throw new WSFederationMetaException("the configuration store is unreachable");
                }
            });

            assertThat(validator.validateForRequest(metaAliasRequest("/sp"), "https://attacker.example.com/"))
                    .isFalse();
            assertThat(validator.validateForRequest(metaAliasRequest("/sp"), "https://openam.example.com/openam/"))
                    .isFalse();
            assertThat(validator.validateForRequest(metaAliasRequest("/sp"), "/openam/XUI/")).isFalse();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldOnlyAcceptOwnOriginWhenNoEntityIsDeployedUnderTheMetaAlias() {
        // WSFederationMetaManager.getEntityByMetaAlias answers null for a metaAlias it does not know and throws only
        // when the store fails, so a made up metaAlias in the request URI must not read as an endpoint without a list.
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(new PerRoleExtractor(),
                    entitiesFor(null, SAML2Constants.UNKNOWN_ROLE));

            assertThat(validator.validateForRequest(metaAliasRequest("/bogus"), "https://attacker.example.com/")).isFalse();
            assertThat(validator.validateForRequest(metaAliasRequest("/bogus"), "https://openam.example.com/openam/"))
                    .isTrue();
            assertThat(validator.validateForRequest(metaAliasRequest("/bogus"), "/openam/XUI/")).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    @Test
    public void shouldTreatAnEndpointWithoutAMetaAliasAsOneWithNoListToRead() {
        setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "true");
        try {
            WReplyValidator validator = new WReplyValidator(new PerRoleExtractor(), new HostedEntities());
            HttpServletRequest plain = mock(HttpServletRequest.class);
            when(plain.getRequestURI()).thenReturn("/openam/wsfederation/jsp/realmSelection.jsp");

            assertThat(validator.validateForRequest(plain, "https://partner.example.com/logout")).isTrue();
        } finally {
            setProperty(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, "false");
        }
    }

    /*
     * The static entry points below are what the JSPs and WSFederationActionFactory call. The tests above that go
     * through them pass no realm, so nothing is read behind them; these put a recording validator in place and check
     * what the entry points hand on, so a wrapper that swapped the realm and the entity or dropped the role would
     * show.
     */

    @Test
    public void shouldForwardTheRealmEntityAndRoleThroughTheStaticEntryPoint() {
        PerRoleExtractor extractor = new PerRoleExtractor();
        WReplyValidator.setInstance(new WReplyValidator(extractor, new HostedEntities()));
        try {
            WReplyValidator.isValidForEntity(request, "/sub", "urn:entity", "https://partner.example.com/",
                    SAML2Constants.IDP_ROLE);
        } finally {
            WReplyValidator.setInstance(null);
        }

        assertThat(extractor.asked).hasSize(1);
        assertThat(extractor.asked.get(0).getRealm()).isEqualTo("/sub");
        assertThat(extractor.asked.get(0).getEntityID()).isEqualTo("urn:entity");
        assertThat(extractor.asked.get(0).getRole()).isEqualTo(SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldForwardTheEntityAndRoleOfTheMetaAliasThroughTheStaticEntryPoint() {
        PerRoleExtractor extractor = new PerRoleExtractor();
        WReplyValidator.setInstance(new WReplyValidator(extractor, entitiesFor("urn:entity", SAML2Constants.SP_ROLE)));
        try {
            WReplyValidator.isValidForRequest(metaAliasRequest("/sub/sp"), "https://partner.example.com/");
        } finally {
            WReplyValidator.setInstance(null);
        }

        assertThat(extractor.asked).hasSize(1);
        assertThat(extractor.asked.get(0).getRealm()).isEqualTo("/sub");
        assertThat(extractor.asked.get(0).getEntityID()).isEqualTo("urn:entity");
        assertThat(extractor.asked.get(0).getRole()).isEqualTo(SAML2Constants.SP_ROLE);
    }

    private HttpServletRequest metaAliasRequest(String metaAlias) {
        HttpServletRequest metaAliasRequest = mock(HttpServletRequest.class);
        when(metaAliasRequest.getScheme()).thenReturn("https");
        when(metaAliasRequest.getServerName()).thenReturn("openam.example.com");
        when(metaAliasRequest.getServerPort()).thenReturn(443);
        when(metaAliasRequest.getRequestURI()).thenReturn("/openam/WSFederationServlet/metaAlias" + metaAlias);
        return metaAliasRequest;
    }

    private static HostedEntities entitiesFor(final String entityId, final String role) {
        return new HostedEntities() {
            @Override
            String entityId(String metaAlias) {
                return entityId;
            }

            @Override
            String role(String metaAlias) {
                return role;
            }
        };
    }

    /**
     * A hosted entity configured for both roles, each with a wreplyList of its own.
     */
    private static final class PerRoleExtractor implements ValidDomainExtractor<WSFederationEntityInfo> {

        private final List<WSFederationEntityInfo> asked = new ArrayList<WSFederationEntityInfo>();

        @Override
        public Collection<String> extractValidDomains(WSFederationEntityInfo entityInfo) {
            asked.add(entityInfo);
            if (SAML2Constants.SP_ROLE.equals(entityInfo.getRole())) {
                return Collections.singletonList(SP_WREPLY);
            }
            if (SAML2Constants.IDP_ROLE.equals(entityInfo.getRole())) {
                return Collections.singletonList(IDP_WREPLY);
            }
            // The role decides which list applies, so an unresolved one leaves the list unknown.
            return null;
        }
    }

    /** Stands for an entity whose configuration could not be read at all. */
    private static final ValidDomainExtractor<WSFederationEntityInfo> UNREADABLE_LIST =
            new ValidDomainExtractor<WSFederationEntityInfo>() {
                @Override
                public Collection<String> extractValidDomains(WSFederationEntityInfo entityInfo) {
                    return null;
                }
            };

    /** Stands for an entity that was read and has no wreplyList. */
    private static final ValidDomainExtractor<WSFederationEntityInfo> NO_LIST =
            new ValidDomainExtractor<WSFederationEntityInfo>() {
                @Override
                public Collection<String> extractValidDomains(WSFederationEntityInfo entityInfo) {
                    return Collections.emptyList();
                }
            };

    private boolean isValid(String wreply) {
        return WReplyValidator.isValidForEntity(request, null, null, wreply, SAML2Constants.SP_ROLE);
    }

    private static void setProperty(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        new FedLibSystemProperties().initializeProperties(properties);
    }
}
