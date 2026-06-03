package uz.kidzone.app.kidzo;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import uz.kidzone.app.BuildConfig;

public class RealGeminiCaller implements GeminiCaller {

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public RealGeminiCaller() {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    @Override
    public void call(String prompt, OnSuccess onSuccess, OnError onError) {
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = model.generateContent(content);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                onSuccess.accept(text != null ? text : "");
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                onError.accept(t.getMessage() != null ? t.getMessage() : "Xato yuz berdi");
            }
        }, executor);
    }
}
