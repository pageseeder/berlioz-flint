/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.search.Query
 *  org.apache.lucene.search.Sort
 *  org.apache.lucene.search.SortField
 *  org.apache.lucene.search.SortField$Type
 *  org.pageseeder.berlioz.BerliozException
 *  org.pageseeder.berlioz.content.ContentRequest
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.query.BasicQuery
 *  org.pageseeder.flint.query.SearchPaging
 *  org.pageseeder.flint.query.SearchParameter
 *  org.pageseeder.flint.query.SearchQuery
 *  org.pageseeder.flint.query.SearchResults
 *  org.pageseeder.flint.query.TermParameter
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

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.query.BasicQuery;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchParameter;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.TermParameter;
import org.pageseeder.flint.search.FieldFacet;
import org.pageseeder.flint.util.Facets;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supported parameters are:
 *   facets
 *   field (default fulltext)
 *   term
 *   with
 *   sort
 *   page
 *   results
 */
public class BasicSearch extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    SearchQuery query = buildQuery(req);
    if (query == null) {
      xml.emptyElement("index-search");
      return;
    }
    SearchPaging paging = buildPaging(req);
    ArrayList<Index> theIndexes = new ArrayList<Index>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    try {
      IndexManager manager = FlintConfig.get().getManager();
      SearchResults results = manager.query(theIndexes, query, paging);
      List<FieldFacet> facetsList = Facets.getFacets(manager, Arrays.asList(facets.split(",")), 10, query.toQuery(), theIndexes);
      outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    SearchQuery query = buildQuery(req);
    if (query == null) {
      xml.emptyElement("index-search");
      return;
    }
    SearchPaging paging = buildPaging(req);
    IndexManager manager = FlintConfig.get().getManager();
    try {
      SearchResults results = index.query(query, paging);
      List<FieldFacet> facetsList = Facets.getFacets(manager, Arrays.asList(facets.split(",")), 10, query.toQuery(), index.getIndex());
      outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  private SearchQuery buildQuery(ContentRequest req) {
    String field = req.getParameter("field", "fulltext");
    String typed = req.getParameter("term");
    if (typed == null) return null;
    TermParameter term = new TermParameter(field, typed);
    LOGGER.debug("Search in field {} for term {}", field, term);
    List<SearchParameter> params = new ArrayList<SearchParameter>();
    String with = req.getParameter("with");
    if (with != null) {
      for (String w : with.split(",")) {
        String[] both = w.split(":");
        if (both.length == 2) {
          LOGGER.debug("Adding facet with field {} and term {}", both[0], both[1]);
          params.add(new TermParameter(both[0], both[1]));
        } else {
          LOGGER.warn("Ignoring invalid facet {}", w);
        }
      }
    }
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(term, params);
    query.setSort(new Sort(new SortField(req.getParameter("sort"), SortField.Type.SCORE)));
    return query;
  }

  private SearchPaging buildPaging(ContentRequest req) {
    SearchPaging paging = new SearchPaging();
    int page = req.getIntParameter("page", 1);
    int results = req.getIntParameter("results", 100);
    paging.setPage(page);
    paging.setHitsPerPage(results);
    return paging;
  }

  private void outputResults(SearchQuery query, SearchResults results, List<FieldFacet> facets, XMLWriter xml) throws IOException {
    xml.openElement("index-search", true);
    xml.openElement("facets");
    for (FieldFacet facet : facets) {
      facet.toXML(xml);
    }
    xml.closeElement();
    query.toXML(xml);
    results.toXML(xml);
    xml.closeElement();
  }
}
