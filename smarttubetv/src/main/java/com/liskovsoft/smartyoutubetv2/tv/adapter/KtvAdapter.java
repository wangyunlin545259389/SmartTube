package com.liskovsoft.smartyoutubetv2.tv.adapter;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

public class KtvAdapter extends RecyclerView.Adapter<KtvAdapter.SongViewHolder> {
    private final Context context;
    private final List<Video> mVideos = new ArrayList<>();
    private final int mThumbQuality;

    private OnItemClickListener clickListener;
    private OnItemMenuListener menuListener;

    public interface OnItemClickListener {
        void onItemClick(Video video, int position);
    }

    public interface OnItemMenuListener {
        void onItemMenu(View anchor, Video video, int position);
    }

    public KtvAdapter(Context context, List<Video> videos) {
        this.context = context;

        if (videos != null) {
            this.mVideos.addAll(videos);
        }

        this.mThumbQuality = getThumbQuality(context);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemMenuListener(OnItemMenuListener listener) {
        this.menuListener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);

        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        view.setClickable(true);
        view.setLongClickable(true);

        // 搜索结果 item 向上回到搜索框，向下到底部分类按钮
        view.setNextFocusUpId(R.id.et_search);
        view.setNextFocusDownId(R.id.btn_karaoke);

        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Video video = mVideos.get(position);

        Log.i("wyltest", safe(video.getTitle()) + "," + safe(video.getCardImageUrl()));

        holder.titleText.setText(safe(video.getTitle()));
//        holder.subtitleText.setText(safe(video.getSecondTitle()));

        loadImage(holder.albumImage, video);

        holder.itemView.setOnFocusChangeListener(null);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setOnKeyListener(null);

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setSelected(hasFocus);

            v.animate()
                    .scaleX(hasFocus ? 1.06f : 1.0f)
                    .scaleY(hasFocus ? 1.06f : 1.0f)
                    .setDuration(120)
                    .start();

            RecyclerView recyclerView = findParentRecyclerView(v);
            int adapterPosition = holder.getAdapterPosition();

            if (hasFocus && recyclerView != null && adapterPosition != RecyclerView.NO_POSITION) {
                recyclerView.smoothScrollToPosition(adapterPosition);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            Video clickedVideo = mVideos.get(adapterPosition);
            if (clickListener != null) {
                clickListener.onItemClick(clickedVideo, adapterPosition);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return false;

            if (menuListener != null) {
                menuListener.onItemMenu(v, mVideos.get(adapterPosition), adapterPosition);
                return true;
            }

            return false;
        });

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            boolean isCenter =
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;

            boolean isMenu = keyCode == KeyEvent.KEYCODE_MENU;

            if (isCenter) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
            }

            if (isMenu) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && menuListener != null) {
                        menuListener.onItemMenu(v, mVideos.get(adapterPosition), adapterPosition);
                    }
                }
                return true;
            }

            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }

            RecyclerView recyclerView = findParentRecyclerView(v);
            if (recyclerView == null || !(recyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                return false;
            }

            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            int spanCount = layoutManager.getSpanCount();
            int totalCount = getItemCount();

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: {
                    int target = adapterPosition - spanCount;
                    if (target >= 0) {
                        moveFocusTo(recyclerView, target);
                        return true;
                    }
                    return false;
                }

                case KeyEvent.KEYCODE_DPAD_DOWN: {
                    int target = adapterPosition + spanCount;
                    if (target < totalCount) {
                        moveFocusTo(recyclerView, target);
                        return true;
                    }
                    return false;
                }

                case KeyEvent.KEYCODE_DPAD_LEFT: {
                    boolean isFirstColumn = adapterPosition % spanCount == 0;
                    if (!isFirstColumn) {
                        moveFocusTo(recyclerView, adapterPosition - 1);
                        return true;
                    }
                    return false;
                }

                case KeyEvent.KEYCODE_DPAD_RIGHT: {
                    boolean isLastColumn = (adapterPosition + 1) % spanCount == 0;
                    if (!isLastColumn && adapterPosition + 1 < totalCount) {
                        moveFocusTo(recyclerView, adapterPosition + 1);
                        return true;
                    }
                    return false;
                }

                default:
                    return false;
            }
        });
    }

    private void moveFocusTo(RecyclerView recyclerView, int targetPosition) {
        recyclerView.scrollToPosition(targetPosition);

        recyclerView.post(() -> {
            RecyclerView.ViewHolder targetHolder =
                    recyclerView.findViewHolderForAdapterPosition(targetPosition);

            if (targetHolder != null) {
                targetHolder.itemView.requestFocus();
            }
        });
    }

    private RecyclerView findParentRecyclerView(View view) {
        View parent = (View) view.getParent();

        while (parent != null) {
            if (parent instanceof RecyclerView) {
                return (RecyclerView) parent;
            }

            if (!(parent.getParent() instanceof View)) {
                return null;
            }

            parent = (View) parent.getParent();
        }

        return null;
    }

    private void loadImage(ImageView imageView, Video video) {
        if (imageView == null || video == null) {
            return;
        }

        String imageUrl = ClickbaitRemover.updateThumbnail(video, mThumbQuality);
//        float radiusPx = TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics()
//        );
        Glide.with(context)
                .load(imageUrl)
//                .transform(new TopRoundedCornersTransform(radiusPx))
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    private int getThumbQuality(Context context) {
        try {
            com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData mainUIData =
                    com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.instance(context);

            return mainUIData.getThumbQuality();
        } catch (Exception e) {
            return 1;
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public int getItemCount() {
        return mVideos.size();
    }

    public void updateData(List<Video> newVideos) {
        mVideos.clear();

        if (newVideos != null) {
            mVideos.addAll(newVideos);
        }

        notifyDataSetChanged();
    }

    public void addVideos(List<Video> videos) {
        if (videos == null || videos.isEmpty()) return;

        int startPosition = mVideos.size();
        mVideos.addAll(videos);

        notifyItemRangeInserted(startPosition, videos.size());
    }

    public void clearVideos() {
        int size = mVideos.size();
        if (size == 0) return;

        mVideos.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<Video> getVideos() {
        return mVideos;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage;
        TextView titleText;
//        TextView subtitleText;

        SongViewHolder(View itemView) {
            super(itemView);

            albumImage = itemView.findViewById(R.id.album_image);
            titleText = itemView.findViewById(R.id.song_title);
//            subtitleText = itemView.findViewById(R.id.song_subtitle);
        }
    }
}