package com.github.beardyking.simpleclargs.actions;

import com.github.beardyking.simpleclargs.toolWindow.CLAToolWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class CLArgsActionsDelete extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CLAToolWindow.CLArgTree.button_minus_action();
    }
}

