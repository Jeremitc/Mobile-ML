package com.example.mobile_final.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_final.R;
import com.example.mobile_final.databinding.FragmentHomeBinding;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.classifier.Classifications;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    private ImageView imageViewPreview;
    private TextView textViewPrediction;
    private Button buttonSelectImage;
    private Button buttonPredict;

    private Bitmap selectedBitmap;
    private ImageClassifier imageClassifier;

    // ActivityResultLauncher for picking an image
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(), imageUri);
                            imageViewPreview.setImageBitmap(selectedBitmap);
                            textViewPrediction.setText(getString(R.string.prediction_results_placeholder)); // Reset prediction text
                            buttonPredict.setEnabled(true);
                        } catch (IOException e) {
                            Log.e("HomeFragment", "Error loading image: " + e.getMessage());
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                            selectedBitmap = null;
                            buttonPredict.setEnabled(false);
                        }
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set initial text for text_home from strings.xml via ViewModel
        homeViewModel.setText(getString(R.string.welcome_home_screen_message));
        final TextView textViewHome = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textViewHome::setText);

        imageViewPreview = binding.imagePreview;
        textViewPrediction = binding.textPredictionResult;
        buttonSelectImage = binding.buttonSelectImage;
        buttonPredict = binding.buttonPredict;

        setupImageClassifier();

        buttonSelectImage.setOnClickListener(v -> openImagePicker());
        buttonPredict.setOnClickListener(v -> runPrediction());
        buttonPredict.setEnabled(false); // Initially disable predict button

        return root;
    }

    private void setupImageClassifier() {
        try {
            // Make sure "model.tflite" is in your assets folder
            ImageClassifier.ImageClassifierOptions options =
                    ImageClassifier.ImageClassifierOptions.builder()
                            .setMaxResults(3) // Get top 3 results
                            .build();
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                    requireContext(), "model.tflite", options); // Ensure model.tflite is in assets
        } catch (IOException e) {
            Log.e("HomeFragment", "TFLite failed to load model with error: " + e.getMessage());
            Toast.makeText(getContext(), getString(R.string.error_model_load), Toast.LENGTH_LONG).show();
            textViewPrediction.setText(getString(R.string.error_model_load));
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void runPrediction() {
        if (selectedBitmap == null) {
            Toast.makeText(getContext(), getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageClassifier == null) {
            Toast.makeText(getContext(), getString(R.string.error_model_load) + " Classifier not initialized.", Toast.LENGTH_LONG).show();
            textViewPrediction.setText(getString(R.string.error_model_load) + " Classifier not initialized.");
            return;
        }

        // Create a TensorImage object from the selected bitmap
        TensorImage image = TensorImage.fromBitmap(selectedBitmap);

        // Run inference
        try {
            List<Classifications> results = imageClassifier.classify(image);

            if (results != null && !results.isEmpty() && results.get(0).getCategories() != null && !results.get(0).getCategories().isEmpty()) {
                StringBuilder resultText = new StringBuilder();
                for (Classifications classification : results) {
                    for (Category category : classification.getCategories()) {
                        resultText.append(String.format(Locale.getDefault(), "%s: %.2f%%\n",
                                category.getLabel(), category.getScore() * 100));
                    }
                }
                textViewPrediction.setText(resultText.toString());
            } else {
                textViewPrediction.setText("No results found.");
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Error during classification: " + e.getMessage(), e);
            Toast.makeText(getContext(), getString(R.string.error_classification), Toast.LENGTH_SHORT).show();
            textViewPrediction.setText(getString(R.string.error_classification));
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Close the classifier when the view is destroyed
        if (imageClassifier != null) {
            imageClassifier.close();
        }
    }
}