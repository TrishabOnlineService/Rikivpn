package com.rikivpn.nitaistudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    public static class Page {
        final int iconRes;
        final String title;
        final String desc;

        public Page(int iconRes, String title, String desc) {
            this.iconRes = iconRes;
            this.title = title;
            this.desc = desc;
        }
    }

    private final List<Page> pages;

    public OnboardingAdapter(List<Page> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        Page page = pages.get(position);
        holder.ivIcon.setImageResource(page.iconRes);
        holder.tvTitle.setText(page.title);
        holder.tvDesc.setText(page.desc);

        // Gentle entrance animation each time a page is bound/scrolled to.
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(350).start();
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvDesc;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }
    }
}
