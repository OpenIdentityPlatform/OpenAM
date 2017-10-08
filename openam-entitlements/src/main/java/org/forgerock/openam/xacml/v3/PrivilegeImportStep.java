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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.xacml.v3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.IPrivilegeManager;

/**
 * Describes how Privilege read from XACML will be imported into OpenAM.
 *
 * @since 13.5.0
 */
class PrivilegeImportStep<T extends IPrivilege> implements PersistableImportStep {

    public static final String TYPE = "Privilege";

    private final IPrivilegeManager<T> privilegeManager;
    private final DiffStatus diffStatus;
    private final T privilege;

    /**
     * Constructs an instance of PrivilegeImportStep.
     *
     * @param manager
     *         the privilege manager.
     * @param diffStatus
     *         status of the import step.
     * @param privilege
     *         the instance being imported.
     */
    PrivilegeImportStep(IPrivilegeManager<T> manager, DiffStatus diffStatus, T privilege) {
        this.privilegeManager = manager;
        this.diffStatus = diffStatus;
        this.privilege = privilege;
    }

    @Override
    public DiffStatus getDiffStatus() {
        return diffStatus;
    }

    @Override
    public String getName() {
        return privilege.getName();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void apply() throws EntitlementException {
        switch (diffStatus) {
        case ADD:
            privilegeManager.add(privilege);
            break;
        case UPDATE:
            privilegeManager.modify(privilege);
            break;
        }
    }

    @Override
    public Object get() {
        return privilege;
    }

}
