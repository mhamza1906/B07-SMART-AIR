package com.example.smart_air;

import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;

public class LoginActivityPresenterTest {
    @Mock
    private LoginActivityView mockView;

    @Mock
    private LoginActivityModel mockModel;

    @Mock
    private FirebaseUser mockUser;

    @Mock
    private Task<AuthResult> mockAuthTask;

    @Mock
    private Task<DataSnapshot> mockDataSnapshotTask;

    @Mock
    DataSnapshot mockSnapshot;

    @Mock
    Task<DataSnapshot> mockTask;


    private LoginActivityPresenter presenter;

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        presenter = new LoginActivityPresenter(mockView, mockModel);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }


    // 1. Login Successful
    @Test
    public void loginUser_SuccessfulLogin_CallsGetUserFromRTDB() {
        when(mockAuthTask.isSuccessful()).thenReturn(true);
        when(mockModel.getSuccessfulLoginUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("user123");

        presenter.onSignInComplete(mockAuthTask);

        verify(mockView).toastMessage("Login successful!");
        verify(mockModel).getUserFromRTDB("user123");
    }

    // 2. Test getUserEmail specifically
    @Test
    public void loginUser_callsModelGetUserEmail() {
        String email = "test@example.com";
        String password = "password123";

        presenter.loginUser(email, password);

        verify(mockModel).getUserEmail(email);
    }


    // 3. Login Failed: ERROR_INVALID_CREDENTIAL
    @Test
    public void loginUser_ErrorInvalidCredential_ShowsToastAndForgotPassword() {
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getErrorCode()).thenReturn("ERROR_INVALID_CREDENTIAL");

        presenter.onSignInComplete(mockAuthTaskWithException(exception));

        verify(mockView).showForgotPasswordLink();
        verify(mockView).toastMessage("Incorrect password. Please try again.");
    }

    // 4. Login Failed: ERROR_INVALID_EMAIL
    @Test
    public void loginUser_ErrorInvalidEmail_ShowsToast() {
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getErrorCode()).thenReturn("ERROR_INVALID_EMAIL");

        presenter.onSignInComplete(mockAuthTaskWithException(exception));

        verify(mockView).toastMessage("Invalid email format.");
    }

    // 5. Login Failed: Other Exception
    @Test
    public void loginUser_OtherException_ShowsToast() {
        Exception exception = new Exception("Some error");

        presenter.onSignInComplete(mockAuthTaskWithException(exception));

        verify(mockView).toastMessage("Login failed: Some error");
    }

    // 6. Switch to Parent dashboard successfully
    @Test
    public void onUserFetchComplete_ParentUser_CallsViewNavigate() {
        // 1. create mock DataSnapshot and child node
        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        DataSnapshot mockChild = mock(DataSnapshot.class);

        // 2. Simulate child("accountType") and return mockChild
        when(mockSnapshot.child("accountType")).thenReturn(mockChild);

        // 3. Simulate getValue and return "Parent"
        when(mockChild.getValue(String.class)).thenReturn("Parent");

        // 4. Simulate DataSnapshot and call other methods
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getKey()).thenReturn("parent123");

        // 5. call Presenter.relative method
        presenter.onUserFetchComplete(Tasks.forResult(mockSnapshot));

        // 6. check if Presenter called View.navigation methods
        verify(mockView).navigateToParentDashboard("parent123");
        verify(mockView, never()).navigateToProviderDashboard(anyString());
    }




    // 7. Switch to Provider dashboard successfully

    @Test
    public void onUserFetchComplete_ProviderUser_CallsViewNavigate() {
        // 1. create mock DataSnapshot and child node
        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        DataSnapshot mockChild = mock(DataSnapshot.class);

        // 2. Simulate child("accountType") and return mockChild
        when(mockSnapshot.child("accountType")).thenReturn(mockChild);

        // 3. Simulate getValue and return "Provider"
        when(mockChild.getValue(String.class)).thenReturn("Healthcare Provider");

        // 4. Simulate DataSnapshot and call other methods
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getKey()).thenReturn("provider123");

        // 5. call Presenter.relative method
        presenter.onUserFetchComplete(Tasks.forResult(mockSnapshot));

        // 6. check if Presenter called View.navigation methods
        verify(mockView).navigateToProviderDashboard("provider123");
        verify(mockView, never()).navigateToParentDashboard(anyString());


    }

    // 8. Fail to switch user Dashboard: fail to get user's information
    // i.e. the information is wrong (e.g. accountType == null)

    @Test
    public void onUserFetchComplete_Failure_ShowsToast() {
        when(mockDataSnapshotTask.isSuccessful()).thenReturn(false);

        presenter.onUserFetchComplete(mockDataSnapshotTask);

        verify(mockView).toastMessage("Failed to retrieve user info");
    }

    // 9. Send the password reset email successfully

    @Test
    public void onResetEmailComplete_Success_ShowsDialog() {
        @SuppressWarnings("unchecked")
        Task<Void> mockTask = mock(Task.class, RETURNS_DEEP_STUBS);
        when(mockTask.isSuccessful()).thenReturn(true);

        presenter.onResetEmailComplete(mockTask);

        verify(mockView).showResetEmailSentDialog();
    }

    // 10. Fail to send the password reset email

    @Test
    public void onResetEmailComplete_Failure_ShowsToast() {
        @SuppressWarnings("unchecked")
        Task<Void> mockTask = mock(Task.class, RETURNS_DEEP_STUBS);
        when(mockTask.isSuccessful()).thenReturn(false);

        presenter.onResetEmailComplete(mockTask);

        verify(mockView).toastMessage("Failed to send reset email.");
    }

    // 11. Specifically test handleLoginError()

    @Test
    public void onSignInComplete_Failure_CallsHandleLoginError() {
        // mock an FirebaseAuthException, which does not call Android codes
        FirebaseAuthException mockEx = mock(FirebaseAuthException.class);
        when(mockEx.getErrorCode()).thenReturn("ERROR_INVALID_EMAIL");
        when(mockEx.getMessage()).thenReturn("Invalid email");

        // use Tasks.forException mock an exception
        Task<AuthResult> failedTask = Tasks.forException(mockEx);

        // call presenter,relative method
        presenter.onSignInComplete(failedTask);

        // check if view.relative method is called
        verify(mockView).toastMessage("Invalid email format.");
    }

    // 12. Login Successful, but no user found in the database (a very rare problem)

    @Test
    public void onSignInComplete_SuccessButNoUser_DoesNotCrash() {
        when(mockModel.getSuccessfulLoginUser()).thenReturn(null);
        Task<AuthResult> task = Tasks.forResult(mock(AuthResult.class));

        presenter.onSignInComplete(task);
        // the following view.toastMessage("Login successful!")，should not be called
        // even login is successful
        verify(mockView, never()).toastMessage("Login successful!");
    }

    // 13. Fail to switch user Dashboard: the behaviour of getting user's information failed
    // e.g. a network error

    @Test
    public void onUserFetchComplete_TaskFailure_ShowsToast() {
        Task<DataSnapshot> failedTask = Tasks.forException(new Exception("DB error"));
        presenter.onUserFetchComplete(failedTask);
        verify(mockView).toastMessage("Failed to retrieve user info");
    }

    // 14. Fail to switch user Dashboard: the behaviour of getting user's information successful
    // but the information does not exist

    @Test
    public void onUserFetchComplete_SnapshotNotExist_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        when(snapshot.exists()).thenReturn(false);
        presenter.onUserFetchComplete(Tasks.forResult(snapshot));
        verify(mockView).toastMessage("Failed to retrieve user info");
    }

    // 15. Fail to switch user Dashboard: the behaviour of getting user's information successful
    // but the user does not have data field: accountType

    @Test
    public void onUserFetchComplete_AccountTypeNull_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.child("accountType")).thenReturn(mock(DataSnapshot.class));
        when(snapshot.child("accountType").getValue(String.class)).thenReturn(null);

        presenter.onUserFetchComplete(Tasks.forResult(snapshot));
        verify(mockView).toastMessage("Account type cannot be found");
    }

    // 16. Fail to switch user Dashboard: the behaviour of getting user's information successful
    // but the user's accountType is neither Parent nor Healthcare Provider

    @Test
    public void onUserFetchComplete_AccountTypeUnknown_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        DataSnapshot child = mock(DataSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.child("accountType")).thenReturn(child);
        when(child.getValue(String.class)).thenReturn("UnknownType");
        when(snapshot.getKey()).thenReturn("id123");

        presenter.onUserFetchComplete(Tasks.forResult(snapshot));
        verify(mockView).toastMessage("Unknown account type");
    }

    // 17. Login Failed: specifically test the default branch that deal with other exception

    @Test
    public void handleLoginError_FirebaseAuthExceptionDefault_ShowsMessage() throws Exception {
        FirebaseAuthException mockEx = mock(FirebaseAuthException.class);
        when(mockEx.getErrorCode()).thenReturn("SOME_ERROR");
        when(mockEx.getMessage()).thenReturn("Something went wrong");

        presenterTestHandleLoginError(mockEx);

        verify(mockView).toastMessage("Login failed: Something went wrong");

    }

    // 18. Login Failed: the exception is null (a very rare problem)
    // e.g. it is difficult to give an example
    // but may happen when some complex firebase bugs occur

    @Test
    public void handleLoginError_NullException_ShowsDefaultMessage() throws Exception {
        presenterTestHandleLoginError(null);
        verify(mockView).toastMessage("Login failed.");
    }

    // 19. The user pressed the link "Forget Password?" to frequent
    // so a 2-minute cooling restriction is applied

    @Test
    public void handleCredentialRecoveryClick_WhenCooling_ShowsTooFrequentDialog() {
        when(mockView.isRecoveryCooling()).thenReturn(true);

        presenter.handleCredentialRecoveryClick("user@example.com");

        verify(mockView).showTooFrequentDialog();
        verify(mockModel, never()).sendResetEmail(anyString());
    }

    // 20. Test the user clicks the link "Forget Password?" at the first time
    // or the 2-minute cooling has passed

    @Test
    public void handleCredentialRecoveryClick_WhenNotCooling_SendsResetEmail() {
        when(mockView.isRecoveryCooling()).thenReturn(false);

        presenter.handleCredentialRecoveryClick("user@example.com");

        verify(mockModel).sendResetEmail("user@example.com");
        verify(mockView).startRecoveryCooldown();
    }

    // 21. onCheckEmailComplete: Email does not exist in database
    // Simulates a task where no children are returned from the database query
    // Expected behavior: View shows toast and redirects to Sign Up, Model.signUserIn is never called


    @Test
    public void onCheckEmailComplete_emailNotExist_showsToastAndRedirect() {
        presenter.loginUser("missing@example.com", "dummy");

        when(mockTask.isSuccessful()).thenReturn(true);
        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        when(mockTask.getResult()).thenReturn(mockSnapshot);
        when(mockTask.getResult().getChildren()).thenReturn(Collections.emptyList());

        presenter.onCheckEmailComplete(mockTask, "missing@example.com");

        verify(mockView).toastMessage("Your account is not found. Redirecting to Sign Up...");
        verify(mockView).redirectToSignUp();
        verify(mockModel, never()).signUserIn(anyString(), anyString());
    }

    // 22. onCheckEmailComplete: Email exists in database
    // Simulates a task returning a child with matching email
    // Expected behavior: Model.signUserIn is called, no redirect to Sign Up

    @Test
    public void onCheckEmailComplete_emailExists_callsSignUserIn() {
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockSnapshot.child("email")).thenReturn(mock(DataSnapshot.class));
        when(mockSnapshot.child("email").getValue(String.class)).thenReturn("exists@example.com");
        when(mockTask.getResult()).thenReturn(mock(DataSnapshot.class));
        when(mockTask.getResult().getChildren()).thenReturn(Collections.singletonList(mockSnapshot));

        presenter.loginUser("exists@example.com", "secret");

        presenter.onCheckEmailComplete(mockTask, "exists@example.com");

        verify(mockModel).signUserIn("exists@example.com", "secret");

        verify(mockView, never()).redirectToSignUp();
    }

    // 23. onCheckEmailComplete: Task fails with exception
    // Simulates a failed task where getException() is non-null
    // Expected behavior: View shows toast with exception message, no redirect, no login

    @Test
    public void onCheckEmailComplete_taskFailed_showsFailToast() {
        Exception ex = new Exception("Simulated failure");
        when(mockTask.isSuccessful()).thenReturn(false);
        when(mockTask.getException()).thenReturn(ex);

        presenter.onCheckEmailComplete(mockTask, "any@example.com");

        verify(mockView).toastMessage("Fail to check email exist: Simulated failure");
        verify(mockView, never()).redirectToSignUp();
        verify(mockModel, never()).signUserIn(anyString(), anyString());
    }

    // 24. onCheckEmailComplete: Task fails with null exception
    // Simulates a failed task where getException() returns null
    // Expected behavior: View shows toast with "unknown error", no redirect, no login

    @Test
    public void onCheckEmailComplete_taskFails_noException() {
        when(mockTask.isSuccessful()).thenReturn(false);
        when(mockTask.getException()).thenReturn(null);

        presenter.onCheckEmailComplete(mockTask, "any@example.com");

        verify(mockView).toastMessage("Fail to check email exist: unknown error");
    }

    // 25. onCheckEmailComplete: dbEmail is null
    // Simulates a task where the child email field is null
    // Expected behavior: View shows toast and redirects to Sign Up, login not called

    @Test
    public void onCheckEmailComplete_emailFieldNull_orMismatch() {

        presenter.loginUser("exists@example.com", "secret");

        when(mockTask.isSuccessful()).thenReturn(true);


        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        DataSnapshot mockChild = mock(DataSnapshot.class);
        when(mockSnapshot.child("email")).thenReturn(mockChild);
        when(mockChild.getValue(String.class)).thenReturn(null); // dbEmail 为 null

        DataSnapshot mockResult = mock(DataSnapshot.class);
        when(mockResult.getChildren()).thenReturn(Collections.singletonList(mockSnapshot));
        when(mockTask.getResult()).thenReturn(mockResult);

        presenter.onCheckEmailComplete(mockTask, "exists@example.com");


        verify(mockView).toastMessage("Your account is not found. Redirecting to Sign Up...");
        verify(mockView).redirectToSignUp();
    }

    // 26. onCheckEmailComplete: dbEmail is null but dbEmail not equal to input email
    // Simulates a task where dbEmail is not null but does not match input email
    // Expected behavior: treated as email not existing; view shows toast + redirect, login not called

    @Test
    public void onCheckEmailComplete_dbEmailNotEqual_emailExistsFalse() {
        when(mockTask.isSuccessful()).thenReturn(true);

        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        DataSnapshot mockChild = mock(DataSnapshot.class);
        when(mockSnapshot.child("email")).thenReturn(mockChild);

        when(mockChild.getValue(String.class)).thenReturn("other@example.com");

        DataSnapshot mockResult = mock(DataSnapshot.class);
        when(mockResult.getChildren()).thenReturn(Collections.singletonList(mockSnapshot));
        when(mockTask.getResult()).thenReturn(mockResult);

        presenter.loginUser("other@example.com", "secret");

        presenter.onCheckEmailComplete(mockTask, "exists@example.com");

        verify(mockView).toastMessage("Your account is not found. Redirecting to Sign Up...");
        verify(mockView).redirectToSignUp();
        verify(mockModel, never()).signUserIn(anyString(), anyString());
    }

    // 27. Test invalid email input (a null email)

    @Test
    public void loginUser_nullEmail_showsToastAndDoesNotCallModel() {
        presenter.loginUser(null, "pwd");

        verify(mockView).toastMessage("Invalid email format.");
        verify(mockModel, never()).getUserEmail(anyString());
    }


    // Helper Function: To call a private method

    private void presenterTestHandleLoginError(Exception ex) throws Exception {
        Method method = LoginActivityPresenter.class.getDeclaredMethod("handleLoginError", Exception.class);
        method.setAccessible(true);
        method.invoke(presenter, ex);
    }

    // Helper Function: Simulate AuthTask Exception

    private Task<AuthResult> mockAuthTaskWithException(Exception exception) {
        @SuppressWarnings("unchecked")
        Task<AuthResult> task = mock(Task.class, RETURNS_DEEP_STUBS);
        when(task.isSuccessful()).thenReturn(false);
        when(task.getException()).thenReturn(exception);
        return task;
    }

}
