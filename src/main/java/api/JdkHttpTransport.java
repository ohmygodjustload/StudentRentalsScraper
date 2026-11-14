/**
 * {@link HttpTransport} backed by {@link java.net.http.HttpClient}.
 *
 * @author Andrew Peirce
 */
package api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient client;

    public JdkHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build());
    }

    public JdkHttpTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
