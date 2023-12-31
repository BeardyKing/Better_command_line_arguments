package com.github.beardyking.simpleclargs.ui;

import com.github.beardyking.simpleclargs.renderer.CLArgsRenderers;
import com.github.beardyking.simpleclargs.serialization.NodeDataJsonParser;
import com.github.beardyking.simpleclargs.transferHandler.CheckboxTreeTransferHandler;
import com.github.beardyking.simpleclargs.types.NodeData;
import com.github.beardyking.simpleclargs.utils.CLArgUtils;
import com.github.beardyking.simpleclargs.utils.EnvironmentVariableExtractor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.sort;

public class CLArgumentTree {
    public static String saveFileName = "CLArgs.json";
    public final JPanel frame = new JPanel(new BorderLayout());
    static Dimension squareButtonSize = new Dimension(32, 32);

    public void updateClargData(String inCommandLineArguments) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        VirtualFile workspaceFile = project.getWorkspaceFile();
        assert workspaceFile != null;
        System.out.println(workspaceFile.getName());

        // if we don't resolve environment variable we will have extra "<" and ">" which will break the XML in workspace.xml
        inCommandLineArguments = inCommandLineArguments.replaceAll("<", "&lt;");
        inCommandLineArguments = inCommandLineArguments.replaceAll(">", "&gt;");

