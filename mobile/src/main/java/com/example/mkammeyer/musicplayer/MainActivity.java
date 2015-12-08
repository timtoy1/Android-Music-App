package com.example.mkammeyer.musicplayer;

import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuItem;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.Comparator;
import java.util.logging.Logger;

import android.net.Uri;
        import android.content.ContentResolver;
        import android.database.Cursor;
        import android.widget.ListView;
        import android.os.IBinder;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.content.ServiceConnection;
        import android.view.MenuItem;
        import android.view.View;
        import com.example.mkammeyer.musicplayer.MusicService.MusicBinder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.widget.SeekBar;
        import android.widget.SeekBar.OnSeekBarChangeListener;
        import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OnSeekBarChangeListener{

    private ArrayList<Song> songList;
    private ListView rightSongView;
    private ListView leftSongView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private SeekBar faderBar;
    private SeekBar songBar1;
    private SeekBar songBar2;
    private boolean pausedLeft = false;
    private boolean pausedRight = false;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dj);
        leftSongView = (ListView)findViewById(R.id.left_song_list);
        rightSongView = (ListView)findViewById(R.id.right_song_list);
        songList = new ArrayList<Song>();

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR_DEVICE_HASH")
                .build();
        mAdView.loadAd(adRequest);

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        leftSongView.setAdapter(songAdt);
        rightSongView.setAdapter(songAdt);

        faderBar = (SeekBar)findViewById(R.id.seekBar);
        faderBar.setOnSeekBarChangeListener(this);

        songBar1 = (SeekBar)findViewById(R.id.songBar1);
        songBar1.setOnSeekBarChangeListener(this);
        songBar2 = (SeekBar)findViewById(R.id.songBar2);
        songBar2.setOnSeekBarChangeListener(this);

        TextView name1 = (TextView)findViewById(R.id.textView);
        TextView artist1 = (TextView)findViewById(R.id.textView3);
        TextView name2 = (TextView)findViewById(R.id.textView2);
        TextView artist2 = (TextView)findViewById(R.id.textView4);
        name1.setText(songList.get(0).getTitle());
        artist1.setText(songList.get(0).getArtist());
        name2.setText(songList.get(0).getTitle());
        artist2.setText(songList.get(0).getArtist());

        updateProgressBar();
    }


    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     * */
    private Runnable mUpdateTimeTask = new Runnable() {

        public void run() {
            int position, max;
            if(musicSrv.isPng(0)) {
                position = musicSrv.getPosn(0);
                max = musicSrv.getDur(0);
                songBar1.setProgress(position);
                songBar1.setMax(max);
            }

            if(musicSrv.isPng(1)) {
                position = musicSrv.getPosn(1);
                max = musicSrv.getDur(1);
                songBar2.setProgress(position);
                songBar2.setMax(max);
            }

            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onStartTrackingTouch(SeekBar seekbar){
        if(seekbar.getId() != R.id.seekBar)
            mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekbar){
        int progress = seekbar.getProgress();

        if(seekbar.getId() == R.id.songBar1) {
            musicSrv.seek(0, progress);
            updateProgressBar();
        }

        else if(seekbar.getId() == R.id.songBar2) {
            musicSrv.seek(1, progress);
            updateProgressBar();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser){

        if(seekbar.getId() == R.id.seekBar) {
            musicSrv.updateVolume(progress);
        }
    }


    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void leftSongPicked(View view){
        pausedLeft = false;
        View pauseButton = findViewById(R.id.imageButton3);
        View playButton = findViewById(R.id.imageButton);
        pauseButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);

        int index = Integer.parseInt(view.getTag().toString());
        musicSrv.setLeftSong(index);

        TextView name = (TextView)findViewById(R.id.textView);
        TextView artist = (TextView)findViewById(R.id.textView3);
        name.setText(songList.get(index).getTitle());
        artist.setText(songList.get(index).getArtist());

        musicSrv.setLeftSong(Integer.parseInt(view.getTag().toString()));
        songBar1.setMax(musicSrv.getDur(0));
        songBar1.setProgress(musicSrv.getPosn(0));

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(findViewById(R.id.nav_view_left));
    }

    public void rightSongPicked(View view){
        pausedRight = false;
        View pauseButton = findViewById(R.id.imageButton4);
        View playButton = findViewById(R.id.imageButton2);
        pauseButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);

        int index = Integer.parseInt(view.getTag().toString());
        musicSrv.setRightSong(index);

        TextView name = (TextView)findViewById(R.id.textView2);
        TextView artist = (TextView)findViewById(R.id.textView4);
        name.setText(songList.get(index).getTitle());
        artist.setText(songList.get(index).getArtist());

        musicSrv.setRightSong(Integer.parseInt(view.getTag().toString()));
        songBar2.setMax(musicSrv.getDur(1));
        songBar2.setProgress(musicSrv.getPosn(1));

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(findViewById(R.id.nav_view_right));
    }

    public void playLeft(View view) {
        View pauseButton = findViewById(R.id.imageButton3);
        pauseButton.setVisibility(View.VISIBLE);
        view.setVisibility(View.INVISIBLE);
        if(pausedLeft) {
            musicSrv.go(0);
        }
        else
            musicSrv.playSong(0);
    }

    public void playRight(View view){
        View pauseButton = findViewById(R.id.imageButton4);
        pauseButton.setVisibility(View.VISIBLE);
        view.setVisibility(View.INVISIBLE);
        if(pausedRight) {
            musicSrv.go(1);
        }
        else
            musicSrv.playSong(1);
    }

    public void pauseLeft(View view){
        pausedLeft = true;
        View playButton = findViewById(R.id.imageButton);
        playButton.setVisibility(View.VISIBLE);
        view.setVisibility(View.INVISIBLE);
        musicSrv.pausePlayer(0);
    }

    public void pauseRight(View view){
        pausedRight = true;
        View playButton = findViewById(R.id.imageButton2);
        playButton.setVisibility(View.VISIBLE);
        view.setVisibility(View.INVISIBLE);
        musicSrv.pausePlayer(1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }
}
