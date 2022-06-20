package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.core.CloudSim;

public class NewVm extends PowerVm {

    public double startTime = 0;

    public double totalMigrationTime = 0;

    public int delay = 0;

    public NewVm(
            final int id,
            final int userId,
            final double mips,
            final int pesNumber,
            final int ram,
            final long bw,
            final long size,
            final int priority,
            final String vmm,
            final CloudletScheduler cloudletScheduler,
            final double schedulingInterval,
            final int delay) {
        super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, schedulingInterval);
        this.startTime = CloudSim.clock();
        this.delay = delay;
    }
}
