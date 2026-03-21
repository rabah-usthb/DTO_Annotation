package rabah.usthb.dtoprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the processor will include the annotated field in the DTOs (unless they are excluded by {@link #excludedDTO}), and
 * in the entity class if generated.
 * @author rabah-usthb
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface DTOField {

    /**
     * (Optional) Array of DTO names to exclude.
     */
    String[] excludedDTO() default {""};
}
