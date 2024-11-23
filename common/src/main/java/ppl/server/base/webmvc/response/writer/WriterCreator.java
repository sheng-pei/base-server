package ppl.server.base.webmvc.response.writer;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface WriterCreator {
    Writer json(ObjectMapper mapper);
    Writer json();
    Writer plain();
}
