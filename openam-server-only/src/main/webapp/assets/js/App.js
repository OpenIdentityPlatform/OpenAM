if ( typeof APP == "undefined" ) {
    var APP = {
        lang: {},
        util: {}
    };
}

APP.callDelayed = function callDelayed( element, func, delay ) {
    var el = YAHOO.util.Dom.get(element);
    if ( el.zid ) {
        clearTimeout(el.zid);
    }
    if ( delay == null ) {
        delay = 600;
    }
    el.zid = setTimeout(func, delay);
}

APP.onCallReturn = function( jsonEncodedYUIResponse ) {
    var args = jsonEncodedYUIResponse.argument;
    var successCallback = ( args != null ? args[0] : null );
    var failureCallback = ( args != null ? args[1] : null );

    var jsonResponse = jsonEncodedYUIResponse.responseText.parseJSON();

    if ( jsonResponse.valid ) {
        if ( successCallback != null ) {
            successCallback(jsonResponse.body);
        }
    } else {
        if ( failureCallback != null ) {
            failureCallback(jsonResponse.body);
        }

    }
}

APP.call = function call( url, methodName, params, successCallback, failureCallback ) {
    var callUrl = url + "?actionLink=" + methodName + "&" + params;
    var args = new Array( successCallback, failureCallback );
    AjaxUtils.call( callUrl, APP.onCallReturn, args );
}


APP.util.PaginatedTable = function(container, urlRequest, initialRequestValues, columnDefs, responseSchema, ddGroup, tableChanges) {
    this.container = container;
    this.urlRequest = urlRequest;
    this.initialRequestValues = initialRequestValues;
    this.columnDefs = columnDefs;
    this.responseSchema = responseSchema;
    this.ddGroup = ddGroup;
    this.tableChanges = tableChanges;
    this.init();
};

