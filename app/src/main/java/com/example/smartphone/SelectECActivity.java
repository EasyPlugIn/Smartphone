package com.example.smartphone;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAN.DAN;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectECActivity extends Activity {
	static final String local_tag = SelectECActivity.class.getSimpleName();
	
	final ArrayList<ECListItem> ec_endpoint_list = new ArrayList<ECListItem>();
    ArrayAdapter<ECListItem> adapter;
	final DAN.Subscriber event_subscriber = new EventSubscriber();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.logging(local_tag, "========== SelectECActivity start ==========");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_ec_list);

        DAN.init(Constants.log_tag);
        if (DAN.session_status()) {
        	Utils.logging(local_tag, "Already registered, jump to SessionActivity");
            Intent intent = new Intent(SelectECActivity.this, FeatureActivity.class);
            startActivity(intent);
            finish();
        }
        
        adapter = new ECListAdapter(this, R.layout.item_ec_list, ec_endpoint_list);
        for (String i: DAN.available_ec()) {
            ec_endpoint_list.add(new ECListItem(i));
        }

        // show available EC ENDPOINTS
        final ListView lv_available_ec_endpoints = (ListView)findViewById(R.id.lv_available_ec_endpoints);
        lv_available_ec_endpoints.setAdapter(adapter);
        lv_available_ec_endpoints.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                    View view, int position, long id) {
            	ECListItem ec_list_item = ec_endpoint_list.get(position);
            	String clean_mac_addr = DAN.get_clean_mac_addr(Utils.get_mac_addr(SelectECActivity.this));
            	String EC_ENDPOINT = ec_list_item.ec_endpoint;
            	JSONObject profile = new JSONObject();
    	        try {
    		        profile.put("d_name", "Android"+ clean_mac_addr);
    		        profile.put("dm_name", Constants.dm_name);
    		        JSONArray feature_list = new JSONArray();
    		        for (String f: Constants.df_list) {
    		        	feature_list.put(f);
    		        }
    		        profile.put("df_list", feature_list);
    		        profile.put("u_name", Constants.u_name);
    		        profile.put("monitor", clean_mac_addr);
    	        	DAN.register(EC_ENDPOINT, clean_mac_addr, profile);
    			} catch (JSONException e) {
    				e.printStackTrace();
    			}
    	        ec_list_item.status = ECListItem.Status.CONNECTING;
    	        adapter.notifyDataSetChanged();
    	        Utils.show_ec_status_on_notification(SelectECActivity.this, EC_ENDPOINT, false);
            }
        });
        
    	DAN.subscribe(DAN.CONTROL_CHANNEL, event_subscriber);
    }
    
    @Override
    public void onPause () {
    	super.onPause();
		DAN.unsubcribe(event_subscriber);
    	if (isFinishing()) {
    		if (!DAN.session_status()) {
    	        Utils.remove_all_notification(SelectECActivity.this);
    			DAN.shutdown();
    		}
    	}
    }
    
    class EventSubscriber extends DAN.Subscriber {
	    public void odf_handler (final String feature, final DAN.ODFObject odf_object) {
			if (!feature.equals(DAN.CONTROL_CHANNEL)) {
				Utils.logging(local_tag, "EventSubscriber should only receive {} events", DAN.CONTROL_CHANNEL);
				return;
			}
	    	switch (odf_object.event_tag) {
			case FOUND_NEW_EC:
				Utils.logging(local_tag, "FOUND_NEW_EC: %s", odf_object.message);
				ec_endpoint_list.add(new ECListItem(odf_object.message));
    	    	runOnUiThread(new Thread () {
    	    		@Override
    	    		public void run () {
    	    			adapter.notifyDataSetChanged();
    	    		}
    	    	});
				break;
			case REGISTER_FAILED:
                for (ECListItem ec_list_item: ec_endpoint_list) {
                    if (ec_list_item.ec_endpoint == odf_object.message) {
                        ec_list_item.status = ECListItem.Status.FAILED;
                    }
                }
				runOnUiThread(new Thread () {
    	    		@Override
    	    		public void run () {
    					Toast.makeText(
    							getApplicationContext(),
    							"Register to "+ odf_object.message +" failed.",
    							Toast.LENGTH_LONG).show();
                        adapter.notifyDataSetChanged();
    	    		}
    	    	});
    	        Utils.remove_all_notification(SelectECActivity.this);
				break;
			case REGISTER_SUCCEED:
    	        Utils.show_ec_status_on_notification(SelectECActivity.this, DAN.ec_endpoint(), true);
	            Intent intent = new Intent(SelectECActivity.this, FeatureActivity.class);
	            startActivity(intent);
	            finish();
				break;
			default:
				break;
	    	}
	    }
	}
    
    public static class ECListItem {
        public enum Status {
            UNKNOWN,
            FAILED,
            CONNECTING,
            // no SUCCEED needed, because once successfully connected to an EC,
            //  there would be no reason to stay in this Activity
        }
    	public String ec_endpoint;
    	public Status status;
    	public ECListItem (String e) {
    		this.ec_endpoint = e;
    		this.status = Status.UNKNOWN;
    	}
    }

	public class ECListAdapter extends ArrayAdapter<ECListItem> {
	    Context context;
	    int layout_resource_id;
	    ArrayList<ECListItem> data = null;
	    
	    public ECListAdapter (Context context, int layout_resource_id, ArrayList<ECListItem> data) {
	        		super(context, layout_resource_id, data);
	        this.context = context;
	        this.layout_resource_id = layout_resource_id;
	        this.data = data;
	    }
	    
	    @Override
	    public View getView (int position, View convertView, ViewGroup parent) {
	        View row = convertView;
	        ECEndpointHolder holder = null;
	        
	        if(row == null) {
	            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
	            row = inflater.inflate(layout_resource_id, parent, false);
	            holder = new ECEndpointHolder();
	            holder.ec_endpoint = (TextView)row.findViewById(R.id.tv_ec_endpoint);
	            holder.connecting = (TextView)row.findViewById(R.id.tv_connecting);
	            row.setTag(holder);
	        } else {
	            holder = (ECEndpointHolder)row.getTag();
	        }
	        
	        ECListItem i = data.get(position);
	        holder.ec_endpoint.setText(i.ec_endpoint.replace("http://", "").replace(":9999", ""));
	        switch (i.status) {
                case UNKNOWN:
                    holder.connecting.setText("");
                    break;
                case FAILED:
                    holder.connecting.setText("x");
                    break;
                case CONNECTING:
                    holder.connecting.setText("...");
                    break;
            }
	        return row;
	    }
	
	    class ECEndpointHolder {
	        TextView ec_endpoint;
	        TextView connecting;
	    }
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, Constants.MENU_ITEM_ID_DAN_VERSION, 0, "DAN Version: "+ DAN.version);
        menu.add(0, Constants.MENU_ITEM_ID_DAI_VERSION, 0, "DAI Version: "+ Constants.version);
        menu.add(0, Constants.MENU_ITEM_WIFI_SSID, 0, "WiFi: "+ Utils.get_wifi_ssid(getApplicationContext()));
        return super.onPrepareOptionsMenu(menu);
    }
}
