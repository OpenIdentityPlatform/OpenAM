<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved

   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: Home.jsp,v 1.19 2009/11/18 00:00:12 asyhuang Exp $

--%>
<%--
   Portions Copyrighted 2010-2014 ForgeRock AS
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
   Portions Copyrighted 2013 Nomura Research Institute, Ltd
--%>

<%@ page info="Home" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
        className="com.sun.identity.console.task.HomeViewBean"
        fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
               locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true">

<link id="styleSheet" href="../console/css/commontask.css" type="text/css" rel="stylesheet" />
<link id="styleSheet" href="../console/css/css_master.css" type="text/css" rel="stylesheet" />
<script language="javascript" src="../com_sun_web_ui/js/browserVersion.js"></script>
<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript" src="../console/js/tasksPage.js"></script>

<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:tabs name="tabCommon" bundleID="amConsole" submitFormData="false" />

<div id="info1" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close1" onclick="closeAll(1); event.cancelBubble = true;return false;"><img alt="close hosted idp" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpCreateHostedIDP" defaultValue="commontask.create.hosted.idp" bundleID="amConsole" /></span></p>
</div>

<div id="info2" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close2" onclick="closeAll(2); event.cancelBubble = true;return false;"><img alt="close hosted sp" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpCreateHostedSP" defaultValue="commontask.create.hosted.sp" bundleID="amConsole" /></span></p>
</div>

<div id="info3" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close3" onclick="closeAll(3); event.cancelBubble = true;return false;"><img alt="close remote idp" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpCreateRemoteIDP" defaultValue="commontask.create.remote.idp" bundleID="amConsole" /></span></p>
</div>

<div id="info4" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close4" onclick="closeAll(4); event.cancelBubble = true;return false;"><img alt="close remote sp" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpCreateRemoteSP" defaultValue="commontask.create.remote.sp" bundleID="amConsole" /></span></p>
</div>

<div id="info5" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close5" onclick="closeAll(5); event.cancelBubble = true;return false;"><img alt="close create fedlet" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpCreateFedlet" defaultValue="commontask.create.fedlet" bundleID="amConsole" /></span></p>
</div>

<div id="info13" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close13" onclick="closeAll(13); event.cancelBubble = true;return false;"><img alt="close configure google apps" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureGoogleApps" defaultValue="commontask.configure.google.apps" bundleID="amConsole" /></span></p>
</div>

<div id="info14" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close14" onclick="closeAll(14); event.cancelBubble = true;return false;"><img alt="close configure salesforce" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureSalesForce" defaultValue="commontask.configure.salesforce.apps" bundleID="amConsole" /></span></p>
</div>

<div id="info6" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close6" onclick="closeAll(6); event.cancelBubble = true;return false;"><img alt="close validate samlv2" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpValidate" defaultValue="commontask.saml2.validate" bundleID="amConsole" /></span></p>
</div>

<div id="info11" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close11" onclick="closeAll(11); event.cancelBubble = true;return false;"><img alt="close documentation" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpDoc" defaultValue="commontask.doc" bundleID="amConsole" /></span></p>
</div>

<div id="info12" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close12" onclick="closeAll(12); event.cancelBubble = true;return false;"><img alt="close register product" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtRegisterProduct" defaultValue="commontask.register.product" bundleID="amConsole" /></span></p>
</div>

<div id="info7" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close7" onclick="closeAll(7); event.cancelBubble = true;return false;"><img alt="close configure oauth2" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpValidate" defaultValue="commontask.configure.oauth2" bundleID="amConsole" /></span></p>
</div>

<div id="info15" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close15" onclick="closeAll(15); event.cancelBubble = true;return false;"><img alt="close configure facebook authentication" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureFacebookAuthn" defaultValue="commontask.configure.facebook.authn" bundleID="amConsole" /></span></p>
</div>

<div id="info16" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close16" onclick="closeAll(16); event.cancelBubble = true;return false;"><img alt="close configure google authentication" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureGoogleAuthn" defaultValue="commontask.configure.google.authn" bundleID="amConsole" /></span></p>
</div>

