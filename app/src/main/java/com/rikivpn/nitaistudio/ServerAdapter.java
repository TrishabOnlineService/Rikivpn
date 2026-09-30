package com.rikivpn.nitaistudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Server list adapter. There is no subscription/lock concept anymore - every server is free.
 * Premium-tier servers just show a small "watch ads to connect" icon as a hint that tapping
 * them will run the 5-ad unlock sequence (see SelectLocationActivity) before selecting them.
 */
public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {

    public interface OnServerClickListener {
        void onServerClicked(VpnServer server);
    }

    private final List<VpnServer> servers = new ArrayList<>();
    private final OnServerClickListener listener;
    private String selectedKey;

    public ServerAdapter(OnServerClickListener listener, String selectedKey) {
        this.listener = listener;
        this.selectedKey = selectedKey;
    }

    public void submitList(List<VpnServer> newServers) {
        servers.clear();
        servers.addAll(newServers);
        notifyDataSetChanged();
    }

    public void setSelectedKey(String key) {
        this.selectedKey = key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server_row, parent, false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        VpnServer server = servers.get(position);
        holder.tvFlag.setText(server.flagEmoji);
        holder.tvCountry.setText(server.countryName);
        // Premium servers are free to use, but tapping one runs a 5-ad unlock sequence first.
        holder.ivLock.setVisibility(server.premium ? View.VISIBLE : View.GONE);
        holder.itemView.setSelected(server.key().equals(selectedKey));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onServerClicked(server);
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    static class ServerViewHolder extends RecyclerView.ViewHolder {
        TextView tvFlag;
        TextView tvCountry;
        ImageView ivLock;

        ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFlag = itemView.findViewById(R.id.tvFlag);
            tvCountry = itemView.findViewById(R.id.tvCountry);
            ivLock = itemView.findViewById(R.id.ivLock);
        }
    }
}
