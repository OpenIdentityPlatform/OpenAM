/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define Backgrid Backbone _ $*/

define("org/forgerock/openam/ui/uma/util/BackgridUtils", [
  "moment",
  "org/forgerock/commons/ui/common/util/UIUtils"
], function (moment, uiUtils) {
    /**
     * @exports org/forgerock/openam/ui/uma/util/BackgridUtils
     */
    var obj = {};

    /**
     * Datetime Ago Cell Renderer
     * <p>
     * Displays human friendly date time text (e.g. 4 "hours ago") with a tooltip of the exact time
     */
    obj.DatetimeAgoCell = Backgrid.Cell.extend({
        className: 'date-time-ago-cell',
        formatter: {
            fromRaw: function(rawData, model) {
                return moment(rawData).fromNow();
            }
        },
        render: function() {
            obj.DatetimeAgoCell.__super__.render.apply(this);
            this.$el.attr('title', moment(this.model.get(this.column.get('name'))).format('Do MMMM YYYY, h:mm:ssa'));
            return this;
        }
    });

    obj.UnversalIdToUsername = Backgrid.Cell.extend({
        formatter: {
            fromRaw: function(rawData, model) {
                return rawData.substring(3,rawData.indexOf(',ou=user'));
            }
        },
        render: function() {
            obj.UnversalIdToUsername.__super__.render.apply(this);
            this.$el.attr('title', this.model.get(this.column.get('name')));
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
    obj.TemplateCell = Backgrid.Cell.extend({
        className: 'template-cell',
        render: function () {
            uiUtils.renderTemplate(this.template, this.$el);
            this.delegateEvents();

            return this;
        }
    });

    obj.UriExtCell = Backgrid.UriCell.extend({
        render: function () {
            this.$el.empty();
            var rawValue = this.model.get(this.column.get("name")),
            formattedValue = this.formatter.fromRaw(rawValue, this.model),
            href = _.isFunction(this.column.get("href")) ? this.column.get('href')(rawValue, formattedValue, this.model) : this.column.get('href');

            this.$el.append($("<a>", {
                tabIndex: -1,
                href: href || rawValue,
                title: this.title || formattedValue
            }).text(formattedValue));

            if (this.column.get("model")) {
                this.$el.data(this.model.attributes);
            }

            this.delegateEvents();
            return this;
        }
    });

    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: 'filter-header-cell',
        render: function() {
            var filter = new Backgrid.Extension.ServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t('policy.uma.resources.all.grid.filter', { header: this.column.get("label") }),
                collection: this.collection
            });
            this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
            this.collection.state.filters.push(filter);
            obj.FilterHeaderCell.__super__.render.apply(this);
            this.$el.append(filter.render().el);
            return this;
        }
    });

    obj.queryFilter = function () {
        var params = [];
        _.each(this.state.filters, function(filter){
            if (filter.query() !== '') {
                params.push( filter.name + '+eq+' + encodeURIComponent('"*' + filter.query() + '*"') );
            }
        });
        return params.length === 0 ? true : params.join('+AND+');
    };

    obj.parseState = function (resp, queryParams, state, options) {
        if (!this.state.totalRecords) { this.state.totalRecords = resp.remainingPagedResults + resp.resultCount; }
        if (!this.state.totalPages)   { this.state.totalPages = Math.ceil(this.state.totalRecords/this.state.pageSize); }
        return this.state;
    };

    obj.pagedResultsOffset = function () {
        return (this.state.currentPage - 1) * this.state.pageSize;
    };

    obj.sync = function(method, model, options){
        var params = [];

        _.forIn(options.data, function(val, key){
            if(!_.include(['page', 'total_pages', 'total_entries', 'order', 'per_page'], key)) {
                params.push(key + '=' + val);
            }
        });

        options.data = params.join('&');
        options.processData = false;
        options.beforeSend = function(xhr){
            xhr.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=1.0');
        };
        return Backbone.sync(method, model, options);
    };

    obj.parseRecords = function (data, options) {
        return data.result;
    };

    return obj;
});
