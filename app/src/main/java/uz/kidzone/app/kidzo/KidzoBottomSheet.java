package uz.kidzone.app.kidzo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private LinearLayout layoutRecommendations;
    private LinearLayout layoutChat;
    private LinearLayout layoutError;
    private RecyclerView rvCards;
    private LinearLayout chatMessages;
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

        layoutThinking        = view.findViewById(R.id.layout_thinking);
        layoutRecommendations = view.findViewById(R.id.layout_recommendations);
        layoutChat            = view.findViewById(R.id.layout_chat);
        layoutError           = view.findViewById(R.id.layout_error);
        rvCards               = view.findViewById(R.id.rv_kidzo_cards);
        chatMessages          = view.findViewById(R.id.chat_messages);
        etChatInput           = view.findViewById(R.id.et_chat_input);
        tvErrorMessage        = view.findViewById(R.id.tv_error_message);

        rvCards.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.btn_start_chat).setOnClickListener(v -> agent.startChat());

        view.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addChatBubble(msg, true);
                etChatInput.setText("");
                agent.sendChatMessage(msg);
            }
        });

        view.findViewById(R.id.btn_retry).setOnClickListener(v ->
            agent.requestRecommendations()
        );

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
            layoutRecommendations.setVisibility(View.GONE);
            layoutChat.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);

            switch (newState) {
                case THINKING:
                    layoutThinking.setVisibility(View.VISIBLE);
                    break;

                case RECOMMENDATIONS:
                    layoutRecommendations.setVisibility(View.VISIBLE);
                    if (payload instanceof List) {
                        List<ContentCard> cards = (List<ContentCard>) payload;
                        rvCards.setAdapter(new KidzoCardAdapter(cards, contentId -> {
                            agent.openContent(contentId);
                            dismiss();
                        }));
                    }
                    break;

                case CHATTING:
                    layoutChat.setVisibility(View.VISIBLE);
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
        // Handled by MainActivity listener
    }

    private void addChatBubble(String text, boolean isUser) {
        TextView bubble = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 8;
        if (isUser) {
            params.gravity = android.view.Gravity.END;
            bubble.setBackgroundResource(android.R.color.holo_blue_light);
        } else {
            params.gravity = android.view.Gravity.START;
            bubble.setBackgroundResource(android.R.color.holo_green_light);
        }
        bubble.setLayoutParams(params);
        bubble.setPadding(16, 10, 16, 10);
        bubble.setText(text);
        bubble.setTextSize(15f);
        bubble.setTextColor(android.graphics.Color.BLACK);
        chatMessages.addView(bubble);
    }
}
