package org.keeber.esprit.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.keeber.esprit.EspritAPI;
import org.keeber.esprit.EspritAPI.ApiResponse;
import org.keeber.esprit.EspritAPI.EspritConnectionException;
import org.keeber.esprit.EspritAPIManager;
import org.keeber.esprit.service.handlers.EspritMessageBodyHandler;

import com.dalim.esprit.api.EsClass;
import com.dalim.esprit.api.EsObject;
import com.dalim.esprit.api.EsRef;
import com.dalim.esprit.api.EsStream;
import com.dalim.esprit.api.customer.EsCustomer;
import com.dalim.esprit.api.document.EsDocument;
import com.dalim.esprit.api.job.EsJob;
import com.dalim.esprit.api.production.EsSelectResult;
import com.google.gson.annotations.SerializedName;

@Path("/")
@ApplicationPath("/rest")
public class Rest extends Application {
  @Context
  ServletContext context;

  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> classes = new HashSet<>();
    classes.add(Rest.class);
    classes.add(EspritMessageBodyHandler.class);
    return classes;
  }

  @GET
  @Path("/customers")
  @Produces(MediaType.APPLICATION_JSON)
  public Response allCustomers() {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsObject.ListOf> r = api.production.list(EsObject.ROOT);
      return (r.hasResult() ? Response.ok(r.get().getByESClass(EsClass.Customer)) : Response.status(Status.NOT_FOUND)).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/customers/{customerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCustomer(@PathParam("customerId") int customerId) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsCustomer> r = api.customer.get(EsRef.from(customerId), false);
      return (r.hasResult() ? Response.ok(r.get()) : Response.status(Status.NOT_FOUND)).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  /**
   * A fake job to mimic a real job from the select response (because it's faster than listing jobs
   * and returns the actual name.
   * 
   * @author Jason Keeber <jason@keeber.org>
   *
   */
  @SuppressWarnings("unused")
  private static class FakeJob {
    private Date lastModificationDate, creationDate;
    private String name;
    private Integer ID;
    @SerializedName("class")
    private EsClass esclass = EsClass.Job;
  }

  private String[] fakeProperties = new String[] {"name", "ID", "lastModificationDate", "creationDate"};

  @GET
  @Path("/customers/{customerId}/jobs")
  @Produces(MediaType.APPLICATION_JSON)
  public Response allJObs(@PathParam("customerId") int customerId) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsSelectResult> r = api.production.select(EsClass.Job, api.production.newSelectWhereBuilder().addClause("customerID", "=", Integer.toString(customerId)).build(), fakeProperties);
      return (r.hasResult() ? Response.ok(r.get().convert(FakeJob.class)) : Response.serverError()).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/customers/{customerId}/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJob(@PathParam("jobId") int jobId) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsJob> r = api.job.get(EsRef.from(jobId), false);
      return (r.hasResult() ? Response.ok(r.get()) : Response.status(Status.NOT_FOUND)).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  /**
   * A fake page to mimic a real job from the select response (because it's faster than listing
   * pages and returns the actual name.
   * 
   * @author Jason Keeber <jason@keeber.org>
   *
   */
  @SuppressWarnings("unused")
  private static class FakePage {
    private Date lastModificationDate, creationDate;
    private String name;
    private Integer ID;
    @SerializedName("class")
    private EsClass esclass = EsClass.PageOrder;
  }

  @GET
  @Path("/customers/{customerId}/jobs/{jobId}/documents")
  @Produces(MediaType.APPLICATION_JSON)
  public Response allDocuments(@PathParam("jobId") int jobId) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsSelectResult> r = api.production.select(EsClass.PageOrder, api.production.newSelectWhereBuilder().addClause("jobID", "=", Integer.toString(jobId)).build(), fakeProperties);
      return (r.hasResult() ? Response.ok(r.get().convert(FakePage.class)) : Response.serverError()).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/customers/{customerId}/jobs/{jobId}/documents/{docId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDocument(@PathParam("docId") int docId) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsDocument> r = api.document.get(EsRef.from(docId), false);
      return (r.hasResult() ? Response.ok(r.get()) : Response.status(Status.NOT_FOUND)).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/customers/{customerId}/jobs/{jobId}/documents/{docId}/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDocument(@PathParam("docId") int docId, @PathParam("type") EsStream type) {
    try (EspritAPI api = getManager().acquireAPI()) {
      ApiResponse<EsDocument> doc = api.document.get(EsRef.from(docId), false);
      String name = doc.get().getName() + ((type == EsStream.preview || type == EsStream.thumbnail) ? "_" + type.toString() + ".jpg" : "");
      ApiResponse<InputStream> r = api.document.get(docId, type);
      return (r.hasResult() ? Response.ok(new StreamingOutput() {

        @Override
        public void write(OutputStream os) throws IOException, WebApplicationException {
          Streams.copy(r.get(), os, true);
        }
      }).header("Content-Disposition", "attachment; filename=\"" + name + "\"") : Response.status(Status.NOT_FOUND)).build();
    } catch (EspritConnectionException e) {
      return Response.serverError().build();
    }
  }

  private EspritAPIManager getManager() {
    return (EspritAPIManager) context.getAttribute(Main.MANAGER_REF);
  }

}
