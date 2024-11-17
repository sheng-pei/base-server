package ppl.server.base.security.userdetails;

import org.springframework.security.core.GrantedAuthority;

public class PermissionCode implements GrantedAuthority {
    private final String code;

    public PermissionCode(String code) {
        this.code = code;
    }

    @Override
    public String getAuthority() {
        return code;
    }
}
