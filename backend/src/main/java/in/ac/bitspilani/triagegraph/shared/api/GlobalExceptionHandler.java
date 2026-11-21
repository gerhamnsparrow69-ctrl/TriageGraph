package in.ac.bitspilani.triagegraph.shared.api;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import in.ac.bitspilani.triagegraph.shared.exception.AssignmentException;
import in.ac.bitspilani.triagegraph.shared.exception.AuthenticationFailedException;
import in.ac.bitspilani.triagegraph.shared.exception.BusinessRuleException;
import in.ac.bitspilani.triagegraph.shared.exception.CyclicDependencyException;
import in.ac.bitspilani.triagegraph.shared.exception.ExternalServiceException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ItsmException;
import in.ac.bitspilani.triagegraph.shared.exception.PersistenceFailureException;
import in.ac.bitspilani.triagegraph.shared.exception.ResourceNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;

/**
 * Turns every exception into an RFC 9457 ProblemDetail with {@code errorCode}, {@code timestamp},
 * {@code path} and, for validation failures, {@code fieldErrors}. The frontend shows these as toasts.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** Status for each branch of the exception tree. Most specific class wins. */
    static HttpStatus statusFor(ItsmException ex) {
        return switch (ex) {
            case AuthenticationFailedException e -> HttpStatus.UNAUTHORIZED;
            case UnauthorizedOperationException e -> HttpStatus.FORBIDDEN;
            case ResourceNotFoundException e -> HttpStatus.NOT_FOUND;
            case CyclicDependencyException e -> HttpStatus.UNPROCESSABLE_CONTENT;
            case BusinessRuleException e -> HttpStatus.CONFLICT;
            case AssignmentException e -> HttpStatus.CONFLICT;
            case ExternalServiceException e -> HttpStatus.SERVICE_UNAVAILABLE;
            case PersistenceFailureException e -> HttpStatus.SERVICE_UNAVAILABLE;
            case InvalidInputException e -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(ItsmException.class)
    ResponseEntity<ProblemDetail> handleItsm(ItsmException ex, WebRequest request) {
        HttpStatus status = statusFor(ex);
        if (status.is5xxServerError()) {
            log.error("{}: {}", ex.errorCode(), ex.getMessage(), ex);
        }
        return ResponseEntity.status(status).body(problem(status, ex.getMessage(), ex.errorCode(), request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(problem(HttpStatus.FORBIDDEN, "You do not have permission to do this.",
                        UnauthorizedOperationException.CODE, request));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(HttpStatus.CONFLICT, "This incident was just updated by someone else; refresh.",
                        "CONCURRENT_UPDATE", request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Some fields are invalid.", InvalidInputException.CODE, request);
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("field", fe.getField());
                    m.put("message", fe.getDefaultMessage());
                    return m;
                })
                .toList();
        body.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            decorate(pd, pd.getProperties() != null && pd.getProperties().containsKey("errorCode")
                    ? String.valueOf(pd.getProperties().get("errorCode")) : "HTTP_" + statusCode.value(), request);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problem(HttpStatus status, String detail, String errorCode, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        decorate(pd, errorCode, request);
        return pd;
    }

    private void decorate(ProblemDetail pd, String errorCode, WebRequest request) {
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("timestamp", Instant.now(clock));
        if (request instanceof ServletWebRequest swr) {
            pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
            pd.setProperty("path", swr.getRequest().getRequestURI());
        }
    }
}
