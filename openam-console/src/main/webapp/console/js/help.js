/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 *
 */

function hideHelp(num) {
    if(document.getElementById("help"+num) && document.getElementById("helpImg"+num)) {
        document.getElementById("help"+num).style.display = "none";   
        setSrcURL("helpImg"+num, "/console/images/help/info.gif");
    }
}

function showHelp(num) {
  if (document.getElementById("help"+num).style.display != "block") {
    setSrcURL("helpImg"+num, "/console/images/help/info-selected.gif");

    document.getElementById("help"+num).style.display = "block";
    document.getElementById("close"+num).focus();
  }
  else if (document.getElementById("help"+num).style.display = "block"){
    document.getElementById("help"+num).style.display = "none";
    setSrcURL("helpImg"+num, "/console/images/help/info-hover.gif");
  }
}

// Hover Functions
function hoverHelp(num) {
  if (document.getElementById("help"+num).style.display != "block") {
    setSrcURL("helpImg"+num, "/console/images/help/info-hover.gif");
  }
  else { 
    setSrcURL("helpImg"+num, "/console/images/help/info-selected.gif");
  }
}
function outHelp(num) {
  if (document.getElementById("help"+num).style.display != "block") {
    setSrcURL("helpImg"+num, "/console/images/help/info.gif");
  }
  else {
    setSrcURL("helpImg"+num, "/console/images/help/info-selected.gif");
  }
}

function setSrcURL(id, newPath) {
   var tmp = document.getElementById(id);
   tmp.src = amContextRoot + newPath;
}