APP.util.PaginatedTable.prototype = {

    container: null,
    urlRequest: null,
    initialRequestValues: null,
    columnDefs: null,
    responseSchema: null,
    dataStore: null,
    dataTable: null,
    totalRecords: 0,
    rowsPerPage: 50,
    ddGroup: null,
    paginator: null,
    tableChanges: null,


    init: function() {
    },

    loadData: function() {
        var dataTableConfigs = {
            paginated: true,
            paginator: {
                rowsPerPage: this.rowsPerPage,
                pageLinks: -1
            },
            initialRequest: this.initialRequestValues,
            scrollable: true
        };
        this.dataSource = new YAHOO.util.DataSource(this.urlRequest);
        this.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
        this.dataSource.responseSchema = this.responseSchema;
        this.dataSource.doBeforeCallback = this._parseData;
        this.dataTable = new YAHOO.widget.DataTable(this.container, this.columnDefs, this.dataSource, dataTableConfigs);

        this.dataTable.subscribe("initEvent", this.initPaginator, this, true);
        this.dataTable.subscribe("refreshEvent", this.destroyDragDrops, this, true);
        this.dataTable.subscribe("refreshEvent", this.createDragDrops, this, true);
    },

    _parseData: function(pRequest, pRawResponse, pParsedResponse) {
        // Let the DataSource parse the rest of the response
        return pParsedResponse;
    },

    initPaginator: function() {
        var dataTablePaginator = this.dataTable.get("paginator");
        var startIndex = (dataTablePaginator.currentPage - 1) * 50 + 1;
        var endIndex = (dataTablePaginator.currentPage) * 50;
        endIndex = endIndex > dataTablePaginator.totalRecords ? dataTablePaginator.totalRecords : endIndex;

        YAHOO.util.Dom.get(this.container + "_startIndex").innerHTML = startIndex;
        YAHOO.util.Dom.get(this.container + "_endIndex").innerHTML = endIndex;
        YAHOO.util.Dom.get(this.container + "_ofTotal").innerHTML = dataTablePaginator.totalRecords;        

        if (this.paginator) {
            this.paginator.paginatedTable = this;
            this.paginator.init();
        }
    },

    refreshPage: function() {
        var dataTable = this.dataTable;
        dataTable.refreshView();
        var dataTablePaginator = this.dataTable.get("paginator");
        var startIndex = (dataTablePaginator.currentPage - 1) * 50 + 1;
        var endIndex = (dataTablePaginator.currentPage) * 50;
        endIndex = endIndex > dataTablePaginator.totalRecords ? dataTablePaginator.totalRecords : endIndex;

        YAHOO.util.Dom.get(this.container + "_startIndex").innerHTML = startIndex;
        YAHOO.util.Dom.get(this.container + "_endIndex").innerHTML = endIndex;
        YAHOO.util.Dom.get(this.container + "_ofTotal").innerHTML = dataTablePaginator.totalRecords;
    },

    getPreviousPage: function(e) {
        YAHOO.util.Event.stopEvent(e);
        var dataTablePaginator = this.dataTable.get("paginator");
        // Already at first page
        if (dataTablePaginator.currentPage - 1 < 1) {
            return;
        }
        dataTablePaginator.currentPage--;
        // Move to previous page
        this.refreshPage();

        if (this.paginator) {
            this.paginator.moveToPrevPage();
        }
    },

    getNextPage: function(e) {
        YAHOO.util.Event.stopEvent(e);
        var dataTablePaginator = this.dataTable.get("paginator");
        // Already at last page
        if (dataTablePaginator.currentPage + 1 > dataTablePaginator.totalPages) {
            return;
        }
        // Move to next page
        dataTablePaginator.currentPage++;
        this.refreshPage();

        if (this.paginator) {
            this.paginator.moveToNextPage();
        }
    },

    getPage: function(e) {
        YAHOO.util.Event.stopEvent(e);
        var pageNumber = parseInt(YAHOO.util.Event.getTarget(e).innerHTML);
        var dataTablePaginator = this.dataTable.get("paginator");
        dataTablePaginator.currentPage = pageNumber;
        this.refreshPage();

        if (this.paginator) {
            this.paginator.moveToPage(dataTablePaginator.currentPage);
        }        
    },

    createDragDrops: function() {
        var nextTrEl = this.dataTable.getTrEl(0);
        while (nextTrEl) {
            new APP.util.CustomDDProxy(nextTrEl.id, this.ddGroup, {resizeFrame:true}, this);
            nextTrEl = nextTrEl.nextSibling;
        }
    },

    destroyDragDrops: function() {
        var DDM = YAHOO.util.DragDropMgr;
        var ids = DDM.ids[this.ddGroup];
        if ( ids ) {
            var nextTrEl = this.dataTable.getTrEl(0);
            while ( nextTrEl ) {
                var currentDD = ids[nextTrEl.id];
                nextTrEl = nextTrEl.nextSibling;
                if ( ! DDM.isTypeOfDD(currentDD) ) {
                    continue;
                }
                currentDD.removeFromGroup(this.ddGroup);
            }
        }
    }

};


APP.util.SimpleTable = function(container, urlRequest, initialRequestValues, columnDefs, responseSchema, enableSelection) {
    this.container = container;
    this.urlRequest = urlRequest;
    this.initialRequestValues = initialRequestValues;
    this.columnDefs = columnDefs;
    this.responseSchema = responseSchema;
    this.selectionEnabled = enableSelection;
    this.init();
};

APP.util.SimpleTable.prototype = {

    container: null,
    urlRequest: null,
    initialRequestValues: null,
    columnDefs: null,
    responseSchema: null,
    selectionEnabled: false,
    dataStore: null,
    dataTable: null,

    init: function() {
        var dataTableConfigs = {
            paginated: true,
            paginator: {
                rowsPerPage: 50,
                pageLinks: -1
            },
            initialRequest: this.initialRequestValues,
            scrollable: true
        };
        this.dataSource = new YAHOO.util.DataSource(this.urlRequest);
        this.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
        this.dataSource.responseSchema = this.responseSchema;
        this.dataTable = new YAHOO.widget.DataTable(this.container, this.columnDefs, this.dataSource, dataTableConfigs);

        if (this.selectionEnabled) {
            this.dataTable.set("selectionMode", "single");
            this.dataTable.subscribe("rowMouseoverEvent", this.dataTable.onEventHighlightRow);
            this.dataTable.subscribe("rowMouseoutEvent", this.dataTable.onEventUnhighlightRow);
            this.dataTable.subscribe("rowClickEvent", this.dataTable.onEventSelectRow);
        }
    }

};


