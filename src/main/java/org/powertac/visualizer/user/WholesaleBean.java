package org.powertac.visualizer.user;

import java.io.Serializable;

import org.primefaces.model.TreeNode;

public class WholesaleBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private TreeNode selectedNode;
	
	public TreeNode getSelectedNode() {
		return selectedNode;
	}
	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}
}
