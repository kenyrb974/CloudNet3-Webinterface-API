package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.rest.HttpHandler;
import de.dytanic.cloudnet.ext.rest.utils.gson.PlayerGroupPut;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class V1HttpHandlerPlayers extends HttpHandler {
    public V1HttpHandlerPlayers(String permission) {
        super(permission);
    }

    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry()
            .getFirstService(IPlayerManager.class);

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("user")) {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(playerManager.getRegisteredPlayers().stream()
                            .filter(iPermissionUser -> iPermissionUser.getUniqueId().toString()
                                    .equalsIgnoreCase(context.request().pathParameters().get("user")))
                            .map(iPermissionUser -> new JsonDocument()
                                    .append("name", iPermissionUser.getName())
                                    .append("firstlogin", iPermissionUser.getFirstLoginTimeMillis())
                                    .append("lastlogin", iPermissionUser.getLastLoginTimeMillis())
                                    .append("uuid", iPermissionUser.getUniqueId())
                                    .append("groups", getCloudNet().getPermissionManagement()
                                            .getUser(iPermissionUser.getUniqueId()) != null ?
                                            getCloudNet().getPermissionManagement().getUser(iPermissionUser.getUniqueId()).getGroups() :
                                            new ArrayList<>())
                                    .append("permissions", getCloudNet().getPermissionManagement()
                                            .getUser(iPermissionUser.getUniqueId()) != null ? getCloudNet().getPermissionManagement()
                                            .getUser(iPermissionUser.getUniqueId()).getPermissions() :
                                            new ArrayList<>())
                                    .append("status", playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()) != null ? "Online" : "Offline")
                                    .append("server", playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()) != null ?
                                            playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()).
                                                    getConnectedService().getServerName() : ""))
                            .collect(Collectors.toList())))
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        } else {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(
                            playerManager.getRegisteredPlayers().stream()
                                    .map(iPermissionUser -> new JsonDocument()
                                            .append("name", iPermissionUser.getName())
                                            .append("firstlogin", iPermissionUser.getFirstLoginTimeMillis())
                                            .append("lastlogin", iPermissionUser.getLastLoginTimeMillis())
                                            .append("uuid", iPermissionUser.getUniqueId())
                                            .append("groups", getCloudNet().getPermissionManagement()
                                                    .getUser(iPermissionUser.getUniqueId()) != null ?
                                                    getCloudNet().getPermissionManagement().getUser(iPermissionUser.getUniqueId()).getGroups() :
                                                    new ArrayList<>())
                                            .append("permissions", getCloudNet().getPermissionManagement()
                                                    .getUser(iPermissionUser.getUniqueId()) != null ? getCloudNet().getPermissionManagement()
                                                    .getUser(iPermissionUser.getUniqueId()).getPermissions() :
                                                    new ArrayList<>())
                                            .append("status", playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()) != null ? "Online" : "Offline")
                                            .append("server", playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()) != null ?
                                                    playerManager.getOnlinePlayer(iPermissionUser.getUniqueId()).
                                                            getConnectedService().getServerName() : ""))
                                    .collect(Collectors.toList())
                    ))
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        }
        super.handleGet(path, context);
    }

    @Override
    public void handlePut(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("user") && context.request().pathParameters().containsKey("operation")) {
            PlayerGroupPut playerGroupPut = GSON
                    .fromJson(new String(context.request().body(), StandardCharsets.UTF_8), PlayerGroupPut.class);
            if (playerGroupPut.getGroup() != null) {
                switch (context.request().pathParameters().get("operation")) {
                    case "remove":
                        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                            permissionUser.removeGroup(playerGroupPut.getGroup());
                        });
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .context()
                                .closeAfter(true)
                                .cancelNext();
                        break;
                    case "add":
                        if (playerGroupPut.getTime().equalsIgnoreCase("Infinity")) {
                            CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                                permissionUser.addGroup(playerGroupPut.getGroup());
                            });
                        } else {
                            CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                                permissionUser.addGroup(playerGroupPut.getGroup(), playerGroupPut.getTime_number(), TimeUnit.valueOf(playerGroupPut.getTime()));
                            });
                        }
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_CREATED)
                                .header("Content-Type", "application/json")
                                .context()
                                .closeAfter(true)
                                .cancelNext();
                        break;
                }
            } else if (playerGroupPut.getPermission() != null) {
                switch (context.request().pathParameters().get("operation")) {
                    case "remove":
                        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                            permissionUser.removePermission(playerGroupPut.getPermission());
                        });
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .context()
                                .closeAfter(true)
                                .cancelNext();
                        break;
                    case "add":
                        if (playerGroupPut.getServiceGroup() == null) {
                            CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                                permissionUser.addPermission(playerGroupPut.getPermission());
                            });
                        } else {
                            CloudNetDriver.getInstance().getPermissionManagement().modifyUser(UUID.fromString(context.request().pathParameters().get("user")), permissionUser -> {
                                permissionUser.addPermission(playerGroupPut.getServiceGroup(), playerGroupPut.getPermission());
                            });
                        }
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_CREATED)
                                .header("Content-Type", "application/json")
                                .context()
                                .closeAfter(true)
                                .cancelNext();
                        break;
                }
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            }
        }
        super.handlePut(path, context);
    }
}
