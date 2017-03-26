package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.List;

import org.powertac.visualizer.domain.genco.Genco;
import org.powertac.visualizer.services.GencoService;
import org.springframework.beans.factory.annotation.Autowired;

public class GencosBean implements Serializable {

	private GencoService gencoService;
	private List<Genco> list;
	
	@Autowired
	public GencosBean(GencoService gencoService) {
		this.gencoService = gencoService;
		list = gencoService.getGencoList();
	}
	
	public List<Genco> getList() {
		return list;
	}

}
