/*
 * Decompiled with CFR 0_110.
 */
package org.pageseeder.berlioz.flint.helper;

import org.pageseeder.berlioz.flint.model.FlintConfig;
import org.pageseeder.berlioz.flint.model.IndexMaster;

public final class Etags {
  private Etags() {
  }

  public static String getETag(String name) {
    StringBuilder etag = new StringBuilder();
    IndexMaster master = FlintConfig.getMaster(name);
    if (master != null) {
      etag.append(name).append('-').append(master.lastModified());
    }
    return etag.length() > 0 ? etag.toString() : null;
  }
}
