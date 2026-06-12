package com.literandltx.timer.model;

import java.util.UUID;

/**
 * Represents an entity that is owned by a specific user.
 * Implementing this interface allows for standardized ownership validation.
 */
public interface UserOwned {

    /**
     * Retrieves the unique identifier of the entity.
     * * @return the UUID of the entity
     */
    UUID getUuid();

    /**
     * Retrieves the user who owns this entity.
     * * @return the owning User
     */
    User getUser();

}
