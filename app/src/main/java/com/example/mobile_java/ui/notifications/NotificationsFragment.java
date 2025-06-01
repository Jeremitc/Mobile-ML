package com.example.mobile_java.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.TextView; // Ya no es necesario para el texto principal

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Añadir para onViewCreated
import androidx.fragment.app.Fragment;
// import androidx.lifecycle.ViewModelProvider; // No necesario si el ViewModel no hace nada dinámico para esta UI

import com.example.mobile_java.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Si NotificationsViewModel ya no es necesario para la UI, puedes omitir estas líneas:
        // NotificationsViewModel notificationsViewModel =
        //        new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Ya no necesitamos esto si todo el texto es estático desde strings.xml
        // final TextView textView = binding.textNotifications; // Este ID ya no existe o su propósito cambió
        // notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    // Opcional: Animación de entrada para la tarjeta
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding.getRoot().getChildCount() > 0 && binding.getRoot().getChildAt(0) instanceof com.google.android.material.card.MaterialCardView) {
            View card = binding.getRoot().getChildAt(0); // Asume que la CardView es el primer hijo del ConstraintLayout raíz
            card.setAlpha(0f);
            card.setScaleX(0.8f);
            card.setScaleY(0.8f);
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}