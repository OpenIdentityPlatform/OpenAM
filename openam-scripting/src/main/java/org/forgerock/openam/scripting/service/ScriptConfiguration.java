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
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;

import org.forgerock.openam.scripting.ScriptConstants.ScriptContext;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.utils.StringUtils;

import java.util.UUID;

/**
 * This class represents the configuration for a script in OpenAM. It supplies a builder class for easy
 * construction and error reporting.
 *
 * @since 13.0.0
 */
public class ScriptConfiguration {

    private final String uuid;
    private final String name;
    private final String script;
    private final SupportedScriptingLanguage language;
    private final ScriptContext context;
    private final String description;
    private final String createdBy;
    private final long creationDate;
    private final String lastModifiedBy;
    private final long lastModifiedDate;

    private volatile int hashCode = 0;

    /**
     * Builder for {@code ScriptConfiguration}.
     */
    public static class Builder {
        private String uuid;
        private String name;
        private String script;
        private SupportedScriptingLanguage language;
        private ScriptContext context;
        private String description;
        private String createdBy;
        private long creationDate;
        private String lastModifiedBy;
        private long lastModifiedDate;

        /**
         * This {@code Builder} can be constructed from {@code ScriptConfiguration} with the
         * {@link org.forgerock.openam.scripting.service.ScriptConfiguration#builder()} builder} method.
         */
        private Builder() {
        }

        /**
         * Generate a universally unique identifier for the {@code ScriptConfiguration}.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder generateUuid() {
            this.uuid = UUID.randomUUID().toString();
            return this;
        }

        /**
         * Set a universally unique identifier for the {@code ScriptConfiguration}.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        /**
         * Set the name for the {@code ScriptConfiguration}.
         * @param name A display name for the {@code ScriptConfiguration}.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the script for the {@code ScriptConfiguration}.
         * @param script The script that is represented by this configuration.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setScript(String script) {
            this.script = script;
            return this;
        }

        /**
         * Set the language for the {@code ScriptConfiguration}.
         * @param language The language in which the script is written.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setLanguage(SupportedScriptingLanguage language) {
            this.language = language;
            return this;
        }

        /**
         * Set the context for the {@code ScriptConfiguration}.
         * @param context The context in which the script will be used.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setContext(ScriptContext context) {
            this.context = context;
            return this;
        }

        /**
         * Add the description for the {@code ScriptConfiguration}.
         * @param description The description.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the ID of the user that created the {@code ScriptConfiguration}.
         * @param createdBy The user ID.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * Set the creation date of the {@code ScriptConfiguration}.
         * @param creationDate The creation date in milliseconds.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setCreationDate(long creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * Set the ID of the user that last modified the {@code ScriptConfiguration}.
         * @param lastModifiedBy The user ID.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setLastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        /**
         * Set the last modified date of the {@code ScriptConfiguration}.
         * @param lastModifiedDate The last modified date.
         * @return The {@code ScriptConfiguration} builder.
         */
        public Builder setLastModifiedDate(long lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        /**
         * Construct the {@code ScriptConfiguration} with the parameters set on this builder.
         * @return An instance of {@code ScriptConfiguration}.
         * @throws ScriptException if any of the required parameters are null.
         */
        public ScriptConfiguration build() throws ScriptException {
            if (uuid == null) {
                throw new ScriptException(MISSING_SCRIPT_UUID);
            }
            if (name == null) {
                throw new ScriptException(MISSING_SCRIPT_NAME);
            }
            if (script == null) {
                throw new ScriptException(MISSING_SCRIPT);
            }
            if (language == null) {
                throw new ScriptException(MISSING_SCRIPTING_LANGUAGE);
            }
            if (context == null) {
                throw new ScriptException(MISSING_SCRIPT_CONTEXT);
            }
            return new ScriptConfiguration(this);
        }
    }

    /**
     * Construct a {@code ScriptConfiguration} with the given builder.
     * @param builder The builder that contains the parameters for the {@code ScriptConfiguration}.
     */
    private ScriptConfiguration(Builder builder) {
        this.uuid = builder.uuid;
        this.name = builder.name;
        this.script = builder.script;
        this.language = builder.language;
        this.context = builder.context;
        this.description = builder.description;
        this.createdBy = builder.createdBy;
        this.creationDate = builder.creationDate;
        this.lastModifiedBy = builder.lastModifiedBy;
        this.lastModifiedDate = builder.lastModifiedDate;
    }

    /**
     * Create a builder for {@code ScriptConfiguration}.
     * @return A {@code ScriptConfiguration} builder.
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Get the universally unique identifier for the {@code ScriptConfiguration}.
     * @return The UUID.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Get the name for the {@code ScriptConfiguration}.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the script represented by the {@code ScriptConfiguration}.
     * @return The script.
     */
    public String getScript() {
        return script;
    }

    /**
     * Get the language in which the script is written.
     * @return The scripting language.
     */
    public SupportedScriptingLanguage getLanguage() {
        return language;
    }

    /**
     * Get the context in which the script will be used.
     * @return The script context.
     */
    public ScriptContext getContext() {
        return context;
    }

    /**
     * Get the description for the script.
     * @return The script description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the ID of the user that created the script.
     *
     * @return The user ID.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Get the date this script was created.
     * @return The creation date.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Get the ID of the user that last modified the script.
     *
     * @return The user ID.
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Get the date this script was last modified.
     * @return The last modified date.
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ScriptConfiguration)) {
            return false;
        }
        ScriptConfiguration sc = (ScriptConfiguration)o;
        return sc.uuid.equals(uuid)
                && sc.name.equals(name)
                && StringUtils.isEqualTo(sc.script, script)
                && sc.language.equals(language)
                && sc.context.equals(context)
                && StringUtils.isEqualTo(sc.description, description)
                && StringUtils.isEqualTo(sc.createdBy, createdBy)
                && sc.creationDate == creationDate
                && StringUtils.isEqualTo(sc.lastModifiedBy, lastModifiedBy)
                && sc.lastModifiedDate == lastModifiedDate;
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            int prime = 31;
            result = 17;
            result = prime * result + uuid.hashCode();
            result = prime * result + name.hashCode();
            result = prime * result + script.hashCode();
            result = prime * result + language.hashCode();
            result = prime * result + context.hashCode();
            result = prime * result + (description == null ? 0 : description.hashCode());
            result = prime * result + (createdBy == null ? 0 : createdBy.hashCode());
            result = prime * result + (int) (creationDate ^ (creationDate >>> 32));
            result = prime * result + (lastModifiedBy == null ? 0 : lastModifiedBy.hashCode());
            result = prime * result + (int) (lastModifiedDate ^ (lastModifiedDate >>> 32));
            hashCode = result;
        }
        return result;
    }

    /**
     * Create a builder for this {@code ScriptConfiguration} with all fields populated and ready for modification.
     * @return A populated {@code ScriptConfiguration} builder.
     */
    public Builder populatedBuilder() {
        return builder().setUuid(uuid).setName(name).setContext(context).setLanguage(language).setScript(script)
                .setDescription(description).setCreatedBy(createdBy).setCreationDate(creationDate)
                .setLastModifiedBy(lastModifiedBy).setLastModifiedDate(lastModifiedDate);
    }

}
