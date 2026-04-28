package org.powertac.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.xml.stream.*;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * The WeatherXmlExtractor class is responsible for processing a weather-related
 * XML file and extracting a filtered subset of weather reports and forecasts
 * based on a specific request date.
 */
@Component
public class WeatherXmlExtractor
{

  static private final Logger log =
          LogManager.getLogger(WeatherXmlExtractor.class);

  public String extractPartialXml (WeatherService weatherService,
                                   String filePath, ZonedDateTime requestDate,
                                   int weatherReqInterval, int forecastHorizon)
  {
    try {
      log.info("Starting StAX extraction from file: {}", filePath);

      // Setup StAX processing
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      FileInputStream fileInputStream = new FileInputStream(filePath);
      XMLStreamReader reader =
              inputFactory.createXMLStreamReader(fileInputStream);

      // Setup output
      StringWriter outputWriter = new StringWriter();
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLStreamWriter writer =
              outputFactory.createXMLStreamWriter(outputWriter);

      // Initialize output document
      writer.writeStartDocument();
      writer.writeStartElement("data");
      writer.writeStartElement("weatherReports");

      // Tracking variables
      boolean inWeatherReports = false;
      boolean inWeatherForecasts = false;
      int reportCount = 0;
      int forecastCount = 0;
      boolean inReportsSection = true;
      String reportDateTarget = weatherService.dateStringLong(requestDate);

      log.debug("Looking for reports starting from date: {}", reportDateTarget);

      Set<String> originTargets = new HashSet<>();

      // Pre-compute forecast origins we need
      for (int i = 0; i < weatherReqInterval; i++) {
        String target = weatherService.dateStringLong(requestDate.plusHours(i));
        originTargets.add(target);
        log.debug("Added forecast target: {}", target);
      }

      // Process XML stream
      while (reader.hasNext()) {
        int event = reader.next();

        if (event == XMLStreamConstants.START_ELEMENT) {
          String elementName = reader.getLocalName();

          if ("weatherReports".equals(elementName)) {
            inWeatherReports = true;
            log.debug("Entered weatherReports section");
          }
          else if ("weatherForecasts".equals(elementName)) {
            inWeatherForecasts = true;
            log.debug("Entered weatherForecasts section");

            // If we've collected all reports, close reports element and start forecasts
            if (reportCount == weatherReqInterval && inReportsSection) {
              writer.writeEndElement(); // close weatherReports
              writer.writeStartElement("weatherForecasts");
              inReportsSection = false;
              log.debug(
                      "Closed weatherReports and started weatherForecasts section");
            }
          }
          else if (inWeatherReports && "weatherReport".equals(elementName)) {
            // Check if this report is what we want
            String date = reader.getAttributeValue(null, "date");
            if (reportCount < weatherReqInterval
                && date.compareTo(reportDateTarget) >= 0) {
              // Copy this element (with attributes and children) to output
              copyNodeWithContents(reader, writer);
              reportCount++;
              log.debug("Added weather report {} with date {}", reportCount,
                        date);
            }
          }
          else if (inWeatherForecasts && "weatherForecast".equals(
                  elementName)) {
            // Check if this forecast is for one of our target dates
            String origin = reader.getAttributeValue(null, "origin");
            if (originTargets.contains(origin)) {
              // Copy this element to output
              copyNodeWithContents(reader, writer);
              forecastCount++;
              log.debug("Added weather forecast {} with origin {}",
                        forecastCount, origin);
            }
          }
        }

        // Exit early if we have all the data we need
        if (reportCount >= weatherReqInterval
            && forecastCount >= weatherReqInterval * forecastHorizon) {
          log.debug(
                  "Early exit: Got all required data ({} reports, {} forecasts)",
                  reportCount, forecastCount);
          break;
        }
      }

      // Close any open elements
      writer.writeEndElement(); // close weatherReports
      // close weatherForecasts
      if (inReportsSection) {
        writer.writeStartElement("weatherForecasts");
        writer.writeEndElement(); // close weatherForecasts
        log.debug(
                "Closed weatherReports and added empty weatherForecasts section");
      }
      else {
        log.debug("Closed weatherForecasts section");
      }

      writer.writeEndElement(); // close data
      writer.writeEndDocument();
      writer.flush();
      reader.close();

      String result = outputWriter.toString();
      log.debug("Generated XML of length: {}", result.length());

      // Debug output the first 200 chars of the XML
      if (!result.isEmpty()) {
        String preview = result.substring(0, Math.min(200, result.length()));
        log.debug("XML preview: {}", preview);
      }

      return result;
    }
    catch (Exception e) {
      log.error("StAX processing error: " + e.getMessage(), e);
      return null;
    }
  }

  private void copyNodeWithContents (XMLStreamReader reader,
                                     XMLStreamWriter writer)
          throws XMLStreamException
  {
    // Get the current element name
    String elementName = reader.getLocalName();

    // Start the element in the output
    writer.writeStartElement(elementName);

    // Copy all attributes
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String attrName = reader.getAttributeLocalName(i);
      String attrValue = reader.getAttributeValue(i);
      writer.writeAttribute(attrName, attrValue);
    }

    // Process the element content including any child elements
    boolean done = false;
    int depth = 1; // Start at depth 1 since we're inside an element

    while (!done && reader.hasNext()) {
      int eventType = reader.next();

      switch (eventType) {
      case XMLStreamConstants.CHARACTERS:
        writer.writeCharacters(reader.getText());
        break;

      case XMLStreamConstants.START_ELEMENT:
        depth++;
        String childName = reader.getLocalName();
        writer.writeStartElement(childName);

        // Copy child element attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
          writer.writeAttribute(reader.getAttributeLocalName(i),
                                reader.getAttributeValue(i));
        }
        break;

      case XMLStreamConstants.END_ELEMENT:
        depth--;
        writer.writeEndElement();

        // If we're back at the original element's end, we're done
        if (depth == 0) {
          done = true;
        }
        break;
      }
    }
  }
}
