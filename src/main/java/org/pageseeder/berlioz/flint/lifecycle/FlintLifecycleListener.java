package org.pageseeder.berlioz.flint.lifecycle;

import java.io.File;

import org.pageseeder.berlioz.LifecycleListener;
import org.pageseeder.berlioz.flint.model.FlintConfig;

public class FlintLifecycleListener implements LifecycleListener {

  @Override
  public boolean start() {
    System.out.println("[BERLIOZ_INIT] Lifecycle: Loading Flint Indexes");
    FlintConfig config = FlintConfig.get();
    int nb = 0;
    File root = config.getRootDirectory();
    for (File folder : root.listFiles()) {
      if (folder.isDirectory()) {
        // autosuggest index?
        if (folder.getName().endsWith("_autosuggest") &&
            new File(root, folder.getName().split("_")[0]).exists()) {
          continue;
        }
        if (config.getMaster(folder.getName()) == null) {
          System.out.println("[BERLIOZ_INIT] Lifecycle: Failed to load index "+folder.getName());
        } else {
          nb++;
        }
      }
    }
    System.out.println("[BERLIOZ_INIT] Lifecycle: Successfully loaded "+nb+" index"+(nb == 1 ? "" : "es"));
    return true;
  }

  @Override
  public boolean stop() {
    // stop it all
    FlintConfig.get().stop();
    return true;
  }

}
