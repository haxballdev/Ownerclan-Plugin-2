package com.cuticu.ownerclan.manager;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends simple content-only messages to the two configured Discord webhooks.
 * Requests are fired off asynchronously so they never block the main server thread.
 */
public class WebhookManager {

    private final JavaPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WebhookManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendNews(String message) {
        send(plugin.getConfig().getString("webhooks.news"), message);
    }

    public void sendLog(String message) {
        send(plugin.getConfig().getString("webhooks.logs"), message);
    }

    private void send(String url, String message) {
        if (url == null || url.isBlank()) return;
        String json = "{\"content\":\"" + escape(message) + "\"}";

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 300) {
                    plugin.getLogger().warning("Discord webhook devolvio codigo " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error enviando webhook a Discord: " + e.getMessage());
            }
        });
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
