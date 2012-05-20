package org.powertac.factoredcustomer.utils;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 * @author Prashant Reddy
 */
public class BootDataExtractor
{
    static String inFile = "../server-main/bootstrap.xml";
    static String outFile = "../factored-customer/fcm-bootdata.csv";
    		
    public static void main(String[] args) 
    {
        try {
            String in = inFile;
            if (args.length > 0) in = args[0];
            String out = outFile;
            if (args.length > 1) in = args[1];
            
            System.out.println("Input: " + in);
            
            InputStream inStream = new FileInputStream(in);
            DataOutputStream outStream = new DataOutputStream(new FileOutputStream(out));
            
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inStream);

            Element bootstrap = (Element) doc.getElementsByTagName("bootstrap").item(0);
            NodeList nodes = bootstrap.getElementsByTagName("customer-bootstrap-data");
            for (int i=0; i < nodes.getLength(); ++i) {
                Element csd = (Element) nodes.item(i);
                String name = csd.getAttribute("customerName");
                String type = csd.getAttribute("powerType");
                String data = csd.getElementsByTagName("netUsage").item(0).getTextContent();
                
                outStream.writeBytes(name + "," + type + "," + data + "\r\n");
            }
            outStream.close();
            inStream.close();
            
            System.out.println("Output: " + out);
        } 
        catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
} // end class
