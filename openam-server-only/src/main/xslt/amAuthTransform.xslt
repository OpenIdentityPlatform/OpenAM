<?xml version="1.0" encoding="UTF-8"?>
<!--
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" doctype-public="=//iPlanet//Service Management Services (SMS) 1.0 DTD//EN" doctype-system="jar://com/sun/identity/sm/sms.dtd"/>

<xsl:template match="@* | node()">
<xsl:copy>
    <xsl:apply-templates select="@* | node()"/>
</xsl:copy>
</xsl:template>

<xsl:template match="Value">
<xsl:copy-of select="."/>
<xsl:if test="text()='com.sun.identity.authentication.modules.ldap.LDAP'">
    <Value>com.sun.identity.authentication.modules.securid.SecurID</Value>
</xsl:if>
</xsl:template>

</xsl:stylesheet>