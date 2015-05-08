/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade.steps;

import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This interface defines the methods that needs to be implemented for an upgrade step. Non-abstract implementations
 * must have the @{@link org.forgerock.openam.upgrade.UpgradeStepInfo} annotation.
 *
 * @author Peter Major
 */
public interface UpgradeStep {

    /**
     * Tells whether this given upgrade step is applicable for this version/configuration of OpenAM.
     *
     * @return <code>true</code> if this upgrade step needs to be performed.
     */
    public boolean isApplicable();

    /**
     * Initializes the upgrade step during which it determines what sort of changes needs to be applied to the
     * configurationif any.
     *
     * @throws UpgradeException If there was an error while determining the required changes.
     */
    public void initialize() throws UpgradeException;

    /**
     * Performs the required changes for this upgrade step.
     *
     * @throws UpgradeException If there was an error while upgrading the configuration.
     */
    public void perform() throws UpgradeException;

    /**
     * Returns a short report for this upgrade step that should be suitable for the upgrade screen.
     *
     * @param delimiter The delimiter to use between lines.
     * @return The short upgrade report.
     */
    public String getShortReport(String delimiter);

    /**
     * Generates a detailed upgrade report suitable for reviewing changes.
     *
     * @param delimiter The delimiter to use between lines.
     * @return The detailed upgrade report.
     */
    public String getDetailedReport(String delimiter);
}
