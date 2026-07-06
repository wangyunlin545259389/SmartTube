package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.io.InputStream;
import java.util.List;

/**
 * https://chatgpt.com/c/6806b729-1ab0-8010-94f0-56f6b71cdbfb
 */
public class EmbedPlayerView extends PlayerView implements PlaybackView {
    private static final String TAG = EmbedPlayerView.class.getSimpleName();
    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_NORMAL = 1;
    public static final int QUALITY_HIGH = 2;
    private SimpleExoPlayer mPlayer;
    private ExoPlayerInitializer mPlayerInitializer;
    private ExoPlayerController mExoPlayerController;
    private PlaybackPresenter mPlaybackPresenter;
    private Video mVideo;
    private boolean mIsMute;
    private final Runnable mShowView = this::showView;
    private final Runnable mStopPlayback = this::finish;
    private int mQuality;
    private float mPercentWatched;
    /** 下一次打开视频时是否强制从 0 开始播放。 */
    private boolean mForceStartFromBeginning = false;

    /** 下一次打开视频后需要跳转到的位置。-1 表示不处理。 */
    private long mPendingStartPositionMs = -1L;

    /** pending seek 重试次数。 */
    private int mPendingStartSeekAttempts = 0;

    private static final int PENDING_START_SEEK_MAX_ATTEMPTS = 20;

    private final Runnable mApplyPendingStartPositionRunnable = new Runnable() {
        @Override
        public void run() {
            applyPendingStartPosition();
        }
    };

    public EmbedPlayerView(Context context) {
        super(context);
        hideView();
    }

