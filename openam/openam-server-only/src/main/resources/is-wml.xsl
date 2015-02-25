<?xml version="1.0" encoding="UTF-8"?>

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

   $Id: is-wml.xsl,v 1.2 2008/06/25 05:48:39 qcheng Exp $

-->

<!-- This stylesheet converts an is:Inquiry into a WML deck.
      Note that this  an example.
      This could be customized during deployement.
      -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:is="urn:liberty:is:2003-08" exclude-result-prefixes="is">

    <xsl:output method="xml" version="4.0" encoding="UTF-8" omit-xml-declaration="yes" />

    <xsl:variable name="trueLabel">
        <xsl:value-of select="//is:Inquiry/@trueLabel"/>
    </xsl:variable>

    <xsl:variable name="falseLabel">
        <xsl:value-of select="//is:Inquiry/@falseLabel"/>
    </xsl:variable>

    <xsl:variable name="helpLabel">
        <xsl:value-of select="//is:Inquiry/@helpLabel"/>
    </xsl:variable>

    <xsl:variable name="hintLabel">
        <xsl:value-of select="//is:Inquiry/@hintLabel"/>
    </xsl:variable>

    <xsl:variable name="linkLabel">
        <xsl:value-of select="//is:Inquiry/@linkLabel"/>
    </xsl:variable>

    <xsl:variable name="moreLinkLabel">
        <xsl:value-of select="//is:Inquiry/@moreLinkLabel"/>
    </xsl:variable>

    <xsl:template match="/">
        <xsl:apply-templates select="//is:Inquiry" />
    </xsl:template>

    <xsl:template match="is:Inquiry">
        <wml>
            <xsl:apply-templates select="is:Confirm"/>
            <xsl:apply-templates select="is:Select"/>
            <xsl:apply-templates select="is:Text"/>
        </wml>
    </xsl:template>

    <xsl:template match="is:Text">
        <card>
        <p>
        <xsl:value-of select="../@title"/>
        <xsl:value-of select="is:Label"/>
        <xsl:element name="input">
            <xsl:attribute name="type">text</xsl:attribute>
            <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
            <xsl:attribute name="value"><xsl:value-of select="value"/></xsl:attribute>
        </xsl:element>
        <xsl:element name="do">
            <xsl:attribute name="type">accept</xsl:attribute>
            <xsl:element name="go">
                <xsl:attribute name="href">
                    <xsl:value-of select="../@action"/>
                </xsl:attribute>
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:element name="postfield">
                    <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
                    <xsl:attribute name="value">$(isparam_<xsl:value-of select="@name"/>)</xsl:attribute>
                </xsl:element>
            </xsl:element>
        </xsl:element>
        </p>
        </card>
    </xsl:template>

    <xsl:template match="is:Confirm">
        <card>
        <p>
        <xsl:value-of select="../@title"/>
        <xsl:value-of select="is:Label"/>
        <xsl:element name="select">
            <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
            <xsl:element name="option">
                <xsl:attribute name="value">true</xsl:attribute>
                <xsl:value-of select="$trueLabel"/>
            </xsl:element>
            <xsl:element name="option">
                <xsl:attribute name="value">false</xsl:attribute>
                <xsl:value-of select="$falseLabel"/>
            </xsl:element>
        </xsl:element>
        <xsl:element name="do">
            <xsl:attribute name="type">accept</xsl:attribute>
            <xsl:element name="go">
                <xsl:attribute name="href">
                    <xsl:value-of select="../@action"/>
                </xsl:attribute>
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:element name="postfield">
                    <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
                    <xsl:attribute name="value">$(isparam_<xsl:value-of select="@name"/>)</xsl:attribute>
                </xsl:element>
            </xsl:element>
        </xsl:element>
        </p>
        </card>
    </xsl:template>

    <xsl:template match="is:Select">
        <card>
        <p>
        <xsl:value-of select="../@title"/>
        <xsl:value-of select="is:Label"/>
        <xsl:element name="select">
            <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
                <xsl:apply-templates select="is:Item"/>
        </xsl:element>
        <xsl:element name="do">
            <xsl:attribute name="type">accept</xsl:attribute>
            <xsl:element name="go">
                <xsl:attribute name="href">
                    <xsl:value-of select="../@action"/>
                </xsl:attribute>
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:element name="postfield">
                    <xsl:attribute name="name">isparam_<xsl:value-of select="@name"/></xsl:attribute>
                    <xsl:attribute name="value">$(isparam_<xsl:value-of select="@name"/>)</xsl:attribute>
                </xsl:element>
            </xsl:element>
        </xsl:element>
        </p>
        </card>
    </xsl:template>

    <xsl:template match="is:Item">
        <xsl:element name="option">
            <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
                <xsl:value-of select="@label"/>
        </xsl:element>
    </xsl:template>


</xsl:stylesheet>
