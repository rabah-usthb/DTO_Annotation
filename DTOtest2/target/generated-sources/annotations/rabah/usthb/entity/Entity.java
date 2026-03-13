package rabah.usthb.entity;

import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.springframework.data.annotation.Id;
import rabah.usthb.dtoprocessor.DTO;
import rabah.usthb.dtoprocessor.DTOExtraField;
import rabah.usthb.dtoprocessor.DTOField;
import java.util.HashMap;
import java.util.Map;

public class Entity {
	private String fullName;
	private int age;
	private Map<String, Integer> tags = new HashMap<>();
	private Map<String, Integer> tag = new HashMap<>();
	private String password;
}
