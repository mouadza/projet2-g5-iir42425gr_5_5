package projet2_compilation;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.util.*;
import java.util.List;

public class SimpleXmlToJson {
    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("XML to JSON Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Main panel with modern layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for file selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel fileLabel = new JLabel("Selected File:");
        JTextField filePathField = new JTextField(30);
        filePathField.setEditable(false);
        JButton browseButton = new JButton("Browse...");
        topPanel.add(fileLabel);
        topPanel.add(filePathField);
        topPanel.add(browseButton);

        // Center panel for text area
        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output (JSON Format)"));

        // Bottom panel for action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton convertButton = new JButton("Convert");
        JButton clearButton = new JButton("Clear");
        bottomPanel.add(clearButton);
        bottomPanel.add(convertButton);

        // Add panels to the main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add main panel to the frame
        frame.add(mainPanel);

        // File chooser logic
        final File[] selectedFile = {null};
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile[0].getAbsolutePath());
            }
        });

        // Convert button logic
        convertButton.addActionListener(e -> {
            if (selectedFile[0] == null) {
                JOptionPane.showMessageDialog(frame, "Please select an XML file first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                String json = convertXmlToJson(selectedFile[0]);
                outputArea.setText(json);
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
                ex.printStackTrace(); // Debugging output to console
            }
        });

        // Clear button logic
        clearButton.addActionListener(e -> {
            filePathField.setText("");
            outputArea.setText("");
            selectedFile[0] = null;
        });

        // Show the frame
        frame.setVisible(true);
    }

    private static String convertXmlToJson(File file) throws Exception {
        // Parse the XML file into a DOM Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Convert the DOM to JSON
        Map<String, Object> jsonMap = parseElement(document.getDocumentElement());
        return formatJson(jsonMap, 0);
    }

    private static Map<String, Object> parseElement(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();

        // Get attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            map.put("@" + attribute.getNodeName(), attribute.getNodeValue());
        }

        // Get child elements
        NodeList children = element.getChildNodes();
        Map<String, List<Object>> childMap = new LinkedHashMap<>();
        StringBuilder textContent = new StringBuilder();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String tagName = childElement.getTagName();
                childMap.putIfAbsent(tagName, new ArrayList<>());
                childMap.get(tagName).add(parseElement(childElement));
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent().trim();
                if (!text.isEmpty()) {
                    textContent.append(text);
                }
            }
        }

        // Merge child elements into the main map
        for (Map.Entry<String, List<Object>> entry : childMap.entrySet()) {
            if (entry.getValue().size() == 1) {
                map.put(entry.getKey(), entry.getValue().get(0));
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        // Add text content if present
        if (textContent.length() > 0) {
            map.put("#text", textContent.toString());
        }

        return map;
    }

    public static String formatJson(Map<String, Object> map, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentation = "  ".repeat(indent);
        sb.append("{\n");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(indentation).append("  \"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof Map) {
                sb.append(formatJson((Map<String, Object>) entry.getValue(), indent + 1));
            } else if (entry.getValue() instanceof List) {
                sb.append("[\n");
                for (Object item : (List<?>) entry.getValue()) {
                    if (item instanceof Map) {
                        sb.append(formatJson((Map<String, Object>) item, indent + 1)).append(",\n");
                    } else {
                        sb.append(indentation).append("    \"").append(item).append("\"\n");
                    }
                }
                sb.append(indentation).append("  ]");
            } else {
                sb.append("\"").append(entry.getValue()).append("\"");
            }
            sb.append(",\n");
        }
        sb.append(indentation).append("}");
        return sb.toString();
    }
}
