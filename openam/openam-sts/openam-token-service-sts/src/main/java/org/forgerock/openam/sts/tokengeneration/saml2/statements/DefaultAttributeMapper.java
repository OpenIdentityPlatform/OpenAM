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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.DefaultLibraryIDPAttributeMapper;
import com.sun.identity.saml2.plugins.SAML2PluginsUtils;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class implements the default attribute mapping functionality. It does so by extending the SAML2
 * DefaultLibraryIDPAttributeMapper, as this functionality is relatively involved, and should not be duplicated so that
 * bug updates don't need to be propagated to multiple places.
 * This does mean, however, that a few inelegant elements must be tolerated: the DefaultLibraryIDPAttributeMapper obtains
 * the attributeMap and information about dynamic or ignored profiles from IDP/SP identifiers. These values undergo non-null
 * checks, and thus faux values must be created to satisfy those checks. Hence the FAUX_* values below.
 *
 * @see com.sun.identity.saml2.plugins.DefaultLibraryIDPAttributeMapper
 *
 */
public class DefaultAttributeMapper extends DefaultLibraryIDPAttributeMapper implements AttributeMapper {
    private static final String FAUX_HOST_ENTITY_ID = "faux_host_entity_id";
    private static final String FAUX_REMOTE_ENTITY_ID = "faux_remote_entity_id";
    private static final String ORGANIZATION = "Organization";
    private final Map<String, String> attributeMap;

    public DefaultAttributeMapper(Map<String, String> attributeMap) {
        this.attributeMap = Collections.unmodifiableMap(attributeMap);
    }

    /**
     *
     * @param token The token corresponding to the subject whose attributes will be returned
     * @param attributeMap The mapping of saml attributes (keys) to the local AM LDAP attributes (values) Note that in
     *                     this implementation, the attributeMap is ignored, as it is provided to the ctor as it needs
     *                     to be referenced outside of this method, due to the DefaultLibraryIDPAttributeMapper superclass.
     *                     This implementation detail should not change the specifics of the contract, however, in which
     *                     the attributeMap is a fundamental constituent.
     * @return The list of SAML2 Attribute instances to be included in the AttributeStatement.
     * @throws TokenCreationException
     */
    public List<Attribute> getAttributes(SSOToken token, Map<String, String> attributeMap) throws TokenCreationException {
        try {
            return (List<Attribute>)super.getAttributes(token, FAUX_HOST_ENTITY_ID, FAUX_REMOTE_ENTITY_ID, token.getProperty(ORGANIZATION));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught getting attributes in DefaultAttributeMapper: " + e, e);
        } catch (SSOException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught getting attributes in DefaultAttributeMapper: " + e, e);
        }
    }

    /**
     * This method is consulted by the DefaultLibraryIDPAttributeMapper to determine whether to actually look-up keys in
     * the AttributeMap in the id-repo. User accounts in a given realm can be set to by dynamic or ignored, which means that no id-repo
     * state exists corresponding to these accounts. The DefaultLibraryIDPAttributeMapper will only consult id-repo state
     * if this method returns false.
     * @param realm The realm for which profile state should be looked-up - will be the realm for the principal for
     *              whom the token is being generated - the realm value corresponds to the realm passed in the super.getAttributes call
     *              above.
     * @return whether the dynamic or ignored profile has been set up for user accounts in this realm
     */
    @Override
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return SAML2PluginsUtils.isDynamicalOrIgnoredProfile(realm);
    }

    /**
     * This method is called to obtain the attribute mappings defined for the hosted provider corresponding to the entity
     * id and realm. These parameters will be ignored, as the attributeMap passed to the ctor of this class will always
     * be returned. This attribute map is defined in the SAML2Config state associated with the STSInstanceConfig state
     * associated with the published STS instance which is consuming the TokenGenerationService.
     * @param realm realm name. Parameter ignored.
     * @param hostEntityID <code>EntityID</code> of the hosted provider. Parameter ignored.
     * @param role Parameter ignored.
     * @return the Attribute map passed to this class' ctor.
     * @throws SAML2Exception never thrown.
     */
    @Override
    public Map<String, String> getConfigAttributeMap(String realm, String hostEntityID,
                                                     String role) throws SAML2Exception {
        return attributeMap;
    }
}
