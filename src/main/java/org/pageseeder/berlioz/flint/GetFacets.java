/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.search.Query
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.Cacheable
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.berlioz.content.ContentStatus
 *  org.pageseeder.berlioz.util.MD5
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.search.Facet
 *  org.pageseeder.xmlwriter.XMLWriter
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.search.Facet;
import org.pageseeder.flint.search.FieldFacet;
import org.pageseeder.flint.util.Facets;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GetFacets extends IndexGenerator implements Cacheable {

  private static final Logger LOGGER = LoggerFactory .getLogger(GetFacets.class);

  @Override
  public String getETag(ContentRequest req) {
    StringBuilder etag = new StringBuilder();
    etag.append(req.getParameter("base", "")).append('%');
    etag.append(req.getParameter("facets", "")).append('%');
    etag.append(req.getParameter("max-number", "20")).append('%');
    etag.append(buildIndexEtag(req));
    return MD5.hash(etag.toString());
  }

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String base = req.getParameter("base", "");
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-number", 20);
    if (facets.isEmpty() && base.isEmpty()) {
      xml.emptyElement("facets");
      return;
    }
    Query query = null;
    if (!base.isEmpty()) {
      query = buildQuery(base, req, xml);
      if (query == null) return;
    }
    ArrayList<Index> theIndexes = new ArrayList<Index>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    try {
      IndexManager manager = FlintConfig.get().getManager();
      List<FieldFacet> facetsList = query == null ?
            Facets.getFacets(manager, facets.isEmpty() ? null : Arrays.asList(facets.split(",")), maxNumber, theIndexes) :
            Facets.getFacets(manager, Arrays.asList(facets.split(",")), maxNumber, query, theIndexes);
      this.outputResults(base, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String base = req.getParameter("base", "");
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-number", 20);
    if (facets.isEmpty() && base.isEmpty()) {
      xml.emptyElement("facets");
      return;
    }
    Query query = null;
    if (!base.isEmpty()) {
      query = buildQuery(base, req, xml);
      if (query == null) return;
    }
    IndexManager manager = FlintConfig.get().getManager();
    try {
      List<FieldFacet> facetsList = Facets.getFacets(manager, facets.isEmpty() ? null : Arrays.asList(facets.split(",")), maxNumber, query, index.getIndex());
      this.outputResults(base, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  public Query buildQuery(String base, ContentRequest req, XMLWriter xml) throws IOException {
    Query query;
    try {
      query = IndexMaster.toQuery(base);
    } catch (IndexException ex) {
      LOGGER.error("Unable to parse query", (Throwable) ex);
      xml.openElement("error");
      xml.attribute("type", "invalid-parameter");
      xml.attribute("message", "Unable to create query from condition " + base);
      xml.closeElement();
      req.setStatus(ContentStatus.BAD_REQUEST);
      return null;
    }
    LOGGER.debug("Computing facets for {}", (Object) base);
    return query;
  }

  public void outputResults(String base, Collection<FieldFacet> facets, XMLWriter xml) throws IOException {
    xml.openElement("facets");
    xml.attribute("for", base);
    for (Facet facet : facets) {
      facet.toXML(xml);
    }
    xml.closeElement();
  }
}
