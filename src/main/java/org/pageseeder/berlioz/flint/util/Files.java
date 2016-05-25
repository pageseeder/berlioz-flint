package org.pageseeder.berlioz.flint.util;
/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of utility functions for files.
 *
 * @author Jean-Baptiste Reure
 *
 * @version Flint 5.0.0
 * @since Flint 5.0.0
 */
public final class Files {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Files.class);

  /** Utility classes need no constructor. */
  private Files() {
  }

  /**
   * Returns the path from the root file to the specified file.
   * This method uses absolute path to accommodate for symbolic links.
   *
   * <p>Note: implementation note, only works if the root contains the specified file.
   *
   * @param root the container
   * @param file the file to check.
   *
   * @return The path to the file from the root.
   */
  public static String path(File root, File file) {
    if (root == null || file == null)
      throw new NullPointerException("Cannot determine the path between the specified files.");
    try {
      String from = root.getAbsolutePath();
      String to = file.getAbsolutePath();
      if (to.startsWith(from)) {
        String path = to.substring(from.length()).replace("\\", "/");
        return path.startsWith("/")? path.substring(1) : path;
      } else {
        from = root.getCanonicalPath();
        to = file.getCanonicalPath();
        if (to.startsWith(from)) {
          String path = to.substring(from.length()).replace("\\", "/");
          return path.startsWith("/")? path.substring(1) : path;
        } else
          throw new IllegalArgumentException("Cannot determine the path between the specified files.");
      }
    } catch (IOException ex) {
      LOGGER.warn("Unable to compute path between {} and {}", root, file, ex);
      return null;
    } catch (SecurityException ex) {
      LOGGER.warn("Unable to compute path between {} and {}", root, file, ex);
      return null;
    }
  }

}
