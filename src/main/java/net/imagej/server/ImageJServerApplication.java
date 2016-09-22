
package net.imagej.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.server.resources.IOResource;
import net.imagej.server.resources.ModulesResource;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ImageJServerApplication extends
	Application<ImageJServerConfiguration>
{

	public static void main(final String[] args) throws Exception {
		new ImageJServerApplication().run(args);
	}

	private ImageJ ij;

	private List<Dataset> datasetRepo;

	@Override
	public String getName() {
		return "ImageJ";
	}

	@Override
	public void initialize(final Bootstrap<ImageJServerConfiguration> bootstrap) {
		ij = new ImageJ();
		ij.ui().setHeadless(true);
		datasetRepo = new CopyOnWriteArrayList<>();
	}

	@Override
	public void run(final ImageJServerConfiguration configuration,
		final Environment environment)
	{
//		final TemplateHealthCheck healthCheck = //
//			new TemplateHealthCheck(configuration.getTemplate());
//		environment.healthChecks().register("template", healthCheck);

		environment.jersey().register(MultiPartFeature.class);

		final ModulesResource modulesResource = new ModulesResource(ij,
			datasetRepo);
		environment.jersey().register(modulesResource);

		final IOResource ioResource = new IOResource(ij, datasetRepo,
			"C:\\Users\\Guohong\\tmp");
		environment.jersey().register(ioResource);
	}
}
