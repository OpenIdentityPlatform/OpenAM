/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.common;

import org.forgerock.i18n.LocalizableMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 *
 * @author peter.major
 * @author steve
 */
public final class SystemProperties {
    private static Properties properties;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        properties = new Properties();
        InputStream pin = ClassLoader.getSystemResourceAsStream(Constants.PROPERTIES_FILE);
        
        try {
            properties.load(pin);
        } catch (IOException ioe) {
            System.out.println("IOException " + ioe.getMessage());
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null) ? value : defaultValue;
    }

    public static boolean getAsBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.valueOf(value) : defaultValue;
    }

    public static int getAsInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return value != null? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException nfe) {
            final LocalizableMessage message = DB_PROP_ERR.get(value);
            Log.logger.log(Level.WARNING, message.toString());
            return defaultValue;
        }
    }
}
