
package net.imagej.server.resources;

import com.codahale.metrics.annotation.Timed;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.imagej.ImageJ;
import net.imagej.server.api.Modules;

import org.scijava.Identifiable;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

@Path("/modules")
@Produces(MediaType.APPLICATION_JSON)
public class ModulesResource {

	private final ImageJ ij;
	private final ArrayList<MInfo> mInfos = new ArrayList<>();

	public ModulesResource(final ImageJ ij) {
		this.ij = ij;
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

	@GET
	@Path("{id}")
	public MInfoLong getWidget(@PathParam("id") String id) {
		// TODO: Handle all the errors!
		final ModuleInfo info = ij.module().getModuleById(id);
		if (info != null) return new MInfoLong(info);
		try {
			final int index = Integer.parseInt(id) - 1;
			return new MInfoLong(mInfos.get(index).info);
		}
		catch (final NumberFormatException exc) {
			// NB: No action needed.
		}
		return null; // FIXME
	}

	// -- Helper classes --

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
