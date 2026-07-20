package com.liskovsoft.smartyoutubetv2.tv.ui.search;

import android.net.NetworkInfo;
import android.annotation.SuppressLint;
import android.os.Build;
import android.provider.Settings;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.liskovsoft.mediaserviceinterfaces.ContentService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.views.KTVView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.adapter.KtvAdapter;
import com.liskovsoft.smartyoutubetv2.tv.adapter.PlaylistAdapter;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.manager.AuthorizeManager;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.update.UpdateCheckActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.update.UpdateService;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import android.graphics.Color;
import android.view.GestureDetector;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer.EmbedPlayerView;
import org.json.JSONObject;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import android.graphics.Rect;
import android.net.TrafficStats;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
/**
 * KTV 主页面。
 *
 * 主要职责：
 * 1. 搜索 YouTube 视频并展示歌曲卡片。
 * 2. 管理 Playlist / Favorites 的保存、删除、置顶和展示。
 * 3. 使用 EmbedPlayerView 在左侧窗口播放视频，并支持全屏切换。
 * 4. 监听播放进度，在歌曲接近结束时自动播放 Playlist 下一首。
 *
 * 注意：
 * - Playlist / Favorites 的 key 统一使用 videoId。
 * - SharedPreferences 中保存两类数据：videoId 顺序列表，以及 videoId -> Video.toString() 的快照。
 * - 播放队列 playlistPlaybackQueue 是运行时队列，真正自动播放时读取它，而不是直接读取 RecyclerView。
 */
public class KTVActivity extends LeanbackActivity implements KTVView {
    /** 普通页面日志标签。 */
    private static final String TAG = "KTVActivity";

    /** Playlist 自动播放、删除、置顶等队列相关日志标签。 */
    private static final String PLAYLIST_TAG = "KTVPlaylist";

    /** 控制播放器状态日志的输出频率，避免 1.5 秒轮询时刷爆 logcat。 */
    private long lastPlaylistWatcherLogMs = 0L;

    /** 中间右侧歌曲列表。搜索结果、Playlist、Favorites 都复用这个 RecyclerView。 */
    private RecyclerView songResultList;

    /** 搜索结果网格 Adapter。只负责搜索列表，不负责 Playlist / Favorites。 */
    private KtvAdapter ktvAdapter;

    /** 搜索输入框。 */
    private EditText etSearch;

    /** 搜索按钮。 */
    private View btnSearch;

    /** 当前正在播放的歌曲标题。 */
    private TextView tvNowPlayingTitle;

    /** 下一首提示，只显示 Playlist 里的下一首。 */
    private TextView tvNextTitle;

    /** 当前系统媒体音量百分比。 */
    private TextView tvVolumeValue;

    /** 左侧视频播放容器。EmbedPlayerView 会被动态添加到这里。 */
    private FrameLayout ktvVideoContainer;

    /** 搜索分类按钮：Karaoke。 */
    private MaterialButton btnKaraoke;
    /** 搜索分类按钮：All。 */
    private MaterialButton btnAll;
    /** 搜索分类按钮：Official Music。 */
    private MaterialButton btnOfficial;
    /** 搜索分类按钮：Remix。 */
    private MaterialButton btnRemix;
    /** 搜索分类按钮：DJ。 */
    private MaterialButton btnDj;

    /** 底部播放 / 暂停按钮。 */
    private MaterialButton btnPause;
    /** 底部重播按钮。 */
    private MaterialButton btnReplay;
    /** 底部下一首按钮。 */
    private MaterialButton btnNext;
    /** 音量减按钮。 */
    private MaterialButton btnVolumeDown;
    /** 音量加按钮。 */
    private MaterialButton btnVolumeUp;
    /** 静音按钮。 */
    private MaterialButton btnMute;
    /** Favorites 入口按钮，长按可收藏当前歌曲。 */
    private MaterialButton favoriteBtn;
    /** Playlist 入口按钮，长按可把当前歌曲加入播放列表。 */
    private MaterialButton playlistBtn;

    /** 底部音量进度条，用来调节系统 STREAM_MUSIC 音量。 */
    private SeekBar seekbarVolume;

    /** 主线程 Handler，用于延迟刷新 UI、轮询播放器进度和执行自动下一首。 */
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /** 当前搜索订阅，用于新搜索开始时取消旧请求，防止过期结果回写 UI。 */
    private Disposable searchDisposable;

    /** 当前搜索结果对应的 VideoGroup，用于列表滑到底后 continueGroup。 */
    private VideoGroup currentSearchVideoGroup;

    /** continueGroup 请求订阅，防止重复加载下一页。 */
    private Disposable continueGroupDisposable;

    /** 是否正在加载下一页搜索结果。 */
    private boolean continueGroupLoading = false;

    /** 当前搜索结果是否已经没有更多内容。 */
    private boolean continueGroupFinished = false;

    /** 距离底部多少个 item 时开始加载下一页。0 表示真正到底才加载。 */
    private static final int SEARCH_CONTINUE_PRELOAD_THRESHOLD = 4;

    /** 搜索版本号。每次搜索递增，只接收最新版本的结果。 */
    private long searchGeneration = 0L;

    /** 当前页面正在展示的视频列表。搜索、Playlist、Favorites 切换时都会更新。 */
    private final ArrayList<Video> currentVideos = new ArrayList<>();

    /** 搜索结果去重集合，key 为 videoId，避免同一批搜索结果重复加入列表。 */
    private final HashSet<String> loadedVideoKeys = new HashSet<>();

    /** Playlist 的内存副本，主要用于删除后同步局部 UI 状态。持久数据仍以 SharedPreferences 为准。 */
    private final ArrayList<Video> localPlaylist = new ArrayList<>();

    /** KTV 页面本地保存数据使用的 SharedPreferences 名称。 */
    private static final String PREF_NAME = "ktv_data";

    /** Playlist 保存的 key 列表名称。列表内容为 videoId。 */
    private static final String KEY_PLAYLIST = "ktv_playlist";

    /** Favorites 保存的 key 列表名称。列表内容为 videoId。 */
    private static final String KEY_FAVORITES = "ktv_favorites";

    /** 顺序列表后缀。实际存储名类似 ktv_playlist_ordered。 */
    private static final String KEY_ORDER_SUFFIX = "_ordered";

    /** SharedPreferences 中保存有序 videoId 列表时使用的分隔符，避免和普通标题字符冲突。 */
    private static final String KEY_SEPARATOR = "\u001E";
    /** 当前 RecyclerView 显示的数据来源。 */
    private enum ListMode {
        /** 搜索结果网格。 */
        SEARCH,
        /** 收藏列表。 */
        FAVORITES,
        /** 播放列表。 */
        PLAYLIST
    }

    /** 当前列表模式。决定 RecyclerView 使用搜索 Adapter、Playlist Adapter 还是 Favorites Adapter。 */
    private ListMode listMode = ListMode.SEARCH;

    /** 当前播放视频在 currentVideos 里的位置。Playlist 自动播放时可能为 -1。 */
    private int currentIndex = -1;

    /** 页面层面的暂停状态。和播放器内部状态一起用于判断是否继续自动下一首。 */
    private boolean paused = false;

    /** 是否静音。静音时音量按钮和 AudioManager 状态会同步更新。 */
    private boolean muted = false;

    /** 静音前最后一次非 0 音量，用于取消静音时恢复。 */
    private int lastNonZeroVolumePercent = 50;

    /** 系统音频管理器，用于控制电视 / 设备的媒体音量。 */
    private AudioManager audioManager;

    /** 用户输入的基础搜索词，不包含 karaoke / remix 等模式后缀。 */
    private String baseSearchText = "";

    /** 当前搜索分类。会影响 buildSearchQuery 拼接的搜索关键词。 */
    private SearchMode searchMode = SearchMode.KARAOKE;

    private int mUploadDateOptions;
    private int mDurationOptions;
    private int mTypeOptions;
    private int mFeatureOptions;
    private int mSortingOptions;

    private View searchBox;
    private View btnHeaderWifi;
    private View btnHeaderSettings;
    private TextView tvHeaderTime;
    /** 最近一次搜索结果缓存。用于从 Playlist / Favorites 返回搜索结果页。 */
    private final ArrayList<Video> lastSearchVideos = new ArrayList<>();

    /** Favorites 数量角标。 */
    private TextView favoriteCountBadge;

    /** Playlist 数量角标。 */
    private TextView playlistCountBadge;

    /** 当前播放视频的 videoId。用于高亮列表项和计算 NEXT 标题。 */
    private String currentPlayingVideoKey = "";
    /**
     * 当前歌曲已经从 Playlist 中移除，但播放器仍在播放。
     * 等播放器自然结束后，需要清空 Now Playing。
     */
    private boolean clearNowPlayingWhenCurrentVideoEnds = false;
    /** 左侧视频面板，包含视频容器、播放信息浮层和全屏按钮。 */
    private View leftVideoPanel;

    /** 实际播放器 View。动态创建，添加到 ktvVideoContainer 中。 */
    private EmbedPlayerView ktvEmbedPlayer;

    /** 当前播放器正在打开的 Video 对象。播放器重建、恢复播放时会用到。 */
    private Video ktvPlayingVideo;

    /** 播放器暂停状态。用于按钮显示和自动下一首判断。 */
    private boolean ktvPlayerPaused = true;

    /** 覆盖在播放器上方的透明手势层，用来接收长按菜单等手势。 */
    private View ktvVideoGestureLayer;

    /** 视频区域手势识别器，目前主要用于长按呼出播放器菜单。 */
    private GestureDetector ktvVideoGestureDetector;

    /** 限制播放器菜单弹出的频率，防止长按或事件重复触发。 */
    private long lastKtvPlayerMenuShownMs = 0L;
    /** Playlist 列表 Adapter，负责置顶、删除、收藏状态等操作。 */
    private PlaylistAdapter playlistAdapter;

    /** Favorites 列表 Adapter，负责收藏列表展示和删除。 */
    private PlaylistAdapter favoritesAdapter;
    /** 顶部 Header。进入视频全屏时隐藏，退出时恢复。 */
    private View headerView;

    /** 中间内容区。全屏时会临时改成 match_parent。 */
    private View contentArea;

    /** 底部控制栏。进入视频全屏时隐藏，退出时恢复。 */
    private View bottomControls;

    /** 视频上的 Now Playing / NEXT 浮层。全屏时默认隐藏。 */
    private View videoInfoOverlay;

    /** 视频右下角全屏 / 退出全屏按钮。 */
    private ImageButton btnVideoFullscreen;

    /** 当前视频区域是否处于全屏模式。只改变布局，不重新创建播放器。 */
    private boolean ktvVideoFullscreen = false;

    /** 进入全屏前 contentArea 的原始 LayoutParams，用于退出全屏恢复。 */
    private FrameLayout.LayoutParams normalContentAreaLp;

    /** 进入全屏前 leftVideoPanel 的原始 LayoutParams。 */
    private LinearLayout.LayoutParams normalLeftVideoPanelLp;

    /** 进入全屏前 songResultList 的原始 LayoutParams。 */
    private LinearLayout.LayoutParams normalSongResultListLp;

    private int normalContentPaddingLeft;
    private int normalContentPaddingTop;
    private int normalContentPaddingRight;
    private int normalContentPaddingBottom;

    private int normalLeftPanelPaddingLeft;
    private int normalLeftPanelPaddingTop;
    private int normalLeftPanelPaddingRight;
    private int normalLeftPanelPaddingBottom;

    private int normalHeaderVisibility = View.VISIBLE;
    private int normalBottomControlsVisibility = View.VISIBLE;
    private int normalSongResultListVisibility = View.VISIBLE;
    private int normalVideoInfoOverlayVisibility = View.VISIBLE;

    /** Playlist 自动播放运行时队列。真正自动下一首时读取这个队列。 */
    private final ArrayList<Video> playlistPlaybackQueue = new ArrayList<>();

    /** 当前播放的不是 Playlist 歌曲时，标记播完后要接入 Playlist。 */
    private boolean startPlaylistAfterCurrentVideo = false;

    /** 当前播放到 playlistPlaybackQueue 的第几个元素。 */
    private int playlistPlaybackIndex = -1;

    /** Playlist 自动播放是否正在运行。 */
    private boolean playlistPlaybackActive = false;

    /** 当前正在播放的歌曲是否来自 Playlist 自动播放队列。 */
    private boolean currentPlaybackFromPlaylist = false;

    /** 上一次读取到的播放位置。播放器释放后用于判断是否刚好接近结尾。 */
    private long lastPlaylistPositionMs = -1;

    /** 上一次读取到的视频总时长。 */
    private long lastPlaylistDurationMs = -1;

    /** 上一次轮询时是否已经接近视频结尾。 */
    private boolean lastPlaylistNearEnd = false;
    private View emptyListOverlay;
    private TextView tvEmptyList;
    /** 搜索结果歌曲卡片上的自定义菜单弹窗。 */
    private PopupWindow currentVideoMenuPopup;
    /** 最近一次打开歌曲菜单时对应的歌曲 item，用于菜单关闭后恢复焦点。 */
    private View lastVideoMenuAnchor;
    /** 菜单打开期间是否临时禁止 Header 区域抢焦点。 */
    private boolean headerFocusBlockedByVideoMenu = false;
    /** 最近一次打开歌曲菜单时对应的 adapter position。anchor 被回收时用 position 兜底找回 item。 */
    private int lastVideoMenuPosition = -1;

    /** 菜单关闭后是否需要恢复焦点。 */
    private boolean restoreFocusAfterVideoMenuDismiss = false;
    /** 保存 Video.toString() 快照的 SharedPreferences 后缀。 */
    private static final String KEY_VIDEO_SPEC_SUFFIX = "_video_specs";
    private static final int VIDEO_MENU_LEFT_MARGIN_DP = 4;
    private static final int VIDEO_MENU_RIGHT_MARGIN_DP = 4;
    private static final int VIDEO_MENU_TOP_BOTTOM_MARGIN_DP = 4;
    private static final long VIDEO_MENU_AUTO_DISMISS_MS = 50_000L;

