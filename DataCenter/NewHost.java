package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewHost extends PowerHostUtilizationHistory{

    private double utilizationMips;
    private double previousUtilizationMips;
    private final List<HostStateHistoryEntry> stateHistory = new LinkedList<HostStateHistoryEntry>();
    private double cloudResponseTime = 0.01;

    public NewHost(
            int id,
            RamProvisioner ramProvisioner,
            BwProvisioner bwProvisioner,
            long storage,
            List<? extends Pe> peList,
            VmScheduler vmScheduler,
            PowerModel powerModel) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
    }

    public double updateVmsProcessing(double currentTime) {
        Log.setDisabled(true);
        double smallerTime = super.updateVmsProcessing(currentTime);
        Log.setDisabled(false);
        setPreviousUtilizationMips(getUtilizationMips());
        setUtilizationMips(0);
        double hostTotalRequestedMips = 0;

        for (Vm vm : getVmList()) {
            getVmScheduler().deallocatePesForVm(vm);
        }

        for (Vm vm : getVmList()) {
            getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips());
        }

        for (Vm vm : getVmList()) {
            double totalRequestedMips = vm.getCurrentRequestedTotalMips();
            double totalAllocatedMips = getVmScheduler().getTotalAllocatedMipsForVm(vm);

            if (!Log.isDisabled()) {
                try {
                    Log.formatLine(
                            "%.2f: [Host #" + getId() + "] Total allocated MIPS for VM #" + vm.getId()
                                    + " (Host #" + vm.getHost().getId()
                                    + ") is %.2f, was requested %.2f out of total %.2f (%.2f%%)",
                            CloudSim.clock(),
                            totalAllocatedMips,
                            totalRequestedMips,
                            vm.getMips(),
                            totalRequestedMips / vm.getMips() * 100);
                }
                catch (Exception e){}

                List<Pe> pes = getVmScheduler().getPesAllocatedForVM(vm);
                StringBuilder pesString = new StringBuilder();
                for (Pe pe : pes) {
                    pesString.append(String.format(" PE #" + pe.getId() + ": %.2f.", pe.getPeProvisioner()
                            .getTotalAllocatedMipsForVm(vm)));
                }
                Log.formatLine(
                        "%.2f: [Host #" + getId() + "] MIPS for VM #" + vm.getId() + " by PEs ("
                                + getNumberOfPes() + " * " + getVmScheduler().getPeCapacity() + ")."
                                + pesString,
                        CloudSim.clock());
            }

            if (getVmsMigratingIn().contains(vm)) {
                Log.formatLine("%.2f: [Host #" + getId() + "] VM #" + vm.getId()
                        + " is being migrated to Host #" + getId(), CloudSim.clock());
            } else {
                if (totalAllocatedMips + 0.1 < totalRequestedMips) {
                    Log.formatLine("%.2f: [Host #" + getId() + "] Under allocated MIPS for VM #" + vm.getId()
                            + ": %.2f", CloudSim.clock(), totalRequestedMips - totalAllocatedMips);
                }

                vm.addStateHistoryEntry(
                        currentTime,
                        totalAllocatedMips,
                        totalRequestedMips,
                        (vm.isInMigration() && !getVmsMigratingIn().contains(vm)));

                if (vm.isInMigration()) {
                    Log.formatLine(
                            "%.2f: [Host #" + getId() + "] VM #" + vm.getId() + " is in migration",
                            CloudSim.clock());
                    totalAllocatedMips /= 0.9;
                }
            }

            setUtilizationMips(getUtilizationMips() + totalAllocatedMips);
            hostTotalRequestedMips += totalRequestedMips;
        }

        addStateHistoryEntry(
                currentTime,
                getUtilizationMips(),
                hostTotalRequestedMips,
                (getUtilizationMips() > 0));

        return smallerTime;
    }

    public List<Vm> getCompletedVms() {
        List<Vm> vmsToRemove = new ArrayList<Vm>();
        for (Vm vm : getVmList()) {
            if (vm.isInMigration()) {
                continue;
            }
            if (vm.getCurrentRequestedTotalMips() == 0) {
                vmsToRemove.add(vm);
            }
        }
        return vmsToRemove;
    }

    public double getMaxUtilization() {
        return PeList.getMaxUtilization(getPeList());
    }

    public double getMaxUtilizationAmongVmsPes(Vm vm) {
        return PeList.getMaxUtilizationAmongVmsPes(getPeList(), vm);
    }

    public double getUtilizationOfRam() {
        return getRamProvisioner().getUsedRam();
    }

    public double getUtilizationOfBw() {
        return getBwProvisioner().getUsedBw();
    }

    public double getUtilizationOfCpu() {
        double utilization = getUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    public double getPreviousUtilizationOfCpu() {
        double utilization = getPreviousUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    public double getUtilizationOfCpuMips() {
        return getUtilizationMips();
    }

    public double getUtilizationMips() {
        return utilizationMips;
    }

    protected void setUtilizationMips(double utilizationMips) {
        this.utilizationMips = utilizationMips;
    }

    public double getPreviousUtilizationMips() {
        return previousUtilizationMips;
    }

    protected void setPreviousUtilizationMips(double previousUtilizationMips) {
        this.previousUtilizationMips = previousUtilizationMips;
    }

    public List<HostStateHistoryEntry> getStateHistory() {
        return stateHistory;
    }

    public double getResponseTime(){return cloudResponseTime;}

    public void addStateHistoryEntry(double time, double allocatedMips, double requestedMips, boolean isActive) {

        HostStateHistoryEntry newState = new HostStateHistoryEntry(
                time,
                allocatedMips,
                requestedMips,
                isActive);
        if (!getStateHistory().isEmpty()) {
            HostStateHistoryEntry previousState = getStateHistory().get(getStateHistory().size() - 1);
            if (previousState.getTime() == time) {
                getStateHistory().set(getStateHistory().size() - 1, newState);
                return;
            }
        }
        getStateHistory().add(newState);
    }
}
