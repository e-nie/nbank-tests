package models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse extends BaseModel {

    private long id;
    private double amount;
    private String type;
    private String timestamp;
    private long relatedAccountId;
}