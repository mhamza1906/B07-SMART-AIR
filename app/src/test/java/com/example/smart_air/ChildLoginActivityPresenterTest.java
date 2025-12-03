package com.example.smart_air;

import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChildLoginActivityPresenterTest {
    @Mock
    ChildLoginActivityView mockView;

    @Mock
    ChildLoginActivityModel mockModel;

    @Mock
    FirebaseAuth mockFirebaseAuth;

    @Mock
    FirebaseUser mockFirebaseUser;

    private ChildLoginActivityPresenter presenter;

    private AutoCloseable mocks;

    private static final Logger logger = Logger.getLogger(ChildLoginActivityPresenterTest.class.getName());

    @Before
    public void setUp(){
        mocks = MockitoAnnotations.openMocks(this);
        presenter = new ChildLoginActivityPresenter(mockView, mockModel);
    }

    @After
    public void tearDown() {
        if (mocks != null) {
            try {
                mocks.close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to close mocks", e);
            }
        }
    }

    // 1. Login successful, Firebase returns a valid user

    @Test
    public void onSignInComplete_Success_CallsGetUserFromRTDB() {
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockModel.getFirebaseAuth()).thenReturn(mockAuth);
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("child123");

        Task<AuthResult> task = Tasks.forResult(mock(AuthResult.class));

        presenter.onSignInComplete(task);

        verify(mockView).toastMessage("Welcome to SMART-AIR!");
        verify(mockModel).getUserFromRTDB("child123");

    }

    // 2. Login failed due to invalid password

    @Test
    public void onSignInComplete_Failure_InvalidCredential_ShowsToast() {
        FirebaseAuthException ex = mock(FirebaseAuthException.class);
        when(ex.getErrorCode()).thenReturn("ERROR_INVALID_CREDENTIAL");
        when(ex.getMessage()).thenReturn("Wrong password");

        Task<AuthResult> failedTask = Tasks.forException(ex);
        presenter.onSignInComplete(failedTask);

        verify(mockView).toastMessage("Incorrect password. Please try again.");
    }

    // 3. Login failed due to user not found

    @Test
    public void onSignInComplete_Failure_UserNotFound_ShowsToast() {
        FirebaseAuthException ex = mock(FirebaseAuthException.class);
        when(ex.getErrorCode()).thenReturn("ERROR_USER_NOT_FOUND");

        Task<AuthResult> failedTask = Tasks.forException(ex);
        presenter.onSignInComplete(failedTask);

        verify(mockView).toastMessage("You DON'T have an account, please tell your parent to register you first");
    }

    // 4. Login failed due to other exceptions

    @Test
    public void onSignInComplete_Failure_OtherException_ShowsToast() {
        Exception ex = new Exception("Some error");
        Task<AuthResult> failedTask = Tasks.forException(ex);
        presenter.onSignInComplete(failedTask);

        verify(mockView).toastMessage("Login failed: Some error");
    }

    // 5. Login successful but FirebaseAuth returns null (rare)

    @Test
    public void onSignInComplete_SuccessButNoUser_DoesNothing() {
        // mock AuthResult
        AuthResult mockAuthResult = mock(AuthResult.class);
        Task<AuthResult> mockTask = Tasks.forResult(mockAuthResult);

        when(mockModel.getFirebaseAuth()).thenReturn(mockFirebaseAuth);
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(null);

        presenter.onSignInComplete(mockTask);

        verify(mockView, never()).toastMessage(anyString());
    }

    // 6. Task failed (e.g., network error)

    @Test
    public void onUserFetchComplete_TaskFailure_ShowsToast() {
        Task<DataSnapshot> failedTask = Tasks.forException(new Exception("DB error"));
        presenter.onUserFetchComplete(failedTask);

        verify(mockView).toastMessage("Failed to retrieve child info");
    }

    // 7. Task successful but snapshot does not exist

    @Test
    public void onUserFetchComplete_SnapshotNotExist_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        when(snapshot.exists()).thenReturn(false);

        Task<DataSnapshot> task = Tasks.forResult(snapshot);
        presenter.onUserFetchComplete(task);

        verify(mockView).toastMessage("Failed to retrieve child info");
    }

    // 8. Task successful, snapshot exists, but accountType is null

    @Test
    public void onUserFetchComplete_AccountTypeNull_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        DataSnapshot child = mock(DataSnapshot.class);

        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getKey()).thenReturn("child123");
        when(snapshot.child("accountType")).thenReturn(child);
        when(child.getValue(String.class)).thenReturn(null);

        presenter.onUserFetchComplete(Tasks.forResult(snapshot));

        verify(mockView).toastMessage("Account type cannot be found");
    }

    // 9. Task successful, accountType unknown

    @Test
    public void onUserFetchComplete_AccountTypeUnknown_ShowsToast() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        DataSnapshot child = mock(DataSnapshot.class);

        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getKey()).thenReturn("child123");
        when(snapshot.child("accountType")).thenReturn(child);
        when(child.getValue(String.class)).thenReturn("UnknownType");

        presenter.onUserFetchComplete(Tasks.forResult(snapshot));

        verify(mockView).toastMessage("Unknown account type");
    }

    // 10. Task successful, accountType is Child

    @Test
    public void onUserFetchComplete_AccountTypeChild_CallsSwitchToDashboard() {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        DataSnapshot child = mock(DataSnapshot.class);

        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getKey()).thenReturn("child123");
        when(snapshot.child("accountType")).thenReturn(child);
        when(child.getValue(String.class)).thenReturn("Child");

        presenter.onUserFetchComplete(Tasks.forResult(snapshot));

        verify(mockView).switchToChildDashboard("child123");
    }

    // 11. Test if loginUser calls model.signUserIn

    @Test
    public void loginUser_CallsModelSignUserIn() {
        String email = "child@example.com";
        String password = "123";

        presenter.loginUser(email, password);

        verify(mockModel).signUserIn(email, password);
    }

    // 12. Default branch of handleLoginError (FirebaseAuthException unknown code)

    @Test
    public void handleLoginError_DefaultBranch_ShowsCorrectToast() throws Exception {
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getErrorCode()).thenReturn("SOME_OTHER_ERROR");
        when(exception.getMessage()).thenReturn("Something went wrong");

        // call private methods
        Method method = ChildLoginActivityPresenter.class.getDeclaredMethod("handleLoginError", Exception.class);
        method.setAccessible(true);
        method.invoke(presenter, exception);

        verify(mockView).toastMessage("Login failed: Something went wrong");
    }


    // 13. Exception is null (rare case)

    @Test
    public void handleLoginError_NullException_ShowsDefaultMessage() throws Exception {
        Method method = ChildLoginActivityPresenter.class
                .getDeclaredMethod("handleLoginError", Exception.class);
        method.setAccessible(true);
        method.invoke(presenter, new Object[]{null});

        verify(mockView).toastMessage("Login failed.");
    }


}

