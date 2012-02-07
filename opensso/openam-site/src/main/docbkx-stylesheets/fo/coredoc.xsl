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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
 <xsl:import href="urn:docbkx:stylesheet"/>
 <xsl:import href="titlepages.xsl"/>
 <xsl:import href="urn:docbkx:stylesheet/highlight.xsl" />

 <xsl:param name="page.height.portrait">9in</xsl:param>
 <xsl:param name="page.width.portrait">7.5in</xsl:param>
 <xsl:param name="double.sided" select="1"></xsl:param>
 <xsl:param name="fop1.extensions" select="1" />
 
 <xsl:param name="body.font.master">9</xsl:param>
 <xsl:param name="body.font.family">DejaVuSerif</xsl:param>
 <xsl:param name="dingbat.font.family">DejaVuSerif</xsl:param>
 <xsl:param name="monospace.font.family">DejaVuSansMono</xsl:param>
 <xsl:param name="sans.font.family">DejaVuSans</xsl:param>
 <xsl:param name="title.font.family">DejaVuSans</xsl:param>
 
 <xsl:attribute-set name="section.title.level1.properties">
  <xsl:attribute name="font-size">14pt</xsl:attribute>
 </xsl:attribute-set>
 <xsl:attribute-set name="section.title.level2.properties">
  <xsl:attribute name="font-size">12pt</xsl:attribute>
 </xsl:attribute-set>
 <xsl:attribute-set name="section.title.level3.properties">
  <xsl:attribute name="font-size">11pt</xsl:attribute>
 </xsl:attribute-set>
 <xsl:attribute-set name="section.title.level4.properties">
  <xsl:attribute name="font-size">10pt</xsl:attribute>
 </xsl:attribute-set>
 <xsl:attribute-set name="section.title.level5.properties">
  <xsl:attribute name="font-size">10pt</xsl:attribute>
  <xsl:attribute name="font-weight">normal</xsl:attribute>
  <xsl:attribute name="font-style">italic</xsl:attribute>
 </xsl:attribute-set>
 
 <xsl:param name="generate.toc">
  appendix  nop
  article/appendix  nop
  article   nop
  book      toc,title
  chapter   nop
  part      toc,title
  preface   nop
  qandadiv  nop
  qandaset  nop
  reference toc,title
  sect1     nop
  sect2     nop
  sect3     nop
  sect4     nop
  sect5     nop
  section   nop
  set       toc,title
 </xsl:param>
 <xsl:param name="toc.max.depth">0</xsl:param>
 
 <xsl:param name="default.table.frame">topbot</xsl:param>
 <xsl:param name="default.table.rules">none</xsl:param>
 <xsl:param name="table.cell.border.thickness">0pt</xsl:param>
 
 <xsl:param name="variablelist.as.blocks" select="1"></xsl:param>
 <xsl:param name="variablelist.term.separator"></xsl:param>
 <xsl:param name="variablelist.term.break.after">1</xsl:param>
 
 <xsl:attribute-set name="monospace.properties">
  <xsl:attribute name="font-size">0.9em</xsl:attribute>
 </xsl:attribute-set>
 <xsl:param name="shade.verbatim" select="1" />
 <xsl:attribute-set name="shade.verbatim.style">
  <xsl:attribute name="background-color">#fafafa</xsl:attribute>
  <xsl:attribute name="border-width">0.5pt</xsl:attribute>
  <xsl:attribute name="border-style">solid</xsl:attribute>
  <xsl:attribute name="border-color">#e0eeee</xsl:attribute>
  <xsl:attribute name="padding">3pt</xsl:attribute>
  <xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
  <xsl:attribute name="font-size">0.75em</xsl:attribute>
 </xsl:attribute-set>
 
 <xsl:param name="ulink.footnotes" select="1" />
</xsl:stylesheet>