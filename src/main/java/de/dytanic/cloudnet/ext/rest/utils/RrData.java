package de.dytanic.cloudnet.ext.rest.utils;

import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.ext.rest.CloudNetRestModule;

import java.util.*;

public class RrData {

    private final CloudNetRestModule cloudNetRestModule;

    public RrData(CloudNetRestModule cloudNetRestModule) {
        this.cloudNetRestModule = cloudNetRestModule;
    }

    public void runData() {
        new Thread(() -> {
            while (true) {
                Calendar calendar = Calendar.getInstance();
                int minutes = calendar.get(Calendar.MINUTE);
                if(minutes == 0 || minutes == 5 || minutes == 10 || minutes == 15 || minutes == 20 || minutes == 25 ||
                        minutes == 30 || minutes == 35 || minutes == 40 || minutes == 45 || minutes == 50 || minutes ==55) {
                    if(cloudNetRestModule.getRrDataMemory().size() >= 10) {
                        cloudNetRestModule.getRrDataMemory().remove(0);
                        cloudNetRestModule.getRrDataCpu().remove(0);
                    }
                    Map<String, Object> memory = new HashMap<>();
                    memory.put("time", calendar.getTimeInMillis());
                    memory.put("memory",
                            String.valueOf(cloudNetRestModule.getCloudNet().getClusterNodeServerProvider().getSelfNode()
                                    .getNodeInfoSnapshot().getUsedMemory()));
                    Map<String, Object> cpu = new HashMap<>();
                    cpu.put("time", calendar.getTimeInMillis());
                    cpu.put("cpu",
                            String.valueOf(cloudNetRestModule.getCloudNet().getClusterNodeServerProvider().getSelfNode()
                                    .getNodeInfoSnapshot().getSystemCpuUsage()));
                    cloudNetRestModule.getRrDataMemory().add(memory);
                    cloudNetRestModule.getRrDataCpu().add(cpu);
                }
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
