package ppl.server.base.security.userdetails;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class EnhancedUser implements UserDetails, CredentialsContainer {
    private final Long id;
    private final String username;
    private String password;
    private final String name;
    private final String email;
    private final String phone;
    private final List<Organization> organizations;
    private final Date expires;
    private final Boolean enabled;
    private final Boolean locked;
    private final List<PermissionCode> permissionCodes;

    public EnhancedUser(Long id, String username, String password,
                        String name, String email, String phone,
                        List<Organization> organizations,
                        Boolean enabled, Boolean locked,
                        List<PermissionCode> permissionCodes,
                        Date expires) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.organizations = Collections.unmodifiableList(new ArrayList<>(organizations));
        this.enabled = enabled;
        this.locked = locked;
        this.permissionCodes = Collections.unmodifiableList(new ArrayList<>(permissionCodes));
        this.expires = expires;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<Organization> getOrganizations() {
        return organizations;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissionCodes;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return expires == null || expires.after(new Date());
    }

    @Override
    public boolean isAccountNonLocked() {
        return locked == null || !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

}
