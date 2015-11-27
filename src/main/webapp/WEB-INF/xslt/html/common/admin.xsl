<!--
  A set templates and functions common to all groups of services on the PageSeeder Main site.

  @author Christophe Lauret
  @version 7 December 2011
-->
<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:ps="http://www.pageseeder.com/editing/2.0"
        xmlns:bf="http://weborganic.org/Berlioz/XSLT/Function"
        exclude-result-prefixes="#all">


<!-- GLOBAL VARIABLES                                                                           -->
<!-- ========================================================================================== -->

<!-- The current URL (provided by Berlioz) -->
<xsl:variable name="service" select="/root/@service"/>


<!-- GLOBAL TEMPLATES                                                                           -->
<!-- ========================================================================================== -->

<!--
  The global styles using CDN
-->
<xsl:template name="global-styles">
  <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/foundation/6.0.1/css/foundation.min.css"></link>
</xsl:template>

<!--
  The global scripts using CDN
-->
<xsl:template name="global-scripts">
  <script src="//code.jquery.com/jquery-1.10.2.min.js"></script>
  <script src="//code.jquery.com/ui/1.10.4/jquery-ui.min.js"></script>
  <script src="//cdnjs.cloudflare.com/ajax/libs/foundation/6.0.1/js/foundation.min.js"></script>
</xsl:template>

<!-- Main navigation -->
<xsl:template match="content[@name='navigation']">
<nav>
  <ul>
    <li>
      <xsl:if test="$service = 'overview'"><xsl:attribute name="class">active</xsl:attribute></xsl:if>
      <a href="/index.html">Index</a>
    </li>
  </ul>
</nav>
</xsl:template>

<!-- GLOBAL FUNCTIONS                                                                           -->
<!-- ========================================================================================== -->


</xsl:stylesheet>
