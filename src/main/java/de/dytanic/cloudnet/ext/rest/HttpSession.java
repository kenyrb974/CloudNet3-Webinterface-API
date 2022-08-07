package de.dytanic.cloudnet.ext.rest;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.driver.network.http.HttpCookie;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class HttpSession {
    private static final Pattern BASE64_PATTERN = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    private static final String COOKIE_NAME = "CloudNet-REST_V1-Session." + (new Random()).nextInt();
    private static final long EXPIRE_TIME = 86400000L;
    private final Collection<SessionEntry> entries = new CopyOnWriteArrayList();

    public HttpSession() {
    }

    public Map auth(IHttpContext context) throws Exception {
        Map map = new HashMap();
        if (this.isAuthorized(context)) {
            this.logout(context);
        }

        if (!context.request().hasHeader("Authorization")) {
            map.put("status", false);
            return map;
        }

        String[] typeAndCredentials = context.request().header("Authorization").split(" ");

        if (typeAndCredentials.length != 2 || !typeAndCredentials[0].equalsIgnoreCase("Basic")) {
            map.put("status", false);
            return map;
        }

        if (!BASE64_PATTERN.matcher(typeAndCredentials[1]).matches()) {
            map.put("status", false);
            return map;
        }

        String[] credentials = new String(Base64.getDecoder().decode(typeAndCredentials[1]), StandardCharsets.UTF_8)
                .split(":");
        if (credentials.length != 2) {
            map.put("status", false);
            return map;
        }

        List<IPermissionUser> permissionUsers = CloudNet.getInstance().getPermissionManagement().getUsers(credentials[0]);
        IPermissionUser permissionUser = permissionUsers.stream()
                .filter(user -> user.checkPassword(credentials[1])).findFirst().orElse(null);

        if (permissionUser == null) {
            map.put("status", false);
            return map;
        }

        SessionEntry sessionEntry = new SessionEntry(
                System.nanoTime(),
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
                permissionUser.getUniqueId().toString()
        );

        String key = this.createKey(sessionEntry, context);

        context.addCookie(new HttpCookie(
                        COOKIE_NAME,
                        key,
                        null,
                        "/",
                        sessionEntry.lastUsageMillis + EXPIRE_TIME))
                .response()
                .statusCode(200);

        this.entries.add(sessionEntry);
        map.put("status", true);
        map.put("cookie", COOKIE_NAME + "=" + key);
        return map;
    }

    public boolean isAuthorized(IHttpContext context) {
        if (!context.hasCookie(COOKIE_NAME)) {
            return false;
        } else {
            HttpCookie httpCookie = context.cookie(COOKIE_NAME);
            SessionEntry sessionEntry = this.getValidSessionEntry(httpCookie.getValue(), context);
            if (sessionEntry == null) {
                return false;
            } else if (sessionEntry.lastUsageMillis + 86400000L < System.currentTimeMillis()) {
                this.logout(context);
                return false;
            } else {
                sessionEntry.lastUsageMillis = System.currentTimeMillis();
                httpCookie.setMaxAge(sessionEntry.lastUsageMillis + 86400000L);
                return true;
            }
        }
    }

    public SessionEntry getValidSessionEntry(String cookieValue, IHttpContext context) {
        if (cookieValue != null && context != null) {
            Iterator var3 = this.entries.iterator();

            SessionEntry entry;
            do {
                if (!var3.hasNext()) {
                    return null;
                }

                entry = (SessionEntry) var3.next();
            } while (!cookieValue.equals(this.createKey(entry, context)));

            return entry;
        } else {
            return null;
        }
    }

    public void logout(IHttpContext context) {
        Preconditions.checkNotNull(context);
        SessionEntry sessionEntry = this.getValidSessionEntry(this.getCookieValue(context), context);
        if (sessionEntry != null) {
            this.entries.remove(sessionEntry);
        }

        context.removeCookie(COOKIE_NAME);
    }

    public IPermissionUser getUser(IHttpContext context) {
        Preconditions.checkNotNull(context);
        SessionEntry sessionEntry = this.getValidSessionEntry(this.getCookieValue(context), context);
        return this.getUser(sessionEntry, context);
    }

    private IPermissionUser getUser(SessionEntry sessionEntry, IHttpContext context) {
        return sessionEntry != null && context != null ? CloudNet.getInstance().getPermissionManagement().getUser(UUID.fromString(sessionEntry.userUniqueId)) : null;
    }

    private String getCookieValue(IHttpContext context) {
        HttpCookie httpCookie = context.cookie(COOKIE_NAME);
        return httpCookie != null ? httpCookie.getValue() : null;
    }

    private String createKey(SessionEntry sessionEntry, IHttpContext context) {
        return Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(sessionEntry.creationTime + ":" + context.channel().clientAddress().getHost() + "#" + sessionEntry.uniqueId + "#" + sessionEntry.userUniqueId));
    }

    public static class SessionEntry {
        private final long creationTime;
        private final String uniqueId;
        private final String userUniqueId;
        private long lastUsageMillis;

        public SessionEntry(long creationTime, long lastUsageMillis, String uniqueId, String userUniqueId) {
            this.creationTime = creationTime;
            this.lastUsageMillis = lastUsageMillis;
            this.uniqueId = uniqueId;
            this.userUniqueId = userUniqueId;
        }
    }
}
