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

import { t } from "i18next";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";

/**
 * Shows a confirmation dialog before performing a dangerous action and calls action callback if needed.
 * @module org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction
 * @param  {object} msg Message object
 * @param  {string} [msg.type] Type of object on which action is performed
 * @param  {string} [msg.message] Confirmation message to show
 * @param  {function} actionCallback Action callback
 * @param  {string} [actionName] Name of the performed action
 * @example
 * clickHandler: function (event) {
 *   event.preventDefault();
 *   showConfirmationBeforeAction({type: "console.scripts.edit.script"}, deleteEntity);
 * }
 */
export default function showConfirmationBeforeAction (msg, actionCallback, actionName = t("common.form.delete")) {
    BootstrapDialog.confirm({
        type: BootstrapDialog.TYPE_DANGER,
        title: `${t("common.form.confirm")} ${actionName}`,
        message: msg.message ? msg.message : t("console.common.confirmDeleteText", { type: msg.type }),
        btnOKLabel: actionName,
        btnOKClass: "btn-danger",
        callback: (result) => {
            if (result && actionCallback) {
                actionCallback();
            }
        }
    });
}
