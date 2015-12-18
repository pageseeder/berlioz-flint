/*
 * Copyright (c) 1999-2014 allette systems pty. ltd.
 */
package org.pageseeder.berlioz.flint.helper;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors a folder and its sub-folders for changes.
 *
 * <p>This class will automatically start a thread, and create the appropriate watchers on the
 * directories found. Events are then reported to the watch listener.
 *
 * @author Christophe Lauret
 *
 * @version 0.1.4
 * @since 0.1.0
 */
public final class FileTreeWatcher implements Runnable {

  /** To know what's going on */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileTreeWatcher.class);

  /** The max nb of keys to store */
  private final int _maxKeys;

  /** The root of the file tree to watch. */
  private final Path _root;

  /** A list of path to ignore within the root. */
  private final List<Path> _ignore;

  /** The listener to report events to. */
  private final WatchListener _listener;

  /** Maintains the status of this watcher. */
  private AtomicBoolean running;

  private WatchService watchService;
  private Thread watchThread;

  /** Maps Watch keys to the watched directory path. */
  private final Map<WatchKey,Path> _keys;

  /**
   * Creates a new watcher.
   *
   * @param root     The root of the file tree to watch.
   * @param ignore   A list of paths to ignore
   * @param listener The listener which receives the events
   */
  public FileTreeWatcher(Path root, List<Path> ignore, WatchListener listener, int max) {
    this._root = root;
    this._ignore = ignore == null ? new ArrayList<Path>() : ignore;
    this._listener = listener;
    this._keys = new HashMap<WatchKey,Path>();
    this._maxKeys = max;

    this.running = new AtomicBoolean(false);

    this.watchService = null;
    this.watchThread = null;
  }

  /**
   * Starts the watcher service and registers watches in all of the sub-folders of
   * the given root folder.
   *
   * <p><b>Important:</b> This method returns immediately, even though the watches
   * might not be in place yet. For large file trees, it might take several seconds
   * until all directories are being monitored. For normal cases (1-100 folders), this
   * should not take longer than a few milliseconds.
   */
  public void start() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
    this.watchThread = new Thread(this, "Watcher");

    this.watchThread.start();
  }

  /**
   * Stops the file tree watcher and any associated thread,
   */
  public synchronized void stop() {
    if (this.watchThread != null) {
      try {
        this.watchService.close();
        this.running.set(false);
        this.watchThread.interrupt();
      } catch (IOException ex) {
        // Don't care
      }
    }
  }

  /**
   * Run method.
   */
  @Override
  public void run() {
    this.running.set(true);
    // add to all sub-folders
    registerAll(this._root);
    // loop
    while (this.running.get()) {
      try {
        // wait for events
        WatchKey key = this.watchService.take();
        Path dir = this._keys.get(key);
        if (dir == null) {
          LOGGER.warn("WatchKey not recognized!!");
          continue;
        }

        // Iterate through events
        for (WatchEvent<?> event: key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == OVERFLOW) { continue; }

          // Context for directory entry event is the file name of entry
          WatchEvent<Path> ev = cast(event);
          Path name = ev.context();
          Path child = dir.resolve(name);

          // Register new directories
          if (kind == ENTRY_CREATE && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
            registerAll(child);
          }

          // Report all other events to the listener
          else if (this._listener != null) {
            this._listener.received(child, ev.kind());
          }
        }

        // Remove deleted directories
        boolean valid = key.reset();
        if (!valid) {
          this._keys.remove(key);
          // all directories are inaccessible
          if (this._keys.isEmpty()) {
            break;
          }
        }

      } catch (InterruptedException | ClosedWatchServiceException ex) {
        break;
      }
    }
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Walk the file tree and registers the specified directory and all of its sub-directories
   * except those who match the list of ignored paths.
   *
   * @param start The directory to start from.
   */
  private synchronized void registerAll(final Path start) {
    LOGGER.debug("Registering new folders at watch service ...");
    try {
      int before = this._keys.size();
      Files.walkFileTree(start, new FileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          // ignore folder?
          if (FileTreeWatcher.this._ignore.contains(dir))
            return FileVisitResult.SKIP_SUBTREE;
          // add it to registry then
          register(dir);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex)  {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException ex)  {
          return FileVisitResult.CONTINUE;
        }
      });
      LOGGER.info("Added {} folders to watch service", this._keys.size() - before);
    } catch (IOException ex) {
      // Don't care
    }
  }

  /**
   * Registers the specified directory with the {@link WatchService}.
   *
   * @param dir The directory to watch.
   */
  private void register(Path dir) {
    try {
      this._keys.put(dir.register(this.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
      // check for max size
      if (this._maxKeys != -1 && this._keys.size() > this._maxKeys) {
        LOGGER.warn("Turning off index watcher as number of folders to watch is larger than max {}", this._maxKeys);
        stop();
      }
    } catch (IOException ex) {
      LOGGER.error("Faield to register watcher on folder {}", dir, ex);
    }
  }

  /**
   * Cast watch event to avoid type safety warnings on the code.
   */
  @SuppressWarnings("unchecked")
  private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>)event;
  }

}
