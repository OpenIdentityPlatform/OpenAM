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
 * Copyright 2014-2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

import com.sun.identity.console.base.model.AMConsoleException;

import java.util.Map;
import java.util.Set;

/**
 * Defines the model functionality necessary to support the Rest/Soap Add/Edit ViewBean classes.
 */
public interface STSInstanceModel extends STSHomeViewBeanModel {
    /**
     * Publishes a rest sts instance by POSTing to the rest-sts publish endpoint.
     * @param stsType is the request for soap or rest instances
     * @param configurationState the configuration state defining the published instance
     * @param realm the realm in which the instance should be published. Necessary to add the realm state
     *              to the configuration state, as the end-user should not be able to set this directly (i.e. it is
     *              implicit in the ViewBean display)
     * @return A RestSTSModelResonse instance, which encapsulates success or failure, and a optional success/failure message
     * @throws AMConsoleException If an IOException occurs in making the POST
     */
    STSInstanceModelResponse createInstance(STSType stsType, Map<String, Set<String>> configurationState, String realm) throws AMConsoleException;

    /**
     * Updates a rest sts instance by PUTing to the rest-sts publish endpoint.
     * @param stsType is the request for soap or rest instances
     * @param configurationState the configuration state defining the updated instance
     * @param realm the realm in which the instance should be published. Necessary to add the realm state
     *              to the configuration state, as the end-user should not be able to set this directly (i.e. it is
     *              implicit in the ViewBean display)
     * @param instanceName The name of the instance, as set by the ViewBean. Used to constitute the PUT url.
     * @return A RestSTSModelResonse instance, which encapsulates success or failure, and optional success/failure messages
     * @throws AMConsoleException If an IOException occurs in making the PUT
     */
    STSInstanceModelResponse updateInstance(STSType stsType, Map<String, Set<String>> configurationState, String realm, String instanceName)
            throws AMConsoleException;

    /**
     * Called by the RestSTSEditViewBean to obtain the instance state necessary to edit an existing rest sts instance.
     * @param stsType is the request for soap or rest instances
     * @param realm the realm to which the instance has been published
     * @param instanceName the instance name
     * @return The state corresponding to the rest sts instance which is used to constitute the property-sheet displayed
     * by the RestSTSEditViewBean
     * @throws AMConsoleException If the SMS cannot be consulted.
     */
    Map<String, Set<String>> getInstanceState(STSType stsType, String realm, String instanceName) throws AMConsoleException;

    /**
     * Called by the ViewBean context prior to updating or publishing a rest sts instance.
     * @param stsType is the request for soap or rest instances
     * @param configurationState the to-be-validated configuration state
     * @return A STSInstanceModelResponse indicating validation success or failure. If validation was not successful, the
     * message will indicate the validation error.
     */
    STSInstanceModelResponse validateConfigurationState(STSType stsType, Map<String, Set<String>> configurationState);
}
