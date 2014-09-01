/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LogHtmlTemplate.java,v 1.2 2008/06/25 05:44:12 qcheng Exp $
 *
 */

package com.sun.identity.tools.logdoc;

/**
 * Template HTML, Cascading Stylesheet Sheet and Javascript.
 */
public class LogHtmlTemplate {
    /**
     * index html template.
     */
    public static String indexPage = "<html> <head> <script language=\"javascript\" src=\"browserVersion.js\" /> <script language=\"javascript\" src=\"stylesheet.js\" /> </head> <body> <table> <tr><td width=\"99%\">&nbsp;</td><td nowrap width=\"1%\">Log Reference Document</td></tr> </table> <h1>OpenSSO Project</h1> <h2>Log Reference Document</h2> Components <ul> @indices@ </ul> </body> </html>";

    /**
     * link template in index html page.
     */
    public static String indexLink = "<li><a href=\"@htmlpage@\">@name@</a></li>";

    /**
     * message IDs html page.
     */
    public static String page = "<html><head><script language=\"javascript\" src=\"browserVersion.js\"></script><script language=\"javascript\" src=\"stylesheet.js\"></script><script language=\"javascript\">function jumpto(list) {var options = list.options; this.location = '#' + options[list.selectedIndex].value; } </script> </head> <body> <table> <tr><td width=\"99%\">OpenSSO</td><td nowrap width=\"1%\"><a href=\"index.html\">Log Reference Document</a></td></tr> </table> <h1>OpenSSO Project</h1> <h2>Log Reference Document for @NAME@</h2> <hr noshade size=\"1\"/> <table> <tr><td width=\"99%\"></td><td nowrap width=\"1%\"> <form name=\"dummy\"> Go To: <select name=\"jump\" onchange=\"jumpto(this)\"> @ID_OPTIONS@ </select> </form> </td></tr></table> <table class=\"Tbl\"> <tr background-color=\"#BEC7CC\"> <th><b>Id</b></th> <th><b>Log Level</b></th> <th><b>Description</b></th> <th><b>Data</b></th> <th><b>Triggers</b></th> <th><b>Remedial Actions</b></th> </tr> @messages@ </table> <hr noshade size=\"1\"/> OpenSSO Project </body> </html>";

    /**
     * option templae in message IDs html page.
     */
    public static String option = "<option value=\"@id@\">@id@</value>"; 

    /**
     * log entry templae in message IDs html page.
     */
    public static String logmessage = "<tr><td valign=\"top\" align=\"center\"><a name=\"@id@\"></a>@id@</td><td valign=\"top\">@level@</td><td valign=\"top\">@description@</td><td valign=\"top\" nowrap>@datainfos@</td><td valign=\"top\">@triggers@</td><td valign=\"top\">@actions@</td></tr>";

    /**
     * css_ie5win.css
     */
    public static String css_ie5win = "BODY, TH, TD, P, DIV, SPAN, INPUT, SELECT, TEXTAREA, FORM, B, STRONG, I, U, DL, DD, DT, UL, LI, OL, OPTION, OPTGROUP, A {font-family:arial, helvetica, sans-serif;font-size:12px}\nH1 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:18px}\nH2 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:15px}\nH3, H4 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:12px}\nH5, H6 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:11px}\n.DefBdy {color:#333;background-color:#FFF;margin:0px}\na:link {color:#003399;text-decoration:none}\na:visited {color:#003399;text-decoration:none}\na:hover {color:#003399;text-decoration:underline}\n.ConMgn {margin:0px 10px}\n\ntable.Tbl {background-color:#BEC7CC;color:#333;width:100%;padding:6px;border-top:none;border-bottom:none;border-left:solid 10px #FFF;border-right:solid 10px #FFF}\n.TblMgn {}\ntable.Tbl td {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:4px 5px 1px 5px;background-color:#fff}\ntable.Tbl th {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:3px 5px 1px 5px;background-color:#fff;font-weight:normal}\n";

    /**
     * css_ie6up.css
     */
    public static String css_ie6up = "BODY, TH, TD, P, DIV, SPAN, INPUT, SELECT, TEXTAREA, FORM, B, STRONG, I, U, DL, DD, DT, UL, LI, OL, OPTION, OPTGROUP, A {font-family:arial, helvetica, sans-serif;font-size:12px}\nH1 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:18px}\nH2 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:15px}\nH3, H4 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:12px}\nH5, H6 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:11px}\n.DefBdy {color:#333;background-color:#FFF;margin:0px}\na:link {color:#003399;text-decoration:none}\na:visited {color:#003399;text-decoration:none}\na:hover {color:#003399;text-decoration:underline}\n.ConMgn {margin:0px 10px}\n\ntable.Tbl {background-color:#BEC7CC;color:#333;width:100%;padding:6px;border-top:none;border-bottom:none;border-left:solid 10px #FFF;border-right:solid 10px #FFF}\n.TblMgn {}\ntable.Tbl td {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:4px 5px 1px 5px;background-color:#fff}\ntable.Tbl th {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:3px 5px 1px 5px;background-color:#fff;font-weight:normal}\n";

