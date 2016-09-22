
package net.imagej.server.resources;

import com.codahale.metrics.annotation.Timed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.server.api.Modules;

import org.scijava.Identifiable;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

@Path("/modules")
@Produces(MediaType.APPLICATION_JSON)
public class ModulesResource {

	private final ImageJ ij;
	private final List<Dataset> datasets;
	private final ArrayList<MInfo> mInfos = new ArrayList<>();

	public ModulesResource(final ImageJ ij, final List<Dataset> datasets) {
		this.ij = ij;
		this.datasets = datasets;

		int index = 1;
		for (final ModuleInfo info : ij.module().getModules()) {
			final MInfo mInfo = new MInfo();
			mInfo.info = info;
			mInfo.index = index++;
			if (info instanceof Identifiable) {
				mInfo.identifier = ((Identifiable) info).getIdentifier();
			}
			mInfos.add(mInfo);
		}
	}

	@GET
	@Timed
	public Modules retrieveModules() {
		return new Modules(mInfos);
	}

	private ModuleInfo getModule(final String id) {
		final ModuleInfo info = ij.module().getModuleById(id);
		if (info != null) return info;
		try {
			final int index = Integer.parseInt(id) - 1;
			return mInfos.get(index).info;
		}
		catch (final NumberFormatException exc) {
			// NB: No action needed.
		}
		return null;
	}

//	@POST
//	@Path("test")
//	public Object test(RunSpec spec) {
//		for (final String key : spec.inputMap.keySet())
//			System.out.println(spec.inputMap.get(key));
//		return new Object() {
//			public String val = "bar";
//			public double num = 1.01;
//			@Override
//			public String toString() {
//				System.out.println("print string");
//				return "foo";
//			}
//		};
//	}

	@GET
	@Path("{id}")
	public MInfoLong getWidget(@PathParam("id") String id) {
		// TODO: Handle all the errors!
		final ModuleInfo info = getModule(id);
		return info == null ? null : new MInfoLong(info); // FIXME: return null
	}

	@POST
	@Path("{id}")
	public Map<String, Object> runModule(@PathParam("id") String id,
		final RunSpec runSpec) throws InterruptedException, ExecutionException
	{
		final ModuleInfo info = getModule(id);
		final Map<String, Object> inputs = runSpec.inputMap;

		// substitute String with Dataset
		for (final String key : inputs.keySet()) {
			if (inputs.get(key) instanceof String) {
				final String val = (String) inputs.get(key);
				if (val.startsWith("_img_")) {
					final int idx = Integer.parseInt(val.substring("_img_".length()));
					inputs.put(key, datasets.get(idx));
				}
			}
		}

		final Module module = ij.module().run(info, runSpec.process,
			runSpec.inputMap).get();

		final Map<String, Object> outputs = module.getOutputs();

		// substitute Dataset with String
		for (final String key : outputs.keySet()) {
			if (outputs.get(key) instanceof Dataset) {
				final Dataset ds = (Dataset) outputs.get(key);
				int idx = datasets.indexOf(ds);
				if (idx == -1) {
					datasets.add(ds);
					idx = datasets.lastIndexOf(ds);
				}
				outputs.put(key, "_img_" + idx);
			}
		}
		
		return outputs;
	}

	// -- Helper classes --

	public static class RunSpec {

		public boolean process = true;
		public Map<String, Object> inputMap;
	}

	public static class MInfo {

		public transient ModuleInfo info;
		public long index;
		public String identifier;
	}

	public static class MInfoLong {

		public String identifier;
		public String name;
		public String label;
		public List<MItem> inputs = new ArrayList<>();
		public List<MItem> outputs = new ArrayList<>();

		public MInfoLong(final ModuleInfo info) {
			identifier = info instanceof Identifiable ? //
				((Identifiable) info).getIdentifier() : null;
			name = info.getName();
			label = info.getLabel();
			for (final ModuleItem<?> input : info.inputs()) {
				inputs.add(new MItem(input));
			}
			for (final ModuleItem<?> output : info.outputs()) {
				outputs.add(new MItem(output));
			}
		}
	}

	public static class MItem {

		public String name;
		public String label;
		public List<?> choices;

		public MItem(final ModuleItem<?> item) {
			name = item.getName();
			label = item.getLabel();
			choices = item.getChoices();
		}
	}

}
