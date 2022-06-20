package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.util.MathUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;

public class NewDatacenter extends PowerDatacenter {

    public NewDatacenterBroker broker;

    private double[] hostEnergy;

    private double savedCurrentTime;

    private double savedLastTime;

    private double savedTimeDiff;

    private double numVmsEnded;

    private int InputLimit = 100;

    private int lastMigrationCount = 0;

    private PowerVmAllocationPolicyMigrationStaticThreshold vmAllocSt = new PowerVmAllocationPolicyMigrationStaticThreshold(this.getHostList(),null,0.9);

    private PowerVmSelectionPolicyMinimumUtilization vmSelMu = new PowerVmSelectionPolicyMinimumUtilization();

    protected static Process pythonProc;

    protected static PrintStream toPython;

    protected  static BufferedReader fromPython;

    public NewDatacenter(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            NewDatacenterBroker broker,
            String execFile) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        this.broker = broker;
        this.hostEnergy = new double[this.getHostList().size()];
        try{
            ProcessBuilder pb = new ProcessBuilder("python",execFile);
            pythonProc = pb.start();
        }
        catch(Exception e){System.out.println(e.getMessage());}
        System.out.println("Ran python code : " + execFile);
        toPython = new PrintStream(pythonProc.getOutputStream());
        fromPython = new BufferedReader(new InputStreamReader(pythonProc.getInputStream(), Charset.defaultCharset()));
    }

    protected void updateDLModel(){
        String loss = "";
        try{
            if(getVmAllocationPolicy().getClass().getName().equals("org.cloudbus.cloudsim.power.DRLVmPlacementPolicy")){
                toPython.println(getLoss()+"END");
                toPython.flush();
            }
            else{
                toPython.println((getInputMap()+"END"));
                toPython.flush();
            }
           loss = NewDatacenter.fromPython.readLine();
            toPython.println((getInput()+"END"));
            toPython.flush();
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String getLoss(){
        String loss = "";
        double totalDataCenterEnergy = 0.0;
        double totalDataCenterCost = 0.0;
        for(NewHost host : this.<NewHost>getHostList()){
            totalDataCenterEnergy += this.hostEnergy[getHostList().indexOf(host)];
        }
        loss = loss + "CurrentTime " + this.savedCurrentTime +  "\n";
        loss = loss + "LastTime " + this.savedLastTime +  "\n";
        loss = loss + "TimeDiff " + this.savedTimeDiff +  "\n";
        loss = loss + "TotalEnergy " + totalDataCenterEnergy +  "\n";
        loss = loss + "NumVsEnded " + this.numVmsEnded +  "\n";
        this.numVmsEnded = 0;
        loss = loss + "SLAOverall " + getSlav(this.getVmList()) +  "\n";
        loss = loss + "VMsMigrated " + (this.getMigrationCount() - this.lastMigrationCount) + "\n";
        this.lastMigrationCount = this.getMigrationCount();
        if(getVmAllocationPolicy().getClass().getName().equals("org.cloudbus.cloudsim.power.DRLVmPlacementPolicy")){
            loss = loss + "HostPenalty " + ((NewVmPlacementPolicy) getVmAllocationPolicy()).hostPenalty +  "\n";
            loss = loss + "MigrationPenalty " + ((PowerVmSelectionPolicy)
                    ((NewVmPlacementPolicy)
                            getVmAllocationPolicy()).getVmSelectionPolicy()).migrationPenalty +  "\n";
        }
        return loss;
    }

    public String getInput(){
        String input = "";
        input = input + "number of VMs " + this.getVmList().size() +  "\n";
        String temp;
        for(PowerVm vm : this.<PowerVm>getVmList()){
            temp = "";
            PowerHost host = (PowerHost)vm.getHost();
            temp = temp + vm.getNumberOfPes() + " ";
            temp = temp + MathUtil.sum(vm.getCurrentRequestedMips()) + " ";
            temp = temp + vm.getCurrentRequestedMaxMips() + " ";
            temp = temp + vm.getUtilizationMean() + " ";
            temp = temp + vm.getUtilizationVariance() + " ";
            temp = temp + vm.getSize() + " ";
            temp = temp + vm.getCurrentAllocatedSize() + " ";
            temp = temp + vm.getRam() + " ";
            temp = temp + vm.getCurrentAllocatedRam() + " ";
            temp = temp + vm.getCurrentRequestedRam() + " ";
            temp = temp + vm.getBw() + " ";
            temp = temp + vm.getCurrentAllocatedBw() + " ";
            temp = temp + vm.getCurrentRequestedBw() + " ";
            temp = temp + vm.isInMigration() + " ";
            ArrayList<Vm> list = new ArrayList<Vm>(Arrays.asList(vm));
            temp = temp + getSlav(list) + " ";
            temp = temp + ((host != null) ? (host.getUtilizationOfCpuMips()) : "NA")  + " ";
            temp = temp + ((host != null) ? (host.getAvailableMips()) : "NA")  + " ";
            temp = temp + ((host != null) ? (host.getRamProvisioner().getAvailableRam()) : "NA")  + " ";
            temp = temp + ((host != null) ? (host.getBwProvisioner().getAvailableBw()) : "NA")  + " ";
            temp = temp + ((host != null) ? (host.getDiskBwProvisioner().getAvailableDiskBw()) : "NA")  + " ";
            temp = temp + ((host != null) ? (host.getVmList().size()) : "0")  + " ";
            temp = temp + ((host != null) ? (this.hostEnergy[getHostList().indexOf(vm.getHost())]) : "NA") + " ";
            temp = temp + ((host != null) ? (((double)vm.getRam())/vm.getHost().getBw()) : "NA");
            input = input + temp +  "\n";
        }
        input = input + "LSTM data\n";
        for(NewHost host : this.<NewHost>getHostList()){
            temp = "";
            temp = temp + this.hostEnergy[getHostList().indexOf(host)] + " ";
            temp = temp + host.getPower() + " ";
            temp = temp + host.getMaxPower() + " ";
            temp = temp + getSlav(host.getVmList()) + " ";
            temp = temp + host.getUtilizationOfCpu() + " ";
            temp = temp + host.getMaxUtilization() + " ";
            temp = temp + host.getNumberOfPes() + " ";
            temp = temp + host.getRamProvisioner().getAvailableRam() + " ";
            temp = temp + host.getRamProvisioner().getRam() + " ";
            temp = temp + host.getBwProvisioner().getAvailableBw() + " ";
            temp = temp + host.getBwProvisioner().getBw() + " ";
            temp = temp + host.getVmsMigratingIn().size() + " ";
            temp = temp + vmAllocSt.isHostOverUtilized(host) + " ";
            input = input + temp +  "\n";
        }
        return input;
    }

    public String oneHot(int value, int range){
        String res = "";
        for(int i = 0; i < range; i++){
            res = res + ((value == i) ? "1" : "0");
            if(i < range - 1)
                res += " ";
        }
        return res;
    }

    @Override
    protected void updateCloudletProcessing() {
        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            return;
        }
        double currentTime = CloudSim.clock();

        if (currentTime > getLastProcessTime()) {
            double minTime = this.updateCloudetProcessingWithoutSchedulingFutureEventsForce();
            System.out.println((int)currentTime/3600 + " hr " + ((int)(currentTime/60)-(60*((int)currentTime/3600))) + " min - " + getVmList().size());

            if (!isDisableMigrations()) {
                List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
                        getVmList());

                if (migrationMap != null) {
                    for (Map<String, Object> migrate : migrationMap) {
                        NewVm vm = (NewVm) migrate.get("vm");
                        PowerHost targetHost = (PowerHost) migrate.get("host");
                        PowerHost oldHost = (PowerHost) vm.getHost();
                        targetHost.addMigratingInVm(vm);
                        incrementMigrationCount();
                        send(
                                getId(),
                                vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
                                CloudSimTags.VM_MIGRATE,
                                migrate);
                        vm.totalMigrationTime += (vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)));
                    }
                }
            }

            if (minTime != Double.MAX_VALUE) {
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            }
            setLastProcessTime(currentTime);
        }
    }

    @Override
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        this.savedCurrentTime = currentTime;
        this.savedLastTime = getLastProcessTime();
        this.savedTimeDiff = timeDiff;

        for (NewHost host : this.<NewHost> getHostList()) {
            double time = host.updateVmsProcessing(currentTime);
            if (time < minTime) {
                minTime = time;
            }
        }

        if (timeDiff > 0) {
            for (PowerHost host : this.<PowerHost> getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;
                this.hostEnergy[this.getHostList().indexOf(host)] = timeFrameHostEnergy;
            }
        }
        setPower(getPower() + timeFrameDatacenterEnergy);
        checkCloudletCompletion();
        for (NewHost host : this.<NewHost> getHostList()) {
            for (Vm vm : host.getCompletedVms()) {
                if(currentTime - ((NewVm)vm).startTime < 2 || vm.getCloudletScheduler().getCloudletExecList().size() < 1){
                    continue;
                }
                processVMDestroy(vm);
            }
        }
        for(Vm vm : getVmList()){
            if(vm.getHost() == null){
                getVmAllocationPolicy().deallocateHostForVm(vm);
                getVmList().remove(vm);
            }
        }
        int numRemove = getVmList().size()-this.InputLimit;
        for(int i = 0; i < Math.max(0, numRemove); i++){
            Vm vm = getVmList().get(i);
            getVmAllocationPolicy().deallocateHostForVm(vm);
            getVmList().remove(vm);
        }
        if(this.savedTimeDiff > 300){
            updateDLModel();
        }
        setLastProcessTime(currentTime);
        return minTime;
    }

    public String getVmHostMap(){
        String map = "";
        for(Vm vm : this.getVmList()){
            map = map + ("VM #" + vm.getId() + " index " + this.getVmList().indexOf(vm) + " <-> Host #" + vm.getHost().getId()) + "\n";
        }
        return map;
    }

    public String getInputMap(){
        String map = "";
        for(Vm vm : this.getVmList()){
            map = map + (this.getVmList().indexOf(vm) + " " + vm.getHost().getId()) +  "\n";
        }
        return map;
    }

    public void processVMDestroy(Vm vm) {
        int vmId = vm.getId();

        broker.getVmsCreatedList().remove(vm);
        broker.finilizeVM(vm);

        for (Cloudlet cloudlet : broker.getCloudletSubmittedList()) {
            if (!cloudlet.isFinished() && vmId == cloudlet.getVmId()) {
                try {
                    vm.getCloudletScheduler().cloudletCancel(cloudlet.getCloudletId());
                    cloudlet.setCloudletStatus(Cloudlet.FAILED_RESOURCE_UNAVAILABLE);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                sendNow(cloudlet.getUserId(), CloudSimTags.CLOUDLET_RETURN, cloudlet);
            }
        }
        this.numVmsEnded += 1;
        getVmAllocationPolicy().deallocateHostForVm(vm);
        getVmList().remove(vm);
        this.broker.getVmList().remove(vm);
    }

    protected static double getSlav(List<Vm> vms) {
        Map<String, Double> metrics = new HashMap<String, Double>();
        List<Double> slaViolation = new LinkedList<Double>();
        double totalAllocated = 0;
        double totalRequested = 0;

        for (Vm vm : vms) {
            double vmTotalAllocated = 0;
            double vmTotalRequested = 0;
            double previousTime = -1;
            double previousAllocated = 0;
            double previousRequested = 0;
            boolean previousIsInMigration = false;

            for (VmStateHistoryEntry entry : vm.getStateHistory()) {
                if (previousTime != -1) {
                    double timeDiff = entry.getTime() - previousTime;
                    vmTotalAllocated += previousAllocated * timeDiff;
                    vmTotalRequested += previousRequested * timeDiff;

                    if (previousAllocated < previousRequested) {
                        slaViolation.add((previousRequested - previousAllocated) / previousRequested);
                        if (previousIsInMigration) {
                            vmUnderAllocatedDueToMigration += (previousRequested - previousAllocated)
                                    * timeDiff;
                        }
                    }
                }

                previousAllocated = entry.getAllocatedMips();
                previousRequested = entry.getRequestedMips();
                previousTime = entry.getTime();
                previousIsInMigration = entry.isInMigration();
            }

            totalAllocated += vmTotalAllocated;
            totalRequested += vmTotalRequested;
        }

        Double slav = 0.0;
        for (Double d : slaViolation) {
            slav += d;
        }

        return slav;
    }
}
