package uz.kidzone.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BirdAiManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "BirdAiManager";
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    
    private final Context context;
    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final TextToSpeech tts;
    private boolean isTtsReady = false;

    public BirdAiManager(Context context) {
        this.context = context;
        this.tts = new TextToSpeech(context, this);
        
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("uz", "UZ"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
            isTtsReady = true;
        }
    }

    public void showBirdDialog() {
        android.util.Log.d(TAG, "showBirdDialog: Tapped");
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_story);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvContent = dialog.findViewById(R.id.tvStoryContent);
        ProgressBar pb = dialog.findViewById(R.id.pbThinking);
        ImageView imgBird = dialog.findViewById(R.id.imgBirdIcon);
        Button btnClose = dialog.findViewById(R.id.btnCloseStory);
        Button btnMusic = dialog.findViewById(R.id.btnPlayMusic);

        // Animation for the bird
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(imgBird, "scaleX", 1f, 1.2f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(imgBird, "scaleY", 1f, 1.2f);
        scaleX.setDuration(500);
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleY.setDuration(500);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleX.start();
        scaleY.start();

        btnClose.setOnClickListener(v -> {
            stopSpeaking();
            dialog.dismiss();
        });
        
        btnMusic.setOnClickListener(v -> {
            MusicManager.getInstance().setMuted(false);
            MusicManager.getInstance().startMusic(context);
            Toast.makeText(context, context.getString(R.string.btn_play_music), Toast.LENGTH_SHORT).show();
        });

        dialog.show();
        generateStory(tvContent, pb);
    }

    private void generateStory(TextView tvContent, ProgressBar pb) {
        android.util.Log.d(TAG, "generateStory: Starting");
        pb.setVisibility(View.VISIBLE);
        tvContent.setText(R.string.ai_thinking);

        Content content = new Content.Builder()
            .addText("Sen bolalar uchun juda mehribon ertakchi qushchasan. O'zbek tilida 4-5 gapdan iborat qisqa, qiziqarli va tarbiyaviy ertak aytib ber. Ertak oxirida bolalarga bitta foydali maslahat ber.")
            .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        
        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String story = result.getText();
                android.util.Log.d(TAG, "onSuccess: Story generated");
                new Handler(Looper.getMainLooper()).post(() -> {
                    pb.setVisibility(View.GONE);
                    tvContent.setText(story);
                    speak(story);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                android.util.Log.e(TAG, "onFailure: AI Error", t);
                new Handler(Looper.getMainLooper()).post(() -> {
                    pb.setVisibility(View.GONE);
                    tvContent.setText(R.string.ai_error);
                });
            }
        }, executor);
    }

    private void speak(String text) {
        if (isTtsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "StoryID");
        }
    }

    private void stopSpeaking() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
