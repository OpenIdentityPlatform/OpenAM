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
 */
package org.forgerock.openam.core.rest.server;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents server version information that can be queried via a REST interface.
 *
 * This resource is a read-only.
 * @since 13.0.0
 */
@SingletonProvider(
    value = @Handler(
        title = SERVER_VERSION_RESOURCE + TITLE,
        description = SERVER_VERSION_RESOURCE + DESCRIPTION,
        resourceSchema = @Schema(schemaResource = "ServerVersionResource.schema.json"),
        mvccSupported = false
    )
)
public class ServerVersionResource implements SingletonResourceProvider {

    private final static String SERVER_VERSION = "version";

    /**
     * Used to identity the numeric version element of the AM version string.
     */
    private final static Pattern NUMERIC_VERSION_PATTERN = Pattern.compile("^(?:.*?(\\d+\\.\\d+\\.?\\d*\\S*))");

    /**
     * Parses the version string, extracts and returns it's numeric element
     * @return The numeric element of the server version
     */
    private String getVersion() {
        String version = SystemProperties.get(Constants.AM_BUILD_VERSION);
        Matcher matcher = NUMERIC_VERSION_PATTERN.matcher(version);

        if (matcher.matches()) {
            version = matcher.group(1);
        }

        return version;
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest actionRequest) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest patchRequest) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Read(
        operationDescription = @Operation(
            description = SERVER_VERSION_RESOURCE + READ_DESCRIPTION,
            errors = {
                @ApiError(
                    code = 403,
                    description = SERVER_VERSION_RESOURCE + ERROR_403_DESCRIPTION
                )
            }
        )
    )
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest readRequest) {
        JsonValue result = json(object(
                field("version", getVersion()),
                field("revision", SystemProperties.get(Constants.AM_BUILD_REVISION)),
                field("date", SystemProperties.get(Constants.AM_BUILD_DATE))
        ));

        return newResourceResponse(SERVER_VERSION, Integer.toString(result.asMap().hashCode()), result).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest updateRequest) {
        return new NotSupportedException().asPromise();
    }
}
