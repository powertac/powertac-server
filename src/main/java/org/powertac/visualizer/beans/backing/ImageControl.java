package org.powertac.visualizer.beans.backing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.primefaces.component.chart.line.LineChart;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.springframework.stereotype.Controller;
 
@ManagedBean
@RequestScoped
public class ImageControl{

	private StreamedContent graphicText;
	private StreamedContent chart;
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

			// Chart
			JFreeChart jfreechart = ChartFactory.createPieChart("Turkish Cities", createDataset(), true, true, false);
			
			File chartFile = new File("dynamichart");
			ChartUtilities.saveChartAsPNG(chartFile, jfreechart, 375, 300);
			
			chart = new DefaultStreamedContent(new FileInputStream(chartFile), "image/png");
			
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

	public StreamedContent getChart() {
		return chart;
	}

	private PieDataset createDataset() {
		DefaultPieDataset dataset = new DefaultPieDataset();
		
		dataset.setValue("Istanbul", new Double(45.0));
		dataset.setValue("Ankara", new Double(15.0));
		dataset.setValue("Izmir", new Double(25.2));
		dataset.setValue("Antalya", new Double(14.8));

		return dataset;
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
