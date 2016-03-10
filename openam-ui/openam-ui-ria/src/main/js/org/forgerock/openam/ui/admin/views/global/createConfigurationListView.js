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
  * @module org/forgerock/openam/ui/admin/views/global/createConfigurationListView
  */
 define("org/forgerock/openam/ui/admin/views/global/createConfigurationListView", [
     "jquery",
     "org/forgerock/commons/ui/common/main/AbstractView"
 ], function ($, AbstractView) {

     /**
      * Returns a list view
      * @param   {string} title Title to display
      * @param   {function} getItems a function that returns items
      * @param   {string} location the location of list items
      * @returns {function} createConfigurationListView a function that creates a view
      */
     var createConfigurationListView = function (title, getItems) {
         return AbstractView.extend({
             template: "templates/admin/views/global/ConfigurationListTemplate.html",
             render: function () {
                 var self = this;
                 self.data.title = title;
                 getItems().then(function (items) {
                     self.data.items = items;
                     self.parentRender();
                 }, function (reason) {
                     console.error(reason);
                     // display reason
                     // TODO data load failed, click to retry
                     self.parentRender();
                 });
             }
         });
     };

     return createConfigurationListView;
 });
