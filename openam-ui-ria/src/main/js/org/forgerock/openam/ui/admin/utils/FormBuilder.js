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
 * Copyright 2015 ForgeRock AS.
 */

/*global $ _ define*/
define("org/forgerock/openam/ui/admin/utils/FormBuilder", [
    "jsonEditor"
], function(JSONEditor) {
    var obj = {};

    obj.build = function(element, schema, values) {
      var editor = new JSONEditor(element, {
          disable_collapse: true,
          disable_edit_json: true,
          disable_properties: true,
          iconlib: "fontawesome4",
          schema: schema,
          theme: 'bootstrap3'
      });
      editor.setValue(_.pick(values, _.keys(schema.properties)));

      element = $(element);

      element.find('div[data-schematype="array"] p').addClass('help-block');

      element.find('.help-block').addClass('help-block-collapsed');
      element.find('.help-block').click(function(event) {
          if(event.target === this) {
              $(event.target).toggleClass('help-block-collapsed help-block-expanded');
          }
      });

      return editor;
    };

    return obj;
});
