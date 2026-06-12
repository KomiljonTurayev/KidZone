package uz.kidzone.app;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirebaseManagerTest {

    @Test
    public void notConfigured_isAvailableFalse() {
        FirebaseManager manager = new FirebaseManager(null);
        assertFalse(manager.isAvailable());
    }

    @Test
    public void notConfigured_getCurrentUserNull() {
        FirebaseManager manager = new FirebaseManager(null);
        assertNull(manager.getCurrentUser());
    }

    @Test
    public void notConfigured_getUidNull() {
        FirebaseManager manager = new FirebaseManager(null);
        assertNull(manager.getUid());
    }

    @Test
    public void notConfigured_signInWithEmail_callsOnError() {
        FirebaseManager manager = new FirebaseManager(null);
        final String[] error = {null};
        manager.signInWithEmail("a@b.com", "password1", new FirebaseManager.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) { fail("should not succeed"); }
            @Override public void onError(String message) { error[0] = message; }
        });
        assertNotNull(error[0]);
    }

    @Test
    public void notConfigured_signOut_doesNotThrow() {
        FirebaseManager manager = new FirebaseManager(null);
        manager.signOut();
    }

    @Test
    public void configured_isAvailableTrue() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseManager manager = new FirebaseManager(auth);
        assertTrue(manager.isAvailable());
    }

    @Test
    public void configured_getUid_returnsCurrentUserUid() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("uid-123");
        when(auth.getCurrentUser()).thenReturn(user);

        FirebaseManager manager = new FirebaseManager(auth);
        assertEquals("uid-123", manager.getUid());
    }

    @Test
    public void configured_getUid_noCurrentUser_returnsNull() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        when(auth.getCurrentUser()).thenReturn(null);

        FirebaseManager manager = new FirebaseManager(auth);
        assertNull(manager.getUid());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void configured_signInWithEmail_delegatesToFirebaseAuth() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        Task<AuthResult> task = mock(Task.class);
        when(task.addOnSuccessListener(any())).thenReturn(task);
        when(task.addOnFailureListener(any())).thenReturn(task);
        when(auth.signInWithEmailAndPassword("a@b.com", "password1")).thenReturn(task);

        FirebaseManager manager = new FirebaseManager(auth);
        manager.signInWithEmail("a@b.com", "password1", new FirebaseManager.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {}
            @Override public void onError(String message) {}
        });

        verify(auth).signInWithEmailAndPassword("a@b.com", "password1");
        verify(task).addOnSuccessListener(any());
        verify(task).addOnFailureListener(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void configured_createAccountWithEmail_delegatesToFirebaseAuth() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        Task<AuthResult> task = mock(Task.class);
        when(task.addOnSuccessListener(any())).thenReturn(task);
        when(task.addOnFailureListener(any())).thenReturn(task);
        when(auth.createUserWithEmailAndPassword("a@b.com", "password1")).thenReturn(task);

        FirebaseManager manager = new FirebaseManager(auth);
        manager.createAccountWithEmail("a@b.com", "password1", new FirebaseManager.AuthCallback() {
            @Override public void onSuccess(FirebaseUser user) {}
            @Override public void onError(String message) {}
        });

        verify(auth).createUserWithEmailAndPassword("a@b.com", "password1");
        verify(task).addOnSuccessListener(any());
        verify(task).addOnFailureListener(any());
    }

    @Test
    public void configured_signOut_delegatesToFirebaseAuth() {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseManager manager = new FirebaseManager(auth);
        manager.signOut();
        verify(auth).signOut();
    }
}
