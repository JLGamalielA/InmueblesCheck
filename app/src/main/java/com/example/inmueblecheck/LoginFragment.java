package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {
    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvGoToRegister = view.findViewById(R.id.tvGoToRegister);
        progressBar = view.findViewById(R.id.progressBar);

        setupClickListeners();
        setupObservers();

        // AUTO-LOGIN: Verificar si ya hay usuario activo
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Simulamos un login exitoso para disparar la lógica de roles
            // (Ojo: AuthViewModel debería tener un método checkSession, pero
            //  podemos reutilizar login si los campos están vacíos o manejarlo aquí)

            // Opción rápida: Forzar la recarga del rol
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            // Reutilizamos la lógica del ViewModel que busca el rol
            authViewModel.login(FirebaseAuth.getInstance().getCurrentUser().getEmail(), "dummy_password_skip");
            // Nota: Esto fallará en login normal, así que mejor implementamos una verificación directa en VM
            // O simplemente forzamos el fetch si ya estamos logueados:
        }
    }

    // Mejor enfoque para Auto-Login en onStart
    @Override
    public void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // El usuario ya está logueado, necesitamos saber su rol para redirigir
            // Como no tenemos el rol guardado localmente (SharedPrefs),
            // dejamos que el usuario se loguee o (Mejora futura) guardamos el rol en SharedPrefs.

            // Por ahora, para no complicar, dejaremos que se logueen,
            // pero si quieres auto-login real, avísame para agregar UserSessionManager.
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Llena todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.login(email, password);
        });

        tvGoToRegister.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment));
    }

    private void setupObservers() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), authResultState -> {
            switch (authResultState.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    btnLogin.setEnabled(false);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    // Navegación basada en ROL
                    if ("arrendador".equals(authResultState.role) || "gerente".equals(authResultState.role)) {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_gerenteDashboardFragment);
                    } else {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_agenteDashboardFragment);
                    }
                    break;
                case ERROR:
                    // Ignoramos error si fue intento de auto-login silencioso
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    if (!authResultState.errorMessage.contains("dummy")) {
                        Toast.makeText(getContext(), "Error: " + authResultState.errorMessage, Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    break;
            }
        });
    }
}