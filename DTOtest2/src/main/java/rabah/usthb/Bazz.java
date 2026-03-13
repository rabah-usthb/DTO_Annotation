package rabah.usthb;

import jakarta.persistence.Entity;
import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOField;

@Entity
@DTO
public class Bazz {
    @DTOField
    int id;

    String pass;

}
