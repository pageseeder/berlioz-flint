/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.analysis.Analyzer
 *  org.apache.lucene.index.IndexReader
 *  org.apache.lucene.queryparser.classic.ParseException
 *  org.apache.lucene.queryparser.classic.QueryParser
 *  org.apache.lucene.search.IndexSearcher
 *  org.apache.lucene.search.Query
 *  org.pageseeder.berlioz.util.FileUtils
 *  org.pageseeder.berlioz.util.ISO8601
 *  org.pageseeder.flint.IndexException
 *  org.pageseeder.flint.IndexJob
 *  org.pageseeder.flint.IndexJob$Priority
 *  org.pageseeder.flint.api.ContentType
 *  org.pageseeder.flint.api.Index
 *  org.pageseeder.flint.api.Requester
 *  org.pageseeder.flint.local.LocalFileContentType
 *  org.pageseeder.flint.local.LocalIndex
 *  org.pageseeder.flint.query.SearchPaging
 *  org.pageseeder.flint.query.SearchQuery
 *  org.pageseeder.flint.query.SearchResults
 *  org.pageseeder.flint.query.SuggestionQuery
 *  org.pageseeder.flint.util.Terms
 */
package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.api.ContentType;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.local.LocalFileContentType;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.SuggestionQuery;
import org.pageseeder.flint.util.Terms;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class IndexMaster {
  private final LocalIndex _index;

  protected IndexMaster(String name) {
    FlintConfig cfg = FlintConfig.get();
    File dir = name == null ? cfg.getRootDirectory() : new File(cfg.getRootDirectory(), name);
    this._index = new LocalIndex(dir, FlintConfig.newAnalyzer());
  }

  public Index getIndex() {
    return this._index.getIndex();
  }

  public LocalIndex getLocalIndex() {
    return this._index;
  }

  public void indexFile(File file, Map<String, String> parameters) {
    if (file.isFile()) {
      Requester requester = IndexMaster.buildRequester("index berlioz file", parameters);
      FlintConfig.getManager().index(file.getAbsolutePath(),
          (ContentType) LocalFileContentType.SINGLETON, this._index.getIndex(),
          requester, IndexJob.Priority.HIGH);
    }
  }

  public void clear() {
    Requester requester = new Requester("clear berlioz index");
    FlintConfig.getManager().clear(this._index.getIndex(), requester, IndexJob.Priority.HIGH);
  }

  public SearchResults query(SearchQuery query) throws IndexException {
    return FlintConfig.getManager().query(this._index.getIndex(), query);
  }

  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return FlintConfig.getManager().query(this._index.getIndex(), query, paging);
  }

  public long lastModified() {
    return FlintConfig.getManager().getLastTimeUsed(this._index.getIndex());
  }

  public SearchResults getSuggestions(List<String> fields, List<String> texts, int max, String predicate) throws IOException, IndexException {
    List<Term> terms = Terms.terms(fields, texts);
    Query condition = IndexMaster.toQuery(predicate);
    SuggestionQuery query = new SuggestionQuery(terms, condition);
    IndexReader reader = null;
    try {
      reader = FlintConfig.getManager().grabReader(this._index.getIndex());
      query.compute(this._index.getIndex(), reader);
    } finally {
      this.releaseSilently(reader);
    }
    SearchPaging pages = new SearchPaging();
    if (max > 0) {
      pages.setHitsPerPage(max);
    }
    return FlintConfig.getManager().query(this._index.getIndex(), (SearchQuery) query, pages);
  }

  public static Query toQuery(String predicate) throws IndexException {
    QueryParser parser = new QueryParser("type", FlintConfig.newAnalyzer());
    Query condition = null;
    if (predicate != null && !"".equals(predicate)) {
      try {
        condition = parser.parse(predicate);
      } catch (ParseException ex) {
        throw new IndexException(
            "Condition for the suggestion could not be parsed.",
            (Exception) ex);
      }
    }
    return condition;
  }

  public IndexReader grabReader() throws IndexException {
    return FlintConfig.getManager().grabReader(this._index.getIndex());
  }

  public IndexSearcher grabSearcher() throws IndexException {
    return FlintConfig.getManager().grabSearcher(this._index.getIndex());
  }

  public void releaseSilently(IndexReader reader) {
    FlintConfig.getManager().releaseQuietly(this._index.getIndex(), reader);
  }

  public void releaseSilently(IndexSearcher searcher) {
    FlintConfig.getManager().releaseQuietly(this._index.getIndex(), searcher);
  }

  private static Requester buildRequester(String name, final Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return new Requester(name);
    }
    return new Requester(name) {
      public Map<String, String> getParameters(String path, ContentType type) {
        File f = new File(path);
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("path", path);
        p.put("visibility", "private");
        p.put("mediatype", FileUtils.getMediaType((File) f));
        p.put("last-modified", ISO8601.format((long) f.lastModified(), (ISO8601) ISO8601.DATETIME));
        if (parameters != null) {
          p.putAll(parameters);
        }
        return p;
      }
    };
  }

}
