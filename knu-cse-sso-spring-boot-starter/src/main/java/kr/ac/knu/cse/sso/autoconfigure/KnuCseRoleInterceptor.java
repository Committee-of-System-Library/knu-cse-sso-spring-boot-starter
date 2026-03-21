package kr.ac.knu.cse.sso.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ac.knu.cse.sso.core.annotation.RequireCseStudent;
import kr.ac.knu.cse.sso.core.annotation.RequireRole;
import kr.ac.knu.cse.sso.core.user.KnuCseUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class KnuCseRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        KnuCseUser user = getCurrentUser();

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if (requireRole != null) {
            if (user == null || !user.hasRole(requireRole.value())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "역할 권한이 부족합니다.");
                return false;
            }
        }

        RequireCseStudent requireCseStudent = handlerMethod.getMethodAnnotation(RequireCseStudent.class);
        if (requireCseStudent == null) {
            requireCseStudent = handlerMethod.getBeanType().getAnnotation(RequireCseStudent.class);
        }

        if (requireCseStudent != null) {
            if (user == null || !user.isCseStudent()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "학부생 전용 기능입니다.");
                return false;
            }
        }

        return true;
    }

    private KnuCseUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KnuCseUser user) {
            return user;
        }
        return null;
    }
}
