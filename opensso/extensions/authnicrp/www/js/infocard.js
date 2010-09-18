/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: infocard.js,v 1.1 2009/10/28 08:35:25 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */

var BrowserUtility= {
    
    GetElementsByClass :function(cssClass, node, tag) {
		
		var result = new Array();
		var els = (node?node:document).getElementsByTagName(tag?tag:'*');
		var pattern = new RegExp("(^|\\s)" + cssClass + "(\\s|$)");
		for(each in els)
		    if(pattern.test(els[each].className))
			    result[result.length] = els[each];

		return result;
    },

    ShowClass :function(classname) {
		
	    var elements = this.GetElementsByClass(classname);
	    for(each in elements)
		    elements[each].style.display = 'block';
    },
    
    AddField :function(form, name, value ) {
		
        var p=document.createElement('input');
        p.type = "hidden";
        p.name = name;
        p.value = value;
        form.appendChild(p);
        return p;
    },
    
    AppendToForm :function(form, element) {
		
         form.appendChild(element);
    },

    AddObjectParameter :function( element, name , value ) {
		
        var trimValue = value.toString().replace(/,/g, "\n");
        var p = document.createElement('param');
        p.setAttribute('value' , trimValue );
        p.setAttribute('name' , name );
        element.appendChild(p);
    }
}

var InformationCard = {
	
    init : function() {
	        
        // shows the elements in the page that enable Information Cards.
        if( InformationCard.AreCardsSupported() ) {
            BrowserUtility.ShowClass("InformationCardsSupported");
        } else {
            BrowserUtility.ShowClass("InformationCardsNotSupported");
        }
    },
  
    _areCardsSupported : 'undefined',

    AreCardsSupported : function()  {
   
		// short circuit after the first call by caching the value.
		if( this._areCardsSupported != 'undefined')
			return this._areCardsSupported;
			
		  var IEVer = -1; 
		  if (navigator.appName == 'Microsoft Internet Explorer') 
			if (new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})")
			   .exec(navigator.userAgent) != null) 
			  IEVer = parseFloat( RegExp.$1 ); 

		  // Look for IE 7+. 
		  if( IEVer >= 7 ) { 
			var embed = document.createElement("object"); 
			embed.setAttribute("type", "application/x-informationcard"); 

			return (this._areCardsSupported = (""+embed.issuerPolicy != "undefined" && embed.isInstalled));
		  }     
		  // not IE (any version)
		  if( IEVer < 0 && navigator.mimeTypes && navigator.mimeTypes.length)  { 
			// check to see if there is a mimeType handler. 
			x = navigator.mimeTypes['application/x-informationcard']; 
			if (x && x.enabledPlugin) 
			  return (this._areCardsSupported = true);

			// check for the IdentitySelector event handler is there. 
			var event = document.createEvent("Events"); 
			event.initEvent("IdentitySelectorAvailable", true, true); 
			top.dispatchEvent(event); 

			if( top.IdentitySelectorAvailable == true) 
			  return (this._areCardsSupported = true); 
		  } 
		   return (this._areCardsSupported = false); 
    },

    GetInformationCardObject :function(form, icobj) {
		
        // create object
        var labels = ["requiredClaims", "optionalClaims", "tokenType",
            "issuer", "issuerPolicy" , "privacyUrl", "privacyVersion"];
        var obj = document.createElement('object');
        var requiredClaimsFound = false, privacyUrlFound = false,
            privacyVersionFound = false;
        var claim;
        
        for(i = 0; i < labels.length; i++) {
            claim = null;
            claim = icobj[labels[i]];
        
            if (claim) {
                BrowserUtility.AddObjectParameter(obj, labels[i], claim);
                if (labels[i] == "requiredClaims")
                    requiredClaimsFound = true;
                else if (labels[i] == "privacyUrl")
                    privacyUrlFound = true;
                else if (labels[i] == "privacyVersion")
                    privacyVersionFound = true;
            }
        }
                
        if (!requiredClaimsFound) {
            alert("Error: missing required claims in Information Card object");
            return null;
        } else if (privacyVersionFound && !privacyUrlFound) {
            alert("Error: found inconsistant privacy claims in Information Card object");
            return null;
        }

        // set the name to xmlToken
        obj.setAttribute( "name", "xmlToken");
        
        // set the type, and the it suddenly 'understands'
        obj.setAttribute( "type", "application/x-informationcard");      
        
        // adding the object to the body activates it.
        BrowserUtility.AppendToForm(form, obj);
        
        return obj;
    },
    
    // Matchs infocard.jsp 'Login' form constraints
    AddCardToLoginForm :function(form, icobj, trouble ) {
		/// <summary>
		/// Modify the Login form to add the information card token
		/// </summary>
		///
		/// <param name="form">the Login form.</param>
		/// <param name="icobj">Information Card object.</param>
		/// <param name="trouble">either a URL or a javascript function to call if the get token fails.</param>
		/// <param name="rememberUser">a helper value to tell the action page to 'remember me'</param>

        var card = this.GetInformationCardObject(form, icobj);
        
        if (card && card.innerHTML.charAt(0) == '<') {
            //Have a card, let's submit it in.
            BrowserUtility.AddField( form , "xmltoken" , card );
        } else {
            //error, let's go troubleshooting. Want to call a function?
            if (trouble instanceof Function )
                return trouble( card );

            // post to the error to the default page
            BrowserUtility.AddField( form , "errorvalue" , card );
            BrowserUtility.AddField( form , "sourcepage" , window.location.href);
        }
        return card;
    }
}

/* initial setup for Information Cards */
var _ic_init =  document.addEventListener?document.addEventListener("DOMContentLoaded", function(){InformationCard.init()}, false): setInterval(function(){if (/loaded|complete/.test(document.readyState)){clearInterval(_ic_init);InformationCard.init(); } }, 10);