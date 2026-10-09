package org.example.earthjukebox.nasa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class NasaCsvClientTest {
    @Test
    void downloadsCsvAndPreservesEncodedPlusInTheNasaPath() throws Exception {
        AtomicReference<String> requested = new AtomicReference<>();
        HttpServer server = server(200, completeCsv(), requested);
        try {
            NasaCsvClient client = client(server);
            var download = client.fetch();
            assertThat(requested.get()).isEqualTo("/tabledata_v4/GLB.Ts%2BdSST.csv");
            assertThat(download.data().firstYear()).isEqualTo(1880);
            assertThat(download.data().values()).hasSize((Year.now().getValue() - 1880) * 12);
            assertThat(download.sha256()).hasSize(64);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refusesAnHttpErrorInsteadOfParsingAnErrorPage() throws Exception {
        HttpServer server = server(503, "unavailable", new AtomicReference<>());
        try {
            assertThatThrownBy(() -> client(server).fetch()).isInstanceOf(RestClientResponseException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refusesATruncatedHistoricalSnapshot() throws Exception {
        String csv = "Land-Ocean: Global Means\nYear,Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec\n"
                + "1880,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1\n";
        HttpServer server = server(200, csv, new AtomicReference<>());
        try {
            assertThatThrownBy(() -> client(server).fetch()).isInstanceOf(IllegalArgumentException.class);
        } finally {
            server.stop(0);
        }
    }

    private NasaCsvClient client(HttpServer server) {
        URI uri = URI.create("http://localhost:" + server.getAddress().getPort()
                + "/tabledata_v4/GLB.Ts%2BdSST.csv");
        return new NasaCsvClient(RestClient.builder(), new NasaProperties(uri.toString(), false, "-"),
                new GistempCsvParser());
    }

    private HttpServer server(int status, String content, AtomicReference<String> requested) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requested.set(exchange.getRequestURI().getRawPath());
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var body = exchange.getResponseBody()) {
                body.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private String completeCsv() {
        StringBuilder csv = new StringBuilder("Land-Ocean: Global Means\n");
        csv.append("Year,Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec\n");
        for (int year = 1880; year < Year.now().getValue(); year++) {
            csv.append(year).append(",.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1\n");
        }
        return csv.toString();
    }
}

