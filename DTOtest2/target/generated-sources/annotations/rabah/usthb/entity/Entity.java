package rabah.usthb.entity;

import org.springframework.data.annotation.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.util.Map;
import java.util.HashMap;

public class Entity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	@Column(nullable = true)
	private String surname;
	private String haah;
	private String name = "";
	private String fullName;
	private int age;
	private Map<String, Integer> tags = new HashMap<>();
	private Map<String, Integer> tag = new HashMap<>();
	private String password;
}
