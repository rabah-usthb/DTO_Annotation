package rabah.usthb;


import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOField;

@DTO(name = "DTOEntity")
public class Entity {
    private long id;

    @DTOField
    private String name = "";
}
