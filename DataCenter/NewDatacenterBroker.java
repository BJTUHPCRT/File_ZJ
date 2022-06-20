package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

public class NewDatacenterBroker extends DatacenterBroker {

    private static final int BROKER_SUBMIT_VMS_NOW = 50;
    private static final int BROKER_CLOUDLETS_NOW = 50;

    public NewDatacenterBroker(String name) throws Exception {
        super(name);
    }

    @Override
    public void createVmsAfter(List<? extends Vm> vms, double delay) {
        if(getVmList().size() > 90){
            return;
        }
        if (started) {
            send(getId(), delay, BROKER_SUBMIT_VMS_NOW, vms);
        } else {
            presetEvent(getId(), BROKER_SUBMIT_VMS_NOW, vms, delay);
        }
    }

    @Override
    public void submitCloudletList(List<? extends Cloudlet> cloudlets, double delay) {
        if(getVmList().size() > 90){
            return;
        }
        if (started) {
            send(getId(), delay, BROKER_CLOUDLETS_NOW, cloudlets);
        } else {
            presetEvent(getId(), BROKER_CLOUDLETS_NOW, cloudlets, delay);
        }
    }

}
