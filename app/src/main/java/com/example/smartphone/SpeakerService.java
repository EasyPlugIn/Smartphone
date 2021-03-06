package com.example.smartphone;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class SpeakerService extends Service {
	static final String local_tag = SpeakerService.class.getSimpleName();
    
    static private boolean running = false;
    static private boolean working = false;
    HandlerThread handler_thread;
    Handler data_handler;
    int current_sound_Hz;

	AudioTrackManager ATM;

	HashMap<String, Integer> sound_name_table;
	int[] sound_index_table;
    
    private final IBinder mBinder = new MyBinder();
    public class MyBinder extends Binder {
        SpeakerService getService() {
            return SpeakerService.this;
        }
    }
    
    public SpeakerService () {
        running = true;
        working = false;
        data_handler = null;
        handler_thread = null;
        Utils.logging(local_tag, "constructor");
    }
    
    static boolean is_running () {
        return running;
    }
    
    @Override
    public void onCreate () {
        running = true;
        working = false;
        Utils.logging(local_tag, "onCreate");
        
        sound_name_table = new HashMap<String, Integer>();
        sound_name_table.put("Do-", 262);
        sound_name_table.put("Re-", 294);
        sound_name_table.put("Mi-", 330);
        sound_name_table.put("Fa-", 350);
        sound_name_table.put("So-", 392);
        sound_name_table.put("La-", 440);
        sound_name_table.put("Si-", 494);
        sound_name_table.put("Do",  524);
        sound_name_table.put("Re",  588);
        sound_name_table.put("Mi",  660);
        sound_name_table.put("Fa",  698);
        sound_name_table.put("So",  784);
        sound_name_table.put("La",  880);
        sound_name_table.put("Si",  988);
        sound_name_table.put("Do+", 1046);
        sound_name_table.put("Re+", 1174);
        sound_name_table.put("Mi+", 1318);
        sound_name_table.put("Fa+", 1396);
        sound_name_table.put("So+", 1568);
        sound_name_table.put("La+", 1760);
        sound_name_table.put("Si+", 1976);
        
        // set values based on sound_name_table
        sound_index_table = new int[sound_name_table.size()];
        int i = 0;
        for (int value : sound_name_table.values()) {
        	sound_index_table[i] = value;
        	i++;
        }
        Arrays.sort(sound_index_table);
        
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( !working ) {
        	working = true;
            ATM=new AudioTrackManager();
            current_sound_Hz = 0;
            DAN.subscribe("Speaker", new ODFSubscriber());
            
        } else {
            Utils.logging(local_tag, "already initialized");
            
        }
        return Service.START_NOT_STICKY;
    }

    class ODFSubscriber implements DAN.Subscriber {
        public void odf_handler (final String feature, final DAN.ODFObject odf_object) {
            if (!feature.equals("Speaker")) {
                Utils.logging(local_tag, "ODFSubscriber should only receive Speaker feature");
                return;
            }
            try {
                Utils.logging(local_tag, "%s: %d", odf_object.timestamp, odf_object.data.getInt(0));
                int new_sound_Hz = get_sound_rate(odf_object.data.getInt(0));
                Utils.logging(local_tag, "new_sound_Hz: %d", new_sound_Hz);
                if ( current_sound_Hz != new_sound_Hz ) {
                    if (current_sound_Hz == 0) {
                        ATM.isPlaySound = false;
                        ATM.stop();
                    }
                    current_sound_Hz = new_sound_Hz;
                    ATM.setTone(current_sound_Hz);
                    ATM.genTone();
                    ATM.isPlaySound = true;
                    ATM.playSound();
                }
            } catch (JSONException e) {
                Utils.logging(local_tag, "JSONException");
            }
        }
    };
    
    public int get_sound_rate (Object o) {
    	if ( o instanceof Integer ) {
    		int i = ((Integer) o).intValue();
    		if (0 <= i && i < sound_index_table.length) {
    			return sound_index_table[i];
    		}
    		return 0;
    	}
    	if ( o instanceof String ) {
    		Integer s = sound_name_table.get(o);
    		if (s == null) {
    			return 0;
    		}
    		return s;
    	}
    	return 0;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
    
    @Override
    public void onDestroy () {
        running = false;
        working = false;
        DAN.unsubscribe("Speaker");
		ATM.isPlaySound = false;
		ATM.stop();
    }
    
}