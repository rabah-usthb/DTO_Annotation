package rabah.usthb.dtoprocessor;

import java.lang.annotation.*;


@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface DTOExtraField {
    String[] excludedDTO() default {""};

}
