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
package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;
import org.forgerock.openam.entitlement.indextree.events.ModificationEventType;
import org.forgerock.openam.entitlement.indextree.events.ErrorEventType;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.controls.EntryChangeNotificationResponseControl;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;

import javax.inject.Inject;

/**
 * Implementation of the {@link SearchResultHandler} and is responsible for handling changes to path index attributes
 * under service entitlements. All changes and potential errors result in an event which is passed to an observable to
 * notify all its observers.
 *
 * @author andrew.forrest@forgerock.com
 */
public class IndexChangeHandler implements SearchResultHandler {

    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private static final String SERVICE_DECLARATION = "ou=sunEntitlementIndexes,ou=services,";
    private static final String INDEX_PATH_ATT = "pathindex";

    private final IndexChangeObservable observable;
    private final DNWrapper dnMapper;

    @Inject
    public IndexChangeHandler(IndexChangeObservable observable, DNWrapper dnMapper) {
        this.observable = observable;
        this.dnMapper = dnMapper;
    }

    @Override
    public boolean handleEntry(SearchResultEntry entry) {
        EntryChangeNotificationResponseControl control = null;

        try {
            // Retrieve details of the policy change.
            control = entry.getControl(
                    EntryChangeNotificationResponseControl.DECODER, new DecodeOptions());

        } catch (DecodeException dE) {
            DEBUG.error("Error occurred attempting to read policy rule change.", dE);
            // Notify observers of the exception and proceed no further.
            observable.notifyObservers(ErrorEventType.SEARCH_FAILURE.createEvent());
            return true;
        }

        // Extract the realm from the DN to be passed as part of the event.
        String dn = entry.getName().toString();
        String orgName = dn.substring(dn.indexOf(SERVICE_DECLARATION) + SERVICE_DECLARATION.length());
        String realm = dnMapper.orgNameToRealmName(orgName);

        // Retrieve all sunxmlKeyValue attributes.
        Attribute attributes = entry.getAttribute(AttributeDescription.valueOf("sunxmlKeyValue"));

        for (ByteString attrValue : attributes) {
            String attStrValue = attrValue.toString();

            if (attStrValue.startsWith(INDEX_PATH_ATT)) {

                // Extract the path index out of the attribute value.
                String pathIndex = attStrValue.substring(INDEX_PATH_ATT.length() + 1);

                switch (control.getChangeType()) {
                    case MODIFY:
                        // When a rule modification is made in OpenAM, a delete/add approach is taken. If a
                        // rule is updated directly within directory, then a modify notification will be
                        // broadcast which only contains the new value. Therefore, the best approach that can
                        // be taken is to send an update notification, to ensure the index is picked up. However,
                        // this will result in the old index remaining.
                    case ADD:
                        observable.notifyObservers(ModificationEventType.ADD.createEvent(pathIndex, realm));
                        break;
                    case DELETE:
                        observable.notifyObservers(ModificationEventType.DELETE.createEvent(pathIndex, realm));
                        break;
                }

                // Required attribute has been handled.
                break;
            }
        }

        return true;
    }

    @Override
    public void handleErrorResult(ErrorResultException erE) {
        DEBUG.error("Index change persistence search has failed.", erE);
        // Notify all observers of the error.
        observable.notifyObservers(ErrorEventType.SEARCH_FAILURE.createEvent());
    }

    @Override
    public boolean handleReference(SearchResultReference searchResultReference) {
        // Not interested in this scenario.
        return true;
    }

    @Override
    public void handleResult(Result result) {
        // Not interested in this scenario.
    }

}
