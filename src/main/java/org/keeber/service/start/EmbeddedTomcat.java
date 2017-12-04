package org.keeber.service.start;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.keeber.simpleio.File;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * <p>
 * This is the startup class. It runs from the wrapper and starts the embedded Tomcat server.
 */
public class EmbeddedTomcat implements WrapperListener {

  public EmbeddedTomcat() {}

  private static final Logger logger = Logger.getLogger("Embedded-Tomcat");
  private Tomcat tomcat;
  private File webHome;
  private File appBase;
  private int port = 8080;
  private Map<String, Context> contexts = new HashMap<>();
  private boolean running = true;

  /**
   * @param args
   */
  public static void main(String[] args) {
    WrapperManager.start(new EmbeddedTomcat(), args);
  }

  @Override
  public void controlEvent(int arg0) {

  }

  private void startTomcat() {
    try {

      System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
      System.setProperty("org.apache.coyote.USE_CUSTOM_STATUS_MSG_IN_HEADER", "true");

      tomcat = new Tomcat();
      tomcat.setPort(port);
      tomcat.setBaseDir(webHome.create("../tmp/").getPath());
      tomcat.getHost().setCreateDirs(true);
      tomcat.getHost().setAutoDeploy(false);
      appBase = webHome.create("../tmp/apps/");
      appBase.mkdirs();
      tomcat.getHost().setAppBase(appBase.getPath());
    } catch (Exception e) {
      logger.log(Level.SEVERE, null, e);
    }
    try {
      tomcat.start();
    } catch (LifecycleException e) {
      logger.log(Level.SEVERE, null, e);
    }
    if (Boolean.getBoolean("embedded.hotswap")) {
      logger.log(Level.INFO, "\n**\n\tRunning in HOT-SWAP MODE.\n**");
    }
    new Thread(() -> {
      boolean first = true;
      try {
        while (running) {
          if (first || Boolean.getBoolean("embedded.hotswap")) {
            for (File war : webHome.list(f -> "war".equals(f.getExtension()), File.filters.ONLY_THIS_DIRECTORY)) {
              if ((!contexts.containsKey(war.getBaseName())) || System.currentTimeMillis() - war.getLastModified() < 10000) {
                Context context = contexts.remove(war.getBaseName());
                if (context != null) {
                  context.stop();
                  tomcat.getHost().removeChild(context);
                  while (!appBase.create(war.getBaseName() + "/").operations.rmdir()) {
                  }
                }
                logger.info("Deploying:" + war.getBaseName());
                contexts.put(war.getBaseName(), tomcat.addWebapp(tomcat.getHost(), "/" + war.getBaseName(), war.getPath()));
              }
            }
            first = false;
          }
          Thread.sleep(10000);
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, null, e);
      }
    }).start();

  }

  @Override
  public Integer start(String[] args) {
    logger.log(Level.INFO, "Java-version[{0}]", new Object[] {System.getProperty("java.version")});
    logger.log(Level.INFO, "Service-port[{0}]", new Object[] {args[0]});


    port = Integer.parseInt(args[0]);
    try {
      webHome = File.resolve(args[1]);
    } catch (IOException e) {
      logger.log(Level.SEVERE, null, e);
    }

    logger.log(Level.INFO, "Web directory:[{0}]", webHome);
    startTomcat();
    return null;
  }

  @Override
  public int stop(int i) {
    try {
      try {
        WrapperManager.requestShutdownLock();
      } catch (Exception e) {

      }
      logger.log(Level.INFO, "Stopping...");
      try {
        WrapperManager.signalStopping(30000);
      } catch (Exception e) {

      }
      try {
        logger.log(Level.INFO, "Shutting down Tomcat...");
        tomcat.stop();
        tomcat.getServer().await();
        tomcat.destroy();
        logger.log(Level.INFO, "Shut down Tomcat...");
        try {
          webHome.create("../tmp/").operations.rmdir();
        } catch (IOException e) {
          logger.log(Level.SEVERE, null, e);
        }
        running = false;
      } catch (LifecycleException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
      try {
        WrapperManager.releaseShutdownLock();
      } catch (Exception e) {

      }
    } catch (Exception e) {
      logger.log(Level.WARNING, null, e);
    }

    return i;
  }

}
