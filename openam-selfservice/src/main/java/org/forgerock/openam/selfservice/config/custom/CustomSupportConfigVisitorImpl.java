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

package org.forgerock.openam.selfservice.config.custom;

import com.google.inject.Injector;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.stages.CommonConfigVisitor;
import org.forgerock.selfservice.stages.CommonConfigVisitorDecorator;

import javax.inject.Inject;

/**
 * Visitor implementation for the support for custom stages. Makes use of guice to instantiate new progress stages.
 *
 * @since 13.0.0
 */
public final class CustomSupportConfigVisitorImpl
        extends CommonConfigVisitorDecorator implements CustomSupportConfigVisitor {

    private final Injector injector;

    /**
     * Constructs a new custom support configuration visitor.
     *
     * @param decoratedVisitor
     *         Existing visitor to be wrapped
     * @param injector
     *         guice injector
     */
    @Inject
    public CustomSupportConfigVisitorImpl(CommonConfigVisitor decoratedVisitor, Injector injector) {
        super(decoratedVisitor);
        this.injector = injector;
    }

    @Override
    public ProgressStageBinder<?> build(CustomStageConfig config) {
        try {
            Class<? extends ProgressStage> progressStageClass = Class
                    .forName(config.getProgressStageClassName())
                    .asSubclass(ProgressStage.class);

            @SuppressWarnings("unchecked")
            ProgressStage<CustomStageConfig> stage = injector.getInstance(progressStageClass);
            return ProgressStageBinder.bind(stage, config);
        } catch (ClassNotFoundException cnfE) {
            throw new CustomStageNotFoundException(
                    "Unable to find progress stage" + config.getProgressStageClassName(), cnfE);
        }
    }

}
