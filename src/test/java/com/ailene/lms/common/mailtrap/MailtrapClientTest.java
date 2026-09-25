package com.ailene.lms.common.mailtrap;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MailtrapClientTest {

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private int status = 200;

    @BeforeEach
    void startFakeMailtrap() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/send", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reply = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeMailtrap() {
        server.stop(0);
    }

    @Test
    void send_postsBearerTokenAndMessage() {
        MailtrapClient client = client("tok-123");

        boolean sent = client.send("rani@example.com", "Rani", "Undangan", "teks", "<p>html</p>", "LMS Invite");

        assertThat(sent).isTrue();
        assertThat(authorization.get()).isEqualTo("Bearer tok-123");
        assertThat(body.get()).contains("\"email\":\"rani@example.com\"", "\"name\":\"Rani\"",
                "\"email\":\"no-reply@sevenpreneur.com\"", "\"name\":\"Sevenpreneur\"", "\"subject\":\"Undangan\"",
                "\"category\":\"LMS Invite\"");
    }

    @Test
    void send_rejectedByMailtrap_returnsFalseWithoutThrowing() {
        status = 401;

        assertThat(client("bad").send("rani@example.com", null, "s", "t", "h", "c")).isFalse();
    }

    @Test
    void send_notConfigured_skipsWithoutCalling() {
        assertThat(client("").send("rani@example.com", null, "s", "t", "h", "c")).isFalse();
        assertThat(body.get()).isNull();
    }

    private MailtrapClient client(String token) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/send";
        return new MailtrapClient(url, token);
    }
}
