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
                                                                                
   $Id: ReadmeText.xsl,v 1.4 2008/08/19 19:08:26 veiming Exp $
                                                                                
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="text" encoding="iso-8859-1"/>

<xsl:template>
    <xsl:apply-templates select="/sample"/>
</xsl:template>

<xsl:template match="/sample">OpenSSO
<xsl:text>&#10;</xsl:text>
<xsl:value-of select="@title"/>

<xsl:apply-templates select="section" />

<xsl:text>&#10;</xsl:text>
End of Sample
<xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="section">
    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;    </xsl:text><xsl:value-of select="position()" />. <xsl:value-of select="@title" />
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="paragraph">
    <xsl:with-param name="para-pos" select="position()"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="note" />
</xsl:template>

<xsl:template match="paragraph">
    <xsl:param name="para-pos" />

    <xsl:if test="not(orderedlist) and not(unorderedlist)">
    <xsl:copy-of select="." />
    </xsl:if>

    <xsl:if test="orderedlist">
    <xsl:apply-templates select="orderedlist">
    <xsl:with-param name="para-pos" select="$para-pos" />
    </xsl:apply-templates>
    </xsl:if>

    <xsl:if test="unorderedlist">
    <xsl:apply-templates select="unorderedlist" />
    </xsl:if>

</xsl:template>

<xsl:template match="note">
    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <xsl:text>    ----------------------------------------------------------------------</xsl:text>
    <xsl:text>&#10;</xsl:text>

    <xsl:if test="not(orderedlist) and not(unorderedlist)">
    NOTE: <xsl:copy-of select="." />
    </xsl:if>

    <xsl:if test="orderedlist">
    <xsl:text>    NOTE:&#10;</xsl:text>
    <xsl:apply-templates select="orderedlist" />
    </xsl:if>

    <xsl:if test="unorderedlist">
    <xsl:apply-templates select="unorderedlist" />
    </xsl:if>
    
    <xsl:text>&#10;</xsl:text>
    <xsl:text>    ----------------------------------------------------------------------</xsl:text>
</xsl:template>

<xsl:template match="orderedlist">
    <xsl:param name="para-pos" />
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="item">
    <xsl:with-param name="para-pos" select="$para-pos" />
    <xsl:with-param name="nocontext" select="@nocontext" />
    </xsl:apply-templates>
    <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="*/note/orderedlist">
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="item" />
</xsl:template>

<xsl:template match="unorderedlist">
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="item" />
    <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="item">
    <xsl:param name="para-pos" />
    <xsl:param name="nocontext" />

    <xsl:if test="not($nocontext = 'yes')">
    <xsl:text>    </xsl:text><xsl:value-of select="$para-pos" />.<xsl:value-of select="position()" /><xsl:text> </xsl:text><xsl:value-of select="." />
    </xsl:if>

    <xsl:if test="$nocontext = 'yes'">
    <xsl:text>    </xsl:text><xsl:value-of select="position()" /><xsl:text>. </xsl:text><xsl:value-of select="." />
    </xsl:if>

    <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="*/unorderedlist/item">
    <xsl:text>    </xsl:text>* <xsl:text></xsl:text><xsl:value-of select="." />
    <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="*/note/orderedlist/item">
    <xsl:text>    </xsl:text><xsl:value-of select="position()" /><xsl:text>. </xsl:text><xsl:value-of select="." />
    <xsl:text>&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
