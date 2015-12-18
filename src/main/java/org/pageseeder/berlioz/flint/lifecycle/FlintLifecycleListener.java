package org.pageseeder.berlioz.flint.lifecycle;

import java.io.File;

import org.pageseeder.berlioz.LifecycleListener;
import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;

public class FlintLifecycleListener implements LifecycleListener {

  @Override
  public boolean start() {
    System.out.println("[BERLIOZ_INIT] Lifecycle: Loading Flint Indexes");
    FlintConfig config = FlintConfig.get();
    int nb = 0;
    for (File folder : config.getRootDirectory().listFiles()) {
      if (folder.isDirectory()) {
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
    // shutdown all indexes
    for (IndexMaster index : FlintConfig.get().listIndexes()) {
      index.close();
    }
    // stop it all
    FlintConfig.get().getManager().stop();
    return true;
  }

}
