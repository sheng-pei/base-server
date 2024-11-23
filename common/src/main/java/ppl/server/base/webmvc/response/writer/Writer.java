package ppl.server.base.webmvc.response.writer;

import java.io.IOException;

public interface Writer {
    void write(Object bean) throws IOException;
}
