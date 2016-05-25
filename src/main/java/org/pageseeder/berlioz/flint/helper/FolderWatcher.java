package org.pageseeder.berlioz.flint.helper;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexDefinition;
import org.pageseeder.berlioz.flint.model.IndexMaster;
import org.pageseeder.berlioz.util.FileUtils;
import org.pageseeder.berlioz.util.Pair;
import org.pageseeder.flint.IndexException;
import org.pageseeder.flint.IndexJob.Priority;
import org.pageseeder.flint.api.Requester;
import org.pageseeder.flint.local.LocalFileContentType;
import org.pageseeder.flint.local.LocalIndex;
import org.pageseeder.flint.query.BasicQuery;
import org.pageseeder.flint.query.PrefixParameter;
import org.pageseeder.flint.query.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(FolderWatcher.class);

  private final File _root;
  private final int _maxFolders;
  private final DelayedIndexer _delayedIndexer;
  private final ExecutorService _indexThread;

  /**
   * @param root          the root folder
   * @param maxFolders    the max nb of folders to watch (-1 means unlimited)
   * @param indexingDelay the delay between a file change and its indexing
   */
  public FolderWatcher(File root, int maxFolders, int indexingDelay) {
    this._maxFolders = maxFolders;
    this._root = root;
    if (indexingDelay > 0) {
      this._delayedIndexer = new DelayedIndexer(indexingDelay);
      this._indexThread = Executors.newSingleThreadExecutor();
    } else {
      this._delayedIndexer = null;
      this._indexThread = null;
    }
  }
  
  /**
   * Start the folder watcher
   */
  public void start() {
    LOGGER.debug("Starting watcher on root folder {} (max folders is {}) ", this._root.getAbsolutePath(), this._maxFolders);
    // start delayed indexing if setup
    if (this._delayedIndexer != null) {
      this._indexThread.execute(this._delayedIndexer);
    }
    // go through folder hierarchy to add watchers
    FileTreeCrawler crawler = new FileTreeCrawler(this._root.toPath(), null, new WatchListener() {
      @Override
      public void received(Path path, Kind<Path> kind) {
        // if deleted, file does not exist anymore so
        // can't check for folder, use filename TODO is this the best?
        if (kind == ENTRY_DELETE && path.getFileName().toString().indexOf('.') == -1) {
          folderDeleted(path.toFile());
        } else if (kind == ENTRY_CREATE && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
          folderAdded(path.toFile());
        } else if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
          fileChanged(path.toFile());
        }
      }
    }, this._maxFolders);
    // start crawling
    try {
      crawler.start();
    } catch (IOException ex) {
      LOGGER.error("Failed to start watcher", ex);
    }
  }

  /**
   * Stop delayed indexing thread if setup.
   */
  public void stop() {
    if (this._delayedIndexer != null) {
      this._delayedIndexer.stop();
      this._indexThread.shutdown();
    }
  }

  /**
   * When a file was changed on the file system.
   * 
   * @param file the modified file
   */
  private void fileChanged(File file) {
    LOGGER.debug("File changed {}", file);
    FlintConfig config = FlintConfig.get();
    IndexMaster destination = getDestination(file, config);
    // index it if there's a destination
    if (destination != null) {
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        config.getManager().index(file.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(),
                                  new Requester("Berlioz File Watcher"), Priority.HIGH, null);
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        this._delayedIndexer.index(destination.getIndex(), file.getAbsolutePath());
      }
    }
  }

  private void folderAdded(File file) {
    LOGGER.debug("Folder added {}", file);
    FlintConfig config = FlintConfig.get();
    IndexMaster destination = getDestination(file, config);
    // index it if there's a destination
    if (destination != null) {
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        Requester req = new Requester("Berlioz File Watcher");
        for (File afile : file.listFiles())
          config.getManager().index(afile.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(), req, Priority.HIGH, null);
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        for (File afile : file.listFiles())
          this._delayedIndexer.index(destination.getIndex(), afile.getAbsolutePath());
      }
    }
  }

  private void folderDeleted(File file) {
    LOGGER.debug("Folder deleted {}", file);
    FlintConfig config = FlintConfig.get();
    IndexMaster destination = getDestination(file, config);
    // index it if there's a destination
    if (destination != null) {
      Collection<File> toReIndex = new ArrayList<File>();
      // find all files located in there
      try {
        String path = destination.fileToPath(file);
        SearchResults results = destination.query(BasicQuery.newBasicQuery(new PrefixParameter(new Term("_path", path))));
        for (Document doc : results.documents()) {
          toReIndex.add(destination.pathToFile(doc.get("_path")));
        }
        results.terminate();
      } catch (IndexException ex) {
        LOGGER.error("Failed to load files in folder {}", file, ex);
        return;
      }
      // delayed indexing?
      if (this._delayedIndexer == null) {
        LOGGER.debug("Re-indexing file {}", file);
        Requester req = new Requester("Berlioz File Watcher");
        for (File afile : toReIndex)
          config.getManager().index(afile.getAbsolutePath(), LocalFileContentType.SINGLETON, destination.getIndex(), req, Priority.HIGH, null);
      } else {
        LOGGER.debug("Delay re-indexing of file {}", file);
        for (File afile : toReIndex)
          this._delayedIndexer.index(destination.getIndex(), afile.getAbsolutePath());
      }
    }
  }

  private IndexMaster getDestination(File file, FlintConfig config) {
    // find which index that file is in
    for (IndexMaster master : config.listIndexes()) {
      if (master.isInIndex(file)) {
        return master;
      }
    }
    // no index, check the configs then
    String path = '/' + FileUtils.path(GlobalSettings.getRepository(), file);
    for (IndexDefinition def : config.listDefinitions()) {
      String name = def.findIndexName(path);
      if (name != null) {
        // create new index
        return config.getMaster(name);
      }
    }
    return null;
  }
  /**
   * Delayed indexing thread.
   * A loop checks every second for all the files in the list.
   * If a file's delay has expired, it is indexed then removed from the list.
   */
  private static class DelayedIndexer implements Runnable {

    /** if the thread has been stopped */ 
    private boolean keepGoing = true;
    /** the indexing delay (in seconds) */
    private final int _delay;
    /** the list of delayed files */
    private final Map<Pair<String, LocalIndex>, AtomicInteger> _delayedIndexing = new ConcurrentHashMap<>(100, 0.8f);

    /**
     * @param delay the indexing delay in seconds
     */
    public DelayedIndexer(int delay) {
      this._delay = delay;
    }

    /**
     * Mark the thread so that is will stop when possible next.
     */
    public void stop() {
      this.keepGoing = false;
    }

    /**
     * Add a new file to be indexed
     * @param index the index
     * @param file  the file
     */
    public void index(LocalIndex index, String path) {
      Pair<String, LocalIndex> key = new Pair<String, LocalIndex>(path, index);
      AtomicInteger delay = this._delayedIndexing.get(key);
      // add it if not there
      if (delay == null)
        this._delayedIndexing.put(key, new AtomicInteger(this._delay));
      // reset otherwise
      else delay.set(this._delay);
    }

    @Override
    public void run() {
      FlintConfig config = FlintConfig.get();
      bigloop: while (this.keepGoing) {
        // check every second
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          if (!this.keepGoing) break bigloop;
          LOGGER.error("Failed to wait 1s in delayed indexer", ex);
          break;
        }
        // loop through delayed files
        for (Pair<String, LocalIndex> toIndex : new ArrayList<>(this._delayedIndexing.keySet())) {
          AtomicInteger timer = this._delayedIndexing.get(toIndex);
          if (timer.decrementAndGet() == 0) {
            LOGGER.debug("Re-indexing file {} after delay", toIndex.first());
            // index now
            config.getManager().index(
                toIndex.first(), LocalFileContentType.SINGLETON,
                toIndex.second(), new Requester("Berlioz File Watcher Delayed Indexer"), Priority.HIGH, null);
            // delete it
            this._delayedIndexing.remove(toIndex);
          }
          if (!this.keepGoing) break bigloop;
        }
      }
    }
  }
  
}
