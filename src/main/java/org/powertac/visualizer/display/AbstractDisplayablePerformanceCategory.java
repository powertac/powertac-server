package org.powertac.visualizer.display;

import org.powertac.visualizer.statistical.Grade;

public class AbstractDisplayablePerformanceCategory {
	private Grade grade;
	public AbstractDisplayablePerformanceCategory(Grade grade) {
		this.grade = grade;
	}
	public Grade getGrade() {
		return grade;
	}

}
