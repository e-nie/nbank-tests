package models;

import generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ChangeProfileRequest extends BaseModel {

    @GeneratingRule(regex = "^[A-Za-z]+ [A-Za-z]+$")
    private String name;
}
