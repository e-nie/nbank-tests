package requests.skeleton;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),

    LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class

    ),
    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponse.class
    ),
    DEPOSIT("/accounts/deposit",
            DepositRequest.class,
            BaseModel.class
    ),
    CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            AccountResponse.class
    ),
    PROFILE(
            "/customer/profile",
            BaseModel.class,
            GetProfileResponse.class
    );


    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;

}
