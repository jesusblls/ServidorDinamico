package test;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.List;

public class SystemEvaluator {
    public static int calculateSystemScore() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        // Obtener RAM
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal(); // RAM total en bytes

        // Obtener Procesador
        CentralProcessor processor = hardware.getProcessor();
        int cores = processor.getLogicalProcessorCount();
        long frequency = processor.getMaxFreq(); // Frecuencia máxima en Hz

        // Obtener Ancho de Banda (Simulación)
        long bandwidth = getNetworkBandwidth(hardware);

        // Calcular el puntaje
        int ramScore = (int) (totalMemory / (1024 * 1024 * 1024)); // RAM en GB
        int coreScore = cores * 10; // Ponderación para núcleos
        int freqScore = (int) (frequency / 1_000_000_000); // Frecuencia en GHz
        int bandwidthScore = (int) (bandwidth / 1_000_000); // Ancho de banda en Mbps

        // Fórmula de puntaje total
        return ramScore * 2 + coreScore + freqScore * 5 + bandwidthScore;
    }

    private static long getNetworkBandwidth(HardwareAbstractionLayer hardware) {
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        if (!networkIFs.isEmpty()) {
            return networkIFs.get(0).getSpeed(); // Velocidad del primer adaptador
        }
        return 100_000_000; // Valor por defecto: 100 Mbps
    }
}
