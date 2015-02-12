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

package org.forgerock.openam.sm.datalayer.api;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * An annotation for marking injectable objects as for a specific data layer.
 */
@Qualifier
@Retention(RUNTIME)
public @interface DataLayer {
    ConnectionType value();

    /**
     * A utility class for creating DataLayer instances for using with injection keys.
     */
    public static final class Types {
        public static DataLayer typed(ConnectionType type) {
            return new DataLayerImpl(type);
        }

        /**
         * Standard implementation of an annotation interface.
         * @see java.lang.annotation.Annotation
         */
        private static final class DataLayerImpl implements DataLayer {

            private final ConnectionType type;

            private DataLayerImpl(ConnectionType type) {
                this.type = type;
            }

            @Override
            public ConnectionType value() {
                return type;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return DataLayer.class;
            }

            @Override
            public int hashCode() {
                return (127 * "value".hashCode()) ^ type.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DataLayer && ((DataLayer) o).value() == type;
            }

            @Override
            public String toString() {
                return "@" + DataLayer.class.getName() + "(value=" + type.name() + ")";
            }
        }
    }
}