    /**
     * css_ns4sol.css
     */
    public static String css_ns4sol = "BODY, TH, TD, P, DIV, SPAN, INPUT, SELECT, TEXTAREA, FORM, B, STRONG, I, U, DL, DD, DT, UL, LI, OL, OPTION, OPTGROUP, A {font-family:sans-serif;font-size:12pt}\nH1 {font-family:sans-serif;font-weight:bold;font-size:18pt}\nH2 {font-family:sans-serif;font-weight:bold;font-size:15pt}\nH3, H4 {font-family:sans-serif;font-weight:bold;font-size:12pt}\nH5, H6 {font-family:sans-serif;font-weight:bold;font-size:11pt}\n.DefBdy {color:#333;background-color:#FFF;margin:-10px 0px 0px -10px}\na:link, a:visited {color:#3a2eb5;text-decoration:none}\n.ConMgn {margin:0px 10px}\n\n.TblMgn {margin:0px 10px}\ntable.Tbl {background-color:#FFF;color:#333}\ntable.Tbl td {}\nth {font-weight:normal}\n";

    /**
     * css_ns4win.css
     */
    public static String css_ns4win = "BODY, TH, TD, P, DIV, SPAN, INPUT, SELECT, TEXTAREA, FORM, B, STRONG, I, U, DL, DD, DT, UL, LI, OL, OPTION, OPTGROUP, A {font-family:arial, helvetica, sans-serif;font-size:12px}\nH1 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:18px}\nH2 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:15px}\nH3, H4 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:12px}\nH5, H6 {font-family:arial, helvetica, sans-serif;font-weight:bold;font-size:11px}\n.DefBdy {color:#333;background-color:#FFF;margin:-10px 0px 0px -10px}\na:link, a:visited {color:#3A2EB5;text-decoration:none}\n.ConMgn {margin:0px 10px}\n\n/* ACTION TABLE  */\n.TblMgn {margin:0px 10px}\ntable.Tbl {background-color:#FFF;color:#333}\ntable.Tbl td {}\nth {font-weight:normal}\n";

    /**
     * css_ns6up.css
     */
    public static String css_ns6up = "BODY, TH, TD, P, DIV, SPAN, INPUT, SELECT, TEXTAREA, FORM, B, STRONG, I, U, H1, H2, H3, H4, H5, H6, \nDL, DD, DT, UL, LI, OL, OPTION, OPTGROUP, A {font-family:sans-serif;font-size:12px}\nH1 {font-family:sans-serif;font-weight:bold;font-size:18px}\nH2 {font-family:sans-serif;font-weight:bold;font-size:15px}\nH3, H4 {font-family:sans-serif;font-weight:bold;font-size:12px}\nH5, H6 {font-family:sans-serif;font-weight:bold;font-size:11px}\n.DefBdy {color:#333;background-color:#fff;margin:0px}\na:link {color:#003399;text-decoration:none}\na:visited {color:#003399;text-decoration:none}\na:hover {color:#003399;text-decoration:underline}\n.ConMgn {margin:0px 10px}\n\ntable.Tbl {background-color:#BEC7CC;color:#333;width:100%;padding:6px;border-right:none;border-bottom:none;empty-cells:show}\n.TblMgn {margin:0px 10px}\ntable.Tbl td {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:4px 5px 1px 5px;background-color:#fff}\ntable.Tbl th {border-right:solid 1px #BEC7CC;border-bottom:solid 1px #BEC7CC;border-left:none;border-top:none;padding:3px 5px 1px 5px;background-color:#fff;font-weight:normal}\n";

    /**
     * stylesheet.js
     */
    public static String stylesheetjs = "document.write(\"<link href='\");\n\nif (is_ie6up) {\ndocument.write(\"css_ie6up.css\");\n} else if (is_ie5up) {\ndocument.write(\"css_ie5win.css\");\n} else if (is_gecko) {\ndocument.write(\"css_ns6up.css\");\n} else if (is_nav4 && is_win) {\ndocument.write(\"css_ns4win.css\");\n} else if (is_nav4) {\ndocument.write(\"css_ns4sol.css\");\n} else {\ndocument.write(\"css_ns6up.css\");\n}\n\ndocument.write(\"' type='text/css' rel='stylesheet' />\");\n";

