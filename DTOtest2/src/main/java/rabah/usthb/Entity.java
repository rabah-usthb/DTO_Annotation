package rabah.usthb;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.springframework.data.annotation.Id;
import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOExtraField;
import rabah.usthb.dtoprocessor.DTOField;
import java.util.HashMap;
import java.util.Map;


@DTO(name = {"post","get"})
public class Entity {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(nullable = true)
    private String surname;

    private String haah;

    @DTOField
    private String name = "";

    @DTOExtraField(excludedDTO = {"post"})
    private String fullName;

    private int age;

    @DTOField
    private Map<String, Integer> tags = new HashMap<>();

    @DTOField
    private Map<String, Integer> tag = new HashMap<>();


    @DTOField(excludedDTO = {"get"})
    private String password;

    public void setId(long id) {
        int nigga;
        this.id = id;
    }
}
