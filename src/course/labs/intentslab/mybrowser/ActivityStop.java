package course.labs.intentslab.mybrowser;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.os.Build;

public class ActivityStop extends Activity {
	Button BacktoMainBtn;
	String TAGtreat="DataTreatment";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activity_stop);
		Log.d(TAGtreat,"Entered ActivitySTOP");
		BacktoMainBtn = (Button) findViewById(R.id.button1);
		BacktoMainBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				Log.d(TAGtreat,"Exit ActivitySTOP");
			}
		});

	}



	

}
