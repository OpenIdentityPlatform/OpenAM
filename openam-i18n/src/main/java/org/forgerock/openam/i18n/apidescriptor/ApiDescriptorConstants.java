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
package org.forgerock.openam.i18n.apidescriptor;

/**
 * Constants for Api Descriptor I18N used across OpenAM.
 *
 * @since 14.0.0
 */
public final class ApiDescriptorConstants {

    /** Constant used as key to API Descriptor translations **/
    public static final String TRANSLATION_KEY_PREFIX = "i18n:api-descriptor/";

    /** Constant used as key to API Descriptor translations **/
    public static final String TITLE = "title";

    /** Constant used as key to API Descriptor translations **/
    public static final String DESCRIPTION = "description";

    /** Constant used as key to API Descriptor translations **/
    public static final String CREATE = "create.";

    /** Constant used as key to API Descriptor translations **/
    public static final String READ = "read.";

    /** Constant used as key to API Descriptor translations **/
    public static final String UPDATE = "update.";

    /** Constant used as key to API Descriptor translations **/
    public static final String DELETE = "delete.";

    /** Constant used as key to API Descriptor translations **/
    public static final String PATCH = "patch.";

    /** Constant used as key to API Descriptor translations **/
    public static final String ACTION = "action.";

    /** Constant used as key to API Descriptor translations **/
    public static final String QUERY = "query.";

    /** Constant used as key to API Descriptor translations **/
    public static final String CREATE_DESCRIPTION = CREATE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String READ_DESCRIPTION = READ + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String UPDATE_DESCRIPTION = UPDATE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String DELETE_DESCRIPTION = DELETE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String PATCH_DESCRIPTION = PATCH + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ACTION_DESCRIPTION = ACTION + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String QUERY_DESCRIPTION = QUERY + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_400_DESCRIPTION = "error.400." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_409_DESCRIPTION = "error.409." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_500_DESCRIPTION = "error.500." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String EXAMPLE_PROVIDER = TRANSLATION_KEY_PREFIX + "ExampleProvider#";

    /** Constant used as key to {@code RecordResource} resource location **/
    public static final String RECORD_RESOURCE = TRANSLATION_KEY_PREFIX + "RecordResource#";

    private ApiDescriptorConstants() {
        // Constants class only
    }
}
