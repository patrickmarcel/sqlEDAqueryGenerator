package com.alexscode.utilities;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.jcurand.curandGenerator;

import static jcuda.jcurand.JCurand.*;
import static jcuda.jcurand.curandRngType.CURAND_RNG_PSEUDO_DEFAULT;
import static jcuda.runtime.JCuda.cudaMalloc;
import static jcuda.runtime.JCuda.cudaMemcpy;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToHost;

public class CudaRand {
    public static void main(String[] args) {
        int n = 1000000;
        // Allocate device memory
        Pointer deviceData = new Pointer();
        cudaMalloc(deviceData, n * Sizeof.FLOAT);

        // Create and initialize a pseudo-random number generator
        curandGenerator generator = new curandGenerator();
        curandCreateGenerator(generator, CURAND_RNG_PSEUDO_DEFAULT);
        curandSetPseudoRandomGeneratorSeed(generator, 1234);

        // Generate random numbers
        curandGenerateUniform(generator, deviceData, n);

        // Copy the random numbers from the device to the host
        float hostData[] = new float[n];
        cudaMemcpy(Pointer.to(hostData), deviceData,
                n * Sizeof.FLOAT, cudaMemcpyDeviceToHost);

    }
}
