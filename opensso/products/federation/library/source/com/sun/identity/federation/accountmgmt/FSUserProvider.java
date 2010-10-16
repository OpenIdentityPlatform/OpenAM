/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSUserProvider.java,v 1.3 2008/11/10 22:56:56 veiming Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import java.util.Map;

/**
 * Interface used to search federation user.
 */
public interface FSUserProvider {
    /**
     * Initializes the provider.
     *
     * @param hostedProviderId provider id of hosted provider
     * @throws FSAccountMgmtException if an error occured during initialization.
     */
    public void init(String hostedProviderId)
        throws FSAccountMgmtException;

    /**
     * Searches user.
     * @param orgDN The organization to search the user.
     * @param avPairs Attribute value pairs that will be used for searching
     *        the user.
     * @param env Extra parameters that can be used for user mapping.
     *        The followings are possible key-value pair of the Map:
     *        Used for federation termination:
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_TERMINATION_KEY
     *        value: FSFederationTerminationNotification object;
     *        Used for federation/sso with POST and LECP profile:
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_AUTHNRESPONSE_KEY
     *        value: FSAuthnResponse object;
     *        Used for single logout:
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_LOGOUT_KEY
     *        value: FSLogoutNotification object;
     *        Used for name registration:
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_REGISTRATION_KEY
     *        value: FSNameRegistrationRequest object;
     *        Used for federation/sso with artifact profile:
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_FSRESPONSE_KEY
     *        value: FSResponse object.
     *        key: IFSConstants.FS_USER_PROVIDER_ENV_NAMEMAPPING_KEY
     *        value: FSNameIdentifierMappingRequest
     * @throws FSAccountMgmtException if an error occured.
     */
    public String getUserID(String orgDN, Map avPairs, Map env)
        throws FSAccountMgmtException;
}
