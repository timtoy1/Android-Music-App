package com.example.mkammeyer.musicplayer;

        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuItem;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.Comparator;
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
        import android.widget.SeekBar;
        import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends AppCompatActivity implements OnSeekBarChangeListener{

    private ArrayList<Song> songList;
    private ListView rightSongView;
    private ListView leftSongView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private SeekBar faderBar;
    private boolean pausedLeft = false;
    private boolean pausedRight = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dj);


        leftSongView = (ListView)findViewById(R.id.left_song_list);
        rightSongView = (ListView)findViewById(R.id.right_song_list);
        songList = new ArrayList<Song>();

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
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekbar){

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekbar){

    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser){
        musicSrv.updateVolume(progress);
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
        musicSrv.setLeftSong(Integer.parseInt(view.getTag().toString()));
    }

    public void rightSongPicked(View view){
        pausedRight = false;
        View pauseButton = findViewById(R.id.imageButton4);
        View playButton = findViewById(R.id.imageButton2);
        pauseButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);
        musicSrv.setRightSong(Integer.parseInt(view.getTag().toString()));
    }

    public void playLeft(View view){
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
