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

package de.dytanic.cloudnet.ext.rest.http;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.ext.rest.HttpHandler;
import de.dytanic.cloudnet.ext.rest.utils.gson.EditGroup;
import de.dytanic.cloudnet.ext.rest.utils.gson.ServiceCommand;
import de.dytanic.cloudnet.http.V1HttpHandler;
import de.dytanic.cloudnet.service.ICloudService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class V1HttpHandlerServices extends HttpHandler {

    public V1HttpHandlerServices(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET, PUT");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("uuid")) {
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider()
                    .getCloudServices().stream()
                    .filter(serviceInfoSnapshot12 -> serviceInfoSnapshot12.getServiceId().getUniqueId().toString()
                            .contains(context.request().pathParameters().get("uuid")))
                    .findFirst()
                    .orElse(null);

            if (serviceInfoSnapshot == null) {
                serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                        .filter(serviceInfoSnapshot1 -> serviceInfoSnapshot1.getServiceId().getName()
                                .contains(context.request().pathParameters().get("uuid")))
                        .findFirst().orElse(null);
            }

            if (serviceInfoSnapshot == null) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;

                return;
            }

            if (context.request().pathParameters().containsKey("operation")) {
                switch (context.request().pathParameters().get("operation").toLowerCase()) {
                    case "start": {
                        serviceInfoSnapshot.provider().start();
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "stop": {
                        serviceInfoSnapshot.provider().stop();
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "restart": {
                        serviceInfoSnapshot.provider().restart();
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "delete": {
                        serviceInfoSnapshot.provider().delete();
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "log": {
                        Queue<String> queue = serviceInfoSnapshot.provider().getCachedLogMessages();

                        StringBuilder stringBuilder = new StringBuilder();

                        for (String item : queue) {
                            stringBuilder.append(item).append("\n");
                        }

                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "text/plain")
                                .body(stringBuilder.toString())
                        ;
                    }
                    break;
                    case "log_json": {
                        Queue<String> queue = serviceInfoSnapshot.provider().getCachedLogMessages();

                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(queue))
                        ;
                    }
                    break;
                    default:
                        break;
                }
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/json")
                        .body(GSON.toJson(serviceInfoSnapshot))
                ;
            }

            context
                    .closeAfter(true)
                    .cancelNext()
            ;
        } else if (context.request().pathParameters().containsKey("serviceTaskName")) {
            String tasks = context.request().pathParameters().get("serviceTaskName");
            if (CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(tasks)) {
                int count = Integer.parseInt(context.request().pathParameters().getOrDefault("counter", "1"));
                boolean start = Boolean.parseBoolean(context.request().pathParameters().getOrDefault("start", "true"));
                for (int i = 0; i < count; i++) {
                    ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(tasks);
                    ServiceInfoSnapshot serviceInfoSnapshot = ServiceConfiguration.builder(serviceTask).build()
                            .createNewService();
                    if (serviceInfoSnapshot != null) {
                        if (start) {
                            serviceInfoSnapshot.provider().start();
                        }
                    }
                }
                context
                        .response()
                        .header("Content-Type", "application/json")
                        .statusCode(201)
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
            }
        } else {
            context
                    .response()
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                            .filter(serviceInfoSnapshot -> {
                                if (context.request().queryParameters().containsKey("name") &&
                                        !context.request().queryParameters().get("name").contains(serviceInfoSnapshot.getServiceId().getName())) {
                                    return false;
                                }

                                if (context.request().queryParameters().containsKey("task") &&
                                        !context.request().queryParameters().get("task")
                                                .contains(serviceInfoSnapshot.getServiceId().getTaskName())) {
                                    return false;
                                }

                                return !context.request().queryParameters().containsKey("node") ||
                                        context.request().queryParameters().get("node")
                                                .contains(serviceInfoSnapshot.getServiceId().getNodeUniqueId());
                            }).collect(Collectors.toList())))
                    .statusCode(200)
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }

    @Override
    public void handlePut(String path, IHttpContext context) throws Exception {
        ServiceCommand serviceCommand = GSON
                .fromJson(new String(context.request().body(), StandardCharsets.UTF_8), ServiceCommand.class);
        if (context.request().pathParameters().containsKey("uuid") && !serviceCommand.getCommand().equalsIgnoreCase("")) {
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider()
                    .getCloudServices().stream()
                    .filter(serviceInfoSnapshot12 -> serviceInfoSnapshot12.getServiceId().getUniqueId().toString()
                            .contains(context.request().pathParameters().get("uuid")))
                    .findFirst()
                    .orElse(null);

            if (serviceInfoSnapshot == null) {
                serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().stream()
                        .filter(serviceInfoSnapshot1 -> serviceInfoSnapshot1.getServiceId().getName()
                                .contains(context.request().pathParameters().get("uuid")))
                        .findFirst().orElse(null);
            }

            if (serviceInfoSnapshot == null) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
                        .context()
                        .closeAfter(true)
                        .cancelNext();
                return;
            }

            serviceInfoSnapshot.provider().runCommand(serviceCommand.getCommand());

            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_ACCEPTED)
                    .body(new JsonDocument("success", true).toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        }
    }
}
