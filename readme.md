


- [What is DTO\_Annotation?](#what-is-dto_annotation)
- [Examples](#examples)
  - [Generate DTOs](#generate-dtos)
  - [Generate DTOs \& Entity](#generate-dtos--entity)
- [Using DTO\_Annotation](#using-dto_annotation)
  - [Maven](#maven)
  - [Gradle](#gradle)
  - [No Dependency Management Tool](#no-dependency-management-tool)
  - [IntelliJ](#intellij)
- [Licensing](#licensing)

## What is DTO_Annotation?

DTO_Annotation is a Java [annotation processor](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html#annotation-processing) designed to generate DTOs classes at compile time from entity classes to avoid manually creating them.
*Easily debuggable mapping code** (or editable by hand—e.g. in case of a bug in the generator)


## Examples

### Generate DTOs

We have the following EntityTest class :

``` java
package rabah.usthb;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;
import io.github.rabah.usthb.DTO;
import io.github.rabah.usthb.DTOField;
import java.util.HashMap;
import java.util.Map;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DTO(name = {"get", "post"})
public class EntityTest {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(nullable = true)
    @DTOField
    private String name;

    @DTOField
    private Map<String, Integer> tags = new HashMap<>();

   
    @DTOField(excludedDTO = {"get"})
    private String password;

}
```
DTO_Annotation will generate two DTOs (get, post) from EntityTest , fields that are annotated with `@DTOField`  are included in the DTOs unless they are excluded with `excludedDTO` array, the output will be :

``` java
package rabah.usthb;

import java.util.HashMap;
import java.util.Map;


public class getEntityTestDTO {

    private String name;
    private Map<String, Integer> tags = new HashMap<>();

}
```

``` java
package rabah.usthb;

import java.util.HashMap;
import java.util.Map;


public class postEntityTestDTO {

    private String name;
    private Map<String, Integer> tags = new HashMap<>();
    private String password;
}
```


### Generate DTOs & Entity
DTO_Annotation also allows us to add extra field that only figures in DTOs but to do so it also needs to generate the Entity class , so whenever a class has a field with `@DTOExtraField` it has to also generate the entity example following the EntityTest :

``` java
package rabah.usthb;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;
import io.github.rabah.usthb.DTO;
import io.github.rabah.usthb.DTOField;
import io.github.rabah.usthb.DTOExtraField;
import java.util.HashMap;
import java.util.Map;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DTO
public class EntityTest {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @DTOExtraField
    private String fullName;
    
    @DTOField
    private Map<String, Integer> tags = new HashMap<>();

   
    private String password;

}
```

DTO_Annotation will generate one DTO that has full name as its extra field, and an entity that doesn't have full name :

``` java
package rabah.usthb;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;
import java.util.HashMap;
import java.util.Map;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DTO
public class EntityTest {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;
    
    
    private Map<String, Integer> tags = new HashMap<>();

    private String password;

}
```


``` java
package rabah.usthb;

import java.util.HashMap;
import java.util.Map;


public class EntityTestDTO {

    private String fullName;
    private Map<String, Integer> tags = new HashMap<>(); 

}
```

## Using DTO_Annotation

### Maven

For Maven-based projects, add the following to your POM file in order to use DTO_Annotation (the dependencies are available at Maven Central):

```xml
    <dependencies>
        <dependency>
            <groupId>io.github.rabah-usthb</groupId>
            <artifactId>DTO_Annotation</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
```

### Gradle

For Gradle, you need something along the following lines:

```groovy
dependencies {
    implementation 'io.github.rabah-usthb:DTO_Annotation:1.0.0'
}
```

### No Dependency Management Tool

If you don't work with a dependency management tool, you can obtain a distribution bundle from [Releases page](https://github.com/rabah-usthb/DTO_Annotation/releases).

    

### IntelliJ 

Make sure that you have at least IntelliJ 2018.2.x (needed since support for `annotationProcessors` from the `maven-compiler-plugin` is from that version).
Enable annotation processing in IntelliJ (Build, Execution, Deployment -> Compiler -> Annotation Processors)


## Licensing

DTO_Annotation is licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.