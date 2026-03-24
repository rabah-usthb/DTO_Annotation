package io.github.rabah.usthb;

import java.lang.annotation.*;

/**
 * Indicates that the processor will include the annotated field in the DTOs (unless they are excluded by {@link #excludedDTO}) Only and
 * not in the entity class.
 * @author rabah-usthb
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface DTOExtraField {

    /**
     * (Optional) Array of DTO names to exclude.
     */
    String[] excludedDTO() default {""};

}
