package com.example.termometr2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public ImageView iv;
    public Bitmap bitmap;
    public Canvas canvas;
    public Paint paint;
    public Paint text;
    public FFT fft;

    int samplingfrequency = 4000;
    int blockSize = 1024;

    double[] x;
    double[] y;
    double[] ampl;


    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    boolean loop = false;

    private final static float BASE = 370;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x = new double[blockSize];
        y = new double[blockSize];

        ampl = new double[blockSize / 2];

        iv = (ImageView) this.findViewById(R.id.iv0);
        bitmap = Bitmap.createBitmap((int) blockSize / 2, (int) 410, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        paint = new Paint();
        paint.setColor(Color.RED);

        text = new Paint();
        text.setColor(Color.WHITE);
        text.setStyle(Paint.Style.FILL);
        canvas.drawPaint(text);

        text.setColor(Color.RED);
        text.setTextSize(12);
        text.setTextScaleX((float) 1.0);

        iv.setImageBitmap(bitmap);

        fft = new FFT(blockSize);

        Button btn1 = (Button) findViewById(R.id.start);
        btn1.setOnClickListener(view -> {
            loop = true;
        });

        Button btn2 = (Button) findViewById(R.id.stop);
        btn2.setOnClickListener(view -> {
            loop = false;
        });

        this.startThread();
    }

    private void startThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    while (loop) {
                        drawView();
                        try {
                            Thread.sleep(100);
                        } catch (Throwable e) {
                        }
                    }
                }
            }
        });
        t.start();
    }

    public void drawView() {

        readAudio();

        int pick = 0;
        double max = 0.0;

        canvas.drawColor(Color.WHITE);
        paint.setColor(Color.RED);
        iv.setImageBitmap(bitmap);


        fft.calculate(x, y);

        for(int i = 0; i < blockSize / 2; i++) {

            ampl[i] = x[i] * x[i] + y[i] * y[i];
            if (i > 0) {
                if(ampl[i] > max) {
                    max = ampl[i];
                    pick = i;
                }
            }
        }

        for (int i = 0; i < blockSize / 2; i++) {

            canvas.drawLine(
                    (float) i,
                    (float) ((double) BASE - (double) 10.0),
                    (float) i,
                    (float) ((double) BASE - (double) 10.0  - (double)  ampl[i]/20), paint);
        }


        int freq = (pick * samplingfrequency) / blockSize;

        for (int i = 0; i < blockSize / 2; i++) {
            int freq2 = (i * samplingfrequency) / blockSize;
            if(freq2%500==0) {
                canvas.drawLine(
                        (float) i,
                        (float) ((double) BASE),
                        (float) i,
                        (float) ((double) BASE - (double) 20.0), paint);
                if(freq2<1000)
                canvas.drawText(""+freq2, (float)i-10, (float) ((double) BASE + (double) 30.0) , text);
                else
                    canvas.drawText(""+freq2, (float)i-14, (float) ((double) BASE + (double) 30.0) , text);
            }
            else
            {
                canvas.drawPoint(
                        (float) i,
                        (float) ((double) BASE - (double) 10.0),paint );
            }
        }

        double a = 0.104;
        double b = -25.576;

        double temp = Math.floor((a * freq + b)*100)/100;

        final TextView czest = (TextView) findViewById(R.id.czest);
        czest.setText("częstotliwość: " + freq +" hz");

        final TextView tem = (TextView) findViewById(R.id.temp);
        tem.setText("temperatura: " + temp+" °C");


        iv.invalidate();
    }

    protected void readAudio() {

        short[] audioBuffer = new short[blockSize];
        int bufferSize = AudioRecord.getMinBufferSize(samplingfrequency, channelConfiguration, audioEncoding);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                samplingfrequency,
                channelConfiguration,
                audioEncoding,
                bufferSize
        );

        audioRecord.startRecording();

        int bufferReadResult = audioRecord.read(audioBuffer, 0, blockSize);

        for(int i = 0; i < blockSize && i < bufferReadResult; i++) {
            x[i] = (double) audioBuffer[i] / 32768.0;
        }

        for(int i = 0; i < blockSize && i < bufferReadResult; i++) {
            y[i] = 0;
        }

        audioRecord.stop();
    }
}