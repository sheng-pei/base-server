package ppl.server.base.webmvc.response.writer;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class Writers {

    private ObjectMapper mapper;

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public WriterCreator http(ServletResponse response) {
        return new HttpServletResponseWriterCreator(response);
    }

    private class HttpServletResponseWriterCreator implements WriterCreator {
        private final ServletResponse response;

        private HttpServletResponseWriterCreator(ServletResponse response) {
            this.response = response;
        }

        @Override
        public Writer json(ObjectMapper mapper) {
            mapper = mapper == null ? Writers.this.mapper : mapper;
            if (mapper == null) {
                throw new IllegalStateException("Mapper is required for json");
            }
            return new JacksonHttpServletResponseWriter(response,
                    "application/json; charset=utf-8", mapper);
        }

        @Override
        public Writer json() {
            return json(Writers.this.mapper);
        }

        @Override
        public Writer plain() {
            return new PlainHttpServletResponseWriter(response, "text/plain; charset=utf-8");
        }
    }

    private static class JacksonHttpServletResponseWriter implements Writer {
        private final ServletResponse response;
        private final ObjectMapper mapper;
        private final String contentType;

        private JacksonHttpServletResponseWriter(
                ServletResponse response,
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
    }

    private static class PlainHttpServletResponseWriter implements Writer {
        private final ServletResponse response;
        private final String contentType;

        private PlainHttpServletResponseWriter(
                ServletResponse response,
                String contentType) {
            this.response = response;
            this.contentType = contentType;
        }

        @Override
        public void write(Object bean) throws IOException {
            Objects.requireNonNull(bean);
            response.setContentType(contentType);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(bean.toString());
            }
        }
    }
}