APP.util.CustomDDProxy = function(id, sGroup, config, paginatedTableRef) {
    APP.util.CustomDDProxy.superclass.constructor.call(this, id, sGroup, config);
    this.isTarget = false;
    this.paginatedTableRef = paginatedTableRef;
};

YAHOO.extend(APP.util.CustomDDProxy, YAHOO.util.DDProxy, {

    paginatedTableRef: null,

    startDrag: function( x, y ) {
        var DOM = YAHOO.util.Dom;
        var sourceEl = this.getEl();
        var proxyEl = this.getDragEl();

        proxyEl.innerHTML = sourceEl.innerHTML;
        DOM.setStyle(proxyEl, "color", DOM.getStyle(sourceEl, "color"));
        DOM.setStyle(proxyEl, "backgroundColor", DOM.getStyle(sourceEl, "backgroundColor"));
        DOM.setStyle(proxyEl, "border", "2px solid gray");
        DOM.setStyle(proxyEl, "opacity", 0.7);
        DOM.setStyle(sourceEl, "opacity", 0.1);
    },

    endDrag: function(e) {
        var DOM = YAHOO.util.Dom;
        var sourceEl = this.getEl();
        var proxyEl = this.getDragEl();
        var endDragMotion = new YAHOO.util.Motion(
                proxyEl, {
                    points: {
                        to: DOM.getXY(sourceEl)
                    }
                },
                0.2,
                YAHOO.util.Easing.easeOut);

        // Hide the proxy and show the source element when finished with the animation
        endDragMotion.onComplete.subscribe(function() {
            DOM.setStyle(proxyEl, "visibility", "hidden");
            DOM.setStyle(sourceEl, "opacity", 1);
        });
        // Show the proxy element and animate it to the src element's location
        DOM.setStyle(proxyEl, "visibility", "");
        endDragMotion.animate();
    },

    onDragDrop: function(e, id) {
        var DOM = YAHOO.util.Dom;
        var sourceEl = this.getEl();
        var targetEl = DOM.get(id);
        var parentEl = targetEl.getElementsByTagName("tbody")[1];
        var firstRowEl = parentEl.getElementsByTagName("tr")[0];
        var sourceElValue = sourceEl.childNodes[0].innerHTML + "." + sourceEl.childNodes[1].innerHTML;

        parentEl.insertBefore(sourceEl, firstRowEl);
        DOM.setStyle(targetEl, "opacity", 1);

        if (this.groups["G1"]) {
            this.addToGroup("G2");
            this.removeFromGroup("G1");

            if (this.paginatedTableRef.tableChanges.realmUsers[sourceElValue]) {
                delete this.paginatedTableRef.tableChanges.realmUsers[sourceElValue];
            }
            this.paginatedTableRef.tableChanges.admins[sourceElValue] = true;
        }
        else {
            this.addToGroup("G1");
            this.removeFromGroup("G2");

            if (this.paginatedTableRef.tableChanges.admins[sourceElValue]) {
                delete this.paginatedTableRef.tableChanges.admins[sourceElValue];
            }
            this.paginatedTableRef.tableChanges.realmUsers[sourceElValue] = true;
        }
    },

    onDrag: function(e) {
    },

    onDragOver: function(e, id) {
    },

    onDragEnter: function(e, id) {
        var DOM = YAHOO.util.Dom;
        DOM.setStyle(DOM.get(id), "opacity", 0.50);
    },

    onDragOut: function(e, id) {
        var DOM = YAHOO.util.Dom;
        DOM.setStyle(DOM.get(id), "opacity", 1);
    }

});


APP.util.Paginator = function(container, imagesDir) {
    this.container = container;
    this.imagesDir = imagesDir;
};


