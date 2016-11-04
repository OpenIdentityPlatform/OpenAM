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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.cts.impl;

import static org.forgerock.openam.cts.api.CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION;

import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.controls.AssertionRequestControl;
import org.forgerock.opendj.ldap.requests.Request;
import org.forgerock.util.Options;

/**
 * Function that is invoked for the {@link CTSOptions#OPTIMISTIC_CONCURRENCY_CHECK_OPTION}.
 * <p>
 * If the {@link Options} contains a non-{@code null} {@literal etag} value then the
 * {@link AssertionRequestControl} will be added to the {@literal request}.
 *
 * @since 14.0.0
 */
public class ETagAssertionCTSOptionFunction implements LdapOptionFunction {

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request> R apply(R request, Options options) {
        String etag = options.get(OPTIMISTIC_CONCURRENCY_CHECK_OPTION);
        if (etag != null) {
            return (R) request.addControl(AssertionRequestControl.newControl(true,
                    Filter.equality(CoreTokenField.ETAG.toString(), etag)));
        }
        return request;
    }
}
