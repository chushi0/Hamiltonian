package org.cszt0.hamiltonian;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import online.cszt0.androidcommonutils.view.CommonRecyclerViewAdapter;
import online.cszt0.androidcommonutils.view.ViewHolder;

public class MainActivity extends AppCompatActivity {

    private static final String SNAPSHOT_FRAGMENT = "snapshot";
    private static final String HELLO_WORLD_FRAGMENT = "hello_world";
    private static final String SELECT_CHAPTER_FRAGMENT = "select_chapter";
    private static final String SELECT_CHECKPOINT_FRAGMENT = "select_checkpoint";
    private static final String GAME_FRAGMENT = "game";

    private GLSurfaceView glSurfaceView;
    private NativeRenderer nativeRenderer;

    private Handler handler = new Handler();
    private GameDatabase gameDatabase;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameDatabase = new GameDatabase(this);
        sharedPreferences = getSharedPreferences("game", MODE_PRIVATE);
        glSurfaceView = findViewById(R.id.surface_view);
        nativeRenderer = new NativeRenderer();
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setRenderer(nativeRenderer);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (Hamiltonian.SNAPSHOT) {
            MainActivityFragment fragment = addFragment(fragmentTransaction, new SnapshotFragment(), SNAPSHOT_FRAGMENT);
            fragmentTransaction.show(fragment);
            handler.post(fragment::onChangeTo);
        }
        MainActivityFragment fragment = addFragment(fragmentTransaction, new HelloWorldFragment(), HELLO_WORLD_FRAGMENT);
        if (!Hamiltonian.SNAPSHOT) {
            fragmentTransaction.show(fragment);
            handler.post(fragment::onChangeTo);
        }
        addFragment(fragmentTransaction, new SelectChapterFragment(), SELECT_CHAPTER_FRAGMENT);
        addFragment(fragmentTransaction, new SelectCheckpointFragment(), SELECT_CHECKPOINT_FRAGMENT);
        addFragment(fragmentTransaction, new GameFragment(), GAME_FRAGMENT);
        fragmentTransaction.commit();
    }

    private <T extends Fragment> T addFragment(FragmentTransaction fragmentTransaction, T fragment, String tag) {
        fragmentTransaction.add(R.id.fragment_container, fragment, tag);
        fragmentTransaction.hide(fragment);
        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    private void changeFragment(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentManager.getFragments()) {
            fragmentTransaction.hide(fragment);
        }
        MainActivityFragment fragmentByTag = (MainActivityFragment) fragmentManager.findFragmentByTag(tag);
        handler.post(fragmentByTag::onChangeTo);
        fragmentTransaction.show(fragmentByTag);
        fragmentTransaction.commit();
    }

    private void changeFragmentWithArgument(String tag, Bundle argument) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentManager.getFragments()) {
            fragmentTransaction.hide(fragment);
        }
        MainActivityFragment fragmentByTag = (MainActivityFragment) fragmentManager.findFragmentByTag(tag);
        fragmentByTag.setArguments(argument);
        handler.post(fragmentByTag::onChangeTo);
        fragmentTransaction.show(fragmentByTag);
        fragmentTransaction.commit();
    }

    private Fragment getFragment(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return fragmentManager.findFragmentByTag(tag);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("确定要退出游戏？")
                .setPositiveButton("退出", (dia, btn) -> finish())
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        nativeRenderer.dispatchTouchEvent(ev);
        super.dispatchTouchEvent(ev);
        return true;
    }

    private static abstract class MainActivityFragment extends Fragment {
        private static final SparseIntArray checkpointCount = new SparseIntArray();

        protected MainActivity getMainActivity() {
            return (MainActivity) getContext();
        }

        protected void changeFragment(String tag) {
            getMainActivity().changeFragment(tag);
        }

        protected void changeFragment(String tag, Bundle argument) {
            getMainActivity().changeFragmentWithArgument(tag, argument);
        }

        @SuppressWarnings("unchecked")
        protected <T extends Fragment> T getFragment(String tag) {
            return (T) getMainActivity().getFragment(tag);
        }

        protected AlertDialog showTeachDialog(String file) {
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(getContext().getAssets().open(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(bitmap);
            return new AlertDialog.Builder(getContext())
                    .setTitle("游戏说明")
                    .setView(imageView)
                    .setPositiveButton("确定", null)
                    .setCancelable(false)
                    .show();
        }

        protected void onChangeTo() {
        }

        protected Bundle getArgumentsOrNew() {
            Bundle arguments = getArguments();
            if (arguments == null) {
                arguments = new Bundle();
            }
            return arguments;
        }

        protected GameDatabase getDatabase() {
            return getMainActivity().gameDatabase;
        }

        protected int getChapterCheckpointCount(int chapter) {
            int count = checkpointCount.get(chapter, -1);
            if (count == -1) {
                count = readChapterCheckpointCount(chapter);
                checkpointCount.put(chapter, count);
            }
            return count;
        }

        protected boolean getFlag(String key) {
            return getMainActivity().sharedPreferences.getBoolean(key, false);
        }

        protected void saveFlag(String key, boolean flag) {
            SharedPreferences.Editor editor = getMainActivity().sharedPreferences.edit();
            editor.putBoolean(key, flag);
            editor.apply();
        }

        private int readChapterCheckpointCount(int chapter) {
            try {
                String[] checkpoints = getContext().getAssets().list("checkpoint/" + chapter);
                if (checkpoints == null) return 0;
                return checkpoints.length;
            } catch (IOException e) {
                return 0;
            }
        }
    }

    public static class SnapshotFragment extends MainActivityFragment {
        Handler handler = new Handler();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(getContext().getAssets().open("snapshot.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(bitmap);
            imageView.setOnClickListener(v -> finish());
            return imageView;
        }

        @Override
        public void onChangeTo() {
            super.onChangeTo();
            handler.postDelayed(this::finish, 5000);
        }

        private void finish() {
            handler.removeCallbacksAndMessages(null);
            changeFragment(HELLO_WORLD_FRAGMENT);
        }
    }

    public static class HelloWorldFragment extends MainActivityFragment {

        private static final String FLAG_HELLO_WORLD = "hello_world";

        GameView gameView;
        long startTime;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            gameView = new GameView(getContext(), null);
            gameView.loadGraph("checkpoint/helloworld.dat");
            gameView.setOnFinishListener(new GameView.GameListener() {
                @Override
                public void onEnergyChange(int energy) {
                }

                @Override
                public void onFinish() {
                    HelloWorldFragment.this.onFinish();
                }
            });
            return gameView;
        }

        @Override
        public void onChangeTo() {
            super.onChangeTo();
            if (getFlag(FLAG_HELLO_WORLD)) {
                gotoMenu();
                return;
            }
            gameView.clearRoad();
            showTeachDialog("teach/1.png").setOnDismissListener(dialog -> startTime = SystemClock.elapsedRealtime());
        }

        private void onFinish() {
            long endTime = SystemClock.elapsedRealtime();
            long run = endTime - startTime + 1000;
            int second = (int) (run / 1000 % 60);
            int minute = (int) (run / 1000 / 60);
            View view = getLayoutInflater().inflate(R.layout.dialog_pass, null);
            TextView timeView = view.findViewById(R.id.time_used);
            timeView.setText(String.format("%02d'%02d\"", minute, second));
            view.findViewById(R.id.back).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.retry).setVisibility(View.INVISIBLE);
            Button button = view.findViewById(R.id.next);
            button.setText("确定");
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(view)
                    .setCancelable(false)
                    .show();
            button.setOnClickListener(v -> {
                dialog.dismiss();
                saveFlag(FLAG_HELLO_WORLD, true);
                gotoMenu();
            });
        }

        private void gotoMenu() {
            changeFragment(SELECT_CHAPTER_FRAGMENT);
        }
    }

    public static class SelectChapterFragment extends MainActivityFragment {
        List<ChapterInfo> chapterInfoList;
        CommonRecyclerViewAdapter<ChapterInfo> viewPagerAdapter;

        public SelectChapterFragment() {
            chapterInfoList = Arrays.asList(
                    new ChapterInfo(1),
                    new ChapterInfo(2),
                    new ChapterInfo(3),
                    new ChapterInfo(4)
            );
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_select_chapter, container, false);
            ViewPager2 viewPager = view.findViewById(R.id.view_pager);
            viewPager.setClipChildren(false);
            viewPagerAdapter = new CommonRecyclerViewAdapter<ChapterInfo>(getContext(), chapterInfoList, R.layout.fragment_item_select_chapter) {
                @Override
                protected void bindView(ViewHolder viewHolder, ChapterInfo chapterInfo, int position, int viewType) {
                    viewHolder.setTextViewText(R.id.title, "Chapter " + chapterInfo.chapter);
                    viewHolder.setTextViewText(R.id.completion, String.format("完成度：%d/%d", chapterInfo.getFinishCount(), getChapterCheckpointCount(chapterInfo.chapter)));
                    viewHolder.getView(R.id.card).setOnClickListener(v -> onClick(position));
                }
            };
            viewPager.setAdapter(viewPagerAdapter);
            view.findViewById(R.id.left).setOnClickListener(v -> {
                int page = viewPager.getCurrentItem();
                if (page > 0) {
                    viewPager.setCurrentItem(page - 1, true);
                }
            });
            view.findViewById(R.id.right).setOnClickListener(v -> {
                int page = viewPager.getCurrentItem() + 1;
                if (page < viewPagerAdapter.getItemCount()) {
                    viewPager.setCurrentItem(page, true);
                }
            });
            return view;
        }

        @Override
        protected void onChangeTo() {
            super.onChangeTo();
            for (ChapterInfo chapterInfo : chapterInfoList) {
                chapterInfo.clearFinishCount();
            }
            viewPagerAdapter.notifyDataSetChanged();
        }

        private void onClick(int position) {
            Bundle argument = new Bundle();
            argument.putInt(SelectCheckpointFragment.ARGUMENT_CHAPTER, position + 1);
            changeFragment(SELECT_CHECKPOINT_FRAGMENT, argument);
        }

        class ChapterInfo {
            private int chapter;
            private int finishCount;

            ChapterInfo(int chapter) {
                this.chapter = chapter;
                finishCount = -1;
            }

            int getFinishCount() {
                if (finishCount == -1) {
                    finishCount = getDatabase().getFinishCount(chapter);
                }
                return finishCount;
            }

            void clearFinishCount() {
                finishCount = -1;
            }
        }
    }

    public static class SelectCheckpointFragment extends MainActivityFragment {

        static final String ARGUMENT_CHAPTER = "chapter";

        TextView titleView;
        RecyclerView recyclerView;
        CommonRecyclerViewAdapter<CheckpointInfo> recyclerViewAdapter;

        int chapter;
        List<CheckpointInfo> checkpointInfoList = new ArrayList<>();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_select_checkpoint, container, false);
            titleView = view.findViewById(R.id.title);
            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));
            recyclerViewAdapter = new CommonRecyclerViewAdapter<CheckpointInfo>(getContext(), checkpointInfoList, R.layout.fragment_item_select_checkpoint) {
                @Override
                protected void bindView(ViewHolder viewHolder, CheckpointInfo checkpointInfo, int position, int viewType) {
                    viewHolder.setTextViewText(R.id.text, String.valueOf(checkpointInfo.checkpoint));
                    viewHolder.setTextViewText(R.id.time, checkpointInfo.getTime());
                    viewHolder.getView(R.id.text).setOnClickListener(v -> onSelectCheckpoint(position));
                }
            };
            recyclerView.setAdapter(recyclerViewAdapter);
            view.findViewById(R.id.back).setOnClickListener(v -> changeFragment(SELECT_CHAPTER_FRAGMENT));
            return view;
        }

        @Override
        protected void onChangeTo() {
            super.onChangeTo();
            recyclerView.scrollToPosition(0);

            Bundle argument = getArgumentsOrNew();
            chapter = argument.getInt(ARGUMENT_CHAPTER, 1);
            titleView.setText("Chapter " + chapter);

            checkpointInfoList.clear();
            int count = getChapterCheckpointCount(chapter);
            for (int i = 1; i <= count; i++) {
                checkpointInfoList.add(new CheckpointInfo(i));
            }
            recyclerViewAdapter.notifyDataSetChanged();
        }

        private void onSelectCheckpoint(int checkpoint) {
            Bundle argument = new Bundle();
            argument.putInt(GameFragment.ARGUMENT_CHAPTER, chapter);
            argument.putInt(GameFragment.ARGUMENT_CHECKPOINT, checkpoint + 1);
            changeFragment(GAME_FRAGMENT, argument);
        }

        class CheckpointInfo {
            int checkpoint;
            int time;

            public CheckpointInfo(int checkpoint) {
                this.checkpoint = checkpoint;
                time = -1;
            }

            String getTime() {
                if (time == -1) {
                    time = getDatabase().getCheckpointPassTime(chapter, checkpoint);
                }
                if (time == -1) {
                    return "---";
                }
                int minute = time / 60;
                int second = time % 60;
                return String.format("%02d'%02d\"", minute, second);
            }
        }
    }

    public static class GameFragment extends MainActivityFragment {
        static final String ARGUMENT_CHAPTER = "chapter";
        static final String ARGUMENT_CHECKPOINT = "checkpoint";

        static final SparseIntArray teachPage = new SparseIntArray();

        static {
            teachPage.put(1006, 2);
            teachPage.put(2001, 3);
            teachPage.put(3001, 4);
            teachPage.put(4001, 5);
        }

        GameView gameView;
        TextView energyView;
        long startTime;

        int chapter;
        int checkpoint;
        boolean hasNext;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_game, container, false);
            gameView = view.findViewById(R.id.game_view);
            energyView = view.findViewById(R.id.energy);
            gameView.setOnFinishListener(new GameView.GameListener() {
                @Override
                public void onEnergyChange(int energy) {
                    GameFragment.this.onEnergyChange(energy);
                }

                @Override
                public void onFinish() {
                    GameFragment.this.onFinish();
                }
            });
            view.findViewById(R.id.back).setOnClickListener(v -> changeFragment(SELECT_CHECKPOINT_FRAGMENT));
            view.findViewById(R.id.undo).setOnClickListener(v -> gameView.gotoLast());
            view.findViewById(R.id.retry).setOnClickListener(v -> gameView.clearRoad());
            return view;
        }

        @Override
        protected void onChangeTo() {
            super.onChangeTo();

            Bundle argument = getArgumentsOrNew();
            chapter = argument.getInt(ARGUMENT_CHAPTER, 1);
            checkpoint = argument.getInt(ARGUMENT_CHECKPOINT, 1);
            hasNext = checkpoint < getChapterCheckpointCount(chapter);

            energyView.setVisibility(View.GONE);
            gameView.loadGraph(String.format("checkpoint/%d/%d.dat", chapter, checkpoint));
            if (!getFlag(String.valueOf(chapter * 1000 + checkpoint))) {
                int page = teachPage.get(chapter * 1000 + checkpoint, -1);
                if (page != -1) {
                    showTeachDialog(String.format("teach/%d.png", page)).setOnDismissListener(dia -> startTime = SystemClock.elapsedRealtime());
                }
            }
            startTime = SystemClock.elapsedRealtime();
        }

        private void onFinish() {
            long endTime = SystemClock.elapsedRealtime();
            long run = endTime - startTime + 1000;
            getDatabase().newRecord(chapter, checkpoint, (int) (run / 1000));
            if (teachPage.get(chapter * 1000 + checkpoint, -1) != -1) {
                saveFlag(String.valueOf(chapter * 1000 + checkpoint), true);
            }
            int second = (int) (run / 1000 % 60);
            int minute = (int) (run / 1000 / 60);
            View view = getLayoutInflater().inflate(R.layout.dialog_pass, null);
            TextView timeView = view.findViewById(R.id.time_used);
            timeView.setText(String.format("%02d'%02d\"", minute, second));
            if (!hasNext) {
                view.findViewById(R.id.next).setVisibility(View.INVISIBLE);
            }
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(view)
                    .setCancelable(false)
                    .show();
            view.findViewById(R.id.next).setOnClickListener(v -> {
                dialog.dismiss();
                Bundle bundle = new Bundle();
                bundle.putInt(ARGUMENT_CHAPTER, chapter);
                bundle.putInt(ARGUMENT_CHECKPOINT, checkpoint + 1);
                changeFragment(GAME_FRAGMENT, bundle);
            });
            view.findViewById(R.id.retry).setOnClickListener(v -> {
                dialog.dismiss();
                gameView.clearRoad();
                startTime = SystemClock.elapsedRealtime();
            });
            view.findViewById(R.id.back).setOnClickListener(v -> {
                dialog.dismiss();
                changeFragment(SELECT_CHECKPOINT_FRAGMENT);
            });
        }

        private void onEnergyChange(int energy) {
            energyView.setVisibility(View.VISIBLE);
            energyView.setText("能量：" + energy);
        }
    }

    static class GameDatabase extends SQLiteOpenHelper {
        public GameDatabase(Context context) {
            super(context, "game.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table checkpoint(chapter int, checkpoint int, record int)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        int getFinishCount(int chapter) {
            try (SQLiteDatabase db = getReadableDatabase()) {
                Cursor cursor = db.rawQuery("select count(distinct checkpoint) from checkpoint where chapter=?", new String[]{String.valueOf(chapter)});
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                } else {
                    return -1;
                }
            }
        }

        int getCheckpointPassTime(int chapter, int checkpoint) {
            try (SQLiteDatabase db = getReadableDatabase()) {
                Cursor cursor = db.rawQuery("select min(record) from checkpoint where chapter=? and checkpoint=?", new String[]{String.valueOf(chapter), String.valueOf(checkpoint)});
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    return cursor.getInt(0);
                } else {
                    return -1;
                }
            }
        }

        void newRecord(int chapter, int checkpoint, int record) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                db.execSQL("insert into checkpoint (chapter, checkpoint, record) values (?, ?, ?)", new Object[]{chapter, checkpoint, record});
            }
        }
    }
}
