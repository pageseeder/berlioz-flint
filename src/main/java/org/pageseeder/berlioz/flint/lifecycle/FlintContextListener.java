package org.pageseeder.berlioz.flint.lifecycle;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.pageseeder.berlioz.servlet.InitServlet;

@WebListener("WebListener for Berlioz Flint")
public class FlintContextListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    System.out.println("[BERLIOZ_INIT] FlintInitialiser: Registering Flint Listener");
    InitServlet.registerListener(new FlintLifecycleListener());
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    // nothing to do here
  }

}
