package rabah.usthb;

import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOField;
import java.util.HashMap;
import java.util.Map;


@DTO(name = {"get" , "create"})
public class Entity {
    private long id;
    @DTOField
    private String name = "";

    @DTOField
    private Map<String,Integer> tags = new HashMap<>();

    @DTOField(excludedDTO = {"get"})
    private String password;
}
