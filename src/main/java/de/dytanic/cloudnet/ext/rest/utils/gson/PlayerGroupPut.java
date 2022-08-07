package de.dytanic.cloudnet.ext.rest.utils.gson;

public class PlayerGroupPut {

    private String group;

    private String permission;

    private String serviceGroup;

    private String time;

    private int time_number;

    public int getTime_number() {
        return time_number;
    }

    public String getGroup() {
        return group;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public String getPermission() {
        return permission;
    }

    public String getTime() {
        return time;
    }
}
