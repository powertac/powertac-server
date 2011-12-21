package org.powertac.visualizer.domain;

import org.powertac.common.PluginConfig;

public class GencoModel extends BrokerModel {

	private PluginConfig pluginConfig;

	public GencoModel(String name, Appearance appearance,
			PluginConfig pluginConfig) {
		super(name, appearance);
		this.pluginConfig = pluginConfig;
	}

	public PluginConfig getPluginConfig() {
		return pluginConfig;
	}
	
}
