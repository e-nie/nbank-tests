
# UI Test Cases — Iteration 2

## 1. Deposit Money

### Positive Test Cases

### TC-01: Successful deposit 
**Test Type:** End-to-End (E2E)


### TC-01: Successful deposit with valid amounts

**Test Type:** End-to-End (E2E), Parameterized

**Preconditions:**
- A user and a bank account are created via API.
- The user is logged in.
- The initial account balance is retrieved via API (GET request).

**Test Data:** `0.01`, `5000`

**Steps:**
1. Click "Deposit Money".
2. Select the account from the dropdown.
3. Enter an amount from the test data.
4. Click "Deposit".
5. Repeat for each test value.

**Expected Result:**
- A success alert is displayed.
- The account balance is increased by the deposited amount.
- The updated balance is retrieved via API (GET request) and equals the initial balance + deposited amount.


### Negative Test Cases

### TC-02: Deposit amount exceeds 5000
**Test Type:** Client-Side UI Validation

**Preconditions:**
- A user and a bank account are created via API.
- The user is logged in.

**Steps:**
1. Click "Deposit Money".
2. Select the account from the dropdown.
3. Enter `5001` in the amount field.
4. Click "Deposit".

**Expected Result:**
- An alert is displayed: "❌ Please deposit less or equal to 5000$."
- No POST request is sent to the deposit API.
- The account balance remains unchanged.



### TC-03: Deposit with invalid amount

**Test Type:** Client-Side UI Validation (Parameterized)

**Preconditions:**
- A user and a bank account are created via API.
- The user is logged in.

**Test Data:** `0`, `-1`, empty value

**Steps:**
1. Click "Deposit Money".
2. Select the account from the dropdown.
3. Enter an amount from the test data (or leave the field empty).
4. Click "Deposit".
5. Repeat for each test value.

**Expected Result:**
- An error alert is displayed: "❌ Please enter a valid amount."
- No POST request is sent to the deposit API.
- The account balance remains unchanged.


### TC-04: Deposit without selecting an account

**Test Type:** Client-Side UI Validation

**Preconditions:**
- A user and a bank account are created via API.
- The user is logged in.

**Steps:**
1. Click "Deposit Money".
2. Leave the account dropdown unselected.
3. Enter a valid deposit amount in the amount field.
4. Click "Deposit".

**Expected Result:**
- An error alert is displayed: "❌ Please select an account."
- No POST request is sent to the deposit API.
- The account balance remains unchanged.


### TC-05: User can see only their own accounts

**Test Type:** End-to-End (E2E)

**Preconditions:**
- User A and User B are created via API.
- Each user has a bank account created via API.

**Steps:**
1. Log in as User A.
2. Click "Deposit Money".
3. Open the account dropdown.

**Expected Result:**
- User A's account is displayed in the dropdown.
- User B's account is not displayed.


### TC-06: Error handling when deposit fails

**Test Type:** UI–API Integration (Mocked API)

**Preconditions:**
- A user and a bank account are created via API.
- The user is logged in.
- The POST /accounts/deposit response is mocked with HTTP 500.

**Steps:**
1. Click "Deposit Money".
2. Select an account from the dropdown.
3. Enter a valid deposit amount.
4. Click "Deposit".

**Expected Result:**
- An error alert is displayed: "❌ Failed to deposit. Please try again."
- No success message is displayed.
- The user remains on the Deposit Money page.



## 2. Update Profile Name

### Positive Test Cases

### TC-07: Successful profile name update

**Test Type:** End-to-End (E2E)

**Preconditions:**
- A user is created via API.
- The user is logged in.
- The current profile name is retrieved via API (GET request).

**Steps:**
1. Click the user profile in the top-right corner.
2. Enter a new valid name consisting of two words with letters only.
3. Click "Save Changes".

**Expected Result:**
- A success alert is displayed: "✅ Name updated successfully!"
- The new profile name is displayed in the UI.
- The updated name is confirmed via API (GET request).


### Negative Test Cases

### TC-08: Profile name with invalid number of words

**Test Type:** UI–API Integration (Real API), Parameterized

**Preconditions:**
- A user is created via API.
- The user is logged in.

