/* Offset position of tooltip */
var x_offset_tooltip = 5;
var y_offset_tooltip = 0;

var ajax_tooltipObj = false;
var ajax_tooltipObj_iframe = false;

var ajax_tooltip_MSIE = false;
if(navigator.userAgent.indexOf('MSIE')>=0)ajax_tooltip_MSIE=true;

var enableCache = true;
var jsCache = new Array();

var dynamicContent_ajaxObjects = new Array();

    function getLocale() {
        var queryString = window.top.location.search.substring(1);
        var locale = 'locale=';
        var localeValue = '';
        if (queryString.length > 0) {
            var idx = queryString.indexOf(locale);
            if (idx != -1) {
                idx += locale.length;
                var idx1 = queryString.indexOf('&', idx);
                if (idx1 == -1) {
                    idx1 = queryString.length;
                }
                localeValue = queryString.substring (idx, idx1);
            }
        }
        return locale + localeValue;
    }

	

var AjaxUtils = {

    showLoading: function() {
        YAHOO.util.Dom.setStyle('loading', 'display', '');
    },

    hideLoading: function() {
        YAHOO.util.Dom.setStyle('loading', 'display', 'none');
    },

    serializeForm: function(formId) {
        return YAHOO.util.Connect.setForm(formId);
    },

   /**
    * Does not load any particular div - only calls a URL (expected to be parameter encoded) for AJAX, where the
    * callback method will be executed when the response is returned.  This callback is expected to interpret
    * YUI's Response object, e.g. parse the response.responseText and manipulate the DOM accordingly.
    */
    simpleCall: function(url, successCallback, callbackArgs ) {
        AjaxUtils.doGet( null, url, successCallback, null, callbackArgs );
    },
    call: function(url, successCallback, callbackArgs ) {
        this.simpleCall(url, successCallback, callbackArgs);
    },

    load: function(elementId, url, successCallback, callbackArgs ) {
        AjaxUtils.doGet( elementId, url, successCallback, null, callbackArgs );
    },

    doGet: function( elementId, url, successCallback, failureCallback, callbackArgs ) {
        AjaxUtils.doAjaxCall(elementId, 'GET', url, null, successCallback, failureCallback, callbackArgs );
    },

    doPost: function(elementId, url, postData, callbackSuccess, callbackFailure, callbackArgs ) {
        YAHOO.util.Connect.resetFormState();    // Resets HTML form properties
        AjaxUtils.doAjaxCall(elementId, 'POST', url, postData, callbackSuccess, callbackFailure, callbackArgs);
    },

    doAjaxCall: function(elementId, method, url, postData, callbackSuccess, callbackFailure, callbackArgs ) {
        //AjaxUtils.showLoading();

        var el = YAHOO.util.Dom.get(elementId);

        var handleSuccess = function(o) {
            YAHOO.log( "ajax handleSuccess called", "info" );
            if (o.responseText !== undefined) {
                if (elementId != null) {
                    el.innerHTML = '&nbsp;'; //IE hack
                    el.innerHTML += o.responseText;
                    
                    // If it's a page evaluates any possible script
                    //if (options.mimeType == "text/html") {
                    AjaxUtils.executeScripts(elementId);
                    //}
                }
            }

            if (callbackSuccess != null) {
                callbackSuccess(o);
            }
            //AjaxUtils.hideLoading();
        }

        var handleFailure = function(o) {
            YAHOO.log( "ajax handleFailure called", "warn");
            //if (o.responseText !== undefined) {
                //YAHOO.log( "responseText != undefined" );
                el.innerHTML = "";
                el.innerHTML = "<li>Transaction id: " + o.tId + "</li>";
		        el.innerHTML += "<li>HTTP status: " + o.status + "</li>";
		        el.innerHTML += "<li>Status code message: " + o.statusText + "</li>";
            //}

            if (callbackFailure != null) {
                callbackFailure(o);
            }
            //AjaxUtils.hideLoading();
        }

        var callback = {
            success: handleSuccess,
            failure: handleFailure,
            argument: callbackArgs
        };

        YAHOO.util.Connect.asyncRequest(method, url, callback, postData);
    },
	
	/*Function for the toolTip*/
	/*showToolTip: function(inputObj, name, url, x, y) {			
		AjaxUtils.load(name, url);		
		document.getElementById(name).style.left = x + 'px';		
		document.getElementById(name).style.top = y + 'px';			
    },	*/




/*

doGet: function(url, parameters, targetId, callbackFunction) {
   var ajaxOptions = {
       method: "GET",
       mimeType: "text/html",
       charset: "utf-8",
       preventCache: true,
       targetId : targetId,
       callbackFunction : callbackFunction
   };

   AjaxUtils.doAjaxCall(url + "?" + parameters, ajaxOptions);
},

doPost: function(url, parameters, targetId, callbackFunction) {
   var ajaxOptions = {
       method: "POST",
       mimeType: "text/html",
       charset: "utf-8",
       preventCache: true,
       targetId : targetId,
       callbackFunction : callbackFunction
   };

   AjaxUtils.doAjaxCall(url + "?" + parameters, ajaxOptions);
},

doPostByForm: function(formNode, targetId, callbackFunction) {
   var ajaxOptions = {
       method: "POST",
       mimeType: "text/html",
       charset: "utf-8",
       preventCache: true,
       targetId : targetId,
       formNode : formNode,
       callbackFunction : callbackFunction
   };

   AjaxUtils.doAjaxCall(null, ajaxOptions);
},

doUpload: function(formId, callbackFunction) {
   var formElement = dojo.byId(formId);
   var ajaxOptions = {
       mimeType: "text/html",
       callbackFunction : callbackFunction,
       formNode: formElement
   };
   AjaxUtils.doAjaxCall(formElement.action, ajaxOptions);
},

doAjaxCall: function(url, options) {
   // Shows loading
   AjaxUtils.showLoading();

   if (url != null) {
       if (options.targetId != null){
           if (url != '')
               url += "&";
           url += "targetId="+options.targetId;
       }
   }
   var successCallback = function(type, data, evt) {
       if (options.targetId != null) {

           dojo.byId(options.targetId).innerHTML = '<input type="hidden"/>';
           dojo.byId(options.targetId).innerHTML += data;

           // If it's a page evaluates any possible script
           if (options.mimeType == "text/html") {
               AjaxUtils.executeScripts(options.targetId);
           }
       }
       if (options.callbackFunction != null) {
           options.callbackFunction(true);
       }
       // Hides loading
       AjaxUtils.hideLoading();
   }

   var errorCallback = function(type, error) {
       if (!(options.targetId.Equals('tab_content') && error instanceof TypeError)) { //  	Fixes Bug RBXLOR-317
                                                                                     // An error message is showing
                                                                               // while the users pass from document to their main folder.
           alert("error=[" + error.message + "]");
        }
       if (options.callbackFunction != null) {
           options.callbackFunction(false);
       }
       // Hides loading
       AjaxUtils.hideLoading();
   }

   dojo.io.bind({
       url: url,
       load: successCallback,
       error: errorCallback,
       mimetype: options.mimeType,
       method: options.method,
       formNode: options.formNode
   });
}, */

    executeScripts : function (elementId) {
        var element = YAHOO.util.Dom.get(elementId);
        var scriptNodes = AjaxUtils.executeElementScripts(element);
        for (var i = 0; i < scriptNodes.length; i++) {
            element.appendChild(scriptNodes[i]);
        }
    },

    executeElementScripts : function (element) {
        var childNodes = element.childNodes;
        var scriptNodes = new Array();

        for (var i = 0; i < childNodes.length; i++) {
            if (childNodes[i].tagName && childNodes[i].tagName.toUpperCase() == 'SCRIPT') {
                var scriptNode = AjaxUtils.getScriptNode(childNodes[i]);
                if ( YAHOO.lang.trim(scriptNode.innerHTML) != "" ) {
                    scriptNodes.push(scriptNode);
                }
            }
            else {
                var subScriptNodes = AjaxUtils.executeElementScripts(childNodes[i]);
                for (var j = 0; j < subScriptNodes.length; j++) {
                    scriptNodes.push(subScriptNodes[j]);
                }
            }
        }
        return scriptNodes;
    },

    getScriptNode : function (scriptNode) {
        var scriptElement = document.createElement("script");
        scriptElement.type = 'text/javascript';
        scriptElement.defer = true;
        if (scriptNode.src == null || scriptNode.src == '') {
            scriptElement.text = scriptNode.text;
        }
        else {
            scriptElement.src = scriptNode.src;
        }
        return scriptElement;
    },
	
	
	ajax_loadContent : function (divId,url)
	{
		if(enableCache && jsCache[url]){
			document.getElementById(divId).innerHTML = jsCache[url];
			return;
		}
	
		var ajaxIndex = dynamicContent_ajaxObjects.length;
		document.getElementById(divId).innerHTML = 'Loading content - please wait';
		dynamicContent_ajaxObjects[ajaxIndex] = new AjaxUtils.sack();
		dynamicContent_ajaxObjects[ajaxIndex].requestFile = url;	// Specifying which file to get
		dynamicContent_ajaxObjects[ajaxIndex].onCompletion = function(){ AjaxUtils.ajax_showContent(divId,ajaxIndex,url); };	// Specify function that will be executed after file has been found
		dynamicContent_ajaxObjects[ajaxIndex].runAJAX();		// Execute AJAX function	
	},

	ajax_showTooltip : function (externalFile,inputObj)
	{
		if(!ajax_tooltipObj)	/* Tooltip div not created yet ? */
		{
			ajax_tooltipObj = document.createElement('DIV');
			ajax_tooltipObj.style.position = 'absolute';
			ajax_tooltipObj.id = 'ajax_tooltipObj';		
			document.body.appendChild(ajax_tooltipObj);

		
			var leftDiv = document.createElement('DIV');	/* Create arrow div */
			leftDiv.className='ajax_tooltip_arrow';
			leftDiv.id = 'ajax_tooltip_arrow';
			ajax_tooltipObj.appendChild(leftDiv);
		
			var contentDiv = document.createElement('DIV'); /* Create tooltip content div */
			contentDiv.className = 'ajax_tooltip_content';
			ajax_tooltipObj.appendChild(contentDiv);
			contentDiv.id = 'ajax_tooltip_content';
		
			if(ajax_tooltip_MSIE){	/* Create iframe object for MSIE in order to make the tooltip cover select boxes */
				ajax_tooltipObj_iframe = document.createElement('<IFRAME frameborder="0">');
				ajax_tooltipObj_iframe.style.position = 'absolute';
				ajax_tooltipObj_iframe.border='0';
				ajax_tooltipObj_iframe.frameborder=0;
				ajax_tooltipObj_iframe.style.backgroundColor='#FFF';
				ajax_tooltipObj_iframe.src = 'about:blank';
				contentDiv.appendChild(ajax_tooltipObj_iframe);
				ajax_tooltipObj_iframe.style.left = '0px';
				ajax_tooltipObj_iframe.style.top = '0px';
			}
		}
		// Find position of tooltip
		ajax_tooltipObj.style.display='block';
		AjaxUtils.ajax_loadContent('ajax_tooltip_content',externalFile);
		if(ajax_tooltip_MSIE){
			ajax_tooltipObj_iframe.style.width = ajax_tooltipObj.clientWidth + 'px';
			ajax_tooltipObj_iframe.style.height = ajax_tooltipObj.clientHeight + 'px';
		}
		AjaxUtils.ajax_positionTooltip(inputObj);
	},

	ajax_positionTooltip : function (inputObj)
	{
		var leftPos = (AjaxUtils.ajaxTooltip_getLeftPos(inputObj) + inputObj.offsetWidth);
		var topPos = AjaxUtils.ajaxTooltip_getTopPos(inputObj);
		var tooltipWidth = document.getElementById('ajax_tooltip_content').offsetWidth +  document.getElementById('ajax_tooltip_arrow').offsetWidth; 
		// Dropping this reposition for now because of flickering
		//var offset = tooltipWidth - rightedge; 
		//if(offset>0)leftPos = Math.max(0,leftPos - offset - 5);
		ajax_tooltipObj.style.left = leftPos + 'px';
		ajax_tooltipObj.style.top = topPos-95 + 'px';
		
		
	},

	ajax_hideTooltip: function ()
	{	
		if (ajax_tooltipObj != false)
		{
			ajax_tooltipObj.style.display='none';
			ajax_tooltipObj = false;
		}
	},

	ajaxTooltip_getTopPos : function (inputObj)
	{		
  		var returnValue = inputObj.offsetTop;
 		while((inputObj = inputObj.offsetParent) != null)
		{
  			if(inputObj.tagName!='HTML')returnValue += inputObj.offsetTop;
  		}
 	 	return returnValue;
	},

	ajaxTooltip_getLeftPos : function (inputObj)
	{
  		var returnValue = inputObj.offsetLeft;
  		while((inputObj = inputObj.offsetParent) != null)
		{
  			if(inputObj.tagName!='HTML')returnValue += inputObj.offsetLeft;
  		}
  		return returnValue;
	},

	/*--------*/


	ajax_showContent : function (divId,ajaxIndex,url)
	{
		document.getElementById(divId).innerHTML = dynamicContent_ajaxObjects[ajaxIndex].response;
		if(enableCache){
			jsCache[url] = 	dynamicContent_ajaxObjects[ajaxIndex].response;
		}
		dynamicContent_ajaxObjects[ajaxIndex] = false;
	},

	


	sack : function (file){
		this.AjaxFailedAlert = "Your browser does not support the enhanced functionality of this website, and therefore you will have an experience that differs from the intended one.\n";
		this.requestFile = file;
		this.method = "POST";
		this.URLString = "";
		this.encodeURIString = true;
		this.execute = false;

		this.onLoading = function() { };
		this.onLoaded = function() { };
		this.onInteractive = function() { };
		this.onCompletion = function() { };

		this.createAJAX = function() {
			try
			{
				this.xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
			} catch (e)
			{
				try
				{
					this.xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
				} catch (err) 
				{
					this.xmlhttp = null;
				}
			}
			if(!this.xmlhttp && typeof XMLHttpRequest != "undefined")
				this.xmlhttp = new XMLHttpRequest();
			if (!this.xmlhttp)
			{
				this.failed = true; 
			}
		};
	
		this.setVar = function(name, value){
			if (this.URLString.length < 3){
				this.URLString = name + "=" + value;
			} else {
				this.URLString += "&" + name + "=" + value;
			}
		}
	
		this.encVar = function(name, value){
			var varString = encodeURIComponent(name) + "=" + encodeURIComponent(value);
			return varString;
		}
	
		this.encodeURLString = function(string){
			varArray = string.split('&');
			for (i = 0; i < varArray.length; i++){
				urlVars = varArray[i].split('=');
				if (urlVars[0].indexOf('amp;') != -1){
					urlVars[0] = urlVars[0].substring(4);
				}
				varArray[i] = this.encVar(urlVars[0],urlVars[1]);
				}
			return varArray.join('&');
		}
	
		this.runResponse = function(){
			eval(this.response);
		}
		
		this.runAJAX = function(urlstring){
			this.responseStatus = new Array(2);
			if(this.failed && this.AjaxFailedAlert){ 
				alert(this.AjaxFailedAlert); 
			} else {
				if (urlstring){ 
					if (this.URLString.length){
						this.URLString = this.URLString + "&" + urlstring; 
					} else {
						this.URLString = urlstring; 
					}
				}
				if (this.encodeURIString){
					var timeval = new Date().getTime(); 
					this.URLString = this.encodeURLString(this.URLString);
					this.setVar("rndval", timeval);
				}
				if (this.element) { this.elementObj = document.getElementById(this.element); }
				if (this.xmlhttp) {
					var self = this;
					if (this.method == "GET") {
						var totalurlstring = this.requestFile + "?" + this.URLString;
						this.xmlhttp.open(this.method, totalurlstring, true);
					} else {
						this.xmlhttp.open(this.method, this.requestFile, true);
					}
					if (this.method == "POST"){
						try {
							this.xmlhttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded')  
						} catch (e) {}
					}
	
					this.xmlhttp.send(this.URLString);
					this.xmlhttp.onreadystatechange = function() {
						switch (self.xmlhttp.readyState){
							case 1:
								self.onLoading();
							break;
							case 2:
								self.onLoaded();
							break;
							case 3:
								self.onInteractive();
							break;
							case 4:
								self.response = self.xmlhttp.responseText;
								self.responseXML = self.xmlhttp.responseXML;
								self.responseStatus[0] = self.xmlhttp.status;
								self.responseStatus[1] = self.xmlhttp.statusText;
								self.onCompletion();
								if(self.execute){ self.runResponse(); }
								if (self.elementObj) {
									var elemNodeName = self.elementObj.nodeName;
									elemNodeName.toLowerCase();
									if (elemNodeName == "input" || elemNodeName == "select" || elemNodeName == "option" || elemNodeName == "textarea"){
										self.elementObj.value = self.response;
									} else {
										self.elementObj.innerHTML = self.response;
									}
								}
								self.URLString = "";
							break;
						}
					};
				}
			}
		};
		this.createAJAX();
	}
}
