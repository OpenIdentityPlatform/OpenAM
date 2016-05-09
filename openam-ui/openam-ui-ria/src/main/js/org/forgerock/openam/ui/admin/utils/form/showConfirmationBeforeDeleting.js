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

 /**
  * @module org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeDeleting
  */
 define([
     "jquery",
     "lodash",
     "org/forgerock/commons/ui/common/components/Messages",
     "org/forgerock/commons/ui/common/components/BootstrapDialog"
 ], ($, _, Messages, BootstrapDialog) =>

     /**
      * Shows a confirm dialog before deleting and call delete callback if needed.
      * @param  {object} msg The object to define warning text
      * @param  {string} msg.type The parameter for the default confirm message ("Are you sure that you want to
      *                           delete this _type_?")
      * @param  {string} msg.message The text which overrides default confirm message
      * @param  {function} deleteCallback The callback to remove the edited entity
      * @example
      * clickHandler: function (event) {
      *   event.preventDefault();
      *   showConfirmationBeforeDeleting({type: "console.scripts.edit.script"}, deleteEntity);
      * }
      */
     (msg, deleteCallback) => {
         _.defaults(msg, { message: $.t("console.common.confirmDeleteText", { type: $.t(msg.type) }) });
         BootstrapDialog.confirm({
             type: BootstrapDialog.TYPE_DANGER,
             cssClass: "delete-confirmation",
             title: $.t("console.common.confirmDelete"),
             message: msg.message,
             btnOKLabel: $.t("common.form.delete"),
             btnOKClass: "btn-danger",
             callback: (result) => {
                 if (result && deleteCallback) {
                     deleteCallback();
                 }
             }
         });
     }
);
