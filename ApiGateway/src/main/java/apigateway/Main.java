package apigateway;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLOutput;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
    private static HttpServer server;
    private final static String HOSTNAME = "ec2-54-145-190-43.compute-1.amazonaws.com";
    private final static int PORT = 3000;

    private static HttpClient client;
    private final static String EVENT_ADDR       = "http://" + HOSTNAME + ":3001";
    private final static String PARTICIPANT_ADDR = "http://" + HOSTNAME + ":3002";

    private static class RequestHandler implements HttpHandler {
        private URI uri; 

        private RequestHandler(String uriString) {
            this.uri = URI.create(uriString);
        }

        public static RequestHandler routeTo(String uriString) {
            return new RequestHandler(uriString);
        }

        @Override
        public void handle(HttpExchange exchange) {
            System.out.println("Handling");
            System.out.println(exchange.getRequestBody().toString());
            var requestBuilder = HttpRequest.newBuilder(uri);
            System.out.println("URI:");
            System.out.println(uri);
            requestBuilder.method(exchange.getRequestMethod(), BodyPublishers.ofInputStream(() -> exchange.getRequestBody()));
            HttpRequest request = requestBuilder.build();
            HttpResponse<InputStream> response;

            try {
                response = client.send(request, BodyHandlers.ofInputStream());
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to communicate with service at '" + uri + "': " + e.getMessage());
                e.printStackTrace();
                exchange.close();
                return;
            }

            try {
                exchange.getResponseHeaders().putAll(response.headers().map());
                exchange.sendResponseHeaders(200, 0);
                response.body().transferTo(exchange.getResponseBody());
            } catch (IOException e) {
                System.err.println("Failed to respond to client: " + e.getMessage());
            }
            exchange.close();
        }
    }

    public static void main(String[] args) {
        client = HttpClient.newHttpClient();

        try {
            server = HttpServer.create(new InetSocketAddress(HOSTNAME, PORT), 0);
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            return;
        }
        
        server.createContext("/api/list-events",        RequestHandler.routeTo(EVENT_ADDR + "/api/list-events"));
        server.createContext("/api/list-participants",  RequestHandler.routeTo(PARTICIPANT_ADDR + "/api/list-participants"));
        server.createContext("/api/event",              RequestHandler.routeTo(EVENT_ADDR + "/api/event"));
        server.createContext("/api/participant",        RequestHandler.routeTo(PARTICIPANT_ADDR + "/api/participant"));

        server.start();
        System.out.println("Server started");
    }
}
