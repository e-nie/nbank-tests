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
public class CreateUserRequest extends BaseModel {
    @GeneratingRule(regex = "^[a-zA-Z0-9._-]{3,15}$")
    private String username;

    @GeneratingRule(regex = "[a-z][A-Z][0-9][!@#$%&][a-zA-Z0-9!@#$%&]{4,12}")
    private String password;

    @GeneratingRule(regex = "^USER$")
    private String role;
}