        try {
            String xmlContent = new String(workspaceFile.contentsToByteArray(), workspaceFile.getCharset());
            String tagToSearch = "<configuration name=";
            String attributeToSearch = "type=\"CMakeRunConfiguration\"";

            int startIndex = 0;
            while ((startIndex = xmlContent.indexOf(tagToSearch, startIndex)) != -1) {
                int endIndex = xmlContent.indexOf(">", startIndex) + 1;
                String configurationContent = xmlContent.substring(startIndex, endIndex);

                Pattern pattern = Pattern.compile("PROGRAM_PARAMS=\"[^\"]*\"");
                Matcher matcher = pattern.matcher(configurationContent);

                if (matcher.find()) {
                    String match = matcher.group();
                    configurationContent = configurationContent.replaceAll(match, "PROGRAM_PARAMS=\"" + inCommandLineArguments + "\"");
                } else {
                    // likely did not find `PROGRAM_PARAMS` as it doesn't exist in the XML file
                    // so add `PROGRAM_PARAMS` to `configuration`  in the workspace virtual file
                    configurationContent = configurationContent.replaceAll(">", " PROGRAM_PARAMS=\"" + inCommandLineArguments + "\">");
                }
                xmlContent = xmlContent.substring(0, startIndex) + configurationContent + xmlContent.substring(endIndex);
                startIndex = endIndex;
            }

            Application application = ApplicationManager.getApplication();
            final String finalXmlContent = xmlContent;
            application.invokeLater(() -> application.runWriteAction(() -> {
                try {
                    VfsUtil.saveText(workspaceFile, finalXmlContent);
                    workspaceFile.refresh(true, true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public CLArgumentTree() {
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("CL Args", createTreeTab());
        tabbedPane.addTab("CL Vars", createTableTab());

        toggleTreeOnMiddleMouseClick(tree);

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static String updateNodeTexts(DefaultMutableTreeNode node) {
        StringBuilder labelText = new StringBuilder();

        if (node.getUserObject() instanceof NodeData nodeData) {
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

    public static DefaultMutableTreeNode rootNode;
    static JEditorPane nodeTextLabel = new JEditorPane();
    static Tree tree;

    private void toggleTreeOnMiddleMouseClick(JTree tree) {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());

                    if (clickedPath != null) {
                        Rectangle checkBoxBounds = tree.getPathBounds(clickedPath);
                        if (checkBoxBounds != null && checkBoxBounds.contains(e.getPoint())) {
                            toggleSelectedNodes(tree);
                            updatePreviewNodeText();
                            CLArgUtils.saveCommandTreeToFile();
                            updateClargData(updateNodeTexts(rootNode));
                            tree.repaint();
                        }
                    }
                }
            }
        });
    }

    private JPanel createTreeTab() {
        JPanel frame = new JBPanel<>(new BorderLayout());
        NodeDataJsonParser jsonParser = new NodeDataJsonParser();
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        String basePath = project.getBasePath();
        String saveFilePath = basePath + "/.idea/" + saveFileName;
        rootNode = jsonParser.loadNodeDataTreeFromJson(saveFilePath);

        if (rootNode == null) {
            rootNode = new DefaultMutableTreeNode(new NodeData(true, "CL Args", false));
            DefaultMutableTreeNode leafEnv = new DefaultMutableTreeNode(new NodeData(false, "-environment variables <PROCESSOR_ARCHITECTURE>", true));
            rootNode.add(leafEnv);
        }

        //==Tree settings===========
        CLArgsRenderers.CustomTreeModel treeModel = new CLArgsRenderers.CustomTreeModel(rootNode);
        tree = new Tree(treeModel);

        tree.setEditable(true);
        tree.setCellRenderer(new CLArgsRenderers.EditableCheckboxTreeCellRenderer());
        tree.setCellEditor(new CLArgsRenderers.EditableCheckboxTreeCellEditor());
        tree.setToggleClickCount(0);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new CheckboxTreeTransferHandler(tree));
        tree.setUI(new CLArgsTreeUI());

        CLArgUtils.expandAllNodes(tree, rootNode);

        nodeTextLabel.setEditable(false);
        nodeTextLabel.setOpaque(false);

        JBScrollPane nodeTextScrollPane = new JBScrollPane(nodeTextLabel);
        nodeTextScrollPane.setPreferredSize(new Dimension(400, 100));

        JPanel treePanel = new JBPanel<>(new BorderLayout());
        treePanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        //==Setup buttons===========
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        //==Add node button=========
        JButton addNodeButton = new JButton((AllIcons.General.Add));
        addNodeButton.setPreferredSize(squareButtonSize);
        String addTooltipText = "<html>Add CLArg to selected folder<br>Shortcut: \"Insert\"</html>";
        addNodeButton.setToolTipText(addTooltipText);
        addNodeButton.addActionListener(e -> addNode(tree, new NodeData()));

        //==Remove node button======
        JButton removeNodeButton = new JButton((AllIcons.General.Remove));
        removeNodeButton.setPreferredSize(squareButtonSize);
        String removeTooltipText = "<html>Remove All Selected CLArgs<br>Shortcut: \"Delete\"</html>";
        removeNodeButton.setToolTipText(removeTooltipText);
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

        //==Folder node button======
        JButton folderNodeButton = new JButton((AllIcons.Nodes.Folder));
        folderNodeButton.setPreferredSize(squareButtonSize);
        String folderTooltipText = "<html>Add All Selected CLArgs to folder<br>Shortcut: \"Home\"</html>";
        folderNodeButton.setToolTipText(folderTooltipText);
        folderNodeButton.addActionListener(e -> moveSelectedNodesToFolder(tree));

        //==Folder node move up=====
        JButton moveUpNodeButton = new JButton((AllIcons.General.ArrowUp));
        moveUpNodeButton.setPreferredSize(squareButtonSize);
        String moveUpTooltipText = "<html>Move Selected CLArgs up in folder<br>Shortcut: \"Alt+Up\"</html>";
        moveUpNodeButton.setToolTipText(moveUpTooltipText);
        moveUpNodeButton.addActionListener(e -> moveSelectedNodesUp(tree));

        //==Folder node move down===
        JButton moveDownNodeButton = new JButton((AllIcons.General.ArrowDown));
        moveDownNodeButton.setPreferredSize(squareButtonSize);
        String moveDownTooltipText = "<html>Move Selected CLArgs down in folder<br>Shortcut: \"Alt+Down\"</html>";
        moveDownNodeButton.setToolTipText(moveDownTooltipText);
        moveDownNodeButton.addActionListener(e -> moveSelectedNodesDown(tree));

        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && e.isAltDown()) {
                    moveSelectedNodesUp(tree);
                }
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isAltDown()) {
                    moveSelectedNodesDown(tree);
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
                    CLArgUtils.expandAllNodes(tree, rootNode);
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
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    toggleSelectedNodes(tree);
                    updatePreviewNodeText();
                    CLArgUtils.saveCommandTreeToFile();
                    updateClargData(updateNodeTexts(rootNode));
                    tree.repaint();
                }
            }
        });

        //==START: debug code=======
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    updateClargData(updateNodeTexts(rootNode));
                }
            }
        });
        //==END: debug code=========

        String labelText = updateNodeTexts(rootNode);
        nodeTextLabel.setText(labelText);

        buttonPanel.add(addNodeButton);
        buttonPanel.add(removeNodeButton);
        buttonPanel.add(folderNodeButton);
        buttonPanel.add(moveUpNodeButton);
        buttonPanel.add(moveDownNodeButton);

        frame.add(treePanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(nodeTextScrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);

        treeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                updatePreviewNodeText();
                CLArgUtils.saveCommandTreeToFile();
                updateClargData(updateNodeTexts(rootNode));
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                updatePreviewNodeText();
                CLArgUtils.saveCommandTreeToFile();
                updateClargData(updateNodeTexts(rootNode));
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                updatePreviewNodeText();
                CLArgUtils.saveCommandTreeToFile();
                updateClargData(updateNodeTexts(rootNode));
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                updatePreviewNodeText();
                CLArgUtils.saveCommandTreeToFile();
                updateClargData(updateNodeTexts(rootNode));
            }
        });
        return frame;
    }

    private void moveSelectedNodesToFolder(JTree tree) {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) {
            return;
        }

        DefaultMutableTreeNode firstSelectedNode = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) firstSelectedNode.getParent();
        if (parent == null) {
            return;
        }

        CLArgsRenderers.CustomTreeModel treeModel = (CLArgsRenderers.CustomTreeModel) tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new NodeData(true, "new folder", false));

        for (TreePath selectedPath : selectedPaths) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            treeModel.removeNodeFromParent(selectedNode);
            treeModel.insertNodeIntoWithoutCollapse(selectedNode, newNode);
        }

        treeModel.insertNodeIntoWithoutCollapse(newNode, parent);

        TreePath newNodePath = new TreePath(newNode.getPath());
        tree.expandPath(newNodePath);
        tree.setSelectionPath(newNodePath);

        expandAllNodes(tree, newNodePath);
    }

    private void moveSelectedNodesUp(JTree tree) {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths != null && selectedPaths.length > 0) {
            sort(selectedPaths, (path1, path2) -> {
                DefaultMutableTreeNode n1 = (DefaultMutableTreeNode) path1.getLastPathComponent();
                DefaultMutableTreeNode pn1 = (DefaultMutableTreeNode) n1.getParent();

                DefaultMutableTreeNode n2 = (DefaultMutableTreeNode) path2.getLastPathComponent();
                DefaultMutableTreeNode pn2 = (DefaultMutableTreeNode) n2.getParent();

                int index1 = pn1.getIndex(n1);
                int index2 = pn2.getIndex(n2);
                return Integer.compare(index1, index2);
            });

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            Vector<DefaultMutableTreeNode> movedNodes = new Vector<>();

            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();

                if (parent != null) {
                    int selectedIndex = parent.getIndex(selectedNode);

                    if (selectedIndex > 0) {
                        model.removeNodeFromParent(selectedNode);
                        model.insertNodeInto(selectedNode, parent, selectedIndex - 1);

                        movedNodes.add(selectedNode);
                    }
                }
            }

            // Update the selection paths to keep the moved nodes selected
            TreePath[] newSelectionPaths = new TreePath[movedNodes.size()];
            for (int i = 0; i < movedNodes.size(); i++) {
                newSelectionPaths[i] = new TreePath(movedNodes.get(i).getPath());
            }
            tree.setSelectionPaths(newSelectionPaths);
        }
    }


    private void moveSelectedNodesDown(JTree tree) {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths != null && selectedPaths.length > 0) {
            sort(selectedPaths, (path1, path2) -> {
                DefaultMutableTreeNode n1 = (DefaultMutableTreeNode) path1.getLastPathComponent();
                DefaultMutableTreeNode pn1 = (DefaultMutableTreeNode) n1.getParent();

                DefaultMutableTreeNode n2 = (DefaultMutableTreeNode) path2.getLastPathComponent();
                DefaultMutableTreeNode pn2 = (DefaultMutableTreeNode) n2.getParent();

                int index1 = pn1.getIndex(n1);
                int index2 = pn2.getIndex(n2);
                return Integer.compare(index2, index1);
            });

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            Vector<DefaultMutableTreeNode> movedNodes = new Vector<>();

            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();

                if (parent != null) {
                    int selectedIndex = parent.getIndex(selectedNode);
                    int parentSize = parent.getChildCount();

                    if (selectedIndex >= 0 && selectedIndex < parentSize - 1) {
                        model.removeNodeFromParent(selectedNode);
                        model.insertNodeInto(selectedNode, parent, selectedIndex + 1);

                        movedNodes.add(selectedNode);
                    }
                }
            }

            TreePath[] newSelectionPaths = new TreePath[movedNodes.size()];
            for (int i = 0; i < movedNodes.size(); i++) {
                newSelectionPaths[i] = new TreePath(movedNodes.get(i).getPath());
            }
            tree.setSelectionPaths(newSelectionPaths);
        }
    }


    private void toggleSelectedNodes(JTree tree) {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) {
            return;
        }

        DefaultMutableTreeNode firstSelectedNode = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) firstSelectedNode.getParent();
        if (parent == null) {
            return;
        }

        boolean firstSelectedToggleState = false;
        if (firstSelectedNode.getUserObject() instanceof NodeData firstNode) {
            firstSelectedToggleState = firstNode.isSelected();
        }

        for (TreePath selectedPath : selectedPaths) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof NodeData nodeData) {
                nodeData.setSelected(!firstSelectedToggleState);
            }
        }
    }

    private void expandAllNodes(JTree tree, TreePath parentPath) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            TreePath childPath = parentPath.pathByAddingChild(childNode);
            tree.expandPath(childPath);
            expandAllNodes(tree, childPath);
        }
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

        tableModel.addTableModelListener(e -> {
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
        });

        JButton addButton = new JButton((AllIcons.General.Add));
        addButton.setPreferredSize(squareButtonSize);

        JButton removeButton = new JButton((AllIcons.General.Remove));
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

        JPanel buttonPanel2 = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        buttonPanel2.add(addButton);
        buttonPanel2.add(removeButton);

        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(buttonPanel2, BorderLayout.NORTH);

        JBScrollPane tableScrollPane = new JBScrollPane(table);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        return tablePanel;
    }


    private static void addNode(JTree tree, NodeData newRawNodeData) {
        CLArgsRenderers.CustomTreeModel treeModel = (CLArgsRenderers.CustomTreeModel) tree.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();

        // Check if the root has children
        if (rootNode.getChildCount() == 0) {
            // If there are no children, add the new node directly under the root
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newRawNodeData);
            treeModel.insertNodeIntoWithoutCollapse(newNode, rootNode);

            // Create the path to the newly added node
            TreePath newNodePath = new TreePath(newNode.getPath());

            // Set the newly added node as the selected node
            tree.setSelectionPath(newNodePath);
        } else {
            // Handle the case where a node is selected and add the new node accordingly
            TreePath[] selectedPaths = tree.getSelectionPaths();
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (selectedPaths != null) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newRawNodeData);

                if (selectedNode == null) {
                    selectedNode = rootNode; // Select the root if no node is selected
                } else if (selectedNode.isLeaf()) {
                    selectedNode = (DefaultMutableTreeNode) selectedNode.getParent();
                }

                treeModel.insertNodeIntoWithoutCollapse(newNode, selectedNode);

                // Create the path to the newly added node
                TreePath newNodePath = new TreePath(newNode.getPath());

                // Set the newly added node as the selected node
                tree.setSelectionPath(newNodePath);
            }
        }
        tree.expandPath(new TreePath(rootNode.getPath()));
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

    private static String getEnvironmentVariable(String variableName) {
        String outStr = System.getenv(variableName);
        if (outStr == null) {
            outStr = "null";
        }
        return outStr;
    }

    public static boolean hasDisabledParent(DefaultMutableTreeNode node) {
        TreeNode[] path = node.getPath();
        for (TreeNode treeNode : path) {
            if (treeNode instanceof DefaultMutableTreeNode parentNode) {
                if (parentNode.getUserObject() instanceof NodeData parentNodeData) {
                    if (!parentNodeData.isSelected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void pasteClipboardText(JTree tree) {
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

                    CLArgsRenderers.CustomTreeModel treeModel = (CLArgsRenderers.CustomTreeModel) tree.getModel();
                    if (parent != null) {
                        if (selectedNode.isLeaf()) {
                            treeModel.insertNodeIntoWithoutCollapse(newNode, parent);
                        } else {
                            treeModel.insertNodeIntoWithoutCollapse(newNode, selectedNode);
                        }

                        TreePath parentPath = new TreePath(parent.getPath());
                        tree.expandPath(parentPath);
                    } else {
                        treeModel.insertNodeIntoWithoutCollapse(newNode, rootNode);

                        TreePath rootPath = new TreePath(rootNode.getPath());
                        tree.expandPath(rootPath);
                    }
                } else {
                    CLArgsRenderers.CustomTreeModel treeModel = (CLArgsRenderers.CustomTreeModel) tree.getModel();
                    treeModel.insertNodeIntoWithoutCollapse(newNode, rootNode);
                    TreePath rootPath = new TreePath(rootNode.getPath());
                    tree.expandPath(rootPath);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}