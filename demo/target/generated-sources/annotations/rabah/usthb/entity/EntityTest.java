package rabah.usthb.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Column;
import java.util.Map;
import java.util.HashMap;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class EntityTest {
	@Column(nullable = true)
	private String surname;
	private String haah;
	private String name = "";
	private int age;
	private Map<String, Integer> tags = new HashMap<>();
	private Map<String, Integer> tag = new HashMap<>();
	private String password;
}
