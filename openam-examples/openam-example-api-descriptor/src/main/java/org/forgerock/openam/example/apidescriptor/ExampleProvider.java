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
package org.forgerock.openam.example.apidescriptor;

import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

@CollectionProvider(details = @Handler(
        title = EXAMPLE_PROVIDER + TITLE,
        description = EXAMPLE_PROVIDER + DESCRIPTION,
        mvccSupported = false))
public class ExampleProvider {

    @Read(operationDescription = @Operation(
            description = EXAMPLE_PROVIDER + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> read(Context context, String id, ReadRequest request) {
        return null;
    }
}
