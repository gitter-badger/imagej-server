
package net.imagej.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.imagej.Dataset;
import net.imagej.ImageJ;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import io.scif.config.SCIFIOConfig;

@Path("/io")
public class IOResource {

	private ImageJ ij;

	private List<Dataset> datasets;
	private Set<String> serving;
	private String filePath;

	private static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static final Random random = new Random();
	private static final JsonNodeFactory factory = JsonNodeFactory.instance;

	private String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		return sb.toString();
	}

	public IOResource(final ImageJ ij, final List<Dataset> datasets,
		final String filePath)
	{
		this.ij = ij;
		this.datasets = datasets;
		this.filePath = filePath;
		serving = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	@POST
	@Path("file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Timed
	public JsonNode uploadFile(
		@FormDataParam("file") final InputStream fileInputStream,
		@SuppressWarnings("unused") @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader)
		throws IOException
	{
		final String filename = "io_upload_" + randomString(10);
		final java.nio.file.Path tmpFile = Paths.get(filePath, filename);
		Files.copy(fileInputStream, tmpFile);

		final Dataset ds = ij.scifio().datasetIO().open(tmpFile.toString());
		datasets.add(ds);
		// not using size() for concurrency concern
		final int idx = datasets.lastIndexOf(ds);

		return factory.objectNode().set("id", factory.textNode("_img_" + idx));
	}

	@POST
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Timed
	public JsonNode requestDataset(@PathParam("id") final String id,
		@QueryParam("ext") @NotEmpty final String ext, final SCIFIOConfig config)
		throws IOException
	{
		if (!id.startsWith("_img_")) {
			throw new IllegalArgumentException("Invalid id");
		}

		final int idx = Integer.parseInt(id.substring("_img_".length()));

		final Dataset ds = datasets.get(idx);
// need to have extension
		final String filename = randomString(10) + '.' + ext;
		ij.scifio().datasetIO().save(ds, Paths.get(filePath, filename).toString(),
			config);

		serving.add(filename);
		return factory.objectNode().set("token", factory.textNode(filename));
	}

	@GET
	@Path("{token}")
	@Produces("image/*")
	@Timed
	public Response retrieveDataset(@PathParam("token") final String token) {
		if (!serving.contains(token)) return Response.status(Status.BAD_REQUEST)
			.build();

		final File file = new File(Paths.get(filePath, token).toString());
		final String mt = new MimetypesFileTypeMap().getContentType(file);
		return Response.ok(file, mt).build();
	}
}
