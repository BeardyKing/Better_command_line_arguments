package com.github.beardyking.simpleclargs.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

import static java.util.Arrays.sort;

public class CLAToolWindow implements ToolWindowFactory, DumbAware {


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CLArgTree toolWindowContent = new CLArgTree(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    FlowLayout experimentLayout = new FlowLayout();

    public class CLArgTree {

        public final JPanel contentPanel = new JPanel();
        //        static ArrayList<CLArgs> data = new ArrayList<>();
        Object[] columnNames = {"Enabled", "CLArg"};
        static DefaultTableModel model;
        static JBTable table;

        public CLArgTree(ToolWindow toolWindow) {

            final JPanel buttonGroup = new JPanel();



            {
                JButton button = new JButton("+");
                button.setPreferredSize(new Dimension(30, 30));
                button.setSize(new Dimension(30, 30));
                button.addActionListener(e -> button_plus_action());
                buttonGroup.add(button);
            }
            {
                JButton button = new JButton("-");
                button.setPreferredSize(new Dimension(30, 30));
                button.setSize(new Dimension(30, 30));
                button.addActionListener(e -> button_minus_action());
                buttonGroup.add(button);
            }

            model = new DefaultTableModel(columnNames, 0);
            Object[] exampleCLArg0 = {true, "-load_map default.map"};
            Object[] exampleCLArg1 = {true, "-fly_cam 1"};
            Object[] exampleCLArg2 = {true, "-start_pos 30 100 30"};
            Object[] exampleCLArg3 = {false, "-launch_server 0"};
            model.addRow(exampleCLArg0);
            model.addRow(exampleCLArg1);
            model.addRow(exampleCLArg2);
            model.addRow(exampleCLArg3);

            table = new JBTable(model) {
                public Class getColumnClass(int column) {
                    switch (column) {
                        case 0:
                            return Boolean.class;
                        default:
                            return String.class;
                    }
                }
            };


            table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
            table.getColumnModel().getColumn(0).setPreferredWidth(64);
            table.getColumnModel().getColumn(1).setPreferredWidth(4096);
            table.setPreferredScrollableViewportSize(table.getPreferredSize());
            JBScrollPane scrollPane = new JBScrollPane(table);
            contentPanel.add(scrollPane);

            contentPanel.add(new JBLabel("CommandLineArgument:"));
            JBTextArea commandLineArgumentOutText = new JBTextArea();
            contentPanel.add(buttonGroup);
            contentPanel.add(commandLineArgumentOutText);

            SpringLayout layout = new SpringLayout();
            { // table
                layout.putConstraint(SpringLayout.WEST, scrollPane, 6, SpringLayout.WEST, contentPanel);
                layout.putConstraint(SpringLayout.EAST, scrollPane, 6, SpringLayout.EAST, contentPanel);
                layout.putConstraint(SpringLayout.NORTH, scrollPane, 64, SpringLayout.NORTH, contentPanel);
            }
            {
                layout.putConstraint(SpringLayout.NORTH, buttonGroup, 6);
            }
            contentPanel.setLayout(layout);
//
        }

        public static void button_plus_action() {
            int rowInsetIndex = 0;
            if (table.getSelectedRowCount() > 0) {
                rowInsetIndex = table.getSelectedRow() + 1;
            }
            Object[] rawData = {true, ""};
            model.insertRow(rowInsetIndex, rawData);
            model.fireTableDataChanged();
            table.getSelectionModel().addSelectionInterval(rowInsetIndex, rowInsetIndex);
        }

        public static void button_minus_action() {
            int[] rows = table.getSelectedRows();
            sort(rows);
            ArrayUtils.reverse(rows);

            for (int i = 0; i < rows.length; i++) {
                ((DefaultTableModel) table.getModel()).removeRow(rows[i]);
//                data.remove(rows[i]);
                System.out.println(rows[i]);
            }

            model.fireTableDataChanged();
            int smallestSelection = rows[rows.length - 1];
            table.getSelectionModel().addSelectionInterval(smallestSelection, smallestSelection);
        }

        public static void deselect_table_action() {
            table.getSelectionModel().clearSelection();
        }

        public static void toggle_table_action() {
            int[] rows = table.getSelectedRows();
            boolean firstSelectionState = (boolean) table.getModel().getValueAt(rows[0], 0);
            for (int i = 0; i < rows.length; i++) {
                table.getModel().setValueAt(!firstSelectionState, rows[i], 0);
            }
        }

//        static class CLArgs {
//            public boolean active = true;
//            public String inputText = "";
//
//            public CLArgs(boolean inBool, String inString) {
//                active = inBool;
//                inputText = inString;
//            }
//        }


    }
}