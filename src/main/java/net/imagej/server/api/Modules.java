
package net.imagej.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import net.imagej.server.resources.ModulesResource.MInfo;

public class Modules {

	private List<MInfo> modules;

	public Modules() {
		// Jackson deserialization
	}

	public Modules(final List<MInfo> modules) {
		this.modules = modules;
	}

	@JsonProperty
	public List<MInfo> getModules() {
		return modules;
	}

}
