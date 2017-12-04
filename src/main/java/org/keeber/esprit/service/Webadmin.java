package org.keeber.esprit.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.tanukisoftware.wrapper.WrapperManager;

/**
 * This handles shutting the server down - in the IDE or could be used in place to terminate it.
 * 
 * @author Jason Keeber <jason@keeber.org>
 *
 */
@Path("/")
@ApplicationPath("/admin")
public class Webadmin extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> classes = new HashSet<>();
    classes.add(Webadmin.class);
    return classes;
  }
  
  @GET
  @Path("/KILLSWITCH")
  public Response shutdown() {
    new Thread(new Runnable() {

      @Override
      public void run() {
        WrapperManager.stop(0);
      }
    }).start();
    return Response.noContent().build();
  }

}
