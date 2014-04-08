package com.tesis.datacollector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.SynchronizedClock;
import com.tesis.commonclasses.data.CallMadeData;
import com.tesis.commonclasses.data.FailedLatencyCheckData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.data.InternetCheckData;
import com.tesis.commonclasses.data.SMSData;
import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.commonclasses.listeners.SignalChangedListener;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.datacollector.listeners.CallIsInProgressListener;
import com.tesis.datacollector.listeners.GPSSignalLostListener;
import com.tesis.datacollector.listeners.LocationChangedListener;
import com.tesis.datacollector.listeners.SMSSentListener;

public class DataCollectorService extends Service implements SignalChangedListener, LocationChangedListener, CallIsInProgressListener, SMSSentListener, GPSSignalLostListener, SharedPreferences.OnSharedPreferenceChangeListener {

   ///Listeners
    private LocationMonitor locationRetriever;
    private PhoneSignalMonitor signalMonitor;
    private BatteryLevelInspector batteryInspector;
    private OutgoingCallsMonitor callsMonitor;
    private TrafficStats trafficMonitor;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
    private PowerManager powerManager;

    //Datos a enviar
    private Float currentSignal;
    private Float oldSignal;
    private Location lastKnownLocation;
    private String lastDestination;
    private String operatorName;

    //Datos de configuracion
    private String mPhoneNumber;
    private String destinationNumber;
    
    private Runnable makeACallStrategy;
    private Runnable testLatencyStrategy;

    //Lista de paquetes
    private DataList dataList;
    private SharedPreferences preferences;

    //TS de ultima llamada
    private Date lastCallDate;
    private Location lastCallLocation;

    //Timer para futura llamada si pasan mas de 30 min
    private ScheduledFuture<?> futureCall;
    
    private Date timeOfLastKnownLocation;
    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        dataList = DataList.load(this);
        makeACallStrategy = getEmptyTask();
        testLatencyStrategy = getEmptyTask();        
        //Obtener el proveedor de telefonia
        operatorName = telephonyManager.getNetworkOperatorName();

        setPhoneNumbers(telephonyManager);

        //Inicializar Variables
        signalMonitor = new PhoneSignalMonitor(telephonyManager);
        locationRetriever = new LocationMonitor(this);
        batteryInspector = new BatteryLevelInspector(this);
        callsMonitor = new OutgoingCallsMonitor(this);
        trafficMonitor = new TrafficStats();
        currentSignal = 0f;
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        
        /// Registra el observer para los SMS
        SMSObserver smsObserver = new SMSObserver(new Handler(), this);
        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"),true, smsObserver);
        //end Registrar SMS OBSERVER

        boolean synced = SynchronizedClock.synchronize();
        if (!synced) {
        	stopSelf();
        }
        
        //ADD TODOS LOS REGISTRERS
        addAndRegisterMonitor(smsObserver);
        addAndRegisterMonitor(locationRetriever);
        addAndRegisterMonitor(signalMonitor);
        addAndRegisterMonitor(callsMonitor);
    
		boolean forceMobileConnectionForAddress = MobileDataUseForcer.forceMobileConnectionForAddress(DataCollectorService.this, Constants.LatencyTestAddress);
		