**Test Data:** `Anna`, `Anna Maria Schmidt`

**Steps:**
1. Click the user profile in the top-right corner.
2. Enter a name from the test data.
3. Click "Save Changes".
4. Repeat for each test value.

**Expected Result:**
- An error alert is displayed: "Name must contain two words with letters only."
- The profile name remains unchanged.


### TC-09: Profile name is empty

**Test Type:** Client-Side UI Validation (Parameterized)

**Preconditions:**
- A user is created via API.
- The user is logged in.

**Test Data:** Empty value, spaces only

**Steps:**
1. Click the user profile in the top-right corner.
2. Leave the name field empty or enter spaces only.
3. Click "Save Changes".
4. Repeat for each test value.

**Expected Result:**
- An error alert is displayed: "❌ Please enter a valid name."
- No PUT request is sent to the API.
- The profile name remains unchanged.



### TC-10: New profile name is the same as the current name

**Test Type:** Client-Side UI Validation

**Preconditions:**
- A user is created via API.
- A valid profile name is set via API.
- The user is logged in.

**Steps:**
1. Click the user profile in the top-right corner.
2. Enter the current profile name.
3. Click "Save Changes".

**Expected Result:**
- A warning alert is displayed: "⚠️ New name is the same as the current one."
- No PUT request is sent to the API.
- The profile name remains unchanged.


### TC-11: Profile name contains invalid characters

**Test Type:** UI–API Integration (Real API), Parameterized

**Preconditions:**
- A user is created via API.
- The user is logged in.

**Test Data:** `Anna123 Schmidt`, `Anna Schmidt!`, `Anna "Schmidt"`

**Steps:**
1. Click the user profile in the top-right corner.
2. Enter a name from the test data.
3. Click "Save Changes".
4. Repeat for each test value.

**Expected Result:**
- An error alert is displayed: "Name must contain two words with letters only."
- The profile name remains unchanged.


## 3. Transfer Money

### Positive Test Cases

### TC-12: Successful transfer between accounts

**Test Type:** End-to-End (E2E), Parameterized

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The sender account is funded via API (deposit) with sufficient funds for the transfer.
- The user is logged in.
- The initial balances of both accounts are retrieved via API (GET request).

**Test Data:** `0.01`, `10000`

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter the registered recipient name.
4. Enter the recipient account number.
5. Enter an amount from the test data.
6. Check "Confirm details are correct".
7. Click "Send Transfer".
8. Repeat for each test value.

**Expected Result:**
- A success alert is displayed.
- The sender account balance is decreased by the transferred amount.
- The recipient account balance is increased by the transferred amount.
- Both updated balances are confirmed via API (GET requests).


### Negative Test Cases

### TC-13: Transfer with empty required fields

**Test Type:** Client-Side UI Validation, Parameterized

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The user is logged in.

**Test Data:** Missing sender account, missing recipient account, empty amount

**Steps:**
1. Click "Make a Transfer".
2. Fill in all required fields with valid data, except one field from the test data.
3. Check "Confirm details are correct".
4. Click "Send Transfer".
5. Repeat for each missing field.

**Expected Result:**
- An error alert is displayed: "❌ Please fill all fields and confirm."
- No POST transfer request is sent to the API.
- The account balances remain unchanged.


### TC-14: Transfer without confirmation

**Test Type:** Client-Side UI Validation

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The user is logged in.

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter the registered recipient name.
4. Enter the recipient account number.
5. Enter a valid transfer amount.
6. Leave "Confirm details are correct" unchecked.
7. Click "Send Transfer".

**Expected Result:**
- An error alert is displayed: "❌ Please fill all fields and confirm."
- No POST transfer request is sent to the API.
- The account balances remain unchanged.


### TC-15: Transfer to the same account

**Test Type:** Client-Side UI Validation

**Preconditions:**
- A user with a bank account is created via API.
- A valid profile name is set via API.
- The user is logged in.

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter the registered recipient name.
4. Enter the same account number as the sender's account.
5. Enter a valid transfer amount.
6. Check "Confirm details are correct".
7. Click "Send Transfer".

**Expected Result:**
- An error alert is displayed: "❌ You cannot transfer money to the same account."
- No POST transfer request is sent to the API.
- The account balance remains unchanged.


