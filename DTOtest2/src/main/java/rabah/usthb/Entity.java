package rabah.usthb;


import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOField;

@DTO(name = {"get" , "create"})
public class Entity {
    private long id;
    @DTOField
    private String name = "";

    @DTOField(excludedDTO = {"get"})
    private String password;
}
