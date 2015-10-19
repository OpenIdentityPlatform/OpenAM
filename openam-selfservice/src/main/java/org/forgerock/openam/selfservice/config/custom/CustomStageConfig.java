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

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Stage config that represents a customisable stage.
 *
 * @since 13.0.0
 */
public final class CustomStageConfig implements StageConfig<CustomSupportConfigVisitor> {

    private static final String NAME = "Custom stage config";

    private String progressStageClassName;
    private final Map<String, String> options;

    /**
     * Constructs a new custom stage config.
     */
    public CustomStageConfig() {
        options = new HashMap<>();
    }

    /**
     * Gets the underlying progress stage class name to be invoked.
     *
     * @return the progress stage class name
     */
    public String getProgressStageClassName() {
        return progressStageClassName;
    }

    /**
     * Sets the underlying progress stage class name to be invoked.
     *
     * @param progressStageClassName
     *         the progress stage class name
     *
     * @return this config
     */
    public CustomStageConfig setProgressStageClassName(String progressStageClassName) {
        this.progressStageClassName = progressStageClassName;
        return this;
    }

    /**
     * Gets the map of potential options to be consumed by the custom stage.
     *
     * @return map of options
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Sets the map of potential options to be consumed by the custom stage.
     *
     * @param options
     *         map of options
     *
     * @return this config
     */
    public CustomStageConfig setOptions(Map<String, String> options) {
        this.options.putAll(options);
        return this;
    }

    /**
     * Adds an option to the map of options to be consumed by the custom stage.
     *
     * @param key
     *         option key
     * @param value
     *         option value
     *
     * @return this config
     */
    public CustomStageConfig addOption(String key, String value) {
        options.put(key, value);
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ProgressStageBinder<?> accept(CustomSupportConfigVisitor visitor) {
        return visitor.build(this);
    }

}
