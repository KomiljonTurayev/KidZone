package uz.kidzone.app.kidzo;

public interface GeminiCaller {
    void call(String prompt, OnSuccess onSuccess, OnError onError);

    interface OnSuccess { void accept(String text); }
    interface OnError   { void accept(String message); }
}
