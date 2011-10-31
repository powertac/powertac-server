package org.powertac.distributionutility;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;

class DistributionUtilityInitializationService implements InitializationService {
	private static boolean transactional = true;
	Logger log = Logger.getLogger(this.getClass().getName());

	@Autowired
	private DistributionUtilityService distributionUtilityService;

	@Autowired
	private PluginConfigRepo pluginConfigRepo;

	public void setDefaults() {
		PluginConfig config = new PluginConfig("DistributionUtility", "");
		// TODO implement configurable values for below variables.
		// configuration: ['distributionFeeMin': '0.003',
		// 'distributionFeeMax': '0.03',
		// 'balancingCostMin': '0.02',
		// 'balancingCostMax': '0.06',
		// 'defaultSpotPrice': '50.0']);
		// config.save();
	}

	public String initialize(Competition competition,
			List<String> completedInits) {
		PluginConfig duConfig = pluginConfigRepo.findByRoleName("DistributionUtility");
		if (duConfig == null) {
			log.error("PluginConfig for DistributionUtility does not exist");
			return "fail";
		} else {
			distributionUtilityService.init(duConfig);
			return "DistributionUtility";
		}
	}
}
