package ppl.server.base.security.userdetails;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EnhancedUser implements UserDetails, CredentialsContainer {
    private final Long userId;
    private final String username;
    private String password;
    private final String fullname;
    private final String email;
    private final String phone;
    private final List<Organization> organizations;
    private final Boolean depart;
    private final Boolean enabled;
    private final Boolean locked;
    private final List<PermissionCode> permissionCodes;

    public EnhancedUser(Long userId, String username, String password,
                        String fullname, String email, String phone,
                        List<Organization> organizations,
                        Boolean depart, Boolean enabled, Boolean locked,
                        List<PermissionCode> permissionCodes) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.organizations = Collections.unmodifiableList(organizations);
        this.depart = depart;
        this.enabled = enabled;
        this.locked = locked;
        this.permissionCodes = Collections.unmodifiableList(permissionCodes);
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullname() {
        return fullname;
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
        return true;
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

    public boolean isDepart() {
        return depart != null && depart;
    }
}
