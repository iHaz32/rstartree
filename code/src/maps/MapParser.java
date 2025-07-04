package maps;

import domain.DataRecord;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class MapParser {
    // Parses an OSM XML file and returns a list of DataRecord objects
    public static List<DataRecord> parseOSM(String filePath) {
        List<DataRecord> resultSet = new ArrayList<>();
        try {
            File osmXml = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(osmXml);
            NodeList nodes = document.getElementsByTagName("node");

            int parsedCount = 0;
            for (int idx = 0; idx < nodes.getLength(); idx++) {
                Element nodeElem = (Element) nodes.item(idx);
                String idString = nodeElem.getAttribute("id");
                String latitudeString = nodeElem.getAttribute("lat");
                String longitudeString = nodeElem.getAttribute("lon");
                String uidString = nodeElem.getAttribute("uid");
                String changesetString = nodeElem.getAttribute("changeset");

                // Default label if not found
                String nodeLabel = "Unknown";

                NodeList tags = nodeElem.getElementsByTagName("tag");
                for (int tg = 0; tg < tags.getLength(); tg++) {
                    Element tagElem = (Element) tags.item(tg);
                    if ("name".equals(tagElem.getAttribute("k"))) {
                        nodeLabel = tagElem.getAttribute("v");
                        break;
                    }
                }

                long identifier = Long.parseLong(idString);
                double latitude = Double.parseDouble(latitudeString);
                double longitude = Double.parseDouble(longitudeString);
                long uid = uidString.isEmpty() ? -1 : Long.parseLong(uidString);
                long changeset = changesetString.isEmpty() ? -1 : Long.parseLong(changesetString);

                resultSet.add(new DataRecord(identifier, nodeLabel, latitude, longitude, uid, changeset));
                parsedCount++;
            }
            System.out.println("Parsed " + parsedCount + " OSM records.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultSet;
    }
}