APP.util.Paginator.prototype = {

    container: null,
    paginatedTable: null,
    imagesDir: null,
    currentPage: 1,
    visiblePages: 5,
    minVisiblePage: 1,
    totalPages: 0,

    init: function() {
        var tablePaginator = this.paginatedTable.dataTable.get("paginator");
        this.currentPage = 1;
        this.totalPages = tablePaginator.totalPages;
        this.visiblePages = (this.totalPages < 5) ? this.totalPages : 5;
        this.minVisiblePage = 1;
        this._paintPaginator();
    },

    _paintPaginator: function() {
        var DOM = YAHOO.util.Dom;
        var paginatorDiv = DOM.get(this.container);

        var node = DOM.get(this.container + "_prevLink");
        if (node) {
            node.parentNode.removeChild(node);
        }
        for(var i = 1; i <= 5; i++) {
            node = DOM.get(this.container + "_page_" + i + "_link");
            if (node) {
                node.parentNode.removeChild(node);
            }
        }
        node = DOM.get(this.container + "_nextLink");
        if (node) {
            node.parentNode.removeChild(node);
        }
        
        var prevLink = paginatorDiv.appendChild(document.createElement("span"));
        var prevLinkImg = prevLink.appendChild(document.createElement("img"));
        prevLink.id = this.container + "_prevLink";
        prevLinkImg.setAttribute("class", "pointer");
        prevLinkImg.setAttribute("src", this.imagesDir + "left.GIF");
        prevLinkImg.setAttribute("alt", "#");
        YAHOO.util.Event.addListener(prevLink, "click", this.paginatedTable.getPreviousPage, this.paginatedTable, true);
        DOM.setStyle(prevLink, "visibility", "hidden");

        for(var i = 1; i <= this.visiblePages; i++) {
            var pageLink = paginatorDiv.appendChild(document.createElement("span"));
            pageLink.id = this.container + "_page_" + i + "_link";
            pageLink.setAttribute("class", "pointer");
            pageLink.innerHTML = i;

            DOM.setStyle(pageLink, "padding", "10px");
            DOM.setStyle(pageLink, "color", i == 1 ? "#000000" : "#60a2e1");
            DOM.setStyle(pageLink, "fontFamily", "Arial, Helvetica, sans-serif");
            DOM.setStyle(pageLink, "fontSize", "12px");
            DOM.setStyle(pageLink, "fontWeight", "bold");
            
            if (i < this.visiblePages) {
                DOM.setStyle(pageLink, "borderRight", "1px #000000 solid");
            }

            YAHOO.util.Event.addListener(pageLink, "click", this.paginatedTable.getPage, this.paginatedTable, true);
        }

        var nextLink = paginatorDiv.appendChild(document.createElement("span"));
        var nextLinkImg = nextLink.appendChild(document.createElement("img"));
        nextLink.id = this.container + "_nextLink";
        nextLinkImg.setAttribute("class", "pointer");
        nextLinkImg.setAttribute("src", this.imagesDir + "right.GIF");
        nextLinkImg.setAttribute("alt", "#");
        YAHOO.util.Event.addListener(nextLink, "click", this.paginatedTable.getNextPage, this.paginatedTable, true);
        if (this.totalPages <= this.visiblePages) {
            DOM.setStyle(nextLink, "visibility", "hidden");
        }
    },

    moveToPrevPage: function() {
        this.currentPage--;
        this.minVisiblePage = this.currentPage < this.minVisiblePage ? this.minVisiblePage - 1 : this.minVisiblePage;
        this.refreshPaginator();
    },

    moveToNextPage: function() {
        this.currentPage++;
        this.minVisiblePage = this.currentPage - this.minVisiblePage >= this.visiblePages ? this.currentPage - this.visiblePages + 1 : this.minVisiblePage;
        this.refreshPaginator();
    },

    moveToPage: function(page) {
        this.currentPage = page;
        this.refreshPaginator();
    },

    refreshPaginator: function() {
        var DOM = YAHOO.util.Dom;
        for(var i = 1; i <= this.visiblePages; i++) {
            var pageLink = DOM.get(this.container + "_page_" + i + "_link");
            var pageNumber = this.minVisiblePage + i - 1;
            pageLink.innerHTML = pageNumber;
            DOM.setStyle(pageLink, "color", (pageNumber == this.currentPage ? "#000000" : "#60a2e1"));
        }

        var prevLink = DOM.get(this.container + "_prevLink");
        var nextLink = DOM.get(this.container + "_nextLink");
        DOM.setStyle(prevLink, "visibility", (this.currentPage > 1 ? "" : "hidden"));
        DOM.setStyle(nextLink, "visibility", ((this.totalPages > this.visiblePages && this.currentPage < this.totalPages) ? "" : "hidden"));
    }


};





