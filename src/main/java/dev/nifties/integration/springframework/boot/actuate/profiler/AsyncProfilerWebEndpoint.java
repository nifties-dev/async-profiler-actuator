package dev.nifties.integration.springframework.boot.actuate.profiler;

import dev.nifties.integration.springframework.boot.annotation.EnableAsyncProfiler;
import one.profiler.AsyncProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Spring Actuator for invoking
 * <a href="https://github.com/async-profiler/async-profiler">Async Profiler</a> via WEB endpoint.
 * </p>
 * <p>
 * Pulls in async-profiler dependency transitively. For Maven can be added like this:
 * <pre>
 *     &lt;dependency&gt;
 *         &lt;groupId&gt;dev.nifties.integration&lt;/groupId&gt;
 *         &lt;artifactId&gt;async-profiler-actuator&lt;/artifactId&gt;
 *         &lt;version&gt;1.0.0&lt;/version&gt;
 *     &lt;/dependency&gt;
 * </pre>
 * </p>
 * <p>
 * Enable by adding {@link EnableAsyncProfiler} annotation to
 * your Spring context configuration:
 * <pre>{@code
 *  import dev.nifties.integration.springframework.boot.annotation.EnableAsyncProfiler;
 *
 *  @Configuration
 *  @EnableAsyncProfiler
 *  public class ApplicationConfiguration {
 *      ...
 *  }
 *  }</pre>
 * </p>
 * <p>
 * Activate by exposing "<i>profiler</i>" endpoint, for example, with following config in
 * {@code application.yaml}:
 * <pre>{@code
 *   endpoints:
 *     web:
 *       exposure:
 *         include: profiler
 * }</pre>
 * </p>
 * <p>
 * Usage: profiler commands can be triggered by issuing GET requests on
 * <i>/actuator/profiler/{operation}</i> endpoint. <br/>
 * Path variable and additional request parameters are translated into <a href=
 * "https://github.com/async-profiler/async-profiler/blob/v2.9/src/arguments.cpp#L52">
 * AsyncProfile execution arguments</a>. Currently <i>dump</i> and <i>stop</i> operations
 * will produce flame-graph HTML, all other operations will simply return output generated
 * by AsyncProfiler.<br/>
 * Examples:
 * <ul>
 * <li>https://.../actuator/profiler/start - start profiling (will add event=cpu by
 * default)</li>
 * <li>https://.../actuator/profiler/start?event=wall - start wall profiling</li>
 * <li>https://.../actuator/profiler/stop - stop profiling and download flame-graph</li>
 * </ul>
 * </p>
 * <p>
 * Additionally, request on common <i>/actuator/profiler</i> endpoint will trigger
 * composite start, wait, stop operation for a specified duration (5 seconds by default).
 * </p>
 * Example:
 * <ul>
 * <li>https://.../actuator/profiler?event=wall&amp;duration=10 - invoke wall profiling
 * for 10 seconds and download flame-graph immediately.</li>
 * <li>https://.../actuator/profiler?event=wall&amp;duration=10&amp;total&amp;threads - invoke the same
 * but with additional options - total, to produce output in total milliseconds rather than samples, threads, to
 * include additional row with thread names in flame-graph.</li>
 * </ul>
 * For full documentation on all available options, please check AsyncProfiler home-page. This class really just
 * translates WEB request parameters into AsyncProfiler arguments.
 *
 * @author Andris Rauda
 * @since 1.0.0
 */
@RestControllerEndpoint(id = "profiler")
public class AsyncProfilerWebEndpoint {

    private static final Log log = LogFactory.getLog(AsyncProfilerWebEndpoint.class);

    private static final String OPERATION_START = "start";

    private final AsyncProfiler asyncProfiler;

    public AsyncProfilerWebEndpoint(final AsyncProfiler asyncProfiler) {
        this.asyncProfiler = asyncProfiler;
    }

    @GetMapping("{operation:^(?!dump|stop).+}")
    public ResponseEntity<String> executeCommand(@PathVariable String operation, WebRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("operation: " + operation);
            log.debug("parameters: " + request.getParameterMap());
        }

        final String command = getCommand(operation, request);
        log.info("command: " + command);

        final String result;
        try {
            result = asyncProfiler.execute(command);
            log.info(result);
            return ResponseEntity.ok(result);
        } catch (IOException | RuntimeException e) {
            log.error("Failed to invoke AsyncProfiler " + operation, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("{operation:dump|stop}")
    public ResponseEntity<?> collectFlameGraph(@PathVariable String operation, WebRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("operation: " + operation);
        }
        File file = null;
        try {
            file = createTempFile();
            String command = operation;
            if (request.getParameter("total") != null) {
                command += ",total";
            }
            command += ",file=" + file.getAbsolutePath();
            log.info("command: " + command);
            log.info(asyncProfiler.execute(command));
            return ResponseEntity.ok().body(new TemporaryFileSystemResource(file));
        } catch (IOException | RuntimeException e) {
            log.error("Failed to invoke AsyncProfiler " + operation, e);
            if (file != null) {
                file.delete();
            }
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> executeAndCollectFlamegraph(
            @RequestParam(value = "duration", required = false, defaultValue = "5") long duration, WebRequest request) {

        try {
            final long durationMillis = duration * 1000L;
            if (log.isDebugEnabled()) {
                log.debug("parameters: " + request.getParameterMap());
            }

            final String command = getCommand("start", request);

            if (log.isInfoEnabled()) {
                log.info("duration: " + durationMillis + ", command: " + command);
                log.info(asyncProfiler.execute(command));
            }
            Thread.sleep(durationMillis);
            return collectFlameGraph("stop", request);
        } catch (IOException | RuntimeException e) {
            log.error("Failed to invoke AsyncProfiler", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE.value()).body(e.getMessage());
        }
    }

    private static String getCommand(String operation, WebRequest request) {
        Objects.requireNonNull(operation);
        String parameters = request.getParameterMap().entrySet().stream()
                .filter(e -> !"duration".equalsIgnoreCase(e.getKey()))
                .filter(e -> !"total".equals(e.getKey()))
                .map(e -> parseParameter(e.getKey(), e.getValue())).collect(Collectors.joining(","));

        if (OPERATION_START.equals(operation) && parameters.isEmpty()) {
            parameters = "event=cpu";
        }
        return parameters.isEmpty() ? operation : String.join(",", operation, parameters);
    }

    private static String parseParameter(String key, String[] values) {
        if (values == null || values.length == 0
                || (values.length == 1 && (values[0] == null || values[0].isEmpty()))) {
            return key;
        }
        return Stream.of(values).map(v -> key + "=" + v).collect(Collectors.joining(","));
    }

    private File createTempFile() throws IOException {
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(LocalDateTime.now());
        File file = File.createTempFile("async-profiler-" + date, ".html");
        file.delete();
        return file;
    }
}
