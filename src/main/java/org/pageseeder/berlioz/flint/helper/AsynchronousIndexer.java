package org.pageseeder.berlioz.flint.helper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
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
import org.pageseeder.berlioz.flint.util.Files;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexManager;
import org.pageseeder.flint.local.LocalIndexer;
import org.pageseeder.flint.local.LocalIndexer.Action;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousIndexer implements Runnable, XMLWritable, FileFilter {

  /**
   * private logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousIndexer.class);

  private final IndexMaster _index;

  private String folder = null;

  private Date modifiedAfter = null;

  private String pathRegex = null;

  private long started = -1;

  private boolean done = false;

  private LocalIndexer indexer = null;

  private boolean useIndexDate = true;

  private final static ExecutorService threads = Executors.newCachedThreadPool();

  private final static Map<String, AsynchronousIndexer> indexers = new ConcurrentHashMap<>();

  /**
   * Create a new indexer for the index provided.
   * @param index
   */
  public AsynchronousIndexer(IndexMaster index) {
    this._index = index;
  }

  /**
   * Set a date that is the lower limit for the last modified date of the files to index.
   * 
   * @param modifiedAfter the date
   */
  public void setModifiedAfter(Date modifiedAfter) {
    this.modifiedAfter = modifiedAfter;
  }

  /**
   * Set a Regex that the path of the files to index must match.
   * It is relative to the folder specified.
   * 
   * @param regex the regular expression
   */
  public void setPathRegex(String regex) {
    if (regex == null || regex.isEmpty()) this.pathRegex = null;
    else this.pathRegex = regex.replaceFirst("^/", ""); // remove first '/'
  }

  /**
   * If the index last modified date is used to select which files to index
   * @param useIndxDate whether or not to use index last modif date
   */
  public void setUseIndexDate(boolean useIndxDate) {
    this.useIndexDate = useIndxDate;
  }
  
  /**
   * the root folder of the files to index.
   * 
   * @param afolder the folder, relative to the index's content root
   */
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
        // folder and regex
        if (lm != null && src != null && path != null &&
            path.startsWith(afolder) && 
            (this.pathRegex == null || path.substring(afolder.length()).matches(this.pathRegex))) {
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
    this.indexer.setFileFilter(this);
    this.indexer.setUseIndexDate(this.useIndexDate);
    this.indexer.indexFolder(root, existing);

    // mark as finished
    this.done = true;
  }

  /**
   * File filter method
   */
  @Override
  public boolean accept(File file) {
    // check with index's file filter
    if (!this._index.getIndexingFileFilter().accept(file))
      return false;
    // now check with regex
    if (this.pathRegex != null) {
      File root = new File(this._index.getContent(), this.folder == null ? "/" : this.folder);
      String path = Files.path(root, file);
      if (path != null && !path.matches(this.pathRegex))
        return false;
    }
    // last check is date
    return this.modifiedAfter == null || file.lastModified() > this.modifiedAfter.getTime();
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
