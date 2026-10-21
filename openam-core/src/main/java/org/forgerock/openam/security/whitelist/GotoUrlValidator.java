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
package org.forgerock.openam.security.whitelist;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviders;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Validates the <code>goto</code> and <code>gotoOnFail</code> URLs of the authentication endpoints.
 *
 * <p>The realm's <code>Valid goto URL Resources</code> list decides which URLs are allowed. That list is empty on a
 * fresh installation, and {@link RedirectUrlValidator} reads an empty list as permission to redirect anywhere, which
 * turns every unconfigured realm into an open redirect. This validator reads an empty list as permission to redirect
 * back to this server only.
 *
 * <p>Deployments that need cross domain goto URLs and cannot configure the Validation Service can set
 * {@link Constants#GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST} to restore the old behaviour. The property is read on every
 * call, so it can be changed without a restart. It decides what an <em>empty</em> list means: a populated list rules
 * either way, and so does a list that could not be read at all, which is never taken for an empty one.
 */
public final class GotoUrlValidator {

    private static final Debug DEBUG = Debug.getInstance("patternMatching");

    /**
     * The realm {@link ValidGotoUrlExtractor} reads a null one as: <code>DNMapper.orgNameToDN</code> maps
     * <code>null</code>, the empty string and <code>/</code> onto the same base DN.
     */
    private static final String ROOT_REALM = "/";

    /**
     * Reads the base URL a realm is published under.
     *
     * <p>A seam rather than a direct call: the production implementation reaches the Base URL Source service through
     * Guice, which is not up in every context this class runs in, and tests supply their own.
     */
    interface BaseUrlSupplier {

        /**
         * Returns the base URL of the realm, or <code>null</code> when there is none to be had.
         *
         * @param request The request being processed.
         * @param realm The realm to read the base URL of.
         * @return The base URL, for example <code>https://sso.example.com/openam</code>.
         */
        String getBaseUrl(HttpServletRequest request, String realm);
    }

    /**
     * Reads the base URL from the realm's Base URL Source service, but only where it does not come from the request.
     *
     * <p>A base URL built out of the request - the <code>X-Forwarded-*</code> and <code>Forwarded</code> header
     * types, or the extension class type wrapping one of them - names an origin the caller chose, so treating it as
     * an origin this server answers on would hand back the open redirect this class exists to close. The request
     * values type yields the origin of the request, which has already been compared by the time this is reached.
     * {@link BaseURLProviders#isRequestDerived(BaseURLProvider)} draws the line, and looks through the extension
     * wrapper to do it.
     *
     * <p>Package private, and reaching the factory through an overridable method, so that the decision can be
     * exercised where production makes it without an injector behind the test.
     */
    static class ConfiguredBaseUrl implements BaseUrlSupplier {

        @Override
        public String getBaseUrl(final HttpServletRequest request, final String realm) {
            final BaseURLProvider provider = providerFor(realm);
            if (BaseURLProviders.isRequestDerived(provider)) {
                DEBUG.message("GotoUrlValidator: the base URL of realm {} is derived from the request, so it is "
                        + "not treated as an origin of this server", realm);
                return null;
            }
            return provider.getRootURL(request);
        }

        /**
         * The base URL provider configured for the realm.
         *
         * @param realm The realm to read the configuration of.
         * @return The provider, never <code>null</code>.
         */
        BaseURLProvider providerFor(final String realm) {
            return InjectorHolder.getInstance(BaseURLProviderFactory.class).get(realm);
        }
    }

    /**
     * Holds the shared validator, which drags in the Validation Service configuration on first use.
     */
    private static final class Holder {

        private static final GotoUrlValidator DEFAULT =
                new GotoUrlValidator(ValidGotoUrlExtractor.getInstance(), new ConfiguredBaseUrl());

        private static volatile GotoUrlValidator instance = DEFAULT;
    }

    /**
     * Test seam: puts the given validator behind the static entry points, so that a test can see the realm the call
     * sites forward. <code>null</code> restores the validator that reads the configuration.
     *
     * @param replacement The validator to use, or <code>null</code>.
     */
    static void setInstance(final GotoUrlValidator replacement) {
        Holder.instance = replacement == null ? Holder.DEFAULT : replacement;
    }

    /**
     * The validator the static entry points use when no test has put another in place.
     *
     * @return The validator reading the configuration.
     */
    static GotoUrlValidator defaultInstance() {
        return Holder.DEFAULT;
    }

    private final FailSafeRedirectUrlValidator<String> failSafeValidator;

    private final BaseUrlSupplier baseUrls;

    /**
     * Creates a validator reading the goto URL list through the provided extractor. Package private: the
     * configuration is reached through a single shared instance, tests supply their own extractor.
     *
     * @param extractor The extractor of the configured goto URL patterns.
     * @param baseUrls The source of the realm's published base URL.
     */
    GotoUrlValidator(final ValidDomainExtractor<String> extractor, final BaseUrlSupplier baseUrls) {
        this.failSafeValidator = new RealmOriginAwareValidator(extractor, baseUrls);
        this.baseUrls = baseUrls;
    }

    /**
     * The source of the realm's published base URL this validator was built with.
     *
     * @return The base URL source.
     */
    BaseUrlSupplier baseUrls() {
        return baseUrls;
    }

    /**
     * Tells whether it is safe to redirect to the given goto URL.
     *
     * @param request The request being processed, used to establish the origin of this server. May be
     * <code>null</code>, in which case an absolute URL is only accepted when the realm has a whitelist matching it
     * or when it names an origin this server was configured with.
     * @param url The URL to validate.
     * @param orgDN The realm whose goto URL list applies.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    public static boolean isValid(final HttpServletRequest request, final String url, final String orgDN) {
        return Holder.instance.validate(request, url, orgDN);
    }

    boolean validate(final HttpServletRequest request, final String url, final String orgDN) {
        return failSafeValidator.isRedirectUrlValid(request, url, realmToRead(orgDN));
    }

    /**
     * The realm whose goto URL list is read for the given one.
     *
     * <p>Only the escape hatch makes a difference here, and only for a call that names no realm at all.
     * {@link RedirectUrlValidator}, the pre-fix path the hatch restores, hands a null realm to
     * {@link ValidGotoUrlExtractor}, which reads it as the root realm - so turning the hatch on must not stop the
     * root realm's list from applying to such a call. Without the hatch a null realm keeps its stricter reading:
     * no whitelist, fall back to the origins this server answers on.
     *
     * @param orgDN The realm the caller named, may be <code>null</code>.
     * @return The realm to read the goto URL list of, may be <code>null</code>.
     */
    private static String realmToRead(final String orgDN) {
        return orgDN == null && allowAnyUrlWithoutWhitelist() ? ROOT_REALM : orgDN;
    }

    /**
     * Returns the goto URL when it is safe to redirect to it, and the provided alternative otherwise.
     *
     * <p>The alternative comes from the configuration rather than from the request, so it is returned as it is.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param orgDN The realm whose goto URL list applies.
     * @param gotoUrl The goto URL received from the caller. May be <code>null</code>.
     * @param alternateUrl The URL to fall back to. May be <code>null</code>.
     * @return The URL to redirect to, or <code>null</code> when there is none.
     */
    public static String getRedirectUrl(final HttpServletRequest request, final String orgDN, final String gotoUrl,
            final String alternateUrl) {
        return Holder.instance.redirectUrl(request, orgDN, gotoUrl, alternateUrl);
    }

    String redirectUrl(final HttpServletRequest request, final String orgDN, final String gotoUrl,
            final String alternateUrl) {
        if (gotoUrl == null) {
            return alternateUrl;
        }

        String returnValue = null;
        if (validate(request, gotoUrl, orgDN)) {
            returnValue = gotoUrl;
        } else {
            DEBUG.message("GotoUrlValidator.getRedirectUrl: the goto URL {} is not allowed in realm {},"
                    + " falling back to {}", gotoUrl, orgDN, alternateUrl);
        }

        if (returnValue == null || returnValue.isEmpty()) {
            returnValue = alternateUrl;
        }

        return returnValue;
    }

    private static boolean allowAnyUrlWithoutWhitelist() {
        return SystemProperties.getAsBoolean(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, false);
    }

    /**
     * Adds the realm's published base URL to the origins a goto URL may point at.
     *
     * <p>The container only reports the origin it was reached on, which behind a TLS terminating reverse proxy is
     * the internal one rather than the one OpenAM is published under. The Base URL Source service of the realm is
     * where a deployment records that public origin, so a goto URL naming OpenAM itself keeps working.
     */
    private static final class RealmOriginAwareValidator extends FailSafeRedirectUrlValidator<String> {

        private final BaseUrlSupplier baseUrls;

        RealmOriginAwareValidator(final ValidDomainExtractor<String> extractor, final BaseUrlSupplier baseUrls) {
            super(extractor);
            this.baseUrls = baseUrls;
        }

        @Override
        protected boolean allowAnyUrlWithoutWhitelist() {
            // Read on every call, so it can be changed without a restart. The validator only asks once it knows
            // the realm has no list, so a populated one - and a list that could not be read - still rules.
            return GotoUrlValidator.allowAnyUrlWithoutWhitelist();
        }

        @Override
        protected Collection<String> getAdditionalOrigins(final HttpServletRequest request,
                final Collection<String> realms) {
            final Collection<String> origins = super.getAdditionalOrigins(request, realms);
            if (realms == null) {
                return origins;
            }
            for (final String realm : realms) {
                if (realm == null) {
                    continue;
                }
                try {
                    addOrigin(origins, baseUrls.getBaseUrl(request, realm));
                } catch (final RuntimeException re) {
                    // The service is optional and the injector is not up in every context this runs in.
                    DEBUG.message("GotoUrlValidator: could not read the base URL of realm {}", realm, re);
                }
            }
            return origins;
        }
    }
}
