<?xml version="1.0"?>
<!--

  @author Christophe Lauret
  @version 26 September 2011
-->
<xsl:stylesheet version="2.0" xmlns="http://www.w3.org/1999/xhtml"
                              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:dec="java:java.net.URLDecoder"
                              xmlns:ps="http://www.pageseeder.com/editing/2.0"
                              xmlns:bf="http://weborganic.org/Berlioz/XSLT/Function"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              exclude-result-prefixes="xsl ps dec bf xs">

<!--
  Returns an overview of the index configuration
-->
<xsl:template match="content[@name='indexes']" mode="content">
  <xsl:choose>
    <xsl:when test="count(indexes/index) = 1 and indexes/index/@name = 'default'">
      <h2>Single Index configuration</h2>
      <a href="/index/default.html" class="button tiny">Access index</a>
    </xsl:when>
    <xsl:otherwise>
      <h2>Multiple Index configuration</h2>
      <h3><xsl:value-of select="count(index)"/> indexes</h3>
      <div id="index-list">
        <table class="basic indexes">
          <thead>
            <th>Index name</th>
            <th>Last modified</th>
            <th>Current</th>
            <th>Version</th>
            <th>Nb Docs</th>
          </thead>
          <tbody>
            <xsl:for-each select="indexes/index"> 
              <xsl:sort select="lower-case(@name)"/>
              <tr>
                <td><a href="/index/{@name}.html"><xsl:value-of select="@name"/></a></td>
                <td><xsl:sequence select="if (@modified) then bf:datetime-diff(@modified) else '--'"/></td>
                <td><xsl:sequence select="@current"/></td>
                <td><xsl:sequence select="@version"/></td>
                <td><xsl:sequence select="documents/@count"/></td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
      </div>
      <div class="create"><a href="/index/create.html" class="button tiny create-index">Create New Index</a></div>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
<!--
  Returns an overview of the index configuration
-->
<xsl:template match="content[@name='index-config']" mode="content">
  <xsl:for-each select="flint-config">
    <h2>Index Configuration</h2>
    <section id="config-status">
    <xsl:apply-templates select="../../content[@name='index-templates']" mode="appadmin-aside"/>
    <xsl:apply-templates select="../../content[@name='index-queue']" mode="appadmin-aside"/>
    </section>
    <h3><xsl:value-of select="count(index)"/> indexes</h3>
    <div id="index-list">
    <table class="basic indexes">
      <thead>
        <th>Index name</th>
        <th>Last modified</th>
      </thead>
      <tbody>
        <xsl:for-each select="index"> 
          <xsl:sort select="lower-case(@name)"/>
          <tr>
            <td><a href="/appadmin/index/{@name}/overview.html"><xsl:value-of select="@name"/></a></td>
            <td><xsl:sequence select="if (@modified) then bf:datetime-diff(@modified) else '--'"/></td>
          </tr>
        </xsl:for-each>
      </tbody>
    </table>
    </div>
    <div class="create"><a href="/appadmin/index/create.html" class="button tiny create-index">Create New Index</a></div>
  </xsl:for-each>
</xsl:template>

<!--
  Returns list of index templates
-->
<xsl:template match="content[@name='index-templates']" mode="appadmin-aside">
<aside id="indexing-templates">
  <h4>Indexing templates</h4>
  <table class="basic">
    <thead>
      <th>Name</th>
      <th>Status</th>
    </thead>
    <tbody>
      <xsl:for-each select="index-templates/templates">
        <tr>
          <td><xsl:value-of select="@filename"/></td>
          <td class="{@status}"><xsl:value-of select="@status"/></td>
        </tr>
      </xsl:for-each>
    </tbody>
  </table>
</aside>
</xsl:template>

<!--
  Returns list of index templates
-->
<xsl:template match="content[@name='index-queue']" mode="appadmin-aside">
<aside id="indexing-queue">
  <h4>Indexing queue <a class="toggle-poll" title="start/stop" href="#"><i class="fa fa-refresh" /></a></h4>
  <span class="chart" title="Number of job in indexing Queue"></span>
  <p>Jobs in Queue: <span id="queue-size"><xsl:value-of select="index-jobs/@count"/></span></p>
</aside>
</xsl:template>

<!--
  Returns a quick summary of the index
