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
package org.forgerock.openam.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.openam.keystore.KeyStoreConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Holds Bootstrap Configuration and serializes to/from json
 *
 * @since 14.0
 */
public class BootstrapConfig {
    @JsonIgnore
    private final static ObjectMapper objectMapper = new ObjectMapper();
    // pattern we use to match for ${env.ENV_VAR} or ${ENV} to run expansions
    private final static Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{(\\w*\\.?\\w+)\\}|\\$(\\w+)");
    private String instance;
    private String dsameUser;
    private Map<String, KeyStoreConfig> keystores = new HashMap<String, KeyStoreConfig>();
    // Dont save password json - it must be in the keystore
    @JsonIgnore
    private String dsameUserPassword;
    private List<ConfigStoreProperties> configStore = new ArrayList<ConfigStoreProperties>();


    public BootstrapConfig() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static BootstrapConfig fromJson(String json) throws IOException {
        String expanded = expandEnvironmentVariables(json);
        return objectMapper.readValue(expanded, BootstrapConfig.class);
    }

    public static BootstrapConfig fromJsonFile(String file) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(file)));
        return BootstrapConfig.fromJson(json);
    }

    /**
     * Return the string value, expanding any environment variables
     * found in the expression.
     * <p>
     * For example    "Home dir is ${env.HOME}"  will expand using the env var "HOME"
     * to something like "Home dir is /home/justin_bieber"
     *
     * @param value The string to perform env val substitution on
     * @return The expanded string value
     */

    static String expandEnvironmentVariables(String value) {
        if (null == value) {
            return null;
        }
        Matcher m = ENV_VAR_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String envVarValue = null;
            String envVarName = null == m.group(1) ? m.group(2) : m.group(1);
            if( envVarName.startsWith(("env."))) {
                envVarValue = System.getenv(envVarName.substring(3));
            }
            else {
                envVarValue = System.getProperty(envVarName);
            }
            m.appendReplacement(sb,
                    null == envVarValue ? "" : Matcher.quoteReplacement(envVarValue));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String getDsameUserPassword() {
        return dsameUserPassword;
    }

    public void setDsameUserPassword(String dsameUserPassword) {
        this.dsameUserPassword = dsameUserPassword;
    }

    public Map<String, KeyStoreConfig> getKeystores() {
        return keystores;
    }

    public String getDsameUser() {
        return dsameUser;
    }

    public void setDsameUser(String dsameUser) {
        this.dsameUser = dsameUser;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public List<ConfigStoreProperties> getConfigStoreList() {
        return configStore;
    }

    public void addConfigStore(ConfigStoreProperties configStore) {
        this.configStore.add(configStore);
    }

    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public void addKeystoreConfig(String name, KeyStoreConfig ksc) {
        this.keystores.put(name, ksc);
    }

    // Pattern we match on is   ${env.NAME}

    public KeyStoreConfig getKeyStoreConfig(String name) {
        return this.keystores.get(name);
    }

    public void writeConfig(String file) throws IOException {
        Files.write(Paths.get(file), toJson().getBytes());
    }

    public String toString() {
        String s = null;
        try {
            s = toJson();
        }
        catch( Exception e) {
            // ignore - we try to convert to string - but if this fails return the default
            s = "BootStrapConfig " + instance;
        }
        return s;
    }
}
