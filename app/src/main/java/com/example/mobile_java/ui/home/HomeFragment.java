package com.example.mobile_java.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_java.databinding.FragmentHomeBinding;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private Uri imageUri; // Para guardar la URI de la imagen tomada con la cámara

    // ActivityResultLauncher para permisos
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher para la cámara
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result && imageUri != null) { // 'result' es true si la foto se tomó y guardó en imageUri
                    try {
                        Bitmap bitmap = loadBitmapFromUri(imageUri);
                        if (bitmap != null) {
                            homeViewModel.classifyImage(bitmap);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image from URI: ", e);
                        Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "Camera cancelled or imageUri is null");
                }
            });

    // ActivityResultLauncher para la galería
    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = loadBitmapFromUri(uri);
                        if (bitmap != null) {
                            homeViewModel.classifyImage(bitmap);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image from gallery: ", e);
                        Toast.makeText(getContext(), "Error al cargar imagen de galería", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar el clasificador en el ViewModel
        homeViewModel.initializeInterpreter(requireContext());


        // Observadores para actualizar la UI
        homeViewModel.getText().observe(getViewLifecycleOwner(), binding.textViewResult::setText); // Usamos textViewResult para mensajes iniciales
        homeViewModel.getSelectedImage().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                binding.imageViewPreview.setImageBitmap(bitmap);
                // Animación de fade-in para la imagen
                binding.imageViewPreview.setAlpha(0f);
                binding.imageViewPreview.animate().alpha(1f).setDuration(500).start(); // 500 ms de duración
            }
        });

        homeViewModel.getClassificationResult().observe(getViewLifecycleOwner(), resultText -> {
            binding.textViewResult.setText(resultText);
            if (resultText != null && !resultText.isEmpty()) {
                // Animación de fade-in para el texto de resultados
                binding.cardViewResult.setAlpha(0f); // Animar la tarjeta contenedora
                binding.cardViewResult.animate().alpha(1f).setDuration(500).start();
            }
        });

        binding.buttonCamera.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        binding.buttonGallery.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        return root;
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        takePictureLauncher.launch(imageUri);
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireActivity().getContentResolver(), uri));
        } else {
            bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
        }
        // Copiamos el bitmap para hacerlo mutable si es necesario, y para evitar problemas con bitmaps respaldados por hardware.
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}