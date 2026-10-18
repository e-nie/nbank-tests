package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferResponse extends BaseModel{
    private int receiverAccountId;
    private int senderAccountId;
    private String message;
    private double amount;

}
