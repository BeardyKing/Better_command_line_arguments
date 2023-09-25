package com.github.beardyking.simpleclargs.toolWindow;

import java.awt.*;
import java.awt.BorderLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.EventObject;

import static com.github.beardyking.simpleclargs.toolWindow.CLArgumentsToolWindow.CLArgumentTree.*;
import static java.util.Arrays.sort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.*;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;

public class CLArgumentsToolWindow implements ToolWindowFactory, DumbAware {
    String filePath = "CLArgs.json";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CLArgumentsToolWindow.CLArgumentTree toolWindowContent = new CLArgumentTree(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.frame, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public class CLArgumentTree {
        public final JPanel frame = new JPanel(new BorderLayout());
        static Dimension squareButtonSize = new Dimension(32, 32);


        public CLArgumentTree(ToolWindow toolWindow) {
            JBTabbedPane tabbedPane = new JBTabbedPane();
            tabbedPane.addTab("CLArgs", createTreeTab());
            tabbedPane.addTab("CL Vars", createTableTab());

            frame.add(tabbedPane, BorderLayout.CENTER);
            frame.setVisible(true);
        }

        private static String updateNodeTexts(DefaultMutableTreeNode node) {
            StringBuilder labelText = new StringBuilder();

            if (node.getUserObject() instanceof NodeData) {
                NodeData nodeData = (NodeData) node.getUserObject();
                if (nodeData.isSelected() && !hasDisabledParent(node) && node.isLeaf()) {
                    labelText.append(nodeData.getText()).append(" ");
                }
            }

            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
                labelText.append(updateNodeTexts(childNode));
            }
            return EnvironmentVariableExtractor.extractEnvironmentVariables(labelText.toString());
        }

        static DefaultMutableTreeNode rootNode;
        static JEditorPane nodeTextLabel = new JEditorPane();
        static Tree tree;

        private JPanel createTreeTab() {
            JPanel frame = new JPanel(new BorderLayout());
            NodeDataJsonParser jsonParser = new NodeDataJsonParser();
            rootNode = jsonParser.loadNodeDataTreeFromJson(filePath);

            if (rootNode == null) {
                rootNode = new DefaultMutableTreeNode(new NodeData(true, "root", false));
                DefaultMutableTreeNode leafEnv = new DefaultMutableTreeNode(new NodeData(false, "-CLA environment variables <CLion>", true));
                rootNode.add(leafEnv);
            }

            CustomTreeModel treeModel = new CustomTreeModel(rootNode);
            tree = new Tree(treeModel);
            tree.setEditable(true);
            tree.setCellRenderer(new EditableCheckboxTreeCellRenderer(tree));
            tree.setCellEditor(new EditableCheckboxTreeCellEditor(tree));

            tree.setDragEnabled(true);
            tree.setDropMode(DropMode.ON_OR_INSERT);
            tree.setTransferHandler(new CheckboxTreeTransferHandler(tree));

            jsonParser.expandAllNodes(tree, rootNode);

            nodeTextLabel.setEditable(false);
            nodeTextLabel.setOpaque(false);

            JScrollPane nodeTextScrollPane = new JScrollPane(nodeTextLabel);
            nodeTextScrollPane.setPreferredSize(new Dimension(400, 100));

            JPanel treePanel = new JPanel(new BorderLayout());
            treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addNodeButton = new JButton((AllIcons.General.Add));
            addNodeButton.setPreferredSize(squareButtonSize);

            JButton removeNodeButton = new JButton((AllIcons.General.Remove));
            removeNodeButton.setPreferredSize(squareButtonSize);

            JButton folderNodeButton = new JButton((AllIcons.Nodes.Folder));
            folderNodeButton.setPreferredSize(squareButtonSize);

            folderNodeButton.addActionListener(e -> {
                moveSelectedNodesToFolder(tree);
            });

            addNodeButton.addActionListener(e -> {
                addNode(tree, new NodeData());
            });

            removeNodeButton.addActionListener(e -> {
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if (selectedPaths != null) {
                    for (TreePath selectedPath : selectedPaths) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
                        if (parentNode != null) {
                            treeModel.removeNodeFromParent(selectedNode);
                        }
                    }
                }
            });

            tree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_V && e.isControlDown()) {
                        pasteClipboardText(tree);
                    }
                }
            });
            tree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        deleteSelectedNodes(tree);
                    }
                }
            });
            tree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_HOME) {
                        moveSelectedNodesToFolder(tree);
                    }
                }
            });
            tree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                        addNode(tree, new NodeData());
                    }
                }
            });

            String labelText = updateNodeTexts(rootNode);
            nodeTextLabel.setText(labelText);

            buttonPanel.add(addNodeButton);
            buttonPanel.add(removeNodeButton);
            buttonPanel.add(folderNodeButton);

            frame.add(treePanel, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.NORTH);
            frame.add(nodeTextScrollPane, BorderLayout.SOUTH);

            frame.setVisible(true);

            treeModel.addTreeModelListener(new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent e) {
                    updatePreviewNodeText();
                    saveCommandTreeToFile();
                }

                @Override
                public void treeNodesInserted(TreeModelEvent e) {
                    updatePreviewNodeText();
                    saveCommandTreeToFile();
                }

                @Override
                public void treeNodesRemoved(TreeModelEvent e) {
                    updatePreviewNodeText();
                    saveCommandTreeToFile();
                }

                @Override
                public void treeStructureChanged(TreeModelEvent e) {
                    updatePreviewNodeText();
                    saveCommandTreeToFile();
                }
            });
            return frame;
        }

        private void moveSelectedNodesToFolder(JTree tree) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null) {
                return;
            }
            CustomTreeModel treeModel = (CustomTreeModel) tree.getModel();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new NodeData(true, "new folder", false));

            treeModel.insertNodeIntoWithoutCollapse(newNode, parent);
            treeModel.removeNodeFromParent(selectedNode);
            treeModel.insertNodeIntoWithoutCollapse(selectedNode, newNode);
            tree.expandPath(new TreePath(newNode.getPath()));
        }

        public static void updatePreviewNodeText() {
            String labelText = updateNodeTexts(rootNode);
            nodeTextLabel.setText(labelText);

        }

        private JPanel createTableTab() {
            JPanel tablePanel = new JPanel(new BorderLayout());


            DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Variable Name", "Environment Variable Lookup"}, 0);
            JTable table = new JTable(tableModel) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 1;
                }
            };

            table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (!table.isCellEditable(row, column)) {
                        component.setForeground(JBColor.gray);
                    } else {
                        component.setForeground(table.getForeground());
                        component.setBackground(table.getBackground());
                    }
                    return component;
                }
            });

            String defaultString = "CLion";
            tableModel.addRow(new Object[]{defaultString, getEnvironmentVariable(defaultString)});

            tableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                        int row = e.getFirstRow();
                        String variableName = (String) tableModel.getValueAt(row, 0);
                        String environmentValue = getEnvironmentVariable(variableName);

                        if (!environmentValue.isEmpty()) {
                            table.getColumnModel().getColumn(1).setCellEditor(null);
                        } else {
                            table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()));
                        }

                        tableModel.setValueAt(environmentValue, row, 1);
                    }
                }
            });

            JButton addButton = new JButton("+");
            addButton.setPreferredSize(squareButtonSize);

            JButton removeButton = new JButton("-");
            removeButton.setPreferredSize(squareButtonSize);


            addButton.addActionListener(e -> {
                int[] rows = table.getSelectedRows();
                sort(rows);

                int rowInsertIndex = 0;
                if (rows.length != 0) {
                    rowInsertIndex = rows[rows.length - 1] + 1;
                }

                tableModel.insertRow(rowInsertIndex, new Object[]{"", ""});
                tableModel.fireTableDataChanged();
                table.getSelectionModel().addSelectionInterval(rowInsertIndex, rowInsertIndex);
            });

            removeButton.addActionListener(e -> {
                if (table.getSelectedRows().length == 0) {
                    return;
                }

                int[] rows = table.getSelectedRows();
                sort(rows);
                int rowRemoveIndex = rows[0];

                for (int i = rows.length; i-- > 0; ) {
                    tableModel.removeRow(rows[i]);
                }
                table.getSelectionModel().addSelectionInterval(rowRemoveIndex - 1, rowRemoveIndex - 1);
            });

            JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel2.add(addButton);
            buttonPanel2.add(removeButton);

            tablePanel.setLayout(new BorderLayout());
            tablePanel.add(buttonPanel2, BorderLayout.NORTH);

            JScrollPane tableScrollPane = new JScrollPane(table);
            tablePanel.add(tableScrollPane, BorderLayout.CENTER);

            return tablePanel;
        }

        private void pasteClipboardText(JTree tree) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transferable = clipboard.getContents(this);

            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String clipboardText = (String) transferable.getTransferData(DataFlavor.stringFlavor);

                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new NodeData(true, clipboardText, true));

                    if (selectedNode != null) {
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();

                        if (parent != null) {
                            int selectedIndex = parent.getIndex(selectedNode);
                            CustomTreeModel treeModel = (CustomTreeModel) tree.getModel();
                            if (selectedNode.isLeaf()) {
                                treeModel.insertNodeIntoWithoutCollapse(newNode, parent);
                            } else {
                                treeModel.insertNodeIntoWithoutCollapse(newNode, selectedNode);
                            }

                            TreePath parentPath = new TreePath(parent.getPath());
                            tree.expandPath(parentPath);
                        } else {
                            CustomTreeModel treeModel = (CustomTreeModel) tree.getModel();
                            treeModel.insertNodeIntoWithoutCollapse(newNode, rootNode);

                            TreePath rootPath = new TreePath(rootNode.getPath());
                            tree.expandPath(rootPath);
                        }
                    } else {
                        CustomTreeModel treeModel = (CustomTreeModel) tree.getModel();
                        treeModel.insertNodeIntoWithoutCollapse(newNode, rootNode);
                        TreePath rootPath = new TreePath(rootNode.getPath());
                        tree.expandPath(rootPath);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private static void addNode(JTree tree, NodeData newRawNodeData) {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                selectedNode = rootNode;
            }

            if (selectedPaths != null) {
                CustomTreeModel treeModel = (CustomTreeModel) tree.getModel();
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newRawNodeData);
                if (selectedNode.isLeaf()) {
                    selectedNode = (DefaultMutableTreeNode) selectedNode.getParent();
                }
                treeModel.insertNodeIntoWithoutCollapse(newNode, selectedNode);
            }
            tree.expandPath(new TreePath(selectedNode.getPath()));
        }

        static void deleteSelectedNodes(JTree tree) {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths != null) {
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                for (TreePath selectedPath : selectedPaths) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
                    if (parent != null) {
                        model.removeNodeFromParent(selectedNode);
                    }
                }
            }
        }

        private static void updateTableData(DefaultTableModel tableModel, DefaultMutableTreeNode rootNode) {
            tableModel.setRowCount(0);
            addNodeDataToTable(tableModel, rootNode);
        }

        private static void addNodeDataToTable(DefaultTableModel tableModel, DefaultMutableTreeNode node) {
            if (node.getUserObject() instanceof NodeData) {
                NodeData nodeData = (NodeData) node.getUserObject();
                tableModel.addRow(new Object[]{nodeData.getText(), getEnvironmentVariable(nodeData.getText())});
            }

            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
                addNodeDataToTable(tableModel, childNode);
            }
        }

        private static String getEnvironmentVariable(String variableName) {
            String outStr = System.getenv(variableName);
            if (outStr == null) {
                outStr = "null";
            }
            return outStr;
        }

        static boolean hasDisabledParent(DefaultMutableTreeNode node) {
            TreeNode[] path = node.getPath();
            for (int i = 0; i < path.length; i++) {
                if (path[i] instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path[i];
                    if (parentNode.getUserObject() instanceof NodeData) {
                        NodeData parentNodeData = (NodeData) parentNode.getUserObject();
                        if (!parentNodeData.isSelected()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

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
                    DefaultMutableTreeNode loadedRootNode = loadNodeFromJson(treeData);
                    return loadedRootNode;
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

        private void expandAllNodes(JTree tree, DefaultMutableTreeNode node) {
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

    public void saveCommandTreeToFile() {
        NodeDataJsonParser jsonParser = new NodeDataJsonParser();
        jsonParser.saveNodeDataTreeToJson(rootNode, filePath);
        //TODO:refactor all of this it's all pretty crusty :^(
    }

    class NodeData {
        private boolean isSelected;
        private String text;
        private boolean isLeaf;

        public NodeData(boolean isSelected, String text, boolean isLeaf) {
            this.isSelected = isSelected;
            this.text = text;
            this.isLeaf = isLeaf;
        }

        public NodeData() {
            this.isSelected = true;
            this.text = "-clarg";
            this.isLeaf = false;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isLeaf() {
            return isLeaf;
        }

        public void setLeaf(boolean leaf) {
            isLeaf = leaf;
        }
    }

    class EditableCheckboxTreeCellRenderer extends JPanel implements TreeCellRenderer {
        private JCheckBox checkBox = new JCheckBox();
        private JTextField textField = new JTextField();
        private JLabel iconLabel = new JLabel();
        private JTree tree;
        private Border defaultTextFieldBorder;
        private Color defaultTextFieldBackgroundColor;

        public EditableCheckboxTreeCellRenderer(JTree tree) {
            this.tree = tree;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            iconLabel.setPreferredSize(new Dimension(16, 16));

            defaultTextFieldBackgroundColor = textField.getBackground();
            defaultTextFieldBorder = textField.getBorder();

            add(iconLabel);
            add(checkBox);
            add(textField);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof NodeData) {
                    NodeData nodeData = (NodeData) node.getUserObject();
                    checkBox.setSelected(nodeData.isSelected());
                    textField.setText(nodeData.getText());
                    textField.requestFocusInWindow();

                    textField.setBackground(JBColor.WHITE);

                    if (selected) {
                        textField.setBorder(new LineBorder(JBColor.BLACK));
                    } else {
                        textField.setBorder(new LineBorder(JBColor.lightGray));
                    }

                    if (hasDisabledParent(node)) {
                        textField.setForeground(JBColor.gray);
                    } else {
                        textField.setForeground(JBColor.black);
                    }

                    if (leaf) {
                        iconLabel.setIcon(AllIcons.Actions.InSelection);
                    } else {
                        iconLabel.setIcon(AllIcons.Nodes.Folder);
                    }
                    tree.repaint();
                }
            }
            return this;
        }
    }

    private Color getBackgroundSelectionColor() {
        return UIManager.getColor("Tree.selectionBackground");
    }

    private Color defaultTextFieldBackgroundColor() {
        return UIManager.getColor("Tree.textBackground");
    }

    class EditableCheckboxTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {
        private DefaultMutableTreeNode editingNode;
        private JTextField textField = new JTextField();
        private JCheckBox checkBox = new JCheckBox();
        private JPanel editorComponent;
        private JLabel iconLabel = new JLabel();
        private JTree tree;


        public EditableCheckboxTreeCellEditor(JTree tree) {
            this.tree = tree;
            editorComponent = new JPanel(new BorderLayout());
            editorComponent.add(iconLabel, BorderLayout.WEST);
            editorComponent.add(checkBox, BorderLayout.CENTER);
            editorComponent.add(textField, BorderLayout.EAST);

            checkBox.addActionListener(e -> stopCellEditing());

            textField.addActionListener(e -> stopCellEditing());

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    stopCellEditing();
                    updatePreviewNodeText();
                    saveCommandTreeToFile();
                }
            });

            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        if (path != null) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                            if (tree.isExpanded(path)) {
                                tree.collapsePath(path);
                            } else {
                                tree.expandPath(path);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            if (value instanceof DefaultMutableTreeNode) {
                editingNode = (DefaultMutableTreeNode) value;
                if (editingNode.getUserObject() instanceof NodeData) {
                    NodeData nodeData = (NodeData) editingNode.getUserObject();
                    checkBox.setSelected(nodeData.isSelected());
                    textField.setText(nodeData.getText());
                    textField.setEditable(true);
                    textField.requestFocusInWindow();
                    if (leaf) {
                        iconLabel.setIcon(AllIcons.Actions.InSelection);
                    } else {
                        iconLabel.setIcon(AllIcons.Nodes.Folder);
                    }
                }
            }
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            if (editingNode != null && editingNode.getUserObject() instanceof NodeData) {
                NodeData nodeData = (NodeData) editingNode.getUserObject();
                nodeData.setText(textField.getText());
                nodeData.setSelected(checkBox.isSelected());
            }
            return editingNode.getUserObject();
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                JTree tree = (JTree) mouseEvent.getSource();
                TreePath path = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    return node.getUserObject() instanceof NodeData;
                }
            }
            return false;
        }
    }


    class CheckboxTreeTransferHandler extends TransferHandler {
        private JTree tree;
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
            return TransferHandler.COPY_OR_MOVE;
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
                DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(nodeFlavor);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                for (DefaultMutableTreeNode node : nodes) {
                    if (!newParent.getAllowsChildren()) {
                        continue;
                    }

                    if (!isNodeDescendant(node, newParent)) {
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node.getUserObject());
                        model.insertNodeInto(newNode, newParent, newParent.getChildCount());
                        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
                        if (oldParent != null) {
                            model.removeNodeFromParent(node);
                        }
                    }
                }

                tree.expandPath(dl.getPath());

                return true;
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
    }


    class TransferableNode implements Transferable {
        private DefaultMutableTreeNode[] nodes;

        public TransferableNode(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{CheckboxTreeTransferHandler.nodeFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(CheckboxTreeTransferHandler.nodeFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return nodes;
        }
    }

    class CustomTreeModel extends DefaultTreeModel {
        public CustomTreeModel(TreeNode root) {
            super(root);
        }

        public void insertNodeIntoWithoutCollapse(MutableTreeNode newChild, MutableTreeNode parent) {
            super.insertNodeInto(newChild, parent, parent.getChildCount());
        }
    }
}

