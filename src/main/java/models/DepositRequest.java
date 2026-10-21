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
public class DepositRequest extends BaseModel {
    private long id;

    @GeneratingRule(regex = "^[1-9][0-9]{0,2}\\.[0-9]{2}$")
    private double balance;

}