-->
<xsl:template match="content[@name='index-summary']" mode="content">
  <xsl:choose>
    <xsl:when test="index-summary/index/@exists= 'false'"><p>No index.</p></xsl:when>
    <xsl:otherwise>
    <xsl:for-each select="index-summary/index">
      <xsl:variable name="indexname" select="@name"/>
      <h2>Index: <xsl:value-of select="@name"/><a id="refresh-summary" title="refresh" href="#"><i class="fa fa-refresh" /></a></h2>
      <section class="panel">
        <div class="inner row">
          <div class="medium-4 columns">
            <table class="valuepairs indexproperties">
              <tbody>
                <tr><td>Last modified</td><td><xsl:sequence select="bf:last-modified(@last-modified)"/></td></tr>
                <tr><td>Current</td><td><xsl:value-of select="if (@current = 'true') then 'Yes' else 'No'"/></td></tr>
                <tr><td>Optimized</td><td><xsl:value-of select="if (@optimized = 'true') then 'Yes' else 'No'"/></td></tr>
                <tr><td>Documents</td><td><xsl:value-of select="documents/@count"/></td></tr>
                <xsl:for-each select="//content[@name='index-templates']/index-templates">
                <tr><td>IXML Templates</td>
                  <td class="{@status}">
                    <xsl:value-of select="if (@status = 'ok') then 'OK' else message"/>
                  </td></tr>
                </xsl:for-each>
              </tbody>
            </table>
          </div>
          <div class="medium-8 columns">
            <div class="actions">
              <form action="/appadmin/index/{@name}/generate.html" method="POST" id="update-index">
                <button type="submit" class="button tiny">Index updated files</button>
              </form>
              <form action="/appadmin/index/{@name}/generate.html" method="POST" id="reindex-index">
                <input type="hidden" name="all" value="true" />
                <button type="submit" class="button tiny">Index ALL files</button>
              </form>
              <xsl:if test="@name != 'dictionary'">
                <form action="/appadmin/index/{@name}.html" method="DELETE" id="clear-index">
                  <button type="submit" class="strong button alert tiny">Clear Index</button>
                </form>
              </xsl:if>
            </div>
            <form action="/appadmin/index/{@name}/search.html" class="search-form">
              <div class="row inner">
                <div class="small-10 columns">
                  <input name="predicate" type="text" value="" placeholder="Enter a Lucene predicate" required="required" size="60"/>
                </div>
                <div class="small-2 columns"><button type="submit" class="button tiny">Search</button></div>
              </div>
            </form>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="inner row">
          <div class="medium-4 columns">
            <h3>Fields</h3>
            <ul class="fields">
              <xsl:for-each select="fields/field">
                <xsl:sort select="@name"/>
                <li><a href="/appadmin/index/{$indexname}/terms/{@name}.html"><xsl:value-of select="@name"/></a></li>
              </xsl:for-each>
            </ul>
          </div>
          <div class="medium-8 columns terms">
          </div>
        </div>
      </section>
    </xsl:for-each>
    <div class="clear"/>
  </xsl:otherwise>
</xsl:choose>
</xsl:template>

<!-- Displays the index terms -->
<xsl:template match="content[@name='index-terms']" mode="content">
<section>
  <xsl:if test="not(terms/@field)">
    <h3>Terms</h3>
  </xsl:if>
  <xsl:for-each select="terms">
    <xsl:variable name="terms" select="term"/>
    <xsl:variable name="unique-fields" select="distinct-values($terms/@field)"/>
    <xsl:variable name="index" select="@index"/>
    <xsl:for-each select="$unique-fields">
      <xsl:variable name="t" select="$terms[@field = current()]"/>
      <h4>Field: <xsl:value-of select="."/></h4>
      <table class="basic field-info">
      <thead><tr><th>Field name</th><th>Unique Terms</th><th>Occurrences</th><th>ID</th></tr></thead>
      <tbody>
        <xsl:for-each select="$unique-fields">
          <xsl:variable name="t" select="$terms[@field = current()]"/>
          <tr>
            <td><xsl:value-of select="."/></td>
            <td><xsl:value-of select="count($t)"/></td>
            <td><xsl:value-of select="sum($t/@doc-freq)"/></td>
            <td><xsl:value-of select="if ($t/@doc-freq != 1) then 'No' else 'Yes'"/></td>
          </tr>
        </xsl:for-each>
      </tbody>
      </table>
      <table class="basic term-list" data-field="{.}">
        <thead>
          <tr>
            <th>Term</th>
            <th width="80px">Occurrences</th>
            <th width="20px">&#xA0;</th>
          </tr>
        </thead>
        <tbody>
        <xsl:for-each select="$t">
          <xsl:sort select="@doc-freq" data-type="number" order="descending"/>
          <xsl:sort select="@text"/>
          <tr>
            <td><xsl:sequence select="bf:handle-empty(@text)"/></td>
            <td><xsl:value-of select="@doc-freq"/></td>
            <td><xsl:if test="@text != ''"><a href="/appadmin/index/{$index}/search/{@field}.html?term={encode-for-uri(@text)}"><i class="fa fa-search" /></a></xsl:if></td>
          </tr>
        </xsl:for-each>
        </tbody>
      </table>
    </xsl:for-each>
  </xsl:for-each>
