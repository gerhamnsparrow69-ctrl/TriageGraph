package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import in.ac.bitspilani.triagegraph.notification.application.NotifyProperties;

/** POSTs to https://ntfy.sh/&lt;topic&gt; (or a self-hosted server). Install the ntfy app and subscribe to the topic. */
@Component
class HttpNtfyGateway implements NtfyGateway {

    private final RestClient http;

    // Boot 4 only auto-configures a RestClient.Builder when the restclient module is on the classpath,
    // so build our own rather than depend on one being there.
    @Autowired
    HttpNtfyGateway(NotifyProperties props) {
        this(RestClient.builder(), props);
    }

    HttpNtfyGateway(RestClient.Builder builder, NotifyProperties props) {
        this.http = builder.baseUrl(props.ntfyBaseUrl()).build();
    }

    @Override
    public void publish(String topic, String title, String message, int priority) {
        http.post()
                .uri("/{topic}", topic)
                .header("Title", asciiHeader(title))
                .header("Priority", String.valueOf(priority))
                .header("Tags", priority >= 5 ? "rotating_light" : "bell")
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(message)
                .retrieve()
                .toBodilessEntity();
    }

    /** HTTP headers must be ASCII; ntfy shows the body in full anyway. */
    private static String asciiHeader(String s) {
        return s.replaceAll("[^\\x20-\\x7E]", "");
    }
}
