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

package org.forgerock.openam.utils.file;

/**
 * FileSize units.
 */
public enum FileSizeUnit {
    /**
     * Gigabyte file size unit.
     */
    GB {
        @Override
        public long convert(long sourceDuration, FileSizeUnit sourceUnit) {
            return sourceUnit.toGB(sourceDuration);
        }

        @Override
        public long toB(long value) {
            return MB.toB(value) * 1024;
        }

        @Override
        public long toKB(long value) {
            return MB.toKB(value) * 1024;
        }

        @Override
        public long toMB(long value) {
            return MB.toMB(value) * 1024;
        }

        @Override
        public long toGB(long value) {
            return value;
        }
    },
    /**
     * Megabyte file size unit.
     */
    MB {
        @Override
        public long convert(long sourceDuration, FileSizeUnit sourceUnit) {
            return sourceUnit.toMB(sourceDuration);
        }

        @Override
        public long toB(long value) {
            return KB.toB(value) * 1024;
        }

        @Override
        public long toKB(long value) {
            return KB.toKB(value) * 1024;
        }

        @Override
        public long toMB(long value) {
            return value;
        }

        @Override
        public long toGB(long value) {
            return GB.toGB(value) / 1024;
        }
    },
    /**
     * Kilobyte file size unit.
     */
    KB {
        @Override
        public long convert(long sourceDuration, FileSizeUnit sourceUnit) {
            return sourceUnit.toKB(sourceDuration);
        }

        @Override
        public long toB(long value) {
            return B.toB(value) * 1024;
        }

        @Override
        public long toKB(long value) {
            return value;
        }

        @Override
        public long toMB(long value) {
            return MB.toMB(value) / 1024;
        }

        @Override
        public long toGB(long value) {
            return MB.toGB(value) / 1024;
        }
    },
    /**
     * Byte file size unit.
     */
    B {
        @Override
        public long convert(long sourceDuration, FileSizeUnit sourceUnit) {
            return sourceUnit.toB(sourceDuration);
        }

        @Override
        public long toB(long value) {
            return value;
        }

        @Override
        public long toKB(long value) {
            return KB.toKB(value) / 1024;
        }

        @Override
        public long toMB(long value) {
            return KB.toMB(value) / 1024;
        }

        @Override
        public long toGB(long value) {
            return KB.toGB(value) / 1024;
        }
    };

    /**
     * Converts a value to a specified unit.
     *
     * @param sourceDuration the value.
     * @param sourceUnit the unit.
     * @return The converted duration.
     */
    public abstract long convert(long sourceDuration, FileSizeUnit sourceUnit);

    /**
     * Convert to B.
     *
     * @param value the value
     * @return size in B
     */
    public abstract long toB(long value);

    /**
     * Convert to KB.
     *
     * @param value the value
     * @return size in KB
     */
    public abstract long toKB(long value);

    /**
     * Convert to MB.
     *
     * @param value the value
     * @return size in MB
     */
    public abstract long toMB(long value);

    /**
     * Convert to GB.
     *
     * @param value the value
     * @return size in GB
     */
    public abstract long toGB(long value);
}