    /**
     * browserVersion.js
     */
    public static String browserjs = "var agt=navigator.userAgent.toLowerCase();\n\nvar is_major = parseInt(navigator.appVersion);\nvar is_minor = parseFloat(navigator.appVersion);\n\nvar is_nav  = ((agt.indexOf('mozilla')!=-1) && (agt.indexOf('spoofer')==-1)\n&& (agt.indexOf('compatible') == -1) && (agt.indexOf('opera')==-1)\n&& (agt.indexOf('webtv')==-1) && (agt.indexOf('hotjava')==-1));\nvar is_nav2 = (is_nav && (is_major == 2));\nvar is_nav3 = (is_nav && (is_major == 3));\nvar is_nav4 = (is_nav && (is_major == 4));\nvar is_nav4up = (is_nav && (is_major >= 4));\nvar is_navonly      = (is_nav && ((agt.indexOf(\";nav\") != -1) ||\n(agt.indexOf(\"; nav\") != -1)) );\nvar is_nav6 = (is_nav && (is_major == 5));\nvar is_nav6up = (is_nav && (is_major >= 5));\nvar is_gecko = (agt.indexOf('gecko') != -1);\n\n\nvar is_ie     = ((agt.indexOf(\"msie\") != -1) && (agt.indexOf(\"opera\") == -1));\nvar is_ie3    = (is_ie && (is_major < 4));\nvar is_ie4    = (is_ie && (is_major == 4) && (agt.indexOf(\"msie 4\")!=-1) );\nvar is_ie4up  = (is_ie && (is_major >= 4));\nvar is_ie5    = (is_ie && (is_major == 4) && (agt.indexOf(\"msie 5.0\")!=-1) );\nvar is_ie5_5  = (is_ie && (is_major == 4) && (agt.indexOf(\"msie 5.5\") !=-1));\nvar is_ie5up = (is_ie && !is_ie3 && !is_ie4);\nvar is_ie5_5up =(is_ie && !is_ie3 && !is_ie4 && !is_ie5);\nvar is_ie6    = (is_ie && (is_major == 4) && (agt.indexOf(\"msie 6.\")!=-1) );\nvar is_ie6up  = (is_ie && !is_ie3 && !is_ie4 && !is_ie5 && !is_ie5_5);\n\nvar is_aol   = (agt.indexOf(\"aol\") != -1);\nvar is_aol3  = (is_aol && is_ie3);\nvar is_aol4  = (is_aol && is_ie4);\nvar is_aol5  = (agt.indexOf(\"aol 5\") != -1);\nvar is_aol6  = (agt.indexOf(\"aol 6\") != -1);\n\nvar is_opera = (agt.indexOf(\"opera\") != -1);\nvar is_opera2 = (agt.indexOf(\"opera 2\") != -1 || agt.indexOf(\"opera/2\") != -1);\nvar is_opera3 = (agt.indexOf(\"opera 3\") != -1 || agt.indexOf(\"opera/3\") != -1);\nvar is_opera4 = (agt.indexOf(\"opera 4\") != -1 || agt.indexOf(\"opera/4\") != -1);\nvar is_opera5 = (agt.indexOf(\"opera 5\") != -1 || agt.indexOf(\"opera/5\") != -1);\nvar is_opera5up = (is_opera && !is_opera2 && !is_opera3 && !is_opera4);\n\nvar is_webtv = (agt.indexOf(\"webtv\") != -1); \n\nvar is_TVNavigator = ((agt.indexOf(\"navio\") != -1) || (agt.indexOf(\"navio_aoltv\") != -1)); \nvar is_AOLTV = is_TVNavigator;\n\nvar is_hotjava = (agt.indexOf(\"hotjava\") != -1);\nvar is_hotjava3 = (is_hotjava && (is_major == 3));\nvar is_hotjava3up = (is_hotjava && (is_major >= 3));\n\nvar is_js;\nif (is_nav2 || is_ie3) is_js = 1.0;\nelse if (is_nav3) is_js = 1.1;\nelse if (is_opera5up) is_js = 1.3;\nelse if (is_opera) is_js = 1.1;\nelse if ((is_nav4 && (is_minor <= 4.05)) || is_ie4) is_js = 1.2;\nelse if ((is_nav4 && (is_minor > 4.05)) || is_ie5) is_js = 1.3;\nelse if (is_hotjava3up) is_js = 1.4;\nelse if (is_nav6 || is_gecko) is_js = 1.5;\nelse if (is_nav6up) is_js = 1.5;\nelse if (is_ie5up) is_js = 1.3\n\nelse is_js = 0.0;\n\nvar is_win   = ( (agt.indexOf(\"win\")!=-1) || (agt.indexOf(\"16bit\")!=-1) );\nvar is_win95 = ((agt.indexOf(\"win95\")!=-1) || (agt.indexOf(\"windows 95\")!=-1));\n\nvar is_win16 = ((agt.indexOf(\"win16\")!=-1) || \n(agt.indexOf(\"16bit\")!=-1) || (agt.indexOf(\"windows 3.1\")!=-1) || \n(agt.indexOf(\"windows 16-bit\")!=-1) );  \n\nvar is_win31 = ((agt.indexOf(\"windows 3.1\")!=-1) || (agt.indexOf(\"win16\")!=-1) ||\n(agt.indexOf(\"windows 16-bit\")!=-1));\n\nvar is_winme = ((agt.indexOf(\"win 9x 4.90\")!=-1));\nvar is_win2k = ((agt.indexOf(\"windows nt 5.0\")!=-1));\n\nvar is_win98 = ((agt.indexOf(\"win98\")!=-1) || (agt.indexOf(\"windows 98\")!=-1));\nvar is_winnt = ((agt.indexOf(\"winnt\")!=-1) || (agt.indexOf(\"windows nt\")!=-1));\nvar is_win32 = (is_win95 || is_winnt || is_win98 || \n((is_major >= 4) && (navigator.platform == \"Win32\")) ||\n(agt.indexOf(\"win32\")!=-1) || (agt.indexOf(\"32bit\")!=-1));\n\nvar is_os2   = ((agt.indexOf(\"os/2\")!=-1) || \n(navigator.appVersion.indexOf(\"OS/2\")!=-1) ||   \n(agt.indexOf(\"ibm-webexplorer\")!=-1));\n\nvar is_mac    = (agt.indexOf(\"mac\")!=-1);\nif (is_mac && is_ie5up) is_js = 1.4;\nvar is_mac68k = (is_mac && ((agt.indexOf(\"68k\")!=-1) || \n(agt.indexOf(\"68000\")!=-1)));\nvar is_macppc = (is_mac && ((agt.indexOf(\"ppc\")!=-1) || \n(agt.indexOf(\"powerpc\")!=-1)));\n\nvar is_sun   = (agt.indexOf(\"sunos\")!=-1);\nvar is_sun4  = (agt.indexOf(\"sunos 4\")!=-1);\nvar is_sun5  = (agt.indexOf(\"sunos 5\")!=-1);\nvar is_suni86= (is_sun && (agt.indexOf(\"i86\")!=-1));\nvar is_irix  = (agt.indexOf(\"irix\") !=-1);    // SGI\nvar is_irix5 = (agt.indexOf(\"irix 5\") !=-1);\nvar is_irix6 = ((agt.indexOf(\"irix 6\") !=-1) || (agt.indexOf(\"irix6\") !=-1));\nvar is_hpux  = (agt.indexOf(\"hp-ux\")!=-1);\nvar is_hpux9 = (is_hpux && (agt.indexOf(\"09.\")!=-1));\nvar is_hpux10= (is_hpux && (agt.indexOf(\"10.\")!=-1));\nvar is_aix   = (agt.indexOf(\"aix\") !=-1);      // IBM\nvar is_aix1  = (agt.indexOf(\"aix 1\") !=-1);    \nvar is_aix2  = (agt.indexOf(\"aix 2\") !=-1);    \nvar is_aix3  = (agt.indexOf(\"aix 3\") !=-1);    \nvar is_aix4  = (agt.indexOf(\"aix 4\") !=-1);    \nvar is_linux = (agt.indexOf(\"inux\")!=-1);\nvar is_sco   = (agt.indexOf(\"sco\")!=-1) || (agt.indexOf(\"unix_sv\")!=-1);\nvar is_unixware = (agt.indexOf(\"unix_system_v\")!=-1); \nvar is_mpras    = (agt.indexOf(\"ncr\")!=-1); \nvar is_reliant  = (agt.indexOf(\"reliantunix\")!=-1);\nvar is_dec   = ((agt.indexOf(\"dec\")!=-1) || (agt.indexOf(\"osf1\")!=-1) || \n(agt.indexOf(\"dec_alpha\")!=-1) || (agt.indexOf(\"alphaserver\")!=-1) || \n(agt.indexOf(\"ultrix\")!=-1) || (agt.indexOf(\"alphastation\")!=-1)); \nvar is_sinix = (agt.indexOf(\"sinix\")!=-1);\nvar is_freebsd = (agt.indexOf(\"freebsd\")!=-1);\nvar is_bsd = (agt.indexOf(\"bsd\")!=-1);\nvar is_unix  = ((agt.indexOf(\"x11\")!=-1) || is_sun || is_irix || is_hpux || \nis_sco ||is_unixware || is_mpras || is_reliant || \nis_dec || is_sinix || is_aix || is_linux || is_bsd || is_freebsd);\n\nvar is_vms   = ((agt.indexOf(\"vax\")!=-1) || (agt.indexOf(\"openvms\")!=-1));\n\n";
}

