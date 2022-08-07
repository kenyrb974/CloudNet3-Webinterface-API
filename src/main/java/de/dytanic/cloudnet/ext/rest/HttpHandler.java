package de.dytanic.cloudnet.ext.rest;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;
import de.dytanic.cloudnet.driver.permission.Permission;

import java.util.Collection;
import java.util.Iterator;

public class HttpHandler extends MethodHttpHandlerAdapter {

    protected static final Gson GSON = (new GsonBuilder()).registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(JsonDocument.class, new JsonDocumentTypeAdapter())).serializeNulls().disableHtmlEscaping().create();
    protected static final HttpSession HTTP_SESSION = new HttpSession();
    private final String permission;

    public HttpHandler(String permission) {
        this.permission = permission;
    }

    public void handle(String path, IHttpContext context) throws Exception {
        context.response().header("Access-Control-Allow-Origin", "*");
        if (this.permission != null && !this.checkPermission(context, this.permission + "." + context.request().method().toLowerCase())) {
            this.send403Response(context, "permission required " + this.permission + "." + context.request().method().toLowerCase());
        } else {
            super.handle(path, context);
        }
    }

    protected IHttpContext send400Response(IHttpContext context, String reason) {
        ((IHttpResponse)((IHttpResponse)context.response().statusCode(400).header("Content-Type", "application/json")).body((new JsonDocument("success", false)).append("reason", reason).toByteArray())).context().closeAfter(true).cancelNext();
        return context;
    }

    protected IHttpContext send403Response(IHttpContext context, String reason) {
        ((IHttpResponse)((IHttpResponse)context.response().statusCode(403).header("Content-Type", "application/json")).body((new JsonDocument("success", true)).append("reason", reason).toByteArray())).context().closeAfter(true).cancelNext();
        return context;
    }

    protected IHttpContext sendOptions(IHttpContext context, String allowedMethods) {
        ((IHttpResponse)((IHttpResponse)((IHttpResponse)((IHttpResponse)((IHttpResponse)((IHttpResponse)((IHttpResponse)((IHttpResponse)context.response().statusCode(200).header("Allow", allowedMethods)).header("Content-Type", "application/json")).header("Access-Control-Allow-Origin", "*")).header("Access-Control-Allow-Credentials", "true")).header("Access-Control-Allow-Headers", "Accept, Origin, if-none-match, Access-Control-Allow-Headers, Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")).header("Access-Control-Expose-Headers", "Accept, Origin, if-none-match, Access-Control-Allow-Headers, Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")).header("Access-Control-Allow-Methods", allowedMethods)).header("Access-Control-Max-Age", "3600")).context().closeAfter(true);
        return context;
    }

    protected boolean checkPermission(IHttpContext context, String permission) {
        try {
            if (!permission.isEmpty() && !this.getCloudNet().getPermissionManagement().hasPermission(HTTP_SESSION.getUser(context), new Permission(permission, 1))) {
                this.send403Response(context, "permission required " + permission);
                return false;
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return true;
    }

    protected boolean containsStringElementInCollection(Collection<String> collection, String name) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(name);
        Iterator var3 = collection.iterator();

        String queryString;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            queryString = (String)var3.next();
        } while(!queryString.contains(name));

        return true;
    }

    protected final CloudNet getCloudNet() {
        return CloudNet.getInstance();
    }

    public String getPermission() {
        return this.permission;
    }

}
