package kr.ac.knu.cse.sso.autoconfigure;

import kr.ac.knu.cse.sso.core.role.Role;
import kr.ac.knu.cse.sso.core.user.KnuCseUser;
import kr.ac.knu.cse.sso.core.user.UserType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

public class KnuCseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        KnuCseUser user = extractUser(jwt);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private KnuCseUser extractUser(Jwt jwt) {
        String id = requireNonBlank(jwt.getSubject(), "sub");
        UserType userType = parseUserType(jwt.getClaimAsString("user_type"));

        String studentNumber = jwt.getClaimAsString("student_number");
        String name = jwt.getClaimAsString("name");
        String email = jwt.getClaimAsString("email");
        String major = jwt.getClaimAsString("major");
        Role role = parseRole(jwt.getClaimAsString("role"));

        return new KnuCseUser(id, studentNumber, name, email, userType, role, major);
    }

    private UserType parseUserType(String value) {
        if (value == null || value.isBlank()) {
            throw invalidToken("user_type claim is required");
        }
        try {
            return UserType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalidToken("user_type claim has invalid value: " + value);
        }
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalidToken("role claim has invalid value: " + value);
        }
    }

    private String requireNonBlank(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw invalidToken(claimName + " claim is required");
        }
        return value;
    }

    private OAuth2AuthenticationException invalidToken(String description) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null)
        );
    }
}
