/**
 * Thrown when an external API call fails.
 *
 * @author Andrew Peirce
 */
package api;

public class ApiException extends RuntimeException {

    private final int statusCode;

    public ApiException(String message) {
        this(message, -1, null);
    }

    public ApiException(String message, Throwable cause) {
        this(message, -1, cause);
    }

    public ApiException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public ApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
