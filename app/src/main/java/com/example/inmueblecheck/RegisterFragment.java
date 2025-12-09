package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private RadioGroup radioGroupRole;
    private RadioButton radioArrendatario, radioArrendador; // Referencias para cambiar texto si es necesario
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etEmail = view.findViewById(R.id.etEmailRegister);
        etPassword = view.findViewById(R.id.etPasswordRegister);
        radioGroupRole = view.findViewById(R.id.radioGroupRole);

        // Reutilizamos los IDs del layout anterior, pero cambiamos su significado lógico
        radioArrendatario = view.findViewById(R.id.radioAgente); // ID original era radioAgente
        radioArrendador = view.findViewById(R.id.radioGerente); // ID original era radioGerente

        radioArrendatario.setText("Busco Inmueble ");
        radioArrendador.setText("Vendo Inmueble ");

        btnRegister = view.findViewById(R.id.btnRegister);
        tvGoToLogin = view.findViewById(R.id.tvGoToLogin);
        progressBar = view.findViewById(R.id.progressBarRegister);

        setupClickListeners();
        setupObservers();
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
            String role;

            // Mapeo de IDs antiguos a Roles Nuevos
            if (selectedRoleId == R.id.radioGerente) {
                role = "arrendador"; // Dueño
            } else {
                role = "arrendatario"; // Cliente
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, llena todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.register(email, password, role);
        });

        tvGoToLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    private void setupObservers() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), authResultState -> {
            switch (authResultState.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    btnRegister.setEnabled(false);
                    break;
                case REGISTRATION_SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(getContext(), "¡Registro exitoso! Inicia sesión para continuar.", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(getView()).navigate(R.id.action_registerFragment_to_loginFragment);
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(getContext(), "Error: " + authResultState.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    break;
            }
        });
    }
}