</section>
</xsl:template>


<!--
  Displays the results of an index job.
-->
<xsl:template match="content[@name='generate-index']" mode="content">
  <section>
  <h2>Generate Index</h2>
  <h3>Summary</h3>
  <p>Index files since <b><xsl:value-of select="translate(format-dateTime(index-job/@last-modified, '[D1] [MNn,*-3] [Y] at [h]:[m01][PN]'),'.','')"/></b></p>
  <table>
    <thead><tr><th>Inserted</th><th>Updated</th><th>Deleted</th><th>Ignored</th><th>TOTAL</th></tr></thead>
    <tbody>
      <tr>
        <th><xsl:value-of select="count(index-job/file[@action='INSERT'])"/></th>
        <th><xsl:value-of select="count(index-job/file[@action='UPDATE'])"/></th>
        <th><xsl:value-of select="count(index-job/file[@action='DELETE'])"/></th>
        <th><xsl:value-of select="count(index-job/file[@action='IGNORE'])"/></th>
        <th><xsl:value-of select="count(index-job/file)"/></th>
      </tr>
    </tbody>
  </table>
  <a href="/appadmin/index/{//uri-parameters/parameter[@name='index']}/overview.html" class="button tiny">Return to index overview</a>
  <h3>File list</h3>
  <p>The following files will be processed for indexing:</p>
  <p><a href="#show-ignored" class="show-ignored button tiny">Show ignored files</a></p>
  <xsl:for-each select="index-job">
  <table>
    <thead><tr><th>Path</th><th>Last Modified</th><th>Action</th></tr></thead>
    <tbody>
    <xsl:for-each select="file">
      <xsl:sort select="@path"/>
      <tr class="{lower-case(@action)}"><td><xsl:value-of select="@path"/></td><td><xsl:value-of select="translate(format-dateTime(@last-modified, '[D1] [MNn,*-3] [Y] at [h]:[m01][PN]'),'.','')"/></td><td><xsl:value-of select="@action"/></td></tr>
    </xsl:for-each>
    </tbody>
  </table>
  </xsl:for-each>
  </section>
</xsl:template>


<xsl:template match="content[@name='index-stats']" mode="content">
<section>
  <h2>Index Statistics</h2>
  <xsl:for-each select="index-stats/index">
    <div class="index-stats row" id="stats-{@name}">
      <div class="small-6 columns">
        <table>
          <tbody>
            <tr><td>Name</td><td><xsl:value-of select="@name"/></td></tr>
            <tr><td>Last modified</td><td><xsl:value-of select="format-dateTime(@last-modified, '[H01]:[m01] on [FNn] [D] [MNn] [Y]')"/></td></tr>
            <tr><td>Current</td><td><xsl:value-of select="if (@current = 'true') then 'Yes' else 'No'"/></td></tr>
            <tr><td>Version</td><td><xsl:value-of select="@version"/></td></tr>
            <tr><td>Documents</td><td><xsl:value-of select="documents/@count"/></td></tr>
          </tbody>
        </table>
      </div>
      <div class="small-6 columns">
        <div class="text-right">
          <form action="/index/{@name}/index.html" method="POST">
            <button type="submit" class="button">Index files</button>
          </form>
          <form action="/index/{@name}/clear.html" method="POST">
            <button type="submit" class="button">Clear files</button>
          </form>
          <a href="/index/{@name}/terms.html" class="button show-terms">Show Terms</a>
        </div>
        <form action="/index/{@name}/search.html" class="search">
          <label>Search: <input name="predicate" type="text" placeholder="Enter a Lucene predicate" required="required" size="80"/></label>
          <button type="submit" class="button">Search</button>
        </form>
      </div>
      <div class="results">
      </div>
    </div>
  </xsl:for-each>
  <div class="create"><a href="/index/create.html" class="button create-index">Create New Index</a></div>
</section>
</xsl:template>

