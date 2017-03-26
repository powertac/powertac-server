package org.powertac.visualizer.domain;

import java.io.Serializable;

public class Appearance implements Serializable {

	private static final long serialVersionUID = 1L;
	private String colorCode;
	private int R;
	private int G;
	private int B;
	private double shadingFactor = 0.2;
	private String iconLocation;

	public Appearance(String colorCode, String iconLocation, int R, int G,
			int B) {
		this.colorCode = colorCode;
		this.iconLocation = iconLocation;
		this.R = R;
		this.G = G;
		this.B = B;
	}

	public String getColorCode() {
		return colorCode;
	}

	public String getIconLocation() {
		return iconLocation;
	}
	
	public String getColorCodeRGBShading(){
		return "rgba("+R+","+G+","+B+","+shadingFactor+")";
	}
}