		executor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						SynchronizedClock.synchronize();
					} catch (Throwable e) {
						Log.e(Constants.LogTag, "Failed to synchronize");
					}
				}
			}, 0, 60, TimeUnit.SECONDS);

        executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					dataList.sendDataListAndClearIfSuccessful();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}, 0, 600, TimeUnit.SECONDS);
        
        executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				testLatencyStrategy.run();
			}
		}, 0, 120, TimeUnit.SECONDS);
        
        executor.scheduleWithFixedDelay(getLocationTimeoutTask(), 0, 60, TimeUnit.SECONDS);
        
        Intent resultIntent = new Intent(this, MainActivity.class);
        Notification notification = new NotificationCompat.Builder(this)
        	.setSmallIcon(R.drawable.ic_stat_dc)
        	.setContentTitle("DataCollector")
        	.setContentText("Tesis Data Collector is working")
        	.setContentIntent(PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        	.build();
        
        startForeground(1, notification);
        
        return START_STICKY;
    }

	private Runnable getTestLatencyTask() {
		return new Runnable() {
			
			@Override
			public void run() {
				try {
					testLatency();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
	}

	private void setPhoneNumbers(TelephonyManager telephonyManager) {
        mPhoneNumber = telephonyManager.getLine1Number();
        if (mPhoneNumber.equals("")) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            mPhoneNumber = preferences.getString(SettingsActivity.PHONE_NUMBER, "");
        }
        destinationNumber = preferences.getString(SettingsActivity.DESTINATION_NUMBER, "");
	}
    
    @Override
	public void onDestroy() {
    	executor.shutdownNow();
    	locationRetriever.stopListening();
    	signalMonitor.stopListening();
    	callsMonitor.stopListening();
    	ExecutorService localExecutor = Executors.newSingleThreadExecutor();
    	Future<?> saveListFuture = localExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					dataList.sendDataListAndClearIfSuccessful();
				} catch (URISyntaxException e) {
					Log.e(Constants.LogTag, "Failed to send data to server uppon service closing");
				}
		    	dataList.save();
			}
		});
    	try {
			saveListFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	super.onDestroy();
	}

	private void addAndRegisterMonitor(EventsProducer producer) {
        producer.addListener(this);
        producer.startListening();
    }

    public Runnable getPositionAqcuiredMakeACallTask() {
        return new Runnable() {
            @Override
            public void run(){
                try {
                    tryToCall();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
      };
    }
    
    public Runnable getEmptyTask() {
    	return new Runnable() {
			
			@Override
			public void run() {}
		};
    }
    
    public Runnable getHandleLatencyErrorsTask() {
    	return new Runnable() {
			
			@Override
			public void run() {
				FailedLatencyCheckData failedLatencyCheckData = new FailedLatencyCheckData(currentSignal, batteryInspector.getBatteryLevelAsPercentage(), lastKnownLocation, operatorName, mPhoneNumber, 0l);
				JSONObject failedLatencyCheck = failedLatencyCheckData.getAsJson();
				dataList.addToPack(failedLatencyCheck);
			}
		};
    }
 /*   public Runnable getExecutionSendingTask() {
        return new Runnable() {
            public boolean shouldMakeACall;
            public boolean shouldSendData;
            public Date newDate;
            public float oldSignalValue;
            public Location oldLocation;
            public Date oldDate;

            @Override
            public void run() {
                shouldMakeACall = false;
                shouldSendData = false;
                newDate = new Date();
                System.out.println("*********Ejecucion del timer");
                if(lastKnownLocation == null)
                    System.out.println("*********No hay datos de ubicacion");
                else if(oldSignalValue == -1f || oldLocation == null){
                    oldSignalValue = signalAvg.getSignal();
                    oldLocation = new Location(lastKnownLocation);
                    signalAvg.clear();
                    shouldMakeACall = true;
                    System.out.println("*********Primera vez");
                }else if (Math.abs(oldSignalValue - signalAvg.getSignal()) > 1) {
                    shouldMakeACall = true;
                    oldSignalValue = signalAvg.getSignal();
                    System.out.println("*********Cambio de señal " + signalAvg.getSignal());
                    signalAvg.clear();
                } else if (oldLocation.distanceTo(lastKnownLocation) >= 3f) {
                    shouldMakeACall = true;
                    System.out.println("*********Cambio de Distancia " + oldLocation.distanceTo(lastKnownLocation) + " mts");
                    oldLocation = lastKnownLocation;
                } else if (newDate.getTime() - oldDate.getTime() > 1800000) {
                    shouldMakeACall = true;
                    oldDate = newDate;
                    System.out.println("*********Vencimiento de tiempo");
                }

                if(shouldMakeACall){
                    makeCall(mPhoneNumber);
                    CallData callData = new CallData(oldSignalValue,batteryLevel, lastKnownLocation,(new Date()).getTime(),lastDestination, operatorName, mPhoneNumber);
                    JSONObject callJson = callData.getAsJson();
                    System.out.println("********* OBJETO JSON AGREGADO A LA LISTA: " + callJson.toString());
                    dataList.addToPack(callJson);
                    shouldSendData = true;
                }

                if(shouldSendData) {
                    try {
                        dataList.sendDataListAndClearIfSuccessful();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
*/
    public void makeCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }


    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void handleCallIsInProgress(Date startDate, String destination) {
        Log.d(Constants.LogTag, String.format("Call is in progress: %s, %s", startDate, destination));
        lastDestination = destination;

        lastCallDate = startDate; //registrar TS de ultima llamada
        lastCallLocation = lastKnownLocation; //registrar locacion de ultima llamada
        CallMadeData callData = null;
        try {
            callData = new CallMadeData(currentSignal,batteryInspector.getBatteryLevelAsPercentage(), lastKnownLocation, SynchronizedClock.getCurrentTime(),lastDestination, operatorName, mPhoneNumber);
            JSONObject callJson = callData.getAsJson();
            System.out.println("********* OBJETO JSON AGREGADO A LA LISTA: " + callJson.toString());
            dataList.addToPack(callJson);
        } catch (Exception e) {
			e.printStackTrace();
		}
        
        if(futureCall != null) //Si sigue activo el timer cancelarlo
            futureCall.cancel(true);
        //Programar proxima llamada
        futureCall = executor.schedule(makeACallStrategy, 60, TimeUnit.SECONDS);
        System.out.println("*********Resetear Timeout");

        DeleteCallLogByNumber(destinationNumber);
    }

    private void turnOffScreen() {
    	try {
			devicePolicyManager.lockNow();
    	} catch (Exception e) {
			Log.e(Constants.LogTag, "Could not turn off screen\n" + e);
    	}
	}

	@Override
    public void handleLocationChanged(Location location) {
        Log.d(Constants.LogTag, String.format("Location has changed: %s", location));
        if (location.getAccuracy() < 100) {
        	makeACallStrategy = getPositionAqcuiredMakeACallTask();
        	testLatencyStrategy = getTestLatencyTask();
        	Location previousLocation = lastKnownLocation;
	        this.lastKnownLocation = location;
	        timeOfLastKnownLocation = new Date();
	        
	        //Si la nueva loacacion difiere de la ultima en la que se llamo X metros, se deben generar datos
	        if (previousLocation == null || lastCallLocation == null || lastCallLocation != null && lastCallLocation.distanceTo(lastKnownLocation) >= 15f) {
                Log.d(Constants.LogTag, "Cambio Locacion");
                makeACallStrategy.run();
	        }
        } else {
        	makeACallStrategy = getEmptyTask();
        	testLatencyStrategy = getEmptyTask();
        }
    }

    @Override
    public void handleGpsOn() {
        Log.d(Constants.LogTag, "GPS is on");
        lastKnownLocation = null;
    }

    @Override
    public void handleSMSSent(long timeInMs, Date initDate, Date finishDate, String destinationNumber) {
        SMSData messageData = null;
        try {
            messageData = new SMSData(currentSignal,batteryInspector.getBatteryLevelAsPercentage(), lastKnownLocation,timeInMs, operatorName, SynchronizedClock.getCurrentTime(), mPhoneNumber, destinationNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject messageJson = messageData.getAsJson();
        System.out.println("********* Interrupcion por SMS");
        System.out.println("********* OBJETO JSON ENVIADO" + messageJson.toString()+"*********");
        dataList.addToPack(messageJson);
        Log.d(Constants.LogTag, String.format("An SMS was sent on the "));
    }

    @Override
    public void handleSignalChanged(SignalChangedArgs args) {
        if(isScreenOn(powerManager))
            Log.d(Constants.LogTag, "Pantalla encendida");
        else
            Log.d(Constants.LogTag, "Pantalla apagada");

        oldSignal = currentSignal;
        int strength = args.getNewSignalStrength().getGsmSignalStrength();
        currentSignal = (float) strength;
        Log.d(Constants.LogTag, String.format("The signal has changed from %s to %s", oldSignal, currentSignal));
        //Si la senial cambio de golpe en X porcentaje, se deben generar datos
        if(Math.abs(currentSignal-Math.abs(oldSignal))>3){
            System.out.println("*********Cambio de senial");
            makeACallStrategy.run();
        }
    }

    @Override
	public void handleGPSSignalLost() {
		//TODO: implement this handler
	}

    
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.PHONE_NUMBER)) {
            mPhoneNumber = sharedPreferences.getString(SettingsActivity.PHONE_NUMBER, "");
        }
    }


    public void testLatency() throws InterruptedException, ExecutionException {
        AsyncTask<Void, Void, Long> latencyCheckTask = new LatencyChecker(getHandleLatencyErrorsTask()).execute();
        Long timeInMs = latencyCheckTask.get();
        dataList.addToPack(new InternetCheckData(currentSignal, batteryInspector.getBatteryLevelAsPercentage(), lastKnownLocation, operatorName, mPhoneNumber, timeInMs).getAsJson());
    }

    private void tryToCall() throws IOException {
        //Si nunca se llamo o paso un tiempo razonable desde ultima llamada se debe llamar
        // agregar && isScreenOn()
        if(!powerManager.isScreenOn() && callsMonitor.getActualState() != TelephonyManager.CALL_STATE_OFFHOOK && (lastCallDate == null || (new Date()).getTime() - lastCallDate.getTime() > 30000 )){
            if(lastCallDate == null) {
				Log.d(Constants.LogTag, "Llamando por primera vez");
			}
            makeCall(destinationNumber); ///llamar al numero
        }
        else {
        	Log.d(Constants.LogTag, "Aun no se debe llamar");        	
        }
    }

    public void DeleteCallLogByNumber(String number) {
        String queryString="NUMBER="+number;
        this.getContentResolver().delete(CallLog.Calls.CONTENT_URI,queryString,null);
    }

    public boolean isScreenOn(PowerManager powerManager){
       return powerManager.isScreenOn();
    }
    
    public Runnable getLocationTimeoutTask() {
    	return new Runnable() {
			@Override
			public void run() {
				if (timeOfLastKnownLocation != null && (new Date().getTime() - timeOfLastKnownLocation.getTime() > 5000 * 60)) { //Si pasaron mas de 5 minutos desde el ultimo locaitonUpdate
					testLatencyStrategy = getEmptyTask();
					makeACallStrategy = getEmptyTask();
				}
			}
		};
    }
}
