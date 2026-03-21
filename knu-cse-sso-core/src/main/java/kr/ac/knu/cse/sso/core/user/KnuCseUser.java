package kr.ac.knu.cse.sso.core.user;

import java.util.Collection;
import java.util.List;
import kr.ac.knu.cse.sso.core.role.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class KnuCseUser implements UserDetails {

    private final String id;
    private final String studentNumber;
    private final String name;
    private final String email;
    private final UserType userType;
    private final Role role;
    private final String major;

    public KnuCseUser(
            String id,
            String studentNumber,
            String name,
            String email,
            UserType userType,
            Role role,
            String major
    ) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.name = name;
        this.email = email;
        this.userType = userType;
        this.role = role;
        this.major = major;
    }

    public String getId() {
        return id;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserType getUserType() {
        return userType;
    }

    public Role getRole() {
        return role;
    }

    public String getMajor() {
        return major;
    }

    public boolean isCseStudent() {
        return userType == UserType.CSE_STUDENT;
    }

    public boolean hasRole(Role required) {
        if (role == null) {
            return false;
        }
        return role.isHigherOrEqual(required);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return id;
    }
}
