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

import $ from "jquery";
import _ from "lodash";

import {
    remove as removeOAth,
    getAll as getAllOAth
} from "org/forgerock/openam/ui/user/dashboard/services/DeviceManagementService";
import {
    remove as removePush,
    getAll as getAllPush
} from "org/forgerock/openam/ui/user/dashboard/services/PushDeviceService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import DeviceDetailsDialog from "org/forgerock/openam/ui/user/dashboard/views/DeviceDetailsDialog";
import DevicesSettingsDialog from "org/forgerock/openam/ui/user/dashboard/views/DevicesSettingsDialog";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const getAttributeFromElement = (element, attribute) => $(element).closest(`div[${attribute}]`).attr(attribute);
const getUUIDFromElement = (element) => getAttributeFromElement(element, "data-device-uuid");
const getTypeFromElement = (element) => getAttributeFromElement(element, "data-device-type");
const handleReject = (response) => {
    Messages.addMessage({
        type: Messages.TYPE_DANGER,
        response
    });
};

class DeviceManagementView extends AbstractView {
    constructor () {
        super();
        this.template = "templates/user/dashboard/AuthenticationDevicesTemplate.html";
        this.noBaseTemplate = true;
        this.element = "#authenticationDevices";
        this.events = {
            "click [data-delete]":  "handleDelete",
            "click [data-recovery-codes]": "handleShowDeviceDetails",
            "click [data-details]": "handleShowDeviceDetails",
            "click [data-settings]" : "showDevicesSettings"
        };
    }
    handleDelete (event) {
        event.preventDefault();

        const uuid = getUUIDFromElement(event.currentTarget);
        const type = getTypeFromElement(event.currentTarget);
        const deleteFunc = type === "oath" ? removeOAth : removePush;

        deleteFunc(uuid).then(() => {
            this.render();
        }, handleReject);
    }
    handleShowDeviceDetails (event) {
        event.preventDefault();

        const uuid = getUUIDFromElement(event.currentTarget);
        const device = _.find(this.data.devices, { uuid });

        DeviceDetailsDialog(uuid, device);
    }
    showDevicesSettings (event) {
        event.preventDefault();

        DevicesSettingsDialog();
    }
    render () {
        Promise.all([getAllOAth(), getAllPush()]).then((value) => {
            const oathDevices = _.map(value[0], _.partial(_.merge, { type: "oath", icon: "clock-o" }));
            const pushDevices = _.map(value[1], _.partial(_.merge, { type: "push", icon: "bell-o" }));

            this.data.devices = [...oathDevices, ...pushDevices];

            this.parentRender();
        }, handleReject);
    }
}

export default new DeviceManagementView();
