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
import org.apache.lucene.search.SortField.Type;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.catalog.Catalog;
import org.pageseeder.flint.catalog.Catalogs;
import org.pageseeder.flint.query.BasicQuery;
import org.pageseeder.flint.query.PhraseParameter;
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
 * 
 *   facets      comma-separated list of fields to use as facets
 *   max-facets  the max number of values for a facet (default is 20)
 *   field       the field being searched (default fulltext)
 *   term        the text that is searched
 *   with        comma-separated list of facets values with the format [field]:[value]
 *   sort        field name, if starting with "-", the order is reversed
 *   sort-type   [int|double|float|long|document|string|score], default is score
 *   page        the page number
 *   results     the nb of results per page
 *   
 */
public class BasicSearch extends IndexGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicSearch.class);

  @Override
  public void processMultiple(Collection<IndexMaster> indexes, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-facets", 20);
    // get first catalog as they should all have the same one
    SearchQuery query = buildQuery(req, indexes.iterator().next().getCatalog());
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
      List<FieldFacet> facetsList = Facets.getFacets(manager, Arrays.asList(facets.split(",")), maxNumber, query.toQuery(), theIndexes);
      outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  @Override
  public void processSingle(IndexMaster index, ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String facets = req.getParameter("facets", "");
    int maxNumber = req.getIntParameter("max-facets", 20);
    SearchQuery query = buildQuery(req, index.getCatalog());
    if (query == null) {
      xml.emptyElement("index-search");
      return;
    }
    SearchPaging paging = buildPaging(req);
    IndexManager manager = FlintConfig.get().getManager();
    try {
      SearchResults results = index.query(query, paging);
      List<FieldFacet> facetsList = Facets.getFacets(manager, Arrays.asList(facets.split(",")), maxNumber, query.toQuery(), index.getIndex());
      outputResults(query, results, facetsList, xml);
    } catch (IndexException ex) {
      LOGGER.warn("Fail to retrieve search result using query: {}",
          (Object) query.toString(), (Object) ex);
    }
  }

  private SearchQuery buildQuery(ContentRequest req, String catalog) {
    String field = req.getParameter("field", "fulltext");
    String typed = req.getParameter("term");
    if (typed == null) return null;
    // compute parameters
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
    // check if we should tokenize
    Catalog theCatalog = Catalogs.getCatalog(catalog);
    boolean tokenize = theCatalog != null && theCatalog.isTokenizedForSearch(field);
    BasicQuery<?> query;
    if (tokenize) {
      query = BasicQuery.newBasicQuery(new PhraseParameter(field, typed), params);
      LOGGER.debug("Search in field {} for phrase {}", field, typed);
    } else {
      query = BasicQuery.newBasicQuery(new TermParameter(field, typed), params);
      LOGGER.debug("Search in field {} for term {}", field, typed);
    }
    Sort sort = buildSort(req);
    if (sort != null) query.setSort(sort);
    return query;
  }

  private Sort buildSort(ContentRequest req) {
    // field name
    String field = req.getParameter("sort");
    if (field == null) return null;
    // sort type
    SortField.Type sortType = null;
    String type  = req.getParameter("sort-type", "score");
    if ("int".equalsIgnoreCase(type))           sortType = Type.INT;
    else if ("double".equalsIgnoreCase(type))   sortType = Type.DOUBLE;
    else if ("float".equalsIgnoreCase(type))    sortType = Type.FLOAT;
    else if ("long".equalsIgnoreCase(type))     sortType = Type.LONG;
    else if ("document".equalsIgnoreCase(type)) sortType = Type.DOC;
    else if ("string".equalsIgnoreCase(type))   sortType = Type.STRING;
    else sortType = Type.SCORE;
    // reverse order
    boolean reverse = field.startsWith("-");
    if (reverse) field = field.substring(1);
    // build sort field
    return new Sort(new SortField(field, sortType, reverse));
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
