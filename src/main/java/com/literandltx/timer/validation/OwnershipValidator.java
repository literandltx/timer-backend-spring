package com.literandltx.timer.validation;

import com.literandltx.timer.model.User;
import com.literandltx.timer.model.UserOwned;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
public class OwnershipValidator {

    private OwnershipValidator() {
    }

    public static <T extends UserOwned> void validateOwnership(@NonNull T entity, User authUser) {
        if (entity.getUser() != null && !entity.getUser().getId().equals(authUser.getId())) {
            String entityName = entity.getClass().getSimpleName();

            log.warn("Access denied: User {} tried to access {} {} owned by user {}",
                    authUser.getId(), entityName, entity.getUuid(), entity.getUser().getId());

            throw new AccessDeniedException("User do not have permission to access this " + entityName);
        }
    }

}
