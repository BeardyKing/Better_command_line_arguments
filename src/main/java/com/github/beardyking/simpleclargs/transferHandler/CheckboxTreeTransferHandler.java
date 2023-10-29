package com.github.beardyking.simpleclargs.transferHandler;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.github.beardyking.simpleclargs.utils.CLArgUtils;

public class CheckboxTreeTransferHandler extends TransferHandler {
    private final JTree tree;
    public static DataFlavor nodeFlavor;

    public CheckboxTreeTransferHandler(JTree tree) {
        this.tree = tree;
        try {
            nodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=javax.swing.tree.DefaultMutableTreeNode");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.MOVE; // Allow moving nodes
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree source = (JTree) c;
        TreePath[] paths = source.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[paths.length];
            for (int i = 0; i < paths.length; i++) {
                nodes[i] = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
            }
            return new TransferableNode(nodes);
        }
        return null;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDataFlavorSupported(nodeFlavor)) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();

        return newParent != null && newParent.getAllowsChildren();
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();

        try {
            Transferable transferable = support.getTransferable();
            if (transferable.isDataFlavorSupported(nodeFlavor)) {
                DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) transferable.getTransferData(nodeFlavor);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                for (DefaultMutableTreeNode node : nodes) {
                    if (!isNodeDescendant(node, newParent)) {
                        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
                        if (oldParent != null) {
                            model.removeNodeFromParent(node);
                        }

                        model.insertNodeInto(node, newParent, newParent.getChildCount());
                    }
                }

                tree.expandPath(dl.getPath());
                CLArgUtils.expandAllNodes(tree, dl.getPath());

                return true;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
            System.out.println("Outside area");
        }
        return false;
    }

    private boolean isNodeDescendant(DefaultMutableTreeNode node, DefaultMutableTreeNode potentialDescendant) {
        if (potentialDescendant == null) {
            return false;
        }

        TreeNode[] path = potentialDescendant.getPath();
        for (TreeNode treeNode : path) {
            if (treeNode == node) {
                return true;
            }
        }
        return false;
    }

    static class TransferableNode implements Transferable {
        private final DefaultMutableTreeNode[] nodes;

        public TransferableNode(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{nodeFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(nodeFlavor);
        }

        @Override
        public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return nodes;
        }
    }
}
