package com.github.beardyking.simpleclargs.toolWindow;
//
//import com.intellij.openapi.project.DumbAware;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowFactory;
//import com.intellij.ui.*;
//import com.intellij.ui.components.*;
//import com.intellij.ui.content.Content;
//import com.intellij.ui.content.ContentFactory;
//import com.intellij.ui.treeStructure.treetable.TreeTable;
//import com.intellij.ui.treeStructure.treetable.TreeTableModel;
//import com.intellij.ui.treeStructure.treetable.TreeTableTree;
//import com.intellij.util.ui.StatusText;
//import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities;
//import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
//import org.jetbrains.annotations.NotNull;
//import com.intellij.ui.treeStructure.Tree;
//
//import javax.swing.*;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.tree.*;
//import java.awt.*;
//
//import java.awt.Container;
//import java.awt.FlowLayout;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.util.Enumeration;
//import java.util.Set;
//import java.util.Vector;
//
//import javax.swing.JFrame;
//import javax.swing.JScrollPane;
//import javax.swing.JTree;
//import javax.swing.tree.DefaultMutableTreeNode;
//
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.MouseEvent;
//import java.util.EventObject;
//
////class CLArgs {
////    public boolean active = true;
////    public String inputText = "butts";
////}
//
//class NTree {
////
////    class NodeTreeRenderer extends CheckboxTree.CheckboxTreeCellRenderer{
////
////    }
//
//    static class NodeRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
//        private final JBCheckBox checkbox = new JBCheckBox();
//
//
//    }
//
//    static class Node extends JLabel {
//        int data;
//
//        // List of children
//        Node children[];
//
//        Node(int n, int data) {
//            children = new Node[n];
//            this.data = data;
//        }
//
////        @Override
////        void
//
//    }
//
//    static void buildArguments(Node node, StringBuilder builder) {
//
//        if (node == null)
//            return;
//
//        int total = node.children.length;
//        for (int i = 0; i < total - 1; i++) {
//            buildArguments(node.children[i], builder);
//        }
//        builder.append(node.data).append(" ");
//        buildArguments(node.children[total - 1], builder);
//    }
//}
//
//
//public class CLArgsToolWindow implements ToolWindowFactory, DumbAware {
//
//
//    @Override
//    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        CLArgToolWindowContent toolWindowContent = new CLArgToolWindowContent(toolWindow);
//        Content content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false);
//        toolWindow.getContentManager().addContent(content);
//    }
//
//    public static void expandAllNodes(DefaultMutableTreeNode treeNode,
//                                      JTree tree) {
//        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
//        Enumeration enum1 = treeNode.breadthFirstEnumeration();
//        while (enum1.hasMoreElements()) {
//            treeNode = (DefaultMutableTreeNode) enum1.nextElement();
//            if (treeNode.getChildCount() > 0) {
//                TreePath path = new TreePath(model.getPathToRoot(treeNode));
//                tree.expandPath(path);
//            }
//        }
//    }
//
//    private class CLArgToolWindowContent {
//
//        public final JPanel contentPanel = new JPanel();
//
//        private JTree tree;
//        private JLabel selectedLabel;
//        SpringLayout layout = new SpringLayout();
//
//        private void buildWindowLayout() {
//            layout.putConstraint(SpringLayout.WEST, tree, 0, SpringLayout.WEST, contentPanel);
//            layout.putConstraint(SpringLayout.EAST, tree, 0, SpringLayout.EAST, contentPanel);
//            layout.putConstraint(SpringLayout.NORTH, tree, 0, SpringLayout.NORTH, contentPanel);
//        }
//
//        public CLArgToolWindowContent(ToolWindow toolWindow) {
//
//
//            CheckBoxNode accessibilityOptions[] = {
//                    new CheckBoxNode("Move system caret with focus/selection changes", false),
//                    new CheckBoxNode("Always expand alt text for images", true)
//            };
//
//            Vector accessVector = new NamedVector("Accessibility", accessibilityOptions);
//
//            Object rootNodes[] = {accessVector};
//            Vector rootVector = new NamedVector("Root", rootNodes);
//            tree = new JTree(rootVector);
//            CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
//
//            tree.setCellRenderer(renderer);
//            tree.setCellEditor(new CheckBoxNodeEditor(tree));
//            tree.setEditable(true);
//            buildWindowLayout();
//            contentPanel.setLayout(layout);
//            contentPanel.add(tree);
//        }
//    }
//
//    //
////            tree = new JTree();
////            tree.setEditable(true);
////            DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
////
////            String elements[] = {"Root", "chartreuse", "rugby", "sushi"};
////            JComboBox comboBox = new JComboBox(elements);
////            comboBox.setEditable(true);
////
////            TreeCellEditor comboEditor = new DefaultCellEditor(comboBox);
////            TreeCellEditor editor = new DefaultTreeCellEditor(tree, renderer, comboEditor);
////            tree.setCellEditor(editor);
////
////            contentPanel.add(tree);
////            buildWindowLayout();
////            contentPanel.setLayout(layout);
////            contentPanel.setVisible(true);
//
//}
//
//class CheckBoxNodeRenderer implements TreeCellRenderer {
//    private JBCheckBox leafRenderer = new JBCheckBox();
////    private JCheckBox leafRenderer = new JCheckBox();
//
//    private DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
//
//    Color selectionBorderColor, selectionForeground, selectionBackground, textForeground, textBackground;
//
//    protected JBCheckBox getLeafRenderer() {
//        return leafRenderer;
//    }
//
//    public CheckBoxNodeRenderer() {
//        Font fontValue;
//        fontValue = UIManager.getFont("Tree.font");
//        if (fontValue != null) {
//            leafRenderer.setFont(fontValue);
//        }
//        Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
//        leafRenderer.setFocusPainted((booleanValue != null) && (booleanValue.booleanValue()));
//
//        selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
//        selectionForeground = UIManager.getColor("Tree.selectionForeground");
//        selectionBackground = UIManager.getColor("Tree.selectionBackground");
//        textForeground = UIManager.getColor("Tree.textForeground");
//        textBackground = UIManager.getColor("Tree.textBackground");
//    }
//
//    public Component getTreeCellRendererComponent(JTree tree, Object value,
//                                                  boolean selected, boolean expanded, boolean leaf, int row,
//                                                  boolean hasFocus) {
//
//        Component returnValue;
//        if (leaf) {
//
//            String stringValue = tree.convertValueToText(value, selected,
//                    expanded, leaf, row, false);
//            leafRenderer.setText(stringValue);
//            leafRenderer.setSelected(false);
//
//            leafRenderer.setEnabled(tree.isEnabled());
//
//            if (selected) {
//                leafRenderer.setForeground(selectionForeground);
//                leafRenderer.setBackground(selectionBackground);
//            } else {
//                leafRenderer.setForeground(textForeground);
//                leafRenderer.setBackground(textBackground);
//            }
//
//            if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
//                Object userObject = ((DefaultMutableTreeNode) value)
//                        .getUserObject();
//                if (userObject instanceof CheckBoxNode) {
//                    CheckBoxNode node = (CheckBoxNode) userObject;
//                    leafRenderer.setText(node.getText() + "4");
//                    leafRenderer.setSelected(node.isSelected());
//                }
//            }
//            returnValue = leafRenderer;
//        } else {
//            returnValue = nonLeafRenderer.getTreeCellRendererComponent(tree,
//                    value, selected, expanded, leaf, row, hasFocus);
//        }
//        return returnValue;
//    }
//}
//
//class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
//
//    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
//
//    ChangeEvent changeEvent = null;
//
//    JTree tree;
//
//    public CheckBoxNodeEditor(JTree tree) {
//        this.tree = tree;
//    }
//
//    public Object getCellEditorValue() {
//        JBCheckBox checkbox = renderer.getLeafRenderer();
//        CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(),
//                checkbox.isSelected());
//        return checkBoxNode;
//    }
//
//    public boolean isCellEditable(EventObject event) {
//        boolean returnValue = false;
//        if (event instanceof MouseEvent) {
//            MouseEvent mouseEvent = (MouseEvent) event;
//            TreePath path = tree.getPathForLocation(mouseEvent.getX(),
//                    mouseEvent.getY());
//            if (path != null) {
//                Object node = path.getLastPathComponent();
//                if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
//                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
//                    Object userObject = treeNode.getUserObject();
//                    returnValue = ((treeNode.isLeaf()) && (userObject instanceof CheckBoxNode));
//                }
//            }
//        }
//        return returnValue;
//    }
//
//    public Component getTreeCellEditorComponent(JTree tree, Object value,
//                                                boolean selected, boolean expanded, boolean leaf, int row) {
//
//        Component editor = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
//
//        // editor always selected / focused
//        ItemListener itemListener = new ItemListener() {
//            public void itemStateChanged(ItemEvent itemEvent) {
//                if (stopCellEditing()) {
//                    fireEditingStopped();
//                }
//            }
//        };
//        if (editor instanceof JCheckBox) {
//            ((JBCheckBox) editor).addItemListener(itemListener);
//        }
//
//        return editor;
//    }
//}
//
//class CheckBoxNode {
//    //    String text;
//    JBTextArea text = new JBTextArea();
//
//    boolean selected;
//
//    public CheckBoxNode(String text, boolean selected) {
//        this.text.setText(text);
//        this.selected = selected;
//    }
//
//    public boolean isSelected() {
//        return selected;
//    }
//
//    public void setSelected(boolean newValue) {
//        selected = newValue;
//    }
//
//    public String getText() {
//        return text.getText();
//    }
//
//    public void setText(String newValue) {
//        text.setText(newValue);
//    }
//
//    public String toString() {
//        return getClass().getName() + "[" + text + "/" + selected + "]";
//    }
//}
//
//class NamedVector extends Vector {
//    String name;
//
//    public NamedVector(String name) {
//        this.name = name;
//    }
//
//    public NamedVector(String name, Object elements[]) {
//        this.name = name;
//        for (int i = 0, n = elements.length; i < n; i++) {
//            add(elements[i]);
//        }
//    }
//
//    public String toString() {
//        return "[" + name + "]";
//    }
//}
