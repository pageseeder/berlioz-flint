<?xml version="1.0"?> 
<!--
  The default stylesheet to generate an Indexable document from source XML.

  @author Christophe Lauret
  @version 7 June 2010
-->
<xsl:stylesheet version="2.0" xmlns:idx="http://weborganic.com/Berlioz/Index"
                              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:ps="http://www.pageseeder.com/editing/2.0"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              exclude-result-prefixes="idx ps xs">

<!-- Standard output for Flint Documents 2.0 -->
<xsl:output method="xml" indent="no" encoding="utf-8"
            doctype-public="-//Weborganic//DTD::Flint Index Documents 2.0//EN"
            doctype-system="http://weborganic.org/schema/flint/index-documents-2.0.dtd"/>

<!-- Send by the indexer -->
<xsl:param name="path"          />
<xsl:param name="visibility"    />
<xsl:param name="last-modified" />

<!-- Matches the root -->
<xsl:template match="/">
<!-- if not in version copy folder then index -->
 <documents version="2.0">
    <xsl:if test="not(substring-before($path,'/') castable as xs:double)">
    <document>
      <!-- Common fields -->
      <field name="path"          store="yes" index="not-analyzed"><xsl:value-of select="$path"/></field>
      <field name="visibility"    store="yes" index="not-analyzed"><xsl:value-of select="$visibility"/></field>
      <field name="last-modified" store="yes" index="not-analyzed"><xsl:value-of select="$last-modified"/></field>
      <field name="category"      store="yes" index="not-analyzed"><xsl:value-of select="idx:category()"/></field>    
      <field name="title"         store="yes" index="not-analyzed"><xsl:value-of select="(//heading1)[1]"/></field>
      <!-- Content-specific -->
      <xsl:apply-templates select="*" mode="specific"/>
    </document>
    </xsl:if>
 </documents>
</xsl:template>


<!-- PageSeeder documents -->
<xsl:template match="text()" mode="specific"/>

<xsl:template match="root[ps:documentinfo]" mode="specific">
  <xsl:variable name="filename" select="substring-after($path,'/blog/')"/>
  <xsl:if test="matches($filename, '\d{4}-\d{2}-\d{2}')">
    <field name="pspostdate" store="yes" index="not-analyzed"><xsl:value-of select="substring($filename, 1, 10)"/></field>
  </xsl:if>
  <xsl:if test="//property[@name='document-type']">
    <field name="psdocumenttype" store="yes" index="not-analyzed"><xsl:value-of select="//property[@name='document-type']"/></field>
  </xsl:if>
  <xsl:for-each select="descendant::section[@id='metadata']">
    <field name="pubdate" store="yes" index="not-analyzed"><xsl:value-of select="descendant::inlineLabel[@name='pubdate']"/></field>
    <field name="author" store="yes" index="not-analyzed"><xsl:value-of select="descendant::inlineLabel[@name='author']"/></field>
    <xsl:for-each select="tokenize(descendant::inlineLabel[@name='tags'], ',')">
      <field name="tag" store="yes" index="not-analyzed"><xsl:value-of select="."/></field>
    </xsl:for-each>
  </xsl:for-each>
  <!-- PageSeeder documents -->
  <xsl:apply-templates select="//section/body/*" mode="psml"/>
</xsl:template>

<!--
  Returns the category for the specified item given the current path. 
-->
<xsl:function name="idx:category">
  <xsl:value-of select="
   if (starts-with($path, '/'))
     then 
       if (starts-with($path,' /content' ) )
       then 
        tokenize(substring($path, 1), '/')[2]
       else 
        tokenize(substring($path, 1), '/')[1]
     else 
      if (starts-with($path,'content' ) )
      then 
        tokenize($path, '/')[2]
       else 
        tokenize($path, '/')[1]
      
  "/>
</xsl:function>

<!-- Standard PSML indexing ====================================================================== -->

<!-- Top level elements -->
<xsl:template match="body/*" mode="psml">
  <xsl:variable name="text"><xsl:value-of select="descendant::text()"/></xsl:variable>
  <xsl:if test="string-length(normalize-space($text)) gt 0">
    <field name="fulltext" store="yes" index="analyzed"><xsl:value-of select="$text"/></field>
  </xsl:if>
</xsl:template>

<!-- Top level lists -->
<xsl:template match="body/table" mode="psml" priority="2">
<field name="fulltext" store="yes" index="analyzed">
  <xsl:for-each select="row">
    <xsl:for-each select="cell|hcell">
      <xsl:variable name="text" select="descendant::text()"/>
      <xsl:if test="string-length(normalize-space($text)) gt 0">
        <xsl:value-of select="$text"/><xsl:text> | </xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:for-each>
</field>
</xsl:template>

</xsl:stylesheet>