    private final Runnable videoMenuAutoDismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismissVideoMenu();
        }
    };
    /**
     * Playlist 自动下一首轮询任务。
     *
     * 由于 EmbedPlayerView 没有直接暴露完整的播放完成回调，
     * 这里通过定时读取 position / duration 判断当前歌曲是否接近结束。
     */
    private final Runnable playlistAutoNextRunnable = new Runnable() {
        @Override
        public void run() {
            checkPlaylistAutoNext();
        }
    };
    @Override
    public Intent getNewIntent() {
        return null;
    }

    @Override
    public void finishView() {

    }

    /** 搜索分类，会影响最终搜索关键词。 */
    private enum SearchMode {
        /** 默认 KTV 搜索，在关键词后拼接 karaoke。 */
        KARAOKE,
        /** 不额外拼接分类词。 */
        ALL,
        /** 官方 MV。 */
        OFFICIAL,
        /** Remix 版本。 */
        REMIX,
        /** DJ / mix 版本。 */
        DJ
    }
    private View searchLoadingOverlay;
    private TextView tvSearchLoading;

    private View ktvPlayerLoadingOverlay;
    private TextView tvKtvPlayerLoading;
    private boolean ktvPlayerLoading = false;
    private long ktvPlayerLoadingStartMs = 0L;
    private AtomicBoolean hasStartAuth = new AtomicBoolean(false);
    private AtomicBoolean hasResultAuth = new AtomicBoolean(false);
    private static final long KTV_PLAYER_LOADING_SLOW_MS = 8_000L;

    private final Runnable ktvPlayerLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            checkKtvPlayerLoadingReady();
        }
    };

    /** 播放中网络 / 卡顿检测间隔。 */
    private static final long KTV_BUFFER_MONITOR_INTERVAL_MS = 1000L;

    /** 播放位置超过这个时间不动，就认为可能正在缓冲。 */
    private static final long KTV_BUFFER_STALL_MS = 2500L;

    /** 上一次检测到的播放位置。 */
    private long lastBufferCheckPositionMs = -1L;

    /** 上一次播放位置检测时间。 */
    private long lastBufferCheckTimeMs = 0L;

    /** 播放位置开始停住的时间。 */
    private long bufferStallStartMs = 0L;

    /** 上一次读取到的网络接收字节数。 */
    private long lastTrafficRxBytes = -1L;

    /** 上一次读取网络速度的时间。 */
    private long lastTrafficCheckMs = 0L;

    /** 当前是否正在显示网络缓冲提示。 */
    private boolean ktvBufferingHintShown = false;



    /** 播放中缓冲 / 网速检测任务。 */
    private final Runnable ktvBufferMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            checkKtvPlaybackBuffering();
        }
    };
    private static final String BUFFER_TAG = "KTVBuffer";
    private static final boolean DEBUG_KTV_BUFFER = true;

    private ConnectivityManager ktvConnectivityManager;
    private ConnectivityManager.NetworkCallback ktvNetworkCallback;

    private boolean ktvPendingNetworkRecovery = false;
    private Video ktvPendingRecoveryVideo;
    private long ktvPendingRecoveryPositionMs = 0L;

    private static final long KTV_NETWORK_RECOVERY_RETRY_DELAY_MS = 1500L;

    /** 网络断开期间是否强制保持断网提示。 */
    private boolean ktvNetworkDisconnectedHintShowing = false;

    private final Runnable ktvNetworkRecoveryRunnable = new Runnable() {
        @Override
        public void run() {
            retryKtvPlaybackAfterNetworkReturn();
        }
    };

    private void logBuffer(String message) {
        if (DEBUG_KTV_BUFFER) {
            Log.d(BUFFER_TAG, message);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        UpdateService.startCheck(this);
//        UpdateCheckActivity.startCheck(this);
        UpdateCheckActivity updateCheckActivity = new UpdateCheckActivity();
        updateCheckActivity.startCheck(this);
//        Log.i("KTVActivity", "onStart=" + hasStartAuth.get() + ":" + hasResultAuth.get());
//        if (hasStartAuth.get()) {
//            if (hasResultAuth.get()) {
//                if (!AuthorizeManager.getInstance(this).isAuth()) {
//                    finish();
//                }
//            } else {
//                hasResultAuth.set(true);
//            }
//        }
    }
    /**
     * 页面入口。负责绑定 View、初始化播放器容器、列表、搜索框、按钮、音量等模块。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lb_ktv_fragment);
        if (!AuthorizeManager.getInstance(this).isAuth() ) { //
            startAuth();
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        bindViews();
        setupKtvVideoContainerActions();
        setupVideoFullscreenButton();
        // 提前创建播放器 View，避免第一次点击菜单播放时页面闪一下。
        prepareKtvPlayerIfNeeded();
        setupSongResultList();
        setupKtvNetworkRecoveryMonitor();
        // 先设置通用焦点
        setupTvFocusAndClicks();

        // 再设置具体控件逻辑，避免监听器被覆盖
        setupSearchBox();
        setupHeaderStatus();
        setupCategoryButtons();
        setupPlaybackButtons();
        setupVolumeControls();
        updateSavedBadgeCounts();
        loadMySearchResult(baseSearchText);

        // 页面打开后，尝试从 Playlist 自动顺序播放。
        // 延迟一下，避免 RecyclerView / Adapter 还没完成初始化。
        uiHandler.postDelayed(this::tryStartPlaylistPlaybackIfNeeded, 800);
    }
    private void startAuth() {
        startActivityForResult(
                new Intent(this, AuthActivity.class),
                AuthActivity.REQUEST_LICE_COE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != AuthActivity.REQUEST_LICE_COE) {
            return;
        }

        if (resultCode == RESULT_OK
                && AuthorizeManager.getInstance(this).isAuth()) {
            Log.i(TAG, "Authorization succeeded");
            return;
        }

        Log.w(TAG, "Authorization failed or canceled");
        finish();
    }
    /**
     * 控制搜索列表的加载提示。
     *
     * show=true：
     * 搜索接口正在请求，搜索结果列表还没有内容时显示“正在搜索歌曲...”。
     *
     * show=false：
     * 搜索成功、搜索失败、搜索完成、切换到 Playlist/Favorites 时隐藏提示。
     */
    private void showSearchLoading(boolean show) {
        if (searchLoadingOverlay == null) {
            return;
        }

        if (show) {
            showEmptyListHint(false);
        }

        searchLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            if (tvSearchLoading != null) {
                tvSearchLoading.setText("Searching for songs…");
            }

            searchLoadingOverlay.bringToFront();
        }
    }
    /**
     * 提前创建 KTV 播放器 View。
     *
     * 作用：
     * 1. 避免第一次点击 Play Now / Add to Playlist 时才创建播放器导致页面闪烁。
     * 2. 播放器只创建一次，后续播放只调用 openVideo(video)。
     * 3. 不会立即播放视频，只是提前把播放器 View 放进 ktv_video_container。
     */
    private void prepareKtvPlayerIfNeeded() {
        if (ktvVideoContainer == null || ktvEmbedPlayer != null) {
            return;
        }

        ktvEmbedPlayer = new EmbedPlayerView(this);
        ktvEmbedPlayer.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        ktvEmbedPlayer.setQuality(EmbedPlayerView.QUALITY_NORMAL);
        // 关键：打开播放器自带控制器
        ktvEmbedPlayer.setUseController(false);
        ktvEmbedPlayer.setMute(false);

        // 用黑色背景兜底，避免播放器 Surface 第一次初始化时露出透明闪烁。
        ktvEmbedPlayer.setBackgroundColor(Color.BLACK);
        // 允许点击
        ktvEmbedPlayer.setClickable(true);
        ktvEmbedPlayer.setLongClickable(true);
        // 触摸设备点一下可以操作，遥控器也可以聚焦到播放器
        ktvEmbedPlayer.setFocusable(true);
        ktvEmbedPlayer.setFocusableInTouchMode(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        ktvVideoContainer.addView(ktvEmbedPlayer, lp);
        ktvVideoContainer.setVisibility(View.VISIBLE);

        ensureKtvVideoGestureLayer();
        bringVideoFullscreenButtonToFront();
    }
    /**
     * 初始化视频右下角全屏按钮。
     *
     * 点击按钮只切换布局大小，不重建播放器，因此播放不会中断。
     */
    private void setupVideoFullscreenButton() {
        if (btnVideoFullscreen == null) {
            return;
        }

        btnVideoFullscreen.setFocusable(true);
        btnVideoFullscreen.setFocusableInTouchMode(false);
        btnVideoFullscreen.setClickable(true);
        btnVideoFullscreen.setEnabled(true);

        btnVideoFullscreen.setOnClickListener(v -> toggleKtvVideoFullscreen());

        btnVideoFullscreen.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.12f : 1.0f;
            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(120)
                    .start();
        });

        btnVideoFullscreen.setOnKeyListener((v, keyCode, event) -> {
            boolean isCenter =
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;

            if (!isCenter) {
                return false;
            }

            if (event.getAction() == KeyEvent.ACTION_UP) {
                v.performClick();
            }

            return true;
        });

        bringVideoFullscreenButtonToFront();
    }
    /** 根据当前全屏状态，在进入全屏和退出全屏之间切换。 */
    private void toggleKtvVideoFullscreen() {
        if (ktvVideoFullscreen) {
            exitKtvVideoFullscreen();
        } else {
            enterKtvVideoFullscreen();
        }
    }
    /**
     * 进入视频全屏模式。
     *
     * 隐藏 Header、底部控制栏和右侧歌曲列表，把 leftVideoPanel 铺满整个屏幕。
     */
    private void enterKtvVideoFullscreen() {
        if (ktvVideoFullscreen) {
            return;
        }

        if (contentArea == null || leftVideoPanel == null || songResultList == null) {
            return;
        }

        saveNormalVideoLayoutState();

        ktvVideoFullscreen = true;

        if (headerView != null) {
            headerView.setVisibility(View.GONE);
        }

        if (bottomControls != null) {
            bottomControls.setVisibility(View.GONE);
        }

        if (songResultList != null) {
            songResultList.setVisibility(View.GONE);
        }

        // 全屏时隐藏顶部播放信息，只保留视频和右下角按钮。
        // 如果你想全屏仍显示 Now Playing / NEXT，把这段删掉。
        if (videoInfoOverlay != null) {
            videoInfoOverlay.setVisibility(View.GONE);
        }
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        contentLp.setMargins(0, 0, 0, 0);
        contentArea.setLayoutParams(contentLp);
        contentArea.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        leftLp.setMargins(0, 0, 0, 0);
        leftVideoPanel.setLayoutParams(leftLp);
        leftVideoPanel.setPadding(0, 0, 0, 0);
        leftVideoPanel.setBackgroundColor(Color.BLACK);

        if (ktvVideoContainer != null) {
            ktvVideoContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            ktvVideoContainer.setBackgroundColor(Color.BLACK);
        }

        hideSystemUiForKtvFullscreen();

        if (btnVideoFullscreen != null) {
            btnVideoFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
            btnVideoFullscreen.setContentDescription("Exit fullscreen");
        }

        bringVideoFullscreenButtonToFront();

        if (btnVideoFullscreen != null) {
            btnVideoFullscreen.requestFocus();
        }

        logPlaylist("enterKtvVideoFullscreen");
    }
    /**
     * 退出视频全屏模式。
     *
     * 恢复进入全屏前保存的 LayoutParams、padding 和可见性状态。
     */
    private void exitKtvVideoFullscreen() {
        if (!ktvVideoFullscreen) {
            return;
        }

        ktvVideoFullscreen = false;

        restoreNormalVideoLayoutState();
        showSystemUiAfterKtvFullscreen();

        if (btnVideoFullscreen != null) {
            btnVideoFullscreen.setImageResource(R.drawable.ic_fullscreen);
            btnVideoFullscreen.setContentDescription("Fullscreen");
        }

        bringVideoFullscreenButtonToFront();

        if (btnVideoFullscreen != null) {
            btnVideoFullscreen.requestFocus();
        }

        logPlaylist("exitKtvVideoFullscreen");
    }
    /** 保存进入全屏之前的布局参数和可见性，用于退出全屏时原样恢复。 */
    private void saveNormalVideoLayoutState() {
        if (contentArea != null && normalContentAreaLp == null
                && contentArea.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                normalContentAreaLp = new FrameLayout.LayoutParams(
                        (FrameLayout.LayoutParams) contentArea.getLayoutParams()
                );
            }

            normalContentPaddingLeft = contentArea.getPaddingLeft();
            normalContentPaddingTop = contentArea.getPaddingTop();
            normalContentPaddingRight = contentArea.getPaddingRight();
            normalContentPaddingBottom = contentArea.getPaddingBottom();
        }

        if (leftVideoPanel != null && normalLeftVideoPanelLp == null
                && leftVideoPanel.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                normalLeftVideoPanelLp = new LinearLayout.LayoutParams(
                        (LinearLayout.LayoutParams) leftVideoPanel.getLayoutParams()
                );
            }

            normalLeftPanelPaddingLeft = leftVideoPanel.getPaddingLeft();
            normalLeftPanelPaddingTop = leftVideoPanel.getPaddingTop();
            normalLeftPanelPaddingRight = leftVideoPanel.getPaddingRight();
            normalLeftPanelPaddingBottom = leftVideoPanel.getPaddingBottom();
        }

        if (songResultList != null && normalSongResultListLp == null
                && songResultList.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                normalSongResultListLp = new LinearLayout.LayoutParams(
                        (LinearLayout.LayoutParams) songResultList.getLayoutParams()
                );
            }
        }

        if (headerView != null) {
            normalHeaderVisibility = headerView.getVisibility();
        }

        if (bottomControls != null) {
            normalBottomControlsVisibility = bottomControls.getVisibility();
        }

        if (songResultList != null) {
            normalSongResultListVisibility = songResultList.getVisibility();
        }

        if (videoInfoOverlay != null) {
            normalVideoInfoOverlayVisibility = videoInfoOverlay.getVisibility();
        }
    }
    /** 恢复进入全屏前保存的布局状态。 */
    private void restoreNormalVideoLayoutState() {
        if (contentArea != null && normalContentAreaLp != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentArea.setLayoutParams(new FrameLayout.LayoutParams(normalContentAreaLp));
            }
            contentArea.setPadding(
                    normalContentPaddingLeft,
                    normalContentPaddingTop,
                    normalContentPaddingRight,
                    normalContentPaddingBottom
            );
        }

        if (leftVideoPanel != null && normalLeftVideoPanelLp != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                leftVideoPanel.setLayoutParams(new LinearLayout.LayoutParams(normalLeftVideoPanelLp));
            }
            leftVideoPanel.setPadding(
                    normalLeftPanelPaddingLeft,
                    normalLeftPanelPaddingTop,
                    normalLeftPanelPaddingRight,
                    normalLeftPanelPaddingBottom
            );

            // 恢复背景交给 XML/原背景。这里不强制重设，避免你自定义 drawable 被覆盖。
        }

        if (songResultList != null) {
            if (normalSongResultListLp != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    songResultList.setLayoutParams(new LinearLayout.LayoutParams(normalSongResultListLp));
                }
            }

            songResultList.setVisibility(normalSongResultListVisibility);
        }

        if (headerView != null) {
            headerView.setVisibility(normalHeaderVisibility);
        }

        if (bottomControls != null) {
            bottomControls.setVisibility(normalBottomControlsVisibility);
        }

        if (videoInfoOverlay != null) {
            videoInfoOverlay.setVisibility(normalVideoInfoOverlayVisibility);
        }
        if (ktvVideoContainer != null) {
            ktvVideoContainer.setBackgroundColor(Color.BLACK);
        }
    }
    /** 隐藏系统状态栏和导航栏，让视频全屏更接近沉浸式显示。 */
    private void hideSystemUiForKtvFullscreen() {
        try {
            View decorView = getWindow().getDecorView();

            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } catch (Exception ignored) {
        }
    }
    /** 退出视频全屏后恢复系统 UI。 */
    private void showSystemUiAfterKtvFullscreen() {
        try {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Exception ignored) {
        }
    }
    /** 确保全屏按钮位于播放器和手势层上方，否则按钮可能被覆盖无法点击。 */
    private void bringVideoFullscreenButtonToFront() {
        if (btnVideoFullscreen == null) {
            return;
        }

        btnVideoFullscreen.bringToFront();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            btnVideoFullscreen.setElevation(dp(12));
            btnVideoFullscreen.setTranslationZ(dp(12));
        }
    }
    /**
     * 尝试启动 Playlist 自动播放。
     *
     * 从 SharedPreferences 中读取 Playlist 的 videoId 顺序，
     * 再通过 VideoSpec 恢复 Video 对象，构建 playlistPlaybackQueue。
     *
     * @return true 表示已经处于播放状态或成功启动播放；false 表示没有可播放歌曲。
     */
    /**
     * 尝试启动 Playlist。
     *
     * 默认情况下，如果当前正在播放用户手动 Play Now 的歌曲，
     * 不允许立刻切到 Playlist，只设置播完后接入。
     */
    private boolean tryStartPlaylistPlaybackIfNeeded() {
        return tryStartPlaylistPlaybackIfNeeded(false);
    }

    /**
     * @param forceStart true 表示当前歌曲已经播放完成，可以强制切入 Playlist。
     */
    private boolean tryStartPlaylistPlaybackIfNeeded(boolean forceStart) {
        logPlaylist("tryStartPlaylistPlaybackIfNeeded ENTER"
                + ", active=" + playlistPlaybackActive
                + ", queueSize=" + playlistPlaybackQueue.size()
                + ", forceStart=" + forceStart
                + ", immediatePlaying=" + isImmediateSongPlayingOrLoaded());

        if (playlistPlaybackActive) {
            logPlaylist("tryStartPlaylistPlaybackIfNeeded SKIP: already active");
            return true;
        }

        ArrayList<String> savedKeys = getSavedVideoKeyList(KEY_PLAYLIST);
        dumpSavedKeys("tryStart saved playlist", savedKeys);

        if (savedKeys.isEmpty()) {
            logPlaylist("tryStartPlaylistPlaybackIfNeeded STOP: savedKeys empty");
            startPlaylistAfterCurrentVideo = false;
            updateNextTitle();
            return false;
        }

        // 关键：
        // 当前正在播放 Play Now 歌曲时，不允许直接启动 Playlist。
        // 只标记“当前歌播完后再进入 Playlist”。
        if (!forceStart && isImmediateSongPlayingOrLoaded()) {
            startPlaylistAfterCurrentVideo = true;

            logPlaylist("tryStartPlaylistPlaybackIfNeeded DEFER: immediate song is playing"
                    + ", playlistSize=" + savedKeys.size()
                    + ", current=" + videoDebug(ktvPlayingVideo));

            updateNextTitle();
            startPlaylistEndWatcher();

            return true;
        }

        ArrayList<Video> savedVideos = getSavedVideosFromCache(KEY_PLAYLIST);

        logPlaylist("tryStart savedVideos.size=" + savedVideos.size()
                + ", savedKeys.size=" + savedKeys.size());

        if (savedVideos.isEmpty()) {
            logPlaylist("tryStartPlaylistPlaybackIfNeeded STOP: savedVideos empty");
            updateNextTitle();
            return false;
        }

        playlistPlaybackQueue.clear();
        playlistPlaybackQueue.addAll(savedVideos);

        playlistPlaybackActive = true;
        currentPlaybackFromPlaylist = true;
        startPlaylistAfterCurrentVideo = false;
        playlistPlaybackIndex = 0;

        logPlaylist("tryStartPlaylistPlaybackIfNeeded START queueSize="
                + playlistPlaybackQueue.size()
                + ", index=" + playlistPlaybackIndex);

        playPlaylistQueueCurrent();

        return true;
    }

    /** 初始化电视遥控器焦点、方向键确认键，以及默认焦点位置。 */
    private void setupTvFocusAndClicks() {


        // 分类按钮
        // 分类按钮不做焦点放大，避免 Karaoke 初始获得焦点后看起来比其他按钮大一圈。
        makeFocusable(btnKaraoke, true, false);
        makeFocusable(btnAll, true, false);
        makeFocusable(btnOfficial, true, false);
        makeFocusable(btnRemix, true, false);
        makeFocusable(btnDj, true, false);

        // 播放控制按钮
        makeFocusable(btnPause, true);
        makeFocusable(btnReplay, true);
        makeFocusable(btnNext, true);
        makeFocusable(btnVolumeDown, true);
        makeFocusable(btnVolumeUp, true);
        makeFocusable(btnMute, true);
        makeFocusable(favoriteBtn, true);
        makeFocusable(playlistBtn, true);
        // 音量条
        setupSeekBarFocus();

        // 搜索结果列表
        if (songResultList != null) {
            songResultList.setFocusable(true);
            songResultList.setFocusableInTouchMode(false);
            songResultList.setClickable(true);
            songResultList.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            songResultList.setNextFocusUpId(R.id.et_search);
            songResultList.setNextFocusDownId(R.id.btn_karaoke);

            songResultList.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    requestFirstSongFocus();
                    hideKeyboard();
                }
            });
        }

        setupFocusOrder();

        // 初始先让搜索框能拿到焦点；等搜索结果回来后，你的 addSearchResults 会让第一个影片拿焦点
        /*
        当 Activity 刚启动时，系统默认处于 “触摸模式（Touch Mode）”（即 isInTouchMode() 返回 true）。在触摸模式下，只有 focusableInTouchMode 属性为 true 的 View 才能通过 requestFocus() 成功获得焦点。
        btnKaraoke 被代码强制设为了 focusableInTouchMode(false)，因此在启动瞬间无法获取焦点。
        et_search 在 XML 中设为 true，且它是页面中默认的可输入控件，系统会自动将焦点保留或分配给它。
        如果一个 Activity 有一个 EditText，系统通常会在启动时自动将焦点放在它上面，除非 有东西显式地将其移开。
         */
        uiHandler.postDelayed(() -> {
            if (btnKaraoke != null) {
                // 1. 临时允许按钮在触摸模式下获取焦点（仅这次有效）
                btnKaraoke.setFocusableInTouchMode(true);

                // 2. 强制请求焦点（此时一定能成功）
                boolean focused = btnKaraoke.requestFocus();

                // 3. 关键步骤：立即恢复原状，避免后续点击需要两次
                //    因为此时 btnKaraoke 已经持有焦点，设为 false 不会让它失去焦点，
                //    但后续用户触摸点击时，由于焦点已在它身上，系统会直接触发 onClick，
                //    不会再消耗一次点击去转移焦点。
                btnKaraoke.setFocusableInTouchMode(false);

                if (focused) {
//                    btnKaraoke.setSelected(true);
                    if (etSearch != null && etSearch.hasFocus()) {
                        etSearch.clearFocus();
                    }
                    hideKeyboard();
                }
            } else if (etSearch != null) {
                etSearch.requestFocus();
            }
        }, 200);
    }

    /**
     * 让普通 View 支持 TV 焦点效果和中键点击。
     *
     * @param centerKeyClick true 时拦截遥控器中键并触发 performClick。
     */
    /**
     * 让普通 View 支持 TV 焦点效果和中键点击。
     *
     * @param centerKeyClick 是否用遥控器中键触发 performClick
     * @param scaleOnFocus 是否在获得焦点时放大
     */
    private void makeFocusable(View view, boolean centerKeyClick, boolean scaleOnFocus) {
        if (view == null) {
            return;
        }

        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        view.setClickable(true);
        view.setEnabled(true);

        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.setSelected(hasFocus || isSavedModeSelected(v));

            float scale = scaleOnFocus && hasFocus ? 1.08f : 1.0f;

            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(120)
                    .start();
        });

        if (centerKeyClick) {
            view.setOnKeyListener((v, keyCode, event) -> {
                boolean isCenter =
                        keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                                || keyCode == KeyEvent.KEYCODE_ENTER
                                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;

                if (!isCenter) {
                    return false;
                }

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    v.performClick();
                }

                return true;
            });
        }
    }

    /**
     * 默认版本：普通控件获得焦点时放大。
     */
    private void makeFocusable(View view, boolean centerKeyClick) {
        makeFocusable(view, centerKeyClick, true);
    }
    /** 判断 Favorites / Playlist 按钮是否应该显示选中态。 */
    private boolean isSavedModeSelected(View view) {
        return (view == favoriteBtn && listMode == ListMode.FAVORITES)
                || (view == playlistBtn && listMode == ListMode.PLAYLIST);
    }

    /** 根据当前列表模式更新 Favorites / Playlist 按钮的选中态和透明度。 */
    private void updateSavedButtonState() {
        if (favoriteBtn != null) {
            favoriteBtn.setSelected(listMode == ListMode.FAVORITES || favoriteBtn.hasFocus());
            favoriteBtn.setAlpha(listMode == ListMode.FAVORITES ? 1.0f : 0.78f);
        }

        if (playlistBtn != null) {
            playlistBtn.setSelected(listMode == ListMode.PLAYLIST || playlistBtn.hasFocus());
            playlistBtn.setAlpha(listMode == ListMode.PLAYLIST ? 1.0f : 0.78f);
        }
    }
    /** 初始化音量 SeekBar 的遥控器焦点和左右键调节逻辑。 */
    private void setupSeekBarFocus() {
        if (seekbarVolume == null) return;

        seekbarVolume.setFocusable(true);
        seekbarVolume.setFocusableInTouchMode(false);
        seekbarVolume.setClickable(true);
        seekbarVolume.setNextFocusLeftId(R.id.btn_volume_down);
        seekbarVolume.setNextFocusRightId(R.id.btn_volume_up);
        seekbarVolume.setNextFocusUpId(R.id.btn_official);

        seekbarVolume.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.06f : 1.0f;
            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(120)
                    .start();
        });

        seekbarVolume.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    setSystemVolume(getSystemCurrentVolume() - 1, true);
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    setSystemVolume(getSystemCurrentVolume() + 1, true);
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    return true;

                default:
                    return false;
            }
        });
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean result = super.dispatchKeyEvent(event);

        if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                    || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                uiHandler.postDelayed(this::syncVolumeFromSystem, 80);
            }
        }

        return result;
    }
    @Override
    protected void onResume() {
        super.onResume();
        syncVolumeFromSystem();
    }
    /** 设置页面各个可聚焦控件之间的遥控器方向键导航关系。 */
    private void setupFocusOrder() {
        // 顶部搜索区域
        setNextFocus(etSearch, 0, R.id.btn_search, 0, R.id.song_result_list);
        setNextFocus(btnSearch, R.id.et_search, R.id.btn_header_wifi, 0, R.id.song_result_list);

        setNextFocus(btnHeaderWifi, R.id.btn_search, R.id.btn_header_settings, 0, R.id.song_result_list);
        setNextFocus(btnHeaderSettings, R.id.btn_header_wifi, 0, 0, R.id.song_result_list);

        // 分类行：Karaoke / All / Official / Remix / DJ
        setNextFocus(btnKaraoke, 0, R.id.btn_all, R.id.song_result_list, R.id.btn_pause);
        setNextFocus(btnAll, R.id.btn_karaoke, R.id.btn_official, R.id.song_result_list, R.id.btn_replay);
        setNextFocus(btnOfficial, R.id.btn_all, R.id.btn_remix, R.id.song_result_list, R.id.seekbar_volume);
        setNextFocus(btnRemix, R.id.btn_official, R.id.btn_dj, R.id.song_result_list, R.id.btn_volume_up);
        setNextFocus(btnDj, R.id.btn_remix, 0, R.id.song_result_list, R.id.favorite_btn);

        // 底部播放控制行
        setNextFocus(btnPause, 0, R.id.btn_replay, R.id.btn_karaoke, 0);
        setNextFocus(btnReplay, R.id.btn_pause, R.id.btn_next, R.id.btn_all, 0);
        setNextFocus(btnNext, R.id.btn_replay, R.id.btn_volume_down, R.id.btn_all, 0);
        setNextFocus(btnVolumeDown, R.id.btn_next, R.id.seekbar_volume, R.id.btn_official, 0);
        setNextFocus(seekbarVolume, R.id.btn_volume_down, R.id.btn_volume_up, R.id.btn_official, 0);
        setNextFocus(btnVolumeUp, R.id.seekbar_volume, R.id.btn_mute, R.id.btn_remix, 0);
        setNextFocus(btnMute, R.id.btn_volume_up, R.id.favorite_btn, R.id.btn_remix, 0);
        setNextFocus(favoriteBtn, R.id.btn_mute, R.id.playlist_btn, R.id.btn_dj, 0);
        setNextFocus(playlistBtn, R.id.favorite_btn, 0, R.id.btn_dj, 0);
    }

    /** 给指定 View 设置上下左右焦点跳转目标。 */
    private void setNextFocus(View view, int leftId, int rightId, int upId, int downId) {
        if (view == null) return;

        if (leftId != 0) {
            view.setNextFocusLeftId(leftId);
        }

        if (rightId != 0) {
            view.setNextFocusRightId(rightId);
        }

        if (upId != 0) {
            view.setNextFocusUpId(upId);
        }

        if (downId != 0) {
            view.setNextFocusDownId(downId);
        }
    }

    /** 请求列表第一个歌曲 item 获得焦点。Playlist / Favorites 中优先聚焦置顶按钮。 */
    private void requestFirstSongFocus() {
        if (songResultList == null
                || songResultList.getAdapter() == null
                || songResultList.getAdapter().getItemCount() <= 0) {
            return;
        }

        songResultList.scrollToPosition(0);

        songResultList.postDelayed(() -> {
            RecyclerView.ViewHolder holder =
                    songResultList.findViewHolderForAdapterPosition(0);

            if (holder == null || holder.itemView == null) {
                songResultList.requestFocus();
                return;
            }

            if (listMode == ListMode.PLAYLIST || listMode == ListMode.FAVORITES) {
                View firstButton = holder.itemView.findViewById(R.id.btn_pin_top);

                if (firstButton != null
                        && firstButton.getVisibility() == View.VISIBLE
                        && firstButton.isFocusable()) {
                    firstButton.requestFocus();
                    return;
                }
            }

            if (holder.itemView.isFocusable()) {
                holder.itemView.requestFocus();
            } else {
                songResultList.requestFocus();
            }
        }, 120);
    }
    /** 绑定 XML 中的所有控件引用。 */
    private void bindViews() {
        songResultList = findViewById(R.id.song_result_list);

        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);

        searchBox = findViewById(R.id.search_box);
        btnHeaderWifi = findViewById(R.id.btn_header_wifi);
        btnHeaderSettings = findViewById(R.id.btn_header_settings);
        tvHeaderTime = findViewById(R.id.tv_header_time);

        tvNowPlayingTitle = findViewById(R.id.tv_now_playing_title);
        tvNextTitle = findViewById(R.id.tv_next_title);
        tvVolumeValue = findViewById(R.id.tv_volume_value);

        leftVideoPanel = findViewById(R.id.left_video_panel);
        ktvVideoContainer = findViewById(R.id.ktv_video_container);

        btnKaraoke = findViewById(R.id.btn_karaoke);
        btnAll = findViewById(R.id.btn_all);
        btnOfficial = findViewById(R.id.btn_official);
        btnRemix = findViewById(R.id.btn_remix);
        btnDj = findViewById(R.id.btn_dj);

        btnPause = findViewById(R.id.btn_pause);
        btnReplay = findViewById(R.id.btn_replay);
        btnNext = findViewById(R.id.btn_next);
        btnVolumeDown = findViewById(R.id.btn_volume_down);
        btnVolumeUp = findViewById(R.id.btn_volume_up);
        btnMute = findViewById(R.id.btn_mute);
        favoriteBtn = findViewById(R.id.favorite_btn);
        playlistBtn = findViewById(R.id.playlist_btn);
        favoriteCountBadge = findViewById(R.id.favorite_count_badge);
        playlistCountBadge = findViewById(R.id.playlist_count_badge);
        seekbarVolume = findViewById(R.id.seekbar_volume);

        if (etSearch != null) {
            etSearch.setText(baseSearchText);
            etSearch.setSelectAllOnFocus(true);
        }

        headerView = findViewById(R.id.header);
        contentArea = findViewById(R.id.content_area);
        bottomControls = findViewById(R.id.bottom_controls);
        videoInfoOverlay = findViewById(R.id.video_info_overlay);
        btnVideoFullscreen = findViewById(R.id.btn_video_fullscreen);

        searchLoadingOverlay = findViewById(R.id.search_loading_overlay);
        tvSearchLoading = findViewById(R.id.tv_search_loading);

        ktvPlayerLoadingOverlay = findViewById(R.id.ktv_player_loading_overlay);
        tvKtvPlayerLoading = findViewById(R.id.tv_ktv_player_loading);

        emptyListOverlay = findViewById(R.id.empty_list_overlay);
        tvEmptyList = findViewById(R.id.tv_empty_list);
    }
    /**
     * 控制 Favorites / Playlist 空列表提示。
     *
     * show=true：
     * 当前打开的是 Favorites 或 Playlist，并且列表没有任何歌曲时显示。
     *
     * show=false：
     * 搜索列表、正在加载、列表有内容、返回搜索结果时隐藏。
     */
    /**
     * 控制 Favorites / Playlist 空列表提示。
     *
     * 搜索页永远不显示这个提示，避免从空 Playlist / Favorites 返回搜索页时残留。
     */
    private void showEmptyListHint(boolean show) {
        if (emptyListOverlay == null) {
            return;
        }

        boolean allowShow =
                show && (listMode == ListMode.PLAYLIST || listMode == ListMode.FAVORITES);

        emptyListOverlay.setVisibility(allowShow ? View.VISIBLE : View.GONE);

        if (!allowShow) {
            return;
        }

        if (tvEmptyList != null) {
            if (listMode == ListMode.PLAYLIST) {
                tvEmptyList.setText("The playlist is empty. Please press the back button.");
            } else if (listMode == ListMode.FAVORITES) {
                tvEmptyList.setText("The favorites list is empty. Please press the back button.");
            }
        }

        emptyListOverlay.bringToFront();
    }
    /** 刷新 Favorites 和 Playlist 的数量角标。 */
    private void updateSavedBadgeCounts() {
        updateBadge(favoriteCountBadge, getSavedVideoKeyList(KEY_FAVORITES).size());
        updateBadge(playlistCountBadge, getSavedVideoKeyList(KEY_PLAYLIST).size());
    }
    private int getSavedCount(String prefKey) {
        SharedPreferences sp = getSharedPreferences("ktv_data", MODE_PRIVATE);
        Set<String> set = sp.getStringSet(prefKey, Collections.emptySet());
        return set == null ? 0 : set.size();
    }

    /** 根据数量显示、隐藏并更新角标文本。 */
    private void updateBadge(TextView badge, int count) {
        if (badge == null) return;

        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }

        badge.setVisibility(View.VISIBLE);
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
    }
    /**
     * 初始化歌曲列表 RecyclerView。
     *
     * 搜索模式使用 KtvAdapter，单击歌曲弹出自定义菜单。
     * Playlist / Favorites 模式使用 PlaylistAdapter，并通过 Listener 处理置顶、删除、收藏等操作。
     */
    private void setupSongResultList() {
        if (songResultList == null) return;

        songResultList.setFocusable(true);
        songResultList.setFocusableInTouchMode(false);
        songResultList.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        songResultList.setClipToPadding(false);
        songResultList.setItemViewCacheSize(24);
        songResultList.setHasFixedSize(true);

        songResultList.setLayoutManager(new GridLayoutManager(this, 4));

        ktvAdapter = new KtvAdapter(this, new ArrayList<>());

        ktvAdapter.setOnItemClickListener((video, position) -> {
            View anchor = findSearchItemAnchor(position);

            if (anchor == null) {
                anchor = songResultList;
            }

            showVideoMenu(anchor, video, position);
        });

        ktvAdapter.setOnItemMenuListener((anchor, video, position) -> {
            if (anchor == null) {
                anchor = findSearchItemAnchor(position);
            }

            if (anchor == null) {
                anchor = songResultList;
            }

            showVideoMenu(anchor, video, position);
        });

        songResultList.setAdapter(ktvAdapter);

        songResultList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    dismissVideoMenu();
                }

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    maybeContinueSearchGroup(recyclerView);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dx != 0 || dy != 0) {
                    dismissVideoMenu();
                }

                // 只在向下滚动时检查是否需要加载下一页。
                if (dy > 0) {
                    maybeContinueSearchGroup(recyclerView);
                }
            }
        });

        PlaylistAdapter.Listener savedListListener = new PlaylistAdapter.Listener() {
            @Override
            public void onPlay(Video video, int position) {
                if (listMode == ListMode.PLAYLIST) {
                    Toast.makeText(KTVActivity.this, "Playlist is already playing automatically", Toast.LENGTH_SHORT).show();
                    return;
                }
                stopKtvPlayer(true);
                playVideo(video, position);
            }

            @Override
            public void onMoveToTop(Video video, int position) {
                movePlaylistItemToTop(video);
            }

            @Override
            public void onDeleteFromPlaylist(Video video, int position) {
                removeFromSavedList(KEY_PLAYLIST, video);
            }

            @Override
            public void onToggleFavorite(Video video, int position) {
                if (isVideoSaved(KEY_FAVORITES, video)) {
                    removeFromSavedList(KEY_FAVORITES, video);
                } else {
                    addToFavorites(video);
                }

                refreshPlaylistView();
            }

            @Override
            public void onAddToPlaylist(Video video, int position) {
                addToPlaylist(video);
            }

            @Override
            public void onDeleteFromFavorites(Video video, int position) {
                removeFromSavedList(KEY_FAVORITES, video);
            }

            @Override
            public boolean isFavorite(Video video) {
                return isVideoSaved(KEY_FAVORITES, video);
            }

            @Override
            public String getVideoKey(Video video) {
                return KTVActivity.this.getVideoKey(video);
            }
        };

        playlistAdapter = new PlaylistAdapter(this, new ArrayList<>(), savedListListener);
        playlistAdapter.setMode(PlaylistAdapter.Mode.PLAYLIST);

        favoritesAdapter = new PlaylistAdapter(this, new ArrayList<>(), savedListListener);
        favoritesAdapter.setMode(PlaylistAdapter.Mode.FAVORITES);
    }
    /**
     * 搜索列表滑到底或接近底部时，继续加载下一页。
     *
     * 只处理 SEARCH 模式，Playlist / Favorites 不走 continueGroup。
     */
    private void maybeContinueSearchGroup(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }

        if (listMode != ListMode.SEARCH) {
            return;
        }

        if (continueGroupLoading || continueGroupFinished) {
            return;
        }

        if (currentSearchVideoGroup == null) {
            return;
        }

        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        if (adapter == null || adapter.getItemCount() <= 0) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (!(layoutManager instanceof GridLayoutManager)) {
            return;
        }

        GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;

        int totalCount = adapter.getItemCount();
        int lastVisiblePosition = gridLayoutManager.findLastVisibleItemPosition();

        boolean reachBottom =
                lastVisiblePosition >= totalCount - 1 - SEARCH_CONTINUE_PRELOAD_THRESHOLD;

        if (!reachBottom) {
            return;
        }

        continueGroup(currentSearchVideoGroup);
    }
    /**
     * 继续加载当前搜索结果的下一页。
     *
     * 核心接口：
     * contentService.continueGroupObserve(mediaGroup)
     */
    private void continueGroup(VideoGroup group) {
        if (group == null) {
            continueGroupFinished = true;
            return;
        }

        if (continueGroupLoading) {
            return;
        }

        if (listMode != ListMode.SEARCH) {
            return;
        }

        MediaGroup mediaGroup = group.getMediaGroup();

        if (mediaGroup == null) {
            continueGroupFinished = true;
            return;
        }

        ContentService contentService = getContentService();

        if (contentService == null) {
            Toast.makeText(this, "ContentService is null", Toast.LENGTH_SHORT).show();
            return;
        }

        continueGroupLoading = true;

        if (tvSearchLoading != null) {
            tvSearchLoading.setText("Loading more songs…");
        }

        if (searchLoadingOverlay != null) {
            searchLoadingOverlay.setVisibility(View.VISIBLE);
            searchLoadingOverlay.bringToFront();
        }

        final long generation = searchGeneration;

        Log.d(TAG, "continueGroup: start continue group: " + group.getTitle());

        if (continueGroupDisposable != null && !continueGroupDisposable.isDisposed()) {
            continueGroupDisposable.dispose();
        }

        continueGroupDisposable = contentService
                .continueGroupObserve(mediaGroup)
                .subscribe(
                        continueMediaGroup -> {
                            VideoGroup newGroup = VideoGroup.from(group, continueMediaGroup);

                            ArrayList<Video> videos = new ArrayList<>();

                            if (newGroup != null && newGroup.getVideos() != null) {
                                videos.addAll(newGroup.getVideos());
                            }

                            runOnUiThread(() -> {
                                if (generation != searchGeneration) {
                                    return;
                                }

                                if (listMode != ListMode.SEARCH) {
                                    return;
                                }

                                if (newGroup == null || videos.isEmpty()) {
                                    continueGroupFinished = true;
                                    return;
                                }

                                // 关键：保存新的 group，下一次到底继续从新的 MediaGroup 接着加载。
                                currentSearchVideoGroup = newGroup;

                                addSearchResults(videos);
                            });
                        },
                        error -> runOnUiThread(() -> {
                            if (generation != searchGeneration) {
                                return;
                            }

                            continueGroupLoading = false;

                            if (searchLoadingOverlay != null) {
                                searchLoadingOverlay.setVisibility(View.GONE);
                            }

                            Log.e(TAG, "continueGroup error: "
                                    + (error != null ? error.getMessage() : "unknown"));
                        }),
                        () -> runOnUiThread(() -> {
                            if (generation != searchGeneration) {
                                return;
                            }

                            continueGroupLoading = false;

                            if (searchLoadingOverlay != null) {
                                searchLoadingOverlay.setVisibility(View.GONE);
                            }

                            Log.d(TAG, "continueGroup complete");
                        })
                );
    }
    /**
     * 显示搜索结果 item 的自定义菜单。
     *
     * 菜单宽度等于歌曲 item 宽度，底边贴住 item 底边，包含：加入 Playlist、加入 Favorites、立即播放。
     */
    private void showVideoMenu(View anchor, Video video, int position) {
        if (anchor == null || video == null) {
            return;
        }

        dismissVideoMenu();
// 菜单打开期间保持屏幕常亮，避免系统 idle dim 后 PopupWindow 和主界面亮度不一致。
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View itemRoot = findSongItemRoot(anchor, position);

        lastVideoMenuAnchor = itemRoot;
        lastVideoMenuPosition = position;
        restoreFocusAfterVideoMenuDismiss = true;

        blockHeaderFocusWhileVideoMenuOpen(true);

        View contentView = LayoutInflater.from(this).inflate(R.layout.popup_video_menu, null, false);

        LinearLayout menuAddPlaylist = contentView.findViewById(R.id.menu_add_playlist);
        LinearLayout menuAddFavorites = contentView.findViewById(R.id.menu_add_favorites);
        LinearLayout menuPlayNow = contentView.findViewById(R.id.menu_play_now);

        Rect itemRect = new Rect();
        boolean hasRect = itemRoot != null && itemRoot.getGlobalVisibleRect(itemRect);

        int menuLeftMargin = dp(VIDEO_MENU_LEFT_MARGIN_DP);
        int menuRightMargin = dp(VIDEO_MENU_RIGHT_MARGIN_DP);
        int menuVerticalMargin = dp(VIDEO_MENU_TOP_BOTTOM_MARGIN_DP);

        int itemWidth;

        if (hasRect) {
            itemWidth = itemRect.width();
        } else {
            itemWidth = itemRoot != null ? itemRoot.getWidth() : anchor.getWidth();
        }

        if (itemWidth <= 0) {
            itemWidth = dp(220);
        }

// 右外边距靠缩小 popupWidth 实现。
// 如果左边也要外边距，就同时减 menuLeftMargin。
        int popupWidth = Math.max(
                dp(120),
                itemWidth - menuLeftMargin - menuRightMargin
        );
        popupWidth = itemWidth - menuLeftMargin - menuRightMargin;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        contentView.measure(widthSpec, heightSpec);

        int popupHeight = contentView.getMeasuredHeight();

        if (popupHeight <= 0) {
            popupHeight = dp(190);
        }

        currentVideoMenuPopup = new PopupWindow(
                contentView,
                popupWidth,
                popupHeight,
                true
        );

        currentVideoMenuPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        currentVideoMenuPopup.setOutsideTouchable(true);
        currentVideoMenuPopup.setTouchable(true);
        currentVideoMenuPopup.setFocusable(true);
        currentVideoMenuPopup.setClippingEnabled(true);
        currentVideoMenuPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        currentVideoMenuPopup.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            currentVideoMenuPopup.setElevation(dp(12));
        }

        if (menuAddPlaylist != null) {
            menuAddPlaylist.setOnClickListener(v -> {
                restoreFocusAfterVideoMenuDismiss = true;

                dismissVideoMenu();

                uiHandler.post(() -> {
                    addToPlaylist(video);
                    uiHandler.postDelayed(this::restoreFocusToVideoMenuAnchor, 60);
                });
            });

            menuAddPlaylist.setOnKeyListener((v, keyCode, event) -> {
                if (isCenterKeyUp(keyCode, event)) {
                    v.performClick();
                    return true;
                }

                return false;
            });
        }

        if (menuAddFavorites != null) {
            menuAddFavorites.setOnClickListener(v -> {
                restoreFocusAfterVideoMenuDismiss = true;

                dismissVideoMenu();

                uiHandler.post(() -> {
                    addToFavorites(video);
                    uiHandler.postDelayed(this::restoreFocusToVideoMenuAnchor, 60);
                });
            });

            menuAddFavorites.setOnKeyListener((v, keyCode, event) -> {
                if (isCenterKeyUp(keyCode, event)) {
                    v.performClick();
                    return true;
                }

                return false;
            });
        }

        if (menuPlayNow != null) {
            menuPlayNow.setOnClickListener(v -> {
                restoreFocusAfterVideoMenuDismiss = true;

                dismissVideoMenu();

                uiHandler.post(() -> {
                    stopKtvPlayer(true);
                    playVideo(video, position);
                    uiHandler.postDelayed(this::restoreFocusToVideoMenuAnchor, 60);
                });
            });

            menuPlayNow.setOnKeyListener((v, keyCode, event) -> {
                if (isCenterKeyUp(keyCode, event)) {
                    v.performClick();
                    return true;
                }

                return false;
            });
        }

        currentVideoMenuPopup.setOnDismissListener(() -> {
            currentVideoMenuPopup = null;

            if (restoreFocusAfterVideoMenuDismiss) {
                restoreFocusToVideoMenuAnchor();

                uiHandler.postDelayed(this::restoreFocusToVideoMenuAnchor, 80);
                uiHandler.postDelayed(this::finishVideoMenuFocusRestore, 180);
            } else {
                blockHeaderFocusWhileVideoMenuOpen(false);
            }
        });

        View parent = getWindow() != null ? getWindow().getDecorView() : itemRoot;

        Rect parentRect = new Rect();
        boolean hasParentRect = parent != null && parent.getGlobalVisibleRect(parentRect);

        int x;
        int y;

        if (hasRect && hasParentRect) {
            x = itemRect.left - parentRect.left + menuLeftMargin;
            y = itemRect.bottom - parentRect.top - popupHeight - menuVerticalMargin;
        } else {
            int[] location = new int[2];
            itemRoot.getLocationOnScreen(location);

            x = location[0] + menuLeftMargin;
            y = location[1] + itemRoot.getHeight() - popupHeight - menuVerticalMargin;
        }

        if (y < 0) {
            y = 0;
        }
        currentVideoMenuPopup.showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        uiHandler.removeCallbacks(videoMenuAutoDismissRunnable);
        uiHandler.postDelayed(videoMenuAutoDismissRunnable, VIDEO_MENU_AUTO_DISMISS_MS);
        if (menuAddPlaylist != null) {
            menuAddPlaylist.requestFocus();
        }
    }
    /**
     * 菜单焦点恢复流程结束。
     *
     * 到这里歌曲 item 已经重新拿到焦点，
     * 再恢复搜索框可聚焦状态，避免它在 PopupWindow 关闭瞬间抢焦点。
     */
    private void finishVideoMenuFocusRestore() {
        restoreFocusToVideoMenuAnchor();
        blockHeaderFocusWhileVideoMenuOpen(false);
    }
    /**
     * 菜单打开期间临时禁止搜索框获取焦点。
     *
     * 原因：
     * PopupWindow 关闭时系统会重新分配焦点，
     * et_search 因为 focusableInTouchMode=true，很容易被系统选中，
     * 于是出现焦点先跳到搜索框、再被代码拉回歌曲 item 的闪烁。
     */
    /**
     * 菜单打开期间临时禁止 Header 区域控件获取焦点。
     *
     * 原因：
     * PopupWindow 关闭时系统会重新分配焦点。
     * et_search、btn_search、btn_header_wifi、btn_header_settings 都在顶部，
     * 而且都是可聚焦控件，所以它们可能抢走遥控器焦点。
     *
     * 菜单关闭并恢复歌曲 item 焦点后，再恢复这些控件的可聚焦状态。
     */
    private void blockHeaderFocusWhileVideoMenuOpen(boolean block) {
        headerFocusBlockedByVideoMenu = block;

        setViewFocusBlocked(etSearch, block);
        setViewFocusBlocked(btnSearch, block);
        setViewFocusBlocked(btnHeaderWifi, block);
        setViewFocusBlocked(btnHeaderSettings, block);

        // searchBox 本身不应该参与遥控器焦点竞争，只作为点击区域存在。
        if (searchBox != null) {
            searchBox.setFocusable(false);
            searchBox.setFocusableInTouchMode(false);
        }

        if (block) {
            hideKeyboard();
        }
    }
    /**
     * 临时设置某个 View 是否可以获取焦点。
     *
     * 注意：
     * 这里不调用 setEnabled(false)，否则按钮会变灰，UI 看起来像坏了。
     */
    private void setViewFocusBlocked(View view, boolean block) {
        if (view == null) {
            return;
        }

        view.setFocusable(!block);
        view.setFocusableInTouchMode(!block);

        if (block && view.hasFocus()) {
            view.clearFocus();
        }
    }
    /**
     * 关闭歌曲菜单后，把遥控器焦点还给刚才打开菜单的歌曲 item。
     *
     * 原因：
     * PopupWindow 是 focusable=true，关闭后系统会重新分配焦点。
     * 如果不手动恢复，焦点经常会跑到 et_search。
     */
    private void restoreFocusToVideoMenuAnchor() {
        if (!restoreFocusAfterVideoMenuDismiss) {
            return;
        }

        if (songResultList == null) {
            return;
        }

        hideKeyboard();

        View target = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (lastVideoMenuAnchor != null
                    && lastVideoMenuAnchor.isAttachedToWindow()
                    && lastVideoMenuAnchor.getVisibility() == View.VISIBLE) {
                target = lastVideoMenuAnchor;
            }
        }

        if (target == null && lastVideoMenuPosition >= 0) {
            RecyclerView.ViewHolder holder =
                    songResultList.findViewHolderForAdapterPosition(lastVideoMenuPosition);

            if (holder != null && holder.itemView != null) {
                target = holder.itemView;
            }
        }

        if (target != null) {
            target.setFocusable(true);
            target.setFocusableInTouchMode(false);
            target.requestFocus();

            if (target.hasFocus()) {
                return;
            }
        }
        // 最后兜底：让 RecyclerView 拿焦点，不要让搜索框拿。
        songResultList.setFocusable(true);
        songResultList.setFocusableInTouchMode(false);
        songResultList.requestFocus();
    }
    /**
     * 从当前点击的 View 向上查找真正的歌曲 item 根布局。
     *
     * 原因：
     * 菜单宽度必须以 song_item_root 为准，不能用 item 内部按钮、图片、文字的宽度。
     */
    private View findSongItemRoot(View anchor, int position) {
        View root = findSelfOrParentById(anchor, R.id.song_item_root);

        if (root != null) {
            return root;
        }

        if (songResultList != null && position >= 0) {
            RecyclerView.ViewHolder holder =
                    songResultList.findViewHolderForAdapterPosition(position);

            if (holder != null && holder.itemView != null) {
                View holderRoot = holder.itemView.findViewById(R.id.song_item_root);

                if (holderRoot != null) {
                    return holderRoot;
                }

                return holder.itemView;
            }
        }

        return anchor;
    }

    /**
     * 从当前 View 开始向父 View 查找指定 id 的 View。
     */
    private View findSelfOrParentById(View view, int targetId) {
        View current = view;

        while (current != null) {
            if (current.getId() == targetId) {
                return current;
            }

            if (!(current.getParent() instanceof View)) {
                break;
            }

            current = (View) current.getParent();
        }

        return null;
    }
    /** 根据 adapter position 找到当前歌曲 item View，用作 PopupWindow 的定位锚点。 */
    private View findSearchItemAnchor(int position) {
        if (songResultList == null || position < 0) {
            return null;
        }

        RecyclerView.ViewHolder holder =
                songResultList.findViewHolderForAdapterPosition(position);

        if (holder != null && holder.itemView != null) {
            return holder.itemView;
        }

        View focused = getCurrentFocus();

        if (focused != null) {
            return focused;
        }

        return songResultList;
    }

    /** 关闭当前歌曲 item 的自定义菜单。 */
    private void dismissVideoMenu() {
        uiHandler.removeCallbacks(videoMenuAutoDismissRunnable);
        if (currentVideoMenuPopup != null) {
            currentVideoMenuPopup.dismiss();
            currentVideoMenuPopup = null;
        }
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void clearVideoMenuFocusRecord() {
        lastVideoMenuAnchor = null;
        lastVideoMenuPosition = -1;
        restoreFocusAfterVideoMenuDismiss = false;
        blockHeaderFocusWhileVideoMenuOpen(false);
    }
    /** 判断是否为遥控器确认键抬起事件。 */
    private boolean isCenterKeyUp(int keyCode, KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }
    /**
     * 将指定歌曲移动到 Playlist 顶部。
     *
     * 如果当前 Playlist 正在播放，不打断当前歌曲，重建队列后让置顶歌曲成为下一首。
     */
    private void movePlaylistItemToTop(Video video) {
        if (video == null) return;

        ArrayList<String> keys = getSavedVideoKeyList(KEY_PLAYLIST);
        if (keys.isEmpty()) return;

        String movedKey = moveSavedVideoKeyToTop(keys, video);

        if (TextUtils.isEmpty(movedKey)) {
            logPlaylist("movePlaylistItemToTop FAIL: key not found, video=" + videoDebug(video));
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show();
            return;
        }

        writeSavedVideoKeys(KEY_PLAYLIST, keys);

        logPlaylist("movePlaylistItemToTop OK"
                + ", movedKey=" + shortKey(movedKey)
                + ", video=" + videoDebug(video));

        rebuildPlaylistPlaybackQueueAfterMoveToTop();

        Toast.makeText(this, "Moved to top", Toast.LENGTH_SHORT).show();

        if (listMode == ListMode.PLAYLIST) {
            refreshPlaylistView();
        }

        updateSavedBadgeCounts();
        updateNextTitle();

        if (songResultList != null && listMode == ListMode.PLAYLIST) {
            songResultList.scrollToPosition(0);
            requestFirstSongFocus();
        }
    }

    /** 在保存的 videoId 顺序列表中，把指定 Video 的 videoId 移到第一个位置。 */
    private String moveSavedVideoKeyToTop(ArrayList<String> keys, Video video) {
        if (keys == null || keys.isEmpty() || video == null) {
            return "";
        }

        String key = getVideoKey(video);

        if (TextUtils.isEmpty(key)) {
            return "";
        }

        if (!keys.remove(key)) {
            return "";
        }

        keys.add(0, key);

        logPlaylist("moveSavedVideoKeyToTop OK key=" + shortKey(key));

        return key;
    }

    /**
     * 置顶后重建运行时播放队列。
     *
     * 关键点：正在播放 Playlist 时，当前歌曲保留在队列第 0 位，置顶后的歌曲排到下一首。
     */
    private void rebuildPlaylistPlaybackQueueAfterMoveToTop() {
        ArrayList<Video> savedVideos = getSavedVideosFromCache(KEY_PLAYLIST);

        logPlaylist("rebuildPlaylistPlaybackQueueAfterMoveToTop ENTER"
                + ", playlistPlaybackActive=" + playlistPlaybackActive
                + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                + ", savedVideos=" + savedVideos.size()
                + ", oldQueueSize=" + playlistPlaybackQueue.size()
                + ", oldIndex=" + playlistPlaybackIndex
                + ", current=" + videoDebug(getCurrentVideo()));

        if (savedVideos.isEmpty()) {
            playlistPlaybackQueue.clear();
            playlistPlaybackIndex = -1;
            playlistPlaybackActive = false;
            startPlaylistAfterCurrentVideo = false;
            updateNextTitle();
            return;
        }

        if (playlistPlaybackActive) {
            Video currentVideo = getCurrentVideo();

            playlistPlaybackQueue.clear();

            if (currentVideo != null) {
                // 当前正在唱的歌不打断，放在队列第 0 位。
                // 置顶后的 Playlist 第一首会变成下一首。
                playlistPlaybackQueue.add(currentVideo);

                for (Video video : savedVideos) {
                    if (!isSameVideoForSavedList(currentVideo, video)) {
                        playlistPlaybackQueue.add(video);
                    }
                }

                playlistPlaybackIndex = 0;
            } else {
                playlistPlaybackQueue.addAll(savedVideos);
                playlistPlaybackIndex = 0;
            }

            logPlaylist("rebuildPlaylistPlaybackQueueAfterMoveToTop ACTIVE_DONE"
                    + ", newQueueSize=" + playlistPlaybackQueue.size()
                    + ", newIndex=" + playlistPlaybackIndex
                    + ", next=" + videoDebug(getNextPlaylistVideoForTitle()));

            startPlaylistEndWatcher();
            updateNextTitle();
            return;
        }

        // 当前不是 Playlist 自动播放，比如正在播放搜索结果。
        // 这时置顶只影响：当前歌播完后从置顶歌曲开始播。
        playlistPlaybackQueue.clear();

        if (ktvEmbedPlayer != null) {
            startPlaylistAfterCurrentVideo = true;
            startPlaylistEndWatcher();

            logPlaylist("rebuildPlaylistPlaybackQueueAfterMoveToTop HANDOFF_ENABLED"
                    + ", next=" + videoDebug(savedVideos.get(0)));

            updateNextTitle();
            return;
        }

        // 没有播放器在播，直接从置顶后的第一首开始播放。
        tryStartPlaylistPlaybackIfNeeded();

        updateNextTitle();
    }
    /** 刷新 Playlist 页面数据。如果当前不在 Playlist 页面，直接返回。 */
    private void refreshPlaylistView() {
        if (listMode != ListMode.PLAYLIST) {
            return;
        }

        ArrayList<Video> savedVideos = getSavedVideosFromCache(KEY_PLAYLIST);

        currentVideos.clear();
        currentVideos.addAll(savedVideos);

        if (playlistAdapter != null) {
            playlistAdapter.setMode(PlaylistAdapter.Mode.PLAYLIST);
            playlistAdapter.updateData(savedVideos);
            playlistAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        updateNextTitle();
        updateSavedBadgeCounts();
        showEmptyListHint(listMode == ListMode.PLAYLIST && currentVideos.isEmpty());
    }
    /** 从 Playlist 第一首开始自动播放。当前代码主要保留作备用入口。 */
    private void startPlaylistPlaybackFromFirst() {
        if (playlistPlaybackActive) {
            return;
        }

        playlistPlaybackQueue.clear();
        playlistPlaybackQueue.addAll(getSavedVideosFromCache(KEY_PLAYLIST));

        if (playlistPlaybackQueue.isEmpty()) {
            playlistPlaybackActive = false;
            playlistPlaybackIndex = -1;
            uiHandler.removeCallbacks(playlistAutoNextRunnable);
            return;
        }

        playlistPlaybackActive = true;
        playlistPlaybackIndex = 0;

        playPlaylistQueueCurrent();
    }

    /** 播放 playlistPlaybackQueue 当前索引指向的歌曲，并启动结束监听。 */
    private void playPlaylistQueueCurrent() {
        logPlaylist("playPlaylistQueueCurrent ENTER"
                + ", active=" + playlistPlaybackActive
                + ", index=" + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size());

        if (!playlistPlaybackActive
                || playlistPlaybackIndex < 0
                || playlistPlaybackIndex >= playlistPlaybackQueue.size()) {
            logPlaylist("playPlaylistQueueCurrent INVALID: finish playlist");
            finishPlaylistPlayback();
            return;
        }

        Video video = playlistPlaybackQueue.get(playlistPlaybackIndex);

        logPlaylist("playPlaylistQueueCurrent video=" + videoDebug(video));

        if (video == null) {
            logPlaylist("playPlaylistQueueCurrent video=null, go next as completed");
            playNextPlaylistQueueVideo(true);
            return;
        }

        // 关键：从这里开始，当前歌曲来自 Playlist 队列。
        currentPlaybackFromPlaylist = true;

        currentPlayingVideoKey = getVideoKey(video);
        currentIndex = findVideoIndexByKey(currentPlayingVideoKey, currentVideos);

        logPlaylist("playPlaylistQueueCurrent currentIndexInCurrentVideos="
                + currentIndex
                + ", currentPlayingVideoKey=" + shortKey(currentPlayingVideoKey));

        paused = false;
        ktvPlayerPaused = false;

        updateNowPlaying(video);
        updateNextTitle();
        updatePauseButton();

        if (playlistAdapter != null) {
            playlistAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        if (favoritesAdapter != null) {
            favoritesAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        stopKtvPlayer(true);

        lastPlaylistPositionMs = -1;
        lastPlaylistDurationMs = -1;
        lastPlaylistNearEnd = false;

        createAndStartKtvPlayer(video);

        logPlaylist("playPlaylistQueueCurrent START_WATCHER");
        startPlaylistEndWatcher();
    }
    /**
     * Playlist 歌曲自然播放完成后，静默从 Playlist 中删除该歌曲。
     *
     * 不弹 Toast，只更新保存数据、列表 UI 和角标。
     */
    private void removePlayedVideoFromPlaylistSilently(Video video) {
        logPlaylist("removePlayedVideoFromPlaylistSilently ENTER video=" + videoDebug(video));

        if (video == null) {
            logPlaylist("REMOVE_ABORT: video=null");
            return;
        }

        ArrayList<String> keys = getSavedVideoKeyList(KEY_PLAYLIST);

        logPlaylist("REMOVE_BEFORE size=" + keys.size());
        dumpSavedKeys("REMOVE_BEFORE", keys);

        String removedKey = removeSavedVideoKey(KEY_PLAYLIST, keys, video);

        if (TextUtils.isEmpty(removedKey)) {
            logPlaylist("REMOVE_FAILED: saved key not found for video=" + videoDebug(video));
            return;
        }

        writeSavedVideoKeys(KEY_PLAYLIST, keys);

        ArrayList<String> afterKeys = getSavedVideoKeyList(KEY_PLAYLIST);

        logPlaylist("REMOVE_AFTER size=" + afterKeys.size()
                + ", expected=" + keys.size()
                + ", removedKey=" + shortKey(removedKey));

        dumpSavedKeys("REMOVE_AFTER", afterKeys);

        removeVideoFromMemoryList(localPlaylist, removedKey);

        if (listMode == ListMode.PLAYLIST) {
            logPlaylist("REMOVE refreshPlaylistView because listMode=PLAYLIST");
            refreshPlaylistView();
        } else {
            logPlaylist("REMOVE skip refreshPlaylistView because listMode=" + listMode);
        }

        updateSavedBadgeCounts();

        logPlaylist("REMOVE_DONE video=" + videoDebug(video));
    }
    /**
     * 结束 Playlist 自动播放。
     *
     * 注意：
     * Playlist 结束不等于播放器一定已经停止。
     * 比如 Playlist 只有一首歌，用户手动点 Next 时，
     * 这首歌会从 Playlist 删除，但播放器画面仍然可能还在播放它。
     *
     * 所以这里不要无条件清空 currentPlayingVideoKey / currentIndex。
     */
    /**
     * 兼容旧调用。
     *
     * 默认不清空 Now Playing，避免用户手动 Next 到空 Playlist 时，
     * 播放器仍在播放最后一首歌，但标题被错误清空。
     */
    private void finishPlaylistPlayback() {
        finishPlaylistPlayback(false);
    }
    /**
     * 结束 Playlist 自动播放。
     *
     * @param clearNowPlayingTitle true 表示 Playlist 是自然播放到最后结束，
     *                             需要清空 tvNowPlayingTitle。
     */
    private void finishPlaylistPlayback(boolean clearNowPlayingTitle) {
        playlistPlaybackActive = false;
        playlistPlaybackIndex = -1;
        playlistPlaybackQueue.clear();

        // 注意：
        // 如果 clearNowPlayingWhenCurrentVideoEnds=true，说明还要继续监听播放器自然结束。
        // 这种情况下不能 removeCallbacks。
        if (!clearNowPlayingWhenCurrentVideoEnds) {
            uiHandler.removeCallbacks(playlistAutoNextRunnable);
        }

        currentPlaybackFromPlaylist = false;
        startPlaylistAfterCurrentVideo = false;

        lastPlaylistPositionMs = -1;
        lastPlaylistDurationMs = -1;
        lastPlaylistNearEnd = false;

        if (clearNowPlayingTitle) {
            currentIndex = -1;
            currentPlayingVideoKey = "";
            ktvPlayingVideo = null;

            updateNowPlaying(null);
            ktvPlayerPaused = true;
        } else if (ktvPlayingVideo != null) {
            currentPlayingVideoKey = getVideoKey(ktvPlayingVideo);
            currentIndex = findVideoIndexByKey(currentPlayingVideoKey, currentVideos);
            updateNowPlaying(ktvPlayingVideo);
        } else {
            currentIndex = -1;
            currentPlayingVideoKey = "";
            updateNowPlaying(null);
        }

        if (playlistAdapter != null) {
            playlistAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        if (favoritesAdapter != null) {
            favoritesAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        updateNextTitle();
        updatePauseButton();

        Toast.makeText(this, "Playlist finished", Toast.LENGTH_SHORT).show();
    }
    /**
     * 播放 Playlist 队列下一首。
     *
     * @param completedCurrentSong true 表示当前歌曲自然播完，需要从 Playlist 中删除；false 表示手动下一首，不删除。
     */
    /**
     * 播放 Playlist 队列下一首。
     *
     * @param removeCurrentSongFromPlaylist true 表示离开当前歌曲前，先从 Playlist 中删除当前歌曲。
     *                                      自然播放完成和用户手动点击 Next 都应该传 true。
     */
    private void playNextPlaylistQueueVideo(boolean removeCurrentSongFromPlaylist) {
        playNextPlaylistQueueVideo(removeCurrentSongFromPlaylist, false);
    }

    /**
     * 播放 Playlist 队列下一首。
     *
     * @param removeCurrentSongFromPlaylist true 表示离开当前歌曲前，从 Playlist 删除当前歌曲
     * @param currentSongCompleted true 表示当前歌曲是自然播放完成，不是手动点 Next
     */
    private void playNextPlaylistQueueVideo(
            boolean removeCurrentSongFromPlaylist,
            boolean currentSongCompleted
    ) {
        logPlaylist("playNextPlaylistQueueVideo ENTER"
                + ", removeCurrentSongFromPlaylist=" + removeCurrentSongFromPlaylist
                + ", currentSongCompleted=" + currentSongCompleted
                + ", active=" + playlistPlaybackActive
                + ", index=" + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size()
                + ", current=" + videoDebug(getCurrentVideo()));

        if (!playlistPlaybackActive) {
            logPlaylist("playNextPlaylistQueueVideo RETURN: playlistPlaybackActive=false");
            return;
        }

        Video leavingVideo = getCurrentVideo();

        if (removeCurrentSongFromPlaylist) {
            logPlaylist("playNextPlaylistQueueVideo REMOVE current playlist song before next");
            removeCurrentPlaylistSongAfterCompleted();
        }

        playlistPlaybackIndex++;

        logPlaylist("playNextPlaylistQueueVideo AFTER index++ index="
                + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size());

        if (playlistPlaybackIndex >= playlistPlaybackQueue.size()) {
            logPlaylist("playNextPlaylistQueueVideo FINISH: index >= queueSize"
                    + ", currentSongCompleted=" + currentSongCompleted
                    + ", leavingVideo=" + videoDebug(leavingVideo));

            if (currentSongCompleted) {
                // 自然播放完成到最后一首：直接清 Now Playing。
                clearNowPlayingWhenCurrentVideoEnds = false;
                finishPlaylistPlayback(true);
            } else {
                // 手动 Next 到空 Playlist：
                // 歌已经从 Playlist 删除，但播放器可能还在播 leavingVideo。
                // 不能立刻清标题，等它自然播放完成后再清。
                clearNowPlayingWhenCurrentVideoEnds = true;
                finishPlaylistPlayback(false);
                startPlaylistEndWatcher();
            }

            return;
        }

        clearNowPlayingWhenCurrentVideoEnds = false;

        playPlaylistQueueCurrent();

        if (songResultList != null && listMode == ListMode.PLAYLIST) {
            songResultList.smoothScrollToPosition(0);
        }
    }
    /** 删除当前播放完成的 Playlist 歌曲。 */
    private void removeCurrentPlaylistSongAfterCompleted() {
        logPlaylist("removeCurrentPlaylistSongAfterCompleted ENTER"
                + ", index=" + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size());

        if (playlistPlaybackIndex < 0 || playlistPlaybackIndex >= playlistPlaybackQueue.size()) {
            logPlaylist("removeCurrentPlaylistSongAfterCompleted RETURN: invalid index");
            return;
        }

        Video completedVideo = playlistPlaybackQueue.get(playlistPlaybackIndex);

        logPlaylist("removeCurrentPlaylistSongAfterCompleted video="
                + videoDebug(completedVideo));

        removePlayedVideoFromPlaylistSilently(completedVideo);
    }
    /** 初始化搜索框、搜索按钮和键盘搜索事件。 */
    private void setupSearchBox() {
        if (searchBox != null) {
            searchBox.setOnClickListener(v -> {
                if (etSearch != null) {
                    etSearch.requestFocus();
                    etSearch.setSelection(etSearch.getText().length());
                    showKeyboard(etSearch);
                }
            });
        }

        if (btnSearch != null) {
            makeHeaderButtonFocusable(btnSearch);

            btnSearch.setOnClickListener(v -> submitSearchFromInput());

            btnSearch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSearchBoxSelected();
                scaleHeaderView(v, hasFocus ? 1.08f : 1.0f);
            });
        }

        if (etSearch != null) {
            etSearch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSearchBoxSelected();
            });

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                boolean isKeyboardSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
                boolean isEnterDown = event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER);

                if (isKeyboardSearch || isEnterDown) {
                    submitSearchFromInput();
                    return true;
                }

                return false;
            });
        }

        updateSearchBoxSelected();
    }

    /** 初始化 Header 上的 WiFi、设置按钮和时间显示。 */
    private void setupHeaderStatus() {
        makeHeaderButtonFocusable(btnHeaderWifi);
        makeHeaderButtonFocusable(btnHeaderSettings);

        if (btnHeaderWifi != null) {
            btnHeaderWifi.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "WiFi Settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnHeaderSettings != null) {
            btnHeaderSettings.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        updateHeaderClock();
    }

    /** 每 30 秒刷新一次 Header 时间。 */
    private void updateHeaderClock() {
        if (tvHeaderTime != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            tvHeaderTime.setText(time);
        }

        uiHandler.postDelayed(this::updateHeaderClock, 30_000);
    }

    /** 让 Header 按钮支持 TV 焦点和确认键点击。 */
    private void makeHeaderButtonFocusable(View view) {
        if (view == null) return;

        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        view.setClickable(true);
        view.setEnabled(true);

        view.setOnFocusChangeListener((v, hasFocus) -> {
            scaleHeaderView(v, hasFocus ? 1.1f : 1.0f);
        });

        view.setOnKeyListener((v, keyCode, event) -> {
            boolean isCenter =
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;

            if (!isCenter) {
                return false;
            }

            if (event.getAction() == KeyEvent.ACTION_UP) {
                v.performClick();
            }

            return true;
        });
    }

    private void scaleHeaderView(View view, float scale) {
        if (view == null) return;

        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(120)
                .start();
    }

    private void updateSearchBoxSelected() {
        if (searchBox == null) return;

        boolean selected =
                (etSearch != null && etSearch.hasFocus())
                        || (btnSearch != null && btnSearch.hasFocus());

        searchBox.setSelected(selected);
    }

    private void showKeyboard(View view) {
        try {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null && view != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Exception ignored) {
        }
    }

    /** 读取搜索框内容并发起搜索；空搜索词使用默认歌手。 */
    private void submitSearchFromInput() {
        String text = getCurrentSearchInput();

        hideKeyboard();

        if (listMode != ListMode.SEARCH
                && text.equals(baseSearchText)
                && !lastSearchVideos.isEmpty()) {
            returnToSearchResults();
            return;
        }

        baseSearchText = text;
        loadMySearchResult(baseSearchText);
    }
    /**
     * 返回键处理顺序：
     * 1. 先关闭歌曲菜单。
     * 2. 再退出视频全屏。
     * 3. 如果在 Playlist / Favorites 页面，则返回搜索结果。
     * 4. 其余情况走系统返回。
     */
    @Override
    public void onBackPressed() {
        if (currentVideoMenuPopup != null) {
            dismissVideoMenu();
            return;
        }
        if (ktvVideoFullscreen) {
            exitKtvVideoFullscreen();
            return;
        }

        if (listMode == ListMode.FAVORITES || listMode == ListMode.PLAYLIST) {
            returnToSearchResults();
            return;
        }

        super.onBackPressed();
    }
    /** 初始化搜索分类按钮：Karaoke、All、Official、Remix、DJ。 */
    private void setupCategoryButtons() {
        if (btnKaraoke != null) {
            btnKaraoke.setOnClickListener(v -> switchSearchMode(SearchMode.KARAOKE));
        }

        if (btnAll != null) {
            btnAll.setOnClickListener(v -> switchSearchMode(SearchMode.ALL));
        }

        if (btnOfficial != null) {
            btnOfficial.setOnClickListener(v -> switchSearchMode(SearchMode.OFFICIAL));
        }

        if (btnRemix != null) {
            btnRemix.setOnClickListener(v -> switchSearchMode(SearchMode.REMIX));
        }

        if (btnDj != null) {
            btnDj.setOnClickListener(v -> switchSearchMode(SearchMode.DJ));
        }

//        updateCategoryButtonState();
    }

    /** 切换搜索分类并重新搜索。 */
    private void switchSearchMode(SearchMode mode) {
        String inputText = getCurrentSearchInput();

        // 只有在搜索框内容没有变化时，才直接返回原搜索结果
        if (listMode != ListMode.SEARCH
                && searchMode == mode
                && inputText.equals(baseSearchText)
                && !lastSearchVideos.isEmpty()) {
            returnToSearchResults();
            return;
        }

        baseSearchText = inputText;
        searchMode = mode;

        updateCategoryButtonState();
        hideKeyboard();

        loadMySearchResult(baseSearchText);
    }
    /** 获取搜索框当前文本。 */
    private String getCurrentSearchInput() {
        if (etSearch == null || etSearch.getText() == null) {
            return "";
        }

        return etSearch.getText().toString().trim();
    }
    private void updateCategoryButtonState() {
        updateCategoryButton(btnKaraoke, searchMode == SearchMode.KARAOKE);
        updateCategoryButton(btnAll, searchMode == SearchMode.ALL);
        updateCategoryButton(btnOfficial, searchMode == SearchMode.OFFICIAL);
        updateCategoryButton(btnRemix, searchMode == SearchMode.REMIX);
        updateCategoryButton(btnDj, searchMode == SearchMode.DJ);
    }

    private void updateCategoryButton(MaterialButton button, boolean selected) {
        if (button == null) return;

        button.setAlpha(selected ? 1.0f : 0.72f);
        button.setStrokeWidth(selected ? dp(2) : dp(1));
    }

    /** 初始化底部播放控制按钮：播放/暂停、重播、下一首，以及收藏/列表按钮。 */
    private void setupPlaybackButtons() {
        if (btnPause != null) {
            btnPause.setOnClickListener(v -> togglePause());
        }

        if (btnReplay != null) {
            btnReplay.setOnClickListener(v -> replayCurrent());
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> playNextVideo());
        }

        setupSavedListButtons();

        updatePauseButton();
    }
    /** 初始化 Favorites / Playlist 按钮。单击进入列表，长按把当前播放歌曲加入对应列表。 */
    private void setupSavedListButtons() {
        if (favoriteBtn != null) {
            favoriteBtn.setOnClickListener(v -> {
                showFavoritesList();
            });

            favoriteBtn.setOnLongClickListener(v -> {
                Video video = getCurrentVideo();
                if (video != null) {
                    addToFavorites(video);
                } else {
                    Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        if (playlistBtn != null) {
            playlistBtn.setOnClickListener(v -> {
                showPlaylistList();
            });

            playlistBtn.setOnLongClickListener(v -> {
                Video video = getCurrentVideo();
                if (video != null) {
                    addToPlaylist(video);
                } else {
                    Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        updateSavedButtonState();
    }
    /** 切换到 Favorites 页面并刷新收藏歌曲列表。 */
    private void showFavoritesList() {
        showSearchLoading(false);
        stopSearchLoading();

        listMode = ListMode.FAVORITES;
        updateSavedButtonState();

        if (songResultList != null) {
            songResultList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

            if (favoritesAdapter != null) {
                songResultList.setAdapter(favoritesAdapter);
            }
        }

        refreshFavoritesView();

        if (currentVideos.isEmpty()) {
            showEmptyListHint(true);
            return;
        }

        showEmptyListHint(false);
        requestFirstSongFocus();
    }

    /** 刷新 Favorites 页面数据。如果当前不在 Favorites 页面，直接返回。 */
    private void refreshFavoritesView() {
        if (listMode != ListMode.FAVORITES) {
            return;
        }

        ArrayList<Video> savedVideos = getSavedVideosFromCache(KEY_FAVORITES);

        currentIndex = -1;
        currentVideos.clear();
        currentVideos.addAll(savedVideos);

        if (favoritesAdapter != null) {
            favoritesAdapter.setMode(PlaylistAdapter.Mode.FAVORITES);
            favoritesAdapter.updateData(savedVideos);
            favoritesAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        updateNextTitle();
        updateSavedBadgeCounts();
        showEmptyListHint(listMode == ListMode.FAVORITES && currentVideos.isEmpty());
    }
    /** 切换到 Playlist 页面并刷新播放列表。 */
    private void showPlaylistList() {
        showSearchLoading(false);
        stopSearchLoading();

        listMode = ListMode.PLAYLIST;
        updateSavedButtonState();

        if (songResultList != null) {
            songResultList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

            if (playlistAdapter != null) {
                playlistAdapter.setMode(PlaylistAdapter.Mode.PLAYLIST);
                songResultList.setAdapter(playlistAdapter);
            }
        }

        refreshPlaylistView();

        if (currentVideos.isEmpty()) {
            showEmptyListHint(true);
            return;
        }

        showEmptyListHint(false);
        requestFirstSongFocus();
    }
    /**
     * 根据保存的 videoId 顺序读取 Video 列表。
     *
     * 虽然方法名还带 Cache，但当前已经不使用 videoCache；
     * 实际逻辑是从 SharedPreferences 的 VideoSpec 中恢复 Video 对象。
     */
    private ArrayList<Video> getSavedVideosFromCache(String prefKey) {
        ArrayList<String> savedKeys = getSavedVideoKeyList(prefKey);
        ArrayList<Video> savedVideos = new ArrayList<>();

        logPlaylist("getSavedVideosFromCache ENTER prefKey=" + prefKey
                + ", savedKeys=" + savedKeys.size());

        for (String key : savedKeys) {
            Video video = restoreVideoFromSpec(prefKey, key);

            if (video != null && !TextUtils.isEmpty(getVideoKey(video))) {
                savedVideos.add(video);
            } else {
                logPlaylist("getSavedVideosFromCache MISS key=" + shortKey(key));
            }
        }

        logPlaylist("getSavedVideosFromCache EXIT prefKey=" + prefKey
                + ", videos=" + savedVideos.size());

        return savedVideos;
    }
    /** 从指定保存列表中删除 Video 的 videoId，并同步删除对应 VideoSpec。 */
    private String removeSavedVideoKey(String prefKey, ArrayList<String> keys, Video video) {
        if (keys == null || video == null) {
            return "";
        }

        String key = getVideoKey(video);

        if (TextUtils.isEmpty(key)) {
            logPlaylist("removeSavedVideoKey FAIL empty key, video=" + videoDebug(video));
            return "";
        }

        if (!keys.remove(key)) {
            logPlaylist("removeSavedVideoKey FAIL key not found prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", video=" + videoDebug(video));
            return "";
        }

        removeVideoSpec(prefKey, key);

        logPlaylist("removeSavedVideoKey OK prefKey=" + prefKey
                + ", key=" + shortKey(key));

        return key;
    }

    /** 根据 videoId 判断两个 Video 是否是同一首歌。 */
    private boolean isSameVideoForSavedList(Video a, Video b) {
        String keyA = getVideoKey(a);
        String keyB = getVideoKey(b);

        return !TextUtils.isEmpty(keyA) && keyA.equals(keyB);
    }

    /** 判断 Video 的 videoId 是否等于保存列表中的某个 key。 */
    private boolean videoMatchesSavedKey(Video video, String savedKey) {
        String videoKey = getVideoKey(video);

        return !TextUtils.isEmpty(videoKey)
                && !TextUtils.isEmpty(savedKey)
                && videoKey.equals(savedKey);
    }
    /** 初始化音量按钮、SeekBar 和当前音量显示。 */
    /**
     * 初始化音量控制。
     *
     * 关键点：
     * SeekBar 使用 Android 系统真实音量档位，而不是 0~100 百分比。
     * 这样 KTV 页面音量和系统自带音量条能保持一致。
     */
    private void setupVolumeControls() {
        if (audioManager == null) {
            return;
        }

        int maxVolume = getSystemMaxVolume();
        int currentVolume = getSystemCurrentVolume();

        if (currentVolume > 0) {
            lastNonZeroVolumePercent = volumeToPercent(currentVolume, maxVolume);
        }

        muted = currentVolume == 0;

        if (seekbarVolume != null) {
            seekbarVolume.setMax(maxVolume);
            seekbarVolume.setProgress(currentVolume);

            seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        setSystemVolume(progress, true);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    syncVolumeFromSystem();
                }
            });
        }

        updateVolumeLabelFromSystem();

        if (btnVolumeDown != null) {
            btnVolumeDown.setOnClickListener(v -> {
                setSystemVolume(getSystemCurrentVolume() - 1, true);
            });
        }

        if (btnVolumeUp != null) {
            btnVolumeUp.setOnClickListener(v -> {
                setSystemVolume(getSystemCurrentVolume() + 1, true);
            });
        }

        if (btnMute != null) {
            btnMute.setOnClickListener(v -> toggleMute());
        }

        updateMuteButton();
    }
    /** 获取 Android 系统媒体音量最大档位。 */
    private int getSystemMaxVolume() {
        if (audioManager == null) {
            return 100;
        }

        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return Math.max(1, max);
    }

    /** 获取 Android 系统当前媒体音量档位。 */
    private int getSystemCurrentVolume() {
        if (audioManager == null) {
            return 0;
        }

        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = getSystemMaxVolume();

        return Math.max(0, Math.min(max, current));
    }

    /** 把系统音量档位转成百分比，只用于显示。 */
    private int volumeToPercent(int volume, int maxVolume) {
        if (maxVolume <= 0) {
            return 0;
        }

        return Math.round(volume * 100f / maxVolume);
    }

    /**
     * 设置 Android 系统媒体音量。
     *
     * @param volume 系统真实音量档位，不是百分比
     * @param showSystemUi true 时显示 Android 系统音量条
     */
    private void setSystemVolume(int volume, boolean showSystemUi) {
        if (audioManager == null) {
            return;
        }

        int maxVolume = getSystemMaxVolume();
        volume = Math.max(0, Math.min(maxVolume, volume));

        int flags = showSystemUi ? AudioManager.FLAG_SHOW_UI : 0;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, flags);

        muted = volume == 0;

        if (volume > 0) {
            lastNonZeroVolumePercent = volumeToPercent(volume, maxVolume);
        }

        syncVolumeFromSystem();
    }
    protected ContentService getContentService() {
        return YouTubeServiceManager.instance().getContentService();
    }
    /**
     * 发起搜索并异步加载结果。
     *
     * 使用 searchGeneration 防止旧请求结果覆盖新搜索结果。
     */
    public void loadMySearchResult(String searchText) {
        showEmptyListHint(false);
        listMode = ListMode.SEARCH;

        if (songResultList != null) {
            songResultList.setLayoutManager(new GridLayoutManager(this, 4));
        }

        if (songResultList != null && ktvAdapter != null) {
            songResultList.setAdapter(ktvAdapter);
        }
        updateSavedButtonState();
        lastSearchVideos.clear();
        String cleanText = TextUtils.isEmpty(searchText) ? "" : searchText.trim();
        String finalQuery = buildSearchQuery(cleanText);
//        String finalQuery = cleanText;
        Log.i("wyltest", "finalQuery = " + finalQuery);
        com.liskovsoft.sharedutils.mylogger.Log.d(TAG, "Start search for '%s'", finalQuery);

        baseSearchText = cleanText;

        long generation = ++searchGeneration;

        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }

        if (continueGroupDisposable != null && !continueGroupDisposable.isDisposed()) {
            continueGroupDisposable.dispose();
        }

        currentSearchVideoGroup = null;
        continueGroupLoading = false;
        continueGroupFinished = false;

        currentVideos.clear();
        loadedVideoKeys.clear();
        currentIndex = -1;

        if (ktvAdapter != null) {
            ktvAdapter.updateData(Collections.emptyList());
        }

        // 搜索接口开始请求，此时列表为空，显示加载提示。
        showSearchLoading(true);

//        updateNowPlaying(null);
        updateNextTitle();

        ContentService contentService = getContentService();
        if (contentService == null) {
            Toast.makeText(this, "ContentService is null", Toast.LENGTH_SHORT).show();
            return;
        }

        searchDisposable = contentService
                .getSearchObserve(
                        finalQuery,
                        mUploadDateOptions | mDurationOptions | mTypeOptions | mFeatureOptions | mSortingOptions
                )
                .subscribe(
                        mediaGroups -> {
                            ArrayList<Video> batch = new ArrayList<>();
                            final VideoGroup[] firstSearchGroup = new VideoGroup[1];

                            Log.i("wyltest", "mediaGroups size = "
                                    + (mediaGroups == null ? -1 : mediaGroups.size()));

                            if (mediaGroups != null) {
                                for (MediaGroup mediaGroup : mediaGroups) {
                                    VideoGroup group = VideoGroup.from(mediaGroup);

                                    if (group == null || group.getVideos() == null) {
                                        continue;
                                    }

                                    Log.i("wyltest", "video size = " + group.getVideos().size());

                                    if (firstSearchGroup[0] == null && !group.getVideos().isEmpty()) {
                                        firstSearchGroup[0] = group;
                                    }

                                    batch.addAll(group.getVideos());
                                }
                            }

                            runOnUiThread(() -> {
                                Log.i("wyltest", "generation = " + generation
                                        + ",searchGeneration = " + searchGeneration);

                                if (generation != searchGeneration) {
                                    return;
                                }

                                if (currentSearchVideoGroup == null && firstSearchGroup[0] != null) {
                                    currentSearchVideoGroup = firstSearchGroup[0];
                                }

                                Log.i("wyltest", "batch size = " + batch.size());
                                addSearchResults(batch);
                            });
                        },
                        error -> runOnUiThread(() -> {
                            if (generation != searchGeneration) {
                                return;
                            }

                            showSearchLoading(false);

                            com.liskovsoft.sharedutils.mylogger.Log.e(
                                    TAG,
                                    "loadSearchData error: %s",
                                    error != null ? error.getMessage() : "unknown"
                            );

                            Toast.makeText(
                                    this,
                                    "Search failed: " + (error != null ? error.getMessage() : "unknown"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }),
                        () -> runOnUiThread(() -> {
                            if (generation != searchGeneration) {
                                return;
                            }

                            showSearchLoading(false);

                            com.liskovsoft.sharedutils.mylogger.Log.d(TAG, "Search complete for '%s'", finalQuery);
                        })
                );
    }

    /** 根据当前搜索分类，把基础搜索词拼成最终查询词。 */
    private String buildSearchQuery(String base) {
        switch (searchMode) {
            case KARAOKE:
                return base + " karaoke";
            case OFFICIAL:
                return base + " official music video";
            case REMIX:
                return base + " remix";
            case DJ:
                return base + " dj mix";
            case ALL:
            default:
                return base + " song";
        }
    }

    /** 把搜索接口返回的视频追加到搜索列表，并按 videoId 去重。 */
    private void addSearchResults(List<Video> videos) {
        Log.i("wyltest", "addSearchResults input size = "
                + (videos == null ? -1 : videos.size())
                + ", adapter = " + ktvAdapter);
        if (videos == null || videos.isEmpty() || ktvAdapter == null) {
            return;
        }

        ArrayList<Video> newVideos = new ArrayList<>();

        for (Video video : videos) {
            if (video == null) continue;

            String key = getVideoKey(video);
            Log.i("wyltest",
                    "video title = " + safeTitle(video)
                            + ", key empty = " + TextUtils.isEmpty(key));
            if (TextUtils.isEmpty(key)) continue;

            if (loadedVideoKeys.add(key)) {
                newVideos.add(video);
            }
        }
        Log.i("wyltest", "addSearchResults newVideos size = " + newVideos.size());

        if (newVideos.isEmpty()) {
            if (continueGroupLoading) {
                continueGroupFinished = true;
            }
            return;
        }

        if (listMode != ListMode.SEARCH) {
            return;
        }
        currentVideos.addAll(newVideos);
        lastSearchVideos.addAll(newVideos);
        // 第一批有效搜索结果到达后，隐藏“正在搜索歌曲...”。
        showSearchLoading(false);

        ktvAdapter.addVideos(newVideos);
        Log.i("wyltest", "adapter count after add = " + ktvAdapter.getItemCount());
        updateNextTitle();

        if (currentVideos.size() == newVideos.size()) {
            requestFirstSongFocus();
            hideKeyboard();
        }

        // 搜索结果刷新后，再尝试启动 Playlist 自动播放。
        // 如果 Playlist 中已有歌曲，会从保存的 VideoSpec 中恢复播放队列。
        tryStartPlaylistPlaybackIfNeeded();
    }
    /** 读取某个列表对应的 VideoSpec JSON 根对象。 */
    private JSONObject getVideoSpecRoot(String prefKey) {
        String json = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getString(prefKey + KEY_VIDEO_SPEC_SUFFIX, "");

        if (TextUtils.isEmpty(json)) {
            return new JSONObject();
        }

        try {
            return new JSONObject(json);
        } catch (Exception e) {
            logPlaylist("getVideoSpecRoot parse error prefKey=" + prefKey
                    + ", error=" + e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * 保存 Video 快照。
     *
     * key 是 videoId，value 是 video.toString()。
     * 这样 App 重启后还能通过 Video.fromString() 恢复标题、封面、作者等信息。
     */
    private void saveVideoSpec(String prefKey, String key, Video video) {
        if (TextUtils.isEmpty(prefKey) || TextUtils.isEmpty(key) || video == null) {
            return;
        }

        try {
            JSONObject root = getVideoSpecRoot(prefKey);

            // Video 自己已经实现了 toString/fromString，别再手搓字段。
            root.put(key, video.toString());

            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(prefKey + KEY_VIDEO_SPEC_SUFFIX, root.toString())
                    .apply();

            logPlaylist("saveVideoSpec OK prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", video=" + videoDebug(video));
        } catch (Exception e) {
            logPlaylist("saveVideoSpec ERROR prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", error=" + e.getMessage());
        }
    }

    /**
     * 从 VideoSpec 中恢复 Video。
     *
     * 如果没有完整快照，则用 Video.from(videoId) 恢复一个最小可播放对象。
     */
    private Video restoreVideoFromSpec(String prefKey, String key) {
        if (TextUtils.isEmpty(prefKey) || TextUtils.isEmpty(key)) {
            return null;
        }

        try {
            JSONObject root = getVideoSpecRoot(prefKey);
            String spec = root.optString(key, "");

            if (TextUtils.isEmpty(spec)) {
                logPlaylist("restoreVideoFromSpec MISS prefKey=" + prefKey
                        + ", key=" + shortKey(key));

                // 新 key 就是 videoId。没有完整 spec 时，至少恢复一个可播放的轻量 Video。
                Video video = Video.from(key);

                logPlaylist("restoreVideoFromSpec fallback Video.from(videoId) key="
                        + shortKey(key));

                return video;
            }

            Video video = Video.fromString(spec);

            if (video == null || TextUtils.isEmpty(video.videoId)) {
                logPlaylist("restoreVideoFromSpec FAIL Video.fromString invalid, key="
                        + shortKey(key));
                return null;
            }

            logPlaylist("restoreVideoFromSpec OK prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", video=" + videoDebug(video));

            return video;
        } catch (Exception e) {
            logPlaylist("restoreVideoFromSpec ERROR prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", error=" + e.getMessage());
            return null;
        }
    }

    /** 删除某个 videoId 对应的 VideoSpec 快照。 */
    private void removeVideoSpec(String prefKey, String key) {
        if (TextUtils.isEmpty(prefKey) || TextUtils.isEmpty(key)) {
            return;
        }

        try {
            JSONObject root = getVideoSpecRoot(prefKey);
            root.remove(key);

            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(prefKey + KEY_VIDEO_SPEC_SUFFIX, root.toString())
                    .apply();

            logPlaylist("removeVideoSpec OK prefKey=" + prefKey
                    + ", key=" + shortKey(key));
        } catch (Exception e) {
            logPlaylist("removeVideoSpec ERROR prefKey=" + prefKey
                    + ", key=" + shortKey(key)
                    + ", error=" + e.getMessage());
        }
    }

    /** 从 Playlist / Favorites 页面返回最近一次搜索结果页。 */
    private void returnToSearchResults() {
        listMode = ListMode.SEARCH;

        // 关键：从 Playlist/Favorites 返回搜索页时，清掉空状态浮层。
        showEmptyListHint(false);
        showSearchLoading(false);

        updateSavedButtonState();

        if (songResultList != null) {
            songResultList.setLayoutManager(new GridLayoutManager(this, 4));

            if (ktvAdapter != null) {
                songResultList.setAdapter(ktvAdapter);
            }
        }

        currentVideos.clear();
        currentVideos.addAll(lastSearchVideos);

        currentIndex = findVideoIndexByKey(currentPlayingVideoKey, currentVideos);

        if (ktvAdapter != null) {
            ktvAdapter.updateData(currentVideos);
        }

        updateNextTitle();

        if (!currentVideos.isEmpty()) {
            requestFirstSongFocus();
        } else {
            loadMySearchResult(baseSearchText);
        }
    }
    /**
     * 控制视频播放窗口里的加载提示。
     *
     * show=true：
     * 用户已经选择播放，但播放器还没有准备好媒体内容时显示。
     *
     * show=false：
     * 播放器已经加载到媒体、停止播放、销毁播放器时隐藏。
     */
    private void showKtvPlayerLoading(boolean show) {
        ktvPlayerLoading = show;
        uiHandler.removeCallbacks(ktvPlayerLoadingRunnable);

        if (ktvPlayerLoadingOverlay == null) {
            return;
        }

        if (!show && ktvBufferingHintShown) {
            return;
        }

        ktvPlayerLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            ktvPlayerLoadingStartMs = SystemClock.elapsedRealtime();

            if (tvKtvPlayerLoading != null) {
                tvKtvPlayerLoading.setText("Loading songs…");
            }

            bringKtvPlayerLoadingOverlayToFront();
            uiHandler.postDelayed(ktvPlayerLoadingRunnable, 300);
        }
    }

    /**
     * 把播放器 loading 层放到播放器 View 上面。
     *
     * 因为 ktvEmbedPlayer 是代码里动态 addView 到 ktvVideoContainer 的，
     * 它可能盖住 XML 里写好的 loading 层，所以显示 loading 时必须 bringToFront。
     */
    private void bringKtvPlayerLoadingOverlayToFront() {
        if (ktvPlayerLoadingOverlay != null) {
            ktvPlayerLoadingOverlay.bringToFront();
        }

        // 全屏按钮要继续压在最上面，不然 loading 层会挡住右下角按钮。
        bringVideoFullscreenButtonToFront();
    }

    /**
     * 轮询播放器状态。
     *
     * EmbedPlayerView 没有直接暴露“视频首帧已显示”的回调，
     * 所以这里用 containsMedia / duration / isPlaying 做一个轻量判断。
     */
    private void checkKtvPlayerLoadingReady() {
        if (!ktvPlayerLoading) {
            return;
        }

        boolean ready = false;

        try {
            ready = ktvEmbedPlayer != null
                    && ktvEmbedPlayer.isEngineInitialized()
                    && ktvEmbedPlayer.containsMedia()
                    && ktvEmbedPlayer.getDurationMs() > 0;
        } catch (Exception e) {
            ready = false;
        }

        if (ready) {
            showKtvPlayerLoading(false);
            return;
        }

        long loadingMs = SystemClock.elapsedRealtime() - ktvPlayerLoadingStartMs;

        if (loadingMs > KTV_PLAYER_LOADING_SLOW_MS && tvKtvPlayerLoading != null) {
            tvKtvPlayerLoading.setText("Songs are loading slowly, please wait...");
        }

        uiHandler.postDelayed(ktvPlayerLoadingRunnable, 500);
    }
    /** 在指定列表中查找某个 videoId 的位置。找不到返回 -1。 */
    private int findVideoIndexByKey(String videoKey, List<Video> videos) {
        if (TextUtils.isEmpty(videoKey) || videos == null) {
            return -1;
        }

        for (int i = 0; i < videos.size(); i++) {
            if (videoKey.equals(getVideoKey(videos.get(i)))) {
                return i;
            }
        }

        return -1;
    }
    /**
     * 播放一首非 Playlist 队列歌曲。
     *
     * 例如搜索结果中点击“立即播放”时会调用这里。
     * 如果 Playlist 中已有歌曲，会设置 startPlaylistAfterCurrentVideo，
     * 让当前歌播完后再接入 Playlist。
     */
    private void playVideo(Video video, int position) {
        if (video == null) return;

        playlistPlaybackActive = false;
        playlistPlaybackIndex = -1;
        playlistPlaybackQueue.clear();
        uiHandler.removeCallbacks(playlistAutoNextRunnable);

        // 关键：这是用户手动立即播放，不是 Playlist 自动播放。
        currentPlaybackFromPlaylist = false;

        currentIndex = position;
        currentPlayingVideoKey = getVideoKey(video);
        paused = false;
        ktvPlayerPaused = false;

        lastPlaylistPositionMs = -1;
        lastPlaylistDurationMs = -1;
        lastPlaylistNearEnd = false;

        updateNowPlaying(video);
        updatePauseButton();

        if (playlistAdapter != null) {
            playlistAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        if (favoritesAdapter != null) {
            favoritesAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        createAndStartKtvPlayer(video);

        startPlaylistAfterCurrentVideo = !getSavedVideoKeyList(KEY_PLAYLIST).isEmpty();

        logPlaylist("playVideo"
                + ", video=" + videoDebug(video)
                + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                + ", playlistSize=" + getSavedVideoKeyList(KEY_PLAYLIST).size());

        updateNextTitle();

        if (startPlaylistAfterCurrentVideo) {
            startPlaylistEndWatcher();
        }
    }

    /** 创建或复用 EmbedPlayerView，并打开指定视频开始播放。 */
    /**
     * 播放指定视频。
     *
     * 注意：
     * 播放器 View 由 prepareKtvPlayerIfNeeded() 负责提前创建，
     * 这里不再第一次播放时 addView，避免页面闪烁。
     */
    private void createAndStartKtvPlayer(Video video) {
        createAndStartKtvPlayer(video, false);
    }

    private void createAndStartKtvPlayer(Video video, boolean fromStart) {
        if (video == null) {
            return;
        }

        prepareKtvPlayerIfNeeded();

        ktvPlayingVideo = video;

        showKtvPlayerLoading(true);
        ktvEmbedPlayer.openVideoFromStart(video);

        startKtvBufferMonitor();
        if (fromStart) {
            ktvEmbedPlayer.openVideoFromStart(video);
        } else {
            ktvEmbedPlayer.openVideo(video);
        }

        ktvPlayerPaused = false;
        updatePauseButton();
    }
    /** 确保视频区域上方存在透明手势层，用于长按菜单和触摸转发。 */
    private void ensureKtvVideoGestureLayer() {
        if (ktvVideoContainer == null) {
            return;
        }

        if (ktvVideoGestureDetector == null) {
            ktvVideoGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    showKtvPlayerMenuSafely(ktvVideoContainer);
                }
            });
        }

        if (ktvVideoGestureLayer == null) {
            ktvVideoGestureLayer = new View(this);
            ktvVideoGestureLayer.setBackgroundColor(Color.TRANSPARENT);
            ktvVideoGestureLayer.setFocusable(false);
            ktvVideoGestureLayer.setFocusableInTouchMode(false);
            ktvVideoGestureLayer.setClickable(true);
            ktvVideoGestureLayer.setLongClickable(true);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );

            ktvVideoContainer.addView(ktvVideoGestureLayer, lp);
        }

        ktvVideoGestureLayer.setOnTouchListener((v, event) -> {
            if (ktvVideoGestureDetector != null) {
                ktvVideoGestureDetector.onTouchEvent(event);
            }

            forwardTouchToKtvEmbedPlayer(event);

            // 返回 true：手势层持续收到 MOVE/UP/LONG_PRESS。
            // 同时我们手动转发给播放器，所以普通点击仍然能到播放器。
            return true;
        });

        ktvVideoGestureLayer.bringToFront();
        bringKtvPlayerLoadingOverlayToFront();
        bringVideoFullscreenButtonToFront();

        //如果你发现 loading 层盖住了手势层，那就用这个顺序：
//        ktvVideoGestureLayer.bringToFront();
//        ktvPlayerLoadingOverlay.bringToFront();
//        bringVideoFullscreenButtonToFront();
    }
    /** 把手势层收到的触摸事件转发给真实播放器，避免控制器无法响应点击。 */
    private void forwardTouchToKtvEmbedPlayer(MotionEvent event) {
        if (ktvEmbedPlayer == null || event == null) {
            return;
        }

        try {
            MotionEvent copy = MotionEvent.obtain(event);
            ktvEmbedPlayer.dispatchTouchEvent(copy);
            copy.recycle();
        } catch (Exception ignored) {
        }
    }
    /** 安全显示播放器菜单，带防抖处理。 */
    private void showKtvPlayerMenuSafely(View anchor) {
        long now = SystemClock.uptimeMillis();

        if (now - lastKtvPlayerMenuShownMs < 800) {
            return;
        }

        lastKtvPlayerMenuShownMs = now;

        if (anchor == null) {
            anchor = ktvVideoContainer;
        }

        showKtvPlayerMenu(anchor);
    }
    /** 初始化视频容器点击和长按事件。 */
    private void setupKtvVideoContainerActions() {
        if (ktvVideoContainer == null) return;

        ktvVideoContainer.setClickable(true);
        ktvVideoContainer.setLongClickable(true);
        ktvVideoContainer.setFocusable(true);
        ktvVideoContainer.setFocusableInTouchMode(false);

        ktvVideoContainer.setOnClickListener(v -> {
            // 点击视频区域，尝试让内置 controller 显示
            if (ktvEmbedPlayer != null) {
                ktvEmbedPlayer.performClick();
            }
        });

        ktvVideoContainer.setOnLongClickListener(v -> {
            showKtvPlayerMenu(v);
            return true;
        });
    }
    /** 显示播放器质量 / 播放暂停菜单。 */
    private void showKtvPlayerMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(0, 1, 0, "Quality: Low");
        popupMenu.getMenu().add(0, 2, 1, "Quality: Normal");
        popupMenu.getMenu().add(0, 3, 2, "Quality: High");
        popupMenu.getMenu().add(0, 4, 3, ktvPlayerPaused ? "Play" : "Pause");

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    setKtvPlayerQuality(EmbedPlayerView.QUALITY_LOW);
                    return true;

                case 2:
                    setKtvPlayerQuality(EmbedPlayerView.QUALITY_NORMAL);
                    return true;

                case 3:
                    setKtvPlayerQuality(EmbedPlayerView.QUALITY_HIGH);
                    return true;

                case 4:
                    togglePause();
                    return true;

                default:
                    return false;
            }
        });

        popupMenu.show();
    }
    /** 设置播放器清晰度，并重新打开当前视频使设置生效。 */
    private void setKtvPlayerQuality(int quality) {
        if (ktvEmbedPlayer == null) {
            Toast.makeText(this, "No video playing", Toast.LENGTH_SHORT).show();
            return;
        }

        ktvEmbedPlayer.setQuality(quality);

        // 有些播放器 setQuality 后需要重新 open 当前视频才生效
        if (ktvPlayingVideo != null) {
            ktvEmbedPlayer.openVideo(ktvPlayingVideo);
        }

        Toast.makeText(this, "Quality changed", Toast.LENGTH_SHORT).show();
    }
    /**
     * 停止播放器。
     *
     * @param removeView true 时同时从容器中移除 EmbedPlayerView；false 只释放播放状态。
     */
    private void stopKtvPlayer(boolean removeView) {
        stopKtvBufferMonitor();

        showKtvPlayerLoading(false);
        if (ktvEmbedPlayer == null) {
            return;
        }

        try {
            ktvEmbedPlayer.finish();
        } catch (Exception ignored) {
        }

        if (removeView && ktvVideoContainer != null) {
            try {
                if (ktvVideoGestureLayer != null) {
                    ktvVideoContainer.removeView(ktvVideoGestureLayer);
                }
            } catch (Exception ignored) {
            }

            ktvVideoGestureLayer = null;
            ktvVideoGestureDetector = null;

            try {
                ktvVideoContainer.removeView(ktvEmbedPlayer);
            } catch (Exception ignored) {
            }

            ktvEmbedPlayer = null;
        }

        ktvPlayerPaused = true;
        updatePauseButton();
    }

    /**
     * 重播当前正在播放的歌曲。
     *
     * 注意：
     * Playlist 只有一首歌时，用户点 Next 会把这首歌从 Playlist 删除，
     * 但播放器里仍然可能还在播放这首歌。
     *
     * 所以这里不能只依赖 getCurrentVideo()。
     * getCurrentVideo() 可能因为 currentIndex=-1 或 Playlist 已空而返回 null。
     * 这种情况下要回退使用 ktvPlayingVideo。
     */
    private void replayCurrent() {
        Video video = getCurrentVideo();

        if (video == null) {
            video = ktvPlayingVideo;
        }

        if (video == null || TextUtils.isEmpty(video.videoId)) {
            Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show();
            return;
        }

        int replayIndex = findVideoIndexByKey(getVideoKey(video), currentVideos);

        if (replayIndex < 0) {
            replayIndex = currentIndex;
        }

        stopKtvPlayer(true);

        currentPlayingVideoKey = getVideoKey(video);
        currentIndex = replayIndex;

        paused = false;
        ktvPlayerPaused = false;

        updateNowPlaying(video);
        updateNextTitle();
        updatePauseButton();

        if (playlistAdapter != null) {
            playlistAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        if (favoritesAdapter != null) {
            favoritesAdapter.setCurrentPlayingKey(currentPlayingVideoKey);
        }

        createAndStartKtvPlayer(video, true);
    }

    /** 底部 Next 按钮逻辑。Playlist 自动播放中则切队列下一首，否则播放 currentVideos 下一项。 */
    /**
     * 点击底部 Next 按钮。
     *
     * 规则：
     * 1. 如果 Playlist 正在自动播放，Next 播放 Playlist 队列下一首。
     * 2. 如果当前是 Play Now 播放的搜索歌曲，并且 Playlist 有歌，Next 立刻切入 Playlist。
     * 3. 如果 Playlist 没有歌，才播放当前搜索列表 / 收藏列表里的下一首。
     */
    /**
     * 点击底部 Next 按钮。
     *
     * 规则：
     * 1. 如果 Playlist 正在播放，Next 会跳到 Playlist 下一首，并删除当前 Playlist 歌曲。
     * 2. 如果当前是 Play Now 播放的搜索歌曲，但这首歌也在 Playlist 中，点击 Next 时也先从 Playlist 删除它。
     * 3. 如果 Playlist 有歌，Next 优先切入 Playlist。
     * 4. Playlist 为空时，才走当前搜索列表 / 收藏列表的下一首。
     */
    private void playNextVideo() {
        // Playlist 正在播放：手动 Next 也删除当前 Playlist 歌曲。
        if (playlistPlaybackActive) {
            playNextPlaylistQueueVideo(true);
            updateNextTitle();
            return;
        }

        Video currentVideo = getCurrentVideo();

        // 当前不是 Playlist 播放，但当前歌曲如果也存在于 Playlist 中，
        // 用户点击 Next 时也按“跳过当前歌”处理：先从 Playlist 删除。
        if (currentVideo != null && isVideoSaved(KEY_PLAYLIST, currentVideo)) {
            logPlaylist("playNextVideo REMOVE current video from playlist before manual next"
                    + ", current=" + videoDebug(currentVideo));

            removePlayedVideoFromPlaylistSilently(currentVideo);
        }

        ArrayList<String> playlistKeys = getSavedVideoKeyList(KEY_PLAYLIST);

        // 当前不是 Playlist 播放，但 Playlist 里还有歌：
        // 用户点 Next 时，立刻切入 Playlist 第一首。
        if (!playlistKeys.isEmpty()) {
            logPlaylist("playNextVideo START playlist manually"
                    + ", playlistSize=" + playlistKeys.size()
                    + ", current=" + videoDebug(currentVideo));

            startPlaylistAfterCurrentVideo = false;

            lastPlaylistPositionMs = -1;
            lastPlaylistDurationMs = -1;
            lastPlaylistNearEnd = false;

            stopKtvPlayer(true);

            boolean started = tryStartPlaylistPlaybackIfNeeded(true);

            if (!started) {
                Toast.makeText(this, "Playlist has no playable songs", Toast.LENGTH_SHORT).show();
                updateNextTitle();
            }

            return;
        }

        // Playlist 为空，才按当前页面列表播放下一首。
        if (currentVideos.isEmpty()) {
            Toast.makeText(this, "No songs", Toast.LENGTH_SHORT).show();
            return;
        }

        int nextIndex = currentIndex + 1;

        if (nextIndex < 0) {
            nextIndex = 0;
        }

        if (nextIndex >= currentVideos.size()) {
            Toast.makeText(this, "Already last song", Toast.LENGTH_SHORT).show();
            return;
        }

        stopKtvPlayer(true);
        playVideo(currentVideos.get(nextIndex), nextIndex);

        if (songResultList != null) {
            songResultList.smoothScrollToPosition(nextIndex);
        }
    }



    /** 播放 / 暂停切换。如果播放器尚未创建，会尝试播放当前歌曲。 */
    private void togglePause() {
        if (ktvEmbedPlayer == null) {
            Video video = getCurrentVideo();

            if (video == null && !currentVideos.isEmpty()) {
                int index = currentIndex >= 0 ? currentIndex : 0;
                playVideo(currentVideos.get(index), index);
            } else if (video != null) {
                playVideo(video, currentIndex);
            } else {
                Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show();
            }

            return;
        }

        if (ktvPlayerPaused) {
            resumeKtvPlayer();
        } else {
            pauseKtvPlayer();
        }
    }
    /** 暂停播放器并停止自动下一首轮询。 */
    private void pauseKtvPlayer() {
        if (ktvEmbedPlayer == null
                || !ktvEmbedPlayer.isEngineInitialized()
                || !ktvEmbedPlayer.containsMedia()) {
            ktvPlayerPaused = true;
            paused = true;
            updatePauseButton();
            uiHandler.removeCallbacks(playlistAutoNextRunnable);
            return;
        }

        ktvEmbedPlayer.setPlayWhenReady(false);

        ktvPlayerPaused = true;
        paused = true;

        updatePauseButton();
        uiHandler.removeCallbacks(playlistAutoNextRunnable);
    }

    /** 恢复播放器播放，并在 Playlist 模式下重新启动结束监听。 */
    private void resumeKtvPlayer() {
        if (ktvEmbedPlayer == null) {
            if (ktvPlayingVideo != null) {
                createAndStartKtvPlayer(ktvPlayingVideo);
            }
            return;
        }

        if (!ktvEmbedPlayer.isEngineInitialized() || !ktvEmbedPlayer.containsMedia()) {
            if (ktvPlayingVideo != null) {
                createAndStartKtvPlayer(ktvPlayingVideo);
            }
            return;
        }

        ktvEmbedPlayer.setPlayWhenReady(true);

        ktvPlayerPaused = false;
        paused = false;

        updatePauseButton();

        if (playlistPlaybackActive) {
            startPlaylistEndWatcher();
        }
    }


    /** 根据暂停状态更新底部 Play / Pause 按钮文案和图标。 */
    private void updatePauseButton() {
        if (btnPause == null) return;

        if (ktvPlayerPaused || paused) {
            btnPause.setText("Play");
            btnPause.setIconResource(R.drawable.ic_play);
        } else {
            btnPause.setText("Pause");
            btnPause.setIconResource(R.drawable.ic_pause);
        }
    }

    /**
     * 更新 Now Playing 标题。
     *
     * 效果：
     * 1. “Now Playing” 文字加紫色背景。
     * 2. “Now Playing” 字体为白色。
     * 3. 后面的歌曲标题保持 TextView 原本颜色。
     * 4. 超长时由 XML 的 ellipsize="end" 自动显示 ...
     */
    private void updateNowPlaying(Video video) {
        if (tvNowPlayingTitle == null) {
            return;
        }

        String label = "Now Playing";
        String title = video == null ? "No song selected" : safeTitle(video);
        String text = label + ": " + title;

        SpannableString spannable = new SpannableString(text);

        int labelStart = 0;
        int labelEnd = label.length();

        // 紫色背景。这里用你项目里的 primary_purple。
        spannable.setSpan(
                new BackgroundColorSpan(getColorCompat(R.color.primary_purple)),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // “Now Playing” 字体白色。
        spannable.setSpan(
                new ForegroundColorSpan(Color.WHITE),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // “Now Playing” 加粗。
        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvNowPlayingTitle.setText(spannable);
    }

    /**
     * 更新 Next 标题。
     *
     * 效果：
     * 1. “Next” 文字为紫色。
     * 2. “Next” 文字加粗。
     * 3. 不给 “Next” 添加背景。
     * 4. 后面的歌曲标题保持 TextView 原本颜色。
     * 5. 没有下一首时清空。
     */
    private void updateNextTitle() {
        if (tvNextTitle == null) {
            return;
        }

        Video nextVideo = getNextPlaylistVideoForTitle();

        if (nextVideo == null) {
            tvNextTitle.setText("");
            return;
        }

        String label = "Next";
        String text = label + ": " + safeTitle(nextVideo);

        SpannableString spannable = new SpannableString(text);

        int labelStart = 0;
        int labelEnd = label.length();

        // “Next” 紫色。
        spannable.setSpan(
                new ForegroundColorSpan(getColorCompat(R.color.primary_purple)),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // “Next” 加粗。
        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvNextTitle.setText(spannable);
    }
    /**
     * 兼容不同 Android 版本的颜色获取方法。
     */
    private int getColorCompat(int colorResId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return getColor(colorResId);
        }

        return getResources().getColor(colorResId);
    }
    /** 计算 NEXT 应该显示哪一首 Playlist 歌曲。 */
    private Video getNextPlaylistVideoForTitle() {
        // 正在播放 Playlist 队列时，从播放队列里取下一首。
        if (playlistPlaybackActive && !playlistPlaybackQueue.isEmpty()) {
            int nextIndex = playlistPlaybackIndex + 1;

            if (nextIndex >= 0 && nextIndex < playlistPlaybackQueue.size()) {
                return playlistPlaybackQueue.get(nextIndex);
            }

            return null;
        }

        ArrayList<Video> playlistVideos = getSavedVideosFromCache(KEY_PLAYLIST);

        if (playlistVideos.isEmpty()) {
            return null;
        }

        // 关键：
        // 当前播放的是 Play Now 歌曲，且已经设置“播完后接 Playlist”，
        // 那 NEXT 应该显示 Playlist 第一首。
        if (startPlaylistAfterCurrentVideo && isImmediateSongPlayingOrLoaded()) {
            return playlistVideos.get(0);
        }

        // 当前没有播放任何歌，就显示 Playlist 第一首。
        if (TextUtils.isEmpty(currentPlayingVideoKey)) {
            return playlistVideos.get(0);
        }

        // 如果当前播放的歌刚好在 Playlist 里，就显示它后面一首。
        for (int i = 0; i < playlistVideos.size(); i++) {
            Video video = playlistVideos.get(i);

            if (isVideoKeyMatchCurrentPlaying(video)) {
                int nextIndex = i + 1;

                if (nextIndex >= 0 && nextIndex < playlistVideos.size()) {
                    return playlistVideos.get(nextIndex);
                }

                return null;
            }
        }

        // 当前播放的不是 Playlist 里的歌，也显示 Playlist 第一首。
        return playlistVideos.get(0);
    }

    private Video getNextPlaylistVideoForTitle2() {
        if (playlistPlaybackActive && !playlistPlaybackQueue.isEmpty()) {
            int nextIndex = playlistPlaybackIndex + 1;

            if (nextIndex >= 0 && nextIndex < playlistPlaybackQueue.size()) {
                return playlistPlaybackQueue.get(nextIndex);
            }

            return null;
        }

        ArrayList<Video> playlistVideos = getSavedVideosFromCache(KEY_PLAYLIST);

        if (playlistVideos.isEmpty()) {
            return null;
        }

        return playlistVideos.get(0);
    }
    private boolean isVideoKeyMatchCurrentPlaying(Video video) {
        return videoMatchesSavedKey(video, currentPlayingVideoKey);
    }
    /** 获取当前正在播放或当前选中的 Video。Playlist 自动播放时优先从队列读取。 */
    private Video getCurrentVideo() {
        if (playlistPlaybackActive
                && playlistPlaybackIndex >= 0
                && playlistPlaybackIndex < playlistPlaybackQueue.size()) {
            return playlistPlaybackQueue.get(playlistPlaybackIndex);
        }

        if (currentIndex >= 0 && currentIndex < currentVideos.size()) {
            return currentVideos.get(currentIndex);
        }

        return null;
    }

    /** 读取系统媒体音量并转换成 0-100 百分比。 */
    private int getCurrentVolumePercent() {
        if (audioManager == null) return 50;

        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (max <= 0) return 50;

        return Math.round(current * 100f / max);
    }
    /** 启动 Playlist 播放结束轮询任务。 */
    private void startPlaylistEndWatcher() {
        uiHandler.removeCallbacks(playlistAutoNextRunnable);

        if (playlistPlaybackActive
                || startPlaylistAfterCurrentVideo
                || clearNowPlayingWhenCurrentVideoEnds) {
            uiHandler.postDelayed(playlistAutoNextRunnable, 2500);
        }
    }

    /**
     * 定时检查当前歌曲是否接近播放结束。
     *
     * 如果 Playlist 正在播放，当前歌结束后删除并播放下一首；
     * 如果是搜索歌播放完成且 startPlaylistAfterCurrentVideo=true，则切入 Playlist。
     */
    private void checkPlaylistAutoNext() {
        logPlayerStateThrottled("checkPlaylistAutoNext");

        if (!playlistPlaybackActive
                && !startPlaylistAfterCurrentVideo
                && !clearNowPlayingWhenCurrentVideoEnds) {
            logPlaylist("checkPlaylistAutoNext RETURN: no playlist active, no handoff, no pending clear title");
            return;
        }

        if (ktvPlayerPaused || paused) {
            logPlaylist("checkPlaylistAutoNext RETURN: paused. ktvPlayerPaused="
                    + ktvPlayerPaused + ", paused=" + paused);
            return;
        }

        if (ktvEmbedPlayer == null) {
            logPlaylist("checkPlaylistAutoNext player=null"
                    + ", playlistPlaybackActive=" + playlistPlaybackActive
                    + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                    + ", lastNearEnd=" + lastPlaylistNearEnd
                    + ", lastPositionMs=" + lastPlaylistPositionMs
                    + ", lastDurationMs=" + lastPlaylistDurationMs);

            if (lastPlaylistNearEnd) {
                handleCurrentVideoCompletedForPlaylistFlow();
                return;
            }

            uiHandler.postDelayed(playlistAutoNextRunnable, 1500);
            return;
        }

        boolean engineInitialized = false;
        boolean containsMedia = false;

        try {
            engineInitialized = ktvEmbedPlayer.isEngineInitialized();
            containsMedia = ktvEmbedPlayer.containsMedia();
        } catch (Exception e) {
            logPlaylist("checkPlaylistAutoNext read engine state error=" + e.getMessage());
        }

        if (!engineInitialized || !containsMedia) {
            logPlaylist("checkPlaylistAutoNext engine/media missing"
                    + ", engineInitialized=" + engineInitialized
                    + ", containsMedia=" + containsMedia
                    + ", playlistPlaybackActive=" + playlistPlaybackActive
                    + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                    + ", lastNearEnd=" + lastPlaylistNearEnd
                    + ", lastPositionMs=" + lastPlaylistPositionMs
                    + ", lastDurationMs=" + lastPlaylistDurationMs);

            if (lastPlaylistNearEnd) {
                handleCurrentVideoCompletedForPlaylistFlow();
                return;
            }

            uiHandler.postDelayed(playlistAutoNextRunnable, 1500);
            return;
        }

        boolean ended = isKtvPlayerProbablyEnded();

        logPlaylist("checkPlaylistAutoNext ended=" + ended
                + ", playlistPlaybackActive=" + playlistPlaybackActive
                + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                + ", index=" + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size());

        if (ended) {
            handleCurrentVideoCompletedForPlaylistFlow();
            return;
        }

        uiHandler.postDelayed(playlistAutoNextRunnable, 1500);
    }
    /** 根据当前状态处理“当前歌曲已完成”的后续流程。 */
    private void handleCurrentVideoCompletedForPlaylistFlow() {
        logPlaylist("handleCurrentVideoCompletedForPlaylistFlow ENTER"
                + ", playlistPlaybackActive=" + playlistPlaybackActive
                + ", startPlaylistAfterCurrentVideo=" + startPlaylistAfterCurrentVideo
                + ", clearNowPlayingWhenCurrentVideoEnds=" + clearNowPlayingWhenCurrentVideoEnds
                + ", current=" + videoDebug(getCurrentVideo())
                + ", ktvPlayingVideo=" + videoDebug(ktvPlayingVideo));

        // 手动 Next 后 Playlist 已空，但播放器里的最后一首歌刚刚自然结束。
        if (clearNowPlayingWhenCurrentVideoEnds) {
            clearNowPlayingWhenCurrentVideoEnds = false;

            currentIndex = -1;
            currentPlayingVideoKey = "";
            currentPlaybackFromPlaylist = false;
            ktvPlayingVideo = null;

            updateNowPlaying(null);
            updateNextTitle();

            ktvPlayerPaused = true;
            paused = false;
            updatePauseButton();

            if (playlistAdapter != null) {
                playlistAdapter.setCurrentPlayingKey("");
            }

            if (favoritesAdapter != null) {
                favoritesAdapter.setCurrentPlayingKey("");
            }

            uiHandler.removeCallbacks(playlistAutoNextRunnable);

            logPlaylist("handleCurrentVideoCompletedForPlaylistFlow CLEAR now playing after manual next empty playlist");
            return;
        }

        if (playlistPlaybackActive) {
            logPlaylist("handleCurrentVideoCompletedForPlaylistFlow: completed playlist song");
            // true, true:
            // 第一个 true = 离开当前歌前从 Playlist 删除
            // 第二个 true = 当前歌是自然播放完成，如果 Playlist 结束，需要清空 Now Playing 标题
            playNextPlaylistQueueVideo(true, true);
            return;
        }

        if (startPlaylistAfterCurrentVideo) {
            logPlaylist("handleCurrentVideoCompletedForPlaylistFlow: completed non-playlist song, start playlist");

            startPlaylistAfterCurrentVideo = false;

            lastPlaylistPositionMs = -1;
            lastPlaylistDurationMs = -1;
            lastPlaylistNearEnd = false;

            tryStartPlaylistPlaybackIfNeeded(true);
        }
    }

    /**
     * 判断播放器是否大概率已经播放到结尾。
     *
     * 通过 duration - position 的剩余时间判断，而不是依赖播放器 ended 回调。
     */
    private boolean isKtvPlayerProbablyEnded() {
        if (ktvEmbedPlayer == null) {
            logPlaylist("isEnded=false: ktvEmbedPlayer=null");
            return false;
        }

        if (!ktvEmbedPlayer.isEngineInitialized()) {
            logPlaylist("isEnded=false: engine not initialized");
            return false;
        }

        if (!ktvEmbedPlayer.containsMedia()) {
            logPlaylist("isEnded=false: containsMedia=false");
            return false;
        }

        long durationMs = ktvEmbedPlayer.getDurationMs();
        long positionMs = ktvEmbedPlayer.getPositionMs();

        if (durationMs <= 0 || positionMs < 0) {
            logPlaylist("isEnded=false: invalid duration/position. positionMs="
                    + positionMs + ", durationMs=" + durationMs);
            return false;
        }

        long remainMs = durationMs - positionMs;

        boolean nearEnd = remainMs <= 1500;
        boolean loading = ktvEmbedPlayer.isLoading();
        boolean playing = ktvEmbedPlayer.isPlaying();

        lastPlaylistPositionMs = positionMs;
        lastPlaylistDurationMs = durationMs;
        lastPlaylistNearEnd = nearEnd;

        boolean ended = nearEnd && !loading;

        logPlaylist("isEnded check:"
                + " positionMs=" + positionMs
                + ", durationMs=" + durationMs
                + ", remainMs=" + remainMs
                + ", nearEnd=" + nearEnd
                + ", playing=" + playing
                + ", loading=" + loading
                + ", ended=" + ended
                + ", video=" + videoDebug(getCurrentVideo()));

        return ended;
    }


    /** 设置系统媒体音量百分比，并同步 UI。 */
    /**
     * 设置媒体音量百分比。
     *
     * 注意：
     * 这里只作为兼容入口。
     * 最终仍然转换成 Android 系统真实音量档位。
     */
    private void setVolumePercent(int percent) {
        int maxVolume = getSystemMaxVolume();

        percent = Math.max(0, Math.min(100, percent));

        int volume = Math.round(maxVolume * percent / 100f);

        setSystemVolume(volume, true);
    }
    /** 从系统当前音量同步 SeekBar、文字和静音按钮。 */
    private void syncVolumeFromSystem() {
        if (audioManager == null) {
            return;
        }

        int maxVolume = getSystemMaxVolume();
        int currentVolume = getSystemCurrentVolume();

        if (seekbarVolume != null) {
            if (seekbarVolume.getMax() != maxVolume) {
                seekbarVolume.setMax(maxVolume);
            }

            if (seekbarVolume.getProgress() != currentVolume) {
                seekbarVolume.setProgress(currentVolume);
            }
        }

        muted = currentVolume == 0;

        if (currentVolume > 0) {
            lastNonZeroVolumePercent = volumeToPercent(currentVolume, maxVolume);
        }

        updateVolumeLabelFromSystem();
        updateMuteButton();
    }

    /** 更新音量文本，显示百分比。 */
    private void updateVolumeLabelFromSystem() {
        if (tvVolumeValue == null) {
            return;
        }

        tvVolumeValue.setText(getSystemCurrentVolume() + "/" + getSystemMaxVolume());
    }

    /** 兼容旧调用。 */
    private void updateVolumeLabel(int percent) {
        updateVolumeLabelFromSystem();
    }


    /** 切换静音状态。 */
    private void toggleMute() {
        int currentVolume = getSystemCurrentVolume();
        int maxVolume = getSystemMaxVolume();

        if (muted || currentVolume == 0) {
            int restorePercent = lastNonZeroVolumePercent > 0 ? lastNonZeroVolumePercent : 50;
            int restoreVolume = Math.round(maxVolume * restorePercent / 100f);

            if (restoreVolume <= 0) {
                restoreVolume = Math.max(1, maxVolume / 2);
            }

            setSystemVolume(restoreVolume, true);
        } else {
            lastNonZeroVolumePercent = volumeToPercent(currentVolume, maxVolume);
            setSystemVolume(0, true);
        }

        syncVolumeFromSystem();
    }

    private void updateMuteButton() {
        if (btnMute == null) {
            return;
        }

        if (muted || getSystemCurrentVolume() == 0) {
            btnMute.setText("Unmute");
        } else {
            btnMute.setText("Mute");
        }
    }
    /**
     * 将歌曲加入 Playlist。
     *
     * 如果当前正在播放非 Playlist 歌曲，会让该歌曲结束后自动接入 Playlist。
     */
    /**
     * 添加歌曲到 Playlist。
     *
     * 规则：
     * 1. 如果当前正在播放 Play Now 的歌曲，不立刻切歌。
     * 2. 只设置 startPlaylistAfterCurrentVideo=true。
     * 3. 当前歌曲播放完成后，再自动播放 Playlist。
     * 4. 立即更新 Next 标题。
     */
    /**
     * 添加歌曲到 Playlist。
     *
     * 规则：
     * 1. 如果当前正在播放 Play Now 的歌曲，不立刻切歌。
     * 2. 只设置 startPlaylistAfterCurrentVideo=true。
     * 3. 当前歌曲播放完成后，再自动播放 Playlist。
     * 4. 如果当前没有任何歌曲在播放，添加后立即启动 Playlist。
     * 5. 立即更新 Next 标题。
     */
    private void addToPlaylist(Video video) {
        if (video == null) return;

        boolean added = saveVideoKeyToPrefs(KEY_PLAYLIST, video);

        if (added) {
            addVideoToMemoryList(localPlaylist, video);
            Toast.makeText(this, "Added to Playlist", Toast.LENGTH_SHORT).show();

            if (playlistPlaybackActive) {
                appendVideoToPlaybackQueueIfNeeded(video);
            }
        } else {
            Toast.makeText(this, "Already in Playlist", Toast.LENGTH_SHORT).show();
        }

        updateSavedButtonState();
        updateSavedBadgeCounts();

        if (listMode == ListMode.PLAYLIST) {
            refreshPlaylistView();
        } else if (listMode == ListMode.FAVORITES && favoritesAdapter != null) {
            favoritesAdapter.notifyDataSetChanged();
        }

        // 当前正在播放 Play Now 歌曲时，只排队，不切歌。
        if (isImmediateSongPlayingOrLoaded()) {
            startPlaylistAfterCurrentVideo = true;

            logPlaylist("addToPlaylist DEFER playlist until current immediate song completed"
                    + ", added=" + added
                    + ", playlistSize=" + getSavedVideoKeyList(KEY_PLAYLIST).size()
                    + ", current=" + videoDebug(ktvPlayingVideo)
                    + ", next=" + videoDebug(video));

            updateNextTitle();
            startPlaylistEndWatcher();
            return;
        }

        // 当前正在播放 Playlist，追加进运行时队列即可，不打断当前 Playlist 歌曲。
        if (playlistPlaybackActive) {
            updateNextTitle();
            startPlaylistEndWatcher();
            return;
        }

        updateNextTitle();

        // 当前没有任何有效播放任务时，添加歌曲后立即启动 Playlist。
        if (shouldStartPlaylistImmediatelyAfterAdd()) {
            logPlaylist("addToPlaylist START playlist immediately"
                    + ", added=" + added
                    + ", playlistSize=" + getSavedVideoKeyList(KEY_PLAYLIST).size()
                    + ", staleKtvPlayingVideo=" + videoDebug(ktvPlayingVideo));

            tryStartPlaylistPlaybackIfNeeded(true);
        }
    }
    /**
     * 添加歌曲到 Playlist 后，是否应该立刻启动 Playlist 播放。
     *
     * 场景：
     * 1. 页面刚打开，没有歌曲在播。
     * 2. Playlist 已经全部自然播放完成。
     * 3. 当前没有 Play Now 歌曲等待播放完成。
     *
     * 注意：
     * 不要只判断 ktvPlayingVideo == null。
     * 因为播放器结束后，ktvPlayingVideo 可能还残留最后一首歌对象。
     */
    private boolean shouldStartPlaylistImmediatelyAfterAdd() {
        if (playlistPlaybackActive) {
            return false;
        }

        if (startPlaylistAfterCurrentVideo) {
            return false;
        }

        // currentPlayingVideoKey 非空，说明业务上仍然认为有当前歌曲。
        if (!TextUtils.isEmpty(currentPlayingVideoKey)) {
            return false;
        }

        // currentPlayingVideoKey 已空，说明当前没有有效播放任务。
        // 即使 ktvPlayingVideo 残留旧对象，也应该允许启动新 Playlist。
        return true;
    }
    /**
     * 当前是否正在播放一首“手动立即播放”的歌曲。
     *
     * 这种情况下，Playlist 只能排队等待，不能立刻切歌。
     */
    private boolean isImmediateSongPlayingOrLoaded() {
        return !playlistPlaybackActive
                && !currentPlaybackFromPlaylist
                && ktvPlayingVideo != null
                && !TextUtils.isEmpty(currentPlayingVideoKey);
    }
    /** Playlist 正在自动播放时，把新加入的歌曲追加到运行时队列末尾。 */
    private void appendVideoToPlaybackQueueIfNeeded(Video video) {
        if (video == null) return;

        for (Video oldVideo : playlistPlaybackQueue) {
            if (isSameVideoForSavedList(oldVideo, video)) {
                return;
            }
        }

        playlistPlaybackQueue.add(video);

        logPlaylist("appendVideoToPlaybackQueueIfNeeded ADD"
                + ", queueSize=" + playlistPlaybackQueue.size()
                + ", video=" + videoDebug(video));
    }

    /** 将歌曲加入 Favorites，只刷新当前可见页面，不强制跳转 Favorites 页面。 */
    private void addToFavorites(Video video) {
        if (video == null) return;

        if (saveVideoKeyToPrefs(KEY_FAVORITES, video)) {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Already in Favorites", Toast.LENGTH_SHORT).show();
        }

        updateSavedButtonState();
        updateSavedBadgeCounts();

        if (listMode == ListMode.FAVORITES) {
            refreshFavoritesView();
        } else if (listMode == ListMode.PLAYLIST && playlistAdapter != null) {
            playlistAdapter.notifyDataSetChanged();
        } else if (listMode == ListMode.SEARCH && ktvAdapter != null) {
            ktvAdapter.notifyDataSetChanged();
        }
    }

    /** 取消当前搜索请求，避免离开搜索页后仍然回写结果。 */
    private void stopSearchLoading() {
        searchGeneration++;

        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }
    }
    /** 向内存列表追加歌曲，按 videoId 去重。 */
    private void addVideoToMemoryList(ArrayList<Video> list, Video video) {
        if (list == null || video == null) return;

        String key = getVideoKey(video);
        if (TextUtils.isEmpty(key)) return;

        for (Video oldVideo : list) {
            if (key.equals(getVideoKey(oldVideo))) {
                return;
            }
        }

        list.add(video);
    }

    /**
     * 保存歌曲到 Playlist / Favorites。
     *
     * 保存内容包括 videoId 顺序列表，以及 videoId -> Video.toString() 的快照。
     */
    private boolean saveVideoKeyToPrefs(String prefKey, Video video) {
        String videoKey = getVideoKey(video);

        if (TextUtils.isEmpty(videoKey)) {
            logPlaylist("saveVideoKeyToPrefs FAIL empty key, video=" + videoDebug(video));
            return false;
        }
        ArrayList<String> keys = getSavedVideoKeyList(prefKey);

        if (keys.contains(videoKey)) {
            saveVideoSpec(prefKey, videoKey, video);
            logPlaylist("saveVideoKeyToPrefs EXISTS prefKey=" + prefKey
                    + ", key=" + shortKey(videoKey));
            return false;
        }

        keys.add(videoKey);
        writeSavedVideoKeys(prefKey, keys);
        saveVideoSpec(prefKey, videoKey, video);

        logPlaylist("saveVideoKeyToPrefs OK prefKey=" + prefKey
                + ", key=" + shortKey(videoKey)
                + ", size=" + keys.size());

        return true;
    }

    /** 从 Playlist 或 Favorites 中删除歌曲，并刷新对应 UI。 */
    private void removeFromSavedList(String prefKey, Video video) {
        if (video == null) return;

        ArrayList<String> keys = getSavedVideoKeyList(prefKey);
        String removedKey = removeSavedVideoKey(prefKey, keys, video);

        if (TextUtils.isEmpty(removedKey)) {
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show();
            return;
        }

        writeSavedVideoKeys(prefKey, keys);

        if (KEY_PLAYLIST.equals(prefKey)) {
            removeVideoFromMemoryList(localPlaylist, removedKey);

            if (playlistPlaybackActive) {
                rebuildPlaylistPlaybackQueueAfterDelete(removedKey);
            }

            if (listMode == ListMode.PLAYLIST) {
                refreshPlaylistView();
            }
        } else if (KEY_FAVORITES.equals(prefKey)) {
            if (listMode == ListMode.FAVORITES) {
                refreshFavoritesView();
            } else if (listMode == ListMode.PLAYLIST && playlistAdapter != null) {
                playlistAdapter.notifyDataSetChanged();
            }
        }

        updateSavedBadgeCounts();
        if (KEY_PLAYLIST.equals(prefKey)) {
            returnToSearchResultsIfPlaylistEmpty();
        } else if (KEY_FAVORITES.equals(prefKey)) {
            returnToSearchResultsIfFavoritesEmpty();
        }
        logPlaylist("removeFromSavedList DONE prefKey=" + prefKey
                + ", removedKey=" + shortKey(removedKey)
                + ", leftSize=" + keys.size());

        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
    }
    /** 删除 Playlist 歌曲后同步运行时播放队列，避免自动播放继续引用已删除歌曲。 */
    private void rebuildPlaylistPlaybackQueueAfterDelete(String deletedVideoKey) {
        if (TextUtils.isEmpty(deletedVideoKey)) {
            return;
        }

        int oldIndex = playlistPlaybackIndex;

        for (int i = playlistPlaybackQueue.size() - 1; i >= 0; i--) {
            if (videoMatchesSavedKey(playlistPlaybackQueue.get(i), deletedVideoKey)) {
                playlistPlaybackQueue.remove(i);

                if (i < oldIndex) {
                    playlistPlaybackIndex--;
                } else if (i == oldIndex) {
                    playlistPlaybackIndex = Math.min(i, playlistPlaybackQueue.size() - 1);
                }
            }
        }

        if (playlistPlaybackQueue.isEmpty()) {
            playlistPlaybackActive = false;
            playlistPlaybackIndex = -1;
            uiHandler.removeCallbacks(playlistAutoNextRunnable);
        }
    }
    /** 从内存列表中删除指定 videoId 对应的歌曲。 */
    private void removeVideoFromMemoryList(ArrayList<Video> list, String videoKey) {
        if (list == null || TextUtils.isEmpty(videoKey)) return;

        for (int i = list.size() - 1; i >= 0; i--) {
            if (videoMatchesSavedKey(list.get(i), videoKey)) {
                list.remove(i);
            }
        }
    }

    /** 判断指定歌曲是否已经保存在 Playlist / Favorites 中。 */
    private boolean isVideoSaved(String prefKey, Video video) {
        String videoKey = getVideoKey(video);

        if (TextUtils.isEmpty(videoKey)) {
            return false;
        }

        return getSavedVideoKeyList(prefKey).contains(videoKey);
    }

    /** 读取 Playlist / Favorites 中保存的有序 videoId 列表。 */
    private ArrayList<String> getSavedVideoKeyList(String prefKey) {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String ordered = sp.getString(prefKey + KEY_ORDER_SUFFIX, "");

        ArrayList<String> result = new ArrayList<>();

        if (!TextUtils.isEmpty(ordered)) {
            String[] parts = ordered.split(KEY_SEPARATOR, -1);
            for (String part : parts) {
                if (!TextUtils.isEmpty(part) && !result.contains(part)) {
                    result.add(part);
                }
            }
            return result;
        }

        // 兼容你之前用 StringSet 保存的数据
        Set<String> oldSet = sp.getStringSet(prefKey, Collections.emptySet());
        if (oldSet != null) {
            for (String key : oldSet) {
                if (!TextUtils.isEmpty(key) && !result.contains(key)) {
                    result.add(key);
                }
            }
        }

        return result;
    }

    /** 写入 Playlist / Favorites 的有序 videoId 列表。 */
    private void writeSavedVideoKeys(String prefKey, ArrayList<String> keys) {
        if (keys == null) {
            keys = new ArrayList<>();
        }

        logPlaylist("writeSavedVideoKeys ENTER prefKey=" + prefKey
                + ", size=" + keys.size());

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String ordered = TextUtils.join(KEY_SEPARATOR, keys);
        Set<String> keySet = new LinkedHashSet<>(keys);

        sp.edit()
                .putString(prefKey + KEY_ORDER_SUFFIX, ordered)
                .putStringSet(prefKey, keySet)
                .apply();

        ArrayList<String> verify = getSavedVideoKeyList(prefKey);

        logPlaylist("writeSavedVideoKeys VERIFY prefKey=" + prefKey
                + ", verifySize=" + verify.size());

        dumpSavedKeys("writeSavedVideoKeys VERIFY", verify);
    }

    /** 获取 Video 的唯一 key。当前只使用 videoId，不兼容旧 legacy key。 */
    private String getVideoKey(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) {
            return "";
        }

        return video.videoId;
    }

    /** 安全获取标题，避免 null 文本影响 UI。 */
    private String safeTitle(Video video) {
        if (video == null || video.getTitle() == null) {
            return "";
        }

        return video.getTitle();
    }

    /** 隐藏软键盘。 */
    private void hideKeyboard() {
        try {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            View view = getCurrentFocus();
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    /** dp 转 px。 */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 页面销毁时清理搜索订阅、播放器、Handler 回调和弹窗。 */
    @Override
    protected void onDestroy() {
        stopKtvBufferMonitor();

        if (getWindow() != null) {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        dismissVideoMenu();
        clearVideoMenuFocusRecord();

        showKtvPlayerLoading(false);

        if (ktvVideoFullscreen) {
            showSystemUiAfterKtvFullscreen();
        }

        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }

        if (continueGroupDisposable != null && !continueGroupDisposable.isDisposed()) {
            continueGroupDisposable.dispose();
        }

        stopKtvPlayer(true);

        if (ktvConnectivityManager != null && ktvNetworkCallback != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    ktvConnectivityManager.unregisterNetworkCallback(ktvNetworkCallback);
                } catch (Throwable ignored) {
                    // 兼容老 Android TV / 魔改系统。
                }
            }

            ktvNetworkCallback = null;
        }

        uiHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }


    /** 输出 Playlist 相关日志。 */
    private void logPlaylist(String msg) {
        Log.i(PLAYLIST_TAG, msg);
    }

    /** 生成 Video 的调试文本，便于日志排查。 */
    private String videoDebug(Video video) {
        if (video == null) {
            return "null";
        }

        return "{title=" + safeTitle(video)
                + ", videoId=" + video.videoId
                + ", key=" + shortKey(getVideoKey(video))
                + "}";
    }

    /** 缩短很长的 key，避免日志过长。 */
    private String shortKey(String key) {
        if (key == null) {
            return "null";
        }

        if (key.length() > 120) {
            return key.substring(0, 120) + "...";
        }

        return key;
    }

    /** 打印保存列表中的 videoId，用于排查删除、置顶和队列同步问题。 */
    private void dumpSavedKeys(String prefix, ArrayList<String> keys) {
        if (keys == null) {
            logPlaylist(prefix + " keys=null");
            return;
        }

        logPlaylist(prefix + " keys.size=" + keys.size());

        for (int i = 0; i < keys.size() && i < 5; i++) {
            logPlaylist(prefix + " key[" + i + "]=" + shortKey(keys.get(i)));
        }
    }

    /** 节流输出播放器状态，避免轮询期间日志过多。 */
    private void logPlayerStateThrottled(String from) {
        long now = SystemClock.uptimeMillis();

        if (now - lastPlaylistWatcherLogMs < 3000) {
            return;
        }

        lastPlaylistWatcherLogMs = now;

        if (ktvEmbedPlayer == null) {
            logPlaylist(from + " player=null"
                    + ", active=" + playlistPlaybackActive
                    + ", index=" + playlistPlaybackIndex
                    + ", queueSize=" + playlistPlaybackQueue.size()
                    + ", paused=" + paused
                    + ", ktvPlayerPaused=" + ktvPlayerPaused);
            return;
        }

        boolean engine = false;
        boolean contains = false;
        boolean playing = false;
        boolean loading = false;
        long position = -1;
        long duration = -1;

        try {
            engine = ktvEmbedPlayer.isEngineInitialized();
            contains = ktvEmbedPlayer.containsMedia();
            playing = ktvEmbedPlayer.isPlaying();
            loading = ktvEmbedPlayer.isLoading();
            position = ktvEmbedPlayer.getPositionMs();
            duration = ktvEmbedPlayer.getDurationMs();
        } catch (Exception e) {
            logPlaylist(from + " read player state error=" + e.getMessage());
        }

        logPlaylist(from
                + " active=" + playlistPlaybackActive
                + ", index=" + playlistPlaybackIndex
                + ", queueSize=" + playlistPlaybackQueue.size()
                + ", paused=" + paused
                + ", ktvPlayerPaused=" + ktvPlayerPaused
                + ", engine=" + engine
                + ", contains=" + contains
                + ", playing=" + playing
                + ", loading=" + loading
                + ", positionMs=" + position
                + ", durationMs=" + duration
                + ", current=" + videoDebug(getCurrentVideo()));
    }
    /** Playlist / Favorites 删除到空时，自动返回搜索结果页。 */
    private void returnToSearchResultsIfSavedListEmpty(String prefKey, ListMode mode) {
        if (!getSavedVideoKeyList(prefKey).isEmpty()) {
            return;
        }

        updateNextTitle();

        if (listMode != mode) {
            return;
        }

        logPlaylist("Saved list empty, return to search results. prefKey=" + prefKey + ", mode=" + mode);

        // 关键：先切回搜索模式，再隐藏空列表提示。
        // showEmptyListHint(false) 必须执行，否则 Playlist/Favorites 的空提示会残留到搜索页。
        listMode = ListMode.SEARCH;
        showEmptyListHint(false);
        showSearchLoading(false);

        if (songResultList != null) {
            songResultList.setLayoutManager(new GridLayoutManager(this, 4));
        }

        if (songResultList != null && ktvAdapter != null) {
            songResultList.setAdapter(ktvAdapter);
        }

        currentVideos.clear();
        currentVideos.addAll(lastSearchVideos);

        currentIndex = findVideoIndexByKey(currentPlayingVideoKey, currentVideos);

        if (ktvAdapter != null) {
            ktvAdapter.updateData(currentVideos);
        }

        updateSavedButtonState();
        updateNextTitle();

        if (!currentVideos.isEmpty()) {
            requestFirstSongFocus();
        } else {
            loadMySearchResult(baseSearchText);
        }
    }

    /** Playlist 为空时返回搜索结果页。 */
    private void returnToSearchResultsIfPlaylistEmpty() {
        returnToSearchResultsIfSavedListEmpty(KEY_PLAYLIST, ListMode.PLAYLIST);
    }

    /** Favorites 为空时返回搜索结果页。 */
    private void returnToSearchResultsIfFavoritesEmpty() {
        returnToSearchResultsIfSavedListEmpty(KEY_FAVORITES, ListMode.FAVORITES);
    }

    /**
     * 开始监控播放过程中的缓冲状态。
     *
     * 用途：
     * 1. 播放器卡住时显示 buffering 提示。
     * 2. 显示当前 App 下载网速。
     */
    private void startKtvBufferMonitor() {
        logBuffer("startKtvBufferMonitor"
                + ", ktvEmbedPlayer=" + ktvEmbedPlayer
                + ", ktvPlayingVideo=" + videoDebug(ktvPlayingVideo)
                + ", paused=" + paused
                + ", ktvPlayerPaused=" + ktvPlayerPaused);

        resetKtvBufferMonitorState();

        uiHandler.removeCallbacks(ktvBufferMonitorRunnable);
        uiHandler.postDelayed(ktvBufferMonitorRunnable, 500);
    }

    /** 停止播放缓冲监控。 */
    private void stopKtvBufferMonitor() {
        logBuffer("stopKtvBufferMonitor");

        uiHandler.removeCallbacks(ktvBufferMonitorRunnable);
        resetKtvBufferMonitorState();

        ktvNetworkDisconnectedHintShowing = false;

        showKtvBufferingHint(false, "");
    }

    /** 重置缓冲检测状态。 */
    private void resetKtvBufferMonitorState() {
        lastBufferCheckPositionMs = -1L;
        lastBufferCheckTimeMs = 0L;
        bufferStallStartMs = 0L;
        lastTrafficRxBytes = -1L;
        lastTrafficCheckMs = 0L;
        ktvBufferingHintShown = false;
    }

    /**
     * 检测当前播放是否疑似缓冲。
     *
     * 判断逻辑：
     * 1. 播放器正在播放任务中。
     * 2. 没有暂停。
     * 3. 播放位置一段时间没有变化。
     * 4. 或 EmbedPlayerView 自己报告 isLoading=true。
     */
    /**
     * 检测当前播放是否疑似缓冲。
     *
     * 关键修正：
     * 1. containsMedia=false 时不能直接结束监控，因为刚 openVideo 后媒体还没 attach。
     * 2. isLoading=true 不一定代表卡住，因为播放器可能边播边缓冲。
     * 3. 主要根据 positionMs 是否长时间不变判断卡顿。
     */
    private void checkKtvPlaybackBuffering() {
        // 断网期间强制持续显示提示，不要被 containsMedia=false / error 状态隐藏掉。
        if (ktvNetworkDisconnectedHintShowing || !isKtvNetworkAvailable()) {
            if (ktvPlayingVideo != null || ktvPendingNetworkRecovery) {
                rememberKtvNetworkRecoveryState();
                ktvNetworkDisconnectedHintShowing = true;

                showKtvNetworkDisconnectedHint();

                uiHandler.postDelayed(
                        ktvBufferMonitorRunnable,
                        KTV_BUFFER_MONITOR_INTERVAL_MS
                );
                return;
            }
        }
        if (ktvEmbedPlayer == null || ktvPlayingVideo == null) {
            logBuffer("check STOP: player or video is null"
                    + ", ktvEmbedPlayer=" + ktvEmbedPlayer
                    + ", ktvPlayingVideo=" + videoDebug(ktvPlayingVideo));

            showKtvBufferingHint(false, "");
            return;
        }

        boolean engineInitialized = false;
        boolean containsMedia = false;
        boolean loading = false;
        boolean playing = false;
        long positionMs = -1L;
        long durationMs = -1L;

        try {
            engineInitialized = ktvEmbedPlayer.isEngineInitialized();
            containsMedia = ktvEmbedPlayer.containsMedia();
            loading = engineInitialized && ktvEmbedPlayer.isLoading();
            playing = engineInitialized && ktvEmbedPlayer.isPlaying();
            positionMs = engineInitialized ? ktvEmbedPlayer.getPositionMs() : -1L;
            durationMs = engineInitialized ? ktvEmbedPlayer.getDurationMs() : -1L;
        } catch (Exception e) {
            logBuffer("check READ_ERROR " + e.getMessage());
        }

        String speedText = getNetworkSpeedText();

        // 关键：
        // openVideo 后一段时间 containsMedia=false 是正常的。
        // 不能 return 后结束监控，必须继续轮询。
        if (!engineInitialized || !containsMedia) {
            logBuffer("check WAIT_MEDIA"
                    + ", engine=" + engineInitialized
                    + ", containsMedia=" + containsMedia
                    + ", loading=" + loading
                    + ", playing=" + playing
                    + ", positionMs=" + positionMs
                    + ", durationMs=" + durationMs
                    + ", speed=" + speedText);

            // 初始加载阶段继续交给 showKtvPlayerLoading() 显示 Loading songs…
            // 这里只保持监控继续跑。
            uiHandler.postDelayed(ktvBufferMonitorRunnable, KTV_BUFFER_MONITOR_INTERVAL_MS);
            return;
        }

        if (paused || ktvPlayerPaused) {
            logBuffer("check PAUSED"
                    + ", paused=" + paused
                    + ", ktvPlayerPaused=" + ktvPlayerPaused
                    + ", positionMs=" + positionMs);

            showKtvBufferingHint(false, "");
            uiHandler.postDelayed(ktvBufferMonitorRunnable, KTV_BUFFER_MONITOR_INTERVAL_MS);
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long positionDelta = -1L;

        if (positionMs >= 0 && lastBufferCheckPositionMs >= 0) {
            positionDelta = Math.abs(positionMs - lastBufferCheckPositionMs);

            // 进度已经开始播放后，连续不动才算卡顿。
            // positionMs=0 的初始加载不算播放中卡顿。
            if (positionMs > 0 && positionDelta < 300) {
                if (bufferStallStartMs <= 0) {
                    bufferStallStartMs = now;
                }
            } else {
                bufferStallStartMs = 0L;
            }
        }

        lastBufferCheckPositionMs = positionMs;
        lastBufferCheckTimeMs = now;

        long stallMs = bufferStallStartMs > 0 ? now - bufferStallStartMs : 0L;

        boolean stalled =
                bufferStallStartMs > 0
                        && stallMs >= KTV_BUFFER_STALL_MS;

        // 不要只看 loading。
        // 日志显示 loading=true 时 position 也可能正常增长。
        boolean buffering = stalled;

        logBuffer("check"
                + ", pos=" + positionMs
                + ", duration=" + durationMs
                + ", delta=" + positionDelta
                + ", playing=" + playing
                + ", loading=" + loading
                + ", stallMs=" + stallMs
                + ", stalled=" + stalled
                + ", buffering=" + buffering
                + ", speed=" + speedText
                + ", overlay=" + (ktvPlayerLoadingOverlay != null
                ? ktvPlayerLoadingOverlay.getVisibility()
                : -1));

        if (buffering) {
            boolean networkAvailable = isKtvNetworkAvailable();

            if (!networkAvailable) {
                rememberKtvNetworkRecoveryState();
            }

            String message = networkAvailable
                    ? "Network buffering…"
                    : "Network disconnected…\nWaiting to reconnect.";

            // 直接复用前面已经定义过的 speedText，不要重复定义。
            if (!TextUtils.isEmpty(speedText)) {
                message += "\nDownload: " + speedText;
            }

            showKtvBufferingHint(true, message);
        } else {
            showKtvBufferingHint(false, "");
        }

        uiHandler.postDelayed(ktvBufferMonitorRunnable, KTV_BUFFER_MONITOR_INTERVAL_MS);
    }

    /**
     * 显示 / 隐藏播放过程中的网络缓冲提示。
     *
     * 复用 ktv_player_loading_overlay，不再额外加一套 XML。
     */
    private void showKtvBufferingHint(boolean show, String message) {
        logBuffer("showKtvBufferingHint"
                + ", show=" + show
                + ", message=" + message
                + ", overlayNull=" + (ktvPlayerLoadingOverlay == null)
                + ", loadingFlag=" + ktvPlayerLoading
                + ", bufferingHintShown=" + ktvBufferingHintShown
                + ", networkDisconnectedHintShowing=" + ktvNetworkDisconnectedHintShowing);

        if (ktvPlayerLoadingOverlay == null) {
            return;
        }

        if (show) {
            ktvBufferingHintShown = true;

            if (tvKtvPlayerLoading != null) {
                tvKtvPlayerLoading.setText(
                        TextUtils.isEmpty(message)
                                ? "Network buffering…"
                                : message
                );
            }

            ktvPlayerLoadingOverlay.setVisibility(View.VISIBLE);
            bringKtvPlayerLoadingOverlayToFront();
            return;
        }

        // 关键：
        // 断网期间不要隐藏提示。
        if (ktvNetworkDisconnectedHintShowing) {
            return;
        }

        ktvBufferingHintShown = false;

        if (!ktvPlayerLoading) {
            ktvPlayerLoadingOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * 获取当前 App 的下载速度。
     *
     * 优先读当前 App UID 的接收字节数。
     * 如果系统不支持，再退回 total rx bytes。
     */
    private String getNetworkSpeedText() {
        long now = SystemClock.elapsedRealtime();
        long rxBytes = getCurrentRxBytes();

        if (rxBytes < 0) {
            logBuffer("speed unsupported");
            return "";
        }

        if (lastTrafficRxBytes < 0 || lastTrafficCheckMs <= 0) {
            lastTrafficRxBytes = rxBytes;
            lastTrafficCheckMs = now;

            logBuffer("speed init rxBytes=" + rxBytes);

            return "calculating";
        }

        long deltaBytes = rxBytes - lastTrafficRxBytes;
        long deltaTimeMs = now - lastTrafficCheckMs;

        lastTrafficRxBytes = rxBytes;
        lastTrafficCheckMs = now;

        if (deltaBytes < 0 || deltaTimeMs <= 0) {
            logBuffer("speed invalid"
                    + ", deltaBytes=" + deltaBytes
                    + ", deltaTimeMs=" + deltaTimeMs);

            return "";
        }

        double bytesPerSecond = deltaBytes * 1000.0 / deltaTimeMs;
        String text = formatNetworkSpeed(bytesPerSecond);

        logBuffer("speed"
                + ", deltaBytes=" + deltaBytes
                + ", deltaTimeMs=" + deltaTimeMs
                + ", text=" + text);

        return text;
    }

    /** 当前接收字节数。 */
    private long getCurrentRxBytes() {
        long uidBytes = TrafficStats.getUidRxBytes(getApplicationInfo().uid);

        if (uidBytes != TrafficStats.UNSUPPORTED) {
            return uidBytes;
        }

        long totalBytes = TrafficStats.getTotalRxBytes();

        if (totalBytes != TrafficStats.UNSUPPORTED) {
            return totalBytes;
        }

        return -1L;
    }

    /** 格式化网速。 */
    private String formatNetworkSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return Math.round(bytesPerSecond) + " B/s";
        }

        double kb = bytesPerSecond / 1024.0;

        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB/s", kb);
        }

        double mb = kb / 1024.0;

        return String.format(Locale.US, "%.2f MB/s", mb);
    }

    /**
     * 监听网络恢复。
     *
     * 断网后 ExoPlayer 可能会进入错误状态。
     * 网络恢复时，需要主动重新打开当前歌曲并 seek 回断网前的位置。
     */
    private void setupKtvNetworkRecoveryMonitor() {
        ktvConnectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (ktvConnectivityManager == null || ktvNetworkCallback != null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "NetworkCallback is not supported on this Android version.");
            return;
        }

        ktvNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                uiHandler.postDelayed(() -> {
                    if (!isKtvNetworkAvailable()) {
                        onKtvNetworkLost();
                    }
                }, 500);
            }

            @Override
            public void onAvailable(Network network) {
                uiHandler.postDelayed(() -> {
                    if (isKtvNetworkAvailable()) {
                        onKtvNetworkAvailable();
                    }
                }, 1200);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                uiHandler.post(() -> {
                    if (isKtvNetworkAvailable()) {
                        onKtvNetworkAvailable();
                    }
                });
            }
        };

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            ktvConnectivityManager.registerNetworkCallback(request, ktvNetworkCallback);
        } catch (Throwable e) {
            Log.e(TAG, "setupKtvNetworkRecoveryMonitor error: " + e.getMessage());
            ktvNetworkCallback = null;
        }
    }
    //新增网络判断：
    /**
     * 判断当前网络是否可用。
     *
     * 注意：
     * 不使用 ConnectivityManager.getActiveNetwork()。
     * 一些 Android TV / 老盒子系统没有这个方法，运行时会直接 NoSuchMethodError。
     */
    @SuppressWarnings("deprecation")
    private boolean isKtvNetworkAvailable() {
        if (ktvConnectivityManager == null) {
            return true;
        }

        try {
            NetworkInfo networkInfo = ktvConnectivityManager.getActiveNetworkInfo();

            return networkInfo != null && networkInfo.isConnected();
        } catch (Throwable e) {
            Log.e(TAG, "isKtvNetworkAvailable error: " + e.getMessage());

            // 不确定时返回 true，避免误判断网导致播放恢复逻辑乱跑。
            return true;
        }
    }
    //新增断网处理：
    private void onKtvNetworkLost() {
        if (ktvPlayingVideo == null) {
            return;
        }

        rememberKtvNetworkRecoveryState();

        ktvNetworkDisconnectedHintShowing = true;

        showKtvNetworkDisconnectedHint();

        // 关键：断网后继续跑 buffer monitor，用它每秒刷新提示。
        uiHandler.removeCallbacks(ktvBufferMonitorRunnable);
        uiHandler.post(ktvBufferMonitorRunnable);

        Log.d(TAG, "KTV network lost, save recovery position="
                + ktvPendingRecoveryPositionMs
                + ", video=" + videoDebug(ktvPendingRecoveryVideo));
    }
    /**
     * 断网期间持续显示提示。
     *
     * 注意：
     * 断网时下载速度通常会显示 0 B/s 或 calculating。
     * 这里保留 speedText，主要是为了让用户看到播放器仍在检测网络。
     */
    private void showKtvNetworkDisconnectedHint() {
        String speedText = getNetworkSpeedText();

        String message = "Network disconnected…\nWaiting to reconnect.";

        if (!TextUtils.isEmpty(speedText)) {
            message += "\nDownload: " + speedText;
        }

        showKtvBufferingHint(true, message);
    }
    //新增恢复处理：
    private void onKtvNetworkAvailable() {
        if (!ktvPendingNetworkRecovery || ktvPendingRecoveryVideo == null) {
            ktvNetworkDisconnectedHintShowing = false;
            return;
        }

        ktvNetworkDisconnectedHintShowing = false;

        showKtvBufferingHint(
                true,
                "Network restored.\nResuming playback…"
        );

        uiHandler.removeCallbacks(ktvNetworkRecoveryRunnable);
        uiHandler.postDelayed(
                ktvNetworkRecoveryRunnable,
                KTV_NETWORK_RECOVERY_RETRY_DELAY_MS
        );
    }
    //新增记录当前播放状态：
    private void rememberKtvNetworkRecoveryState() {
        if (ktvPlayingVideo == null) {
            return;
        }

        long positionMs = 0L;

        try {
            if (ktvEmbedPlayer != null && ktvEmbedPlayer.isEngineInitialized()) {
                positionMs = ktvEmbedPlayer.getPositionMs();
            }
        } catch (Exception ignored) {
        }

        if (positionMs < 0) {
            positionMs = 0L;
        }

        // 稍微往前退一点，避免恢复后漏掉一句歌词。
        if (positionMs > 3000L) {
            positionMs -= 2000L;
        }

        ktvPendingRecoveryVideo = ktvPlayingVideo;
        ktvPendingRecoveryPositionMs = positionMs;
        ktvPendingNetworkRecovery = true;
    }
    //新增恢复播放：
    private void retryKtvPlaybackAfterNetworkReturn() {
        if (!ktvPendingNetworkRecovery || ktvPendingRecoveryVideo == null) {
            return;
        }

        if (!isKtvNetworkAvailable()) {
            uiHandler.postDelayed(
                    ktvNetworkRecoveryRunnable,
                    KTV_NETWORK_RECOVERY_RETRY_DELAY_MS
            );
            return;
        }

        Video video = ktvPendingRecoveryVideo;
        long positionMs = Math.max(0L, ktvPendingRecoveryPositionMs);

        Log.d(TAG, "retryKtvPlaybackAfterNetworkReturn"
                + ", positionMs=" + positionMs
                + ", video=" + videoDebug(video));

        prepareKtvPlayerIfNeeded();

        if (ktvEmbedPlayer == null) {
            return;
        }

        ktvPlayingVideo = video;
        ktvPlayerPaused = false;
        paused = false;

        showKtvPlayerLoading(true);

        // 关键：断网错误后强制重建播放器，并从断网前位置恢复。
        ktvEmbedPlayer.reopenVideoAt(video, positionMs);
        ktvNetworkDisconnectedHintShowing = false;
        startKtvBufferMonitor();

        if (playlistPlaybackActive || startPlaylistAfterCurrentVideo || clearNowPlayingWhenCurrentVideoEnds) {
            startPlaylistEndWatcher();
        }

        updateNowPlaying(video);
        updateNextTitle();
        updatePauseButton();

        ktvPendingNetworkRecovery = false;
        ktvPendingRecoveryVideo = null;
        ktvPendingRecoveryPositionMs = 0L;
    }

}
