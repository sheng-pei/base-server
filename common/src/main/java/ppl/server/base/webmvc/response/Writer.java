package ppl.server.base.webmvc.response;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface Writer {
    void write(Object bean) throws IOException;
    Writer json(ObjectMapper mapper);
}
