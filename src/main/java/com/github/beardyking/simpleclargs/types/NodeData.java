package com.github.beardyking.simpleclargs.types;

public class NodeData {
    private boolean isSelected;
    private String text;
    private final boolean isLeaf;

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
}
