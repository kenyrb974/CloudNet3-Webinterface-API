package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.ext.rest.HttpHandler;
import de.dytanic.cloudnet.ext.rest.utils.gson.EditGroup;
import de.dytanic.cloudnet.ext.rest.utils.gson.PermissionPost;

import java.nio.charset.StandardCharsets;

public class V1HttpHandlerPermissions extends HttpHandler {

    public V1HttpHandlerPermissions(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET, PUT, POST, DELETE");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("group")) {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(getCloudNet().getPermissionManagement().getGroup(context.request().pathParameters().get("group"))))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
            return;
        }
        context
                .response()
                .statusCode(HttpResponseCode.HTTP_OK)
                .header("Content-Type", "application/json")
                .body(GSON.toJson(getCloudNet().getPermissionManagement().getGroups()))
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
        super.handleGet(path, context);
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("type") && context.request().pathParameters().containsKey("operation")) {
            PermissionPost permissionPost = GSON
                    .fromJson(new String(context.request().body(), StandardCharsets.UTF_8), PermissionPost.class);
            IPermissionGroup iPermissionGroup = getCloudNet().getPermissionManagement().getGroup(permissionPost.getGroupName());
            String operation = context.request().pathParameters().get("operation");
            if (iPermissionGroup != null) {
                switch (context.request().pathParameters().get("type")) {
                    case "permission":
                        if ("delete".equalsIgnoreCase(operation)) {
                            iPermissionGroup.removePermission(permissionPost.getPermission());
                            context
                                    .response()
                                    .statusCode(HttpResponseCode.HTTP_OK)
                                    .header("Content-Type", "application/json")
                                    .context()
                                    .closeAfter(true)
                                    .cancelNext();
                        } else if ("add".equalsIgnoreCase(operation)) {
                            iPermissionGroup.addPermission(permissionPost.getPermission());
                            context
                                    .response()
                                    .statusCode(HttpResponseCode.HTTP_OK)
                                    .header("Content-Type", "application/json")
                                    .context()
                                    .closeAfter(true)
                                    .cancelNext();
                        }
                        break;

                    case "group":
                        

                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void handlePut(String path, IHttpContext context) throws Exception {
        EditGroup editGroup = GSON
                .fromJson(new String(context.request().body(), StandardCharsets.UTF_8), EditGroup.class);
        if (editGroup.getName() != null) {
            IPermissionGroup iPermissionGroup = getCloudNet().getPermissionManagement().getGroup(editGroup.getName());
            if (iPermissionGroup != null) {
                iPermissionGroup.setColor(editGroup.getColor());
                iPermissionGroup.setDefaultGroup(editGroup.getDefaultGroup());
                iPermissionGroup.setDisplay(editGroup.getDisplay());
                iPermissionGroup.setPrefix(editGroup.getPrefix());
                iPermissionGroup.setSortId(editGroup.getSortId());
                iPermissionGroup.setSuffix(editGroup.getSuffix());
                iPermissionGroup.setName(editGroup.getName());

                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/json")
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            }
        }
        super.handlePut(path, context);
    }

    @Override
    public void handleDelete(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("group")) {
            getCloudNet().getPermissionManagement().deleteGroup(context.request().pathParameters().get("group"));
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
            return;
        }
        super.handleDelete(path, context);
    }
}
