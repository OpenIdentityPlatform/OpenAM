/**
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

/*global define Backgrid, Backbone, _, $*/

define("org/forgerock/openam/ui/policy/util/BackgridUtils", [
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "backgrid"
], function (UIUtils, Router, Backgrid) {
    var obj = {};

    // TODO: the difference between this implementation and the one used for UMA is that here the cell is not clickable
    obj.UriExtCell = Backgrid.UriCell.extend({
        render: function () {
            this.$el.empty();
            var rawValue = this.model.get(this.column.get("name")),
                formattedValue = this.formatter.fromRaw(rawValue, this.model),
                href = _.isFunction(this.column.get("href")) ? this.column.get('href')(rawValue, formattedValue, this.model) : this.column.get('href');

            this.$el.append($("<a>", {
                href: href || rawValue,
                title: this.title || formattedValue
            }).text(formattedValue));

            if (href) {
                this.$el.data('href', href);
                this.$el.prop('title', this.title || formattedValue);
            }

            this.delegateEvents();
            return this;
        }
    });

    /**
     * Handlebars Template Cell Renderer
     * <p>
     * You must extend this renderer and specify a "template" attribute e.g.
     * <p>
     * MyCell = backgridUtils.TemplateCell.extend({
     *     template: "templates/MyTemplate.html"
     * });
     */
     // TODO: difference from UMA: using fillTemplateWithData() instead of render() to pass data to the template
    obj.TemplateCell = Backgrid.Cell.extend({
        className: 'template-cell',
        render: function () {
            this.$el.html(UIUtils.fillTemplateWithData(this.template, this.model.attributes));
            this.delegateEvents();

            return this;
        }
    });

    // TODO: candidate for commons
    obj.ObjectCell = Backgrid.Cell.extend({
        className: 'object-formatter-cell',

        render: function () {
            this.$el.empty();

            var object = this.model.get(this.column.attributes.name),
                result = '<div class="multiple-columns"><dl class="dl-horizontal">',
                prop;

            for (prop in object) {
                if (_.isString(object[prop])) {
                    result += '<dt>' + prop + '</dt><dd>' + object[prop] + '</dd>';
                } else {
                    result += '<dt>' + prop + '</dt><dd>' + JSON.stringify(object[prop]) + '</dd>';
                }
            }
            result += '</dl></div>';

            this.$el.append(result);

            this.delegateEvents();
            return this;
        }
    });

    // TODO: candidate for commons
    obj.ArrayCell = Backgrid.Cell.extend({
        className: 'array-formatter-cell',

        render: function () {
            this.$el.empty();

            var arrayVal = this.model.get(this.column.attributes.name),
                result = '<ul>',
                i = 0;

            for (; i < arrayVal.length; i++) {
                if (_.isString(arrayVal[i])){
                    result += '<li>' + arrayVal[i] + '</li>';
                } else{
                    result += '<li>' + JSON.stringify(arrayVal[i]) + '</li>';
                }
            }
            result += '</ul>';

            this.$el.append(result);

            this.delegateEvents();
            return this;
        }
    });

    // TODO: candidate for commons, placeholder is the only difference with UMA
    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: 'filter-header-cell', // todo
        render: function () {
            var filter = new Backgrid.Extension.ServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t('policy.applications.list.filterBy', { value: this.column.get("label") }),
                collection: this.collection
            });
            this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
            this.collection.state.filters.push(filter);
            obj.FilterHeaderCell.__super__.render.apply(this);
            this.$el.append(filter.render().el);
            return this;
        }
    });

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.queryFilter = function () {
        var params = [];
        _.each(this.state.filters, function (filter) {
            if (filter.query() !== '') {
                // todo: No server side support for 'co' ATM, this is effectively an 'eq'
                params.push(filter.name + '+co+' + encodeURIComponent('"' + filter.query() + '"'));
            }
        });
        return params.length === 0 || params.join('+AND+');
    };

    // TODO: not working properly yet
    obj.sync = function (method, model, options) {
        var params = [],
            excludeList = ['page', 'total_pages', 'total_entries', 'order', 'per_page', 'sort_by'];

        _.forIn(options.data, function (val, key) {
            if (!_.include(excludeList, key)) {
                params.push(key + '=' + val);
            }
        });

        options.data = params.join('&');

        return Backbone.sync(method, model, options);
    };

    /**
     * Clickable Row
     * <p>
     * You must extend this row and specify a "callback" attribute e.g.
     * <p>
     * MyRow = BackgridUtils.ClickableRow.extend({
     *     callback: myCallback
     * });
     */
    //TODO: commons candidate
    obj.ClickableRow = Backgrid.Row.extend({
        events: {
            "click": "onClick"
        },

        onClick: function (e) {
            if (this.callback) {
                this.callback(e);
            }
        }
    });

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.parseRecords = function (data, options) {
        return data.result;
    };

    return obj;
});