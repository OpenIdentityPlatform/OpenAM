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
define("org/forgerock/openam/ui/admin/delegates/SMSDelegate", [
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants"
], function(AbstractDelegate, Constants) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/");

    obj.getRealmAuthentication = function(username, policyId, permissions) {
      var deferred = new $.Deferred(),
          url = "realm-config/authentication",
          schemaPromise = obj.serviceCall({
              url: url + "?_action=template",
              type: "POST"
          }).done(obj.sanitize),
          valuesPromise = obj.serviceCall({
              url: url
          });

      $.when(schemaPromise, valuesPromise).done(function(schemaData, valuesData) {
          deferred.resolve({
              schema: obj.sanitizeSchema(schemaData[0]._schema),
              values: valuesData[0]
          });
      });

      return deferred;
    };

    obj.sanitizeSchema = function(schema) {
        // Filter out 'defaults'
        schema.properties = _.omit(schema.properties, 'defaults');

        // Translate order into propertyOrder
        _.forEach(schema.properties, function(property) {
            if(property.hasOwnProperty("order")) {
                console.error('Property still using "order" and not "propertyOrder"');
                property.propertyOrder = parseInt(property.order.slice(1), 10);
            }
            delete property.order;
        });

        // Create ordered array
        schema.orderedProperties = _.sortBy(_.map(schema.properties, function(value, key) {
            value._id = key;
            return value;
        }), 'propertyOrder');

        return schema;
    };

    return obj;
});
