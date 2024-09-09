package api;

import java.net.*;
import java.net.http.*;

public class TravellerMapAccess {


    public static void main(String[] args) {
        // create REST api GET call
        // to get all systems in a sector
        // e.g. /api/sector/Solomani%20Rim
        // returns JSON array of systems
        // e.g. [{"name": "Sol", "location": [1827, 1827]}, ...]

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://travellermap.com/api/sec?sector=Deneb"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        // send the request
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
    }
}
