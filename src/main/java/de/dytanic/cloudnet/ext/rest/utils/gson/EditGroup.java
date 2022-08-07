package de.dytanic.cloudnet.ext.rest.utils.gson;

public class EditGroup {

    private String name;
    private boolean defaultGroup;
    private String edit_name;
    private String prefix;
    private String display;
    private String color;
    private String suffix;
    private int sortId;

    public String getName() {
        return name;
    }

    public boolean getDefaultGroup() {
        return defaultGroup;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getSortId() {
        return sortId;
    }

    public String getColor() {
        return color;
    }

    public String getDisplay() {
        return display;
    }

    public String getEdit_name() {
        return edit_name;
    }

    public String getSuffix() {
        return suffix;
    }

}
