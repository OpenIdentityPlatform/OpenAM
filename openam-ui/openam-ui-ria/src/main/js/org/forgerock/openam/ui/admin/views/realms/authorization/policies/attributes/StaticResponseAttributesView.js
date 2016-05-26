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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "lodash",
    "backbone",
    "org/forgerock/openam/ui/common/components/table/InlineEditTable"
], (_, Backbone, InlineEditTable) => {

    const StaticResponseAttributesView = Backbone.View.extend({

        initialize ({ staticAttributes }) {
            this.staticAttributes = staticAttributes;
        },

        render () {
            const getFlattenedStaticAttributes = () => _.flatten(
                _.map(this.staticAttributes, (attribute) =>
                    _.map(attribute.propertyValues, (value) => ({ key: attribute.propertyName, value }))
                ));

            this.inlineEditList = new InlineEditTable({
                values: getFlattenedStaticAttributes()
            });
            this.$el.append(this.inlineEditList.render().$el);

            return this;
        },

        getGroupedData () {
            return _(this.inlineEditList.getData())
                .groupBy("key")
                .map((values, key) => ({
                    type: "Static",
                    propertyName: key,
                    propertyValues: _.map(values, "value")
                }))
                .value();
        }
    });

    return StaticResponseAttributesView;
});
