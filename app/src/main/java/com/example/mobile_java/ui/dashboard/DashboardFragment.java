package com.example.mobile_java.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.TextView; // Ya no es necesario para el texto principal

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
// import androidx.lifecycle.ViewModelProvider; // No necesario si el ViewModel no hace nada dinámico para esta UI

import com.example.mobile_java.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Si DashboardViewModel ya no es necesario para la UI, puedes omitir estas líneas:
        // DashboardViewModel dashboardViewModel =
        //        new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Ya no necesitamos esto si todo el texto es estático desde strings.xml
        // final TextView textView = binding.textDashboard; // Este ID ya no existe o su propósito cambió
        // dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Aquí podrías añadir lógica si quieres animaciones para la aparición de elementos
        // por ejemplo, hacer un fade-in de las tarjetas secuencialmente.

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}