<!--
<xsl:template match="content[@name='index-terms']" mode="content">
<h2>Fields and Terms for Index <xsl:value-of select="/root/header/uri-parameters/parameter[@name='index']" /></h2>
<section>
  <h3>Fields</h3>
  <xsl:variable name="terms" select="terms/term"/>
  <xsl:variable name="unique-fields" select="distinct-values($terms/@field)"/>
  <table>
  <thead><tr><th>Field name</th><th>Unique Terms</th><th>Occurrences</th><th>Terms</th></tr></thead>
  <tbody>
    <xsl:for-each select="$unique-fields">
      <xsl:variable name="t" select="$terms[@field = current()]"/>
      <tr>
        <td><xsl:value-of select="."/></td>
        <td><xsl:value-of select="count($t)"/></td>
        <td><xsl:value-of select="sum($t/@doc-freq)"/></td>
        <td><a href="#{.}-terms">list</a></td>
      </tr>
    </xsl:for-each>
  </tbody>
  </table>
  <h3>Terms</h3>
  <xsl:variable name="terms" select="terms/term"/>
  <xsl:variable name="unique-fields" select="distinct-values($terms/@field)"/>
  <xsl:for-each select="$unique-fields">
    <xsl:variable name="t" select="$terms[@field = current()]"/>
    <h4 id="{.}-terms"><xsl:value-of select="."/></h4>
    <div class="terms">
    <table>
      <thead><tr><th>Term</th><th>Occurrences</th></tr></thead>
      <tbody>
      <xsl:for-each select="$t">
        <xsl:sort select="@doc-freq" data-type="number" order="descending"/>
        <xsl:sort select="@text"/>
        <tr><td><xsl:value-of select="@text"/></td><td><xsl:value-of select="@doc-freq"/></td></tr>
      </xsl:for-each>
      </tbody>
    </table>
    </div>
  </xsl:for-each>
</section>
</xsl:template>
 -->

<xsl:template match="content[@name='index-search']" mode="content">
<section>
  <h2>Search</h2>
  <form action="/appadmin/index/search.html">
    <input name="field" value="fulltext" type="hidden"/>
    <label>Predicate: <input name="predicate" type="text" value="{(//predicate)[1]}" placeholder="Enter a Lucene predicate" required="required" size="80"/></label>
    <button type="submit" class="button tiny">Search</button>
  </form>
</section>
<section id="search-results">
  <h3>Search Results</h3>
  <xsl:choose>
    <xsl:when test="not(//search-results)"><p>No results.</p></xsl:when>
    <xsl:otherwise>
      <pre class="xml">
        <xsl:apply-templates select="//search-results" mode="xml"/>
      </pre>
    </xsl:otherwise>
  </xsl:choose>
</section>
</xsl:template>

<xsl:template match="*" mode="xml">
<xsl:param name="level" select="0"/>
<xsl:if test="not(../text())"><xsl:text>
</xsl:text>
<xsl:for-each select="1 to $level"><xsl:text>  </xsl:text></xsl:for-each>
</xsl:if>
<xsl:text>&lt;</xsl:text>
<span class="e"><xsl:value-of select="name()"/></span>
<xsl:apply-templates select="@*" mode="xml"/>
<xsl:text>&gt;</xsl:text>
<xsl:apply-templates select="*|text()" mode="xml">
  <xsl:with-param name="level" select="$level + 1"/>
</xsl:apply-templates>
<xsl:if test="not(text())"><xsl:text>
</xsl:text>
<xsl:for-each select="1 to $level"><xsl:text>  </xsl:text></xsl:for-each>
</xsl:if>
<xsl:text>&lt;/</xsl:text>
<span class="e"><xsl:value-of select="name()"/></span>
<xsl:text>&gt;</xsl:text>
</xsl:template>

<xsl:template match="@*" mode="xml">
<xsl:text> </xsl:text>
<span class="a"><xsl:value-of select="name()"/></span>
<xsl:text>="</xsl:text>
<span class="v"><xsl:value-of select="."/></span>
<xsl:text>"</xsl:text>
</xsl:template>

<xsl:template match="text()" mode="xml">
<span class="t"><xsl:value-of select="."/></span>
</xsl:template>


