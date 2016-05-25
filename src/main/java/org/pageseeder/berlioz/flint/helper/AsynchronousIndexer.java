package org.pageseeder.berlioz.flint.helper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.flint.util.FileFilters;
import org.pageseeder.berlioz.flint.util.Files;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexBatch;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.local.LocalIndexer.Action;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousIndexer implements Runnable, XMLWritable {

  /**
   * private logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousIndexer.class);

  private final IndexMaster _index;

  private String folder = null;

  private long started = -1;

  private boolean done = false;

  private LocalIndexer indexer = null;

  private final static ExecutorService threads = Executors.newCachedThreadPool();

  private final static Map<String, AsynchronousIndexer> indexers = new ConcurrentHashMap<>();
  
  public AsynchronousIndexer(IndexMaster index) {
    this._index = index;
  }

  public void setFolder(String afolder) {
    this.folder = afolder;
  }

  public boolean start() {
    // only one active thread per index
    AsynchronousIndexer indexer = getIndexer(_index);
    if (indexer != null) {
      if (!indexer.done) return false;
    }
    // put new one
    indexers.put(this._index.getName(), this);
    threads.execute(this);
    return true;
  }

  @Override
  public void run() {
    // set started time
    this.started = System.currentTimeMillis();

    String afolder = this.folder == null ? "/" : this.folder;

     // load existing documents
    IndexManager manager = FlintConfig.get().getManager();
    Map<File, Long> existing = new HashMap<>();
    IndexReader reader;
    try {
      reader = manager.grabReader(this._index.getIndex());
    } catch (IndexException ex) {
      LOGGER.error("Failed to retrieve logger for index {}", this._index.getName(), ex);
      return;
    }
    try {
      for (int i = 0; i < reader.numDocs(); i++) {
        Document doc = reader.document(i);
        String src = doc.get("_src");
        String path = doc.get("_path");
        String lm   = doc.get("_lastmodified");
        if (src != null && path != null && path.startsWith(afolder) && lm != null) {
          try {
            existing.put(new File(src), Long.valueOf(lm));
          } catch (NumberFormatException ex) {
            // ignore, should never happen anyway
          }
        }
      }
    } catch (IOException ex) {
      LOGGER.error("Failed to load existing documents from index {}", this._index.getName(), ex);
    } finally {
      manager.releaseQuietly(this._index.getIndex(), reader);
    }

    // find root folder
    File root = new File(this._index.getContent(), afolder);

    // use local indexer
    this.indexer = new LocalIndexer(manager, this._index.getIndex());
    this.indexer.setFileFilter(FileFilters.getPSMLFiles());
    this.indexer.indexFolder(root, existing);

    // mark as finished
    this.done = true;
  }

  public Map<File, Action> getFiles() {
    return this.indexer == null ? null : this.indexer.getIndexedFiles();
  }

  public IndexBatch getBatch() {
    return this.indexer == null ? null : this.indexer.getBatch();
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("indexer");
    if (this.started > 0) xml.attribute("started", ISO8601.DATETIME.format(this.started));
    xml.attribute("index", this._index.getName());
    xml.attribute("completed", String.valueOf(this.done));
    if (this.folder != null) xml.attribute("folder", this.folder);
    if (this.indexer != null) {
      // batch
      BatchXMLWriter.batchToXML(this.indexer.getBatch(), xml);
      // files
      Map<File, Action> files = this.indexer.getIndexedFiles();
      xml.openElement("files");
      xml.attribute("count", files.size());
      File root = GlobalSettings.getRepository();
      int max = 100;
      int current = 0;
      for (File file : files.keySet()) {
        xml.openElement("file");
        try {
          xml.attribute("path", '/'+Files.path(root, file));
        } catch (IllegalArgumentException ex) {
          xml.attribute("path", file.getAbsolutePath());
        }
        xml.attribute("action", files.get(file).name().toLowerCase());
        xml.closeElement();
        if (current++ > max) break;
      }
      xml.closeElement();
    }
    xml.closeElement();
  }

  // static methods
  public static AsynchronousIndexer getIndexer(IndexMaster index) {
    return indexers.get(index.getName());
  }

}
