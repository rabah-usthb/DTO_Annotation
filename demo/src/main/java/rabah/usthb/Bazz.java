package rabah.usthb;

import jakarta.persistence.Entity;
import io.github.rabah.usthb.DTO;
import io.github.rabah.usthb.DTOField;
import lombok.Builder;
import lombok.Data;
import lombok.Value;


@Entity
@Builder
@DTO(packageName = "io.rabah.bazz")
@Value
@Data
public class Bazz {
    @DTOField
    int id;

    String pass;

}
