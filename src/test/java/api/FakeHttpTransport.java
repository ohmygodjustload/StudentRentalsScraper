package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import javax.net.ssl.SSLSession;

/**
 * Test double that returns canned responses in order.
 */
public class FakeHttpTransport implements HttpTransport {

    private final Queue<CannedResponse> responses = new ArrayDeque<>();
    private final List<HttpRequest> requests = new java.util.ArrayList<>();

    public FakeHttpTransport enqueue(int status, String body) {
        responses.add(new CannedResponse(status, body));
        return this;
    }

    public List<HttpRequest> getRequests() {
        return requests;
    }

    @Override
    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        requests.add(request);
        CannedResponse canned = responses.poll();
        if (canned == null) {
            throw new IOException("No canned response left for " + request.uri());
        }
        return new SimpleResponse(request, canned.status, canned.body);
    }

    private record CannedResponse(int status, String body) {
    }

    private static final class SimpleResponse implements HttpResponse<String> {
        private final HttpRequest request;
        private final int status;
        private final String body;

        private SimpleResponse(HttpRequest request, int status, String body) {
            this.request = request;
            this.status = status;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
