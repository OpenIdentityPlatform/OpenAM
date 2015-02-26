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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.guice;

import com.google.inject.Module;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Creates instances of Guice modules.
 *
 * <p>All Guice modules must have one and only one public no-arg constructor.</p>
 *
 * <p>This class must be able to operate before Guice is initialised, as it is used to initialise Guice.</p>
 *
 * @author Phill Cunnington
 */
public class GuiceModuleCreator {

    /**
     * Creates an instance of the Guice module class.
     *
     * @param clazz The Guice module class.
     * @param <T> The Guice module class type.
     * @return An instance of the Guice module.
     */
    public <T extends Module> T createInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = getConstructor(clazz);
            return constructor.newInstance();
        } catch (InstantiationException e) {
            throw new ModuleCreationException(e);
        } catch (IllegalAccessException e) {
            throw new ModuleCreationException(e);
        } catch (NoSuchMethodException e) {
            throw new ModuleCreationException(e);
        } catch (InvocationTargetException e) {
            throw new ModuleCreationException(e);
        }
    }

    /**
     * Finds and returns the Constructor to use to create the Guice module.
     *
     * Note: There must be one and only one public no-arg constructor for the Guice module.
     *
     * @param clazz The Guice module class.
     * @param <T> The Guice module class type.
     * @return The public no-arg constructor.
     * @throws NoSuchMethodException If no public no-arg constructor exists in this class.
     */
    private <T> Constructor<T> getConstructor(Class<T> clazz) throws NoSuchMethodException {

        Constructor constructor = ConstructorUtils.getAccessibleConstructor(clazz, new Class[]{});

        if (constructor != null) {
            return constructor;
        } else {
            throw new NoSuchMethodException(String.format("No public zero-arg constructor found on %s",
                clazz.getCanonicalName()));
        }
    }
}
