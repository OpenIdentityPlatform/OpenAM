/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.ResourceAttribute;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Jackson module to customise JSON serialisation for policies and entitlements. Registers custom "mixin" classes that
 * adapt how conditions and subjects are serialised to/from JSON.
 *
 * @since 12.0.0
 */
public class JsonEntitlementConditionModule extends SimpleModule {
    public JsonEntitlementConditionModule() {
        super("EntitlementCondition", new Version(0, 0, 1, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        // Add mixins for conditions and subjects to rename/ignore particular attributes
        context.setMixInAnnotations(EntitlementCondition.class, JsonEntitlementConditionMixin.class);
        context.setMixInAnnotations(EntitlementSubject.class, JsonEntitlementSubjectMixin.class);
        context.setMixInAnnotations(ResourceAttribute.class, JsonResourceAttributeMixin.class);
    }

}
