package com.example.smartphone;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioTrackManager {
	//�n�����������
	private int duration = 1;
	//�C��ļ˼ƾ�(����ƫ�)
    private int sampleRate = 8000;
    //�ҥH�A�ƾǤW�`�@�ݭnduration * sampleRate�o��h�I���ļ˼ƾ�(�����ƫ᪺������ƭ�)
    private int numSamples = duration * sampleRate;
    //�x�s������ƭȪ��}�C
    private double sample[] = new double[numSamples];
    //�Q�n�����n�����W�v(���GHZ)
    private double freqOfTone = 400;
    //�ϥ�AudioFormat.ENCODING_PCM_16BIT�A�ҥH�n��2(* numSamples)
    //�x�s�u������PCM�ƾ�
    private byte generatedSnd[] = new byte[2 * numSamples];
    //�Ы�AudioTrack����PCM�ƾ�
    AudioTrack audioTrack;
    public boolean isPlaySound = true;
    public AudioTrackManager()
    {
    	
	}
    void setTone(double freqOfTone)
    {
    	this.freqOfTone = freqOfTone;
    }
    //�إ߳�@����PCM�ƾ�
	void genTone(){
        //�إߥ�����������ƪ��ƾ�
		sample = Waveshape.sin(sample, numSamples, sampleRate, freqOfTone);
        // �ഫ�� 16 bit pcm �n���ƾڰ}�C
        // �ѩ󥿩���ƬO�k�@�ƨ�ơA�_�T�Ӥp�A�ݭn��j
        int idx = 0;
        for (final double dVal : sample) {
            //��j�_�T
            final short val = (short) ((dVal * 32767));
            //�b 16 bit wav PCM���ƾڸ̭�, �Ĥ@��byte�O�C��byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            //���첾�A�h�x�s���줸���ƾ�
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }
    
    //��������
    void playSound(){
    	//AudioTrack.MODE_STREAM�i�H���ݼƾڼg�J�A���L�@�w�n���켽���~�|����
    	//AudioTrack.MODE_STATIC�@�w�n�����ƾڤ~�i�Hplay
    	pause();
    	if(audioTrack==null)
    	{
	        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	                AudioFormat.ENCODING_PCM_16BIT, numSamples,
	                AudioTrack.MODE_STATIC);
    	}
        //audioTrack.write(generatedSnd, 0, generatedSnd.length);
       
    	audioTrack.write(generatedSnd, 0, generatedSnd.length);
    	//�L�����j����A�@�w�ngeneratedSnd.length/4�A-1��L����������C
    	audioTrack.setLoopPoints(0, generatedSnd.length/4, -1);
    	audioTrack.play();
    	/*
        new Thread(new Runnable() 
        {
            
            public void run() 
            {
                // TODO Auto-generated method stub
                while(isPlaySound)
                {

                }
            }
        }).start();
        */
    }
	/**
	 * �����
	 */
	public void stop()
	{
		if(audioTrack!=null)
		{
			audioTrack.pause();
			audioTrack.release();
			audioTrack=null;
		}
	}
	public void pause(){
		if(audioTrack!=null)
		{
			audioTrack.pause();
		}
	}

}
