package org.powertac.visualizer.domain;

import java.io.Serializable;

public class Response implements Serializable {

	private static final long serialVersionUID = 1L;
	private boolean booleanAnswer;
	public Response(boolean b) {
		booleanAnswer = b;
	}

	public boolean isBooleanAnswer() {
		return booleanAnswer;
	}

}
