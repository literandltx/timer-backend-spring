package com.literandltx.timer.validation;

import com.literandltx.timer.model.User;
import com.literandltx.timer.model.UserOwned;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;

/**
 * Utility class responsible for validating whether an authenticated user
 * has permission to access a specific user-owned entity.
 */
@Slf4j
public class OwnershipValidator {

    /**
     * Prevent instantiation
     */
    private OwnershipValidator() {
    }

    /**
     * Validates that the provided authenticated user owns the given entity.
     *
     * @param entity   the entity to check for ownership
     * @param authUser the currently authenticated user
     * @param <T>      the type of the entity, which must implement {@link UserOwned}
     * @throws AccessDeniedException if the authenticated user does not own the entity
     */
    public static <T extends UserOwned> void validateOwnership(@NonNull T entity, User authUser) {
        if (entity.getUser() != null && !entity.getUser().getId().equals(authUser.getId())) {
            String entityName = entity.getClass().getSimpleName();

            log.warn("Access denied: User {} tried to access {} {} owned by user {}",
                    authUser.getId(), entityName, entity.getUuid(), entity.getUser().getId());

            throw new AccessDeniedException("User do not have permission to access this " + entityName);
        }
    }

}
