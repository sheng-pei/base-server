package ppl.server.base.webmvc.response;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Writers {
    public static Writer http(Object response) {
        return HttpServletResponseWriter.create(response);
    }

    private static class HttpServletResponseWriter implements Writer {
        private static final String DEFAULT_CONTENT_TYPE = "application/json; charset=utf-8";
        private final HttpServletResponse response;
        private ObjectMapper mapper = new ObjectMapper();
        private String contentType = DEFAULT_CONTENT_TYPE;

        private HttpServletResponseWriter(HttpServletResponse response) {
            this.response = response;
        }

        private HttpServletResponseWriter(
                HttpServletResponse response,
                ObjectMapper mapper) {
            this.response = response;
            this.mapper = mapper;
        }

        private HttpServletResponseWriter(
                HttpServletResponse response,
                String contentType,
                ObjectMapper mapper) {
            this.response = response;
            this.contentType = contentType;
            this.mapper = mapper;
        }

        @Override
        public void write(Object bean) throws IOException {
            response.setContentType(contentType);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(mapper.writeValueAsString(bean));
            }
        }

        @Override
        public Writer json(ObjectMapper mapper) {
            return new HttpServletResponseWriter(response, "application/json; charset=utf-8", mapper);
        }

        public static HttpServletResponseWriter create(Object response) {
            if (response instanceof HttpServletResponse) {
                return new HttpServletResponseWriter((HttpServletResponse) response);
            }
            return null;
        }
    }
}
