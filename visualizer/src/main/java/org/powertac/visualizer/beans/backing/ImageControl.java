package org.powertac.visualizer.beans.backing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.imageio.ImageIO;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
 
@ManagedBean
@RequestScoped
public class ImageControl{

	private StreamedContent graphicText;
	private CartesianChartModel fakeChart;
	private String fakeValues;
	private String fakeId;

	public ImageControl() {
		try {
			// Graphic Text
			BufferedImage bufferedImg = new BufferedImage(100, 25, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = bufferedImg.createGraphics();
			g2.drawString("This is a text", 0, 10);
			ByteArrayOutputStream os = new ByteArrayOutputStream(); 
			ImageIO.write(bufferedImg, "png", os);
			graphicText = new DefaultStreamedContent(new ByteArrayInputStream(os.toByteArray()), "image/png");

					
			
			fakeChart = new CartesianChartModel();
			ChartSeries chartSeries  =new ChartSeries();
			chartSeries.set("0", 2);
			fakeChart.addSeries(chartSeries);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public StreamedContent getGraphicText() {
		return graphicText;
	}
	
	public CartesianChartModel getFakeChart() {
		return fakeChart;
	}
	public String getFakeValues() {
		fakeValues="[ [ "+Math.random()*10+", "+Math.random()*10+" ],[ 3, 5.12 ], [ 5, 13.1 ], [ 7, 33.6 ], [ 9, 85.9 ],[ 11, 219.9 ] ]";
		
		return fakeValues;
	}
	public String getFakeId() {
		fakeId="chartabela";
		return fakeId;
	}
}
