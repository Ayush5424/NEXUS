package com.NEXUS.NEXUS.config;

import java.net.URI;

public final class RenderDatabaseUrlAdapter {

    private RenderDatabaseUrlAdapter() {
    }

    public static void configure() {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        if (databaseUrl.startsWith("jdbc:")) {
            setIfMissing("spring.datasource.url", databaseUrl);
            return;
        }

        if (!databaseUrl.startsWith("postgres://") &&
                !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String[] credentials = parseCredentials(uri);
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() +
                ":" + uri.getPort() +
                uri.getPath() +
                queryString(uri);

        setIfMissing("spring.datasource.url", jdbcUrl);
        setIfMissing("spring.datasource.username", credentials[0]);
        setIfMissing("spring.datasource.password", credentials[1]);
        setIfMissing("spring.datasource.driver-class-name", "org.postgresql.Driver");
    }

    private static String[] parseCredentials(URI uri) {
        String userInfo = uri.getUserInfo();

        if (userInfo == null || userInfo.isBlank()) {
            return new String[]{"", ""};
        }

        String[] parts = userInfo.split(":", 2);
        String username = parts[0];
        String password = parts.length > 1 ? parts[1] : "";
        return new String[]{username, password};
    }

    private static String queryString(URI uri) {
        String query = uri.getQuery();
        return query == null || query.isBlank() ? "" : "?" + query;
    }

    private static void setIfMissing(String key, String value) {
        if (System.getProperty(key) == null && value != null) {
            System.setProperty(key, value);
        }
    }
}
