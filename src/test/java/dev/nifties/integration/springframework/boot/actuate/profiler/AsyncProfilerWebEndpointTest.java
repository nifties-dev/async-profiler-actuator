package dev.nifties.integration.springframework.boot.actuate.profiler;

import one.profiler.AsyncProfiler;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncProfilerWebEndpointTest {

    @Mock
    private AsyncProfiler asyncProfiler;

    @InjectMocks
    private AsyncProfilerWebEndpoint asyncProfilerWebEndpoint;

    @BeforeAll
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(asyncProfiler);
    }

    @Test
    public void executeCommand() throws IOException {
        WebRequest request = Mockito.mock(WebRequest.class);
        assertThrows(NullPointerException.class,
                () -> asyncProfilerWebEndpoint.executeCommand(null, request));

        ResponseEntity<String> responseEntity = asyncProfilerWebEndpoint.executeCommand("start", request);
        Mockito.verify(asyncProfiler).execute("start,event=cpu");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = asyncProfilerWebEndpoint.executeCommand("stop", request);
        Mockito.verify(asyncProfiler).execute("stop");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void collectFlameGraph() throws IOException {
        WebRequest request = Mockito.mock(WebRequest.class);
        assertThrows(NullPointerException.class,
                () -> asyncProfilerWebEndpoint.executeCommand(null, request));

        // dump
        ResponseEntity<?> responseEntity = asyncProfilerWebEndpoint.collectFlameGraph("dump", null, request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, responseEntity.getHeaders().getContentType());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        Resource resource = (Resource)responseEntity.getBody();
        assertTrue(resource.getFile().getName().endsWith(".html"));
        Mockito.verify(asyncProfiler).execute("dump,file=" + resource.getFile().getAbsolutePath());

        // stop
        Mockito.reset(asyncProfiler, request);
        responseEntity = asyncProfilerWebEndpoint.collectFlameGraph("stop", null, request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, responseEntity.getHeaders().getContentType());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        assertTrue(resource.getFile().getName().endsWith(".html"));
        Mockito.verify(asyncProfiler).execute("stop,file=" + resource.getFile().getAbsolutePath());

        // stop?total
        Mockito.reset(asyncProfiler, request);
        Mockito.when(request.getParameter("total")).thenReturn("");

        responseEntity = asyncProfilerWebEndpoint.collectFlameGraph("stop", null, request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, responseEntity.getHeaders().getContentType());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        assertTrue(resource.getFile().getName().endsWith(".html"));
        Mockito.verify(asyncProfiler).execute("stop,total,file=" + resource.getFile().getAbsolutePath());

        // stop?file=.JFR
        // stop
        Mockito.reset(asyncProfiler, request);
        responseEntity = asyncProfilerWebEndpoint.collectFlameGraph("stop", ".JFR", request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, responseEntity.getHeaders().getContentType());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        assertTrue(resource.getFile().getName().endsWith(".jfr"));
        Mockito.verify(asyncProfiler).execute("stop,file=" + resource.getFile().getAbsolutePath());
    }

    @Test
    public void executeAndCollectFlameGraph() throws IOException {
        WebRequest request = Mockito.mock(WebRequest.class);
        assertThrows(NullPointerException.class,
                () -> asyncProfilerWebEndpoint.executeCommand(null, request));

        // duration=1
        Mockito.reset(asyncProfiler, request);
        long startTime = System.currentTimeMillis();
        ResponseEntity<?> responseEntity = asyncProfilerWebEndpoint.executeAndCollectFlamegraph(1L, null, request);
        long elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(elapsedMillis >= 1_000L,
                "Expected elapsedMillis to be at least 1 sec, but was " + elapsedMillis + " ms");
        InOrder inOrder = Mockito.inOrder(asyncProfiler);
        inOrder.verify(asyncProfiler).execute("start,event=cpu");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        Resource resource = (Resource)responseEntity.getBody();
        inOrder.verify(asyncProfiler).execute("stop,file=" + resource.getFile().getAbsolutePath());

        // event=wall
        Mockito.reset(asyncProfiler, request);
        Mockito.when(request.getParameterMap()).thenReturn(buildParameterMap("event", "wall"));

        startTime = System.currentTimeMillis();
        responseEntity = asyncProfilerWebEndpoint.executeAndCollectFlamegraph(0L, null, request);
        elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(elapsedMillis < 100L,
                "Expected elapsedMillis to be less then 100 ms, but was " + elapsedMillis + " ms");

        inOrder = Mockito.inOrder(asyncProfiler);
        inOrder.verify(asyncProfiler).execute("start,event=wall");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        inOrder.verify(asyncProfiler).execute("stop,file=" + resource.getFile().getAbsolutePath());

        // event=wall&total
        Mockito.reset(asyncProfiler, request);
        Mockito.when(request.getParameterMap()).thenReturn(
                buildParameterMap("event", "wall", "total", ""));
        Mockito.when(request.getParameter("total")).thenReturn("");

        startTime = System.currentTimeMillis();
        responseEntity = asyncProfilerWebEndpoint.executeAndCollectFlamegraph(0L, null, request);
        elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(elapsedMillis < 100L,
                "Expected elapsedMillis to be less then 100 ms, but was " + elapsedMillis + " ms");

        inOrder = Mockito.inOrder(asyncProfiler);
        inOrder.verify(asyncProfiler).execute("start,event=wall");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        inOrder.verify(asyncProfiler).execute("stop,total,file=" + resource.getFile().getAbsolutePath());

        // alloc=100k&threads
        Mockito.reset(asyncProfiler, request);
        Mockito.when(request.getParameterMap()).thenReturn(
                buildParameterMap("alloc", "100k", "threads", ""));
        Mockito.when(request.getParameter("threads")).thenReturn("");

        startTime = System.currentTimeMillis();
        responseEntity = asyncProfilerWebEndpoint.executeAndCollectFlamegraph(0L, null, request);
        elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(elapsedMillis < 100L,
                "Expected elapsedMillis to be less then 100 ms, but was " + elapsedMillis + " ms");

        inOrder = Mockito.inOrder(asyncProfiler);
        inOrder.verify(asyncProfiler).execute("start,alloc=100k,threads");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        inOrder.verify(asyncProfiler).execute("stop,file=" + resource.getFile().getAbsolutePath());

        // event=ctimer&interval=999us&threads&total
        Mockito.reset(asyncProfiler, request);
        Mockito.when(request.getParameterMap()).thenReturn(
                buildParameterMap("event", "ctimer", "interval", "999us",
                        "threads", "", "total", ""));
        Mockito.when(request.getParameter("threads")).thenReturn("");
        Mockito.when(request.getParameter("total")).thenReturn("");

        startTime = System.currentTimeMillis();
        responseEntity = asyncProfilerWebEndpoint.executeAndCollectFlamegraph(0L, null, request);
        elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(elapsedMillis < 100L,
                "Expected elapsedMillis to be less then 100 ms, but was " + elapsedMillis + " ms");

        inOrder = Mockito.inOrder(asyncProfiler);
        inOrder.verify(asyncProfiler).execute("start,event=ctimer,interval=999us,threads");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertInstanceOf(Resource.class, responseEntity.getBody());
        resource = (Resource)responseEntity.getBody();
        inOrder.verify(asyncProfiler).execute("stop,total,file=" + resource.getFile().getAbsolutePath());
    }

    private Map<String, String[]> buildParameterMap(String ... parameters) {
        final Map<String, String[]> parameterMap = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i += 2) {
            parameterMap.put(parameters[i], new String[] {parameters[i + 1]});
        }
        return Collections.unmodifiableMap(parameterMap);
    }
}
