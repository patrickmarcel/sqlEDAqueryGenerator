package fr.univtours.info;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.jcurand.curandGenerator;

import java.time.LocalTime;
import java.util.Random;

import static jcuda.jcurand.JCurand.*;
import static jcuda.jcurand.curandRngType.CURAND_RNG_PSEUDO_DEFAULT;
import static jcuda.runtime.JCuda.*;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToHost;

public class CudaRand extends Random {
    public static int BUFFER_SIZE = 4000000;
    public float[] buffer;
    public int pos;
    static Pointer deviceData;
    static curandGenerator generator;

    static {
        // Allocate device memory
        deviceData = new Pointer();
        cudaMalloc(deviceData, BUFFER_SIZE * Sizeof.FLOAT);

        // Create and initialize a pseudo-random number generator
        generator = new curandGenerator();
        curandCreateGenerator(generator, CURAND_RNG_PSEUDO_DEFAULT);
        curandSetPseudoRandomGeneratorSeed(generator, LocalTime.now().toNanoOfDay());
    }

    public CudaRand() {
        //Allocate RAM
        buffer = new float[BUFFER_SIZE];

        pos = 0;
        genAndCopy(this.buffer);
    }

    private static synchronized void genAndCopy(float[] buffer){
        // Generate random numbers
        curandGenerateUniform(generator, deviceData, BUFFER_SIZE);

        // Copy the random numbers from the gpu to ram
        cudaMemcpy(Pointer.to(buffer), deviceData,((long) BUFFER_SIZE) * Sizeof.FLOAT, cudaMemcpyDeviceToHost);
    }

    @Override
    public int nextInt(int ub){
        if (pos == BUFFER_SIZE - 1){
            pos = 0;
            genAndCopy(this.buffer);
        }
        return (int) (buffer[pos++] * (ub-1));
    }
}
