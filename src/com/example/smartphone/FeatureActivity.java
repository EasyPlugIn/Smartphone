package com.example.smartphone;

import org.json.JSONException;

import DAN.DAN;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

public class FeatureActivity extends Activity implements FeatureFragment.DeregisterCallback {
	final String version = "20160408";
    
    final int MENU_ITEM_ID_DAN_VERSION = 0;
    final int MENU_ITEM_ID_DAI_VERSION = 1;
    final int MENU_ITEM_REQUEST_INTERVAL = 2;
    final int MENU_ITEM_REREGISTER = 3;
	
    final String TITLE_FEATURES = "Features";
    final String TITLE_DISPLAY = "Display";
	
    final FragmentManager fragment_manager = getFragmentManager();
    FeatureFragment feature_fragment;
    DisplayFeatureListFragment display_feature_list_fragment;
    DisplayFeatureDataFragment display_feature_data_fragment;
	final EventSubscriber event_subscriber = new EventSubscriber();
	final ODFSubscriber display_subscriber = new ODFSubscriber();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_feature);
        
    	if (!DAN.session_status()) {
    		Intent intent = new Intent(FeatureActivity.this, SelectECActivity.class);
            startActivity(intent);
            finish();
    	}
        
        final ActionBar actionbar = getActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionbar.setDisplayHomeAsUpEnabled(true);

        feature_fragment = (FeatureFragment) fragment_manager.findFragmentById(R.id.frag_features);
        display_feature_list_fragment = (DisplayFeatureListFragment) fragment_manager.findFragmentById(R.id.frag_display_feature_list);
        display_feature_data_fragment = (DisplayFeatureDataFragment) fragment_manager.findFragmentById(R.id.frag_display_feature_data);
        
        ActionBar.TabListener tablistener = new ActionBar.TabListener () {
    		@Override
    		public void onTabSelected(Tab tab, FragmentTransaction ft) {
    			switch ((String) tab.getText()) {
    			case TITLE_FEATURES:
    				ft.show(feature_fragment);
    				DAN.unsubscribe("Display");
    				break;
    			case TITLE_DISPLAY:
    				ft.show(display_feature_list_fragment);
    				DAN.subscribe("Display", display_subscriber);
    				break;
    			}
    		}

    		@Override
    		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    			switch ((String) tab.getText()) {
    			case TITLE_FEATURES:
    				ft.hide(feature_fragment);
    				break;
    			case TITLE_DISPLAY:
    				ft.hide(display_feature_list_fragment);
    				break;
    			}
    		}

    		@Override
    		public void onTabReselected(Tab tab, FragmentTransaction ft) {
    		}
    	};

    	actionbar.addTab(actionbar.newTab().setText(TITLE_FEATURES).setTabListener(tablistener));
    	actionbar.addTab(actionbar.newTab().setText(TITLE_DISPLAY).setTabListener(tablistener));
    	
    	fragment_manager.beginTransaction()
    			.show(feature_fragment)
    			.hide(display_feature_list_fragment)
    			.hide(display_feature_data_fragment)
    			.commit();
        
    	DAN.subscribe("Control_channel", event_subscriber);

    	feature_fragment.show_d_name_on_ui(DAN.get_d_name());
		feature_fragment.show_ec_status_on_ui(DAN.ec_endpoint(), DAN.session_status());
    }
    
	@Override
	public void trigger() {
		DAN.deregister();
		DAN.shutdown();
		Utils.remove_all_notification(FeatureActivity.this);
        finish();
	}
	
	class EventSubscriber extends DAN.Subscriber {
	    public void odf_handler (final DAN.ODFObject odf_object) {
	    	runOnUiThread(new Thread () {
	    		@Override
	    		public void run () {
    	    		switch (odf_object.event_tag) {
        	        case REGISTER_FAILED:
        	        	feature_fragment.show_ec_status_on_ui(odf_object.message, false);
        	        	Utils.show_ec_status_on_notification(FeatureActivity.this, odf_object.message, false);
        	        	break;
        	        	
        	        case REGISTER_SUCCEED:
        	        	feature_fragment.show_ec_status_on_ui(odf_object.message, true);
        	        	Utils.show_ec_status_on_notification(FeatureActivity.this, odf_object.message, true);
        	        	String d_name = DAN.get_d_name();
        	        	logging("Get d_name:"+ d_name);
        				TextView tv_d_name = (TextView)findViewById(R.id.tv_d_name);
        				tv_d_name.setText(d_name);
        				break;
        	        }
	    		}
	    	});
	    }
	};
	
	class ODFSubscriber extends DAN.Subscriber {
	    public void odf_handler (final DAN.ODFObject odf_object) {
	    	// send feature list to display_device_list_fragment
			runOnUiThread(new Thread () {
				@Override
				public void run () {
					try {
						display_feature_list_fragment.set_newest_metadata(
								odf_object.dataset.newest().data.getJSONObject(0));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
	    	
	    	// send feature data to display_feature_data_fragment
	    }
	};
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, MENU_ITEM_ID_DAN_VERSION, 0, "DAN Version: "+ DAN.version);
        menu.add(0, MENU_ITEM_ID_DAI_VERSION, 0, "DAI Version: "+ version);
        menu.add(0, MENU_ITEM_REQUEST_INTERVAL, 0, "Request Interval: "+ DAN.get_request_interval() +" ms");
        menu.add(0, MENU_ITEM_REREGISTER, 0, "Reregsiter to another EC");
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        case MENU_ITEM_REQUEST_INTERVAL:
        	show_input_dialog("Change Request Interval", "Input a integer as request interval (unit: ms)", ""+ DAN.get_request_interval());
            break;
        case MENU_ITEM_REREGISTER:
        	show_selection_dialog("Reregister to EC", DAN.available_ec());
        	break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void show_input_dialog (String title, String message, String hint) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title);
        dialog.setMessage(message);

        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialog.setView(input);
        
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener () {
            public void onClick (DialogInterface dialog, int id) {
                String value = input.getText().toString();
                try {
                	DAN.set_request_interval(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                	logging("Input is not a integer");
                }
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener () {
            public void onClick (DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        dialog.create().show();
    }

    private void show_selection_dialog (String title, String[] available_ec) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title);
        final ArrayAdapter<String> array_adapter = new ArrayAdapter<String>(
        		this,
                android.R.layout.select_dialog_item);
        array_adapter.addAll(available_ec);
        
        dialog.setAdapter(array_adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String which_ec = array_adapter.getItem(which);
                logging("selected: "+ which_ec);
                DAN.reregister(which_ec);
            }
        });
        dialog.create().show();
    }
    
    static public void logging (String message) {
        Log.i(Constants.log_tag, "[SessionActivity] " + message);
    }

}
