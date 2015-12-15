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

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.flint.helper.FileTreeWatcher;
import org.pageseeder.berlioz.flint.helper.QuietListener;
import org.pageseeder.berlioz.flint.helper.WatchListener;
import org.pageseeder.berlioz.flint.model.IndexDefinition.InvalidIndexDefinitionException;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.flint.IndexBatch;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.ContentTranslator;
import org.pageseeder.flint.api.ContentTranslatorFactory;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.pageseeder.flint.local.LocalFileContentType;
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
  protected static final int DEFAULT_MAX_WATCH_FOLDERS = 1000;
  protected static final File PUBLIC = GlobalSettings.getRepository().getParentFile();
  private static volatile AnalyzerFactory analyzerFactory = null;
  private final File _directory;
  private final File _ixml;
  private static FlintConfig SINGLETON = null;
  private final QuietListener listener;
  private final IndexManager manager;
  private final Map<String, IndexDefinition> indexConfigs = new HashMap<>();
  private final Map<String, IndexMaster> indexes = new HashMap<>();
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

  public static FlintConfig get() {
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
    return new FlintConfig(index, ixml);
  }

  private FlintConfig(File directory, File ixml) {
    this._directory = directory;
    this._ixml = ixml;
    // manager
    int nbThreads      = GlobalSettings.get("flint.threads.number",   10);
    int threadPriority = GlobalSettings.get("flint.threads.priority", 5);
    this.listener = new QuietListener(LOGGER);
    this.manager = new IndexManager(new LocalFileContentFetcher(), this.listener, nbThreads);
    this.manager.setThreadPriority(threadPriority);
    this.manager.registerTranslatorFactory(TRANSLATORS);
    // watch is on?
    boolean watch = GlobalSettings.get("flint.watcher.watch", true);
    if (watch) {
      File root = new File(GlobalSettings.getRepository(), GlobalSettings.get("flint.watcher.root", DEFAULT_CONTENT_LOCATION));
      startWatching(root, GlobalSettings.get("flint.watcher.max-folders", DEFAULT_MAX_WATCH_FOLDERS));
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

  public final File getRootDirectory() {
    return this._directory;
  }

  public final File getTemplatesDirectory() {
    return this._ixml;
  }

  public Collection<IndexMaster> listIndexes() {
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

  /**
   * Start the folder watcher
   */
  private void startWatching(File root, int maxFolders) {
    FileTreeWatcher watcher = new FileTreeWatcher(root.toPath(), null, new WatchListener() {
      @Override
      public void received(Path path, Kind<Path> kind) {
        if (path.toFile().isFile() || kind == ENTRY_DELETE)
          fileChanged(path.toFile());
      }
    }, maxFolders);
    try {
      watcher.start();
    } catch (IOException ex) {
      LOGGER.error("Failed to start watcher", ex);
    }
  }

  /**
   * When a file was changed on the file system.
   * 
   * @param file the modified file
   */
  private void fileChanged(File file) {
    LOGGER.debug("File changed {}", file);
    // find which index that file is in
    IndexMaster destination = null;
    for (IndexMaster master : this.indexes.values()) {
      if (master.isInIndex(file)) {
        destination = master;
        // we're done
        break;
      }
    }
    // found it?
    if (destination == null) {
      // no index, check the configs then
      String path = '/' + FileUtils.path(GlobalSettings.getRepository(), file);
      for (IndexDefinition def : this.indexConfigs.values()) {
        String name = def.findIndexName(path);
        if (name != null) {
          // create new index
          destination = createMaster(name, def);
          // store it
          this.indexes.put(name, destination);
          break;
        }
      }
    }
    
    // index it if there's a destination
    if (destination != null) {
      this.manager.index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(),
                         new Requester("Berlioz File Watcher"), Priority.HIGH);
    } else {
      // log it?
      LOGGER.debug("Modified file does not belong to any index {}", file);
    }
  }

  private final static Collection<String> VALID_AUTOSUGGEST_TYPES = Arrays.asList(new String[] {"documents", "fields", "terms"});
  private void loadAutoSuggests(IndexDefinition def) {
    String propPrefix = "flint.index."+def.getName()+'.';
    // autosuggests
    String autosuggests = GlobalSettings.get(propPrefix+"autosuggests");
    if (autosuggests != null) {
      for (String autosuggest : autosuggests.split(",")) {
        String fields = GlobalSettings.get(propPrefix+autosuggest+".fields");
        String type   = GlobalSettings.get(propPrefix+autosuggest+".type", "fields");
        if (fields != null && VALID_AUTOSUGGEST_TYPES.contains(type)) {
          def.addAutoSuggest(autosuggest, fields, type);
        } else {
          LOGGER.warn("Ignoring invalid autosuggest definition {} with fields {} and type {}", autosuggest, fields, type);
        }
      }
    }
  }
}