<div id="info17" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close17" onclick="closeAll(17); event.cancelBubble = true;return false;"><img alt="close configure microsoft authentication" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureMicrosoftAuthn" defaultValue="commontask.configure.microsoft.authn" bundleID="amConsole" /></span></p>
</div>

<div id="info18" onclick="showDiv(); event.cancelBubble = true;" class="TskPgeInfPnl">
    <div><a href="#" id="close18" onclick="closeAll(18); event.cancelBubble = true;return false;"><img alt="close configure other social authentication" src="../console/images/tasks/close.gif" border="0" /></a></div>
    <p><span class="TskPgeHdr"><cc:text name="txtHelpConfigureOtherSocialAuthn" defaultValue="commontask.configure.other.social.authn" bundleID="amConsole" /></span></p>
</div>

<div class="TskPgeFllPge" id="TskPge" onclick="hideAllMenus()">
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr valign="top"><td>&nbsp;</td><td colspan="4"></td></tr>
        <tr>
            <td>&nbsp;</td>
            <td width="40%"><img alt="spacer" src="../console/images/tasks/spacer.gif" width="220" height="1" /></td>
            <td width="9%">&nbsp;</td>
            <td width="39%"><img alt="spacer" src="../console/images/tasks/spacer.gif" width="220" height="1" /></td>
            <td width="7%">&nbsp;</td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td valign="top">
                <table width="100%" border="0" cellspacing="0" cellpadding="0" class="TskPgeBtmSpc">
                    <tr>
                        <td colspan="3"><span class="TskPgeSbHdr"><cc:text name="txtSectionSAMLv2" defaultValue="page.title.common.tasks.section.SAML2" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF">
                                <span class="TskPgeHdr"><cc:text name="txtSectionDescSAMLv2" defaultValue="page.title.common.tasks.section.desc.SAML2" bundleID="amConsole" /></span>
                            </td></tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif1" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/CreateHostedIDP" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateHostedIDP" defaultValue="commontask.label.create.hosted.idp" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(1); event.cancelBubble = true; return false;"  onmouseover="hoverImg(1); event.cancelBubble = true;" onmouseout="outImg(1); event.cancelBubble = true;" onfocus="hoverImg(1); event.cancelBubble = true;" onblur="outImg(1); event.cancelBubble = true;" id="i1"><img alt="create hosted idp" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg1" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif2" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/CreateHostedSP" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateHostedSP" defaultValue="commontask.label.create.hosted.sp" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(2); event.cancelBubble = true; return false;"  onmouseover="hoverImg(2); event.cancelBubble = true;" onmouseout="outImg(2); event.cancelBubble = true;" onfocus="hoverImg(2); event.cancelBubble = true;" onblur="outImg(2); event.cancelBubble = true;" id="i2"><img alt="create hosted sp" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg2" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif3" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/CreateRemoteIDP" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateRemoteIDP" defaultValue="commontask.label.create.remote.idp" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(3); event.cancelBubble = true; return false;"  onmouseover="hoverImg(3); event.cancelBubble = true;" onmouseout="outImg(3); event.cancelBubble = true;" onfocus="hoverImg(3); event.cancelBubble = true;" onblur="outImg(3); event.cancelBubble = true;" id="i3"><img alt="create remote idp" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg3" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif4" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/CreateRemoteSP" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateRemoteSP" defaultValue="commontask.label.create.remote.sp" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(4); event.cancelBubble = true; return false;"  onmouseover="hoverImg(4); event.cancelBubble = true;" onmouseout="outImg(4); event.cancelBubble = true;" onfocus="hoverImg(4); event.cancelBubble = true;" onblur="outImg(4); event.cancelBubble = true;" id="i4"><img alt="create remote sp" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg4" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSelectionOAuth2" defaultValue="page.title.common.tasks.section.OAuth2" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionOAuth2" defaultValue="page.title.common.tasks.section.desc.OAuth2" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif7" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureOAuth2" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureOAuth2" defaultValue="commontask.label.configure.oauth2" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(7); event.cancelBubble = true; return false;"  onmouseover="hoverImg(7); event.cancelBubble = true;" onmouseout="outImg(7); event.cancelBubble = true;" onfocus="hoverImg(7); event.cancelBubble = true;" onblur="outImg(7); event.cancelBubble = true;" id="i7"><img alt="samlv2 validate" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg7" /></a></td>
                            </tr>
                        </table>
                    </td></tr>


                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionCreateFedlet" defaultValue="page.title.common.tasks.section.createFedlet" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionCreateDescFedlet" defaultValue="page.title.common.tasks.section.desc.createFedlet" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif5" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/CreateFedlet" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateFedlet" defaultValue="commontask.label.create.fedlet" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(5); event.cancelBubble = true; return false;"  onmouseover="hoverImg(5); event.cancelBubble = true;" onmouseout="outImg(5); event.cancelBubble = true;" onfocus="hoverImg(5); event.cancelBubble = true;" onblur="outImg(5); event.cancelBubble = true;" id="i5"><img alt="create fedlet" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg5" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionConfigureGoogleApps" defaultValue="page.title.common.tasks.section.configure.google.apps" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionDescGoogleApps" defaultValue="page.title.common.tasks.section.desc.configure.google.apps" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif13" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureGoogleApps" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureGoogleApps" defaultValue="commontask.label.configure.google.apps" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(13); event.cancelBubble = true; return false;"  onmouseover="hoverImg(13); event.cancelBubble = true;" onmouseout="outImg(13); event.cancelBubble = true;" onfocus="hoverImg(13); event.cancelBubble = true;" onblur="outImg(13); event.cancelBubble = true;" id="i13"><img alt="configure google apps" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg13" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                </table>
            <td>&nbsp;</td>
            <td valign="top">
                <table width="100%" border="0" cellspacing="0" cellpadding="0" class="TskPgeBtmSpc">

                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionConfigureSalesForce" defaultValue="page.title.common.tasks.section.configure.salesforce.apps" bundleID="amConsole" /></span></td>
                    </tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionDescSalesForce" defaultValue="page.title.common.tasks.section.desc.configure.salesforce.apps" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td>
                    </tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif14" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureSalesForceApps" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureSalesForceApps" defaultValue="commontask.label.configure.salesforce.apps" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(14); event.cancelBubble = true; return false;"  onmouseover="hoverImg(14); event.cancelBubble = true;" onmouseout="outImg(14); event.cancelBubble = true;" onfocus="hoverImg(14); event.cancelBubble = true;" onblur="outImg(14); event.cancelBubble = true;" id="i14"><img alt="configure sales force" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg14" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionConfigureSocialAuthn" defaultValue="page.title.common.tasks.section.configure.social.authn" bundleID="amConsole" /></span></td>
                    </tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionDescSocialAuthn" defaultValue="page.title.common.tasks.section.desc.configure.social.authn" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td>
                    </tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif15" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureSocialAuthN?type=facebook" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureFacebookAuthn" defaultValue="commontask.label.configure.facebook.authn" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(15); event.cancelBubble = true; return false;"  onmouseover="hoverImg(15); event.cancelBubble = true;" onmouseout="outImg(15); event.cancelBubble = true;" onfocus="hoverImg(15); event.cancelBubble = true;" onblur="outImg(15); event.cancelBubble = true;" id="i15"><img alt="configure facebook authentication" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg15" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif16" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureSocialAuthN?type=google" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureGoogleAuthn" defaultValue="commontask.label.configure.google.authn" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(16); event.cancelBubble = true; return false;"  onmouseover="hoverImg(16); event.cancelBubble = true;" onmouseout="outImg(16); event.cancelBubble = true;" onfocus="hoverImg(16); event.cancelBubble = true;" onblur="outImg(16); event.cancelBubble = true;" id="i16"><img alt="configure google authentication" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg16" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif17" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureSocialAuthN?type=microsoft" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureMicrosoftAuthn" defaultValue="commontask.label.configure.microsoft.authn" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(17); event.cancelBubble = true; return false;"  onmouseover="hoverImg(17); event.cancelBubble = true;" onmouseout="outImg(17); event.cancelBubble = true;" onfocus="hoverImg(17); event.cancelBubble = true;" onblur="outImg(17); event.cancelBubble = true;" id="i17"><img alt="configure microsoft authentication" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg17" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif18" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ConfigureSocialAuthN?type=other" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtConfigureOtherSocialAuthn" defaultValue="commontask.label.configure.other.social.authn" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(18); event.cancelBubble = true; return false;"  onmouseover="hoverImg(18); event.cancelBubble = true;" onmouseout="outImg(18); event.cancelBubble = true;" onfocus="hoverImg(18); event.cancelBubble = true;" onblur="outImg(18); event.cancelBubble = true;" id="i18"><img alt="configure other social authentication" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg18" /></a></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr>
                        <td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionValidateSAMLv2" defaultValue="page.title.common.tasks.section.validateSAMLv2" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionCreateDescValidateSAMLv2" defaultValue="page.title.common.tasks.section.desc.validateSAMLv2" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif6" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%;" class="TskPgeTskCntrTd"><a href="../task/ValidateSAML2Setup" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtCreateRemoteSP" defaultValue="commontask.label.saml2.validate" bundleID="amConsole" /></span></a></td>
                                <td width="3%" class="TskPgeTskRghtTd" align="right" valign="top"><a href="#" onclick="test(6); event.cancelBubble = true; return false;"  onmouseover="hoverImg(6); event.cancelBubble = true;" onmouseout="outImg(6); event.cancelBubble = true;" onfocus="hoverImg(6); event.cancelBubble = true;" onblur="outImg(6); event.cancelBubble = true;" id="i6"><img alt="samlv2 validate" src="../console/images/tasks/rightToggle.gif" width="29" height="21" border="0" id="togImg6" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                    <tr><td valign="top" colspan=3><span class="TskPgeSbHdr"><cc:text name="txtSectionDocumentation" defaultValue="page.title.common.tasks.section.documentation" bundleID="amConsole" /></span></td>
                    </tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%" border="0" cellspacing="0" cellpadding="0">
                            <tr><td colspan=3 style="background-color:#FFFFFF"><span class="TskPgeHdr"><cc:text name="txtSectionCreateDescDocumentation" defaultValue="page.title.common.tasks.section.desc.documentation" bundleID="amConsole" /></span></td>
                            </tr>
                        </table>
                    </td></tr>

                    <tr><td class="TskPgeBckgrTd">
                        <table width="100%"  border="0" cellspacing="0" cellpadding="0">
                            <tr>
                                <td width="2%" valign="bottom" class="TskPgeTskLftTd"><img alt="spacer" id="gif11" src="../console/images/tasks/spacer.gif" width="12" height="8" /></td>
                                <td width="100%" class="TskPgeTskCntrTd"><a href="http://docs.forgerock.org/en/index.html?product=openam" target="_blank" class="TskPgeTxtBg" onmouseover="this.className='TskPgeTxtBgOvr'" onfocus="this.className='TskPgeTxtBgOvr'" onmouseout="this.className='TskPgeTxtBg'" onblur="this.className='TskPgeTxtBg'"><span class="TskPgeTskLftBtm"></span><span class="TskPgeTskLftTp"></span><span class="TskPgeTskRghtBtm"></span><span class="TskPgeTskRghtTp"></span><span class="TskPgeTskRghtBrdr"></span><span class="TskPgeTskPdng"><cc:text name="txtLblDoc" defaultValue="commontask.label.doc" bundleID="amConsole"/></span></a></td>

                                <td width="3%" align="right" valign="top" class="TskPgeTskRghtTd" ><a href="#" onclick="test(11); event.cancelBubble = true; return false;" onmouseover="hoverImg(11); event.cancelBubble = true;" onmouseout="outImg(11); event.cancelBubble = true;" onfocus="hoverImg(11); event.cancelBubble = true;" onblur="outImg(11); event.cancelBubble = true;" id="i11"><img alt="get documentation" id="togImg11" src="../console/images/tasks/rightToggle.gif" width="29" height="21"  border="0" /></a></td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
            </td></tr>
    </table>
</div>

</cc:header>
</jato:useViewBean>
