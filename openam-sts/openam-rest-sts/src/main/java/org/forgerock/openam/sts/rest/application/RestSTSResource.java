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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.forgerock.openam.sts.rest.config.user.RestDeploymentConfig;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;

@Path("/")
public class RestSTSResource {
    private final RestSTS restSts;
    private final Logger logger;

    public RestSTSResource() {
        RestSTSInstanceConfig instanceConfig = null;
        try {
            instanceConfig = createInstanceConfig("not_used_at_the_moment",
                    "http://localhost:8080/openam");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Injector injector = Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
        restSts = injector.getInstance(RestSTS.class);
        logger = injector.getInstance(Logger.class);
    }


    /*
    This is a stubbed method at this point. At some point in the future, the STSInstanceConfig instance will come from
    UI elements providing for the configuration of STS instances.
    The uriElement is maintained for when we programtically publish REST resources.
     */
    private RestSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder().build();

        RestDeploymentConfig deploymentConfig =
                RestDeploymentConfig.builder()
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        KeystoreConfig keystoreConfig =
                KeystoreConfig.builder()
                        .fileName("stsstore.jks")
                        .password("frstssrvkspw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("frstssrval")
                        .signatureKeyAlias("frstssrval")
                        .encryptionKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return RestSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .amDeploymentUrl(amDeploymentUrl)
                .amJsonRestBase("/json")
                .amRestAuthNUriElement("/authenticate")
                .amRestLogoutUriElement("/sessions/?_action=logout")
                .amRestIdFromSessionUriElement("/users/?_action=idFromSession")
                .amSessionCookieName("iPlanetDirectoryPro")
                .keystoreConfig(keystoreConfig)
                .issuerName("OpenAM")
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.OPENAM,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.USERNAME,
                        TokenType.SAML2,
                        AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)
                .addSupportedTokenTranslation(
                        TokenType.OPENAM,
                        TokenType.SAML2,
                        !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION)

                .build();
    }

    @POST
    @Path("/translate")
//    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response translate(@Context HttpHeaders headers, @Context HttpServletRequest request,
                                 @Context HttpServletResponse response, @QueryParam("desiredTokenType") String desiredTokenType,
                                 String postBody) {
        String returnToken = null;
        try {
            returnToken = restSts.translateToken(postBody, desiredTokenType, request);
            logger.info("Token returned from translateToken: " + returnToken);
//            return Response.ok(buildJson(returnToken), MediaType.APPLICATION_JSON).build();
            return Response.ok(returnToken, MediaType.APPLICATION_XML_TYPE).build();
        } catch (Exception e) {
//            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(buildError(e.getMessage())).build();
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }

    }

    private String buildJson(String returnToken) {
        return JsonValue.json(JsonValue.object(JsonValue.field("translated_token", returnToken))).toString();
    }

    private String buildError(String errorMessage) {
        return JsonValue.json(JsonValue.object(JsonValue.field("error_message", errorMessage))).toString();
    }
}
