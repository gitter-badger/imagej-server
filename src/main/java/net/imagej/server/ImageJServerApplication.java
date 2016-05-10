
package net.imagej.server;

import net.imagej.ImageJ;
import net.imagej.server.resources.ModulesResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ImageJServerApplication extends
	Application<ImageJServerConfiguration>
{

	public static void main(final String[] args) throws Exception {
		new ImageJServerApplication().run("server");
	}

	private ImageJ ij;

	@Override
	public String getName() {
		return "ImageJ";
	}

	@Override
	public void initialize(final Bootstrap<ImageJServerConfiguration> bootstrap) {
		ij = new ImageJ();
	}

	@Override
	public void run(final ImageJServerConfiguration configuration,
		final Environment environment)
	{
//		final TemplateHealthCheck healthCheck = //
//			new TemplateHealthCheck(configuration.getTemplate());
//		environment.healthChecks().register("template", healthCheck);

		final ModulesResource modulesResource = new ModulesResource(ij);
		environment.jersey().register(modulesResource);
	}
}
