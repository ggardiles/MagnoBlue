package course.labs.intentslab.mybrowser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MyBrowserActivity extends Activity {

	private static final String TAG = "MyBrowser";
	private static final String TAGin = "InputDataMyBrowser";
	private static final String TAGtreat = "DataTreatment";
	private static final String CalFile = "CalNoToolsData.txt";
	private static final String MeasureFile = "MeasureNoToolsData.txt";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	//Settings values MODIFIABLE
	private String address = "00:06:66:48:55:FB";
	private String strbtDevice = "RN42-55FB";
	private int numValuesAcq = 25;

	//UI Views
	 Button btnOn;
	 private RadioGroup radioGroup;
	 private RadioButton radioCalib;
	 private RadioButton radioCompare;
	TextView txtArduino;
	
	//Progressbar data
	ProgressDialog progressBar;
	private int progressBarStatus = 0;
	private Handler progressBarHandler = new Handler();
	
	
	//Thread
	Handler h;
	private ConnectedThread mConnectedThread;
	
	//BT non modifiable
	final int RECIEVE_MESSAGE = 1; // Status for Handler
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();

	
	//Global modifiable values
	String sbprint;
	int checker = 1, isFirstIteration=1,CurrentNumCount=-1, compare2check4=-1;;
	int STATE_ACQ = 0; // 0 for calibration, 1 for comparison
	
	//Database Storage
	ArrayList<ArrayList<Integer>> DataCompare = new ArrayList<ArrayList<Integer>>();
	ArrayList<ArrayList<Integer>> DataCalib = new ArrayList<ArrayList<Integer>>();
	ArrayList<Integer> currentnum = new ArrayList<Integer>();

	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "Entered onCreate()");
		// Set the User Interface
		setUIElements();

		// Get the Bluetooth Adapter Pointer
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		// Check For BluetoothState
		checkBTState();

		h = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case RECIEVE_MESSAGE:
					//Log.d(TAGin, "Entered RECEIVE_MESSAGE");

					// Create Byte Object for In Buffer
					byte[] readBuf = (byte[]) msg.obj;

					// Byte passed onto String
					String strIncom = new String(readBuf, 0, msg.arg1);
					//Log.d(TAGin, "Entered RECEIVE_MESSAGE 2: String strIncom:"
							//+ strIncom);

					// String appended onto StringBuilder
					sb.append(strIncom);

					// Check length of StringBuilder
					int endOfLineIndex = sb.length() - 1;
					//Log.d(TAGin, "Entered RECEIVE_MESSAGE 3: endOfLine:"
						//	+ String.valueOf(endOfLineIndex));

					// It reads twice, first one letter, then the rest.
					// After we have completed the second read we can form a
					// String with the msg.
					if (true) {

						// String sbprint contains the message
						sbprint = sbprint + sb.toString();

						// Delete the String Builder
						// sb.delete(0, sb.length());
						sb.setLength(0);

						if (sbprint.substring(sbprint.length() - 1).equals("#")) {
							// sbprint= sbprint.substring(0,sbprint.length() -
							// 1);
							Log.d(TAGin,
									"Entered RECEIVE_MESSAGE Final sbPrint:"
											+ sbprint);
							if (sbprint.length() >=9){
							ReceivedTreat(sbprint);
							}else{
								Log.d(TAGin,
										sbprint +" is not correct length. Didnt enter ReceivedTreat()");
							}
							
							sbprint = "";
							Log.d(TAGin,
									"sbprint cleared");
							// checker = 0;
						}

						// Modify the TextView
						//txtArduino.setText("Data from Arduino: " + sbprint);
					}
					break;
				}
			};
		};
		Log.d(TAG, "Exit onCreate()");
	}

	//Treats final String received to save it into an ArrayList
	public void ReceivedTreat(String sbprint) {
		
		currentnum = new ArrayList<Integer>();

		int changer = 1, pos = 0, letterpos = 0;
		String SumCheck = "-1";
		boolean check, check2 = true, check3= true, check4=true, isCorrect = true;

		// Divides Input String into important parts
		if (sbprint.substring(sbprint.length() - 1).equals("#") && sbprint.substring(sbprint.length() - 3, sbprint.length()-2).equals("~")) {
			SumCheck = sbprint.substring(sbprint.length() - 2,
					sbprint.length() - 1);
			try {
				
				Integer.parseInt(SumCheck);
				check2 = true;
			} catch (Exception e) {
				check2 = false;
			}
			sbprint = sbprint.substring(0, sbprint.length() - 3);
		}else{
			check2=false;
		}
		
		Log.d(TAGtreat, "sbprint: " + sbprint + " SumCheck: " + SumCheck);
		Log.d(TAGtreat,"Check2= " +String.valueOf(check2));
		//CHECK2: if SumCheck is actually a numerical value

		
		//CHECK3: checks for x10y-z30 
		int tempNonIntCount=0;
		int numCount=0;
		String tempSubStr="";
		for (int u=0;u<sbprint.length()-1;u++){
			tempSubStr=sbprint.substring(u,u+1);
			try{
				Integer.parseInt(tempSubStr);
				tempNonIntCount=0;
				numCount++;
			}catch (Exception e){
				tempNonIntCount++;
				if (tempNonIntCount==3){
					check3=false;
					break;
				}
			}
		}
		Log.d(TAGtreat,"Check3= " +String.valueOf(check3));
		//CHECK4: checks for '-' that are changed for number x10y4400z50 instead of x10y-400z50
		/*	
		check4=true;
		if (isFirstIteration==1){
			CurrentNumCount=numCount;
			Log.d("temporal111","CurrentNumCount= " +String.valueOf(CurrentNumCount));
			isFirstIteration=0; 
		}else if (isFirstIteration == 0){
			Log.d("temporal111","numCount= " +String.valueOf(numCount));
			if (compare2check4==numCount){
				CurrentNumCount=numCount;
			}
			if (!(CurrentNumCount==numCount)){
				check4=false;
				compare2check4=numCount;
			}
			
		}
		
		Log.d(TAGtreat,"Check4= " +String.valueOf(check4));*/
		
		//CHECK= combination of all checks. HAS TO BE TRUE TO TREAT DATA INPUT.
		//ALSO: Starts by x && Length: 6 < L < 22
		check = sbprint.substring(0, 1).equals("x") && sbprint.length() > 6
				&& sbprint.length() < 22 && check2 && check3 && check4;
		Log.d(TAGtreat, "Check = " + String.valueOf(check));

		// If there is minimum quality we continue to save vectors to a
		// ArrayList of ArrayLists
		if (check) {

			for (int j = 0; j < sbprint.length(); j++) {
				// If it is a number, we will save a position to cut the string
				// from there and convert to integer
				// If it is not a number we will take account of which symbol
				// and if it is not an allowed character saving
				// process is terminated for this InputStream
				try {
					Integer.parseInt(sbprint.substring(j, j + 1));

					if (pos == 0) {
						pos = j;

					}
					//Log.d(TAGtreat, "Position " + String.valueOf(j)
						//	+ " saved\n");
				} catch (Exception e) {
					//Log.d(TAGtreat, sbprint.substring(j, j + 1));
					if (sbprint.substring(j, j + 1).equals("-")) {
						changer = -1;
						//Log.d(TAGtreat, "Entered changer" + "\n");
					} else {
						if (sbprint.substring(j, j + 1).equals("x")) {
							//Log.d(TAGtreat, "We are in X" + "\n");
							letterpos = 0;
						} else if (sbprint.substring(j, j + 1).equals("y")) {
							//Log.d(TAGtreat, "We are in Y" + "\n");
							letterpos = 1;
						} else if (sbprint.substring(j, j + 1).equals("z")) {
							//Log.d(TAGtreat, "We are in Z" + "\n");
							letterpos = 2;
						} else {
							isCorrect = false;
							break;
						}

					}
					if (pos != 0) {
						int myNumber = Integer.parseInt(sbprint.substring(pos,
								j)) * changer;
						currentnum.add(myNumber);
						//Log.d(TAGtreat,
							//	"Final number saved: "
								//		+ String.valueOf(currentnum
									//			.get(letterpos - 1)) + "\n");
						pos = 0;
						changer = 1;
					}

				}
			}
			if (pos != 0 && isCorrect) {
				currentnum.add(Integer.parseInt(sbprint.substring(pos,
						sbprint.length()))
						* changer);
				//Log.d(TAGtreat,
					//	"Final number saved OUT of loop: "
						//		+ String.valueOf(currentnum.get(letterpos))
							//	+ "\n");
				pos = 0;
				changer = 1;
			}
			
			if (isCorrect) {
				// Sum all values of the vector
				int sum = 0;
				for (int i = 0; i < currentnum.size(); i++) {
					sum += currentnum.get(i);
				}
				// Convert the sum to string, take last value and check with
				// .equal(SumCheck)
				String tempStr = String.valueOf(sum);
				tempStr = tempStr.substring(tempStr.length() - 1);
				//Log.d(TAGtreat, "The sum is: " + String.valueOf(sum));
				Log.d(TAGtreat, "SumCheck on Android: " + tempStr);
				// Final check for SumCheck
				if (tempStr.equals(SumCheck)) {
					if (STATE_ACQ==0){
						Log.d(TAGtreat,"DataCompare Size= "+ String.valueOf(DataCompare.size()));
						if (DataCalib.size()<numValuesAcq){
						DataCalib.add(currentnum);
						}
						else if (DataCalib.size()==numValuesAcq){
							btnOn.setEnabled(false);
							radioCompare.setEnabled(true);
							mConnectedThread.write("0"); // Send "1" via Bluetooth
							Log.d(TAGtreat, "FINAL "+String.valueOf(DataCalib.size())+" Calibration FINAL data stored.");
						} else{
							Log.d(TAGtreat, "UNECESSARY Calibration data received.");
						}
					}
					if (STATE_ACQ==1){
						Log.d(TAGtreat,"DataCompare Size= "+ String.valueOf(DataCompare.size()));
						if (DataCompare.size()<numValuesAcq){
						DataCompare.add(currentnum);
						}
						else if (DataCompare.size()==numValuesAcq){
							
							mConnectedThread.write("0"); // Send "1" via Bluetooth
							Log.d(TAGtreat, "FINAL "+String.valueOf(DataCompare.size())+" Comparisson FINAL data stored.");
							//Prepare for Check
							btnOn.setText(R.string.btn_Check);
							//btnOn.setBackgroundColor(getResources().getColor(R.color.myGreen));
							//btnOn.invalidate();
							STATE_ACQ=3;
						}else{
							Log.d(TAGtreat, "UNNECESSARY Comparisson data received.");
						}
					}
					
					Log.d(TAGtreat,
							"Values added: ["
									+ String.valueOf(currentnum.get(0)) + ","
									+ String.valueOf(currentnum.get(1)) + ","
									+ String.valueOf(currentnum.get(2)) + "]");
					
					
					
				} else {
					Log.d(TAGtreat, "SumChecker not the same");
				}
			} else {
				Log.d(TAGtreat, "Wrong String Received");
			}

		}// end of if(check)
		else{
			Log.d(TAGtreat, "Skipped whole ReceiveTreat()");
		}
		Log.d(TAGtreat, "Exit ReceiveTreat();");
	}
	

	//Makes mean of two ArrayList with Calibration and Comparison Data
	public float[] MakeMean(ArrayList<ArrayList<Integer>> full){
		int myArraySize= full.size();
		float[] dataCal;
		dataCal = new float[3];
		//check if the Size is bigger than 1
		if (myArraySize>1){
		//To Calculate average value of vector data at different times
		

		for(int k=0;k<3;k++){
		for (int y=0;y<myArraySize;y++){
				dataCal[k]=dataCal[k] + full.get(y).get(k)/(float)full.size();
			}
		}
		//To check dataCal and print it in the debugger
		String TempStrVect= floatArrayToString(dataCal);
		Log.d(TAGtreat,"Final mean in MakeandPrintMean function: " + TempStrVect);
		
		
		// Modify the TextView NOT GONNA DO IT HERE FOR NOW
		//txtArduino.setText("Data from Arduino: " + vectStrtemp);
		
		}
		return dataCal;
	}
	
	//Converts floats array into a PRINTABLE string for Debugger and setText()
	public String floatArrayToString(float[] dataCal){
		String vectStrtemp="";
		for (int u=0;u<3;u++){
			vectStrtemp= vectStrtemp + dataCal[u]+", ";
		}
		vectStrtemp = "["+vectStrtemp.substring(0, vectStrtemp.length()-2)+"]";
		return vectStrtemp;
	}
	
	//Checks if the vectors holding the mean are sufficiently equal
	public Boolean CheckMeans(float[] vect1,float[] vect2){
		boolean Check=true;
		//Factor A determines the maximum absolute difference of vector coordinates
		float A= (float) 25;
		
		for (int u=0;u<3;u++){
			// BELOW THAT HIGH VALUE && OVER SMALL VALUE
				if (Math.abs(vect2[u])<= A + Math.abs(vect1[u]) && Math.abs(vect2[u])>= Math.abs(vect1[u]) - A){
				}else{
				Check = false;
				break;
			}
		}
		return Check;
	}
         
	
	public void setUIElements() {
		// Set UI Elements
		
		//RadioButtons
		
		radioCalib=(RadioButton)findViewById(R.id.radioCalib);
        radioCompare=(RadioButton)findViewById(R.id.radioCompare); 
        if (DataCalib.size()<numValuesAcq) radioCompare.setEnabled(false);

		// Buttons
		// btnSettings = (Button) findViewById(R.id.buttonSettings);
		
		
		

		btnOn = (Button) findViewById(R.id.btnOn);
		btnOn.setEnabled(true);


		// TextView
		txtArduino = (TextView) findViewById(R.id.txtArduino);
		txtArduino.setText("Arduino not Connected");
		
		radioCalib.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				STATE_ACQ=0;
				btnOn.setText(R.string.btn_Recieve);
				Log.d(TAGtreat, "STATE_ACQ = " + String.valueOf(STATE_ACQ) +" All previous data has been errased.");
				isFirstIteration = 1;
				CurrentNumCount=0;
				DataCalib = new ArrayList<ArrayList<Integer>>();
				DataCompare= new ArrayList<ArrayList<Integer>>();
				Toast.makeText(getBaseContext(), "Saved data has been reset.",
						Toast.LENGTH_SHORT).show();
				btnOn.setEnabled(true);
				
			}
		});
		
		radioCompare.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					
					STATE_ACQ = 1;
					btnOn.setText(R.string.btn_Recieve);
					isFirstIteration = 1;
					CurrentNumCount=0;
					Log.d(TAGtreat, "STATE_ACQ = " + String.valueOf(STATE_ACQ) + "Saving on DataCompare.");
					btnOn.setEnabled(true);
					
			}
		});
		
		
		// Set Listeners
		
		/*
		btnCheck.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				float[] CalibMean, CompareMean;
				CalibMean=MakeMean(DataCalib);
				String CalibMeantoPrint= floatArrayToString(CalibMean);
				Log.d(TAGtreat,"Final CalibMeantoPrint in: " + CalibMeantoPrint);
				CompareMean=MakeMean(DataCompare);
				String CompareMeantoPrint= floatArrayToString(CompareMean);
				Log.d(TAGtreat,"Final CompareMeantoPrint in: " + CompareMeantoPrint);
				
				//Final decision
				boolean CheckFinal = CheckMeans(CalibMean,CompareMean);
				Log.d(TAGtreat,"CheckFinal: " + String.valueOf(CheckFinal));
				//Display new Activity to User
				lanzarRespuesta(CheckFinal);
				/*
				String FinalDecision="";
				
				if (CheckFinal){
					FinalDecision = "No tool has been left behind!";
				}else{
					FinalDecision = "Surgeon there is a retained tool!";
				}
				//Print in Arduino
				txtArduino.setText("Data from Arduino: \n" + "Calib: " + CalibMeantoPrint + "\nCompare: " +
						CompareMeantoPrint + "\n\n" + FinalDecision);
						
			}
		});*/

		btnOn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!(STATE_ACQ==3)){
				
				Toast.makeText(getBaseContext(), "Acquiring Data. Please Wait.",
						Toast.LENGTH_SHORT).show();
				btnOn.setEnabled(true);


				mConnectedThread.write("1"); // Send "1" via Bluetooth
				
				// prepare for a progress bar dialog
				progressBar = new ProgressDialog(v.getContext());
				progressBar.setCancelable(true);
				progressBar.setMessage("Acquiring Data ...");
				progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressBar.setProgress(0);
				progressBar.setMax(100);
				progressBar.show();
				
				//reset progress bar status
				progressBarStatus = 0;


				new Thread(new Runnable() {
				  public void run() {
					while (progressBarStatus < 100) {

					  // process some tasks
					  if (STATE_ACQ==0){
						  progressBarStatus = (DataCalib.size()*100)/numValuesAcq;
					  }else if(STATE_ACQ==1){
						  progressBarStatus = (DataCompare.size()*100)/numValuesAcq;
					  }

					  // your computer is too fast, sleep 0.5 seconds
					  try {
						Thread.sleep(50);
					  } catch (InterruptedException e) {
						e.printStackTrace();
					  }

					  // Update the progress bar
					  progressBarHandler.post(new Runnable() {
						public void run() {
						  progressBar.setProgress(progressBarStatus);
						}
					  });
					}

					// ok, file is downloaded,
					if (progressBarStatus >= 100) {

						// sleep 1 seconds, so that you can see the 100%
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// close the progress bar dialog
						progressBar.dismiss();
					}
				  }
			       }).start();

				
			} else if (STATE_ACQ==3){
				
				float[] CalibMean, CompareMean;
				CalibMean=MakeMean(DataCalib);
				String CalibMeantoPrint= floatArrayToString(CalibMean);
				Log.d(TAGtreat,"Final CalibMeantoPrint in: " + CalibMeantoPrint);
				CompareMean=MakeMean(DataCompare);
				String CompareMeantoPrint= floatArrayToString(CompareMean);
				Log.d(TAGtreat,"Final CompareMeantoPrint in: " + CompareMeantoPrint);
				
				//Final decision
				boolean CheckFinal = CheckMeans(CalibMean,CompareMean);
				Log.d(TAGtreat,"CheckFinal: " + String.valueOf(CheckFinal));
				//Display new Activity to User
				lanzarRespuesta(CheckFinal);
			}
				
			}//end of onclick()
		});
		
		
		/*
		 * btnSettings.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { Toast.makeText(getBaseContext(),
		 * "No Settings Available yet", Toast.LENGTH_SHORT).show(); } });
		 */

	}

	//Shows new activity to User indicating if No Tools are Left Behid or not
	public void lanzarRespuesta(Boolean value) {
		if (value){
			Log.d(TAGtreat,"to ActivityGo");
		 Intent i = new Intent(this, ActivityGo.class);//Creamos un nuevo intent para llamar a la siguiente actividad
	        startActivityForResult(i,1);//Ejecutamos la actividad para que muestre la segunda actividad
	        Log.d(TAGtreat,"Exit MyBrowserActivity");
		}else if (!value){
			Log.d(TAGtreat,"to ActivityStop");
	        	Intent i = new Intent(this, ActivityStop.class);//Creamos un nuevo intent para llamar a la siguiente actividad
		        startActivityForResult(i,1);
		        Log.d(TAGtreat,"Exit MyBrowserActivity");
	        }
		else{
			Toast.makeText(getBaseContext(), "Failed to compare.",
					Toast.LENGTH_SHORT).show();
			Log.d(TAGtreat,"Failed to jump to next activity on lanzarRespuesta();");
		}
		
	}
	// Bluetooth Socket Server Creator
	private BluetoothSocket createBluetoothSocket(BluetoothDevice device)
			throws IOException {
		if (Build.VERSION.SDK_INT >= 10) {
			try {
				final Method m = device.getClass().getMethod(
						"createInsecureRfcommSocketToServiceRecord",
						new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection", e);
			}
		}
		return device.createRfcommSocketToServiceRecord(MY_UUID);
	}

	public void ConnectToDevice() {
		// Set up a pointer to the remote Device using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		try {
			Log.d(TAG, "CreateBluetoothSocket(device) initiated.");
			btSocket = createBluetoothSocket(device);
			Log.d(TAG, "CreateBluetoothSocket(device) FINALIZED.");
		} catch (IOException e) {
			Log.d(TAG, "Socket creation failed: ");
		}

		// Make sure Discovery doesn't take any Resources
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.

		try {
			Log.d(TAG, "...Connecting...");
			btSocket.connect();
			Log.d(TAG, "....Connection ok...");
			txtArduino.setText("Arduino Connected.");
			btnOn.setEnabled(true);

			// Create a data stream so we can talk to server.
			Log.d(TAG, "...Create Out and In Stream...");

			mConnectedThread = new ConnectedThread(btSocket);
			mConnectedThread.start();

			Log.d(TAG, "...Stream Created...");
			//btnConnect.setEnabled(false);
		} catch (IOException e) {
			try {
				Log.d(TAG, "....Connection FAILED...");
				//btnConnect.setEnabled(true);
				btSocket.close();
			} catch (IOException e2) {
				Log.d(TAG,"Unable to close socket during connection failure");
			}
		}

	}

	
	private void checkBTState() {
		// Check for Bluetooth support and then check to make sure it is turned
		// on
		if (btAdapter == null) {
			Log.d(TAG,"Bluetooth not supported");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(TAG, "...Bluetooth ON...");
			} else {
				// Prompt user to turn on Bluetooth
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 1);
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()
			// int bytes2,bytes3,finalbytes;

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer); // Get number of bytes and
														// message in "buffer"
					/*
					 * bytes2 = mmInStream.read(buffer);
					 * bytes3=mmInStream.read(buffer);
					 * finalbytes=bytes*100+bytes2*10+bytes3;
					 */
					Log.d(TAG,
							"Connectedthread receivedA:"
									+ String.valueOf(bytes));
					/*
					 * Log.d(TAG, "Connectedthread receivedB:" +
					 * String.valueOf(bytes2)); Log.d(TAG,
					 * "Connectedthread receivedC:" + String.valueOf(bytes3));
					 * Log.d(TAG, "Connectedthread receivedFinal:" +
					 * String.valueOf(finalbytes));
					 */
					h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer)
							.sendToTarget(); // Send to message queue Handler
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {				
			Log.d(TAG, "...Data to send: " + message + "...");
			byte[] msgBuffer = message.getBytes();
			try {
				mmOutStream.write(msgBuffer);
			} catch (IOException e) {
				Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
			}
			
		}
	}

	
	
	
	public void guardar1(String nomarchivo, ArrayList<ArrayList<Integer>> full) {
		try {
			Log.d(TAG, " Entered save file on "  + nomarchivo);
			File tarjeta = Environment.getExternalStorageDirectory();
			File myFile = new File(tarjeta.getAbsolutePath(), nomarchivo);
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			
			myOutWriter.write("[ ");
			for (int y=0;y<full.size();y++){
				for(int k=0;k<3;k++){
					myOutWriter.write(full.get(y).get(k) + ",");
				}
				myOutWriter.write("+");
			}
			myOutWriter.flush();
			myOutWriter.close();
			fOut.close();
			Log.d(TAG, " File saved on " + nomarchivo);
		} catch (Exception e) {
			Log.e(TAG, "Data NOT saved in " + nomarchivo);
		}
	}

	private void readFromFile1(String nomarchivo) {
		Log.d(TAG, " Entered read file on " + nomarchivo);
		// String nomarchivo = "Data.txt";
		File tarjeta = Environment.getExternalStorageDirectory();
		File file = new File(tarjeta.getAbsolutePath(), nomarchivo);
		try {
			FileInputStream fIn = new FileInputStream(file);
			InputStreamReader archivo = new InputStreamReader(fIn);
			BufferedReader br = new BufferedReader(archivo);
			String linea = br.readLine();
			String todo = "";
			while (linea != null) {
				// todo is the string that is read
				todo = todo + linea + "";
				linea = br.readLine();
			}
			Log.d(TAG, " full read file has " + String.valueOf(todo.length())
					+ " Characters");
			br.close();
			archivo.close();
			// Cambiar en proximas versiones ubicaciones datos fecha

		} catch (IOException e) {
			Log.e(TAG, nomarchivo + " Not Found.");
			Toast.makeText(getBaseContext(), "Data not found!",
					Toast.LENGTH_SHORT).show();
		}
	}

	
	
	
	// Funcion para salir de la aplicacion
	public void exitall() {
		System.exit(0);
	}
	
	
	
	

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(TAG, "...onResume - try connect...");

		btnOn.setEnabled(true);
		//STATE_ACQ=0;
		ConnectToDevice();

	}
	
	@Override
	protected void onPause() {
			super.onPause();

			Log.d(TAG, "...In onPause()...");
/*
			try {
				btSocket.close();
			} catch (IOException e) {
			}
*/
		}
		
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "Entered onStart() do nothing");
	}

	protected void onRestart() {

		super.onRestart();
		Log.d(TAG, "Entered onRestart() set everything to starting point");
		
		STATE_ACQ=0;
		btnOn.setText(R.string.btn_Recieve);
		btnOn.setEnabled(true);
		
		radioCalib.setChecked(true);
		radioCompare.setEnabled(false);
		Log.d(TAGtreat, "STATE_ACQ = " + String.valueOf(STATE_ACQ) +" All previous data has been errased.");
		isFirstIteration = 1;
		CurrentNumCount=0;
		
		DataCalib = new ArrayList<ArrayList<Integer>>();
		DataCompare= new ArrayList<ArrayList<Integer>>();
		Toast.makeText(getBaseContext(), "Saved data has been reset.",
				Toast.LENGTH_SHORT).show();
		

	}

	protected void onStop() {
		super.onStop();
		Log.d(TAG, "Entered onStop()");
		
		
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Entered onDestroy()");
		try {
			btSocket.close();
		} catch (IOException e) {
		}
	}
}
