package com.example.mobile_final.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        // Updated welcome message from strings.xml
        // This will be set in HomeFragment using R.string.welcome_home_screen_message
        // mText.setValue("Welcome! Select an image and run prediction.");
    }

    public LiveData<String> getText() {
        return mText;
    }

    // Setter for the text, so Fragment can set it from R.string
    public void setText(String text) {
        mText.setValue(text);
    }
}