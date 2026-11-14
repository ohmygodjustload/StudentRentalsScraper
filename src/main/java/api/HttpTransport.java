/**
 * Minimal HTTP client abstraction for API callers (enables test doubles).
 *
 * @author Andrew Peirce
 */
package api;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface HttpTransport {
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
