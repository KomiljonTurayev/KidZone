package uz.kidzone.app.kidzo;

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
        holder.tvTitle.setText(card.displayText);
        holder.tvEmoji.setText("🐥"); // Task 11: replaced with ContentFilter lookup
        holder.btnPlay.setOnClickListener(v -> onCardClick.onClick(card.contentId));
        holder.itemView.setOnClickListener(v -> onCardClick.onClick(card.contentId));
    }

    @Override
    public int getItemCount() { return cards.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle;
        View btnPlay;
        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tv_card_emoji);
            tvTitle = v.findViewById(R.id.tv_card_title);
            btnPlay = v.findViewById(R.id.btn_card_play);
        }
    }
}
