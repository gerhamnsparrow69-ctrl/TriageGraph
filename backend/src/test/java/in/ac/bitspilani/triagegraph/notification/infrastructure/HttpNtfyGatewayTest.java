package in.ac.bitspilani.triagegraph.notification.infrastructure;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import in.ac.bitspilani.triagegraph.notification.application.NotifyProperties;

class HttpNtfyGatewayTest {

    private final NotifyProperties props = new NotifyProperties("noreply@triagegraph.local", "https://ntfy.example",
            true, true, 3, Duration.ofMillis(500));

    @Test
    void postsTitlePriorityAndBodyToTheTopic() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ntfy.example/tg-dev-sharma"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Title", "[P1] INC-7 assigned to you"))
                .andExpect(header("Priority", "5"))
                .andExpect(header("Tags", "rotating_light"))
                .andExpect(content().string("Email server down"))
                .andRespond(withSuccess());

        new HttpNtfyGateway(builder, props).publish("tg-dev-sharma", "[P1] INC-7 assigned to you", "Email server down", 5);

        server.verify();
    }

    @Test
    void stripsNonAsciiFromTheTitleHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ntfy.example/t"))
                .andExpect(header("Title", "SLA breached  INC-9"))
                .andExpect(header("Tags", "bell"))
                .andRespond(withSuccess());

        new HttpNtfyGateway(builder, props).publish("t", "SLA breached → INC-9", "body", 3);

        server.verify();
    }

    @Test
    void startsWithoutASpringProvidedBuilder() {
        new HttpNtfyGateway(props);
    }
}
