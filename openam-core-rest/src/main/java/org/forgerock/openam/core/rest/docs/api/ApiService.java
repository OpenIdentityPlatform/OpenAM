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
package org.forgerock.openam.core.rest.docs.api;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.transform.LocalizableOperation;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.http.ApiDescriptorFilter;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;

import io.swagger.models.Operation;
import io.swagger.models.Path;

/**
 * This service provides an HTML representation of the OpenAM REST API.
 *
 * @since 14.0.0
 */
public class ApiService {

    private static final String ROOT_URI = "/?_api";
    private DescribableHandler describableHandler;
    private static final String EMPTY_STRING = "";
    private static final Pattern REMOVE_VERSION = Pattern.compile(" v[0-9].[0-9]");
    private static final Pattern REMOVE_RESOURCE_ID = Pattern.compile("/\\{(.+)\\}");
    private static final String OPERATION_DELIMITER = "#";

    /**
     *
     * @param describableHandler
     */
    @Inject
    public ApiService( @Named("RestHandler") DescribableHandler describableHandler) {
        this.describableHandler = describableHandler;
    }

    /**
     *
     * @param request
     * @return
     * @throws URISyntaxException
     * @throws MalformedHeaderException
     */
    @Get
    public Response handle(@Contextual Request request) throws URISyntaxException, MalformedHeaderException {

        if (!ApiDescriptorFilter.State.INSTANCE.isEnabled()) {
            return new Response(Status.NOT_IMPLEMENTED);
        }

        Map<String, Set<String>> groups = getGroups(
                describableHandler.handleApiRequest(new UriRouterContext(new RootContext(),EMPTY_STRING, EMPTY_STRING,
                        Collections.<String, String>emptyMap()), new Request().setUri(ROOT_URI)).getPaths(),
                getPreferredLocales(request));

        return new Response(Status.OK).setEntity(JsonValue.json(groups));
    }

    private Map<String, Set<String>> getGroups(Map<String, Path> paths, PreferredLocales preferredLocales) {
        Map<String, Set<String>> groups = new HashMap<>();

        for ( Map.Entry<String, Path> pathEntry : paths.entrySet()) {
            List<Operation> operations = pathEntry.getValue().getOperations();
            String apiPath = getApiPath(pathEntry.getKey());

            for (Operation operation : operations) {
                List<String> groupNames = getGroupNames(operation, preferredLocales);

                for (String groupName : groupNames) {
                    if (groups.containsKey(groupName)) {
                        groups.get(groupName).add(apiPath);
                    } else {
                        groups.put(groupName, Sets.newHashSet(apiPath));
                    }
                }
            }
        }
        return groups;
    }

    private String getApiPath(String path) {
        return REMOVE_RESOURCE_ID.matcher(path.split(OPERATION_DELIMITER)[0]).replaceAll(EMPTY_STRING);
    }

    private PreferredLocales getPreferredLocales(Request request) throws MalformedHeaderException {
        return request.getHeaders().get(AcceptLanguageHeader.class).getLocales();
    }

    private List<String> getGroupNames(Operation operation, PreferredLocales locales) {
        List<String> groupNames = Lists.newArrayList();

        if (operation instanceof LocalizableOperation) {
            List<LocalizableString> localizableTags = ((LocalizableOperation)operation).getLocalizableTags();
            for (LocalizableString operationTag : localizableTags) {
                groupNames.add(REMOVE_VERSION.matcher(operationTag.toTranslatedString(locales)).replaceAll(EMPTY_STRING));
            }
        } else {
            List<String> operationTags = operation.getTags();
            for (String operationTag : operationTags) {
                groupNames.add(REMOVE_VERSION.matcher(operationTag).replaceAll(EMPTY_STRING));
            }
        }
        return groupNames;
    }

}
