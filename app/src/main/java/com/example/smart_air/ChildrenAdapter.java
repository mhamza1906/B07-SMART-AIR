package com.example.smart_air;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

class ChildrenAdapter extends RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder> {

    private final List<String> childIds;
    private final List<String> usernames;
    private final Context context;

    ChildrenAdapter(Context context, List<String> ids, List<String> names) {
        this.context = context;
        this.childIds = ids;
        this.usernames = names;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.children_item, parent, false);
        return new ChildViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {

        String childId = childIds.get(position);
        String childName = usernames.get(position);

        holder.txtChildName.setText(childName);

        // 清空旧 badges
        holder.layoutBadges.removeAllViews();

        FirebaseFirestore.getInstance()
                .collection("badges")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Boolean budding = doc.getBoolean("allBadges.budding_star");
                    Boolean shining = doc.getBoolean("allBadges.shining_star");
                    Boolean lucky = doc.getBoolean("allBadges.lucky_star");

                    if (Boolean.TRUE.equals(budding))
                        addBadge(holder.layoutBadges, R.drawable.budding_star);
                    if (Boolean.TRUE.equals(shining))
                        addBadge(holder.layoutBadges, R.drawable.shining_star);
                    if (Boolean.TRUE.equals(lucky))
                        addBadge(holder.layoutBadges, R.drawable.lucky_star);
                });

        holder.itemView.setOnClickListener(v ->
                ((ParentDashboardActivity) context).showChildOptions(childId)
        );
    }

    @Override
    public int getItemCount() {
        return childIds.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView txtChildName;
        LinearLayout layoutBadges;
        TextView txtArrow;

        ChildViewHolder(View v) {
            super(v);
            txtChildName = v.findViewById(R.id.txtChildName);
            layoutBadges = v.findViewById(R.id.layoutBadges);
            txtArrow = v.findViewById(R.id.txtArrow);
        }
    }

    private void addBadge(LinearLayout layout, int drawableId) {
        ImageView img = new ImageView(layout.getContext());
        img.setImageResource(drawableId);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(48, 48);
        params.setMarginEnd(8);

        img.setLayoutParams(params);
        layout.addView(img);
    }
}
