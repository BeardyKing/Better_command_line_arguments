package com.github.beardyking.simpleclargs.serialization;

import com.github.beardyking.simpleclargs.types.NodeData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public class NodeDataJsonParser {
    private final ObjectMapper objectMapper;
    private final String clargVersion = "1.0";

    public NodeDataJsonParser() {
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveNodeDataTreeToJson(DefaultMutableTreeNode rootNode, String filePath) {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("clarg_version", clargVersion);
            jsonNode.set("root", saveNodeToJson(rootNode));

            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(new File(filePath), jsonNode);
            System.out.println("NodeData tree saved to JSON successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save NodeData tree to JSON.");
        }
    }

    public DefaultMutableTreeNode loadNodeDataTreeFromJson(String filePath) {
        try {
            JsonNode rootNode = objectMapper.readTree(new File(filePath));
            String loadedVersion = rootNode.get("clarg_version").asText();

            if (clargVersion.equals(loadedVersion)) {
                JsonNode treeData = rootNode.get("root");
                return loadNodeFromJson(treeData);
            } else {
                System.err.println("Incompatible CLArg version. Expected: " + clargVersion + ", Loaded: " + loadedVersion);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load NodeData tree from JSON.");
            return null;
        }
    }

    private ObjectNode saveNodeToJson(DefaultMutableTreeNode node) {
        ObjectNode nodeObject = objectMapper.createObjectNode();
        NodeData nodeData = (NodeData) node.getUserObject();
        nodeObject.put("isSelected", nodeData.isSelected());
        nodeObject.put("text", nodeData.getText());
        nodeObject.put("isLeaf", nodeData.isLeaf());

        ArrayNode childrenArray = objectMapper.createArrayNode();
        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
            childrenArray.add(saveNodeToJson(childNode));
        }

        nodeObject.set("children", childrenArray);
        return nodeObject;
    }

    private DefaultMutableTreeNode loadNodeFromJson(JsonNode jsonNode) {
        NodeData nodeData = new NodeData(
                jsonNode.get("isSelected").asBoolean(),
                jsonNode.get("text").asText(),
                jsonNode.get("isLeaf").asBoolean()
        );

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeData);
        JsonNode childrenArray = jsonNode.get("children");
        if (childrenArray != null) {
            for (JsonNode childNode : childrenArray) {
                node.add(loadNodeFromJson(childNode));
            }
        }
        return node;
    }

}
