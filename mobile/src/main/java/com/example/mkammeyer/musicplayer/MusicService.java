package com.example.mkammeyer.musicplayer;

import java.util.ArrayList;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by mkammeyer on 9/23/15.
 */
//
//, MediaPlayer.OnErrorListener,
//        MediaPlayer.OnCompletionListener
public class MusicService extends Service {

    //media player
    int numSongs;
    private MediaPlayer player;
    private MediaPlayer player2;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    private final IBinder musicBind = new MusicBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCreate(){
        //create the service
        //create the service
        super.onCreate();
        //initialize position
        numSongs=0;
        songPosn=0;
        //create player
        player = new MediaPlayer();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        player2 = new MediaPlayer();
        player2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player2.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player2.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void updateVolume(int sliderVal){
        if(sliderVal == 50){
            player.setVolume(1, 1);
            player2.setVolume(1, 1);
        }
        else if(sliderVal > 50 ){
            float vol1 = 1 - ((sliderVal - 50)/ 50.0f);
            player2.setVolume(1, 1);
            player.setVolume(vol1, vol1);
        }
        else if(sliderVal < 50){
            float vol2 = 1 - ((50 - sliderVal)/ 50.0f);
            player.setVolume(1, 1);
            player2.setVolume(vol2, vol2);
        }

    }

    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        if(numSongs % 2 == 0) {
            //play a song
            player.reset();
            //get song
            Song playSong = songs.get(songPosn);
            //get id
            long currSong = playSong.getID();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);

            try {
                player.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }

            player.prepareAsync();
        }
        else {
            //play a song
            player2.reset();
            //get song
            Song playSong = songs.get(songPosn);
            //get id
            long currSong = playSong.getID();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);

            try {
                player2.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }

            player2.prepareAsync();
        }
        numSongs++;
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

//    @Override
//    public void onPrepared(MediaPlayer mp) {
//        //start playback
//        mp.start();
//    }
}