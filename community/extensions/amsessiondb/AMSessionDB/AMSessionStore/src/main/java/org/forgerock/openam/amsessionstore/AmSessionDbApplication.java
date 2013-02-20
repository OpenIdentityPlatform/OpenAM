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
package org.forgerock.openam.amsessionstore;

import org.forgerock.openam.amsessionstore.impl.ConfigResourceImpl;
import org.forgerock.openam.amsessionstore.impl.DBDumpResourceImpl;
import org.forgerock.openam.amsessionstore.impl.DeleteByDateResourceImpl;
import org.forgerock.openam.amsessionstore.impl.DeleteResourceImpl;
import org.forgerock.openam.amsessionstore.impl.GetRecordCountResourceImpl;
import org.forgerock.openam.amsessionstore.impl.ReadResourceImpl;
import org.forgerock.openam.amsessionstore.impl.ReadWithSecKeyResourceImpl;
import org.forgerock.openam.amsessionstore.impl.ReplicationResourceImpl;
import org.forgerock.openam.amsessionstore.impl.ShutdownResourceImpl;
import org.forgerock.openam.amsessionstore.impl.StatsResourceImpl;
import org.forgerock.openam.amsessionstore.impl.WriteResourceImpl;
import org.forgerock.openam.amsessionstore.resources.ConfigResource;
import org.forgerock.openam.amsessionstore.resources.DBDumpResource;
import org.forgerock.openam.amsessionstore.resources.DeleteByDateResource;
import org.forgerock.openam.amsessionstore.resources.DeleteResource;
import org.forgerock.openam.amsessionstore.resources.GetRecordCountResource;
import org.forgerock.openam.amsessionstore.resources.ReadResource;
import org.forgerock.openam.amsessionstore.resources.ReadWithSecKeyResource;
import org.forgerock.openam.amsessionstore.resources.ReplicationResource;
import org.forgerock.openam.amsessionstore.resources.ShutdownResource;
import org.forgerock.openam.amsessionstore.resources.StatsResource;
import org.forgerock.openam.amsessionstore.resources.WriteResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Adds the REST resources into the RESTlet router.
 * 
 * @author steve
 */
public class AmSessionDbApplication extends Application {
    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        // Create a router Restlet that routes each call to a new instance of HelloWorldResource.
        Router router = new Router(getContext());

        // Define the routes
        router.attach(ReadResource.URI + ReadResource.PKEY, ReadResourceImpl.class);
        router.attach(WriteResource.URI, WriteResourceImpl.class);
        router.attach(DeleteByDateResource.URI + DeleteByDateResource.DATE, DeleteByDateResourceImpl.class);
        router.attach(DeleteResource.URI + DeleteResource.PKEY, DeleteResourceImpl.class);
        router.attach(ShutdownResource.URI, ShutdownResourceImpl.class);
        router.attach(ReadWithSecKeyResource.URI + ReadWithSecKeyResource.UUID, ReadWithSecKeyResourceImpl.class);
        router.attach(GetRecordCountResource.URI + GetRecordCountResource.UUID, GetRecordCountResourceImpl.class);
        router.attach(StatsResource.URI, StatsResourceImpl.class);
        router.attach(ConfigResource.URI, ConfigResourceImpl.class);
        router.attach(ReplicationResource.URI, ReplicationResourceImpl.class);
        // TODO remove as this is debugging code
        router.attach(DBDumpResource.URI, DBDumpResourceImpl.class);

        return router;
    }
}
