/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: tasksPage.js,v 1.3 2008/07/24 18:16:55 veiming Exp $
 *
 */

// JavaScript Document

var hide  = true;
var tleft;
var ttop; 
var ileft;
var colnum = 18; // number of tasks

function getElementPositionByName(elemID) {
    getElementPositionEx(document.getElementsByName(elemID)[0]);
}

function getElementPosition(elemID) {
    getElementPositionEx(document.getElementById(elemID));
}

function getElementPositionEx(offsetTrail) {
    var offsetLeft = 0;
    var offsetTop = 0;


    while (offsetTrail) {
        offsetLeft += offsetTrail.offsetLeft;
        offsetTop += offsetTrail.offsetTop;
        offsetTrail = offsetTrail.offsetParent;
    }
    if (navigator.userAgent.indexOf("Mac") != -1 && 
        typeof document.body.leftMargin != "undefined") {
        offsetLeft += document.body.leftMargin;
        offsetTop += document.body.topMargin;
    }
   tleft=offsetLeft;
   ttop=offsetTop;

   //return {left:offsetLeft, top:offsetTop}; 

}
function getElementPosition2(elemID) {
    getElementPosition2Ex(document.getElementById(elemID));
}

function getElementPosition2Ex(offsetTrail) {
    var offsetLeft = 0;
    var offsetTop = 0;


    while (offsetTrail) {
        offsetLeft += offsetTrail.offsetLeft;
        offsetTop += offsetTrail.offsetTop;
        offsetTrail = offsetTrail.offsetParent;
    }
    if (navigator.userAgent.indexOf("Mac") != -1 && 
        typeof document.body.leftMargin != "undefined") {
        offsetLeft += document.body.leftMargin;
        offsetTop += document.body.topMargin;
    }
   ileft=offsetLeft;
}

function getElementPosition2ByName(elemID) {
    getElementPosition2Ex(document.getElementsByName(elemID)[0]);
}

function closeAll(num) {
  for(i=1;i<=colnum;i++) {
    if(document.getElementById("info"+i) && document.getElementById("togImg"+i)) {
      document.getElementById("info"+i).style.display = "none";   
      document.getElementById("togImg"+i).src = "../console/images/tasks/rightToggle.gif";
    }
  }
  document.getElementById("i"+num).focus();
}
function showDiv(num) {
document.getElementById("info"+num).style.display = "block";
}

function hideAllMenus() {
  for(i=1;i<=colnum;i++) {
    if(document.getElementById("info"+i) && document.getElementById("togImg"+i)) {
      document.getElementById("info"+i).style.display = "none";
      document.getElementById("togImg"+i).src = "../console/images/tasks/rightToggle.gif";  
    }
  }
}


// Toggle functions

function test(num) {
  getElementPosition2("togImg"+num);
  if (document.getElementById("info"+num).style.display != "block") {
    for(i=1;i<=colnum;i++) {
      if(i!=num && document.getElementById("togImg"+i) && document.getElementById("info"+i)) {
        document.getElementById("togImg"+i).src = "../console/images/tasks/rightToggle.gif";
        document.getElementById("info"+i).style.display = "none";
      }
    }
    document.getElementById("togImg"+num).src = "../console/images/tasks/rightToggle-selected.gif";

    getElementPosition("gif"+num);



    document.getElementById("info"+num).style.display = "block";
    document.getElementById("info"+num).style.top = (ttop + 10) + 'px';
    document.getElementById("info"+num).style.left = (tleft -1) + 'px';
    document.getElementById("info"+num).style.width = (ileft - tleft) + 29 + 'px';
    
    document.getElementById("close"+num).focus();
  }
  else if (document.getElementById("info"+num).style.display = "block"){
    for(i=1;i<=colnum;i++) {
      if(document.getElementById("togImg"+i)) {
        document.getElementById("togImg"+i).src = "../console/images/tasks/rightToggle.gif";
      }
    }
    document.getElementById("info"+num).style.display = "none";
  }
}


// Hover Functions

function hoverImg(num) {
  if (document.getElementById("info"+num).style.display != "block") {
    document.getElementById("togImg"+num).src = "../console/images/tasks/rightToggle-rollover.gif"
  }
  else { 
    document.getElementById("togImg"+num).src = "../console/images/tasks/rightToggle-selected.gif";
  }
}
function outImg(num) {
  if (document.getElementById("info"+num).style.display != "block") {
    document.getElementById("togImg"+num).src = "../console/images/tasks/rightToggle.gif";
  }
  else {
    document.getElementById("togImg"+num).src = "../console/images/tasks/rightToggle-selected.gif";
  }
}

function hideHelp() {
    var divHelp = document.getElementById('divhelp');
    divHelp.style.display = 'none';
}

function showHelp(icon, msg) {
    var divHelp = document.getElementById('divhelp');
    getElementPosition2ByName(icon);
    getElementPositionByName(icon);

    divHelp.style.display = '';
    divHelp.style.top = (ttop + 10) + 'px';
    var left = (tleft -400);
    if (left < 0) {
        left = 1;
    }
    divHelp.style.left = left + 'px';
    document.getElementById('divHelpmsg').innerHTML = msg;
}


