<?xml version="1.0" encoding="iso-8859-1"?>

<!--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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
                                                                                
   $Id: ReadmeHTML.xsl,v 1.4 2008/08/19 19:15:12 veiming Exp $
                                                                                
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="iso-8859-1"/>

<xsl:template>
    <xsl:apply-templates select="/sample"/>
</xsl:template>

<xsl:template match="/sample">
<xsl:text disable-output-escaping='yes'>&lt;!doctype html public "-//w3c//dtd html 4.0 transitional//en">

</xsl:text>

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <title>OpenSSO - Samples</title>
    <link rel="stylesheet" href="{@relativePath}/sample.css" />
</head>

<body bgcolor="#FFFFFF" link="#0000FF" vlink="#800080">
<table cellspacing="8" cellpadding="4" border="0" width="100%" >
<tr nowrap="yes">
<td class="bannerContentLeft" width="20%" align="center"><a href="http://www.sun.com"><img border="0" src="{@relativePath}/sunLogo.gif" /></a></td>
<td class="bannerContentCenter" width="60%" valign="bottom" nowrap="yes">OpenSSO<br />Sample</td>
<td class="bannerContentRight" width="20%" valign="bottom" nowrap="yes">&#160;</td>
</tr>
</table>

<table cellspacing="8" cellpadding="4" border="0" width="100%">
<tr>
<td>
<p><xsl:apply-templates select="parentagepaths"/></p>
<h2><xsl:value-of select="@title" /></h2>

<xsl:apply-templates select="section" />
</td>
</tr>
</table>

<hr noshade="yes" size="1" />
End of Sample
</body>
</html>
</xsl:template>

<xsl:template match="section">
<p>
<hr noshade="yes" size="1" />
<h3><xsl:value-of select="position()" />. <xsl:value-of select="@title" /></h3>
<xsl:apply-templates select="paragraph" />
<xsl:apply-templates select="note" />
</p>
</xsl:template>

<xsl:template match="parentagepaths">
    <xsl:apply-templates select="path">
    <xsl:with-param name="node-count" select="count(./path)" />
    </xsl:apply-templates>
</xsl:template>

<xsl:template match="path">
    <xsl:param name="node-count" />

    <xsl:if test="not(position() = $node-count)">
    <a href="{@link}"><xsl:value-of select="@label" /></a>&#160;>
    </xsl:if>

    <xsl:if test="position() = $node-count">
    <a href="{@link}"><xsl:value-of select="@label" /></a>
    </xsl:if>
</xsl:template>

<xsl:template match="paragraph">
    <p>
    <xsl:if test="not(orderedlist) and not(unorderedlist)">
    <xsl:copy-of select="text() | *" />
    </xsl:if>

    <xsl:if test="orderedlist">
    <xsl:apply-templates select="orderedlist" />
    </xsl:if>

    <xsl:if test="unorderedlist">
    <xsl:apply-templates select="unorderedlist" />
    </xsl:if>
    </p>
</xsl:template>

<xsl:template match="note">
    <blockquote>
    <xsl:if test="not(orderedlist) and not(unorderedlist)">
    <b>NOTE:</b> <xsl:copy-of select="text() | *" />
    </xsl:if>

    <xsl:if test="orderedlist">
    <xsl:apply-templates select="orderedlist" />
    </xsl:if>

    <xsl:if test="unorderedlist">
    <xsl:apply-templates select="unorderedlist" />
    </xsl:if>
    </blockquote>
</xsl:template>

<xsl:template match="orderedlist">
    <ol>
    <xsl:apply-templates select="item" />
    </ol>
</xsl:template>

<xsl:template match="unorderedlist">
    <ul>
    <xsl:apply-templates select="item" />
    </ul>
</xsl:template>

<xsl:template match="item">
    <li><xsl:copy-of select="text() | *" /></li>
</xsl:template>

</xsl:stylesheet>
