package com.example.smartphone;

public class Waveshape {
	
	public static double[] sin(double sample[], int numSamples, int sampleRate, double freqOfTone) 
	{//�إߥ�����������ƪ��ƾ�    	
        for (int i = 0; i < numSamples; ++i) 
        {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }
		return sample;
	}

}
