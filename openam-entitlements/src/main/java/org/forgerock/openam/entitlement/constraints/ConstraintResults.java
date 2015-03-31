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
package org.forgerock.openam.entitlement.constraints;

/**
 * Helpful class for creating constraint results.
 *
 * @since 13.0.0
 */
public final class ConstraintResults {

    private ConstraintResults() {
        throw new UnsupportedOperationException();
    }

    /**
     * Embodies a successful result.
     */
    private static final class ConstraintSuccessImpl implements ConstraintResult {

        @Override
        public boolean isSuccessful() {
            return true;
        }

        @Override
        public void throwExceptionIfFailure() throws ConstraintFailureException {
            // Do nothing by default.
        }

    }

    /**
     * Embodies a failure result.
     */
    private static final class ConstraintFailureImpl implements ConstraintResult {

        private final String attributeName;
        private final String invalidValue;

        ConstraintFailureImpl(String attributeName, String invalidValue) {
            this.attributeName = attributeName;
            this.invalidValue = invalidValue;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        @Override
        public void throwExceptionIfFailure() throws ConstraintFailureException {
            throw new ConstraintFailureException(attributeName, invalidValue);
        }

    }

    /**
     * Creates a new successful result.
     *
     * @return successful result
     */
    public static ConstraintResult newSuccess() {
        return new ConstraintSuccessImpl();
    }

    /**
     * Creates a new failure result.
     *
     * @param attributeName
     *         the attribute name associated with the invalid value
     * @param invalidValue
     *         the invalid value
     *
     * @return failure result
     */
    public static ConstraintResult newFailure(String attributeName, String invalidValue) {
        return new ConstraintFailureImpl(attributeName, invalidValue);
    }

}
