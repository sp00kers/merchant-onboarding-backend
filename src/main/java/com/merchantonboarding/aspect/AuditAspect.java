package com.merchantonboarding.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.merchantonboarding.annotation.Auditable;
import com.merchantonboarding.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditService auditService;

    @Around("@annotation(com.merchantonboarding.annotation.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String action = auditable.action().isEmpty() ? method.getName() : auditable.action();
        String entityType = auditable.entityType();

        // Get user info from security context
        String userId = null;
        String userEmail = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userEmail = authentication.getName();
            userId = userEmail;
        }

        // Get IP address
        String ipAddress = getClientIpAddress();

        // Get entity ID from method arguments if possible
        String entityId = extractEntityId(joinPoint.getArgs());

        // Capture old value before execution (for updates)
        Object oldValue = null;

        Object result = null;
        String status = "SUCCESS";
        String details = null;

        try {
            result = joinPoint.proceed();

            // Extract entity ID from result if not found in arguments
            if (entityId == null && result != null) {
                entityId = extractEntityIdFromResult(result);
            }

            return result;
        } catch (Exception e) {
            status = "FAILURE";
            details = e.getMessage();
            throw e;
        } finally {
            try {
                auditService.logAction(
                    action,
                    entityType,
                    entityId,
                    userId,
                    userEmail,
                    ipAddress,
                    oldValue,
                    result,
                    status,
                    details
                );
            } catch (Exception e) {
                // Log audit failure but don't fail the main operation
                System.err.println("Failed to create audit log: " + e.getMessage());
            }
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String extractEntityId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // Usually the first argument is the entity ID
        Object firstArg = args[0];
        if (firstArg instanceof String) {
            return (String) firstArg;
        } else if (firstArg instanceof Long) {
            return String.valueOf(firstArg);
        }

        return null;
    }

    private String extractEntityIdFromResult(Object result) {
        if (result == null) {
            return null;
        }

        try {
            // Try to get ID via reflection
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? String.valueOf(id) : null;
        } catch (Exception e) {
            // Try getCaseId for case entities
            try {
                Method getCaseIdMethod = result.getClass().getMethod("getCaseId");
                Object id = getCaseIdMethod.invoke(result);
                return id != null ? String.valueOf(id) : null;
            } catch (Exception ex) {
                // Ignore
            }
        }

        return null;
    }
}
