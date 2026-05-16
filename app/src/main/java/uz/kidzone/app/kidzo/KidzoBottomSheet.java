package uz.kidzone.app.kidzo;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import uz.kidzone.app.R;
import java.util.List;

public class KidzoBottomSheet extends BottomSheetDialogFragment
        implements KidzoStateListener {

    private final KidzoAgent agent;

    private LinearLayout layoutThinking;
    private LinearLayout layoutError;
    private View sectionCarousel;
    private View sectionRecommendationsFooter;
    private View sectionChat;
    private RecyclerView rvCards;
    private LinearLayout chatMessages;
    private ScrollView scrollChat;
    private EditText etChatInput;
    private TextView tvErrorMessage;

    public KidzoBottomSheet(KidzoAgent agent) {
        this.agent = agent;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_kidzo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutThinking               = view.findViewById(R.id.layout_thinking);
        layoutError                  = view.findViewById(R.id.layout_error);
        sectionCarousel              = view.findViewById(R.id.section_carousel);
        sectionRecommendationsFooter = view.findViewById(R.id.section_recommendations_footer);
        sectionChat                  = view.findViewById(R.id.section_chat);
        rvCards                      = view.findViewById(R.id.rv_kidzo_cards);
        chatMessages                 = view.findViewById(R.id.chat_messages);
        scrollChat                   = view.findViewById(R.id.scroll_chat);
        etChatInput                  = view.findViewById(R.id.et_chat_input);
        tvErrorMessage               = view.findViewById(R.id.tv_error_message);

        rvCards.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        Button btnKidzoClose = view.findViewById(R.id.btn_kidzo_close);
        btnKidzoClose.setOnClickListener(v -> agent.dismiss());

        view.findViewById(R.id.btn_start_chat).setOnClickListener(v -> agent.startChat());

        view.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addChatBubble(msg, true);
                etChatInput.setText("");
                agent.sendChatMessage(msg);
            }
        });

        view.findViewById(R.id.btn_retry).setOnClickListener(v -> agent.requestRecommendations());
        view.findViewById(R.id.btn_error_close).setOnClickListener(v -> dismiss());

        agent.setListener(this);
        onStateChanged(agent.getCurrentState(), null);
    }

    @Override
    public void onDestroyView() {
        agent.setListener(null);
        super.onDestroyView();
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        agent.dismiss();
        super.onCancel(dialog);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStateChanged(KidzoState newState, Object payload) {
        if (getView() == null) return;
        requireActivity().runOnUiThread(() -> {
            layoutThinking.setVisibility(View.GONE);
            sectionCarousel.setVisibility(View.GONE);
            sectionRecommendationsFooter.setVisibility(View.GONE);
            sectionChat.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);

            switch (newState) {
                case THINKING:
                    layoutThinking.setVisibility(View.VISIBLE);
                    break;

                case RECOMMENDATIONS:
                    sectionCarousel.setVisibility(View.VISIBLE);
                    sectionRecommendationsFooter.setVisibility(View.VISIBLE);
                    if (payload instanceof List) {
                        List<ContentCard> cards = (List<ContentCard>) payload;
                        rvCards.setAdapter(new KidzoCardAdapter(cards, contentId -> {
                            agent.openContent(contentId);
                            dismiss();
                        }));
                    }
                    break;

                case CHATTING:
                    sectionCarousel.setVisibility(View.VISIBLE);
                    sectionChat.setVisibility(View.VISIBLE);
                    if (payload instanceof String) {
                        addChatBubble((String) payload, false);
                    }
                    break;

                case ERROR:
                    layoutError.setVisibility(View.VISIBLE);
                    if (payload instanceof String) {
                        tvErrorMessage.setText((String) payload);
                    }
                    break;

                case IDLE:
                    dismiss();
                    break;
            }
        });
    }

    @Override
    public void onActionRequested(String contentId) {
        // Handled by MainActivity — future task
    }

    private void addChatBubble(String text, boolean isUser) {
        TextView bubble = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int dp12 = dp(12);
        int dp4  = dp(4);
        int dp48 = dp(48);
        int dp16 = dp(16);
        params.topMargin    = dp12;
        params.bottomMargin = dp4;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp16);

        if (isUser) {
            params.gravity    = Gravity.END;
            params.leftMargin = dp48;
            bg.setColor(0xFFFF6B35);
            bubble.setTextColor(android.graphics.Color.WHITE);
            bubble.setText(text);
        } else {
            params.gravity     = Gravity.START;
            params.rightMargin = dp48;
            bg.setColor(android.graphics.Color.WHITE);
            bg.setStroke(dp(2), 0xFFFF6B35);
            bubble.setTextColor(0xFF333333);
            bubble.setText("🐦 " + text);
        }

        bubble.setBackground(bg);
        bubble.setLayoutParams(params);
        bubble.setPadding(dp12, dp(8), dp12, dp(8));
        bubble.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);
        chatMessages.addView(bubble);
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
