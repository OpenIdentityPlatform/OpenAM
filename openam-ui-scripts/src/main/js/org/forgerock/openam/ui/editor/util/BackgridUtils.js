/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

/*global define Backbone, _, $*/

define("org/forgerock/openam/ui/editor/util/BackgridUtils", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/Router"
], function (Backgrid, Router) {
    var obj = {};

    // todo: candidate for commons, have not changed it, using UMA version
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

        gotoUrl: function (e) {
            e.preventDefault();
            var href = $(e.currentTarget).data('href');
            Router.navigate(href, {trigger: true});
        }
    });

    // todo: candidate for commons, placeholder is the only difference with UMA
    obj.FilterHeaderCell = Backgrid.HeaderCell.extend({
        className: 'filter-header-cell', // todo
        render: function () {
            var filter = new Backgrid.Extension.ServerSideFilter({
                name: this.column.get("name"),
                placeholder: $.t('scripts.grid.filterBy', { value: this.column.get("label") }),
                collection: this.collection
            });
            this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
            this.collection.state.filters.push(filter);
            obj.FilterHeaderCell.__super__.render.apply(this);
            this.$el.append(filter.render().el);
            return this;
        }
    });

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

    // todo: candidate for commons, have not changed it, using UMA version
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

    // todo not working properly yet
    obj.sync = function (method, model, options) {
        var params = [],
            excludeList = ['page', 'total_pages', 'total_entries', 'order', 'per_page', 'sort_by'];

        _.forIn(options.data, function (val, key) {
            if (!_.include(excludeList, key)) {
                params.push(key + '=' + val);
            }
        });

        options.data = params.join('&');
        options.beforeSend = function (xhr) {
            xhr.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=1.0');
        };
        return Backbone.sync(method, model, options);
    };

    // todo: candidate for commons, have not changed it, using UMA version
    obj.parseRecords = function (data, options) {
        return data.result;
    };

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.sortKeys = function () {
        return this.state.order === 1 ? '-' + this.state.sortKey : this.state.sortKey;
    };

    // TODO: candidate for commons, have not changed it, using UMA version
    // FIXME: Workaround to fix "Double sort indicators" issue
    // @see https://github.com/wyuenho/backgrid/issues/453
    obj.doubleSortFix = function (model) {
        // No ids so identify model with CID
        var cid = model.cid,
            filtered = model.collection.filter(function (model) {
                return model.cid !== cid;
            });

        _.each(filtered, function (model) {
            model.set('direction', null);
        });
    };

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.parseState = function (resp, queryParams, state, options) {
        if (!this.state.totalRecords) {
            this.state.totalRecords = resp.remainingPagedResults + resp.resultCount;
        }
        if (!this.state.totalPages) {
            this.state.totalPages = Math.ceil(this.state.totalRecords / this.state.pageSize);
        }
        return this.state;
    };

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.pagedResultsOffset = function () {
        return (this.state.currentPage - 1) * this.state.pageSize;
    };

    // TODO: candidate for commons, have not changed it, using UMA version
    obj.getQueryParams = function (data) {
        var params = {
            _sortKeys: this.sortKeys,
            _queryFilter: this.queryFilter,
            pageSize: "_pageSize",
            _pagedResultsOffset: this.pagedResultsOffset
        };

        if (data && typeof data === 'object') {
            _.extend(params, data);
        }
        return params;
    };

    obj.getState = function (data) {
        var state = {
            pageSize: 10,
            sortKey: "name"
        };

        if (data && typeof data === 'object') {
            _.extend(state, data);
        }
        return state;
    };

    return obj;
});
