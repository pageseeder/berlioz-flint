package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.pageseeder.berlioz.flint.util.FileFilters;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.local.LocalIndexConfig;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.query.SearchPaging;
import org.pageseeder.flint.query.SearchQuery;
import org.pageseeder.flint.query.SearchResults;
import org.pageseeder.flint.query.SuggestionQuery;
import org.pageseeder.flint.util.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public final class IndexMaster extends LocalIndexConfig {

  /**
   * private logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexMaster.class);
  private final IndexManager _manager;
  private final String _name;
  private final File _indexRoot;
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
    this._indexRoot = index;
    this._index = new LocalIndex(this);
    this._index.setTemplate(extension, template.toURI());
  }

  public void setTemplate(File template) throws TransformerException {
    setTemplate("psml", template);
  }

  public void setTemplate(String extension, File template) throws TransformerException {
    this._index.setTemplate(extension, template.toURI());
  }
  
  public boolean isInIndex(File file) {
    return FileUtils.contains(_contentRoot, file);
  }

  public LocalIndex getIndex() {
    return this._index;
  }

  public String getName() {
    return this._name;
  }

  public void clear() {
    Requester requester = new Requester("clear berlioz index");
    this._manager.clear(this._index, requester, IndexJob.Priority.HIGH);
  }

  public SearchResults query(SearchQuery query) throws IndexException {
    return this._manager.query(this._index, query);
  }

  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return this._manager.query(this._index, query, paging);
  }

  public long lastModified() {
    return this._manager.getLastTimeUsed(this._index);
  }

  public SearchResults getSuggestions(List<String> fields, List<String> texts, int max, String predicate) throws IOException, IndexException {
    List<Term> terms = Terms.terms(fields, texts);
    Query condition = IndexMaster.toQuery(predicate);
    SuggestionQuery query = new SuggestionQuery(terms, condition);
    IndexReader reader = null;
    try {
      reader = this._manager.grabReader(this._index);
      query.compute(this._index, reader);
    } finally {
      this.releaseSilently(reader);
    }
    SearchPaging pages = new SearchPaging();
    if (max > 0) {
      pages.setHitsPerPage(max);
    }
    return this._manager.query(this._index, (SearchQuery) query, pages);
  }

  public static Query toQuery(String predicate) throws IndexException {
    QueryParser parser = new QueryParser("type", FlintConfig.newAnalyzer());
    Query condition = null;
    if (predicate != null && !"".equals(predicate)) {
      try {
        condition = parser.parse(predicate);
      } catch (ParseException ex) {
        throw new IndexException("Condition for the suggestion could not be parsed.", ex);
      }
    }
    return condition;
  }

  public IndexReader grabReader() throws IndexException {
    return this._manager.grabReader(this._index);
  }

  public IndexSearcher grabSearcher() throws IndexException {
    return this._manager.grabSearcher(this._index);
  }

  public void releaseSilently(IndexReader reader) {
    this._manager.releaseQuietly(this._index, reader);
  }

  public void releaseSilently(IndexSearcher searcher) {
    this._manager.releaseQuietly(this._index, searcher);
  }

  public int indexFolder(String afolder) throws IOException, IndexException {

    // find root folder
    String folder = afolder == null ? "/" : afolder;
    File root = new File(this._contentRoot, folder);
    
     // load existing documents
    Map<File, Long> existing = new HashMap<>();
    IndexReader reader = this._manager.grabReader(this._index);
    try {
      for (int i = 0; i < reader.numDocs(); i++) {
        String path = reader.document(i).get("_path");
        String lm   = reader.document(i).get("_lastmodified");
        if (path != null && path.startsWith(folder) && lm != null) {
          try {
            existing.put(pathToFile(path), Long.valueOf(lm));
          } catch (NumberFormatException ex) {
            // ignore, should never happen anyway
          }
        }
      }
    } finally {
      this._manager.release(this._index, reader);
    }

    // use local indexer
    LocalIndexer indexer = new LocalIndexer(this._manager, this._index);
    indexer.setFileFilter(FileFilters.getPSMLFiles());
    return indexer.indexFolder(root, existing);
  }

  // -------------------------------------------------------------------------------
  // local index config methods
  // -------------------------------------------------------------------------------
  
  @Override
  public Analyzer getAnalyzer() {
    return FlintConfig.newAnalyzer();
  }

  @Override
  public File getIndexLocation() {
   return this._indexRoot;
  }
  
  @Override
  public File getContent() {
    return this._contentRoot;
  }

  @Override
  public DeleteRule getDeleteRule(File f) {
    try {
      return new DeleteRule("_src", f.getCanonicalPath());
    } catch (IOException ex) {
      LOGGER.error("Failed to compute canonical path for {}", f, ex);
      return null;
    }
  }

  @Override
  public Map<String, String> getParameters(File file) {
    HashMap<String, String> params = new HashMap<>();
    if (file.exists()) {
      try {
        params.put("_src", file.getCanonicalPath());
      } catch (IOException ex) {
        LOGGER.error("Failed to compute canonical path for {}", file, ex);
      }
      params.put("_path", fileToPath(file));
      params.put("_filename", file.getName());
      params.put("_visibility", "private");
      params.put("_lastmodified", String.valueOf(file.lastModified()));
    }
    return params;
  }
  
  private String fileToPath(File f) {
    String path = FileUtils.path(_contentRoot, f);
    return path == null ? f.getAbsolutePath() : '/'+path.replace('\\', '/').replaceFirst("\\.(psml|PSML)$", "");
  }
  
  private File pathToFile(String path) {
    return new File(this._contentRoot, path+".psml");
  }
}