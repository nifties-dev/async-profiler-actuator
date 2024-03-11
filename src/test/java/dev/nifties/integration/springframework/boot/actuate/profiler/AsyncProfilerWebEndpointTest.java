package dev.nifties.integration.springframework.boot.actuate.profiler;

import one.profiler.AsyncProfiler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void executeCommand() throws IOException {
        WebRequest request = Mockito.mock(WebRequest.class);
        ResponseEntity<String> responseEntity = asyncProfilerWebEndpoint.executeCommand(null, request);
        Mockito.verify(asyncProfiler).execute("null,");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = asyncProfilerWebEndpoint.executeCommand("start", request);
        Mockito.verify(asyncProfiler).execute("start,event=cpu");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = asyncProfilerWebEndpoint.executeCommand("stop", request);
        Mockito.verify(asyncProfiler).execute("stop,");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
