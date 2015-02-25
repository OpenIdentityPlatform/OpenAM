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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.publish;

/**
 * Interface defining functionality to publish an STS instance. Necessary parameters are not passed in interface
 * definition, as the implementation of this interface is injected by Guice, which injects the state necessary to
 * publish a STS instance.
 * Going forward, as the STS-instance-publish workflow is formalized, it may make sense to refactor this interface
 * into a more transparent one, in which all state necessary to perform functionality defined in the interface are
 * actually specified as parameters to the methods defined in the interface. Currently, it is easier to let
 * Guice inject the wired-up instance, but that is not optimal from the perspective of 'interface transparency.' TODO
 */
public interface STSInstancePublisher {
    public void publishSTSInstance();
}
