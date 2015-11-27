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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.flint.helper.QuietListener;
import org.pageseeder.berlioz.flint.model.AnalyzerFactory;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.api.ContentFetcher;
import org.pageseeder.flint.api.ContentTranslator;
import org.pageseeder.flint.api.ContentTranslatorFactory;
import org.pageseeder.flint.api.IndexListener;
import org.pageseeder.flint.content.SourceForwarder;
import org.pageseeder.flint.local.LocalFileContentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flint config in berlioz config:
 *    <flint [watch="true|false"]>
 *      <index types="default,books,schools,products">
 *        <default name="default"     path="/psml/content/"            />
 *        <books   name="book-{name}" path="/psml/content/book/{name}" template="book.xsl"/>
 *        <schools name="school"      path="/psml/content/schools"     template="school.xsl"/>
 *        <products name="product"    path="/psml/content/products"/>
 *      </index>
 *    </flint>
 * 
 * @author jbreure
 *
 */
public class FlintConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlintConfig.class);
  protected static final String SINGLE_INDEX_ID = "frizzen";
  protected static final String DEFAULT_INDEX_LOCATION = "index";
  protected static final String DEFAULT_ITEMPLATES_LOCATION = "ixml";
  protected static final String DEFAULT_TEMPLATES_NAME = "default.xsl";
  protected static final File PUBLIC = GlobalSettings.getRepository().getParentFile();
  public static final File PRIVATE_XML = new File(GlobalSettings.getRepository(), "xml");
  public static final File PRIVATE_PSML = new File(GlobalSettings.getRepository(), "psml");
  private static volatile AnalyzerFactory analyzerFactory = null;
  private final File _directory;
  private final File _ixml;
  private static FlintConfig SINGLETON = null;
  private static IndexManager MANAGER = null;
  private static Map<String, IndexMaster> indexes = null;
  private static final String DEFAULT_INDEX_NAME = null;
  private static ContentTranslatorFactory TRANSLATORS = new ContentTranslatorFactory() {

    public ContentTranslator createTranslator(String mimeType) {
      if ("psml".equals(mimeType) || "xml".equals(mimeType)) {
        return new SourceForwarder(mimeType, "UTF-8");
      }
      return null;
    }

    public Collection<String> getMimeTypesSupported() {
      ArrayList<String> mimes = new ArrayList<String>();
      if (GlobalSettings.get((String) "flint.supports.psml", (boolean) true)) {
        mimes.add("psml");
      }
      if (GlobalSettings.get((String) "flint.supports.xml", (boolean) true)) {
        mimes.add("xml");
      }
      return mimes;
    }
  };

  public static IndexManager getManager() {
    if (MANAGER == null) {
      int nbThreads = GlobalSettings.get((String) "flint.threads.max",
          (int) 10);
      int threadPriority = GlobalSettings.get((String) "flint.threads.priority",
          (int) 5);
      MANAGER = new IndexManager((ContentFetcher) new LocalFileContentFetcher(),
          (IndexListener) new QuietListener(LOGGER), nbThreads);
      MANAGER.registerTranslatorFactory(TRANSLATORS);
      MANAGER.setThreadPriority(threadPriority);
    }
    return MANAGER;
  }

  public static IndexMaster getMaster() {
    return FlintConfig.getMaster(DEFAULT_INDEX_NAME);
  }

  public static IndexMaster getMaster(String name) {
    String key = name == null ? DEFAULT_INDEX_NAME : name;
    if (indexes == null) {
      indexes = new HashMap<String, IndexMaster>();
    }
    if (!indexes.containsKey(key)) {
      indexes.put(key, new IndexMaster(key));
    }
    return indexes.get(key);
  }

  public static FlintConfig get() {
    if (SINGLETON == null) {
      SINGLETON = FlintConfig.buildDefaultConfig();
    }
    return SINGLETON;
  }

  private static FlintConfig buildDefaultConfig() {
    File index = new File(GlobalSettings.getRepository(), "index");
    File ixml = new File(GlobalSettings.getRepository(), "ixml");
    // watch is on?
    boolean watch = GlobalSettings.get("flint.watch", true);
    // load index definitions
    List<IndexConfig> indexes = new ArrayList<IndexConfig>();
    String types = GlobalSettings.get("flint.index.types", "default");
    for (String type : types.split(",")) {
      IndexConfig cfg = new IndexConfig();
      cfg.configName = type;
      cfg.indexName  = GlobalSettings.get("flint.index."+type+".name", "default");
      cfg.path       = GlobalSettings.get("flint.index."+type+".path", "/psml/content");
      cfg.template   = GlobalSettings.get("flint.index."+type+".template", "default.xsl");
      indexes.add(cfg);
    }
    return new FlintConfig(index, ixml);
  }

  public static void setupFlintConfig(File index, File ixml) {
    SINGLETON = new FlintConfig(index, ixml);
  }

  private FlintConfig(File directory, File ixml) {
    this._directory = directory;
    this._ixml = ixml;
  }

  public final File getRootDirectory() {
    return this._directory;
  }

  public final File getTemplatesDirectory() {
    return this._ixml;
  }

  public static synchronized void setAnalyzerFactory(AnalyzerFactory factory) {
    analyzerFactory = factory;
  }

  public static synchronized Analyzer newAnalyzer() {
    if (analyzerFactory == null) {
      return new StandardAnalyzer();
    }
    return analyzerFactory.getAnalyzer();
  }

  private static class IndexConfig {
    private String configName;
    private String indexName;
    private String path;
    private String template;
  }
}
