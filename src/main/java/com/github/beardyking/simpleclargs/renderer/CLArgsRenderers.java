package com.github.beardyking.simpleclargs.renderer;

import com.github.beardyking.simpleclargs.toolWindow.CLArgumentsToolWindow;
import com.github.beardyking.simpleclargs.types.NodeData;
import com.github.beardyking.simpleclargs.ui.CLArgumentTree;
import com.github.beardyking.simpleclargs.utils.CLArgUtils;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;


public class CLArgsRenderers {
    public static class CustomTreeModel extends DefaultTreeModel {
        public CustomTreeModel(TreeNode root) {
            super(root);
        }

        public void insertNodeIntoWithoutCollapse(MutableTreeNode newChild, MutableTreeNode parent) {
            super.insertNodeInto(newChild, parent, parent.getChildCount());
        }
    }

    public static class EditableCheckboxTreeCellRenderer extends JPanel implements TreeCellRenderer {
        private final JCheckBox checkBox = new JCheckBox();
        private final JTextField textField = new JTextField();
        private final JLabel iconLabel = new JLabel();

        public EditableCheckboxTreeCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            iconLabel.setPreferredSize(new Dimension(16, 16));

            add(iconLabel);
            add(checkBox);
            add(textField);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof NodeData nodeData) {
                    checkBox.setSelected(nodeData.isSelected());
                    textField.setText(nodeData.getText());
                    textField.requestFocusInWindow();

                    textField.setBackground(JBColor.WHITE);

                    if (selected) {
                        textField.setBorder(new LineBorder(JBColor.BLACK));
                    } else {
                        textField.setBorder(new LineBorder(JBColor.lightGray));
                    }

                    if (CLArgumentTree.hasDisabledParent(node)) {
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

    public static class EditableCheckboxTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {
        private DefaultMutableTreeNode editingNode;
        private final JTextField textField = new JTextField();
        private final JCheckBox checkBox = new JCheckBox();
        private final JPanel editorComponent;
        private final JLabel iconLabel = new JLabel();

        public EditableCheckboxTreeCellEditor() {
            editorComponent = new JPanel(new BorderLayout());
            editorComponent.add(iconLabel, BorderLayout.WEST);
            editorComponent.add(checkBox, BorderLayout.CENTER);
            editorComponent.add(textField, BorderLayout.EAST);
            textField.setPreferredSize(new Dimension(2048, 75));

            checkBox.addActionListener(e -> stopCellEditing());
            textField.addActionListener(e -> stopCellEditing());

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    stopCellEditing();
                    CLArgumentTree.updatePreviewNodeText();
                    CLArgUtils.saveCommandTreeToFile();
                }
            });
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            if (value instanceof DefaultMutableTreeNode) {
                editingNode = (DefaultMutableTreeNode) value;
                if (editingNode.getUserObject() instanceof NodeData nodeData) {
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
            if (editingNode != null && editingNode.getUserObject() instanceof NodeData nodeData) {
                nodeData.setText(textField.getText());
                nodeData.setSelected(checkBox.isSelected());
            }
            assert editingNode != null;
            return editingNode.getUserObject();
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            if (event instanceof MouseEvent mouseEvent) {
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
}
