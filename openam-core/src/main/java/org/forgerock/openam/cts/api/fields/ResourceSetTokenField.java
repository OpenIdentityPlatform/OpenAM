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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.cts.api.fields;

import org.forgerock.openam.tokens.CoreTokenField;

/**
 * The ResourceSetTokenField provides a mapping between known ResourceSetDescription fields and the LDAP Attributes
 * that they map to.
 *
 * This class, like all other Token Field classes references the CoreTokenField fields. It goes
 * one stage further by mapping ResourceSetDescription attribute names to the corresponding CoreTokenField attributes.
 */
public final class ResourceSetTokenField {
    public static final String RESOURCE_SET_ID = CoreTokenField.TOKEN_ID.toString();
    public static final String POLICY_URI = "policyUri";
    public static final String CLIENT_ID = "clientId";
    public static final String NAME = "name";
}
