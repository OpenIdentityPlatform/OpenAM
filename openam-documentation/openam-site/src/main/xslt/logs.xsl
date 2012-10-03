<?xml version="1.0" encoding="UTF-8"?>
<!--
  ! CCPL HEADER START
  !
  ! This work is licensed under the Creative Commons
  ! Attribution-NonCommercial-NoDerivs 3.0 Unported License.
  ! To view a copy of this license, visit
  ! http://creativecommons.org/licenses/by-nc-nd/3.0/
  ! or send a letter to Creative Commons, 444 Castro Street,
  ! Suite 900, Mountain View, California, 94041, USA.
  !
  ! You can also obtain a copy of the license at
  ! src/main/resources/legal-notices/CC-BY-NC-ND.txt.
  ! See the License for the specific language governing permissions
  ! and limitations under the License.
  !
  ! If applicable, add the following below this CCPL HEADER, with the fields
  ! enclosed by brackets "[]" replaced with your own identifying information:
  !      Portions Copyright [yyyy] [name of copyright owner]
  !
  ! CCPL HEADER END
  !
  !      Copyright 2011 ForgeRock AS
  !    
-->
<!-- 
 This style sheet transforms a logmessages document into a DocBook
 variablelist block element, intended for inclusion in a log reference.
 
 This use of variablelist is, strictly speaking, a mistranslation into
 DocBook. An idiomatic translation would use msgset. Yet, as Norm Walsh's
 Definitive Guide says, "On the whole, the semantics of msgset are not
 clearly defined." Current DocBook XSL style sheets fail to render msgset
 content legibly.
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
 <xsl:template match="/logmessages">
  <variablelist>
   <xsl:attribute name="xml:id">
    <xsl:text>log-ref-</xsl:text><xsl:value-of select="@prefix"/>
   </xsl:attribute>
   <para>OpenAM logs the following <xsl:value-of select="@prefix"/> messages.</para>
   <xsl:for-each select="logmessage">
    <varlistentry>
     <term><xsl:value-of select="@name" /></term>
     <listitem>
      <para>ID: <xsl:value-of select="../@prefix"/>-<xsl:value-of select="@id" /></para>
      <para>Level: <xsl:value-of select="substring(@loglevel,4)" /></para>
      <para>Description: <xsl:value-of select="normalize-space(@description)" /></para>
      <xsl:if test="datainfo/item">
       <para>Data: <xsl:for-each select="datainfo/item">
        <xsl:value-of select="normalize-space()" />
        <xsl:if test="position() &lt; last()">, </xsl:if>
       </xsl:for-each></para>
      </xsl:if>
      <xsl:if test="triggers/item">
       <para>Triggers: <xsl:for-each select="triggers/item">
        <xsl:value-of select="normalize-space()" />
        <xsl:if test="position() &lt; last()">; </xsl:if>
       </xsl:for-each></para>
      </xsl:if>
      <xsl:if test="actions/item">
       <para>Actions: <xsl:for-each select="actions/item">
        <xsl:value-of select="normalize-space()" />
        <xsl:if test="position() &lt; last()">; </xsl:if>
       </xsl:for-each></para>
      </xsl:if>
     </listitem>
    </varlistentry>
   </xsl:for-each>
  </variablelist>
 </xsl:template>
</xsl:stylesheet>