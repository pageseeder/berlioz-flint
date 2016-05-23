/*
 * Decompiled with CFR 0_110.
 * 
 * Could not load the following classes:
 *  org.apache.lucene.analysis.Analyzer
 *  org.apache.lucene.analysis.standard.StandardAnalyzer
 *  org.pageseeder.berlioz.GlobalSettings
 *  org.pageseeder.flint.IndexManager
 *  org.pageseeder.flint.api.ContentFetcher
 *  org.pageseeder.flint.api.ContentTranslator
 *  org.pageseeder.flint.api.ContentTranslatorFactory
 *  org.pageseeder.flint.api.IndexListener
 *  org.pageseeder.flint.content.SourceForwarder
 *  org.pageseeder.flint.local.LocalFileContentFetcher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.pageseeder.berlioz.flint.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.flint.helper.FolderWatcher;
import org.pageseeder.berlioz.flint.helper.QuietListener;
import org.pageseeder.berlioz.flint.model.IndexDefinition.InvalidIndexDefinitionException;
import org.pageseeder.flint.IndexBatch;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.ContentTranslator;
import org.pageseeder.flint.api.ContentTranslatorFactory;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flint config in berlioz config:
 *    <flint>
 *      <watcher [watch="true|false"] [root="/psml/content"] [max-folders="1000"] />
 *      <threads [number="10"] [priority="5"] />
 *      <index types="default,books,schools,products">
 *        <default  name="default"     path="/psml/content/"            />
 *        <books    name="book-{name}" path="/psml/content/book/{name}" template="book.xsl"/>
 *        <schools  name="school"      path="/psml/content/schools"     template="school.xsl"/>
 *        <products name="product"     path="/psml/content/products"    [template="products.xsl"]/>
 *      </index>
 *    </flint>
 * 
 * @author jbreure
 *
 */
