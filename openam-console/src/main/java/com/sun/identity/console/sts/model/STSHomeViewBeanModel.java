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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;

import java.util.Set;

/**
 * The model interface definition supporting the STSHomeViewBean class. Provides functionality to populate the tables
 * of published rest and soap instances, and to delete instances out of those tables.
 */
public interface STSHomeViewBeanModel extends AMModel {
    enum STSType {
        REST, SOAP;

        public boolean isRestSTS() {
            return this == REST;
        }
    }

    /**
     * Returns the set of rest sts instances in the SMS under the given realm.
     * @param stsType is the request for soap or rest instances
     * @param realm the realm to consult for the rest sts instances.
     * @return The names of rest sts instances in the SMS
     * @throws AMConsoleException if the SMS cannot be successfully consulted
     */
    Set<String> getPublishedInstances(STSType stsType, String realm) throws AMConsoleException;

    /**
     * Performs DELETEs against the rest-sts publish endpoint to remove the published instances
     * @param stsType is the request for soap or rest instances
     * @param instanceNames the set of instances to be deleted
     * @throws AMConsoleException If an IOException occurs in making the DELETE
     */
    void deleteInstances(STSType stsType, Set<String> instanceNames) throws AMConsoleException;
}
