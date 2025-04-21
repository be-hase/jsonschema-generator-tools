import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class Wrapper {
    public record Pojo(Animal animal) {}

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = UMA.class
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(Dog.class),
            @JsonSubTypes.Type(Cat.class),
            @JsonSubTypes.Type(UMA.class)
    })
    public interface Animal {}

    @JsonTypeName("dog")
    public record Dog(
            String name,
            @JsonPropertyDescription("breed description") String breed
    ) implements Animal {}

    @JsonTypeName("cat")
    public record Cat(
            String name,
            String color
    ) implements Animal {}

    @JsonTypeName("uma")
    public static class UMA implements Animal {}

    public record Person(
            @NotBlank String name,
            @Min(0) @Max(200) int age
    ) {
    }

    public static void main(String[] args) {
        System.out.println(Pojo.class.getName());
    }

    public enum Gender {
        MALE {
            @Override
            public String toString() {
                return "M";
            }
        },
        FEMALE {
            @Override
            public String toString() {
                return "F";
            }
        },
        OTHERS {
            @Override
            public String toString() {
                return "O";
            }
        },
    }

}
