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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.selfservice;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.selfservice.config.beans.SecurityQuestionTransformer;
import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * KBA resource is responsible for delivering up configured security questions.
 *
 * @since 13.0.0
 */
@SingletonProvider(value = @Handler(
        title = KBA_RESOURCE + TITLE,
        description = KBA_RESOURCE + DESCRIPTION,
        mvccSupported = false,
        resourceSchema = @Schema(schemaResource = "KbaResource.schema.json")))
public final class KbaResource {

    private final ConsoleConfigHandler configHandler;

    @Inject
    KbaResource(ConsoleConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    /**
     * Read the configured security questions.
     *
     * @param context The request server context.
     * @param readRequest The read request.
     *
     * @return A {@code Promise} containing the result of the operation.
     */
    @Read(operationDescription =
        @Operation(description = KBA_RESOURCE + READ_DESCRIPTION,
            errors = {
                @ApiError(code = INTERNAL_ERROR, description = KBA_RESOURCE + READ + ERROR_500_DESCRIPTION)
            }))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest readRequest) {
        String realm = RealmContext.getRealm(context).asPath();
        JsonValue kbaJson = configHandler.getConfig(realm, KbaBuilder.class);
        ResourceResponse response = Responses.newResourceResponse("1", "1.0", kbaJson);
        return Promises.newResultPromise(response);
    }

    /**
     * Builder intended for the use by {@link KbaResource} for the purpose of retrieve KBA config.
     */
    @ConfigSource("selfService")
    public static final class KbaBuilder implements ConsoleConfigBuilder<JsonValue> {

        private final Map<String, Map<String, String>> securityQuestions;
        private int minimumAnswersToDefine;
        private int minimumAnswersToVerify;

        KbaBuilder() {
            securityQuestions = new HashMap<>();
        }

        /**
         * Set the security questions.
         *
         * @param securityQuestions A list of security questions and their locals.
         */
        @ConfigAttribute(value = "selfServiceKBAQuestions", transformer = SecurityQuestionTransformer.class)
        public void setSecurityQuestions(Map<String, Map<String, String>> securityQuestions) {
            this.securityQuestions.putAll(securityQuestions);
        }

        /**
         * Set the minimum answers to define.
         *
         * @param minimumAnswersToDefine Minimum answers to define.
         */
        @ConfigAttribute("selfServiceMinimumAnswersToDefine")
        public void setMinimumAnswersToDefine(int minimumAnswersToDefine) {
            this.minimumAnswersToDefine = minimumAnswersToDefine;
        }

        /**
         * Set the minimum answers to verify.
         *
         * @param minimumAnswersToVerify Minimum answers to verify.
         */
        @ConfigAttribute("selfServiceMinimumAnswersToVerify")
        public void setMinimumAnswersToVerify(int minimumAnswersToVerify) {
            this.minimumAnswersToVerify = minimumAnswersToVerify;
        }

        @Override
        public JsonValue build(Map<String, Set<String>> attributes) {
            Reject.ifTrue(minimumAnswersToVerify > minimumAnswersToDefine,
                    "Number of answers to verify must be equal or less than those defined");

            return json(
                    object(
                            field("questions", securityQuestions),
                            field("minimumAnswersToDefine", minimumAnswersToDefine),
                            field("minimumAnswersToVerify", minimumAnswersToVerify)));
        }

    }

}
