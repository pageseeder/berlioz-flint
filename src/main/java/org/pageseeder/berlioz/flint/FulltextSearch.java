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
import org.pageseeder.flint.search.Facet;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class FulltextSearch extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(FulltextSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    SearchQuery query = this.buildQuery(req);
    SearchPaging paging = this.buildPaging(req);
    ArrayList<Index> theIndexes = new ArrayList<Index>();
    for (IndexMaster index : indexes) {
      theIndexes.add(index.getIndex());
    }
    try {
      IndexManager manager = FlintConfig.getManager();
      SearchResults results = manager.query(theIndexes, query, paging);
      List<Facet> facetsList = manager.getFacets(Arrays.asList(facets.split(",")), 10, query.toQuery(), theIndexes);
      this.outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    SearchQuery query = this.buildQuery(req);
    SearchPaging paging = this.buildPaging(req);
    IndexManager manager = FlintConfig.getManager();
    try {
      SearchResults results = index.query(query, paging);
      List<Facet> facetsList = manager.getFacets(Arrays.asList(facets.split(",")), 10, query.toQuery(), index.getIndex());
      this.outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  private SearchQuery buildQuery(ContentRequest req) {
    String field = req.getParameter("field", "fulltext");
    TermParameter term = new TermParameter(field, req.getParameter("term"));
    LOGGER.debug("Search for " + (Object) term);
    List<SearchParameter> params = new ArrayList<SearchParameter>();
    String category = req.getParameter("category");
    if (category != null) {
      params.add(new TermParameter("category", category));
    }
    BasicQuery<TermParameter> query = BasicQuery.newBasicQuery(term, params);
    query.setSort(new Sort(new SortField(null, SortField.Type.SCORE)));
    return query;
  }

  private SearchPaging buildPaging(ContentRequest req) {
    SearchPaging paging = new SearchPaging();
    int page = req.getIntParameter("page", 1);
    paging.setPage(page);
    paging.setHitsPerPage(100);
    return paging;
  }

  private void outputResults(SearchQuery query, SearchResults results, List<Facet> facets, XMLWriter xml) throws IOException {
    for (Facet facet : facets) {
      facet.toXML(xml);
    }
    xml.openElement("index-search", true);
    query.toXML(xml);
    results.toXML(xml);
    xml.writeXML("<content-type>search-result</content-type>");
    xml.closeElement();
  }
}
