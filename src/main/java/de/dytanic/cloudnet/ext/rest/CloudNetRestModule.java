/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.rest.http.*;
import de.dytanic.cloudnet.ext.rest.listener.ConsoleListener;
import de.dytanic.cloudnet.ext.rest.utils.RrData;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class CloudNetRestModule extends NodeCloudNetModule {

    private static CloudNetRestModule cloudNetRestModule;

    private List<Map<String, Object>> rrDataCpu;
    private List<Map<String, Object>> rrDataMemory;

    private static final Pattern BASE64_PATTERN = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");

    @Getter
    private Map<IWebSocketChannel, String> channels = new HashMap<IWebSocketChannel, String>();

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void webSocket() {
        registerListener(new ConsoleListener(getCloudNetRestModule()));

        CloudNet.getInstance().getHttpServer().registerHandler("/websocket/console", (path, context) -> {
            String typeAndCredentials = context.request().queryParameters().get("auth").get(0);
            String serviceId = context.request().queryParameters().get("serviceId").get(0);

            if (typeAndCredentials.equalsIgnoreCase("")) {
                return;
            }

            if (!BASE64_PATTERN.matcher(typeAndCredentials).matches()) {
                return;
            }

            String[] credentials = new String(Base64.getDecoder().decode(typeAndCredentials), StandardCharsets.UTF_8)
                    .split(":");
            if (credentials.length != 2) {
                return;
            }

            List<IPermissionUser> permissionUsers = CloudNet.getInstance().getPermissionManagement().getUsers(credentials[0]);
            IPermissionUser permissionUser = permissionUsers.stream()
                    .filter(user -> user.checkPassword(credentials[1])).findFirst().orElse(null);

            if (permissionUser != null) {

                IWebSocketChannel channel = context.upgrade(); //upgraded context to WebSocket


                ICloudService cloudService = CloudNet.getInstance().getCloudServiceManager()
                        .getCloudService(UUID.fromString(serviceId));

                this.channels.put(channel, serviceId);

                channel
                        .addListener(new IWebSocketListener() { //Add a listener for received WebSocket channel messages and closing
                            @Override
                            public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) {
                                switch (type) {
                                    case PONG:
                                        channel
                                                .sendWebSocketFrame(WebSocketFrameType.TEXT, "Successful connect to server");
                                        break;
                                    case TEXT:

                                        break;
                                    default:
                                        break;
                                }
                            }

                            @Override
                            public void handleClose(IWebSocketChannel channel, AtomicInteger statusCode,
                                                    AtomicReference<String> reasonText) { // handle the closing output
                                if (!CloudNetRestModule.this.channels.containsKey(channel)) {
                                    statusCode.set(500);
                                }

                                CloudNetRestModule.this.channels.remove(channel);
                                System.out.println("I close");
                            }
                        });
                channel.sendWebSocketFrame(WebSocketFrameType.PING, "");
            }
        });
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initHttpHandlers() {

        cloudNetRestModule = this;

        rrDataCpu = new ArrayList<>();
        rrDataMemory = new ArrayList<>();

        this.getHttpServer()
                .registerHandler("/api/v1", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerShowOpenAPI())
                //HttpHandler API implementation
                .registerHandler("/api/v1/*", IHttpHandler.PRIORITY_HIGH, new V1SecurityProtectionHttpHandler())
                .registerHandler("/api/v1/auth", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerAuthentication())
                .registerHandler("/api/v1/logout", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLogout())
                .registerHandler("/api/v1/ping", IHttpHandler.PRIORITY_NORMAL, new V1HttpHandlerPing("cloudnet.http.v1.ping"))
                .registerHandler("/api/v1/status", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerStatus("cloudnet.http.v1.status"))
                .registerHandler("/api/v1/command", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerCommand("cloudnet.http.v1.command"))
                .registerHandler("/api/v1/permissions", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPermissions("cloudnet.http.v1.permissions"))
                .registerHandler("/api/v1/permissions/{group}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPermissions("cloudnet.http.v1.permissions"))
                .registerHandler("/api/v1/permissions/{type}/{operation}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPermissions("cloudnet.http.v1.permissions"))
                .registerHandler("/api/v1/players", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPlayers("cloudnet.http.v1.players"))
                .registerHandler("/api/v1/rrdata", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerRrData("cloudnet.http.v1.rrdata"))
                .registerHandler("/api/v1/players/{user}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPlayers("cloudnet.http.v1.players"))
                .registerHandler("/api/v1/players/{user}/{operation}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerPlayers("cloudnet.http.v1.players"))
                .registerHandler("/api/v1/modules", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerModules("cloudnet.http.v1.modules"))
                .registerHandler("/api/v1/cluster", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/cluster/{node}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/services", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}/{operation}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerServices("cloudnet.http.v1.services.operation"))
                .registerHandler("/api/v1/services/{serviceTaskName}/{counter}/{start}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerServices("cloudnet.http.v1.services.operation"))
                .registerHandler("/api/v1/tasks", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/tasks/{name}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/groups", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/groups/{name}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/syncproxy", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerSyncproxy("cloudnet.http.v1.syncproxy"))
                .registerHandler("/api/v1/signs", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerSign("cloudnet.http.v1.signs"))
                .registerHandler("/api/v1/db/{name}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/db/{name}/{key}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/local_templates", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.list"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.template"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files/*", IHttpHandler.PRIORITY_NORMAL,
                        new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"));


        new RrData(getCloudNetRestModule()).runData();
    }

    public static CloudNetRestModule getCloudNetRestModule() {
        return cloudNetRestModule;
    }

    public List<Map<String, Object>> getRrDataCpu() {
        return rrDataCpu;
    }

    public List<Map<String, Object>> getRrDataMemory() {
        return rrDataMemory;
    }
}
