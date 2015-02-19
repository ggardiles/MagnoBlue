package course.labs.intentslab.mybrowser;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.os.Build;

public class ActivityGo extends Activity {
	Button BacktoMainBtn;
	String TAGtreat="DataTreatment";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activity_go);
		Log.d(TAGtreat,"Entered ActivityGo");
		BacktoMainBtn = (Button) findViewById(R.id.button1);
		BacktoMainBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				Log.d(TAGtreat,"Exit ActivityGo");
			}
		});
		
	}

	
}
