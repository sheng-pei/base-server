package ppl.server.base.security.userdetails;

import java.io.Serializable;

public class Organization implements Serializable {

    //TODO, 使用自增ID表示组织好吗？？？？不好，应该使用固定标识
    private final String code;
    private final String name;

    public Organization(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
