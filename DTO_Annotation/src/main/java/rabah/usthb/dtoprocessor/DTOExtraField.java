package rabah.usthb.dtoprocessor;

import java.lang.annotation.*;

@Repeatable(DTOExtraFields.class)
@Retention(RetentionPolicy.SOURCE)
//@Target(ElementType.ANNOTATION_TYPE)
public @interface DTOExtraField {
    String type();
    String name();
    String[] excludedDTO() default {""};

}