public class FlintConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlintConfig.class);
  protected static final String DEFAULT_INDEX_NAME = "default";
  protected static final String DEFAULT_INDEX_LOCATION = "index";
  protected static final String DEFAULT_CONTENT_LOCATION = "/psml/content";
  protected static final String DEFAULT_ITEMPLATES_LOCATION = "ixml";
  protected static final int DEFAULT_MAX_WATCH_FOLDERS = 100000;
  protected static final int DEFAULT_WATCHER_DELAY_IN_SECONDS = 5;
  private static volatile AnalyzerFactory analyzerFactory = null;
  private final File _directory;
  private final File _ixml;
  private static FlintConfig SINGLETON = null;
  private final QuietListener listener;
  private final IndexManager manager;
  private final Map<String, IndexDefinition> indexConfigs = new HashMap<>();
  private final Map<String, IndexMaster> indexes = new HashMap<>();
  private final FolderWatcher watcher;
  private static ContentTranslatorFactory TRANSLATORS = new ContentTranslatorFactory() {

    public ContentTranslator createTranslator(String mimeType) {
      if ("psml".equals(mimeType))
        return new SourceForwarder(mimeType, "UTF-8");
      return null;
    }

    public Collection<String> getMimeTypesSupported() {
      ArrayList<String> mimes = new ArrayList<String>();
      mimes.add("psml");
//      mimes.add("xml");
      return mimes;
    }
  };

  public static void setupFlintConfig(File index, File ixml) {
    SINGLETON = new FlintConfig(index, ixml);
  }

  public static synchronized FlintConfig get() {
    if (SINGLETON == null) SINGLETON = FlintConfig.buildDefaultConfig();
    return SINGLETON;
  }

  public IndexManager getManager() {
    return this.manager;
  }

  public IndexMaster getMaster() {
    return getMaster(DEFAULT_INDEX_NAME);
  }

  public IndexMaster getMaster(String name) {
    String key = name == null ? DEFAULT_INDEX_NAME : name;
    // make sure only one gets created
    synchronized (this.indexes) {
      if (!this.indexes.containsKey(key)) {
        IndexDefinition def = getIndexDefinitionFromIndexName(key);
        if (def == null) {
          // no config found
          LOGGER.error("Failed to create index {}, no matching index definition found in configuration", key);
        } else {
          IndexMaster master = createMaster(key, def);
          if (master != null) this.indexes.put(key, master);
        }
      }
      return this.indexes.get(key);
    }
  }

  /**
   * Close and removes index from list. Also deletes index files from index root folder.
   * 
   * @param name the index name
   * 
   * @return true if completely removed
   */
  public boolean deleteMaster(String name) {
    String key = name == null ? DEFAULT_INDEX_NAME : name;
    if (this.indexes.containsKey(key)) {
      // close index
      IndexMaster master = this.indexes.remove(name);
      this.manager.closeIndex(master.getIndex());
      // remove files
      File root = new File(GlobalSettings.getRepository(), DEFAULT_INDEX_LOCATION + File.separator + name);
      if (root.exists() && root.isDirectory()) {
        for (File f : root.listFiles()) {
          f.delete();
        }
        return root.delete();
      }
      return true; // ???
    }
    return false;
  }

  private static FlintConfig buildDefaultConfig() {
    File index = new File(GlobalSettings.getRepository(), DEFAULT_INDEX_LOCATION);
    File ixml  = new File(GlobalSettings.getRepository(), DEFAULT_ITEMPLATES_LOCATION);
    if (!index.exists()) {
      index.mkdirs(); 
    }
    return new FlintConfig(index, ixml);
  }

  private FlintConfig(File directory, File ixml) {
    this._directory = directory;
    this._ixml = ixml;
    // manager
    int nbThreads      = GlobalSettings.get("flint.threads.number",   10);
    int threadPriority = GlobalSettings.get("flint.threads.priority", 5);
    this.listener = new QuietListener(LOGGER);
    this.manager = new IndexManager(new LocalFileContentFetcher(), this.listener, nbThreads, false);
    this.manager.setThreadPriority(threadPriority);
    this.manager.registerTranslatorFactory(TRANSLATORS);
    // watch is on?
    boolean watch = GlobalSettings.get("flint.watcher.watch", true);
    if (watch) {
      File root         = new File(GlobalSettings.getRepository(), GlobalSettings.get("flint.watcher.root", DEFAULT_CONTENT_LOCATION));
      int maxFolders    = GlobalSettings.get("flint.watcher.max-folders", DEFAULT_MAX_WATCH_FOLDERS);
      int indexingDelay = GlobalSettings.get("flint.watcher.delay",       DEFAULT_WATCHER_DELAY_IN_SECONDS);
      this.watcher = new FolderWatcher(root, maxFolders, indexingDelay);
      this.watcher.start();
    } else {
      this.watcher = null;
    }
    // load index definitions
    String types = GlobalSettings.get("flint.index.types", "default");
    for (String type : types.split(",")) {
      String indexName  = GlobalSettings.get("flint.index."+type+".name", DEFAULT_INDEX_NAME);
      String path       = GlobalSettings.get("flint.index."+type+".path", DEFAULT_CONTENT_LOCATION);
      File template = new File(ixml, GlobalSettings.get("flint.index."+type+".template", indexName+".xsl"));
      IndexDefinition def;
      try {
        def = new IndexDefinition(type, indexName, path, template);
        LOGGER.debug("New index config for {} with index name {}, path {} and template {}", type, indexName, path, template.getAbsolutePath());
      } catch (InvalidIndexDefinitionException ex) {
        LOGGER.warn("Ignoring invalid index definition {}: {}", type, ex.getMessage());
        continue;
      }
      // autosuggests
      loadAutoSuggests(def);
      this.indexConfigs.put(type, def);
    }
  }

  /**
   * Stop the watcher if there is one and the manager.
   */
  public final void stop() {
    // stop watcher if there is one
    if (this.watcher != null)
      this.watcher.stop();
    // shutdown all indexes
    for (IndexMaster index : this.indexes.values()) {
      index.close();
    }
    // stop everything
    this.manager.stop();
  }

  public final File getRootDirectory() {
    return this._directory;
  }

  public final File getTemplatesDirectory() {
    return this._ixml;
  }

  public Collection<IndexMaster> listIndexes() {
    if (this.indexes.isEmpty()) {
      // load indexes
      for (File folder : this._directory.listFiles()) {
        if (folder.isDirectory()) {
          if (!folder.getName().endsWith("_autosuggest"))
            getMaster(folder.getName());
        } else {
          // delete all files from old index
          folder.delete();
        }
      }
    }
    return this.indexes.values();
  }

  public Collection<IndexDefinition> listDefinitions() {
    return new ArrayList<IndexDefinition>(this.indexConfigs.values());
  }

  public static synchronized void setAnalyzerFactory(AnalyzerFactory factory) {
    analyzerFactory = factory;
  }

  public static synchronized Analyzer newAnalyzer() {
    if (analyzerFactory == null) return new StandardAnalyzer();
    return analyzerFactory.getAnalyzer();
  }

  public IndexDefinition getIndexDefinition(String defname) {
    return this.indexConfigs.get(defname);
  }

  public IndexDefinition getIndexDefinitionFromIndexName(String indexname) {
    if (indexname == null) return null;
    // find config
    for (IndexDefinition config : this.indexConfigs.values()) {
      // name matches?
      if (config.indexNameMatches(indexname))
        return config;
    }
    return null;
  }

  public void reloadTemplate(String defname) {
    IndexDefinition def = getIndexDefinition(defname);
    if (def != null) {
      // loop through index folders
      for (File folder : this._directory.listFiles()) {
        if (folder.isDirectory() && def.indexNameMatches(folder.getName())) {
          IndexMaster master = getMaster(folder.getName());
          try {
            master.reloadTemplate();
            def.setTemplateError(null); // reset error
          } catch (TransformerException ex) {
            def.setTemplateError(ex.getMessageAndLocation());
            return;
          }
        }
      }
    }
  }

  public Collection<IndexBatch> getPastBatches() {
    return this.listener.getBatches();
  }

  private IndexMaster createMaster(String name, IndexDefinition def) {
    // build content path
    File content = def.buildContentRoot(GlobalSettings.getRepository(), name);
    File index   = new File(GlobalSettings.getRepository(), DEFAULT_INDEX_LOCATION + File.separator + name);
    try {
      IndexMaster master = IndexMaster.create(getManager(), name, content, index, def);
      def.setTemplateError(null); // reset error
      return master;
    } catch (TransformerException ex) {
      def.setTemplateError(ex.getMessageAndLocation());
      return null;
    }
  }

  private void loadAutoSuggests(IndexDefinition def) {
    String propPrefix = "flint.index."+def.getName()+'.';
    // autosuggests
    String autosuggests = GlobalSettings.get(propPrefix+"autosuggests");
    if (autosuggests != null) {
      for (String autosuggest : autosuggests.split(",")) {
        String fields  = GlobalSettings.get(propPrefix+autosuggest+".fields");
        String terms   = GlobalSettings.get(propPrefix+autosuggest+".terms", "false");
        String rfields = GlobalSettings.get(propPrefix+autosuggest+".result-fields");
        if (fields != null) {
          def.addAutoSuggest(autosuggest, fields, terms, rfields);
        } else {
          LOGGER.warn("Ignoring invalid autosuggest definition for {}: fields {}, terms {}, result fields {}", autosuggest, fields, terms, rfields);
        }
      }
    }
  }
}
