package uz.kidzone.app.kidzo;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import uz.kidzone.app.R;
import java.util.List;

public class KidzoCardAdapter extends RecyclerView.Adapter<KidzoCardAdapter.VH> {

    public interface OnCardClick { void onClick(String contentId); }

    static final int[] CARD_COLORS = {
        0xFFFF6B35,  // orange-red
        0xFF4ECDC4,  // teal
        0xFFA78BFA,  // purple
        0xFFFFD93D,  // yellow
        0xFF6BCB77,  // green
        0xFF4D96FF   // blue
    };

    private final List<ContentCard> cards;
    private final OnCardClick onCardClick;

    public KidzoCardAdapter(List<ContentCard> cards, OnCardClick onCardClick) {
        this.cards = cards;
        this.onCardClick = onCardClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kidzo_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContentCard card = cards.get(position);
        holder.tvEmoji.setText(card.emoji);
        holder.tvTitle.setText(card.displayText);
        holder.tvType.setText(card.type);

        // Set background colour from palette
        int color = CARD_COLORS[position % CARD_COLORS.length];
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dpToPx(holder.itemView.getContext(), 16));
        holder.cardContainer.setBackground(bg);

        // Dynamic accessibility description (overrides android:contentDescription="@null" from XML)
        holder.cardContainer.setContentDescription(card.displayText + " " + card.type);

        holder.itemView.setOnClickListener(v -> onCardClick.onClick(card.contentId));
    }

    @Override
    public int getItemCount() { return cards.size(); }

    private static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle, tvType;
        View cardContainer;

        VH(@NonNull View v) {
            super(v);
            tvEmoji       = v.findViewById(R.id.tv_card_emoji);
            tvTitle       = v.findViewById(R.id.tv_card_title);
            tvType        = v.findViewById(R.id.tv_card_type);
            cardContainer = v.findViewById(R.id.card_container);
        }
    }
}
