package org.powertac.visualizer.domain;

import java.io.Serializable;

public class Appearance implements Serializable {

    
	private static final long serialVersionUID = 1L;
	private String colorCode;
    private String iconLocation;
    

    public Appearance(String colorCode, String iconLocation) {
	this.colorCode = colorCode;
	this.iconLocation = iconLocation;
    }
    
    public String getColorCode() {
	return colorCode;
    }
    
    public String getIconLocation() {
	return iconLocation;
    }
}