<xsl:function name="bf:handle-empty">
<xsl:param name="t"/>
<xsl:choose>
  <xsl:when test="$t = ''"><i>(empty string)</i></xsl:when>
  <xsl:when test="$t = ' '"><mark><xsl:value-of select="$t"/></mark> <i>(single space U+0020)</i></xsl:when>
  <xsl:when test="$t = '&#xA0;'"><mark><xsl:value-of select="$t"/></mark> <i>(non-breaking space U+00A0)</i></xsl:when>
  <xsl:when test="not($t = normalize-space($t))">
    <xsl:analyze-string regex="^\s+|\s+$$" select="$t">
      <xsl:matching-substring><mark><xsl:value-of select="."/></mark></xsl:matching-substring>
      <xsl:non-matching-substring><xsl:value-of select="."/></xsl:non-matching-substring>
    </xsl:analyze-string>
    <i>(contains leading/trailing spaces)</i>
  </xsl:when>
  <xsl:otherwise><xsl:value-of select="$t"/></xsl:otherwise>
</xsl:choose>
</xsl:function>

<!--
  Return a long string representation of a duration
  @param time The time in seconds
-->
<xsl:function name="bf:datetime-diff">
  <xsl:param name="datetime" as="xs:dateTime"/>
  <time title="{format-dateTime($datetime, '[D1] [MNn,*-3] [Y0001] at [H01]:[m01]:[s01]')}" datetime="{$datetime}">
    <xsl:variable name="now"      select="current-dateTime()"/>
    <xsl:variable name="past"     select="$datetime lt $now"/>
    <xsl:variable name="diff"     select="if ($past) then $now - $datetime else $datetime - $now"/>
    <xsl:variable name="days"     select="days-from-duration($diff)"/>
    <xsl:variable name="hours"    select="hours-from-duration($diff)"/>
    <xsl:variable name="minutes"  select="minutes-from-duration($diff)"/>
    <xsl:variable name="seconds"  select="round(seconds-from-duration($diff))"/>
    <xsl:if test="not($past)">in </xsl:if>
    <xsl:choose>
      <xsl:when test="$days gt 1"><xsl:value-of select="$days"/> days</xsl:when>
      <xsl:when test="$hours gt 1"><xsl:value-of select="if ($days gt 0) then $days*24 + $hours else $hours"/> hrs</xsl:when>
      <xsl:when test="$minutes gt 1"><xsl:value-of select="if ($hours gt 0) then $hours*60 + $minutes else $minutes"/> mins</xsl:when>
      <xsl:otherwise><xsl:value-of select="if ($minutes gt 0) then $minutes*60 + $seconds else $seconds"/> secs</xsl:otherwise>
    </xsl:choose>
    <xsl:if test="$past"> ago</xsl:if>
  </time>
</xsl:function>

<!--
  Return a long string representation of a duration
  @param time The time in seconds
-->
<xsl:function name="bf:last-modified">
  <xsl:param name="datetime" as="xs:dateTime"/>
  <time title="{format-dateTime($datetime, '[D1] [MNn,*-3] [Y0001] at [H01]:[m01]:[s01]')}" datetime="{$datetime}">
    <xsl:variable name="now"      select="current-dateTime()"/>
    <xsl:variable name="past"     select="$datetime lt $now"/>
    <xsl:variable name="diff"     select="if ($past) then $now - $datetime else $datetime - $now"/>
    <xsl:variable name="days"     select="days-from-duration($diff)"/>
    <xsl:variable name="hours"    select="hours-from-duration($diff)"/>
    <xsl:variable name="minutes"  select="minutes-from-duration($diff)"/>
    <xsl:variable name="seconds"  select="round(seconds-from-duration($diff))"/>
    <xsl:if test="not($past)">in </xsl:if>
    <xsl:choose>
      <xsl:when test="$days gt 730"><xsl:value-of select="$days idiv 365"/> years</xsl:when>
      <xsl:when test="$days gt 60"><xsl:value-of select="$days idiv 30"/> months</xsl:when>
      <xsl:when test="$days gt 14"><xsl:value-of select="$days idiv 7"/> weeks</xsl:when>
      <xsl:when test="$days gt 1"><xsl:value-of select="$days"/> days</xsl:when>
      <xsl:when test="$hours gt 1"><xsl:value-of select="if ($days gt 0) then $days*24 + $hours else $hours"/> hrs</xsl:when>
      <xsl:when test="$minutes gt 1"><xsl:value-of select="if ($hours gt 0) then $hours*60 + $minutes else $minutes"/> mins</xsl:when>
      <xsl:otherwise><xsl:value-of select="if ($minutes gt 0) then $minutes*60 + $seconds else $seconds"/> secs</xsl:otherwise>
    </xsl:choose>
    <xsl:if test="$past"> ago</xsl:if>
  </time>
</xsl:function>


</xsl:stylesheet>
