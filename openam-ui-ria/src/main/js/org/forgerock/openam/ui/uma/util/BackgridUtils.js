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

/*global define Backgrid, Backbone, _, $*/

define("org/forgerock/openam/ui/uma/util/BackgridUtils", [
    "backgrid",
    "moment",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (Backgrid, moment, Router, UIUtils) {
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

    obj.UniversalIdToUsername = Backgrid.Cell.extend({
        formatter: {
            fromRaw: function(rawData, model) {
                return rawData.substring(3,rawData.indexOf(',ou=user'));
            }
        },
        render: function() {
            obj.UniversalIdToUsername.__super__.render.apply(this);
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
            UIUtils.renderTemplate(this.template, this.$el);
            this.delegateEvents();

            return this;
        }
    });

    obj.UriExtCell = Backgrid.UriCell.extend({
        events: {
            'click': 'gotoUrl'
        },
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
        },

        gotoUrl: function(e){
            e.preventDefault();
            var href = $(e.currentTarget).data('href');
            Router.navigate( href, {trigger: true});
        }

    });

    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: 'filter-header-cell',
        render: function() {
            var filter = new Backgrid.Extension.ServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t('uma.resources.all.grid.filter', { header: this.column.get("label") }),
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
                // FIXME: No server side support for 'co' ATM, this is effectively an 'eq'
                params.push( filter.name + '+co+' + encodeURIComponent('"' +filter.query() + '"') );
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

    obj.sortKeys = function() {
        return this.state.order === 1 ? '-' + this.state.sortKey : this.state.sortKey;
    };

    obj.sync = function(method, model, options){
        var params = [],
            exculdeList = ['page', 'total_pages', 'total_entries', 'order', 'per_page', 'sort_by'];

        // TODO: Workaround as resourceset endpoind requires a _queryId=* to indicate a blank query,
        // while the historyAudit endpint requires a _queryFilter=true to indicate a blank query.
        if (options.data._queryId === '*' && options.data._queryFilter === true){
            exculdeList.push('_queryFilter');
        } else {
            exculdeList.push('_queryId');
        }

        _.forIn(options.data, function(val, key){
            if(!_.include(exculdeList, key)) {
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

    obj.getQueryParams = function (data) {
        var params = {
            pageSize: "_pageSize",
            sortKey: "_sortKeys",
            _queryFilter: this.queryFilter,
            _pagedResultsOffset:  this.pagedResultsOffset
        };

        if (data && typeof data === 'object') {
            _.extend(params,data);
        }
        return params;
    };

    // FIXME: Workaround to fix "Double sort indicators" issue
    // @see https://github.com/wyuenho/backgrid/issues/453
    obj.doubleSortFix = function(model) {
        // No ids so identify model with CID
        var cid = model.cid,
            filtered = model.collection.filter(function(model) {
                return model.cid !== cid;
            });

        _.each(filtered, function(model) {
            model.set('direction', null);
        });
    };

    return obj;
});
