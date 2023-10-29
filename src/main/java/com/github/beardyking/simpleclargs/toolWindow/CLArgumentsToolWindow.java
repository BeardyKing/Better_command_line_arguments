package com.github.beardyking.simpleclargs.toolWindow;

import com.github.beardyking.simpleclargs.ui.CLArgumentTree;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

public class CLArgumentsToolWindow implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CLArgumentTree toolWindowContent = new CLArgumentTree();
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.frame, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

