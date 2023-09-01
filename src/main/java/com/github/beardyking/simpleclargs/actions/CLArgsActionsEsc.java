package com.github.beardyking.simpleclargs.actions;

import com.github.beardyking.simpleclargs.toolWindow.CLAToolWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class CLArgsActionsEsc extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        CLAToolWindow.CLArgTree.deselect_table_action();
    }
}

