package com.example.mobile_java.ui.home;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp; // Opcional, depende de tu modelo
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";
    private static final String MODEL_FILE_NAME = "model_unquant.tflite";
    private static final String LABELS_FILE_NAME = "labels.txt"; // Asegúrate que este archivo exista en assets

    // Parámetros del modelo (DEBES AJUSTARLOS SI SON DIFERENTES)
    // Los modelos de Teachable Machine suelen ser 224x224
    private static final int INPUT_IMAGE_WIDTH = 224;
    private static final int INPUT_IMAGE_HEIGHT = 224;
    // Normalización para modelos Float32 de Teachable Machine (rango 0-1)
    private static final float NORMALIZE_MEAN = 0.0f;
    private static final float NORMALIZE_STDDEV = 255.0f;
    // Si tu modelo espera -1 a 1, usa:
    // private static final float NORMALIZE_MEAN = 127.5f;
    // private static final float NORMALIZE_STDDEV = 127.5f;

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Bitmap> mSelectedImage;
    private final MutableLiveData<String> mClassificationResult;

    private Interpreter tfliteInterpreter;
    private List<String> labels;
    private ImageProcessor imageProcessor;
    private boolean isInterpreterInitialized = false;

    private int inputImageWidth;
    private int inputImageHeight;
    private int modelOutputClasses;


    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Selecciona una imagen para clasificar");
        mSelectedImage = new MutableLiveData<>();
        mClassificationResult = new MutableLiveData<>();
    }

    public void initializeInterpreter(Context context) {
        if (isInterpreterInitialized) {
            return;
        }
        try {
            // Cargar el modelo
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_FILE_NAME);
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            // Puedes añadir opciones como el número de hilos si es necesario
            // tfliteOptions.setNumThreads(4);
            tfliteInterpreter = new Interpreter(tfliteModel, tfliteOptions);

            // Cargar las etiquetas
            labels = FileUtil.loadLabels(context, LABELS_FILE_NAME);
            Log.d(TAG, "Labels loaded: " + labels.size());

            // Obtener dimensiones de entrada y salida del modelo (importante)
            int[] inputShape = tfliteInterpreter.getInputTensor(0).shape(); // Asume 1 tensor de entrada
            inputImageWidth = inputShape[1]; // Usualmente [batch, height, width, channels]
            inputImageHeight = inputShape[2];
            Log.d(TAG, "Model Input Shape: Width=" + inputImageWidth + ", Height=" + inputImageHeight);


            int[] outputShape = tfliteInterpreter.getOutputTensor(0).shape(); // Asume 1 tensor de salida
            modelOutputClasses = outputShape[1]; // Usualmente [batch, num_classes]
            Log.d(TAG, "Model Output Classes: " + modelOutputClasses);


            if (labels.size() != modelOutputClasses) {
                Log.e(TAG, "Mismatch between labels count (" + labels.size() + ") and model output classes (" + modelOutputClasses + ")");
                mClassificationResult.postValue("Error: El número de etiquetas no coincide con las salidas del modelo.");
                isInterpreterInitialized = false;
                return;
            }


            // Crear el procesador de imágenes
            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                    // .add(new ResizeWithCropOrPadOp(inputImageHeight, inputImageWidth)) // Alternativa si tu modelo lo requiere
                    .add(new NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STDDEV)) // ¡LA NORMALIZACIÓN CLAVE!
                    .build();

            isInterpreterInitialized = true;
            Log.d(TAG, "TensorFlow Lite Interpreter initialized.");

        } catch (IOException e) {
            Log.e(TAG, "Error initializing TensorFlow Lite Interpreter.", e);
            mClassificationResult.postValue("Error al inicializar el intérprete: " + e.getMessage());
            isInterpreterInitialized = false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error during interpreter initialization (shape/type mismatch?): " + e.getMessage(), e);
            mClassificationResult.postValue("Error de configuración del intérprete: " + e.getMessage());
            isInterpreterInitialized = false;
        }
    }

    public void classifyImage(Bitmap bitmap) {
        if (!isInterpreterInitialized || tfliteInterpreter == null || imageProcessor == null || labels == null) {
            mClassificationResult.postValue("Intérprete no inicializado.");
            Log.e(TAG, "Interpreter not initialized before calling classifyImage");
            return;
        }
        if (bitmap == null) {
            mClassificationResult.postValue("No hay imagen para clasificar.");
            return;
        }

        mSelectedImage.postValue(bitmap);

        try {
            // 1. Preprocesar la imagen
            TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // 2. Preparar el buffer de salida
            // El modelo de Teachable Machine devuelve un array de floats con las probabilidades para cada clase
            // La forma será [1, num_classes]
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, modelOutputClasses}, org.tensorflow.lite.DataType.FLOAT32);

            // 3. Ejecutar la inferencia
            tfliteInterpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer().rewind());

            // 4. Obtener los resultados
            float[]
                    probabilities = outputBuffer.getFloatArray();

            // 5. Mapear probabilidades a etiquetas y mostrar las mejores
            // (Este código es un ejemplo para obtener las N mejores predicciones)
            final int MAX_RESULTS = 3;
            PriorityQueue<Category> pq =
                    new PriorityQueue<>(
                            MAX_RESULTS,
                            (lhs, rhs) -> Float.compare(rhs.getScore(), lhs.getScore())); // Orden descendente

            for (int i = 0; i < labels.size(); i++) {
                pq.add(new Category(labels.get(i), probabilities[i]));
            }

            StringBuilder resultText = new StringBuilder();
            int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
            for (int i = 0; i < recognitionsSize; i++) {
                Category recognition = pq.poll();
                if (recognition != null) {
                    resultText.append(String.format(Locale.getDefault(), "%s: %.2f%%\n",
                            recognition.getLabel(), recognition.getScore() * 100));
                }
            }
            mClassificationResult.postValue(resultText.toString().trim());

        } catch (Exception e) {
            Log.e(TAG, "Error during image classification with Interpreter", e);
            mClassificationResult.postValue("Error al clasificar: " + e.getMessage());
        }
    }

    public LiveData<String> getText() { return mText; }
    public LiveData<Bitmap> getSelectedImage() { return mSelectedImage; }
    public LiveData<String> getClassificationResult() { return mClassificationResult; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
            tfliteInterpreter = null;
            Log.d(TAG, "TFLite Interpreter closed.");
        }
    }
}