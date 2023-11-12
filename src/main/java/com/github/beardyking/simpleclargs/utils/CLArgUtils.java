package com.github.beardyking.simpleclargs.utils;

import com.github.beardyking.simpleclargs.serialization.NodeDataJsonParser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

import static com.github.beardyking.simpleclargs.ui.CLArgumentTree.filePath;
import static com.github.beardyking.simpleclargs.ui.CLArgumentTree.rootNode;

public class CLArgUtils {
    public static void saveCommandTreeToFile() {
        NodeDataJsonParser jsonParser = new NodeDataJsonParser();
        jsonParser.saveNodeDataTreeToJson(rootNode, filePath);
    }

    public static void expandAllNodes(JTree tree, TreePath parentPath) {
        TreeNode parentNode = (TreeNode) parentPath.getLastPathComponent();
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            TreeNode childNode = parentNode.getChildAt(i);
            TreePath childPath = parentPath.pathByAddingChild(childNode);
            tree.expandPath(childPath);
            expandAllNodes(tree, childPath);
        }
    }

    public static void expandAllNodes(JTree tree, DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }

        TreePath path = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node));
        tree.expandPath(path);

        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
            expandAllNodes(tree, childNode);
        }
    }
}
