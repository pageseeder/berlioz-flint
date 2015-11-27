<?xml version="1.0" encoding="UTF-8"?>
<!--
  Global template invoked by Berlioz

  @author Christophe Lauret
  @version 2 September 2014
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ps="http://www.pageseeder.com/editing/2.0"
                xmlns:bf="http://weborganic.org/Berlioz/XSLT/Function"
                exclude-result-prefixes="#all">

<!-- Common stylesheets -->
<xsl:import href="common/admin.xsl"/>

<!-- By convention, we organise by [group]/[service].xsl -->
<xsl:import href="default/index.xsl"/>

<!-- General Output properties (HTML5) -->
<xsl:output method="html" encoding="utf-8" indent="yes" undeclare-prefixes="no" media-type="text/html" />

<!--
  Main template called in all cases.
-->
<xsl:template match="/">
<!-- Display the HTML Doctype -->
<xsl:text disable-output-escaping="yes"><![CDATA[<!doctype html>
]]></xsl:text>
  <html>
    <head>
      <title>Index Admininistration</title>
      <xsl:call-template name="global-styles" />
      <xsl:for-each select="root/content[@name='bundles']/style">
        <link rel="stylesheet" href="{@src}"></link>
      </xsl:for-each>
      <link rel="shortcut icon" href="{root/location/@base}/favicon.ico"></link>
    </head>
    <body class="bz-group-{root/@group} bz-service-{root/@service}">
      <xsl:apply-templates mode="body"/>
      <xsl:call-template name="global-scripts" />
      <xsl:for-each select="root/content[@name='bundles']/script">
        <script src="{@src}"></script>
      </xsl:for-each>
    </body>
  </html>
</xsl:template>

<!--
  Defines the structure of the default template.
-->
<xsl:template match="/root" mode="body">
<div id="berlioz-container" class="bz-container">
  <!-- Header -->
  <header id="berlioz-header" role="banner">
    <div>
      <h1><a href="/index.html" title="allette">Flint Sample App</a></h1>
      <xsl:apply-templates select="content[@target='header']"/>
      <xsl:apply-templates select="content[@target='navigation']"/>
    </div>
  </header>
  <!-- Main containers -->
  <main id="berlioz-main" role="main">
    <xsl:apply-templates select="content[@target='main']" mode="content" />
  </main>
  <!-- Footer -->
  <footer id="berlioz-footer" role="contentinfo">Copyright &#169; 2014 &#8211; Sydney, Australia &#8211;</footer>
</div>
</xsl:template>

<!-- Error handling -->
<xsl:template match="content[@target='main'][berlioz-exception]" mode="content">
  <h2>Internal Server Error</h2>
  <p>We're sorry, but we are not able to display the page you are trying to reach.</p>
  <p>We're working on getting this fixed as soon as we can.</p>
  <a id="moredetails" href="#">More details</a>
  <div id="error-details" style="display: none;">
  <dl>
    <dt>Generator:</dt>
    <dd><xsl:value-of select="@generator"/></dd>
    <dt>Name:</dt>
    <dd><xsl:value-of select="@name"/></dd>
    <dt>Message:</dt>
    <dd><xsl:value-of select="berlioz-exception/cause"/></dd>
    <dt>Stack Trace:</dt>
    <dd class="stacktrace"><pre>
      <xsl:value-of select="berlioz-exception/stack-trace"/>
    </pre></dd>
  </dl>
  <script src="/script/error.js"/>
</div>
</xsl:template>

</xsl:stylesheet>
