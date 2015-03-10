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
package org.forgerock.openam.entitlement.configuration;

/**
 * Resource type attribute definitions for interactions with the SMS layer.
 *
 * @since 13.0.0
 */
public final class ResourceTypeSmsAttributes {

    private ResourceTypeSmsAttributes() {
    }

    public static final SmsAttribute NAME = SmsAttribute.newNoneSearchableInstance("name");
    public static final SmsAttribute ACTIONS = SmsAttribute.newNoneSearchableInstance("actions");
    public static final SmsAttribute PATTERNS = SmsAttribute.newNoneSearchableInstance("patterns");
    public static final SmsAttribute DESCRIPTION = SmsAttribute.newNoneSearchableInstance("description");

    public static final SmsAttribute CREATED_BY = SmsAttribute.newNoneSearchableInstance("createdBy");
    public static final SmsAttribute CREATED_DATE = SmsAttribute.newNoneSearchableInstance("createdDate");
    public static final SmsAttribute LAST_MODIFIED_BY = SmsAttribute.newNoneSearchableInstance("lastModifiedBy");
    public static final SmsAttribute LAST_MODIFIED_DATE = SmsAttribute.newNoneSearchableInstance("lastModifiedDate");

}
