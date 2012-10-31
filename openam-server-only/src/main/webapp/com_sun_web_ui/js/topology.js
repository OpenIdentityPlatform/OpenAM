//
// ident        "@(#)topology.js 1.5 04/08/23 SMI"
//
// Copyright 2003-2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code defines functions for the topology tag.
// 

function openTopoMenu(form,id,menu) {
  ccScroll.set();
  form.cctopologyid.value = id;
  window.showMenu(menu);
}
function closeTopoMenu(form,action) {
  ccScroll.set();
  form.cctopologyaction.value = action;
  form.submit();
}
function topoClick(form,id,action) {
  ccScroll.set();
  form.cctopologyid.value = id;
  form.cctopologyaction.value = action;
  form.submit();
}
function topoKeyPress(e,form,id,action) {
  var keycode = (window.event)
    ? window.event.keyCode
    : e.which;
  if (keycode == 13) {
    topoClick(form,id,action);
    return false;
  }
  return true;
}
