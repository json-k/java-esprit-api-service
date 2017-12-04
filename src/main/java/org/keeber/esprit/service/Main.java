package org.keeber.esprit.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.keeber.esprit.EspritAPIManager;
import org.keeber.simpleio.File;

import com.google.gson.Gson;


@WebListener
public class Main implements ServletContextListener {
  public static final String MANAGER_REF = "EspritAPIManager";
  private static Logger logger = Logger.getLogger(Main.class.getName());

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      /*
       * Read the configuration and create a manager instance for this web application.
       */
      Configuration conf = new Gson().fromJson(File.resolve("../conf/service.json").operations.getStringContent(), Configuration.class);
      EspritAPIManager manager = new EspritAPIManager(conf.host, conf.user, conf.pass);
      sce.getServletContext().setAttribute(MANAGER_REF, manager);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Terminal error reading conifguration file.", e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    /*
     * Shut down the manager instance.
     */
    EspritAPIManager manager = (EspritAPIManager) sce.getServletContext().getAttribute(MANAGER_REF);
    manager.shutdown();
  }

  private static class Configuration {
    private String host, user, pass;
  }

}
