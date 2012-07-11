package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;

import javax.faces.event.AjaxBehaviorEvent;

import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.services.CustomerService;
import org.primefaces.event.ToggleEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class UserSessionBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	//customers:
	private String genericType = "CONSUMPTION";
	
	//wholesale:
	private boolean globalWholesaleGraphCollapsed = false;
	private boolean timeslotWholesaleGraphCollapsed = false;
	private boolean beforeClearingGraphCollapsed = false;
	private boolean afterClearingGraphCollapsed = false;
	private boolean clearingDetails = false;
	
	
		
	public void setGenericType(String genericType) {
		this.genericType = genericType;
		}
	
	public String getGenericType() {
		return genericType;
	}
	
	public void onToggleGlobalWholesaleGraph(){
		if(globalWholesaleGraphCollapsed){
			globalWholesaleGraphCollapsed=false;
		} else{
			globalWholesaleGraphCollapsed=true;
		}
	}
	public void onToggleTimeslotWholesaleGraph(){
		if(timeslotWholesaleGraphCollapsed){
			timeslotWholesaleGraphCollapsed=false;
		} else{
			timeslotWholesaleGraphCollapsed=true;
		}
	}
	public void onToggleBeforeClearingGraph(){
		if(beforeClearingGraphCollapsed){
			beforeClearingGraphCollapsed=false;
		} else{
			beforeClearingGraphCollapsed=true;
		}
	}
	public void onToggleAfterClearingGraph(){
		if(afterClearingGraphCollapsed){
			afterClearingGraphCollapsed=false;
		} else{
			afterClearingGraphCollapsed=true;
		}
	}
	
	public void onToggleClearingDetails(){
		if(clearingDetails){
			clearingDetails=false;
		} else{
			clearingDetails=true;
		}
	}

	public boolean isGlobalWholesaleGraphCollapsed() {
		return globalWholesaleGraphCollapsed;
	}

	public void setGlobalWholesaleGraphCollapsed(
			boolean globalWholesaleGraphCollapsed) {
		this.globalWholesaleGraphCollapsed = globalWholesaleGraphCollapsed;
	}

	public boolean isTimeslotWholesaleGraphCollapsed() {
		return timeslotWholesaleGraphCollapsed;
	}

	public void setTimeslotWholesaleGraphCollapsed(
			boolean timeslotWholesaleGraphCollapsed) {
		this.timeslotWholesaleGraphCollapsed = timeslotWholesaleGraphCollapsed;
	}

	public boolean isBeforeClearingGraphCollapsed() {
		return beforeClearingGraphCollapsed;
	}

	public void setBeforeClearingGraphCollapsed(boolean beforeClearingGraphCollapsed) {
		this.beforeClearingGraphCollapsed = beforeClearingGraphCollapsed;
	}

	public boolean isAfterClearingGraphCollapsed() {
		return afterClearingGraphCollapsed;
	}

	public void setAfterClearingGraphCollapsed(boolean afterClearingGraphCollapsed) {
		this.afterClearingGraphCollapsed = afterClearingGraphCollapsed;
	}

	public boolean isClearingDetails() {
		return clearingDetails;
	}

	public void setClearingDetails(boolean clearingDetails) {
		this.clearingDetails = clearingDetails;
	}

	
	
	
	

}
