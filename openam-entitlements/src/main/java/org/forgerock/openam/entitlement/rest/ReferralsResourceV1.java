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
* Copyright 2014-2015 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.entitlement.rest.query.AttributeType.STRING;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.rest.query.QueryFilterVisitorAdapter;
import org.forgerock.openam.entitlement.rest.wrappers.ReferralWrapper;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

/**
 * An endpoint for interacting with legacy referrals. This will be deprecated
 * eventually, but is necessary as an intermediate step while referrals are
 * removed from customer-managed space.
 *
 * @since 12.0.0
 */
public class ReferralsResourceV1 extends RealmAwareResource {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Debug debug;

    private final Map<String, QueryAttribute> queryAttributes;

    @Inject
    public ReferralsResourceV1(@Named("frRest") Debug debug) {
        this.debug = debug;

        queryAttributes = new HashMap<>();
        queryAttributes.put("applicationName", new QueryAttribute(STRING, Privilege.APPLICATION_SEARCH_ATTRIBUTE));

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context serverContext,
            ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context serverContext, String resourceId,
            ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     *
     * This method will NOT allow you to create referrals against applications which do not exist.
     * This method WILL NOT allow you to create referrals pointing to realms which do not exist.
     * This method WILL NOT allow you to create referrals pointing to a realm's super-realm(s), or themselves.
     *
     * This method WILL allow you to create referrals against resources which do not exist in your application,
     * or which are not compliant to your applications resource-pattern.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context serverContext,
            CreateRequest createRequest) {

        final Subject callingSubject = getContextSubject(serverContext);

        if (callingSubject == null) {
            debug.error("ReferralsResource :: CREATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(serverContext);
        final ReferralWrapper wrapper;
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final ReferralPrivilegeManager rpm = createPrivilegeManager(realm, callingSubject);

        try {
            wrapper = createReferralWrapper(createRequest.getContent());
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: CREATE by " + principalName +
                        ": Referral failed to create resource from provided JSON. ", e);
            }
            return new BadRequestException().asPromise();
        }

        // This test for null added to avoid test failure in
        // ReferralsResourceTest.shouldInternalErrorIfCannotSaveReferral
        //
        if (wrapper.getName() != null) {
            // OPENAM-5031.  Reject a referral whose name contains invalid characters.
            // I'm not sure if this is a bad solution or not.  Theoretically we should encode the name, and use that
            // to store the referral (decoding it again before presenting to the user).  However, referrals are being
            // phased out, so perhaps this behaviour will encourage the user not to use them.
            //
            String encodedName = DN.escapeAttributeValue(wrapper.getName());
            if (!wrapper.getName().equals(encodedName)) {
                if (debug.errorEnabled()) {
                    debug.error("ReferralsResource :: CREATE by "
                            + principalName
                            + ": Referral name \"" + wrapper.getName() + "\" was invalid");
                }
                return new BadRequestException().asPromise();
            }
        }

        try {
            if (rpm.canFindByName(wrapper.getName())) { //return conflict
                if (debug.errorEnabled()) {
                    debug.error("ReferralsResource :: CREATE by " + principalName +
                            ": Referral already exists " + wrapper.getName());
                }
                return new ConflictException().asPromise();
            }
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: CREATE by " + principalName +
                        ": Unable to read existing referral for " + wrapper.getName());
            }
            return new InternalServerErrorException().asPromise();
        }

        if (!isRequestRealmsValidPeerOrSubrealms(serverContext, realm, wrapper.getRealms())) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: CREATE by " + principalName +
                        ": Referral failed to validate realm list. ");
            } //thrown by referencing invalid application
            return new BadRequestException().asPromise();
        }

        final ReferralPrivilege referral = wrapper.getReferral();

        try {
            rpm.add(referral);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: CREATE by " + principalName +
                        ": Referral failed to return the resource created. ", e);
            }
            return new InternalServerErrorException().asPromise();
        }

        try {
            final ResourceResponse resource = newResourceResponse(wrapper.getName(),
                    Long.toString(wrapper.getLastModifiedDate()), wrapper.toJsonValue());
            if (debug.messageEnabled()) {
                debug.message("ReferralsResource :: CREATE by " + principalName +
                        ": for Referral: " + wrapper.getName());
            }
            return newResultPromise(resource);
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: CREATE by " + principalName +
                        ": Referral failed to return the resource created. ", e);
            }
            return new InternalServerErrorException().asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context serverContext, String resourceId,
            DeleteRequest deleteRequest) {
        final Subject callingSubject = getContextSubject(serverContext);

        if (callingSubject == null) {
            debug.error("ReferralsResource :: DELETE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final String realm = getRealm(serverContext);
        final ReferralPrivilegeManager rpm = createPrivilegeManager(realm, callingSubject);

        try {
            if (!rpm.canFindByName(resourceId)) {
                if (debug.errorEnabled()) {
                    debug.error("ReferralsResource :: DELETE by " + principalName +
                            ": Referral does not exist " + resourceId);
                }
                return new NotFoundException().asPromise();
            }

            rpm.remove(resourceId);
            final ResourceResponse resource = newResourceResponse(resourceId, "0", JsonValue.json(JsonValue.object()));
            return newResultPromise(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: DELETE by " + principalName +
                        ": Referral could not be removed " + resourceId);
            }
            return new InternalServerErrorException().asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context serverContext, String resourceId,
            PatchRequest patchRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context serverContext,
            QueryRequest queryRequest, QueryResourceHandler queryResultHandler) {
        final Subject callingSubject = getContextSubject(serverContext);

        if (callingSubject == null) {
            debug.error("ReferralsResource :: QUERY : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final String realm = getRealm(serverContext);
        final ReferralPrivilegeManager rpm = createPrivilegeManager(realm, callingSubject);

        Set<ReferralWrapper> allReferralPrivileges = new HashSet<ReferralWrapper>();

        try {
            QueryFilter<JsonPointer> queryFilter = queryRequest.getQueryFilter();
            if (queryFilter == null) {
                queryFilter = QueryFilter.alwaysTrue();
            }

            final Set<SearchFilter> searchFilters = queryFilter.accept(new ReferralQueryBuilder(queryAttributes),
                              new HashSet<SearchFilter>());

            final Set<String> names = rpm.searchNames(searchFilters);

            for (String name : names) {
                ReferralPrivilege referralPrivilege = rpm.findByName(name);
                ReferralWrapper wrapper = new ReferralWrapper(referralPrivilege);
                allReferralPrivileges.add(wrapper);
            }

        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: QUERY by " + principalName +
                        ": Unable to convert resource to JSON.", e);
            }
            return new InternalServerErrorException().asPromise();
        }

        int remaining;
        try {
            remaining = allReferralPrivileges.size();
            for (ReferralWrapper wrapper : allReferralPrivileges) {
                boolean keepGoing = queryResultHandler.handleResource(newResourceResponse(wrapper.getName(),
                        Long.toString(wrapper.getLastModifiedDate()), wrapper.toJsonValue()));
                if (debug.messageEnabled()) {
                    debug.message("ReferralsResource :: QUERY by " + principalName +
                            ": Added resource to response: " + wrapper.getName());
                }
                if (!keepGoing) {
                    break;
                }
            }
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: QUERY by " + principalName +
                        ": Unable to convert resource to JSON.", e);

            }
            return new InternalServerErrorException().asPromise();
        }

        return newResultPromise(newQueryResponse(null, CountPolicy.EXACT, remaining));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context serverContext, String resourceId,
            ReadRequest readRequest) {
        final Subject callingSubject = getContextSubject(serverContext);

        if (callingSubject == null) {
            debug.error("ReferralResource :: READ : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(serverContext);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final ReferralPrivilegeManager rpm = createPrivilegeManager(realm, callingSubject);

        try {
            final ReferralPrivilege referralPrivilege = rpm.findByName(resourceId);
            final ReferralWrapper wrapp = new ReferralWrapper(referralPrivilege);

            final ResourceResponse resource = newResourceResponse(resourceId, Long.toString(referralPrivilege.getLastModifiedDate()),
                    wrapp.toJsonValue());
            return newResultPromise(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: READ by " + principalName +
                        ": Referral failed to retrieve the resource specified.", e);
            }
            return new NotFoundException().asPromise();
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: READ by " + principalName +
                        ": Error converting resource to JSON format.", e);
            }
            return new InternalServerErrorException().asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context serverContext, String resourceId,
            UpdateRequest updateRequest) {
        final Subject callingSubject = getContextSubject(serverContext);

        if (callingSubject == null) {
            debug.error("ReferralResource :: UPDATE : Unknown Subject");
            return new BadRequestException().asPromise();
        }

        final String realm = getRealm(serverContext);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(callingSubject);
        final ReferralPrivilegeManager rpm = createPrivilegeManager(realm, callingSubject);

        final ReferralWrapper wrapper;

        try {
            wrapper = createReferralWrapper(updateRequest.getContent());
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to create resource from provided JSON. ", e);
            }
            return new BadRequestException().asPromise();
        }

        final ReferralPrivilege previousPriv;

        try {
            previousPriv = rpm.findByName(resourceId);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to query the resource set. ", e);
            }
            return new NotFoundException().asPromise();
        }

        if (!isRequestRealmsValidPeerOrSubrealms(serverContext, realm, wrapper.getRealms())) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to validate new realm list. ");
            }
            return new BadRequestException().asPromise();
        }

        //conflict if the name we're changing TO is currently taken, and isn't this one
        try {
            if (!resourceId.equals(wrapper.getName()) && rpm.canFindByName(wrapper.getName())) { //return conflict
                if (debug.errorEnabled()) {
                    debug.error("ReferralsResource :: UPDATE by " + principalName +
                            ": Referral already exists " + wrapper.getName());
                }
                return new ConflictException().asPromise();
            }
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to query the resource set. ", e);
            }
            return new InternalServerErrorException().asPromise();
        }

        try { //then add - if this fails we try to re-add old
            rpm.remove(previousPriv.getName());
            rpm.add(wrapper.getReferral());
            final ResourceResponse resource = newResourceResponse(wrapper.getName(),
                    Long.toString(wrapper.getLastModifiedDate()), wrapper.toJsonValue());
            if (debug.messageEnabled()) {
                debug.message("ReferralsResource :: UPDATE by " + principalName +
                        ": for Referral: " + wrapper.getName());
            }
            return newResultPromise(resource);
        } catch (EntitlementException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to restore the old resource after updating ", e);
            }
            return new InternalServerErrorException().asPromise();
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("ReferralsResource :: UPDATE by " + principalName +
                        ": Referral failed to store the updated resource. ", e);
            }
            return new InternalServerErrorException().asPromise();
        }
    }

    /**
     * Abstracts out the createReferralWrapper method so that we can easily test this class by decoupling
     * from actual impl.
     *
     * @param jsonValue The JsonValue to create the wrapper from
     * @return An ApplicationWrapper, wrapping the Application represented by the JsonValue provided
     * @throws IOException If there were errors creating the application
     */
     ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
        return mapper.readValue(jsonValue.toString(), ReferralWrapper.class);
    }

    /**
     * Abstracts out the createPrivilegeManager method so that we can easily test this class by decoupling
     * from actual impl.
     *
     * @param realm The realm to create the privilege manager from.
     * @param callingSubject The subject with (supposed) authorization to use the privilege manager.
     * @return A newly-created {@link ReferralPrivilegeManager}.
     */
    ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
        return new ReferralPrivilegeManager(realm, callingSubject);
    }

    /**
     * Compares the provided set of realms with a set of realms which are sub/peer to the realm in which
     * the operation is being carried out.
     */
    @SuppressWarnings("unchecked")
    boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm, Set<String> realms) {
        try {
            final SSOToken callerToken = serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();
            final OrganizationConfigManager ocm = new OrganizationConfigManager(callerToken, realm);
            final Set<String> subRealms = ocm.getSubOrganizationNames("*", true); //all subrealms
            final Set<String> peerRealms = ocm.getPeerOrganizationNames(); //all peers (including context realm)

            return isRealmsValid(realm, realms, subRealms, peerRealms);
        } catch (SMSException e) {
            if (debug.warningEnabled()) {
                debug.error("ReferralsResource :: Querying for realm-information failed. ", e);
            }
        } catch (SSOException e) {
            if (debug.warningEnabled()) {
                debug.error("ReferralsResource :: User SSOToken not valid for querying subrealms. ", e);
            }
        }

        return false;
    }

    boolean isRealmsValid(String realm, Set<String> realms, Set<String> subRealms, Set<String> peerRealms) {

        Set<String> validRealms = new HashSet<String>();

        //the format returned from the ocm has no prefix, so prepend here (unless root context)
        for (String subRealm : subRealms) {
            String fixedRealm = realm.endsWith("/") ? realm + subRealm : realm + "/" + subRealm;
            if (!fixedRealm.equals(realm)) { //ignore context realm
                validRealms.add(fixedRealm);
            }
        }

        for (String peerRealm : peerRealms) {
            String fixedRealm = realm.substring(0, realm.lastIndexOf("/") + 1) + peerRealm;
            if (!fixedRealm.equals(realm)) { //ignore context realm
                validRealms.add(fixedRealm);
            }
        }

        return validRealms.containsAll(realms);

    }


    /**
     * Converts a set of CREST {@link QueryFilter}s into a set of privilege {@link SearchFilter}s.
     *
     * @since 12.0.0
     */
    private static final class ReferralQueryBuilder extends QueryFilterVisitorAdapter {
        ReferralQueryBuilder(Map<String, QueryAttribute> queryAttributes) {
            super("referral", queryAttributes);
        }

        @Override
        public Set<SearchFilter> visitEqualsFilter(Set<SearchFilter> filters, JsonPointer field,
                                                   Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.EQUALS_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                        Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.GREATER_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitGreaterThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                                       Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.GREATER_THAN_OR_EQUAL_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanFilter(Set<SearchFilter> filters, JsonPointer field,
                                                     Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESS_THAN_OPERATOR, valueAssertion));
            return filters;
        }

        @Override
        public Set<SearchFilter> visitLessThanOrEqualToFilter(Set<SearchFilter> filters, JsonPointer field,
                                                              Object valueAssertion) {
            filters.add(comparison(field.leaf(), SearchFilter.Operator.LESS_THAN_OR_EQUAL_OPERATOR, valueAssertion));
            return filters;
        }
    }
}
