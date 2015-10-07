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
package org.forgerock.openam.selfservice;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.email.EmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.services.context.Context;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Utilises the common anonymous process service to deliver forgotten password behaviour.
 *
 * @since 13.0.0
 */
final class ForgottenPasswordRequestHandler extends AbstractSelfServiceRequestHandler {

    private final BaseURLProviderFactory baseURLProviderFactory;

    @Inject
    public ForgottenPasswordRequestHandler(ProgressStageFactory stageFactory,
            SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore localStore,
            BaseURLProviderFactory baseURLProviderFactory) {
        super(stageFactory, tokenHandlerFactory, localStore);
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    @Override
    protected ProcessInstanceConfig getServiceConfig(Context context, String realm) {
        String baseUrl = baseURLProviderFactory
                .get(realm)
                .getURL(context.asContext(HttpContext.class));

        StringBuilder serverUrl = new StringBuilder(baseUrl);

        if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
            serverUrl.append('/');
        }

        serverUrl.append("XUI/#passwordReset/&realm=").append(realm);

        StageConfig verifyUserIdConfig = new VerifyUserIdConfig(new EmailAccountConfig())
                .setQueryFields(new HashSet<>(Arrays.asList("uid", "mail")))
                .setIdentityIdField("/uid/0")
                .setIdentityEmailField("/mail/0")
                .setIdentityServiceUrl("/users")
                .setEmailServiceUrl("/email")
                .setEmailFrom("info@admin.org")
                .setEmailSubject("Reset password email")
                .setEmailMessage("<h3>This is your reset email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailMimeType("text/html")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink(serverUrl.toString());

        StageConfig resetConfig = new ResetStageConfig()
                .setIdentityServiceUrl("/users")
                .setIdentityPasswordField("userPassword");

        String secret = SystemProperties.get(Constants.ENC_PWD_PROPERTY);
        JwtTokenHandlerConfig jwtTokenConfig = new JwtTokenHandlerConfig();
        jwtTokenConfig.setSharedKey(secret);
        jwtTokenConfig.setKeyPairAlgorithm("RSA");
        jwtTokenConfig.setKeyPairSize(1024);
        jwtTokenConfig.setJweAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5);
        jwtTokenConfig.setEncryptionMethod(EncryptionMethod.A128CBC_HS256);
        jwtTokenConfig.setJwsAlgorithm(JwsAlgorithm.HS256);
        jwtTokenConfig.setTokenLifeTimeInSeconds(3L * 60L);

        return new ProcessInstanceConfig()
                .setStageConfigs(Arrays.asList(verifyUserIdConfig, resetConfig))
                .setSnapshotTokenConfig(jwtTokenConfig)
                .setStorageType(StorageType.STATELESS);
    }

}