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
define("org/forgerock/openam/ui/admin/views/console/realms/authentication/ComponentBuilder", [
], function() {
    var object = {
        create: function(type, options) {
          var method ,component;
          if(_.isObject(type)) {
              method = this.createEnum;
          } else {
              method = this[$.camelCase("create-" + type)];
          }

          if(method) {
              component = method.call(null, options);
          } else {
              console.warn("Unable to build component type of " + type);
          }
          return component;
        },
        createArray: function(options) {
            var div = $("<div class='form-group'/>")
            .append($("<label for='" + $.camelCase(options._id) + "'>" + options.title + "</label>")),
            input = $("<input type='text' id=" + options._id + " value='" + options._initial.join(",") + "'>");
            div.append(input);

            input.selectize({
                delimiter: ',',
                persist: false,
                create: function(input) {
                    if(options.items.type === 'number') {
                        var parsedInt = parseInt(input, 10);
                        if(_.isNumber(parsedInt) && !_.isNaN(parsedInt)) {
                            return {
                                value: parsedInt,
                                text: parsedInt
                            };
                        }
                    } else if(options.items.type === 'string') {
                        if(!_.isEmpty(input)) {
                            return {
                                value: input,
                                text: input
                            };
                        }
                    } else {
                        return false;
                    }
                }
            });

            return div;
        },
        createBoolean: function(options) {
            var div = $("<div class='checkbox'/>"),
            label = $("<label/>"),
            input = $("<input type='checkbox'>");

            if(options._initial) {
                input.attr('checked', true);
            }

            div.append(label);
            label.append(input);
            label.append(options.title);

            return div;
        },
        createEnum: function(options) {
            // TODO: Implement once there is a live example
            throw 'Not Implemented';
        },
        createNumber: function(options) {
            return $("<div class='form-group'/>")
            .append($("<label for='" + $.camelCase(options._id) + "'>" + options.title + "</label>"))
            .append($("<input type='number' class='form-control' id=" + options._id + " placeholder='Number' value='" + options._initial + "'>"));
        },
        createString: function(options) {
            return $("<div class='form-group'/>")
            .append($("<label for='" + $.camelCase(options._id) + "'>" + options.title + "</label>"))
            .append($("<input type='text' class='form-control' id=" + options._id + " placeholder='Text' value='" + options._initial + "'>"));
        }
    };

    return object;
});
