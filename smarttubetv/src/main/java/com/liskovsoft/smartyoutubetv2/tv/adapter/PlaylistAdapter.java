package com.liskovsoft.smartyoutubetv2.tv.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;
import java.util.Locale;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    public enum Mode {
        PLAYLIST,
        FAVORITES
    }

    public interface Listener {
        void onPlay(Video video, int position);
        void onMoveToTop(Video video, int position);
        void onDeleteFromPlaylist(Video video, int position);
        void onToggleFavorite(Video video, int position);

        void onAddToPlaylist(Video video, int position);
        void onDeleteFromFavorites(Video video, int position);

        boolean isFavorite(Video video);
        String getVideoKey(Video video);
    }

    private final Context context;
    private final List<Video> data;
    private final Listener listener;

    private String currentPlayingKey = "";
    private Mode mode = Mode.PLAYLIST;

    public PlaylistAdapter(Context context, List<Video> data, Listener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.PLAYLIST;
        notifyDataSetChanged();
    }

    public Mode getMode() {
        return mode;
    }

    public void updateData(List<Video> newData) {
        data.clear();

        if (newData != null) {
            data.addAll(newData);
        }

        notifyDataSetChanged();
    }

    public void setCurrentPlayingKey(String key) {
        currentPlayingKey = key != null ? key : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_playlist_song, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Video video = data.get(position);

        holder.tvIndex.setText(String.format(Locale.getDefault(), "%02d", position + 1));
        holder.tvTitle.setText(getSafeTitle(video));
        holder.tvSubtitle.setText(getSafeSubtitle(video));
        holder.tvDuration.setText(formatDuration(video));

        String key = listener != null ? listener.getVideoKey(video) : "";
        boolean isPlaying = !TextUtils.isEmpty(key) && key.equals(currentPlayingKey);
        holder.itemView.setSelected(isPlaying);

        loadCover(holder.ivCover, video);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onPlay(data.get(adapterPosition), adapterPosition);
        });

        if (mode == Mode.PLAYLIST) {
            bindPlaylistButtons(holder, position, video);
        } else {
            bindFavoritesButtons(holder);
        }

        setupFocusable(holder);
    }

    private void bindPlaylistButtons(ViewHolder holder, int position, Video video) {
        boolean isFavorite = listener != null && listener.isFavorite(video);

        holder.btnPinTop.setImageResource(R.drawable.ic_arrow_upward);
        holder.btnDelete.setImageResource(R.drawable.ic_delete);
        holder.btnFavorite.setImageResource(
                isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
        );

        holder.btnPinTop.setVisibility(View.VISIBLE);
        holder.btnPinTop.setEnabled(true);
        holder.btnPinTop.setAlpha(position == 0 && mode == Mode.PLAYLIST ? 0.45f : 1.0f);

        holder.btnPinTop.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            if (mode == Mode.PLAYLIST && adapterPosition == 0) {
                return;
            }

            if (mode == Mode.PLAYLIST) {
                listener.onMoveToTop(data.get(adapterPosition), adapterPosition);
            } else {
                listener.onPlay(data.get(adapterPosition), adapterPosition);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onDeleteFromPlaylist(data.get(adapterPosition), adapterPosition);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onToggleFavorite(data.get(adapterPosition), adapterPosition);
        });
    }

    private void bindFavoritesButtons(ViewHolder holder) {
        // Favorites: 立即播放 / 点歌 / 删除
        holder.btnPinTop.setVisibility(View.VISIBLE);
        holder.btnPinTop.setEnabled(true);

        holder.btnPinTop.setImageResource(R.drawable.ic_microphone);
        holder.btnDelete.setImageResource(R.drawable.ic_add);
        holder.btnFavorite.setImageResource(R.drawable.ic_delete);

        holder.btnPinTop.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onPlay(data.get(adapterPosition), adapterPosition);
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onAddToPlaylist(data.get(adapterPosition), adapterPosition);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            int adapterPosition = getSafeAdapterPosition(holder);
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) {
                return;
            }

            listener.onDeleteFromFavorites(data.get(adapterPosition), adapterPosition);
        });
    }

    private void setupFocusable(ViewHolder holder) {
        // 整行不拿遥控器焦点，只保留触摸点击
        holder.itemView.setFocusable(false);
        holder.itemView.setFocusableInTouchMode(false);
        holder.itemView.setClickable(true);

        if (holder.itemView instanceof ViewGroup) {
            ((ViewGroup) holder.itemView).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }

        holder.btnPinTop.setFocusable(true);
        holder.btnPinTop.setFocusableInTouchMode(false);
        holder.btnPinTop.setClickable(true);

        holder.btnDelete.setFocusable(true);
        holder.btnDelete.setFocusableInTouchMode(false);
        holder.btnDelete.setClickable(true);

        holder.btnFavorite.setFocusable(true);
        holder.btnFavorite.setFocusableInTouchMode(false);
        holder.btnFavorite.setClickable(true);

        setupActionButtonKeyNavigation(holder);
    }
    private void setupActionButtonKeyNavigation(ViewHolder holder) {
        holder.btnPinTop.setOnKeyListener((v, keyCode, event) ->
                handleActionButtonKey(holder, v, R.id.btn_pin_top, keyCode, event));

        holder.btnDelete.setOnKeyListener((v, keyCode, event) ->
                handleActionButtonKey(holder, v, R.id.btn_delete, keyCode, event));

        holder.btnFavorite.setOnKeyListener((v, keyCode, event) ->
                handleActionButtonKey(holder, v, R.id.btn_favorite, keyCode, event));
    }
    private boolean handleActionButtonKey(
            ViewHolder holder,
            View currentView,
            int currentButtonId,
            int keyCode,
            KeyEvent event
    ) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                currentView.performClick();
                return true;
            }

            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        int adapterPosition = getSafeAdapterPosition(holder);
        if (adapterPosition == RecyclerView.NO_POSITION) {
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (currentButtonId == R.id.btn_delete) {
                    return requestButtonFocus(holder, R.id.btn_pin_top);
                }

                if (currentButtonId == R.id.btn_favorite) {
                    return requestButtonFocus(holder, R.id.btn_delete);
                }

                return false;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (currentButtonId == R.id.btn_pin_top) {
                    return requestButtonFocus(holder, R.id.btn_delete);
                }

                if (currentButtonId == R.id.btn_delete) {
                    return requestButtonFocus(holder, R.id.btn_favorite);
                }

                return false;

            case KeyEvent.KEYCODE_DPAD_UP:
                return requestButtonFocusInRow(currentView, adapterPosition - 1, currentButtonId);

            case KeyEvent.KEYCODE_DPAD_DOWN:
                return requestButtonFocusInRow(currentView, adapterPosition + 1, currentButtonId);

            default:
                return false;
        }
    }
    private boolean requestButtonFocus(ViewHolder holder, int buttonId) {
        if (holder == null || holder.itemView == null) {
            return false;
        }

        View target = holder.itemView.findViewById(buttonId);

        if (target == null || target.getVisibility() != View.VISIBLE || !target.isFocusable()) {
            return false;
        }

        target.requestFocus();
        return true;
    }

    private boolean requestButtonFocusInRow(View sourceView, int targetPosition, int buttonId) {
        if (targetPosition < 0 || targetPosition >= data.size()) {
            return false;
        }

        RecyclerView recyclerView = findParentRecyclerView(sourceView);
        if (recyclerView == null) {
            return false;
        }

        recyclerView.scrollToPosition(targetPosition);

        recyclerView.postDelayed(() -> {
            RecyclerView.ViewHolder rawHolder =
                    recyclerView.findViewHolderForAdapterPosition(targetPosition);

            if (!(rawHolder instanceof ViewHolder)) {
                return;
            }

            ViewHolder targetHolder = (ViewHolder) rawHolder;
            View target = targetHolder.itemView.findViewById(buttonId);

            if (target != null && target.getVisibility() == View.VISIBLE && target.isFocusable()) {
                target.requestFocus();
            }
        }, 80);

        return true;
    }

    private RecyclerView findParentRecyclerView(View view) {
        if (view == null) {
            return null;
        }

        ViewParent parent = view.getParent();

        while (parent != null) {
            if (parent instanceof RecyclerView) {
                return (RecyclerView) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }
    private int getSafeAdapterPosition(ViewHolder holder) {
        if (holder == null) {
            return RecyclerView.NO_POSITION;
        }

        int adapterPosition = holder.getAdapterPosition();

        if (adapterPosition == RecyclerView.NO_POSITION
                || adapterPosition < 0
                || adapterPosition >= data.size()) {
            return RecyclerView.NO_POSITION;
        }

        return adapterPosition;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvDuration;
        ImageView ivCover;
        ImageButton btnPinTop;
        ImageButton btnDelete;
        ImageButton btnFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvIndex = itemView.findViewById(R.id.tv_index);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            ivCover = itemView.findViewById(R.id.iv_cover);
            btnPinTop = itemView.findViewById(R.id.btn_pin_top);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }

    private void loadCover(ImageView imageView, Video video) {
        if (imageView == null) {
            return;
        }

        String url = video != null ? video.getCardImageUrl() : null;

        if (TextUtils.isEmpty(url)) {
            imageView.setImageDrawable(null);
            return;
        }

        Glide.with(context.getApplicationContext())
                .load(url)
                .apply(ViewUtil.glideOptions())
                .into(imageView);
    }

    private String getSafeTitle(Video video) {
        if (video == null || video.getTitle() == null) {
            return "";
        }

        return video.getTitle();
    }

    private String getSafeSubtitle(Video video) {
        if (video == null) {
            return "";
        }

        if (video.getAuthor() != null) {
            return video.getAuthor();
        }

        if (video.getSecondTitle() != null) {
            return video.getSecondTitle().toString();
        }

        return "";
    }

    private String formatDuration(Video video) {
        if (video == null || video.badge == null) {
            return "";
        }

        return String.valueOf(video.badge);
    }
}