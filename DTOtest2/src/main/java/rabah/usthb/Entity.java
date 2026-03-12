package rabah.usthb;

import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOField;
import java.util.HashMap;
import java.util.Map;


@DTO(name = {"name"})
public class Entity {
    private long id;

    private String surname;

    private String haah;
    @DTOField
    private String name = "";

    private int age;

    @DTOField
    private Map<String, Integer> tags = new HashMap<>();

    @DTOField
    private Map<String, Integer> tag = new HashMap<>();


    @DTOField(excludedDTO = {"get"})
    private String password;

    public void setId(long id) {
        int test;
        this.id = id;
    }
}