    public EmbedPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        hideView();
    }

    public EmbedPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        hideView();
    }

    private void hideView() {
        setAlpha(0);
    }

    private void showView() {
        setAlpha(1);
    }

    @Override
    public void updateSuggestions(VideoGroup group) {

    }

    @Override
    public void removeSuggestions(VideoGroup group) {

    }

    @Override
    public int getSuggestionsIndex(VideoGroup group) {
        return 0;
    }

    @Override
    public VideoGroup getSuggestionsByIndex(int index) {
        return null;
    }

    @Override
    public void focusSuggestedItem(int index) {

    }

    @Override
    public void focusSuggestedItem(Video video) {

    }

    @Override
    public void resetSuggestedPosition() {

    }

    @Override
    public boolean isSuggestionsEmpty() {
        return false;
    }

    @Override
    public void clearSuggestions() {

    }

    @Override
    public void showOverlay(boolean show) {

    }

    @Override
    public boolean isOverlayShown() {
        return false;
    }

    @Override
    public void showSuggestions(boolean show) {

    }

    @Override
    public boolean isSuggestionsShown() {
        return false;
    }

    @Override
    public void showControls(boolean show) {

    }

    @Override
    public boolean isControlsShown() {
        return false;
    }

    @Override
    public int getButtonState(int buttonId) {
        return -1;
    }

    @Override
    public void setButtonState(int buttonId, int buttonState) {

    }

    @Override
    public void setChannelIcon(String iconUrl) {

    }

    @Override
    public void setSeekPreviewTitle(String title) {

    }

    @Override
    public void setNextTitle(Video nextVideo) {

    }

    @Override
    public void showDebugInfo(boolean show) {

    }

    @Override
    public void showSubtitles(boolean show) {

    }

    @Override
    public void loadStoryboard() {

    }

    @Override
    public void setTitle(String title) {

    }

    @Override
    public void showProgressBar(boolean show) {

    }

    @Override
    public void setSeekBarSegments(List<SeekBarSegment> segments) {

    }

    @Override
    public void updateEndingTime() {

    }

    @Override
    public void setChatReceiver(ChatReceiver chatReceiver) {

    }

    @Override
    public void setVideo(Video item) {
        mVideo = item;

        if (mExoPlayerController != null) {
            mExoPlayerController.setVideo(mVideo);
        }
    }

    @Override
    public Video getVideo() {
        return mVideo;
    }

    @Override
    public void finish() {
        destroyPlayerObjects();
    }

    @Override
    public void finishReally() {
        finish();
    }

    @Override
    public void showBackground(String url) {

    }

    @Override
    public void showBackgroundColor(int colorResId) {

    }

    @Override
    public void resetPlayerState() {

    }

    @Override
    public boolean isEmbed() {
        return true;
    }

    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openSabr(formatInfo);
    }

    @Override
    public void openDash(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openDash(formatInfo);
    }

    @Override
    public void openDash(InputStream dashManifest) {
        mExoPlayerController.openDash(dashManifest);
    }

    @Override
    public void openDashUrl(String dashManifestUrl) {
        mExoPlayerController.openDashUrl(dashManifestUrl);
    }

    @Override
    public void openHlsUrl(String hlsPlaylistUrl) {
        mExoPlayerController.openHlsUrl(hlsPlaylistUrl);
    }

    @Override
    public void openUrlList(List<String> urlList) {
        mExoPlayerController.openUrlList(urlList);
    }

    @Override
    public void openMerged(MediaItemFormatInfo formatInfo, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(formatInfo, hlsPlaylistUrl);
    }

    @Override
    public void openMerged(InputStream dashManifest, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(dashManifest, hlsPlaylistUrl);
    }

    @Override
    public long getPositionMs() {
        return mExoPlayerController.getPositionMs();
    }

    @Override
    public void setPositionMs(long positionMs) {
        mExoPlayerController.setPositionMs(positionMs);
    }

    @Override
    public long getDurationMs() {
        long durationMs = mExoPlayerController.getDurationMs();

        long liveDurationMs = getVideo() != null ? getVideo().getLiveDurationMs() : 0;

        if (durationMs > Video.MAX_LIVE_DURATION_MS && liveDurationMs != 0) {
            durationMs = liveDurationMs;
        }

        return durationMs;
    }

    @Override
    public void setPlayWhenReady(boolean play) {
        mExoPlayerController.setPlayWhenReady(play);
    }

    @Override
    public boolean getPlayWhenReady() {
        return mExoPlayerController.getPlayWhenReady();
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayerController.isPlaying();
    }

    @Override
    public boolean isLoading() {
        return mExoPlayerController.isLoading();
    }

    @Override
    public List<FormatItem> getVideoFormats() {
        return mExoPlayerController.getVideoFormats();
    }

    @Override
    public List<FormatItem> getAudioFormats() {
        return mExoPlayerController.getAudioFormats();
    }

    @Override
    public List<FormatItem> getSubtitleFormats() {
        return mExoPlayerController.getSubtitleFormats();
    }

    @Override
    public void setFormat(FormatItem option) {
        // NOP. Use internal selection
    }

    @Override
    public FormatItem getVideoFormat() {
        return mExoPlayerController.getVideoFormat();
    }

    @Override
    public FormatItem getAudioFormat() {
        return mExoPlayerController.getAudioFormat();
    }

    @Override
    public FormatItem getSubtitleFormat() {
        return mExoPlayerController.getSubtitleFormat();
    }

    @Override
    public boolean isEngineInitialized() {
        return mPlayer != null;
    }

    @Override
    public void restartEngine() {

    }

    @Override
    public void reloadPlayback() {

    }

    @Override
    public void blockEngine(boolean block) {

    }

    @Override
    public boolean isEngineBlocked() {
        return false;
    }

    @Override
    public boolean isInPIPMode() {
        return false;
    }

    @Override
    public boolean containsMedia() {
        if (mExoPlayerController == null) {
            return false;
        }

        return mExoPlayerController.containsMedia();
    }

    @Override
    public void setSpeed(float speed) {
        if (mExoPlayerController != null) {
            mExoPlayerController.setSpeed(speed);
        }
    }

    @Override
    public float getSpeed() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getSpeed();
    }

    @Override
    public void setPitch(float pitch) {
        if (mExoPlayerController != null) {
            mExoPlayerController.setPitch(pitch);
        }
    }

    @Override
    public float getPitch() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getPitch();
    }

    @Override
    public void setVolume(float volume) {
        if (!mIsMute && mExoPlayerController != null) {
            mExoPlayerController.setVolume(volume);
        }
    }

    @Override
    public float getVolume() {
        if (mExoPlayerController == null) {
            return 1f;
        }

        return mExoPlayerController.getVolume();
    }

    @Override
    public void setZoomPercents(int percents) {

    }

    @Override
    public int getResizeMode() {
        return super.getResizeMode();
    }

    @Override
    public void setAspectRatio(float ratio) {

    }

    @Override
    public void setRotationAngle(int angle) {

    }

    @Override
    public void setVideoFlipEnabled(boolean enabled) {

    }

    @Override
    public void setVideoGravity(int gravity) {

    }

    @Override
    protected void finalize() throws Throwable {
        try {
            finish();
        } finally {
            super.finalize();
        }
    }

    public void setQuality(int quality) {
        mQuality = quality;
    }

    public void openVideo(String videoId) {
        openVideo(Video.from(videoId));
    }

    /**
     * 从头播放视频。
     *
     * KTV 场景使用这个方法，避免 SmartTube 按历史播放进度续播。
     */
    public void openVideoFromStart(@Nullable Video video) {
        if (video == null) {
            return;
        }

        mForceStartFromBeginning = true;
        mPendingStartPositionMs = 0L;
        mPendingStartSeekAttempts = 0;

        // 关键：清掉 Video 自带的观看进度，避免 PlaybackPresenter 恢复上次进度。
        video.percentWatched = 0;
        video.pendingPosMs = 0;
        openVideo(video);
    }
    /**
     * 从指定位置播放视频。
     *
     * 用于网络断开后恢复播放。
     */
    public void openVideoAt(@Nullable Video video, long positionMs) {
        if (video == null) {
            return;
        }

        long safePositionMs = Math.max(0L, positionMs);

        mForceStartFromBeginning = false;
        mPendingStartPositionMs = safePositionMs;
        mPendingStartSeekAttempts = 0;

        // PlaybackPresenter / VideoLoaderController 里会读取这个字段。
        video.pendingPosMs = safePositionMs;

        openVideo(video);
    }

    /**
     * 强制重建播放器，并从指定位置恢复播放。
     *
     * 断网后 ExoPlayer 可能已经进入错误状态。
     * 这种情况下只 openVideoAt 不够，必须先 release 再重新打开。
     */
    public void reopenVideoAt(@Nullable Video video, long positionMs) {
        if (video == null) {
            return;
        }

        finish();
        openVideoAt(video, positionMs);
    }
    public void openVideo(@Nullable Video video) {
        if (video == null) {
            return;
        }

        if (mPlaybackPresenter == null) {
            mPlaybackPresenter = getPlaybackPresenter();
        }

        // Fullscreen playback is running. Skipping
        // Fix Android TV 12 playing on Home (top view check)
        PlaybackView view = mPlaybackPresenter.getView();
        if (view == null || view instanceof EmbedPlayerView || (!mPlaybackPresenter.isEngineInitialized() && getViewManager().getTopView() != PlaybackView.class)) {
            initPlayer();
            createPlayerObjects();

            if (mForceStartFromBeginning) {
                video.percentWatched = 0;
            }

            mPlaybackPresenter.onNewVideo(video);
            mPercentWatched = mForceStartFromBeginning ? 0 : video.percentWatched;
        }
    }

    private void initPlayer() {
        if (isEngineInitialized()) {
            mPlaybackPresenter.setView(this);
            return;
        }

        mPlayerInitializer = new ExoPlayerInitializer(getContext());
        mPlaybackPresenter.setView(this);
        mExoPlayerController = new ExoPlayerController(getContext(), mPlaybackPresenter);
        mExoPlayerController.setOnVideoLoaded(this::onVideoLoaded);
        mPlaybackPresenter.onViewInitialized(); // init all controllers

//        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM); // Fix unfilled borders
        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
    }

    private void createPlayerObjects() {
        if (isEngineInitialized()) {
            setPlayer(mPlayer);
            return;
        }

        // Use default or pass your bandwidthMeter here: bandwidthMeter = new DefaultBandwidthMeter.Builder(getContext()).build()
        DefaultTrackSelector trackSelector = new RestoreTrackSelector(new AdaptiveTrackSelection.Factory());
        mExoPlayerController.setTrackSelector(trackSelector);

        DefaultRenderersFactory renderersFactory = new CustomOverridesRenderersFactory(getContext());
        mPlayer = mPlayerInitializer.createPlayer(getContext(), renderersFactory, trackSelector);
        mPlayer.setPlayWhenReady(true);
        //mPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);

        mExoPlayerController.setPlayer(mPlayer);
        switch (mQuality) {
            case QUALITY_LOW:
                mExoPlayerController.selectFormat(FormatItem.VIDEO_SD_AVC_30);
                break;
            case QUALITY_NORMAL:
                mExoPlayerController.selectFormat(FormatItem.VIDEO_HD_AVC_30);
                break;
            case QUALITY_HIGH:
                mExoPlayerController.selectFormat(FormatItem.VIDEO_FHD_AVC_60);
                break;
        }
        // Don't use subs! Not efficient. High cpu load. Cause input lags.
        mExoPlayerController.selectFormat(FormatItem.SUBTITLE_NONE);
        if (mIsMute) {
            mExoPlayerController.setVolume(0);
        }

        setPlayer(mPlayer);

        mPlaybackPresenter.onEngineInitialized(); // start playback
    }

    private void destroyPlayerObjects() {
        if (isEngineInitialized()) {
            Utils.removeCallbacks(mShowView);
            Utils.removeCallbacks(mStopPlayback);
            Utils.removeCallbacks(mApplyPendingStartPositionRunnable);
            // Don't replace main player!
            if (mPlaybackPresenter.getView() == null || mPlaybackPresenter.getView() == this) {
                mPlaybackPresenter.onEngineReleased();
            }
            mPlayerInitializer.release();
            mExoPlayerController.setOnVideoLoaded(null);
            // Fix access calls when player isn't initialized
            mExoPlayerController.release();
            mPlayer = null;
            setPlayer(null);
            hideView();
            syncPositionIfNeeded();
        }
    }

    private void syncPositionIfNeeded() {
        if (!mIsMute && isPositionChanged()) {
            BasePresenter<?> presenter = getViewManager().getTopPresenter();
            if (presenter != null) {
                presenter.syncItem(mVideo);
            }
        }
    }

    private boolean isPositionChanged() {
        return mVideo != null && Math.abs(mPercentWatched - mVideo.percentWatched) > 20;
    }

    private void onVideoLoaded() {
        // Fix the screen becomes black for a moment
        Utils.postDelayed(mShowView, 1_000);

        if (mPendingStartPositionMs >= 0) {
            scheduleApplyPendingStartPosition(300);
        }

        if (mIsMute) { // Save bandwidth if the previews are muted
            Utils.postDelayed(mStopPlayback, 5 * 60 * 1_000);
        }
    }
    private void scheduleApplyPendingStartPosition(long delayMs) {
        Utils.removeCallbacks(mApplyPendingStartPositionRunnable);
        Utils.postDelayed(mApplyPendingStartPositionRunnable, delayMs);
    }

    private void applyPendingStartPosition() {
        if (mExoPlayerController == null || mPendingStartPositionMs < 0) {
            return;
        }

        long targetPositionMs = Math.max(0L, mPendingStartPositionMs);

        try {
            mExoPlayerController.setPositionMs(targetPositionMs);
            mExoPlayerController.setPlayWhenReady(true);
        } catch (Exception ignored) {
        }

        long currentPositionMs = -1L;

        try {
            currentPositionMs = mExoPlayerController.getPositionMs();
        } catch (Exception ignored) {
        }

        boolean seekApplied =
                targetPositionMs == 0L
                        || currentPositionMs >= targetPositionMs - 1500L;

        if (seekApplied) {
            mPendingStartPositionMs = -1L;
            mPendingStartSeekAttempts = 0;
            mForceStartFromBeginning = false;
            return;
        }

        mPendingStartSeekAttempts++;

        if (mPendingStartSeekAttempts <= PENDING_START_SEEK_MAX_ATTEMPTS) {
            scheduleApplyPendingStartPosition(500);
            return;
        }

        mPendingStartPositionMs = -1L;
        mPendingStartSeekAttempts = 0;
        mForceStartFromBeginning = false;
    }
    public void setMute(boolean mute) {
        mIsMute = mute;

        if (mExoPlayerController != null) {
            mExoPlayerController.setVolume(mute ? 0 : 1f);
        }
    }

    private ViewManager getViewManager() {
        return ViewManager.instance(getContext());
    }

    private PlaybackPresenter getPlaybackPresenter() {
        return PlaybackPresenter.instance(getContext());
    }
}
