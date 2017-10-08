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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.cts.api.CTSOptions;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.controls.PreReadRequestControl;
import org.forgerock.opendj.ldap.requests.Request;
import org.forgerock.util.Options;

/**
 * Function that is invoked for the {@link CTSOptions#PRE_DELETE_READ_OPTION}.
 * <p>
 * If the {@link Options} contains a non-{@code null} {@code CoreTokenField[]} of fields to read
 * then the {@link PreReadRequestControl} will be added to the {@literal request}.
 *
 * @since 14.0.0
 */
public class DeletePreReadOptionFunction implements LdapOptionFunction {

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Request> R apply(R request, Options options) {
        CoreTokenField[] preReadAttributes = options.get(CTSOptions.PRE_DELETE_READ_OPTION);
        if (preReadAttributes != null) {
            Set<CoreTokenField> attributes = new HashSet<>(Arrays.asList(preReadAttributes));
            attributes.add(CoreTokenField.TOKEN_ID);
            attributes.add(CoreTokenField.TOKEN_TYPE);
            return (R) request.addControl(PreReadRequestControl.newControl(true, convertToString(attributes)));
        }
        return request;
    }

    private Set<String> convertToString(Set<CoreTokenField> fields) {
        Set<String> attributes = new HashSet<>();
        for (CoreTokenField field : fields) {
            attributes.add(field.toString());
        }
        return attributes;
    }
}
