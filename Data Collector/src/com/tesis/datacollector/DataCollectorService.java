package com.tesis.datacollector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.antonc.phone_schedule.Dummy.DummyBrightnessActivity;
import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.SynchronizedClock;
import com.tesis.commonclasses.data.CallMadeData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.data.FailedLatencyCheckData;
import com.tesis.commonclasses.data.InternetCheckData;
import com.tesis.commonclasses.data.SMSData;
import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.commonclasses.listeners.SignalChangedListener;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.datacollector.listeners.CallEndedListener;
import com.tesis.datacollector.listeners.CallIsInProgressListener;
import com.tesis.datacollector.listeners.GPSSignalLostListener;
import com.tesis.datacollector.listeners.LocationChangedListener;
import com.tesis.datacollector.listeners.SMSSentListener;

public class DataCollectorService extends Service implements
		SignalChangedListener, LocationChangedListener,
		CallIsInProgressListener, CallEndedListener, SMSSentListener, 
		GPSSignalLostListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	public volatile static DataCollectorService instance;
	// /Listeners
	private volatile LocationMonitor locationRetriever;
	private volatile PhoneSignalMonitor signalMonitor;
	private volatile BatteryLevelInspector batteryInspector;
	private volatile OutgoingCallsMonitor callsMonitor;
	private volatile TrafficStats trafficMonitor;
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
	private volatile PowerManager powerManager;

	// Datos a enviar
	private volatile Float currentGsmSignal;
	private volatile Float oldGsmSignal;
	private volatile Float current3GSignal;
	private volatile Float old3GSignal;
	private volatile Location lastKnownLocation;
	private volatile String lastDestination;
	private String operatorName;

	// Datos de configuracion
	private String mPhoneNumber;
	private String destinationNumber;

	private volatile Runnable makeACallStrategy;
	private volatile Runnable testLatencyStrategy;
	private volatile Runnable sendSmsStrategy;

	// Lista de paquetes
	private volatile DataList dataList;
	private SharedPreferences preferences;

	// TS de ultima llamada
	private volatile Date lastCallDate;
	private volatile Location lastCallLocation;

	// Timer para futura llamada si pasan mas de 30 min
	private ScheduledFuture<?> futureCall;

	private volatile Date timeOfLastKnownLocation;
	private volatile int normalScreenBrightness;
	private volatile CallMadeData callData;

	private Messenger activityMessenger;
	private volatile boolean monitorsWereInitialized = false;
	
	private volatile boolean cancelInitialization = false;
	
	private ServiceMessenger serviceMessenger = new ServiceMessenger();
	private volatile ServiceState state = new ServiceState();
	
	private SmsSender smsSender;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final Context ctx = this;
		instance = this;
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		smsSender = new SmsSender(this);
		smsSender.addListener(this);
		state.setInitializing(true);
		if (intent != null && intent.getExtras().containsKey("MESSENGER")) {
			activityMessenger = (Messenger) intent.getExtras().get("MESSENGER");
		}

        final TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // Inicializar Variables
        signalMonitor = new PhoneSignalMonitor(telephonyManager);
        locationRetriever = new LocationMonitor(ctx);
        batteryInspector = new BatteryLevelInspector(ctx);
        callsMonitor = new OutgoingCallsMonitor(ctx);
        trafficMonitor = new TrafficStats();
        currentGsmSignal = 0f;
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        monitorsWereInitialized = true;
        // / Registra el observer para los SMS
        final SMSObserver smsObserver = new SMSObserver(new Handler(), ctx);
        ContentResolver contentResolver = ctx.getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"),
                true, smsObserver);
        // end Registrar SMS OBSERVER

		executor.execute(handleInitializationErrors(new Runnable() {
			@Override
			public void run() {
				//Handler is used to run code on the main UI thread.
				final Handler handler = new Handler(Looper.getMainLooper());
				dataList = DataList.load(ctx);
				makeACallStrategy = getEmptyTask();
				testLatencyStrategy = getEmptyTask();
				sendSmsStrategy = getEmptyTask();
				// Obtener el proveedor de telefonia
				operatorName = telephonyManager.getNetworkOperatorName();

				setPhoneNumbers(telephonyManager);
                boolean synced = SynchronizedClock.synchronize();
                if (!synced || cancelInitialization) {
                    sendServiceEndedMessage();
                    stopSelf();
                    return;
                }
                Log.d(Constants.LogTag, "Clock successfully synchronized");
				handler.post(handleInitializationErrors(new Runnable() {
					@Override
					public void run() {
                        // ADD TODOS LOS REGISTRERS
                        addAndRegisterMonitor(smsObserver);
                        addAndRegisterMonitor(locationRetriever);
                        addAndRegisterMonitor(signalMonitor);
                        addAndRegisterMonitor(callsMonitor);
                        callsMonitor.addListener((CallEndedListener) ctx);
                        if (cancelInitialization) {
                            stopSelf();
                            return;
                        }

                        executor.execute(handleInitializationErrors(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(Constants.LogTag, "Setting network tests configuration");
                                boolean forceMobileConnectionForAddress = MobileDataUseForcer
                                        .forceMobileConnectionForAddress(ctx,
                                                Constants.LatencyTestAddress);
                                if (cancelInitialization) {
                                    return;
                                }

                                Log.d(Constants.LogTag, "Scheduling tasks");
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

                                if (forceMobileConnectionForAddress) {
                                    executor.scheduleWithFixedDelay(new Runnable() {
                                        @Override
                                        public void run() {
                                            testLatencyStrategy.run();
                                        }
                                    }, 0, 120, TimeUnit.SECONDS);
                                }

                                boolean shouldSendChronicSms = preferences.getBoolean(SettingsActivity.SEND_SMS, false);
                                if (shouldSendChronicSms) {
                                    executor.scheduleWithFixedDelay(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendSmsStrategy.run();
                                        }
                                    }, 0, 300, TimeUnit.SECONDS); //5 minutes
                                }

                                executor.scheduleWithFixedDelay(getLocationTimeoutTask(), 0, 60,
                                        TimeUnit.SECONDS);

                                handler.post(handleInitializationErrors(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent resultIntent = new Intent(DataCollectorService.this, MainActivity.class);
                                        Notification notification = new NotificationCompat.Builder(DataCollectorService.this)
                                                .setSmallIcon(R.drawable.ic_stat_dc)
                                                .setContentTitle("DataCollector")
                                                .setContentText("Tesis Data Collector is working")
                                                .setContentIntent(
                                                        PendingIntent.getActivity(DataCollectorService.this, 1, resultIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT)).build();

                                        startForeground(1, notification);
                                        Message activityLaunchedMessage = Message.obtain();
                                        activityLaunchedMessage.what = Constants.ServiceLaunchedSuccesfully;
                                        Messenger messenger = new Messenger(serviceMessenger);
                                        activityLaunchedMessage.replyTo = messenger;
                                        state.setServiceIsWorking(true);
                                        state.setInitializing(false);
                                        try {
                                            activityMessenger.send(activityLaunchedMessage);
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }));
                            }
                        }));
                    }
				}));
			}
		}));
		
		return START_STICKY;
	}

	private Runnable handleInitializationErrors(final Runnable code) {
		return new Runnable() {
			
			@Override
			public void run() {
				try {
					code.run();
				} catch (Exception e) {
					state.setServiceIsWorking(false);
					state.setInitializing(false);
					state.setServiceIsWorking(false);
					
					stopSelf();
				}
			}
		};
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
			mPhoneNumber = preferences.getString(SettingsActivity.PHONE_NUMBER,
					"");
		}
		destinationNumber = preferences.getString(SettingsActivity.DESTINATION_NUMBER, "");
	}
	

	@Override
	public void onDestroy() {
		cancelInitialization = true;
		executor.shutdownNow();
		if (monitorsWereInitialized) {
			locationRetriever.stopListening();
			signalMonitor.stopListening();
			callsMonitor.stopListening();
		}
		
		ExecutorService localExecutor = Executors.newCachedThreadPool();
		Future<?> saveListFuture = localExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					if (dataList != null) { //dataList might be null if we cancel the service before the initialization ended.
						dataList.sendDataListAndClearIfSuccessful();
						dataList.save();
					}
				} catch (URISyntaxException e) {
					Log.e(Constants.LogTag,
							"Failed to send data to server uppon service closing");
				}
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						sendServiceEndedMessage();
						DataCollectorService.instance = null;
						DataCollectorService.super.onDestroy();					
					}
				});
			}
		});
	}

	private void addAndRegisterMonitor(EventsProducer producer) {
		producer.addListener(this);
		producer.startListening();
	}

	public Runnable getPositionAqcuiredMakeACallTask() {
		return new Runnable() {
			@Override
			public void run() {
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
			public void run() {
			}
		};
	}

	public Runnable getHandleLatencyErrorsTask() {
		return new Runnable() {

			@Override
			public void run() {
				FailedLatencyCheckData failedLatencyCheckData = new FailedLatencyCheckData(
						currentGsmSignal,
						batteryInspector.getBatteryLevelAsPercentage(),
						lastKnownLocation, operatorName, mPhoneNumber, 0l);
				JSONObject failedLatencyCheck = failedLatencyCheckData
						.getAsJson();
				dataList.addToPack(failedLatencyCheck);
			}
		};
	}

	/*
	 * public Runnable getExecutionSendingTask() { return new Runnable() {
	 * public boolean shouldMakeACall; public boolean shouldSendData; public
	 * Date newDate; public float oldSignalValue; public Location oldLocation;
	 * public Date oldDate;
	 * 
	 * @Override public void run() { shouldMakeACall = false; shouldSendData =
	 * false; newDate = new Date();
	 * System.out.println("*********Ejecucion del timer"); if(lastKnownLocation
	 * == null) System.out.println("*********No hay datos de ubicacion"); else
	 * if(oldSignalValue == -1f || oldLocation == null){ oldSignalValue =
	 * signalAvg.getSignal(); oldLocation = new Location(lastKnownLocation);
	 * signalAvg.clear(); shouldMakeACall = true;
	 * System.out.println("*********Primera vez"); }else if
	 * (Math.abs(oldSignalValue - signalAvg.getSignal()) > 1) { shouldMakeACall
	 * = true; oldSignalValue = signalAvg.getSignal();
	 * System.out.println("*********Cambio de señal " + signalAvg.getSignal());
	 * signalAvg.clear(); } else if (oldLocation.distanceTo(lastKnownLocation)
	 * >= 3f) { shouldMakeACall = true;
	 * System.out.println("*********Cambio de Distancia " +
	 * oldLocation.distanceTo(lastKnownLocation) + " mts"); oldLocation =
	 * lastKnownLocation; } else if (newDate.getTime() - oldDate.getTime() >
	 * 1800000) { shouldMakeACall = true; oldDate = newDate;
	 * System.out.println("*********Vencimiento de tiempo"); }
	 * 
	 * if(shouldMakeACall){ makeCall(mPhoneNumber); CallData callData = new
	 * CallData(oldSignalValue,batteryLevel, lastKnownLocation,(new
	 * Date()).getTime(),lastDestination, operatorName, mPhoneNumber);
	 * JSONObject callJson = callData.getAsJson();
	 * System.out.println("********* OBJETO JSON AGREGADO A LA LISTA: " +
	 * callJson.toString()); dataList.addToPack(callJson); shouldSendData =
	 * true; }
	 * 
	 * if(shouldSendData) { try { dataList.sendDataListAndClearIfSuccessful(); }
	 * catch (URISyntaxException e) { e.printStackTrace(); } } } }; }
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
		if (!destination.equals(destinationNumber)) {
			return;
		}
		
		Log.d(Constants.LogTag, String.format("Call is in progress: %s, %s",
				startDate, destination));
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, 20);
		lastDestination = destination;

		lastCallDate = startDate; // registrar TS de ultima llamada
		lastCallLocation = lastKnownLocation; // registrar locacion de ultima
		callData = null;
		try {
			callData = new CallMadeData(currentGsmSignal,
					batteryInspector.getBatteryLevelAsPercentage(),
					lastKnownLocation, SynchronizedClock.getCurrentTime(),
					lastDestination, operatorName, mPhoneNumber);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (futureCall != null) // Si sigue activo el timer cancelarlo
			futureCall.cancel(true);
		// Programar proxima llamada
		futureCall = executor.schedule(makeACallStrategy, 60, TimeUnit.SECONDS);

		DeleteCallLogByNumber(destinationNumber);
		Toast.makeText(this, "About to turn off screen", Toast.LENGTH_SHORT);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			normalScreenBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		turnOffScreen();
	}
	

	@Override
	public void handleCallEnded(Date date, String destinationNumber) {
		if (destinationNumber == null || !destinationNumber.equals(this.destinationNumber)) {
			return;
		}
		
		if (callData != null) {
			callData.setTimeOfFinalization(SynchronizedClock.getCurrentTime());
			JSONObject callJson = callData.getAsJson();
			dataList.addToPack(callJson);
			Settings.System.putInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, normalScreenBrightness);			
			
			callData = null;
		}
	}

	private void turnOffScreen() {
		float brightness = 0;
		// This is important. In the next line 'brightness' 
		// should be a float number between 0.0 and 1.0
		int brightnessInt = (int)(brightness*255);


		// Set systemwide brightness setting. 
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessInt);

		// Apply brightness by creating a dummy activity
		Intent intent = new Intent(getBaseContext(), DummyBrightnessActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("brightness value", brightness); 
		getApplication().startActivity(intent);
	}

	@Override
	public void handleLocationChanged(Location location) {
		Log.d(Constants.LogTag,
				String.format("Location has changed. Accuracy: %f", location.getAccuracy()));
		if (location.getAccuracy() < 100) {
			makeACallStrategy = getPositionAqcuiredMakeACallTask();
			sendSmsStrategy = getSendSmsTask();
			testLatencyStrategy = getTestLatencyTask();
			Location previousLocation = lastKnownLocation;
			this.lastKnownLocation = location;
			timeOfLastKnownLocation = new Date();

			// Si la nueva loacacion difiere de la ultima en la que se llamo X
			// metros, se deben generar datos
			if (previousLocation == null || lastCallLocation == null
					|| lastCallLocation != null
					&& lastCallLocation.distanceTo(lastKnownLocation) >= 15f) {
				Log.d(Constants.LogTag, "Cambio ubicacion");
				makeACallStrategy.run();
			}
		} else {
			makeACallStrategy = getEmptyTask();
			sendSmsStrategy = getEmptyTask();
			testLatencyStrategy = getEmptyTask();
		}
	}

	@Override
	public void handleGpsOn() {
		Log.d(Constants.LogTag, "GPS is on");
		lastKnownLocation = null;
	}

	@Override
	public void handleSMSSent(long timeInMs, Date initDate, Date finishDate,
			String destinationNumber) {
		SMSData messageData = null;
		try {
			messageData = new SMSData(currentGsmSignal,
					batteryInspector.getBatteryLevelAsPercentage(),
					lastKnownLocation, timeInMs, operatorName,
					SynchronizedClock.getCurrentTime(), mPhoneNumber,
					destinationNumber);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject messageJson = messageData.getAsJson();
		System.out.println("********* Interrupcion por SMS");
		System.out.println("********* OBJETO JSON ENVIADO"
				+ messageJson.toString() + "*********");
		dataList.addToPack(messageJson);
		Log.d(Constants.LogTag, String.format("An SMS was sent on the "));
	}

	@Override
	public void handleSignalChanged(SignalChangedArgs args) {
		if (isScreenOn(powerManager))
			Log.d(Constants.LogTag, "Pantalla encendida");
		else
			Log.d(Constants.LogTag, "Pantalla apagada");
		oldGsmSignal = currentGsmSignal;
		old3GSignal = current3GSignal;
		int gsmStrength = args.getNewSignalStrength().getGsmSignalStrength();
		current3GSignal = (float) args.getNewSignalStrength().getCdmaDbm();
		currentGsmSignal = (float) gsmStrength;
		Log.d(Constants.LogTag, String.format(
				"The signal has changed from %s to %s", oldGsmSignal,
				currentGsmSignal));
		// Si la senial cambio de golpe en X porcentaje, se deben generar datos
		if (Math.abs(currentGsmSignal - Math.abs(oldGsmSignal)) > 3) {
			System.out.println("*********Cambio de senial");
			makeACallStrategy.run();
		}
	}

	@Override
	public void handleGPSSignalLost() {
		// TODO: implement this handler
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(SettingsActivity.PHONE_NUMBER)) {
			mPhoneNumber = sharedPreferences.getString(SettingsActivity.PHONE_NUMBER, "");
		}
	}

	public void testLatency() throws InterruptedException, ExecutionException {
		AsyncTask<Void, Void, Long> latencyCheckTask = new LatencyChecker(
				getHandleLatencyErrorsTask()).execute();
		Long timeInMs = latencyCheckTask.get();
		dataList.addToPack(new InternetCheckData(currentGsmSignal,
				batteryInspector.getBatteryLevelAsPercentage(),
				lastKnownLocation, operatorName, mPhoneNumber, timeInMs)
				.getAsJson());
	}

	private void tryToCall() throws IOException {
		// Si nunca se llamo o paso un tiempo razonable desde ultima llamada se
		// debe llamar
		// agregar && isScreenOn()
		if (!powerManager.isScreenOn()
				&& callsMonitor.getActualState() != TelephonyManager.CALL_STATE_OFFHOOK
				&& (lastCallDate == null || (new Date()).getTime()
						- lastCallDate.getTime() > 30000)) {
			if (lastCallDate == null) {
				Log.d(Constants.LogTag, "Llamando por primera vez");
			}
			makeCall(destinationNumber); // /llamar al numero
		} else {
			Log.d(Constants.LogTag, "Aun no se debe llamar");
		}
	}

	public void DeleteCallLogByNumber(String number) {
		String queryString = "NUMBER=" + number;
		this.getContentResolver().delete(CallLog.Calls.CONTENT_URI,
				queryString, null);
	}

	public boolean isScreenOn(PowerManager powerManager) {
		return powerManager.isScreenOn();
	}
	
	public ServiceState getState() {
		return state;
	}
	
	private Runnable getSendSmsTask() {
		return new Runnable() {
			@Override
			public void run() {
				smsSender.sendSms(destinationNumber, "");
			}
		};
	}
	
	public Runnable getLocationTimeoutTask() {
		return new Runnable() {
			@Override
			public void run() {
				if (timeOfLastKnownLocation != null
						&& (new Date().getTime()
								- timeOfLastKnownLocation.getTime() > 5000 * 60)) { // Si pasaron mas de 5 minutos desde el ultimo location update.
					testLatencyStrategy = getEmptyTask();
					makeACallStrategy = getEmptyTask();
					sendSmsStrategy = getEmptyTask();
				}
			}
		};
	}
	
	private void sendServiceEndedMessage() {
		Message serviceFailMessage = Message.obtain();
		serviceFailMessage.what = Constants.ServiceLaunchFailed;
		try {
			activityMessenger.send(serviceFailMessage);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private class ServiceMessenger extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == Constants.StartGPS) {
				locationRetriever.startGPS();
				state.setGpsIsOn(true);
			} else if (msg.what == Constants.StopGPS) {
				locationRetriever.stopGPS();
				state.setGpsIsOn(false);
			}
			super.handleMessage(msg);
		}
	}
}
