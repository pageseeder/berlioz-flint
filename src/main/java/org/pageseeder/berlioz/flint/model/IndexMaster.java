package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Index;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.SuggestionQuery;
import org.pageseeder.flint.util.Terms;

/**
 * 
 */
public final class IndexMaster {
  private final IndexManager _manager;
  private final String _name;
  private final File _contentRoot;
  private final LocalIndex _index;

  public static IndexMaster create(IndexManager mgr, String name,
         File content, File index, File template) throws TransformerException {
    return create(mgr, name, content, index, "psml", template);
  }

  public static IndexMaster create(IndexManager mgr, String name,
         File content, File index, String extension, File template) throws TransformerException {
    return new IndexMaster(mgr, name, content, index, extension, template);
  }

  private IndexMaster(IndexManager mgr, String name, File content,
      File index, String extension, File template) throws TransformerException {
    this._manager = mgr;
    this._name = name;
    this._contentRoot = content;
    this._index = new LocalIndex(index, this._contentRoot, FlintConfig.newAnalyzer());
    this._index.setTemplate(extension, template.toURI());
  }

  public boolean isInIndex(File file) {
    return FileUtils.contains(_contentRoot, file);
  }

  public Index getIndex() {
    return this._index.getIndex();
  }

  public LocalIndex getLocalIndex() {
    return this._index;
  }

  public File getContentRoot() {
    return this._contentRoot;
  }

  public String getName() {
    return this._name;
  }

  public void clear() {
    Requester requester = new Requester("clear berlioz index");
    this._manager.clear(this._index.getIndex(), requester, IndexJob.Priority.HIGH);
  }

  public SearchResults query(SearchQuery query) throws IndexException {
    return this._manager.query(this._index.getIndex(), query);
  }

  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return this._manager.query(this._index.getIndex(), query, paging);
  }

  public long lastModified() {
    return this._manager.getLastTimeUsed(this._index.getIndex());
  }

  public SearchResults getSuggestions(List<String> fields, List<String> texts, int max, String predicate) throws IOException, IndexException {
    List<Term> terms = Terms.terms(fields, texts);
    Query condition = IndexMaster.toQuery(predicate);
    SuggestionQuery query = new SuggestionQuery(terms, condition);
    IndexReader reader = null;
    try {
      reader = this._manager.grabReader(this._index.getIndex());
      query.compute(this._index.getIndex(), reader);
    } finally {
      this.releaseSilently(reader);
    }
    SearchPaging pages = new SearchPaging();
    if (max > 0) {
      pages.setHitsPerPage(max);
    }
    return this._manager.query(this._index.getIndex(), (SearchQuery) query, pages);
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
    return this._manager.grabReader(this._index.getIndex());
  }

  public IndexSearcher grabSearcher() throws IndexException {
    return this._manager.grabSearcher(this._index.getIndex());
  }

  public void releaseSilently(IndexReader reader) {
    this._manager.releaseQuietly(this._index.getIndex(), reader);
  }

  public void releaseSilently(IndexSearcher searcher) {
    this._manager.releaseQuietly(this._index.getIndex(), searcher);
  }

}
