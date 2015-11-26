package org.powertac.visualizer.beans;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import java.io.Serializable;

/**
 * Backing bean that returns context-relative path of images for use in PrimeFaces components. This pattern is used for avoiding
 *  use of DefaultStreamedContent object from Primefaces (quite buggy as of 2.12.2012.).  
 * Properties are set in one of beans declaration (XML).
 * @author Jurica Babic
 * 
 */

public class ImageBean implements Serializable {

  private static final long serialVersionUID = 1L;

  Logger log = LogManager.getLogger(ImageBean.class);

  private String logoPath;
  private String offeredTarrifPath;
  private String moneyPath;

  public String getLogoPath() {
	  return logoPath;
  }

  public void setLogoPath(String logoPath) {
	  this.logoPath = logoPath;
  }

	public String getOfferedTarrifPath() {
		return offeredTarrifPath;
	}

	public void setOfferedTarrifPath(String offeredTarrifPath) {
		this.offeredTarrifPath = offeredTarrifPath;
	}

	public String getMoneyPath() {
		return moneyPath;
	}

	public void setMoneyPath(String moneyPath) {
		this.moneyPath = moneyPath;
	}
}
