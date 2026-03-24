package io.github.rabah.usthb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the processor will generate DTOs (and maybe an entity) from the annotated class :
 * <ul>
 *     <li>Generates Only DTOs if there are no {@link DTOExtraField} annotation present on its fields</li>
 *     <li>Else generates DTOs along with the entity</li>
 * </ul>
 * @author rabah-usthb
 */

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DTO {

    /**
     * (Optional) Array of DTO names to generate.
     */

    String[] name() default {""};

    /**
     * (Optional) Target package where generated classes will be placed if empty it will use the package of the class being processed.
     */

    String packageName() default "";


}