### TC-16: Transfer with incorrect recipient name

**Test Type:** UI–API Integration (Real API)

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The user is logged in.
- The sender account is funded via API (deposit) with sufficient funds for the transfer.

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter an incorrect recipient name.
4. Enter the recipient account number.
5. Enter a valid transfer amount.
6. Check "Confirm details are correct".
7. Click "Send Transfer".

**Expected Result:**
- An error alert is displayed: "❌ The recipient name does not match the registered name."
- No POST transfer request is sent to the API.
- The account balances remain unchanged.



### TC-17: Transfer amount exceeds maximum limit

**Test Type:** UI–API Integration (Real API)

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The sender account is funded via API (deposit) with a balance greater than 10000.01.
- The user is logged in.

**Test Data:** `10000.01`

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter the registered recipient name.
4. Enter the recipient account number.
5. Enter `10000.01` as the transfer amount.
6. Check "Confirm details are correct".
7. Click "Send Transfer".

**Expected Result:**
- The transfer request is sent to the API.
- An error message from the backend is displayed.
- The transfer is rejected.
- Both account balances remain unchanged, confirmed via API (GET requests).


### TC-18: Transfer with insufficient funds

**Test Type:** UI–API Integration (Real API)

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The sender account balance is set up via API to be lower than the transfer amount.
- The user is logged in.
- The initial balances of both accounts are retrieved via API (GET requests).

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter the registered recipient name.
4. Enter the recipient account number.
5. Enter a transfer amount greater than the sender's balance but within the allowed limit.
6. Check "Confirm details are correct".
7. Click "Send Transfer".

**Expected Result:**
- The transfer request is sent to the API.
- An error message from the backend is displayed.
- The transfer is rejected.
- Both account balances remain unchanged, confirmed via API (GET requests).


### TC-19: Transfer to a non-existing recipient account

**Test Type:** UI–API Integration (Real API)

**Preconditions:**
- A user with a bank account is created via API.
- A valid profile name is set via API.
- The sender account is funded via API (deposit).
- The user is logged in.
- A non-existing recipient account number is prepared.

**Steps:**
1. Click "Make a Transfer".
2. Select the sender account.
3. Enter a recipient name.
4. Enter the non-existing recipient account number.
5. Enter a valid transfer amount.
6. Check "Confirm details are correct".
7. Click "Send Transfer".

**Expected Result:**
- An error alert is displayed: "❌ No user found with this account number."
- No POST transfer request is sent to the API.
- The sender account balance remains unchanged.


### Positive Test Cases

### TC-20: Repeat a successful transfer

**Test Type:** End-to-End (E2E)

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- The sender account is funded via API (deposit).
- A successful transfer between the accounts is performed via API.
- The user is logged in.
- The initial balances of both accounts are retrieved via API (GET requests).

**Steps:**
1. Click "Make a Transfer".
2. Select "Transfer Again".
3. Select the previous transfer.
4. Verify that the transfer details are prefilled.
5. Check "Confirm details are correct".
6. Click "Send Transfer".

**Expected Result:**
- The previous transfer details are displayed correctly.
- A success alert is displayed.
- The sender's balance decreases by the transferred amount.
- The recipient's balance increases by the transferred amount.
- Both updated balances are confirmed via API (GET requests).



### Negative Test Cases

### TC-21: Repeat transfer with invalid amount

**Test Type:** UI–API Integration (Real API), Parameterized

**Preconditions:**
- A user with two bank accounts is created via API.
- A valid profile name is set via API.
- A successful transfer is performed via API.
- The sender account is funded via API (deposit).
- The user is logged in.

**Test Data:** `0`, `-1`, `10000.01`

**Steps:**
1. Click "Make a Transfer".
2. Select "Transfer Again".
3. Find a previous transfer and click "Repeat".
4. Select the sender account.
5. Enter an amount from the test data.
6. Check "Confirm details are correct".
7. Click "Send Transfer".
8. Repeat for each test value.

**Expected Result:**
- The transfer request is sent to the API.
- An error alert with the backend message is displayed.
- The transfer is rejected.
- Both account balances remain unchanged, confirmed via API (GET requests).
