/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package org.forgerock.openam.session.ha.amsessionstore.store;

import com.iplanet.dpro.session.service.AMSessionRepository;

import java.util.*;


/**
 * AM Session Repository Type
 *
 * @author jeff.schenk@forgerock.com
 */
public enum AMSessionRepositoryType {

    /**
     * Enumerator Values
     */
    none(0, 0, "none", "None", "Session Failover High Availability Disabled", "session.store.type.none", null),

    embedded(1, 1, "embedded", "Embedded", "OpenAM Configuration Directory", "session.store.type.embedded",
            org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore.class),

    external(2, 2, "external", "External", "OpenAM External OpenDJ Directory", "session.store.type.external",
            org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore.class),

    plugin(3, 3, "plugin", "Plug-In", "External Plug-In", "session.store.type.plugin",
            org.forgerock.openam.session.ha.amsessionstore.store.plugin.PlugInPersistentStore.class);

    /**
     * Index
     */
    private final int index;
    /**
     * Text for Type
     */
    private final String type;
    /**
     * Text for Type
     */
    private final String displayType;
    /**
     * Display Order
     */
    private final int displayOrder;
    /**
     * Long Text For Type
     */
    private final String textDefinition;
    /**
     * Associated AMSessionRepository Implementation Class.
     */
    private final Class<? extends AMSessionRepository> amSessionRepositoryImplementationClass;

    /**
     * Index Entry
     * @return int of Enum
     */
    public int index() {
        return this.index;
    }
    /**
     * Index Entry
     * @return int of Enum
     */
    public Class<? extends AMSessionRepository> amSessionRepositoryImplementationClass() {
        return this.amSessionRepositoryImplementationClass;
    }
    /**
     * Index Entry
     * @return int of Enum
     */
    public String type() {
        return this.type;
    }
    /**
     * Display Type
     * @return String of Display Type
     */
    public String displayType() {
        return this.displayType;
    }
    /**
     * Text Definition
     * @return String of Textual Definition
     */
    public String textDefinition() {
        return this.textDefinition;
    }

    /**
     * Private, internally used constructor.
     *
     * @param index
     * @param type
     * @param textDefinition
     */
    private AMSessionRepositoryType(int index, int displayOrder, String type, String displayType, String textDefinition,
                                    String i18nKeyName, Class<? extends AMSessionRepository> amSessionRepositoryImplementationClass) {
        this.index = index;
        this.displayOrder = displayOrder;
        this.type = type;
        this.displayType = displayType;
        this.textDefinition = textDefinition;

        this.amSessionRepositoryImplementationClass = amSessionRepositoryImplementationClass;
    }

    /**
     * Obtain a Map of the Various Types.
     * @return Map<String, String>
     */
    public static Map<String, String> getAMSessionRepositoryTypes() {
        Map<String, String> typeMap = new TreeMap<String, String>();
        for (AMSessionRepositoryType type : AMSessionRepositoryType.values()) {
            typeMap.put(String.valueOf(type.type), type.textDefinition);
        }
        return typeMap;
    }

    /**
     * Obtain the Am Session Repository Type Textual Information.
     *
     * @param type
     * @return String
     */
    public static String getAMSessionRepositoryTypeText(String type) {
        for (AMSessionRepositoryType amSessionRepositoryType : AMSessionRepositoryType.values()) {
            if (amSessionRepositoryType.type().equalsIgnoreCase(type)) {
                return amSessionRepositoryType.textDefinition;
            } else if (amSessionRepositoryType.displayType.equalsIgnoreCase(type)) {
                return amSessionRepositoryType.textDefinition;
            }
        }
        return null;
    }

    /**
     * Obtain the AM Session Repository Implementation Class.
     *
     * @param type
     * @return String
     */
    public static Class<? extends AMSessionRepository> getAMSessionRepositoryTypeImplementationClass(String type) {
        for (AMSessionRepositoryType amSessionRepositoryType : AMSessionRepositoryType.values()) {
            if (amSessionRepositoryType.type().equalsIgnoreCase(type)) {
                return amSessionRepositoryType.amSessionRepositoryImplementationClass;
            }  else if (amSessionRepositoryType.displayType.equalsIgnoreCase(type)) {
                return amSessionRepositoryType.amSessionRepositoryImplementationClass;
            }
        }
        return null;
    }

}
